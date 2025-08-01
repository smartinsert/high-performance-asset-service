package com.tankit.service.data;

import com.tankit.service.model.Asset;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates mock asset data for the prototype
 */
@Component
public class AssetDataGenerator {

    private static final String[] ASSET_TYPES = {"EQUITY", "BOND", "COMMODITY", "CURRENCY", "DERIVATIVE"};
    private static final String[] COMPANIES = {
        "Apple Inc", "Microsoft Corp", "Amazon.com Inc", "Alphabet Inc", "Tesla Inc",
        "Meta Platforms Inc", "NVIDIA Corp", "Berkshire Hathaway", "Johnson & Johnson", "JPMorgan Chase",
        "Visa Inc", "Walmart Inc", "Procter & Gamble", "UnitedHealth Group", "Mastercard Inc",
        "Home Depot Inc", "Bank of America", "Pfizer Inc", "Coca-Cola Co", "Intel Corp"
    };
    
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF"};
    
    private final SecureRandom random = new SecureRandom();

    /**
     * Generate a list of mock assets
     */
    public List<Asset> generateAssets(int count) {
        List<Asset> assets = new ArrayList<>(count);
        
        for (int i = 1; i <= count; i++) {
            Asset asset = new Asset();
            asset.setAssetId(String.format("ASSET_%06d", i));
            asset.setName(generateAssetName());
            asset.setDescription(generateDescription());
            asset.setCusip(generateCusip());
            asset.setBloombergId(generateBloombergId());
            asset.setIsin(generateIsin());
            asset.setSedol(generateSedol());
            asset.setCreatedTimestamp(generateRandomTimestamp());
            asset.setMarketValue(generateMarketValue());
            asset.setCurrency(getRandomCurrency());
            
            assets.add(asset);
        }
        
        return assets;
    }

    private String generateAssetName() {
        String company = COMPANIES[random.nextInt(COMPANIES.length)];
        String type = ASSET_TYPES[random.nextInt(ASSET_TYPES.length)];
        return company + " " + type;
    }

    private String generateDescription() {
        return "Financial instrument representing " + 
               ASSET_TYPES[random.nextInt(ASSET_TYPES.length)].toLowerCase() + 
               " security";
    }

    /**
     * Generate CUSIP (Committee on Uniform Securities Identification Procedures)
     * Format: 9 characters (8 alphanumeric + 1 check digit)
     */
    private String generateCusip() {
        StringBuilder cusip = new StringBuilder();
        
        // First 6 characters: issuer identifier
        for (int i = 0; i < 6; i++) {
            cusip.append(getRandomAlphanumeric());
        }
        
        // Next 2 characters: issue identifier
        for (int i = 0; i < 2; i++) {
            cusip.append(getRandomAlphanumeric());
        }
        
        // Check digit (simplified - normally calculated)
        cusip.append(random.nextInt(10));
        
        return cusip.toString();
    }

    /**
     * Generate Bloomberg ID
     * Format: Ticker + Exchange code
     */
    private String generateBloombergId() {
        String ticker = generateTicker();
        String[] exchanges = {"US", "LN", "JP", "GR", "FP"};
        String exchange = exchanges[random.nextInt(exchanges.length)];
        return ticker + " " + exchange + " Equity";
    }

    private String generateTicker() {
        StringBuilder ticker = new StringBuilder();
        int length = 2 + random.nextInt(4); // 2-5 characters
        
        for (int i = 0; i < length; i++) {
            ticker.append((char) ('A' + random.nextInt(26)));
        }
        
        return ticker.toString();
    }

    /**
     * Generate ISIN (International Securities Identification Number)
     * Format: 2-letter country code + 9 alphanumeric + 1 check digit
     */
    private String generateIsin() {
        StringBuilder isin = new StringBuilder();
        
        // Country code
        String[] countries = {"US", "GB", "DE", "FR", "JP", "CA"};
        isin.append(countries[random.nextInt(countries.length)]);
        
        // 9 alphanumeric characters
        for (int i = 0; i < 9; i++) {
            isin.append(getRandomAlphanumeric());
        }
        
        // Check digit (simplified)
        isin.append(random.nextInt(10));
        
        return isin.toString();
    }

    /**
     * Generate SEDOL (Stock Exchange Daily Official List)
     * Format: 6 alphanumeric characters + 1 check digit
     */
    private String generateSedol() {
        StringBuilder sedol = new StringBuilder();
        
        // 6 alphanumeric characters
        for (int i = 0; i < 6; i++) {
            sedol.append(getRandomAlphanumeric());
        }
        
        // Check digit (simplified)
        sedol.append(random.nextInt(10));
        
        return sedol.toString();
    }

    private char getRandomAlphanumeric() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return chars.charAt(random.nextInt(chars.length()));
    }

    private Instant generateRandomTimestamp() {
        // Random timestamp within the last year
        Instant now = Instant.now();
        long randomDays = ThreadLocalRandom.current().nextLong(0, 365);
        return now.minus(randomDays, ChronoUnit.DAYS);
    }

    private Double generateMarketValue() {
        // Random market value between $1 and $10,000
        return 1.0 + (random.nextDouble() * 9999.0);
    }

    private String getRandomCurrency() {
        return CURRENCIES[random.nextInt(CURRENCIES.length)];
    }
}