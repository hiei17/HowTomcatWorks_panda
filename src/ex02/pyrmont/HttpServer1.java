package ex02.pyrmont;//package ex02.pyrmont;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer1 {

  /** WEB_ROOT is the directory where our HTML and other files reside.
   *  For this package, WEB_ROOT is the "webroot" directory under the working
   *  directory.
   *  The working directory is the location in the file system
   *  from where the java command was invoked.
   */
  // shutdown command
  private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

  // the shutdown command received
  private boolean shutdown = false;

  public static void main(String[] args) {
    HttpServer1 server = new HttpServer1();
    server.await();
  }

  public void await() {
    ServerSocket serverSocket = null;
    try {
      serverSocket =  new ServerSocket(8080, 1, InetAddress.getByName("127.0.0.1"));
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    // Loop waiting for a request
    while (!shutdown) {
      Socket socket = null;
      InputStream input = null;
      OutputStream output = null;
      try {
        socket = serverSocket.accept();//没有对这个端口的请求 就会卡这里
        input = socket.getInputStream();
        output = socket.getOutputStream();

        // create Request object and parse
        Request request = new Request(input);
        request.parse();//输入流解析出url

        // create Response object
        Response response = new Response(output);
        response.setRequest(request);

        //http://localhost:8080/index.html
        //http://localhost:8080/servlet/PrimitiveServlet
        String uri = request.getUri();

        //TODO 这个新加
        // a request for a servlet begins with "/servlet/"
        if (uri.startsWith("/servlet/")) {//request for a servlet
          ServletProcessor1 processor = new ServletProcessor1();
          processor.process(request, response);//
        }
        else {//for  a static resource
          //第一章实现的
          StaticResourceProcessor processor = new StaticResourceProcessor();
          processor.process(request, response);//内部调用response的方法找到request要的文件 输出到输出流
        }

        // Close the socket
        socket.close();
        //check if the previous URI is a shutdown command
        shutdown = uri.equals(SHUTDOWN_COMMAND);

      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
