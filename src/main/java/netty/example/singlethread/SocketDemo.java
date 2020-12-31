package netty.example.singlethread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import netty.example.common.NioSocket;

public class SocketDemo {

  private static String UPLOAD_FILE = "";

  public static void main(String[] args) {
    sendFile();
  }

  private static void sendFile() {
    changeUploadFile();

    File file = new File(UPLOAD_FILE);
    if (!file.exists()) {
      System.out.println("文件不存在");
      return;
    }

    try {
      SocketChannel socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);

      socketChannel.connect(new InetSocketAddress(
          NioSocket.HOST,
          NioSocket.PORT
      ));
      System.out.println("Start to connect to server.");
      while (!socketChannel.finishConnect()) {
        // 异步模式， 自旋验证是否已经成功连接到服务器端
        // 这里也可以做其他事情
        System.out.println("CONNECTING...");
        TimeUnit.MILLISECONDS.sleep(100);
      }

      System.out.println("成功连接到服务器端");

      ByteBuffer buffer = ByteBuffer.allocate(NioSocket.BUFFER_CAPACITY);

      ByteBuffer encode = NioSocket.CHARSET.encode(file.getName());


      // 发送文件名称长度
      // 这里如果直接使用 encode.capacity() 的话， 会多两个字节的长度
      buffer.putInt(file.getName().trim().length());
//            buffer.flip();
//            socketChannel.write(buffer);
//            buffer.clear();
      System.out.printf("文件名称长度发送：%s \n" , encode.capacity());

      // 发送文件名称
      buffer.put(encode);
//            socketChannel.write(encode);
      System.out.printf("文件名称发送：%s \n", file.getName());

      // 发送文件大小
      buffer.putLong(file.length());
//            buffer.flip();
//            socketChannel.write(buffer);
//            buffer.clear();
      System.out.printf("发送文件长度：%s \n", file.length());

      // 发送文件
      int len = 0;
      long progess = 0;

      FileChannel fileChannel = new FileInputStream(file).getChannel();

      while ((len = fileChannel.read(buffer)) > 0) {
        buffer.flip();
        socketChannel.write(buffer);
        buffer.clear();

        progess += len;
        System.out.println("上传文件进度：" + (progess / file.length() * 100) + "%");
      }

      // 发送完成， 常规关闭操作
      if (len == -1) {
        // 发送完成， 关闭操作
        fileChannel.close();
        socketChannel.shutdownOutput();
        socketChannel.close();
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }


  private static void changeUploadFile() {
    System.out.println("请输入想要上传文件的完整路径");
    Scanner scanner = new Scanner(System.in);
    UPLOAD_FILE = scanner.next();
  }
}

