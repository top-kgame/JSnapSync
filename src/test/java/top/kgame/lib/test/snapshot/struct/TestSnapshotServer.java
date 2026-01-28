package top.kgame.lib.test.snapshot.struct;

import top.kgame.lib.snapshot.SnapshotClient;
import top.kgame.lib.snapshot.SnapshotServer;

public class TestSnapshotServer extends SnapshotServer {

    public TestSnapshotServer() {
        // 注册测试实体类型 - 使用Supplier提供默认参数
        getDeserializeFactory().registerEntityType(100, () -> new TestSyncObject(0, 100));
        getDeserializeFactory().registerEntityType(101, () -> new TestSyncObject(0, 101));
        
        // 或者使用类引用方式注册（需要无参构造函数）
        // getDeserializeFactory().registerEntityType(102, TestSyncObject.class);
    }

    @Override
    protected SnapshotClient generateConnection(long connectionId) {
        return new TestSnapshotClient(connectionId, this);
    }
}