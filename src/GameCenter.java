import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图形用户界面游戏中心
 * 包含贪吃蛇、俄罗斯方块、扫雷等多个小游戏
 */
public class GameCenter extends JFrame {
    
    private CardLayout cardLayout;
    private JPanel gamePanel;
    private Timer gameTimer;
    
    public GameCenter() {
        setTitle("游戏中心 - Java GUI 演示");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        initializeComponents();
        setVisible(true);
    }
    
    private void initializeComponents() {
        cardLayout = new CardLayout();
        gamePanel = new JPanel(cardLayout);
        
        // 创建主菜单
        JPanel mainMenu = createMainMenu();
        gamePanel.add(mainMenu, "MAIN");
        
        // 创建各种游戏
        gamePanel.add(new SnakeGame(), "SNAKE");
        gamePanel.add(new TetrisGame(), "TETRIS");
        gamePanel.add(new MinesweeperGame(), "MINESWEEPER");
        gamePanel.add(new CalculatorApp(), "CALCULATOR");
        
        add(gamePanel);
        cardLayout.show(gamePanel, "MAIN");
    }
    
    private JPanel createMainMenu() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        
        // 标题
        JLabel titleLabel = new JLabel("游戏中心", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 36));
        titleLabel.setForeground(new Color(50, 50, 150));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 30, 0));
        
        // 游戏按钮面板
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));
        buttonsPanel.setBackground(new Color(240, 240, 240));
        
        // 创建游戏按钮
        String[] games = {"贪吃蛇", "俄罗斯方块", "扫雷", "计算器"};
        String[] gameKeys = {"SNAKE", "TETRIS", "MINESWEEPER", "CALCULATOR"};
        Color[] colors = {
            new Color(76, 175, 80),   // 绿色
            new Color(63, 81, 181),   // 蓝色
            new Color(255, 152, 0),   // 橙色
            new Color(156, 39, 176)   // 紫色
        };
        
        for (int i = 0; i < games.length; i++) {
            JButton gameButton = createGameButton(games[i], gameKeys[i], colors[i]);
            buttonsPanel.add(gameButton);
        }
        
        // 底部信息
        JLabel infoLabel = new JLabel("选择一个游戏开始体验", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        infoLabel.setForeground(Color.GRAY);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JButton createGameButton(String text, String gameKey, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setBorder(BorderFactory.createRaisedBorderBorder());
        button.setPreferredSize(new Dimension(150, 100));
        button.setFocusPainted(false);
        
        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        button.addActionListener(e -> cardLayout.show(gamePanel, gameKey));
        
        return button;
    }
    
    /**
     * 贪吃蛇游戏
     */
    class SnakeGame extends JPanel implements ActionListener, KeyListener {
        private final int BOARD_SIZE = 600;
        private final int UNIT_SIZE = 25;
        private final int GAME_UNITS = (BOARD_SIZE * BOARD_SIZE) / UNIT_SIZE;
        private final int DELAY = 100;
        
        private List<Point> snake;
        private Point food;
        private char direction = 'R';
        private boolean running = false;
        private Timer timer;
        private int score = 0;
        
        public SnakeGame() {
            setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE + 100));
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);
            
            startGame();
        }
        
        public void startGame() {
            snake = new ArrayList<>();
            snake.add(new Point(0, 0));
            newFood();
            running = true;
            timer = new Timer(DELAY, this);
            timer.start();
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            draw(g);
        }
        
        public void draw(Graphics g) {
            if (running) {
                // 画食物
                g.setColor(Color.RED);
                g.fillOval(food.x, food.y, UNIT_SIZE, UNIT_SIZE);
                
                // 画蛇
                for (int i = 0; i < snake.size(); i++) {
                    if (i == 0) {
                        g.setColor(Color.GREEN);
                    } else {
                        g.setColor(new Color(45, 180, 0));
                    }
                    g.fillRect(snake.get(i).x, snake.get(i).y, UNIT_SIZE, UNIT_SIZE);
                }
                
                // 画分数
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                FontMetrics metrics = getFontMetrics(g.getFont());
                g.drawString("分数: " + score, 
                    (BOARD_SIZE - metrics.stringWidth("分数: " + score)) / 2, 
                    BOARD_SIZE + 50);
                    
                // 返回按钮
                g.drawString("按ESC返回主菜单", 10, BOARD_SIZE + 30);
                
            } else {
                gameOver(g);
            }
        }
        
        public void newFood() {
            food = new Point(
                ThreadLocalRandom.current().nextInt(0, BOARD_SIZE / UNIT_SIZE) * UNIT_SIZE,
                ThreadLocalRandom.current().nextInt(0, BOARD_SIZE / UNIT_SIZE) * UNIT_SIZE
            );
        }
        
        public void move() {
            Point newHead = new Point(snake.get(0));
            
            switch (direction) {
                case 'U': newHead.y -= UNIT_SIZE; break;
                case 'D': newHead.y += UNIT_SIZE; break;
                case 'L': newHead.x -= UNIT_SIZE; break;
                case 'R': newHead.x += UNIT_SIZE; break;
            }
            
            snake.add(0, newHead);
            
            // 检查是否吃到食物
            if (newHead.equals(food)) {
                score++;
                newFood();
            } else {
                snake.remove(snake.size() - 1);
            }
        }
        
        public void checkFood() {
            if (snake.get(0).equals(food)) {
                score++;
                newFood();
            }
        }
        
        public void checkCollisions() {
            // 检查头部是否撞到身体
            for (int i = 1; i < snake.size(); i++) {
                if (snake.get(0).equals(snake.get(i))) {
                    running = false;
                }
            }
            
            // 检查头部是否撞到边界
            if (snake.get(0).x < 0 || snake.get(0).x >= BOARD_SIZE || 
                snake.get(0).y < 0 || snake.get(0).y >= BOARD_SIZE) {
                running = false;
            }
            
            if (!running) {
                timer.stop();
            }
        }
        
        public void gameOver(Graphics g) {
            // 游戏结束文本
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            FontMetrics metrics1 = getFontMetrics(g.getFont());
            g.drawString("游戏结束", 
                (BOARD_SIZE - metrics1.stringWidth("游戏结束")) / 2, 
                BOARD_SIZE / 2);
            
            // 最终分数
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics metrics2 = getFontMetrics(g.getFont());
            g.drawString("最终分数: " + score, 
                (BOARD_SIZE - metrics2.stringWidth("最终分数: " + score)) / 2, 
                BOARD_SIZE / 2 + 50);
                
            g.drawString("按空格重新开始，ESC返回主菜单", 
                (BOARD_SIZE - metrics2.stringWidth("按空格重新开始，ESC返回主菜单")) / 2, 
                BOARD_SIZE / 2 + 100);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (running) {
                move();
                checkFood();
                checkCollisions();
            }
            repaint();
        }
        
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (direction != 'R') direction = 'L';
                    break;
                case KeyEvent.VK_RIGHT:
                    if (direction != 'L') direction = 'R';
                    break;
                case KeyEvent.VK_UP:
                    if (direction != 'D') direction = 'U';
                    break;
                case KeyEvent.VK_DOWN:
                    if (direction != 'U') direction = 'D';
                    break;
                case KeyEvent.VK_SPACE:
                    if (!running) {
                        score = 0;
                        startGame();
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    if (timer != null) timer.stop();
                    cardLayout.show(gamePanel, "MAIN");
                    break;
            }
        }
        
        @Override public void keyTyped(KeyEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}
    }
    
    /**
     * 简化版俄罗斯方块
     */
    class TetrisGame extends JPanel {
        private JLabel statusLabel;
        private AtomicInteger score = new AtomicInteger(0);
        
        public TetrisGame() {
            setLayout(new BorderLayout());
            setBackground(Color.DARK_GRAY);
            
            statusLabel = new JLabel("俄罗斯方块 - 分数: 0", SwingConstants.CENTER);
            statusLabel.setForeground(Color.WHITE);
            statusLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
            statusLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            
            JLabel gameArea = new JLabel("游戏区域 (简化演示)", SwingConstants.CENTER);
            gameArea.setForeground(Color.WHITE);
            gameArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
            gameArea.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            
            JPanel controlPanel = createTetrisControls();
            
            add(statusLabel, BorderLayout.NORTH);
            add(gameArea, BorderLayout.CENTER);
            add(controlPanel, BorderLayout.SOUTH);
        }
        
        private JPanel createTetrisControls() {
            JPanel panel = new JPanel(new FlowLayout());
            panel.setBackground(Color.DARK_GRAY);
            
            JButton startButton = new JButton("开始游戏");
            JButton pauseButton = new JButton("暂停");
            JButton backButton = new JButton("返回主菜单");
            
            Color buttonColor = new Color(63, 81, 181);
            JButton[] buttons = {startButton, pauseButton, backButton};
            
            for (JButton button : buttons) {
                button.setBackground(buttonColor);
                button.setForeground(Color.WHITE);
                button.setFocusPainted(false);
                button.setBorder(BorderFactory.createRaisedBorderBorder());
                panel.add(button);
            }
            
            startButton.addActionListener(e -> {
                score.set(ThreadLocalRandom.current().nextInt(100, 1000));
                statusLabel.setText("俄罗斯方块 - 分数: " + score.get());
                JOptionPane.showMessageDialog(this, "游戏开始！得分：" + score.get());
            });
            
            pauseButton.addActionListener(e -> 
                JOptionPane.showMessageDialog(this, "游戏已暂停"));
            
            backButton.addActionListener(e -> cardLayout.show(gamePanel, "MAIN"));
            
            JLabel instructionLabel = new JLabel("使用方向键控制方块", SwingConstants.CENTER);
            instructionLabel.setForeground(Color.LIGHT_GRAY);
            
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setBackground(Color.DARK_GRAY);
            wrapperPanel.add(panel, BorderLayout.CENTER);
            wrapperPanel.add(instructionLabel, BorderLayout.SOUTH);
            
            return wrapperPanel;
        }
    }
    
    /**
     * 扫雷游戏
     */
    class MinesweeperGame extends JPanel {
        private final int GRID_SIZE = 10;
        private final int MINE_COUNT = 15;
        private JButton[][] buttons;
        private boolean[][] mines;
        private boolean[][] revealed;
        private boolean gameOver = false;
        private int revealedCount = 0;
        
        public MinesweeperGame() {
            setLayout(new BorderLayout());
            setBackground(Color.LIGHT_GRAY);
            
            JLabel titleLabel = new JLabel("扫雷游戏 - 找出所有地雷", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            
            JPanel gameBoard = createMinesweeperBoard();
            
            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBackground(Color.LIGHT_GRAY);
            
            JButton newGameButton = new JButton("新游戏");
            JButton backButton = new JButton("返回主菜单");
            
            newGameButton.addActionListener(e -> newGame());
            backButton.addActionListener(e -> cardLayout.show(gamePanel, "MAIN"));
            
            controlPanel.add(newGameButton);
            controlPanel.add(backButton);
            
            add(titleLabel, BorderLayout.NORTH);
            add(gameBoard, BorderLayout.CENTER);
            add(controlPanel, BorderLayout.SOUTH);
            
            newGame();
        }
        
        private JPanel createMinesweeperBoard() {
            JPanel board = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 2, 2));
            board.setBackground(Color.DARK_GRAY);
            board.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            buttons = new JButton[GRID_SIZE][GRID_SIZE];
            mines = new boolean[GRID_SIZE][GRID_SIZE];
            revealed = new boolean[GRID_SIZE][GRID_SIZE];
            
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    JButton button = new JButton();
                    button.setPreferredSize(new Dimension(40, 40));
                    button.setBackground(Color.GRAY);
                    button.setBorder(BorderFactory.createRaisedBorderBorder());
                    
                    final int row = i, col = j;
                    button.addActionListener(e -> revealCell(row, col));
                    
                    buttons[i][j] = button;
                    board.add(button);
                }
            }
            
            return board;
        }
        
        private void newGame() {
            gameOver = false;
            revealedCount = 0;
            
            // 重置所有状态
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    mines[i][j] = false;
                    revealed[i][j] = false;
                    buttons[i][j].setText("");
                    buttons[i][j].setBackground(Color.GRAY);
                    buttons[i][j].setEnabled(true);
                }
            }
            
            // 随机放置地雷
            Random random = new Random();
            int minesPlaced = 0;
            while (minesPlaced < MINE_COUNT) {
                int row = random.nextInt(GRID_SIZE);
                int col = random.nextInt(GRID_SIZE);
                if (!mines[row][col]) {
                    mines[row][col] = true;
                    minesPlaced++;
                }
            }
        }
        
        private void revealCell(int row, int col) {
            if (gameOver || revealed[row][col]) return;
            
            revealed[row][col] = true;
            revealedCount++;
            
            if (mines[row][col]) {
                // 踩到地雷
                buttons[row][col].setText("💣");
                buttons[row][col].setBackground(Color.RED);
                gameOver = true;
                JOptionPane.showMessageDialog(this, "游戏结束！你踩到了地雷！");
                revealAllMines();
            } else {
                // 安全区域
                int nearbyMines = countNearbyMines(row, col);
                if (nearbyMines > 0) {
                    buttons[row][col].setText(String.valueOf(nearbyMines));
                    buttons[row][col].setForeground(getNumberColor(nearbyMines));
                }
                buttons[row][col].setBackground(Color.LIGHT_GRAY);
                buttons[row][col].setEnabled(false);
                
                // 检查胜利条件
                if (revealedCount == GRID_SIZE * GRID_SIZE - MINE_COUNT) {
                    gameOver = true;
                    JOptionPane.showMessageDialog(this, "恭喜你赢了！所有安全区域都被揭开了！");
                }
            }
        }
        
        private int countNearbyMines(int row, int col) {
            int count = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int newRow = row + i;
                    int newCol = col + j;
                    if (newRow >= 0 && newRow < GRID_SIZE && 
                        newCol >= 0 && newCol < GRID_SIZE && 
                        mines[newRow][newCol]) {
                        count++;
                    }
                }
            }
            return count;
        }
        
        private Color getNumberColor(int number) {
            Color[] colors = {
                Color.BLACK, Color.BLUE, Color.GREEN, Color.RED, 
                Color.PURPLE, Color.MAGENTA, Color.CYAN, Color.ORANGE
            };
            return colors[Math.min(number, colors.length - 1)];
        }
        
        private void revealAllMines() {
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (mines[i][j] && !revealed[i][j]) {
                        buttons[i][j].setText("💣");
                        buttons[i][j].setBackground(Color.ORANGE);
                    }
                }
            }
        }
    }
    
    /**
     * 计算器应用
     */
    class CalculatorApp extends JPanel {
        private JTextField displayField;
        private double currentValue = 0;
        private String currentOperator = "";
        private boolean startNewNumber = true;
        
        public CalculatorApp() {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            
            displayField = new JTextField("0");
            displayField.setFont(new Font("Arial", Font.BOLD, 32));
            displayField.setHorizontalAlignment(JTextField.RIGHT);
            displayField.setEditable(false);
            displayField.setBackground(Color.BLACK);
            displayField.setForeground(Color.WHITE);
            displayField.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JPanel buttonPanel = createCalculatorButtons();
            
            JPanel topPanel = new JPanel(new BorderLayout());
            JLabel titleLabel = new JLabel("科学计算器", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            
            JButton backButton = new JButton("返回主菜单");
            backButton.addActionListener(e -> cardLayout.show(gamePanel, "MAIN"));
            
            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.add(titleLabel, BorderLayout.CENTER);
            titlePanel.add(backButton, BorderLayout.EAST);
            
            add(titlePanel, BorderLayout.NORTH);
            add(displayField, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
        }
        
        private JPanel createCalculatorButtons() {
            JPanel panel = new JPanel(new GridLayout(5, 4, 5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            String[][] buttonLabels = {
                {"C", "CE", "√", "/"},
                {"7", "8", "9", "*"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"±", "0", ".", "="}
            };
            
            for (String[] row : buttonLabels) {
                for (String label : row) {
                    JButton button = createCalculatorButton(label);
                    panel.add(button);
                }
            }
            
            return panel;
        }
        
        private JButton createCalculatorButton(String text) {
            JButton button = new JButton(text);
            button.setFont(new Font("Arial", Font.BOLD, 20));
            button.setPreferredSize(new Dimension(60, 60));
            
            if (text.matches("[0-9.]")) {
                button.setBackground(Color.LIGHT_GRAY);
            } else if (text.matches("[+\\-*/=]")) {
                button.setBackground(new Color(255, 152, 0));
                button.setForeground(Color.WHITE);
            } else {
                button.setBackground(Color.GRAY);
                button.setForeground(Color.WHITE);
            }
            
            button.addActionListener(e -> handleCalculatorInput(text));
            
            return button;
        }
        
        private void handleCalculatorInput(String input) {
            try {
                switch (input) {
                    case "C":
                        currentValue = 0;
                        currentOperator = "";
                        displayField.setText("0");
                        startNewNumber = true;
                        break;
                    case "CE":
                        displayField.setText("0");
                        startNewNumber = true;
                        break;
                    case "=":
                        if (!currentOperator.isEmpty()) {
                            double displayValue = Double.parseDouble(displayField.getText());
                            double result = performOperation(currentValue, displayValue, currentOperator);
                            displayField.setText(formatResult(result));
                            currentValue = result;
                            currentOperator = "";
                            startNewNumber = true;
                        }
                        break;
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                        if (!currentOperator.isEmpty()) {
                            double displayValue = Double.parseDouble(displayField.getText());
                            currentValue = performOperation(currentValue, displayValue, currentOperator);
                            displayField.setText(formatResult(currentValue));
                        } else {
                            currentValue = Double.parseDouble(displayField.getText());
                        }
                        currentOperator = input;
                        startNewNumber = true;
                        break;
                    case "√":
                        double value = Double.parseDouble(displayField.getText());
                        double sqrt = Math.sqrt(value);
                        displayField.setText(formatResult(sqrt));
                        startNewNumber = true;
                        break;
                    case "±":
                        double currentDisplay = Double.parseDouble(displayField.getText());
                        displayField.setText(formatResult(-currentDisplay));
                        break;
                    default:
                        if (startNewNumber) {
                            displayField.setText(input);
                            startNewNumber = false;
                        } else {
                            String current = displayField.getText();
                            if (input.equals(".") && current.contains(".")) {
                                return;
                            }
                            displayField.setText(current + input);
                        }
                        break;
                }
            } catch (Exception e) {
                displayField.setText("错误");
                startNewNumber = true;
            }
        }
        
        private double performOperation(double a, double b, String operator) {
            switch (operator) {
                case "+": return a + b;
                case "-": return a - b;
                case "*": return a * b;
                case "/": return b != 0 ? a / b : Double.NaN;
                default: return b;
            }
        }
        
        private String formatResult(double result) {
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "错误";
            }
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long)result);
            } else {
                return String.format("%.8g", result);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                // 使用默认外观
            }
            new GameCenter();
        });
    }
}
