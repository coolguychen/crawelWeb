import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpUtil;
import util.RandomUtil;
import util.excelUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static util.HttpUtil.LocalAddress;

/**
 * 一个测试爬取mvnrepository是否正确可行的demo，只爬取了popular categories中的第一页的10个子类目
 * 每个子类目的子链接也只爬了第一页
 * 以防访问量过大造成403
 */
public class crawlDemo {

    // 地址
    private static final String URL = "https://mvnrepository.com/open-source?p=";
    //统计库的总数
    private static int sum = 0;
    // 用于存类目地址及其软件包数目
    private static HashMap<String, Integer> subLinks = new HashMap<>();
    //表格目录
    private static String fileName = "test.xls";


    public static void main(String[] args) {
        excelUtil excelUtil = new excelUtil();
        //先创建一个表格
        excelUtil.createExcel(fileName);
        //第一页的爬取 防止由于访问量过大出现java.net.SocketTimeoutException
        try {
            getFromMvn();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 只爬top categories的第一页，并将子链接存在subLinks里面
     *
     * @throws InterruptedException
     */
    private static void getFromMvn() throws InterruptedException {
        //CountDownLatch可以使一个获多个线程等待其他线程各自执行完毕后再执行。
        int pageNum = 1;
        final CountDownLatch latch = new CountDownLatch(pageNum);//使用java并发库concurrent
        //测试第一页
        for (int i = 1; i <= 1; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        //每10秒爬取一页
                        Thread.sleep(new RandomUtil().getRandomNumTest());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String address = URL + finalI;

                    //爬取网站
                    Response response = HttpUtil.synGetHttp(address);
                    //响应码为200才可以继续
                    if (response != null) {
                        //  System.out.println("子线程：爬取页面ing");
                        //得到html代码
                        String html = null;
                        try {
                            html = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //格式化解析html
                        Document doc = Jsoup.parse(html);
                        //获取tag为h4的，即需要的category标题
                        Elements elements = doc.getElementsByTag("h4");
                        for (Element element : elements) {
//                        System.out.println(element);
                            Elements urls = element.select("a[href]");
                            //由于只有一个href
                            String url = urls.get(0).attr("href");
                            //url排除掉android
                            if (!url.equals("/open-source/android")) {
                                url = LocalAddress + url;//网站首页加上该链接
//                            System.out.println(url);
                                Elements num = element.getElementsByTag("b");
                                String num1 = num.get(0).text();
                                //获取到数量 形如(a)
//                            System.out.println(num1);
                                //去掉左右两个括号
                                String num2 = num1.substring(1, num1.length() - 1);
//                            System.out.println(num2);
                                int cnt = Integer.parseInt(num2);
                                sum += cnt;
                                subLinks.put(url, cnt); //放入子链接（类目）和对应的软件包数目
                            }
                        }
                    } else {
                        System.out.println("页面响应失败！");
                    }
                    latch.countDown();//让latch中的数值减一
                }
            }).start();
        }
        //主线程
        latch.await();//阻塞当前线程直到latch中数值为零才执行
        System.out.println("共统计" + subLinks.size() + "个类目");
        System.out.println("库的总数为：" + sum);
        System.out.println("-------------");
        if (subLinks.size() == 9) {
            System.out.println("接下来爬取子链接");
            getFromSubLinks();
        } else {
            System.out.println("页面爬取不完整，请重试！");
        }

    }

    public static void getFromSubLinks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(subLinks.size());
        //遍历hashmap中的元素
        for (Map.Entry<String, Integer> entry : subLinks.entrySet()) {
            String address = entry.getKey(); //地址
            int num = entry.getValue(); //软件包总数
            System.out.println("对" + address + "进行爬取");
            try {
                crawlCategory(address, num);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();

        }
        latch.await();
        System.out.println("-------------");
        System.out.println("子链接爬取完毕，完成。");
    }

    /**
     * 在子页面进行爬取groupId 和 artifactId
     *
     * @param address
     * @param num
     */
    private static void crawlCategory(String address, int num) throws InterruptedException {
        //页面数：
//        int pageNum = (int) Math.ceil(num / 10); //一页十个软件包, 向上取整
        int pageNum = 1; //假设只爬每个categories的第一页
        CountDownLatch latch = new CountDownLatch(pageNum);
        for (int i = 1; i < 1 + pageNum; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int randomNum = new RandomUtil().getRandomNumTest();
                        Thread.sleep(randomNum);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String address1 = address + "?p=" + finalI;
                    //爬取网站
                    Response response = HttpUtil.synGetHttp(address1);
                    //响应码为200才可以继续
                    if (response != null) {
                        System.out.println("正在爬取网站:" + address1);
                        System.out.println("-------------");
                        //得到html代码
                        String html = null;
                        try {
                            html = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //格式化解析html
                        Document doc = Jsoup.parse(html);
                        //获取groupId和artifactId,在class：im-substitute
                        //首先获取软件包的title
                        Elements title = doc.getElementsByClass("im-title");
                        //遍历所有软件包
                        for (int j = 0; j < title.size(); j++) {
                            Elements href = title.get(j).select("a[href]");
                            //获取软件包名
                            String libraryName = href.get(0).text();
                            System.out.println("软件包为:" + libraryName);
                            //获取groupId和artifactId
                            Elements subTitle = doc.getElementsByClass("im-subtitle");
                            //对应第j条元素信息
                            Elements idInfo = subTitle.get(j).select("a[href]");
                            String groupId = idInfo.get(0).text();
                            String artifactId = idInfo.get(1).text();
                            System.out.println("对应的groupId为：" + groupId + "，对应的artifactId为：" + artifactId);
                            String[] idPair = {groupId, artifactId};
                            //加入表格中
                            new excelUtil().appendToExcel(fileName, libraryName, idPair);
                        }
                    } else {
                        System.out.println("页面爬取失败！");
                    }
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
    }


}