/* explains Tomcat's default container */
package ex04.pyrmont.startup;

import ex04.pyrmont.core.SimpleContainer;
import org.apache.catalina.connector.http.HttpConnector;

public final class Bootstrap {
  public static void main(String[] args) {

    //这次用catalina默认的的  实现了 org.apache.catalina.Connector; 接口
    HttpConnector connector = new HttpConnector();

    //这个自己的 也实现了catalina.Container
    //里面有 实现了 public void invoke(org.apache.catalina.Request request,org.apache.catalina.Response response);
    //容器负责接收连接器给它的request和response
    //然后 the container loads the servlet class, call its servicemethod, manage sessions, log error messages, etc.
    SimpleContainer container = new SimpleContainer();

    //比上一章节的改进是
    //1. provide a pool of various(HttpProcessor) objects to avoid the expensive   有一个HttpProcessor的线程pool
    //2.uses char arrays instead of strings. 解析head的时候
    //3. 支持HTTP 1.1 放while循环里面 没完就不断
    connector.setContainer(container);//会调用容器的invoke方法 传入request和response
    try {
      connector.initialize();//就是准备了 new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
      connector.start();//主要是线程开始

      // make the application wait until we press any key.
      System.in.read();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}