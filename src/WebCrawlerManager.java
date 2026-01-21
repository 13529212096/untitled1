import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 多功能网页爬虫管理器
 * 支持多线程爬取、URL队列管理、内容过滤、数据导出等功能
 */
public class WebCrawlerManager {
    
    // 核心配置
    private final int maxThreads;
    private final int maxDepth;
    private final int timeoutMs;
    private final String userAgent;
    
    // 数据结构
    private final BlockingQueue<CrawlTask> urlQueue;
    private final Set<String> visitedUrls;
    private final Map<String, PageContent> crawledData;
    private final ExecutorService threadPool;
    
    // 统计信息
    private volatile int totalCrawled;
    private volatile int errorCount;
    private final long startTime;
    
    // 正则表达式模式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "<title[^>]*>([^<]+)</title>", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    public WebCrawlerManager(int maxThreads, int maxDepth, int timeoutMs) {
        this.maxThreads = maxThreads;
        this.maxDepth = maxDepth;
        this.timeoutMs = timeoutMs;
        this.userAgent = "WebCrawlerManager/1.0 (+https://example.com/bot)";
        
        this.urlQueue = new LinkedBlockingQueue<>();
        this.visitedUrls = Collections.synchronizedSet(new HashSet<>());
        this.crawledData = new ConcurrentHashMap<>();
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        
        this.totalCrawled = 0;
        this.errorCount = 0;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 爬虫任务内部类
     */
    private static class CrawlTask {
        final String url;
        final int depth;
        final String referrer;
        
        CrawlTask(String url, int depth, String referrer) {
            this.url = url;
            this.depth = depth;
            this.referrer = referrer;
        }
    }
    
    /**
     * 页面内容数据结构
     */
    public static class PageContent {
        private final String url;
        private final String title;
        private final String content;
        private final List<String> links;
        private final Map<String, String> metadata;
        private final LocalDateTime crawlTime;
        
        public PageContent(String url, String title, String content, 
                         List<String> links, Map<String, String> metadata) {
            this.url = url;
            this.title = title;
            this.content = content;
            this.links = links;
            this.metadata = metadata;
            this.crawlTime = LocalDateTime.now();
        }
        
        // Getters
        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public List<String> getLinks() { return links; }
        public Map<String, String> getMetadata() { return metadata; }
        public LocalDateTime getCrawlTime() { return crawlTime; }
    }
    
    /**
     * 开始爬取指定URL
     */
    public void startCrawling(String seedUrl) {
        if (!isValidUrl(seedUrl)) {
            throw new IllegalArgumentException("无效的起始URL: " + seedUrl);
        }
        
        urlQueue.offer(new CrawlTask(seedUrl, 0, null));
        
        // 启动工作线程
        for (int i = 0; i < maxThreads; i++) {
            threadPool.submit(new CrawlWorker());
        }
        
        System.out.println("开始爬取，种子URL: " + seedUrl);
        System.out.println("最大线程数: " + maxThreads + ", 最大深度: " + maxDepth);
    }
    
    /**
     * 爬虫工作线程
     */
    private class CrawlWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CrawlTask task = urlQueue.poll(5, TimeUnit.SECONDS);
                    if (task == null) {
                        continue;
                    }
                    
                    if (shouldCrawl(task)) {
                        crawlPage(task);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("爬取错误: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 判断是否应该爬取该URL
     */
    private boolean shouldCrawl(CrawlTask task) {
        return task.depth <= maxDepth && 
               isValidUrl(task.url) && 
               visitedUrls.add(task.url);
    }
    
    /**
     * 爬取单个页面
     */
    private void crawlPage(CrawlTask task) {
        try {
            System.out.println("正在爬取 [深度:" + task.depth + "]: " + task.url);
            
            String content = fetchPageContent(task.url);
            if (content != null) {
                PageContent pageContent = parseContent(task.url, content);
                crawledData.put(task.url, pageContent);
                totalCrawled++;
                
                // 提取并加入新的链接
                if (task.depth < maxDepth) {
                    extractAndQueueLinks(content, task.url, task.depth + 1);
                }
                
                System.out.println("成功爬取: " + task.url + 
                                 " (标题: " + pageContent.getTitle() + ")");
            }
            
        } catch (Exception e) {
            errorCount++;
            System.err.println("爬取失败 " + task.url + ": " + e.getMessage());
        }
    }
    
    /**
     * 获取网页内容
     */
    private String fetchPageContent(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setInstanceFollowRedirects(true);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP错误码: " + responseCode);
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
    
    /**
     * 解析页面内容
     */
    private PageContent parseContent(String url, String htmlContent) {
        String title = extractTitle(htmlContent);
        String textContent = extractTextContent(htmlContent);
        List<String> links = extractLinks(htmlContent, url);
        Map<String, String> metadata = extractMetadata(htmlContent);
        
        return new PageContent(url, title, textContent, links, metadata);
    }
    
    /**
     * 提取页面标题
     */
    private String extractTitle(String htmlContent) {
        Matcher matcher = TITLE_PATTERN.matcher(htmlContent);
        return matcher.find() ? matcher.group(1).trim() : "无标题";
    }
    
    /**
     * 提取文本内容（移除HTML标签）
     */
    private String extractTextContent(String htmlContent) {
        String textContent = htmlContent.replaceAll("<[^>]+>", " ");
        textContent = textContent.replaceAll("\\s+", " ");
        return textContent.trim();
    }
    
    /**
     * 提取并排队新链接
     */
    private void extractAndQueueLinks(String htmlContent, String baseUrl, int depth) {
        List<String> links = extractLinks(htmlContent, baseUrl);
        for (String link : links) {
            if (!visitedUrls.contains(link) && isValidUrl(link)) {
                urlQueue.offer(new CrawlTask(link, depth, baseUrl));
            }
        }
    }
    
    /**
     * 从HTML中提取链接
     */
    private List<String> extractLinks(String htmlContent, String baseUrl) {
        List<String> links = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(htmlContent);
        
        while (matcher.find()) {
            String url = matcher.group().trim();
            if (isValidUrl(url)) {
                links.add(url);
            }
        }
        
        return links;
    }
    
    /**
     * 提取元数据
     */
    private Map<String, String> extractMetadata(String htmlContent) {
        Map<String, String> metadata = new HashMap<>();
        
        // 提取meta标签信息
        Pattern metaPattern = Pattern.compile(
            "<meta[^>]*name=[\"']([^\"']+)[\"'][^>]*content=[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = metaPattern.matcher(htmlContent);
        while (matcher.find()) {
            metadata.put(matcher.group(1), matcher.group(2));
        }
        
        return metadata;
    }
    
    /**
     * 验证URL格式
     */
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * 停止爬虫
     */
    public void stopCrawling() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        printStatistics();
    }
    
    /**
     * 打印爬取统计信息
     */
    public void printStatistics() {
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("\n=== 爬取统计信息 ===");
        System.out.println("总爬取页面: " + totalCrawled);
        System.out.println("错误次数: " + errorCount);
        System.out.println("访问过的URL: " + visitedUrls.size());
        System.out.println("队列剩余: " + urlQueue.size());
        System.out.println("运行时间: " + (duration / 1000.0) + " 秒");
        System.out.println("平均速度: " + String.format("%.2f", totalCrawled / (duration / 1000.0)) + " 页/秒");
    }
    
    /**
     * 导出数据到文件
     */
    public void exportToFile(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, StandardCharsets.UTF_8))) {
            writer.println("# 网页爬虫数据导出");
            writer.println("# 导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println("# 总页面数: " + crawledData.size());
            writer.println();
            
            for (PageContent page : crawledData.values()) {
                writer.println("## URL: " + page.getUrl());
                writer.println("标题: " + page.getTitle());
                writer.println("爬取时间: " + page.getCrawlTime());
                writer.println("链接数量: " + page.getLinks().size());
                writer.println("内容预览: " + 
                              (page.getContent().length() > 200 ? 
                               page.getContent().substring(0, 200) + "..." : 
                               page.getContent()));
                writer.println("---");
            }
        }
        System.out.println("数据已导出到: " + filename);
    }
    
    /**
     * 获取爬取结果
     */
    public Map<String, PageContent> getCrawledData() {
        return new HashMap<>(crawledData);
    }
    
    /**
     * 搜索功能
     */
    public List<PageContent> searchContent(String keyword) {
        return crawledData.values().stream()
                .filter(page -> page.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                               page.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .sorted((a, b) -> b.getCrawlTime().compareTo(a.getCrawlTime()))
                .collect(ArrayList::new, (list, page) -> list.add(page), List::addAll);
    }
    
    /**
     * 示例使用方法
     */
    public static void main(String[] args) {
        WebCrawlerManager crawler = new WebCrawlerManager(3, 2, 5000);
        
        try {
            // 开始爬取
            crawler.startCrawling("https://example.com");
            
            // 运行一段时间
            Thread.sleep(30000);
            
            // 停止爬取
            crawler.stopCrawling();
            
            // 搜索内容
            List<PageContent> results = crawler.searchContent("example");
            System.out.println("搜索结果: " + results.size() + " 页");
            
            // 导出数据
            crawler.exportToFile("crawl_results.txt");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
