(function () {
  function renderNav(active) {
    const auth = window.StorageHelper.getAuth();
    const nav = document.getElementById('topNav');
    if (!nav) return;

    const links = [
      ['dashboard.html', 'Dashboard'],
      ['kiosk.html', 'Kiosk'],
      ['handler.html', 'Handler'],
      ['admin.html', 'Admin'],
      ['display.html', 'Display'],
      ['archived.html', 'Archived']
    ];

    nav.innerHTML = `
      <a href="/index.html">Login</a>
      ${links.map(([href, label]) => `<a href="/${href}" ${active === href ? 'style="text-decoration:underline"' : ''}>${label}</a>`).join('')}
      <span class="spacer"></span>
      <span class="small">Role: ${auth.role || 'N/A'} | User: ${auth.user_id || 'N/A'} | Dept: ${auth.department_id || 'N/A'} | Token: ${auth.token ? 'present' : 'none'}</span>
      <button id="logoutBtn" class="secondary" style="width:auto;margin:0">Logout</button>
    `;

    const btn = document.getElementById('logoutBtn');
    if (btn) {
      btn.onclick = () => {
        window.StorageHelper.clearAuth();
        location.href = '/index.html';
      };
    }
  }

  function ensureLoggedIn() {
    const auth = window.StorageHelper.getAuth();
    if (!auth.token) {
      location.href = '/index.html';
      return false;
    }
    return true;
  }

  function setDebug(panelId, apiResult) {
    const el = document.getElementById(panelId);
    if (!el) return;
    el.textContent = window.Utils.toPrettyJson({
      endpoint: apiResult.endpoint,
      status: apiResult.status,
      request: apiResult.requestBody,
      response: apiResult.data
    });
  }

  window.App = { renderNav, ensureLoggedIn, setDebug };
})();
