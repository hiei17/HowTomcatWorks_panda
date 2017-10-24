package test;


import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.RequestUtil;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Created by Administrator on 2017/10/23.
 */
public class Ex3test {
    protected final HashMap parameters = new HashMap();;
    protected void parseParameters() {

        //为了防止下面加工过程出现异常
        HashMap results = parameters;


        results.put("a","3");//不改变parameters

     //   parameters = results;
    }
    private void parseRequest() {
      String  uri="http://www.brainysoftware.com/index.html?name=Tarzan";

        // Checking for an absolute URI (with the HTTP protocol)
        //可能是绝对路径 如 http://www.brainysoftware.com/index.html?name=Tarzan
        if (!uri.startsWith("/")) {

            int pos = uri.indexOf("://");
            // Parsing out protocol and host name
            if (pos != -1) {
                pos = uri.indexOf('/', pos + 3);
                if (pos == -1) {
                    uri = "";
                } else {
                    uri = uri.substring(pos);
                }
            }
        }
    }
    public static void main(String[] d){
        Ex3test e=new Ex3test();
       // e.parseParameters();
        e.parseRequest();
    }
}
