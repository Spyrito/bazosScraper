package api.bazos;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.concurrent.ThreadLocalRandom;

class Scraper {
    private final Config config;
    private final DatabaseManager dbManager;
    private final TelegramNotifier telegramNotifier;

    public Scraper(Config config, DatabaseManager dbManager, TelegramNotifier telegramNotifier) {
        this.config = config;
        this.dbManager = dbManager;
        this.telegramNotifier = telegramNotifier;
    }

    public void run() {
        for (SearchConfig search : config.getSearches()) {
            System.out.println("\nZpracovávám frázi: " + search.getPhrase());

            String baseUrl = "https://www.bazos.cz/search.php";
            String query = "?hledat=" + search.getPhrase() + "&hlokalita=&humkreis=25&cenaod=&cenado=&order=&crz=";

            boolean hasNextPage = true;
            int offset = 0;
            boolean firstPage = true;
            int maxCharSendTitle = 70;

            while (hasNextPage) {
                try {
                    if (!firstPage) {
                        int delay = ThreadLocalRandom.current().nextInt(2000, 5000);
                        System.out.println("Čekám " + delay + " ms před načtením další stránky...");
                        Thread.sleep(delay);
                    }
                    String url = baseUrl + query + offset;
                    System.out.println("Načítám stránku: " + url);

                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .timeout(10000)
                            .get();

                    Elements results = doc.select(".inzeraty .inzeratynadpis");
                    System.out.println("Počet nalezených inzerátů: " + results.size());

                    if (results.isEmpty()) {
                        Element nextPageButton = doc.selectFirst("a.paging[title='další stránka']");
                        if (nextPageButton == null) {
                            hasNextPage = false;
                        } else {
                            offset += 20;
                        }
                        continue;
                    }

                    for (Element result : results) {
                        String title = result.text();
                        String urlResult = result.select("a").attr("abs:href");
                        String priceText = result.parent().select(".inzeratycena").text();

                        double price = parsePrice(priceText);
                        if (price < search.getMinPrice() || price > search.getMaxPrice()) {
                            System.out.println("Inzerát mimo cenové rozpětí: " + title + " - " + priceText);
                            continue;
                        }

                        if (!dbManager.isAdInDatabase(urlResult)) {
                            dbManager.saveAd(title, priceText, urlResult);
                            System.out.println("Inzerát uložen: " + title);

                            // Odeslání zprávy přes Telegram
                            if (title.length() > maxCharSendTitle) {
                                title = title.substring(0, maxCharSendTitle) + "...";
                            }
                            String message = "Nový inzerát:\n" +
                                    "Název: " + title + "\n" +
                                    "Cena: " + priceText + "\n" +
                                    "Odkaz: " + urlResult;
                            telegramNotifier.sendMessage(message);
                        } else {
                            System.out.println("Inzerát již existuje: " + urlResult);
                        }
                    }

                    offset += 20;
                    firstPage = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    hasNextPage = false;
                }
            }
        }

        try {
            int waitTimeMillis = config.getWaitTimeMinutes() * 60 * 1000;
            System.out.println("\nČekám " + config.getWaitTimeMinutes() + " minut před dalším spuštěním.");
            Thread.sleep(waitTimeMillis);
        } catch (InterruptedException e) {
            System.out.println("Proces čekání byl přerušen.");
            Thread.currentThread().interrupt();
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
