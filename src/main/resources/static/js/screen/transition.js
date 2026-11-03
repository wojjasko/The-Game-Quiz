// Full-screen effects of the player screen: the start countdown and the stage-change animation.
import { h } from '../core/dom.js';
import { PHASE_INTRO } from '../core/constants.js';

export const TRANSITION_MS = 2300;

/** Plays the "ETAP N" animation. The new view is already rendered underneath; the panels slide away to reveal it. */
export function playPhaseTransition(phase) {
    const intro = PHASE_INTRO[phase];
    if (!intro) return null;
    const doc = globalThis.document;
    doc.querySelectorAll('.stage-transition').forEach((el) => el.remove());
    const el = h('div', { class: 'stage-transition', dataset: { phase } },
        h('div', { class: 'panel panel-top' }), h('div', { class: 'panel panel-bottom' }),
        h('div', { class: 'intro' },
            h('div', { class: 'intro-title' }, intro[0]),
            h('div', { class: 'intro-sub' }, intro[1])));
    doc.body.appendChild(el);
    globalThis.setTimeout(() => el.remove(), TRANSITION_MS);
    return el;
}

/** Shows (or updates, or hides when `seconds` is null) the big start countdown. */
export function updateCountdown(seconds) {
    const doc = globalThis.document;
    let overlay = doc.querySelector('.countdown-overlay');
    if (seconds === null || seconds === undefined) {
        if (overlay) overlay.remove();
        return;
    }
    if (!overlay) {
        overlay = h('div', { class: 'countdown-overlay' },
            h('div', { class: 'countdown-label' }, 'GRA STARTUJE ZA'),
            h('div', { class: 'countdown-ring' }));
        doc.body.appendChild(overlay);
    }
    const ring = overlay.querySelector('.countdown-ring');
    const current = ring.querySelector('.countdown-number');
    if (current && current.dataset.value === String(seconds)) return;
    // A new element every second restarts the pop animation.
    const number = h('div', { class: `countdown-number${seconds <= 3 ? ' final' : ''}`, dataset: { value: seconds } }, String(seconds));
    ring.replaceChildren(number);
}
