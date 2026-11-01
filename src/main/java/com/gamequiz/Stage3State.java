package com.gamequiz;

import java.util.*;

public class Stage3State {
    private String nazwaDruzyny1;
    private List<String> uczestnicyDruzyny1;
    private int punktyDruzyny1;
    private int mnoznikDruzyny1 = 15;
    private boolean koloUsedDruzyna1 = false;

    private String nazwaDruzyny2;
    private List<String> uczestnicyDruzyny2;
    private int punktyDruzyny2;
    private int mnoznikDruzyny2 = 15;
    private boolean koloUsedDruzyna2 = false;

    private List<Karta> pulaKart = new ArrayList<>();
    private List<Karta> kartyNaStole = new ArrayList<>();

    private Karta wybranePytanie = null;
    private boolean pokazPytanie = false;
    private Set<String> ukryteOdpowiedzi = new HashSet<>();
    public Stage3State(MatchState mecz){
        odswiezDaneMeczu(mecz);
        inicjalizujPuleKart();
        losujKartyNaStart();
    }

    public void odswiezDaneMeczu(MatchState mecz){
        this.nazwaDruzyny1 = mecz.getNazwaDruzyny1();
        this.uczestnicyDruzyny1 = mecz.getGraczeDruzyny1();
        this.punktyDruzyny1 = mecz.getPunktyDruzyny1();

        this.nazwaDruzyny2 = mecz.getNazwaDruzyny2();
        this.uczestnicyDruzyny2 = mecz.getGraczeDruzyny2();
        this.punktyDruzyny2 = mecz.getPunktyDruzyny2();
    }

    private void inicjalizujPuleKart() {
        pulaKart.add(new Karta(1, "Trylogie", "Jak nazywa się trylogia gier od CD Projekt RED?", "A", "Gothic", "Wiedźmin", "Cyberpunk", "Mass Effect"));
        pulaKart.add(new Karta(2, "Gran Turismo", "Kto jest twórcą serii Gran Turismo?", "B", "Hideo Kojima", "Kazunori Yamauchi", "Shigeru Miyamoto", "Gabe Newell"));
        pulaKart.add(new Karta(3, "Scooby-Doo", "Jak nazywa się pojazd brygady detektywów?", "C", "Mystery Ship", "Ghost Bus", "Wehikuł Tajemnic", "Scooby Van"));
        pulaKart.add(new Karta(4, "Uciekinierzy", "W jakiej grze uciekamy z więzienia w trybie co-op?", "A", "A Way Out", "Prison Architect", "Escapists", "Payday"));
        pulaKart.add(new Karta(5, "Kangurek Kao", "Jaki element stroju jest symbolem Kao?", "D", "Czapka", "Buty", "Szalik", "Czerwone Rękawice"));
        pulaKart.add(new Karta(6, "Hotline Miami", "W jakim mieście toczy się akcja gry?", "B", "New York", "Miami", "Los Angeles", "Chicago"));
        pulaKart.add(new Karta(7, "Zmiany pogody", "W której grze dynamiczna pogoda zmieniała mapę w 2013r?", "C", "COD Ghosts", "GTA V", "Battlefield 4", "Crysis 3"));
        pulaKart.add(new Karta(8, "Killing Floor", "Przeciwko komu walczymy w Killing Floor?", "A", "Zedom", "Zombie", "Mutantom", "Ksenomorfom"));
        pulaKart.add(new Karta(9, "Ratowanie świata", "Jak nazywa się płatny tryb PvE w Fortnite?", "B", "Battle Royale", "Ratowanie Świata", "Creative", "Save Earth"));
        pulaKart.add(new Karta(10, "Niszczenie świata", "W jakiej grze wcielamy się w kosmitę niszczącego Ziemię?", "C", "Red Faction", "Mercenaries", "Destroy All Humans!", "Rampage"));
        pulaKart.add(new Karta(11, "Cyberpunk", "Jak nazywa się główny bohater Cyberpunk 2077?", "A", "V", "Johnny", "Jackie", "David"));
        pulaKart.add(new Karta(12, "Soulsborne", "Która gra NIE została stworzona przez FromSoftware?", "D", "Demon's Souls", "Bloodborne", "Elden Ring", "Lies of P"));
        pulaKart.add(new Karta(13, "Pikselowa Przygoda", "Jak nazywa się twórca gry Minecraft?", "B", "Jeb_", "Notch", "Dinnerbone", "C418"));
        pulaKart.add(new Karta(14, "Kosmiczny horror", "Na jakim statku toczy się akcja Dead Space?", "A", "USG Ishimura", "SSV Normandy", "Nostromo", "Pillar of Autumn"));
        pulaKart.add(new Karta(15, "Szybcy i wściekli", "Która seria słynie z nielegalnych wyścigów ulicznych?", "C", "Forza Horizon", "F1", "Need for Speed", "GRID"));
    }

    public void losujKartyNaStart() {
        Collections.shuffle(pulaKart);
        kartyNaStole = new ArrayList<>(pulaKart.subList(0, 10));
    }

    public void wymienKarte(int idKarty) {
        Karta doUsuniecia = null;
        for (Karta k : kartyNaStole) {
            if (k.getId() == idKarty) {
                doUsuniecia = k;
                break;
            }
        }
        if (doUsuniecia != null) {
            int index = kartyNaStole.indexOf(doUsuniecia);
            kartyNaStole.remove(doUsuniecia);
            pulaKart.remove(doUsuniecia);
            for (Karta p : pulaKart) {
                if (!kartyNaStole.contains(p)) {
                    kartyNaStole.add(index, p);
                    break;
                }
            }
        }
    }

    public void uzyjKolo5050(int druzyna) {
        if (wybranePytanie == null) return;
        if (druzyna == 1 && koloUsedDruzyna1) return;
        if (druzyna == 2 && koloUsedDruzyna2) return;

        if (druzyna == 1) koloUsedDruzyna1 = true;
        if (druzyna == 2) koloUsedDruzyna2 = true;

        ukryteOdpowiedzi.clear();
        List<String> bledne = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        bledne.remove(wybranePytanie.getPoprawna());
        Collections.shuffle(bledne);

        ukryteOdpowiedzi.add(bledne.get(0));
        ukryteOdpowiedzi.add(bledne.get(1));
    }

    public static class Karta {
        private int id;
        private String kategoria;
        private String trescPytania;
        private String poprawna;
        private String odpA, odpB, odpC, odpD;
        private boolean zuzyta = false;

        // Stan karty po rozstrzygnięciu odpowiedzi: DEFAULT, RED-CORRECT, RED-WRONG, BLUE-CORRECT, BLUE-WRONG
        private String stanRozstrzygniecia = "DEFAULT";

        public Karta(int id, String kategoria, String trescPytania, String poprawna, String odpA, String odpB, String odpC, String odpD) {
            this.id = id;
            this.kategoria = kategoria;
            this.trescPytania = trescPytania;
            this.poprawna = poprawna;
            this.odpA = odpA;
            this.odpB = odpB;
            this.odpC = odpC;
            this.odpD = odpD;
        }

        public int getId() { return id; }
        public String getKategoria() { return kategoria; }
        public String getTrescPytania() { return trescPytania; }
        public String getPoprawna() { return poprawna; }
        public String getOdpA() { return odpA; }
        public String getOdpB() { return odpB; }
        public String getOdpC() { return odpC; }
        public String getOdpD() { return odpD; }
        public boolean isZuzyta() { return zuzyta; }
        public void setZuzyta(boolean zuzyta) { this.zuzyta = zuzyta; }

        public String getStanRozstrzygniecia() { return stanRozstrzygniecia; }
        public void setStanRozstrzygniecia(String stanRozstrzygniecia) { this.stanRozstrzygniecia = stanRozstrzygniecia; }
    }

    public String getNazwaDruzyny1() { return nazwaDruzyny1; }
    public int getPunktyDruzyny1() { return punktyDruzyny1; }
    public int getMnoznikDruzyny1() { return mnoznikDruzyny1; }
    public void setMnoznikDruzyny1(int m) { this.mnoznikDruzyny1 = m; }
    public boolean isKoloUsedDruzyna1() { return koloUsedDruzyna1; }

    public String getNazwaDruzyny2() { return nazwaDruzyny2; }
    public int getPunktyDruzyny2() { return punktyDruzyny2; }
    public int getMnoznikDruzyny2() { return mnoznikDruzyny2; }
    public void setMnoznikDruzyny2(int m) { this.mnoznikDruzyny2 = m; }
    public boolean isKoloUsedDruzyna2() { return koloUsedDruzyna2; }

    public List<Karta> getKartyNaStole() { return kartyNaStole; }
    public Karta getWybranePytanie() { return wybranePytanie; }
    public void setWybranePytanie(Karta wybranePytanie) { this.wybranePytanie = wybranePytanie; }
    public boolean isPokazPytanie() { return pokazPytanie; }
    public void setPokazPytanie(boolean pokazPytanie) { this.pokazPytanie = pokazPytanie; }
    public Set<String> getUkryteOdpowiedzi() { return ukryteOdpowiedzi; }
    public List<String> getUczestniciDruzyny1() { return uczestnicyDruzyny1; }
    public List<String> getUczestniciDruzyny2() { return uczestnicyDruzyny2; }
}