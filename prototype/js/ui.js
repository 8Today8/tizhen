/* ============================================================
 * 题阵 · UI 基础组件
 * ============================================================ */
(function () {
  const TZ = (window.TZ = window.TZ || {});

  const ICONS = {
    home: '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/>',
    route: '<circle cx="6" cy="19" r="2"/><circle cx="18" cy="5" r="2"/><path d="M8 19h3a4 4 0 0 0 4-4V9a4 4 0 0 1 4-4h-1"/>',
    code: '<polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>',
    trophy: '<path d="M8 21h8"/><path d="M12 17v4"/><path d="M7 4h10v4a5 5 0 0 1-10 0V4Z"/><path d="M7 6H4a2 2 0 0 0 2 4h1"/><path d="M17 6h3a2 2 0 0 1-2 4h-1"/>',
    swords: '<polyline points="14.5 17.5 3 6 3 3 6 3 17.5 14.5"/><line x1="13" y1="19" x2="19" y2="13"/><line x1="16" y1="16" x2="20" y2="20"/><line x1="19" y1="21" x2="21" y2="19"/><polyline points="14.5 6.5 18 3 21 3 21 6 17.5 9.5"/><line x1="5" y1="14" x2="9" y2="18"/><line x1="7" y1="17" x2="4" y2="20"/><line x1="3" y1="19" x2="5" y2="21"/>',
    wand: '<path d="m15 4 5 5"/><path d="M4 20 20 4"/><path d="m5 5 1 1"/><path d="m8 2 1 3"/><path d="m2 8 3 1"/>',
    history: '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><polyline points="3 3 3 8 8 8"/><polyline points="12 7 12 12 15 14"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    book: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z"/>',
    chart: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z"/>',
    file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14 2 14 8 20 8"/><line x1="8" y1="13" x2="16" y2="13"/><line x1="8" y1="17" x2="16" y2="17"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>',
    plus: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
    search: '<circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>',
    arrowRight: '<line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>',
    check: '<polyline points="20 6 9 17 4 12"/>',
    x: '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',
    clock: '<circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 14"/>',
    download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
    external: '<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/>',
    edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
    trash: '<polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>',
    play: '<polygon points="5 3 19 12 5 21 5 3"/>',
    refresh: '<polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>',
    medal: '<circle cx="12" cy="8" r="6"/><path d="m8.2 13.8-1.7 6.2 5.5-3.3 5.5 3.3-1.7-6.2"/>',
    sparkles: '<path d="m12 3 1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5Z"/><path d="m19 15 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8Z"/>',
    terminal: '<polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/>',
    menu: '<line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>',
    chevronDown: '<polyline points="6 9 12 15 18 9"/>',
    chevronLeft: '<polyline points="15 18 9 12 15 6"/>',
    mail: '<rect x="2" y="4" width="20" height="16" rx="2"/><polyline points="22 6 12 13 2 6"/>',
    phone: '<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.79 19.79 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92Z"/>',
    school: '<path d="m3 9 9-6 9 6-9 6-9-6Z"/><path d="M21 9v6"/><path d="M6 12v5c3 2 9 2 12 0v-5"/>',
    link: '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>',
    alert: '<path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
    info: '<circle cx="12" cy="12" r="9"/><line x1="12" y1="11" x2="12" y2="16"/><line x1="12" y1="8" x2="12.01" y2="8"/>',
    flag: '<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V4s-1 1-4 1-5-2-8-2-4 1-4 1Z"/><line x1="4" y1="22" x2="4" y2="15"/>',
    zap: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
    layers: '<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/>',
    target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
    copy: '<rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>',
    filter: '<polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>'
  };

  TZ.esc = function (str) {
    return String(str == null ? '' : str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  TZ.icon = function (name, size, cls) {
    size = size || 18;
    return '<svg class="icon ' + (cls || '') + '" width="' + size + '" height="' + size +
      '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
      (ICONS[name] || ICONS.info) + '</svg>';
  };

  /* ---------- 固定 emoji 语言 ----------
   * 同一入口永远使用同一个 emoji；AI 造数据统一使用 🤖，不与动物 emoji 混用。
   * 可用集合：😸 😻 🍋 🍎 🍌 😽 🍉 🐱 🍈 😼 🐼 🍇 🐻‍❄️ 🐻 🐨 🐰 🐹 🦊（🤖 仅用于 AI 造数据）
   */
  TZ.EMOJI = {
    cNav: { home: '😸', routes: '🍋', problems: '🍎', leaderboard: '🍉', battle: '😼', datagen: '🤖', records: '🍇' },
    aNav: {
      home: '🐨', routes: '🍋', stages: '🐰', oj: '🍎', objective: '🍈', short: '😽', external: '🐱',
      users: '🐻', leaderboard: '🍉', datagen: '🤖', roles: '🦊', logs: '🐹'
    },
    extra: { profile: '😻', wrongbook: '🍌', xcpc: '🐼' }
  };

  TZ.emojiIcon = function (emoji) {
    return '<span class="emoji-ico" aria-hidden="true">' + emoji + '</span>';
  };

  TZ.emojiFor = function (text) {
    const t = String(text || '').replace(/<[^>]*>/g, '');
    const rules = [
      [/造数据|数据任务|LLM 造|LLM 凭证/, '🤖'],
      [/学习路线|路线/, '🍋'],
      [/阶段/, '🐰'],
      [/题库|OJ/, '🍎'],
      [/客观题/, '🍈'],
      [/简答题/, '😽'],
      [/外链题/, '🐱'],
      [/榜单|排行|周榜|总榜/, '🍉'],
      [/对战/, '😼'],
      [/错题/, '🍌'],
      [/学习记录|提交历史|记录/, '🍇'],
      [/个人中心|个人资料/, '😻'],
      [/XCPC/, '🐼'],
      [/用户/, '🐻'],
      [/角色|权限/, '🦊'],
      [/日志/, '🐹'],
      [/工作台|运营后台/, '🐨'],
      [/首页|欢迎|快捷练习/, '😸']
    ];
    for (let i = 0; i < rules.length; i++) {
      if (rules[i][0].test(t)) return rules[i][1];
    }
    return '';
  };

  TZ.decorateHeads = function (html) {
    return String(html || '').replace(/<(h2|h3)(\s[^>]*)?>([^<]{2,24})<\/\1>/g, function (m, tag, attrs, text) {
      if (/[\u{1F300}-\u{1FAFF}]/u.test(text)) return m;
      const emo = TZ.emojiFor(text);
      if (!emo) return m;
      return '<' + tag + (attrs || '') + '><span class="head-emoji" aria-hidden="true">' + emo + '</span>' + text + '</' + tag + '>';
    });
  };

  TZ.brand = function (light, href) {
    return '<a class="brand ' + (light ? 'brand-light' : '') + '" href="' + (href || '#/home') + '">' +
      '<span class="logo-mark" aria-hidden="true">🐻‍❄️</span>' +
      '<span class="brand-name">题阵</span>' +
      '<span class="brand-tag">TIZHEN</span></a>';
  };

  TZ.themeToggle = function (extraClass) {
    const dusk = document.documentElement.getAttribute('data-theme') === 'dusk';
    const label = dusk ? '明亮' : '暗淡';
    const hint = dusk ? '切换到灵动明亮风格' : '切换到暗淡忧郁风格';
    return '<button class="btn btn-ghost btn-sm nav-theme theme-toggle ' + (extraClass || '') +
      '" data-action="toggle-theme" data-theme-toggle title="' + hint + '" aria-label="' + hint + '">' +
      TZ.emojiIcon('😽') + '<span class="theme-label">' + label + '</span></button>';
  };

  TZ.avatar = function (user, size) {
    const palettes = ['#f7c9d3', '#c9e8d8', '#fbe6ac', '#cfe2f5', '#e3d2ec', '#f7d7c2'];
    let hash = 0;
    const key = (user && (user.id || user.name)) || '?';
    for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
    const bg = palettes[hash % palettes.length];
    const s = size || 36;
    const avatarUrl = user && user.avatarUrl ? String(user.avatarUrl) : '';
    if (avatarUrl) {
      return '<span class="avatar avatar-img" style="width:' + s + 'px;height:' + s + 'px">' +
        '<img src="' + TZ.esc(avatarUrl) + '" alt="" loading="lazy" /></span>';
    }
    return '<span class="avatar" style="width:' + s + 'px;height:' + s + 'px;font-size:' + Math.round(s * 0.4) +
      'px;background:' + bg + '">' + TZ.esc((user && (user.avatar || user.name)) ? (user.avatar || user.name).slice(0, 1) : '?') + '</span>';
  };

  /**
   * 校验并压缩头像文件，返回可直接写入 localStorage 的 data URL。
   * 原型不调用服务端，压缩到 256px 以控制本地存储体积。
   */
  TZ.readAvatarFile = function (file, callback) {
    if (!file) { callback('请选择头像图片'); return; }
    if (['image/jpeg', 'image/png'].indexOf(file.type) < 0) {
      callback('仅支持 JPG 或 PNG 图片'); return;
    }
    if (file.size > 2 * 1024 * 1024) {
      callback('头像图片不能超过 2MB'); return;
    }
    const reader = new FileReader();
    reader.onerror = function () { callback('读取图片失败，请重试'); };
    reader.onload = function () {
      const img = new Image();
      img.onerror = function () { callback('图片格式无法识别'); };
      img.onload = function () {
        try {
          const max = 256;
          const scale = Math.min(1, max / Math.max(img.width, img.height));
          const width = Math.max(1, Math.round(img.width * scale));
          const height = Math.max(1, Math.round(img.height * scale));
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          ctx.fillStyle = '#fff';
          ctx.fillRect(0, 0, width, height);
          ctx.drawImage(img, 0, 0, width, height);
          callback(null, canvas.toDataURL('image/jpeg', 0.86));
        } catch (e) {
          callback('图片处理失败，请换一张重试');
        }
      };
      img.src = reader.result;
    };
    reader.readAsDataURL(file);
  };

  TZ.badge = function (text, tone) {
    return '<span class="badge badge-' + (tone || 'slate') + '">' + text + '</span>';
  };
  TZ.tag = function (text, tone) {
    return '<span class="tag tag-' + (tone || 'slate') + '">' + text + '</span>';
  };
  TZ.difficultyTone = function (level) {
    return ({ '入门': 'green', '基础': 'cyan', '进阶': 'amber', '困难': 'red', '挑战': 'purple' })[level] || 'slate';
  };
  TZ.difficultyBadge = function (level) {
    return TZ.badge(level || '未定级', TZ.difficultyTone(level));
  };
  TZ.statusTone = function (status) {
    return ({ active: 'green', published: 'green', success: 'green', disabled: 'slate', draft: 'slate', failed: 'red', generating: 'purple', llm_data: 'purple', running: 'amber', waiting: 'cyan', ready: 'indigo', playing: 'amber', finished: 'slate' })[status] || 'slate';
  };
  TZ.statusText = function (status) {
    return ({ active: '正常', disabled: '已禁用', published: '已上架', draft: '草稿', success: '成功', failed: '失败', generating: 'LLM 构造中', llm_data: 'LLM 生成数据中', running: '生成中', waiting: '等待对手', ready: '可以开始', playing: '对战中', finished: '已结束' })[status] || status;
  };
  TZ.statusBadge = function (status) {
    return TZ.badge(TZ.statusText(status), TZ.statusTone(status));
  };
  TZ.genModeBadge = function (mode) {
    if (mode === 'llm') return TZ.badge('🤖 LLM 构造', 'purple');
    if (mode === 'llm_direct') return TZ.badge('🤖 LLM 直接生成数据', 'amber');
    return TZ.badge('🤖 随机生成', 'slate');
  };
  TZ.genModeText = function (mode) {
    if (mode === 'llm') return 'LLM 构造';
    if (mode === 'llm_direct') return 'LLM 直接生成数据';
    return '随机生成';
  };
  TZ.failureStageText = function (stage) {
    return ({ llm: 'LLM 构造', generator: '生成器校验', data: '数据范围校验', llm_data: 'LLM 数据格式/范围校验', credential: '凭证校验', std: 'std 执行' })[stage] || stage;
  };
  TZ.normalizeBiliUrl = function (raw) {
    const input = String(raw || '').trim();
    if (!input) return '';
    let url;
    try {
      url = new URL(/^https?:\/\//i.test(input) ? input : 'https://' + input);
    } catch (e) {
      return '';
    }
    if (!/(^|\.)bilibili\.com$/i.test(url.hostname)) return '';
    const matched = url.pathname.match(/\/video\/([Bb][Vv][0-9A-Za-z]+)/);
    if (!matched) return '';
    const bv = 'BV' + matched[1].slice(2);
    const p = parseInt(url.searchParams.get('p') || '1', 10);
    return 'https://www.bilibili.com/video/' + bv + (p > 1 ? '?p=' + p : '');
  };
  TZ.courseRowHtml = function (course) {
    return '<a class="course-row" href="' + TZ.esc(course.url) + '" target="_blank" rel="noopener noreferrer" title="在 B 站观看">' +
      '<span class="course-play">' + TZ.icon('play', 11) + '</span>' +
      '<span class="course-title">' + TZ.esc(course.title) + '</span>' +
      (course.duration ? '<span class="course-duration">' + TZ.esc(course.duration) + '</span>' : '') +
      '<span class="course-source">' + TZ.esc(course.source || 'B 站') + TZ.icon('external', 11) + '</span>' +
      '</a>';
  };
  TZ.courseListHtml = function (courses) {
    if (!courses || !courses.length) return '<p class="muted course-empty">暂无视频课程。</p>';
    return '<div class="course-list">' + courses.map(TZ.courseRowHtml).join('') + '</div>';
  };
  TZ.verdictTone = function (v) {
    return ({ AC: 'green', WA: 'red', TLE: 'amber', MLE: 'purple', RE: 'red', CE: 'slate' })[v] || 'slate';
  };
  TZ.verdictBadge = function (v) {
    return '<span class="verdict-badge verdict-' + String(v).toLowerCase() + '">' + v + '</span>';
  };
  TZ.typeBadge = function (type) {
    if (type === 'oj') return TZ.badge('编程题', 'indigo');
    if (type === 'objective') return TZ.badge('客观题', 'purple');
    if (type === 'short') return TZ.badge('简答题', 'green');
    if (type === 'external') return TZ.badge('外链题', 'cyan');
    return TZ.badge(type, 'slate');
  };
  TZ.typeText = function (type) {
    return ({ oj: '编程题', objective: '客观题', short: '简答题', external: '外链题' })[type] || type;
  };

  TZ.progress = function (percent, small) {
    return '<div class="progress ' + (small ? 'progress-sm' : '') + '"><span style="width:' + Math.max(0, Math.min(100, percent)) + '%"></span></div>';
  };

  TZ.ring = function (percent, label) {
    const p = Math.max(0, Math.min(100, percent));
    return '<div class="ring" style="background:conic-gradient(var(--primary) ' + p + '%, var(--border) 0)">' +
      '<div class="ring-inner"><strong>' + p + '%</strong><span>' + (label || '完成度') + '</span></div></div>';
  };

  TZ.pageHeader = function (title, subtitle, actions, breadcrumb) {
    const emo = TZ.emojiFor(title);
    return '<div class="page-head">' +
      '<div class="page-head-main">' +
      (breadcrumb ? '<div class="breadcrumb">' + breadcrumb + '</div>' : '') +
      '<h1 class="page-title">' + (emo ? '<span class="page-emoji" aria-hidden="true">' + emo + '</span>' : '') + title + '</h1>' +
      (subtitle ? '<div class="page-sub">' + subtitle + '</div>' : '') +
      '</div>' +
      (actions ? '<div class="page-actions">' + actions + '</div>' : '') +
      '</div>';
  };

  const EMPTY_EMOJI = {
    route: '🍋', layers: '🐰', code: '🍎', book: '🍌', trophy: '🍉', swords: '😼', wand: '🤖',
    history: '🍇', users: '🐻', user: '😻', shield: '🦊', search: '😸', file: '🍈', info: '😸'
  };

  TZ.empty = function (iconName, title, desc, action) {
    const emo = EMPTY_EMOJI[iconName] || '😸';
    return '<div class="empty">' +
      '<div class="empty-emoji" aria-hidden="true">' + emo + '</div>' +
      '<div class="empty-title">' + title + '</div>' +
      (desc ? '<div class="empty-desc">' + desc + '</div>' : '') +
      (action ? '<div class="empty-action">' + action + '</div>' : '') +
      '</div>';
  };

  TZ.sourcePills = function (row) {
    let html = '<span class="data-pill data-system">站内</span>';
    if (row.cf > 0) html += '<span class="data-pill data-cf">CF 同步</span>';
    if (row.manual > 0) html += '<span class="data-pill data-manual">手动申报</span>';
    return html;
  };

  TZ.navLink = function (href, label, active, key) {
    const emo = TZ.EMOJI.cNav[key] || '';
    return '<a class="nav-link ' + (active === key ? 'active' : '') + '" href="' + href + '">' +
      (emo ? TZ.emojiIcon(emo) : '') + '<span>' + label + '</span></a>';
  };

  TZ.layoutC = function (content, active) {
    const user = TZ.currentUser();
    if (!user) return content;
    const admin = TZ.currentAdmin();
    content = TZ.decorateHeads(content);
    return '<header class="topnav"><div class="container topnav-inner">' +
      TZ.brand() +
      '<nav class="nav-links" id="nav-links">' +
      TZ.navLink('#/home', '首页', active, 'home') +
      TZ.navLink('#/routes', '学习路线', active, 'routes') +
      TZ.navLink('#/problems', '题库', active, 'problems') +
      TZ.navLink('#/leaderboard', '榜单', active, 'leaderboard') +
      TZ.navLink('#/battle', '对战', active, 'battle') +
      TZ.navLink('#/datagen', '造数据', active, 'datagen') +
      TZ.navLink('#/records', '记录', active, 'records') +
      '</nav>' +
      '<div class="nav-right">' +
      TZ.themeToggle() +
      '<a class="btn btn-ghost btn-sm nav-xcpc" href="#/xcpc">' + TZ.emojiIcon('🐼') + ' XCPC</a>' +
      '<div class="user-chip-wrap">' +
      '<button class="user-chip" data-action="toggle-user-menu" aria-haspopup="true">' +
      TZ.avatar(user, 30) +
      '<span class="user-chip-name">' + TZ.esc(user.name) + '</span>' +
      TZ.icon('chevronDown', 14, 'chev') +
      '</button>' +
      '<div class="user-menu" id="user-menu">' +
      '<div class="user-menu-head">' + TZ.avatar(user, 42) + '<div><strong>' + TZ.esc(user.name) + '</strong><span>' + TZ.esc(user.school || '未填写学校') + '</span></div></div>' +
      '<a class="user-menu-item" href="#/profile">' + TZ.emojiIcon('😻') + ' 个人中心</a>' +
      '<a class="user-menu-item" href="#/records">' + TZ.emojiIcon('🍇') + ' 学习记录</a>' +
      '<a class="user-menu-item" href="#/wrongbook">' + TZ.emojiIcon('🍌') + ' 错题本</a>' +
      '<a class="user-menu-item" href="#/battle-records">' + TZ.emojiIcon('😼') + ' 对战记录</a>' +
      '<a class="user-menu-item" href="#/datagen/tasks">' + TZ.emojiIcon('🤖') + ' 造数据任务</a>' +
      '<div class="user-menu-divider"></div>' +
      '<button class="user-menu-item" data-action="switch-account">' + TZ.emojiIcon('🐻') + ' 切换演示账号</button>' +
      '<a class="user-menu-item" href="' + (admin ? '#/admin' : '#/admin/login') + '">' + TZ.emojiIcon('🦊') + ' 运营后台</a>' +
      '<button class="user-menu-item danger" data-action="logout">' + TZ.emojiIcon('😼') + ' 退出登录</button>' +
      '</div></div>' +
      '<button class="nav-toggle" data-action="toggle-nav" aria-label="展开导航">' + TZ.icon('menu', 20) + '</button>' +
      '</div></div></header>' +
      '<main class="app-main module-' + (active || 'home') + '"><div class="container">' + content + '</div></main>' +
      '<footer class="app-footer"><div class="container footer-inner">' +
      '<span>题阵 · 本地交互原型</span>' +
      '<span class="footer-dot">·</span>' +
      '<span>数据保存在浏览器 localStorage，可随时重置</span>' +
      '</div></footer>';
  };

  const ADMIN_TITLES = {
    home: '工作台', routes: '路线管理', stages: '阶段管理', oj: 'OJ 题库', objective: '客观题',
    short: '简答题', external: '外链题', users: '用户管理', leaderboard: '榜单管理', datagen: '造数据任务',
    roles: '角色管理', logs: '操作日志'
  };

  TZ.adminNavItem = function (href, iconName, label, active, key) {
    const emo = TZ.EMOJI.aNav[key] || '😸';
    return '<a class="admin-nav-link ' + (active === key ? 'active' : '') + '" href="' + href + '">' +
      TZ.emojiIcon(emo) + '<span>' + label + '</span></a>';
  };

  TZ.layoutAdmin = function (content, active) {
    const admin = TZ.currentAdmin();
    if (!admin) return content;
    content = TZ.decorateHeads(content);
    const isSuper = admin.role === 'super';
    const roleText = isSuper ? '超级管理员' : '内容运营';
    const topEmoji = TZ.EMOJI.aNav[active] || '🐨';
    return '<div class="admin-shell">' +
      '<aside class="admin-sidebar" id="admin-sidebar">' +
      '<div class="admin-brand-wrap">' + TZ.brand(true, '#/admin') + '</div>' +
      '<div class="admin-workspace"><span class="workspace-dot"></span> 运营工作台</div>' +
      '<nav class="admin-nav">' +
      '<div class="admin-nav-group">概览</div>' +
      TZ.adminNavItem('#/admin', 'chart', '工作台', active, 'home') +
      '<div class="admin-nav-group">内容</div>' +
      TZ.adminNavItem('#/admin/routes', 'route', '路线管理', active, 'routes') +
      TZ.adminNavItem('#/admin/stages', 'layers', '阶段管理', active, 'stages') +
      '<div class="admin-nav-group">题库</div>' +
      TZ.adminNavItem('#/admin/oj', 'code', 'OJ 题库', active, 'oj') +
      TZ.adminNavItem('#/admin/objective', 'book', '客观题', active, 'objective') +
      TZ.adminNavItem('#/admin/short', 'edit', '简答题', active, 'short') +
      TZ.adminNavItem('#/admin/external', 'link', '外链题', active, 'external') +
      '<div class="admin-nav-group">运营</div>' +
      TZ.adminNavItem('#/admin/users', 'users', '用户管理', active, 'users') +
      TZ.adminNavItem('#/admin/leaderboard', 'trophy', '榜单管理', active, 'leaderboard') +
      TZ.adminNavItem('#/admin/datagen', 'wand', '造数据任务', active, 'datagen') +
      '<div class="admin-nav-group">系统</div>' +
      (isSuper ? TZ.adminNavItem('#/admin/roles', 'shield', '角色管理', active, 'roles') : '') +
      TZ.adminNavItem('#/admin/logs', 'history', '操作日志', active, 'logs') +
      '</nav>' +
      '<div class="admin-user">' +
      TZ.avatar(admin, 36) +
      '<div class="admin-user-info"><strong>' + TZ.esc(admin.name) + '</strong><span>' + roleText + '</span></div>' +
      '<button class="icon-btn dark" data-action="admin-logout" title="退出后台">' + TZ.icon('logout', 16) + '</button>' +
      '</div>' +
      '</aside>' +
      '<div class="admin-main module-' + (active || 'home') + '">' +
      '<div class="admin-topbar">' +
      '<div class="admin-topbar-left">' +
      '<button class="nav-toggle admin-toggle" data-action="toggle-admin-nav" aria-label="展开菜单">' + TZ.icon('menu', 20) + '</button>' +
      '<div><div class="admin-crumb">运营后台</div><div class="admin-crumb-title">' + TZ.emojiIcon(topEmoji) + (ADMIN_TITLES[active] || '工作台') + '</div></div>' +
      '</div>' +
      '<div class="admin-topbar-right">' +
      TZ.themeToggle() +
      '<a class="btn btn-ghost btn-sm" href="#/home">' + TZ.icon('external', 15) + ' 返回前台</a>' +
      '<span class="admin-role-badge ' + (isSuper ? 'super' : '') + '">' + roleText + '</span>' +
      '</div></div>' +
      '<div class="admin-content">' + content + '</div>' +
      '</div></div>';
  };

  /* ---------- Toast ---------- */
  TZ.toast = function (message, type) {
    const root = document.getElementById('toast-root');
    if (!root) return;
    const el = document.createElement('div');
    el.className = 'toast toast-' + (type || 'info');
    const emo = type === 'success' ? '😻' : type === 'danger' ? '😼' : type === 'warning' ? '😽' : '😸';
    el.innerHTML = '<span class="toast-emoji" aria-hidden="true">' + emo + '</span><span>' + message + '</span>';
    root.appendChild(el);
    setTimeout(function () {
      el.classList.add('toast-out');
      setTimeout(function () { el.remove(); }, 220);
    }, 3000);
  };

  /* ---------- Modal ---------- */
  TZ.modal = function (opts) {
    const root = document.getElementById('modal-root');
    if (!root) return;
    root.innerHTML = '<div class="modal-overlay" data-action="modal-overlay">' +
      '<div class="modal ' + (opts.size || '') + '" role="dialog" aria-modal="true">' +
      '<div class="modal-head"><h3>' + opts.title + '</h3>' +
      '<button class="modal-close" data-action="close-modal" aria-label="关闭">' + TZ.icon('x', 18) + '</button></div>' +
      '<div class="modal-body">' + (opts.body || '') + '</div>' +
      (opts.footer ? '<div class="modal-foot">' + opts.footer + '</div>' : '') +
      '</div></div>';
  };

  TZ.closeModal = function () {
    const root = document.getElementById('modal-root');
    if (root) root.innerHTML = '';
  };

  TZ.confirm = function (title, message, onConfirm, opts) {
    opts = opts || {};
    TZ._confirmCb = onConfirm;
    TZ.modal({
      title: title || '请确认',
      body: '<div class="confirm-body"><div class="confirm-icon ' + (opts.danger === false ? 'info' : 'danger') + '">' +
        TZ.emojiIcon(opts.danger === false ? '😸' : '😼') + '</div><p>' + message + '</p></div>',
      footer: '<button class="btn btn-outline" data-action="close-modal">取消</button>' +
        '<button class="btn ' + (opts.danger === false ? 'btn-primary' : 'btn-danger') + '" data-action="confirm-yes">' +
        (opts.okText || '确认') + '</button>'
    });
  };

  /* ---------- 重新渲染（保留焦点） ---------- */
  TZ.rerender = function () {
    const ae = document.activeElement;
    const id = ae && ae.id;
    let start = null;
    try { start = ae && ae.selectionStart; } catch (e) {}
    TZ._suppressScroll = true;
    if (typeof TZ.route === 'function') TZ.route();
    if (id) {
      const el = document.getElementById(id);
      if (el && el.focus) {
        el.focus();
        try {
          if (start != null && el.setSelectionRange && (el.type === 'text' || el.tagName === 'TEXTAREA')) {
            el.setSelectionRange(start, start);
          }
        } catch (e) {}
      }
    }
  };

  TZ.scrollToId = function (id) {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };
})();
