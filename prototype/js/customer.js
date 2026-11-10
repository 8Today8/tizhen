/* ============================================================
 * 题阵 · C 端页面
 * ============================================================ */
(function () {
  const TZ = window.TZ;

  TZ.CODE_TEMPLATES = {
    cpp: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    // 在这里编写代码\n\n    return 0;\n}',
    py: 'import sys\n\ndef main():\n    data = sys.stdin.read().split()\n    # 在这里编写代码\n\nif __name__ == "__main__":\n    main()'
  };

  TZ.state = Object.assign({
    problemFilters: { q: '', type: 'all', difficulty: 'all', tag: 'all' },
    recordsTab: 'progress',
    wrongType: 'all',
    leaderboardMode: 'week',
    showAllLeaderboard: false,
    drafts: {},
    languages: {},
    objectiveSelections: {},
    objectiveResults: {},
    shortResults: {},
    lastJudge: null,
    battleJudge: null,
    battleDraft: '',
    datagenMode: 'random'
  }, TZ.state || {});

  TZ.getDraft = function (problemId) {
    const lang = TZ.state.languages[problemId] || 'cpp';
    const key = problemId + ':' + lang;
    return Object.prototype.hasOwnProperty.call(TZ.state.drafts, key)
      ? TZ.state.drafts[key]
      : TZ.CODE_TEMPLATES[lang];
  };

  const C = (TZ.customer = {});

  /* ---------- 局部组件 ---------- */
  function tagsHtml(tags) {
    return (tags || []).map(function (t) { return TZ.tag(t); }).join('');
  }

  function nextStageFor(userId) {
    for (const r of TZ.db.routes) {
      for (const s of r.stages) {
        if (!TZ.getProgress(userId, s.id).completed) return s;
      }
    }
    return null;
  }

  function routeCard(route, user) {
    const p = TZ.routeProgress(user.id, route);
    return '<a class="card route-card card-hover" href="#/route/' + route.id + '">' +
      '<div class="route-cover cover-' + (route.color || 'indigo') + '">' +
      '<span class="route-cover-icon">' + TZ.emojiIcon('🍋') + '</span>' +
      '<span class="route-level">' + TZ.esc(route.level) + '</span>' +
      '<span class="route-cover-deco"></span>' +
      '</div>' +
      '<div class="route-body">' +
      '<div class="route-title-row"><h3>' + TZ.esc(route.title) + '</h3><span class="route-pct">' + p.percent + '%</span></div>' +
      '<p class="route-sub">' + TZ.esc(route.subtitle) + '</p>' +
      '<div class="route-meta"><span>' + TZ.icon('layers', 14) + ' ' + route.stages.length + ' 个阶段</span><span>' + TZ.icon('clock', 14) + ' ' + TZ.esc(route.estimated) + '</span></div>' +
      TZ.progress(p.percent, true) +
      '<div class="route-progress-text">已完成 ' + p.done + '/' + p.total + ' 个阶段</div>' +
      '</div></a>';
  }

  function lbMiniRow(row, meId) {
    const isMe = row.userId === meId;
    return '<a class="lb-mini-row ' + (isMe ? 'me' : '') + '" href="#/leaderboard">' +
      '<span class="rank ' + (row.rank <= 3 ? 'rank-' + row.rank : 'rank-plain') + '">' + row.rank + '</span>' +
      TZ.avatar(row.user, 30) +
      '<span class="lb-mini-user"><strong>' + TZ.esc(row.user.name) + (isMe ? '<em>我</em>' : '') + '</strong><span>' + TZ.esc(row.user.school || '未填写学校') + '</span></span>' +
      '<span class="lb-mini-total">' + row.total + '</span>' +
      '</a>';
  }

  function lbTabsHtml(active, action, small) {
    const btn = function (mode, label) {
      return '<button type="button" class="lb-tab' + (active === mode ? ' active' : '') + '" data-action="' + action + '" data-mode="' + mode + '">' + label + '</button>';
    };
    return '<div class="lb-tabs' + (small ? ' lb-tabs-sm' : '') + '">' + btn('week', '🍉 周榜') + btn('all', '🍇 总榜') + '</div>';
  }

  function externalCompletionHtml(p, user) {
    const done = TZ.isExternalDone(user.id, p.id);
    const isCf = p.platform === 'Codeforces';
    if (done) {
      const entry = TZ.db.externalCompletions[user.id][p.id];
      return '<div class="completion-row done">' +
        '<span class="completion-check">' + TZ.icon('check', 16) + '</span>' +
        '<div class="completion-info"><strong>已完成</strong><span>' + TZ.esc(entry.source) + ' · ' + TZ.fmtDate(entry.at) + '</span></div>' +
        '<button class="btn btn-ghost btn-sm" data-action="toggle-external" data-id="' + p.id + '">取消完成</button>' +
        '</div>';
    }
    return '<div class="completion-row">' +
      '<button class="btn btn-outline btn-sm" data-action="toggle-external" data-id="' + p.id + '">' + TZ.icon('check', 15) + ' 手动标记完成</button>' +
      (isCf && user.cfVerified ? '<button class="btn btn-soft btn-sm" data-action="cf-auto" data-id="' + p.id + '">' + TZ.icon('refresh', 15) + ' 模拟 CF 自动识别</button>' : '') +
      (isCf && !user.cfVerified ? '<span class="hint">绑定 Codeforces 后可自动识别</span>' : '') +
      '</div>';
  }

  function objectiveResultHtml(p, result, selected) {
    if (result.correct) {
      return '<div class="judge-result ac">' +
        '<div class="judge-main"><span class="verdict-badge verdict-ac">AC</span><div><strong>回答正确</strong><p>正确答案：' + p.answer.join('、') + '</p></div></div>' +
        '<div class="explanation"><strong>解析</strong><p>' + TZ.esc(p.explanation) + '</p></div>' +
        '<div class="result-note">客观题不计入周榜统计。</div>' +
        '</div>';
    }
    return '<div class="judge-result fail">' +
      '<div class="judge-main"><span class="verdict-badge verdict-wa">WA</span><div><strong>回答错误</strong><p>你选择了：' + (selected.length ? selected.join('、') : '未选择') + ' · 正确答案：' + p.answer.join('、') + '</p></div></div>' +
      '<div class="explanation"><strong>解析</strong><p>' + TZ.esc(p.explanation) + '</p></div>' +
      '<div class="result-note">本题已加入错题本，可稍后复盘。</div>' +
      '</div>';
  }

  function judgeResultHtml(judge) {
    if (!judge) return '';
    if (judge.loading) {
      return '<div class="judge-result judging"><span class="loading-emoji" aria-hidden="true">😸</span><div><strong>判题中…</strong><p>正在运行测试数据，请稍候</p></div></div>';
    }
    const ac = judge.verdict === 'AC';
    return '<div class="judge-result ' + (ac ? 'ac' : 'fail') + '">' +
      '<div class="judge-main"><span class="verdict-badge verdict-' + judge.verdict.toLowerCase() + '">' + judge.verdict + '</span>' +
      '<div><strong>' + TZ.esc(judge.detail) + '</strong><p>' + judge.time + ' · ' + judge.memory + '</p></div></div>' +
      (ac ? '<div class="judge-success">' + TZ.icon('check', 16) + ' 通过全部测试点，若为本题首次 AC 将计入本周站内过题。</div>' : '') +
      '</div>';
  }

  function submissionResultHtml(s) {
    if (s.type === 'short' && s.aiScore != null) {
      return TZ.badge(s.aiScore + ' 分', s.aiScore >= 60 ? 'green' : 'red');
    }
    return TZ.verdictBadge(s.verdict);
  }

  function submissionTable(subs, emptyDesc) {
    if (!subs.length) return TZ.empty('history', '还没有提交记录', emptyDesc || '提交代码或答案后，记录会出现在这里。');
    return '<div class="table-wrap"><table class="table table-compact"><thead><tr><th>时间</th><th>方式</th><th>结果</th><th>耗时</th><th>内存</th></tr></thead><tbody>' +
      subs.map(function (s) {
        return '<tr><td class="muted">' + TZ.fmtDate(s.createdAt) + '</td><td>' + TZ.esc(s.language) + '</td><td>' + submissionResultHtml(s) + '</td><td class="muted">' + s.time + '</td><td class="muted">' + s.memory + '</td></tr>';
      }).join('') +
      '</tbody></table></div>';
  }

  /* ---------- 登录 ---------- */
  C.login = function () {
    const top = TZ.getLeaderboard().slice(0, 3);
    return '<div class="login-page">' +
      '<div class="login-visual"><div class="login-visual-inner">' +
      '<div class="login-brand">' + TZ.brand(true) + '</div>' +
      '<h1>以题成阵，<br><span>向更强者挑战。</span></h1>' +
      '<p class="login-lead">学习路线、站内 OJ、跨平台周榜、1v1 对战与 AI 造数据，一个平台完成日常训练闭环。</p>' +
      '<div class="login-feature-list">' +
      '<span>' + TZ.emojiIcon('🍋') + ' 结构化学习路线</span>' +
      '<span>' + TZ.emojiIcon('🍎') + ' 站内 OJ 判题</span>' +
      '<span>' + TZ.emojiIcon('🍉') + ' 跨平台周榜</span>' +
      '<span>' + TZ.emojiIcon('😼') + ' 1v1 实时对战</span>' +
      '</div>' +
      '<div class="login-mini-card">' +
      '<div class="login-mini-head"><span>本周跨平台榜</span><span class="badge badge-white">演示数据</span></div>' +
      top.map(function (r, i) {
        return '<div class="login-mini-row"><span class="rank ' + (i < 3 ? 'rank-' + (i + 1) : 'rank-plain') + '">' + r.rank + '</span>' + TZ.avatar(r.user, 28) +
          '<span class="login-mini-name">' + TZ.esc(r.user.name) + '</span><span class="login-mini-total">' + r.total + '</span></div>';
      }).join('') +
      '</div>' +
      '</div></div>' +
      '<div class="login-form-side"><div class="login-form-card">' +
      '<div class="login-form-top"><span class="badge badge-indigo">选手入口</span><span class="muted">本地演示环境</span></div>' +
      '<h2>欢迎回来</h2>' +
      '<p class="muted" id="login-subtitle">使用用户名和密码登录。</p>' +
      '<div class="tabs login-tabs" role="tablist">' +
      '<button type="button" class="tab active" role="tab" aria-selected="true" data-action="login-tab" data-tab="login">😸 登录</button>' +
      '<button type="button" class="tab" role="tab" aria-selected="false" data-action="login-tab" data-tab="register">😽 注册</button>' +
      '</div>' +
      '<form data-form="login" class="form-stack">' +
      '<div class="field"><label class="label" for="login-username">用户名</label><input class="input" id="login-username" name="username" maxlength="20" autocomplete="username" placeholder="请输入用户名" /></div>' +
      '<div class="field"><label class="label" for="login-password">密码</label><input class="input" id="login-password" name="password" type="password" maxlength="32" autocomplete="current-password" placeholder="请输入密码" /></div>' +
      '<p class="hint">演示账号：demo / 123456</p>' +
      '<button class="btn btn-primary btn-lg btn-block" type="submit">登录</button>' +
      '</form>' +
      '<form data-form="register" class="form-stack" hidden>' +
      '<div class="field"><label class="label" for="register-username">用户名</label><input class="input" id="register-username" name="username" maxlength="20" autocomplete="username" placeholder="3–20 位字母、数字或下划线" /></div>' +
      '<div class="field"><label class="label" for="register-name">昵称</label><input class="input" id="register-name" name="nickname" maxlength="20" placeholder="用于周榜、错题本等站内展示" /></div>' +
      '<div class="field"><label class="label" for="register-password">密码</label><input class="input" id="register-password" name="password" type="password" maxlength="32" autocomplete="new-password" placeholder="至少 6 位" /></div>' +
      '<div class="field"><label class="label" for="register-confirm">确认密码</label><input class="input" id="register-confirm" name="confirm" type="password" maxlength="32" autocomplete="new-password" placeholder="请再次输入密码" /></div>' +
      '<p class="hint">注册后自动登录；用户名仅用于登录，昵称可在个人中心修改。</p>' +
      '<button class="btn btn-primary btn-lg btn-block" type="submit">注册并登录</button>' +
      '</form>' +
      '<div class="login-or"><span>或</span></div>' +
      '<button class="btn btn-soft btn-block" data-action="demo-login">使用演示账号进入</button>' +
      '<div class="login-foot"><a href="#/admin/login" class="login-admin-link">' + TZ.emojiIcon('🦊') + ' 运营后台登录</a><span class="muted">数据仅保存在本机浏览器</span></div>' +
      '</div></div></div>';
  };

  /* ---------- 首页 ---------- */
  C.home = function () {
    const user = TZ.currentUser();
    const lbMode = TZ.state.leaderboardMode === 'all' ? 'all' : 'week';
    const meWeek = TZ.getMyRankRow('week');
    const meBoard = TZ.getMyRankRow(lbMode);
    const top5 = TZ.getLeaderboard(lbMode).slice(0, 5);
    const nextStage = nextStageFor(user.id);
    let doneStages = 0;
    TZ.db.routes.forEach(function (r) {
      doneStages += TZ.routeProgress(user.id, r).done;
    });
    const wrongCount = TZ.db.wrongBook.filter(function (w) { return w.userId === user.id; }).length;
    return TZ.layoutC(
      '<section class="hero">' +
      '<div class="hero-grid-bg"></div>' +
      '<div class="hero-content">' +
      '<div class="hero-eyebrow">' + TZ.WEEK_LABEL + ' · 训练进行中</div>' +
      '<h1>你好，' + TZ.esc(user.name) + '。</h1>' +
      '<p>本周站内已过 <strong>' + (meWeek ? meWeek.inSystem : 0) + '</strong> 题，跨平台周榜第 <strong>' + (meWeek ? meWeek.rank : '-') + '</strong> 名。继续把每一道题变成自己的。</p>' +
      '<div class="hero-actions"><a class="btn btn-white" href="#/routes">继续学习 ' + TZ.icon('arrowRight', 16) + '</a>' +
      '<button class="btn btn-ghost-light" data-action="random-problem">随机一题</button></div>' +
      '</div>' +
      '<div class="hero-panel">' +
      '<div class="hero-stat"><span>站内过题</span><strong>' + (meWeek ? meWeek.inSystem : 0) + '</strong><em>本周首次 AC</em></div>' +
      '<div class="hero-stat"><span>CF 过题</span><strong>' + (meWeek ? meWeek.cf : 0) + '</strong><em>模拟同步</em></div>' +
      '<div class="hero-stat"><span>手动申报</span><strong>' + (meWeek ? meWeek.manual : 0) + '</strong><em>其他平台</em></div>' +
      '<div class="hero-stat"><span>学习进度</span><strong>' + doneStages + '</strong><em>已完成阶段</em></div>' +
      '</div></section>' +
      '<section class="section"><div class="section-head"><div><h2 class="section-title">学习路线</h2><div class="section-sub">按阶段推进，题目与外部资源配套</div></div><a class="link" href="#/routes">全部路线 ' + TZ.icon('arrowRight', 14) + '</a></div>' +
      '<div class="grid grid-2">' + TZ.db.routes.map(function (r) { return routeCard(r, user); }).join('') + '</div></section>' +
      '<div class="split section">' +
      '<section><div class="section-head"><div><h2 class="section-title">跨平台榜单</h2><div class="section-sub">' + (lbMode === 'all' ? '全部历史累计 · 我的总榜排名 #' + (meBoard ? meBoard.rank : '-') : TZ.WEEK_LABEL + ' · 站内实时 + CF 模拟同步') + '</div>' + lbTabsHtml(lbMode, 'leaderboard-tab', true) + '</div><a class="link" href="#/leaderboard">查看完整榜单 ' + TZ.icon('arrowRight', 14) + '</a></div>' +
      '<div class="card lb-mini">' + top5.map(function (r) { return lbMiniRow(r, user.id); }).join('') + '</div></section>' +
      '<aside class="home-side">' +
      '<div class="card card-pad quick-card"><div class="card-icon indigo">' + TZ.emojiIcon('🍎') + '</div><h3>快捷练习</h3><p class="muted">从题库随机抽一道题，保持手感。</p>' +
      '<button class="btn btn-primary btn-block" data-action="random-problem">随机一题</button>' +
      '<a class="btn btn-outline btn-block" href="' + (nextStage ? '#/stage/' + nextStage.id : '#/routes') + '">' + (nextStage ? '继续「' + TZ.esc(nextStage.title) + '」' : '浏览学习路线') + '</a></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>错题本</h3><a class="link" href="#/wrongbook">查看</a></div><p class="muted">' + wrongCount + ' 道题等待复盘</p><a class="btn btn-soft btn-block" href="#/wrongbook">去复盘</a></div>' +
      '<div class="card card-pad"><div class="card-icon amber">' + TZ.emojiIcon('🐼') + '</div><h3>XCPC 评分与获奖</h3><p class="muted">查看外部 XCPC Rating 与获奖记录。</p><a class="btn btn-outline btn-block" href="#/xcpc">前往查看 ' + TZ.icon('external', 14) + '</a></div>' +
      '</aside></div>',
      'home'
    );
  };

  /* ---------- 学习路线 ---------- */
  C.routes = function () {
    const user = TZ.currentUser();
    return TZ.layoutC(
      TZ.pageHeader('学习路线', '以「学科路线 → 阶段」组织内容，阶段内配套站内题与外部经典题。', '<span class="badge badge-indigo">' + TZ.db.routes.length + ' 条路线</span>') +
      '<div class="info-strip"><span class="info-strip-icon">' + TZ.icon('info', 17) + '</span><div><strong>内容说明</strong><p>阶段内容为公开学习资料整理，仅用于本地演示；外链题仅提供跳转与完成标记，不复制题面。</p></div></div>' +
      '<div class="grid grid-2 section">' + TZ.db.routes.map(function (r) { return routeCard(r, user); }).join('') + '</div>' +
      '<div class="card card-pad section learning-tip"><div class="card-icon cyan">' + TZ.emojiIcon('🍋') + '</div><div><h3>推荐学习方式</h3><p>先读阶段内容并完成配套题，再去外部平台刷同类题；错题会自动进入错题本，阶段完成后可随时回看。</p></div></div>',
      'routes'
    );
  };

  C.route = function (id) {
    const route = TZ.getRoute(id);
    const user = TZ.currentUser();
    if (!route) {
      return TZ.layoutC(TZ.empty('route', '路线不存在', '该路线可能已被下架。', '<a class="btn btn-primary" href="#/routes">返回路线列表</a>'), 'routes');
    }
    const p = TZ.routeProgress(user.id, route);
    const next = route.stages.find(function (s) { return !TZ.getProgress(user.id, s.id).completed; }) || route.stages[0];
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(route.title), TZ.esc(route.subtitle), '<a class="btn btn-outline" href="#/routes">' + TZ.icon('chevronLeft', 15) + ' 全部路线</a>', '<a href="#/routes">学习路线</a> <span class="crumb-sep">/</span> ' + TZ.esc(route.title)) +
      '<div class="route-detail-head cover-' + (route.color || 'indigo') + '"><div class="route-detail-icon">' + TZ.emojiIcon('🍋') + '</div>' +
      '<div class="route-detail-copy"><div class="route-detail-badges">' + TZ.badge(route.level, TZ.difficultyTone(route.level)) + TZ.badge(route.stages.length + ' 个阶段', 'slate') + TZ.badge(route.estimated, 'indigo') + '</div>' +
      '<p>' + TZ.esc(route.description) + '</p></div></div>' +
      '<div class="split section">' +
      '<section><div class="section-head"><div><h2 class="section-title">阶段列表</h2><div class="section-sub">建议按顺序推进</div></div></div>' +
      '<div class="stage-list">' + route.stages.map(function (s, i) {
        const done = TZ.getProgress(user.id, s.id).completed;
        const courses = s.courses || [];
        return '<div class="stage-block">' +
          '<a class="stage-item ' + (done ? 'done' : '') + '" href="#/stage/' + s.id + '">' +
          '<div class="stage-index">' + (done ? TZ.icon('check', 18) : i + 1) + '</div>' +
          '<div class="stage-main"><div class="stage-title-row"><h3>' + TZ.esc(s.title) + '</h3>' + (done ? TZ.badge('已完成', 'green') : '') + '</div>' +
          '<p>' + TZ.esc(s.summary) + '</p>' +
          '<div class="stage-meta"><span>' + TZ.icon('clock', 14) + ' ' + TZ.esc(s.duration) + '</span><span>' + TZ.icon('file', 14) + ' ' + s.problemIds.length + ' 道题</span>' +
          (courses.length ? '<span>' + TZ.icon('play', 14) + ' ' + courses.length + ' 门课程</span>' : '') + '</div></div>' +
          '<span class="stage-arrow">' + TZ.icon('arrowRight', 18) + '</span></a>' +
          (courses.length ? '<div class="stage-courses"><div class="stage-courses-head"><span class="stage-courses-label">' + TZ.icon('play', 13) + ' 视频课程</span><span>' + courses.length + ' 门 · B 站</span></div>' + TZ.courseListHtml(courses) + '</div>' : '') +
          '</div>';
      }).join('') + '</div></section>' +
      '<aside class="home-side"><div class="card card-pad route-progress-card">' + TZ.ring(p.percent) +
      '<h3>学习进度</h3><p class="muted">已完成 ' + p.done + '/' + p.total + ' 个阶段</p>' +
      (next ? '<a class="btn btn-primary btn-block" href="#/stage/' + next.id + '">' + (p.done ? '继续学习' : '开始学习') + '</a>' : '') +
      '</div><div class="card card-pad"><h3>路线信息</h3><div class="meta-list">' +
      '<div><span>难度</span><strong>' + TZ.esc(route.level) + '</strong></div>' +
      '<div><span>阶段数</span><strong>' + route.stages.length + '</strong></div>' +
      '<div><span>预计用时</span><strong>' + TZ.esc(route.estimated) + '</strong></div>' +
      '<div><span>内容来源</span><strong>公开资料整理</strong></div>' +
      '</div></div></aside></div>',
      'routes'
    );
  };

  /* ---------- 阶段详情 ---------- */
  C.stage = function (id) {
    const found = TZ.getStage(id);
    const user = TZ.currentUser();
    if (!found) {
      return TZ.layoutC(TZ.empty('layers', '阶段不存在', '该阶段可能已被下架。', '<a class="btn btn-primary" href="#/routes">返回学习路线</a>'), 'routes');
    }
    const stage = found.stage;
    const route = found.route;
    const done = TZ.getProgress(user.id, stage.id).completed;
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(stage.title), TZ.esc(stage.summary), '<a class="btn btn-outline" href="#/route/' + route.id + '">' + TZ.icon('chevronLeft', 15) + ' 返回路线</a>',
        '<a href="#/routes">学习路线</a> <span class="crumb-sep">/</span> <a href="#/route/' + route.id + '">' + TZ.esc(route.title) + '</a> <span class="crumb-sep">/</span> ' + TZ.esc(stage.title)) +
      '<div class="split stage-layout">' +
      '<section><div class="card card-pad prose stage-content">' + stage.content + '</div>' +
      ((stage.courses || []).length ? '<div class="card card-pad stage-video-card"><div class="section-head compact"><h3>视频课程</h3><span class="muted">' + stage.courses.length + ' 门 · B 站</span></div>' + TZ.courseListHtml(stage.courses) + '<p class="hint">课程为 B 站外部视频，点击将在新标签页打开；观看不计入阶段进度。</p></div>' : '') +
      '</section>' +
      '<aside class="home-side">' +
      '<div class="card card-pad stage-status-card"><div class="stage-status-icon ' + (done ? 'done' : '') + '">' + TZ.icon(done ? 'check' : 'clock', 24) + '</div>' +
      '<h3>' + (done ? '本阶段已完成' : '标记学习进度') + '</h3>' +
      '<p class="muted">' + (done ? '完成于 ' + TZ.fmtDate(TZ.getProgress(user.id, stage.id).completedAt) : '完成内容与练习后，可手动标记完成。') + '</p>' +
      '<button class="btn ' + (done ? 'btn-outline' : 'btn-primary') + ' btn-block" data-action="toggle-stage" data-id="' + stage.id + '">' + (done ? '取消完成标记' : '标记本阶段完成') + '</button></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>配套题目</h3><span class="muted">' + stage.problemIds.length + ' 道</span></div>' +
      '<div class="related-list">' + stage.problemIds.map(function (pid) {
        const p = TZ.getProblem(pid);
        if (!p) return '';
        if (p.type === 'external') {
          return '<div class="related-external"><div class="related-external-head"><span class="badge badge-cyan">' + TZ.esc(p.platform) + '</span>' + TZ.difficultyBadge(p.difficulty) + '</div>' +
            '<a class="related-title" href="' + p.url + '" target="_blank" rel="noopener">' + TZ.esc(p.title) + TZ.icon('external', 13) + '</a>' +
            externalCompletionHtml(p, user) + '</div>';
        }
        return '<a class="related-problem" href="#/problem/' + p.id + '"><span class="related-type">' + TZ.typeBadge(p.type) + '</span><span class="related-title">' + TZ.esc(p.title) + '</span>' + TZ.difficultyBadge(p.difficulty) + TZ.icon('arrowRight', 14) + '</a>';
      }).join('') + '</div>' +
      '<div class="hint section-hint">外链题完成状态不计入周榜。</div></div>' +
      '<div class="card card-pad"><h3>阶段信息</h3><div class="meta-list"><div><span>所属路线</span><strong>' + TZ.esc(route.title) + '</strong></div><div><span>预计用时</span><strong>' + TZ.esc(stage.duration) + '</strong></div><div><span>内容来源</span><strong>公开资料整理</strong></div></div></div>' +
      '</aside></div>',
      'routes'
    );
  };

  /* ---------- 题库列表 ---------- */
  C.problems = function () {
    const user = TZ.currentUser();
    const f = TZ.state.problemFilters;
    const all = TZ.db.problems.concat(TZ.db.objectives, TZ.db.shortAnswers, TZ.db.externals);
    const tagSet = {};
    all.forEach(function (p) { (p.tags || []).forEach(function (t) { tagSet[t] = true; }); });
    const tags = Object.keys(tagSet).sort(function (a, b) { return a.localeCompare(b, 'zh'); });
    const rows = all.filter(function (p) {
      if (f.type !== 'all' && p.type !== f.type) return false;
      if (f.difficulty !== 'all' && p.difficulty !== f.difficulty) return false;
      if (f.tag !== 'all' && (p.tags || []).indexOf(f.tag) < 0) return false;
      if (f.q && (p.title + ' ' + (p.tags || []).join(' ') + ' ' + (p.source || '')).toLowerCase().indexOf(f.q.toLowerCase()) < 0) return false;
      return true;
    });
    return TZ.layoutC(
      TZ.pageHeader('题库', '站内编程题、客观题、简答题与外部经典题统一检索。', '<span class="badge badge-indigo">共 ' + all.length + ' 道</span>') +
      '<div class="card filter-card"><div class="filter-bar">' +
      '<div class="search-box">' + TZ.icon('search', 17) + '<input id="problem-search" class="input" data-filter="problem-search" placeholder="搜索题目、标签或来源" value="' + TZ.esc(f.q) + '" /></div>' +
      '<select id="filter-type" class="select filter-select" data-filter="problem-type"><option value="all"' + (f.type === 'all' ? ' selected' : '') + '>全部类型</option><option value="oj"' + (f.type === 'oj' ? ' selected' : '') + '>编程题</option><option value="objective"' + (f.type === 'objective' ? ' selected' : '') + '>客观题</option><option value="short"' + (f.type === 'short' ? ' selected' : '') + '>简答题</option><option value="external"' + (f.type === 'external' ? ' selected' : '') + '>外链题</option></select>' +
      '<select id="filter-difficulty" class="select filter-select" data-filter="problem-difficulty"><option value="all"' + (f.difficulty === 'all' ? ' selected' : '') + '>全部难度</option><option value="入门"' + (f.difficulty === '入门' ? ' selected' : '') + '>入门</option><option value="基础"' + (f.difficulty === '基础' ? ' selected' : '') + '>基础</option><option value="进阶"' + (f.difficulty === '进阶' ? ' selected' : '') + '>进阶</option></select>' +
      '<select id="filter-tag" class="select filter-select" data-filter="problem-tag"><option value="all"' + (f.tag === 'all' ? ' selected' : '') + '>全部标签</option>' + tags.map(function (t) { return '<option value="' + TZ.esc(t) + '"' + (f.tag === t ? ' selected' : '') + '>' + TZ.esc(t) + '</option>'; }).join('') + '</select>' +
      '<button class="btn btn-ghost btn-sm" data-action="reset-problem-filters">' + TZ.icon('refresh', 15) + ' 重置</button>' +
      '</div></div>' +
      '<div class="section"><div class="section-head"><div><h2 class="section-title">题目列表</h2><div class="section-sub">筛选结果 ' + rows.length + ' 道</div></div></div>' +
      (rows.length ? '<div class="card"><div class="table-wrap"><table class="table problem-table"><thead><tr><th>题目</th><th>类型</th><th>难度</th><th>标签</th><th>来源</th><th>通过率</th><th></th></tr></thead><tbody>' +
        rows.map(function (p) {
          const title = p.type === 'external'
            ? '<a class="table-title" href="' + p.url + '" target="_blank" rel="noopener">' + TZ.esc(p.title) + TZ.icon('external', 13) + '</a>'
            : '<a class="table-title" href="#/problem/' + p.id + '">' + TZ.esc(p.title) + '</a>';
          const rate = p.type === 'oj' ? Math.round(p.accepted / p.submissions * 100) + '%' : '—';
          const action = p.type === 'external'
            ? '<a class="btn btn-ghost btn-sm" href="' + p.url + '" target="_blank" rel="noopener">打开 ' + TZ.icon('external', 14) + '</a>'
            : '<a class="btn btn-soft btn-sm" href="#/problem/' + p.id + '">练习</a>';
          return '<tr><td><div class="problem-cell">' + title + '<span class="problem-source">' + TZ.esc(p.source) + '</span></div></td>' +
            '<td>' + TZ.typeBadge(p.type) + '</td><td>' + TZ.difficultyBadge(p.difficulty) + '</td>' +
            '<td><div class="tag-row">' + tagsHtml(p.tags) + '</div></td>' +
            '<td class="muted">' + TZ.esc(p.source) + '</td><td class="num">' + rate + '</td><td class="row-actions">' + action + '</td></tr>';
        }).join('') + '</tbody></table></div></div>'
        : TZ.empty('search', '没有匹配的题目', '试试调整搜索关键词或筛选条件。', '<button class="btn btn-outline" data-action="reset-problem-filters">重置筛选</button>')) +
      '</div>',
      'problems'
    );
  };

  /* ---------- 题目详情 ---------- */
  C.problem = function (id) {
    const p = TZ.getProblem(id);
    if (!p) return TZ.layoutC(TZ.empty('file', '题目不存在', '该题目可能已下架。', '<a class="btn btn-primary" href="#/problems">返回题库</a>'), 'problems');
    if (p.type === 'external') return C.externalDetail(p);
    if (p.type === 'objective') return C.objectiveDetail(p);
    if (p.type === 'short') return C.shortAnswerDetail(p);
    return C.ojDetail(p);
  };

  C.ojDetail = function (p) {
    const user = TZ.currentUser();
    const lang = TZ.state.languages[p.id] || 'cpp';
    const code = TZ.getDraft(p.id);
    const subs = TZ.getSubmissions(user.id, p.id);
    const judge = TZ.state.lastJudge && TZ.state.lastJudge.problemId === p.id ? TZ.state.lastJudge : null;
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(p.title), '提交代码后由演示判题器返回结果，不连接真实评测服务。',
        '<a class="btn btn-outline" href="#/problems">' + TZ.icon('chevronLeft', 15) + ' 返回题库</a>',
        '<a href="#/problems">题库</a> <span class="crumb-sep">/</span> ' + TZ.esc(p.title)) +
      '<div class="problem-layout">' +
      '<div class="card card-pad problem-statement"><div class="problem-badges">' + TZ.typeBadge(p.type) + TZ.difficultyBadge(p.difficulty) + tagsHtml(p.tags) + '</div>' +
      '<div class="statement-meta"><span>' + TZ.icon('clock', 14) + ' ' + p.timeLimit + '</span><span>' + TZ.icon('file', 14) + ' ' + p.memoryLimit + '</span><span>' + TZ.icon('target', 14) + ' 通过率 ' + Math.round(p.accepted / p.submissions * 100) + '%</span></div>' +
      '<div class="prose"><h3>题目描述</h3>' + p.statement + '<h3>输入格式</h3>' + p.inputFormat + '<h3>输出格式</h3>' + p.outputFormat + '<h3>样例</h3>' +
      p.samples.map(function (s, i) {
        return '<div class="sample-block"><div class="sample-label">样例 ' + (i + 1) + '</div><div class="sample-grid"><div><span>输入</span><pre><code>' + TZ.esc(s.input) + '</code></pre></div><div><span>输出</span><pre><code>' + TZ.esc(s.output) + '</code></pre></div></div></div>';
      }).join('') +
      '<h3>数据范围与提示</h3><p>请使用标准输入输出；演示环境不执行真实代码，结果由代码中的标记模拟。</p></div></div>' +
      '<div class="problem-work">' +
      '<div class="code-panel"><div class="code-toolbar"><div class="code-dots"><span></span><span></span><span></span></div>' +
      '<select id="language-select" class="code-select" data-problem="' + p.id + '"><option value="cpp"' + (lang === 'cpp' ? ' selected' : '') + '>C++17</option><option value="py"' + (lang === 'py' ? ' selected' : '') + '>Python 3</option></select>' +
      '<button class="code-tool-btn" data-action="reset-code" data-id="' + p.id + '">重置模板</button>' +
      '<span class="code-file">main.' + (lang === 'py' ? 'py' : 'cpp') + '</span></div>' +
      '<textarea id="code-editor" class="code-editor" data-draft-key="' + p.id + ':' + lang + '" spellcheck="false">' + TZ.esc(code) + '</textarea>' +
      '<div class="code-actions"><span class="code-hint">演示判题：包含 // wa / // tle / runtime_error 可触发对应结果</span>' +
      '<button class="btn btn-primary" data-action="submit-oj" data-id="' + p.id + '"' + (judge && judge.loading ? ' disabled' : '') + '>' + (judge && judge.loading ? '判题中…' : TZ.icon('play', 15) + ' 提交代码') + '</button></div></div>' +
      '<div id="judge-result">' + judgeResultHtml(judge) + '</div>' +
      '<div class="card card-pad submission-card"><div class="section-head compact"><h3>提交记录</h3><span class="muted">' + subs.length + ' 次</span></div>' + submissionTable(subs, '提交代码后，记录会出现在这里。') + '</div>' +
      '</div></div>',
      'problems'
    );
  };

  C.objectiveDetail = function (p) {
    const user = TZ.currentUser();
    const selected = TZ.state.objectiveSelections[p.id] || [];
    const result = TZ.state.objectiveResults[p.id];
    const subs = TZ.getSubmissions(user.id, p.id);
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(p.title), '客观题自动判分，答错后自动进入错题本。',
        '<a class="btn btn-outline" href="#/problems">' + TZ.icon('chevronLeft', 15) + ' 返回题库</a>',
        '<a href="#/problems">题库</a> <span class="crumb-sep">/</span> ' + TZ.esc(p.title)) +
      '<div class="problem-layout objective-layout">' +
      '<div class="card card-pad objective-card"><div class="problem-badges">' + TZ.typeBadge(p.type) + TZ.badge(p.qtype === 'multiple' ? '多选题' : p.qtype === 'judge' ? '判断题' : '单选题', 'slate') + TZ.difficultyBadge(p.difficulty) + tagsHtml(p.tags) + '</div>' +
      '<div class="question-text">' + TZ.esc(p.title) + '</div>' +
      '<div class="question-tip">' + TZ.icon('info', 15) + ' ' + (p.qtype === 'multiple' ? '本题为多选题，请选择所有正确选项。' : '本题为单选题。') + '</div>' +
      '<div class="option-list">' + p.options.map(function (o) {
        let cls = 'option';
        if (selected.indexOf(o.key) >= 0) cls += ' selected';
        if (result) {
          if (p.answer.indexOf(o.key) >= 0) cls += ' correct';
          else if (selected.indexOf(o.key) >= 0) cls += ' wrong';
        }
        return '<button type="button" class="' + cls + '" data-action="select-option" data-id="' + p.id + '" data-key="' + o.key + '"' + (result ? ' disabled' : '') + '>' +
          '<span class="option-key">' + o.key + '</span><span class="option-text">' + TZ.esc(o.text) + '</span>' +
          (result && p.answer.indexOf(o.key) >= 0 ? '<span class="option-mark correct">' + TZ.icon('check', 16) + '</span>' : result && selected.indexOf(o.key) >= 0 ? '<span class="option-mark wrong">' + TZ.icon('x', 16) + '</span>' : '') +
          '</button>';
      }).join('') + '</div>' +
      '<div class="question-actions">' + (result
        ? '<button class="btn btn-outline" data-action="retry-objective" data-id="' + p.id + '">' + TZ.icon('refresh', 15) + ' 再做一次</button>'
        : '<button class="btn btn-primary" data-action="submit-objective" data-id="' + p.id + '"' + (selected.length ? '' : ' disabled') + '>提交答案</button>') +
      '</div>' + (result ? objectiveResultHtml(p, result, selected) : '') +
      '</div>' +
      '<aside class="home-side">' +
      '<div class="card card-pad"><h3>题目信息</h3><div class="meta-list"><div><span>题型</span><strong>' + (p.qtype === 'multiple' ? '多选题' : p.qtype === 'judge' ? '判断题' : '单选题') + '</strong></div><div><span>难度</span><strong>' + TZ.esc(p.difficulty) + '</strong></div><div><span>来源</span><strong>' + TZ.esc(p.source) + '</strong></div><div><span>周榜统计</span><strong>不计入</strong></div></div></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>提交记录</h3><span class="muted">' + subs.length + ' 次</span></div>' + submissionTable(subs, '提交答案后，记录会出现在这里。') + '</div>' +
      '</aside></div>',
      'problems'
    );
  };

  C.shortAnswerDetail = function (p) {
    const user = TZ.currentUser();
    const result = TZ.state.shortResults[p.id];
    const subs = TZ.getSubmissions(user.id, p.id);
    const draft = Object.prototype.hasOwnProperty.call(TZ.state.drafts, p.id) ? TZ.state.drafts[p.id] : '';
    const grading = !!(result && result.grading);
    const done = !!(result && result.done);
    const failed = !!(result && result.failed);
    let resultHtml = '';
    if (grading) {
      resultHtml = '<div class="judge-result judging"><span class="loading-emoji" aria-hidden="true">😽</span><div><strong>AI 判分中…</strong><p>正在对照参考答案与评分要点，请稍候</p></div></div>';
    } else if (failed) {
      resultHtml = '<div class="failure-card"><div class="failure-icon">' + TZ.icon('alert', 22) + '</div><div><strong>判分失败</strong><p>' + TZ.esc(result.message) + '</p>' +
        '<span class="hint">本次作答未记录得分，可保留答案直接重新提交。</span></div></div>';
    } else if (done) {
      resultHtml = '<div class="judge-result ' + (result.passed ? 'ac' : 'fail') + ' short-result">' +
        '<div class="short-score-row"><div class="short-score ' + (result.passed ? 'pass' : 'fail') + '"><strong>' + result.score + '</strong><span>/ 100</span></div>' +
        '<div class="short-score-copy"><span class="' + (result.passed ? 'badge badge-green' : 'badge badge-red') + '">' + (result.passed ? '通过' : '未通过') + '</span>' +
        '<strong>' + (result.passed ? '回答达到通过线' : '低于 60 分，已加入错题本') + '</strong><p>满分 100 分，60 分及格；AI 判分结果仅供参考。</p></div></div>' +
        '<div class="explanation"><strong>AI 评语</strong><p>' + TZ.esc(result.feedback) + '</p></div>' +
        '<div class="short-reference"><strong>参考答案</strong><p>' + TZ.esc(p.referenceAnswer) + '</p></div>' +
        '<div class="short-rubric"><strong>评分要点</strong><ul>' + (p.scoringRubric || []).map(function (r) { return '<li>' + TZ.esc(r) + '</li>'; }).join('') + '</ul></div>' +
        '<div class="result-note">' + (result.passed ? '简答题不计入周榜统计。' : '简答题不计入周榜统计；可修改答案后重新提交。') + '</div>' +
        '</div>';
    }
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(p.title), '简答题由 AI 判分，返回 0–100 分与评语；低于 60 分进入错题本。',
        '<a class="btn btn-outline" href="#/problems">' + TZ.icon('chevronLeft', 15) + ' 返回题库</a>',
        '<a href="#/problems">题库</a> <span class="crumb-sep">/</span> ' + TZ.esc(p.title)) +
      '<div class="problem-layout objective-layout">' +
      '<div class="card card-pad short-answer-card"><div class="problem-badges">' + TZ.typeBadge(p.type) + TZ.badge('AI 判分', 'indigo') + TZ.difficultyBadge(p.difficulty) + tagsHtml(p.tags) + '</div>' +
      '<div class="question-text">' + TZ.esc(p.title) + '</div>' +
      '<div class="question-tip">' + TZ.icon('info', 15) + ' 满分 ' + (p.maxScore || 100) + ' 分，60 分及格；判分结果不计入榜单。</div>' +
      '<div class="field"><label class="label">你的答案</label><textarea id="short-answer-editor" class="textarea short-textarea" data-draft-key="' + p.id + '" rows="9" placeholder="用文字说明你的理解，建议覆盖评分要点中的关键概念。"' + (grading ? ' disabled' : '') + '>' + TZ.esc(draft) + '</textarea>' +
      '<p class="hint">演示提示：答案过短或缺少关键术语会得到低分；填写「模拟失败」可演示判分失败。</p></div>' +
      '<div class="question-actions">' +
      (done || failed
        ? '<button class="btn btn-outline" data-action="retry-short" data-id="' + p.id + '">' + TZ.icon('refresh', 15) + ' 重新作答</button>'
        : '<button class="btn btn-primary" data-action="submit-short" data-id="' + p.id + '"' + (grading ? ' disabled' : '') + '>' + (grading ? '判分中…' : '提交作答') + '</button>') +
      '</div>' + resultHtml +
      '</div>' +
      '<aside class="home-side">' +
      '<div class="card card-pad"><h3>题目信息</h3><div class="meta-list"><div><span>题型</span><strong>简答题</strong></div><div><span>满分</span><strong>' + (p.maxScore || 100) + ' 分</strong></div><div><span>及格线</span><strong>60 分</strong></div><div><span>难度</span><strong>' + TZ.esc(p.difficulty) + '</strong></div><div><span>周榜统计</span><strong>不计入</strong></div></div></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>作答记录</h3><span class="muted">' + subs.length + ' 次</span></div>' + submissionTable(subs, '提交答案后，记录会出现在这里。') + '</div>' +
      '</aside></div>',
      'problems'
    );
  };

  C.externalDetail = function (p) {
    const user = TZ.currentUser();
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(p.title), '外部平台题目，站内仅提供链接与完成标记。',
        '<a class="btn btn-outline" href="#/problems">' + TZ.icon('chevronLeft', 15) + ' 返回题库</a>',
        '<a href="#/problems">题库</a> <span class="crumb-sep">/</span> ' + TZ.esc(p.title)) +
      '<div class="external-detail card card-pad">' +
      '<div class="external-detail-head"><div class="external-platform-icon">' + TZ.icon('link', 28) + '</div><div><div class="problem-badges"><span class="badge badge-cyan">' + TZ.esc(p.platform) + '</span>' + TZ.difficultyBadge(p.difficulty) + tagsHtml(p.tags) + '</div><h2>' + TZ.esc(p.title) + '</h2><p class="muted">来源：' + TZ.esc(p.source) + ' · 外链题不产生站内提交记录</p></div></div>' +
      '<div class="external-actions"><a class="btn btn-primary btn-lg" href="' + p.url + '" target="_blank" rel="noopener">' + TZ.icon('external', 17) + ' 前往 ' + TZ.esc(p.platform) + ' 完成题目</a></div>' +
      '<div class="external-completion"><h3>完成状态</h3>' + externalCompletionHtml(p, user) + '<p class="hint">Codeforces 题目在绑定账号后可模拟自动识别；其他平台需手动标记。完成状态不计入周榜。</p></div>' +
      '<div class="info-strip"><span class="info-strip-icon">' + TZ.icon('info', 17) + '</span><div><strong>外链题说明</strong><p>首期仅收录经典外链题，站内不复制题面、不保存外部提交记录。</p></div></div>' +
      '</div>',
      'problems'
    );
  };

  /* ---------- 周榜 ---------- */
  C.leaderboard = function () {
    const user = TZ.currentUser();
    const mode = TZ.state.leaderboardMode === 'all' ? 'all' : 'week';
    const isAll = mode === 'all';
    const all = TZ.getLeaderboard(mode);
    const me = all.find(function (r) { return r.userId === user.id; });
    const shown = TZ.state.showAllLeaderboard ? all : all.slice(0, 20);
    const meInShown = shown.some(function (r) { return r.userId === user.id; });
    const rankClass = function (rank) {
      return rank <= 3 ? 'rank rank-' + rank : 'rank rank-plain';
    };
    const rowHtml = function (r) {
      return '<tr class="' + (r.userId === user.id ? 'me-row' : '') + '">' +
        '<td><span class="' + rankClass(r.rank) + '">' + r.rank + '</span></td>' +
        '<td><div class="lb-user">' + TZ.avatar(r.user, 34) + '<div class="lb-user-info"><strong>' + TZ.esc(r.user.name) + (r.userId === user.id ? '<em class="me-tag">我</em>' : '') + '</strong><span>' + TZ.esc(r.user.school || '未填写学校') + '</span></div></div></td>' +
        '<td class="num">' + r.inSystem + '</td><td class="num">' + r.cf + '</td><td class="num">' + r.manual + '</td>' +
        '<td class="num"><strong class="total-num">' + r.total + '</strong></td>' +
        '<td><div class="source-pills">' + TZ.sourcePills(r) + '</div></td></tr>';
    };
    const infoHtml = isAll
      ? '全部历史累计（自平台上线起，不设赛季重置）；站内每个 OJ 题目按历史首次 AC 计一次；CF 取账号历史总过题数；手动申报按历史各周累计并标记为未校验；平台内去重，不做跨平台去重。总榜只读。'
      : '北京时间自然周（周一 00:00 至周日 24:00）；站内仅统计 OJ 首次 AC；CF 每小时模拟同步；其他平台手动申报并标记为未校验；平台内去重，不做跨平台去重。';
    const rankTitle = (TZ.state.showAllLeaderboard ? (isAll ? '总榜全部排名' : '全部排名') : (isAll ? '总榜前 20 名' : '前 20 名'));
    return TZ.layoutC(
      TZ.pageHeader(isAll ? '跨平台总榜' : '跨平台周榜',
        isAll ? '全部历史累计 · 站内历史首次 AC + CF 历史过题 + 手动申报累计' : TZ.WEEK_LABEL + ' · 站内首次 AC + CF 模拟同步 + 其他平台手动申报',
        (isAll ? '' : '<button class="btn btn-outline" data-action="open-manual">' + TZ.icon('edit', 15) + ' 手动申报</button>') +
        '<button class="btn btn-soft" data-action="refresh-cf">' + TZ.icon('refresh', 15) + ' 刷新 CF 数据</button>') +
      '<div class="lb-page-tabs">' + lbTabsHtml(mode, 'leaderboard-tab') + '</div>' +
      '<div class="info-strip"><span class="info-strip-icon">' + TZ.icon('info', 17) + '</span><div><strong>统计口径</strong><p>' + infoHtml + '</p></div></div>' +
      (me ? '<div class="my-rank-card"><div class="my-rank-left"><div class="my-rank-badge">我的排名</div><div class="my-rank-number">#' + me.rank + '</div><div class="my-rank-sub">' + (isAll ? '全部历史累计' : '共 ' + all.length + ' 位选手参与统计') + '</div></div>' +
        '<div class="my-rank-stats"><div><span>' + (isAll ? '站内累计' : '站内过题') + '</span><strong>' + me.inSystem + '</strong><em>' + (isAll ? '历史首次 AC' : '首次 AC') + '</em></div><div><span>' + (isAll ? 'CF 累计' : 'CF 过题') + '</span><strong>' + me.cf + '</strong><em>' + (isAll ? '历史同步' : '已绑定') + '</em></div><div><span>' + (isAll ? '手动累计' : '手动申报') + '</span><strong>' + me.manual + '</strong><em>' + (isAll ? '历史申报累计' : '未校验') + '</em></div><div class="total"><span>总分</span><strong>' + me.total + '</strong><em>' + (isAll ? '三项累计' : '平台内去重') + '</em></div></div>' +
        '<div class="my-rank-actions">' + (isAll ? '' : '<button class="btn btn-primary btn-sm" data-action="open-manual">申报本周成绩</button>') + '<button class="btn btn-outline btn-sm" data-action="open-cf">' + (user.cfHandle ? '管理 CF 绑定' : '绑定 CF') + '</button></div></div>'
        : '') +
      '<div class="section"><div class="section-head"><div><h2 class="section-title">' + rankTitle + '</h2><div class="section-sub">' + (isAll ? '总分相同则按站内累计数降序；仍相同则并列' : '总分相同则按站内 AC 数降序；仍相同则并列') + '</div></div>' +
      '<button class="btn btn-ghost btn-sm" data-action="toggle-leaderboard-all">' + (TZ.state.showAllLeaderboard ? '只看前 20' : '查看全部') + '</button></div>' +
      '<div class="card"><div class="table-wrap"><table class="table leaderboard-table"><thead><tr><th>排名</th><th>选手</th><th>' + (isAll ? '站内累计' : '站内') + '</th><th>' + (isAll ? 'CF 累计' : 'CF') + '</th><th>' + (isAll ? '手动累计' : '手动') + '</th><th>总分</th><th>数据来源</th></tr></thead><tbody>' +
      shown.map(rowHtml).join('') +
      (!meInShown && me ? '<tr class="lb-separator"><td colspan="7">我的排名</td></tr>' + rowHtml(me) : '') +
      '</tbody></table></div></div>' +
      '<div class="lb-legend">' + (isAll
        ? '<span><i class="dot dot-system"></i> 站内累计</span><span><i class="dot dot-cf"></i> CF 历史同步</span><span><i class="dot dot-manual"></i> 手动申报累计（未校验）</span>'
        : '<span><i class="dot dot-system"></i> 站内实时</span><span><i class="dot dot-cf"></i> CF 模拟同步</span><span><i class="dot dot-manual"></i> 手动申报（未校验）</span>') +
      '<span class="lb-sync">最近同步：' + TZ.fmtDate(TZ.db.lastCfSync) + '</span></div>' +
      '</div>',
      'leaderboard'
    );
  };

  C.openManualModal = function () {
    const user = TZ.currentUser();
    const existing = TZ.getManualBreakdown(user.id);
    const first = existing[0];
    const platforms = ['牛客', '洛谷', 'AtCoder'];
    const body = '<form data-form="manual" class="form-stack">' +
      '<div class="field"><label class="label">平台</label><select class="select" name="platform">' + platforms.map(function (p) {
        return '<option value="' + p + '"' + (first && first.platform === p ? ' selected' : '') + '>' + p + '</option>';
      }).join('') + '</select></div>' +
      '<div class="field"><label class="label">平台账号</label><input class="input" name="handle" value="' + TZ.esc(first ? first.handle : '') + '" placeholder="例如 demo_nk" /></div>' +
      '<div class="field"><label class="label">本周过题数</label><input class="input" type="number" name="count" min="0" max="999" value="' + (first ? first.count : 0) + '" /></div>' +
      '<div class="hint">手动申报数据会标记为「未校验」，仅用于本周榜单统计；再次申报同一平台会覆盖原记录。</div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存申报</button></div>' +
      '</form>';
    TZ.modal({ title: '手动申报本周过题数', body: body });
  };

  C.openCfModal = function () {
    const user = TZ.currentUser();
    const presets = Object.keys(TZ.CF_PRESET);
    const body = '<form data-form="cf" class="form-stack">' +
      '<div class="field"><label class="label">Codeforces Handle</label><input class="input" name="handle" list="cf-handles" value="' + TZ.esc(user.cfHandle || '') + '" placeholder="输入 Handle" /><datalist id="cf-handles">' + presets.map(function (h) { return '<option value="' + h + '">'; }).join('') + '</datalist></div>' +
      '<div class="hint">演示白名单：' + presets.join(' / ') + '。白名单内视为校验通过，其他 Handle 会返回失败；每个 Handle 只能绑定一个账号。</div>' +
      '<div class="cf-preview"><span>校验通过后，将同步本周 CF 过题数并参与周榜统计。</span></div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存绑定</button></div>' +
      '</form>';
    TZ.modal({ title: user.cfHandle ? '管理 Codeforces 绑定' : '绑定 Codeforces', body: body });
  };

  C.openEditProfile = function () {
    const user = TZ.currentUser();
    const body = '<form data-form="profile" class="form-stack">' +
      '<div class="field"><label class="label">昵称</label><input class="input" name="name" value="' + TZ.esc(user.name) + '" maxlength="20" /></div>' +
      '<div class="field"><label class="label">学校</label><input class="input" name="school" value="' + TZ.esc(user.school || '') + '" maxlength="40" placeholder="例如 示例大学" /></div>' +
      '<div class="field"><label class="label">邮箱</label><input class="input" name="email" type="email" value="' + TZ.esc(user.email || '') + '" placeholder="name@example.com" /></div>' +
      '<div class="hint">昵称、头像与学校会展示在榜单与对战房间中。头像首期自动取昵称首字。</div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存</button></div>' +
      '</form>';
    TZ.modal({ title: '编辑个人资料', body: body });
  };

  C.openLlmCredentialModal = function () {
    const user = TZ.currentUser();
    const cred = TZ.getLlmCredential(user.id);
    const body = '<form data-form="llm-credential" class="form-stack">' +
      '<div class="field"><label class="label">LLM 服务商</label><select class="select" name="provider">' + TZ.LLM_PROVIDERS.map(function (p) {
        return '<option' + (cred && cred.provider === p ? ' selected' : '') + '>' + p + '</option>';
      }).join('') + '</select></div>' +
      '<div class="field"><label class="label">API Key</label><input class="input" name="key" type="password" autocomplete="new-password" placeholder="' + (cred ? '已配置 ' + TZ.maskLlmKey(cred.key) + '，留空保持不变' : '粘贴服务商提供的 API Key') + '" />' +
      '<p class="hint">保存时会模拟校验可用性；以 invalid / expired 开头的 Key 会演示校验失败。Key 仅保存在服务端（原型为本机演示数据），页面只展示脱敏尾号。</p></div>' +
      '<div class="field"><label class="label">模型（选填）</label><input class="input" name="model" value="' + TZ.esc(cred ? cred.model || '' : '') + '" placeholder="留空使用服务商默认模型" /></div>' +
      '<div class="hint">该凭证仅用于 LLM 造数据；简答题 AI 判分仍由平台提供，不需要此凭证。</div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">' + (cred ? '保存凭证' : '校验并保存') + '</button></div>' +
      '</form>';
    TZ.modal({ title: cred ? '🤖 更换 LLM 凭证' : '🤖 配置 LLM 凭证', body: body });
  };

  C.openAccountSwitch = function () {
    const user = TZ.currentUser();
    const ids = ['u_demo', 'u_02', 'u_03', 'u_04', 'u_05'];
    const body = '<div class="account-list">' + ids.map(function (id) {
      const u = TZ.getUser(id);
      if (!u) return '';
      return '<button class="account-item ' + (u.id === user.id ? 'active' : '') + '" data-action="use-account" data-id="' + u.id + '">' +
        TZ.avatar(u, 38) + '<span class="account-item-info"><strong>' + TZ.esc(u.name) + '</strong><span>' + TZ.esc(u.school || '未填写学校') + ' · ' + (u.cfHandle || '未绑定 CF') + '</span></span>' +
        (u.id === user.id ? TZ.badge('当前', 'indigo') : TZ.icon('arrowRight', 16)) + '</button>';
    }).join('') + '</div><p class="hint">演示功能：切换账号用于体验不同角色，例如创建房间后切换对手加入、查看不同用户的周榜数据。</p>';
    TZ.modal({ title: '切换演示账号', body: body });
  };

  /* ---------- 对战 ---------- */
  C.battle = function () {
    const user = TZ.currentUser();
    const recent = TZ.db.battles.filter(function (b) { return b.userId === user.id; }).slice(0, 3);
    const resultText = { win: '胜', loss: '负', draw: '平' };
    return TZ.layoutC(
      TZ.pageHeader('对战大厅', '邀请好友，30 分钟 1v1，先 AC 者胜。', '<a class="btn btn-outline" href="#/battle-records">' + TZ.icon('history', 15) + ' 我的对战记录</a>') +
      '<div class="grid grid-2">' +
      '<div class="card card-pad battle-entry"><div class="card-icon indigo">' + TZ.emojiIcon('😼') + '</div><h3>创建房间</h3><p class="muted">生成房间码，邀请对手加入。题目从题库按难度与知识点随机抽取。</p>' +
      '<div class="form-grid"><div class="field"><label class="label">难度</label><select id="room-difficulty" class="select"><option value="all">全部难度</option><option value="入门">入门</option><option value="基础">基础</option><option value="进阶">进阶</option></select></div>' +
      '<div class="field"><label class="label">知识点</label><select id="room-topic" class="select"><option value="all">全部知识点</option><option value="动态规划">动态规划</option><option value="图论">图论</option><option value="C++ 与 STL 基础">C++ 与 STL 基础</option></select></div></div>' +
      '<button class="btn btn-primary btn-block" data-action="create-room">创建房间</button></div>' +
      '<div class="card card-pad battle-entry"><div class="card-icon cyan">' + TZ.emojiIcon('😼') + '</div><h3>加入房间</h3><p class="muted">输入好友分享的房间码，加入后等待房主开始。</p>' +
      '<div class="field"><label class="label">房间码</label><input class="input" id="join-code" placeholder="例如 TZ-8F2K" maxlength="10" /></div>' +
      '<button class="btn btn-soft btn-block" data-action="join-room">加入房间</button></div></div>' +
      '<div class="grid grid-2 section"><div class="card card-pad"><h3>对战规则</h3><ul class="rule-list">' +
      '<li>' + TZ.icon('clock', 16) + '<span>单局 30 分钟，倒计时结束双方都未 AC 则平局。</span></li>' +
      '<li>' + TZ.icon('zap', 16) + '<span>先 AC 者获胜，只看首次通过。</span></li>' +
      '<li>' + TZ.icon('flag', 16) + '<span>主动退出或断线判负。</span></li>' +
      '<li>' + TZ.icon('shield', 16) + '<span>首期不设积分、不设随机匹配、观战与回放。</span></li></ul></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>最近对战</h3><a class="link" href="#/battle-records">全部</a></div>' +
      (recent.length ? '<div class="battle-recent-list">' + recent.map(function (b) {
        return '<div class="battle-recent"><span class="battle-result-dot ' + b.result + '">' + resultText[b.result] + '</span><div class="battle-recent-info"><strong>vs ' + TZ.esc(b.opponentName) + '</strong><span>' + b.room + ' · ' + TZ.fmtDate(b.startedAt) + '</span></div><span class="muted">' + b.duration + '</span></div>';
      }).join('') + '</div>' : TZ.empty('swords', '还没有对战记录', '创建房间邀请好友开始第一局。')) +
      '</div></div>',
      'battle'
    );
  };

  C.battleRoom = function (code) {
    const user = TZ.currentUser();
    const room = TZ.db.battleRoom;
    if (!room || room.code.toUpperCase() !== String(code || '').toUpperCase()) {
      return TZ.layoutC(TZ.empty('swords', '房间不存在', '房间码可能已失效，或房主已关闭房间。', '<a class="btn btn-primary" href="#/battle">返回对战大厅</a>'), 'battle');
    }
    const isCreator = room.creatorId === user.id;
    const isOpponent = room.opponentId === user.id;
    const opponentId = isCreator ? room.opponentId : room.creatorId;
    const opponent = opponentId ? TZ.getUser(opponentId) : null;
    const creator = TZ.getUser(room.creatorId);
    if (!isCreator && !isOpponent && room.status !== 'waiting') {
      return TZ.layoutC(TZ.empty('shield', '无法加入该房间', '房间已开始或已结束，请让房主重新创建。', '<a class="btn btn-primary" href="#/battle">返回大厅</a>'), 'battle');
    }
    if (!isCreator && !isOpponent && room.status === 'waiting') {
      return TZ.layoutC(
        TZ.pageHeader('加入房间', '你正在查看房间 ' + room.code + '。', '<a class="btn btn-outline" href="#/battle">返回大厅</a>') +
        '<div class="card card-pad battle-room-card"><div class="room-code-label">房间码</div><div class="room-code">' + room.code + '</div>' +
        '<p class="muted">房主：' + TZ.esc(creator ? creator.name : '未知') + ' · 等待对手加入</p>' +
        '<button class="btn btn-primary btn-lg" data-action="join-current-room" data-code="' + room.code + '">加入房间</button></div>',
        'battle'
      );
    }
    if (room.status === 'waiting') {
      return TZ.layoutC(
        TZ.pageHeader('等待对手加入', '把房间码或邀请链接发给好友。', '<a class="btn btn-outline" href="#/battle">返回大厅</a>') +
        '<div class="card card-pad battle-room-card waiting"><div class="room-code-label">房间码</div><div class="room-code">' + room.code + '</div>' +
        '<div class="room-actions"><button class="btn btn-outline" data-action="copy-room" data-code="' + room.code + '">' + TZ.icon('copy', 15) + ' 复制邀请链接</button><button class="btn btn-ghost" data-action="leave-room">关闭房间</button></div>' +
        '<div class="waiting-animation"><span></span><span></span><span></span></div><p class="waiting-text">等待对手加入…</p>' +
        '<div class="demo-controls"><span>演示控制</span><button class="btn btn-soft btn-sm" data-action="simulate-opponent">模拟对手加入</button></div></div>',
        'battle'
      );
    }
    if (room.status === 'ready') {
      return TZ.layoutC(
        TZ.pageHeader('房间已就绪', '双方已就位，房主可以开始对战。', '<a class="btn btn-outline" href="#/battle">返回大厅</a>') +
        '<div class="card card-pad battle-ready-card"><div class="versus-row">' +
        '<div class="versus-player"><span class="versus-label">房主</span>' + TZ.avatar(creator, 56) + '<strong>' + TZ.esc(creator ? creator.name : '未知') + '</strong><span>' + TZ.esc(creator ? creator.school : '') + '</span></div>' +
        '<div class="vs-badge">VS</div>' +
        '<div class="versus-player"><span class="versus-label">对手</span>' + TZ.avatar(opponent, 56) + '<strong>' + TZ.esc(opponent ? opponent.name : '待加入') + '</strong><span>' + TZ.esc(opponent ? opponent.school : '') + '</span></div></div>' +
        '<div class="ready-meta"><span>难度：' + (room.filter.difficulty === 'all' ? '全部' : room.filter.difficulty) + '</span><span>知识点：' + (room.filter.topic === 'all' ? '全部' : room.filter.topic) + '</span><span>单局 30 分钟</span></div>' +
        (isCreator ? '<button class="btn btn-primary btn-lg" data-action="start-battle">开始对战</button>' : '<p class="muted">等待房主开始对战…</p>') +
        '<button class="btn btn-ghost" data-action="leave-room">离开房间</button></div>',
        'battle'
      );
    }
    if (room.status === 'playing') {
      const p = TZ.getOjProblem(room.problemId);
      const code = TZ.state.battleDraft || TZ.CODE_TEMPLATES.cpp;
      const logs = room.logs || [];
      return TZ.layoutC(
        '<div class="battle-topbar card"><div class="battle-topbar-left"><span class="room-code-small">' + room.code + '</span>' + TZ.badge('对战中', 'amber') + '</div>' +
        '<div class="battle-timer-wrap"><span>剩余时间</span><strong id="battle-timer">30:00</strong></div>' +
        '<div class="battle-topbar-right">' + (opponent ? TZ.avatar(opponent, 28) + '<span>' + TZ.esc(opponent.name) + '</span><i class="status-dot"></i>' : '') + '</div></div>' +
        '<div class="problem-layout battle-layout">' +
        '<div class="card card-pad problem-statement"><div class="problem-badges">' + TZ.typeBadge('oj') + TZ.difficultyBadge(p ? p.difficulty : '') + tagsHtml(p ? p.tags : []) + '</div>' +
        '<div class="prose"><h3>题目描述</h3>' + (p ? p.statement : '') + '<h3>输入格式</h3>' + (p ? p.inputFormat : '') + '<h3>输出格式</h3>' + (p ? p.outputFormat : '') +
        (p && p.samples[0] ? '<h3>样例</h3><div class="sample-block"><div class="sample-grid"><div><span>输入</span><pre><code>' + TZ.esc(p.samples[0].input) + '</code></pre></div><div><span>输出</span><pre><code>' + TZ.esc(p.samples[0].output) + '</code></pre></div></div></div>' : '') + '</div></div>' +
        '<div class="problem-work"><div class="code-panel"><div class="code-toolbar"><div class="code-dots"><span></span><span></span><span></span></div><span class="code-file">battle.cpp</span></div>' +
        '<textarea id="battle-code-editor" class="code-editor" data-battle-draft="1" spellcheck="false">' + TZ.esc(code) + '</textarea>' +
        '<div class="code-actions"><span class="code-hint">先 AC 者获胜</span><button class="btn btn-primary" data-action="submit-battle">' + TZ.icon('play', 15) + ' 提交</button></div></div>' +
        (TZ.state.battleJudge ? '<div class="judge-result ' + (TZ.state.battleJudge.verdict === 'AC' ? 'ac' : 'fail') + '"><div class="judge-main"><span class="verdict-badge verdict-' + TZ.state.battleJudge.verdict.toLowerCase() + '">' + TZ.state.battleJudge.verdict + '</span><div><strong>' + TZ.esc(TZ.state.battleJudge.detail) + '</strong><p>' + TZ.state.battleJudge.time + ' · ' + TZ.state.battleJudge.memory + '</p></div></div></div>' : '') +
        '<div class="card card-pad battle-logs"><div class="section-head compact"><h3>本局记录</h3><span class="muted">' + logs.length + ' 次提交</span></div>' +
        (logs.length ? logs.map(function (l) { return '<div class="battle-log-row"><span class="muted">' + TZ.fmtDate(l.at) + '</span>' + TZ.verdictBadge(l.verdict) + '<span>' + TZ.esc(l.text) + '</span></div>'; }).join('') : '<p class="muted">还没有提交。</p>') +
        '<div class="demo-controls"><span>演示控制</span><button class="btn btn-soft btn-sm" data-action="simulate-opponent-ac">模拟对手 AC</button><button class="btn btn-ghost btn-sm" data-action="simulate-timeout">模拟时间结束</button><button class="btn btn-ghost btn-sm danger" data-action="quit-battle">认输退出</button></div></div>' +
        '</div></div>',
        'battle'
      );
    }
    const result = room.result || 'draw';
    const resultTitle = result === 'win' ? '你赢了' : result === 'loss' ? '惜败' : '平局';
    const resultDesc = result === 'win' ? '你率先通过本题，拿下本局。' : result === 'loss' ? '对手率先 AC，或本局因退出而结束。' : '30 分钟内双方都未 AC，本局平局。';
    return TZ.layoutC(
      '<div class="battle-result-card ' + result + '"><div class="battle-result-icon">' + TZ.icon(result === 'win' ? 'trophy' : result === 'loss' ? 'flag' : 'swords', 40) + '</div>' +
      '<h1>' + resultTitle + '</h1><p>' + resultDesc + '</p>' +
      '<div class="battle-result-meta"><span>房间 ' + room.code + '</span><span>对手 ' + TZ.esc(opponent ? opponent.name : '未知') + '</span><span>题目 ' + TZ.esc(room.problemId ? (TZ.getOjProblem(room.problemId) || {}).title || '—' : '—') + '</span></div>' +
      '<div class="battle-result-actions"><a class="btn btn-primary" href="#/battle">返回大厅</a><a class="btn btn-outline" href="#/battle-records">查看对战记录</a></div></div>',
      'battle'
    );
  };

  C.finishBattle = function (result) {
    const room = TZ.db.battleRoom;
    const user = TZ.currentUser();
    if (!room || room.status !== 'playing' || !user) return;
    const opponentId = room.creatorId === user.id ? room.opponentId : room.creatorId;
    const opponent = opponentId ? TZ.getUser(opponentId) : null;
    const started = room.startedAt ? new Date(room.startedAt).getTime() : Date.now() - 1000;
    const seconds = Math.max(1, Math.floor((Date.now() - started) / 1000));
    const duration = String(Math.floor(seconds / 60)).padStart(2, '0') + ':' + String(seconds % 60).padStart(2, '0');
    room.status = 'finished';
    room.result = result;
    room.endedAt = new Date().toISOString();
    TZ.db.battles.unshift({
      id: TZ.uid('battle'),
      room: room.code,
      userId: user.id,
      opponentId: opponentId,
      opponentName: opponent ? opponent.name : '对手',
      problemId: room.problemId,
      result: result,
      startedAt: room.startedAt,
      endedAt: room.endedAt,
      duration: duration
    });
    if (TZ.battleTimer) clearInterval(TZ.battleTimer);
    TZ.save();
    TZ.rerender();
  };

  C.openBattleRecord = function (id) {
    const b = TZ.db.battles.find(function (x) { return x.id === id; });
    if (!b) return;
    const p = TZ.getOjProblem(b.problemId);
    const resultText = { win: '胜利', loss: '失败', draw: '平局' };
    const body = '<div class="record-detail"><div class="record-result-badge ' + b.result + '">' + resultText[b.result] + '</div>' +
      '<div class="meta-list"><div><span>房间</span><strong>' + b.room + '</strong></div><div><span>对手</span><strong>' + TZ.esc(b.opponentName) + '</strong></div>' +
      '<div><span>题目</span><strong>' + TZ.esc(p ? p.title : '—') + '</strong></div><div><span>开始时间</span><strong>' + TZ.fmtDate(b.startedAt) + '</strong></div>' +
      '<div><span>结束时间</span><strong>' + TZ.fmtDate(b.endedAt) + '</strong></div><div><span>用时</span><strong>' + b.duration + '</strong></div></div>' +
      '<p class="hint">首期不提供回放与观战，此处仅展示本局结果。</p></div>';
    TZ.modal({ title: '对战详情', body: body, footer: '<button class="btn btn-outline" data-action="close-modal">关闭</button>' });
  };

  /* ---------- 造数据 ---------- */
  function varRow(v) {
    v = v || { name: '', type: 'int', min: 1, max: 100 };
    return '<div class="var-row">' +
      '<input class="input" name="var-name" placeholder="变量名，如 n" value="' + TZ.esc(v.name) + '" />' +
      '<select class="select" name="var-type"><option value="int"' + (v.type === 'int' ? ' selected' : '') + '>int</option><option value="long long"' + (v.type === 'long long' ? ' selected' : '') + '>long long</option><option value="double"' + (v.type === 'double' ? ' selected' : '') + '>double</option></select>' +
      '<input class="input" name="var-min" type="number" placeholder="最小值" value="' + TZ.esc(v.min) + '" />' +
      '<input class="input" name="var-max" type="number" placeholder="最大值" value="' + TZ.esc(v.max) + '" />' +
      '<button type="button" class="icon-btn danger" data-action="remove-var" title="删除变量">' + TZ.icon('trash', 16) + '</button>' +
      '</div>';
  }
  TZ.varRow = varRow;

  C.datagen = function () {
    const user = TZ.currentUser();
    const cred = TZ.getLlmCredential(user.id);
    const canUseLlm = !!(cred && cred.status === 'valid');
    let mode = TZ.state.datagenMode === 'llm' ? 'llm' : TZ.state.datagenMode === 'llm_direct' ? 'llm_direct' : 'random';
    if (!canUseLlm && mode !== 'random') mode = 'random';
    TZ.state.datagenMode = mode;
    const usesLlm = mode !== 'random';
    const modeHints = {
      random: '按数据范围随机生成，稳定且耗时更短。',
      llm: 'LLM 会结合题面、数据范围与特殊要求构造生成器；构造器代码不展示。',
      llm_direct: 'LLM 直接产出约 10 组输入数据，平台逐组校验格式与范围后再运行 std；不生成构造器。'
    };
    const std = '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // 读取输入，输出标准答案\n    return 0;\n}';
    return TZ.layoutC(
      TZ.pageHeader('AI 造数据', '随机生成、LLM 构造或由 LLM 直接生成测试数据，std 负责产出答案，下载后自行校验。', '<a class="btn btn-outline" href="#/datagen/tasks">' + TZ.emojiIcon('🤖') + ' 我的任务</a>') +
      '<div class="info-strip"><span class="info-strip-icon">' + TZ.emojiIcon('🤖') + '</span><div><strong>能力边界</strong><p>支持随机生成、LLM 构造与 LLM 直接生成数据三种方式；三种方式都由你提供 std 并产出答案。LLM 直接生成的数据会逐组校验格式与范围。构造器代码不展示；约 10 组；不支持 SPJ；失败即终止并给出原因；文件保留 7 天。</p></div></div>' +
      (!canUseLlm ? '<div class="info-strip warning-strip"><span class="info-strip-icon">' + TZ.emojiIcon('😼') + '</span><div><strong>' + (cred ? 'LLM 凭证已失效' : '尚未配置有效的 LLM 凭证') + '</strong><p>' + (cred ? '最近一次校验未通过，两种 LLM 造数据模式已禁用。请更新 API Key 后重试。' : '两种 LLM 造数据模式已禁用。请先在个人中心配置服务商与 API Key；随机生成与简答题 AI 判分不受影响。') + '</p><a class="btn btn-soft btn-sm" href="#/profile">去个人中心' + (cred ? '更新凭证' : '配置') + '</a></div></div>' : '') +
      '<form data-form="datagen" id="datagen-form" class="datagen-layout">' +
      '<div class="card card-pad"><div class="section-head compact"><h3>基础信息</h3><span class="muted">任务提交后异步生成</span></div>' +
      '<div class="form-grid"><div class="field full"><label class="label">生成方式</label>' +
      '<div class="mode-switch mode-switch-3" data-datagen-mode-group>' +
      '<label class="mode-option"><input type="radio" name="mode" value="random"' + (mode === 'random' ? ' checked' : '') + ' /><span class="mode-icon">' + TZ.emojiIcon('🤖') + '</span><span class="mode-copy"><strong>随机生成</strong><em>按数据范围随机产出，稳定快速</em></span></label>' +
      '<label class="mode-option' + (canUseLlm ? '' : ' mode-disabled') + '"><input type="radio" name="mode" value="llm"' + (mode === 'llm' ? ' checked' : '') + (canUseLlm ? '' : ' disabled') + ' /><span class="mode-icon">' + TZ.emojiIcon('🤖') + '</span><span class="mode-copy"><strong>LLM 构造</strong><em>由大模型生成数据构造器，覆盖边界场景</em></span></label>' +
      '<label class="mode-option' + (canUseLlm ? '' : ' mode-disabled') + '"><input type="radio" name="mode" value="llm_direct"' + (mode === 'llm_direct' ? ' checked' : '') + (canUseLlm ? '' : ' disabled') + ' /><span class="mode-icon">' + TZ.emojiIcon('🤖') + '</span><span class="mode-copy"><strong>LLM 直接生成数据</strong><em>模型直接产出输入数据，平台校验后运行 std</em></span></label>' +
      '</div><p class="hint" id="datagen-mode-hint">' + modeHints[mode] + '</p></div>' +
      '<div class="field full"><label class="label">任务名称</label><input class="input" name="title" placeholder="例如 最大子段和 · 测试数据" value="最大子段和 · 测试数据" /></div>' +
      '<div class="field full"><label class="label">题目描述</label><textarea class="textarea" name="statement" rows="3" placeholder="简要描述题目">给定长度为 n 的整数序列，求最大子段和。</textarea></div>' +
      '<div class="field"><label class="label">输入格式</label><textarea class="textarea" name="inputFormat" rows="2" placeholder="描述输入格式">第一行 n，第二行 n 个整数。</textarea></div>' +
      '<div class="field"><label class="label">输出格式</label><textarea class="textarea" name="outputFormat" rows="2" placeholder="描述输出格式">一个整数，表示最大子段和。</textarea></div>' +
      '<div class="field full llm-only" id="llm-requirements-field"' + (usesLlm ? '' : ' hidden') + '><label class="label">特殊构造要求（选填）</label><textarea class="textarea" name="specialRequirements" rows="3" placeholder="例如：包含 1 组全相同元素、1 组链状图、1 组卡精度数据">' + '</textarea><p class="hint">作为 LLM 的上下文，不承诺逐条命中；失败时任务终止并返回原因。</p></div></div></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>数据范围</h3><button type="button" class="btn btn-soft btn-sm" data-action="add-var">' + TZ.icon('plus', 14) + ' 添加变量</button></div>' +
      '<div class="var-head"><span>变量名</span><span>类型</span><span>最小值</span><span>最大值</span><span></span></div>' +
      '<div id="var-rows">' + varRow({ name: 'n', type: 'int', min: 1, max: 20 }) + '</div>' +
      '<div class="form-grid section"><div class="field"><label class="label">数据组数</label><input class="input" name="groupCount" type="number" min="1" max="30" value="10" /></div>' +
      '<div class="field"><label class="label">时间限制</label><input class="input" name="timeLimit" value="1s" /></div>' +
      '<div class="field"><label class="label">内存限制</label><input class="input" name="memoryLimit" value="256MB" /></div></div></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>标准程序（std）</h3><span class="muted">演示中不会真实执行</span></div>' +
      '<textarea class="textarea code-textarea" name="stdCode" id="datagen-std" rows="12" spellcheck="false">' + TZ.esc(std) + '</textarea>' +
      '<p class="hint">失败演示：在 std 中加入 <code>// TLE</code>；LLM 模式还可在特殊要求中填写「模拟失败」。</p></div>' +
      '<div class="datagen-submit"><div class="datagen-summary">' + TZ.emojiIcon('🤖') + '<span>将生成约 <strong>10</strong> 组数据，失败即终止；LLM 直接生成模式的数据会先逐组校验。选择 LLM 模式时，题面与特殊要求会发送给所选服务商' + (cred ? '（当前：<strong>' + TZ.esc(cred.provider) + '</strong>）' : '') + '。</span></div><button class="btn btn-primary btn-lg" id="datagen-submit-btn" type="submit"' + (!canUseLlm && mode !== 'random' ? ' disabled' : '') + '>' + TZ.emojiIcon('🤖') + ' 提交生成任务</button></div>' +
      '</form>',
      'datagen'
    );
  };

  C.datagenTasks = function () {
    const user = TZ.currentUser();
    const tasks = TZ.db.datagenTasks.filter(function (t) { return t.userId === user.id; });
    return TZ.layoutC(
      TZ.pageHeader('造数据任务', '查看生成状态、失败原因与下载数据。', '<a class="btn btn-primary" href="#/datagen">' + TZ.emojiIcon('🤖') + ' 新建任务</a>') +
      (tasks.length ? '<div class="card"><div class="table-wrap"><table class="table"><thead><tr><th>任务</th><th>生成方式</th><th>状态</th><th>组数</th><th>创建时间</th><th>操作</th></tr></thead><tbody>' +
        tasks.map(function (t) {
          return '<tr><td><a class="table-title" href="#/datagen/task/' + t.id + '">' + TZ.esc(t.title) + '</a></td><td>' + TZ.genModeBadge(t.mode) + '</td><td>' + TZ.statusBadge(t.status) + '</td><td class="num">' + t.groupCount + '</td><td class="muted">' + TZ.fmtDate(t.createdAt) + '</td>' +
            '<td class="row-actions"><a class="btn btn-soft btn-sm" href="#/datagen/task/' + t.id + '">查看</a>' +
            (t.status === 'success' ? '<button class="btn btn-ghost btn-sm" data-action="download-all" data-id="' + t.id + '">' + TZ.icon('download', 14) + ' 下载</button>' : '') + '</td></tr>';
        }).join('') + '</tbody></table></div></div>'
        : TZ.empty('wand', '还没有造数据任务', '提交一个任务，生成你的第一组测试数据。', '<a class="btn btn-primary" href="#/datagen">🤖 新建任务</a>')),
      'datagen'
    );
  };

  C.datagenTask = function (id) {
    const task = TZ.db.datagenTasks.find(function (t) { return t.id === id; });
    if (!task) return TZ.layoutC(TZ.empty('wand', '任务不存在', '该任务可能已被清理。', '<a class="btn btn-primary" href="#/datagen/tasks">返回任务列表</a>'), 'datagen');
    return TZ.layoutC(
      TZ.pageHeader(TZ.esc(task.title), '任务编号 ' + task.id + ' · 创建于 ' + TZ.fmtDate(task.createdAt),
        '<a class="btn btn-outline" href="#/datagen/tasks">' + TZ.icon('chevronLeft', 15) + ' 🤖 返回列表</a>' +
        (task.status === 'success' ? '<button class="btn btn-primary" data-action="download-all" data-id="' + task.id + '">' + TZ.icon('download', 15) + ' 下载全部</button>' : ''),
        '<a href="#/datagen/tasks">造数据任务</a> <span class="crumb-sep">/</span> ' + TZ.esc(task.title)) +
      (task.status === 'generating' ? '<div class="card card-pad running-card"><span class="loading-emoji" aria-hidden="true">🤖</span><div><strong>LLM 正在构造数据生成器…</strong><p class="muted">模型正在结合题面、数据范围与特殊要求编写构造器，完成后自动进入生成阶段。</p></div></div>' : '') +
      (task.status === 'llm_data' ? '<div class="card card-pad running-card"><span class="loading-emoji" aria-hidden="true">🤖</span><div><strong>LLM 正在直接生成测试数据…</strong><p class="muted">模型直接产出输入数据，完成后平台会逐组校验格式与范围，再运行 std 生成输出。</p></div></div>' : '') +
      (task.status === 'running' ? '<div class="card card-pad running-card"><span class="loading-emoji" aria-hidden="true">🤖</span><div><strong>' + (task.mode === 'llm' ? '数据构造器已就绪，正在生成数据…' : task.mode === 'llm_direct' ? '数据已通过校验，正在运行 std 生成输出…' : '正在生成数据…') + '</strong><p class="muted">页面会自动刷新结果，请稍候。</p></div></div>' : '') +
      (task.status === 'failed' ? '<div class="failure-card"><div class="failure-icon">' + TZ.icon('alert', 22) + '</div><div><strong>任务失败</strong><p>' + TZ.esc(task.failureReason || '未知原因') + '</p>' +
        (task.failureStage ? '<span class="hint">失败阶段：' + TZ.esc(TZ.failureStageText(task.failureStage)) + '</span>' : '') +
        '<span class="hint">' + (task.failureStage === 'credential' ? '请先在个人中心更新 LLM 凭证，再手动重新提交；失败任务不会自动重试。' : '失败任务不会自动重试，请调整 std 或特殊要求后重新提交。') + '</span>' +
        (task.failureStage === 'credential' ? '<a class="btn btn-soft btn-sm" href="#/profile">去更新凭证</a>' : '') + '</div></div>' : '') +
      '<div class="split section"><section>' +
      (task.status === 'success' ? '<div class="card"><div class="section-head card-pad-head"><div><h3>数据校验</h3><div class="section-sub">共 ' + task.files.length + ' 组，可逐组下载</div></div></div><div class="table-wrap"><table class="table"><thead><tr><th>#</th><th>输入大小</th><th>输出大小</th><th>std 用时</th><th>状态</th><th>下载</th></tr></thead><tbody>' +
        task.files.map(function (f) {
          return '<tr><td class="num">' + f.index + '</td><td class="muted">' + f.inputSize + ' B</td><td class="muted">' + f.outputSize + ' B</td><td class="muted">' + f.stdTime + ' ms</td><td>' + TZ.badge('OK', 'green') + '</td>' +
            '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="download-group" data-id="' + task.id + '" data-index="' + f.index + '" data-kind="input">输入</button><button class="btn btn-ghost btn-sm" data-action="download-group" data-id="' + task.id + '" data-index="' + f.index + '" data-kind="output">输出</button></td></tr>';
        }).join('') + '</tbody></table></div></div>' : '') +
      '<div class="card card-pad section"><div class="section-head compact"><h3>任务参数</h3></div><div class="prose compact-prose"><p><strong>生成方式：</strong>' + TZ.genModeText(task.mode) + '</p>' +
      (task.mode !== 'random' ? '<p><strong>LLM 服务商：</strong>' + TZ.esc(task.llmProvider || '—') + '</p>' : '') +
      (task.mode !== 'random' && task.specialRequirements ? '<p><strong>特殊要求：</strong>' + TZ.esc(task.specialRequirements) + '</p>' : '') +
      '<p><strong>题目：</strong>' + TZ.esc(task.statement) + '</p><p><strong>输入：</strong>' + TZ.esc(task.inputFormat) + '</p><p><strong>输出：</strong>' + TZ.esc(task.outputFormat) + '</p>' +
      '<p><strong>数据范围：</strong>' + task.variables.map(function (v) { return v.name + ' ∈ [' + v.min + ', ' + v.max + '] (' + v.type + ')'; }).join('；') + '</p>' +
      '<p><strong>限制：</strong>' + task.timeLimit + ' / ' + task.memoryLimit + ' · <strong>组数：</strong>' + task.groupCount + '</p></div>' +
      '<details class="std-details"><summary>查看 std 代码</summary><pre><code>' + TZ.esc(task.stdCode) + '</code></pre></details>' +
      (task.mode === 'llm' ? '<p class="hint">LLM 生成的数据构造器不展示，仅保留任务状态与产出文件。</p>' : '') +
      (task.mode === 'llm_direct' ? '<p class="hint">LLM 直接生成的输入数据不展示过程；成功任务的每组数据均已通过格式与范围校验。</p>' : '') + '</div></section>' +
      '<aside class="home-side"><div class="card card-pad"><h3>生成信息</h3><div class="meta-list"><div><span>生成方式</span><strong>' + TZ.genModeBadge(task.mode) + '</strong></div>' +
      (task.mode !== 'random' ? '<div><span>LLM 服务商</span><strong>' + TZ.esc(task.llmProvider || '—') + '</strong></div>' : '') +
      (task.mode === 'llm_direct' ? '<div><span>数据校验</span><strong>' + (task.status === 'success' ? '逐组通过' : task.failureStage === 'llm_data' ? '未通过' : '进行中') + '</strong></div>' : '') +
      '<div><span>状态</span><strong>' + TZ.statusBadge(task.status) + '</strong></div><div><span>数据组数</span><strong>' + task.groupCount + '</strong></div><div><span>创建时间</span><strong>' + TZ.fmtDate(task.createdAt) + '</strong></div><div><span>完成时间</span><strong>' + (task.finishedAt ? TZ.fmtDate(task.finishedAt) : '—') + '</strong></div><div><span>保留时间</span><strong>7 天</strong></div></div></div>' +
      '<div class="card card-pad"><h3>使用说明</h3><p class="muted">下载后请自行校验数据正确性；首期不提供 SPJ。失败任务可调整 std 或特殊要求后重新提交。</p></div></aside></div>',
      'datagen'
    );
  };

  /* ---------- 学习记录 / 错题本 / 对战记录 ---------- */
  C.records = function () {
    const user = TZ.currentUser();
    const tab = TZ.state.recordsTab;
    const subs = TZ.getSubmissions(user.id);
    let body = '';
    if (tab === 'submissions') {
      body = '<div class="card"><div class="table-wrap"><table class="table"><thead><tr><th>时间</th><th>题目</th><th>类型</th><th>结果</th><th>语言</th><th>详情</th></tr></thead><tbody>' +
        (subs.length ? subs.map(function (s) {
          const p = TZ.getProblem(s.problemId);
          return '<tr><td class="muted">' + TZ.fmtDate(s.createdAt) + '</td><td><a class="table-title" href="#/problem/' + s.problemId + '">' + TZ.esc(p ? p.title : s.problemId) + '</a></td><td>' + TZ.typeBadge(s.type) + '</td><td>' + submissionResultHtml(s) + '</td><td class="muted">' + TZ.esc(s.language) + '</td><td class="muted">' + TZ.esc(s.detail) + '</td></tr>';
        }).join('') : '<tr><td colspan="6">' + TZ.empty('history', '还没有提交记录', '去题库完成第一道题吧。', '<a class="btn btn-primary" href="#/problems">去刷题</a>') + '</td></tr>') +
        '</tbody></table></div></div>';
    } else {
      body = '<div class="record-route-list">' + TZ.db.routes.map(function (r) {
        const p = TZ.routeProgress(user.id, r);
        return '<div class="card card-pad record-route"><div class="record-route-head"><div><h3>' + TZ.esc(r.title) + '</h3><span class="muted">' + TZ.esc(r.level) + ' · ' + p.done + '/' + p.total + ' 个阶段</span></div><span class="record-percent">' + p.percent + '%</span></div>' +
          TZ.progress(p.percent) +
          '<div class="record-stage-list">' + r.stages.map(function (s) {
            const done = TZ.getProgress(user.id, s.id).completed;
            return '<a class="record-stage ' + (done ? 'done' : '') + '" href="#/stage/' + s.id + '">' + (done ? TZ.icon('check', 14) : TZ.icon('clock', 14)) + ' ' + TZ.esc(s.title) + '</a>';
          }).join('') + '</div></div>';
      }).join('') + '</div>';
    }
    return TZ.layoutC(
      TZ.pageHeader('学习记录', '阶段进度、提交历史与错题复盘集中查看。',
        '<a class="btn btn-outline" href="#/wrongbook">' + TZ.icon('book', 15) + ' 错题本</a><a class="btn btn-outline" href="#/battle-records">' + TZ.icon('swords', 15) + ' 对战记录</a>') +
      '<div class="tabs"><button class="tab ' + (tab === 'progress' ? 'active' : '') + '" data-action="records-tab" data-tab="progress">🍋 学习进度</button><button class="tab ' + (tab === 'submissions' ? 'active' : '') + '" data-action="records-tab" data-tab="submissions">🍇 提交历史</button></div>' +
      '<div class="section">' + body + '</div>',
      'records'
    );
  };

  C.wrongbook = function () {
    const user = TZ.currentUser();
    const all = TZ.db.wrongBook.filter(function (w) { return w.userId === user.id; });
    const type = TZ.state.wrongType;
    const rows = all.filter(function (w) {
      if (type === 'all') return true;
      const p = TZ.getProblem(w.problemId);
      return p && p.type === type;
    });
    return TZ.layoutC(
      TZ.pageHeader('错题本', '按题目聚合最近一次错误与错误次数，可手动移除。', '<span class="badge badge-red">' + all.length + ' 道待复盘</span>') +
      '<div class="filter-card card"><div class="filter-bar"><select id="wrong-type-filter" class="select filter-select" data-filter="wrong-type"><option value="all"' + (type === 'all' ? ' selected' : '') + '>全部类型</option><option value="oj"' + (type === 'oj' ? ' selected' : '') + '>编程题</option><option value="objective"' + (type === 'objective' ? ' selected' : '') + '>客观题</option><option value="short"' + (type === 'short' ? ' selected' : '') + '>简答题</option></select><span class="muted">错题本记录不计入周榜统计</span></div></div>' +
      (rows.length ? '<div class="card"><div class="table-wrap"><table class="table"><thead><tr><th>题目</th><th>类型</th><th>最近错误</th><th>次数</th><th>最近时间</th><th>操作</th></tr></thead><tbody>' +
        rows.map(function (w) {
          const p = TZ.getProblem(w.problemId);
          const errorHtml = p && p.type === 'short' && w.latestScore != null
            ? TZ.badge(w.latestScore + ' 分', 'red')
            : TZ.verdictBadge(w.latestError);
          return '<tr><td><a class="table-title" href="#/problem/' + w.problemId + '">' + TZ.esc(p ? p.title : w.problemId) + '</a></td><td>' + TZ.typeBadge(p ? p.type : 'oj') + '</td><td>' + errorHtml + '</td><td class="num">' + w.count + '</td><td class="muted">' + TZ.fmtDate(w.lastAt) + '</td>' +
            '<td class="row-actions"><a class="btn btn-soft btn-sm" href="#/problem/' + w.problemId + '">去复盘</a><button class="btn btn-ghost btn-sm danger" data-action="wrong-remove" data-id="' + w.problemId + '">移除</button></td></tr>';
        }).join('') + '</tbody></table></div></div>'
        : TZ.empty('book', '错题本是空的', '答错的编程题、客观题与低于 60 分的简答题会自动进入这里。', '<a class="btn btn-primary" href="#/problems">去刷题</a>')),
      'records'
    );
  };

  C.battleRecords = function () {
    const user = TZ.currentUser();
    const rows = TZ.db.battles.filter(function (b) { return b.userId === user.id; });
    const resultText = { win: '胜', loss: '负', draw: '平' };
    return TZ.layoutC(
      TZ.pageHeader('对战记录', '每局对战的基础结果与用时。', '<a class="btn btn-outline" href="#/battle">' + TZ.icon('swords', 15) + ' 去对战</a>') +
      (rows.length ? '<div class="card"><div class="table-wrap"><table class="table"><thead><tr><th>房间</th><th>对手</th><th>题目</th><th>结果</th><th>开始时间</th><th>用时</th><th></th></tr></thead><tbody>' +
        rows.map(function (b) {
          const p = TZ.getOjProblem(b.problemId);
          return '<tr><td>' + b.room + '</td><td>' + TZ.esc(b.opponentName) + '</td><td class="muted">' + TZ.esc(p ? p.title : '—') + '</td><td><span class="battle-result-badge ' + b.result + '">' + resultText[b.result] + '</span></td><td class="muted">' + TZ.fmtDate(b.startedAt) + '</td><td class="muted">' + b.duration + '</td>' +
            '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="view-battle" data-id="' + b.id + '">详情</button></td></tr>';
        }).join('') + '</tbody></table></div></div>'
        : TZ.empty('swords', '还没有对战记录', '创建房间邀请好友开始第一局。', '<a class="btn btn-primary" href="#/battle">去对战</a>')),
      'battle'
    );
  };

  /* ---------- 个人中心 ---------- */
  C.profile = function () {
    const user = TZ.currentUser();
    const me = TZ.getMyRankRow();
    const subs = TZ.getSubmissions(user.id);
    const acCount = subs.filter(function (s) { return s.verdict === 'AC'; }).length;
    const wrongCount = TZ.db.wrongBook.filter(function (w) { return w.userId === user.id; }).length;
    let doneStages = 0;
    TZ.db.routes.forEach(function (r) { doneStages += TZ.routeProgress(user.id, r).done; });
    const declarations = TZ.getManualBreakdown(user.id);
    const cfCount = me ? me.cf : 0;
    const cred = TZ.getLlmCredential(user.id);
    return TZ.layoutC(
      TZ.pageHeader('个人中心', '管理资料、外部账号与本周申报。', '<button class="btn btn-outline" data-action="open-edit-profile">' + TZ.icon('edit', 15) + ' 编辑资料</button>') +
      '<div class="profile-layout">' +
      '<div class="profile-main">' +
      '<div class="card card-pad profile-card"><div class="profile-banner"></div><div class="profile-card-body">' +
      TZ.avatar(user, 76) +
      '<div class="profile-identity"><h2>' + TZ.esc(user.name) + '</h2><p>' + TZ.esc(user.school || '未填写学校') + '</p><div class="profile-badges">' + TZ.badge('选手', 'indigo') + (user.cfHandle ? TZ.badge('CF 已绑定', 'green') : TZ.badge('CF 未绑定', 'slate')) + TZ.badge('加入于 ' + TZ.fmtDate(user.joinedAt, false), 'slate') + '</div></div></div>' +
      '<div class="profile-stats"><div><strong>' + (me ? me.rank : '-') + '</strong><span>本周排名</span></div><div><strong>' + acCount + '</strong><span>累计 AC</span></div><div><strong>' + doneStages + '</strong><span>完成阶段</span></div><div><strong>' + wrongCount + '</strong><span>错题待复盘</span></div></div></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>Codeforces 绑定</h3>' + (user.cfHandle ? TZ.badge('已绑定', 'green') : TZ.badge('未绑定', 'slate')) + '</div>' +
      (user.cfHandle ? '<div class="cf-account"><div class="cf-account-icon">CF</div><div><strong>' + TZ.esc(user.cfHandle) + '</strong><span>本周模拟同步过题 ' + cfCount + ' 道</span></div><span class="badge badge-green">校验通过</span></div>'
        : '<p class="muted">绑定 Codeforces 后自动同步本周过题数（演示环境使用白名单模拟）。</p>') +
      '<div class="flex gap-8 section"><button class="btn btn-soft btn-sm" data-action="open-cf">' + (user.cfHandle ? '更换绑定' : '绑定 CF') + '</button>' + (user.cfHandle ? '<button class="btn btn-ghost btn-sm danger" data-action="unbind-cf">解绑</button>' : '') + '</div>' +
      '<p class="hint">解绑后历史数据保留，但不再更新。</p></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>LLM 造数据凭证</h3>' + (cred ? TZ.badge(cred.status === 'valid' ? '有效' : '已失效', cred.status === 'valid' ? 'green' : 'red') : TZ.badge('未配置', 'slate')) + '</div>' +
      (cred
        ? '<div class="credential-row"><div class="credential-icon">AI</div><div class="credential-info"><strong>' + TZ.esc(cred.provider) + '</strong><span>' + TZ.maskLlmKey(cred.key) + ' · ' + TZ.esc(cred.model || '服务商默认模型') + '</span><span class="muted">最近校验 ' + TZ.fmtDate(cred.lastValidatedAt) + '</span></div></div>'
        : '<p class="muted">配置自己的 API Key 后可使用「LLM 构造」与「LLM 直接生成数据」；随机生成和简答题 AI 判分不需要此凭证。</p>') +
      '<div class="flex gap-8 section"><button class="btn btn-soft btn-sm" data-action="open-llm-credential">' + (cred ? '更换凭证' : '配置凭证') + '</button>' + (cred ? '<button class="btn btn-ghost btn-sm danger" data-action="delete-llm-credential">删除凭证</button>' : '') + '</div>' +
      '<p class="hint">Key 仅保存在服务端（原型为本机演示数据），页面只展示脱敏尾号；提交 LLM 任务时题面与特殊要求会发送给所选服务商。</p></div>' +
      '<div class="card card-pad"><div class="section-head compact"><h3>本周手动申报</h3><button class="btn btn-soft btn-sm" data-action="open-manual">' + TZ.icon('plus', 14) + ' 申报</button></div>' +
      (declarations.length ? declarations.map(function (d) {
        return '<div class="manual-row"><span class="badge badge-cyan">' + TZ.esc(d.platform) + '</span><span class="manual-handle">' + TZ.esc(d.handle) + '</span><strong>' + d.count + ' 题</strong><span class="badge badge-slate">未校验</span><button class="btn btn-ghost btn-sm danger" data-action="delete-manual" data-id="' + d.id + '">删除</button></div>';
      }).join('') : '<p class="muted">本周还没有手动申报。</p>') +
      '<p class="hint">手动申报按自然周生效，仅用于周榜统计。</p></div>' +
      '</div>' +
      '<aside class="home-side">' +
      '<div class="card card-pad"><h3>账号信息</h3><div class="meta-list"><div><span>用户名</span><strong>' + TZ.esc(user.username || '—') + '</strong></div><div><span>邮箱</span><strong>' + TZ.esc(user.email || '未填写') + '</strong></div><div><span>学校</span><strong>' + TZ.esc(user.school || '未填写') + '</strong></div><div><span>注册时间</span><strong>' + TZ.fmtDate(user.joinedAt, false) + '</strong></div></div></div>' +
      '<div class="card card-pad"><h3>XCPC</h3><p class="muted">查看外部 XCPC 评分与获奖记录。</p><a class="btn btn-outline btn-block" href="#/xcpc">打开 XCPC 页面 ' + TZ.icon('external', 14) + '</a></div>' +
      '<div class="card card-pad danger-zone"><h3>演示与数据</h3><p class="muted">原型数据保存在本机浏览器，可切换账号体验不同角色，或重置全部演示数据。</p><button class="btn btn-outline btn-block" data-action="switch-account">' + TZ.icon('users', 15) + ' 切换演示账号</button><button class="btn btn-ghost btn-block danger" data-action="reset-demo">' + TZ.icon('refresh', 15) + ' 重置演示数据</button></div>' +
      '</aside></div>',
      'profile'
    );
  };

  /* ---------- XCPC ---------- */
  C.xcpc = function () {
    return TZ.layoutC(
      TZ.pageHeader('XCPC 评分与获奖记录', '首期仅提供外部入口，不自建评分与获奖数据。', '<a class="btn btn-outline" href="#/home">返回首页</a>') +
      '<div class="xcpc-hero card"><div class="xcpc-hero-copy"><span class="badge badge-amber">外部服务</span><h2>XCPC Rating & Awards</h2><p>查看 ICPC / CCPC 等赛事的评分与获奖记录。题阵不复制、不修改外部数据，仅提供统一入口。</p>' +
      '<a class="btn btn-primary btn-lg" href="https://hei-maom.github.io/xcpcrating/" target="_blank" rel="noopener">' + TZ.icon('external', 17) + ' 打开 XCPC Rating</a></div>' +
      '<div class="xcpc-hero-visual"><div class="xcpc-medal">' + TZ.emojiIcon('🐼') + '</div><span>XCPC</span></div></div>' +
      '<div class="grid grid-3 section"><div class="card card-pad"><div class="card-icon amber">' + TZ.emojiIcon('🐼') + '</div><h3>获奖记录</h3><p class="muted">在外部服务查看 ICPC / CCPC 等赛事获奖信息。</p></div>' +
      '<div class="card card-pad"><div class="card-icon indigo">' + TZ.emojiIcon('🐼') + '</div><h3>评分排名</h3><p class="muted">查看 XCPC Rating 与历史变化。</p></div>' +
      '<div class="card card-pad"><div class="card-icon cyan">' + TZ.emojiIcon('🐼') + '</div><h3>数据边界</h3><p class="muted">首期不自建评分模型，不抓取外部数据。</p></div></div>',
      'xcpc'
    );
  };
})();
