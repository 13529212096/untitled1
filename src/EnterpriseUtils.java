import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * 企业级工具类集合
 * 包含工作中常用的各种复杂工具方法
 */
public class EnterpriseUtils {
    
    /**
     * 文件处理工具类
     */
    public static class FileProcessor {
        private static final int BUFFER_SIZE = 8192;
        
        /**
         * 批量文件处理器 - 支持多线程处理大量文件
         */
        public static class BatchFileProcessor {
            private final ExecutorService executor;
            private final int threadCount;
            
            public BatchFileProcessor(int threadCount) {
                this.threadCount = threadCount;
                this.executor = Executors.newFixedThreadPool(threadCount);
            }
            
            /**
             * 批量处理文件
             */
            public CompletableFuture<Map<String, ProcessResult>> processFiles(
                    List<Path> files, 
                    FileOperation operation) {
                
                return CompletableFuture.supplyAsync(() -> {
                    Map<String, ProcessResult> results = new ConcurrentHashMap<>();
                    CountDownLatch latch = new CountDownLatch(files.size());
                    
                    for (Path file : files) {
                        executor.submit(() -> {
                            try {
                                long startTime = System.currentTimeMillis();
                                String result = operation.process(file);
                                long endTime = System.currentTimeMillis();
                                
                                results.put(file.toString(), new ProcessResult(
                                    true, result, endTime - startTime, null));
                            } catch (Exception e) {
                                results.put(file.toString(), new ProcessResult(
                                    false, null, 0, e.getMessage()));
                            } finally {
                                latch.countDown();
                            }
                        });
                    }
                    
                    try {
                        latch.await(30, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    return results;
                });
            }
            
            public void shutdown() {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        /**
         * 文件操作接口
         */
        @FunctionalInterface
        public interface FileOperation {
            String process(Path file) throws Exception;
        }
        
        /**
         * 处理结果封装
         */
        public static class ProcessResult {
            private final boolean success;
            private final String result;
            private final long processingTime;
            private final String error;
            
            public ProcessResult(boolean success, String result, long processingTime, String error) {
                this.success = success;
                this.result = result;
                this.processingTime = processingTime;
                this.error = error;
            }
            
            // Getters
            public boolean isSuccess() { return success; }
            public String getResult() { return result; }
            public long getProcessingTime() { return processingTime; }
            public String getError() { return error; }
            
            @Override
            public String toString() {
                return String.format("ProcessResult{success=%b, time=%dms, result='%s', error='%s'}", 
                    success, processingTime, result, error);
            }
        }
        
        /**
         * 计算文件MD5值
         */
        public static String calculateMD5(Path filePath) throws Exception {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream fis = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(fis, md)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                while (dis.read(buffer) != -1) {
                    // 读取文件内容，自动计算摘要
                }
                
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
        }
        
        /**
         * 智能文件备份 - 支持增量备份
         */
        public static void backupFile(Path source, Path backupDir) throws IOException {
            if (!Files.exists(source)) {
                throw new IllegalArgumentException("源文件不存在: " + source);
            }
            
            Files.createDirectories(backupDir);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = source.getFileName().toString();
            String backupFileName = fileName + "_" + timestamp + ".bak";
            Path backupPath = backupDir.resolve(backupFileName);
            
            Files.copy(source, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("文件备份完成: %s -> %s%n", source, backupPath);
        }
    }
    
    /**
     * 数据验证工具类
     */
    public static class DataValidator {
        private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
        
        // 常用正则表达式
        private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
        private static final String ID_CARD_REGEX = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";
        private static final String IP_REGEX = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        
        /**
         * 获取编译后的正则表达式模式
         */
        private static Pattern getPattern(String regex) {
            return PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
        }
        
        /**
         * 批量数据验证器
         */
        public static class BatchValidator {
            private final List<ValidationRule> rules = new ArrayList<>();
            
            public BatchValidator addRule(String fieldName, String value, ValidationRule rule) {
                rule.setFieldName(fieldName);
                rule.setValue(value);
                rules.add(rule);
                return this;
            }
            
            public ValidationResult validate() {
                List<String> errors = new ArrayList<>();
                Map<String, Object> validatedData = new HashMap<>();
                
                for (ValidationRule rule : rules) {
                    try {
                        if (rule.validate()) {
                            validatedData.put(rule.getFieldName(), rule.getProcessedValue());
                        } else {
                            errors.add(rule.getFieldName() + ": " + rule.getErrorMessage());
                        }
                    } catch (Exception e) {
                        errors.add(rule.getFieldName() + ": 验证过程中发生异常 - " + e.getMessage());
                    }
                }
                
                return new ValidationResult(errors.isEmpty(), errors, validatedData);
            }
        }
        
        /**
         * 验证规则抽象类
         */
        public static abstract class ValidationRule {
            protected String fieldName;
            protected String value;
            protected String errorMessage;
            
            public abstract boolean validate();
            public Object getProcessedValue() { return value; }
            
            // Getters and setters
            public String getFieldName() { return fieldName; }
            public void setFieldName(String fieldName) { this.fieldName = fieldName; }
            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }
            public String getErrorMessage() { return errorMessage; }
        }
        
        /**
         * 邮箱验证规则
         */
        public static class EmailValidationRule extends ValidationRule {
            @Override
            public boolean validate() {
                if (value == null || value.trim().isEmpty()) {
                    errorMessage = "邮箱不能为空";
                    return false;
                }
                if (!getPattern(EMAIL_REGEX).matcher(value).matches()) {
                    errorMessage = "邮箱格式不正确";
                    return false;
                }
                return true;
            }
        }
        
        /**
         * 手机号验证规则
         */
        public static class PhoneValidationRule extends ValidationRule {
            @Override
            public boolean validate() {
                if (value == null || value.trim().isEmpty()) {
                    errorMessage = "手机号不能为空";
                    return false;
                }
                if (!getPattern(PHONE_REGEX).matcher(value).matches()) {
                    errorMessage = "手机号格式不正确";
                    return false;
                }
                return true;
            }
        }
        
        /**
         * 验证结果封装
         */
        public static class ValidationResult {
            private final boolean valid;
            private final List<String> errors;
            private final Map<String, Object> validatedData;
            
            public ValidationResult(boolean valid, List<String> errors, Map<String, Object> validatedData) {
                this.valid = valid;
                this.errors = new ArrayList<>(errors);
                this.validatedData = new HashMap<>(validatedData);
            }
            
            public boolean isValid() { return valid; }
            public List<String> getErrors() { return new ArrayList<>(errors); }
            public Map<String, Object> getValidatedData() { return new HashMap<>(validatedData); }
            
            @Override
            public String toString() {
                return String.format("ValidationResult{valid=%b, errors=%d, data=%d}", 
                    valid, errors.size(), validatedData.size());
            }
        }
    }
    
    /**
     * 缓存管理工具类
     */
    public static class CacheManager {
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private final ScheduledExecutorService cleanupExecutor = 
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "CacheCleanup");
                t.setDaemon(true);
                return t;
            });
        
        public CacheManager() {
            // 每分钟清理一次过期缓存
            cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
        }
        
        /**
         * 缓存条目
         */
        private static class CacheEntry {
            private final Object value;
            private final long expirationTime;
            private final long createTime;
            private volatile long lastAccessTime;
            private volatile int accessCount;
            
            public CacheEntry(Object value, long ttlSeconds) {
                this.value = value;
                this.createTime = System.currentTimeMillis();
                this.expirationTime = ttlSeconds > 0 ? 
                    createTime + (ttlSeconds * 1000) : Long.MAX_VALUE;
                this.lastAccessTime = createTime;
                this.accessCount = 1;
            }
            
            public boolean isExpired() {
                return System.currentTimeMillis() > expirationTime;
            }
            
            public Object getValue() {
                lastAccessTime = System.currentTimeMillis();
                accessCount++;
                return value;
            }
            
            public CacheStats getStats() {
                return new CacheStats(createTime, lastAccessTime, accessCount, isExpired());
            }
        }
        
        /**
         * 缓存统计信息
         */
        public static class CacheStats {
            private final long createTime;
            private final long lastAccessTime;
            private final int accessCount;
            private final boolean expired;
            
            public CacheStats(long createTime, long lastAccessTime, int accessCount, boolean expired) {
                this.createTime = createTime;
                this.lastAccessTime = lastAccessTime;
                this.accessCount = accessCount;
                this.expired = expired;
            }
            
            // Getters
            public long getCreateTime() { return createTime; }
            public long getLastAccessTime() { return lastAccessTime; }
            public int getAccessCount() { return accessCount; }
            public boolean isExpired() { return expired; }
        }
        
        /**
         * 存储缓存
         */
        public void put(String key, Object value, long ttlSeconds) {
            cache.put(key, new CacheEntry(value, ttlSeconds));
        }
        
        /**
         * 获取缓存
         */
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return (T) entry.getValue();
        }
        
        /**
         * 获取或计算缓存值
         */
        public <T> T getOrCompute(String key, Class<T> type, long ttlSeconds, 
                                Supplier<T> supplier) {
            T cached = get(key, type);
            if (cached != null) {
                return cached;
            }
            
            T computed = supplier.get();
            if (computed != null) {
                put(key, computed, ttlSeconds);
            }
            return computed;
        }
        
        /**
         * 清理过期缓存
         */
        private void cleanupExpired() {
            int removedCount = 0;
            Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                System.out.printf("清理了 %d 个过期缓存项，当前缓存大小: %d%n", 
                    removedCount, cache.size());
            }
        }
        
        /**
         * 获取缓存统计
         */
        public Map<String, CacheStats> getCacheStats() {
            return cache.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getStats()
                ));
        }
        
        public void shutdown() {
            cleanupExecutor.shutdown();
        }
    }
    
    /**
     * 加密解密工具类
     */
    public static class CryptoUtils {
        private static final String AES_ALGORITHM = "AES";
        private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
        
        /**
         * 生成AES密钥
         */
        public static String generateAESKey() throws Exception {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        }
        
        /**
         * AES加密
         */
        public static String encrypt(String plainText, String key) throws Exception {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        }
        
        /**
         * AES解密
         */
        public static String decrypt(String encryptedText, String key) throws Exception {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, "UTF-8");
        }
    }
    
    /**
     * HTTP客户端工具类
     */
    public static class HttpClientUtils {
        private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        
        /**
         * HTTP请求结果封装
         */
        public static class HttpResult {
            private final int statusCode;
            private final String body;
            private final Map<String, List<String>> headers;
            private final long responseTime;
            
            public HttpResult(int statusCode, String body, Map<String, List<String>> headers, long responseTime) {
                this.statusCode = statusCode;
                this.body = body;
                this.headers = headers;
                this.responseTime = responseTime;
            }
            
            public int getStatusCode() { return statusCode; }
            public String getBody() { return body; }
            public Map<String, List<String>> getHeaders() { return headers; }
            public long getResponseTime() { return responseTime; }
            
            public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
            
            @Override
            public String toString() {
                return String.format("HttpResult{status=%d, time=%dms, bodyLength=%d}", 
                    statusCode, responseTime, body != null ? body.length() : 0);
            }
        }
        
        /**
         * 发送GET请求
         */
        public static CompletableFuture<HttpResult> getAsync(String url, Map<String, String> headers) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET();
                    
                    if (headers != null) {
                        headers.forEach(requestBuilder::header);
                    }
                    
                    HttpRequest request = requestBuilder.build();
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    long endTime = System.currentTimeMillis();
                    
                    return new HttpResult(
                        response.statusCode(),
                        response.body(),
                        response.headers().map(),
                        endTime - startTime
                    );
                } catch (Exception e) {
                    throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
                }
            });
        }
        
        /**
         * 发送POST请求
         */
        public static CompletableFuture<HttpResult> postAsync(String url, String body, 
                                                            Map<String, String> headers) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(body));
                    
                    if (headers != null) {
                        headers.forEach(requestBuilder::header);
                    }
                    
                    HttpRequest request = requestBuilder.build();
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    long endTime = System.currentTimeMillis();
                    
                    return new HttpResult(
                        response.statusCode(),
                        response.body(),
                        response.headers().map(),
                        endTime - startTime
                    );
                } catch (Exception e) {
                    throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
                }
            });
        }
    }
    
    // 演示方法
    public static void main(String[] args) {
        System.out.println("=== 企业级工具类演示 ===\n");
        
        try {
            // 1. 文件处理演示
            System.out.println("1. 文件批量处理演示:");
            FileProcessor.BatchFileProcessor processor = new FileProcessor.BatchFileProcessor(4);
            
            List<Path> testFiles = Arrays.asList(
                Paths.get("src/Main.java"),
                Paths.get("src/QuickSortDemo.java"),
                Paths.get("src/MazeDemo.java")
            );
            
            CompletableFuture<Map<String, FileProcessor.ProcessResult>> future = 
                processor.processFiles(testFiles, file -> {
                    if (Files.exists(file)) {
                        return "文件大小: " + Files.size(file) + " 字节";
                    }
                    return "文件不存在";
                });
            
            Map<String, FileProcessor.ProcessResult> results = future.get(10, TimeUnit.SECONDS);
            results.forEach((file, result) -> 
                System.out.printf("  %s: %s%n", Paths.get(file).getFileName(), result));
            
            processor.shutdown();
            System.out.println();
            
            // 2. 数据验证演示
            System.out.println("2. 数据验证演示:");
            DataValidator.BatchValidator validator = new DataValidator.BatchValidator()
                .addRule("email", "test@example.com", new DataValidator.EmailValidationRule())
                .addRule("phone", "13800138000", new DataValidator.PhoneValidationRule())
                .addRule("email2", "invalid-email", new DataValidator.EmailValidationRule());
            
            DataValidator.ValidationResult validationResult = validator.validate();
            System.out.printf("验证结果: %s%n", validationResult);
            if (!validationResult.isValid()) {
                System.out.println("验证错误:");
                validationResult.getErrors().forEach(error -> System.out.println("  - " + error));
            }
            System.out.println();
            
            // 3. 缓存管理演示
            System.out.println("3. 缓存管理演示:");
            CacheManager cacheManager = new CacheManager();
            
            // 存储缓存
            cacheManager.put("user:1001", "张三", 5);
            cacheManager.put("config:timeout", 30000, 10);
            
            // 获取缓存
            String userName = cacheManager.get("user:1001", String.class);
            Integer timeout = cacheManager.get("config:timeout", Integer.class);
            
            System.out.printf("缓存获取 - 用户名: %s, 超时配置: %d%n", userName, timeout);
            
            // 获取或计算缓存
            String computed = cacheManager.getOrCompute("computed:data", String.class, 60, 
                () -> "这是计算得到的数据: " + System.currentTimeMillis());
            System.out.printf("计算缓存: %s%n", computed);
            
            // 获取缓存统计
            Map<String, CacheManager.CacheStats> stats = cacheManager.getCacheStats();
            System.out.println("缓存统计:");
            stats.forEach((key, stat) -> 
                System.out.printf("  %s: 访问%d次, 创建时间%d%n", 
                    key, stat.getAccessCount(), stat.getCreateTime()));
            
            cacheManager.shutdown();
            System.out.println();
            
            // 4. 加密工具演示
            System.out.println("4. 加密工具演示:");
            String aesKey = CryptoUtils.generateAESKey();
            String plainText = "这是需要加密的敏感数据";
            
            String encrypted = CryptoUtils.encrypt(plainText, aesKey);
            String decrypted = CryptoUtils.decrypt(encrypted, aesKey);
            
            System.out.printf("原文: %s%n", plainText);
            System.out.printf("加密后: %s%n", encrypted);
            System.out.printf("解密后: %s%n", decrypted);
            System.out.println();
            
            // 5. HTTP客户端演示
            System.out.println("5. HTTP客户端演示:");
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "EnterpriseUtils/1.0");
            
            try {
                CompletableFuture<HttpClientUtils.HttpResult> httpFuture = 
                    HttpClientUtils.getAsync("https://httpbin.org/get", headers);
                
                HttpClientUtils.HttpResult httpResult = httpFuture.get(10, TimeUnit.SECONDS);
                System.out.printf("HTTP请求结果: %s%n", httpResult);
                if (httpResult.isSuccess()) {
                    System.out.printf("响应内容长度: %d 字符%n", httpResult.getBody().length());
                }
            } catch (Exception e) {
                System.out.printf("HTTP请求失败: %s%n", e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.printf("演示过程中发生错误: %s%n", e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
}
