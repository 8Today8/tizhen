/* ============================================================
 * 题阵 · B 端运营后台
 * ============================================================ */
(function () {
  const TZ = window.TZ;

  TZ.state.admin = Object.assign({
    routeSearch: '',
    stageRoute: 'all',
    ojSearch: '',
    ojDifficulty: 'all',
    ojStatus: 'all',
    objSearch: '',
    shortSearch: '',
    shortStatus: 'all',
    extSearch: '',
    userSearch: '',
    taskStatus: 'all',
    logSearch: '',
    logAdmin: 'all',
    leaderboardMode: 'week'
  }, TZ.state.admin || {});

  const A = (TZ.admin = {});

  function uniqueProblems(stageIds) {
    const set = {};
    stageIds.forEach(function (id) { set[id] = true; });
    return Object.keys(set).length;
  }

  function tagString(tags) {
    return (tags || []).join('、');
  }

  function parseTags(str) {
    return String(str || '').split(/[,，、]/).map(function (s) { return s.trim(); }).filter(Boolean);
  }

  function problemOptions(selected) {
    selected = selected || [];
    const groups = [
      { label: '编程题', list: TZ.db.problems, tone: 'indigo' },
      { label: '客观题', list: TZ.db.objectives, tone: 'purple' },
      { label: '简答题', list: TZ.db.shortAnswers, tone: 'green' },
      { label: '外链题', list: TZ.db.externals, tone: 'cyan' }
    ];
    return '<div class="check-list">' + groups.map(function (g) {
      return '<div class="check-group-label">' + g.label + '</div>' + g.list.map(function (p) {
        return '<label class="check-item"><input type="checkbox" name="problemIds" value="' + p.id + '"' + (selected.indexOf(p.id) >= 0 ? ' checked' : '') + ' />' +
          '<span class="check-item-main"><span class="check-item-title">' + TZ.esc(p.title) + '</span><span class="check-item-meta">' + TZ.esc(p.difficulty) + ' · ' + TZ.esc(p.id) + '</span></span></label>';
      }).join('');
    }).join('') + '</div>';
  }

  /* ---------- 工作台 ---------- */
  A.home = function () {
    const admin = TZ.currentAdmin();
    const roleText = admin.role === 'super' ? '超级管理员' : '内容运营';
    const logs = TZ.db.logs.slice(0, 5);
    const quick = [
      { href: '#/admin/routes', icon: 'route', title: '路线管理', desc: '维护学习路线与阶段', tone: 'indigo' },
      { href: '#/admin/oj', icon: 'code', title: 'OJ 题库', desc: '新增题目、上下架', tone: 'cyan' },
      { href: '#/admin/users', icon: 'users', title: '用户管理', desc: admin.role === 'super' ? '查看、新增、启禁用与删除用户' : '查看用户信息', tone: 'purple' },
      { href: '#/admin/leaderboard', icon: 'trophy', title: '榜单管理', desc: '查看周榜 / 总榜与手动申报', tone: 'amber' },
      { href: '#/admin/datagen', icon: 'wand', title: '造数据任务', desc: '查看生成结果与失败原因', tone: 'green' },
      { href: '#/admin/logs', icon: 'history', title: '操作日志', desc: '追踪后台关键操作', tone: 'slate' }
    ];
    return TZ.layoutAdmin(
      '<div class="admin-welcome"><div><span class="badge badge-indigo">' + TZ.WEEK_LABEL + '</span><h1>欢迎回来，' + TZ.esc(admin.name) + '</h1>' +
      '<p>当前角色：' + roleText + '。首期后台聚焦内容维护、题库管理、用户与周榜查看、造数据任务与操作日志。</p></div>' +
      '<div class="admin-welcome-icon">' + TZ.emojiIcon('🐨') + '</div></div>' +
      '<div class="grid grid-3 section">' + quick.map(function (q) {
        return '<a class="card card-hover admin-quick" href="' + q.href + '"><span class="card-icon ' + q.tone + '">' + TZ.emojiIcon(TZ.emojiFor(q.title) || '😸') + '</span><div><strong>' + q.title + '</strong><span>' + q.desc + '</span></div>' + TZ.icon('arrowRight', 16) + '</a>';
      }).join('') + '</div>' +
      '<div class="split section"><section class="card"><div class="section-head card-pad-head"><div><h3>最近操作</h3><div class="section-sub">后台关键动作会记录在此</div></div><a class="link" href="#/admin/logs">全部日志</a></div>' +
      '<div class="table-wrap"><table class="table table-compact"><thead><tr><th>时间</th><th>操作人</th><th>动作</th><th>对象</th></tr></thead><tbody>' +
      logs.map(function (l) {
        return '<tr><td class="muted">' + TZ.fmtDate(l.createdAt) + '</td><td>' + TZ.esc(l.adminName) + '</td><td>' + TZ.esc(l.action) + '</td><td class="muted">' + TZ.esc(l.target) + '</td></tr>';
      }).join('') + '</tbody></table></div></section>' +
      '<aside class="card card-pad"><div class="card-icon amber">' + TZ.emojiIcon('🐨') + '</div><h3>首期后台边界</h3><ul class="rule-list compact">' +
      '<li>' + TZ.icon('check', 15) + '<span>不做数据大盘；手动申报审核仅超管可操作。</span></li>' +
      '<li>' + TZ.icon('check', 15) + '<span>内容编辑保存后立即生效。</span></li>' +
      '<li>' + TZ.icon('check', 15) + '<span>题目上下架用状态字段，已引用题目不物理删除。</span></li>' +
      '<li>' + TZ.icon('check', 15) + '<span>角色固定为超级管理员与内容运营。</span></li></ul></aside></div>',
      'home'
    );
  };

  /* ---------- 路线管理 ---------- */
  A.routes = function () {
    const q = TZ.state.admin.routeSearch.trim().toLowerCase();
    const routes = TZ.db.routes.filter(function (r) {
      return !q || (r.title + ' ' + r.subtitle).toLowerCase().indexOf(q) >= 0;
    }).sort(function (a, b) { return a.sort - b.sort; });
    return TZ.layoutAdmin(
      TZ.pageHeader('路线管理', '维护学习路线、阶段与题目引用。', '<button class="btn btn-primary" data-action="admin-add-route">' + TZ.icon('plus', 15) + ' 新增路线</button>') +
      '<div class="card"><div class="table-toolbar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-route-search" class="input" data-filter="admin-route-search" placeholder="搜索路线" value="' + TZ.esc(TZ.state.admin.routeSearch) + '" /></div><span class="muted">共 ' + routes.length + ' 条路线</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>路线</th><th>难度</th><th>阶段</th><th>题目</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (routes.length ? routes.map(function (r) {
        const ids = [];
        r.stages.forEach(function (s) { s.problemIds.forEach(function (id) { ids.push(id); }); });
        return '<tr><td><div class="admin-title-cell"><strong>' + TZ.esc(r.title) + '</strong><span>' + TZ.esc(r.subtitle) + '</span></div></td>' +
          '<td>' + TZ.difficultyBadge(r.level) + '</td><td class="num">' + r.stages.length + '</td><td class="num">' + uniqueProblems(ids) + '</td><td class="num">' + r.sort + '</td><td>' + TZ.statusBadge(r.status) + '</td>' +
          '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="admin-edit-route" data-id="' + r.id + '">' + TZ.icon('edit', 14) + ' 编辑</button>' +
          '<a class="btn btn-soft btn-sm" href="#/admin/route/' + r.id + '/stages">阶段管理</a>' +
          '<button class="btn btn-ghost btn-sm danger" data-action="admin-delete-route" data-id="' + r.id + '">' + TZ.icon('trash', 14) + '</button></td></tr>';
      }).join('') : '<tr><td colspan="7">' + TZ.empty('route', '没有路线', '点击右上角新增第一条路线。') + '</td></tr>') +
      '</tbody></table></div></div>',
      'routes'
    );
  };

  A.openRouteModal = function (id) {
    const r = id ? TZ.getRoute(id) : null;
    const body = '<form data-form="admin-route" class="form-stack">' +
      '<input type="hidden" name="id" value="' + (r ? r.id : '') + '" />' +
      '<div class="form-grid">' +
      '<div class="field full"><label class="label">路线标题</label><input class="input" name="title" value="' + TZ.esc(r ? r.title : '') + '" placeholder="例如 动态规划专项" /></div>' +
      '<div class="field full"><label class="label">副标题</label><input class="input" name="subtitle" value="' + TZ.esc(r ? r.subtitle : '') + '" placeholder="一句话说明路线价值" /></div>' +
      '<div class="field"><label class="label">难度</label><select class="select" name="level">' + ['入门', '基础', '进阶', '困难'].map(function (l) { return '<option' + (r && r.level === l ? ' selected' : '') + '>' + l + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">主题色</label><select class="select" name="color">' + [['indigo', '靛蓝'], ['cyan', '青色'], ['purple', '紫色'], ['amber', '琥珀']].map(function (c) { return '<option value="' + c[0] + '"' + (r && r.color === c[0] ? ' selected' : '') + '>' + c[1] + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">预计用时</label><input class="input" name="estimated" value="' + TZ.esc(r ? r.estimated : '约 6 小时') + '" /></div>' +
      '<div class="field"><label class="label">排序</label><input class="input" type="number" name="sort" value="' + (r ? r.sort : TZ.db.routes.length + 1) + '" /></div>' +
      '<div class="field"><label class="label">状态</label><select class="select" name="status"><option value="published"' + (r && r.status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (r && r.status === 'draft' ? ' selected' : '') + '>草稿</option></select></div>' +
      '<div class="field full"><label class="label">路线描述</label><textarea class="textarea" name="description" rows="4">' + TZ.esc(r ? r.description : '') + '</textarea></div>' +
      '</div><div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存路线</button></div></form>';
    TZ.modal({ title: r ? '编辑路线' : '新增路线', body: body, size: 'modal-lg' });
  };

  /* ---------- 阶段管理 ---------- */
  A.stages = function () {
    const filter = TZ.state.admin.stageRoute;
    const rows = [];
    TZ.db.routes.forEach(function (r) {
      if (filter !== 'all' && r.id !== filter) return;
      r.stages.slice().sort(function (a, b) { return a.sort - b.sort; }).forEach(function (s) {
        rows.push({ route: r, stage: s });
      });
    });
    return TZ.layoutAdmin(
      TZ.pageHeader('阶段管理', '跨路线查看阶段，进入路线后可新增、排序与编辑。', '<a class="btn btn-outline" href="#/admin/routes">' + TZ.icon('route', 15) + ' 路线管理</a>') +
      '<div class="card"><div class="table-toolbar"><select id="admin-stage-route" class="select filter-select" data-filter="admin-stage-route"><option value="all">全部路线</option>' + TZ.db.routes.map(function (r) { return '<option value="' + r.id + '"' + (filter === r.id ? ' selected' : '') + '>' + TZ.esc(r.title) + '</option>'; }).join('') + '</select><span class="muted">共 ' + rows.length + ' 个阶段</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>路线</th><th>阶段</th><th>顺序</th><th>题目</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (x) {
        return '<tr><td class="muted">' + TZ.esc(x.route.title) + '</td><td><strong>' + TZ.esc(x.stage.title) + '</strong></td><td class="num">' + x.stage.sort + '</td><td class="num">' + x.stage.problemIds.length + '</td><td>' + TZ.statusBadge(x.stage.status) + '</td>' +
          '<td class="row-actions"><a class="btn btn-soft btn-sm" href="#/admin/route/' + x.route.id + '/stages">管理</a><button class="btn btn-ghost btn-sm" data-action="admin-edit-stage" data-route="' + x.route.id + '" data-id="' + x.stage.id + '">编辑</button></td></tr>';
      }).join('') : '<tr><td colspan="6">' + TZ.empty('layers', '没有阶段') + '</td></tr>') +
      '</tbody></table></div></div>',
      'stages'
    );
  };

  A.routeStages = function (routeId) {
    const route = TZ.getRoute(routeId);
    if (!route) return TZ.layoutAdmin(TZ.empty('route', '路线不存在', '请返回路线管理。', '<a class="btn btn-primary" href="#/admin/routes">返回路线管理</a>'), 'stages');
    const stages = route.stages.slice().sort(function (a, b) { return a.sort - b.sort; });
    return TZ.layoutAdmin(
      TZ.pageHeader('阶段管理 · ' + TZ.esc(route.title), '调整阶段顺序、编辑内容、关联题目与视频课程。',
        '<a class="btn btn-outline" href="#/admin/routes">' + TZ.icon('chevronLeft', 15) + ' 返回路线</a><button class="btn btn-primary" data-action="admin-add-stage" data-route="' + route.id + '">' + TZ.icon('plus', 15) + ' 新增阶段</button>',
        '<a href="#/admin/routes">路线管理</a> <span class="crumb-sep">/</span> ' + TZ.esc(route.title)) +
      '<div class="card"><div class="table-toolbar"><span class="muted">共 ' + stages.length + ' 个阶段 · 题目可跨阶段复用</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>顺序</th><th>阶段</th><th>用时</th><th>题目</th><th>课程</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (stages.length ? stages.map(function (s, i) {
        return '<tr><td><span class="order-badge">' + (i + 1) + '</span></td><td><div class="admin-title-cell"><strong>' + TZ.esc(s.title) + '</strong><span>' + TZ.esc(s.summary) + '</span></div></td><td class="muted">' + TZ.esc(s.duration) + '</td><td class="num">' + s.problemIds.length + '</td><td class="num">' + (s.courses || []).length + '</td><td>' + TZ.statusBadge(s.status) + '</td>' +
          '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="admin-edit-stage" data-route="' + route.id + '" data-id="' + s.id + '">' + TZ.icon('edit', 14) + ' 编辑</button>' +
          '<button class="btn btn-ghost btn-sm" data-action="admin-move-stage" data-id="' + s.id + '" data-dir="up" title="上移">↑</button>' +
          '<button class="btn btn-ghost btn-sm" data-action="admin-move-stage" data-id="' + s.id + '" data-dir="down" title="下移">↓</button>' +
          '<button class="btn btn-ghost btn-sm danger" data-action="admin-delete-stage" data-id="' + s.id + '">' + TZ.icon('trash', 14) + '</button></td></tr>';
      }).join('') : '<tr><td colspan="7">' + TZ.empty('layers', '还没有阶段', '点击右上角新增阶段。') + '</td></tr>') +
      '</tbody></table></div></div>' +
      '<div class="info-strip section"><span class="info-strip-icon">' + TZ.icon('info', 17) + '</span><div><strong>题目引用</strong><p>阶段只引用全局题库中的题目，题目可被多个阶段复用；删除阶段不会删除题目。</p></div></div>',
      'stages'
    );
  };

  A.courseEditorRow = function (course) {
    const c = course || {};
    return '<div class="course-editor-row" data-course-row data-course-id="' + TZ.esc(c.id || '') + '">' +
      '<input class="input" name="courseTitle" placeholder="课程标题" value="' + TZ.esc(c.title || '') + '" />' +
      '<input class="input" name="courseUrl" placeholder="https://www.bilibili.com/video/BV..." value="' + TZ.esc(c.url || '') + '" />' +
      '<input class="input course-duration" name="courseDuration" placeholder="时长 18:20" value="' + TZ.esc(c.duration || '') + '" />' +
      '<input class="input course-source" name="courseSource" placeholder="来源 / UP 主（可选）" value="' + TZ.esc(c.source || '') + '" />' +
      '<button type="button" class="btn btn-ghost btn-sm danger" data-action="admin-remove-course-row" title="删除课程">' + TZ.icon('trash', 14) + '</button>' +
      '</div>';
  };

  A.openStageModal = function (routeId, stageId) {
    const route = TZ.getRoute(routeId);
    if (!route) return;
    const s = stageId ? route.stages.find(function (x) { return x.id === stageId; }) : null;
    const body = '<form data-form="admin-stage" class="form-stack">' +
      '<input type="hidden" name="routeId" value="' + route.id + '" /><input type="hidden" name="id" value="' + (s ? s.id : '') + '" />' +
      '<div class="form-grid"><div class="field full"><label class="label">阶段标题</label><input class="input" name="title" value="' + TZ.esc(s ? s.title : '') + '" /></div>' +
      '<div class="field full"><label class="label">阶段摘要</label><input class="input" name="summary" value="' + TZ.esc(s ? s.summary : '') + '" /></div>' +
      '<div class="field"><label class="label">预计用时</label><input class="input" name="duration" value="' + TZ.esc(s ? s.duration : '60 分钟') + '" /></div>' +
      '<div class="field"><label class="label">排序</label><input class="input" type="number" name="sort" value="' + (s ? s.sort : route.stages.length + 1) + '" /></div>' +
      '<div class="field"><label class="label">状态</label><select class="select" name="status"><option value="published"' + (s && s.status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (s && s.status === 'draft' ? ' selected' : '') + '>草稿</option></select></div></div>' +
      '<div class="field"><label class="label">阶段内容（HTML）</label><textarea class="textarea admin-content-input" name="content" rows="10">' + TZ.esc(s ? s.content : '<h3>学习目标</h3><p>描述本阶段的学习目标。</p><h4>核心知识点</h4><ul><li>知识点一</li></ul><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>') + '</textarea></div>' +
      '<div class="field"><label class="label">关联题目</label>' + problemOptions(s ? s.problemIds : []) + '</div>' +
      '<div class="field"><label class="label">视频课程（B 站）</label>' +
      '<div class="course-editor" id="stage-course-rows">' + ((s && s.courses) ? s.courses.map(A.courseEditorRow).join('') : '') + '</div>' +
      '<button type="button" class="btn btn-outline btn-sm" data-action="admin-add-course-row">' + TZ.icon('plus', 14) + ' 添加课程</button>' +
      '<p class="hint">粘贴完整 B 站视频链接，保存时自动规范化为 bilibili.com/video/BV...；课程仅提供跳转，不计入阶段进度。</p></div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存阶段</button></div></form>';
    TZ.modal({ title: s ? '编辑阶段' : '新增阶段', body: body, size: 'modal-lg' });
  };

  /* ---------- OJ 题库 ---------- */
  A.oj = function () {
    const f = TZ.state.admin;
    const q = f.ojSearch.trim().toLowerCase();
    const rows = TZ.db.problems.filter(function (p) {
      if (q && (p.title + ' ' + (p.tags || []).join(' ')).toLowerCase().indexOf(q) < 0) return false;
      if (f.ojDifficulty !== 'all' && p.difficulty !== f.ojDifficulty) return false;
      if (f.ojStatus !== 'all' && p.status !== f.ojStatus) return false;
      return true;
    });
    return TZ.layoutAdmin(
      TZ.pageHeader('OJ 题库', '站内编程题的统一管理；已引用题目请使用下架而非删除。', '<button class="btn btn-primary" data-action="admin-add-oj">' + TZ.icon('plus', 15) + ' 新增题目</button>') +
      '<div class="card"><div class="filter-bar table-filter-bar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-oj-search" class="input" data-filter="admin-oj-search" placeholder="搜索题目或标签" value="' + TZ.esc(f.ojSearch) + '" /></div>' +
      '<select id="admin-oj-difficulty" class="select filter-select" data-filter="admin-oj-difficulty"><option value="all">全部难度</option>' + ['入门', '基础', '进阶'].map(function (d) { return '<option' + (f.ojDifficulty === d ? ' selected' : '') + '>' + d + '</option>'; }).join('') + '</select>' +
      '<select id="admin-oj-status" class="select filter-select" data-filter="admin-oj-status"><option value="all">全部状态</option><option value="published"' + (f.ojStatus === 'published' ? ' selected' : '') + '>已上架</option><option value="draft"' + (f.ojStatus === 'draft' ? ' selected' : '') + '>草稿</option></select>' +
      '<span class="muted">' + rows.length + ' 道</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>题目</th><th>难度</th><th>标签</th><th>提交 / 通过</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (p) {
        return '<tr><td><div class="admin-title-cell"><strong>' + TZ.esc(p.title) + '</strong><span>' + TZ.esc(p.source) + ' · ' + p.id + '</span></div></td><td>' + TZ.difficultyBadge(p.difficulty) + '</td><td class="muted">' + TZ.esc(tagString(p.tags)) + '</td><td class="num">' + p.submissions + ' / ' + p.accepted + '</td><td>' + TZ.statusBadge(p.status) + '</td>' +
          '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="admin-edit-oj" data-id="' + p.id + '">' + TZ.icon('edit', 14) + ' 编辑</button>' +
          '<button class="btn btn-ghost btn-sm" data-action="admin-toggle-oj" data-id="' + p.id + '">' + (p.status === 'published' ? '下架' : '上架') + '</button>' +
          '<button class="btn btn-ghost btn-sm danger" data-action="admin-delete-oj" data-id="' + p.id + '">' + TZ.icon('trash', 14) + '</button></td></tr>';
      }).join('') : '<tr><td colspan="6">' + TZ.empty('code', '没有匹配的题目') + '</td></tr>') +
      '</tbody></table></div></div>',
      'oj'
    );
  };

  A.openOjModal = function (id) {
    const p = id ? TZ.getOjProblem(id) : null;
    const body = '<form data-form="admin-oj" class="form-stack">' +
      '<input type="hidden" name="id" value="' + (p ? p.id : '') + '" />' +
      '<div class="form-grid"><div class="field full"><label class="label">题目名称</label><input class="input" name="title" value="' + TZ.esc(p ? p.title : '') + '" /></div>' +
      '<div class="field"><label class="label">难度</label><select class="select" name="difficulty">' + ['入门', '基础', '进阶'].map(function (d) { return '<option' + (p && p.difficulty === d ? ' selected' : '') + '>' + d + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">标签（逗号分隔）</label><input class="input" name="tags" value="' + TZ.esc(p ? (p.tags || []).join(', ') : '') + '" /></div>' +
      '<div class="field"><label class="label">时间限制</label><input class="input" name="timeLimit" value="' + TZ.esc(p ? p.timeLimit : '1s') + '" /></div>' +
      '<div class="field"><label class="label">内存限制</label><input class="input" name="memoryLimit" value="' + TZ.esc(p ? p.memoryLimit : '256MB') + '" /></div>' +
      '<div class="field"><label class="label">状态</label><select class="select" name="status"><option value="published"' + (p && p.status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (p && p.status === 'draft' ? ' selected' : '') + '>草稿</option></select></div></div>' +
      '<div class="field"><label class="label">题目描述（HTML）</label><textarea class="textarea" name="statement" rows="4">' + TZ.esc(p ? p.statement : '<p>题目描述。</p>') + '</textarea></div>' +
      '<div class="form-grid"><div class="field"><label class="label">输入格式</label><textarea class="textarea" name="inputFormat" rows="3">' + TZ.esc(p ? p.inputFormat : '') + '</textarea></div><div class="field"><label class="label">输出格式</label><textarea class="textarea" name="outputFormat" rows="3">' + TZ.esc(p ? p.outputFormat : '') + '</textarea></div>' +
      '<div class="field"><label class="label">样例输入</label><textarea class="textarea mono" name="sampleInput" rows="3">' + TZ.esc(p && p.samples[0] ? p.samples[0].input : '') + '</textarea></div><div class="field"><label class="label">样例输出</label><textarea class="textarea mono" name="sampleOutput" rows="3">' + TZ.esc(p && p.samples[0] ? p.samples[0].output : '') + '</textarea></div></div>' +
      '<div class="field"><label class="label">测试数据（演示）</label><textarea class="textarea mono" name="testData" rows="4">' + TZ.esc(p ? p.testData : '') + '</textarea></div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存题目</button></div></form>';
    TZ.modal({ title: p ? '编辑 OJ 题目' : '新增 OJ 题目', body: body, size: 'modal-lg' });
  };

  /* ---------- 客观题 ---------- */
  A.objective = function () {
    const q = TZ.state.admin.objSearch.trim().toLowerCase();
    const rows = TZ.db.objectives.filter(function (p) {
      return !q || p.title.toLowerCase().indexOf(q) >= 0;
    });
    return TZ.layoutAdmin(
      TZ.pageHeader('客观题', '管理单选、多选与判断题；答错自动进入错题本。', '<button class="btn btn-primary" data-action="admin-add-objective">' + TZ.icon('plus', 15) + ' 新增客观题</button>') +
      '<div class="card"><div class="table-toolbar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-obj-search" class="input" data-filter="admin-obj-search" placeholder="搜索客观题" value="' + TZ.esc(TZ.state.admin.objSearch) + '" /></div><span class="muted">' + rows.length + ' 道</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>题目</th><th>题型</th><th>难度</th><th>标签</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (p) {
        return '<tr><td><div class="admin-title-cell"><strong>' + TZ.esc(p.title) + '</strong><span>' + p.id + ' · 答案 ' + p.answer.join('、') + '</span></div></td>' +
          '<td>' + TZ.badge(p.qtype === 'multiple' ? '多选' : p.qtype === 'judge' ? '判断' : '单选', 'purple') + '</td><td>' + TZ.difficultyBadge(p.difficulty) + '</td><td class="muted">' + TZ.esc(tagString(p.tags)) + '</td><td>' + TZ.statusBadge(p.status) + '</td>' +
          '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="admin-edit-objective" data-id="' + p.id + '">' + TZ.icon('edit', 14) + ' 编辑</button><button class="btn btn-ghost btn-sm danger" data-action="admin-delete-objective" data-id="' + p.id + '">' + TZ.icon('trash', 14) + '</button></td></tr>';
      }).join('') : '<tr><td colspan="6">' + TZ.empty('book', '没有匹配的客观题') + '</td></tr>') +
      '</tbody></table></div></div>',
      'objective'
    );
  };

  A.openObjectiveModal = function (id) {
    const p = id ? TZ.getObjective(id) : null;
    const opts = p ? p.options : [{ key: 'A', text: '' }, { key: 'B', text: '' }, { key: 'C', text: '' }, { key: 'D', text: '' }];
    const optVal = function (key) {
      const o = opts.find(function (x) { return x.key === key; });
      return o ? o.text : '';
    };
    const body = '<form data-form="admin-objective" class="form-stack">' +
      '<input type="hidden" name="id" value="' + (p ? p.id : '') + '" />' +
      '<div class="field"><label class="label">题干</label><input class="input" name="title" value="' + TZ.esc(p ? p.title : '') + '" /></div>' +
      '<div class="form-grid"><div class="field"><label class="label">题型</label><select class="select" name="qtype"><option value="single"' + (p && p.qtype === 'single' ? ' selected' : '') + '>单选</option><option value="multiple"' + (p && p.qtype === 'multiple' ? ' selected' : '') + '>多选</option><option value="judge"' + (p && p.qtype === 'judge' ? ' selected' : '') + '>判断</option></select></div>' +
      '<div class="field"><label class="label">难度</label><select class="select" name="difficulty">' + ['入门', '基础', '进阶'].map(function (d) { return '<option' + (p && p.difficulty === d ? ' selected' : '') + '>' + d + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">标签（逗号分隔）</label><input class="input" name="tags" value="' + TZ.esc(p ? (p.tags || []).join(', ') : '') + '" /></div>' +
      '<div class="field"><label class="label">状态</label><select class="select" name="status"><option value="published"' + (p && p.status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (p && p.status === 'draft' ? ' selected' : '') + '>草稿</option></select></div></div>' +
      '<div class="form-grid">' + ['A', 'B', 'C', 'D'].map(function (k) {
        return '<div class="field"><label class="label">选项 ' + k + '</label><input class="input" name="option' + k + '" value="' + TZ.esc(optVal(k)) + '" /></div>';
      }).join('') + '</div>' +
      '<div class="form-grid"><div class="field"><label class="label">正确答案（如 A 或 A,C）</label><input class="input" name="answer" value="' + TZ.esc(p ? p.answer.join(',') : 'A') + '" /></div></div>' +
      '<div class="field"><label class="label">解析</label><textarea class="textarea" name="explanation" rows="4">' + TZ.esc(p ? p.explanation : '') + '</textarea></div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存客观题</button></div></form>';
    TZ.modal({ title: p ? '编辑客观题' : '新增客观题', body: body, size: 'modal-lg' });
  };

  /* ---------- 简答题 ---------- */
  A.short = function () {
    const q = TZ.state.admin.shortSearch.trim().toLowerCase();
    const status = TZ.state.admin.shortStatus;
    const rows = TZ.db.shortAnswers.filter(function (p) {
      if (status !== 'all' && p.status !== status) return false;
      return !q || p.title.toLowerCase().indexOf(q) >= 0;
    });
    return TZ.layoutAdmin(
      TZ.pageHeader('简答题', '主观作答由 AI 判分（百分制，60 分及格）；未通过进入错题本，不计入周榜。', '<button class="btn btn-primary" data-action="admin-add-short">' + TZ.icon('plus', 15) + ' 新增简答题</button>') +
      '<div class="card"><div class="table-toolbar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-short-search" class="input" data-filter="admin-short-search" placeholder="搜索简答题" value="' + TZ.esc(TZ.state.admin.shortSearch) + '" /></div>' +
      '<select id="admin-short-status" class="select filter-select" data-filter="admin-short-status"><option value="all">全部状态</option><option value="published"' + (status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (status === 'draft' ? ' selected' : '') + '>草稿</option></select><span class="muted">' + rows.length + ' 道</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>题目</th><th>难度</th><th>标签</th><th>评分要点</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (p) {
        return '<tr><td><div class="admin-title-cell"><strong>' + TZ.esc(p.title) + '</strong><span>' + p.id + ' · 满分 ' + (p.maxScore || 100) + ' · ' + ((p.referenceAnswer || '').length > 0 ? '已配置参考答案' : '未配置参考答案') + '</span></div></td>' +
          '<td>' + TZ.difficultyBadge(p.difficulty) + '</td><td class="muted">' + TZ.esc(tagString(p.tags)) + '</td><td class="num">' + (p.scoringRubric || []).length + '</td><td>' + TZ.statusBadge(p.status) + '</td>' +
          '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="admin-edit-short" data-id="' + p.id + '">' + TZ.icon('edit', 14) + ' 编辑</button><button class="btn btn-ghost btn-sm danger" data-action="admin-delete-short" data-id="' + p.id + '">' + TZ.icon('trash', 14) + '</button></td></tr>';
      }).join('') : '<tr><td colspan="6">' + TZ.empty('edit', '没有匹配的简答题') + '</td></tr>') +
      '</tbody></table></div></div>',
      'short'
    );
  };

  A.openShortModal = function (id) {
    const p = id ? TZ.getShortAnswer(id) : null;
    const body = '<form data-form="admin-short" class="form-stack">' +
      '<input type="hidden" name="id" value="' + (p ? p.id : '') + '" />' +
      '<div class="field"><label class="label">题干</label><input class="input" name="title" value="' + TZ.esc(p ? p.title : '') + '" placeholder="例如 简述 vector 的扩容机制" /></div>' +
      '<div class="form-grid"><div class="field"><label class="label">难度</label><select class="select" name="difficulty">' + ['入门', '基础', '进阶'].map(function (d) { return '<option' + (p && p.difficulty === d ? ' selected' : '') + '>' + d + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">标签（逗号分隔）</label><input class="input" name="tags" value="' + TZ.esc(p ? (p.tags || []).join(', ') : '') + '" /></div>' +
      '<div class="field"><label class="label">满分</label><input class="input" value="100" disabled /></div>' +
      '<div class="field"><label class="label">状态</label><select class="select" name="status"><option value="published"' + (p && p.status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (p && p.status === 'draft' ? ' selected' : '') + '>草稿</option></select></div></div>' +
      '<div class="field"><label class="label">参考答案</label><textarea class="textarea" name="referenceAnswer" rows="5" placeholder="上架前必须填写">' + TZ.esc(p ? p.referenceAnswer : '') + '</textarea></div>' +
      '<div class="field"><label class="label">评分要点（每行一条）</label><textarea class="textarea" name="scoringRubric" rows="5" placeholder="例如：连续内存与 capacity 扩容机制（15 分）">' + TZ.esc(p ? (p.scoringRubric || []).join('\n') : '') + '</textarea></div>' +
      '<div class="field"><label class="label">判分关键词（原型模拟用，逗号分隔）</label><input class="input" name="keywords" value="' + TZ.esc(p ? (p.keywords || []).join(', ') : '') + '" /><span class="hint">原型以关键词覆盖度模拟 AI 判分；正式产品由 LLM 按参考答案与评分要点判分。</span></div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存简答题</button></div></form>';
    TZ.modal({ title: p ? '编辑简答题' : '新增简答题', body: body, size: 'modal-lg' });
  };

  /* ---------- 外链题 ---------- */
  A.external = function () {
    const q = TZ.state.admin.extSearch.trim().toLowerCase();
    const rows = TZ.db.externals.filter(function (p) { return !q || p.title.toLowerCase().indexOf(q) >= 0; });
    return TZ.layoutAdmin(
      TZ.pageHeader('外链题', '维护外部平台经典题入口，不复制题面。', '<button class="btn btn-primary" data-action="admin-add-external">' + TZ.icon('plus', 15) + ' 新增外链题</button>') +
      '<div class="card"><div class="table-toolbar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-ext-search" class="input" data-filter="admin-ext-search" placeholder="搜索外链题" value="' + TZ.esc(TZ.state.admin.extSearch) + '" /></div><span class="muted">' + rows.length + ' 道</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>题目</th><th>平台</th><th>难度</th><th>标签</th><th>状态</th><th>操作</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (p) {
        return '<tr><td><div class="admin-title-cell"><strong>' + TZ.esc(p.title) + '</strong><span>' + TZ.esc(p.url) + '</span></div></td><td>' + TZ.badge(p.platform, 'cyan') + '</td><td>' + TZ.difficultyBadge(p.difficulty) + '</td><td class="muted">' + TZ.esc(tagString(p.tags)) + '</td><td>' + TZ.statusBadge(p.status) + '</td>' +
          '<td class="row-actions"><a class="btn btn-ghost btn-sm" href="' + p.url + '" target="_blank" rel="noopener">' + TZ.icon('external', 14) + ' 打开</a><button class="btn btn-ghost btn-sm" data-action="admin-edit-external" data-id="' + p.id + '">' + TZ.icon('edit', 14) + ' 编辑</button><button class="btn btn-ghost btn-sm danger" data-action="admin-delete-external" data-id="' + p.id + '">' + TZ.icon('trash', 14) + '</button></td></tr>';
      }).join('') : '<tr><td colspan="6">' + TZ.empty('link', '没有匹配的外链题') + '</td></tr>') +
      '</tbody></table></div></div>',
      'external'
    );
  };

  A.openExternalModal = function (id) {
    const p = id ? TZ.getExternal(id) : null;
    const body = '<form data-form="admin-external" class="form-stack">' +
      '<input type="hidden" name="id" value="' + (p ? p.id : '') + '" />' +
      '<div class="field"><label class="label">题目名称</label><input class="input" name="title" value="' + TZ.esc(p ? p.title : '') + '" /></div>' +
      '<div class="form-grid"><div class="field"><label class="label">平台</label><select class="select" name="platform">' + ['Codeforces', '牛客', '洛谷', 'AtCoder'].map(function (x) { return '<option' + (p && p.platform === x ? ' selected' : '') + '>' + x + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">难度</label><select class="select" name="difficulty">' + ['入门', '基础', '进阶'].map(function (d) { return '<option' + (p && p.difficulty === d ? ' selected' : '') + '>' + d + '</option>'; }).join('') + '</select></div>' +
      '<div class="field"><label class="label">标签（逗号分隔）</label><input class="input" name="tags" value="' + TZ.esc(p ? (p.tags || []).join(', ') : '') + '" /></div>' +
      '<div class="field"><label class="label">状态</label><select class="select" name="status"><option value="published"' + (p && p.status === 'published' ? ' selected' : '') + '>上架</option><option value="draft"' + (p && p.status === 'draft' ? ' selected' : '') + '>草稿</option></select></div></div>' +
      '<div class="field"><label class="label">外部链接</label><input class="input" name="url" value="' + TZ.esc(p ? p.url : 'https://') + '" placeholder="https://..." /></div>' +
      '<div class="modal-actions"><button type="button" class="btn btn-outline" data-action="close-modal">取消</button><button type="submit" class="btn btn-primary">保存外链题</button></div></form>';
    TZ.modal({ title: p ? '编辑外链题' : '新增外链题', body: body, size: 'modal-lg' });
  };

  /* ---------- 用户管理 ---------- */
  A.users = function () {
    const q = TZ.state.admin.userSearch.trim().toLowerCase();
    const rows = TZ.db.users.filter(function (u) {
      return !q || (u.name + ' ' + (u.username || '') + ' ' + (u.phone || '') + ' ' + (u.school || '') + ' ' + (u.cfHandle || '')).toLowerCase().indexOf(q) >= 0;
    });
    const isSuper = TZ.isSuper();
    return TZ.layoutAdmin(
      TZ.pageHeader('用户管理', isSuper ? '查看用户信息、新增账号、启禁用与删除。' : '内容运营仅可查看用户信息，不可新增、启禁用或删除。', '<span class="badge badge-indigo">' + rows.length + ' 位用户</span>') +
      (!isSuper ? '<div class="info-strip"><span class="info-strip-icon">' + TZ.icon('info', 17) + '</span><div><strong>权限说明</strong><p>当前为内容运营角色，用户写操作、手动申报审核与角色管理仅超级管理员可操作。</p></div></div>' : '') +
      '<div class="card"><div class="table-toolbar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-user-search" class="input" data-filter="admin-user-search" placeholder="搜索用户名、昵称、学校或 CF Handle" value="' + TZ.esc(TZ.state.admin.userSearch) + '" /></div></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>用户</th><th>用户名</th><th>学校</th><th>CF Handle</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead><tbody>' +
      rows.map(function (u) {
        return '<tr><td><div class="lb-user">' + TZ.avatar(u, 32) + '<div class="lb-user-info"><strong>' + TZ.esc(u.name) + '</strong><span>' + u.id + '</span></div></div></td><td class="muted">' + TZ.esc(u.username || '—') + '</td><td class="muted">' + TZ.esc(u.school || '—') + '</td><td class="muted">' + TZ.esc(u.cfHandle || '未绑定') + '</td><td>' + TZ.statusBadge(u.status) + '</td><td class="muted">' + TZ.fmtDate(u.joinedAt, false) + '</td>' +
          '<td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="admin-view-user" data-id="' + u.id + '">查看</button>' +
          (isSuper ? '<button class="btn btn-ghost btn-sm ' + (u.status === 'active' ? 'danger' : '') + '" data-action="admin-toggle-user" data-id="' + u.id + '">' + (u.status === 'active' ? '禁用' : '启用') + '</button>' : '') +
          '</td></tr>';
      }).join('') + '</tbody></table></div></div>',
      'users'
    );
  };

  A.openUserModal = function (id) {
    const u = TZ.getUser(id);
    if (!u) return;
    const subs = TZ.db.submissions.filter(function (s) { return s.userId === id; });
    const ac = subs.filter(function (s) { return s.verdict === 'AC'; }).length;
    const wrong = TZ.db.wrongBook.filter(function (w) { return w.userId === id; }).length;
    const battles = TZ.db.battles.filter(function (b) { return b.userId === id; });
    const tasks = TZ.db.datagenTasks.filter(function (t) { return t.userId === id; });
    const cred = TZ.getLlmCredential(id);
    const me = TZ.getLeaderboard().find(function (r) { return r.userId === id; });
    const body = '<div class="user-detail"><div class="user-detail-head">' + TZ.avatar(u, 56) + '<div><h3>' + TZ.esc(u.name) + '</h3><p class="muted">@' + TZ.esc(u.username || '—') + ' · ' + TZ.esc(u.school || '未填写学校') + '</p></div>' + TZ.statusBadge(u.status) + '</div>' +
      '<div class="profile-stats compact-stats"><div><strong>' + (me ? me.rank : '-') + '</strong><span>本周排名</span></div><div><strong>' + ac + '</strong><span>累计 AC</span></div><div><strong>' + wrong + '</strong><span>错题</span></div><div><strong>' + battles.length + '</strong><span>对战</span></div></div>' +
      '<div class="meta-list"><div><span>用户 ID</span><strong>' + u.id + '</strong></div><div><span>用户名</span><strong>' + TZ.esc(u.username || '—') + '</strong></div><div><span>邮箱</span><strong>' + TZ.esc(u.email || '—') + '</strong></div><div><span>CF Handle</span><strong>' + TZ.esc(u.cfHandle || '未绑定') + '</strong></div><div><span>造数据任务</span><strong>' + tasks.length + '</strong></div><div><span>LLM 凭证</span><strong>' + (cred ? (cred.provider + ' · ' + (cred.status === 'valid' ? '有效' : '无效')) : '未配置') + '</strong></div><div><span>最近校验</span><strong>' + (cred ? (cred.status === 'valid' ? '通过' : '未通过') + ' · ' + TZ.fmtDate(cred.lastValidatedAt) : '—') + '</strong></div><div><span>注册时间</span><strong>' + TZ.fmtDate(u.joinedAt) + '</strong></div></div><p class="hint">后台仅展示凭证配置状态、服务商与最近校验时间，不展示 Key 明文或尾号，也不提供代填。</p></div>';
    TZ.modal({ title: '用户详情', body: body, size: 'modal-lg' });
  };

  /* ---------- 榜单管理 ---------- */
  function adminLeaderboardTabs(mode) {
    const btn = function (key, label) {
      return '<button type="button" class="lb-tab' + (mode === key ? ' active' : '') + '" data-action="admin-leaderboard-tab" data-mode="' + key + '">' + label + '</button>';
    };
    return '<div class="lb-tabs">' + btn('week', '周榜') + btn('all', '总榜') + '</div>';
  }

  A.leaderboard = function () {
    const mode = TZ.state.admin.leaderboardMode === 'all' ? 'all' : 'week';
    const isAll = mode === 'all';
    const isSuper = TZ.isSuper();
    const rows = TZ.getLeaderboard(mode);
    const manual = TZ.db.manualDeclarations.filter(function (m) { return isAll || m.week === TZ.WEEK; });
    return TZ.layoutAdmin(
      TZ.pageHeader('榜单管理', isAll ? '全部历史累计 · 站内历史首次 AC + CF 历史过题 + 手动申报累计。' : TZ.WEEK_LABEL + ' · 站内首次 AC + CF 同步 + 手动申报。',
        isSuper ? '<button class="btn btn-outline" data-action="admin-refresh-cf">' + TZ.icon('refresh', 15) + ' 刷新 CF 数据</button>' : '') +
      '<div class="lb-page-tabs">' + adminLeaderboardTabs(mode) + '</div>' +
      '<div class="admin-stat-row"><div class="card card-pad"><span>参与统计</span><strong>' + rows.length + '</strong><em>位选手</em></div><div class="card card-pad"><span>' + (isAll ? '历史申报' : '手动申报') + '</span><strong>' + manual.length + '</strong><em>条记录</em></div><div class="card card-pad"><span>最近同步</span><strong class="date">' + TZ.fmtDate(TZ.db.lastCfSync) + '</strong><em>CF</em></div></div>' +
      '<div class="card section"><div class="section-head card-pad-head"><div><h3>' + (isAll ? '总榜前 20' : '周榜前 20') + '</h3><div class="section-sub">' + (isAll ? '总分相同按站内累计降序；仍相同并列' : '总分相同按站内 AC 降序；仍相同并列') + '</div></div>' + (isAll ? '<span class="badge badge-amber">只读</span>' : '') + '</div><div class="table-wrap"><table class="table"><thead><tr><th>排名</th><th>选手</th><th>' + (isAll ? '站内累计' : '站内') + '</th><th>' + (isAll ? 'CF 累计' : 'CF') + '</th><th>' + (isAll ? '手动累计' : '手动') + '</th><th>总分</th></tr></thead><tbody>' +
      rows.slice(0, 20).map(function (r) {
        return '<tr><td><span class="rank ' + (r.rank <= 3 ? 'rank-' + r.rank : 'rank-plain') + '">' + r.rank + '</span></td><td><div class="lb-user">' + TZ.avatar(r.user, 30) + '<div class="lb-user-info"><strong>' + TZ.esc(r.user.name) + '</strong><span>' + TZ.esc(r.user.school || '') + '</span></div></div></td><td class="num">' + r.inSystem + '</td><td class="num">' + r.cf + '</td><td class="num">' + r.manual + '</td><td class="num"><strong>' + r.total + '</strong></td></tr>';
      }).join('') + '</tbody></table></div></div>' +
      '<div class="card section"><div class="section-head card-pad-head"><div><h3>手动申报记录</h3><div class="section-sub">' + (isAll ? '历史全部申报，按周累加计入总榜；未校验' : '未校验，仅用于演示周榜') + '</div></div><span class="badge badge-amber">' + manual.length + ' 条</span></div><div class="table-wrap"><table class="table"><thead><tr><th>用户</th><th>平台</th><th>平台账号</th><th>过题数</th><th>周次</th><th>申报时间</th><th>校验状态</th></tr></thead><tbody>' +
      (manual.length ? manual.map(function (m) {
        const u = TZ.getUser(m.userId);
        return '<tr><td>' + TZ.esc(u ? u.name : m.userId) + '</td><td>' + TZ.badge(m.platform, 'cyan') + '</td><td class="muted">' + TZ.esc(m.handle || '-') + '</td><td class="num">' + m.count + '</td><td class="muted">' + (m.week === TZ.WEEK ? '本周' : TZ.esc(m.week)) + '</td><td class="muted">' + TZ.fmtDate(m.createdAt) + '</td><td>' + TZ.badge('未校验', 'amber') + '</td></tr>';
      }).join('') : '<tr><td colspan="7">' + TZ.empty('trophy', '暂无手动申报') + '</td></tr>') +
      '</tbody></table></div></div>',
      'leaderboard'
    );
  };

  /* ---------- 造数据任务 ---------- */
  A.datagen = function () {
    const status = TZ.state.admin.taskStatus;
    const rows = TZ.db.datagenTasks.filter(function (t) {
      return status === 'all' || t.status === status;
    });
    return TZ.layoutAdmin(
      TZ.pageHeader('造数据任务', '查看全平台生成任务、失败原因与数据文件。', '<span class="badge badge-indigo">' + rows.length + ' 个任务</span>') +
      '<div class="card"><div class="table-toolbar"><select id="admin-task-status" class="select filter-select" data-filter="admin-task-status"><option value="all">全部状态</option><option value="success"' + (status === 'success' ? ' selected' : '') + '>成功</option><option value="failed"' + (status === 'failed' ? ' selected' : '') + '>失败</option><option value="generating"' + (status === 'generating' ? ' selected' : '') + '>LLM 构造中</option><option value="llm_data"' + (status === 'llm_data' ? ' selected' : '') + '>LLM 生成数据中</option><option value="running"' + (status === 'running' ? ' selected' : '') + '>生成中</option></select><span class="muted">失败任务不会自动重试</span></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>任务</th><th>用户</th><th>生成方式</th><th>状态</th><th>组数</th><th>创建时间</th><th>操作</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (t) {
        const u = TZ.getUser(t.userId);
        return '<tr><td><div class="admin-title-cell"><strong>' + TZ.esc(t.title) + '</strong><span>' + t.id + '</span></div></td><td class="muted">' + TZ.esc(u ? u.name : t.userId) + '</td><td>' + TZ.genModeBadge(t.mode) + '</td><td>' + TZ.statusBadge(t.status) + '</td><td class="num">' + t.groupCount + '</td><td class="muted">' + TZ.fmtDate(t.createdAt) + '</td>' +
          '<td class="row-actions"><button class="btn btn-soft btn-sm" data-action="admin-view-task" data-id="' + t.id + '">查看</button>' + (t.status === 'success' ? '<button class="btn btn-ghost btn-sm" data-action="download-all" data-id="' + t.id + '">' + TZ.icon('download', 14) + ' 下载</button>' : '') + '</td></tr>';
      }).join('') : '<tr><td colspan="7">' + TZ.empty('wand', '没有匹配的任务') + '</td></tr>') +
      '</tbody></table></div></div>',
      'datagen'
    );
  };

  A.openTaskModal = function (id) {
    const t = TZ.db.datagenTasks.find(function (x) { return x.id === id; });
    if (!t) return;
    const u = TZ.getUser(t.userId);
    const body = '<div class="task-detail"><div class="task-detail-head"><div><h3>' + TZ.esc(t.title) + '</h3><p class="muted">' + t.id + ' · ' + TZ.esc(u ? u.name : t.userId) + ' · ' + TZ.fmtDate(t.createdAt) + '</p></div><div class="cell-badges">' + TZ.genModeBadge(t.mode) + TZ.statusBadge(t.status) + '</div></div>' +
      (t.status === 'failed' ? '<div class="failure-card"><div class="failure-icon">' + TZ.icon('alert', 20) + '</div><div><strong>失败原因</strong><p>' + TZ.esc(t.failureReason) + '</p>' + (t.failureStage ? '<span class="hint">失败阶段：' + TZ.esc(TZ.failureStageText(t.failureStage)) + '</span>' : '') + '</div></div>' : '') +
      (t.status === 'generating' ? '<div class="info-strip"><span class="info-strip-icon">' + TZ.icon('sparkles', 17) + '</span><div><strong>LLM 构造中</strong><p>模型正在生成数据构造器，生成器代码不向选手展示。</p></div></div>' : '') +
      (t.status === 'llm_data' ? '<div class="info-strip"><span class="info-strip-icon">' + TZ.icon('sparkles', 17) + '</span><div><strong>LLM 生成数据中</strong><p>模型正在直接产出测试输入，平台将逐组校验格式与范围，校验通过后运行 std。</p></div></div>' : '') +
      '<div class="meta-list"><div><span>生成方式</span><strong>' + TZ.genModeText(t.mode) + '</strong></div>' + (t.mode !== 'random' ? '<div><span>LLM 服务商</span><strong>' + TZ.esc(t.llmProvider || '—') + '</strong></div>' : '') + '<div><span>数据组数</span><strong>' + t.groupCount + '</strong></div><div><span>时间 / 内存</span><strong>' + t.timeLimit + ' / ' + t.memoryLimit + '</strong></div><div><span>数据范围</span><strong>' + t.variables.map(function (v) { return v.name + ' ∈ [' + v.min + ', ' + v.max + ']'; }).join('；') + '</strong></div>' +
      (t.mode === 'llm_direct' ? '<div><span>数据校验</span><strong>' + (t.llmDataStatus === 'ready' ? '逐组通过' : t.llmDataStatus === 'invalid' ? '未通过' : '进行中') + '</strong></div>' : '') +
      '<div><span>完成时间</span><strong>' + (t.finishedAt ? TZ.fmtDate(t.finishedAt) : '—') + '</strong></div>' +
      (t.mode !== 'random' && t.specialRequirements ? '<div><span>特殊要求</span><strong>' + TZ.esc(t.specialRequirements) + '</strong></div>' : '') + '</div>' +
      (t.files.length ? '<div class="table-wrap section"><table class="table table-compact"><thead><tr><th>#</th><th>输入大小</th><th>输出大小</th><th>std 用时</th><th>状态</th><th>下载</th></tr></thead><tbody>' +
        t.files.map(function (f) {
          return '<tr><td>' + f.index + '</td><td class="muted">' + f.inputSize + ' B</td><td class="muted">' + f.outputSize + ' B</td><td class="muted">' + f.stdTime + ' ms</td><td>' + TZ.badge('OK', 'green') + '</td><td class="row-actions"><button class="btn btn-ghost btn-sm" data-action="download-group" data-id="' + t.id + '" data-index="' + f.index + '" data-kind="input">输入</button><button class="btn btn-ghost btn-sm" data-action="download-group" data-id="' + t.id + '" data-index="' + f.index + '" data-kind="output">输出</button></td></tr>';
        }).join('') + '</tbody></table></div>' : '') +
      '<details class="std-details"><summary>查看 std 代码</summary><pre><code>' + TZ.esc(t.stdCode) + '</code></pre></details>' +
      (t.mode === 'llm' ? '<p class="hint">LLM 生成的数据构造器不向选手展示，后台仅保留状态、失败原因与产出文件。</p>' : '') +
      (t.mode === 'llm_direct' ? '<p class="hint">LLM 直接生成输入数据，平台逐组校验格式与范围；std 始终由选手提供，校验通过后由 platform 运行 std 产出答案。</p>' : '') + '</div>';
    TZ.modal({ title: '🤖 造数据任务详情', body: body, size: 'modal-lg' });
  };

  /* ---------- 角色管理 ---------- */
  A.roles = function () {
    if (!TZ.isSuper()) {
      return TZ.layoutAdmin(TZ.empty('shield', '无访问权限', '角色管理仅超级管理员可访问。', '<a class="btn btn-primary" href="#/admin">返回工作台</a>'), 'roles');
    }
    const permissions = [
      ['内容维护', '学习路线、阶段、阶段内容'],
      ['题库管理', 'OJ 题目、客观题、简答题、外链题'],
      ['用户查看', '用户列表与详情'],
      ['用户管理', '新增、启停用、删除、重置密码'],
      ['榜单查看', '周榜与手动申报记录'],
      ['申报审核', '通过 / 拒绝手动申报'],
      ['造数据任务', '查看全平台任务'],
      ['操作日志', '查看后台操作记录'],
      ['角色管理', '查看角色与权限']
    ];
    const superYes = [true, true, true, true, true, true, true, true, true];
    const opYes = [true, true, true, false, true, false, true, true, false];
    const renderPerm = function (name, arr, tone) {
      return '<div class="perm-list">' + permissions.map(function (p, i) {
        return '<div class="perm-row ' + (arr[i] ? 'yes' : 'no') + '"><span class="perm-icon">' + TZ.icon(arr[i] ? 'check' : 'x', 15) + '</span><div><strong>' + p[0] + '</strong><span>' + p[1] + '</span></div></div>';
      }).join('') + '</div>';
    };
    return TZ.layoutAdmin(
      TZ.pageHeader('角色管理', '首期角色固定，不支持自定义角色与细粒度权限。', '<span class="badge badge-indigo">2 个角色</span>') +
      '<div class="grid grid-2">' +
      '<div class="card card-pad role-card"><div class="role-card-head"><div class="card-icon indigo">' + TZ.emojiIcon('🦊') + '</div><div><h3>超级管理员</h3><p class="muted">平台超管 · 全部权限</p></div><span class="badge badge-indigo">内置</span></div>' + renderPerm('super', superYes) + '</div>' +
      '<div class="card card-pad role-card"><div class="role-card-head"><div class="card-icon cyan">' + TZ.emojiIcon('🦊') + '</div><div><h3>内容运营</h3><p class="muted">内容与题库维护 · 只读运营数据</p></div><span class="badge badge-cyan">内置</span></div>' + renderPerm('operator', opYes) + '</div></div>' +
      '<div class="info-strip section"><span class="info-strip-icon">' + TZ.icon('info', 17) + '</span><div><strong>首期边界</strong><p>角色与权限固定，不支持自定义角色与细粒度权限；手动申报审核仅超级管理员可操作。</p></div></div>',
      'roles'
    );
  };

  /* ---------- 操作日志 ---------- */
  A.logs = function () {
    const q = TZ.state.admin.logSearch.trim().toLowerCase();
    const adminFilter = TZ.state.admin.logAdmin;
    const rows = TZ.db.logs.filter(function (l) {
      if (adminFilter !== 'all' && l.adminId !== adminFilter) return false;
      if (!q) return true;
      return (l.action + ' ' + l.target + ' ' + l.detail + ' ' + l.adminName).toLowerCase().indexOf(q) >= 0;
    });
    return TZ.layoutAdmin(
      TZ.pageHeader('操作日志', '记录后台关键操作，便于追踪内容变更。', '<span class="badge badge-indigo">' + rows.length + ' 条</span>') +
      '<div class="card"><div class="filter-bar table-filter-bar"><div class="search-box">' + TZ.icon('search', 16) + '<input id="admin-log-search" class="input" data-filter="admin-log-search" placeholder="搜索动作、对象或详情" value="' + TZ.esc(TZ.state.admin.logSearch) + '" /></div>' +
      '<select id="admin-log-admin" class="select filter-select" data-filter="admin-log-admin"><option value="all">全部操作人</option>' + TZ.db.admins.map(function (a) { return '<option value="' + a.id + '"' + (adminFilter === a.id ? ' selected' : '') + '>' + TZ.esc(a.name) + '</option>'; }).join('') + '</select></div>' +
      '<div class="table-wrap"><table class="table"><thead><tr><th>时间</th><th>操作人</th><th>角色</th><th>动作</th><th>对象</th><th>详情</th></tr></thead><tbody>' +
      (rows.length ? rows.map(function (l) {
        return '<tr><td class="muted">' + TZ.fmtDate(l.createdAt) + '</td><td>' + TZ.esc(l.adminName) + '</td><td>' + TZ.badge(l.role === 'super' ? '超级管理员' : '内容运营', l.role === 'super' ? 'indigo' : 'cyan') + '</td><td><strong>' + TZ.esc(l.action) + '</strong></td><td>' + TZ.esc(l.target) + '</td><td class="muted">' + TZ.esc(l.detail) + '</td></tr>';
      }).join('') : '<tr><td colspan="6">' + TZ.empty('history', '没有匹配的日志') + '</td></tr>') +
      '</tbody></table></div></div>',
      'logs'
    );
  };

  /* ---------- 后台登录 ---------- */
  A.login = function () {
    return '<div class="admin-login-page">' +
      '<div class="admin-login-side"><div class="admin-login-brand">' + TZ.brand(true) + '</div><h1>运营后台</h1><p>内容维护、题库管理与运营数据查看。首期后台不追求复杂权限，先把内容闭环跑通。</p>' +
      '<div class="admin-login-features"><span>' + TZ.emojiIcon('🍋') + ' 路线 / 阶段</span><span>' + TZ.emojiIcon('🍎') + ' 四类题库</span><span>' + TZ.emojiIcon('🍉') + ' 周榜数据</span><span>' + TZ.emojiIcon('🐹') + ' 操作日志</span></div></div>' +
      '<div class="admin-login-form-wrap"><div class="card card-pad admin-login-form"><div class="login-form-top"><span class="badge badge-indigo">运营入口</span><span class="muted">本地演示</span></div><h2>登录后台</h2><p class="muted">使用运营账号和密码登录，数据保存在本机浏览器。</p>' +
      '<form data-form="admin-login" class="form-stack"><div class="field"><label class="label" for="admin-username">用户名</label><input class="input" id="admin-username" name="username" value="admin" autocomplete="username" /></div>' +
      '<div class="field"><label class="label" for="admin-password">密码</label><input class="input" id="admin-password" name="password" type="password" value="admin" /></div>' +
      '<button class="btn btn-primary btn-lg btn-block" type="submit">登录后台</button></form>' +
      '<div class="demo-creds"><strong>演示账号</strong><span>admin / admin（超级管理员）</span><span>operator / operator（内容运营）</span></div>' +
      '<div class="login-foot"><a href="#/login">返回选手登录</a><span class="muted">本地演示环境</span></div></div></div></div>';
  };
})();
