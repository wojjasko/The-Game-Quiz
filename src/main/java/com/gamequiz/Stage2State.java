package com.gamequiz;

import java.util.ArrayList;
import java.util.List;

public class Stage2State {
    private String nazwaDruzyny1;
    private List<String> uczestnicyDruzyny1;
    private int punktyDruzyny1;

    private String nazwaDruzyny2;
    private List<String> uczestnicyDruzyny2;
    private int punktyDruzyny2;
    private int wybranePytanie = 1;

    // Dla każdego z 8 pytań pamiętamy ścieżki i stan odsłonięcia 3 obrazków
    private List<PytanieGra> pytania = new ArrayList<>();

    // Obrazek obecnie wyśrodkowany (np. "1_2" -> pytanie 1, obrazek 2; null jeśli żaden)
    private String aktywnyObrazekId = null;

    public Stage2State(MatchState mecz) {
        odswiezDaneMeczu(mecz);

        // Generujemy 8 pytań po 3 obrazki
        // Zakładamy, że w Twoim folderze pliki nazywają się np. p1_1.jpg, p1_2.jpg itd.
        for (int i = 1; i <= 8; i++) {
            PytanieGra p = new PytanieGra(i);
            p.getObrazki().add(new ObrazekState("img_" + i + "_1", "/moje-obrazki/p" + i + "_1.jpg", false));
            p.getObrazki().add(new ObrazekState("img_" + i + "_2", "/moje-obrazki/p" + i + "_2.jpg", false));
            p.getObrazki().add(new ObrazekState("img_" + i + "_3", "/moje-obrazki/p" + i + "_3.jpg", false));
            pytania.add(p);
        }
    }

    public void odswiezDaneMeczu(MatchState mecz) {
        this.nazwaDruzyny1 = mecz.getNazwaDruzyny1();
        this.uczestnicyDruzyny1 = mecz.getGraczeDruzyny1();
        this.punktyDruzyny1 = mecz.getPunktyDruzyny1();

        this.nazwaDruzyny2 = mecz.getNazwaDruzyny2();
        this.uczestnicyDruzyny2 = mecz.getGraczeDruzyny2();
        this.punktyDruzyny2 = mecz.getPunktyDruzyny2();
    }

    // Pomocnicze klasy do opisu stanu
    public static class PytanieGra {
        private int id;
        private String stanWlasnosci = "DEFAULT";
        private List<ObrazekState> obrazki = new ArrayList<>();

        public PytanieGra(int id) { this.id = id; }
        public int getId() { return id; }
        public List<ObrazekState> getObrazki() { return obrazki; }

        public String getStanWlasnosci() { return stanWlasnosci; }
        public void setStanWlasnosci(String stanWlasnosci) { this.stanWlasnosci = stanWlasnosci; }
    }

    public static class ObrazekState {
        private String id;
        private String url;
        private boolean odsloniety;

        public ObrazekState(String id, String url, boolean odsloniety) {
            this.id = id;
            this.url = url;
            this.odsloniety = odsloniety;
        }
        public String getId() { return id; }
        public String getUrl() { return url; }
        public boolean isOdsloniety() { return odsloniety; }
        public void setOdsloniety(boolean odsloniety) { this.odsloniety = odsloniety; }
    }

    // Gettery i Settery
    public String getNazwaDruzyny1() { return nazwaDruzyny1; }
    public int getPunktyDruzyny1() { return punktyDruzyny1; }
    public List<String> getUczestniciDruzyny1() { return uczestnicyDruzyny1; }
    public String getNazwaDruzyny2() { return nazwaDruzyny2; }
    public int getPunktyDruzyny2() { return punktyDruzyny2; }
    public List<String> getUczestniciDruzyny2() { return uczestnicyDruzyny2; }
    public int getWybranePytanie() { return wybranePytanie; }
    public void setWybranePytanie(int wybranePytanie) { this.wybranePytanie = wybranePytanie; }
    public List<PytanieGra> getPytania() { return pytania; }
    public String getAktywnyObrazekId() { return aktywnyObrazekId; }
    public void setAktywnyObrazekId(String aktywnyObrazekId) { this.aktywnyObrazekId = aktywnyObrazekId; }
}