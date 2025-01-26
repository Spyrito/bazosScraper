package api.bazos;

class TelegramNotifier {
    private final Config config;

    public TelegramNotifier(Config config) {
        this.config = config;
    }

    public void sendMessage(String message) {
        try {
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + config.getTelegramToken() + "/sendMessage?chat_id=" + config.getTelegramChatId() + "&text=" + encodedMessage;

            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Zpráva byla úspěšně odeslána.");
            } else {
                System.out.println("Chyba při odesílání zprávy. Kód odpovědi: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

