package top.kgame.lib.snapshot;

import top.kgame.lib.snapshot.tools.ReplicatedWriter;

public interface SerializeAttribute {
    Integer getTypeId();
    void serialize(ReplicatedWriter writer);
}
