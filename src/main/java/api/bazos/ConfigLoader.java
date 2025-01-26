package api.bazos;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

class ConfigLoader {
    public Config loadConfig(String filePath) {
        try {
            File configFile = new File(filePath);
            if (!configFile.exists()) {
                System.out.println("Config soubor nebyl nalezen: " + filePath);
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(configFile);

            doc.getDocumentElement().normalize();

            Config config = new Config();
            config.setSearches(new ArrayList<>());

            org.w3c.dom.NodeList searchNodes = doc.getElementsByTagName("search");
            for (int i = 0; i < searchNodes.getLength(); i++) {
                org.w3c.dom.Element searchElement = (org.w3c.dom.Element) searchNodes.item(i);
                SearchConfig searchConfig = new SearchConfig();
                searchConfig.setPhrase(searchElement.getElementsByTagName("phrase").item(0).getTextContent());
                searchConfig.setMinPrice(Double.parseDouble(searchElement.getElementsByTagName("minPrice").item(0).getTextContent()));
                searchConfig.setMaxPrice(Double.parseDouble(searchElement.getElementsByTagName("maxPrice").item(0).getTextContent()));
                config.getSearches().add(searchConfig);
            }

            config.setDbUrl(doc.getElementsByTagName("dbUrl").item(0).getTextContent());
            config.setDbUser(doc.getElementsByTagName("dbUser").item(0).getTextContent());
            config.setDbPassword(doc.getElementsByTagName("dbPassword").item(0).getTextContent());
            config.setTelegramNotifications(Boolean.parseBoolean(doc.getElementsByTagName("telegramNotifications").item(0).getTextContent()));
            config.setTelegramToken(doc.getElementsByTagName("telegramToken").item(0).getTextContent());
            config.setTelegramChatId(doc.getElementsByTagName("telegramChatId").item(0).getTextContent());
            config.setWaitTimeMinutes(Integer.parseInt(doc.getElementsByTagName("waitTimeMinutes").item(0).getTextContent()));

            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

