// Landing page: create a game (host) or join one (player).
import { h, toast } from './core/dom.js';
import { createGame, joinGame } from './core/api.js';
import { setHostToken, setPlayerId } from './core/storage.js';
import { TEAMS, TEAM_LABEL } from './core/constants.js';

export function startLanding() {
  const doc = globalThis.document;
  const teamSelect = doc.getElementById('playerTeam');
  TEAMS.forEach((team) => teamSelect.append(h('option', { value: team }, TEAM_LABEL[team])));

  // Links shared by the host look like /?join=ABCDE
  const prefill = new URLSearchParams(globalThis.location.search).get('join');
  if (prefill) doc.getElementById('gameCode').value = prefill.trim().toUpperCase();

  doc.getElementById('createForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const accessCode = doc.getElementById('accessCode').value;
    try {
      const game = await createGame(accessCode);
      setHostToken(game.code, game.hostToken);
      globalThis.location.assign(`/host.html?game=${encodeURIComponent(game.code)}`);
    } catch (e) {
      toast(e.message);
    }
  });

  doc.getElementById('joinForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const code = form.elements.code.value.trim().toUpperCase();
    try {
      const player = await joinGame(code, form.elements.name.value.trim(), form.elements.team.value);
      setPlayerId(code, player.playerId);
      globalThis.location.assign(`/screen.html?game=${encodeURIComponent(code)}`);
    } catch (e) {
      toast(e.message);
    }
  });
}
