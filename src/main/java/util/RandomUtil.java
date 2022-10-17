package util;

public class RandomUtil {
    private static int min = 20000; // 定义随机数的最小值
    private static int max = 60000; // 定义随机数的最大值

    public static int getRandomNum() {
        // 产生一个min~max的数
        int s = (int) min + (int) (Math.random() * (max - min));
        double second = (double) s/1000;
        System.out.println("休眠" + second + "秒");
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
