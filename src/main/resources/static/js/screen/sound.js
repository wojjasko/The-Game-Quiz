// Sound of the start countdown on the player screen.
//
// Browsers refuse to play sound on a page the user has not clicked yet, so the screen shows a small speaker button.
// One click on it (or anywhere on the page) unlocks the sound for the rest of the visit.
// The file media/sounds/countdown.mp3 ticks once per second; it is started when the countdown shows 10
// (or at the matching position when a player joins in the middle of it) and stopped when the countdown ends.
import { h } from '../core/dom.js';

export const COUNTDOWN_SRC = '/media/sounds/countdown.mp3';
/** Must match GameSession.START_COUNTDOWN_SECONDS on the server. */
const COUNTDOWN_START_SECONDS = 10;
const VOLUME = 0.6;
const PREF_KEY = 'quiz.sound';

const state = { audio: null, unlocked: false, unlocking: false, enabled: readPreference(), seconds: null, button: null };

function readPreference() {
  try { return globalThis.localStorage.getItem(PREF_KEY) !== 'off'; } catch { return true; }
}

function savePreference(enabled) {
  try { globalThis.localStorage.setItem(PREF_KEY, enabled ? 'on' : 'off'); } catch { /* storage may be blocked */ }
}

function getAudio() {
  if (!state.audio) {
    state.audio = new globalThis.Audio(COUNTDOWN_SRC);
    state.audio.preload = 'auto';
    state.audio.volume = VOLUME;
  }
  return state.audio;
}

function startAt(secondsLeft) {
  const audio = getAudio();
  try { audio.currentTime = Math.max(0, COUNTDOWN_START_SECONDS - secondsLeft); } catch { /* not loaded yet */ }
  const result = audio.play();
  if (result && typeof result.catch === 'function') {
    result.catch(() => { state.unlocked = false; refreshButton(); }); // the browser still blocks the sound
  }
}

function stop() {
  if (!state.audio) return;
  state.audio.pause();
  try { state.audio.currentTime = 0; } catch { /* ignore */ }
}

function refreshButton() {
  const button = state.button;
  if (!button) return;
  const locked = state.enabled && !state.unlocked;
  button.textContent = !state.enabled ? '🔇 Dzwiek wylaczony'
    : (state.unlocked ? '🔊 Dzwiek wlaczony' : '🔇 Kliknij, aby wlaczyc dzwiek');
  button.setAttribute('aria-pressed', String(state.enabled && state.unlocked));
  // while a countdown runs and the sound is still locked, draw attention to the button
  button.classList.toggle('attention', locked && state.seconds !== null);
}

/** Called from a click: plays the sound once, muted, which lets the page play it later without a click. */
export async function unlockSound() {
  if (state.unlocked || state.unlocking) return;
  state.unlocking = true;
  const audio = getAudio();
  try {
    audio.muted = true;
    await audio.play();
    audio.pause();
    audio.currentTime = 0;
    audio.muted = false;
    state.unlocked = true;
  } catch {
    audio.muted = false;
  } finally {
    state.unlocking = false;
  }
  refreshButton();
  if (state.unlocked && state.enabled && state.seconds !== null) startAt(state.seconds); // joined mid-countdown
}

function toggle() {
  if (!state.unlocked) {
    state.enabled = true;
    savePreference(true);
    refreshButton();
    unlockSound();
    return;
  }
  state.enabled = !state.enabled;
  savePreference(state.enabled);
  if (!state.enabled) stop();
  else if (state.seconds !== null) startAt(state.seconds);
  refreshButton();
}

/** Adds the speaker button and unlocks the sound on the first click anywhere on the page. */
export function installSoundToggle() {
  const doc = globalThis.document;
  if (state.button && doc.body.contains(state.button)) return state.button;
  state.button = h('button', { type: 'button', class: 'sound-toggle', onclick: toggle });
  doc.body.appendChild(state.button);
  doc.addEventListener('click', (event) => {
    const onButton = event.target && event.target.closest && event.target.closest('.sound-toggle');
    if (!onButton && state.enabled && !state.unlocked) unlockSound();
  }, true);
  refreshButton();
  return state.button;
}

/**
 * Keeps the sound in step with the server countdown: `seconds` is the value of GameSnapshot.countdown while the
 * countdown runs in the lobby, otherwise null. Plays when the countdown appears, stops when it disappears.
 */
export function syncCountdownSound(seconds) {
  const before = state.seconds;
  state.seconds = seconds === undefined ? null : seconds;
  if (state.seconds === null) {
    if (before !== null) stop();
  } else if (before === null && state.unlocked && state.enabled) {
    startAt(state.seconds);
  }
  refreshButton();
}

/** For tests and debugging. */
export function soundState() {
  return { unlocked: state.unlocked, enabled: state.enabled, seconds: state.seconds };
}
