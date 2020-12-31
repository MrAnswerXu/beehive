package netty.example.multithread;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import netty.example.common.NioSocket;
import netty.example.common.ReadHandler;
import netty.example.common.ReceiverFile;

public class AcceptHandler implements Runnable {

  private final ServerSocketChannel serverSocketChannel;
  private final Selector selector;
  private static final String uploadSavePathDir = NioSocket.UPLOAD_SAVE_DIR_PATH;

  public AcceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) {
    this.serverSocketChannel = serverSocketChannel;
    this.selector = selector;
  }

  @Override
  public void run() {
    try {
      SocketChannel accept = serverSocketChannel.accept();
      if (null != accept) {
        ReceiverFile receiverFile = new ReceiverFile();

        // 将上传文件保存路径记录下来
        receiverFile.uploadSavePathDir = uploadSavePathDir;
        NioSocket.MAP.put(accept, receiverFile);

        new ReadHandler(selector, accept);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

