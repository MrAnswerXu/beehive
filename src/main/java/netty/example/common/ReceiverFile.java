package netty.example.common;

import java.nio.channels.FileChannel;

public class ReceiverFile {

  public String fileName;
  public long length;
  public FileChannel outChannel;
  public String uploadSavePathDir;
}