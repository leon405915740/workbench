(function () {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();
  var ok = style.getPropertyValue('--ok').trim();
  var warn = style.getPropertyValue('--warn').trim();
  var bad = style.getPropertyValue('--bad').trim();

  // --- Chart 1: Budget breakdown (donut) ---
  var budgetEl = document.getElementById('chart-budget');
  if (budgetEl) {
    var budget = echarts.init(budgetEl, null, { renderer: 'svg' });
    budget.setOption({
      animation: false,
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        formatter: function (p) {
          return p.name + '：¥' + p.value.toLocaleString() + '（' + p.percent + '%）';
        }
      },
      legend: {
        bottom: 0,
        textStyle: { color: muted, fontFamily: 'WorkSans, Microsoft YaHei, sans-serif' },
        itemGap: 14
      },
      color: [accent, accent2, ok, warn, bad],
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: bg2, borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, color: ink, fontWeight: 700 } },
        data: [
          { name: 'API 垫资', value: 5000 },
          { name: '网站+服务器+域名', value: 1800 },
          { name: '内容创作', value: 3500 },
          { name: 'SEO工具+投放', value: 2500 },
          { name: '隐性人力工时', value: 10000 }
        ]
      }]
    });
    window.addEventListener('resize', function () { budget.resize(); });
  }

  // --- Chart 2: ROI scenarios (bar) ---
  var roiEl = document.getElementById('chart-roi');
  if (roiEl) {
    var roi = echarts.init(roiEl, null, { renderer: 'svg' });
    roi.setOption({
      animation: false,
      tooltip: {
        trigger: 'axis',
        appendToBody: true,
        valueFormatter: function (v) { return '¥' + Number(v).toLocaleString(); }
      },
      grid: { left: 20, right: 20, top: 40, bottom: 24, containLabel: true },
      xAxis: {
        type: 'category',
        data: ['总投入', '悲观收入', '中性收入', '乐观收入'],
        axisLine: { lineStyle: { color: rule } },
        axisTick: { show: false },
        axisLabel: { color: muted, fontFamily: 'WorkSans, Microsoft YaHei, sans-serif' }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: muted, formatter: '¥{value}' },
        splitLine: { lineStyle: { color: rule } }
      },
      series: [{
        type: 'bar',
        barWidth: 44,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: function (params) {
            return params.dataIndex === 0 ? muted : [bad, warn, ok][params.dataIndex - 1];
          }
        },
        label: {
          show: true,
          position: 'top',
          color: ink,
          fontWeight: 700,
          formatter: function (p) { return '¥' + Number(p.value).toLocaleString(); }
        },
        data: [22800, 3000, 9000, 21000]
      }]
    });
    window.addEventListener('resize', function () { roi.resize(); });
  }
})();