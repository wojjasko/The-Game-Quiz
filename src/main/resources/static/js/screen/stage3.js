import { h, role, ensureLayout } from '../core/dom.js';
import { ANSWER_LETTERS } from '../core/constants.js';

const SWAP_MS = 800;

function build(root) {
  root.append(h('div', { class: 'stage3' },
    h('div', { class: 'cards-grid', dataset: { role: 'grid' } }),
    h('div', { class: 'modal-overlay', dataset: { role: 'modal' } },
      h('div', { class: 'question-card-box' },
        h('div', { class: 'question-category-tag', dataset: { role: 'category' } }),
        h('div', { class: 'question-text', dataset: { role: 'text' } }),
        h('div', { class: 'answers-grid' }, ...ANSWER_LETTERS.map((l) =>
          h('div', { class: 'answer-btn', dataset: { letter: l } },
            h('span', { class: 'answer-letter' }, `${l}:`),
            h('span', { class: 'answer-text' }))))))));
}

function renderCards(root, stage3, state) {
  const grid = role(root, 'grid');
  if (grid.children.length !== stage3.table.length) {
    grid.replaceChildren(...stage3.table.map((_, i) => h('div', { class: 'card-item', dataset: { slot: i } })));
    state.prevCardIds = [];
  }
  stage3.table.forEach((card, i) => {
    const el = grid.children[i];
    const classes = ['card-item'];
    if (card.result && card.result !== 'NONE') classes.push(`result-${card.result}`);
    else if (card.used) classes.push('used');
    el.className = classes.join(' ');
    el.dataset.cardId = String(card.id);
    el.dataset.label = card.category;
    const prevId = state.prevCardIds[i];
    if (prevId !== undefined && prevId !== card.id) {
      el.classList.add('swapping');
      globalThis.setTimeout(() => { el.textContent = el.dataset.label; }, SWAP_MS / 2);
      globalThis.setTimeout(() => el.classList.remove('swapping'), SWAP_MS);
    } else if (!el.classList.contains('swapping')) {
      el.textContent = card.category;
    }
  });
  state.prevCardIds = stage3.table.map((c) => c.id);
}

function renderQuestion(root, question) {
  const modal = role(root, 'modal');
  modal.classList.toggle('visible', !!question);
  if (!question) return;
  role(root, 'category').textContent = question.category;
  role(root, 'text').textContent = question.text;
  const hidden = new Set(question.hiddenAnswers || []);
  ANSWER_LETTERS.forEach((letter, i) => {
    const box = modal.querySelector(`.answer-btn[data-letter="${letter}"]`);
    const isHidden = hidden.has(letter);
    box.classList.toggle('hidden-answer', isHidden);
    box.setAttribute('aria-hidden', isHidden ? 'true' : 'false');
    box.querySelector('.answer-text').textContent = isHidden ? '' : (question.answers[i] ?? '');
  });
}

export function renderStage3(root, stage3, ctx) {
  ensureLayout(root, 'stage3', build);
  renderCards(root, stage3, ctx.state);
  renderQuestion(root, stage3.question);
}
