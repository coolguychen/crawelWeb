import okhttp3.Response;
import org.apache.poi.xssf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpUtil;
import util.RandomUtil;
import util.excelUtil;

import java.io.File;
import java.io.FileOutputStream;
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
    private static HashMap<String, Integer> subLinks = new HashMap<>();
    //<groupId, artifactId,libraryName>三元组 最后以表格形式输出
    //key为<groupId, artifactId>，因为libraryName可能同名
    private static HashMap<String[], String> record = new HashMap<>();
    //输出的excel表格的路径
    private static String fileName = "output.xls";

    public static void main(String[] args) {
        excelUtil excelUtil = new excelUtil();
        //先创建一个表格
        excelUtil.createExcel(fileName);
        try {
            //从第一页到第15页
            getFromMvn(1, 15);
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
        //CountDownLatch可以使一个获多个线程等待其他线程各自执行完毕后再执行。
        //15个页面需要爬取
        int pageNum = end - begin + 1;
        final CountDownLatch latch = new CountDownLatch(pageNum);//使用java并发库concurrent
        //启用15个线程
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
        //一共149个类目
        if (subLinks.size() == 149) {
            System.out.println("接下来爬取子链接");
            getFromSubLinks();
        } else {
            System.out.println("爬取不完全，请重试！");
        }
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
//        System.out.println("----数据输出到Excel----");
//        saveAsExcel("output.xls");
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


    /**
     * 在运行完爬取后，把所有数据输入到excel中
     * 选择舍弃该方法，改用每爬到一条数据就追加到表格后——>excelUtil
     *
     * @param src
     */
    private static void saveAsExcel(String src) {
//        System.out.println("数据输出到Excel...");
        // 定义一个新的工作簿
        XSSFWorkbook wb = new XSSFWorkbook();
        // 创建一个Sheet页
        XSSFSheet sheet = wb.createSheet("First sheet");
        //设置行高
        sheet.setDefaultRowHeight((short) (2 * 256));
        //设置列宽
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
        XSSFFont font = wb.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 16);
        //获得表格第一行
        XSSFRow row = sheet.createRow(0);
        //根据需要给第一行每一列设置标题
        XSSFCell cell = row.createCell(0);
        cell.setCellValue("libraryName");
        cell = row.createCell(1);
        cell.setCellValue("groupId");
        cell = row.createCell(2);
        cell.setCellValue("artifactId");
        XSSFRow rows;
        XSSFCell cells;
        int i = 0;
        //遍历record hashmap 每一条数据放在excel中一行的单元格里
        for (Map.Entry<String[], String> entry : record.entrySet()) {
            // 在这个sheet页里创建一行
            rows = sheet.createRow(i + 1);
            String[] idInfo = entry.getKey();
            String libraryName = entry.getValue();
            String groupId = idInfo[0];
            String artifactId = idInfo[1];
            // 该行创建一个单元格,在该单元格里设置值
            cells = rows.createCell(0);
            cells.setCellValue(libraryName);
            cells = rows.createCell(1);
            cells.setCellValue(groupId);
            cells = rows.createCell(2);
            cells.setCellValue(artifactId);
        }
        //保存至文件目录
        try {
            File file = new File(src);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            wb.write(fileOutputStream);
            wb.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}






