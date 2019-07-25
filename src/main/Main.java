package main;

import main.utils.FrameData;
import main.utils.FrameFileUtil;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

//public class Main {
//    private static FileInputStream originReader;
//
//    public static void main(String[] args) {
//        int width = 640, height = 480;
//        int sideLength = width/64;
//        int lossRate = 40;//丢帧率
//        Random random = new Random();//随机数发生器
//        FrameFileUtil.initOutputStream("C:\\Users\\zanhanding\\IdeaProjects\\"+lossRate+"loss_trimmed_"+width+"_"+height+".zip", lossRate+"loss_trimmed_"+width+"_"+height+".yuv");//随机丢帧过后的文件
//        FrameFileUtil.initInputStream("C:\\Users\\zanhanding\\IdeaProjects\\origin.zip");
//        try {
//            while (true) {
//                if (random.nextInt(100)>lossRate) {
//                    FrameFileUtil.addYUVDataToZipFile(FrameFileUtil.readNextFrameDataFromFile(width,height,0,sideLength,sideLength).I420Picture);
//                }else{
//                    FrameFileUtil.readNextFrameDataFromFile(width,height,0,sideLength,sideLength);
//                }
//            }
//        }catch (IOException e){
//            System.out.println("读取结束");
//        }finally {
//            FrameFileUtil.end();
//        }
//    }
//}
public class Main {
    private static FileInputStream originReader;
    private static String sentZipPath = "sent.zip";
    private static String rcvZipPath = "received.zip";
    private static String sentOutputZipPath = "sent_trimmed.zip";
    private static String sentOutputRawPath = "sent_trimmed.yuv";
    private static String rcvdOutputZipPath = "received_trimmed.zip";
    private static String rcvOutputRawPath = "received_trimmed.yuv";

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].isBlank()&&args[0].equals("help")) {

            System.out.println("使用说明：\n" +
                    "从左到右依次传入四个路径\n" +
                    "1.发送端发送的yuv文件压缩包输入路径，默认为sent.zip\n" +
                    "2.接收端接收到的yuv文件压缩包输入路径，默认为received.zip\n" +
                    "3.对齐后的发送端视频文件输出路径，默认为sent_trimmed.yuv\n" +
                    "4.对齐后的接收端视频文件输出路径，默认为received_trimmed.yuv\n" +
                    "例：java -jar YUVFrameAlignment.jar sent.zip received.zip sent_trimmed.yuv received_trimmed.yuv");
            return;
        }
        if (args.length > 0 && !args[0].isBlank()) {
            sentZipPath = args[0];
        }
        if (args.length > 1 &&!args[1].isBlank()) {
            rcvZipPath = args[1];
        }
        if (args.length > 2 &&!args[2].isBlank()) {
            sentOutputRawPath = args[2];
        }
        if (args.length > 3 &&!args[3].isBlank()) {
            rcvOutputRawPath = args[3];
        }
        int width = 368, height = 640;
        int sideLength = width / 64;
        List<Long> trimmedTimeStampList = new ArrayList<>();
        List<Long> recvTimeStampList = new ArrayList<>();
        List<Long> sentTimeStampList = new ArrayList<>();
        List<Long> readedTimeStampList = new ArrayList<>();
        //存储接收端接收到的时间戳序列
        FrameFileUtil.initInputStream(rcvZipPath);
        FrameData frameData;
        while (true) {
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, sideLength, sideLength);
            if (frameData == null)
                break;
            recvTimeStampList.add(frameData.timeStamp);
        }
        FrameFileUtil.end();
        //存储发送端接发出的时间戳序列
        FrameFileUtil.initInputStream(sentZipPath);
        while (true) {
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, sideLength, sideLength);
            if (frameData == null)
                break;
            sentTimeStampList.add(frameData.timeStamp);
        }
        FrameFileUtil.end();
        for (long i : recvTimeStampList) {
            if (sentTimeStampList.indexOf(i) > -1) {
                trimmedTimeStampList.add(i);
            }
        }

        long lastTimeStamp = 0;//控制单向查找，避免重复帧
        //处理发送的文件
        FrameFileUtil.initInputStream(sentZipPath);
        FrameFileUtil.initOutputStream(sentOutputRawPath, "");//对齐后的原始文件

        while (true) {//存储接收端接收到的时间戳序列
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, sideLength, sideLength);
            if (frameData == null)//读取完成
                break;
            //lastIndex控制单向查找
            if (trimmedTimeStampList.indexOf(frameData.timeStamp) > -1 && frameData.timeStamp > lastTimeStamp && readedTimeStampList.indexOf(frameData.timeStamp) == -1) {//若该帧在接收文件中且未写入
                FrameFileUtil.addYUVDataToRawFile(frameData.I420Picture);
                lastTimeStamp = trimmedTimeStampList.indexOf(frameData.timeStamp);
                readedTimeStampList.add(frameData.timeStamp);
            }
        }
        FrameFileUtil.end();
        readedTimeStampList = new ArrayList<>();
        //处理接收的文件
        lastTimeStamp = 0;//控制单向查找，避免重复帧
        FrameFileUtil.initInputStream(rcvZipPath);
        FrameFileUtil.initOutputStream(rcvOutputRawPath, "");//对齐后的原始文件

        while (true) {//存储接收端接收到的时间戳序列
            frameData = FrameFileUtil.readNextFrameDataFromFile(width, height, 0, sideLength, sideLength);
            if (frameData == null)//读取完成
                break;
            //lastTimeStamp控制单向查找
            if (trimmedTimeStampList.indexOf(frameData.timeStamp) > -1 && frameData.timeStamp > lastTimeStamp && readedTimeStampList.indexOf(frameData.timeStamp) == -1) {//若该帧在接收文件中且未写入
                FrameFileUtil.addYUVDataToRawFile(frameData.I420Picture);
                lastTimeStamp = trimmedTimeStampList.indexOf(frameData.timeStamp);
                readedTimeStampList.add(frameData.timeStamp);
            }
        }
        FrameFileUtil.end();
    }
}