import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

/**
 * 复杂的线程池管理器
 * 包含自定义线程池、任务调度、性能监控等功能
 */
public class ThreadPoolManager {
    
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final Map<String, TaskMetrics> taskMetrics = new ConcurrentHashMap<>();
    
    // 任务性能指标
    static class TaskMetrics {
        volatile long totalExecutions = 0;
        volatile long totalTime = 0;
        volatile long maxTime = 0;
        volatile long minTime = Long.MAX_VALUE;
        volatile double avgTime = 0;
        
        void updateMetrics(long executionTime) {
            totalExecutions++;
            totalTime += executionTime;
            maxTime = Math.max(maxTime, executionTime);
            minTime = Math.min(minTime, executionTime);
            avgTime = (double) totalTime / totalExecutions;
        }
        
        @Override
        public String toString() {
            return String.format("TaskMetrics{executions=%d, avg=%.2fms, max=%dms, min=%dms}", 
                totalExecutions, avgTime / 1000000.0, maxTime / 1000000, minTime / 1000000);
        }
    }
    
    // 自定义线程工厂
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-thread-" + threadNumber.getAndIncrement());
            if (t.isDaemon()) t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    // 带监控的任务包装器
    static class MonitoredTask implements Runnable {
        private final Runnable task;
        private final String taskName;
        private final TaskMetrics metrics;
        
        MonitoredTask(Runnable task, String taskName, TaskMetrics metrics) {
            this.task = task;
            this.taskName = taskName;
            this.metrics = metrics;
        }
        
        @Override
        public void run() {
            long startTime = System.nanoTime();
            try {
                System.out.printf("[%s] 开始执行任务: %s%n", 
                    Thread.currentThread().getName(), taskName);
                task.run();
            } catch (Exception e) {
                System.err.printf("任务 %s 执行失败: %s%n", taskName, e.getMessage());
                e.printStackTrace();
            } finally {
                long endTime = System.nanoTime();
                long executionTime = endTime - startTime;
                metrics.updateMetrics(executionTime);
                System.out.printf("[%s] 任务 %s 执行完成，耗时: %.2f ms%n", 
                    Thread.currentThread().getName(), taskName, executionTime / 1000000.0);
            }
        }
    }
    
    public ThreadPoolManager(int corePoolSize, int maximumPoolSize) {
        // 创建自定义线程池
        this.executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new CustomThreadFactory("TaskPool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建定时任务调度器
        this.scheduler = Executors.newScheduledThreadPool(2, 
            new CustomThreadFactory("SchedulerPool"));
            
        // 启动监控线程
        startMonitoringThread();
    }
    
    // 提交普通任务
    public Future<?> submitTask(String taskName, Runnable task) {
        TaskMetrics metrics = taskMetrics.computeIfAbsent(taskName, k -> new TaskMetrics());
        MonitoredTask monitoredTask = new MonitoredTask(task, taskName, metrics);
        
        int taskId = taskCounter.incrementAndGet();
        System.out.printf("提交任务 %s (ID: %d) 到线程池%n", taskName, taskId);
        
        return executor.submit(monitoredTask);
    }
    
    // 提交定时任务
    public ScheduledFuture<?> scheduleTask(String taskName, Runnable task, 
                                         long delay, long period, TimeUnit unit) {
        TaskMetrics metrics = taskMetrics.computeIfAbsent(taskName, k -> new TaskMetrics());
        MonitoredTask monitoredTask = new MonitoredTask(task, taskName, metrics);
        
        System.out.printf("调度定时任务 %s，延迟: %d %s，周期: %d %s%n", 
            taskName, delay, unit.name(), period, unit.name());
            
        return scheduler.scheduleAtFixedRate(monitoredTask, delay, period, unit);
    }
    
    // 启动监控线程
    private void startMonitoringThread() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                lock.lock();
                System.out.println("\n=== 线程池状态报告 ===");
                System.out.printf("核心线程数: %d, 最大线程数: %d, 当前线程数: %d%n",
                    executor.getCorePoolSize(), executor.getMaximumPoolSize(), executor.getPoolSize());
                System.out.printf("活跃线程数: %d, 队列大小: %d, 已完成任务数: %d%n",
                    executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount());
                
                System.out.println("\n=== 任务性能指标 ===");
                taskMetrics.forEach((taskName, metrics) -> 
                    System.out.printf("%s: %s%n", taskName, metrics));
                System.out.println("========================\n");
            } finally {
                lock.unlock();
            }
        }, 5, 10, TimeUnit.SECONDS);
    }
    
    // 优雅关闭
    public void shutdown() {
        System.out.println("开始关闭线程池管理器...");
        
        executor.shutdown();
        scheduler.shutdown();
        
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                System.out.println("强制关闭线程池");
            }
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                System.out.println("强制关闭调度器");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("线程池管理器已关闭");
    }
    
    // 演示方法
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolManager manager = new ThreadPoolManager(3, 5);
        
        // 提交一些CPU密集型任务
        for (int i = 1; i <= 8; i++) {
            final int taskNum = i;
            manager.submitTask("CPU任务-" + i, () -> {
                // 模拟CPU密集型计算
                long sum = 0;
                for (int j = 0; j < 10000000; j++) {
                    sum += Math.sqrt(j);
                }
                System.out.printf("CPU任务-%d 计算结果: %d%n", taskNum, (long)sum);
            });
        }
        
        // 提交一些IO密集型任务
        for (int i = 1; i <= 5; i++) {
            final int taskNum = i;
            manager.submitTask("IO任务-" + i, () -> {
                try {
                    // 模拟IO操作
                    Thread.sleep(2000 + new Random().nextInt(3000));
                    System.out.printf("IO任务-%d 完成%n", taskNum);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 调度定时任务
        manager.scheduleTask("定时清理任务", () -> {
            System.out.println("执行定时清理操作...");
            // 模拟清理工作
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("清理操作完成");
        }, 2, 15, TimeUnit.SECONDS);
        
        // 运行30秒后关闭
        Thread.sleep(30000);
        manager.shutdown();
    }
}
