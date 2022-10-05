package util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class HttpUtil {
    public static final String LocalAddress = "https://mvnrepository.com";

    public static Response synGetHttp(String address) {
//        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        // 设置代理地址
//        SocketAddress sa = new InetSocketAddress("127.0.0.1", 8080);
//        //HTTP代理
//        builder.proxy(new Proxy(Proxy.Type.HTTP, sa));
//
//        OkHttpClient client = builder.build();
//        Request.Builder requestBuilder = new Request.Builder();
//
//        UserAgentUtil userAgentUtil = new UserAgentUtil();
//        String userAgent = userAgentUtil.getUserAgent();
//
//        requestBuilder.url(address).addHeader("User-Agent", userAgent);
//        try {
//            Response response = client.newCall(requestBuilder.build()).execute();
//            System.out.println("页面返回码："+ response.code());
//            if(response.code() == 200) {
//                System.out.println("页面获取成功");
//                return response;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //随机获取一个user-agent
        UserAgentUtil userAgentUtil = new UserAgentUtil();
        String userAgent = userAgentUtil.getUserAgent();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).addHeader("User-Agent", userAgent).build();
        //执行请求
        try {
            Response response = client.newCall(request).execute();
            System.out.println("页面返回码：" + response.code());
            if (response.code() == 200) {
                System.out.println("页面获取成功");
                return response;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
