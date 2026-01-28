package top.kgame.lib.snapshot.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kgame.lib.snapshot.SerializeAttribute;
import top.kgame.lib.snapshot.SerializeObject;
import top.kgame.lib.snapshot.tools.ReplicatedWriter;

public class ComponentSerializer {
    private static final Logger logger = LogManager.getLogger(ComponentSerializer.class);
    private final SerializeObject serializeObject;
    private final SerializeAttribute serializer;
    private ComponentSerializer(SerializeObject serializeObject, SerializeAttribute serializer) {
        this.serializeObject = serializeObject;
        this.serializer = serializer;
    }

    public byte[] serialize() {
        ReplicatedWriter writer = ReplicatedWriter.getInstance();
        writer.writeInteger(serializer.getTypeId());
        serializer.serialize(writer);
        writer.reset();
        return writer.toBytes();
    }

    public SerializeObject getSerializeEntity() {
        return serializeObject;
    }

    public SerializeAttribute getSerializer() {
        return serializer;
    }

    public static ComponentSerializer generate(SerializeObject entity, SerializeAttribute component) {
        return new ComponentSerializer(entity, component);
    }

    public Integer getTypeId() {
        return serializer.getTypeId();
    }

    @Override
    public String toString() {
        return "ReplicatedComponent{" +
                "serializeEntityGuid=" + serializeObject.getGuid() +
                "serializeEntityTypeId=" + serializeObject.getTypeId() +
                ", serializerTypeId=" + serializer.getTypeId() +
                '}';
    }
}
