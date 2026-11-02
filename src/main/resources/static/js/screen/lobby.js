import { h, role, ensureLayout } from '../core/dom.js';
import { TEAMS, TEAM_LABEL } from '../core/constants.js';

/**
 * ctx: { playerId: string|null, onJoin({name, team}), onLeave() }
 * The join form is built once and only shown/hidden afterwards, so typed text survives re-renders.
 */
export function renderLobby(root, snap, ctx) {
  ensureLayout(root, 'lobby', (r) => {
    const origin = (globalThis.location && globalThis.location.origin) || '';
    r.append(h('section', { class: 'lobby' },
      h('div', { class: 'lobby-code-label' }, 'KOD GRY'),
      h('div', { class: 'lobby-code', dataset: { role: 'code' } }),
      h('div', { class: 'lobby-hint' }, `Dołącz na ${origin}/ wpisując kod gry`),
      h('div', { class: 'lobby-teams' }, ...TEAMS.map((team) =>
        h('div', { class: `lobby-team lobby-team-${team.toLowerCase()}`, dataset: { team } },
          h('h3', { dataset: { role: 'name' } }),
          h('div', { class: 'lobby-count', dataset: { role: 'count' } }),
          h('ul', { class: 'lobby-players', dataset: { role: 'players' } })))),
      h('form', { class: 'join-form', dataset: { role: 'join-form' }, onsubmit: (e) => {
        e.preventDefault();
        const form = e.currentTarget;
        ctx.onJoin({ name: form.elements.name.value.trim(), team: form.elements.team.value });
      } },
        h('h3', null, 'Dołącz do gry'),
        h('input', { name: 'name', type: 'text', maxlength: 24, required: true, placeholder: 'Twój nick', autocomplete: 'off' }),
        h('select', { name: 'team' }, ...TEAMS.map((t) => h('option', { value: t }, TEAM_LABEL[t]))),
        h('button', { type: 'submit', class: 'btn btn-primary' }, 'Dołącz')),
      h('div', { class: 'leave-box', dataset: { role: 'leave-box' } },
        h('p', null, 'Jesteś w grze. Czekaj na start rundy.'),
        h('button', { type: 'button', class: 'btn btn-danger', dataset: { role: 'leave' }, onclick: () => ctx.onLeave() },
          'Opuść grę'))));
  });

  role(root, 'code').textContent = snap.code;
  for (const team of TEAMS) {
    const box = root.querySelector(`.lobby-team[data-team="${team}"]`);
    const data = snap.teams[team];
    role(box, 'name').textContent = data.name;
    role(box, 'count').textContent = `Graczy: ${data.players.length}`;
    role(box, 'players').replaceChildren(...data.players.map((p) => h('li', null, p)));
  }
  const joined = !!ctx.playerId;
  role(root, 'join-form').hidden = joined;
  role(root, 'leave-box').hidden = !joined;
}
