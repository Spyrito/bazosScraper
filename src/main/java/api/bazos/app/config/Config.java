package api.bazos.app.config;

import api.bazos.app.SearchConfig;

import java.util.List;

public class Config {
    private List<SearchConfig> searches;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private boolean telegramNotifications;
    private String telegramToken;
    private String telegramChatId;
    private int waitTimeMinutes;

    // Gettery a settery
    public List<SearchConfig> getSearches() {
        return searches;
    }

    public void setSearches(List<SearchConfig> searches) {
        this.searches = searches;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public boolean isTelegramNotifications() {
        return telegramNotifications;
    }

    public void setTelegramNotifications(boolean telegramNotifications) {
        this.telegramNotifications = telegramNotifications;
    }

    public String getTelegramToken() {
        return telegramToken;
    }

    public void setTelegramToken(String telegramToken) {
        this.telegramToken = telegramToken;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public int getWaitTimeMinutes() {
        return waitTimeMinutes;
    }

    public void setWaitTimeMinutes(int waitTimeMinutes) {
        this.waitTimeMinutes = waitTimeMinutes;
    }
}
