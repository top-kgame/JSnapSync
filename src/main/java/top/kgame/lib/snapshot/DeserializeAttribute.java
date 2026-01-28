package top.kgame.lib.snapshot;

import top.kgame.lib.snapshot.tools.ReplicatedReader;

public interface DeserializeAttribute {
    void deserialize(ReplicatedReader reader);
}
