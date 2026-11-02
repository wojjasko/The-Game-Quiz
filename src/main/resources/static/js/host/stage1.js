// Host panel of stage 1: live controls (tiles, question, timers, steals) and the question database editor.
import { h, role, ensureLayout, syncInput, markDirtyOnInput, clearDirty, formatTime } from '../core/dom.js';
import { TEAMS, TEAM_LABEL, TILE_STATUSES, TILE_STATUS_LABEL, ROW_POINTS } from '../core/constants.js';

const SIZE = 4;
const cells = () => Array.from({ length: SIZE }, (_, r) => Array.from({ length: SIZE }, (_, c) => [r, c])).flat();

function buildLive(ctx) {
  const tileGrid = h('div', { class: 'tile-grid' },
    h('div'), ...Array.from({ length: SIZE }, (_, c) => h('div', { class: 'col-head' }, `Kol. ${c + 1}`)));
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

  return h('section', { class: 'card', dataset: { role: 'live' } },
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
      } }, '⚠️ Reset planszy')));
}

function buildEditor(ctx, live) {
  const categoryInputs = Array.from({ length: SIZE }, (_, i) => markDirtyOnInput(
    h('input', { type: 'text', maxlength: 30, dataset: { role: `category-${i}` }, placeholder: `Kategoria ${i + 1}` })));

  const rows = cells().map(([r, c]) => {
    const text = markDirtyOnInput(h('input', { type: 'text', dataset: { role: `q-text-${r}-${c}` }, placeholder: 'Treść pytania...' }));
    const image = markDirtyOnInput(h('input', { type: 'text', list: 'stage1-images', dataset: { role: `q-image-${r}-${c}` }, placeholder: 'Obrazek (np. mario.jpg)' }));
    const sound = markDirtyOnInput(h('input', { type: 'text', list: 'stage1-sounds', dataset: { role: `q-sound-${r}-${c}` }, placeholder: 'Dźwięk (np. d1.wav)' }));
    return h('div', { class: 'question-row' },
      h('span', { class: 'row-label' }, `${r * SIZE + c + 1}. (W${r + 1}, K${c + 1})`), text, image, sound,
      h('div', { class: 'row' },
        h('button', { type: 'button', class: 'btn btn-ok', onclick: async () => {
          const saved = await ctx.run(ctx.api.saveQuestion(r, c, { text: text.value, image: image.value, sound: sound.value }));
          if (saved) clearDirty(text, image, sound);
        } }, '💾 Zapisz'),
        live ? h('button', { type: 'button', class: 'btn btn-primary', onclick: () => ctx.run(ctx.api.activateTile(r, c)) }, '▶ Na ekran') : null));
  });

  return h('section', { class: 'card', dataset: { role: 'editor' } },
    h('h2', null, 'Przygotowanie etapu 1'),
    h('h3', null, 'Kategorie (od lewej do prawej)'),
    h('div', { class: 'row' }, ...categoryInputs,
      h('button', { type: 'button', class: 'btn btn-primary', onclick: async () => {
        if (await ctx.run(ctx.api.setCategories(categoryInputs.map((i) => i.value)))) clearDirty(...categoryInputs);
      } }, 'Zapisz kategorie')),
    h('h3', null, 'Baza 16 pytań'),
    h('p', { class: 'muted' }, 'Obrazki i dźwięki to nazwy plików z katalogu media/ na serwerze.'),
    h('datalist', { id: 'stage1-images', dataset: { role: 'image-options' } }),
    h('datalist', { id: 'stage1-sounds', dataset: { role: 'sound-options' } }),
    ...rows);
}

/** live=false: only the preparation editor (lobby). live=true: live controls + editor. */
export function renderStage1Panel(root, host, ctx, live) {
  ensureLayout(root, live ? 'stage1-live' : 'stage1-prep', (r) => {
    if (live) r.append(buildLive(ctx));
    r.append(buildEditor(ctx, live));
  });

  if (live) {
    const s1 = host.game.stage1;
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

  host.stage1Categories.forEach((name, i) => syncInput(role(root, `category-${i}`), name));
  for (const [r, c] of cells()) {
    const q = host.stage1Questions[r][c];
    syncInput(role(root, `q-text-${r}-${c}`), q.text);
    syncInput(role(root, `q-image-${r}-${c}`), q.image);
    syncInput(role(root, `q-sound-${r}-${c}`), q.sound);
  }
  const options = (names) => (names || []).map((n) => h('option', { value: n }));
  role(root, 'image-options').replaceChildren(...options(ctx.media && ctx.media.stage1Images));
  role(root, 'sound-options').replaceChildren(...options(ctx.media && ctx.media.sounds));
}

/** Update only the two time displays (called for every /timers message and after each snapshot). */
export function updateHostTimers(root, timers) {
  for (const team of TEAMS) {
    const el = root.querySelector(`[data-role="time-${team}"]`);
    if (el && timers && timers[team]) el.textContent = formatTime(timers[team].remainingSeconds);
  }
}
