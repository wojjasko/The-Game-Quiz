package com.gamequiz;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchState {
    private String nazwaDruzyny1 = "KANGURY Z KOSMOSU";
    private List<String> graczeDruzyny1 = new ArrayList<>(List.of("Gracz N1", "Gracz N2", "Gracz N3"));

    private String nazwaDruzyny2 = "BUNIE";
    private List<String> graczeDruzyny2 = new ArrayList<>(List.of("Gracz C1", "Gracz C2", "Gracz C3"));

    private int punktyDruzyny1 = 0;
    private int punktyDruzyny2 = 0;


    public String getNazwaDruzyny1() { return nazwaDruzyny1; }
    public void setNazwaDruzyny1(String nazwaDruzyny1) { this.nazwaDruzyny1 = nazwaDruzyny1; }

    public List<String> getGraczeDruzyny1() { return graczeDruzyny1; }
    public void setGraczeDruzyny1(List<String> graczeDruzyny1) { this.graczeDruzyny1 = graczeDruzyny1; }

    public String getNazwaDruzyny2() { return nazwaDruzyny2; }
    public void setNazwaDruzyny2(String nazwaDruzyny2) { this.nazwaDruzyny2 = nazwaDruzyny2; }

    public List<String> getGraczeDruzyny2() { return graczeDruzyny2; }
    public void setGraczeDruzyny2(List<String> graczeDruzyny2) { this.graczeDruzyny2 = graczeDruzyny2; }

    public int getPunktyDruzyny1() { return punktyDruzyny1; }
    public void setPunktyDruzyny1(int punkty) { this.punktyDruzyny1 = punkty; }
    public void dodajPunktyDruzyny1(int pkt) { this.punktyDruzyny1 += pkt; }

    public int getPunktyDruzyny2() { return punktyDruzyny2; }
    public void setPunktyDruzyny2(int punkty) { this.punktyDruzyny2 = punkty; }
    public void dodajPunktyDruzyny2(int pkt) { this.punktyDruzyny2 += pkt; }
}