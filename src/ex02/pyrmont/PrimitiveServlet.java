package ex02.pyrmont;

import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
/**
 * @author Administrator
 */
public class PrimitiveServlet implements Servlet {
    // is called by the servlet container after the servlet class has been instantiated.
   // can override this method to write initialization code  that needs to run only once, such as loading a database driver, initializing values,
    //放初始化的 一般空着就行
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("init");
    }
    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        System.out.println("from service");
        PrintWriter out = response.getWriter();
        out.println("Hello. Roses are red.");//这可以
        out.print("Violets are blue.");//这个不行
    }

    //calls   before removing a servlet instancefrom service.
    // when the servlet container is shut down or the servlet container needs some free memory
    @Override
    public void destroy() {
        System.out.println("destroy");
    }
    @Override
    public String getServletInfo() {
        return null;
    }
    @Override
    public ServletConfig getServletConfig() {
        return null;
    }
}