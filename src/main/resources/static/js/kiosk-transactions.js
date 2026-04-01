(function () {
  const params = new URLSearchParams(window.location.search);
  const companyId = Number(params.get('companyId') || 0);
  const companyName = decodeURIComponent(params.get('companyName') || '');
  const deviceKey = params.get('device_key') || '';

  const companyHeader = document.getElementById('selectedCompanyHeader');
  const transactionButtons = document.getElementById('transactionButtons');
  companyHeader.textContent = companyName || 'COMPANY';

  function updateDateTime() {
    const now = new Date();
    document.getElementById('liveDate').textContent = now.toLocaleDateString(undefined, {
      month: 'short', day: '2-digit', year: 'numeric'
    });
    document.getElementById('liveTime').textContent = now.toLocaleTimeString();
  }

  function nextUrl(base) {
    return deviceKey ? `${base}${base.includes('?') ? '&' : '?'}device_key=${encodeURIComponent(deviceKey)}` : base;
  }

  async function loadTransactions() {
    if (companyId <= 0) {
      transactionButtons.innerHTML = '<div class="card"><strong>Company is required.</strong></div>';
      return;
    }

    const result = await window.Api.apiRequest(`/company-transactions/kiosk/company/${companyId}`);
    const rows = (result.data && result.data.data) || [];

    if (!rows.length) {
      transactionButtons.innerHTML = '<div class="card"><strong>No transactions available.</strong></div>';
      return;
    }

    transactionButtons.innerHTML = rows.map(t => `
      <button class="transaction-btn transaction-btn-large" data-json='${JSON.stringify(t)}'>
        <span class="transaction-name">${(t.transactionName || '').toUpperCase()}</span>
      </button>
    `).join('');

    document.querySelectorAll('.transaction-btn').forEach(btn => {
      btn.onclick = () => {
        const selectedTransaction = JSON.parse(btn.dataset.json);
        sessionStorage.setItem('kiosk.selectedCompany', JSON.stringify({ id: companyId, name: companyName }));
        sessionStorage.setItem('kiosk.selectedTransaction', JSON.stringify(selectedTransaction));
        location.href = nextUrl('/kiosk.html?resume=transaction');
      };
    });
  }

  document.getElementById('backBtn').onclick = () => {
    location.href = nextUrl('/kiosk.html');
  };
  document.getElementById('homeBtn').onclick = () => {
    location.href = nextUrl('/kiosk.html');
  };
  document.getElementById('refreshBtn').onclick = loadTransactions;

  updateDateTime();
  setInterval(updateDateTime, 1000);
  loadTransactions();
})();
