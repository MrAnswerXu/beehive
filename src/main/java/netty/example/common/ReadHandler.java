package netty.example.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadHandler implements Runnable {

  private SocketChannel socketChannel;
  private ExecutorService executorService = Executors.newFixedThreadPool(4);

  public ReadHandler(Selector selector, SocketChannel accept) throws IOException {
    this.socketChannel = accept;

    socketChannel.configureBlocking(false);
    SelectionKey selectionKey = socketChannel.register(selector, 0);

    selectionKey.attach(this);
    selectionKey.interestOps(SelectionKey.OP_READ);
    selector.wakeup();
  }

  @Override
  public void run() {
    executorService.execute(() -> {
      try {
        processData();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private synchronized void processData() throws IOException {
    ReceiverFile receiverFile = NioSocket.MAP.get(socketChannel);

    ByteBuffer buffer = ByteBuffer.allocate(NioSocket.BUFFER_CAPACITY);

    int len = 0;

    while ((len = socketChannel.read(buffer)) > 0) {

      buffer.flip();

      if (receiverFile.fileName == null) {

        // 处理文件名称
        if (buffer.capacity() < 4) {
          continue;
        }

        int fileNameLength = buffer.getInt();

        byte[] fileNameArr = new byte[fileNameLength];
        buffer.get(fileNameArr);

        String fileName = new String(fileNameArr, NioSocket.CHARSET);
        System.out.println("文件名称：" + fileName);
        receiverFile.fileName = fileName;

        // 处理存储文件
        File dir = new File(receiverFile.uploadSavePathDir);
        if (!dir.exists()) {
          dir.mkdir();
        }

        File file = new File((receiverFile.uploadSavePathDir + File.separator + fileName).trim());
        if (!file.exists()) {
          file.createNewFile();
        }

        receiverFile.outChannel = new FileOutputStream(file).getChannel();

        // 长度
        if (buffer.capacity() < 8) {
          continue;
        }

        long fileLength = buffer.getLong();
        System.out.println("文件大小：" + fileLength);
        receiverFile.length = fileLength;

        // 文件内容
        if (buffer.capacity() < 0) {
          continue;
        }

        receiverFile.outChannel.write(buffer);
      } else {
        // 文件内容
        receiverFile.outChannel.write(buffer);
      }

      buffer.clear();
    }

    if (len == -1) {
      receiverFile.outChannel.close();
    }
  }
}