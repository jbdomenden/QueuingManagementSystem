(function () {
  async function apiRequest(path, options) {
    const auth = window.StorageHelper.getAuth();
    const headers = Object.assign({}, (options && options.headers) || {});
    if (!headers['Content-Type'] && options && options.body) {
      headers['Content-Type'] = 'application/json';
    }
    if (auth.token) {
      headers['Authorization'] = `Bearer ${auth.token}`;
    }

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
