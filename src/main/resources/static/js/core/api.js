// fetch wrapper + typed endpoint helpers matching docs/API.md.

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function request(method, path, { body, token } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['X-Host-Token'] = token;
  let res;
  try {
    res = await globalThis.fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('Brak połączenia z serwerem.', 0);
  }
  let data = null;
  if (res.status !== 204) {
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch { data = null; }
    }
  }
  if (!res.ok) {
    throw new ApiError((data && data.error) || `Błąd serwera (${res.status}).`, res.status);
  }
  return data;
}

const enc = encodeURIComponent;

// ---- public endpoints -------------------------------------------------------
export const createGame = (accessCode) =>
  request('POST', '/api/games', { body: accessCode ? { accessCode } : {} });
export const getGame = (code) => request('GET', `/api/games/${enc(code)}`);
export const joinGame = (code, name, team) =>
  request('POST', `/api/games/${enc(code)}/players`, { body: { name, team } });
export const leaveGame = (code, playerId, token) =>
  request('DELETE', `/api/games/${enc(code)}/players/${enc(playerId)}`, { token });

// ---- host endpoints ---------------------------------------------------------
export function hostApi(code, token) {
  const base = `/api/games/${enc(code)}`;
  const s1 = `${base}/stage1`;
  const s2 = `${base}/stage2`;
  const s3 = `${base}/stage3`;
  const call = (method, path, body) => request(method, path, { body, token });
  return {
    get: () => call('GET', `${base}/host`),
    media: () => call('GET', `${base}/host/media`),
    setPhase: (phase) => call('PUT', `${base}/phase`, { phase }),
    renameTeam: (team, name) => call('PUT', `${base}/teams/${team}/name`, { name }),
    addScore: (team, delta) => call('POST', `${base}/teams/${team}/score`, { delta }),
    setScore: (team, value) => call('PUT', `${base}/teams/${team}/score`, { value }),
    movePlayer: (playerId, team) => call('PUT', `${base}/players/${enc(playerId)}/team`, { team }),
    kickPlayer: (playerId) => call('DELETE', `${base}/players/${enc(playerId)}`),
    // stage 1
    setCategories: (categories) => call('PUT', `${s1}/categories`, { categories }),
    setTile: (row, col, status) => call('PUT', `${s1}/tiles/${row}/${col}`, { status }),
    activateTile: (row, col) => call('POST', `${s1}/tiles/${row}/${col}/activate`),
    saveQuestion: (row, col, q) => call('PUT', `${s1}/questions/${row}/${col}`,
      { text: q.text, image: q.image, sound: q.sound }),
    setCurrentQuestion: (text) => call('PUT', `${s1}/current-question`, { text }),
    clearCurrentQuestion: () => call('DELETE', `${s1}/current-question`),
    setImageZoom: (zoomed) => call('PUT', `${s1}/image-zoom`, { zoomed }),
    timer: (team, action) => call('POST', `${s1}/timers/${team}/${action}`),
    spendSteal: (team) => call('POST', `${s1}/steals/${team}/spend`),
    resetSteals: () => call('POST', `${s1}/steals/reset`),
    timeBonus: (team) => call('POST', `${s1}/time-bonus/${team}`),
    resetBoard: () => call('POST', `${s1}/reset`),
    // stage 2
    selectQuestion: (number) => call('PUT', `${s2}/question`, { number }),
    setOutcome: (number, outcome) => call('PUT', `${s2}/questions/${number}/outcome`, { outcome }),
    focusImage: (imageIndex) => call('PUT', `${s2}/focus`, { imageIndex }),
    unfocusImage: () => call('DELETE', `${s2}/focus`),
    // stage 3
    swapCard: (id) => call('POST', `${s3}/cards/${id}/swap`),
    openCard: (id) => call('POST', `${s3}/cards/${id}/open`),
    useLifeline: (team) => call('POST', `${s3}/lifeline`, { team }),
    answer: (team, correct) => call('POST', `${s3}/answer`, { team, correct }),
    closeQuestion: () => call('POST', `${s3}/close`),
  };
}
