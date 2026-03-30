(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('company-transactions.html');

  let companies = [];
  let editingId = null;

  function getPayload() {
    return {
      companyId: Number(document.getElementById('transactionCompanyId').value || 0),
      transactionCode: document.getElementById('transactionCode').value.trim(),
      transactionName: document.getElementById('transactionName').value.trim(),
      transactionSubtitle: document.getElementById('transactionSubtitle').value.trim() || null,
      sortOrder: Number(document.getElementById('transactionSortOrder').value || 0),
      status: document.getElementById('transactionStatus').value
    };
  }

  function companyNameById(companyId) {
    const item = companies.find(x => x.id === companyId);
    return item ? item.companyShortName : `#${companyId}`;
  }

  function resetForm() {
    editingId = null;
    document.getElementById('transactionFormTitle').textContent = 'Create Company Transaction';
    document.getElementById('transactionCode').value = '';
    document.getElementById('transactionName').value = '';
    document.getElementById('transactionSubtitle').value = '';
    document.getElementById('transactionSortOrder').value = 0;
    document.getElementById('transactionStatus').value = 'ACTIVE';
  }

  async function loadCompanies() {
    const result = await window.Api.apiRequest('/companies/list');
    window.App.setDebug('debugPanel', result);
    companies = (result.data && result.data.data) || [];
    const options = companies.map(c => `<option value="${c.id}">${c.companyShortName} (${c.companyCode})</option>`).join('');
    document.getElementById('transactionCompanyId').innerHTML = options;
    document.getElementById('filterCompanyId').innerHTML = options;
  }

  async function loadTransactionsByCompany(companyId) {
    const result = await window.Api.apiRequest(`/company-transactions/company/${companyId}`);
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.data) || [];

    document.getElementById('companyTransactionsTableBody').innerHTML = rows.map(x => `
      <tr>
        <td>${companyNameById(x.companyId)}</td>
        <td>${x.transactionCode}</td>
        <td>${x.transactionName}</td>
        <td>${x.transactionSubtitle || ''}</td>
        <td>${x.sortOrder}</td>
        <td>${x.status}</td>
        <td>
          <button class="action-btn" data-edit="${x.id}">Edit</button>
          <button class="action-btn secondary" data-toggle="${x.id}" data-status="${x.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}">${x.status === 'ACTIVE' ? 'Set Inactive' : 'Set Active'}</button>
          <button class="action-btn danger" data-deactivate="${x.id}">Deactivate</button>
        </td>
      </tr>
    `).join('');

    document.querySelectorAll('[data-edit]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.edit);
        const detail = await window.Api.apiRequest(`/company-transactions/${id}`);
        window.App.setDebug('debugPanel', detail);
        const item = detail.data && detail.data.data;
        if (!item) return;
        editingId = id;
        document.getElementById('transactionFormTitle').textContent = `Edit Company Transaction #${id}`;
        document.getElementById('transactionCompanyId').value = String(item.companyId);
        document.getElementById('transactionCode').value = item.transactionCode;
        document.getElementById('transactionName').value = item.transactionName;
        document.getElementById('transactionSubtitle').value = item.transactionSubtitle || '';
        document.getElementById('transactionSortOrder').value = item.sortOrder;
        document.getElementById('transactionStatus').value = item.status;
      };
    });

    document.querySelectorAll('[data-toggle]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.toggle);
        const status = btn.dataset.status;
        const toggled = await window.Api.apiRequest(`/company-transactions/toggle/${id}`, {
          method: 'PATCH',
          body: JSON.stringify({ status })
        });
        window.App.setDebug('debugPanel', toggled);
        await loadTransactionsByCompany(companyId);
      };
    });

    document.querySelectorAll('[data-deactivate]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.deactivate);
        const deactivated = await window.Api.apiRequest(`/company-transactions/deactivate/${id}`, { method: 'DELETE' });
        window.App.setDebug('debugPanel', deactivated);
        await loadTransactionsByCompany(companyId);
      };
    });
  }

  document.getElementById('saveTransactionBtn').onclick = async () => {
    const payload = getPayload();
    const endpoint = editingId ? `/company-transactions/update/${editingId}` : '/company-transactions/create';
    const method = editingId ? 'PUT' : 'POST';
    const result = await window.Api.apiRequest(endpoint, { method, body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    if (result.ok) {
      resetForm();
      await loadTransactionsByCompany(Number(document.getElementById('filterCompanyId').value || 0));
    }
  };

  document.getElementById('cancelTransactionEditBtn').onclick = resetForm;
  document.getElementById('filterCompanyId').onchange = async (e) => {
    await loadTransactionsByCompany(Number(e.target.value || 0));
  };

  (async function init() {
    await loadCompanies();
    resetForm();
    const filterCompanyId = Number(document.getElementById('filterCompanyId').value || 0);
    if (filterCompanyId > 0) await loadTransactionsByCompany(filterCompanyId);
  })();
})();
