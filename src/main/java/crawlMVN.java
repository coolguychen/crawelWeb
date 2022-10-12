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

public class crawlMVN {

    // 地址
    private static final String URL = "https://mvnrepository.com/open-source?p=";
    //统计库的总数
    private static int sum = 0;
    // 用于存类目地址及其软件包数目
    private static HashMap<String, Integer> subLinks;
    //<groupId, artifactId,libraryName>三元组 最后以表格形式输出
    //key为<groupId, artifactId>，因为libraryName可能同名
    private static HashMap<String[], String> record;
    //输出的excel表格的路径
    private static String fileName = "output.xls";

    public static void main(String[] args) {
        excelUtil excelUtil = new excelUtil();
        //先创建一个总的结果表格，最后所有数据在output.xls里
        excelUtil.createExcel(fileName);

        for (int i = 0; i < 3; i++) {
            int begin = i + 5 * i;
            int end = 5 + 5 * i;
            try {
                //每次爬取五页
                //1-5
                //6-10
                //11-15
                getFromMvn(begin, end);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("爬取第" + begin + "至" + end + "页时中断，请重试！");
                interruptHandle(begin, end);
            }
        }
    }

    /**
     * 重新爬取begin -> end页的软件包
     *
     * @param begin 起始页
     * @param end   终止页
     */
    private static void interruptHandle(int begin, int end) {
        try {
            getFromMvn(begin, end);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 爬取mvnrepository的popular categoris
     *
     * @param begin 起始页
     * @param end   终止页
     * @throws InterruptedException
     */
    private static void getFromMvn(int begin, int end) throws InterruptedException {
        //每次爬取时初始化subLinks 和 record
        subLinks = new HashMap<>();
        record = new HashMap<>();
        int pageNum = end - begin + 1;
        final CountDownLatch latch = new CountDownLatch(pageNum);//使用java并发库concurrent
        //启用end-begin+1个线程
        for (int i = begin; i <= end; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        //随机休眠时间30-120s
                        Thread.sleep(new RandomUtil().getRandomNum());
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
                        System.out.println("页面" + finalI + "响应失败！请重试！");
                    }
                    latch.countDown();//让latch中的数值减一
                }
            }).start();
        }
        //主线程
        latch.await();//阻塞当前线程直到latch中数值为零才执行
        System.out.println("页面" + begin + "至" + end + "共统计" + subLinks.size() + "个类目");
        System.out.println("库的总数为：" + sum);
        System.out.println("-------------");
        System.out.println("接下来爬取子链接");
        getFromSubLinks();
        //爬取完子链接后
        //对begin -> end页爬取的软件包进行备份
        String fileTmp =  begin + "To" + end + ".xls";
        new excelUtil().backUpExcel(fileTmp, record);
    }

    /**
     * 爬取category子链接
     *
     * @throws InterruptedException
     */
    private static void getFromSubLinks() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
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
        System.out.println("子链接爬取完毕，爬取程序完成");
    }

    /**
     * 在子页面（category的网址）爬取libraryName, groupId, artifactId
     *
     * @param address 类目对应的网址
     * @param num     该类目下的软件包总数（因为不确定到底有多少页，通过num/10判断）
     * @throws InterruptedException
     */
    private static void crawlCategory(String address, int num) throws InterruptedException {
        //页面数：
        int pageNum = (int) Math.ceil(num / 10); //一页十个软件包, 向上取整
//        System.out.println("页面数为：" + pageNum);
        CountDownLatch latch = new CountDownLatch(pageNum);
        for (int i = 1; i < 1 + pageNum; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //随机休眠时间爬取一页
                        Thread.sleep(new RandomUtil().getRandomNum());
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
                            record.put(idPair, libraryName); // <libraryName, groupId, artifactId> 加入进哈希表
                            //加入表格中
                            // TODO: 12/10/2022 断点续传 
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






