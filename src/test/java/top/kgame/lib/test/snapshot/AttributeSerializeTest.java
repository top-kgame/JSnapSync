package top.kgame.lib.test.snapshot;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import top.kgame.lib.snapshot.tools.ReplicatedReader;
import top.kgame.lib.snapshot.tools.ReplicatedWriter;
import top.kgame.lib.test.snapshot.struct.TestSyncAttribute;
import top.kgame.lib.test.snapshot.struct.TestSyncStruct;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Supplier;
import top.kgame.lib.snapshot.DeserializeFactory;
import top.kgame.lib.test.snapshot.struct.TestSyncObject;

public class AttributeSerializeTest {
    @Test
    public void testNormalValues() {
        TestSyncAttribute encodeAttribute = new TestSyncAttribute(1);
        
        // 设置所有基本类型变量
        encodeAttribute.setB((byte) 2);
        encodeAttribute.setC('A');
        encodeAttribute.setBl(true);
        encodeAttribute.setInt16((short) 1234);
        encodeAttribute.setInt32(567890);
        encodeAttribute.setInt64(9876543210L);
        encodeAttribute.setF32(3.14f);
        encodeAttribute.setF64(2.718281828);
        encodeAttribute.setBs(new byte[]{1, 2, 3, 4, 5});
        encodeAttribute.setSs("测试字符串");
        
        // 创建并设置嵌套的TestSyncStruct对象
        TestSyncStruct encodeStruct = new TestSyncStruct();
        encodeStruct.setB((byte) 10);
        encodeStruct.setC('B');
        encodeStruct.setBl(false);
        encodeStruct.setInt16((short) 5678);
        encodeStruct.setInt32(123456);
        encodeStruct.setInt64(1122334455L);
        encodeStruct.setF32(1.618f);
        encodeStruct.setF64(1.4142135623);
        encodeStruct.setBs(new byte[]{6, 7, 8, 9, 10});
        encodeStruct.setSs("嵌套结构测试");
        
        encodeAttribute.setStruct(encodeStruct);
        
        // 设置List类型
        encodeAttribute.setListObj(Arrays.asList(encodeStruct));
        encodeAttribute.setListBoolean(Arrays.asList(true, false, true));
        encodeAttribute.setListByte(Arrays.asList((byte) 1, (byte) 2, (byte) 3));
        encodeAttribute.setListChar(Arrays.asList('A', 'B', '中'));
        encodeAttribute.setListShort(Arrays.asList((short) 100, (short) 200));
        encodeAttribute.setListInt(Arrays.asList(1000, 2000, 3000));
        encodeAttribute.setListLong(Arrays.asList(10000L, 20000L));
        encodeAttribute.setListFloat(Arrays.asList(1.1f, 2.2f, 3.3f));
        encodeAttribute.setListDouble(Arrays.asList(1.11, 2.22, 3.33));
        encodeAttribute.setListString(Arrays.asList("字符串1", "字符串2", "字符串3"));
        
        // 设置数组类型
        encodeAttribute.setBooleanArray(new boolean[]{true, false, true, false});
        encodeAttribute.setCharArray(new char[]{'X', 'Y', 'Z', '测'});
        encodeAttribute.setShortArray(new short[]{300, 400, 500});
        encodeAttribute.setIntArray(new int[]{4000, 5000, 6000});
        encodeAttribute.setLongArray(new long[]{40000L, 50000L, 60000L});
        encodeAttribute.setFloatArray(new float[]{4.4f, 5.5f, 6.6f});
        encodeAttribute.setDoubleArray(new double[]{4.44, 5.55, 6.66});
        encodeAttribute.setStringArray(new String[]{"数组1", "数组2", "数组3"});

        ReplicatedWriter replicatedWriter = ReplicatedWriter.getInstance();
        encodeAttribute.serialize(replicatedWriter);
        byte[] encodeData = replicatedWriter.toBytes();

        ByteBuf decodeByteBuf = Unpooled.buffer();
        decodeByteBuf.writeBytes(encodeData);
        ReplicatedReader replicatedReader = ReplicatedReader.getInstance(decodeByteBuf);
        TestSyncAttribute decodeComponent = new TestSyncAttribute(1);
        decodeComponent.deserialize(replicatedReader);

        assert encodeAttribute.equals(decodeComponent);
        replicatedWriter.reset();
        
        System.out.println("testNormalValues 测试通过！");
    }

    @Test
    public void testMinValuesAndNulls() {
        TestSyncAttribute encodeAttribute = new TestSyncAttribute(1);
        
        // 设置所有基本类型变量为最小值
        encodeAttribute.setB(Byte.MIN_VALUE);
        encodeAttribute.setC('\u0000');
        encodeAttribute.setBl(false);
        encodeAttribute.setInt16(Short.MIN_VALUE);
        encodeAttribute.setInt32(Integer.MIN_VALUE);
        encodeAttribute.setInt64(Long.MIN_VALUE);
        encodeAttribute.setF32(Float.MIN_VALUE);
        encodeAttribute.setF64(Double.MIN_VALUE);
        encodeAttribute.setBs(null);
        encodeAttribute.setSs(null);
        encodeAttribute.setStruct(null);

        // 设置List类型为null或空
        encodeAttribute.setListObj(null);
        encodeAttribute.setListBoolean(new ArrayList<>());
        encodeAttribute.setListByte(null);
        encodeAttribute.setListChar(new ArrayList<>());
        encodeAttribute.setListShort(null);
        encodeAttribute.setListInt(new ArrayList<>());
        encodeAttribute.setListLong(null);
        encodeAttribute.setListFloat(new ArrayList<>());
        encodeAttribute.setListDouble(null);
        encodeAttribute.setListString(Arrays.asList(null, "", null));
        
        // 设置数组类型为null或空
        encodeAttribute.setBooleanArray(null);
        encodeAttribute.setCharArray(new char[0]);
        encodeAttribute.setShortArray(null);
        encodeAttribute.setIntArray(new int[0]);
        encodeAttribute.setLongArray(null);
        encodeAttribute.setFloatArray(new float[0]);
        encodeAttribute.setDoubleArray(null);
        encodeAttribute.setStringArray(new String[]{null, null});

        ReplicatedWriter replicatedWriter = ReplicatedWriter.getInstance();
        encodeAttribute.serialize(replicatedWriter);
        byte[] encodeData = replicatedWriter.toBytes();

        ByteBuf decodeByteBuf = Unpooled.buffer();
        decodeByteBuf.writeBytes(encodeData);
        ReplicatedReader replicatedReader = ReplicatedReader.getInstance(decodeByteBuf);
        TestSyncAttribute decodeComponent = new TestSyncAttribute(1);
        decodeComponent.deserialize(replicatedReader);

        assert encodeAttribute.equals(decodeComponent);
        replicatedWriter.reset();
        
        System.out.println("testMinValuesAndNulls 测试通过！");
    }

    @Test
    public void testMaxValuesAndSpecialCases() {
        TestSyncAttribute encodeAttribute = new TestSyncAttribute(1);

        // 设置所有基本类型变量为最大值和特殊值
        encodeAttribute.setB(Byte.MAX_VALUE);
        encodeAttribute.setC('\uFFFF');
        encodeAttribute.setBl(true);
        encodeAttribute.setInt16(Short.MAX_VALUE);
        encodeAttribute.setInt32(Integer.MAX_VALUE);
        encodeAttribute.setInt64(Long.MAX_VALUE);
        encodeAttribute.setF32(Float.MAX_VALUE);
        encodeAttribute.setF64(Double.MAX_VALUE);
        encodeAttribute.setBs(new byte[]{Byte.MIN_VALUE, Byte.MAX_VALUE, 0});
        encodeAttribute.setSs("🌟特殊字符测试🚀\n\t换行制表符");

        // 创建并设置嵌套的TestSyncStruct对象
        TestSyncStruct encodeStruct = new TestSyncStruct();
        encodeStruct.setB((byte) 0);
        encodeStruct.setC('中');
        encodeStruct.setBl(true);
        encodeStruct.setInt16((short) 0);
        encodeStruct.setInt32(0);
        encodeStruct.setInt64(0);
        encodeStruct.setF32(Float.NaN);
        encodeStruct.setF64(Double.POSITIVE_INFINITY);
        encodeStruct.setBs(new byte[0]);
        encodeStruct.setSs("");

        encodeAttribute.setStruct(encodeStruct);
        
        // 设置List类型为包含各种特殊值
        TestSyncStruct struct2 = new TestSyncStruct();
        struct2.setB((byte) -1);
        struct2.setC('Ω');
        struct2.setBl(false);
        struct2.setInt16((short) -32768);
        struct2.setInt32(-1);
        struct2.setInt64(-1L);
        struct2.setF32(Float.NEGATIVE_INFINITY);
        struct2.setF64(Double.NaN);
        struct2.setBs(new byte[]{-128, 127});
        struct2.setSs("特殊struct");
        
        encodeAttribute.setListObj(Arrays.asList(encodeStruct, struct2, null));
        encodeAttribute.setListBoolean(Arrays.asList(true, true, false, false, true));
        encodeAttribute.setListByte(Arrays.asList(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE));
        encodeAttribute.setListChar(Arrays.asList('\u0000', 'A', '\uFFFF', '中'));
        encodeAttribute.setListShort(Arrays.asList(Short.MIN_VALUE, (short) 0, Short.MAX_VALUE));
        encodeAttribute.setListInt(Arrays.asList(Integer.MIN_VALUE, 0, Integer.MAX_VALUE));
        encodeAttribute.setListLong(Arrays.asList(Long.MIN_VALUE, 0L, Long.MAX_VALUE));
        encodeAttribute.setListFloat(Arrays.asList(Float.MIN_VALUE, 0.0f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY));
        encodeAttribute.setListDouble(Arrays.asList(Double.MIN_VALUE, 0.0, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        encodeAttribute.setListString(Arrays.asList("", "正常字符串", null, "🌟特殊符号🚀", "\n换行\t制表符"));
        
        // 设置数组类型为包含各种边界值
        encodeAttribute.setBooleanArray(new boolean[]{true, false});
        encodeAttribute.setCharArray(new char[]{'\u0000', 'Z', '\uFFFF'});
        encodeAttribute.setShortArray(new short[]{Short.MIN_VALUE, 0, Short.MAX_VALUE});
        encodeAttribute.setIntArray(new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE});
        encodeAttribute.setLongArray(new long[]{Long.MIN_VALUE, Long.MAX_VALUE});
        encodeAttribute.setFloatArray(new float[]{Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY});
        encodeAttribute.setDoubleArray(new double[]{Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY});
        encodeAttribute.setStringArray(new String[]{"边界测试", "", null, "🎯Unicode测试🌟"});

        ReplicatedWriter replicatedWriter = ReplicatedWriter.getInstance();
        encodeAttribute.serialize(replicatedWriter);
        byte[] encodeData = replicatedWriter.toBytes();

        ByteBuf decodeByteBuf = Unpooled.buffer();
        decodeByteBuf.writeBytes(encodeData);
        ReplicatedReader replicatedReader = ReplicatedReader.getInstance(decodeByteBuf);
        TestSyncAttribute decodeComponent = new TestSyncAttribute(1);
        decodeComponent.deserialize(replicatedReader);

        assert encodeAttribute.equals(decodeComponent);
        replicatedWriter.reset();
        
        System.out.println("testMaxValuesAndSpecialCases 测试通过！");
    }

    @Test
    public void testDeserializeFactoryWithSupplier() {
        DeserializeFactory factory = new DeserializeFactory();
        
        // 方式1：使用Lambda表达式
        factory.registerEntityType(100, () -> new TestSyncObject(0, 100));
        
        // 方式2：使用方法引用（如果有无参构造函数）
        factory.registerEntityType(101, TestSyncObject::new);
        
        // 方式3：使用类引用（需要无参构造函数）
         factory.registerEntityType(102, TestSyncObject.class);
        
        // 方式4：使用匿名内部类
        factory.registerEntityType(103, new Supplier<TestSyncObject>() {
            @Override
            public TestSyncObject get() {
                return new TestSyncObject(0, 103);
            }
        });
        
        // 验证注册
        assert factory.isRegistered(100);
        assert factory.isRegistered(101);
        assert factory.isRegistered(102);
        assert factory.isRegistered(103);
        assert !factory.isRegistered(999);
        assert factory.getRegisteredTypeCount() == 4;
        
        System.out.println("testDeserializeFactoryWithSupplier 测试通过！");
    }
}