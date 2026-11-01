package com.gamequiz;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
public class Etap1Service {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchState matchState;

    // Definicja planszy 4x4
    private final TileStatus[][] plansza = new TileStatus[4][4];

    // NOWE: Baza 16 pytań dla siatki 4x4
    private final Stage1Question[][] bazaPytan = new Stage1Question[4][4];

    // Dane rozgrywki
    private String[] kategorie = {"Kategoria 1", "Kategoria 2", "Kategoria 3", "Kategoria 4"};
    private String aktualnePytanie = "Wybierz kafelek, aby wyświetlić pytanie.";

    // NOWE: Obrazki i dźwięki dla pytania
    private String dzwiekUrl = null;
    private String obrazekUrl = null;
    private boolean obrazekPowiekszony = false;

    private int czasDruzyny1 = 360;
    private boolean czasDruzyny1Running = false;
    private int stealeDruzyna1 = 2;

    private int czasDruzyny2 = 360;
    private boolean czasDruzyny2Running = false;
    private int stealeDruzyna2 = 2;

    private final Set<String> zaliczoneLinie = new HashSet<>();

    public Etap1Service(SimpMessagingTemplate messagingTemplate, MatchState matchState) {
        this.messagingTemplate = messagingTemplate;
        this.matchState = matchState;

        // Inicjalizacja pustej bazy pytań
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                bazaPytan[i][j] = new Stage1Question("Pytanie " + (i * 4 + j + 1), "", "");
            }
        }
        resetujPlansze();
    }

    // Wywołanie przy kliknięciu kafelka z panelu Admina: ładuje treść, obrazek i dźwięk
    public void aktywujKafelek(int wiersz, int kolumna) {
        if (wiersz >= 0 && wiersz < 4 && kolumna >= 0 && kolumna < 4) {
            Stage1Question p = bazaPytan[wiersz][kolumna];
            this.aktualnePytanie = p.getTresc();

            this.obrazekUrl = (p.getObrazek() != null && !p.getObrazek().isBlank())
                    ? "/moje-obrazki/" + p.getObrazek() : null;

            this.dzwiekUrl = (p.getDzwiek() != null && !p.getDzwiek().isBlank())
                    ? "/moje-dzwieki/" + p.getDzwiek() : null;

            this.obrazekPowiekszony = false;
            rozsylajStan();
        }
    }

    // Zapis pojedynczego pytania pod wiersz/kolumnę
    public void zapiszPytanieWBazie(int wiersz, int kolumna, String tresc, String obrazek, String dzwiek) {
        if (wiersz >= 0 && wiersz < 4 && kolumna >= 0 && kolumna < 4) {
            bazaPytan[wiersz][kolumna] = new Stage1Question(tresc, obrazek, dzwiek);
        }
    }

    @Scheduled(fixedRate = 1000)
    public void tickTimery() {
        boolean zmiana = false;

        if (czasDruzyny1Running && czasDruzyny1 > 0) {
            czasDruzyny1--;
            zmiana = true;
        }
        if (czasDruzyny2Running && czasDruzyny2 > 0) {
            czasDruzyny2--;
            zmiana = true;
        }
        if (zmiana) {
            rozsylajStan();
        }
    }

    public void sterujCzasem(int druzyna, String akcja){
        if(druzyna == 1){
            if("start".equals(akcja)) czasDruzyny1Running = true;
            if("stop".equals(akcja)) czasDruzyny1Running = false;
            if("reset".equals(akcja)){
                czasDruzyny1 = 360; czasDruzyny1Running = false;
            }
        }else if(druzyna == 2){
            if("start".equals(akcja)) czasDruzyny2Running = true;
            if("stop".equals(akcja)) czasDruzyny2Running = false;
            if("reset".equals(akcja)){
                czasDruzyny2 = 360; czasDruzyny2Running = false;
            }
        }
        rozsylajStan();
    }

    public void modyfikujSteale(int druzyna, int zmiana) {
        if (druzyna == 1) {
            stealeDruzyna1 = Math.max(0, stealeDruzyna1 + zmiana);
        } else if (druzyna == 2) {
            stealeDruzyna2 = Math.max(0, stealeDruzyna2 + zmiana);
        }
        rozsylajStan();
    }

    public void resetujSteale() {
        this.stealeDruzyna1 = 2;
        this.stealeDruzyna2 = 2;
        rozsylajStan();
    }

    public void naliczPunktyZaCzas(int druzyna) {
        int czas = (druzyna == 1) ? czasDruzyny1 : czasDruzyny2;
        int doNaliczenia = (czas / 15) * 5;
        if (doNaliczenia > 40) {
            doNaliczenia = 40;
        }
        if (doNaliczenia > 0) {
            modyfikujPunkty(druzyna, doNaliczenia);
        }
    }

    // Resetowanie planszy do stanu domyślnego
    public void resetujPlansze() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                plansza[i][j] = TileStatus.DEFAULT;
            }
        }
        aktualnePytanie = "Plansza zresetowana. Wybierz kafelek.";
        dzwiekUrl = null;
        obrazekUrl = null;
        obrazekPowiekszony = false;
        czasDruzyny1 = 360;
        czasDruzyny2 = 360;
        czasDruzyny1Running = false;
        czasDruzyny2Running = false;
        stealeDruzyna1 = 2;
        stealeDruzyna2 = 2;
        zaliczoneLinie.clear();
    }

    // Zmiana stanu wybranego kafelka
    public void ustawKafelek(int wiersz, int kolumna, TileStatus nowyStan) {
        if (wiersz >= 0 && wiersz < 4 && kolumna >= 0 && kolumna < 4) {
            plansza[wiersz][kolumna] = nowyStan;
            sprawdzLinieIPrzyznajBonusy();
        }
    }

    // Dodawanie/odejmowanie punktów
    public void modyfikujPunkty(int druzyna, int ilosc) {
        if (druzyna == 1) matchState.dodajPunktyDruzyny1(ilosc);
        else if (druzyna == 2) matchState.dodajPunktyDruzyny2(ilosc);
        rozsylajStan();
    }

    // --- ALGORYTM WYKRYWANIA POTRÓJNYCH LINII (3 w linii) ---
    private void sprawdzLinieIPrzyznajBonusy() {
        // Wszystkie możliwe kombinacje 3 pól obok siebie w siatce 4x4
        int[][][] trójki = {
                // Poziome (Wiersze)
                {{0,0},{0,1},{0,2}}, {{0,1},{0,2},{0,3}},
                {{1,0},{1,1},{1,2}}, {{1,1},{1,2},{1,3}},
                {{2,0},{2,1},{2,2}}, {{2,1},{2,2},{2,3}},
                {{3,0},{3,1},{3,2}}, {{3,1},{3,2},{3,3}},

                // Pionowe (Kolumny)
                {{0,0},{1,0},{2,0}}, {{1,0},{2,0},{3,0}},
                {{0,1},{1,1},{2,1}}, {{1,1},{2,1},{3,1}},
                {{0,2},{1,2},{2,2}}, {{1,2},{2,2},{3,2}},
                {{0,3},{1,3},{2,3}}, {{1,3},{2,3},{3,3}},

                // Skosy Prawe (\)
                {{0,0},{1,1},{2,2}}, {{1,1},{2,2},{3,3}},
                {{0,1},{1,2},{2,3}}, {{1,0},{2,1},{3,2}},

                // Skosy Lewe (/)
                {{0,3},{1,2},{2,1}}, {{1,2},{2,1},{3,0}},
                {{0,2},{1,1},{2,0}}, {{1,3},{2,2},{3,1}}
        };

        for (int[][] t : trójki) {
            String kluczLinii = t[0][0]+","+t[0][1]+"-"+t[1][0]+","+t[1][1]+"-"+t[2][0]+","+t[2][1];

            if (zaliczoneLinie.contains(kluczLinii)) continue;

            TileStatus s1 = plansza[t[0][0]][t[0][1]];
            TileStatus s2 = plansza[t[1][0]][t[1][1]];
            TileStatus s3 = plansza[t[2][0]][t[2][1]];

            // Sprawdzenie dla Drużyny 1 (BLUE lub STEAL)
            if (isTeam1(s1) && isTeam1(s2) && isTeam1(s3)) {
                zaliczoneLinie.add(kluczLinii);
                matchState.dodajPunktyDruzyny1(5);
            }
            // Sprawdzenie dla Drużyny 2 (RED lub STEAL)
            else if (isTeam2(s1) && isTeam2(s2) && isTeam2(s3)) {
                zaliczoneLinie.add(kluczLinii);
                matchState.dodajPunktyDruzyny2(5);
            }
        }
    }

    private boolean isTeam1(TileStatus s) {
        return s == TileStatus.TEAM1_BLUE || s == TileStatus.TEAM1_STEAL;
    }

    private boolean isTeam2(TileStatus s) {
        return s == TileStatus.TEAM2_RED || s == TileStatus.TEAM2_STEAL;
    }

    public void rozsylajStan() {
        messagingTemplate.convertAndSend("/topic/plansza", getPelnyStan());
    }

    // Zbiorczy obiekt stanu gry wysyłany przez WebSocket
    public FullGameState getPelnyStan() {
        return new FullGameState(
                plansza, kategorie, aktualnePytanie,
                dzwiekUrl, obrazekUrl, obrazekPowiekszony,
                matchState.getNazwaDruzyny1(), matchState.getGraczeDruzyny1(), matchState.getPunktyDruzyny1(), czasDruzyny1, czasDruzyny1Running, stealeDruzyna1,
                matchState.getNazwaDruzyny2(), matchState.getGraczeDruzyny2(), matchState.getPunktyDruzyny2(), czasDruzyny2, czasDruzyny2Running, stealeDruzyna2,
                new ArrayList<>(zaliczoneLinie)
        );
    }

    public Stage1Question[][] getBazaPytan() { return bazaPytan; }
    public void setAktualnePytanie(String pytanie) { this.aktualnePytanie = pytanie; }
    public void setKategorie(String[] kategorie) { this.kategorie = kategorie; }
    public void setDzwiekUrl(String dzwiekUrl) { this.dzwiekUrl = dzwiekUrl; }
    public void setObrazekUrl(String obrazekUrl) { this.obrazekUrl = obrazekUrl; }
    public void setObrazekPowiekszony(boolean obrazekPowiekszony) { this.obrazekPowiekszony = obrazekPowiekszony; }
}