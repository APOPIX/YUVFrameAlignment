package main.utils;

import java.nio.ByteBuffer;


public class TimeStampUtil {
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    /**
     * @param timeStamp  欲添加的时间戳（以毫秒为单位）
     * @param yuvPicture 输入图片（I420格式）
     * @param xBegin     黑白色块的最左上角起始位置（x）
     * @param yBegin     黑白色块的最左上角起始位置（y）
     * @param sideLength 黑白色块的边长
     * @return 添加了黑白时间戳水印的I420图像
     * 黑块代表0，白块代表1，从左至右为MSB到LSB
     */
    public static byte[] setTimeStamp(long timeStamp, byte[] yuvPicture, int width, int height, int xBegin, int yBegin, int sideLength) {
        if (timeStamp < 0) {//timeStamp必须大于0
            return yuvPicture;
        }

        byte[] binTimeStamp = new byte[64];
        for (int i = 0; i < 64; i++) {//将timeStamp二进制的形式提取到binTimeStamp数组中
            binTimeStamp[64 - i - 1] = (byte) ((timeStamp >> i) & 0b1);
        }
//        System.out.println("二进制形式" + Arrays.toString(binTimeStamp));
        for (int y = yBegin; y < yBegin + sideLength && y < height; y++) {
            for (int x = xBegin; x < width && x < sideLength * 64 + xBegin; x += sideLength) {
                for (int i = 0; i < sideLength; i++) {//为以xy为起始的长为sideLength的一段像素点赋值
                    int index = binTimeStamp[(x - xBegin) / sideLength] & 0xFF;
                    if(index == 0){
                        yuvPicture[y * width + x + i] = 0b0000_0000;//直接设置二进制形式（0）
                    }
                    if(index == 1){
                        yuvPicture[y * width + x + i] = (byte) 0b1111_1111;//直接设置二进制形式（256）
                    }
                }
            }
        }
        return yuvPicture;
    }


    /**
     * @param yuvPicture 带有黑白色块时间戳的I420图像
     * @param width      图像宽度
     * @param xBegin     黑白色块的最左上角起始位置（x）
     * @param yBegin     黑白色块的最左上角起始位置（y）
     * @param sideLength 黑白色块的边长
     * @return 时间戳对应的长整数
     */
    public static long readTimeStamp(byte[] yuvPicture, int width, int xBegin, int yBegin, int sideLength) {
        byte[] binTimeStamp = new byte[64];
        long timeStamp = 0;

        for (int x = xBegin; x < width && x < sideLength * 64 + xBegin; x += sideLength) {
            //取每个黑白方块中心的像素点
            int index = (yBegin + sideLength / 2) * width + x + sideLength / 2;
            //当byte为0b1111_1111时，与0xFF做与运算可得到256
            if ((yuvPicture[index] & 0xFF) < 128) {//中心像素点为黑色
                binTimeStamp[(x - xBegin) / sideLength] = 0;
            }
            if ((yuvPicture[index] & 0xFF) > 128) {//中心像素点为白色
                binTimeStamp[(x - xBegin) / sideLength] = 1;
            }
        }
//        System.out.println(Arrays.toString(binTimeStamp));
        long flag = 1;//若为int，左移超过32将导致溢出
        for (int i = 0; i < 64; i++) {
            if (binTimeStamp[i] == (byte) 1) {
                timeStamp += flag << (64 - 1 - i);
            }
        }
//        System.out.println(Arrays.toString(binTimeStamp));
        return timeStamp;
    }
}
