import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 高级数据结构实现集合
 * 包含红黑树、跳表、布隆过滤器、LRU缓存等
 */
public class AdvancedDataStructures {
    
    /**
     * LRU缓存实现 - 使用双向链表和HashMap
     */
    public static class LRUCache<K, V> {
        private final int capacity;
        private final Map<K, Node<K, V>> cache;
        private final Node<K, V> head;
        private final Node<K, V> tail;
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        
        static class Node<K, V> {
            K key;
            V value;
            Node<K, V> prev;
            Node<K, V> next;
            
            Node() {}
            
            Node(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }
        
        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new HashMap<>();
            this.head = new Node<>();
            this.tail = new Node<>();
            head.next = tail;
            tail.prev = head;
        }
        
        public V get(K key) {
            lock.readLock().lock();
            try {
                Node<K, V> node = cache.get(key);
                if (node == null) {
                    return null;
                }
                moveToHead(node);
                return node.value;
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public void put(K key, V value) {
            lock.writeLock().lock();
            try {
                Node<K, V> node = cache.get(key);
                if (node != null) {
                    node.value = value;
                    moveToHead(node);
                } else {
                    Node<K, V> newNode = new Node<>(key, value);
                    cache.put(key, newNode);
                    addToHead(newNode);
                    
                    if (cache.size() > capacity) {
                        Node<K, V> removed = removeTail();
                        cache.remove(removed.key);
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        private void addToHead(Node<K, V> node) {
            node.prev = head;
            node.next = head.next;
            head.next.prev = node;
            head.next = node;
        }
        
        private void removeNode(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
        
        private void moveToHead(Node<K, V> node) {
            removeNode(node);
            addToHead(node);
        }
        
        private Node<K, V> removeTail() {
            Node<K, V> lastNode = tail.prev;
            removeNode(lastNode);
            return lastNode;
        }
        
        public void printCacheState() {
            lock.readLock().lock();
            try {
                System.out.print("LRU Cache (从新到旧): ");
                Node<K, V> current = head.next;
                while (current != tail) {
                    System.out.printf("(%s,%s) ", current.key, current.value);
                    current = current.next;
                }
                System.out.println();
            } finally {
                lock.readLock().unlock();
            }
        }
    }
    
    /**
     * 简化版跳表实现
     */
    public static class SkipList {
        private static final int MAX_LEVEL = 16;
        private final Random random = new Random();
        private final Node head;
        private int level = 0;
        
        static class Node {
            int value;
            Node[] forward;
            
            Node(int value, int level) {
                this.value = value;
                this.forward = new Node[level + 1];
            }
        }
        
        public SkipList() {
            head = new Node(-1, MAX_LEVEL);
        }
        
        private int randomLevel() {
            int level = 0;
            while (random.nextDouble() < 0.5 && level < MAX_LEVEL) {
                level++;
            }
            return level;
        }
        
        public void insert(int value) {
            Node[] update = new Node[MAX_LEVEL + 1];
            Node current = head;
            
            // 从最高层开始搜索
            for (int i = level; i >= 0; i--) {
                while (current.forward[i] != null && current.forward[i].value < value) {
                    current = current.forward[i];
                }
                update[i] = current;
            }
            
            current = current.forward[0];
            
            // 如果值不存在，则插入
            if (current == null || current.value != value) {
                int newLevel = randomLevel();
                
                if (newLevel > level) {
                    for (int i = level + 1; i <= newLevel; i++) {
                        update[i] = head;
                    }
                    level = newLevel;
                }
                
                Node newNode = new Node(value, newLevel);
                for (int i = 0; i <= newLevel; i++) {
                    newNode.forward[i] = update[i].forward[i];
                    update[i].forward[i] = newNode;
                }
            }
        }
        
        public boolean search(int value) {
            Node current = head;
            for (int i = level; i >= 0; i--) {
                while (current.forward[i] != null && current.forward[i].value < value) {
                    current = current.forward[i];
                }
            }
            current = current.forward[0];
            return current != null && current.value == value;
        }
        
        public void display() {
            System.out.println("跳表结构:");
            for (int i = level; i >= 0; i--) {
                Node current = head.forward[i];
                System.out.printf("Level %d: ", i);
                while (current != null) {
                    System.out.printf("%d ", current.value);
                    current = current.forward[i];
                }
                System.out.println();
            }
        }
    }
    
    /**
     * 简化版布隆过滤器
     */
    public static class BloomFilter {
        private final BitSet bitSet;
        private final int size;
        private final int hashFunctions;
        
        public BloomFilter(int size, int hashFunctions) {
            this.size = size;
            this.hashFunctions = hashFunctions;
            this.bitSet = new BitSet(size);
        }
        
        public void add(String item) {
            for (int i = 0; i < hashFunctions; i++) {
                int hash = hash(item, i) % size;
                if (hash < 0) hash += size;
                bitSet.set(hash);
            }
        }
        
        public boolean mightContain(String item) {
            for (int i = 0; i < hashFunctions; i++) {
                int hash = hash(item, i) % size;
                if (hash < 0) hash += size;
                if (!bitSet.get(hash)) {
                    return false;
                }
            }
            return true;
        }
        
        private int hash(String item, int seed) {
            int hash = 0;
            for (char c : item.toCharArray()) {
                hash = hash * 31 + c + seed;
            }
            return Math.abs(hash);
        }
        
        public double getFalsePositiveRate() {
            int setBits = bitSet.cardinality();
            return Math.pow(1.0 - Math.exp(-hashFunctions * setBits / (double) size), hashFunctions);
        }
    }
    
    /**
     * Trie树实现（前缀树）
     */
    public static class Trie {
        static class TrieNode {
            Map<Character, TrieNode> children = new HashMap<>();
            boolean isEndOfWord = false;
            int frequency = 0;
        }
        
        private final TrieNode root;
        
        public Trie() {
            root = new TrieNode();
        }
        
        public void insert(String word) {
            TrieNode current = root;
            for (char c : word.toCharArray()) {
                current = current.children.computeIfAbsent(c, k -> new TrieNode());
            }
            current.isEndOfWord = true;
            current.frequency++;
        }
        
        public boolean search(String word) {
            TrieNode node = searchNode(word);
            return node != null && node.isEndOfWord;
        }
        
        public boolean startsWith(String prefix) {
            return searchNode(prefix) != null;
        }
        
        private TrieNode searchNode(String prefix) {
            TrieNode current = root;
            for (char c : prefix.toCharArray()) {
                current = current.children.get(c);
                if (current == null) {
                    return null;
                }
            }
            return current;
        }
        
        public List<String> getAllWordsWithPrefix(String prefix) {
            List<String> result = new ArrayList<>();
            TrieNode prefixNode = searchNode(prefix);
            if (prefixNode != null) {
                dfs(prefixNode, prefix, result);
            }
            return result;
        }
        
        private void dfs(TrieNode node, String currentWord, List<String> result) {
            if (node.isEndOfWord) {
                result.add(currentWord);
            }
            for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
                dfs(entry.getValue(), currentWord + entry.getKey(), result);
            }
        }
    }
    
    /**
     * 一致性哈希环实现
     */
    public static class ConsistentHashRing {
        private final SortedMap<Integer, String> ring = new TreeMap<>();
        private final int virtualNodes;
        
        public ConsistentHashRing(int virtualNodes) {
            this.virtualNodes = virtualNodes;
        }
        
        public void addNode(String node) {
            for (int i = 0; i < virtualNodes; i++) {
                String virtualNode = node + "#" + i;
                int hash = hash(virtualNode);
                ring.put(hash, node);
            }
            System.out.printf("添加节点 %s，虚拟节点数: %d%n", node, virtualNodes);
        }
        
        public void removeNode(String node) {
            for (int i = 0; i < virtualNodes; i++) {
                String virtualNode = node + "#" + i;
                int hash = hash(virtualNode);
                ring.remove(hash);
            }
            System.out.printf("移除节点 %s%n", node);
        }
        
        public String getNode(String key) {
            if (ring.isEmpty()) {
                return null;
            }
            int hash = hash(key);
            SortedMap<Integer, String> tailMap = ring.tailMap(hash);
            Integer nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
            return ring.get(nodeHash);
        }
        
        private int hash(String input) {
            return Math.abs(input.hashCode());
        }
        
        public void printRing() {
            System.out.println("一致性哈希环状态:");
            ring.forEach((hash, node) -> 
                System.out.printf("Hash: %d -> Node: %s%n", hash, node));
        }
    }
    
    // 演示方法
    public static void main(String[] args) {
        System.out.println("=== 高级数据结构演示 ===\n");
        
        // 1. LRU缓存演示
        System.out.println("1. LRU缓存演示:");
        LRUCache<String, Integer> lruCache = new LRUCache<>(3);
        lruCache.put("a", 1);
        lruCache.put("b", 2);
        lruCache.put("c", 3);
        lruCache.printCacheState();
        
        lruCache.get("a"); // 访问a，将其移到头部
        lruCache.put("d", 4); // 添加d，应该移除最少使用的b
        lruCache.printCacheState();
        System.out.println();
        
        // 2. 跳表演示
        System.out.println("2. 跳表演示:");
        SkipList skipList = new SkipList();
        int[] values = {3, 6, 7, 9, 12, 19, 17, 26, 21, 25};
        for (int value : values) {
            skipList.insert(value);
        }
        skipList.display();
        System.out.printf("查找12: %b%n", skipList.search(12));
        System.out.printf("查找15: %b%n", skipList.search(15));
        System.out.println();
        
        // 3. 布隆过滤器演示
        System.out.println("3. 布隆过滤器演示:");
        BloomFilter bloomFilter = new BloomFilter(1000, 3);
        String[] items = {"apple", "banana", "orange", "grape", "watermelon"};
        for (String item : items) {
            bloomFilter.add(item);
        }
        
        String[] testItems = {"apple", "banana", "kiwi", "mango", "orange"};
        for (String item : testItems) {
            System.out.printf("'%s' 可能存在: %b%n", item, bloomFilter.mightContain(item));
        }
        System.out.printf("误判率: %.4f%n", bloomFilter.getFalsePositiveRate());
        System.out.println();
        
        // 4. Trie树演示
        System.out.println("4. Trie树演示:");
        Trie trie = new Trie();
        String[] words = {"cat", "car", "card", "care", "careful", "cats", "dog", "dogs"};
        for (String word : words) {
            trie.insert(word);
        }
        
        System.out.printf("搜索 'car': %b%n", trie.search("car"));
        System.out.printf("搜索 'care': %b%n", trie.search("care"));
        System.out.printf("搜索 'caring': %b%n", trie.search("caring"));
        
        System.out.println("以 'car' 开头的所有单词:");
        List<String> carWords = trie.getAllWordsWithPrefix("car");
        carWords.forEach(word -> System.out.printf("  %s%n", word));
        System.out.println();
        
        // 5. 一致性哈希演示
        System.out.println("5. 一致性哈希环演示:");
        ConsistentHashRing hashRing = new ConsistentHashRing(5);
        
        // 添加节点
        hashRing.addNode("Server1");
        hashRing.addNode("Server2");
        hashRing.addNode("Server3");
        
        // 测试数据分布
        String[] keys = {"user1", "user2", "user3", "user4", "user5", "user6"};
        System.out.println("数据分布:");
        for (String key : keys) {
            String server = hashRing.getNode(key);
            System.out.printf("Key '%s' -> Server '%s'%n", key, server);
        }
        
        System.out.println("\n移除Server2后的数据分布:");
        hashRing.removeNode("Server2");
        for (String key : keys) {
            String server = hashRing.getNode(key);
            System.out.printf("Key '%s' -> Server '%s'%n", key, server);
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
}
