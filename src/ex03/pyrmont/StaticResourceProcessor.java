package ex03.pyrmont;

import ex03.pyrmont.connector.http.HttpRequest;
import ex03.pyrmont.connector.http.HttpResponse;
import java.io.IOException;

public class StaticResourceProcessor {
//��urlָ�����ļ� ֱ�Ӹ������ û������ļ���404
  public void process(HttpRequest request, HttpResponse response) {
    try {
      response.sendStaticResource();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

}
