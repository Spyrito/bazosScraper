
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BazosScraperTest {
    public static void main(String[] args) {
        String baseUrl = "https://www.bazos.cz/search.php";
        String query = "?hledat=playstation+5&hlokalita=&humkreis=25&cenaod=&cenado=&order=&crz=";
        int offset = 0; // Posun pro stránkování

        try {
            // Sestavení URL pro první stránku
            String url = baseUrl + query + offset;

            // Načtení HTML stránky
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // Nastavení user-agenta
                    .timeout(5000) // Timeout na 5 sekund
                    .get();

            // Vyhledání výsledků (závisí na struktuře HTML Bazoše)
            Elements results = doc.select(".inzeraty .nadpis a"); // Selektor pro odkazy na inzeráty

            // Pokud nejsou žádné výsledky
            if (results.isEmpty()) {
                System.out.println("Žádné inzeráty nebyly nalezeny.");
                return;
            }

            // Výpis názvů, cen a URL
            for (Element result : results) {
                String title = result.text(); // Získání textu odkazu (název inzerátu)
                String urlResult = result.absUrl("href"); // Absolutní URL odkazu

                // Hledání ceny pomocí CSS selektoru
                Element priceElement = result.parent().parent().select(".inzeratycena b").first(); // CSS selektor pro cenu
                String price = priceElement != null ? priceElement.text() : "Neuvedeno"; // Získání textu ceny, pokud není nalezena, vypíše "Neuvedeno"

                // Výpis informací
                System.out.println("Název: " + title);
                System.out.println("Cena: " + price);
                System.out.println("URL: " + urlResult);
                System.out.println("-------------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
