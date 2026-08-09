package com.biblia.estudo.model;

public class Highlight {
    private long id;
    private long bookId;
    private int chapter;
    private int verseStart;
    private int verseEnd;
    private String color;
    private long createdAt;
    private String bookName;
    private int testament;
    private String verseText;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }
    public int getChapter() { return chapter; }
    public void setChapter(int chapter) { this.chapter = chapter; }
    public int getVerseStart() { return verseStart; }
    public void setVerseStart(int verseStart) { this.verseStart = verseStart; }
    public int getVerseEnd() { return verseEnd; }
    public void setVerseEnd(int verseEnd) { this.verseEnd = verseEnd; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public int getTestament() { return testament; }
    public void setTestament(int testament) { this.testament = testament; }
    public String getVerseText() { return verseText; }
    public void setVerseText(String verseText) { this.verseText = verseText; }

    public String getReference() {
        return bookName + " " + chapter + ":" + verseStart + (verseEnd > verseStart ? "-" + verseEnd : "");
    }

    public String getTestamentName() {
        switch (testament) {
            case 1: return "Antigo Testamento";
            case 2: return "Novo Testamento";
            case 3: return "Apócrifos";
            default: return "";
        }
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
