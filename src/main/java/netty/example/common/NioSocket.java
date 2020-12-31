package netty.example.common;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NioSocket {

  public static final String HOST = "127.0.0.1";
  public static final int PORT = 23356;
  public static final int BUFFER_CAPACITY = 1024;
  public static final Charset CHARSET = StandardCharsets.UTF_8;
  public static final Map<SocketChannel, ReceiverFile> MAP = new ConcurrentHashMap<>(16);
  public static final String UPLOAD_SAVE_DIR_PATH = "./tmp";
}
