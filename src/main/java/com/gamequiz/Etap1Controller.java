package com.gamequiz;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plansza")
@CrossOrigin(origins = "*") // pozwala na laczenie sie z roznych adresow
public class Etap1Controller {
    private final Etap1Service serwis;
    private final MatchState matchState;

    // Spring automatycznie wstrzyknie tutaj nasz serwis dzięki adnotacji @Autowired / konstruktorowi
    public Etap1Controller(Etap1Service serwis, MatchState matchState) {
        this.serwis = serwis;
        this.matchState = matchState;
    }
    @GetMapping
    public FullGameState pobierzStan() {
        return serwis.getPelnyStan();
    }

    // Kliknięcie kafelka -> Aktywacja pytania + zmiana stanu kafelka (np. na SELECTED_WHITE lub po prostu wybranie)
    @GetMapping("/aktywuj-kafelek")
    public String aktywujKafelek(@RequestParam int wiersz, @RequestParam int kolumna) {
        serwis.aktywujKafelek(wiersz, kolumna);
        return "OK";
    }

    // Zapis pytania z Panelu Admina do bazy (dla konkretnego wiersza i kolumny)
    @PostMapping("/zapisz-pytanie")
    public String zapiszPytanie(@RequestParam int wiersz, @RequestParam int kolumna,
                                @RequestParam String tresc,
                                @RequestParam(required = false) String obrazek,
                                @RequestParam(required = false) String dzwiek) {
        serwis.zapiszPytanieWBazie(wiersz, kolumna, tresc, obrazek, dzwiek);
        return "Zapisano";
    }

    // Pobranie całej bazy 16 pytań do formularzy w panelu admina
    @GetMapping("/baza-pytan")
    public Stage1Question[][] pobierzBazePytan() {
        return serwis.getBazaPytan();
    }

    @GetMapping("/kliknij")
    public String kliknijKafelek(@RequestParam int wiersz, @RequestParam int kolumna, @RequestParam TileStatus stan) {
        serwis.ustawKafelek(wiersz, kolumna, stan);
        serwis.rozsylajStan();
        return "OK";
    }

    @GetMapping("/punkty")
    public String zmienPunkty(@RequestParam int druzyna, @RequestParam int ilosc) {
        serwis.modyfikujPunkty(druzyna, ilosc);
        serwis.rozsylajStan();
        return "OK";
    }

    @GetMapping("/steale/odejmij")
    public String odejmijSteal(@RequestParam int druzyna) {
        serwis.modyfikujSteale(druzyna, -1);
        return "OK";
    }

    @GetMapping("/steale/reset")
    public String resetujSteale() {
        serwis.resetujSteale();
        return "OK";
    }

    @GetMapping("/punkty-za-czas")
    public String naliczPunktyZaCzas(@RequestParam int druzyna) {
        serwis.naliczPunktyZaCzas(druzyna);
        return "OK";
    }

    // Triggger powiększania / pomniejszania obrazka w pytaniu (tak samo jak w Etapie 2)
    @GetMapping("/ustaw-pytanie")
    public String ustawPytanie(@RequestParam(required = false, defaultValue = "") String tekst,
                               @RequestParam(required = false) String dzwiek) {
        serwis.setAktualnePytanie(tekst);
        if (dzwiek != null) {
            serwis.setDzwiekUrl(dzwiek.isBlank() ? null : "/moje-dzwieki/" + dzwiek);
        }
        serwis.rozsylajStan();
        return "OK";
    }

    @RequestMapping(value = "/obrazek/trigger", method = {RequestMethod.GET, RequestMethod.POST})
    public String toggleObrazek(@RequestParam boolean wlacz) {
        serwis.setObrazekPowiekszony(wlacz);
        serwis.rozsylajStan();
        return "OK";
    }

    @PostMapping("/aktualizuj-ustawienia")
    public String aktualizujUstawienia(@RequestBody FullGameState noweUstawienia) {
        matchState.setNazwaDruzyny1(noweUstawienia.nazwaDruzyny1());
        matchState.setNazwaDruzyny2(noweUstawienia.nazwaDruzyny2());
        matchState.setGraczeDruzyny1(noweUstawienia.uczestnicyDruzyny1());
        matchState.setGraczeDruzyny2(noweUstawienia.uczestnicyDruzyny2());
        serwis.setKategorie(noweUstawienia.kategorie());
        serwis.rozsylajStan();
        return "Zaktualizowano";
    }

    // Resetowanie planszy za pomocą GET (wygodne do testów z przeglądarki i panelu)
    @GetMapping("/reset")
    public String resetuj() {
        serwis.resetujPlansze();
        serwis.rozsylajStan();
        return "Zresetowano";
    }

    @GetMapping("/timer")
    public String sterujTimerem(@RequestParam int druzyna, @RequestParam String akcja) {
        serwis.sterujCzasem(druzyna, akcja);
        return "OK";
    }

}
