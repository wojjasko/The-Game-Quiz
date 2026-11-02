// STOMP-over-SockJS wrapper. Uses the global `SockJS` and `Stomp` (CDN scripts).

/**
 * Connect, subscribe to the state + timers topics, reconnect after `reconnectMs`
 * when the connection drops and call `onConnected` after EVERY (re)connect so the
 * caller can re-fetch its snapshot. Returns `{ close() }`.
 */
export function connectGame(code, { onState, onTimers, onConnected, onStatus, reconnectMs = 3000 } = {}) {
  let stopped = false;
  let timer = null;
  let client = null;
  const status = (s) => { if (onStatus) onStatus(s); };

  function scheduleReconnect() {
    if (stopped || timer !== null) return;
    status('offline');
    try { if (client && client.connected) client.disconnect(); } catch { /* ignore */ }
    timer = globalThis.setTimeout(() => { timer = null; connect(); }, reconnectMs);
  }

  function connect() {
    if (stopped) return;
    if (typeof globalThis.SockJS === 'undefined' || typeof globalThis.Stomp === 'undefined') {
      status('unavailable');
      if (onConnected) onConnected();
      return;
    }
    try {
      const socket = new globalThis.SockJS('/ws-quiz');
      client = globalThis.Stomp.over(socket);
      client.debug = null;
      client.connect({}, () => {
        client.subscribe(`/topic/game/${code}/state`, (m) => { if (onState) onState(JSON.parse(m.body)); });
        client.subscribe(`/topic/game/${code}/timers`, (m) => { if (onTimers) onTimers(JSON.parse(m.body)); });
        status('online');
        if (onConnected) onConnected();
      }, scheduleReconnect);
    } catch {
      scheduleReconnect();
    }
  }

  connect();
  return {
    close() {
      stopped = true;
      if (timer !== null) globalThis.clearTimeout(timer);
      try { if (client && client.connected) client.disconnect(); } catch { /* ignore */ }
    },
  };
}
