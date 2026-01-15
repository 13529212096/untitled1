import java.sql.*;

public class Main {
    public static void main(String[] args) {
        // MySQL JDBC连接示例
        String url = "jdbc:mysql://localhost:3306/your_database?useSSL=false&serverTimezone=UTC";
        String user = "your_username";
        String password = "your_password";

        try {
            // 加载JDBC驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 建立连接
            Connection conn = DriverManager.getConnection(url, user, password);

            // 创建查询语句
            String sql = "SELECT id, name FROM user";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            // 输出结果
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                System.out.println("id: " + id + ", name: " + name);
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