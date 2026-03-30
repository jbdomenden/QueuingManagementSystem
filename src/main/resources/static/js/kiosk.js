(function () {
  const companySection = document.getElementById('companySection');
  const transactionSection = document.getElementById('transactionSection');
  const transactionButtons = document.getElementById('transactionButtons');
  const selectedCompanyBadge = document.getElementById('selectedCompanyBadge');
  const footerInstruction = document.getElementById('footerInstruction');

  const queueSection = document.getElementById('queueSection');
  const queueTypeSelect = document.getElementById('queueTypeId');
  const selectedTransactionTitle = document.getElementById('selectedTransactionTitle');
  const createBtn = document.getElementById('createTicketBtn');
  const printable = document.getElementById('printablePanel');
  const printBtn = document.getElementById('printBtn');

  let selectedCompany = null;
  let selectedTransaction = null;
  let queueTypesForCompany = [];

  function updateDateTime() {
    const now = new Date();
    document.getElementById('liveDate').textContent = now.toLocaleDateString();
    document.getElementById('liveTime').textContent = now.toLocaleTimeString();
  }

  function resetToCompanySelection() {
    selectedCompany = null;
    selectedTransaction = null;
    selectedCompanyBadge.innerHTML = '&nbsp;';
    companySection.style.display = 'block';
    transactionSection.style.display = 'none';
    queueSection.style.display = 'none';
    transactionButtons.innerHTML = '';
    footerInstruction.textContent = 'Please select your company';
  }

  function showTransactionSelection() {
    companySection.style.display = 'none';
    transactionSection.style.display = 'block';
    queueSection.style.display = 'none';
    footerInstruction.textContent = 'Please select your transaction';
  }

  function showQueueTypeSelection() {
    queueSection.style.display = 'block';
    footerInstruction.textContent = 'Please select queue type and create ticket';
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
        selectedCompanyBadge.textContent = selectedCompany.name;
        localStorage.setItem('kiosk_selected_company_id', String(selectedCompany.id));
        localStorage.setItem('kiosk_selected_company_name', selectedCompany.name);
        showTransactionSelection();
        await loadTransactionsByCompany(selectedCompany.id);
      };
    });
  }

  function renderTransactions(transactions) {
    if (!transactions.length) {
      transactionButtons.innerHTML = '<div class="card"><strong>No active transactions configured for this company.</strong></div>';
      queueSection.style.display = 'none';
      selectedTransaction = null;
      return;
    }

    transactionButtons.innerHTML = transactions.map(t => `
      <button class="transaction-btn" data-transaction-id="${t.id}" data-transaction-name="${t.transactionName}">
        <span class="transaction-name">${t.transactionName}</span>
        ${t.transactionSubtitle ? `<span class="transaction-subtitle">${t.transactionSubtitle}</span>` : ''}
      </button>
    `).join('');

    document.querySelectorAll('.transaction-btn').forEach(btn => {
      btn.onclick = async () => {
        selectedTransaction = {
          id: Number(btn.dataset.transactionId),
          name: btn.dataset.transactionName
        };
        localStorage.setItem('kiosk_selected_transaction_id', String(selectedTransaction.id));
        localStorage.setItem('kiosk_selected_transaction_name', selectedTransaction.name);
        await loadQueueTypesByCompany(selectedCompany.id, selectedTransaction.name);
        showQueueTypeSelection();
      };
    });
  }

  async function loadCompanies() {
    const result = await window.Api.apiRequest('/companies/kiosk');
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.data) || [];
    renderCompanyTiles(rows);
  }

  async function loadTransactionsByCompany(companyId) {
    const result = await window.Api.apiRequest(`/company-transactions/kiosk/company/${companyId}`);
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.data) || [];
    renderTransactions(rows);
  }

  async function loadQueueTypesByCompany(companyId, transactionName) {
    const result = await window.Api.apiRequest(`/queue-types/company/${companyId}`);
    window.App.setDebug('debugPanel', result);
    queueTypesForCompany = (result.data && result.data.data) || [];

    selectedTransactionTitle.textContent = `Queue Types - ${transactionName}`;
    queueTypeSelect.innerHTML = queueTypesForCompany.map(x => `<option value="${x.id}" data-kiosk-id="${x.kiosk_id || ''}">${x.name} (${x.prefix})</option>`).join('');
  }

  createBtn.onclick = async () => {
    const queueTypeId = Number(queueTypeSelect.value || 0);
    const selectedQueueType = queueTypesForCompany.find(x => x.id === queueTypeId);
    const kioskId = Number((selectedQueueType && selectedQueueType.kiosk_id) || 0);

    if (!selectedCompany || !selectedTransaction || !queueTypeId || !kioskId) return;

    const request = {
      kiosk_id: kioskId,
      queue_type_id: queueTypeId,
      company_id: selectedCompany.id,
      company_transaction_id: selectedTransaction.id
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
        <p><strong>Transaction:</strong> ${printableTicket.companyTransactionName || selectedTransaction.name || '-'}</p>
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

  document.getElementById('backBtn').onclick = () => {
    queueSection.style.display = 'none';
    selectedTransaction = null;
    showTransactionSelection();
  };

  document.getElementById('homeBtn').onclick = () => resetToCompanySelection();

  document.getElementById('refreshBtn').onclick = async () => {
    if (!selectedCompany) {
      await loadCompanies();
      return;
    }
    if (!selectedTransaction) {
      await loadTransactionsByCompany(selectedCompany.id);
      return;
    }
    await loadQueueTypesByCompany(selectedCompany.id, selectedTransaction.name);
  };

  printBtn.onclick = () => window.print();

  updateDateTime();
  setInterval(updateDateTime, 1000);
  resetToCompanySelection();
  loadCompanies();
})();
