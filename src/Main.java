import java.sql.*;

public class Main {
    public static void main(String[] args) {
        // MySQL JDBC连接示例：统计user表中每个name出现的次数，并按次数降序排列
        String url = "jdbc:mysql://localhost:3306/your_database?useSSL=false&serverTimezone=UTC";
        String user = "your_username";
        String password = "your_password";

        try {
            // 加载JDBC驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 建立连接
            Connection conn = DriverManager.getConnection(url, user, password);

            // 查询每个name出现的次数，并按次数降序排列，取前10个
            String sql = "SELECT name, COUNT(*) as cnt FROM user GROUP BY name ORDER BY cnt DESC LIMIT 10";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("name\t出现次数");
            while (rs.next()) {
                String name = rs.getString("name");
                int count = rs.getInt("cnt");
                System.out.println(name + "\t" + count);
            }

            // 关闭资源
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}