(function () {
  const MODULES = [
    { key: 'DASHBOARD', label: 'Dashboard', route: '/superadmin-dashboard.html', icon: '🏠', superOnly: true },
    { key: 'USER_MANAGEMENT', label: 'User Management', route: '/users.html', icon: '👤', access: 'USER_MANAGEMENT_VIEW' },
    { key: 'ASSET_MANAGEMENT', label: 'Asset Management', route: '/assets.html', icon: '🖥️', access: 'ASSET_MANAGEMENT_VIEW' },
    { key: 'COMPANIES', label: 'Companies', route: '/companies.html', icon: '🏢', superOnly: true },
    { key: 'COMPANY_TRANSACTIONS', label: 'Company Transactions', route: '/company-transactions.html', icon: '📋', superOnly: true },
    { key: 'TRANSACTION_DESTINATIONS', label: 'Transaction Destinations', route: '/company-transaction-destinations.html', icon: '📍', superOnly: true },
    { key: 'ADMIN', label: 'Admin', route: '/admin.html', icon: '🛠️', allowedRoles: ['SUPER_ADMIN','COMPANY_ADMIN','MANAGER','SUPERVISOR'] },
    { key: 'HANDLER', label: 'Terminal', route: '/handler.html', icon: '🎧', allowedRoles: ['SUPER_ADMIN','COMPANY_ADMIN','MANAGER','SUPERVISOR','ACCOUNTING','EMPLOYEE'] },
    { key: 'ARCHIVED', label: 'Archived', route: '/archived.html', icon: '🗄️', superOnly: true }
  ];

  function initials(name) {
    if (!name) return 'U';
    return name.split(' ').map(x => x[0]).join('').slice(0, 2).toUpperCase();
  }

  function canSeeModule(auth, module) {
    if (module.superOnly && auth.role !== 'SUPER_ADMIN') return false;
    if (auth.role === 'SUPER_ADMIN') return true;
    if (module.allowedRoles && !module.allowedRoles.includes(auth.role)) return false;
    if (module.access) return (auth.permissions || []).includes(module.access);
    return !!module.allowedRoles;
  }

  function removeDebugUi() {
    document.querySelectorAll('#debugPanel').forEach(el => {
      const card = el.closest('.card');
      if (card) card.remove(); else el.remove();
    });
  }

  function renderShell(active) {
    const auth = window.StorageHelper.getAuth();
    const nav = document.getElementById('topNav');
    if (!nav) return;

    const visibleModules = MODULES.filter(m => canSeeModule(auth, m));
    const showHamburger = visibleModules.length > 1;

    const isTerminalWorkspace = active === 'handler.html' || document.body.classList.contains('terminal-workspace');
    nav.innerHTML = `
      <div class="shell-header ${isTerminalWorkspace ? 'terminal-shell-header' : ''}">
        <div class="shell-header-left">
          <button id="shellHamburger" class="icon-btn" aria-label="Toggle menu" ${showHamburger ? '' : 'style="visibility:hidden"'}>☰</button>
          ${isTerminalWorkspace ? '<div class="shell-title terminal-shell-title"></div>' : '<div class="shell-title">Queuing Management System</div>'}
        </div>
        <div class="shell-header-right ${isTerminalWorkspace ? 'terminal-shell-right' : ''}">
          ${isTerminalWorkspace ? `<div class="terminal-header-meta"><div id="terminalLiveDateTime" class="terminal-live-datetime"></div><div class="terminal-identity">Terminal: <span id="terminalNameLabel">UNASSIGNED</span></div><div class="terminal-identity">Username: <span id="terminalUserLabel">${auth.full_name || 'Staff User'}</span></div></div>` : `<div class="shell-avatar">${initials(auth.full_name)}</div><div class="shell-user-name">${auth.full_name || 'Staff User'}</div>`}
          <button id="shellKebab" class="icon-btn" aria-label="Open account menu">⋮</button>
          <div id="shellKebabMenu" class="kebab-menu hidden">
            <button id="changePasswordAction">Change Password</button>
            <button id="logoutAction">Logout</button>
          </div>
        </div>
      </div>
      <div id="shellOverlay" class="shell-overlay hidden"></div>
      <aside id="shellSidebar" class="shell-sidebar hidden">
        <div class="shell-sidebar-title">Modules</div>
        ${visibleModules.map(m => `<a class="shell-link ${active === m.route.replace('/', '') ? 'active' : ''}" href="${m.route}"><span>${m.icon}</span>${m.label}</a>`).join('')}
      </aside>
    `;

    const body = document.body;
    body.classList.add('staff-shell');

    const hamburger = document.getElementById('shellHamburger');
    const sidebar = document.getElementById('shellSidebar');
    const overlay = document.getElementById('shellOverlay');
    const kebab = document.getElementById('shellKebab');
    const kebabMenu = document.getElementById('shellKebabMenu');

    function closeSidebar() {
      sidebar.classList.add('hidden');
      overlay.classList.add('hidden');
    }

    if (hamburger) {
      hamburger.onclick = () => {
        sidebar.classList.toggle('hidden');
        overlay.classList.toggle('hidden');
      };
    }
    if (overlay) overlay.onclick = closeSidebar;

    kebab.onclick = (e) => {
      e.stopPropagation();
      kebabMenu.classList.toggle('hidden');
    };
    document.addEventListener('click', () => kebabMenu.classList.add('hidden'));

    document.getElementById('logoutAction').onclick = () => {
      window.StorageHelper.clearAuth();
      location.href = '/index.html';
    };

    document.getElementById('changePasswordAction').onclick = async () => {
      const currentPassword = prompt('Current password:');
      if (!currentPassword) return;
      const newPassword = prompt('New password:');
      if (!newPassword) return;
      const result = await window.Api.apiRequest('/api/auth/change-password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword }) });
      alert((result.data && result.data.Status) || 'Password update submitted');
      kebabMenu.classList.add('hidden');
    };

    removeDebugUi();
  }

  function renderNav(active) { renderShell(active); }

  function ensureLoggedIn() {
    const auth = window.StorageHelper.getAuth();
    if (!auth.token) {
      location.href = '/index.html';
      return false;
    }
    return true;
  }

  function setDebug() {}

  window.App = { renderNav, ensureLoggedIn, setDebug };
})();
