package top.kgame.lib.snapshot;

import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kgame.lib.snapshot.core.SnapshotObjectTracker;
import top.kgame.lib.snapshot.tools.SnapshotUtil;
import top.kgame.lib.snapshot.tools.ReplicatedReader;
import top.kgame.lib.snapshot.tools.ReplicatedUtil;

import java.util.*;

/**
 * 快照客户端连接，代表房间内一个玩家，负责向该玩家发送全量/增量快照，并处理其 ack 与上行数据。
 * <p>
 * <b>线程模型</b>：单线程写、单线程读。对本 Client 的调用（如 {@link #sendPackage(int)}、{@link #deserializer(int, byte[])} 等）
 * 必须在同一线程执行，与对应 {@link SnapshotServer} 的线程模型一致，通常由房间主循环线程统一驱动。
 * </p>
 */
public abstract class SnapshotClient {
    private static final Logger logger = LogManager.getLogger(SnapshotClient.class);

    private final DeserializeFactory deserializeFactory;
    private final SnapshotServer snapshotServer;
    private final long uid;

    private int inSequence;
    private int lastSendSequence;

    public SnapshotClient(long uid, SnapshotServer snapshotServer) {
        this.uid = uid;
        this.deserializeFactory = snapshotServer.getDeserializeFactory();
        this.snapshotServer = snapshotServer;
    }

    /**
     * 获取客户端唯一标识（创建连接时传入的 connectionId）
     */
    public long getUid() {
        return uid;
    }

    /**
     * 根据参数提供的序列号发送快照数据。如果上次ack的序列号与目标序列号插值小于buffer大小则发送差异快照，否则发送全量快照
     * @param targetSequence 目标序列号
     */
    public void sendPackage(int targetSequence) {
        if (targetSequence > snapshotServer.getSequence()) {
            throw new IllegalArgumentException("targetSequence > snapshotServer.getSequence. invalid targetSequence: " + targetSequence);
        }

        int baseLine = lastSendSequence;
        if (targetSequence - lastSendSequence > SnapshotConfig.SnapshotBufferSize) {
            baseLine = Integer.MIN_VALUE;
        }
        if (baseLine > 0) {
            sendAdditionSnapshot(baseLine, targetSequence);
        } else {
            sendFullSnapshot(targetSequence);
        }
        if (targetSequence > lastSendSequence) {
            lastSendSequence = targetSequence;
        }
    }

    //发送全量快照
    private void sendFullSnapshot(int serverSequence) {
        List<byte[]> updateEntity = new ArrayList<>();
        for (SnapshotObjectTracker entityInfo : snapshotServer.getAllReplicateEntity()) {
            if (entityInfo.getCreateSequence() == 0) {
                continue;
            }
            boolean destroyed = entityInfo.getDestroySequence() > 0;
            if (destroyed) {
                continue;
            }
            updateEntity.add(entityInfo.getSnapshot(serverSequence));
        }

        ByteBuf byteBuf = SnapshotUtil.getByteBuf(SnapshotUtil.BYTE_BUF_SIZE_HUGE);
        ReplicatedUtil.writeVarInt(byteBuf, updateEntity.size());
        for (byte[] info : updateEntity) {
            byteBuf.writeBytes(info);
        }
        byte[] updateBytes = SnapshotUtil.byteBufToByteArray(byteBuf);
        sendFullSnapshot(inSequence, serverSequence, updateBytes, snapshotServer.getCreateReplicateId());
    }

    //发送增量快照
    private void sendAdditionSnapshot(int baseLine, int serverSequence){
        List<Integer> destroyIds = new ArrayList<>();
        List<byte[]> updateEntity = new ArrayList<>();
        for (SnapshotObjectTracker entityInfo : snapshotServer.getAllReplicateEntity()) {
            if (entityInfo.getCreateSequence() == 0) {
                continue;
            }
            boolean destroyed = entityInfo.getDestroySequence() > 0;
            if (destroyed && entityInfo.getDestroySequence() < baseLine) {
                continue;
            }
            int sendSequence = serverSequence;
            if (destroyed) {
                sendSequence = Math.max(entityInfo.getCreateSequence(), entityInfo.getDestroySequence());
            }
            byte[] additionSnapshot = entityInfo.generateAdditionSnapshot(baseLine, sendSequence);
            if (additionSnapshot != null) {
                updateEntity.add(additionSnapshot);
            }
            if (destroyed) {
                destroyIds.add(entityInfo.getId());
            }
        }

        ByteBuf byteBuf = SnapshotUtil.getByteBuf(SnapshotUtil.BYTE_BUF_SIZE_LARGE);
        ReplicatedUtil.writeVarInt(byteBuf, updateEntity.size());
        for (byte[] info : updateEntity) {
            byteBuf.writeBytes(info);
        }
        byte[] updateBytes = SnapshotUtil.byteBufToByteArray(byteBuf);

        sendAdditionSnapshot(inSequence, serverSequence, updateBytes, snapshotServer.getCreateReplicateId(), destroyIds);
    }

    /**
     * 向client发送增量快照数据
     * @param inSequence 该帧快照对应的客户端快照id
     * @param outSequence   该帧快照的服务器快照id
     * @param updateBytes   更新数据的实体id
     * @param createIds     outSequence帧创建的实体id
     * @param destroyIds    已经删除的实体id
     */
    protected abstract void sendAdditionSnapshot(int inSequence, int outSequence, byte[] updateBytes, Collection<Integer> createIds, Collection<Integer> destroyIds);

    /**
     * 向client发送全量快照数据
     * @param inSequence 该帧快照对应的客户端快照id
     * @param outSequence   该帧快照的服务器快照id
     * @param updateBytes   更新数据的实体id
     * @param createIds     outSequence帧创建的实体id
     */
    protected abstract void sendFullSnapshot(int inSequence, int outSequence, byte[] updateBytes, Collection<Integer> createIds);

    public int getLastSendSequence() {
        return lastSendSequence;
    }

    /**
     * 反序列化输入数据
     * @param inSequence 输入数据序列号
     * @param byteArray 二进制数据
     */
    public void deserializer(int inSequence, byte[] byteArray) {
        this.inSequence = inSequence;
        ByteBuf byteBuf = Unpooled.wrappedBuffer(byteArray);
        int size = ReplicatedUtil.readVarInt(byteBuf);
        ReplicatedReader reader = ReplicatedReader.getInstance(byteBuf);
        for (int i = 0; i < size; i++) {
            DeserializeObject entity = deserializeFactory.deserialize(reader);
            if (null == entity) {
                logger.error("ConnectionId:{} deserialize entity failed! inSequence:{} lastAckSequence:{} dataIndex:{}.",
                        uid, inSequence, lastSendSequence, i);
                return;
            }
            receive(inSequence, entity);
        }
    }

    protected abstract void receive(int inSequence, DeserializeObject deserializeObject);
}