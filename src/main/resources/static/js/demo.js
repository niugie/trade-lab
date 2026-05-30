const API = '/api/v1';

let currentOrder = null;
let demoState = null;
let expireTimer = null;
let userPinnedOrder = false;

const ERROR_HINTS = {
  '40000': '请求参数有误，请检查表单',
  '40400': '资源不存在，请刷新或重置数据后重试',
  '40901': '库存不足，请减少数量或重置数据',
  '40902': '优惠券不可用，请重置数据',
  '40903': '订单状态不允许此操作（可能已支付/已关闭/已过期）',
  '40904': '重复请求（幂等键或支付单号冲突）',
  '50000': '服务器内部错误，请查看后端日志',
};

const els = {
  flowSteps: document.querySelectorAll('.flow-step'),
  orderPanel: document.getElementById('order-panel'),
  couponPanel: document.getElementById('coupon-panel'),
  couponBadge: document.getElementById('coupon-badge'),
  inventoryPanel: document.getElementById('inventory-panel'),
  pendingPanel: document.getElementById('pending-panel'),
  pendingBadge: document.getElementById('pending-badge'),
  pendingSection: document.getElementById('pending-section'),
  historyPanel: document.getElementById('history-panel'),
  logPanel: document.getElementById('log-panel'),
  btnCreate: document.getElementById('btn-create'),
  btnNew: document.getElementById('btn-new'),
  btnPay: document.getElementById('btn-pay'),
  btnClose: document.getElementById('btn-close'),
  btnRefresh: document.getElementById('btn-refresh'),
  btnReset: document.getElementById('btn-reset'),
};

function formatMoney(cents) {
  if (cents == null) return '—';
  return '¥' + (Number(cents) / 100).toFixed(2);
}

function nowTime() {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false });
}

function log(message, ok = true) {
  const entry = document.createElement('div');
  entry.className = 'log-entry ' + (ok ? 'ok' : 'err');
  entry.innerHTML = `<span class="time">${nowTime()}</span>${escapeHtml(message)}`;
  els.logPanel.prepend(entry);
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function formatError(err) {
  const msg = err.message || String(err);
  const match = msg.match(/\[(\d+)\]\s*(.*)/);
  if (!match) return msg;
  const code = match[1];
  const hint = ERROR_HINTS[code];
  return hint ? `[${code}] ${hint}` : msg;
}

function getParams() {
  return {
    userId: Number(document.getElementById('userId').value) || 10001,
    skuId: Number(document.getElementById('skuId').value) || 1,
    couponId: Number(document.getElementById('userCouponId').value) || 1,
  };
}

function getFormValues() {
  return {
    userId: getParams().userId,
    skuId: getParams().skuId,
    quantity: Number(document.getElementById('quantity').value) || 1,
    userCouponId: document.getElementById('useCoupon').checked ? getParams().couponId : null,
    idempotencyKey: document.getElementById('idempotencyKey').value.trim(),
  };
}

function newIdempotencyKey() {
  const key = 'demo-' + Date.now();
  document.getElementById('idempotencyKey').value = key;
  return key;
}

const FORM_INPUT_IDS = ['userId', 'skuId', 'quantity', 'userCouponId', 'idempotencyKey', 'orderIdDisplay'];

function setFormMode(mode) {
  const isView = mode === 'view';
  const badge = document.getElementById('form-mode-badge');
  const hint = document.getElementById('form-hint');
  const orderRow = document.getElementById('row-order-id');

  badge.textContent = isView ? '当前选中订单' : '新建订单';
  badge.className = 'badge ' + (isView ? 'badge-view' : '');
  orderRow.hidden = !isView;
  els.btnNew.hidden = !isView;
  els.btnCreate.hidden = isView;

  FORM_INPUT_IDS.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.readOnly = isView;
  });
  document.getElementById('useCoupon').disabled = isView;

  hint.textContent = isView
    ? '以下为该订单创建时的参数（只读）· 可「模拟支付」或「关单释放」· 点「新建下一单」创建新订单'
    : '填写参数后创建新订单 · SKU 1 = ¥99 · 券 1 = 满 ¥50 减 ¥10';
}

function syncFormFromOrder(order) {
  if (!order) {
    setFormMode('create');
    document.getElementById('orderIdDisplay').value = '';
    return;
  }

  document.getElementById('userId').value = order.userId;
  document.getElementById('skuId').value = order.skuId;
  document.getElementById('quantity').value = order.quantity;

  const hasCoupon = order.userCouponId != null && order.userCouponId !== '';
  document.getElementById('useCoupon').checked = hasCoupon;
  document.getElementById('userCouponId').value = hasCoupon ? order.userCouponId : 1;

  document.getElementById('idempotencyKey').value = order.idempotencyKey || '—';
  document.getElementById('orderIdDisplay').value = order.orderId;
  setFormMode('view');
}

function prepareNewOrderForm() {
  userPinnedOrder = false;
  currentOrder = null;
  setFormMode('create');
  document.getElementById('useCoupon').disabled = false;
  document.getElementById('userCouponId').disabled = !document.getElementById('useCoupon').checked;
  newIdempotencyKey();
  renderOrder(null);
  if (demoState) {
    renderPendingOrders(demoState.pendingOrders);
    renderHistory(demoState.recentOrders);
  }
  log('已切换为新建订单模式 · 可修改参数后创建');
}

async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  const json = await res.json();
  if (json.code !== 0) {
    throw new Error(`[${json.code}] ${json.message}`);
  }
  return json.data;
}

function normalizeOrder(order) {
  if (order && order.orderId != null) {
    order.orderId = String(order.orderId);
  }
  return order;
}

function statusLabel(status) {
  return { PENDING_PAY: '待支付', PAID: '已支付', CLOSED: '已关闭' }[status] || status;
}

function couponStatusLabel(status) {
  return { AVAILABLE: '可用', FROZEN: '已冻结', USED: '已使用' }[status] || status;
}

function formatTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('zh-CN');
}

function formatExpireCountdown(expireAt) {
  if (!expireAt) return '';
  const diff = new Date(expireAt).getTime() - Date.now();
  if (diff <= 0) return ' · <span class="text-danger">已过期</span>';
  const min = Math.floor(diff / 60000);
  const sec = Math.floor((diff % 60000) / 1000);
  return ` · 剩余 ${min}分${sec}秒`;
}

function updateFlow(status) {
  const map = { idle: 0, PENDING_PAY: 1, PAID: 3, CLOSED: 3 };
  const active = map[status ?? 'idle'] ?? 0;
  els.flowSteps.forEach((step, i) => {
    step.classList.remove('active', 'done');
    if (i < active) step.classList.add('done');
    if (i === active) step.classList.add('active');
  });
}

function setButtonsLoading(loading) {
  els.btnCreate.disabled = loading;
  els.btnRefresh.disabled = loading;
  els.btnReset.disabled = loading;
  if (!currentOrder || currentOrder.status !== 'PENDING_PAY') {
    els.btnPay.disabled = true;
    els.btnClose.disabled = true;
  } else if (!loading) {
    els.btnPay.disabled = false;
    els.btnClose.disabled = false;
  }
}

function renderOrder(order) {
  if (expireTimer) {
    clearInterval(expireTimer);
    expireTimer = null;
  }

  if (!order) {
    els.orderPanel.innerHTML =
      '<div class="empty-state">尚未创建订单<br>点击「创建订单」或从下方选择一笔待支付订单</div>';
    els.btnPay.disabled = true;
    els.btnClose.disabled = true;
    updateFlow('idle');
    return;
  }

  const statusClass = 'status-' + order.status;
  const expireHtml = order.status === 'PENDING_PAY' && order.expireAt
    ? `<div class="kv"><span>支付截止</span><span id="expire-countdown">${formatTime(order.expireAt)}</span></div>`
    : '';

  els.orderPanel.innerHTML = `
    <div class="kv-list">
      <div class="kv"><span>订单号</span><strong class="mono">${order.orderId}</strong></div>
      <div class="kv"><span>状态</span><span class="status-pill ${statusClass}">${statusLabel(order.status)}</span></div>
      <div class="kv"><span>商品</span><span>SKU ${order.skuId} × ${order.quantity}</span></div>
      <div class="kv"><span>原价</span><span>${formatMoney(order.totalAmount)}</span></div>
      <div class="kv"><span>优惠</span><span class="money discount">-${formatMoney(order.discountAmount)}</span></div>
      <div class="kv"><span>应付</span><span class="money">${formatMoney(order.payAmount)}</span></div>
      ${order.userCouponId ? `<div class="kv"><span>优惠券</span><span>#${order.userCouponId}</span></div>` : ''}
      ${expireHtml}
      ${order.paidAt ? `<div class="kv"><span>支付时间</span><span>${formatTime(order.paidAt)}</span></div>` : ''}
    </div>`;

  els.btnPay.disabled = order.status !== 'PENDING_PAY';
  els.btnClose.disabled = order.status !== 'PENDING_PAY';
  updateFlow(order.status);

  if (order.status === 'PENDING_PAY' && order.expireAt) {
    expireTimer = setInterval(() => {
      const el = document.getElementById('expire-countdown');
      if (!el) {
        clearInterval(expireTimer);
        return;
      }
      el.innerHTML = formatTime(order.expireAt) + formatExpireCountdown(order.expireAt);
    }, 1000);
  }
}

function renderCoupon(coupon) {
  if (!coupon) {
    els.couponPanel.innerHTML = '<div class="empty-state">暂无优惠券数据</div>';
    els.couponBadge.textContent = '—';
    return;
  }
  const cls = 'status-coupon-' + coupon.status;
  els.couponBadge.textContent = couponStatusLabel(coupon.status);
  els.couponBadge.className = 'badge ' + cls;

  els.couponPanel.innerHTML = `
    <div class="kv-list">
      <div class="kv"><span>券 ID</span><span>#${coupon.couponId}</span></div>
      <div class="kv"><span>名称</span><span>${escapeHtml(coupon.templateName || '—')}</span></div>
      <div class="kv"><span>状态</span><span class="status-pill ${cls}">${couponStatusLabel(coupon.status)}</span></div>
      ${coupon.frozenOrderId ? `<div class="kv"><span>冻结订单</span><span class="mono">${coupon.frozenOrderId}</span></div>` : ''}
      ${coupon.usedAt ? `<div class="kv"><span>使用时间</span><span>${formatTime(coupon.usedAt)}</span></div>` : ''}
    </div>
    <p class="hint">下单冻结 · 支付核销 · 关单/重置释放</p>`;
}

function renderInventory(inv) {
  if (!inv) {
    els.inventoryPanel.innerHTML = '<div class="empty-state">加载中…</div>';
    return;
  }
  const total = Math.max(inv.available + inv.reserved, 1);
  const availPct = ((inv.available / total) * 100).toFixed(1);
  const resPct = ((inv.reserved / total) * 100).toFixed(1);

  els.inventoryPanel.innerHTML = `
    <div class="kv-list">
      <div class="kv"><span>可售</span><strong class="text-success">${inv.available}</strong></div>
      <div class="kv"><span>预占</span><strong class="text-warning">${inv.reserved}</strong></div>
      <div class="kv"><span>版本</span><span>${inv.version}</span></div>
    </div>
    <div class="inventory-bars">
      <div class="bar-item">
        <label><span>可售</span><span>${inv.available}</span></label>
        <div class="bar-track"><div class="bar-fill available" style="width:${availPct}%"></div></div>
      </div>
      <div class="bar-item">
        <label><span>预占</span><span>${inv.reserved}</span></label>
        <div class="bar-track"><div class="bar-fill reserved" style="width:${resPct}%"></div></div>
      </div>
    </div>`;
}

function renderPendingOrders(pendingOrders) {
  if (!pendingOrders || pendingOrders.length === 0) {
    els.pendingPanel.innerHTML = '<div class="empty-state">暂无待支付订单</div>';
    els.pendingSection.style.display = '';
    return;
  }

  els.pendingSection.style.display = '';
  const cards = pendingOrders.map((o) => {
    const id = String(o.orderId);
    const isActive = currentOrder && String(currentOrder.orderId) === id;
    return `<div class="pending-card${isActive ? ' pending-card-active' : ''}" data-order-id="${id}">
      <div class="pending-card-main">
        <div class="mono pending-id" title="${id}">…${id.slice(-10)}</div>
        <div class="pending-meta">${formatMoney(o.payAmount)} · SKU ${o.skuId}×${o.quantity}</div>
      </div>
      ${isActive
        ? '<span class="pending-tag active-tag">当前</span>'
        : `<button type="button" class="btn btn-sm btn-primary btn-switch" data-order-id="${id}">切换</button>`}
    </div>`;
  }).join('');

  els.pendingPanel.innerHTML = `<div class="pending-list">${cards}</div>`;

  els.pendingPanel.querySelectorAll('.btn-switch').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      switchToOrder(btn.getAttribute('data-order-id'));
    });
  });
  els.pendingPanel.querySelectorAll('.pending-card').forEach((card) => {
    card.addEventListener('click', () => switchToOrder(card.getAttribute('data-order-id')));
  });
}

function renderHistory(orders) {
  if (!orders || orders.length === 0) {
    els.historyPanel.innerHTML = '<div class="empty-state">暂无订单记录</div>';
    return;
  }

  const rows = orders.map((o) => {
    const id = String(o.orderId);
    const active = currentOrder && String(currentOrder.orderId) === id ? ' history-row-active' : '';
    const pendingHint = o.status === 'PENDING_PAY' && !active ? ' · 可切换' : '';
    return `<tr class="history-row${active}" data-order-id="${id}" title="${id}">
      <td class="mono">…${id.slice(-10)}</td>
      <td><span class="status-pill status-${o.status}">${statusLabel(o.status)}</span>${pendingHint}</td>
      <td>${formatMoney(o.payAmount)}</td>
      <td>${formatTime(o.createdAt || o.paidAt)}</td>
    </tr>`;
  }).join('');

  els.historyPanel.innerHTML = `
    <table class="history-table">
      <thead><tr><th>订单号</th><th>状态</th><th>应付</th><th>时间</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <p class="hint">待支付订单点击行也可切换；切换后上方「当前订单」会更新，即可支付或关单</p>`;

  els.historyPanel.querySelectorAll('.history-row').forEach((row) => {
    row.addEventListener('click', () => switchToOrder(row.getAttribute('data-order-id')));
  });
}

async function switchToOrder(orderId) {
  if (!orderId) return;
  try {
    const order = normalizeOrder(await api('GET', `/orders/${orderId}`));
    userPinnedOrder = true;
    currentOrder = order;
    syncFormFromOrder(currentOrder);
    renderOrder(currentOrder);
    await refreshDemoState();
    const hint = order.status === 'PENDING_PAY' ? ' · 现在可以「模拟支付」或「关单释放」' : '';
    log(`已切换到订单 …${String(orderId).slice(-10)}（${statusLabel(order.status)}）${hint}`);
  } catch (e) {
    log(formatError(e), false);
  }
}

function applyDemoState(state) {
  demoState = state;
  renderInventory(state.inventory);
  renderCoupon(state.coupon);
  renderPendingOrders(state.pendingOrders);
  renderHistory(state.recentOrders);
  els.pendingBadge.textContent = state.pendingOrderCount > 0
    ? `共 ${state.pendingOrderCount} 笔`
    : '无';

  if (currentOrder) {
    const fresh = state.recentOrders.find((o) => String(o.orderId) === String(currentOrder.orderId))
      || state.pendingOrders.find((o) => String(o.orderId) === String(currentOrder.orderId));
    if (fresh) {
      currentOrder = normalizeOrder({ ...fresh, idempotencyKey: currentOrder.idempotencyKey || fresh.idempotencyKey });
    }
    syncFormFromOrder(currentOrder);
    renderOrder(currentOrder);
  } else if (!userPinnedOrder) {
    const pending = state.pendingOrders[0];
    if (pending) {
      currentOrder = normalizeOrder({ ...pending });
      syncFormFromOrder(currentOrder);
      renderOrder(currentOrder);
    } else {
      syncFormFromOrder(null);
    }
  }
}

async function refreshDemoState() {
  const { userId, skuId, couponId } = getParams();
  const state = await api('GET', `/demo/state?userId=${userId}&skuId=${skuId}&couponId=${couponId}&orderLimit=20`);
  applyDemoState(state);
  return state;
}

async function createOrder() {
  const form = getFormValues();
  if (!form.idempotencyKey) form.idempotencyKey = newIdempotencyKey();

  setButtonsLoading(true);
  try {
    log(`创建订单 · 用户 ${form.userId} · SKU ${form.skuId} × ${form.quantity}`);
    currentOrder = normalizeOrder(await api('POST', '/orders', form));
    userPinnedOrder = true;
    syncFormFromOrder(currentOrder);
    renderOrder(currentOrder);
    await refreshDemoState();
    log(`✓ 订单 ${currentOrder.orderId} 创建成功 · 应付 ${formatMoney(currentOrder.payAmount)}`);
  } catch (e) {
    log(formatError(e), false);
    await refreshDemoState().catch(() => {});
  } finally {
    setButtonsLoading(false);
  }
}

async function payOrder() {
  if (!currentOrder) return;
  const payNo = 'PAY-' + Date.now();
  els.btnPay.disabled = true;
  els.btnClose.disabled = true;
  try {
    log(`支付订单 ${currentOrder.orderId} · ${payNo}`);
    currentOrder = normalizeOrder(await api('POST', `/orders/${currentOrder.orderId}/pay`, { payNo }));
    syncFormFromOrder(currentOrder);
    renderOrder(currentOrder);
    await refreshDemoState();
    log('✓ 支付成功 · 库存已确认扣减 · 优惠券已核销');
  } catch (e) {
    log(formatError(e), false);
    await refreshDemoState().catch(() => {});
    els.btnPay.disabled = currentOrder?.status !== 'PENDING_PAY';
    els.btnClose.disabled = currentOrder?.status !== 'PENDING_PAY';
  }
}

async function closeOrder() {
  if (!currentOrder) return;
  els.btnPay.disabled = true;
  els.btnClose.disabled = true;
  try {
    log(`关单 ${currentOrder.orderId}`);
    currentOrder = normalizeOrder(await api('POST', `/orders/${currentOrder.orderId}/close`, {}));
    syncFormFromOrder(currentOrder);
    renderOrder(currentOrder);
    await refreshDemoState();
    log('✓ 订单已关闭 · 库存与优惠券已释放');
  } catch (e) {
    log(formatError(e), false);
    await refreshDemoState().catch(() => {});
    els.btnPay.disabled = currentOrder?.status !== 'PENDING_PAY';
    els.btnClose.disabled = currentOrder?.status !== 'PENDING_PAY';
  }
}

async function resetDemo() {
  setButtonsLoading(true);
  try {
    log('重置数据…');
    const state = await api('POST', '/demo/reset', {});
    currentOrder = null;
    userPinnedOrder = false;
    syncFormFromOrder(null);
    newIdempotencyKey();
    applyDemoState(state);
    log('已重置：待支付订单已关闭，优惠券与库存已恢复');
  } catch (e) {
    log(formatError(e), false);
  } finally {
    setButtonsLoading(false);
  }
}

els.btnCreate.addEventListener('click', createOrder);
els.btnNew.addEventListener('click', prepareNewOrderForm);
els.btnPay.addEventListener('click', payOrder);
els.btnClose.addEventListener('click', closeOrder);
els.btnRefresh.addEventListener('click', async () => {
  setButtonsLoading(true);
  try {
    await refreshDemoState();
    log('状态已刷新');
  } catch (e) {
    log(formatError(e), false);
  } finally {
    setButtonsLoading(false);
  }
});
els.btnReset.addEventListener('click', resetDemo);

document.getElementById('useCoupon').addEventListener('change', (e) => {
  document.getElementById('userCouponId').disabled = !e.target.checked;
});

document.getElementById('userId').addEventListener('change', () => refreshDemoState().catch(() => {}));

newIdempotencyKey();
refreshDemoState()
  .then(() => log('就绪'))
  .catch((e) => log(formatError(e), false));
