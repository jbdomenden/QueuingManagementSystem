(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('handler.html');

  const auth = window.StorageHelper.getAuth();
  const departmentIdInput = document.getElementById('departmentId');
  const handlerIdInput = document.getElementById('handlerId');
  const windowIdInput = document.getElementById('windowId');
  const liveCountersEl = document.getElementById('liveCounters');
  const activeSessionEl = document.getElementById('activeSession');
  const activeTicketEl = document.getElementById('activeTicket');
  const wsEvents = document.getElementById('wsEvents');
  let currentTicket = null;
  let ws = null;

  departmentIdInput.value = auth.department_id || '';

  async function refreshLiveCounters() {
    const departmentId = Number(departmentIdInput.value || 0);
    if (!departmentId) return;
    const result = await window.Api.apiRequest(`/tickets/live/${departmentId}`);
    window.App.setDebug('debugPanel', result);

    const rows = (result.data && result.data.Data) || [];
    const count = (status) => rows.filter(x => x.status === status).length;

    liveCountersEl.innerHTML = `
      <div class="inline">
        <div class="card"><div class="small">WAITING</div><div class="kpi">${count('WAITING')}</div></div>
        <div class="card"><div class="small">CALLED</div><div class="kpi">${count('CALLED')}</div></div>
        <div class="card"><div class="small">IN_SERVICE</div><div class="kpi">${count('IN_SERVICE')}</div></div>
        <div class="card"><div class="small">SKIPPED</div><div class="kpi">${count('SKIPPED')}</div></div>
        <div class="card"><div class="small">COMPLETED</div><div class="kpi">${count('COMPLETED')}</div></div>
      </div>
    `;
  }

  function renderCurrentTicket() {
    if (!currentTicket) {
      activeTicketEl.innerHTML = '<em>No active ticket selected</em>';
      return;
    }
    activeTicketEl.innerHTML = `
      <p><strong>ID:</strong> ${currentTicket.id}</p>
      <p><strong>Number:</strong> ${currentTicket.ticket_number}</p>
      <p><strong>Status:</strong> ${window.Utils.statusBadge(currentTicket.status)}</p>
      <p><strong>Created:</strong> ${window.Utils.formatDateTime(currentTicket.created_at)}</p>
      <p><strong>Called:</strong> ${window.Utils.formatDateTime(currentTicket.called_at || '')}</p>
      <p><strong>Completed:</strong> ${window.Utils.formatDateTime(currentTicket.completed_at || '')}</p>
    `;
  }

  function connectHandlerSocket() {
    if (ws) ws.close();
    const handlerId = Number(handlerIdInput.value || 0);
    const windowId = Number(windowIdInput.value || 0);
    if (!handlerId || !windowId) return;

    ws = window.WS.connectWebSocket(`/realtime/ws/handler/${handlerId}?windowId=${windowId}`, {
      onOpen: () => { wsEvents.textContent = 'WebSocket connected\n'; },
      onMessage: (msg) => {
        wsEvents.textContent += `${new Date().toISOString()} ${window.Utils.toPrettyJson(msg)}\n`;
      },
      onClose: () => { wsEvents.textContent += 'WebSocket closed\n'; }
    });
  }

  document.getElementById('startSessionBtn').onclick = async () => {
    const payload = { handler_id: Number(handlerIdInput.value), window_id: Number(windowIdInput.value) };
    const result = await window.Api.apiRequest('/handlers/start-session', { method: 'POST', body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    activeSessionEl.textContent = window.Utils.toPrettyJson(result.data);
    connectHandlerSocket();
  };

  document.getElementById('endSessionBtn').onclick = async () => {
    const payload = { handler_id: Number(handlerIdInput.value), window_id: Number(windowIdInput.value) };
    const result = await window.Api.apiRequest('/handlers/end-session', { method: 'POST', body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    activeSessionEl.textContent = window.Utils.toPrettyJson(result.data);
    if (ws) ws.close();
  };

  document.getElementById('callNextBtn').onclick = async () => {
    const payload = { handler_id: Number(handlerIdInput.value) };
    const result = await window.Api.apiRequest('/tickets/call-next', { method: 'POST', body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    currentTicket = result.data;
    renderCurrentTicket();
    refreshLiveCounters();
  };

  async function ticketAction(path) {
    if (!currentTicket || !currentTicket.id) {
      alert('Call next first or set an active ticket.');
      return;
    }
    const payload = {
      handler_id: Number(handlerIdInput.value),
      ticket_id: Number(currentTicket.id),
      notes: document.getElementById('notes').value || null
    };
    const result = await window.Api.apiRequest(path, { method: 'POST', body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    await refreshLiveCounters();
  }

  document.getElementById('startServiceBtn').onclick = () => ticketAction('/tickets/start-service');
  document.getElementById('skipBtn').onclick = () => ticketAction('/tickets/skip');
  document.getElementById('recallBtn').onclick = () => ticketAction('/tickets/recall');
  document.getElementById('completeBtn').onclick = () => ticketAction('/tickets/complete');

  refreshLiveCounters();
  setInterval(refreshLiveCounters, 5000);
})();
