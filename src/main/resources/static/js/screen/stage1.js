import { h, role, ensureLayout, protectMedia } from '../core/dom.js';
import { ROW_POINTS } from '../core/constants.js';

export const LINE_HIGHLIGHT_MS = 3000;
const WAITING_TEXT = 'Oczekiwanie na uruchomienie pytania przez prowadzącego...';

function build(root) {
  const img = protectMedia(h('img', { class: 'question-image', alt: '', hidden: true, dataset: { role: 'image' } }));
  const audio = h('audio', { controls: true, controlsList: 'nodownload', hidden: true, dataset: { role: 'audio' } });
  audio.addEventListener('contextmenu', (e) => e.preventDefault());
  root.append(h('div', { class: 'stage1 main-container' },
    h('div', { class: 'board-grid', dataset: { role: 'board' } }),
    h('div', { class: 'question-box' },
      h('div', { class: 'question-title' }, 'AKTUALNE PYTANIE'),
      h('div', { class: 'question-text', dataset: { role: 'question' } }),
      h('div', { class: 'media' }, img, audio))));
}

function renderBoard(grid, stage1) {
  const kids = [h('div', { class: 'board-corner' })];
  stage1.categories.forEach((c) => kids.push(h('div', { class: 'category-item' }, c)));
  stage1.board.forEach((row, r) => {
    kids.push(h('div', { class: 'point-label' }, `${ROW_POINTS[r]} >`));
    row.forEach((status, c) => kids.push(
      h('div', { class: `cell tile-${status}`, dataset: { row: r, col: c, status } })));
  });
  grid.replaceChildren(...kids);
}

function applyLineClasses(root, state) {
  root.querySelectorAll('.cell.winning-line').forEach((c) => c.classList.remove('winning-line'));
  for (const entry of state.lines.values()) {
    if (!entry.active) continue;
    for (const [r, c] of entry.line) {
      const cell = root.querySelector(`.cell[data-row="${r}"][data-col="${c}"]`);
      if (cell) cell.classList.add('winning-line');
    }
  }
}

/** Highlight each completed line once; the expiry timer is scheduled exactly once per line. */
function trackLines(root, state, lines) {
  const present = new Set();
  for (const line of lines || []) {
    const key = JSON.stringify(line);
    present.add(key);
    if (!state.lines.has(key)) {
      const entry = { line, active: true };
      state.lines.set(key, entry);
      globalThis.setTimeout(() => { entry.active = false; applyLineClasses(root, state); }, LINE_HIGHLIGHT_MS);
    }
  }
  for (const [key, entry] of state.lines) {
    if (!present.has(key) && !entry.active) state.lines.delete(key);
  }
  applyLineClasses(root, state);
}

function setMedia(el, url) {
  if (url) {
    if (el.getAttribute('src') !== url) el.setAttribute('src', url);
    el.hidden = false;
  } else {
    if (el.hasAttribute('src')) {
      if (el.tagName === 'AUDIO') { try { el.pause(); } catch { /* ignore */ } }
      el.removeAttribute('src');
    }
    el.hidden = true;
  }
}

/** ctx.state is the object from createScreenState(). */
export function renderStage1(root, stage1, ctx) {
  ensureLayout(root, 'stage1', build);
  renderBoard(role(root, 'board'), stage1);
  trackLines(root, ctx.state, stage1.completedLines);
  role(root, 'question').textContent = stage1.question || WAITING_TEXT;
  const img = role(root, 'image');
  setMedia(img, stage1.imageUrl);
  img.classList.toggle('img-zoomed', !!stage1.imageZoomed);
  setMedia(role(root, 'audio'), stage1.soundUrl);
}
