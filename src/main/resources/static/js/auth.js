(function () {
  const form = document.getElementById('loginForm');
  const message = document.getElementById('loginMessage');
  const debug = document.getElementById('debugPanel');

  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    message.textContent = '';

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    const result = await window.Api.apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    debug.textContent = window.Utils.toPrettyJson({
      endpoint: result.endpoint,
      status: result.status,
      request: result.requestBody,
      response: result.data
    });

    if (!result.ok || !result.data || !result.data.result || !result.data.result.Access) {
      message.textContent = (result.data && result.data.result && result.data.result.Message) || 'Login failed';
      return;
    }

    window.StorageHelper.saveAuth({
      token: result.data.token,
      role: result.data.role,
      user_id: result.data.user_id,
      department_id: result.data.department_id,
      full_name: result.data.full_name
    });

    if (result.data.role === 'DEPARTMENT_ADMIN') {
      location.href = '/admin.html';
    } else if (result.data.role === 'HANDLER') {
      location.href = '/handler.html';
    } else {
      location.href = '/dashboard.html';
    }
  });
})();
