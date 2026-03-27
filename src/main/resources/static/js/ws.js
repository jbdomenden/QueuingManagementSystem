(function () {
  function toWsUrl(path) {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${location.host}${path}`;
  }

  function connectWebSocket(path, handlers) {
    const socket = new WebSocket(toWsUrl(path));
    const h = handlers || {};

    socket.onopen = () => h.onOpen && h.onOpen(socket);
    socket.onmessage = (event) => {
      let payload = event.data;
      try {
        payload = JSON.parse(event.data);
      } catch {
        payload = { raw: event.data };
      }
      h.onMessage && h.onMessage(payload, socket);
    };
    socket.onerror = (event) => h.onError && h.onError(event, socket);
    socket.onclose = () => h.onClose && h.onClose(socket);

    return socket;
  }

  window.WS = { connectWebSocket };
})();
