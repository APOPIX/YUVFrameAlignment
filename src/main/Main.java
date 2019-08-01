package main;

import main.utils.FrameData;
import main.utils.FrameFileUtil;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static String sentZipPath = "indexed.zip";
    private static String rcvZipPath = "received.zip";
    private static String sentOutputRawPath = "indexed_trimmed.yuv";
    private static String rcvOutputRawPath = "received_trimmed.yuv";
    private static int width = 720, height = 1280;

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].isBlank() && args[0].equals("help")) {

            System.out.println("使用说明：\n" +
                    "从左到右依次传入四个路径和视频宽高\n" +
                    "1.发送端发送的yuv文件压缩包输入路径，例如：indexed.zip\n" +
                    "2.接收端接收到的yuv文件压缩包输入路径，例如：received.zip\n" +
                    "3.对齐后的发送端视频文件输出路径，例如：indexed_trimmed.yuv\n" +
                    "4.对齐后的接收端视频文件输出路径，例如：received_trimmed.yuv\n" +
                    "5.视频宽度，例如：368\n" +
                    "6.视频高度，例如：640\n" +
                    "例：java -jar YUVFrameAlignment.jar indexed.zip received.zip indexed_trimmed.yuv received_trimmed.yuv 368 640");
            return;
        }
        if (args.length > 0 && !args[0].isBlank()) {
            sentZipPath = args[0];
        }
        if (args.length > 1 && !args[1].isBlank()) {
            rcvZipPath = args[1];
        }
        if (args.length > 2 && !args[2].isBlank()) {
            sentOutputRawPath = args[2];
        }
        if (args.length > 3 && !args[3].isBlank()) {
            rcvOutputRawPath = args[3];
        }
        if (args.length > 4 && !args[4].isBlank()) {
            width = Integer.parseInt(args[4]);
        }
        if (args.length > 5 && !args[5].isBlank()) {
            height = Integer.parseInt(args[5]);
        }
        int sideLength = width / 64;
        List<Long> trimmedIndexList = new ArrayList<>();
        List<Long> recvIndexList = new ArrayList<>();
        List<Long> sentIndexList = new ArrayList<>();
        List<Long> readIndexList = new ArrayList<>();
        //存储接收端接收到的索引序列
        FrameFileUtil.initInputStream(rcvZipPath);
        FrameData frameData;
        System.out.println("正在读取接收端接收的索引序列");
        while (true) {
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, 0, sideLength);
            if (frameData == null)
                break;
            recvIndexList.add(frameData.timeStamp);
        }
        FrameFileUtil.end();
        //存储发送端接发出的索引序列
        FrameFileUtil.initInputStream(sentZipPath);
        System.out.println("正在读取发送端发送的索引序列");
        while (true) {
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, 0, sideLength);
            if (frameData == null)
                break;
            sentIndexList.add(frameData.timeStamp);
        }
        FrameFileUtil.end();
        for (long i : recvIndexList) {
            if (sentIndexList.indexOf(i) > -1) {
                trimmedIndexList.add(i);
            }
        }

        long lastIndex = 0;//控制单向查找，避免重复帧
        System.out.println("正在生成对齐后的原始视频帧");
        //处理发送的文件
        FrameFileUtil.initInputStream(sentZipPath);
        FrameFileUtil.initOutputStream(sentOutputRawPath, "");//对齐后的原始文件
        int frameCount = 0;
        while (true) {
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, 0, sideLength);
            System.out.print("\r正在处理第" + frameCount++ + "帧");
            if (frameData == null) {//读取完成
                System.out.println("\n\r处理完成");
                frameCount = 0;
                break;
            }
            //lastIndex控制单向查找
            if (trimmedIndexList.indexOf(frameData.timeStamp) > -1 && frameData.timeStamp > lastIndex && readIndexList.indexOf(frameData.timeStamp) == -1) {//若该帧在接收文件中且未写入
                //存入裁剪掉时间戳后的视频帧
                FrameFileUtil.addYUVDataToRawFile(FrameFileUtil.frameCut(2 * sideLength, width, height, frameData.I420Picture).I420Picture);
                lastIndex = trimmedIndexList.indexOf(frameData.timeStamp);
                readIndexList.add(frameData.timeStamp);
            }
        }
        FrameFileUtil.end();
        readIndexList = new ArrayList<>();
        //处理接收的文件
        System.out.println("正在生成对齐后的接收视频帧");
        lastIndex = 0;//控制单向查找，避免重复帧
        FrameFileUtil.initInputStream(rcvZipPath);
        FrameFileUtil.initOutputStream(rcvOutputRawPath, "");//对齐后的原始文件

        while (true) {
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, 0, sideLength);
            System.out.print("\r正在处理第" + frameCount++ + "帧");
            if (frameData == null) {//读取完成
                System.out.println("\n\r处理完成");
                break;
            }
            //lastTimeStamp控制单向查找
            if (trimmedIndexList.indexOf(frameData.timeStamp) > -1 && frameData.timeStamp > lastIndex && readIndexList.indexOf(frameData.timeStamp) == -1) {//若该帧在接收文件中且未写入
                //存入裁剪掉时间戳后的视频帧
                FrameFileUtil.addYUVDataToRawFile(FrameFileUtil.frameCut(2 * sideLength, width, height, frameData.I420Picture).I420Picture);
                lastIndex = trimmedIndexList.indexOf(frameData.timeStamp);
                readIndexList.add(frameData.timeStamp);
            }
        }
        FrameFileUtil.end();
        System.out.println("生成的YUV视频分辨率为 " + width + "x" + (height - 2 * sideLength));
    }
}