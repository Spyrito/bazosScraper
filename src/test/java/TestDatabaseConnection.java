import java.sql.Connection;
import java.sql.DriverManager;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/bazos?useSSL=false&serverTimezone=UTC";
        String user = "root"; // Nahraďte správným uživatelským jménem
        String password = ""; // Nahraďte správným heslem

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, user, password);
            System.out.println("Připojení úspěšné!");
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
