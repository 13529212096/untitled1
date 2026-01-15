import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {
    // 判断一个数是否为质数
    public static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        int sqrt = (int) Math.sqrt(n);
        for (int i = 3; i <= sqrt; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    // 猴子排序（Bogosort）
    public static void bogosort(int[] arr) {
        java.util.Random rand = new java.util.Random();
        while (!isSorted(arr)) {
            // 随机打乱数组
            for (int i = arr.length - 1; i > 0; i--) {
                int j = rand.nextInt(i + 1);
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
    }

    // 判断数组是否有序
    public static boolean isSorted(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            if (arr[i - 1] > arr[i]) return false;
        }
        return true;
    }

    // 多线程查找质数
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int start = 1_000_000;
        int end = 1_010_000;
        int threadCount = 8;

        System.out.println("多线程并行查找区间 [" + start + ", " + end + "] 内的所有质数...");
        long begin = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<List<Integer>>> futures = new ArrayList<>();

        int range = (end - start + 1) / threadCount;
        for (int i = 0; i < threadCount; i++) {
            int subStart = start + i * range;
            int subEnd = (i == threadCount - 1) ? end : subStart + range - 1;
            futures.add(executor.submit(() -> {
                List<Integer> primes = new ArrayList<>();
                for (int n = subStart; n <= subEnd; n++) {
                    if (isPrime(n)) primes.add(n);
                }
                return primes;
            }));
        }

        List<Integer> allPrimes = new ArrayList<>();
        for (Future<List<Integer>> future : futures) {
            allPrimes.addAll(future.get());
        }
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        System.out.println("查找完成！总质数数量：" + allPrimes.size());
        System.out.println("部分结果：" + allPrimes.stream().limit(10).collect(Collectors.toList()) + " ...");
        System.out.println("总耗时：" + (endTime - begin) + " 毫秒");
    }
}