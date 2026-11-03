// Host control panel: loads the HostSnapshot, renders it and keeps it in sync with the server.
import { h, role, ensureLayout, replaceContent, toast } from '../core/dom.js';
import { hostApi } from '../core/api.js';
import { connectGame } from '../core/realtime.js';
import { getHostToken } from '../core/storage.js';
import { renderTopbar, renderTeams, renderPlayers } from './game-panel.js';
import { renderLobbyPanel } from './lobby.js';
import { renderStage1Panel, updateHostTimers } from './stage1.js';
import { renderStage2Panel } from './stage2.js';
import { renderStage3Panel } from './stage3.js';

function renderPhasePanel(panel, host, ctx) {
  switch (host.game.phase) {
    case 'LOBBY': renderLobbyPanel(panel, host, ctx); break; // review / re-draw the question set before the game
    case 'STAGE_1': renderStage1Panel(panel, host, ctx); break;
    case 'STAGE_2': renderStage2Panel(panel, host, ctx); break;
    case 'STAGE_3': renderStage3Panel(panel, host, ctx); break;
    default:
      ensureLayout(panel, 'finished', (p) => p.append(
          h('section', { class: 'card' }, h('h2', null, 'Koniec gry'),
              h('p', { class: 'muted' }, 'Wyniki są widoczne na ekranie. Zamknij pokój, aby móc utworzyć nową grę.'))));
  }
}

/**
 * Render one HostSnapshot.
 * ctx: { code, api, goHome(), run(promise) } - see startHost().
 */
export function renderHost(root, host, ctx) {
  ensureLayout(root, 'host', (r) => r.append(
      h('section', { class: 'host-topbar', dataset: { role: 'topbar' } }),
      h('div', { class: 'host-grid' },
          h('div', { class: 'host-col' },
              h('section', { class: 'card', dataset: { role: 'teams' } }),
              h('section', { class: 'card', dataset: { role: 'players' } })),
          h('div', { class: 'host-col host-col-wide', dataset: { role: 'panel' } }))));
  renderTopbar(role(root, 'topbar'), host, ctx);
  renderTeams(role(root, 'teams'), host, ctx);
  renderPlayers(role(root, 'players'), host, ctx);
  renderPhasePanel(role(root, 'panel'), host, ctx);
}

function showNotice(root, text) {
  replaceContent(root, h('div', { class: 'notice' },
      h('p', null, text), h('a', { href: '/', class: 'btn btn-primary' }, 'Wróć na stronę główną')));
}

export function startHost() {
  const doc = globalThis.document;
  const root = doc.getElementById('host');
  const code = (new URLSearchParams(globalThis.location.search).get('game') || '').trim().toUpperCase();
  const token = code ? getHostToken(code) : null;
  if (!code || !token) {
    showNotice(root, 'Brak uprawnień prowadzącego dla tej gry w tej przeglądarce. Utwórz nową grę.');
    return null;
  }

  let host = null;
  const ctx = {
    code,
    api: hostApi(code, token),
    /** Leave the panel (after the room was closed). */
    goHome() { globalThis.location.assign('/'); },
    /** Await a host call, apply the returned snapshot, show errors as a toast. */
    async run(promise) {
      try {
        const snapshot = await promise;
        if (snapshot) apply(snapshot);
        return snapshot;
      } catch (e) {
        toast(e.message);
        return null;
      }
    },
  };

  function apply(snapshot) {
    host = snapshot;
    renderHost(root, host, ctx);
  }

  let loading = false;
  let reloadRequested = false;
  async function refresh() {
    if (loading) { reloadRequested = true; return; }
    loading = true;
    try {
      do {
        reloadRequested = false;
        apply(await ctx.api.get());
      } while (reloadRequested);
    } catch (e) {
      if (e.status === 404) showNotice(root, 'Pokój został zamknięty.');
      else if (!host && e.status === 403) showNotice(root, e.message);
      else toast(e.message);
    } finally {
      loading = false;
    }
  }

  return connectGame(code, {
    onState: refresh, // any change (also from another tab) -> re-fetch the host view
    onTimers: (timers) => updateHostTimers(root, timers),
    onClosed: () => showNotice(root, 'Pokój został zamknięty.'),
    onConnected: refresh,
  });
}
