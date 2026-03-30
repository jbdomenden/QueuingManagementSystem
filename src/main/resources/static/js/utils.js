(function () {
  function toPrettyJson(obj) {
    return JSON.stringify(obj || {}, null, 2);
  }

  function formatDateTime(value) {
    if (!value) return '';
    return String(value).replace('T', ' ').replace('Z', '');
  }

  function deriveDate(value) {
    if (!value) return '';
    return String(value).substring(0, 10);
  }

  function deriveTime(value) {
    if (!value || String(value).length < 19) return '';
    return String(value).substring(11, 19);
  }

  function statusBadge(status) {
    if (!status) return '';
    return `<span class="badge ${status}">${status}</span>`;
  }

  function formatDuration(seconds) {
    if (seconds === null || seconds === undefined || seconds < 0) return '--:--:--';
    const s = Number(seconds);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return [h, m, sec].map(v => String(v).padStart(2, '0')).join(':');
  }

  window.Utils = { toPrettyJson, formatDateTime, deriveDate, deriveTime, statusBadge, formatDuration };
})();
