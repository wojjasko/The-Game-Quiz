// Host panel of stage 2: select a question, record who answered, reveal / zoom the images.
import { h, role, ensureLayout } from '../core/dom.js';
import { OUTCOMES, OUTCOME_LABEL } from '../core/constants.js';

export function renderStage2Panel(root, host, ctx) {
  ensureLayout(root, 'stage2', (r) => r.append(
      h('section', { class: 'card' },
          h('h2', null, 'Etap 2 – Co to za gra?'),
          h('h3', null, '1. Wybór pytania'), h('div', { class: 'row', dataset: { role: 'questions' } }),
          h('p', { class: 'active-question' }, 'Poprawna odpowiedź: ', h('b', { class: 'correct-answer', dataset: { role: 'answer' } })),
          h('h3', null, '2. Kto odpowiedział na pytanie ', h('span', { dataset: { role: 'selected-label' } })),
          h('div', { class: 'row', dataset: { role: 'outcomes' } }),
          h('h3', null, '3. Obrazki'), h('div', { dataset: { role: 'images' } }))));

  const s2 = host.game.stage2;
  const selected = s2.selectedQuestion;
  role(root, 'selected-label').textContent = String(selected);
  role(root, 'answer').textContent = host.stage2Questions[selected - 1].answer;

  role(root, 'questions').replaceChildren(...s2.outcomes.map((outcome, i) => {
    const number = i + 1;
    return h('button', { type: 'button', class: `btn q-btn status-${outcome}${number === selected ? ' active' : ''}`,
      dataset: { number }, onclick: () => ctx.run(ctx.api.selectQuestion(number)) }, `Pytanie ${number}`);
  }));

  const current = s2.outcomes[selected - 1];
  role(root, 'outcomes').replaceChildren(...OUTCOMES.map((outcome) =>
      h('button', { type: 'button', class: `btn q-btn status-${outcome}${outcome === current ? ' on' : ''}`,
        dataset: { outcome }, onclick: () => ctx.run(ctx.api.setOutcome(selected, outcome)) }, OUTCOME_LABEL[outcome])));

  role(root, 'images').replaceChildren(...s2.images.map((image) => {
    const focused = s2.focusedImageIndex === image.index;
    return h('div', { class: 'card-line', dataset: { image: image.index } },
        h('span', null, `Obrazek ${image.index + 1} `,
            h('strong', { style: `color:${image.revealed ? '#28a745' : '#aaa'}` }, image.revealed ? '(ODSŁONIĘTY)' : '(ZABLUROWANY)'),
            image.url ? null : h('em', { class: 'muted' }, ' – brak pliku na serwerze')),
        h('button', { type: 'button', class: `btn ${focused ? 'btn-danger' : 'btn-ok'}`,
              onclick: () => ctx.run(focused ? ctx.api.unfocusImage() : ctx.api.focusImage(image.index)) },
            focused ? 'WYŁĄCZ (schowaj na miejsce)' : 'WŁĄCZ (wyśrodkuj i odsłoń)'));
  }));
}
