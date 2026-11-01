package com.gamequiz;

public class Stage1Question {
    private String tresc = "Domyślne pytanie";
    private String obrazek = ""; // nazwa pliku z folderu images_e1, np. "p1.jpg"
    private String dzwiek = "";  // nazwa pliku z folderu sounds, np. "d1.mp3"

    public Stage1Question() {}

    public Stage1Question(String tresc, String obrazek, String dzwiek) {
        this.tresc = tresc;
        this.obrazek = obrazek;
        this.dzwiek = dzwiek;
    }

    public String getTresc() { return tresc; }
    public void setTresc(String tresc) { this.tresc = tresc; }

    public String getObrazek() { return obrazek; }
    public void setObrazek(String obrazek) { this.obrazek = obrazek; }

    public String getDzwiek() { return dzwiek; }
    public void setDzwiek(String dzwiek) { this.dzwiek = dzwiek; }
}