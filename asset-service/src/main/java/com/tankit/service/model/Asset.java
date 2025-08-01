package com.tankit.service.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Asset domain model representing financial instruments
 */
public class Asset {
    private String assetId;
    private String name;
    private String description;
    private String cusip;
    private String bloombergId;
    private String isin;
    private String sedol;
    private Instant createdTimestamp;
    private Double marketValue;
    private String currency;

    public Asset() {
        this.createdTimestamp = Instant.now();
        this.currency = "USD";
    }

    public Asset(String assetId, String name, String description, String cusip, String bloombergId) {
        this();
        this.assetId = assetId;
        this.name = name;
        this.description = description;
        this.cusip = cusip;
        this.bloombergId = bloombergId;
    }

    // Getters and Setters
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCusip() { return cusip; }
    public void setCusip(String cusip) { this.cusip = cusip; }

    public String getBloombergId() { return bloombergId; }
    public void setBloombergId(String bloombergId) { this.bloombergId = bloombergId; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getSedol() { return sedol; }
    public void setSedol(String sedol) { this.sedol = sedol; }

    public Instant getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Instant createdTimestamp) { this.createdTimestamp = createdTimestamp; }

    public Double getMarketValue() { return marketValue; }
    public void setMarketValue(Double marketValue) { this.marketValue = marketValue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return Objects.equals(assetId, asset.assetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId);
    }

    @Override
    public String toString() {
        return "Asset{" +
                "assetId='" + assetId + '\'' +
                ", name='" + name + '\'' +
                ", cusip='" + cusip + '\'' +
                ", bloombergId='" + bloombergId + '\'' +
                ", marketValue=" + marketValue +
                '}';
    }
}