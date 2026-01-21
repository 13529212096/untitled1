import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 设计模式集合展示
 * 包含单例模式、观察者模式、策略模式、装饰器模式、建造者模式等
 */
public class DesignPatternShowcase {
    
    /**
     * 1. 单例模式 - 双重检查锁定
     */
    public static class DatabaseConnection {
        private static volatile DatabaseConnection instance;
        private final String connectionString;
        private final long creationTime;
        
        private DatabaseConnection() {
            // 模拟连接初始化
            this.connectionString = "jdbc:mysql://localhost:3306/testdb";
            this.creationTime = System.currentTimeMillis();
            System.out.println("数据库连接已创建: " + connectionString);
            
            // 模拟初始化耗时
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        public static DatabaseConnection getInstance() {
            if (instance == null) {
                synchronized (DatabaseConnection.class) {
                    if (instance == null) {
                        instance = new DatabaseConnection();
                    }
                }
            }
            return instance;
        }
        
        public void executeQuery(String sql) {
            System.out.printf("[连接ID: %d] 执行SQL: %s%n", creationTime, sql);
        }
        
        public String getConnectionInfo() {
            return String.format("连接字符串: %s, 创建时间: %d", connectionString, creationTime);
        }
    }
    
    /**
     * 2. 观察者模式 - 事件通知系统
     */
    public interface EventListener {
        void onEvent(String eventType, Object data);
    }
    
    public static class EventPublisher {
        private final Map<String, List<EventListener>> listeners = new HashMap<>();
        
        public void subscribe(String eventType, EventListener listener) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
            System.out.printf("订阅者已订阅事件类型: %s%n", eventType);
        }
        
        public void unsubscribe(String eventType, EventListener listener) {
            List<EventListener> eventListeners = listeners.get(eventType);
            if (eventListeners != null) {
                eventListeners.remove(listener);
                System.out.printf("订阅者已取消订阅事件类型: %s%n", eventType);
            }
        }
        
        public void publishEvent(String eventType, Object data) {
            System.out.printf("发布事件: %s, 数据: %s%n", eventType, data);
            List<EventListener> eventListeners = listeners.get(eventType);
            if (eventListeners != null) {
                for (EventListener listener : eventListeners) {
                    try {
                        listener.onEvent(eventType, data);
                    } catch (Exception e) {
                        System.err.printf("处理事件时出错: %s%n", e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * 3. 策略模式 - 支付处理系统
     */
    public interface PaymentStrategy {
        boolean processPayment(double amount);
        String getPaymentMethod();
    }
    
    public static class CreditCardPayment implements PaymentStrategy {
        private final String cardNumber;
        private final String holderName;
        
        public CreditCardPayment(String cardNumber, String holderName) {
            this.cardNumber = cardNumber;
            this.holderName = holderName;
        }
        
        @Override
        public boolean processPayment(double amount) {
            System.out.printf("使用信用卡支付 %.2f 元%n", amount);
            System.out.printf("卡号: %s, 持卡人: %s%n", 
                cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length()-4), 
                holderName);
            
            // 模拟支付处理
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            System.out.println("信用卡支付成功！");
            return true;
        }
        
        @Override
        public String getPaymentMethod() {
            return "信用卡";
        }
    }
    
    public static class PayPalPayment implements PaymentStrategy {
        private final String email;
        
        public PayPalPayment(String email) {
            this.email = email;
        }
        
        @Override
        public boolean processPayment(double amount) {
            System.out.printf("使用PayPal支付 %.2f 元%n", amount);
            System.out.printf("PayPal账户: %s%n", email);
            
            // 模拟支付处理
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            System.out.println("PayPal支付成功！");
            return true;
        }
        
        @Override
        public String getPaymentMethod() {
            return "PayPal";
        }
    }
    
    public static class PaymentProcessor {
        private PaymentStrategy strategy;
        
        public void setPaymentStrategy(PaymentStrategy strategy) {
            this.strategy = strategy;
            System.out.printf("支付方式已设置为: %s%n", strategy.getPaymentMethod());
        }
        
        public boolean processPayment(double amount) {
            if (strategy == null) {
                System.err.println("未设置支付策略！");
                return false;
            }
            return strategy.processPayment(amount);
        }
    }
    
    /**
     * 4. 装饰器模式 - 咖啡订购系统
     */
    public interface Coffee {
        double getCost();
        String getDescription();
    }
    
    public static class SimpleCoffee implements Coffee {
        @Override
        public double getCost() {
            return 10.0;
        }
        
        @Override
        public String getDescription() {
            return "简单咖啡";
        }
    }
    
    public abstract static class CoffeeDecorator implements Coffee {
        protected final Coffee coffee;
        
        public CoffeeDecorator(Coffee coffee) {
            this.coffee = coffee;
        }
        
        @Override
        public double getCost() {
            return coffee.getCost();
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription();
        }
    }
    
    public static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public double getCost() {
            return super.getCost() + 2.0;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " + 牛奶";
        }
    }
    
    public static class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public double getCost() {
            return super.getCost() + 1.0;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " + 糖";
        }
    }
    
    public static class VanillaDecorator extends CoffeeDecorator {
        public VanillaDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public double getCost() {
            return super.getCost() + 3.0;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " + 香草";
        }
    }
    
    /**
     * 5. 建造者模式 - 复杂对象构建
     */
    public static class Computer {
        private final String cpu;
        private final String gpu;
        private final int ram;
        private final int storage;
        private final String motherboard;
        private final String powerSupply;
        private final List<String> accessories;
        
        private Computer(Builder builder) {
            this.cpu = builder.cpu;
            this.gpu = builder.gpu;
            this.ram = builder.ram;
            this.storage = builder.storage;
            this.motherboard = builder.motherboard;
            this.powerSupply = builder.powerSupply;
            this.accessories = new ArrayList<>(builder.accessories);
        }
        
        public static class Builder {
            private String cpu;
            private String gpu = "集成显卡";
            private int ram = 8;
            private int storage = 256;
            private String motherboard;
            private String powerSupply = "标准电源";
            private List<String> accessories = new ArrayList<>();
            
            public Builder(String cpu, String motherboard) {
                this.cpu = cpu;
                this.motherboard = motherboard;
            }
            
            public Builder withGPU(String gpu) {
                this.gpu = gpu;
                return this;
            }
            
            public Builder withRAM(int ram) {
                this.ram = ram;
                return this;
            }
            
            public Builder withStorage(int storage) {
                this.storage = storage;
                return this;
            }
            
            public Builder withPowerSupply(String powerSupply) {
                this.powerSupply = powerSupply;
                return this;
            }
            
            public Builder addAccessory(String accessory) {
                this.accessories.add(accessory);
                return this;
            }
            
            public Computer build() {
                return new Computer(this);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("电脑配置:\n");
            sb.append(String.format("  CPU: %s%n", cpu));
            sb.append(String.format("  GPU: %s%n", gpu));
            sb.append(String.format("  内存: %dGB%n", ram));
            sb.append(String.format("  存储: %dGB%n", storage));
            sb.append(String.format("  主板: %s%n", motherboard));
            sb.append(String.format("  电源: %s%n", powerSupply));
            if (!accessories.isEmpty()) {
                sb.append("  配件: ");
                sb.append(String.join(", ", accessories));
                sb.append("\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * 6. 工厂模式 - 消息处理器工厂
     */
    public interface MessageProcessor {
        void processMessage(String message);
        String getProcessorType();
    }
    
    public static class EmailProcessor implements MessageProcessor {
        @Override
        public void processMessage(String message) {
            System.out.printf("邮件处理器处理消息: %s%n", message);
            System.out.println("  - 验证邮件格式");
            System.out.println("  - 发送到邮件服务器");
            System.out.println("  - 记录发送日志");
        }
        
        @Override
        public String getProcessorType() {
            return "邮件处理器";
        }
    }
    
    public static class SMSProcessor implements MessageProcessor {
        @Override
        public void processMessage(String message) {
            System.out.printf("短信处理器处理消息: %s%n", message);
            System.out.println("  - 检查短信长度");
            System.out.println("  - 发送到短信网关");
            System.out.println("  - 更新发送状态");
        }
        
        @Override
        public String getProcessorType() {
            return "短信处理器";
        }
    }
    
    public static class PushNotificationProcessor implements MessageProcessor {
        @Override
        public void processMessage(String message) {
            System.out.printf("推送通知处理器处理消息: %s%n", message);
            System.out.println("  - 构建推送载荷");
            System.out.println("  - 发送到推送服务");
            System.out.println("  - 跟踪送达率");
        }
        
        @Override
        public String getProcessorType() {
            return "推送通知处理器";
        }
    }
    
    public static class MessageProcessorFactory {
        private static final Map<String, Function<Void, MessageProcessor>> processors = new HashMap<>();
        
        static {
            processors.put("EMAIL", v -> new EmailProcessor());
            processors.put("SMS", v -> new SMSProcessor());
            processors.put("PUSH", v -> new PushNotificationProcessor());
        }
        
        public static MessageProcessor createProcessor(String type) {
            Function<Void, MessageProcessor> creator = processors.get(type.toUpperCase());
            if (creator == null) {
                throw new IllegalArgumentException("未知的消息处理器类型: " + type);
            }
            
            MessageProcessor processor = creator.apply(null);
            System.out.printf("创建了 %s%n", processor.getProcessorType());
            return processor;
        }
        
        public static Set<String> getSupportedTypes() {
            return processors.keySet();
        }
    }
    
    // 演示方法
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 设计模式展示 ===\n");
        
        // 1. 单例模式演示
        System.out.println("1. 单例模式演示:");
        DatabaseConnection db1 = DatabaseConnection.getInstance();
        DatabaseConnection db2 = DatabaseConnection.getInstance();
        
        System.out.printf("db1 == db2: %b%n", db1 == db2);
        System.out.println(db1.getConnectionInfo());
        db1.executeQuery("SELECT * FROM users");
        System.out.println();
        
        // 2. 观察者模式演示
        System.out.println("2. 观察者模式演示:");
        EventPublisher publisher = new EventPublisher();
        
        EventListener userListener = (eventType, data) -> 
            System.out.printf("用户监听器收到事件 %s: %s%n", eventType, data);
        
        EventListener adminListener = (eventType, data) -> 
            System.out.printf("管理员监听器收到事件 %s: %s%n", eventType, data);
        
        publisher.subscribe("USER_LOGIN", userListener);
        publisher.subscribe("USER_LOGIN", adminListener);
        publisher.subscribe("SYSTEM_ERROR", adminListener);
        
        publisher.publishEvent("USER_LOGIN", "用户张三登录成功");
        publisher.publishEvent("SYSTEM_ERROR", "数据库连接超时");
        System.out.println();
        
        // 3. 策略模式演示
        System.out.println("3. 策略模式演示:");
        PaymentProcessor paymentProcessor = new PaymentProcessor();
        
        paymentProcessor.setPaymentStrategy(new CreditCardPayment("1234567890123456", "张三"));
        paymentProcessor.processPayment(99.99);
        
        Thread.sleep(500);
        
        paymentProcessor.setPaymentStrategy(new PayPalPayment("zhangsan@example.com"));
        paymentProcessor.processPayment(149.99);
        System.out.println();
        
        // 4. 装饰器模式演示
        System.out.println("4. 装饰器模式演示:");
        Coffee simpleCoffee = new SimpleCoffee();
        System.out.printf("%s - %.2f元%n", simpleCoffee.getDescription(), simpleCoffee.getCost());
        
        Coffee milkCoffee = new MilkDecorator(simpleCoffee);
        System.out.printf("%s - %.2f元%n", milkCoffee.getDescription(), milkCoffee.getCost());
        
        Coffee fancyCoffee = new VanillaDecorator(new SugarDecorator(new MilkDecorator(simpleCoffee)));
        System.out.printf("%s - %.2f元%n", fancyCoffee.getDescription(), fancyCoffee.getCost());
        System.out.println();
        
        // 5. 建造者模式演示
        System.out.println("5. 建造者模式演示:");
        Computer gamingPC = new Computer.Builder("Intel i9-12900K", "ASUS ROG Maximus")
            .withGPU("NVIDIA RTX 4080")
            .withRAM(32)
            .withStorage(1024)
            .withPowerSupply("850W 80+ Gold")
            .addAccessory("机械键盘")
            .addAccessory("游戏鼠标")
            .addAccessory("高刷显示器")
            .build();
        
        System.out.println(gamingPC);
        
        Computer officePC = new Computer.Builder("Intel i5-12400", "MSI B660M")
            .withRAM(16)
            .withStorage(512)
            .addAccessory("无线键鼠套装")
            .build();
        
        System.out.println(officePC);
        
        // 6. 工厂模式演示
        System.out.println("6. 工厂模式演示:");
        System.out.printf("支持的消息类型: %s%n", MessageProcessorFactory.getSupportedTypes());
        
        String[] messageTypes = {"EMAIL", "SMS", "PUSH"};
        String message = "您的验证码是：123456";
        
        for (String type : messageTypes) {
            try {
                MessageProcessor processor = MessageProcessorFactory.createProcessor(type);
                processor.processMessage(message);
                System.out.println();
            } catch (Exception e) {
                System.err.printf("处理消息类型 %s 时出错: %s%n", type, e.getMessage());
            }
        }
        
        System.out.println("=== 演示完成 ===");
    }
}
