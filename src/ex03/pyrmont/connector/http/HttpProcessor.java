package ex03.pyrmont.connector.http;

import ex03.pyrmont.ServletProcessor;
import ex03.pyrmont.StaticResourceProcessor;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/* this class used to be called HttpServer */
public class HttpProcessor {

  public HttpProcessor(HttpConnector connector) {
    this.connector = connector;
  }
  /**
   * The HttpConnector with which this processor is associated.
   */
  private HttpConnector connector = null;
  private HttpRequest request;
  private HttpRequestLine requestLine = new HttpRequestLine();
  private HttpResponse response;

  protected String method = null;
  protected String queryString = null;

  /**
   * The string manager for this package.
   *///TODO
  protected StringManager sm = StringManager.getManager("ex03.pyrmont.connector.http");

  public void process(Socket socket) {

    SocketInputStream input = null;
    OutputStream output = null;
    try {
      //TODO InputStream的子类 更方便  有readRequestLine and readHeader
      input = new SocketInputStream(socket.getInputStream(), 2048);
      output = socket.getOutputStream();

      // create HttpRequest object and parse
      request = new HttpRequest(input);

      // create HttpResponse object
      response = new HttpResponse(output);
      response.setRequest(request);
      response.setHeader("Server", "Pyrmont Servlet Container");

      //TODO  are called to help populate the HttpRequest
      //必须是这个顺序 因为里面的输入字节流是从前往后处理  不可能回来
      //得get/post url 参数(可以放session_id
      parseRequest(input, output);//第一行 url;里面带的传参只整块保存 不解析 要了才解析

      //各种头 比如cookie content-length content-type
      parseHeaders(input);//headers
     // HTTP request body 开始不解析 用到才去解析

      //check if this is a request for a servlet or a static resource
      //a request for a servlet begins with "/servlet/"
      if (request.getRequestURI().startsWith("/servlet/")) {
        ServletProcessor servletProcessor = new ServletProcessor();
        servletProcessor.process(request, response);
      }
      else {//静态资源
        StaticResourceProcessor staticResourceProcessor = new StaticResourceProcessor();
        staticResourceProcessor.process(request, response);
      }

      // Close the socket
      socket.close();
      // no shutdown for this application
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This method is the simplified version of the similar method in
   * org.apache.catalina.connector.http.HttpProcessor.
   * However, this method only parses some "easy" headers, such as
   * "cookie", "content-length", and "content-type", and ignore other headers.
   * @param input The input stream connected to our socket
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if a parsing error occurs
   */
  private void parseHeaders(SocketInputStream input) throws IOException, ServletException {

    //解析没对head k-v  直到没有
    while (true) {// until there is no more header.

      HttpHeader header = new HttpHeader();;

      // Read the next header
      input.readHeader(header);

      //If there is no more header to read, both nameEnd and valueEnd fields of the HttpHeader instance will be zero.
      if (header.nameEnd == 0) {
        if (header.valueEnd == 0) {
          return;//这里出去
        }
        //不成对
        throw new ServletException(sm.getString("httpProcessor.parseHeaders.colon"));
      }

      //加一对
      String name = new String(header.name, 0, header.nameEnd);
      String value = new String(header.value, 0, header.valueEnd);
      request.addHeader(name, value);

      //有些需要特别处理 单独放好 cookie content-length content-type
      populateSpecialties(name, value);
    } //end while
  }

  private void populateSpecialties(String name, String value) throws ServletException {
    
    //Cookie: userName=budi; password=pwd;
    if ("cookie".equals(name)) {
      //the value is the cookie name/value pair(s).
      Cookie cookies[] = RequestUtil.parseCookieHeader(value);

      for (int i=0;i<cookies.length;i++) {
        request.addCookie(cookies[i]);

        //只有jsessionid要特殊处理
        if (!"jsessionid".equals( cookies[i].getName()))
          continue;

        // head里面设的 比url里面设的 优先级高  url已经设了就覆盖它
        if (!request.isRequestedSessionIdFromCookie()) {
          // Accept only the first session id cookie
          request.setRequestedSessionId( cookies[i].getValue());
          request.setRequestedSessionCookie(true);
          request.setRequestedSessionURL(false);
        }

      }
      return;
    }

    if ("content-length".equals(name)) {
       int n = -1;
       try {
         n = Integer.parseInt(value);
       }
       catch (Exception e) {
         throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
       }
       request.setContentLength(n);
      return;
     }

    if ("content-type".equals(name)) {
       request.setContentType(value);
     }
  }


  //就是为了填request
  private void parseRequest(SocketInputStream input, OutputStream output)
    throws IOException, ServletException {

    // Parse the incoming request line
    input.readRequestLine(requestLine);//requestLine里面放入如 GET /myApp/ModernServlet?userName=tarzan&password=pwd HTTP/1.1

    String method = new String(requestLine.method, 0, requestLine.methodEnd);//GET

    String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);//HTTP/1.1


    // Validate the incoming request line
    if (method.length() < 1) {
      throw new ServletException("Missing HTTP request method");
    }
    else if (requestLine.uriEnd < 1) {
      throw new ServletException("Missing HTTP request URI");
    }
    request.setMethod(method);
    request.setProtocol(protocol);

    //TODO  Parse any query parameters out of the request URI
    //?name=Tarzan
    int question = requestLine.indexOf("?");
    String uri = null;
    if (question >= 0) {//有带参
      //整段传参部分存request
      //只存整个字符串 不解析 用到才解析 省点
      request.setQueryString(new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1));
      //这段截掉
      uri = new String(requestLine.uri, 0, question);
    }
    else {
      //无参
      request.setQueryString(null);
      //整行都要
      uri = new String(requestLine.uri, 0, requestLine.uriEnd);
    }


    // Checking for an absolute URI (with the HTTP protocol)
    //绝对路径 如 http://www.brainysoftware.com/index.html
    if (!uri.startsWith("/")) {

      int pos = uri.indexOf("://");
      // Parsing out protocol and host name
      if (pos != -1) {
        // http:// 之后第一个/
        pos = uri.indexOf('/', pos + 3);

        if (pos == -1) {
          uri = "";
        } else {
          uri = uri.substring(pos);//只保留第一个/后面的
        }

      }
    }

    // may  contain a session identifier
    // Parse any requested session ID out of the request URI
    String match = ";jsessionid=";//session_id 一般会放cookie里面 但是这样用;jsessionid=XXX;放url里面也行(cookie被禁用时只能这样
    int semicolon = uri.indexOf(match);
    if (semicolon >= 0) {
      String rest = uri.substring(semicolon + match.length());
      int semicolon2 = rest.indexOf(';');
      if (semicolon2 >= 0) {
        request.setRequestedSessionId(rest.substring(0, semicolon2));
        rest = rest.substring(semicolon2);
      }
      else {
        request.setRequestedSessionId(rest);
        rest = "";
      }
      // means that the session identifier is carried in the query string, and not in a cookie.
      request.setRequestedSessionURL(true);
      //TODO been stripped off the jsessionid.
      uri = uri.substring(0, semicolon) + rest;
    }
    else {//session identifier is carried  in a cookie.
      request.setRequestedSessionId(null);
      request.setRequestedSessionURL(false);
    }

//第一个/和?之间那段 去掉";jsessionid=XXX;"
    // Normalize URI (using String operations at the moment)
    //If uri is in good format or if the abnormality can be corrected, normalize returns the same URI or the corrected one.
    String normalizedUri = normalize(uri);
    if (normalizedUri != null) {
      //url正常
     request.setRequestURI(normalizedUri);
    }
    else {
      //url不正常 报错
      request.setRequestURI(uri);
      throw new ServletException("Invalid URI: " + uri + "'");
    }


  }

  /**
   * Return a context-relative path, beginning with a "/", that represents
   * the canonical version of the specified path after ".." and "." elements
   * are resolved out.  If the specified path attempts to go outside the
   * boundaries of the current context (i.e. too many ".." path elements
   * are present), return <code>null</code> instead.
   *
   * @param path Path to be normalized
   */
  protected String normalize(String path) {
    if (path == null)
      return null;

    // Create a place for the normalized path
    String normalized = path;

    // Normalize "/%7E" and "/%7e" at the beginning to "/~"
    if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e"))
      normalized = "/~" + normalized.substring(4);

    //这些是保留字符 出现了就不行
    // Prevent encoding '%', '/', '.' and '\', which are special reserved
    // characters
    if ((normalized.indexOf("%25") >= 0)
      || (normalized.indexOf("%2F") >= 0)
      || (normalized.indexOf("%2E") >= 0)
      || (normalized.indexOf("%5C") >= 0)
      || (normalized.indexOf("%2f") >= 0)
      || (normalized.indexOf("%2e") >= 0)
      || (normalized.indexOf("%5c") >= 0)) {
      return null;
    }

    if (normalized.equals("/."))
      return "/";

    //'\\'变'/'
    // Normalize the slashes and add leading slash if necessary
    if (normalized.indexOf('\\') >= 0)
      normalized = normalized.replace('\\', '/');

    //以"/"开头
    if (!normalized.startsWith("/"))
      normalized = "/" + normalized;

    //有"//" 去掉
    // Resolve occurrences of "//" in the normalized path
    while (true) {
      int index = normalized.indexOf("//");
      if (index < 0)
        break;

      normalized = normalized.substring(0, index) + normalized.substring(index + 1);
    }

    // Resolve occurrences of "/./" in the normalized path
    while (true) {
      int index = normalized.indexOf("/./");
      if (index < 0)
        break;
      normalized = normalized.substring(0, index) +
        normalized.substring(index + 2);
    }

    // Resolve occurrences of "/../" in the normalized path
    while (true) {
      int index = normalized.indexOf("/../");
      if (index < 0)
        break;
      if (index == 0)
        return (null);  // Trying to go outside our context

      int index2 = normalized.lastIndexOf('/', index - 1);
      normalized = normalized.substring(0, index2) +
        normalized.substring(index + 3);
    }

    // Declare occurrences of "/..." (three or more dots) to be invalid
    // (on some Windows platforms this walks the directory tree!!!)
    if (normalized.indexOf("/...") >= 0)
      return (null);

    // Return the normalized path that we have completed
    return (normalized);

  }

}
