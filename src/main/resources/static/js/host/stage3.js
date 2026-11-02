// Host panel of stage 3: cards on the table, the open question with its correct answer, lifelines, resolution.
import { h, role, ensureLayout } from '../core/dom.js';
import { TEAMS, TEAM_LABEL, RESULT_LABEL, ANSWER_LETTERS } from '../core/constants.js';

function renderCards(list, s3, openId, ctx) {
  list.replaceChildren(...s3.table.map((card) => {
    const isOpen = card.id === openId;
    const status = card.result !== 'NONE' ? RESULT_LABEL[card.result] : (card.used ? 'zamknięta bez punktów' : '');
    return h('div', { class: `card-line${card.used ? ' used' : ''}${isOpen ? ' open' : ''}`, dataset: { cardId: card.id } },
      h('span', null, h('strong', null, card.category), status ? ` (${status})` : ''),
      h('div', { class: 'row' },
        h('button', { type: 'button', class: 'btn btn-small', disabled: card.used || isOpen,
          onclick: () => ctx.run(ctx.api.swapCard(card.id)) }, 'Wymień kartę'),
        h('button', { type: 'button', class: 'btn btn-ok btn-small', disabled: card.used,
          onclick: () => ctx.run(ctx.api.openCard(card.id)) }, 'Otwórz pytanie')));
  }));
}

function renderActive(box, host, ctx) {
  const s3 = host.game.stage3;
  const question = s3.question;
  box.hidden = !question;
  if (!question) return;
  const secret = host.stage3;
  box.replaceChildren(
    h('h3', null, 'Otwarte pytanie: ', h('span', { style: 'color:#ff007f' }, question.category)),
    h('p', null, question.text),
    h('ol', { type: 'A' }, ...question.answers.map((text) => h('li', null, text))),
    h('p', null, 'Poprawna odpowiedź: ',
      h('b', { class: 'correct-answer', dataset: { role: 'correct' } },
        secret ? `${secret.correctAnswer} (${secret.correctAnswerText})` : '?')),
    h('h3', null, 'Koło ratunkowe 50:50'),
    h('div', { class: 'row' }, ...TEAMS.map((team) => h('button', { type: 'button', class: 'btn btn-warn',
      disabled: s3.lifelineUsed[team], onclick: () => ctx.run(ctx.api.useLifeline(team)) },
    `50:50 – ${TEAM_LABEL[team]}`))),
    h('h3', null, 'Rozstrzygnięcie odpowiedzi'),
    ...TEAMS.map((team) => h('div', { class: 'row', dataset: { team } },
      h('button', { type: 'button', class: 'btn btn-ok', onclick: () => ctx.run(ctx.api.answer(team, true)) },
        `${TEAM_LABEL[team]} – POPRAWNA (+${s3.multipliers[team]} pkt)`),
      h('button', { type: 'button', class: 'btn btn-danger', onclick: () => ctx.run(ctx.api.answer(team, false)) },
        `${TEAM_LABEL[team]} – BŁĘDNA (reset mnożnika)`))),
    h('button', { type: 'button', class: 'btn', onclick: () => ctx.run(ctx.api.closeQuestion()) },
      'Zamknij bez przyznawania punktów'));
}

export function renderStage3Panel(root, host, ctx) {
  ensureLayout(root, 'stage3', (r) => r.append(
    h('section', { class: 'card' },
      h('h2', null, 'Etap 3 – Karty'),
      h('div', { class: 'active-question', dataset: { role: 'active' }, hidden: true }),
      h('h3', null, 'Karty na stole'), h('div', { dataset: { role: 'cards' } }))));

  const s3 = host.game.stage3;
  const openId = host.stage3 ? host.stage3.activeCardId : null;
  renderCards(role(root, 'cards'), s3, openId, ctx);
  renderActive(role(root, 'active'), host, ctx);
}
