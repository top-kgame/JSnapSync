# JSnapSync

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/javase-downloads.html)

JSnapSync 是一个专为游戏服务器设计的 Java 快照同步库。采用对象-属性模式，以对象和属性为粒度支持增量快照同步。

如果这个项目帮到了你，欢迎点个 star⭐ 支持一下～ 这会让更多人发现它 😊

## 🚀 核心特性

- **对象-属性架构**：以对象（SerializeObject）与属性（SerializeAttribute）组织同步数据，支持灵活的数据结构设计
- **增量同步**：仅同步发生变化的属性数据，大幅减少网络带宽使用
- **高性能序列化**：使用优化的二进制序列化协议（VarInt、ByteBuf），支持多种基础类型与集合
- **缓冲机制**：内置快照缓冲区（SnapshotBuffer），支持历史状态追溯和全量/增量快照发送

## 📋 系统要求

- Java 21 或更高版本
- Maven 3.6 或更高版本

## 🏗️ 架构概览

JSnapSync 采用以下核心架构：

#### 同步对象层次结构

对象即参与同步的实体（如玩家、怪物、道具），属性即挂在对象上的数据块（如血量、位置、背包等）；一个对象可包含多个属性，对应接口如下：

```
同步对象 (SerializeObject / DeserializeObject)
├── 同步属性1 (SerializeAttribute / DeserializeAttribute)
│   ├── 字段1
│   ├── 字段2
│   └── ...
├── 同步属性2 (SerializeAttribute / DeserializeAttribute)
└── 同步属性3 (SerializeAttribute / DeserializeAttribute)
```

#### 消息结构

JSnapSync 使用二进制协议进行高效数据传输：

```
单帧消息: [实体数量] [实体1] [实体2] ...
实体数据: [UID] [类型ID] [属性数量] [属性1] [属性2] ...
属性数据: [属性字节长度] [属性ID] [字段数据...]
```

**特点**：

- 紧凑的二进制格式（如整数 VarInt 编码）
- 支持增量同步与全量快照
- 自动处理数据类型和长度编码

#### 核心类

- **SnapshotServer**：快照服务端，管理对象注册、快照生成与向各客户端的广播
- **SnapshotClient**：快照客户端连接，代表房间内一个玩家，负责全量/增量快照发送及上行数据反序列化
- **SnapshotObjectTracker**：单个同步对象的快照跟踪器，管理该对象的历史快照与差异
- **SnapshotBuffer**：快照缓冲区，按序列号存储历史快照
- **AttributeSerializer**：属性序列化器，将 SerializeAttribute 序列化为字节
- **DeserializeFactory**：反序列化工厂，根据类型 ID 创建 DeserializeObject 并驱动反序列化

## 🔧 快速开始

#### 1. 定义同步属性

实现 `SerializeAttribute` 与 `DeserializeAttribute`，使用 `ReplicatedWriter` / `ReplicatedReader` 读写字段，顺序需一致。

```java
public class PlayerAttribute implements SerializeAttribute, DeserializeAttribute {
    private String name;
    private int level;
    private float health;

    @Override
    public Integer getTypeId() {
        return 1; // 属性类型 ID
    }

    @Override
    public void serialize(ReplicatedWriter writer) {
        writer.writeString(name);
        writer.writeInteger(level);
        writer.writeFloat(health);
    }

    @Override
    public void deserialize(ReplicatedReader reader) {
        name = reader.readString();
        level = reader.readInteger();
        health = reader.readFloat();
    }

    // getters and setters...
}
```

#### 2. 定义同步对象

实现 `SerializeObject` 与 `DeserializeObject`：序列化侧通过 `getAttributes()` 提供属性集合；反序列化侧在 `deserializeAttribute(ReplicatedReader reader)` 中按“属性数量 + 每块长度 + 类型 ID + 数据”读取并填充各属性。

```java
public class Player implements SerializeObject, DeserializeObject {
    private int guid;
    private int typeId;
    private final List<SerializeAttribute> attributes = new ArrayList<>();

    public Player(int guid, int typeId) {
        this.guid = guid;
        this.typeId = typeId;
        attributes.add(new PlayerAttribute());
    }

    public Player() {}

    @Override
    public Collection<SerializeAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public int getGuid() {
        return guid;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void setGuid(int id) {
        this.guid = id;
    }

    @Override
    public void setTypeId(int type) {
        this.typeId = type;
    }

    @Override
    public void deserializeAttribute(ReplicatedReader reader) {
        int size = reader.readInteger();
        for (int i = 0; i < size; i++) {
            int attributeSize = reader.readInteger();
            int attributeType = reader.readInteger();
            // 根据 attributeType 找到对应属性并 deserialize(reader)
        }
    }
}
```

#### 3. 创建快照服务端

继承 `SnapshotServer`，在构造函数或初始化方法中注册实体类型与客户端生成方式；游戏循环中调用 `stepSnapshot()`。

```java
public class GameSnapshotServer extends SnapshotServer {

    public GameSnapshotServer() {
        getDeserializeFactory().registerEntityType(100, () -> new Player(0, 100));
        getDeserializeFactory().registerEntityType(101, () -> new Monster(0, 101));
    }

    @Override
    protected SnapshotClient generateConnection(long connectionId) {
        return new GameSnapshotClient(connectionId, this);
    }

    @Override
    protected void onClientRemove(SnapshotClient client) {}

    @Override
    protected void onClientAdd(SnapshotClient client) {}

    public void gameLoop() {
        // 注册对象、生成/移除客户端后，每帧调用
        stepSnapshot();
    }
}
```

- 注册对象：`registerObject(SerializeObject entity)`
- 注销对象：`unregisterEntity(SerializeObject)` 或 `unregisterEntity(int replicateId)`
- 生成客户端连接：`generateClient(long clientId)`
- 移除客户端：`removeClient(SnapshotClient)` 或 `removeClient(long clientId)`

#### 4. 实现快照客户端连接

继承 `SnapshotClient`，实现 `sendFullSnapshot`、`sendAdditionSnapshot` 和 `receive`。服务端每帧对每个客户端调用 `sendPackage(serverSequence)`，由库内部根据缓冲决定发全量或增量；客户端上行数据通过 `deserializer(inSequence, byteArray)` 注入，反序列化后的对象会回调 `receive(inSequence, deserializeObject)`。

```java
public class GameSnapshotClient extends SnapshotClient {

    public GameSnapshotClient(long uid, SnapshotServer server) {
        super(uid, server);
    }

    @Override
    protected void sendFullSnapshot(int inSequence, int outSequence, byte[] updateBytes, Collection<Integer> createIds) {
        // 将全量快照数据发送给客户端
    }

    @Override
    protected void sendAdditionSnapshot(int inSequence, int outSequence, byte[] updateBytes,
                                       Collection<Integer> createIds, Collection<Integer> destroyIds) {
        // 将增量快照数据发送给客户端
    }

    @Override
    protected void receive(int inSequence, DeserializeObject deserializeObject) {
        // 处理客户端上行反序列化后的对象
    }
}
```

- 服务端驱动发送：在 `stepSnapshot()` 之后对每个客户端调用 `client.sendPackage(server.getSequence())`（或等价序列号）。
- 客户端上行：收到客户端包时调用 `client.deserializer(inSequence, byteArray)`。

## 📊 支持的数据类型

#### 基础类型

- `byte`, `char`, `boolean`, `short`, `int`, `long`, `float`, `double`
- `String`, `byte[]`

#### 集合与数组

- `List<T>`：`writeBooleanList` / `readBooleanList`、`writeIntList` / `readIntList`、`writeStringList` / `readStringList` 等
- 基础类型数组：`writeIntArray` / `readIntArray`、`writeStringArray` / `readStringArray` 等
- 自定义对象列表：`writeObjList(List<? extends SerializeAttribute>)` / `readObjList(Class<T extends DeserializeAttribute>)`

#### 自定义结构体

- 实现 `SerializeAttribute` 与 `DeserializeAttribute` 的类，可作为字段或列表元素，通过 `writeObject` / `readObject(Class)`、`writeObjList` / `readObjList(Class)` 序列化。

## ⚙️ 高级特性

#### 增量快照

服务端通过 `SnapshotObjectTracker` 比较相邻序列号快照，仅在有变化时下发该对象的增量数据；若客户端落后超过 `SnapshotConfig.SnapshotBufferSize`（默认 64），则自动切换为全量快照。

#### 快照缓冲

- 服务端为每个已注册对象维护 `SnapshotBuffer`，用于历史快照与差异计算。
- 缓冲区大小由 `SnapshotConfig.SnapshotBufferSize` 控制。

#### 实体类型注册

- **Supplier**：`getDeserializeFactory().registerEntityType(typeId, () -> new Player(0, typeId));`
- **类引用**（需无参构造）：`getDeserializeFactory().registerEntityType(typeId, Player.class);`

可选：为实体类标注 `@SnapshotDeserializer(typeId)`，使用 `ClassUtils.getClassByAnnotation(packageName, SnapshotDeserializer.class)` 扫描后批量注册。

## 🧪 测试

运行测试套件：

```bash
mvn test
```

测试包含属性序列化/反序列化、边界值、增量快照等。

## 📝 注意事项

1. **对象与属性固定性**：对象注册后，其 `getAttributes()` 返回的集合不应在运行期增删；反序列化侧需能根据类型 ID 找到对应属性实例并填充。
2. **类型 ID 唯一性**：同一上下文中对象类型 ID、属性类型 ID 需唯一。
3. **序列化顺序**：`serialize` 与 `deserialize` 的字段顺序必须严格一致。
4. **线程模型**：SnapshotServer 与 SnapshotClient 均为**单线程写、单线程读**。对同一 Server/Client 的调用（如 `registerObject`、`stepSnapshot`、`generateClient`、`sendPackage`、`deserializer` 等）应在同一线程执行，适用于守望先锋等开房间类游戏中**一个房间**由单线程主循环驱动的场景，无需框架内部加锁。

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详细信息请查看 [LICENSE](LICENSE) 文件。

## 📋 后续开发计划

- 二进制快照反序列化可视化工具（进行中）
- 支持回放的快照数据存储模块（未开始）

## 🔗 相关链接

- [项目主页](https://github.com/ZKGameDev/JSnapSync)
- [问题反馈](https://github.com/ZKGameDev/JSnapSync/issues)
- [KGame 生态系统](https://github.com/ZKGameDev)

---

*JSnapSync - 让游戏服务器状态同步变得简单高效* 🎮
