package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class BazosScraper {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- Spouštěcí menu ---");
            System.out.println("1. Spustit program");
            System.out.println("2. Promazat tabulku 'inzeraty'");
            System.out.println("3. Konec");
            System.out.print("Vyberte možnost: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Neplatná volba. Zadejte číslo 1, 2 nebo 3.");
                continue;
            }

            switch (choice) {
                case 1:
                    spustitProgram();
                    break;
                case 2:
                    promazatTabulku();
                    break;
                case 3:
                    System.out.println("Program ukončen.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Neplatná volba. Zadejte číslo 1, 2 nebo 3.");
            }
        }
    }

    private static void spustitProgram() {
        Config config = načístNastavení();
        if (config == null || config.searchPhrase == null || config.dbUrl == null || config.dbUser == null || config.dbPassword == null) {
            System.out.println("Chyba: Nepodařilo se načíst konfiguraci z config souboru.");
            return;
        }

        String searchPhrase = config.searchPhrase;
        String baseUrl = "https://www.bazos.cz/search.php";
        String query = "?hledat=" + searchPhrase + "&hlokalita=&humkreis=25&cenaod=&cenado=&order=&crz=";
        int offset = 0;
        boolean hasResults = true;
        int page = 0;

        try (Connection connection = DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword)) {
            System.out.println("Připojeno k databázi.");

            while (hasResults) {
                try {
                    String url = baseUrl + query + offset;
                    System.out.println("Načítám stránku: " + url);

                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(10000)
                            .get();

                    Elements results = doc.select(".inzeraty .inzeratynadpis");
                    System.out.println("Počet nalezených inzerátů: " + results.size());
                    page++;
                    System.out.println("Stránka: " + page);

                    if (results.isEmpty()) {
                        hasResults = false;
                        page = 0;
                        break;
                    }

                    for (Element result : results) {
                        String title = result.text();
                        String urlResult = result.select("a").attr("abs:href");
                        String priceText = result.parent().select(".inzeratycena").text();

                        double price = parsePrice(priceText);
                        if (price < config.minPrice || price > config.maxPrice) {
                            System.out.println("Inzerát mimo cenové rozpětí: " + title + " - " + priceText);
                            continue;
                        }

                        String checkQuery = "SELECT COUNT(*) FROM inzeraty WHERE url = ?";
                        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                            checkStmt.setString(1, urlResult);
                            ResultSet rs = checkStmt.executeQuery();
                            rs.next();
                            int count = rs.getInt(1);

                            if (count == 0) {
                                String insertQuery = "INSERT INTO inzeraty (nazev, cena, url) VALUES (?, ?, ?)";
                                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                                    insertStmt.setString(1, title);
                                    insertStmt.setString(2, priceText.isEmpty() ? "Neuvedeno" : priceText);
                                    insertStmt.setString(3, urlResult);
                                    insertStmt.executeUpdate();
                                    System.out.println("Inzerát uložen: " + title);

                                    if (config.telegramNotifications) {
                                        odeslatTelegramZpravu(config.telegramToken, config.telegramChatId, "Nový inzerát: " + title + " (" + priceText + ")\n" + urlResult);
                                    }
                                }
                            } else {
                                System.out.println("Inzerát již existuje: " + urlResult);
                            }
                        }
                    }

                    offset += 20;
                    Thread.sleep(5000 + (int) (Math.random() * 5000));

                } catch (Exception e) {
                    e.printStackTrace();
                    hasResults = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            int minutes = 10;
            System.out.println("Čekám " + minutes + " minut před dalším cyklem...");
            Thread.sleep(minutes * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void promazatTabulku() {
        Config config = načístNastavení();
        if (config == null || config.dbUrl == null || config.dbUser == null || config.dbPassword == null) {
            System.out.println("Chyba: Nepodařilo se načíst konfiguraci z config souboru.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword)) {
            System.out.println("Připojeno k databázi.");
            String deleteQuery = "DELETE FROM inzeraty";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                int rowsAffected = deleteStmt.executeUpdate();
                System.out.println("Tabulka 'inzeraty' byla promazána. Počet smazaných řádků: " + rowsAffected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Config načístNastavení() {
        String configFilePath = "src/main/java/org/example/config.xml";
        try {
            File configFile = new File(configFilePath);
            if (!configFile.exists()) {
                System.out.println("Config soubor nebyl nalezen: " + configFilePath);
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(configFile);

            doc.getDocumentElement().normalize();

            Config config = new Config();
            config.searchPhrase = doc.getElementsByTagName("searchPhrase").item(0).getTextContent();
            config.dbUrl = doc.getElementsByTagName("dbUrl").item(0).getTextContent();
            config.dbUser = doc.getElementsByTagName("dbUser").item(0).getTextContent();
            config.dbPassword = doc.getElementsByTagName("dbPassword").item(0).getTextContent();
            config.minPrice = Double.parseDouble(doc.getElementsByTagName("minPrice").item(0).getTextContent());
            config.maxPrice = Double.parseDouble(doc.getElementsByTagName("maxPrice").item(0).getTextContent());
            config.telegramNotifications = Boolean.parseBoolean(doc.getElementsByTagName("telegramNotifications").item(0).getTextContent());
            config.telegramToken = doc.getElementsByTagName("telegramToken").item(0).getTextContent();
            config.telegramChatId = doc.getElementsByTagName("telegramChatId").item(0).getTextContent();

            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void odeslatTelegramZpravu(String token, String chatId, String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + encodedMessage;

            System.out.println("URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Zpráva byla úspěšně odeslána.");
            } else {
                System.out.println("Chyba při odesílání zprávy. Kód odpovědi: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Nepodařilo se odeslat zprávu přes Telegram.");
            e.printStackTrace();
        }
    }


    private static double parsePrice(String priceText) {
        try {
            return Double.parseDouble(priceText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE; // Pokud cena není platná, považujeme ji za mimo rozsah
        }
    }

    private static class Config {
        String searchPhrase;
        String dbUrl;
        String dbUser;
        String dbPassword;
        double minPrice;
        double maxPrice;
        boolean telegramNotifications;
        String telegramToken;
        String telegramChatId;
    }
}
