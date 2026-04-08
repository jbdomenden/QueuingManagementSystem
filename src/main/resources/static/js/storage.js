(function () {
  const KEY = 'qms_auth';

  function normalizeToken(rawToken) {
    if (!rawToken) return '';
    const token = String(rawToken).trim();
    if (!token || token === 'undefined' || token === 'null') return '';
    if (token.startsWith('"') && token.endsWith('"')) {
      return token.slice(1, -1).trim();
    }
    return token;
  }

  function saveAuth(auth) {
    localStorage.setItem(KEY, JSON.stringify(auth || {}));
  }

  function getAuth() {
    try {
      return JSON.parse(localStorage.getItem(KEY) || '{}');
    } catch {
      return {};
    }
  }

  function getToken() {
    const auth = getAuth();
    const tokenCandidates = [
      auth.token,
      auth.accessToken,
      auth.access_token,
      auth.jwt,
      localStorage.getItem('token'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('qms_token')
    ];

    for (const candidate of tokenCandidates) {
      const normalized = normalizeToken(candidate);
      if (normalized) return normalized;
    }

    return '';
  }

  function clearAuth() {
    localStorage.removeItem(KEY);
  }

  window.StorageHelper = { saveAuth, getAuth, getToken, clearAuth };
})();
