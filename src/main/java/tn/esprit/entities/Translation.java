package tn.esprit.entities;

import java.time.LocalDateTime;

public class Translation {

    private Integer id;
    private String contentHash;
    private String itemType;
    private Integer itemId;
    private String sourceLang;
    private String targetLang;
    private String originalText;
    private String translatedText;
    private String provider;
    private LocalDateTime createdAt;

    public Translation() {
    }

    public Translation(Integer id, String contentHash, String itemType, Integer itemId, String sourceLang, String targetLang, String originalText, String translatedText, String provider, LocalDateTime createdAt) {
        this.id = id;
        this.contentHash = contentHash;
        this.itemType = itemType;
        this.itemId = itemId;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.provider = provider;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Translation{" +
                "id=" + id + ", " + "contentHash=" + contentHash + ", " + "itemType=" + itemType + ", " + "itemId=" + itemId + ", " + "sourceLang=" + sourceLang + ", " + "targetLang=" + targetLang + ", " + "originalText=" + originalText + ", " + "translatedText=" + translatedText + ", " + "provider=" + provider + ", " + "createdAt=" + createdAt +
                "}";
    }
}
