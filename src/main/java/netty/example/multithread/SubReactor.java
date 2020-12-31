package netty.example.multithread;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class SubReactor implements Runnable {

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

