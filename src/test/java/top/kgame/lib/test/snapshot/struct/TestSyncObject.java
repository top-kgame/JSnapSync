package top.kgame.lib.test.snapshot.struct;

import top.kgame.lib.snapshot.DeserializeObject;
import top.kgame.lib.snapshot.SerializeAttribute;
import top.kgame.lib.snapshot.SerializeObject;
import top.kgame.lib.snapshot.tools.ReplicatedReader;

import java.util.*;

public class TestSyncObject implements SerializeObject, DeserializeObject {
    private final Map<Integer, TestSyncAttribute> attributeHashMap = new HashMap<>();
    private int guid;
    private int type;
    public TestSyncObject(int guid, int type) {
        this.guid = guid;
        this.type = type;
    }

    public TestSyncObject() {

    }

    @Override
    public void deserializeAttribute(ReplicatedReader reader) {
        int size = reader.readInteger();
        for (int i = 0; i < size; i++) {
            int attributeSize = reader.readInteger();
            int attributeType = reader.readInteger();
            TestSyncAttribute attribute = attributeHashMap.get(attributeType);
            attribute.deserialize(reader);
        }
    }

    @Override
    public void setGuid(int id) {
        this.guid = id;
    }

    @Override
    public Collection<SerializeAttribute> getAttributes() {
        return new ArrayList<>(attributeHashMap.values());
    }

    @Override
    public int getGuid() {
        return guid;
    }

    @Override
    public void setTypeId(int type) {
        this.type = type;
    }

    @Override
    public int getTypeId() {
        return type;
    }

    public void addComponent(TestSyncAttribute component) {
        attributeHashMap.put(component.getTypeId(), component);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TestSyncObject that = (TestSyncObject) o;
        return guid == that.guid && type == that.type && Objects.equals(attributeHashMap, that.attributeHashMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeHashMap, guid, type);
    }
}