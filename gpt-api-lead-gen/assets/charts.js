(function () {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();

  var el = document.getElementById('chart-time');
  if (!el) return;

  var chart = echarts.init(el, null, { renderer: 'svg' });
  chart.setOption({
    animation: false,
    tooltip: {
      trigger: 'item',
      appendToBody: true,
      formatter: function (p) { return p.name + '：' + p.value + ' 分钟（' + p.percent + '%）'; }
    },
    legend: {
      bottom: 0,
      textStyle: { color: muted, fontFamily: 'WorkSans, Microsoft YaHei, sans-serif' },
      itemGap: 14
    },
    color: [accent, accent2, accent + '99', accent2 + '99'],
    series: [{
      type: 'pie',
      radius: ['40%', '66%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: bg2, borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, color: ink, fontWeight: 700 } },
      data: [
        { name: '内容生产', value: 60 },
        { name: '社区互动', value: 30 },
        { name: '私域承接', value: 18 },
        { name: '数据复盘', value: 12 }
      ]
    }]
  });
  window.addEventListener('resize', function () { chart.resize(); });
})();