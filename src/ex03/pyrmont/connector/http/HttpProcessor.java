package ex03.pyrmont.connector.http;

import ex03.pyrmont.ServletProcessor;
import ex03.pyrmont.StaticResourceProcessor;

import java.net.Socket;
import java.io.OutputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;

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
      response.setRequest(request);

      response.setHeader("Server", "Pyrmont Servlet Container");

      //TODO  are called to help populate the HttpRequest
      parseRequest(input, output);//第一行
      parseHeaders(input);//headers
     // HTTP request body 开始不解析 用到才去解析

      //check if this is a request for a servlet or a static resource
      //a request for a servlet begins with "/servlet/"
      if (request.getRequestURI().startsWith("/servlet/")) {
        ServletProcessor processor = new ServletProcessor();
        processor.process(request, response);
      }
      else {
        StaticResourceProcessor processor = new StaticResourceProcessor();
        processor.process(request, response);
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

    while (true) {// until there is no more header.
      HttpHeader header = new HttpHeader();;

      // Read the next header
      input.readHeader(header);
      //If there is no more header to read, both nameEnd and valueEnd fields of the HttpHeader instance will be zero.
      if (header.nameEnd == 0) {
        if (header.valueEnd == 0) {
          return;
        }
        //不成对
        throw new ServletException(sm.getString("httpProcessor.parseHeaders.colon"));
      }

      //加一对
      String name = new String(header.name, 0, header.nameEnd);
      String value = new String(header.value, 0, header.valueEnd);
      request.addHeader(name, value);

      // do something for some headers, ignore others.

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
        continue;
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
        continue;
      }

     if ("content-type".equals(name)) {
        request.setContentType(value);
      }
    } //end while
  }


  //就是为了填request
  private void parseRequest(SocketInputStream input, OutputStream output)
    throws IOException, ServletException {

    // Parse the incoming request line
    input.readRequestLine(requestLine);//requestLine里面放入如 GET /myApp/ModernServlet?userName=tarzan&password=pwd HTTP/1.1
    String method = new String(requestLine.method, 0, requestLine.methodEnd);//GET
    String uri = null;
    String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);//HTTP/1.1

    // Validate the incoming request line
    if (method.length() < 1) {
      throw new ServletException("Missing HTTP request method");
    }
    else if (requestLine.uriEnd < 1) {
      throw new ServletException("Missing HTTP request URI");
    }

    //TODO  Parse any query parameters out of the request URI
    int question = requestLine.indexOf("?");
    if (question >= 0) {
      //只存 不解析 用到才解析 省点
      request.setQueryString(new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1));
      uri = new String(requestLine.uri, 0, question);
    }
    else {
      request.setQueryString(null);
      uri = new String(requestLine.uri, 0, requestLine.uriEnd);
    }


    // Checking for an absolute URI (with the HTTP protocol)
    //可能是绝对路径 如 http://www.brainysoftware.com/index.html?name=Tarzan
    if (!uri.startsWith("/")) {
      int pos = uri.indexOf("://");
      // Parsing out protocol and host name
      if (pos != -1) {
        pos = uri.indexOf('/', pos + 3);
        if (pos == -1) {
          uri = "";
        }
        else {
          uri = uri.substring(pos);
        }
      }
    }

    // may  contain a session identifier
    // Parse any requested session ID out of the request URI
    String match = ";jsessionid=";
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
      //been stripped off the jsessionid.
      uri = uri.substring(0, semicolon) + rest;
    }
    else {//session identifier is carried  in a cookie.
      request.setRequestedSessionId(null);
      request.setRequestedSessionURL(false);
    }

    // Normalize URI (using String operations at the moment)
    //If uri is in good format or if the abnormality can be corrected, normalize returns the same URI or the corrected one.
    String normalizedUri = normalize(uri);

    // Set the corresponding request properties
    request.setMethod(method);
    request.setProtocol(protocol);
    if (normalizedUri != null) {
     request.setRequestURI(normalizedUri);
    }
    else {
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

    // Normalize the slashes and add leading slash if necessary
    if (normalized.indexOf('\\') >= 0)
      normalized = normalized.replace('\\', '/');
    if (!normalized.startsWith("/"))
      normalized = "/" + normalized;

    // Resolve occurrences of "//" in the normalized path
    while (true) {
      int index = normalized.indexOf("//");
      if (index < 0)
        break;
      normalized = normalized.substring(0, index) +
        normalized.substring(index + 1);
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
