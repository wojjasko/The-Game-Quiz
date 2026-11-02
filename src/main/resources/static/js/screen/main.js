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

const RENDERERS = {
  LOBBY: (root, snap, ctx) => renderLobby(root, snap, ctx),
  STAGE_1: (root, snap, ctx) => renderStage1(root, snap.stage1, ctx),
  STAGE_2: (root, snap) => renderStage2(root, snap.stage2),
  STAGE_3: (root, snap, ctx) => renderStage3(root, snap.stage3, ctx),
  FINISHED: (root, snap) => renderFinal(root, snap),
};

/** Render one snapshot: HUD plus the view of snap.phase. */
export function renderScreen(hudRoot, viewRoot, snap, ctx) {
  renderHud(hudRoot, snap, ctx.state);
  viewRoot.dataset.phase = snap.phase;
  const render = RENDERERS[snap.phase];
  if (render) render(viewRoot, snap, ctx);
}

export function startScreen() {
  const doc = globalThis.document;
  const hudRoot = doc.getElementById('hud');
  const viewRoot = doc.getElementById('view');
  const code = (new URLSearchParams(globalThis.location.search).get('game') || '').trim().toUpperCase();

  if (!code) {
    hudRoot.hidden = true;
    replaceContent(viewRoot, h('div', { class: 'notice' },
      h('p', null, 'Brak kodu gry w adresie.'), h('a', { href: '/', class: 'btn btn-primary' }, 'Wróć na stronę główną')));
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
        toast('Dołączono do gry.', 'ok');
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

  const show = (snap) => { snapshot = snap; renderScreen(hudRoot, viewRoot, snap, ctx); };
  async function refresh() {
    try { show(await getGame(code)); } catch (e) {
      toast(e.message);
      if (e.status === 404 && !snapshot) {
        hudRoot.hidden = true;
        replaceContent(viewRoot, h('div', { class: 'notice' },
          h('p', null, 'Nie znaleziono gry.'), h('a', { href: '/', class: 'btn btn-primary' }, 'Wróć na stronę główną')));
      }
    }
  }

  doc.addEventListener('contextmenu', (e) => {
    if (!/^(INPUT|TEXTAREA|SELECT)$/.test(e.target.tagName)) e.preventDefault();
  });
  doc.addEventListener('dragstart', (e) => { if (e.target.tagName === 'IMG') e.preventDefault(); });

  return connectGame(code, {
    onState: show,
    onTimers: (t) => updateTimers(hudRoot, t),
    onConnected: refresh,
  });
}
