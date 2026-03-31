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

  const onQueueCountEl = document.getElementById('onQueueCount');
  const noShowCountEl = document.getElementById('noShowCount');
  const onHoldCountEl = document.getElementById('onHoldCount');
  const visitorSupplierCountEl = document.getElementById('visitorSupplierCount');

  let selectedFilter = 'all';
  let pollHandle = null;
  let ws = null;
  let fetchInFlight = false;

  const panelState = {
    filterOptions: '',
    selectedFilter: '',
    called: '',
    onQueueLeft: '',
    onQueueRight: '',
    noShow: '',
    onHold: '',
    visitorSupplier: '',
    counts: ''
  };

  function serialize(value) {
    return JSON.stringify(value || []);
  }

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

  function patchCounts(counts) {
    const nextKey = JSON.stringify({
      onQueue: counts.onQueue || 0,
      noShow: counts.noShow || 0,
      onHold: counts.onHold || 0,
      visitorSupplier: counts.visitorSupplier || 0
    });

    if (panelState.counts === nextKey) return;
    panelState.counts = nextKey;
    onQueueCountEl.textContent = `[${counts.onQueue || 0}]`;
    noShowCountEl.textContent = `[${counts.noShow || 0}]`;
    onHoldCountEl.textContent = `[${counts.onHold || 0}]`;
    visitorSupplierCountEl.textContent = `[${counts.visitorSupplier || 0}]`;
  }

  function patchFilter(options, selected) {
    const nextOptions = options && options.length ? options : [{ id: 'all', label: 'Select' }];
    const optionsKey = JSON.stringify(nextOptions);
    const nextSelected = selected || selectedFilter || 'all';

    if (panelState.filterOptions !== optionsKey) {
      const previousValue = filterEl.value;
      filterEl.innerHTML = nextOptions.map((opt) => `<option value="${opt.id}">${opt.label}</option>`).join('');
      panelState.filterOptions = optionsKey;

      if (nextOptions.some((opt) => opt.id === previousValue)) {
        filterEl.value = previousValue;
      }
    }

    if (!nextOptions.some((opt) => opt.id === filterEl.value)) {
      filterEl.value = nextOptions[0].id;
    }

    if (nextOptions.some((opt) => opt.id === nextSelected) && panelState.selectedFilter !== nextSelected) {
      filterEl.value = nextSelected;
    }

    selectedFilter = filterEl.value || 'all';
    panelState.selectedFilter = selectedFilter;
  }

  function patchPanel(key, rows, el, renderer) {
    const nextKey = serialize(rows);
    if (panelState[key] === nextKey) return;
    panelState[key] = nextKey;
    el.innerHTML = renderer(rows);
  }

  function renderPayload(payload) {
    patchFilter(payload.filterOptions, payload.selectedFilter);
    patchCounts(payload.counts || {});

    const split = splitOnQueue(payload.onQueue || []);

    patchPanel('called', payload.called || [], calledBody, rowHtmlThreeCols);
    patchPanel('onQueueLeft', split.left, onQueueLeftBody, rowHtmlTwoCols);
    patchPanel('onQueueRight', split.right, onQueueRightBody, rowHtmlTwoCols);
    patchPanel('noShow', payload.noShow || [], noShowBody, rowHtmlThreeCols);
    patchPanel('onHold', payload.onHold || [], onHoldBody, rowHtmlTwoCols);
    patchPanel('visitorSupplier', payload.visitorSupplier || [], visitorSupplierBody, rowHtmlTwoCols);
  }

  async function fetchWallboard() {
    if (!displayId || fetchInFlight) return;
    fetchInFlight = true;
    try {
      const query = selectedFilter ? `?filter=${encodeURIComponent(selectedFilter)}` : '';
      const result = await window.Api.apiRequest(`/displays/wallboard/${displayId}${query}`);
      if (result.ok && result.data && result.data.result && result.data.result.Access) {
        renderPayload(result.data);
      }
    } finally {
      fetchInFlight = false;
    }
  }

  function connectWs() {
    if (!displayId) return;
    if (ws) ws.close();

    ws = window.WS && window.WS.connectWebSocket
      ? window.WS.connectWebSocket(`/realtime/ws/display/${displayId}`, {
          onOpen: () => {},
          onMessage: () => { fetchWallboard(); },
          onClose: () => {}
        })
      : null;
  }

  filterEl.addEventListener('change', () => {
    selectedFilter = filterEl.value || 'all';
    panelState.selectedFilter = selectedFilter;
    fetchWallboard();
  });

  fetchWallboard();
  connectWs();
  pollHandle = setInterval(fetchWallboard, 5000);

  window.addEventListener('beforeunload', () => {
    if (pollHandle) clearInterval(pollHandle);
    if (ws) ws.close();
  });
})();
