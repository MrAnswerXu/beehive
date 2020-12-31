package netty.example.multithread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import netty.example.common.NioSocket;

class NioReactorSingleThreadServer implements Runnable {


  ServerSocketChannel serverSocketChannel;
  Selector selector;

  public static void main(String[] args) {
    new Thread(new NioReactorSingleThreadServer()).start();
  }

  public NioReactorSingleThreadServer() {

    // 服务器端编写
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
      selector = Selector.open();
      SelectionKey register = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

      register
          .attach(new AcceptHandler(serverSocketChannel, selector));


    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void run() {
    try {
      while (!Thread.interrupted()) {
        selector.select();
        Set<SelectionKey> selectionKeys = selector.selectedKeys();

        for (SelectionKey key : selectionKeys) {
          // 监控IO事件，然后分发
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
