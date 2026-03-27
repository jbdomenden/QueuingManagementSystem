(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('admin.html');

  const auth = window.StorageHelper.getAuth();
  const departmentIdInput = document.getElementById('departmentId');
  const dateFromInput = document.getElementById('dateFrom');
  const dateToInput = document.getElementById('dateTo');

  if (auth.role === 'DEPARTMENT_ADMIN') {
    departmentIdInput.value = auth.department_id || '';
    departmentIdInput.readOnly = true;
  }

  function today() {
    return new Date().toISOString().substring(0, 10);
  }
  dateFromInput.value = dateFromInput.value || today();
  dateToInput.value = dateToInput.value || today();

  async function loadLive() {
    const dep = Number(departmentIdInput.value || 0);
    if (!dep) return;
    const result = await window.Api.apiRequest(`/tickets/live/${dep}`);
    window.App.setDebug('debugPanel', result);

    const rows = (result.data && result.data.Data) || [];
    const count = (s) => rows.filter(x => x.status === s).length;
    document.getElementById('summaryCards').innerHTML = `
      <div class="inline">
        <div class="card"><div class="small">WAITING</div><div class="kpi">${count('WAITING')}</div></div>
        <div class="card"><div class="small">CALLED</div><div class="kpi">${count('CALLED')}</div></div>
        <div class="card"><div class="small">IN_SERVICE</div><div class="kpi">${count('IN_SERVICE')}</div></div>
        <div class="card"><div class="small">SKIPPED</div><div class="kpi">${count('SKIPPED')}</div></div>
        <div class="card"><div class="small">COMPLETED</div><div class="kpi">${count('COMPLETED')}</div></div>
      </div>
    `;

    document.getElementById('liveTableBody').innerHTML = rows.map(t => `
      <tr>
        <td>${t.ticket_number}</td>
        <td>${t.queue_type_id}</td>
        <td>${window.Utils.statusBadge(t.status)}</td>
        <td>${window.Utils.deriveDate(t.created_at)}</td>
        <td>${window.Utils.deriveTime(t.created_at)}</td>
        <td>${window.Utils.formatDateTime(t.created_at)}</td>
        <td>${window.Utils.formatDateTime(t.called_at || '')}</td>
        <td>${window.Utils.formatDateTime(t.completed_at || '')}</td>
        <td>${t.assigned_window_id || ''}</td>
      </tr>
    `).join('');
  }

  async function loadArchived() {
    const dep = Number(departmentIdInput.value || 0);
    if (!dep) return;
    const params = new URLSearchParams({ dateFrom: dateFromInput.value, dateTo: dateToInput.value, departmentId: String(dep) });
    const qt = document.getElementById('queueTypeFilter').value;
    const status = document.getElementById('statusFilter').value;
    if (qt) params.set('queueTypeId', qt);
    if (status) params.set('status', status);

    const result = await window.Api.apiRequest(`/tickets/archived?${params.toString()}`);
    window.App.setDebug('debugPanel', result);

    const rows = (result.data && result.data.Data) || [];
    document.getElementById('archivedTableBody').innerHTML = rows.map(t => `
      <tr>
        <td>${t.ticket_number}</td>
        <td>${t.queue_type_id}</td>
        <td>${window.Utils.statusBadge(t.status)}</td>
        <td>${t.service_date || ''}</td>
        <td>${window.Utils.deriveTime(t.queuedAt)}</td>
        <td>${window.Utils.formatDateTime(t.queuedAt)}</td>
        <td>${t.waitingDisplay || window.Utils.formatDuration(t.waitingSeconds)}</td>
        <td>${t.servedDisplay || window.Utils.formatDuration(t.servedSeconds)}</td>
      </tr>
    `).join('');
  }

  document.getElementById('refreshBtn').onclick = async () => { await loadLive(); await loadArchived(); };
  document.getElementById('archiveBtn').onclick = async () => {
    const dep = Number(departmentIdInput.value || 0);
    const serviceDate = document.getElementById('archiveDate').value;
    const payload = { serviceDate, departmentId: dep || null };
    const result = await window.Api.apiRequest('/tickets/archive/day', { method: 'POST', body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    await loadArchived();
  };

  loadLive();
  loadArchived();
  setInterval(loadLive, 5000);
})();
