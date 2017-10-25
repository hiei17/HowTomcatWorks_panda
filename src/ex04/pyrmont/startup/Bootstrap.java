/* explains Tomcat's default container */
package ex04.pyrmont.startup;

import ex04.pyrmont.core.SimpleContainer;
import org.apache.catalina.connector.http.HttpConnector;

public final class Bootstrap {
  public static void main(String[] args) {
    //这次用catalina默认的的
   /* The default connector also employs a few optimizations not used in Chapter 3's
    connector. The first is to provide a pool of various objects to avoid the expensive
    object creation. Secondly, in many places it uses char arrays instead of strings.*/
    HttpConnector connector = new HttpConnector();
    //这个自己的 也实现了catalina.Container 所以兼容
    SimpleContainer container = new SimpleContainer();
    connector.setContainer(container);
    try {
      connector.initialize();//主要是new ServerSocket(port, backlog)
      connector.start();

      // make the application wait until we press any key.
      System.in.read();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}