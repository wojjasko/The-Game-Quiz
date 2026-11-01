package com.gamequiz;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etap2")
@CrossOrigin(origins = "*")
public class Etap2Controller {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchState matchState;
    private final Stage2State stan;

    public Etap2Controller(SimpMessagingTemplate messagingTemplate, MatchState matchState) {
        this.messagingTemplate = messagingTemplate;
        this.matchState = matchState;
        this.stan = new Stage2State(matchState);
    }

    @GetMapping("/stan")
    public Stage2State getStan() {
        stan.odswiezDaneMeczu(matchState);
        return stan;
    }

    // Wybór pytania (1-8)
    @RequestMapping(value = "/pytanie/{nr}", method = {RequestMethod.GET, RequestMethod.POST})
    public void wybierzPytanie(@PathVariable int nr) {
        stan.setWybranePytanie(nr);
        stan.setAktywnyObrazekId(null);
        wyslijStan();
    }

    // Akcja Admina: Włącz / Wyłącz powiększenie obrazka
    @RequestMapping(value = "/obrazek/trigger", method = {RequestMethod.GET, RequestMethod.POST})
    public void toggleObrazek(@RequestParam int nrPytania, @RequestParam int indexObrazka, @RequestParam boolean wlacz) {
        try {
            // Bezpośredni dostęp po indeksie wysłanym z admina (0, 1 lub 2)
            Stage2State.ObrazekState img = stan.getPytania().get(nrPytania - 1).getObrazki().get(indexObrazka);

            if (wlacz) {
                stan.setAktywnyObrazekId(img.getId());
                img.setOdsloniety(true);
            } else {
                stan.setAktywnyObrazekId(null);
            }

            System.out.println("Akcja: Pytanie " + nrPytania + ", Obrazek " + (indexObrazka + 1) + ", Stan: " + wlacz);
            wyslijStan();
        } catch (Exception e) {
            System.err.println("Błąd podczas przełączania obrazka: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/punkty", method = {RequestMethod.GET, RequestMethod.POST})
    public void zmienPunkty(@RequestParam int druzyna, @RequestParam int ilosc) {
        if (druzyna == 1) {
            matchState.dodajPunktyDruzyny1(ilosc);
        } else if (druzyna == 2) {
            matchState.dodajPunktyDruzyny2(ilosc);
        }
        wyslijStan();
    }

    @RequestMapping(value = "/punkty/ustaw", method = {RequestMethod.GET, RequestMethod.POST})
    public void ustawPunkty(@RequestParam int druzyna, @RequestParam int punkty) {
        if (druzyna == 1) {
            matchState.setPunktyDruzyny1(punkty);
        } else if (druzyna == 2) {
            matchState.setPunktyDruzyny2(punkty);
        }
        wyslijStan();
    }

    @RequestMapping(value = "/pytanie/stan", method = {RequestMethod.GET, RequestMethod.POST})
    public void ustawStanPytania(@RequestParam int nrPytania, @RequestParam String stanPytania) {
        if (nrPytania >= 1 && nrPytania <= 8) {
            stan.getPytania().get(nrPytania - 1).setStanWlasnosci(stanPytania);
            wyslijStan();
        }
    }

    private void wyslijStan() {
        stan.odswiezDaneMeczu(matchState);
        messagingTemplate.convertAndSend("/topic/etap2", stan);
    }
}