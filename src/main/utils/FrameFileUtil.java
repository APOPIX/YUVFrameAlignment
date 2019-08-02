package main.utils;


import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static main.utils.TimeStampUtil.readTimeStamp;

/**
 *
 */
public class FrameFileUtil {
    private static BufferedOutputStream outputStream = null;//输出缓冲区，需要初始化
    private static ZipOutputStream zipOutputStream = null;//压缩输出缓冲区，需要初始化
    private static BufferedInputStream inputStream = null;//输入缓冲区，需要初始化
    private static ZipInputStream zipInputStream = null;//压缩输入缓冲区，需要初始化
    private static ZipEntry zipOutputEntry = null;
    private static boolean outputStreamInitiated = false;
    private static boolean inputStreamInitiated = false;


    /**
     * @param filename 压缩文件名或者未压缩文件名
     * @param entry    若指定压缩文件名，则需要指定entry
     */
    public static void initOutputStream(String filename, String entry) {
        File outputFile = new File(filename);
        //首先清理旧的文件，确保有一个空的文件以待写入
        try {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //创建输出流缓存
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            if (!entry.isEmpty()) {
                zipOutputEntry = new ZipEntry(entry);
                zipOutputStream = new ZipOutputStream(outputStream);
                zipOutputStream.putNextEntry(zipOutputEntry);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStreamInitiated = true;
        }
    }

    /**
     * @param filename 传入数据文件名，初始化类中的BufferedInputStream
     */
    public static void initInputStream(String filename) {
        File inputFile = new File(filename);
        //创建输入流缓存
        try {
            inputStream = new BufferedInputStream(new FileInputStream(inputFile));
            zipInputStream = new ZipInputStream(inputStream);
            zipInputStream.getNextEntry();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputStreamInitiated = true;
        }
    }

    /**
     * 关闭输入和输出文件流
     */
    public static void end() {
        try {
            if (zipInputStream != null)
                zipInputStream.close();
            if (zipOutputStream != null)
                zipOutputStream.close();
            if (outputStream != null)
                outputStream.close();
            if (inputStream != null)
                inputStream.close();
            if (zipOutputEntry != null)
                zipOutputEntry = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream = null;
            zipOutputStream = null;
            outputStreamInitiated = false;
            inputStream = null;
            zipInputStream = null;
            inputStreamInitiated = false;
        }
    }

    /**
     * @return 是否初始化了输出流
     */
    public static boolean isOutputStreamInitiated() {
        return outputStreamInitiated;
    }

    /**
     * @return 是否初始化了输入流
     */
    public static boolean isInputStreamInitiated() {
        return inputStreamInitiated;
    }

    /**
     * 向压缩文件中追加写入一张I420图像
     *
     * @param yuvPicture 待写入的YUV图像
     */
    public static void addYUVDataToZipFile(byte[] yuvPicture) {
        try {
            zipOutputStream.write(yuvPicture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向未压缩文件中追加写入一张I420图像
     *
     * @param yuvPicture 待写入的YUV图像
     */
    public static void addYUVDataToRawFile(byte[] yuvPicture) {
        try {
            outputStream.write(yuvPicture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * /**
     * 从文件中读取下一张I420图像
     *
     * @param width      欲读取图像的宽度
     * @param height     欲读取图像的高度
     * @param xBegin     黑白色块的最左上角起始位置（x）
     * @param yBegin     黑白色块的最左上角起始位置（y）
     * @param sideLength 黑白色块的边长
     * @return FrameData类
     */

    public static FrameData readNextFrameDataFromFile(int width, int height, int xBegin, int yBegin, int sideLength) {
        FrameData outputFrame = new FrameData(width, height);
        int toRead = width * height * 3 / 2;//总共需要读取的字节数
        int readed = 0;//已读取的字节数
        try {
            //https://docs.oracle.com/javase/8/docs/api/java/io/BufferedInputStream.html#read-byte:A-int-int-
            while (toRead > 0) {
                int newRead = zipInputStream.read(outputFrame.I420Picture, readed, toRead);//记录本次读取到的字节数
                if (newRead < 0) {
//                    System.out.println("readNextFrameDataFromFile: 读取数据流意外中断：到达文件结尾");
                    return null;
                }
                readed += newRead;
                toRead -= newRead;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputFrame.width = width;
        outputFrame.height = height;
        outputFrame.timeStamp = readTimeStamp(outputFrame.I420Picture, width, xBegin, yBegin, sideLength);
        return outputFrame;
    }

    /**
     * 返回一个被裁掉顶部高度为cutHeight部分的yuv视频帧
     *
     * @param cutHeight 欲裁掉的高度
     * @param width     原视频宽度
     * @param height    原视频高度
     * @param frame     原视频I420数据
     * @return 裁剪后的视频帧
     */
    public static FrameData frameCut(int cutHeight, int width, int height, byte[] frame) {
        FrameData outputFrame = new FrameData(width, height - cutHeight);
        int yLength = width * height;
        int cutULength = cutHeight * width  / 4;
        int cutVLength = cutHeight * width  / 4;
        int pointer = 0;
        //存入裁剪后的Y像素
        for (int i = cutHeight * width; i < yLength; i++) {
            outputFrame.I420Picture[pointer++] = frame[i];
        }
        //存入裁剪后的U像素
        for (int i = yLength + cutULength; i < yLength + yLength/4; i++) {
            outputFrame.I420Picture[pointer++] = frame[i];
        }
        //存入裁剪后的V像素
        for (int i = yLength +  yLength/4 + cutVLength; i < yLength + yLength/2; i++) {
            outputFrame.I420Picture[pointer++] = frame[i];
        }

        return outputFrame;
    }


}
