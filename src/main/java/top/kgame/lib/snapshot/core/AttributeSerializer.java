package top.kgame.lib.snapshot.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.kgame.lib.snapshot.SerializeAttribute;
import top.kgame.lib.snapshot.SerializeObject;
import top.kgame.lib.snapshot.tools.ReplicatedWriter;

public class AttributeSerializer {
    private static final Logger logger = LogManager.getLogger(AttributeSerializer.class);
    private final SerializeObject serializeObject;
    private final SerializeAttribute serializer;
    private AttributeSerializer(SerializeObject serializeObject, SerializeAttribute serializer) {
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

    public static AttributeSerializer generate(SerializeObject entity, SerializeAttribute component) {
        return new AttributeSerializer(entity, component);
    }

    public Integer getTypeId() {
        return serializer.getTypeId();
    }

    @Override
    public String toString() {
        return "ReplicatedAttribute{" +
                "serializeObjectGuid=" + serializeObject.getGuid() +
                "serializeObjectTypeId=" + serializeObject.getTypeId() +
                ", attributeTypeId=" + serializer.getTypeId() +
                '}';
    }
}
