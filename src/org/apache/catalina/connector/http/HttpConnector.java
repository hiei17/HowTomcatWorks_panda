package org.apache.catalina.connector.http;


import org.apache.catalina.*;
import org.apache.catalina.net.DefaultServerSocketFactory;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Stack;
import java.util.Vector;



/**
 * Implementation of an HTTP/1.1 connector.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.34 $ $Date: 2002/03/18 07:15:39 $
 * 
 */


public final class HttpConnector implements Connector, Lifecycle, Runnable {


    /**
     * The <code>Service</code> we are associated with (if any).
     */
    private Service service = null;


    /**
     * The accept count for this Connector.
     */
    private int acceptCount = 10;

    /**
     * The input buffer size we should create on input streams.
     */
    private int bufferSize = 2048;


    /**
     * The Container used for processing requests received by this Connector.
     *///panda 我们自己实现过一个 叫SimpleContainer 里面有个invoke  加载url指定的Servlet 实例化  调用其service方法(传入参数request response
    protected Container container = null;


    /**
     * The debugging detail level for this component.
     */
    private int debug =  0;

    //我自己定义的
    private HttpProcessorManager httpProcessorManager =new HttpProcessorManager();


    /**
     * The "enable DNS lookups" flag for this Connector.
     */
    private boolean enableLookups = false;


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * Timeout value on the incoming connection.
     * Note : a value of 0 means no timeout.
     */
    private int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT;


    /**
     * The port number on which we listen for HTTP requests.
     */
    private int port = 8080;




    /**
     * Begin processing requests via this Connector.
     *
     * @exception LifecycleException if a fatal startup error occurs
     */
    public void start() throws LifecycleException {

        // Validate and update our current state
        if (started){//已经开始过了
            throw new LifecycleException(stringManager.getString("httpConnector.alreadyStarted"));
        }

        threadName = "HttpConnector[" + port + "]";
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Start our background thread
        threadStart();//TODO 本线程开始run

        //panda Create the specified minimum number of processors

        httpProcessorManager.createMin();


    }




    /**
     * The server name to which we should pretend requests to this Connector
     * were directed.  This is useful when operating Tomcat behind a proxy
     * server, so that redirects get constructed accurately.  If not specified,
     * the server name included in the <code>Host</code> header is used.
     */
    private String proxyName = null;


    /**
     * The server port to which we should pretent requests to this Connector
     * were directed.  This is useful when operating Tomcat behind a proxy
     * server, so that redirects get constructed accurately.  If not specified,
     * the port number specified by the <code>port</code> property is used.
     */
    private int proxyPort = 0;


    /**
     * The redirect port for non-SSL to SSL redirects.
     */
    private int redirectPort = 443;


    /**
     * The request scheme that will be set on all requests received
     * through this connector.
     */
    private String scheme = "http";


    /**
     * The secure connection flag that will be set on all requests received
     * through this connector.
     */
    private boolean secure = false;





    /**
     * The string manager for this package. 用来管理异常打印信息的
     */
    private StringManager stringManager = StringManager.getManager(Constants.Package);





    /**
     * Has this component been started yet?
     */
    private boolean started = false;


    /**
     * The shutdown signal to our background thread
     */
    private boolean stopped = false;


    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * The name to register for the background thread.
     */
    private String threadName = null;


    /**
     * The thread synchronization object.
     */
    private final Object threadSync = new Object();


    /**
     * Is chunking allowed ?
     */
    private boolean allowChunking = true;


    /**
     * Use TCP no delay ?
     */
    private boolean tcpNoDelay = true;


    // ------------------------------------------------------------- Properties


    /**
     * Return the <code>Service</code> with which we are associated (if any).
     */
    public Service getService() {

        return (this.service);

    }


    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
    public void setService(Service service) {

        this.service = service;

    }


    /**
     * Get the allow chunking flag.
     */
    public boolean isChunkingAllowed() {

        return (allowChunking);

    }







    /**
     * Is this connector available for processing requests?
     */
    public boolean isAvailable() {

        return (started);

    }


    /**
     * Return the input buffer size for this Connector.
     */
    public int getBufferSize() {

        return (this.bufferSize);

    }


    /**
     * Set the input buffer size for this Connector.
     *
     * @param bufferSize The new input buffer size.
     */
    public void setBufferSize(int bufferSize) {

        this.bufferSize = bufferSize;

    }



    //以下4个方法是关键
    public Container getContainer() {
        return (container);
    }
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Create (or allocate) and return a Request object suitable for
     * specifying the contents of a Request to the responsible Container.
     *///panda  创造Request  等HttpProcessor调用
    public Request createRequest() {

        //        if (debug >= 2)
        //            log("createRequest: Creating new request");
        HttpRequestImpl request = new HttpRequestImpl();
        request.setConnector(this);
        return (request);

    }

    /**
     * Create (or allocate) and return a Response object suitable for
     * receiving the contents of a Response from the responsible Container.
     *///panda 负责提供一个Response //HttpProcessor构造方法来调用
    public Response createResponse() {

        //        if (debug >= 2)
        //            log("createResponse: Creating new response");
        HttpResponseImpl response = new HttpResponseImpl();
        response.setConnector(this);
        return (response);

    }





    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }


    /**
     * Return the "enable DNS lookups" flag.
     */
    public boolean getEnableLookups() {

        return (this.enableLookups);

    }


    /**
     * Set the "enable DNS lookups" flag.
     *
     * @param enableLookups The new "enable DNS lookups" flag value
     */
    public void setEnableLookups(boolean enableLookups) {

        this.enableLookups = enableLookups;

    }





    /**
     * Set the server socket factory used by this Container.
     *
     * @param factory The new server socket factory
     */
    public void setFactory(ServerSocketFactory factory) {

        this.factory = factory;

    }



    /**
     * Descriptive information about this Connector implementation.
     */
    private static final String INFO = "org.apache.catalina.connector.http.HttpConnector/1.0";
    public String getInfo() {

        return (INFO);

    }




    /**
     * Return the port number on which we listen for HTTP requests.
     */
    public int getPort() {

        return port;

    }


    /**
     * Set the port number on which we listen for HTTP requests.
     *
     * @param port The new port number
     */
    public void setPort(int port) {

        this.port = port;

    }


    /**
     * Return the proxy server name for this Connector.
     */
    public String getProxyName() {

        return (this.proxyName);

    }





    /**
     * Return the proxy server port for this Connector.
     */
    public int getProxyPort() {

        return proxyPort;

    }



    /**
     * Return the port number to which a request should be redirected if
     * it comes in on a non-SSL port and is subject to a security constraint
     * with a transport guarantee that requires SSL.
     */
    public int getRedirectPort() {

        return (this.redirectPort);

    }


    /**
     * Set the redirect port number.
     *
     * @param redirectPort The redirect port number (non-SSL to SSL)
     */
    public void setRedirectPort(int redirectPort) {

        this.redirectPort = redirectPort;

    }


    /**
     * Return the scheme that will be assigned to requests received
     * through this connector.  Default value is "http".
     */
    public String getScheme() {

        return (this.scheme);

    }


    /**
     * Set the scheme that will be assigned to requests received through
     * this connector.
     *
     * @param scheme The new scheme
     */
    public void setScheme(String scheme) {

        this.scheme = scheme;

    }


    /**
     * Return the secure connection flag that will be assigned to requests
     * received through this connector.  Default value is "false".
     */
    public boolean getSecure() {

        return (this.secure);

    }


    /**
     * Set the secure connection flag that will be assigned to requests
     * received through this connector.
     *
     * @param secure The new secure connection flag
     */
    public void setSecure(boolean secure) {

        this.secure = secure;

    }






    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        Logger logger = container.getLogger();
        String localName = threadName;
        if (localName == null)
            localName = "HttpConnector";
        if (logger != null)
            logger.log(localName + " " + message);
        else
            System.out.println(localName + " " + message);

    }


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = container.getLogger();
        String localName = threadName;
        if (localName == null)
            localName = "HttpConnector";
        if (logger != null)
            logger.log(localName + " " + message, throwable);
        else {
            System.out.println(localName + " " + message);
            throwable.printStackTrace(System.out);
        }

    }








    // ---------------------------------------------- Background Thread Methods


    /**
     * The server socket through which we listen for incoming TCP connections.
     */
    private ServerSocket serverSocket = null;
    /**
     * The background thread that listens for incoming TCP/IP connections and
     * hands them off to an appropriate processor.
     * 来给请求开一个HttpProcessor 不本实例传参进去让其解析
     * 不等它解析结束 继续接待下一个请求
     */
    public void run() {//TODO 本类开始 和主体
        // Loop until we receive a shutdown command
        //死循环等待请求
        while (!stopped) {

            Socket socket ;
            try {
                //                if (debug >= 3)
                //                    log("run: Waiting on serverSocket.accept()");
                //卡在这 等请求
                socket = serverSocket.accept();
                //                if (debug >= 3)
                //                    log("run: Returned from serverSocket.accept()");
                if (connectionTimeout > 0)
                    socket.setSoTimeout(connectionTimeout);
                socket.setTcpNoDelay(tcpNoDelay);

            } catch (AccessControlException ace) {
                log("socket accept security exception", ace);
                continue;
            } catch (IOException e) {
                //                if (debug >= 3)
                //                    log("run: Accept returned IOException", e);
                try {
                    // If reopening fails, exit
                    synchronized (threadSync) {
                        if (started && !stopped)
                            log("accept error: ", e);
                        if (!stopped) {
                            //                    if (debug >= 3)
                            //                        log("run: Closing server socket");
                            serverSocket.close();
                            //                        if (debug >= 3)
                            //                            log("run: Reopening server socket");
                            serverSocket = open();
                        }
                    }
                    //                    if (debug >= 3)
                    //                        log("run: IOException processing completed");
                } catch (IOException ioe) {
                    log("socket reopen, io problem: ", ioe);
                    break;
                } catch (KeyStoreException kse) {
                    log("socket reopen, keystore problem: ", kse);
                    break;
                } catch (NoSuchAlgorithmException nsae) {
                    log("socket reopen, keystore algorithm problem: ", nsae);
                    break;
                } catch (CertificateException ce) {
                    log("socket reopen, certificate problem: ", ce);
                    break;
                } catch (UnrecoverableKeyException uke) {
                    log("socket reopen, unrecoverable key: ", uke);
                    break;
                } catch (KeyManagementException kme) {
                    log("socket reopen, key management problem: ", kme);
                    break;
                }

                continue;
            }

            // Hand this socket off to an appropriate processor
            //去池中取一个HttpProcessor(已经run了
            HttpProcessor processor = httpProcessorManager.createProcessor();
            //满了 不接受新请求了
            if (processor == null) {
                try {
                    log(stringManager.getString("httpConnector.noProcessor"));
                    socket.close();
                } catch (IOException e) {
                }
                continue;

            }
            //            if (debug >= 3)
            //                log("run: Assigning socket to processor " + processor);

            //It's now the HttpProcessor instance's job to read the socket's input stream and parse the HTTP request.
            //进去就wait()了
            try {
                processor.assign(socket);//把socket传过去 让这个线程开始解析
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // The processor will recycle itself when it finishes

        }

        // Notify the threadStop() method that we have shut ourselves down
        //        if (debug >= 3)
        //            log("run: Notifying threadStop() that we have shut down");
        synchronized (threadSync) {
            threadSync.notifyAll();
        }

    }


    /**
     * Start the background processing thread.
     */
    private void threadStart() {

        log(stringManager.getString("httpConnector.starting"));

        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();

    }


    /**
     * Stop the background processing thread.
     */
    private void threadStop() {

        log(stringManager.getString("httpConnector.stopping"));

        stopped = true;
        try {
            threadSync.wait(5000);
        } catch (InterruptedException e) {
        }
        thread = null;

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }


    // Has this component been initialized yet?
    private boolean initialized = false;
    //Initialize this connector (create ServerSocket here!)
    public void initialize() throws LifecycleException {

        //第二次调用了 不行
        if (initialized){
            String message = stringManager.getString("httpConnector.alreadyInitialized");
            throw new LifecycleException (message);
        }

        initialized=true;
        Exception eRethrow = null;

        // Establish a server socket on the specified port
        try {
            serverSocket = open();//TODO 主要是这个 不是直接构造new出来  改进为通过工厂
        } catch (IOException ioe) {
            log("httpConnector, io problem: ", ioe);
            eRethrow = ioe;
        } catch (KeyStoreException kse) {
            log("httpConnector, keystore problem: ", kse);
            eRethrow = kse;
        } catch (NoSuchAlgorithmException nsae) {
            log("httpConnector, keystore algorithm problem: ", nsae);
            eRethrow = nsae;
        } catch (CertificateException ce) {
            log("httpConnector, certificate problem: ", ce);
            eRethrow = ce;
        } catch (UnrecoverableKeyException uke) {
            log("httpConnector, unrecoverable key: ", uke);
            eRethrow = uke;
        } catch (KeyManagementException kme) {
            log("httpConnector, key management problem: ", kme);
            eRethrow = kme;
        }

        if ( eRethrow != null )
            throw new LifecycleException(threadName + ".open", eRethrow);

    }
    /**
     * Open and return the server socket for this Connector.  If an IP
     * address has been specified, the socket will be opened only on that
     * address; otherwise it will be opened on all addresses.
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file (SSL only)
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider (SSL only)
     * @exception CertificateException       general certificate error (SSL only)
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate (SSL only)
     * @exception KeyManagementException     problem in the key management
     *                                       layer (SSL only)
     */
    private ServerSocket open() throws IOException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException,
            KeyManagementException
    {

        // Acquire the server socket factory for this Connector
        ServerSocketFactory factory = getFactory();//只是里面同步锁 同步new了一个

        //非空判断
        // If no address is specified, open a connection on all addresses
        if (address == null) {
            log(stringManager.getString("httpConnector.allAddresses"));
            try {
                //new ServerSocket(port, backlog)
                return (factory.createSocket(port, acceptCount));//port 8080   acceptCount默认10
            } catch (BindException be) {
                throw new BindException(be.getMessage() + ":" + port);
            }
        }


        // Open a server socket on the specified address
        try {
            InetAddress is = InetAddress.getByName(address);
            log(stringManager.getString("httpConnector.anAddress", address));
            try {
                return (factory.createSocket(port, acceptCount, is));//里面一个普通的 new ServerSocket(8080, 1, InetAddress.getByName("127.0.0.1"));
            } catch (BindException be) {
                throw new BindException(be.getMessage() + ":" + address +
                        ":" + port);
            }
        } catch (Exception e) {
            log(stringManager.getString("httpConnector.noAddress", address));
            try {
                return (factory.createSocket(port, acceptCount));
            } catch (BindException be) {
                throw new BindException(be.getMessage() + ":" + port);
            }
        }


    }

    /**
     * The server socket factory for this component.
     */
    private ServerSocketFactory factory = null;
    public ServerSocketFactory getFactory() {

        if (this.factory == null) {
            synchronized (this) {
                this.factory = new DefaultServerSocketFactory();
            }
        }
        return (this.factory);

    }


    /**
     * The IP address on which to bind, if any.  If <code>null</code>, all
     * addresses on the server will be bound.
     */
    private String address = null;
    public String getAddress() {

        return (this.address);

    }
    public void setAddress(String address) {

        this.address = address;

    }



    /**
     * Terminate processing requests via this Connector.
     *
     * @exception LifecycleException if a fatal shutdown error occurs
     */
    public void stop() throws LifecycleException {

        // Validate and update our current state
        if (!started)
            throw new LifecycleException
                (stringManager.getString("httpConnector.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Gracefully shut down all processors we have created
        httpProcessorManager.shutDownAll();


        synchronized (threadSync) {
            // Close the server socket we were using
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
            // Stop our background thread
            threadStop();
        }
        serverSocket = null;

    }




    private class HttpProcessorManager {

        /**
         * The set of processors that have been created but are not currently
         * being used to process a request.
         *///TODO HttpProcessor变不止一个 并行 可以同时处理很多请求
        private final Stack processors = new Stack();
        int minProcessors = 5;



        private int maxProcessors = 20;//The maximum number of processors allowed, or <0 for unlimited.

        private int curProcessors = 0;


        /**
         * The set of processors that have ever been created.
         * Each HttpProcessor instance is responsible for parsing the HTTP request line and
         headers and populates a request object.
         */
        private Vector created = new Vector();

        /**
         * Recycle the specified Processor so that it can be used again.
         *
         * @param processor The processor to be recycled
         */
        void recycle(HttpProcessor processor) {

            //        if (debug >= 2)
            //            log("recycle: Recycling processor " + processor);
            processors.push(processor);

        }

        /**
         * Create (or allocate) and return an available processor for use in
         * processing a specific HTTP request, if possible.  If the maximum
         * allowed processors have already been created and are in use, return
         * <code>null</code> instead.
         */
        private HttpProcessor createProcessor() {

            synchronized (processors) {

                //还有 直接给
                if (processors.size() > 0) {
                    // if (debug >= 2)
                    // log("createProcessor: Reusing existing processor");
                    return ((HttpProcessor) processors.pop());
                }
                //没到new上限
                if ((maxProcessors > 0) && (curProcessors < maxProcessors)) {
                    // if (debug >= 2)
                    // log("createProcessor: Creating new processor");
                    return (newProcessor());
                } else {

                    //无上限new
                    if (maxProcessors < 0) {
                        // if (debug >= 2)
                        // log("createProcessor: Creating new processor");
                        return (newProcessor());
                    } else {
                        // if (debug >= 2)
                        // log("createProcessor: Cannot create new processor");
                        return (null);
                    }
                }

            }

        }

        /**
         * Create and return a new processor suitable for processing HTTP
         * requests and returning the corresponding responses.
         */
        private HttpProcessor newProcessor() {

            //        if (debug >= 2)
            //            log("newProcessor: Creating new processor");
            HttpProcessor processor = new HttpProcessor(HttpConnector.this, curProcessors++);
            if (processor instanceof Lifecycle) {
                try {
                    //HttpProcessor一出来就run了 堵在run的wait() 等socket
                    ((Lifecycle) processor).start();
                } catch (LifecycleException e) {
                    log("newProcessor", e);
                    return (null);
                }
            }
            created.addElement(processor);
            return processor;

        }

        private void createMin() {
            while (curProcessors < minProcessors) {
                //超过最大了  就算了 再多也不接待了
                if ((maxProcessors > 0) && (curProcessors >= maxProcessors))
                    break;
                //new 一个HttpProcessor
                HttpProcessor processor = newProcessor();
                //push 入 Stack processors
                recycle(processor);
            }
        }

        private void shutDownAll() {
            for (int i = created.size() - 1; i >= 0; i--) {
                HttpProcessor processor = (HttpProcessor) created.elementAt(i);
                if (processor instanceof Lifecycle) {
                    try {
                        ((Lifecycle) processor).stop();
                    } catch (LifecycleException e) {
                        log("HttpConnector.stop", e);
                    }
                }
            }
        }
    }
    public void recycle(HttpProcessor httpProcessor) {
        httpProcessorManager.recycle(httpProcessor);
    }



}
