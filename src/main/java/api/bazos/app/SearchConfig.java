package api.bazos.app;

import java.util.List;

public class SearchConfig {
    private String phrase;
    private double minPrice;
    private double maxPrice;
    private List<String> blockedWords;

    // Gettery a settery
    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<String> getBlockedWords() {
        return blockedWords;
    }

    public void setBlockedWords(List<String> blockedWords) {
        this.blockedWords = blockedWords;
    }
}