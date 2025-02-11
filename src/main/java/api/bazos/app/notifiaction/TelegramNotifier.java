package api.bazos.app.notifiaction;

import api.bazos.app.config.Config;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class TelegramNotifier {
    private static final Logger logger = Logger.getLogger(TelegramNotifier.class.getName());
    private final Config config;

    public TelegramNotifier(Config config) {
        this.config = config;
    }

    public void sendMessage(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + config.getTelegramToken() + "/sendMessage?chat_id=" + config.getTelegramChatId() + "&text=" + encodedMessage;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                logger.info("Zpráva byla úspěšně odeslána.");
            } else {
                logger.warning("Chyba při odesílání zprávy. Kód odpovědi: " + responseCode);
            }
        } catch (Exception e) {
            logger.severe("Chyba při odesílání zprávy: " + e.getMessage());
        }
    }
}
