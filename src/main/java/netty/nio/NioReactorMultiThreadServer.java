package netty.nio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// ref doc: Netty编程实战之：Reactor反应器模式-https://juejin.cn/post/6850418110898536461
public class NioReactorMultiThreadServer {

  private static final int THREAD_COUNT = 2;

  Selector[] selectors = new Selector[THREAD_COUNT];
  ServerSocketChannel serverSocketChannel;

  private SubReactor[] subReactors = null;

  String UPLOAD_SAVE_PATH = "";
  AtomicInteger atom = new AtomicInteger(0);

  public static void main(String[] args) {
    new NioReactorMultiThreadServer().startServer();
  }

  private void startServer() {
    for (int i = 0; i < THREAD_COUNT; i++) {
      new Thread(subReactors[i]).start();
    }
  }

  private void getUploadSavePath() {
    System.out.println("请输入想要保存文件的路劲：");
    Scanner scanner = new Scanner(System.in);
    UPLOAD_SAVE_PATH = scanner.next();
  }

  private NioReactorMultiThreadServer() {
    getUploadSavePath();

    try {
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);

      // 绑定端口
      serverSocketChannel.bind(
          new InetSocketAddress(
              NioSocket.PORT
          )
      );

      // 绑定选择器
      for (int i = 0; i < THREAD_COUNT; i++) {
        selectors[i] = Selector.open();
      }

      // 第一个选择器 监听连接
      SelectionKey register = serverSocketChannel.register(selectors[0], SelectionKey.OP_ACCEPT);

      // 附加到选择键上
      register.attach(new AcceptHandler(this));

      subReactors = new SubReactor[THREAD_COUNT];
      for (int i = 0; i < THREAD_COUNT; i++) {
        subReactors[i] = new SubReactor(selectors[i]);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  class SubReactor implements Runnable {

    private Selector selector;

    SubReactor(Selector selector) {
      this.selector = selector;
    }

    @Override
    public void run() {
      try {
        while (!Thread.interrupted()) {
          selector.select();
          Set<SelectionKey> selectionKeys = selector.selectedKeys();

          for (SelectionKey key : selectionKeys) {
            dispatch(key);
          }
          selectionKeys.clear();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    private void dispatch(SelectionKey key) {
      Runnable attachment = (Runnable) key.attachment();
      if (null != attachment) {
        attachment.run();
      }
    }
  }


  // 为了简单
  class ReceiverFile {

    public String fileName;
    public long length;
    public FileChannel outChannel;
    public String uploadSavePath;
  }

  class AcceptHandler implements Runnable {

    private NioReactorMultiThreadServer nioReactorMultiThreadServer;

    AcceptHandler(NioReactorMultiThreadServer nioReactorMultiThreadServer) {
      this.nioReactorMultiThreadServer = nioReactorMultiThreadServer;
    }

    @Override
    public void run() {
      try {
        SocketChannel accept = nioReactorMultiThreadServer.serverSocketChannel.accept();
        if (null != accept) {
          ReceiverFile receiverFile = new ReceiverFile();

          // 将上传文件保存路径记录下来
          receiverFile.uploadSavePath = nioReactorMultiThreadServer.UPLOAD_SAVE_PATH;
          NioSocket.MAP.put(accept, receiverFile);

          // 采用轮询的方式获取到选择器
          new ReadHandler(
              nioReactorMultiThreadServer.selectors[nioReactorMultiThreadServer.atom.get()],
              accept);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (nioReactorMultiThreadServer.atom.incrementAndGet()
            == nioReactorMultiThreadServer.selectors.length) {
          nioReactorMultiThreadServer.atom.set(0);
        }
      }
    }
  }

  class ReadHandler implements Runnable {

    private SocketChannel socketChannel;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    ReadHandler(Selector selector, SocketChannel accept) throws IOException {
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
          File dir = new File(receiverFile.uploadSavePath);
          if (!dir.exists()) {
            dir.mkdir();
          }

          File file = new File((receiverFile.uploadSavePath + File.separator + fileName).trim());
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
}
