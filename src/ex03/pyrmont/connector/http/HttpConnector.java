package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
//tomcat2�����֮һ ������
public class HttpConnector implements Runnable {

  boolean stopped;
  private String scheme = "http";

  public String getScheme() {
    return scheme;
  }

  public void run() {
    ServerSocket serverSocket = null;
    int port = 8080;
    try {
      serverSocket =  new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    //是同步的 单线 这个请求处理完才能处理下一个请求
    while (!stopped) {
      // Accept the next incoming connection from the server socket
      Socket socket = null;
      try {
        socket = serverSocket.accept();//等在这
      }
      catch (Exception e) {
        continue;
      }
      // Hand this socket off to an HttpProcessor
      HttpProcessor processor = new HttpProcessor(this);
      //要等这个处理完 才能接待下一个请求
      processor.process(socket);
    }
  }

  public void start() {
    Thread thread = new Thread(this);
    thread.start();
  }
}