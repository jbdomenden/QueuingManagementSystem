(function () {
  function resolveDeviceKey(path) {
    const fromQuery = new URLSearchParams(window.location.search).get('device_key');
    const fromStorage = window.localStorage.getItem('device_key');
    const deviceKey = fromQuery || fromStorage || '';
    if (!deviceKey) return '';

    if (path.startsWith('/companies/kiosk') ||
        path.startsWith('/company-transactions/kiosk') ||
        path.startsWith('/company-transaction-destinations/kiosk') ||
        path.startsWith('/queue-types/company/') ||
        path.startsWith('/tickets/create') ||
        path.startsWith('/displays/wallboard/') ||
        path.startsWith('/displays/snapshot/') ||
        path.startsWith('/displays/aggregate/') ||
        path.startsWith('/realtime/ws/display/')) {
      return deviceKey;
    }
    return '';
  }

  async function apiRequest(path, options) {
    const auth = window.StorageHelper.getAuth();
    const headers = Object.assign({}, (options && options.headers) || {});
    if (!headers['Content-Type'] && options && options.body) {
      headers['Content-Type'] = 'application/json';
    }
    if (auth.token) {
      headers['Authorization'] = `Bearer ${auth.token}`;
    }

    const deviceKey = resolveDeviceKey(path);
    if (deviceKey) headers['X-Device-Key'] = deviceKey;

    const response = await fetch(path, Object.assign({}, options || {}, { headers }));
    let data = null;
    const text = await response.text();
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = { raw: text };
    }

    return {
      ok: response.ok,
      status: response.status,
      data,
      endpoint: path,
      requestBody: options && options.body ? JSON.parse(options.body) : null
    };
  }

  window.Api = { apiRequest };
})();
