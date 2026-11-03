// Always-visible host sections: top bar (code, links, phases), teams and scores, players.
import { h, role, ensureLayout, syncInput, markDirtyOnInput, clearDirty, toast } from '../core/dom.js';
import { TEAMS, TEAM_LABEL, PHASES, PHASE_LABEL } from '../core/constants.js';

const SCORE_BUTTONS = [5, 10, 15, 20, -5, -10];

export function renderTopbar(root, host, ctx) {
    ensureLayout(root, 'topbar', (r) => {
        r.append(
            h('a', { class: 'btn btn-ok', href: `/screen.html?game=${encodeURIComponent(ctx.code)}`, target: '_blank', rel: 'noopener' },
                'Otwórz ekran gry'),
            h('button', { type: 'button', class: 'btn', dataset: { role: 'copy-link' }, onclick: async () => {
                    const link = `${globalThis.location.origin}/`;
                    try {
                        await globalThis.navigator.clipboard.writeText(link);
                        toast('Skopiowano adres dla graczy.', 'ok');
                    } catch {
                        toast(link, 'ok');
                    }
                } }, 'Kopiuj adres dla graczy'),
            h('button', { type: 'button', class: 'btn btn-primary host-start', dataset: { role: 'start' },
                onclick: () => ctx.run(ctx.api.start()) }, '▶ START GRY (odliczanie 10 s)'),
            h('span', { class: 'host-countdown', dataset: { role: 'countdown' }, hidden: true }),
            h('button', { type: 'button', class: 'btn btn-warn', dataset: { role: 'cancel-start' }, hidden: true,
                onclick: () => ctx.run(ctx.api.cancelStart()) }, 'Anuluj odliczanie'),
            h('div', { class: 'host-phases', dataset: { role: 'phases' } }, ...PHASES.map((phase) =>
                h('button', { type: 'button', class: 'btn btn-phase', dataset: { phase },
                    onclick: () => ctx.run(ctx.api.setPhase(phase)) }, PHASE_LABEL[phase]))),
            h('button', { type: 'button', class: 'btn btn-danger', dataset: { role: 'close-room' }, onclick: async () => {
                    if (!globalThis.confirm('Zamknąć pokój? Gra zakończy się dla wszystkich graczy.')) return;
                    try {
                        await ctx.api.closeRoom();
                        ctx.goHome();
                    } catch (e) {
                        toast(e.message);
                    }
                } }, 'Zamknij pokój'));
    });
    const countdown = host.game.countdown;
    const counting = countdown !== null && countdown !== undefined;
    const lobby = host.game.phase === 'LOBBY';
    role(root, 'start').hidden = !lobby || counting;
    role(root, 'countdown').hidden = !counting;
    role(root, 'countdown').textContent = counting ? `Start za ${countdown} s` : '';
    role(root, 'cancel-start').hidden = !counting;
    root.querySelectorAll('.btn-phase').forEach((b) => b.classList.toggle('current', b.dataset.phase === host.game.phase));
}

export function renderTeams(root, host, ctx) {
    ensureLayout(root, 'teams', (r) => {
        r.append(h('h2', null, 'Drużyny i punkty'), ...TEAMS.map((team) => {
            const input = markDirtyOnInput(h('input', { type: 'text', maxlength: 30, dataset: { role: 'name' } }));
            return h('div', { class: `team-card team-card-${team.toLowerCase()}`, dataset: { team } },
                h('strong', null, TEAM_LABEL[team]),
                h('div', { class: 'row' }, input,
                    h('button', { type: 'button', class: 'btn btn-primary', onclick: async () => {
                            if (await ctx.run(ctx.api.renameTeam(team, input.value))) clearDirty(input);
                        } }, 'Zmień nazwę')),
                h('div', { class: 'team-score', dataset: { role: 'score' } }, '0'),
                h('div', { class: 'row' },
                    ...SCORE_BUTTONS.map((delta) => h('button', { type: 'button', class: `btn ${delta < 0 ? 'btn-danger' : ''}`,
                        onclick: () => ctx.run(ctx.api.addScore(team, delta)) }, delta > 0 ? `+${delta}` : String(delta))),
                    h('button', { type: 'button', class: 'btn btn-warn', onclick: () => ctx.run(ctx.api.setScore(team, 0)) }, '0')));
        }));
    });
    for (const team of TEAMS) {
        const box = root.querySelector(`.team-card[data-team="${team}"]`);
        const data = host.game.teams[team];
        syncInput(role(box, 'name'), data.name);
        role(box, 'score').textContent = `${data.score} pkt`;
    }
}

export function renderPlayers(root, host, ctx) {
    const rows = host.players.map((player) => h('div', { class: 'row', dataset: { playerId: player.id } },
        h('span', { class: 'row-label' }, player.name),
        h('select', { onchange: (e) => ctx.run(ctx.api.movePlayer(player.id, e.target.value)) },
            ...TEAMS.map((team) => h('option', { value: team, selected: team === player.team }, TEAM_LABEL[team]))),
        h('button', { type: 'button', class: 'btn btn-danger btn-small', onclick: () => ctx.run(ctx.api.kickPlayer(player.id)
                .then(() => ctx.api.get())) }, 'Usuń')));
    root.replaceChildren(
        h('h2', null, `Gracze (${host.players.length})`),
        ...(rows.length ? rows : [h('p', { class: 'muted' }, 'Nikt jeszcze nie dołączył.')]));
}
