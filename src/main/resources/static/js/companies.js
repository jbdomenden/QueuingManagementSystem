(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('companies.html');

  let editingCompanyId = null;

  function getPayload() {
    return {
      companyCode: document.getElementById('companyCode').value.trim(),
      companyName: document.getElementById('companyName').value.trim(),
      companyDescription: document.getElementById('companyDescription').value.trim(),
      cardSizeType: document.getElementById('cardSizeType').value,
      displayOrder: Number(document.getElementById('displayOrder').value || 0),
      isActive: document.getElementById('isActive').value === 'true'
    };
  }

  function resetForm() {
    editingCompanyId = null;
    document.getElementById('formTitle').textContent = 'Create Company';
    document.getElementById('companyCode').value = '';
    document.getElementById('companyName').value = '';
    document.getElementById('companyDescription').value = '';
    document.getElementById('cardSizeType').value = 'BIG';
    document.getElementById('displayOrder').value = 0;
    document.getElementById('isActive').value = 'true';
  }

  async function loadCompanies() {
    const result = await window.Api.apiRequest('/companies/list');
    window.App.setDebug('debugPanel', result);
    const rows = (result.data && result.data.data) || [];

    document.getElementById('companiesTableBody').innerHTML = rows.map(x => `
      <tr>
        <td>${x.companyCode}</td>
        <td>${x.companyName}</td>
        <td>${x.companyDescription || ''}</td>
        <td>${x.cardSizeType}</td>
        <td>${x.displayOrder}</td>
        <td>${x.isActive ? 'ACTIVE' : 'INACTIVE'}</td>
        <td>
          <button class="action-btn" data-edit="${x.id}">Edit</button>
          <button class="action-btn danger" data-deactivate="${x.id}">Deactivate</button>
          <button class="action-btn danger" data-delete="${x.id}">Delete</button>
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
        document.getElementById('companyName').value = item.companyName;
        document.getElementById('companyDescription').value = item.companyDescription || '';
        document.getElementById('cardSizeType').value = item.cardSizeType;
        document.getElementById('displayOrder').value = item.displayOrder;
        document.getElementById('isActive').value = String(item.isActive);
      };
    });

    document.querySelectorAll('[data-deactivate]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.deactivate);
        const deactivateResult = await window.Api.apiRequest(`/companies/deactivate/${id}`, { method: 'DELETE' });
        window.App.setDebug('debugPanel', deactivateResult);
        await loadCompanies();
      };
    });

    document.querySelectorAll('[data-delete]').forEach(btn => {
      btn.onclick = async () => {
        const id = Number(btn.dataset.delete);
        const deleteResult = await window.Api.apiRequest(`/companies/${id}`, { method: 'DELETE' });
        window.App.setDebug('debugPanel', deleteResult);
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
