(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('kiosk.html');

  const auth = window.StorageHelper.getAuth();
  const depInput = document.getElementById('departmentId');
  const kioskSelect = document.getElementById('kioskId');
  const queueTypeSelect = document.getElementById('queueTypeId');
  const createBtn = document.getElementById('createTicketBtn');
  const printable = document.getElementById('printablePanel');
  const printBtn = document.getElementById('printBtn');

  depInput.value = auth.department_id || '';

  async function loadQueueTypes() {
    const departmentId = Number(depInput.value || 0);
    if (!departmentId) return;
    const result = await window.Api.apiRequest(`/queue-types/list/${departmentId}`);
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.Data) || [];
    queueTypeSelect.innerHTML = rows.map(x => `<option value="${x.id}">${x.name} (${x.prefix})</option>`).join('');
  }

  async function loadKiosks() {
    const result = await window.Api.apiRequest('/kiosks/list');
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.Data) || [];
    kioskSelect.innerHTML = rows.map(x => `<option value="${x.id}">${x.name} (Dept ${x.department_id})</option>`).join('');
  }

  createBtn.onclick = async () => {
    const request = {
      kiosk_id: Number(kioskSelect.value),
      queue_type_id: Number(queueTypeSelect.value)
    };

    const result = await window.Api.apiRequest('/tickets/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
    window.App.setDebug('debugPanel', result);

    const data = result.data || {};
    const printableTicket = data.printableTicket || {};

    printable.innerHTML = `
      <div class="ticket-highlight">
        <h3>Printable Ticket</h3>
        <p><strong>Department:</strong> ${printableTicket.departmentName || '-'}</p>
        <p><strong>Queue Type:</strong> ${printableTicket.queueTypeName || '-'}</p>
        <p><strong>Ticket Number:</strong> ${printableTicket.ticketNumber || '-'}</p>
        <p><strong>Queue Date:</strong> ${printableTicket.queueDate || '-'}</p>
        <p><strong>Queue Time:</strong> ${printableTicket.queueTime || '-'}</p>
        <p><strong>Queued At:</strong> ${printableTicket.queuedAt || '-'}</p>
        <p>${(printableTicket.formattedPrintText || 'Please wait for your number to be called').replace(/\n/g, '<br>')}</p>
      </div>
    `;
  };

  printBtn.onclick = () => window.print();
  document.getElementById('reloadRefBtn').onclick = async () => { await loadKiosks(); await loadQueueTypes(); };

  loadKiosks();
  loadQueueTypes();
})();
