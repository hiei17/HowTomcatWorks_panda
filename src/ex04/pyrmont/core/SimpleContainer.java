package ex04.pyrmont.core;

import org.apache.catalina.*;

import javax.naming.directory.DirContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

public class SimpleContainer implements Container {

  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  + "webroot";

  public SimpleContainer() {
  }

  public String getInfo() {
    return null;
  }

  public Loader getLoader() {
    return null;
  }

  public void setLoader(Loader loader) {
  }

  public Logger getLogger() {
    return null;
  }

  public void setLogger(Logger logger) {
  }

  public Manager getManager() {
    return null;
  }

  public void setManager(Manager manager) {
  }

  public Cluster getCluster() {
    return null;
  }

  public void setCluster(Cluster cluster) {
  }

  public String getName() {
    return null;
  }

  public void setName(String name) {
  }

  public Container getParent() {
    return null;
  }

  public void setParent(Container container) {
  }

  public ClassLoader getParentClassLoader() {
    return null;
  }

  public void setParentClassLoader(ClassLoader parent) {
  }

  public Realm getRealm() {
    return null;
  }

  public void setRealm(Realm realm) {
  }

  public DirContext getResources() {
    return null;
  }

  public void setResources(DirContext resources) {
  }

  public void addChild(Container child) {
  }

  public void addContainerListener(ContainerListener listener) {
  }

  public void addMapper(Mapper mapper) {
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
  }

  public Container findChild(String name) {
    return null;
  }

  public Container[] findChildren() {
    return null;
  }

  public ContainerListener[] findContainerListeners() {
    return null;
  }

  public Mapper findMapper(String protocol) {
    return null;
  }

  public Mapper[] findMappers() {
    return null;
  }

  //TODO 只有这个不是空的 就是原来那个ServletProcessor 没加任何东西
  //加载url指定的Servlet 实例化  调用其service方法(传入参数request response
  public void invoke(Request request, Response response) throws IOException, ServletException {

    String servletName = ( (HttpServletRequest) request).getRequestURI();
    servletName = servletName.substring(servletName.lastIndexOf("/") + 1);
    URLClassLoader loader = getUrlClassLoader( );
    Class myClass = null;
    try {
      myClass = loader.loadClass(servletName);
      Servlet servlet  = (Servlet) myClass.newInstance();
      servlet.service((HttpServletRequest) request, (HttpServletResponse) response);

    } catch (Throwable e) {
      System.out.println(e.toString());
    }



  }

  private URLClassLoader getUrlClassLoader( ) {
    URLClassLoader loader=null;
    try {
      URL[] urls = new URL[1];
      URLStreamHandler streamHandler = null;
      File classPath = new File(WEB_ROOT);
      String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
      urls[0] = new URL(null, repository, streamHandler);
        loader = new URLClassLoader(urls);
    }
    catch (IOException e) {
      System.out.println(e.toString() );
    }
    return loader;
  }

  public Container map(Request request, boolean update) {
    return null;
  }

  public void removeChild(Container child) {
  }

  public void removeContainerListener(ContainerListener listener) {
  }

  public void removeMapper(Mapper mapper) {
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
  }

}