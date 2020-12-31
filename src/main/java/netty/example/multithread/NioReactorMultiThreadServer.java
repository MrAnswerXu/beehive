package netty.example.multithread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import netty.example.common.NioSocket;

/*
ref doc:
1. Netty编程实战之：Reactor反应器模式-https://juejin.cn/post/6850418110898536461
2. Netty编程实战之：Netty基础入门-https://juejin.cn/post/6854573210575486983
 */
public class NioReactorMultiThreadServer {

  private static final int THREAD_COUNT = 2;

  Selector[] selectors = new Selector[THREAD_COUNT];
  ServerSocketChannel serverSocketChannel;

  private SubReactor[] subReactors = null;
  private AtomicInteger atom = new AtomicInteger(0);

  public static void main(String[] args) {
    new NioReactorMultiThreadServer().startServer();
  }

  private void startServer() {
    for (int i = 0; i < THREAD_COUNT; i++) {
      new Thread(subReactors[i]).start();
    }
  }

  private NioReactorMultiThreadServer() {
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
      register.attach(new AcceptHandler(serverSocketChannel, selectors[atom.get()]));
      if (atom.incrementAndGet() == selectors.length) {
        atom.set(0);
      }
      subReactors = new SubReactor[THREAD_COUNT];
      for (int i = 0; i < THREAD_COUNT; i++) {
        subReactors[i] = new SubReactor(selectors[i]);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
