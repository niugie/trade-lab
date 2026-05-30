(function () {
  function injectDemoBar() {
    if (document.getElementById('trade-lab-demo-bar')) return;

    var bar = document.createElement('div');
    bar.id = 'trade-lab-demo-bar';
    bar.innerHTML =
      '<div class="trade-lab-demo-bar-inner">' +
      '<a class="trade-lab-demo-link" href="/">← 控制台</a>' +
      '<span class="trade-lab-demo-title">Trade Lab API</span>' +
      '<a class="trade-lab-demo-link secondary" href="/actuator/health" target="_blank">健康检查</a>' +
      '</div>';

    var style = document.createElement('style');
    style.textContent =
      '#trade-lab-demo-bar{background:#1a2332;border-bottom:1px solid #2d3a4f;padding:10px 16px}' +
      '.trade-lab-demo-bar-inner{max-width:1460px;margin:0 auto;display:flex;align-items:center;gap:16px;flex-wrap:wrap}' +
      '.trade-lab-demo-link{color:#3b82f6;text-decoration:none;font-weight:600;font-size:14px}' +
      '.trade-lab-demo-link.secondary{font-weight:500;color:#8b9cb3}' +
      '.trade-lab-demo-title{color:#e8edf4;font-size:14px;flex:1}';

    document.head.appendChild(style);
    document.body.insertBefore(bar, document.body.firstChild);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', injectDemoBar);
  } else {
    injectDemoBar();
  }
})();
