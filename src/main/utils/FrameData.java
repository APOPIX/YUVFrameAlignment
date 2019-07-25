package main.utils;

/**
 * 用于存储测试帧数据的类
 */
public class FrameData {
    public int width = 0;
    public int height = 0;
    public byte[] I420Picture;
    public long timeStamp = 0;

    public FrameData(int width, int height) {
        I420Picture = new byte[width*height*3/2];
    }

}
