(function () {
  const queueTypeSelect = document.getElementById('queueTypeId');
  const createBtn = document.getElementById('createTicketBtn');
  const printable = document.getElementById('printablePanel');
  const printBtn = document.getElementById('printBtn');
  const queueSection = document.getElementById('queueSection');
  const selectedCompanyTitle = document.getElementById('selectedCompanyTitle');

  let selectedCompany = null;
  let queueTypesForCompany = [];

  function updateDateTime() {
    const now = new Date();
    document.getElementById('liveDate').textContent = now.toLocaleDateString();
    document.getElementById('liveTime').textContent = now.toLocaleTimeString();
  }

  function renderCompanyTiles(companies) {
    const bigGrid = document.getElementById('companyBigGrid');
    const smallGrid = document.getElementById('companySmallGrid');

    const bigCompanies = companies.filter(x => x.displaySize === 'BIG');
    const smallCompanies = companies.filter(x => x.displaySize === 'SMALL');

    const toCard = (company, sizeClass) => `
      <button class="company-card ${sizeClass}" data-company-id="${company.id}" data-company-name="${company.companyShortName}">
        <span class="short-name">${company.companyShortName}</span>
        <span class="full-name">${company.companyFullName}</span>
      </button>
    `;

    bigGrid.innerHTML = bigCompanies.map(x => toCard(x, 'big')).join('');
    smallGrid.innerHTML = smallCompanies.map(x => toCard(x, 'small')).join('');

    document.querySelectorAll('.company-card').forEach(btn => {
      btn.onclick = async () => {
        selectedCompany = {
          id: Number(btn.dataset.companyId),
          name: btn.dataset.companyName
        };
        localStorage.setItem('kiosk_selected_company_id', String(selectedCompany.id));
        localStorage.setItem('kiosk_selected_company_name', selectedCompany.name);
        await loadQueueTypesByCompany(selectedCompany.id, selectedCompany.name);
      };
    });
  }

  async function loadCompanies() {
    const result = await window.Api.apiRequest('/companies/kiosk');
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.data) || [];
    renderCompanyTiles(rows);
  }

  async function loadQueueTypesByCompany(companyId, companyName) {
    const result = await window.Api.apiRequest(`/queue-types/company/${companyId}`);
    window.App.setDebug('debugPanel', result);
    queueTypesForCompany = (result.data && result.data.data) || [];

    selectedCompanyTitle.textContent = `Queue Types - ${companyName}`;
    queueTypeSelect.innerHTML = queueTypesForCompany.map(x => `<option value="${x.id}" data-kiosk-id="${x.kiosk_id || ''}">${x.name} (${x.prefix})</option>`).join('');
    queueSection.style.display = 'block';
  }

  createBtn.onclick = async () => {
    const queueTypeId = Number(queueTypeSelect.value || 0);
    const selectedQueueType = queueTypesForCompany.find(x => x.id === queueTypeId);
    const kioskId = Number((selectedQueueType && selectedQueueType.kiosk_id) || 0);

    if (!selectedCompany || !queueTypeId || !kioskId) {
      return;
    }

    const request = {
      kiosk_id: kioskId,
      queue_type_id: queueTypeId
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
        <p><strong>Company:</strong> ${printableTicket.companyName || selectedCompany.name || '-'}</p>
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
  document.getElementById('refreshBtn').onclick = async () => {
    queueSection.style.display = 'none';
    queueTypeSelect.innerHTML = '';
    await loadCompanies();
  };

  updateDateTime();
  setInterval(updateDateTime, 1000);
  loadCompanies();
})();
