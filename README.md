# JSnapSync

[License](https://opensource.org/licenses/Apache-2.0)
[Java](https://www.oracle.com/java/technologies/javase-downloads.html)

> 📖 **中文文档**: [README.zh-CN.md](README.zh-CN.md)

JSnapSync is a Java snapshot synchronization library designed for game servers. It uses an object-attribute model and supports incremental snapshot sync at the object and attribute level.

If this project helps you, please consider giving it a star ⭐ — it helps more people discover it 😊

## 🚀 Core Features

- **Object-attribute architecture**: Organize sync data with objects (`SerializeObject`) and attributes (`SerializeAttribute`) for flexible data structures
- **Incremental sync**: Sync only changed attribute data to significantly reduce network bandwidth
- **High-performance serialization**: Optimized binary protocol (VarInt, ByteBuf) supporting primitives and collections
- **Buffering**: Built-in snapshot buffer (`SnapshotBuffer`) for historical state tracking and full/incremental snapshot delivery

## 📋 Requirements

- Java 21 or later
- Maven 3.6 or later

## 🏗️ Architecture Overview

JSnapSync is built around the following core architecture:

#### Sync Object Hierarchy

Objects are entities that participate in sync (e.g. players, monsters, items). Attributes are data blocks attached to objects (e.g. health, position, inventory). One object can contain multiple attributes:

```
Sync Object (SerializeObject / DeserializeObject)
├── Sync Attribute 1 (SerializeAttribute / DeserializeAttribute)
│   ├── Field 1
│   ├── Field 2
│   └── ...
├── Sync Attribute 2 (SerializeAttribute / DeserializeAttribute)
└── Sync Attribute 3 (SerializeAttribute / DeserializeAttribute)
```

#### Message Structure

JSnapSync uses a binary protocol for efficient data transfer:

```
Frame:     [entity count] [entity 1] [entity 2] ...
Entity:    [UID] [type ID] [attribute count] [attr 1] [attr 2] ...
Attribute: [attribute byte length] [attribute ID] [field data...]
```

**Characteristics**:

- Compact binary format (e.g. VarInt encoding for integers)
- Supports incremental sync and full snapshots
- Automatic handling of data types and length encoding

#### Core Classes

- **SnapshotServer**: Snapshot server — manages object registration, snapshot generation, and broadcast to clients
- **SnapshotClient**: Snapshot client connection — represents one player in a room; handles full/incremental snapshot delivery and upstream deserialization
- **SnapshotObjectTracker**: Per-object snapshot tracker — manages historical snapshots and diffs for a single sync object
- **SnapshotBuffer**: Snapshot buffer — stores historical snapshots by sequence number
- **AttributeSerializer**: Attribute serializer — serializes `SerializeAttribute` to bytes
- **DeserializeFactory**: Deserialization factory — creates `DeserializeObject` instances by type ID and drives deserialization

## 🔧 Quick Start

#### 1. Define Sync Attributes

Implement `SerializeAttribute` and `DeserializeAttribute`. Use `ReplicatedWriter` / `ReplicatedReader` to read and write fields in the same order.

```java
public class PlayerAttribute implements SerializeAttribute, DeserializeAttribute {
    private String name;
    private int level;
    private float health;

    @Override
    public Integer getTypeId() {
        return 1; // attribute type ID
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

#### 2. Define Sync Objects

Implement `SerializeObject` and `DeserializeObject`. On the serialization side, provide attributes via `getAttributes()`. On the deserialization side, read and populate attributes in `deserializeAttribute(ReplicatedReader reader)` following the format: attribute count + per-block length + type ID + data.

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
            // find the matching attribute by attributeType and call deserialize(reader)
        }
    }
}
```

#### 3. Create a Snapshot Server

Extend `SnapshotServer`. Register entity types and client factory in the constructor or init method. Call `stepSnapshot()` in your game loop.

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
        // call every frame after registering objects and creating/removing clients
        stepSnapshot();
    }
}
```

- Register object: `registerObject(SerializeObject entity)`
- Unregister object: `unregisterEntity(SerializeObject)` or `unregisterEntity(int replicateId)`
- Create client connection: `generateClient(long clientId)`
- Remove client: `removeClient(SnapshotClient)` or `removeClient(long clientId)`

#### 4. Implement Snapshot Client Connection

Extend `SnapshotClient` and implement `sendFullSnapshot`, `sendAdditionSnapshot`, and `receive`. The server calls `sendPackage(serverSequence)` per client each frame; the library decides full vs incremental based on the buffer. Upstream client data is injected via `deserializer(inSequence, byteArray)`; deserialized objects are delivered through `receive(inSequence, deserializeObject)`.

```java
public class GameSnapshotClient extends SnapshotClient {

    public GameSnapshotClient(long uid, SnapshotServer server) {
        super(uid, server);
    }

    @Override
    protected void sendFullSnapshot(int inSequence, int outSequence, byte[] updateBytes, Collection<Integer> createIds) {
        // send full snapshot data to the client
    }

    @Override
    protected void sendAdditionSnapshot(int inSequence, int outSequence, byte[] updateBytes,
                                       Collection<Integer> createIds, Collection<Integer> destroyIds) {
        // send incremental snapshot data to the client
    }

    @Override
    protected void receive(int inSequence, DeserializeObject deserializeObject) {
        // handle upstream deserialized objects from the client
    }
}
```

- Server-driven send: after `stepSnapshot()`, call `client.sendPackage(server.getSequence())` (or equivalent sequence) for each client.
- Client upstream: on receiving a client packet, call `client.deserializer(inSequence, byteArray)`.

## 📊 Supported Data Types

#### Primitives

- `byte`, `char`, `boolean`, `short`, `int`, `long`, `float`, `double`
- `String`, `byte[]`

#### Collections and Arrays

- `List<T>`: `writeBooleanList` / `readBooleanList`, `writeIntList` / `readIntList`, `writeStringList` / `readStringList`, etc.
- Primitive arrays: `writeIntArray` / `readIntArray`, `writeStringArray` / `readStringArray`, etc.
- Custom object lists: `writeObjList(List<? extends SerializeAttribute>)` / `readObjList(Class<T extends DeserializeAttribute>)`

#### Custom Structs

- Classes implementing `SerializeAttribute` and `DeserializeAttribute` can be used as fields or list elements via `writeObject` / `readObject(Class)`, `writeObjList` / `readObjList(Class)`.

## ⚙️ Advanced Features

#### Incremental Snapshots

The server uses `SnapshotObjectTracker` to compare snapshots at adjacent sequence numbers and sends incremental data only when an object changes. If a client falls behind by more than `SnapshotConfig.SnapshotBufferSize` (default 64), a full snapshot is sent automatically.

#### Snapshot Buffering

- The server maintains a `SnapshotBuffer` per registered object for historical snapshots and diff computation.
- Buffer size is controlled by `SnapshotConfig.SnapshotBufferSize`.

#### Entity Type Registration

- **Supplier**: `getDeserializeFactory().registerEntityType(typeId, () -> new Player(0, typeId));`
- **Class reference** (requires no-arg constructor): `getDeserializeFactory().registerEntityType(typeId, Player.class);`

Optional: annotate entity classes with `@SnapshotDeserializer(typeId)` and batch-register via `ClassUtils.getClassByAnnotation(packageName, SnapshotDeserializer.class)`.

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

Tests cover attribute serialization/deserialization, boundary values, incremental snapshots, and more.

## 📝 Notes

1. **Fixed object and attribute layout**: After registration, the collection returned by `getAttributes()` should not be modified at runtime. The deserialization side must resolve attribute instances by type ID and populate them.
2. **Unique type IDs**: Object type IDs and attribute type IDs must be unique within the same context.
3. **Serialization order**: Field order in `serialize` and `deserialize` must match exactly.
4. **Threading model**: `SnapshotServer` and `SnapshotClient` are **single-writer, single-reader**. Calls on the same Server/Client (e.g. `registerObject`, `stepSnapshot`, `generateClient`, `sendPackage`, `deserializer`) should run on the same thread — suitable for room-based games (e.g. Overwatch-style) where **one room** is driven by a single-threaded main loop, with no internal locking required.

## 📄 License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.

## 📋 Roadmap

- Binary snapshot deserialization visualizer (in progress)
- Snapshot storage module with replay support (not started)

## 🔗 Links

- [Project home](https://github.com/ZKGameDev/JSnapSync)
- [Issues](https://github.com/ZKGameDev/JSnapSync/issues)
- [KGame ecosystem](https://github.com/ZKGameDev)

---

*JSnapSync — simple and efficient game server state synchronization* 🎮