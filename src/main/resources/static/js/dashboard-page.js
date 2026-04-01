(function(){
  if(!window.App.ensureLoggedIn()) return;
  window.App.renderNav('dashboard.html');
})();
