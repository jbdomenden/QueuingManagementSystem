(function () {
  if (!window.App.ensureLoggedIn()) return;
  window.App.renderNav('dashboard.html');
  const auth = window.StorageHelper.getAuth();
  document.getElementById('authState').textContent = window.Utils.toPrettyJson(auth);

  document.getElementById('checkMeBtn').onclick = async () => {
    const result = await window.Api.apiRequest('/auth/me');
    window.App.setDebug('debugPanel', result);
  };
})();
