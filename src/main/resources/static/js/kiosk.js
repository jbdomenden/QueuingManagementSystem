(function () {
  const companySection = document.getElementById('companySection');
  const transactionSection = document.getElementById('transactionSection');
  const crewSection = document.getElementById('crewSection');
  const destinationSection = document.getElementById('destinationSection');
  const queueSection = document.getElementById('queueSection');

  const transactionButtons = document.getElementById('transactionButtons');
  const destinationButtons = document.getElementById('destinationButtons');
  const selectedCompanyBadge = document.getElementById('selectedCompanyBadge');
  const footerInstruction = document.getElementById('footerInstruction');
  const crewStatusMessage = document.getElementById('crewStatusMessage');
  const crewIdentifierDisplay = document.getElementById('crewIdentifierDisplay');
  const rfidCaptureInput = document.getElementById('rfidCaptureInput');
  const crewGreetingLine = document.getElementById('crewGreetingLine');
  const kioskTitle = document.getElementById('kioskTitle');

  const queueTypeSelect = document.getElementById('queueTypeId');
  const selectedTransactionTitle = document.getElementById('selectedTransactionTitle');
  const createBtn = document.getElementById('createTicketBtn');
  const printable = document.getElementById('printablePanel');
  const printBtn = document.getElementById('printBtn');

  let selectedCompany = null;
  let selectedTransaction = null;
  let selectedDestination = null;
  let queueTypesForCompany = [];
  let crewValidation = null;
  let currentStage = 'company';

  const pageParams = new URLSearchParams(window.location.search);
  const activeDeviceKey = pageParams.get('device_key') || '';

  function withDeviceKey(path) {
    if (!activeDeviceKey) return path;
    return `${path}${path.includes('?') ? '&' : '?'}device_key=${encodeURIComponent(activeDeviceKey)}`;
  }

  function updateDateTime() {
    const now = new Date();
    document.getElementById('liveDate').textContent = now.toLocaleDateString(undefined, {
      month: 'short', day: '2-digit', year: 'numeric'
    });
    document.getElementById('liveTime').textContent = now.toLocaleTimeString();
  }

  function setStage(stage) {
    currentStage = stage;
    companySection.style.display = stage === 'company' ? 'block' : 'none';
    transactionSection.style.display = stage === 'transaction' ? 'block' : 'none';
    crewSection.style.display = stage === 'crew' ? 'block' : 'none';
    destinationSection.style.display = stage === 'destination' ? 'block' : 'none';
    queueSection.style.display = stage === 'queue' ? 'block' : 'none';

    if (stage === 'company') footerInstruction.textContent = 'Please select your company';
    if (stage === 'transaction') footerInstruction.textContent = 'Please select your transaction';
    if (stage === 'crew') footerInstruction.textContent = 'Please identify yourself';
    if (stage === 'destination') footerInstruction.textContent = 'Please select your destination';
    if (stage === 'queue') footerInstruction.textContent = 'Please create your ticket';
  }

  function resetToCompanySelection() {
    selectedCompany = null;
    selectedTransaction = null;
    selectedDestination = null;
    crewValidation = null;
    selectedCompanyBadge.textContent = '';
    setStage('company');
  }

  function setupKeypad() {
    const keypadGrid = document.getElementById('keypadGrid');
    keypadGrid.innerHTML = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0].map(n => `<button class="keypad-key" data-key="${n}">${n}</button>`).join('');
    document.querySelectorAll('.keypad-key').forEach(btn => {
      btn.onclick = () => {
        if (selectedTransaction && selectedTransaction.inputMode === 'RFID') return;
        crewIdentifierDisplay.value = `${crewIdentifierDisplay.value}${btn.dataset.key}`;
      };
    });

    document.getElementById('crewBackspaceBtn').onclick = () => {
      crewIdentifierDisplay.value = crewIdentifierDisplay.value.slice(0, -1);
    };
    document.getElementById('crewClearBtn').onclick = () => {
      crewIdentifierDisplay.value = '';
    };
    document.getElementById('crewEnterBtn').onclick = async () => {
      await submitCrewValidation('KEYPAD', crewIdentifierDisplay.value);
    };

    rfidCaptureInput.addEventListener('keydown', async (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const value = rfidCaptureInput.value.trim();
        rfidCaptureInput.value = '';
        if (!value) return;
        crewStatusMessage.textContent = 'RFID detected, validating...';
        await submitCrewValidation('RFID', value);
      }
    });
  }

  async function submitCrewValidation(identifierType, identifierValue) {
    if (!selectedCompany || !selectedTransaction) return;
    const result = await window.Api.apiRequest('/crew-validation/validate', {
      method: 'POST',
      body: JSON.stringify({
        companyId: selectedCompany.id,
        companyTransactionId: selectedTransaction.id,
        identifierValue,
        identifierType
      })
    });

    if (!result.data || !result.data.success) {
      crewStatusMessage.textContent = (result.data && result.data.message) || 'Validation failed';
      return;
    }

    crewValidation = {
      identifier: identifierValue,
      identifierType,
      name: result.data.crewDisplayName
    };
    crewStatusMessage.textContent = `Validation successful: ${crewValidation.name}`;
    await loadDestinations();
  }

  async function loadCompanies() {
    const result = await window.Api.apiRequest(withDeviceKey('/companies/kiosk'));
    const board = (result.data && result.data.data) || { title: 'QUEUING SYSTEM', companies: [] };
    kioskTitle.textContent = board.title || 'QUEUING SYSTEM';
    renderCompanyTiles(board.companies || []);
  }

  function renderCompanyTiles(companies) {
    const grid = document.getElementById('companyGrid');
    const sorted = [...companies].sort((a, b) => {
      if (a.displayOrder !== b.displayOrder) return a.displayOrder - b.displayOrder;
      return (a.companyCode || '').localeCompare(b.companyCode || '');
    });

    grid.innerHTML = sorted.map(company => {
      const cardType = company.cardSizeType === 'BIG' ? 'big' : 'small';
      return `
        <button class="company-card ${cardType}" data-company-id="${company.id}" data-company-name="${company.companyName}">
          <span class="company-card-code">${company.companyCode}</span>
          <span class="company-card-subtitle">(${company.companyDescription || company.companyName})</span>
        </button>
      `;
    }).join('');

    document.querySelectorAll('.company-card').forEach(btn => {
      btn.onclick = async () => {
        selectedCompany = { id: Number(btn.dataset.companyId), name: btn.dataset.companyName };
        sessionStorage.setItem('kiosk.selectedCompany', JSON.stringify(selectedCompany));
        location.href = withDeviceKey(`/kiosk-transactions.html?companyId=${selectedCompany.id}&companyName=${encodeURIComponent(selectedCompany.name)}`);
      };
    });
  }

  async function loadTransactionsByCompany(companyId) {
    const result = await window.Api.apiRequest(`/company-transactions/kiosk/company/${companyId}`);
    const rows = (result.data && result.data.data) || [];
    transactionButtons.innerHTML = rows.map(t => `
      <button class="transaction-btn" data-json='${JSON.stringify(t)}'>
        <span class="transaction-name">${t.transactionName}</span>
        ${t.transactionSubtitle ? `<span class="transaction-subtitle">${t.transactionSubtitle}</span>` : ''}
      </button>
    `).join('');

    document.querySelectorAll('.transaction-btn').forEach(btn => {
      btn.onclick = async () => {
        selectedTransaction = JSON.parse(btn.dataset.json);
        selectedDestination = null;
        crewValidation = null;
        if (selectedTransaction.requiresCrewValidation) {
          setStage('crew');
          crewIdentifierDisplay.value = '';
          crewStatusMessage.textContent = selectedTransaction.inputMode === 'RFID' ? 'Waiting for RFID scan' : 'Enter your identifier';
          if (selectedTransaction.inputMode === 'RFID' || selectedTransaction.inputMode === 'BOTH') rfidCaptureInput.focus();
        } else {
          await loadDestinations();
        }
      };
    });
  }

  async function loadDestinations() {
    const result = await window.Api.apiRequest(`/company-transaction-destinations/kiosk/company-transaction/${selectedTransaction.id}`);
    const rows = (result.data && result.data.data) || [];

    setStage('destination');
    crewGreetingLine.textContent = crewValidation && crewValidation.name ? `Hello, ${crewValidation.name}` : '';

    if (!rows.length) {
      destinationButtons.innerHTML = '<div class="card"><strong>No destination options configured.</strong></div>';
      return;
    }

    destinationButtons.innerHTML = rows.map(d => `
      <button class="transaction-btn" data-json='${JSON.stringify(d)}'>
        <span class="transaction-name">${d.destinationName}</span>
        ${d.destinationSubtitle ? `<span class="transaction-subtitle">${d.destinationSubtitle}</span>` : ''}
      </button>
    `).join('');

    document.querySelectorAll('#destinationButtons .transaction-btn').forEach(btn => {
      btn.onclick = async () => {
        selectedDestination = JSON.parse(btn.dataset.json);
        await loadQueueTypesByCompany(selectedCompany.id, selectedDestination.destinationName);
        setStage('queue');
      };
    });
  }

  async function loadQueueTypesByCompany(companyId, title) {
    const result = await window.Api.apiRequest(`/queue-types/company/${companyId}`);
    queueTypesForCompany = (result.data && result.data.data) || [];
    selectedTransactionTitle.textContent = `Queue Types - ${title}`;
    queueTypeSelect.innerHTML = queueTypesForCompany.map(x => `<option value="${x.id}" data-kiosk-id="${x.kiosk_id || ''}">${x.name} (${x.prefix})</option>`).join('');

    if (selectedDestination && selectedDestination.queueTypeId) {
      queueTypeSelect.value = String(selectedDestination.queueTypeId);
    }
  }

  createBtn.onclick = async () => {
    const queueTypeId = Number(queueTypeSelect.value || 0);
    const selectedQueueType = queueTypesForCompany.find(x => x.id === queueTypeId);
    const kioskId = Number((selectedQueueType && selectedQueueType.kiosk_id) || 0);
    if (!selectedCompany || !selectedTransaction || !selectedDestination || !queueTypeId || !kioskId) return;

    const request = {
      kiosk_id: kioskId,
      queue_type_id: queueTypeId,
      company_id: selectedCompany.id,
      company_transaction_id: selectedTransaction.id,
      destination_id: selectedDestination.id,
      crew_identifier: crewValidation ? crewValidation.identifier : null,
      crew_identifier_type: crewValidation ? crewValidation.identifierType : null,
      crew_name: crewValidation ? crewValidation.name : null
    };

    const result = await window.Api.apiRequest('/tickets/create', { method: 'POST', body: JSON.stringify(request) });
    const printableTicket = (result.data && result.data.printableTicket) || {};
    printable.innerHTML = `
      <div class="ticket-highlight">
        <h3>Printable Ticket</h3>
        <p><strong>Company:</strong> ${printableTicket.companyName || selectedCompany.name || '-'}</p>
        <p><strong>Transaction:</strong> ${printableTicket.companyTransactionName || selectedTransaction.transactionName || '-'}</p>
        <p><strong>Destination:</strong> ${printableTicket.destinationName || selectedDestination.destinationName || '-'}</p>
        <p><strong>Crew:</strong> ${(crewValidation && crewValidation.name) || '-'}</p>
        <p><strong>Ticket Number:</strong> ${printableTicket.ticketNumber || '-'}</p>
        <p>${(printableTicket.formattedPrintText || 'Please wait for your number to be called').replace(/\n/g, '<br>')}</p>
      </div>
    `;
  };

  document.getElementById('backBtn').onclick = () => {
    if (currentStage === 'queue') setStage('destination');
    else if (currentStage === 'destination') {
      if (selectedTransaction && selectedTransaction.requiresCrewValidation) setStage('crew');
      else setStage('transaction');
    }
    else if (currentStage === 'crew') setStage('transaction');
    else if (currentStage === 'transaction') resetToCompanySelection();
  };

  document.getElementById('homeBtn').onclick = () => resetToCompanySelection();
  document.getElementById('refreshBtn').onclick = async () => {
    if (currentStage === 'company') return loadCompanies();
    if (currentStage === 'transaction' && selectedCompany) return loadTransactionsByCompany(selectedCompany.id);
    if (currentStage === 'destination') return loadDestinations();
  };
  printBtn.onclick = () => window.print();


  async function resumeFlowIfRequested() {
    const resume = pageParams.get('resume');
    const storedCompany = sessionStorage.getItem('kiosk.selectedCompany');
    const storedTransaction = sessionStorage.getItem('kiosk.selectedTransaction');
    if (resume !== 'transaction' || !storedCompany || !storedTransaction) {
      resetToCompanySelection();
      await loadCompanies();
      return;
    }

    selectedCompany = JSON.parse(storedCompany);
    selectedCompanyBadge.textContent = selectedCompany.name || '';
    selectedTransaction = JSON.parse(storedTransaction);

    if (selectedTransaction.requiresCrewValidation) {
      setStage('crew');
      crewIdentifierDisplay.value = '';
      crewStatusMessage.textContent = selectedTransaction.inputMode === 'RFID' ? 'Waiting for RFID scan' : 'Enter your identifier';
      if (selectedTransaction.inputMode === 'RFID' || selectedTransaction.inputMode === 'BOTH') rfidCaptureInput.focus();
    } else {
      await loadDestinations();
    }
  }

  setupKeypad();
  updateDateTime();
  setInterval(updateDateTime, 1000);
  resumeFlowIfRequested();
})();
