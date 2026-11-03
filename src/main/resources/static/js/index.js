// Landing page. The server hosts one game at a time, so there is no game code to type:
// the page asks which game is running and offers the right buttons.
import { h, toast } from './core/dom.js';
import { createGame, joinGame, getCurrentGame, closeGame } from './core/api.js';
import { getHostToken, setHostToken, getPlayerId, setPlayerId } from './core/storage.js';
import { TEAMS, TEAM_LABEL } from './core/constants.js';

const REFRESH_MS = 4000;

export function startLanding() {
  const doc = globalThis.document;
  const $ = (id) => doc.getElementById(id);
  const teamSelect = $('playerTeam');
  TEAMS.forEach((team) => teamSelect.append(h('option', { value: team }, TEAM_LABEL[team])));

  let currentCode = null;

  /** Show only what makes sense for the current state of the server. */
  function render(code) {
    currentCode = code;
    const running = !!code;
    const myToken = running ? getHostToken(code) : null;
    const myPlayerId = running ? getPlayerId(code) : null;

    // host card
    $('createButton').hidden = running;
    $('hostPanelLink').hidden = !myToken;
    if (myToken) $('hostPanelLink').setAttribute('href', `/host.html?game=${encodeURIComponent(code)}`);
    $('closeButton').hidden = !running;
    $('accessBox').hidden = !!myToken;
    $('hostHint').textContent = !running
        ? 'Utworz nowa gre i zapros graczy.'
        : (myToken ? 'Twoja gra trwa.' : 'Gra juz trwa. Aby ja zamknac, wpisz kod dostepu.');

    // player card
    $('joinHint').textContent = running ? 'Wpisz nick i wybierz druzyne.' : 'Nie ma teraz zadnej gry. Poczekaj, az prowadzacy ja utworzy.';
    for (const el of $('joinForm').querySelectorAll('input, select, #joinButton')) el.disabled = !running;
    $('joinButton').hidden = !!myPlayerId;
    $('playerName').closest('form').querySelectorAll('label, #playerName, #playerTeam')
        .forEach((el) => { el.hidden = !!myPlayerId; });
    $('screenLink').hidden = !(running && myPlayerId);
    if (running && myPlayerId) $('screenLink').setAttribute('href', `/screen.html?game=${encodeURIComponent(code)}`);
  }

  async function refresh() {
    try {
      const current = await getCurrentGame();
      render(current.code || null);
    } catch {
      /* the server is unreachable for a moment: keep what is shown */
    }
  }

  $('createForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      const game = await createGame($('accessCode').value);
      setHostToken(game.code, game.hostToken);
      globalThis.location.assign(`/host.html?game=${encodeURIComponent(game.code)}`);
    } catch (e) {
      toast(e.message);
      refresh();
    }
  });

  $('closeButton').addEventListener('click', async () => {
    if (!currentCode) return;
    if (!globalThis.confirm('Zamknac pokoj? Gra zostanie zakonczona dla wszystkich.')) return;
    try {
      await closeGame(currentCode, { token: getHostToken(currentCode), accessCode: $('accessCode').value });
      toast('Pokoj zamkniety.', 'ok');
    } catch (e) {
      toast(e.message);
    }
    refresh();
  });

  $('joinForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!currentCode) return;
    const form = event.currentTarget;
    try {
      const player = await joinGame(currentCode, form.elements.name.value.trim(), form.elements.team.value);
      setPlayerId(currentCode, player.playerId);
      globalThis.location.assign(`/screen.html?game=${encodeURIComponent(currentCode)}`);
    } catch (e) {
      toast(e.message);
    }
  });

  render(null);
  refresh();
  globalThis.setInterval(refresh, REFRESH_MS);
}
