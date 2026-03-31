(function () {
  const params = new URLSearchParams(window.location.search);
  const displayId = Number(params.get('displayId') || '1');

  const filterEl = document.getElementById('wallboardFilter');
  const calledBody = document.getElementById('calledBody');
  const onQueueLeftBody = document.getElementById('onQueueLeftBody');
  const onQueueRightBody = document.getElementById('onQueueRightBody');
  const noShowBody = document.getElementById('noShowBody');
  const onHoldBody = document.getElementById('onHoldBody');
  const visitorSupplierBody = document.getElementById('visitorSupplierBody');

  let selectedFilter = 'all';
  let refreshHandle = null;

  function rowHtmlTwoCols(rows) {
    return (rows || []).map((row) => `
      <tr>
        <td>${row.ticketNumber || ''}</td>
        <td>${row.transactionName || ''}</td>
      </tr>
    `).join('');
  }

  function rowHtmlThreeCols(rows) {
    return (rows || []).map((row) => `
      <tr>
        <td>${row.ticketNumber || ''}</td>
        <td>${row.terminalNumber || '-'}</td>
        <td>${row.transactionName || ''}</td>
      </tr>
    `).join('');
  }

  function splitOnQueue(items) {
    const list = items || [];
    const leftSize = Math.ceil(list.length / 2);
    return {
      left: list.slice(0, leftSize),
      right: list.slice(leftSize)
    };
  }

  function setCounts(counts) {
    document.getElementById('onQueueCount').textContent = `[${counts.onQueue || 0}]`;
    document.getElementById('noShowCount').textContent = `[${counts.noShow || 0}]`;
    document.getElementById('onHoldCount').textContent = `[${counts.onHold || 0}]`;
    document.getElementById('visitorSupplierCount').textContent = `[${counts.visitorSupplier || 0}]`;
  }

  function renderFilter(options, selected) {
    const nextOptions = options && options.length ? options : [{ id: 'all', label: 'Select' }];
    const currentValue = selected || selectedFilter || 'all';
    const previous = filterEl.value;

    filterEl.innerHTML = nextOptions
      .map((opt) => `<option value="${opt.id}">${opt.label}</option>`)
      .join('');

    filterEl.value = nextOptions.some((opt) => opt.id === currentValue)
      ? currentValue
      : (nextOptions.some((opt) => opt.id === previous) ? previous : nextOptions[0].id);

    selectedFilter = filterEl.value;
  }

  function renderPayload(payload) {
    renderFilter(payload.filterOptions, payload.selectedFilter);
    setCounts(payload.counts || {});

    calledBody.innerHTML = rowHtmlThreeCols(payload.called || []);

    const split = splitOnQueue(payload.onQueue || []);
    onQueueLeftBody.innerHTML = rowHtmlTwoCols(split.left);
    onQueueRightBody.innerHTML = rowHtmlTwoCols(split.right);

    noShowBody.innerHTML = rowHtmlThreeCols(payload.noShow || []);
    onHoldBody.innerHTML = rowHtmlTwoCols(payload.onHold || []);
    visitorSupplierBody.innerHTML = rowHtmlTwoCols(payload.visitorSupplier || []);
  }

  async function fetchWallboard() {
    if (!displayId) return;
    const query = selectedFilter ? `?filter=${encodeURIComponent(selectedFilter)}` : '';
    const result = await window.Api.apiRequest(`/displays/wallboard/${displayId}${query}`);
    if (result.ok && result.data && result.data.result && result.data.result.Access) {
      renderPayload(result.data);
    }
  }

  filterEl.addEventListener('change', () => {
    selectedFilter = filterEl.value || 'all';
    fetchWallboard();
  });

  fetchWallboard();
  refreshHandle = setInterval(fetchWallboard, 5000);

  window.addEventListener('beforeunload', () => {
    if (refreshHandle) clearInterval(refreshHandle);
  });
})();
