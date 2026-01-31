package top.kgame.lib.snapshot;

import top.kgame.lib.snapshot.core.SnapshotObjectTracker;

import java.util.*;

public abstract class SnapshotServer {
    private int serverSequence = 1;
    private final Set<Integer> createIds = new HashSet<>();
    private final Map<Integer, SnapshotObjectTracker> replicateInfoMap = new TreeMap<>();
    private final Map<Long, SnapshotClient> clients = new HashMap<>();
    private final DeserializeFactory deserializeFactory = new DeserializeFactory();

    private Collection<SnapshotClient> getAllClient() {
        return clients.values();
    }

    protected abstract SnapshotClient generateConnection(long connectionId);
    protected abstract void onClientRemove(SnapshotClient client);
    protected abstract void onClientAdd(SnapshotClient client);
    /**
     * 注册一个客户端连接
     */
    public SnapshotClient generateClient(final long clientId) {
        SnapshotClient connection = generateConnection(clientId);
        clients.put(clientId, connection);
        onClientAdd(connection);
        return connection;
    }

    /**
     * 移除指定的客户端连接（如断线、踢出时调用）
     * @param client 要移除的客户端连接实例
     * @return 移除的client示例，如果移除失败返回null
     */
    public SnapshotClient removeClient(SnapshotClient client) {
        return removeClient(client.getUid());
    }

    /**
     * 根据客户端 ID 移除连接
     * @param clientId 客户端 ID（即创建连接时传入的 connectionId）
     * @return 被移除的客户端实例，若未找到则返回 null
     */
    public SnapshotClient removeClient(long clientId) {
        SnapshotClient removedClient = clients.remove(clientId);
        if (removedClient != null) {
            onClientRemove(removedClient);
        }
        return removedClient;
    }

    /**
     * 注册同步对象
     * @param entity 要注册的实体对象
     */
    public void registerObject(SerializeObject entity) {
        SnapshotObjectTracker replicateInfo = SnapshotObjectTracker.generate(entity);
        replicateInfo.setCreateSequence(serverSequence);
        replicateInfoMap.put(entity.getGuid(), replicateInfo);
        createIds.add(entity.getGuid());
    }

    /**
     * 根据ID注销一个实体
     * @param replicateId 实体的复制ID
     * @return 被注销的实体对象，如果不存在则返回null
     */
    public SerializeObject unregisterEntity(int replicateId) {
        SnapshotObjectTracker replicateInfo = replicateInfoMap.get(replicateId);
        if (null == replicateInfo) {
            return null;
        }
        replicateInfo.setDestroySequence(serverSequence);
        return replicateInfo.getEntity();
    }

    /**
     * 注销指定的实体对象
     * @param entity 要注销的实体对象
     */
    public void unregisterEntity(SerializeObject entity) {
        SnapshotObjectTracker replicateInfo = replicateInfoMap.get(entity.getGuid());
        if (null == replicateInfo) {
            return;
        }
        replicateInfo.setDestroySequence(serverSequence);
    }

    /**
     * 执行一个快照同步步骤
     * 包含生成快照、广播到所有连接、递增服务器序列号三个操作
     */
    public void stepSnapshot() {
        generateSnapshot();
        broadcastSnapshot();
        this.serverSequence++;
        createIds.clear();
    }

    private void generateSnapshot() {
        int minClientAck = calMinClientAck();
        ArrayList<Integer> needRemoveReplicateIds = new ArrayList<>();
        for (SnapshotObjectTracker info : replicateInfoMap.values()) {
            //移除已经所有客户端已经收到的已销毁的entity
            if (info.getDestroySequence() > 0 && minClientAck > info.getDestroySequence()) {
                needRemoveReplicateIds.add(info.getId());
                continue;
            }
            if (info.getDestroySequence() > 0 && this.serverSequence > info.getDestroySequence()) {
                continue;
            }
            info.generateEntitySnapshot(serverSequence);
            int preSequence = serverSequence - 1;
            if (info.hasSnapshot(preSequence)) {
                if (!info.compareSnapshotSame(preSequence, serverSequence)) {
                    info.setUpdateSequence(serverSequence);
                }
            } else {
                info.setUpdateSequence(serverSequence);
            }
        }
        for (Integer needRemoveId : needRemoveReplicateIds) {
            replicateInfoMap.remove(needRemoveId);
        }
    }

    private int calMinClientAck() {
        int minClientAck = Integer.MAX_VALUE;
        for (SnapshotClient connection : getAllClient()) {
            int ackedSequence = connection.getLastSendSequence();
            if (ackedSequence < minClientAck) {
                minClientAck = ackedSequence;
            }
        }
        return minClientAck;
    }

    private void broadcastSnapshot() {
        for (SnapshotClient connection : getAllClient()) {
            connection.sendPackage(serverSequence);
        }
    }

    /**
     * 获取当前服务器序列号
     * @return 当前服务器序列号
     */
    public int getSequence() {
        return serverSequence;
    }

    /**
     * 获取所有已注册的实体快照跟踪器
     * @return 实体快照跟踪器集合
     */
    public Collection<SnapshotObjectTracker> getAllReplicateEntity() {
        return replicateInfoMap.values();
    }

    /**
     * 获取本次创建的实体ID集合
     * @return 创建的实体ID集合
     */
    public Collection<Integer> getCreateReplicateId() {
        return createIds;
    }

    public DeserializeFactory getDeserializeFactory() {
        return deserializeFactory;
    }
}
