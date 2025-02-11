package api.bazos.app.ui;
import api.bazos.app.config.ConfigLoader;
import api.bazos.app.Scraper;
import api.bazos.app.config.Config;
import api.bazos.app.db.DatabaseManager;
import api.bazos.app.notifiaction.TelegramNotifier;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MainMenu {
    public static void main(String[] args) {
        ConfigLoader configLoader = new ConfigLoader();

        // Získání cesty k souboru `config.xml`, který je vedle JAR souboru
        Path currentPath = Paths.get("").toAbsolutePath(); // Aktuální pracovní adresář
        Path configPath = currentPath.resolve("config.xml"); // Cesta ke config.xml

        // Načtení konfigurace
        Config config = configLoader.loadConfig(configPath.toString());
        if (config == null) {
            System.out.println("Nepodařilo se načíst konfiguraci z: " + configPath);
            return;
        }

        DatabaseManager dbManager = new DatabaseManager(config);
        TelegramNotifier telegramNotifier = new TelegramNotifier(config); // Inicializace jako první
        Scraper scraper = new Scraper(config, dbManager, telegramNotifier); // Předání `telegramNotifier`

        MainMenu menu = new MainMenu();
        menu.run(scraper, dbManager, telegramNotifier);
    }

    public void run(Scraper scraper, DatabaseManager dbManager, TelegramNotifier telegramNotifier) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        boolean autoRepeat = false; // Řídí, zda se scraper má automaticky opakovat

        while (true) {
            System.out.println("\n--- Spouštěcí menu ---");
            System.out.println("1. Spustit scraper jednorázově");
            System.out.println("2. Spustit scraper s opakováním");
            System.out.println("3. Promazat tabulku 'inzeraty'");
            System.out.println("4. Konec");
            System.out.print("Vyberte možnost: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Neplatná volba. Zadejte číslo 1, 2, 3 nebo 4.");
                continue;
            }

            switch (choice) {
                case 1: // Spustit scraper jednorázově
                    scraper.run();
                    break;

                case 2: // Spustit scraper s opakováním
                    autoRepeat = true;
                    System.out.println("Spouštím scraper s opakováním. Pro ukončení opakování vyberte možnost 4 (Konec).");
                    while (autoRepeat) {
                        scraper.run();
                        System.out.println("Scraper znovu spuštěn po čekání.");
                    }
                    break;

                case 3: // Promazat tabulku
                    dbManager.clearTable();
                    break;

                case 4: // Ukončení programu nebo opakování
                    if (autoRepeat) {
                        System.out.println("Opakování scraperu bylo ukončeno.");
                        autoRepeat = false; // Zastavení opakování
                    } else {
                        System.out.println("Program ukončen.");
                        scanner.close();
                        return;
                    }
                    break;

                default:
                    System.out.println("Neplatná volba. Zadejte číslo 1, 2, 3 nebo 4.");
            }
        }
    }

}
