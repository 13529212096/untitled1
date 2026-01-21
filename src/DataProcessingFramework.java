import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 数据处理框架
 * 包含数据库操作、Excel处理、报表生成、配置管理等工作常用功能
 */
public class DataProcessingFramework {

    /**
     * 数据库连接池管理器
     */
    public static class DatabaseManager {
        private static final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
        
        public static class HikariDataSource {
            private final String url;
            private final String username;
            private final String password;
            private final Queue<Connection> connections = new ConcurrentLinkedQueue<>();
            private final int maxPoolSize;
            private volatile int currentPoolSize = 0;
            
            public HikariDataSource(String url, String username, String password, int maxPoolSize) {
                this.url = url;
                this.username = username;
                this.password = password;
                this.maxPoolSize = maxPoolSize;
            }
            
            public Connection getConnection() throws SQLException {
                Connection conn = connections.poll();
                if (conn != null && !conn.isClosed()) {
                    return conn;
                }
                
                if (currentPoolSize < maxPoolSize) {
                    synchronized (this) {
                        if (currentPoolSize < maxPoolSize) {
                            currentPoolSize++;
                            return DriverManager.getConnection(url, username, password);
                        }
                    }
                }
                
                // 等待可用连接
                conn = connections.poll();
                return conn != null ? conn : DriverManager.getConnection(url, username, password);
            }
            
            public void returnConnection(Connection conn) {
                if (conn != null) {
                    connections.offer(conn);
                }
            }
            
            public void close() {
                connections.forEach(conn -> {
                    try {
                        if (!conn.isClosed()) {
                            conn.close();
                        }
                    } catch (SQLException e) {
                        // 忽略关闭异常
                    }
                });
                connections.clear();
                currentPoolSize = 0;
            }
        }
        
        /**
         * 查询结果处理器
         */
        public static class QueryExecutor {
            private final HikariDataSource dataSource;
            
            public QueryExecutor(HikariDataSource dataSource) {
                this.dataSource = dataSource;
            }
            
            /**
             * 执行查询并返回结果列表
             */
            public <T> List<T> queryForList(String sql, RowMapper<T> mapper, Object... params) 
                    throws SQLException {
                List<T> results = new ArrayList<>();
                
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    // 设置参数
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            results.add(mapper.mapRow(rs));
                        }
                    }
                } finally {
                    // 连接会自动归还到池中
                }
                
                return results;
            }
            
            /**
             * 执行更新操作
             */
            public int executeUpdate(String sql, Object... params) throws SQLException {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                    
                    return stmt.executeUpdate();
                }
            }
            
            /**
             * 批量执行操作
             */
            public int[] executeBatch(String sql, List<Object[]> batchParams) throws SQLException {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    for (Object[] params : batchParams) {
                        for (int i = 0; i < params.length; i++) {
                            stmt.setObject(i + 1, params[i]);
                        }
                        stmt.addBatch();
                    }
                    
                    return stmt.executeBatch();
                }
            }
            
            /**
             * 执行事务
             */
            public <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException {
                Connection conn = dataSource.getConnection();
                boolean autoCommit = conn.getAutoCommit();
                
                try {
                    conn.setAutoCommit(false);
                    T result = callback.execute(conn);
                    conn.commit();
                    return result;
                } catch (Exception e) {
                    conn.rollback();
                    throw new SQLException("事务执行失败", e);
                } finally {
                    conn.setAutoCommit(autoCommit);
                    dataSource.returnConnection(conn);
                }
            }
        }
        
        @FunctionalInterface
        public interface RowMapper<T> {
            T mapRow(ResultSet rs) throws SQLException;
        }
        
        @FunctionalInterface
        public interface TransactionCallback<T> {
            T execute(Connection conn) throws Exception;
        }
    }
    
    /**
     * Excel数据处理工具
     */
    public static class ExcelProcessor {
        
        /**
         * 简化版Excel读取器
         */
        public static class ExcelReader {
            private final Path filePath;
            
            public ExcelReader(Path filePath) {
                this.filePath = filePath;
            }
            
            /**
             * 读取Excel为CSV格式数据
             */
            public List<List<String>> readAsCSV() throws IOException {
                List<List<String>> data = new ArrayList<>();
                
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 简化的CSV解析 (实际项目中应使用专业的CSV库)
                        List<String> row = Arrays.asList(line.split(","));
                        data.add(row);
                    }
                }
                
                System.out.printf("读取Excel文件: %s, 行数: %d%n", filePath.getFileName(), data.size());
                return data;
            }
            
            /**
             * 读取Excel并转换为对象列表
             */
            public <T> List<T> readAsObjects(Class<T> clazz, ExcelRowMapper<T> mapper) 
                    throws IOException, ReflectiveOperationException {
                List<T> objects = new ArrayList<>();
                List<List<String>> csvData = readAsCSV();
                
                if (csvData.isEmpty()) {
                    return objects;
                }
                
                // 跳过标题行
                for (int i = 1; i < csvData.size(); i++) {
                    List<String> row = csvData.get(i);
                    T obj = mapper.mapRow(row, i, clazz);
                    if (obj != null) {
                        objects.add(obj);
                    }
                }
                
                return objects;
            }
        }
        
        /**
         * Excel写入器
         */
        public static class ExcelWriter {
            private final Path outputPath;
            private final List<List<String>> data = new ArrayList<>();
            
            public ExcelWriter(Path outputPath) {
                this.outputPath = outputPath;
            }
            
            /**
             * 添加行数据
             */
            public ExcelWriter addRow(Object... values) {
                List<String> row = Arrays.stream(values)
                    .map(obj -> obj != null ? obj.toString() : "")
                    .collect(Collectors.toList());
                data.add(row);
                return this;
            }
            
            /**
             * 添加标题行
             */
            public ExcelWriter addHeaders(String... headers) {
                return addRow((Object[]) headers);
            }
            
            /**
             * 写入到文件
             */
            public void write() throws IOException {
                Files.createDirectories(outputPath.getParent());
                
                try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                    for (List<String> row : data) {
                        String line = String.join(",", row);
                        writer.write(line);
                        writer.newLine();
                    }
                }
                
                System.out.printf("Excel文件写入完成: %s, 行数: %d%n", outputPath.getFileName(), data.size());
            }
        }
        
        @FunctionalInterface
        public interface ExcelRowMapper<T> {
            T mapRow(List<String> row, int rowNumber, Class<T> clazz) throws ReflectiveOperationException;
        }
        
        /**
         * 通用对象映射器
         */
        public static class GenericRowMapper<T> implements ExcelRowMapper<T> {
            @Override
            public T mapRow(List<String> row, int rowNumber, Class<T> clazz) 
                    throws ReflectiveOperationException {
                T instance = clazz.getDeclaredConstructor().newInstance();
                Field[] fields = clazz.getDeclaredFields();
                
                for (int i = 0; i < Math.min(row.size(), fields.length); i++) {
                    Field field = fields[i];
                    field.setAccessible(true);
                    
                    String value = row.get(i);
                    if (value != null && !value.trim().isEmpty()) {
                        Object convertedValue = convertValue(value, field.getType());
                        field.set(instance, convertedValue);
                    }
                }
                
                return instance;
            }
            
            private Object convertValue(String value, Class<?> targetType) {
                if (targetType == String.class) {
                    return value;
                } else if (targetType == Integer.class || targetType == int.class) {
                    return Integer.parseInt(value.trim());
                } else if (targetType == Double.class || targetType == double.class) {
                    return Double.parseDouble(value.trim());
                } else if (targetType == BigDecimal.class) {
                    return new BigDecimal(value.trim());
                } else if (targetType == Boolean.class || targetType == boolean.class) {
                    return Boolean.parseBoolean(value.trim());
                }
                return value;
            }
        }
    }
    
    /**
     * 报表生成器
     */
    public static class ReportGenerator {
        
        /**
         * 数据报表
         */
        public static class DataReport {
            private final String title;
            private final List<String> headers;
            private final List<List<Object>> rows = new ArrayList<>();
            private final Map<String, Object> metadata = new HashMap<>();
            
            public DataReport(String title, List<String> headers) {
                this.title = title;
                this.headers = new ArrayList<>(headers);
            }
            
            public DataReport addRow(Object... values) {
                rows.add(Arrays.asList(values));
                return this;
            }
            
            public DataReport addMetadata(String key, Object value) {
                metadata.put(key, value);
                return this;
            }
            
            /**
             * 生成HTML报表
             */
            public String generateHTML() {
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n");
                html.append("<html><head><meta charset='UTF-8'>\n");
                html.append("<title>").append(title).append("</title>\n");
                html.append("<style>\n");
                html.append("table { border-collapse: collapse; width: 100%; }\n");
                html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
                html.append("th { background-color: #f2f2f2; font-weight: bold; }\n");
                html.append("tr:nth-child(even) { background-color: #f9f9f9; }\n");
                html.append(".metadata { margin: 20px 0; padding: 10px; background-color: #e7f3ff; }\n");
                html.append("</style></head><body>\n");
                
                html.append("<h1>").append(title).append("</h1>\n");
                
                // 元数据
                if (!metadata.isEmpty()) {
                    html.append("<div class='metadata'>\n");
                    html.append("<h3>报表信息</h3>\n");
                    metadata.forEach((key, value) -> 
                        html.append("<p><strong>").append(key).append(":</strong> ").append(value).append("</p>\n"));
                    html.append("</div>\n");
                }
                
                // 数据表格
                html.append("<table>\n");
                html.append("<tr>");
                headers.forEach(header -> html.append("<th>").append(header).append("</th>"));
                html.append("</tr>\n");
                
                for (List<Object> row : rows) {
                    html.append("<tr>");
                    for (Object cell : row) {
                        html.append("<td>").append(cell != null ? cell.toString() : "").append("</td>");
                    }
                    html.append("</tr>\n");
                }
                
                html.append("</table>\n");
                html.append("</body></html>");
                
                return html.toString();
            }
            
            /**
             * 保存为HTML文件
             */
            public void saveAsHTML(Path outputPath) throws IOException {
                String htmlContent = generateHTML();
                Files.write(outputPath, htmlContent.getBytes("UTF-8"));
                System.out.printf("HTML报表已保存: %s%n", outputPath);
            }
            
            /**
             * 生成统计摘要
             */
            public ReportSummary generateSummary() {
                Map<String, Object> statistics = new HashMap<>();
                
                statistics.put("总行数", rows.size());
                statistics.put("列数", headers.size());
                statistics.put("生成时间", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                // 数值列的统计
                for (int i = 0; i < headers.size(); i++) {
                    List<Object> columnData = new ArrayList<>();
                    for (List<Object> row : rows) {
                        if (i < row.size() && row.get(i) != null) {
                            columnData.add(row.get(i));
                        }
                    }
                    
                    if (!columnData.isEmpty() && columnData.get(0) instanceof Number) {
                        double sum = columnData.stream()
                            .filter(obj -> obj instanceof Number)
                            .mapToDouble(obj -> ((Number) obj).doubleValue())
                            .sum();
                        double avg = sum / columnData.size();
                        
                        statistics.put(headers.get(i) + "_总和", BigDecimal.valueOf(sum).setScale(2, RoundingMode.HALF_UP));
                        statistics.put(headers.get(i) + "_平均值", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                    }
                }
                
                return new ReportSummary(statistics);
            }
        }
        
        /**
         * 报表摘要
         */
        public static class ReportSummary {
            private final Map<String, Object> statistics;
            
            public ReportSummary(Map<String, Object> statistics) {
                this.statistics = new HashMap<>(statistics);
            }
            
            public Map<String, Object> getStatistics() {
                return new HashMap<>(statistics);
            }
            
            public void printSummary() {
                System.out.println("=== 报表统计摘要 ===");
                statistics.forEach((key, value) -> 
                    System.out.printf("%s: %s%n", key, value));
            }
        }
    }
    
    /**
     * 配置管理器
     */
    public static class ConfigurationManager {
        private final Map<String, String> properties = new ConcurrentHashMap<>();
        private final Path configFile;
        
        public ConfigurationManager(Path configFile) {
            this.configFile = configFile;
            loadConfiguration();
        }
        
        /**
         * 加载配置文件
         */
        private void loadConfiguration() {
            if (Files.exists(configFile)) {
                try {
                    Properties props = new Properties();
                    try (InputStream is = Files.newInputStream(configFile)) {
                        props.load(is);
                    }
                    
                    props.forEach((key, value) -> 
                        properties.put(key.toString(), value.toString()));
                    
                    System.out.printf("配置文件加载完成: %s, 配置项: %d个%n", 
                        configFile.getFileName(), properties.size());
                } catch (IOException e) {
                    System.err.printf("配置文件加载失败: %s%n", e.getMessage());
                }
            } else {
                // 创建默认配置
                setDefaults();
                saveConfiguration();
            }
        }
        
        /**
         * 设置默认配置
         */
        private void setDefaults() {
            properties.put("database.url", "jdbc:mysql://localhost:3306/testdb");
            properties.put("database.username", "root");
            properties.put("database.password", "password");
            properties.put("database.maxPoolSize", "10");
            properties.put("report.outputDir", "./reports");
            properties.put("cache.ttlSeconds", "3600");
        }
        
        /**
         * 保存配置文件
         */
        public void saveConfiguration() {
            try {
                Files.createDirectories(configFile.getParent());
                Properties props = new Properties();
                properties.forEach(props::setProperty);
                
                try (OutputStream os = Files.newOutputStream(configFile)) {
                    props.store(os, "Application Configuration");
                }
                
                System.out.printf("配置文件保存完成: %s%n", configFile.getFileName());
            } catch (IOException e) {
                System.err.printf("配置文件保存失败: %s%n", e.getMessage());
            }
        }
        
        /**
         * 获取配置值
         */
        public String get(String key, String defaultValue) {
            return properties.getOrDefault(key, defaultValue);
        }
        
        public int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(properties.getOrDefault(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        public boolean getBoolean(String key, boolean defaultValue) {
            return Boolean.parseBoolean(properties.getOrDefault(key, String.valueOf(defaultValue)));
        }
        
        /**
         * 设置配置值
         */
        public void set(String key, String value) {
            properties.put(key, value);
        }
        
        public void setInt(String key, int value) {
            properties.put(key, String.valueOf(value));
        }
        
        public void setBoolean(String key, boolean value) {
            properties.put(key, String.valueOf(value));
        }
        
        /**
         * 获取所有配置
         */
        public Map<String, String> getAllProperties() {
            return new HashMap<>(properties);
        }
    }
    
    /**
     * 业务规则引擎
     */
    public static class RuleEngine {
        private final List<BusinessRule> rules = new ArrayList<>();
        private final ScriptEngine scriptEngine;
        
        public RuleEngine() {
            ScriptEngineManager manager = new ScriptEngineManager();
            this.scriptEngine = manager.getEngineByName("javascript");
        }
        
        /**
         * 业务规则接口
         */
        public interface BusinessRule {
            String getName();
            boolean evaluate(Map<String, Object> context);
            String getDescription();
        }
        
        /**
         * 简单规则实现
         */
        public static class SimpleRule implements BusinessRule {
            private final String name;
            private final String description;
            private final Predicate<Map<String, Object>> predicate;
            
            public SimpleRule(String name, String description, Predicate<Map<String, Object>> predicate) {
                this.name = name;
                this.description = description;
                this.predicate = predicate;
            }
            
            @Override
            public String getName() { return name; }
            
            @Override
            public boolean evaluate(Map<String, Object> context) {
                return predicate.test(context);
            }
            
            @Override
            public String getDescription() { return description; }
        }
        
        /**
         * 脚本规则实现
         */
        public class ScriptRule implements BusinessRule {
            private final String name;
            private final String description;
            private final String script;
            
            public ScriptRule(String name, String description, String script) {
                this.name = name;
                this.description = description;
                this.script = script;
            }
            
            @Override
            public String getName() { return name; }
            
            @Override
            public boolean evaluate(Map<String, Object> context) {
                try {
                    // 将上下文变量注入脚本引擎
                    context.forEach(scriptEngine::put);
                    
                    Object result = scriptEngine.eval(script);
                    return Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    System.err.printf("规则执行失败: %s, 错误: %s%n", name, e.getMessage());
                    return false;
                }
            }
            
            @Override
            public String getDescription() { return description; }
        }
        
        /**
         * 添加规则
         */
        public RuleEngine addRule(BusinessRule rule) {
            rules.add(rule);
            System.out.printf("添加业务规则: %s%n", rule.getName());
            return this;
        }
        
        /**
         * 执行所有规则
         */
        public RuleExecutionResult executeRules(Map<String, Object> context) {
            List<String> passedRules = new ArrayList<>();
            List<String> failedRules = new ArrayList<>();
            
            for (BusinessRule rule : rules) {
                try {
                    boolean result = rule.evaluate(context);
                    if (result) {
                        passedRules.add(rule.getName());
                    } else {
                        failedRules.add(rule.getName());
                    }
                } catch (Exception e) {
                    failedRules.add(rule.getName() + " (异常: " + e.getMessage() + ")");
                }
            }
            
            return new RuleExecutionResult(passedRules, failedRules);
        }
        
        /**
         * 规则执行结果
         */
        public static class RuleExecutionResult {
            private final List<String> passedRules;
            private final List<String> failedRules;
            
            public RuleExecutionResult(List<String> passedRules, List<String> failedRules) {
                this.passedRules = new ArrayList<>(passedRules);
                this.failedRules = new ArrayList<>(failedRules);
            }
            
            public List<String> getPassedRules() { return new ArrayList<>(passedRules); }
            public List<String> getFailedRules() { return new ArrayList<>(failedRules); }
            
            public boolean allPassed() { return failedRules.isEmpty(); }
            
            public void printResult() {
                System.out.printf("规则执行结果: 通过 %d 个, 失败 %d 个%n", 
                    passedRules.size(), failedRules.size());
                
                if (!passedRules.isEmpty()) {
                    System.out.println("通过的规则:");
                    passedRules.forEach(rule -> System.out.println("  ✓ " + rule));
                }
                
                if (!failedRules.isEmpty()) {
                    System.out.println("失败的规则:");
                    failedRules.forEach(rule -> System.out.println("  ✗ " + rule));
                }
            }
        }
    }
    
    // 示例数据模型
    public static class Employee {
        private String name;
        private String department;
        private double salary;
        private int age;
        
        public Employee() {}
        
        public Employee(String name, String department, double salary, int age) {
            this.name = name;
            this.department = department;
            this.salary = salary;
            this.age = age;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        
        public double getSalary() { return salary; }
        public void setSalary(double salary) { this.salary = salary; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        @Override
        public String toString() {
            return String.format("Employee{name='%s', department='%s', salary=%.2f, age=%d}", 
                name, department, salary, age);
        }
    }
    
    // 演示方法
    public static void main(String[] args) {
        System.out.println("=== 数据处理框架演示 ===\n");
        
        try {
            // 1. 配置管理演示
            System.out.println("1. 配置管理演示:");
            ConfigurationManager config = new ConfigurationManager(Paths.get("config.properties"));
            
            System.out.printf("数据库URL: %s%n", config.get("database.url", "默认URL"));
            System.out.printf("连接池大小: %d%n", config.getInt("database.maxPoolSize", 5));
            
            config.set("app.version", "1.0.0");
            config.setBoolean("debug.enabled", true);
            System.out.println();
            
            // 2. 报表生成演示
            System.out.println("2. 报表生成演示:");
            ReportGenerator.DataReport report = new ReportGenerator.DataReport(
                "员工工资报表", 
                Arrays.asList("姓名", "部门", "工资", "年龄")
            );
            
            report.addMetadata("生成时间", LocalDateTime.now().toString())
                  .addMetadata("数据来源", "人事系统")
                  .addRow("张三", "开发部", 8000.00, 28)
                  .addRow("李四", "销售部", 6500.00, 25)
                  .addRow("王五", "开发部", 9500.00, 32)
                  .addRow("赵六", "市场部", 7200.00, 29);
            
            ReportGenerator.ReportSummary summary = report.generateSummary();
            summary.printSummary();
            
            // 保存HTML报表
            Path reportPath = Paths.get("reports/employee_report.html");
            Files.createDirectories(reportPath.getParent());
            report.saveAsHTML(reportPath);
            System.out.println();
            
            // 3. 业务规则引擎演示
            System.out.println("3. 业务规则引擎演示:");
            RuleEngine ruleEngine = new RuleEngine();
            
            // 添加简单规则
            ruleEngine.addRule(new RuleEngine.SimpleRule(
                "工资范围检查",
                "检查工资是否在合理范围内",
                context -> {
                    Double salary = (Double) context.get("salary");
                    return salary != null && salary >= 3000 && salary <= 50000;
                }
            ));
            
            // 添加脚本规则
            ruleEngine.addRule(ruleEngine.new ScriptRule(
                "年龄检查",
                "检查员工年龄是否符合要求",
                "age >= 18 && age <= 65"
            ));
            
            // 测试规则
            Map<String, Object> employeeContext = new HashMap<>();
            employeeContext.put("salary", 8000.0);
            employeeContext.put("age", 28);
            employeeContext.put("department", "开发部");
            
            RuleEngine.RuleExecutionResult result = ruleEngine.executeRules(employeeContext);
            result.printResult();
            System.out.println();
            
            // 4. Excel处理演示（模拟）
            System.out.println("4. Excel处理演示:");
            ExcelProcessor.ExcelWriter writer = new ExcelProcessor.ExcelWriter(
                Paths.get("reports/employee_data.csv")
            );
            
            writer.addHeaders("姓名", "部门", "工资", "年龄")
                  .addRow("张三", "开发部", 8000.00, 28)
                  .addRow("李四", "销售部", 6500.00, 25)
                  .addRow("王五", "开发部", 9500.00, 32)
                  .write();
            
            // 读取Excel数据
            if (Files.exists(Paths.get("reports/employee_data.csv"))) {
                ExcelProcessor.ExcelReader reader = new ExcelProcessor.ExcelReader(
                    Paths.get("reports/employee_data.csv")
                );
                
                List<Employee> employees = reader.readAsObjects(Employee.class, 
                    new ExcelProcessor.GenericRowMapper<>());
                
                System.out.printf("读取到 %d 条员工记录:%n", employees.size());
                employees.forEach(emp -> System.out.println("  " + emp));
            }
            
        } catch (Exception e) {
            System.err.printf("演示过程中发生错误: %s%n", e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
}
