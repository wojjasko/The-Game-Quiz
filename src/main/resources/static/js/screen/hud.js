import { h, role, formatTime } from '../core/dom.js';
import { TEAMS, PHASE_SUBTITLE } from '../core/constants.js';

const FLOAT_MS = 1400;

/** Per-screen mutable state shared by the renderers (kept out of module scope on purpose). */
export function createScreenState() {
  return { prevScores: {}, lines: new Map(), prevCardIds: [] };
}

function buildTeam(team) {
  return h('div', { class: `team-container team-${team.toLowerCase()}`, dataset: { team } },
    h('div', { class: 'team-row-top' },
      h('h2', { class: 'team-title', dataset: { role: 'name' } }),
      h('div', { class: 'score-timer-wrapper' },
        h('div', { class: 'hud-extra', dataset: { role: 'extra' } }),
        h('div', { class: 'score-wrap', dataset: { role: 'score-wrap' } },
          h('div', { class: 'score', dataset: { role: 'score' } }, '0')))),
    h('ul', { class: 'players-row', dataset: { role: 'players' } }));
}

function buildHud(root) {
  root.append(
    buildTeam('BLUE'),
    h('div', { class: 'center-titles' },
      h('h1', { class: 'main-game-title' }, 'TELETURNIEJ O GRACH'),
      h('div', { class: 'sub-game-title', dataset: { role: 'subtitle' } }),
      h('div', { class: 'game-code', dataset: { role: 'game-code' } })),
    buildTeam('RED'));
}

export function showScoreFloat(wrap, diff) {
  if (!diff) return;
  const old = wrap.querySelector('.score-float');
  if (old) old.remove();
  const el = h('div', { class: `score-float ${diff > 0 ? 'float-plus' : 'float-minus'}` },
    `${diff > 0 ? '+' : ''}${diff}`);
  wrap.appendChild(el);
  globalThis.setTimeout(() => el.remove(), FLOAT_MS);
}

function renderExtra(extra, team, snap) {
  extra.replaceChildren();
  if (snap.phase === 'STAGE_1' && snap.stage1) {
    const s1 = snap.stage1;
    const t = (s1.timers && s1.timers[team]) || { remainingSeconds: 0, running: false };
    extra.append(
      h('div', { class: 'steals-badge', dataset: { role: 'steals' } }, '⚡'.repeat(s1.steals?.[team] ?? 0)),
      h('div', { class: `timer-box${t.running ? ' timer-running' : ''}`, dataset: { timer: team } },
        formatTime(t.remainingSeconds)));
  } else if (snap.phase === 'STAGE_3' && snap.stage3) {
    const s3 = snap.stage3;
    extra.append(
      h('div', { class: `lifeline-badge${s3.lifelineUsed?.[team] ? ' used' : ''}`, dataset: { role: 'lifeline' } }, '50:50'),
      h('div', { class: 'multiplier-badge', dataset: { role: 'multiplier' } }, `+${s3.multipliers?.[team] ?? 15} pkt`));
  }
}

/** Render the shared header. `root` is the header element; `state` comes from createScreenState(). */
export function renderHud(root, snap, state) {
  if (!root.dataset.built) {
    buildHud(root);
    root.dataset.built = '1';
  }
  for (const team of TEAMS) {
    const box = root.querySelector(`.team-container[data-team="${team}"]`);
    const data = snap.teams[team];
    role(box, 'name').textContent = data.name;
    role(box, 'score').textContent = String(data.score);
    role(box, 'players').replaceChildren(...data.players.map((p) => h('li', null, p)));
    const prev = state.prevScores[team];
    if (prev !== undefined && prev !== data.score) showScoreFloat(role(box, 'score-wrap'), data.score - prev);
    state.prevScores[team] = data.score;
    renderExtra(role(box, 'extra'), team, snap);
  }
  role(root, 'subtitle').textContent = PHASE_SUBTITLE[snap.phase] || '';
  role(root, 'game-code').textContent = `Kod gry: ${snap.code}`;
}

/** Update only the stage-1 timer elements (called for every /timers message). */
export function updateTimers(root, timers) {
  for (const team of TEAMS) {
    const el = root.querySelector(`[data-timer="${team}"]`);
    const t = timers && timers[team];
    if (!el || !t) continue;
    el.textContent = formatTime(t.remainingSeconds);
    el.classList.toggle('timer-running', !!t.running);
  }
}
