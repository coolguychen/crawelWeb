import okhttp3.Response;
import org.apache.commons.compress.harmony.pack200.CpBands;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.xslf.draw.geom.XSLFLineTo;
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
 * 一个测试爬取mvnrepository是否正确可行的demo，只爬取了popular categories中的前三页
 * 每个子类目的子链接也只爬了第一页
 * 以防访问量过大造成403
 * 加入备份的表格，如果出现中断，可以从中断处重新开始
 */
public class crawlDemo {

    // 地址
    private static final String URL = "https://mvnrepository.com/open-source?p=";
    //统计库的总数
    private static int sum = 0;
    // 用于存类目地址及其软件包数目
    private static HashMap<String, Integer> subLinks;
//    private static HashMap<String, Integer> failedAddress;

    //表格目录
    private static String fileName = "test.xls";

    private static HashMap<String[], String> hashMap;


    public static void main(String[] args) {
        excelUtil excelUtil = new excelUtil();
        //先创建一个总的表格
        excelUtil.createExcel(fileName);
        //demo:
        for (int i = 1; i <= 1; i++) {
            try {
                getFromMvn(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("爬取第" + i + "页时中断，请重试！");
                interruptHandle(i);
            }
        }

    }

    /**
     * 中断处理
     *
     * @param i
     */
    private static void interruptHandle(int i) {
        //重新爬取第i页
        try {
            getFromMvn(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *  demo: 只爬取第pageNum页的软件包
     *
     * @throws InterruptedException
     */
    /**
     * 爬取popular categories第i页
     *
     * @param pageNum
     * @throws InterruptedException
     */
    private static void getFromMvn(int pageNum) throws InterruptedException {
        System.out.println("爬取第" + pageNum + "页");
        //CountDownLatch可以使一个获多个线程等待其他线程各自执行完毕后再执行。
        final CountDownLatch latch = new CountDownLatch(1);//使用java并发库concurrent
        //每次爬取一页，重新初始化subLinks & hashMap
        subLinks = new HashMap<>();
        hashMap = new HashMap<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //线程休眠
                sleep();
                String address = URL + pageNum;
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
                            int cnt = Integer.parseInt(num2);
                            sum += cnt;
                            if(cnt >= 150) cnt = 150; //注意mvnrepository页面显示上限为15页
                            subLinks.put(url, cnt); //放入子链接（类目）和对应的软件包数目
                        }
                    }
                    response.close();
                } else {
                    System.out.println("页面" + pageNum + "响应失败！请重新爬取！");
                }
                latch.countDown();//让latch中的数值减一

            }
        }).start();

        //主线程
        latch.await();//阻塞当前线程直到latch中数值为零才执行
        System.out.println("共统计" + subLinks.size() + "个类目");
        System.out.println("库的总数为：" + sum);
        System.out.println("-------------");
        if ((pageNum == 1 && subLinks.size() == 9) || (pageNum != 1 && subLinks.size() == 10)) {
            System.out.println("接下来爬取子链接");
            getFromSubLinks();
            //对该页爬取的软件包进行备份
            String fileTmp = "page" + pageNum + ".xls";
            new excelUtil().backUpExcel(fileTmp, hashMap);
        } else {
            System.out.println("爬取不完全，请重试！");
            interruptHandle(pageNum);
        }

    }

    public static void getFromSubLinks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String address = null;
                int num = 0;
                //遍历hashmap中的元素
                for (Map.Entry<String, Integer> entry : subLinks.entrySet()) {
                    address = entry.getKey(); //地址
                    num = entry.getValue(); //软件包总数
                    System.out.println("对" + address + "进行爬取");
                    try {
                        crawlCategory(address, num);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //遍历完后计数器减1
                latch.countDown();
            }
        }).start();
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
        int pageNum = (int) Math.ceil((double) num / 10); //一页十个软件包, 向上取整
//         TODO: 17/10/2022 注意这里页数最多十五页，需要处理一下
        //对每个页面进行爬取
        for (int i = 1; i <= pageNum; i++) {
            crawlCategoryByPage(address, i);
        }
    }

    /**
     * 爬取address的第page页
     *
     * @param address
     * @param page
     * @throws InterruptedException
     */
    private static void crawlCategoryByPage(String address, int page) throws InterruptedException{
        System.out.println("爬取" + address + "的第" + page + "页");
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                sleep();
                String address1 = address + "?p=" + page;
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
                        hashMap.put(idPair, libraryName);
                        //加入总的表格中
                        new excelUtil().appendToExcel(fileName, libraryName, idPair);
                    }
                    response.close(); //关闭请求
                } else {
                    System.out.println("页面爬取失败！");
                    System.out.println(address + "的第" + page + "页爬取失败，记录下来");
//                    failedAddress.put(address, page);
                }
                latch.countDown();
            }
        }).start();
        latch.await();
    }

    private static void sleep(){
        try {
            int randomNum = new RandomUtil().getRandomNum();
            Thread.sleep(randomNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
