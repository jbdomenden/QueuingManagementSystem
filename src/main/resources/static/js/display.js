(function () {
  window.App.renderNav('display.html');

  const displayIdInput = document.getElementById('displayId');
  const wsLog = document.getElementById('wsLog');
  let socket = null;
  let pollHandle = null;

  function rowsHtml(rows) {
    return (rows || []).map(t => `
      <tr>
        <td>${t.ticket_number}</td>
        <td>${t.queue_type_name || t.queue_type_id}</td>
        <td>${window.Utils.statusBadge(t.status)}</td>
        <td>${t.assigned_window_name || t.assigned_window_id || ''}</td>
        <td>${window.Utils.formatDateTime(t.created_at)}</td>
        <td>${t.waitingDisplay || window.Utils.formatDuration(t.waitingSeconds)}</td>
        <td>${t.servedDisplay || window.Utils.formatDuration(t.servedSeconds)}</td>
      </tr>
    `).join('');
  }

  function renderSnapshot(snapshot) {
    document.getElementById('queuedBody').innerHTML = rowsHtml(snapshot.queued);
    document.getElementById('servingBody').innerHTML = rowsHtml(snapshot.now_serving);
    document.getElementById('skippedBody').innerHTML = rowsHtml(snapshot.skipped);
    document.getElementById('displayMeta').textContent = window.Utils.toPrettyJson(snapshot.display || {});
  }

  async function fetchSnapshot() {
    const id = Number(displayIdInput.value || 0);
    if (!id) return;
    const result = await window.Api.apiRequest(`/displays/snapshot/${id}`);
    window.App.setDebug('debugPanel', result);
    if (result.ok && result.data) {
      renderSnapshot(result.data);
    }
  }

  function connectWs() {
    const id = Number(displayIdInput.value || 0);
    if (!id) return;
    if (socket) socket.close();

    socket = window.WS.connectWebSocket(`/realtime/ws/display/${id}`, {
      onOpen: () => { wsLog.textContent += 'connected\n'; },
      onMessage: (msg) => {
        wsLog.textContent += `${new Date().toISOString()} ${window.Utils.toPrettyJson(msg)}\n`;
        if (msg && msg.payload && msg.payload.queued && msg.payload.now_serving) {
          renderSnapshot(msg.payload);
        }
      },
      onClose: () => { wsLog.textContent += 'closed\n'; }
    });
  }

  document.getElementById('connectBtn').onclick = () => {
    connectWs();
    fetchSnapshot();
    if (pollHandle) clearInterval(pollHandle);
    pollHandle = setInterval(fetchSnapshot, 5000);
  };
})();
