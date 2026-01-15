import java.util.*;

public class MazeDemo {
    private static final int N = 15; // 迷宫大小
    private static final char WALL = '#';
    private static final char PATH = ' ';
    private static final char VISITED = '.';
    private static final char SOLUTION = '*';

    private char[][] maze = new char[N][N];
    private boolean[][] visited = new boolean[N][N];

    // 方向：上右下左
    private static final int[] dx = {-1, 0, 1, 0};
    private static final int[] dy = {0, 1, 0, -1};

    public MazeDemo() {
        generateMaze();
    }

    // 递归回溯生成迷宫
    private void generateMaze() {
        for (int i = 0; i < N; i++)
            Arrays.fill(maze[i], WALL);
        carve(1, 1);
        maze[1][1] = PATH;
        maze[N-2][N-2] = PATH;
    }

    private void carve(int x, int y) {
        maze[x][y] = PATH;
        List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
        Collections.shuffle(dirs);
        for (int dir : dirs) {
            int nx = x + dx[dir]*2, ny = y + dy[dir]*2;
            if (inBounds(nx, ny) && maze[nx][ny] == WALL) {
                maze[x + dx[dir]][y + dy[dir]] = PATH;
                carve(nx, ny);
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x > 0 && x < N-1 && y > 0 && y < N-1;
    }

    // 求解迷宫
    public boolean solve() {
        for (int i = 0; i < N; i++)
            Arrays.fill(visited[i], false);
        return dfs(1, 1);
    }

    private boolean dfs(int x, int y) {
        if (x == N-2 && y == N-2) {
            maze[x][y] = SOLUTION;
            return true;
        }
        visited[x][y] = true;
        for (int dir = 0; dir < 4; dir++) {
            int nx = x + dx[dir], ny = y + dy[dir];
            if (inBounds(nx, ny) && maze[nx][ny] == PATH && !visited[nx][ny]) {
                if (dfs(nx, ny)) {
                    maze[x][y] = SOLUTION;
                    return true;
                }
            }
        }
        return false;
    }

    // 打印迷宫
    public void printMaze() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++)
                System.out.print(maze[i][j]);
            System.out.println();
        }
    }

    public static void main(String[] args) {
        MazeDemo mazeDemo = new MazeDemo();
        System.out.println("生成的迷宫：");
        mazeDemo.printMaze();
        if (mazeDemo.solve()) {
            System.out.println("迷宫求解路径（*为路径）：");
            mazeDemo.printMaze();
        } else {
            System.out.println("无解！");
        }
    }
}