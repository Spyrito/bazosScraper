package api.bazos.app.db;

import api.bazos.app.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseManager {
    private final Config config;

    public DatabaseManager(Config config) {
        this.config = config;
    }

    public boolean isAdInDatabase(String url) {
        try (Connection connection = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword())) {
            String query = "SELECT COUNT(*) FROM inzeraty WHERE url = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, url);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void saveAd(String title, String price, String url) {
        try (Connection connection = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword())) {
            String insertQuery = "INSERT INTO inzeraty (nazev, cena, url) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setString(1, title);
                stmt.setString(2, price);
                stmt.setString(3, url);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearTable() {
        try (Connection connection = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword())) {
            String deleteQuery = "DELETE FROM inzeraty";
            try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
                int rowsDeleted = stmt.executeUpdate();
                System.out.println("Tabulka 'inzeraty' byla promazána. Počet smazaných řádků: " + rowsDeleted);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
