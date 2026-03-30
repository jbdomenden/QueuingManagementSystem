(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('companies.html');

  let editingCompanyId = null;

  function getPayload() {
    return {
      companyCode: document.getElementById('companyCode').value.trim(),
      companyShortName: document.getElementById('companyShortName').value.trim(),
      companyFullName: document.getElementById('companyFullName').value.trim(),
      displaySize: document.getElementById('displaySize').value,
      sortOrder: Number(document.getElementById('sortOrder').value || 0),
      status: document.getElementById('status').value
    };
  }

  function resetForm() {
    editingCompanyId = null;
    document.getElementById('formTitle').textContent = 'Create Company';
    document.getElementById('companyCode').value = '';
    document.getElementById('companyShortName').value = '';
    document.getElementById('companyFullName').value = '';
    document.getElementById('displaySize').value = 'BIG';
    document.getElementById('sortOrder').value = 0;
    document.getElementById('status').value = 'ACTIVE';
  }

  async function loadCompanies() {
    const result = await window.Api.apiRequest('/companies/list');
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.data) || [];

    document.getElementById('companiesTableBody').innerHTML = rows.map(x => `
      <tr>
        <td>${x.companyShortName}</td>
        <td>${x.companyFullName}</td>
        <td>${x.displaySize}</td>
        <td>${x.sortOrder}</td>
        <td>${x.status}</td>
        <td>
          <button class="action-btn" data-edit="${x.id}">Edit</button>
          <button class="action-btn danger" data-deactivate="${x.id}">Deactivate</button>
        </td>
      </tr>
    `).join('');

    document.querySelectorAll('[data-edit]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.edit);
        const response = await window.Api.apiRequest(`/companies/${id}`);
        window.App.setDebug('debugPanel', response);
        const item = response.data && response.data.data;
        if (!item) return;
        editingCompanyId = id;
        document.getElementById('formTitle').textContent = `Edit Company #${id}`;
        document.getElementById('companyCode').value = item.companyCode;
        document.getElementById('companyShortName').value = item.companyShortName;
        document.getElementById('companyFullName').value = item.companyFullName;
        document.getElementById('displaySize').value = item.displaySize;
        document.getElementById('sortOrder').value = item.sortOrder;
        document.getElementById('status').value = item.status;
      };
    });

    document.querySelectorAll('[data-deactivate]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.deactivate);
        const result = await window.Api.apiRequest(`/companies/deactivate/${id}`, { method: 'DELETE' });
        window.App.setDebug('debugPanel', result);
        await loadCompanies();
      };
    });
  }

  document.getElementById('saveBtn').onclick = async () => {
    const payload = getPayload();
    const endpoint = editingCompanyId ? `/companies/update/${editingCompanyId}` : '/companies/create';
    const method = editingCompanyId ? 'PUT' : 'POST';
    const result = await window.Api.apiRequest(endpoint, { method, body: JSON.stringify(payload) });
    window.App.setDebug('debugPanel', result);
    if (result.ok) {
      resetForm();
      await loadCompanies();
    }
  };

  document.getElementById('cancelEditBtn').onclick = resetForm;

  resetForm();
  loadCompanies();
})();
