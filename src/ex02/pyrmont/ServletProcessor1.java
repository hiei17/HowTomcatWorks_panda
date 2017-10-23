package ex02.pyrmont;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

public class ServletProcessor1 {

  public void process(Request request, Response response) {

    try {
        //得类加载器
      URLClassLoader  loader = getUrlClassLoader();
      //servlet/servletName
      String uri = request.getUri();
      String servletName = uri.substring(uri.lastIndexOf("/") + 1);
     //加载请求指定的servlet类
      Class myClass = loader.loadClass(servletName);
//从类得实例
      Servlet servlet = (Servlet) myClass.newInstance();
      //request里面有所有请求信息
        //response可得到输出流
        //2个都传要给Servlet
      servlet.service(request, response);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

	//create a URLClassLoader
	private URLClassLoader getUrlClassLoader() throws IOException {

		File classPath = new File(Constants.WEB_ROOT);
		String file = classPath.getCanonicalPath() + File.separator;

		//为啥这么写以后再说
		URL url = new URL("file", null, file);
		URL[] urls = new URL[1];
		urls[0] = new URL(null, url.toString(), (URLStreamHandler) null);

		URLClassLoader	loader = new URLClassLoader(urls);
		return loader;
	}
}