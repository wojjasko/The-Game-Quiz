package com.gamequiz;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etap3")
@CrossOrigin(origins = "*")
public class Etap3Controller {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchState matchState;
    private final Stage3State stan;

    public Etap3Controller(SimpMessagingTemplate messagingTemplate, MatchState matchState) {
        this.messagingTemplate = messagingTemplate;
        this.matchState = matchState;
        this.stan = new Stage3State(matchState);
    }

    @GetMapping("/stan")
    public Stage3State getStan() {
        stan.odswiezDaneMeczu(matchState);
        return stan;
    }

    @PostMapping("/wymien")
    public void wymienKarte(@RequestParam int idKarty) {
        stan.wymienKarte(idKarty);
        wyslijStan();
    }

    @PostMapping("/wybierz")
    public void wybierzKarte(@RequestParam int idKarty) {
        for (Stage3State.Karta k : stan.getKartyNaStole()) {
            if (k.getId() == idKarty) {
                stan.setWybranePytanie(k);
                stan.setPokazPytanie(true);
                stan.getUkryteOdpowiedzi().clear();
                break;
            }
        }
        wyslijStan();
    }

    @PostMapping("/zamknij-pytanie")
    public void zamknijPytanie() {
        if (stan.getWybranePytanie() != null) {
            stan.getWybranePytanie().setZuzyta(true);
        }
        stan.setPokazPytanie(false);
        stan.setWybranePytanie(null);
        stan.getUkryteOdpowiedzi().clear();
        wyslijStan();
    }

    @PostMapping("/kolo")
    public void uzyjKolo(@RequestParam int druzyna) {
        stan.uzyjKolo5050(druzyna);
        wyslijStan();
    }

    @PostMapping("/odpowiedz")
    public void odpowiedz(@RequestParam int druzyna, @RequestParam boolean poprawna) {
        Stage3State.Karta q = stan.getWybranePytanie();

        if (q != null) {
            if (druzyna == 1) { // Niebiescy
                q.setStanRozstrzygniecia(poprawna ? "BLUE-CORRECT" : "BLUE-WRONG");
            } else { // Czerwoni
                q.setStanRozstrzygniecia(poprawna ? "RED-CORRECT" : "RED-WRONG");
            }
        }

        if (poprawna) {
            if (druzyna == 1) {
                matchState.dodajPunktyDruzyny1(stan.getMnoznikDruzyny1());
                stan.setMnoznikDruzyny1(stan.getMnoznikDruzyny1() + 5);
            } else {
                matchState.dodajPunktyDruzyny2(stan.getMnoznikDruzyny2());
                stan.setMnoznikDruzyny2(stan.getMnoznikDruzyny2() + 5);
            }
        } else {
            if (druzyna == 1) stan.setMnoznikDruzyny1(15);
            else stan.setMnoznikDruzyny2(15);
        }
        zamknijPytanie();
    }

    private void wyslijStan() {
        stan.odswiezDaneMeczu(matchState);
        messagingTemplate.convertAndSend("/topic/etap3", stan);
    }
}