(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('handler.html');

  const queuedBody = document.getElementById('queuedTicketsBody');
  const activeTicketEl = document.getElementById('activeTicket');
  const activeSessionEl = document.getElementById('activeSession');
  const wsEvents = document.getElementById('wsEvents');
  const callSelectedBtn = document.getElementById('callSelectedBtn');
  const handlerSummary = document.getElementById('handlerSummary');

  let currentTicket = null;
  let queuedTickets = [];
  let handlerContext = null;
  let ws = null;

  function renderSummary(metrics) {
    if (!handlerSummary) return;
    handlerSummary.innerHTML = `
      <div class="card"><div class="small">WAITING</div><div class="kpi">${metrics.waiting_count || 0}</div></div>
      <div class="card"><div class="small">CALLED</div><div class="kpi">${metrics.called_count || 0}</div></div>
      <div class="card"><div class="small">IN SERVICE</div><div class="kpi">${metrics.serving_count || 0}</div></div>
      <div class="card"><div class="small">ON-HOLD</div><div class="kpi">${metrics.hold_count || 0}</div></div>
      <div class="card"><div class="small">NO SHOW</div><div class="kpi">${metrics.no_show_count || 0}</div></div>
      <div class="card"><div class="small">COMPLETED</div><div class="kpi">${metrics.completed_count || 0}</div></div>
    `;
  }

  function renderSelectedTicket() {
    if (!currentTicket) {
      activeTicketEl.innerHTML = '<em>Select a ticket from the table.</em>';
      callSelectedBtn.disabled = true;
      return;
    }

    activeTicketEl.innerHTML = `
      <p><strong>Ticket #:</strong> ${currentTicket.ticket_number}</p>
      <p><strong>Name:</strong> ${currentTicket.crew_name || '-'}</p>
      <p><strong>Crew ID:</strong> ${currentTicket.crew_identifier || '-'}</p>
      <p><strong>Status:</strong> ${window.Utils.statusBadge(currentTicket.status)}</p>
      <p><strong>Queue Type ID:</strong> ${currentTicket.queue_type_id}</p>
      <p><strong>Queued At:</strong> ${window.Utils.formatDateTime(currentTicket.created_at)}</p>
    `;
    callSelectedBtn.disabled = currentTicket.status !== 'WAITING';
  }

  function renderQueueTable() {
    if (!queuedBody) return;
    if (!queuedTickets.length) {
      queuedBody.innerHTML = '<tr><td colspan="6"><em>No queued tickets available for this window.</em></td></tr>';
      return;
    }

    queuedBody.innerHTML = queuedTickets.map((ticket) => {
      const selectedClass = currentTicket && currentTicket.id === ticket.id ? 'ticket-selected' : '';
      return `
        <tr class="ticket-row ${selectedClass}" data-ticket-id="${ticket.id}">
          <td>${ticket.ticket_number}</td>
          <td>${ticket.crew_identifier || '-'}</td>
          <td>${ticket.crew_name || '-'}</td>
          <td>${ticket.queue_type_id}</td>
          <td>${window.Utils.statusBadge(ticket.status)}</td>
          <td>${window.Utils.formatDateTime(ticket.created_at)}</td>
        </tr>
      `;
    }).join('');

    queuedBody.querySelectorAll('tr.ticket-row').forEach((row) => {
      row.addEventListener('click', () => {
        const id = Number(row.dataset.ticketId);
        currentTicket = queuedTickets.find((t) => t.id === id) || null;
        renderQueueTable();
        renderSelectedTicket();
      });
    });
  }

  async function refreshContext() {
    const contextResult = await window.Api.apiRequest('/tickets/handler/context');
    window.App.setDebug('debugPanel', contextResult);
    if (!contextResult.ok || !contextResult.data || !contextResult.data.result || !contextResult.data.result.Access) {
      activeSessionEl.textContent = window.Utils.toPrettyJson(contextResult.data || {});
      return false;
    }

    handlerContext = contextResult.data;
    activeSessionEl.textContent = window.Utils.toPrettyJson(handlerContext);
    if (handlerContext.active_ticket) {
      currentTicket = handlerContext.active_ticket;
    }
    return true;
  }

  async function refreshQueue() {
    const result = await window.Api.apiRequest('/tickets/handler/queue');
    if (!result.ok || !result.data || !result.data.Data) return;
    queuedTickets = result.data.Data;

    if (currentTicket) {
      const stillExists = queuedTickets.some((t) => t.id === currentTicket.id);
      if (!stillExists && currentTicket.status === 'WAITING') {
        currentTicket = null;
      }
    }

    renderQueueTable();
    renderSelectedTicket();
  }

  async function refreshMetrics() {
    const result = await window.Api.apiRequest('/tickets/handler/dashboard');
    if (result.ok && result.data) renderSummary(result.data);
  }

  function connectHandlerSocket() {
    if (!handlerContext || !handlerContext.handler_id || !handlerContext.window_id) return;
    if (ws) ws.close();

    ws = window.WS.connectWebSocket(`/realtime/ws/handler/${handlerContext.handler_id}?windowId=${handlerContext.window_id}`, {
      onOpen: () => { wsEvents.textContent = 'WebSocket connected\n'; },
      onMessage: (msg) => {
        wsEvents.textContent += `${new Date().toISOString()} ${window.Utils.toPrettyJson(msg)}\n`;
      },
      onClose: () => { wsEvents.textContent += 'WebSocket closed\n'; }
    });
  }

  async function ticketAction(path) {
    if (!currentTicket || !currentTicket.id) {
      alert('Select or call a ticket first.');
      return;
    }

    const payload = {
      handler_id: handlerContext ? handlerContext.handler_id : 0,
      ticket_id: Number(currentTicket.id),
      reason: (document.getElementById('notes').value || '').trim() || null
    };

    const result = await window.Api.apiRequest(path, { method: 'POST', body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);

    if (result.ok && result.data && result.data.ticket) {
      currentTicket = result.data.ticket;
    }

    await refreshQueue();
    await refreshMetrics();
    renderSelectedTicket();
  }

  callSelectedBtn.onclick = () => ticketAction('/tickets/handler/call');
  document.getElementById('startServiceBtn').onclick = () => ticketAction('/tickets/handler/start-service');
  document.getElementById('holdBtn').onclick = () => ticketAction('/tickets/handler/hold');
  document.getElementById('noShowBtn').onclick = () => ticketAction('/tickets/handler/no-show');
  document.getElementById('recallBtn').onclick = () => ticketAction('/tickets/handler/recall');
  document.getElementById('completeBtn').onclick = () => ticketAction('/tickets/handler/complete');

  async function bootstrap() {
    const ok = await refreshContext();
    if (!ok) return;
    connectHandlerSocket();
    await refreshQueue();
    await refreshMetrics();
    renderSelectedTicket();
  }

  bootstrap();
  setInterval(async () => {
    await refreshQueue();
    await refreshMetrics();
  }, 5000);
})();
