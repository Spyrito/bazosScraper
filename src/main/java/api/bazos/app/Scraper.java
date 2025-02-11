package api.bazos.app;
import api.bazos.app.config.Config;
import api.bazos.app.db.DatabaseManager;
import api.bazos.app.notifiaction.TelegramNotifier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

public class Scraper {
    private static final Logger logger = Logger.getLogger(Scraper.class.getName());
    private final Config config;
    private final DatabaseManager dbManager;
    private final TelegramNotifier telegramNotifier;

    static {
        try {
            Files.createDirectories(Paths.get("logs"));
            String logFileName = "logs/scraper_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
            FileHandler fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
            cleanOldLogs();
        } catch (IOException e) {
            System.err.println("Nepodařilo se nastavit logování: " + e.getMessage());
        }
    }

    public Scraper(Config config, DatabaseManager dbManager, TelegramNotifier telegramNotifier) {
        this.config = config;
        this.dbManager = dbManager;
        this.telegramNotifier = telegramNotifier;
    }

    public void run() {
        for (SearchConfig search : config.getSearches()) {
            logger.info(getTimestamp() + " - Zpracovávám frázi: " + search.getPhrase());
            processSearch(search);
        }
        waitBeforeNextRun();
    }

    private void processSearch(SearchConfig search) {
        String baseUrl = "https://www.bazos.cz/search.php";
        String query = "?hledat=" + search.getPhrase() + "&hlokalita=&humkreis=25&cenaod=&cenado=&order=&crz=";

        boolean hasNextPage = true;
        int offset = 0;
        boolean firstPage = true;

        while (hasNextPage) {
            try {
                if (!firstPage) {
                    int delay = ThreadLocalRandom.current().nextInt(2000, 5000);
                    logger.info(getTimestamp() + " - Čekám " + delay + " ms před načtením další stránky...");
                    Thread.sleep(delay);
                }
                String url = baseUrl + query + offset;
                Document doc = fetchPage(url);

                Elements results = doc.select(".inzeraty .inzeratynadpis");
                logger.info(getTimestamp() + " - Počet nalezených inzerátů: " + results.size());

                hasNextPage = processResults(results, search, offset, doc);
                offset += 20;
                firstPage = false;
            } catch (Exception e) {
                logger.severe(getTimestamp() + " - Chyba při zpracování stránky: " + e.getMessage());
                hasNextPage = false;
            }
        }
    }

    private Document fetchPage(String url) throws IOException {
        logger.info(getTimestamp() + " - Načítám stránku: " + url);
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();
    }

    private boolean processResults(Elements results, SearchConfig search, int offset, Document doc) {
        if (results.isEmpty()) {
            return doc.selectFirst("a.paging[title='další stránka']") != null;
        }

        for (Element result : results) {
            String title = result.text();
            String urlResult = result.select("a").attr("abs:href");
            String priceText = result.parent().select(".inzeratycena").text();

            double price = parsePrice(priceText);
            if (price < search.getMinPrice() || price > search.getMaxPrice()) {
                logger.info(getTimestamp() + " - Inzerát mimo cenové rozpětí: " + title + " - " + priceText);
                continue;
            }

            if (isAdBlocked(title, search.getBlockedWords())) {
                logger.info(getTimestamp() + " - Inzerát obsahuje zakázaná slova: " + title);
                continue;
            }

            if (!dbManager.isAdInDatabase(urlResult)) {
                dbManager.saveAd(title, priceText, urlResult);
                logger.info(getTimestamp() + " - Inzerát uložen: " + title);
                sendTelegramMessage(title, priceText, urlResult);
            } else {
                logger.info(getTimestamp() + " - Inzerát již existuje: " + urlResult);
            }
        }
        return true;
    }

    private boolean isAdBlocked(String title, List<String> blockedWords) {
        if (blockedWords != null) {
            for (String word : blockedWords) {
                if (title.toLowerCase().contains(word.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendTelegramMessage(String title, String priceText, String urlResult) {
        int maxCharSendTitle = 80;
        if (title.length() > maxCharSendTitle) {
            title = title.substring(0, maxCharSendTitle) + "...";
        }
        String message = "Nový inzerát:\n" +
                "Název: " + title + "\n" +
                "Cena: " + priceText + "\n" +
                "Odkaz: " + urlResult;
        telegramNotifier.sendMessage(message);
    }

    private void waitBeforeNextRun() {
        try {
            int waitTimeMillis = config.getWaitTimeMinutes() * 60 * 1000;
            logger.info(getTimestamp() + " - Čekám " + config.getWaitTimeMinutes() + " minut před dalším spuštěním.");
            cleanOldLogs();
            Thread.sleep(waitTimeMillis);
        } catch (InterruptedException e) {
            logger.warning(getTimestamp() + " - Proces čekání byl přerušen.");
            Thread.currentThread().interrupt();
        }
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
    private static void cleanOldLogs() {
        try (Stream<Path> files = Files.list(Paths.get("logs"))) {
            files.filter(path -> path.getFileName().toString().matches("scraper_\\d{4}-\\d{2}-\\d{2}\\.log"))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Nepodařilo se smazat starý log: " + path);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Chyba při čištění starých logů: " + e.getMessage());
        }
    }


    private double parsePrice(String priceText) {
        try {
            return Double.parseDouble(priceText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
    }
}
