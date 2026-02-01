package top.kgame.lib.snapshot.tools;

import io.netty.buffer.ByteBuf;

public class ReplicatedUtil {
    /**
     * 将普通 int 编码为 ZigZag 格式（便于 VarInt 对小整数压缩）
     * @param n 普通格式的 int
     * @return ZigZag 编码后的 int
     */
    public static int encodeZigZag(int n) {
        return (n << 1) ^ (n >> 31);
    }

    /**
     * 将 ZigZag 编码的 int 解码为普通 int
     * @param n ZigZag 编码的 int
     * @return 解码后的普通 int
     */
    public static int decodeZigZag(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * 将一个普通int按照VarInt的格式写入ByteBuf
     * @param byteBuf 要写入的ByteBuf实例
     * @param value 普通格式的int
     */
    public static void writeVarInt(ByteBuf byteBuf, int value) {
        value = encodeZigZag(value);
        while (true) {
            if ((value & ~0x7F) == 0) {
                byteBuf.writeByte(value);
                break;
            } else {
                byteBuf.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    /**
     * 从ByteBuf中按照VarInt格式读取出一个普通int
     * 
     * <p>VarInt编码规则：每个字节使用低7位存储数据，最高位表示是否还有后续字节。
     * 当最高位为0时表示最后一个字节，为1时表示还有后续字节。</p>
     * 
     * @param byteBuf ByteBuf实例，必须包含有效的VarInt编码数据
     * @return 读取的一个普通int值
     * @throws IllegalStateException 当发生以下情况时抛出：
     *         <ul>
     *         <li>Buffer underflow: ByteBuf中可读字节不足，数据不完整</li>
     *         <li>VarInt too long: VarInt编码超过5个字节，可能是数据损坏或格式错误</li>
     *         </ul>
     */
    public static  int readVarInt(ByteBuf byteBuf) {
        int value = 0;
        int position = 0;
        int maxBytes = 5; // int最多需要5个字节
        
        while (position < maxBytes * 7) {
            if (!byteBuf.isReadable()) {
                throw new IllegalStateException("Buffer underflow");
            }
            
            int byteValue = byteBuf.readByte();
            value |= (byteValue & 0x7F) << position;
            
            if ((byteValue & 0x80) == 0) {
                return decodeZigZag(value);
            }
            
            position += 7;
        }
        
        throw new IllegalStateException("VarInt too long");
    }
}
