(function () {
  const KEY = 'qms_auth';

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

  function clearAuth() {
    localStorage.removeItem(KEY);
  }

  window.StorageHelper = { saveAuth, getAuth, clearAuth };
})();
