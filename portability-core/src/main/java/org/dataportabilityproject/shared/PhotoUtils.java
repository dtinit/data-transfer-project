package org.dataportabilityproject.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PhotoUtils {
  public static InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }
}
