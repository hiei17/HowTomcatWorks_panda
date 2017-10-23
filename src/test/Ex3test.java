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

    public static void main(String[] d){
        Ex3test e=new Ex3test();
        e.parseParameters();
    }
}
