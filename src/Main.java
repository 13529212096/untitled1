import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("欢迎来到猜数字小游戏！");
        System.out.println("我已经想好了一个1到100之间的数字，你能猜到它吗？");

        Random random = new Random();
        int answer = random.nextInt(100) + 1;
        int guess;
        int attempts = 0;

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("请输入你的猜测（1-100）：");
            if (!scanner.hasNextInt()) {
                System.out.println("请输入一个有效的数字！");
                scanner.next(); // 清除无效输入
                continue;
            }
            guess = scanner.nextInt();
            attempts++;

            if (guess < 1 || guess > 100) {
                System.out.println("数字范围是1到100，请重新输入！");
            } else if (guess < answer) {
                System.out.println("太小了，再试试！");
            } else if (guess > answer) {
                System.out.println("太大了，再试试！");
            } else {
                System.out.println("恭喜你，猜对了！答案就是 " + answer + "。");
                System.out.println("你一共猜了 " + attempts + " 次。");
                break;
            }
        }

        System.out.println("游戏结束，感谢游玩！");
    }
}