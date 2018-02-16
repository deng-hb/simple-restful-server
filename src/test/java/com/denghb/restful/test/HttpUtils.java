package com.denghb.restful.test;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * 可以用socket连接来实现
 */
public class HttpUtils {


    public static String get(String url) {
        return request(url, "GET", null);
    }
    public static String delete(String url) {
        return request(url, "DELETE", null);
    }

    public static String post(String url, Map<String, String> param) {
        String body = "";
        int i = 0;
        for (String key : param.keySet()) {
            if (i != 0) {
                body += "&";
            }
            i = 1;
            try {
                body += key + "=" + URLEncoder.encode(param.get(key), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return request(url, "POST", body);
    }

    public static String post(String url, String body) {
        return request(url, "POST", body);
    }
    public static String put(String url, String body) {
        return request(url, "PUT", body);
    }

    /**
     * @param url
     * @param body
     * @return
     */
    private static String request(String url, String method, String body) {
        String response = null;
        HttpURLConnection connection = null;
        try {
            connection = buildConnection(url, method);

            if(null != body) {
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
                bw.write(body);
                bw.flush();
                out.close();
                bw.close();//使用完关闭
            }
            // 返回信息

            InputStream in = connection.getInputStream();
            if (null == in) {
                in = connection.getErrorStream();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String str = null;
            StringBuffer buffer = new StringBuffer();
            while ((str = br.readLine()) != null) {//BufferedReader特有功能，一次读取一行数据
                buffer.append(str);
            }
            // System.out.println(buffer);
            in.close();
            br.close();

            response = buffer.toString();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();//使用完关闭TCP连接，释放资源
        }
        return response;
    }


    private static HttpURLConnection buildConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);// 3秒
        connection.setReadTimeout(60000);// 1分钟
        connection.setUseCaches(false);
        connection.setDoInput(true);//设置这个连接是否可以写入数据
        connection.setDoOutput(true);//设置这个连接是否可以输出数据


        if (null == method) {
            method = "GET";
        }
        connection.setRequestMethod(method);//设置请求的方式
        // 请求头
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "http://denghb.com");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.setRequestProperty("Content-type", "application/json");

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(getTrustAllSSLSocketFactory());
        }
        return connection;
    }

    private static SSLSocketFactory getTrustAllSSLSocketFactory() {
        // 信任所有证书
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, null);
            return sslContext.getSocketFactory();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

}