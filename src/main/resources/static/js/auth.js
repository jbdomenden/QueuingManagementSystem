(function () {
  const form = document.getElementById('loginForm');
  const message = document.getElementById('loginMessage');

  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    message.textContent = '';

    const email = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    const result = await window.Api.apiRequest('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    if (!result.ok || !result.data || !result.data.result || !result.data.result.Access) {
      message.textContent = (result.data && result.data.result && (result.data.result.Status || result.data.result.Message)) || 'Login failed';
      return;
    }

    const principal = result.data.principal || {};

    window.StorageHelper.saveAuth({
      token: result.data.token,
      role: principal.role,
      user_id: principal.userId,
      department_id: principal.departmentId,
      full_name: principal.fullName,
      permissions: principal.permissions || []
    });

    if (principal.role === 'SUPER_ADMIN') {
      location.href = '/superadmin-dashboard.html';
    } else if ((principal.permissions||[]).includes('USER_MANAGEMENT_VIEW')) {
      location.href = '/users.html';
    } else if (principal.role === 'DEPARTMENT_ADMIN') {
      location.href = '/admin.html';
    } else if (principal.role === 'HANDLER') {
      location.href = '/handler.html';
    } else {
      location.href = '/dashboard.html';
    }
  });
})();
