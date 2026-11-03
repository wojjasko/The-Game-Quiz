// Host panel of the lobby: review the question set drawn for this game (with answers) and re-draw what you dislike.
// The questions themselves live in data/*.json on the server; this panel only shows and draws them.
import { h, role, ensureLayout } from '../core/dom.js';
import { ROW_POINTS } from '../core/constants.js';

function renderStage1(box, host, ctx) {
    box.replaceChildren(...host.stage1Columns.map((column, col) =>
        h('div', { class: 'set-block', dataset: { column: col } },
            h('div', { class: 'set-head' },
                h('strong', null, `Kolumna ${col + 1}: ${column.name}`),
                h('button', { type: 'button', class: 'btn btn-small btn-warn', onclick: () => ctx.run(ctx.api.rerollColumn(col)) },
                    '🎲 Losuj kolumnę')),
            ...column.questions.map((q, row) => h('div', { class: 'set-line' },
                h('span', { class: 'row-label' }, `${ROW_POINTS[row]} pkt`),
                h('span', null, q.text, ' → ', h('b', { class: 'answer' }, q.answer || '(brak odpowiedzi)'),
                    q.image ? h('span', { class: 'muted' }, ` [obrazek: ${q.image}]`) : null,
                    q.sound ? h('span', { class: 'muted' }, ` [dźwięk: ${q.sound}]`) : null))))));
}

function renderStage2(box, host, ctx) {
    box.replaceChildren(...host.stage2Questions.map((q, i) =>
        h('div', { class: 'set-line', dataset: { question: i + 1 } },
            h('span', { class: 'row-label' }, `Pytanie ${i + 1}`),
            h('span', null, h('b', { class: 'answer' }, q.answer), h('span', { class: 'muted' }, ` [${q.images.join(', ')}]`)),
            h('button', { type: 'button', class: 'btn btn-small btn-warn', onclick: () => ctx.run(ctx.api.rerollQuestion(i + 1)) },
                '🎲 Losuj'))));
}

function renderStage3(box, host, ctx) {
    box.replaceChildren(...host.stage3Cards.map((card) =>
        h('div', { class: 'set-line', dataset: { cardId: card.id } },
            h('span', { class: 'row-label' }, card.category),
            h('span', null, card.question, ' → ', h('b', { class: 'answer' }, card.correctAnswer)),
            h('button', { type: 'button', class: 'btn btn-small btn-warn', disabled: card.used,
                onclick: () => ctx.run(ctx.api.swapCard(card.id)) }, '🎲 Wymień'))));
}

export function renderLobbyPanel(root, host, ctx) {
    ensureLayout(root, 'lobby', (r) => r.append(
        h('section', { class: 'card' },
            h('h2', null, 'Zestaw pytań na tę grę'),
            h('p', { class: 'muted' },
                'Zestaw jest losowany z bazy pytań (pliki w katalogu data/ na serwerze). Przed startem możesz przelosować całość albo pojedyncze pozycje. Po starcie gry zestaw jest zablokowany.'),
            h('div', { class: 'row' },
                h('button', { type: 'button', class: 'btn btn-danger', dataset: { role: 'redraw' },
                    onclick: () => ctx.run(ctx.api.redrawQuestions()) }, '🎲 Losuj cały zestaw ponownie')),
            h('h3', null, 'Etap 1 – kategorie i pytania'), h('div', { dataset: { role: 'set1' } }),
            h('h3', null, 'Etap 2 – co to za gra?'), h('div', { dataset: { role: 'set2' } }),
            h('h3', null, 'Etap 3 – karty na stole'), h('div', { dataset: { role: 'set3' } }))));
    renderStage1(role(root, 'set1'), host, ctx);
    renderStage2(role(root, 'set2'), host, ctx);
    renderStage3(role(root, 'set3'), host, ctx);
}
