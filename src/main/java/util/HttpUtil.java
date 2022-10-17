package util;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class HttpUtil {
    public static final String LocalAddress = "https://mvnrepository.com";

    public static Response getHttp(String address){
        //随机获取一个user-agent
        UserAgentUtil userAgentUtil = new UserAgentUtil();
        String userAgent = userAgentUtil.getUserAgent();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(180, TimeUnit.SECONDS) //设置连接超时时间为180秒（三分钟
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(address).addHeader("User-Agent", userAgent).build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                System.out.println("页面响应成功！");
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if(e instanceof SocketTimeoutException){//判断超时异常 如果超时
                    System.out.println("该页面超时未响应");
                }
                if(e instanceof ConnectException){//判断连接异常，我这里是报Failed to connect to 10.7.5.144
                    System.out.println("该页面连接异常");
                }
            }
        });

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
            if(e instanceof SocketTimeoutException){//判断超时异常
                System.out.println("该页面超时未响应");
            }
            if(e instanceof ConnectException){//判断连接异常，我这里是报Failed to connect to 10.7.5.144
                System.out.println("该页面连接异常");
            }
        }
        return null;
    }


    public static Response synGetHttp(String address) {
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
