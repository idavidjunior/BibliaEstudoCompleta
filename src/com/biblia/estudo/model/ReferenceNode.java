package com.biblia.estudo.model;

import java.io.Serializable;

public class ReferenceNode implements Serializable {
    private long id;
    private long mapId;
    private long bookId;
    private String bookName;
    private int chapter;
    private int verse;
    private String verseText;
    private String translation;
    private String color;
    private String note;
    private float positionX;
    private float positionY;
    private long createdAt;
    private long updatedAt;
    private int connectionCount;
    private boolean isExpanded;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMapId() { return mapId; }
    public void setMapId(long mapId) { this.mapId = mapId; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public int getChapter() { return chapter; }
    public void setChapter(int chapter) { this.chapter = chapter; }

    public int getVerse() { return verse; }
    public void setVerse(int verse) { this.verse = verse; }

    public String getVerseText() { return verseText; }
    public void setVerseText(String verseText) { this.verseText = verseText; }

    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public float getPositionX() { return positionX; }
    public void setPositionX(float positionX) { this.positionX = positionX; }

    public float getPositionY() { return positionY; }
    public void setPositionY(float positionY) { this.positionY = positionY; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getConnectionCount() { return connectionCount; }
    public void setConnectionCount(int connectionCount) { this.connectionCount = connectionCount; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public String getReference() {
        return bookName + " " + chapter + ":" + verse;
    }

    public String getColorName() {
        if (color == null) return "Sem cor";
        switch (color) {
            case "#FFF9C4": return "Amarelo";
            case "#FFCDD2": return "Vermelho";
            case "#C8E6C9": return "Verde";
            case "#BBDEFB": return "Azul";
            case "#E1BEE7": return "Roxo";
            case "#FFE0B2": return "Laranja";
            default: return "Personalizada";
        }
    }
}