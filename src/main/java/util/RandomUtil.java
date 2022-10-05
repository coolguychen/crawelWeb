package util;

public class RandomUtil {
    private static int min = 30000; // 定义随机数的最小值
    private static int max = 120000; // 定义随机数的最大值

    public static int getRandomNum() {
        // 产生一个min~max的数
        int s = (int) min + (int) (Math.random() * (max - min));
//        System.out.println("休眠" + s + "毫秒");
        return s;
    }

    public static int getRandomNumTest() {
        int mini = 10000;
        int maxi = 20000;
        // 产生一个min~max的数
        int s = (int) mini + (int) (Math.random() * (maxi - mini));
        System.out.println("休眠" + s + "毫秒");
        return s;
    }
}
