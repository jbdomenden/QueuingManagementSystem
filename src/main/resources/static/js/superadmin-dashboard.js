(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('superadmin-dashboard.html');

  function iconFor(key) {
    const map = { users:'👤', assets:'🖥️', companies:'🏢', transactions:'📋', destinations:'📍', display:'📺', archived:'🗄️' };
    return map[key] || '📦';
  }

  function card(module) {
    return `
      <button class="dashboard-module-card" data-route="${module.route}" ${module.isEnabled ? '' : 'disabled'}>
        <div class="dashboard-icon-circle">${iconFor(module.iconKey)}</div>
        <div class="dashboard-label">${module.moduleLabel}</div>
      </button>
    `;
  }

  async function load() {
    const me = await window.Api.apiRequest('/api/auth/me');
    const principal = me.data && me.data.principal;
    if (!me.ok || !principal) {
      location.href = '/index.html';
      return;
    }

    const dashboard = await window.Api.apiRequest('/api/dashboard/superadmin');
    if (!dashboard.ok) {
      document.getElementById('dashboardGrid').innerHTML = '<div class="small">Dashboard access denied.</div>';
      return;
    }

    const modules = (dashboard.data && dashboard.data.modules) || [];
    const visible = modules.filter(m => m.isVisible);
    document.getElementById('dashboardGrid').innerHTML = visible.map(card).join('');
    document.querySelectorAll('.dashboard-module-card').forEach(btn => {
      btn.addEventListener('click', () => {
        const route = btn.getAttribute('data-route');
        if (route) location.href = route;
      });
    });
  }

  load();
})();
