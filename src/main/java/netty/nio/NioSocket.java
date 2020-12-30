package netty.nio;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import netty.nio.NioReactorMultiThreadServer.ReceiverFile;

class NioSocket {

  static final String HOST = "127.0.0.1";
  static final int PORT = 23356;
  static final int BUFFER_CAPACITY = 1024;
  static final Charset CHARSET = StandardCharsets.UTF_8;
  static final Map<SocketChannel, ReceiverFile> MAP = new ConcurrentHashMap<>(16);
}
