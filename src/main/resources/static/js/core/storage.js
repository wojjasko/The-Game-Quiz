// localStorage wrappers; every access may throw (private mode, blocked storage).

function read(key) {
  try { return globalThis.localStorage.getItem(key); } catch { return null; }
}
function write(key, value) {
  try { globalThis.localStorage.setItem(key, value); return true; } catch { return false; }
}
function remove(key) {
  try { globalThis.localStorage.removeItem(key); } catch { /* ignore */ }
}

export const hostTokenKey = (code) => `quiz.hostToken.${code}`;
export const playerIdKey = (code) => `quiz.playerId.${code}`;

export const getHostToken = (code) => read(hostTokenKey(code));
export const setHostToken = (code, token) => write(hostTokenKey(code), token);
export const getPlayerId = (code) => read(playerIdKey(code));
export const setPlayerId = (code, id) => write(playerIdKey(code), id);
export const clearPlayerId = (code) => remove(playerIdKey(code));
