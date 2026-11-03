// Host panel of stage 1: live controls (tiles, question, timers, steals) and the questions of this game with answers.
import { h, role, ensureLayout, syncInput, markDirtyOnInput, clearDirty, formatTime } from '../core/dom.js';
import { TEAMS, TEAM_LABEL, TILE_STATUSES, TILE_STATUS_LABEL, ROW_POINTS } from '../core/constants.js';

const SIZE = 4;
const cells = () => Array.from({ length: SIZE }, (_, r) => Array.from({ length: SIZE }, (_, c) => [r, c])).flat();

function build(ctx) {
  const tileGrid = h('div', { class: 'tile-grid' },
      h('div'), ...Array.from({ length: SIZE }, (_, c) => h('div', { class: 'col-head', dataset: { role: `col-head-${c}` } }, `Kol. ${c + 1}`)));
  for (let r = 0; r < SIZE; r++) {
    tileGrid.append(h('div', { class: 'row-label' }, `${ROW_POINTS[r]} pkt`));
    for (let c = 0; c < SIZE; c++) {
      tileGrid.append(h('select', { dataset: { row: r, col: c }, onchange: (e) =>
                ctx.run(ctx.api.setTile(r, c, e.target.value)) },
          ...TILE_STATUSES.map((s) => h('option', { value: s }, TILE_STATUS_LABEL[s]))));
    }
  }

  const freeText = markDirtyOnInput(h('textarea', { dataset: { role: 'free-text' }, placeholder: 'Pytanie widoczne na ekranie...' }));

  const timerLines = TEAMS.map((team) => h('div', { class: 'timer-line', dataset: { team } },
      h('strong', null, TEAM_LABEL[team]),
      h('span', { class: 'timer-time', dataset: { role: `time-${team}` } }, '06:00'),
      h('button', { type: 'button', class: 'btn btn-ok', onclick: () => ctx.run(ctx.api.timer(team, 'start')) }, '▶ Start'),
      h('button', { type: 'button', class: 'btn btn-warn', onclick: () => ctx.run(ctx.api.timer(team, 'stop')) }, '⏸ Stop'),
      h('button', { type: 'button', class: 'btn', onclick: () => ctx.run(ctx.api.timer(team, 'reset')) }, '🔄 6:00'),
      h('button', { type: 'button', class: 'btn btn-primary', onclick: () => ctx.run(ctx.api.timeBonus(team)) }, '⏱ Punkty za czas'),
      h('span', { class: 'steals-badge', dataset: { role: `steals-${team}` } }),
      h('button', { type: 'button', class: 'btn btn-orange', onclick: () => ctx.run(ctx.api.spendSteal(team)) }, '⚡ -1 steal')));

  return [
    h('section', { class: 'card', dataset: { role: 'live' } },
        h('h2', null, 'Etap 1 – na żywo'),
        h('h3', null, 'Kafelki planszy'), tileGrid,
        h('h3', null, 'Pytanie na ekranie'), freeText,
        h('div', { class: 'row' },
            h('button', { type: 'button', class: 'btn btn-primary', onclick: async () => {
                if (await ctx.run(ctx.api.setCurrentQuestion(freeText.value))) clearDirty(freeText);
              } }, 'Wyślij na ekran'),
            h('button', { type: 'button', class: 'btn', onclick: async () => {
                freeText.value = '';
                if (await ctx.run(ctx.api.clearCurrentQuestion())) clearDirty(freeText);
              } }, 'Wyczyść'),
            h('button', { type: 'button', class: 'btn btn-warn', onclick: () => ctx.run(ctx.api.setImageZoom(true)) }, '🔍 Powiększ obrazek'),
            h('button', { type: 'button', class: 'btn btn-orange', onclick: () => ctx.run(ctx.api.setImageZoom(false)) }, '❌ Zmniejsz obrazek')),
        h('h3', null, 'Czas i steale'), ...timerLines,
        h('div', { class: 'row' },
            h('button', { type: 'button', class: 'btn btn-orange', onclick: () => ctx.run(ctx.api.resetSteals()) }, '🔄 Reset steali'),
            h('button', { type: 'button', class: 'btn btn-danger', onclick: () => {
                if (globalThis.confirm('Czy na pewno zresetować planszę, pytanie, zegary i steale?')) ctx.run(ctx.api.resetBoard());
              } }, '⚠️ Reset planszy'))),
    h('section', { class: 'card', dataset: { role: 'questions' } },
        h('h2', null, 'Pytania i odpowiedzi'),
        h('p', { class: 'muted' }, 'Odpowiedzi widzisz tylko Ty. „Na ekran” pokazuje pytanie (z obrazkiem/dźwiękiem) graczom.'),
        h('div', { dataset: { role: 'question-list' } })),
  ];
}

function renderQuestionList(box, host, ctx) {
  box.replaceChildren(...host.stage1Columns.map((column, col) =>
      h('div', { class: 'set-block' },
          h('div', { class: 'set-head' }, h('strong', null, `Kolumna ${col + 1}: ${column.name}`)),
          ...column.questions.map((q, row) => h('div', { class: 'set-line', dataset: { row, col } },
              h('span', { class: 'row-label' }, `${ROW_POINTS[row]} pkt`),
              h('span', null, q.text, ' → ', h('b', { class: 'answer' }, q.answer || '(brak odpowiedzi)'),
                  q.image ? h('span', { class: 'muted' }, ` [obrazek: ${q.image}]`) : null,
                  q.sound ? h('span', { class: 'muted' }, ` [dźwięk: ${q.sound}]`) : null),
              h('button', { type: 'button', class: 'btn btn-primary btn-small',
                onclick: () => ctx.run(ctx.api.activateTile(row, col)) }, '▶ Na ekran'))))));
}

export function renderStage1Panel(root, host, ctx) {
  ensureLayout(root, 'stage1-live', (r) => r.append(...build(ctx)));

  const s1 = host.game.stage1;
  if (s1) {
    for (const [r, c] of cells()) {
      const select = root.querySelector(`select[data-row="${r}"][data-col="${c}"]`);
      if (select) select.value = s1.board[r][c];
    }
    syncInput(role(root, 'free-text'), s1.question);
    for (const team of TEAMS) {
      role(root, `steals-${team}`).textContent = '⚡'.repeat(s1.steals[team]) || 'brak';
    }
    updateHostTimers(root, s1.timers);
  }
  host.stage1Columns.forEach((column, c) => { role(root, `col-head-${c}`).textContent = column.name; });
  renderQuestionList(role(root, 'question-list'), host, ctx);
}

/** Update only the two time displays (called for every /timers message and after each snapshot). */
export function updateHostTimers(root, timers) {
  for (const team of TEAMS) {
    const el = root.querySelector(`[data-role="time-${team}"]`);
    if (el && timers && timers[team]) el.textContent = formatTime(timers[team].remainingSeconds);
  }
}
