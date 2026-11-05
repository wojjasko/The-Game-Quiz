import { h, replaceContent, toast } from '../core/dom.js';
import { getGame, joinGame, leaveGame } from '../core/api.js';
import { connectGame } from '../core/realtime.js';
import { getPlayerId, setPlayerId, clearPlayerId } from '../core/storage.js';
import { createScreenState, renderHud, updateTimers } from './hud.js';
import { renderLobby } from './lobby.js';
import { renderStage1 } from './stage1.js';
import { renderStage2 } from './stage2.js';
import { renderStage3 } from './stage3.js';
import { renderFinal } from './final.js';
import { playPhaseTransition, updateCountdown } from './transition.js';
import { installSoundToggle, syncCountdownSound } from './sound.js';

const RENDERERS = {
  LOBBY: (root, snap, ctx) => renderLobby(root, snap, ctx),
  STAGE_1: (root, snap, ctx) => renderStage1(root, snap.stage1, ctx),
  STAGE_2: (root, snap) => renderStage2(root, snap.stage2),
  STAGE_3: (root, snap, ctx) => renderStage3(root, snap.stage3, ctx),
  FINISHED: (root, snap) => renderFinal(root, snap),
};

/** Render one snapshot: HUD plus the view of snap.phase, the start countdown and the stage-change animation. */
export function renderScreen(hudRoot, viewRoot, snap, ctx) {
  renderHud(hudRoot, snap, ctx.state);
  viewRoot.dataset.phase = snap.phase;
  const render = RENDERERS[snap.phase];
  if (render) render(viewRoot, snap, ctx);

  const countdown = snap.phase === 'LOBBY' ? snap.countdown : null;
  updateCountdown(countdown);
  syncCountdownSound(countdown ?? null);
  const previous = ctx.state.prevPhase;
  if (previous !== undefined && previous !== snap.phase) playPhaseTransition(snap.phase);
  ctx.state.prevPhase = snap.phase;
}

function showNotice(hudRoot, viewRoot, text) {
  hudRoot.hidden = true;
  updateCountdown(null);
  syncCountdownSound(null);
  replaceContent(viewRoot, h('div', { class: 'notice' },
      h('p', null, text), h('a', { href: '/', class: 'btn btn-primary' }, 'Wroc na strone glowna')));
}

export function startScreen() {
  const doc = globalThis.document;
  const hudRoot = doc.getElementById('hud');
  const viewRoot = doc.getElementById('view');
  const code = (new URLSearchParams(globalThis.location.search).get('game') || '').trim().toUpperCase();

  if (!code) {
    hudRoot.hidden = true;
    showNotice(hudRoot, viewRoot, 'Brak kodu gry w adresie.');
    return null;
  }

  let snapshot = null;
  const ctx = {
    state: createScreenState(),
    playerId: getPlayerId(code),
    async onJoin({ name, team }) {
      try {
        const res = await joinGame(code, name, team);
        setPlayerId(code, res.playerId);
        ctx.playerId = res.playerId;
        toast('Dolaczono do gry.', 'ok');
        if (snapshot) renderScreen(hudRoot, viewRoot, snapshot, ctx);
        refresh();
      } catch (e) { toast(e.message); }
    },
    async onLeave() {
      try {
        await leaveGame(code, ctx.playerId);
      } catch (e) {
        if (e.status !== 404) { toast(e.message); return; }
      }
      clearPlayerId(code);
      ctx.playerId = null;
      if (snapshot) renderScreen(hudRoot, viewRoot, snapshot, ctx);
      refresh();
    },
  };

  /** The room is gone (closed by the host or the server restarted): forget this player and say so. */
  function closed(text) {
    clearPlayerId(code);
    ctx.playerId = null;
    showNotice(hudRoot, viewRoot, text);
  }

  const show = (snap) => { snapshot = snap; renderScreen(hudRoot, viewRoot, snap, ctx); };
  async function refresh() {
    try { show(await getGame(code)); } catch (e) {
      if (e.status === 404) {
        closed(snapshot ? 'Pokoj zostal zamkniety przez prowadzacego.' : 'Nie znaleziono gry.');
      } else {
        toast(e.message);
      }
    }
  }

  installSoundToggle();
  doc.addEventListener('contextmenu', (e) => {
    if (!/^(INPUT|TEXTAREA|SELECT)$/.test(e.target.tagName)) e.preventDefault();
  });
  doc.addEventListener('dragstart', (e) => { if (e.target.tagName === 'IMG') e.preventDefault(); });

  return connectGame(code, {
    onState: show,
    onTimers: (t) => updateTimers(hudRoot, t),
    onClosed: () => closed('Pokoj zostal zamkniety przez prowadzacego.'),
    onConnected: refresh,
  });
}
