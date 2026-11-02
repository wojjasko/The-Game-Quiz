// Tiny DOM helpers. Nothing here touches `document` at import time, so the
// modules can be imported under Node + jsdom after `globalThis.document` is set.

const PROPERTY_ATTRS = new Set(['value', 'checked', 'disabled', 'hidden', 'selected']);

/**
 * Create an element. `props` supports: class, dataset, text, on<Event> handlers,
 * boolean/string attributes. Children may be strings, numbers, Nodes or arrays.
 * Strings always become text nodes, never markup.
 */
export function h(tag, props, ...children) {
  const el = globalThis.document.createElement(tag);
  if (props) {
    for (const [key, value] of Object.entries(props)) {
      if (value === undefined || value === null || value === false) continue;
      if (key === 'class' || key === 'className') el.className = value;
      else if (key === 'dataset') Object.entries(value).forEach(([k, v]) => { el.dataset[k] = String(v); });
      else if (key === 'text') el.textContent = value;
      else if (key.startsWith('on') && typeof value === 'function') el.addEventListener(key.slice(2).toLowerCase(), value);
      else if (PROPERTY_ATTRS.has(key)) el[key] = value;
      else el.setAttribute(key, value === true ? '' : String(value));
    }
  }
  appendChildren(el, children);
  return el;
}

function appendChildren(el, children) {
  for (const child of children) {
    if (child === undefined || child === null || child === false) continue;
    if (Array.isArray(child)) appendChildren(el, child);
    else if (typeof child === 'object' && 'nodeType' in child) el.appendChild(child);
    else el.appendChild(globalThis.document.createTextNode(String(child)));
  }
}

export function clear(el) {
  el.replaceChildren();
  return el;
}

export function replaceContent(el, ...children) {
  el.replaceChildren();
  appendChildren(el, children);
  return el;
}

/** Build `build(root)` once per `key`; later calls with the same key keep the DOM. Returns true when rebuilt. */
export function ensureLayout(root, key, build) {
  if (root.dataset.layout === key) return false;
  root.replaceChildren();
  root.dataset.layout = key;
  build(root);
  return true;
}

/** Shortcut for `root.querySelector('[data-role="name"]')`. */
export function role(root, name) {
  return root.querySelector(`[data-role="${name}"]`);
}

/** Update an input from server data unless the user is typing in it or has unsaved edits. */
export function syncInput(input, value) {
  if (!input) return;
  if (input.dataset.dirty === '1') return;
  if (input.ownerDocument && input.ownerDocument.activeElement === input) return;
  input.value = value ?? '';
}

export function markDirtyOnInput(input) {
  input.addEventListener('input', () => { input.dataset.dirty = '1'; });
  return input;
}

export function clearDirty(...inputs) {
  inputs.forEach((i) => { if (i) delete i.dataset.dirty; });
}

export function formatTime(totalSeconds) {
  const s = Math.max(0, Math.floor(Number(totalSeconds) || 0));
  const m = Math.floor(s / 60).toString().padStart(2, '0');
  return `${m}:${(s % 60).toString().padStart(2, '0')}`;
}

/** Show a non-blocking message (replaces alert()). */
export function toast(message, kind = 'error') {
  const doc = globalThis.document;
  let box = doc.getElementById('toasts');
  if (!box) {
    box = doc.createElement('div');
    box.id = 'toasts';
    box.className = 'toasts';
    doc.body.appendChild(box);
  }
  const el = h('div', { class: `toast toast-${kind}`, role: 'status' }, String(message));
  box.appendChild(el);
  globalThis.setTimeout(() => el.remove(), 4500);
  return el;
}

/** Block the context menu / dragging of media on the player screen. */
export function protectMedia(el) {
  el.setAttribute('draggable', 'false');
  el.addEventListener('contextmenu', (e) => e.preventDefault());
  el.addEventListener('dragstart', (e) => e.preventDefault());
  return el;
}
