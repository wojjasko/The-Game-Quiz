export const TEAMS = ['BLUE', 'RED'];
export const PHASES = ['LOBBY', 'STAGE_1', 'STAGE_2', 'STAGE_3', 'FINISHED'];
export const ROW_POINTS = [5, 10, 15, 20];
export const TILE_STATUSES = ['DEFAULT', 'BLUE', 'RED', 'INCORRECT', 'BLUE_STEAL', 'RED_STEAL', 'SELECTED'];
export const OUTCOMES = ['PENDING', 'BLUE', 'RED', 'NONE'];
export const ANSWER_LETTERS = ['A', 'B', 'C', 'D'];

export const TEAM_LABEL = { BLUE: 'Niebiescy', RED: 'Czerwoni' };
export const PHASE_LABEL = {
  LOBBY: 'Poczekalnia',
  STAGE_1: 'Etap 1',
  STAGE_2: 'Etap 2',
  STAGE_3: 'Etap 3',
  FINISHED: 'Koniec',
};
/** Shown on the player screen (no Polish letters: the projector font lacks them). */
export const PHASE_SUBTITLE = {
  LOBBY: 'Poczekalnia',
  STAGE_1: 'Runda 1: Matryca Pytan',
  STAGE_2: 'Runda 2: Co to za gra?',
  STAGE_3: 'Runda 3: Karty',
  FINISHED: 'Koniec gry',
};
/** Big title of the stage-change animation: [headline, subline]. */
export const PHASE_INTRO = {
  LOBBY: ['POCZEKALNIA', 'Czekamy na graczy'],
  STAGE_1: ['ETAP 1', 'Matryca pytan'],
  STAGE_2: ['ETAP 2', 'Co to za gra?'],
  STAGE_3: ['ETAP 3', 'Karty'],
  FINISHED: ['KONIEC GRY', 'Wyniki'],
};
export const TILE_STATUS_LABEL = {
  DEFAULT: 'Domyślny',
  BLUE: 'Niebiescy',
  RED: 'Czerwoni',
  INCORRECT: 'Błędna odpowiedź',
  BLUE_STEAL: 'Niebiescy (steal)',
  RED_STEAL: 'Czerwoni (steal)',
  SELECTED: 'Wybrany',
};
export const OUTCOME_LABEL = {
  PENDING: 'W trakcie',
  BLUE: 'Niebiescy',
  RED: 'Czerwoni',
  NONE: 'Nikt',
};
export const RESULT_LABEL = {
  NONE: '',
  BLUE_CORRECT: 'Niebiescy: dobrze',
  BLUE_WRONG: 'Niebiescy: źle',
  RED_CORRECT: 'Czerwoni: dobrze',
  RED_WRONG: 'Czerwoni: źle',
};

export function otherTeam(team) {
  return team === 'BLUE' ? 'RED' : 'BLUE';
}
