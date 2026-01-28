package top.kgame.lib.test.snapshot.struct;

import top.kgame.lib.snapshot.DeserializeObject;
import top.kgame.lib.snapshot.SerializeAttribute;
import top.kgame.lib.snapshot.SerializeObject;
import top.kgame.lib.snapshot.tools.ReplicatedReader;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestSyncObject implements SerializeObject, DeserializeObject {
    private final List<SerializeAttribute> components = new ArrayList<>();
    private int guid;
    private int type;
    public TestSyncObject(int guid, int type) {
        this.guid = guid;
        this.type = type;
    }

    public TestSyncObject() {

    }

    @Override
    public void deserialize(ReplicatedReader reader) {

    }

    @Override
    public void setGuid(int id) {
        this.guid = id;
    }

    @Override
    public Collection<SerializeAttribute> getAttributes() {
        return components;
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
        components.add(component);
    }
}