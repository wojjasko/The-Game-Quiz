package com.gamequiz;

import java.util.List;

public record FullGameState(
        TileStatus[][] plansza,
        String[] kategorie,
        String aktualnePytanie,
        String dzwiekUrl,
        String obrazekUrl,
        boolean obrazekPowiekszony,
        String nazwaDruzyny1,
        List<String> uczestnicyDruzyny1,
        int punktyDruzyny1,
        int czasDruzyny1,
        boolean czasDruzyny1Running,
        int stealeDruzyna1,
        String nazwaDruzyny2,
        List<String> uczestnicyDruzyny2,
        int punktyDruzyny2,
        int czasDruzyny2,
        boolean czasDruzyny2Running,
        int stealeDruzyna2,
        List<String> wygrywajaceLonie // Przesyłamy identyfikatory zaliczonych trójek (np. "0,0-0,1-0,2")
) {}