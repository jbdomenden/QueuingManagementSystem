(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('archived.html');

  const auth = window.StorageHelper.getAuth();
  const departmentInput = document.getElementById('departmentId');
  if (auth.role === 'DEPARTMENT_ADMIN') {
    departmentInput.value = auth.department_id || '';
    departmentInput.readOnly = true;
  }

  function today() {
    return new Date().toISOString().substring(0, 10);
  }
  document.getElementById('dateFrom').value = today();
  document.getElementById('dateTo').value = today();

  document.getElementById('searchBtn').onclick = async () => {
    const params = new URLSearchParams({
      dateFrom: document.getElementById('dateFrom').value,
      dateTo: document.getElementById('dateTo').value
    });

    const dep = departmentInput.value;
    const queueTypeId = document.getElementById('queueTypeId').value;
    const status = document.getElementById('status').value;
    if (dep) params.set('departmentId', dep);
    if (queueTypeId) params.set('queueTypeId', queueTypeId);
    if (status) params.set('status', status);

    const result = await window.Api.apiRequest(`/tickets/archived?${params.toString()}`);
    window.App.setDebug('debugPanel', result);

    const rows = (result.data && result.data.Data) || [];
    document.getElementById('archivedBody').innerHTML = rows.map(t => `
      <tr>
        <td>${t.id}</td>
        <td>${t.ticket_number}</td>
        <td>${t.department_id}</td>
        <td>${t.queue_type_id}</td>
        <td>${window.Utils.statusBadge(t.status)}</td>
        <td>${t.service_date || ''}</td>
        <td>${window.Utils.deriveTime(t.queuedAt)}</td>
        <td>${window.Utils.formatDateTime(t.queuedAt)}</td>
        <td>${t.waitingDisplay || window.Utils.formatDuration(t.waitingSeconds)}</td>
        <td>${t.servedDisplay || window.Utils.formatDuration(t.servedSeconds)}</td>
      </tr>
    `).join('');
  };
})();
