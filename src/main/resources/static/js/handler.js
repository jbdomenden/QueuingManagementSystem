(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('handler.html');

  const auth = window.StorageHelper.getAuth();
  const REFRESH_INTERVAL_MS = 30000;

  const elements = {
    liveDateTime: document.getElementById('terminalLiveDateTime'),
    terminalName: document.getElementById('terminalNameLabel'),
    username: document.getElementById('terminalUserLabel'),
    notice: document.getElementById('terminalNotice'),
    info: {
      mnno: document.getElementById('infoMnno'),
      vessel: document.getElementById('infoVessel'),
      servedBy: document.getElementById('infoServedBy'),
      rank: document.getElementById('infoRank'),
      crewChangeDate: document.getElementById('infoCrewChangeDate'),
      status: document.getElementById('infoStatus'),
      name: document.getElementById('infoName'),
      lastVessel: document.getElementById('infoLastVessel'),
      lastCcDate: document.getElementById('infoLastCcDate')
    },
    buttons: {
      call: document.getElementById('callBtn'),
      start: document.getElementById('startBtn'),
      transfer: document.getElementById('transferBtn'),
      hold: document.getElementById('holdBtn'),
      noShow: document.getElementById('noShowBtn'),
      end: document.getElementById('endBtn')
    },
    tables: {
      all: document.getElementById('allTableBody'),
      hold: document.getElementById('holdTableBody'),
      noshow: document.getElementById('noShowTableBody'),
      completed: document.getElementById('completedTableBody')
    }
  };

  const tableConfigs = {
    all: { defaultStatuses: ['WAITING', 'CALLED', 'IN_SERVICE', 'HOLD', 'SKIPPED', 'COMPLETED'] },
    hold: { defaultStatuses: ['HOLD'] },
    noshow: { defaultStatuses: ['SKIPPED'] },
    completed: { defaultStatuses: ['COMPLETED'] }
  };

  const uiState = {
    handlerContext: null,
    selectedTicketId: null,
    pool: [],
    filters: {
      all: { search: '', status: '' },
      hold: { search: '', status: '' },
      noshow: { search: '', status: '' },
      completed: { search: '', status: '' }
    },
    refreshTimer: null,
    clockTimer: null
  };

  function textOrDash(value) {
    if (value === null || value === undefined || value === '') return '-';
    return value;
  }

  function formatClock(now) {
    return `${now.toLocaleDateString()} ${now.toLocaleTimeString()}`;
  }

  function startClock() {
    const tick = () => {
      if (elements.liveDateTime) elements.liveDateTime.textContent = formatClock(new Date());
    };
    tick();
    if (uiState.clockTimer) clearInterval(uiState.clockTimer);
    uiState.clockTimer = setInterval(tick, 1000);
  }

  function deriveTransaction(ticket) {
    return textOrDash(ticket.transaction_family || ticket.company_transaction_name || ticket.destination_name || (ticket.queue_type_id ? `Queue ${ticket.queue_type_id}` : ''));
  }

  function deriveLastAction(ticket) {
    return window.Utils.formatDateTime(ticket.called_at || ticket.updated_at || ticket.created_at || ticket.completed_at);
  }

  function deriveWaitingTime(ticket) {
    if (ticket.waitingDisplay) return ticket.waitingDisplay;
    if (!ticket.created_at) return '-';
    const created = new Date(ticket.created_at).getTime();
    if (Number.isNaN(created)) return '-';
    const seconds = Math.max(0, Math.floor((Date.now() - created) / 1000));
    const mins = Math.floor(seconds / 60);
    const hrs = Math.floor(mins / 60);
    if (hrs > 0) return `${hrs}h ${mins % 60}m`;
    if (mins > 0) return `${mins}m ${seconds % 60}s`;
    return `${seconds}s`;
  }

  function getSelectedTicket() {
    return uiState.pool.find((ticket) => ticket.id === uiState.selectedTicketId) || null;
  }

  function updateActionButtonStates() {
    const selected = getSelectedTicket();
    const disabledAll = !selected || !uiState.handlerContext || !uiState.handlerContext.result || !uiState.handlerContext.result.Access;

    Object.values(elements.buttons).forEach((btn) => { btn.disabled = true; });
    if (disabledAll) return;

    const status = selected.status;
    elements.buttons.call.disabled = status !== 'WAITING';
    elements.buttons.start.disabled = !(status === 'CALLED' || status === 'HOLD');
    elements.buttons.hold.disabled = !(status === 'CALLED' || status === 'IN_SERVICE');
    elements.buttons.noShow.disabled = !(status === 'CALLED' || status === 'HOLD');
    elements.buttons.end.disabled = !(status === 'IN_SERVICE' || status === 'CALLED');
    elements.buttons.transfer.disabled = !(status === 'CALLED' || status === 'IN_SERVICE' || status === 'HOLD');
  }

  function renderInformationPanel() {
    const selected = getSelectedTicket();

    elements.info.mnno.textContent = textOrDash(selected && selected.crew_identifier);
    elements.info.rank.textContent = textOrDash(selected && (selected.crew_identifier_type || selected.rank));
    elements.info.name.textContent = textOrDash(selected && selected.crew_name);
    elements.info.status.textContent = textOrDash(selected && selected.status);
    elements.info.vessel.textContent = textOrDash(selected && selected.vessel_name);
    elements.info.crewChangeDate.textContent = textOrDash(selected && selected.crew_change_date);
    elements.info.lastVessel.textContent = textOrDash(selected && selected.last_vessel);
    elements.info.lastCcDate.textContent = textOrDash(selected && selected.last_cc_date);

    const servedBy = (selected && selected.assigned_handler_id)
      ? `${auth.full_name || 'Handler'}${uiState.handlerContext?.window_id ? ` / Window ${uiState.handlerContext.window_id}` : ''}`
      : `${auth.full_name || 'Handler'}${uiState.handlerContext?.window_id ? ` / Window ${uiState.handlerContext.window_id}` : ''}`;
    elements.info.servedBy.textContent = textOrDash(servedBy);

    updateActionButtonStates();
  }

  function renderRows(panelKey, rows, completedColumn) {
    const body = elements.tables[panelKey];
    if (!body) return;

    if (!rows.length) {
      body.innerHTML = `<tr><td colspan="7" class="table-empty">No record(s) to display</td></tr>`;
      return;
    }

    body.innerHTML = rows.map((ticket) => {
      const selectedClass = uiState.selectedTicketId === ticket.id ? 'row-selected' : '';
      return `
        <tr class="terminal-ticket-row ${selectedClass}" data-ticket-id="${ticket.id}">
          <td>${textOrDash(ticket.ticket_number)}</td>
          <td>${textOrDash(ticket.crew_identifier)}</td>
          <td>${textOrDash(ticket.crew_name)}</td>
          <td>${deriveTransaction(ticket)}</td>
          <td>${textOrDash(ticket.status)}</td>
          <td>${deriveLastAction(ticket)}</td>
          <td>${completedColumn ? window.Utils.formatDateTime(ticket.completed_at) : deriveWaitingTime(ticket)}</td>
        </tr>
      `;
    }).join('');

    body.querySelectorAll('tr.terminal-ticket-row').forEach((row) => {
      row.addEventListener('click', () => {
        uiState.selectedTicketId = Number(row.dataset.ticketId);
        renderAllTables();
        renderInformationPanel();
      });
    });
  }

  function applyPanelFilter(panelKey, tickets) {
    const filterState = uiState.filters[panelKey];
    return tickets.filter((ticket) => {
      const statusMatch = !filterState.status || ticket.status === filterState.status;
      if (!statusMatch) return false;
      if (!filterState.search) return true;
      const haystack = [
        ticket.ticket_number,
        ticket.crew_identifier,
        ticket.crew_name,
        ticket.transaction_family,
        ticket.status
      ].join(' ').toLowerCase();
      return haystack.includes(filterState.search.toLowerCase());
    });
  }

  function renderAllTables() {
    const allRows = applyPanelFilter('all', uiState.pool.filter((t) => tableConfigs.all.defaultStatuses.includes(t.status)));
    const holdRows = applyPanelFilter('hold', uiState.pool.filter((t) => tableConfigs.hold.defaultStatuses.includes(t.status)));
    const noShowRows = applyPanelFilter('noshow', uiState.pool.filter((t) => tableConfigs.noshow.defaultStatuses.includes(t.status)));
    const completedRows = applyPanelFilter('completed', uiState.pool.filter((t) => tableConfigs.completed.defaultStatuses.includes(t.status)));

    renderRows('all', allRows, false);
    renderRows('hold', holdRows, false);
    renderRows('noshow', noShowRows, true);
    renderRows('completed', completedRows, true);
  }

  function syncSelectedTicket() {
    if (!uiState.selectedTicketId) return;
    const selected = getSelectedTicket();
    if (!selected) uiState.selectedTicketId = null;
  }

  async function loadContext() {
    const result = await window.Api.apiRequest('/tickets/handler/context');
    if (!result.ok || !result.data || !result.data.result) {
      uiState.handlerContext = null;
      showNotice('Unable to load terminal assignment context.', true);
      return false;
    }

    uiState.handlerContext = result.data;
    if (elements.terminalName) {
      elements.terminalName.textContent = result.data.window_id ? `WINDOW ${result.data.window_id}` : 'UNASSIGNED';
    }
    if (elements.username) elements.username.textContent = auth.full_name || 'Staff User';

    if (!result.data.result.Access || !result.data.window_id) {
      showNotice('No active terminal assignment found. Operational actions are disabled until assignment is available.', true);
      return false;
    }

    showNotice('');
    return true;
  }

  function showNotice(message, isError) {
    if (!elements.notice) return;
    if (!message) {
      elements.notice.classList.add('hidden');
      elements.notice.textContent = '';
      return;
    }
    elements.notice.classList.remove('hidden');
    elements.notice.classList.toggle('error', !!isError);
    elements.notice.textContent = message;
  }

  async function loadTickets() {
    const [queueRes, historyRes] = await Promise.all([
      window.Api.apiRequest('/tickets/handler/queue'),
      window.Api.apiRequest('/tickets/handler/history?limit=200&offset=0')
    ]);

    const waiting = queueRes.ok && queueRes.data && queueRes.data.Data ? queueRes.data.Data : [];
    const history = historyRes.ok && historyRes.data && historyRes.data.Data ? historyRes.data.Data : [];
    const active = uiState.handlerContext && uiState.handlerContext.active_ticket ? [uiState.handlerContext.active_ticket] : [];

    const byId = new Map();
    [...waiting, ...history, ...active].forEach((ticket) => {
      if (ticket && ticket.id) byId.set(ticket.id, ticket);
    });

    uiState.pool = Array.from(byId.values()).sort((a, b) => {
      const aTime = new Date(a.created_at || 0).getTime();
      const bTime = new Date(b.created_at || 0).getTime();
      return bTime - aTime;
    });

    syncSelectedTicket();
    renderAllTables();
    renderInformationPanel();
  }

  async function refreshTerminalData() {
    const hasContext = await loadContext();
    if (!hasContext) {
      uiState.pool = [];
      renderAllTables();
      renderInformationPanel();
      return;
    }
    await loadTickets();
  }

  async function runAction(path, extraPayloadBuilder) {
    const selected = getSelectedTicket();
    if (!selected) {
      alert('Select a ticket first.');
      return;
    }

    if (!uiState.handlerContext || !uiState.handlerContext.handler_id) {
      alert('No active handler terminal assignment.');
      return;
    }

    const payload = {
      handler_id: uiState.handlerContext.handler_id,
      ticket_id: Number(selected.id),
      reason: null
    };

    if (extraPayloadBuilder) {
      const extra = extraPayloadBuilder();
      if (!extra) return;
      Object.assign(payload, extra);
    }

    const result = await window.Api.apiRequest(path, { method: 'POST', body: JSON.stringify(payload) });
    if (!result.ok || (result.data && result.data.result && !result.data.result.Access)) {
      alert((result.data && result.data.result && result.data.result.Status) || 'Action failed');
      return;
    }

    if (result.data && result.data.ticket) {
      uiState.selectedTicketId = result.data.ticket.id;
    }

    await refreshTerminalData();
  }

  function bindButtons() {
    elements.buttons.call.addEventListener('click', () => runAction('/tickets/handler/call'));
    elements.buttons.start.addEventListener('click', () => runAction('/tickets/handler/start-service'));
    elements.buttons.hold.addEventListener('click', () => {
      const reason = prompt('Enter hold reason:', 'HOLD');
      if (reason === null) return;
      runAction('/tickets/handler/hold', () => ({ reason: reason.trim() || 'HOLD' }));
    });
    elements.buttons.noShow.addEventListener('click', () => {
      const reason = prompt('Enter no show reason:', 'NO_SHOW');
      if (reason === null) return;
      runAction('/tickets/handler/no-show', () => ({ reason: reason.trim() || 'NO_SHOW' }));
    });
    elements.buttons.end.addEventListener('click', () => runAction('/tickets/handler/complete'));
    elements.buttons.transfer.addEventListener('click', () => {
      runAction('/tickets/handler/transfer', () => {
        const targetQueueType = prompt('Enter target queue type ID for transfer:');
        if (!targetQueueType) return null;
        const parsedQueueType = Number(targetQueueType);
        if (!Number.isFinite(parsedQueueType) || parsedQueueType <= 0) {
          alert('A valid target queue type ID is required.');
          return null;
        }
        const reason = prompt('Enter transfer reason:', 'TRANSFER');
        if (reason === null) return null;
        return {
          reason: reason.trim() || 'TRANSFER',
          target_queue_type_id: parsedQueueType
        };
      });
    });
  }

  function bindTableControls() {
    document.querySelectorAll('.go-btn').forEach((btn) => {
      btn.addEventListener('click', (event) => {
        const panel = event.currentTarget.dataset.panel;
        const input = document.querySelector(`.panel-search[data-panel="${panel}"]`);
        const select = document.querySelector(`.status-filter[data-panel="${panel}"]`);
        uiState.filters[panel].search = input ? input.value.trim() : '';
        uiState.filters[panel].status = select ? select.value : '';
        renderAllTables();
      });
    });

    document.querySelectorAll('.refresh-btn').forEach((btn) => {
      btn.addEventListener('click', async () => {
        await refreshTerminalData();
      });
    });
  }

  async function bootstrap() {
    startClock();
    bindButtons();
    bindTableControls();
    await refreshTerminalData();

    if (uiState.refreshTimer) clearInterval(uiState.refreshTimer);
    uiState.refreshTimer = setInterval(async () => {
      await refreshTerminalData();
    }, REFRESH_INTERVAL_MS);
  }

  bootstrap();
})();
