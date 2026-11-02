import { h, role, ensureLayout, protectMedia } from '../core/dom.js';

function build(root) {
  root.append(h('div', { class: 'stage2 stage-container' },
    h('div', { class: 'backdrop', dataset: { role: 'backdrop' } }),
    h('div', { class: 'numbers-nav', dataset: { role: 'nav' } }),
    h('div', { class: 'gallery-container', dataset: { role: 'gallery' } },
      ...[0, 1, 2].map((i) => h('div', { class: 'img-card', dataset: { index: i } })))));
}

function fillCard(card, image) {
  const url = image && image.url ? image.url : '';
  if (card.dataset.url === url && card.firstChild) return;
  card.dataset.url = url;
  if (url) {
    card.replaceChildren(protectMedia(h('img', { src: url, alt: '' })));
  } else {
    card.replaceChildren(h('div', { class: 'img-placeholder' }, 'Brak obrazka'));
  }
}

export function renderStage2(root, stage2) {
  ensureLayout(root, 'stage2', build);
  const nav = role(root, 'nav');
  nav.replaceChildren(...Array.from({ length: 8 }, (_, i) => {
    const n = i + 1;
    const outcome = stage2.outcomes[i] || 'PENDING';
    return h('div', {
      class: `num-btn status-${outcome}${stage2.selectedQuestion === n ? ' active' : ''}`,
      dataset: { number: n, outcome },
    }, n);
  }));

  let focused = false;
  root.querySelectorAll('.img-card').forEach((card, i) => {
    const image = stage2.images[i];
    fillCard(card, image);
    const isFocused = stage2.focusedImageIndex === i;
    focused = focused || isFocused;
    card.classList.toggle('unblurred', !!(image && image.revealed));
    card.classList.toggle('active-center', isFocused);
  });
  role(root, 'backdrop').classList.toggle('visible', focused);
}
