import { h, ensureLayout, role } from '../core/dom.js';
import { TEAMS } from '../core/constants.js';

export function renderFinal(root, snap) {
  ensureLayout(root, 'final', (r) => {
    r.append(h('section', { class: 'final' },
      h('div', { class: 'final-headline', dataset: { role: 'headline' } }),
      h('div', { class: 'final-teams' }, ...TEAMS.map((team) =>
        h('div', { class: `final-team final-team-${team.toLowerCase()}`, dataset: { team } },
          h('div', { class: 'final-team-name', dataset: { role: 'name' } }),
          h('div', { class: 'final-score', dataset: { role: 'score' } }))))));
  });
  const { BLUE, RED } = snap.teams;
  const headline = role(root, 'headline');
  let winner = null;
  if (BLUE.score > RED.score) winner = 'BLUE';
  else if (RED.score > BLUE.score) winner = 'RED';
  headline.textContent = winner ? `Zwycięzca: ${snap.teams[winner].name}` : 'Remis!';
  headline.dataset.winner = winner || 'DRAW';
  for (const team of TEAMS) {
    const box = root.querySelector(`.final-team[data-team="${team}"]`);
    role(box, 'name').textContent = snap.teams[team].name;
    role(box, 'score').textContent = String(snap.teams[team].score);
    box.classList.toggle('winner', winner === team);
  }
}
