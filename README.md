# Bazoš Scraper

Jednoduchý Java nástroj pro automatické sledování inzerátů na [bazos.cz](https://www.bazos.cz/) a zasílání notifikací přes Telegram.

## Funkce

- Prohledává inzeráty podle klíčových slov, lokality a cenového rozsahu
- Ukládá nalezené inzeráty do SQLite databáze
- Posílá oznámení o nových inzerátech přes Telegram
- Jednoduché textové rozhraní pro spouštění jednotlivých vyhledávání

## Struktura projektu
src/
└── main/
└── java/
└── api.bazos.app/
├── Scraper.java # Hlavní třída pro scraping
├── SearchConfig.java # Konfigurace pro hledání
├── config/
│ ├── Config.java # Globální konfigurace
│ └── ConfigLoader.java # Načítání JSON konfigurace
├── db/
│ └── DatabaseManager.java # SQLite databáze
├── notifiaction/
│ └── TelegramNotifier.java # Telegram boti 
└── ui/
└── MainMenu.java # Textové rozhraní

## ⚙️ Konfigurace (`config.xml`)

Ukázka souboru `config.xml`, který definuje chování programu:

<?xml version="1.0" encoding="UTF-8"?>
<config>
    <!-- Hledané fráze, mezery nahradit "+" -->
    <searches>
        <search>
            <phrase>playstation+5</phrase>
            <minPrice>4799</minPrice>
            <maxPrice>6001</maxPrice>
            <blockedWords>mechanika</blockedWords>
        </search>
    </searches>

    <!-- Databázové připojení -->
    <dbUrl>jdbc:mysql://localhost:3306/bazos</dbUrl>
    <dbUser>root</dbUser>
    <dbPassword></dbPassword>

    <!-- Časová mezera mezi kontrolami -->
    <waitTimeMinutes>10</waitTimeMinutes>

    <!-- Telegram notifikace -->
    <telegramNotifications>true</telegramNotifications>
    <telegramToken>8100700498:AAE7DhEBG0vwsdYRVOoL2F2xBLQnuUXUU30</telegramToken>
    <telegramChatId>6096518663</telegramChatId>
</config>


