/* ============================================================
 * 题阵 · 路由与全局交互
 * ============================================================ */
(function () {
  const TZ = window.TZ;
  const app = document.getElementById('app');
  let filterTimer = null;
  let lastHash = null;

  TZ._suppressScroll = false;
  TZ.battleTimer = null;

  /* ---------- 通用 ---------- */
  function toast(msg, type) { TZ.toast(msg, type); }
  function go(hash) { location.hash = hash; }
  function currentUser() { return TZ.currentUser(); }

  function isReferenced(problemId) {
    return TZ.db.routes.some(function (r) {
      return r.stages.some(function (s) { return s.problemIds.indexOf(problemId) >= 0; });
    });
  }

  function parseTags(str) {
    return String(str || '').split(/[,，、]/).map(function (s) { return s.trim(); }).filter(Boolean);
  }

  function setFilter(key, value) {
    switch (key) {
      case 'problem-search': TZ.state.problemFilters.q = value; break;
      case 'problem-type': TZ.state.problemFilters.type = value; break;
      case 'problem-difficulty': TZ.state.problemFilters.difficulty = value; break;
      case 'problem-tag': TZ.state.problemFilters.tag = value; break;
      case 'wrong-type': TZ.state.wrongType = value; break;
      case 'admin-route-search': TZ.state.admin.routeSearch = value; break;
      case 'admin-stage-route': TZ.state.admin.stageRoute = value; break;
      case 'admin-oj-search': TZ.state.admin.ojSearch = value; break;
      case 'admin-oj-difficulty': TZ.state.admin.ojDifficulty = value; break;
      case 'admin-oj-status': TZ.state.admin.ojStatus = value; break;
      case 'admin-obj-search': TZ.state.admin.objSearch = value; break;
      case 'admin-short-search': TZ.state.admin.shortSearch = value; break;
      case 'admin-short-status': TZ.state.admin.shortStatus = value; break;
      case 'admin-ext-search': TZ.state.admin.extSearch = value; break;
      case 'admin-user-search': TZ.state.admin.userSearch = value; break;
      case 'admin-task-status': TZ.state.admin.taskStatus = value; break;
      case 'admin-log-search': TZ.state.admin.logSearch = value; break;
      case 'admin-log-admin': TZ.state.admin.logAdmin = value; break;
    }
  }

  function scheduleRerender() {
    clearTimeout(filterTimer);
    filterTimer = setTimeout(function () { TZ.rerender(); }, 220);
  }

  /* ---------- 对战计时 ---------- */
  TZ.clearBattleTimer = function () {
    if (TZ.battleTimer) {
      clearInterval(TZ.battleTimer);
      TZ.battleTimer = null;
    }
  };

  TZ.startBattleTimer = function () {
    TZ.clearBattleTimer();
    const room = TZ.db.battleRoom;
    if (!room || room.status !== 'playing' || !room.timerEnd) return;
    const tick = function () {
      const el = document.getElementById('battle-timer');
      const remain = Math.max(0, Math.floor((new Date(room.timerEnd).getTime() - Date.now()) / 1000));
      if (el) el.textContent = String(Math.floor(remain / 60)).padStart(2, '0') + ':' + String(remain % 60).padStart(2, '0');
      if (remain <= 0) {
        TZ.clearBattleTimer();
        if (room.status === 'playing') TZ.customer.finishBattle('draw');
      }
    };
    tick();
    TZ.battleTimer = setInterval(tick, 1000);
  };

  /* ---------- 路由 ---------- */
  TZ.route = function () {
    const raw = (location.hash || '').replace(/^#/, '') || '/home';
    const isNewPage = lastHash !== raw;
    const pathPart = raw.split('?')[0];
    const parts = pathPart.split('/').filter(Boolean);
    const isAdmin = parts[0] === 'admin';
    const isAdminLogin = isAdmin && parts[1] === 'login';
    const isLogin = parts[0] === 'login';

    TZ.clearBattleTimer();

    if (isAdmin) {
      if (!isAdminLogin && !TZ.currentAdmin()) { location.hash = '#/admin/login'; return; }
      if (isAdminLogin && TZ.currentAdmin()) { location.hash = '#/admin'; return; }
    } else {
      if (!isLogin && !TZ.currentUser()) { location.hash = '#/login'; return; }
      if (isLogin && TZ.currentUser()) { location.hash = '#/home'; return; }
    }

    let html = '';
    let title = '题阵';

    if (parts[0] === 'login') {
      html = TZ.customer.login();
      title = '登录';
    } else if (parts[0] === 'admin') {
      if (parts[1] === 'login') {
        html = TZ.admin.login();
        title = '运营后台登录';
      } else if (!parts[1]) {
        html = TZ.admin.home();
        title = '运营工作台';
      } else if (parts[1] === 'routes') {
        html = TZ.admin.routes();
        title = '路线管理';
      } else if (parts[1] === 'stages') {
        html = TZ.admin.stages();
        title = '阶段管理';
      } else if (parts[1] === 'route' && parts[2] && parts[3] === 'stages') {
        html = TZ.admin.routeStages(parts[2]);
        title = '阶段管理';
      } else if (parts[1] === 'oj') {
        html = TZ.admin.oj();
        title = 'OJ 题库';
      } else if (parts[1] === 'objective') {
        html = TZ.admin.objective();
        title = '客观题';
      } else if (parts[1] === 'short') {
        html = TZ.admin.short();
        title = '简答题';
      } else if (parts[1] === 'external') {
        html = TZ.admin.external();
        title = '外链题';
      } else if (parts[1] === 'users') {
        html = TZ.admin.users();
        title = '用户管理';
      } else if (parts[1] === 'leaderboard') {
        html = TZ.admin.leaderboard();
        title = '榜单管理';
      } else if (parts[1] === 'datagen') {
        html = TZ.admin.datagen();
        title = '造数据任务';
      } else if (parts[1] === 'roles') {
        html = TZ.admin.roles();
        title = '角色管理';
      } else if (parts[1] === 'logs') {
        html = TZ.admin.logs();
        title = '操作日志';
      } else {
        html = TZ.layoutAdmin(TZ.empty('info', '页面不存在', '请检查后台地址。', '<a class="btn btn-primary" href="#/admin">返回工作台</a>'), 'home');
        title = '运营后台';
      }
    } else if (parts[0] === 'home') {
      html = TZ.customer.home(); title = '首页';
    } else if (parts[0] === 'routes') {
      html = TZ.customer.routes(); title = '学习路线';
    } else if (parts[0] === 'route' && parts[1]) {
      html = TZ.customer.route(parts[1]); title = '路线详情';
    } else if (parts[0] === 'stage' && parts[1]) {
      html = TZ.customer.stage(parts[1]); title = '阶段详情';
    } else if (parts[0] === 'problems') {
      html = TZ.customer.problems(); title = '题库';
    } else if (parts[0] === 'problem' && parts[1]) {
      html = TZ.customer.problem(parts[1]); title = '题目详情';
    } else if (parts[0] === 'leaderboard') {
      html = TZ.customer.leaderboard(); title = '跨平台周榜';
    } else if (parts[0] === 'battle' && parts[1] === 'room' && parts[2]) {
      html = TZ.customer.battleRoom(parts[2]); title = '对战房间';
    } else if (parts[0] === 'battle-records') {
      html = TZ.customer.battleRecords(); title = '对战记录';
    } else if (parts[0] === 'battle') {
      html = TZ.customer.battle(); title = '对战大厅';
    } else if (parts[0] === 'datagen' && parts[1] === 'tasks') {
      html = TZ.customer.datagenTasks(); title = '造数据任务';
    } else if (parts[0] === 'datagen' && parts[1] === 'task' && parts[2]) {
      html = TZ.customer.datagenTask(parts[2]); title = '造数据任务详情';
    } else if (parts[0] === 'datagen') {
      html = TZ.customer.datagen(); title = 'AI 造数据';
    } else if (parts[0] === 'records') {
      html = TZ.customer.records(); title = '学习记录';
    } else if (parts[0] === 'wrongbook') {
      html = TZ.customer.wrongbook(); title = '错题本';
    } else if (parts[0] === 'profile') {
      html = TZ.customer.profile(); title = '个人中心';
    } else if (parts[0] === 'xcpc') {
      html = TZ.customer.xcpc(); title = 'XCPC';
    } else {
      html = TZ.layoutC(TZ.empty('info', '页面不存在', '你访问的页面不存在。', '<a class="btn btn-primary" href="#/home">返回首页</a>'), '');
      title = '页面不存在';
    }

    app.innerHTML = html;
    if (isNewPage && app.classList) {
      app.classList.add('page-intro');
      setTimeout(function () { app.classList.remove('page-intro'); }, 700);
    }
    document.title = title + ' · 题阵';
    const suppress = TZ._suppressScroll;
    TZ._suppressScroll = false;
    if (!suppress) window.scrollTo(0, 0);
    lastHash = raw;
    TZ.startBattleTimer();
    if (TZ.state.scrollJudge) {
      TZ.state.scrollJudge = false;
      setTimeout(function () { TZ.scrollToId('judge-result'); }, 60);
    }
  };

  /* ---------- 点击交互 ---------- */
  document.addEventListener('click', function (e) {
    const el = e.target.closest('[data-action]');
    if (!el) {
      if (!e.target.closest('.user-chip-wrap')) {
        const m = document.getElementById('user-menu');
        if (m) m.classList.remove('open');
      }
      return;
    }
    const action = el.dataset.action;
    const id = el.dataset.id;
    // 弹窗内容也会命中遮罩的 data-action，不能拦截表单内按钮的默认提交行为。
    if (action === 'modal-overlay' && e.target !== el) return;
    e.preventDefault();

    switch (action) {
      /* 登录与账号 */
      case 'login-tab': {
        const tab = el.dataset.tab;
        document.querySelectorAll('[data-action="login-tab"]').forEach(function (btn) {
          const on = btn.dataset.tab === tab;
          btn.classList.toggle('active', on);
          btn.setAttribute('aria-selected', on ? 'true' : 'false');
        });
        document.querySelectorAll('[data-form="login"], [data-form="register"]').forEach(function (panel) {
          panel.hidden = panel.dataset.form !== tab;
        });
        const sub = document.getElementById('login-subtitle');
        if (sub) sub.textContent = tab === 'register'
          ? '注册后即登录，昵称将展示在周榜与错题本。'
          : '使用用户名和密码登录。';
        const first = document.querySelector('[data-form="' + tab + '"] .input');
        if (first) first.focus();
        break;
      }
      case 'demo-login': {
        TZ.db.currentUserId = 'u_demo';
        TZ.save();
        go('#/home');
        toast('已使用演示账号进入', 'success');
        break;
      }
      case 'logout': {
        TZ.db.currentUserId = null;
        TZ.save();
        go('#/login');
        toast('已退出登录');
        break;
      }
      case 'switch-account': TZ.customer.openAccountSwitch(); break;
      case 'use-account': {
        const u = TZ.getUser(id);
        if (!u) { toast('账号不存在', 'danger'); break; }
        TZ.db.currentUserId = u.id;
        TZ.save();
        TZ.closeModal();
        go('#/home');
        toast('已切换为 ' + u.name, 'success');
        break;
      }
      case 'reset-demo': {
        TZ.confirm('重置演示数据', '将清空本地浏览器中的所有演示数据，并恢复到初始演示状态。确定继续吗？', function () {
          TZ.reset();
          go('#/login');
          toast('演示数据已重置', 'success');
        });
        break;
      }

      /* 学习与题目 */
      case 'toggle-stage': {
        const user = currentUser();
        if (!user) break;
        TZ.toggleStage(user.id, id);
        const done = TZ.getProgress(user.id, id).completed;
        toast(done ? '已标记阶段完成' : '已取消完成标记', done ? 'success' : 'info');
        TZ.rerender();
        break;
      }
      case 'toggle-external': {
        const user = currentUser();
        if (!user) break;
        const done = TZ.isExternalDone(user.id, id);
        TZ.setExternalDone(user.id, id, !done, '手动标记');
        toast(done ? '已取消完成标记' : '已手动标记完成', done ? 'info' : 'success');
        TZ.rerender();
        break;
      }
      case 'cf-auto': {
        const user = currentUser();
        if (!user) break;
        TZ.setExternalDone(user.id, id, true, 'CF 自动识别');
        toast('CF 自动识别完成', 'success');
        TZ.rerender();
        break;
      }
      case 'random-problem': {
        const pool = TZ.db.problems.filter(function (p) { return p.status === 'published'; });
        if (!pool.length) { toast('题库暂无可练习题目', 'danger'); break; }
        const p = pool[Math.floor(Math.random() * pool.length)];
        go('#/problem/' + p.id);
        break;
      }
      case 'reset-code': {
        delete TZ.state.drafts[id + ':cpp'];
        delete TZ.state.drafts[id + ':py'];
        TZ.rerender();
        toast('已恢复代码模板');
        break;
      }
      case 'submit-oj': {
        const user = currentUser();
        if (!user) break;
        const editor = document.getElementById('code-editor');
        const langEl = document.getElementById('language-select');
        const code = editor ? editor.value : '';
        const lang = langEl ? langEl.value : 'cpp';
        if (!code.trim()) { toast('请先编写代码', 'danger'); break; }
        TZ.state.drafts[id + ':' + lang] = code;
        TZ.state.lastJudge = { problemId: id, loading: true };
        TZ.rerender();
        setTimeout(function () {
          const verdict = TZ.judgeCode(code);
          const first = !TZ.hasPriorAC(user.id, id);
          TZ.saveSubmission({
            id: TZ.uid('sub'),
            userId: user.id,
            problemId: id,
            type: 'oj',
            language: lang === 'py' ? 'Python 3' : 'C++17',
            code: code,
            verdict: verdict.verdict,
            time: verdict.time,
            memory: verdict.memory,
            detail: verdict.detail,
            createdAt: new Date().toISOString()
          });
          if (verdict.verdict === 'AC' && first) TZ.bumpInSystem(user.id);
          if (verdict.verdict !== 'AC') TZ.addWrong(user.id, id, verdict.verdict);
          TZ.state.lastJudge = {
            problemId: id,
            loading: false,
            verdict: verdict.verdict,
            detail: verdict.detail,
            time: verdict.time,
            memory: verdict.memory
          };
          TZ.state.scrollJudge = true;
          TZ.save();
          if (location.hash.indexOf('#/problem/' + id) >= 0) TZ.rerender();
          else TZ.toast('判题完成：' + verdict.verdict, verdict.verdict === 'AC' ? 'success' : 'danger');
          toast(verdict.verdict === 'AC' ? 'AC！通过全部测试点' : verdict.verdict + ' · ' + verdict.detail, verdict.verdict === 'AC' ? 'success' : 'danger');
        }, 950);
        break;
      }
      case 'select-option': {
        const user = currentUser();
        if (!user) break;
        const p = TZ.getObjective(id);
        if (!p || TZ.state.objectiveResults[id]) break;
        const key = el.dataset.key;
        let sel = (TZ.state.objectiveSelections[id] || []).slice();
        if (p.qtype === 'multiple') {
          if (sel.indexOf(key) >= 0) sel = sel.filter(function (x) { return x !== key; });
          else sel.push(key);
        } else {
          sel = [key];
        }
        TZ.state.objectiveSelections[id] = sel;
        TZ.rerender();
        break;
      }
      case 'submit-objective': {
        const user = currentUser();
        if (!user) break;
        const p = TZ.getObjective(id);
        const sel = (TZ.state.objectiveSelections[id] || []).slice().sort();
        if (!p || !sel.length) { toast('请先选择答案', 'danger'); break; }
        const correct = sel.length === p.answer.length && p.answer.every(function (a) { return sel.indexOf(a) >= 0; });
        TZ.state.objectiveResults[id] = { correct: correct, at: new Date().toISOString() };
        TZ.saveSubmission({
          id: TZ.uid('sub'),
          userId: user.id,
          problemId: id,
          type: 'objective',
          language: '-',
          code: '',
          verdict: correct ? 'AC' : 'WA',
          time: '-',
          memory: '-',
          detail: '选择 ' + sel.join('、') + '，正确答案 ' + p.answer.join('、'),
          createdAt: new Date().toISOString()
        });
        if (!correct) TZ.addWrong(user.id, id, 'WA');
        TZ.save();
        toast(correct ? '回答正确' : '回答错误，已加入错题本', correct ? 'success' : 'danger');
        TZ.rerender();
        break;
      }
      case 'retry-objective': {
        delete TZ.state.objectiveResults[id];
        delete TZ.state.objectiveSelections[id];
        TZ.rerender();
        break;
      }
      case 'submit-short': {
        const user = currentUser();
        if (!user) break;
        const p = TZ.getShortAnswer(id);
        if (!p) break;
        const editor = document.getElementById('short-answer-editor');
        const text = editor ? editor.value.trim() : '';
        if (!text) { toast('请先填写答案', 'danger'); break; }
        TZ.state.drafts[id] = text;
        TZ.state.shortResults[id] = { grading: true };
        TZ.rerender();
        setTimeout(function () {
          const res = TZ.gradeShortAnswer(p, text);
          if (!res.ok) {
            TZ.state.shortResults[id] = { grading: false, failed: true, message: res.message };
            TZ.rerender();
            toast(res.message, 'danger');
            return;
          }
          TZ.state.shortResults[id] = { grading: false, done: true, score: res.score, passed: res.passed, feedback: res.feedback };
          TZ.saveSubmission({
            id: TZ.uid('sub'),
            userId: user.id,
            problemId: id,
            type: 'short',
            language: '文本作答',
            code: '',
            verdict: res.passed ? 'AC' : 'WA',
            time: '-',
            memory: '-',
            detail: 'AI 判分 ' + res.score + ' 分',
            aiScore: res.score,
            aiFeedback: res.feedback,
            gradingStatus: 'graded',
            gradedAt: new Date().toISOString(),
            answerText: text,
            createdAt: new Date().toISOString()
          });
          if (!res.passed) TZ.addWrong(user.id, id, 'WA', { latestScore: res.score });
          TZ.save();
          if (location.hash.indexOf('#/problem/' + id) >= 0) TZ.rerender();
          toast(res.passed ? '判分完成：' + res.score + ' 分，通过' : '判分完成：' + res.score + ' 分，未通过，已加入错题本', res.passed ? 'success' : 'danger');
        }, 1200);
        break;
      }
      case 'retry-short': {
        delete TZ.state.shortResults[id];
        TZ.rerender();
        break;
      }
      case 'reset-problem-filters': {
        TZ.state.problemFilters = { q: '', type: 'all', difficulty: 'all', tag: 'all' };
        TZ.rerender();
        break;
      }

      /* 周榜 */
      case 'open-manual': TZ.customer.openManualModal(); break;
      case 'open-cf': TZ.customer.openCfModal(); break;
      case 'open-llm-credential': TZ.customer.openLlmCredentialModal(); break;
      case 'delete-llm-credential': {
        const user = currentUser();
        if (!user) break;
        if (TZ.hasRunningLlmTask(user.id)) {
          toast('有进行中的 LLM 造数据任务，任务结束前不能删除凭证', 'danger');
          break;
        }
        TZ.confirm('删除 LLM 凭证', '删除后两种 LLM 造数据模式将立即禁用，已保存的 API Key 会从本地清除。确定删除吗？', function () {
          TZ.db.llmCredentials = TZ.db.llmCredentials.filter(function (c) { return c.userId !== user.id; });
          TZ.save();
          TZ.addLog('删除 LLM 凭证', '个人中心', '用户 ' + user.name + ' 删除 LLM API Key 凭证');
          toast('LLM 凭证已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'refresh-cf': {
        TZ.db.lastCfSync = new Date().toISOString();
        TZ.save();
        toast('CF 模拟数据已刷新', 'success');
        TZ.rerender();
        break;
      }
      case 'toggle-leaderboard-all': {
        TZ.state.showAllLeaderboard = !TZ.state.showAllLeaderboard;
        TZ.rerender();
        break;
      }
      case 'leaderboard-tab': {
        TZ.state.leaderboardMode = el.dataset.mode === 'all' ? 'all' : 'week';
        TZ.state.showAllLeaderboard = false;
        TZ.rerender();
        break;
      }
      case 'admin-leaderboard-tab': {
        TZ.state.admin.leaderboardMode = el.dataset.mode === 'all' ? 'all' : 'week';
        TZ.rerender();
        break;
      }
      case 'delete-manual': {
        TZ.confirm('删除手动申报', '删除后本周该平台申报数据将从周榜中移除。', function () {
          TZ.deleteManual(id);
          toast('已删除手动申报', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'unbind-cf': {
        TZ.confirm('解绑 Codeforces', '解绑后历史 CF 数据保留，但不再更新。确定解绑吗？', function () {
          TZ.unbindCf(currentUser().id);
          toast('已解绑 Codeforces');
          TZ.rerender();
        });
        break;
      }

      /* 对战 */
      case 'create-room': {
        const user = currentUser();
        if (!user) break;
        const diff = document.getElementById('room-difficulty');
        const topic = document.getElementById('room-topic');
        const code = 'TZ-' + Math.random().toString(36).slice(2, 6).toUpperCase();
        TZ.db.battleRoom = {
          code: code,
          creatorId: user.id,
          opponentId: null,
          status: 'waiting',
          filter: { difficulty: diff ? diff.value : 'all', topic: topic ? topic.value : 'all' },
          createdAt: new Date().toISOString(),
          logs: []
        };
        TZ.save();
        go('#/battle/room/' + code);
        toast('房间已创建：' + code, 'success');
        break;
      }
      case 'join-room': {
        const user = currentUser();
        const input = document.getElementById('join-code');
        const code = input ? input.value.trim().toUpperCase() : '';
        if (!code) { toast('请输入房间码', 'danger'); break; }
        const room = TZ.db.battleRoom;
        if (!room || room.code.toUpperCase() !== code) { toast('房间不存在或已关闭', 'danger'); break; }
        if (room.creatorId === user.id) { toast('这是你创建的房间，等待对手加入', 'info'); go('#/battle/room/' + room.code); break; }
        if (room.status !== 'waiting') { toast('房间已开始或已结束', 'danger'); break; }
        room.opponentId = user.id;
        room.status = 'ready';
        TZ.save();
        go('#/battle/room/' + room.code);
        toast('已加入房间', 'success');
        break;
      }
      case 'join-current-room': {
        const user = currentUser();
        const room = TZ.db.battleRoom;
        const code = el.dataset.code;
        if (!room || room.code !== code) { toast('房间不存在或已关闭', 'danger'); break; }
        if (room.status !== 'waiting') { toast('房间已开始或已结束', 'danger'); break; }
        if (room.creatorId === user.id) { toast('这是你创建的房间', 'info'); break; }
        room.opponentId = user.id;
        room.status = 'ready';
        TZ.save();
        TZ.rerender();
        toast('已加入房间', 'success');
        break;
      }
      case 'simulate-opponent': {
        const room = TZ.db.battleRoom;
        if (!room || room.status !== 'waiting') break;
        const candidates = ['u_02', 'u_03', 'u_04', 'u_05'].filter(function (x) { return x !== room.creatorId; });
        room.opponentId = candidates[Math.floor(Math.random() * candidates.length)];
        room.status = 'ready';
        TZ.save();
        TZ.rerender();
        toast('对手已加入房间', 'success');
        break;
      }
      case 'start-battle': {
        const user = currentUser();
        const room = TZ.db.battleRoom;
        if (!room) break;
        if (room.creatorId !== user.id) { toast('只有房主可以开始对战', 'danger'); break; }
        let pool = TZ.db.problems.filter(function (p) {
          if (room.filter.difficulty !== 'all' && p.difficulty !== room.filter.difficulty) return false;
          if (room.filter.topic !== 'all' && (p.tags || []).indexOf(room.filter.topic) < 0) return false;
          return true;
        });
        if (!pool.length) pool = TZ.db.problems.slice();
        const p = pool[Math.floor(Math.random() * pool.length)];
        room.status = 'playing';
        room.problemId = p.id;
        room.startedAt = new Date().toISOString();
        room.timerEnd = new Date(Date.now() + 30 * 60 * 1000).toISOString();
        room.logs = [];
        room.result = null;
        TZ.state.battleDraft = '';
        TZ.state.battleJudge = null;
        TZ.save();
        TZ.rerender();
        toast('对战开始，先 AC 者胜', 'success');
        break;
      }
      case 'submit-battle': {
        const user = currentUser();
        const room = TZ.db.battleRoom;
        if (!room || room.status !== 'playing') break;
        const editor = document.getElementById('battle-code-editor');
        const code = editor ? editor.value : '';
        if (!code.trim()) { toast('请先编写代码', 'danger'); break; }
        TZ.state.battleDraft = code;
        const verdict = TZ.judgeCode(code);
        const first = !TZ.hasPriorAC(user.id, room.problemId);
        TZ.saveSubmission({
          id: TZ.uid('sub'),
          userId: user.id,
          problemId: room.problemId,
          type: 'oj',
          language: 'C++17',
          code: code,
          verdict: verdict.verdict,
          time: verdict.time,
          memory: verdict.memory,
          detail: verdict.detail,
          createdAt: new Date().toISOString()
        });
        room.logs = room.logs || [];
        room.logs.push({ verdict: verdict.verdict, text: verdict.detail, at: new Date().toISOString() });
        if (verdict.verdict === 'AC') {
          if (first) TZ.bumpInSystem(user.id);
          TZ.save();
          TZ.customer.finishBattle('win');
          toast('AC！你赢下了本局', 'success');
        } else {
          if (verdict.verdict !== 'AC') TZ.addWrong(user.id, room.problemId, verdict.verdict);
          TZ.state.battleJudge = verdict;
          TZ.save();
          TZ.rerender();
          toast(verdict.verdict + ' · ' + verdict.detail, 'danger');
        }
        break;
      }
      case 'simulate-opponent-ac': {
        const room = TZ.db.battleRoom;
        if (!room || room.status !== 'playing') break;
        room.logs = room.logs || [];
        room.logs.push({ verdict: 'AC', text: '对手通过本题', at: new Date().toISOString() });
        TZ.save();
        TZ.customer.finishBattle('loss');
        toast('对手率先 AC，本局结束', 'danger');
        break;
      }
      case 'simulate-timeout': {
        TZ.customer.finishBattle('draw');
        toast('时间结束，本局平局', 'info');
        break;
      }
      case 'quit-battle': {
        TZ.confirm('退出对战', '主动退出将立即判负，确定退出吗？', function () {
          TZ.customer.finishBattle('loss');
          toast('已退出对战，本局判负', 'danger');
        });
        break;
      }
      case 'copy-room': {
        const code = el.dataset.code;
        const url = location.href.split('#')[0] + '#/battle/room/' + code;
        TZ.copyText(url).then(function () { toast('邀请链接已复制', 'success'); });
        break;
      }
      case 'leave-room': {
        TZ.db.battleRoom = null;
        TZ.save();
        go('#/battle');
        toast('已离开房间');
        break;
      }
      case 'view-battle': TZ.customer.openBattleRecord(id); break;

      /* 造数据 */
      case 'add-var': {
        const rows = document.getElementById('var-rows');
        if (rows && TZ.varRow) rows.insertAdjacentHTML('beforeend', TZ.varRow({ name: '', type: 'int', min: 1, max: 100 }));
        break;
      }
      case 'remove-var': {
        const row = el.closest('.var-row');
        if (row) row.remove();
        break;
      }
      case 'download-group': {
        const task = TZ.db.datagenTasks.find(function (t) { return t.id === id; });
        if (!task) break;
        const file = task.files.find(function (f) { return f.index === Number(el.dataset.index); });
        if (!file) { toast('数据文件不存在', 'danger'); break; }
        const kind = el.dataset.kind;
        const content = kind === 'input' ? file.input : file.output;
        TZ.download(task.id + '-' + file.index + '.' + (kind === 'input' ? 'in' : 'out'), content);
        break;
      }
      case 'download-all': {
        const task = TZ.db.datagenTasks.find(function (t) { return t.id === id; });
        if (!task || !task.files.length) { toast('暂无可下载数据', 'danger'); break; }
        TZ.download(task.id + '-data.txt', TZ.bundleTask(task));
        toast('数据已打包下载', 'success');
        break;
      }

      /* 记录 */
      case 'records-tab': {
        TZ.state.recordsTab = el.dataset.tab;
        TZ.rerender();
        break;
      }
      case 'wrong-remove': {
        const user = currentUser();
        if (!user) break;
        TZ.confirm('移出错题本', '移出后该题不再出现在错题本中，确定继续吗？', function () {
          TZ.removeWrong(user.id, id);
          toast('已移出错题本', 'success');
          TZ.rerender();
        });
        break;
      }

      /* 个人中心 */
      case 'open-edit-profile': TZ.customer.openEditProfile(); break;
      case 'pick-avatar': {
        const input = document.getElementById('profile-avatar-file');
        if (input) input.click();
        break;
      }
      case 'remove-avatar':
        setProfileAvatarDraft('');
        toast('头像已移除，保存后生效', 'success');
        break;

      /* 后台 */
      case 'admin-add-route': TZ.admin.openRouteModal(); break;
      case 'admin-edit-route': TZ.admin.openRouteModal(id); break;
      case 'admin-delete-route': {
        const route = TZ.getRoute(id);
        if (!route) break;
        TZ.confirm('删除路线', '删除《' + route.title + '》会同时删除其阶段，题目不会被删除。确定继续吗？', function () {
          TZ.db.routes = TZ.db.routes.filter(function (r) { return r.id !== id; });
          TZ.addLog('删除路线', '学习路线', '删除《' + route.title + '》及其 ' + route.stages.length + ' 个阶段');
          toast('路线已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'admin-add-stage': TZ.admin.openStageModal(el.dataset.route); break;
      case 'admin-edit-stage': TZ.admin.openStageModal(el.dataset.route, id); break;
      case 'admin-add-course-row': {
        const wrap = document.getElementById('stage-course-rows');
        if (wrap) {
          wrap.insertAdjacentHTML('beforeend', TZ.admin.courseEditorRow());
          const rows = wrap.querySelectorAll('[data-course-row]');
          const last = rows[rows.length - 1];
          if (last) last.querySelector('input').focus();
        }
        break;
      }
      case 'admin-remove-course-row': {
        const row = el.closest('[data-course-row]');
        if (row) row.remove();
        break;
      }
      case 'admin-delete-stage': {
        const found = TZ.getStage(id);
        if (!found) break;
        TZ.confirm('删除阶段', '删除《' + found.stage.title + '》不会删除关联题目。确定继续吗？', function () {
          found.route.stages = found.route.stages.filter(function (s) { return s.id !== id; });
          TZ.addLog('删除阶段', '阶段管理', '删除《' + found.stage.title + '》');
          toast('阶段已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'admin-move-stage': {
        const found = TZ.getStage(id);
        if (!found) break;
        const stages = found.route.stages.slice().sort(function (a, b) { return a.sort - b.sort; });
        stages.forEach(function (s, i) { s.sort = i + 1; });
        const idx = stages.findIndex(function (s) { return s.id === id; });
        const j = el.dataset.dir === 'up' ? idx - 1 : idx + 1;
        if (j < 0 || j >= stages.length) break;
        const tmp = stages[idx].sort;
        stages[idx].sort = stages[j].sort;
        stages[j].sort = tmp;
        found.route.stages = stages;
        TZ.addLog('调整阶段顺序', '阶段管理', '移动《' + found.stage.title + '》');
        toast('顺序已调整');
        TZ.rerender();
        break;
      }
      case 'admin-add-oj': TZ.admin.openOjModal(); break;
      case 'admin-edit-oj': TZ.admin.openOjModal(id); break;
      case 'admin-toggle-oj': {
        const p = TZ.getOjProblem(id);
        if (!p) break;
        p.status = p.status === 'published' ? 'draft' : 'published';
        TZ.addLog(p.status === 'published' ? '上架题目' : '下架题目', 'OJ 题库', (p.status === 'published' ? '上架' : '下架') + '《' + p.title + '》');
        toast(p.status === 'published' ? '题目已上架' : '题目已下架', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-delete-oj': {
        const p = TZ.getOjProblem(id);
        if (!p) break;
        if (isReferenced(id)) { toast('该题已被阶段引用，请改为下架', 'danger'); break; }
        TZ.confirm('删除题目', '确定删除《' + p.title + '》吗？此操作不可撤销。', function () {
          TZ.db.problems = TZ.db.problems.filter(function (x) { return x.id !== id; });
          TZ.addLog('删除题目', 'OJ 题库', '删除《' + p.title + '》');
          toast('题目已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'admin-add-objective': TZ.admin.openObjectiveModal(); break;
      case 'admin-edit-objective': TZ.admin.openObjectiveModal(id); break;
      case 'admin-delete-objective': {
        const p = TZ.getObjective(id);
        if (!p) break;
        if (isReferenced(id)) { toast('该题已被阶段引用，请先解除引用', 'danger'); break; }
        TZ.confirm('删除客观题', '确定删除《' + p.title + '》吗？', function () {
          TZ.db.objectives = TZ.db.objectives.filter(function (x) { return x.id !== id; });
          TZ.addLog('删除客观题', '客观题', '删除《' + p.title + '》');
          toast('客观题已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'admin-add-short': TZ.admin.openShortModal(); break;
      case 'admin-edit-short': TZ.admin.openShortModal(id); break;
      case 'admin-delete-short': {
        const p = TZ.getShortAnswer(id);
        if (!p) break;
        if (isReferenced(id)) { toast('该题已被阶段引用，请先解除引用', 'danger'); break; }
        TZ.confirm('删除简答题', '确定删除《' + p.title + '》吗？', function () {
          TZ.db.shortAnswers = TZ.db.shortAnswers.filter(function (x) { return x.id !== id; });
          TZ.addLog('删除简答题', '简答题', '删除《' + p.title + '》');
          toast('简答题已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'admin-add-external': TZ.admin.openExternalModal(); break;
      case 'admin-edit-external': TZ.admin.openExternalModal(id); break;
      case 'admin-delete-external': {
        const p = TZ.getExternal(id);
        if (!p) break;
        if (isReferenced(id)) { toast('该题已被阶段引用，请先解除引用', 'danger'); break; }
        TZ.confirm('删除外链题', '确定删除《' + p.title + '》吗？', function () {
          TZ.db.externals = TZ.db.externals.filter(function (x) { return x.id !== id; });
          TZ.addLog('删除外链题', '外链题', '删除《' + p.title + '》');
          toast('外链题已删除', 'success');
          TZ.rerender();
        });
        break;
      }
      case 'admin-view-user': TZ.admin.openUserModal(id); break;
      case 'admin-toggle-user': {
        if (!TZ.isSuper()) { toast('仅超级管理员可启禁用用户', 'danger'); break; }
        const u = TZ.getUser(id);
        if (!u) break;
        u.status = u.status === 'active' ? 'disabled' : 'active';
        TZ.addLog(u.status === 'active' ? '启用用户' : '禁用用户', '用户管理', (u.status === 'active' ? '启用 ' : '禁用 ') + u.name);
        toast(u.status === 'active' ? '用户已启用' : '用户已禁用', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-refresh-cf': {
        if (!TZ.isSuper()) { toast('仅超级管理员可刷新 CF 数据', 'danger'); break; }
        TZ.db.lastCfSync = new Date().toISOString();
        TZ.addLog('刷新 CF 数据', '榜单管理', '同步 CF 周数据');
        toast('CF 数据已刷新', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-view-task': TZ.admin.openTaskModal(id); break;

      /* 布局 */
      case 'toggle-user-menu': {
        const menu = document.getElementById('user-menu');
        if (menu) menu.classList.toggle('open');
        break;
      }
      case 'toggle-nav': {
        const nav = document.getElementById('nav-links');
        if (nav) nav.classList.toggle('open');
        break;
      }
      case 'toggle-theme': {
        const root = document.documentElement;
        const dusk = root.getAttribute('data-theme') === 'dusk';
        const next = dusk ? 'light' : 'dusk';
        if (dusk) root.removeAttribute('data-theme');
        else root.setAttribute('data-theme', 'dusk');
        try { localStorage.setItem('tizhen_theme', next); } catch (err) {}
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
          const hint = next === 'dusk' ? '切换到灵动明亮风格' : '切换到暗淡忧郁风格';
          btn.setAttribute('title', hint);
          btn.setAttribute('aria-label', hint);
          const label = btn.querySelector('.theme-label');
          if (label) label.textContent = next === 'dusk' ? '明亮' : '暗淡';
        });
        root.classList.add('theme-switching');
        setTimeout(function () { root.classList.remove('theme-switching'); }, 340);
        break;
      }
      case 'toggle-admin-nav': {
        const side = document.getElementById('admin-sidebar');
        if (side) side.classList.toggle('open');
        break;
      }
      case 'admin-logout': {
        TZ.db.currentAdminId = null;
        TZ.save();
        go('#/admin/login');
        toast('已退出后台');
        break;
      }

      /* 弹窗 */
      case 'modal-overlay': {
        if (e.target === el) TZ.closeModal();
        break;
      }
      case 'close-modal': TZ.closeModal(); break;
      case 'confirm-yes': {
        const cb = TZ._confirmCb;
        TZ._confirmCb = null;
        TZ.closeModal();
        if (cb) cb();
        break;
      }
    }

    if (action !== 'toggle-user-menu' && !e.target.closest('.user-chip-wrap')) {
      const m = document.getElementById('user-menu');
      if (m) m.classList.remove('open');
    }
  });

  /* ---------- 头像上传（原型本地压缩） ---------- */
  function setProfileAvatarDraft(url) {
    const hidden = document.getElementById('profile-avatar-url');
    if (hidden) hidden.value = url || '';
    const preview = document.getElementById('profile-avatar-preview');
    const user = TZ.currentUser();
    if (preview && user) {
      preview.innerHTML = TZ.avatar({
        id: user.id,
        name: user.name,
        avatar: user.avatar,
        avatarUrl: url || ''
      }, 64);
    }
  }

  function handleProfileAvatarFile(file) {
    TZ.readAvatarFile(file, function (error, dataUrl) {
      if (error) {
        toast(error, 'danger');
        return;
      }
      setProfileAvatarDraft(dataUrl);
      toast('头像已就绪，保存后生效', 'success');
    });
  }

  /* ---------- 表单 ---------- */
  document.addEventListener('submit', function (e) {
    const form = e.target.closest('form[data-form]');
    if (!form) return;
    e.preventDefault();
    const type = form.dataset.form;
    const fd = new FormData(form);
    const data = Object.fromEntries(fd.entries());

    switch (type) {
      case 'login': {
        const username = String(data.username || '').trim();
        const password = String(data.password || '');
        if (!username) { toast('请输入用户名', 'danger'); break; }
        if (!password) { toast('请输入密码', 'danger'); break; }
        const user = TZ.db.users.find(function (u) {
          return String(u.username || '').toLowerCase() === username.toLowerCase();
        });
        if (!user || user.password !== password) { toast('用户名或密码错误', 'danger'); break; }
        if (user.status === 'disabled') { toast('该账号已被禁用', 'danger'); break; }
        TZ.db.currentUserId = user.id;
        TZ.save();
        go('#/home');
        toast('欢迎，' + user.name, 'success');
        break;
      }
      case 'register': {
        const username = String(data.username || '').trim();
        const nickname = String(data.nickname || '').trim();
        const password = String(data.password || '');
        const confirm = String(data.confirm || '');
        if (!/^[A-Za-z0-9_]{3,20}$/.test(username)) { toast('用户名需为 3–20 位字母、数字或下划线', 'danger'); break; }
        if (!nickname) { toast('请输入昵称', 'danger'); break; }
        if (nickname.length > 20) { toast('昵称不能超过 20 个字符', 'danger'); break; }
        if (password.length < 6) { toast('密码至少 6 位', 'danger'); break; }
        if (password !== confirm) { toast('两次输入的密码不一致', 'danger'); break; }
        const exists = TZ.db.users.some(function (u) {
          return String(u.username || '').toLowerCase() === username.toLowerCase();
        });
        if (exists) { toast('该用户名已被注册', 'danger'); break; }
        const user = {
          id: 'u_' + Date.now().toString(36),
          username: username,
          password: password,
          name: nickname,
          avatar: nickname.charAt(0) || '选',
          school: '',
          phone: '',
          email: '',
          cfHandle: '',
          cfVerified: false,
          status: 'active',
          joinedAt: new Date().toISOString()
        };
        TZ.db.users.push(user);
        TZ.db.leaderboard.push({ userId: user.id, inSystem: 0, cf: 0 });
        TZ.db.currentUserId = user.id;
        TZ.save();
        go('#/home');
        toast('注册成功，欢迎，' + user.name, 'success');
        break;
      }
      case 'admin-login': {
        const username = String(data.username || '').trim();
        const password = String(data.password || '');
        const admin = TZ.db.admins.find(function (a) { return a.username === username && a.password === password; });
        if (!admin) { toast('账号或密码错误', 'danger'); break; }
        TZ.db.currentAdminId = admin.id;
        admin.lastLoginAt = new Date().toISOString();
        TZ.save();
        TZ.addLog('登录后台', '系统', (admin.role === 'super' ? '超级管理员' : '内容运营') + '登录');
        go('#/admin');
        toast('欢迎，' + admin.name, 'success');
        break;
      }
      case 'manual': {
        const user = currentUser();
        if (!user) break;
        const platform = String(data.platform || '');
        const handle = String(data.handle || '').trim();
        const count = Number(data.count);
        if (!handle) { toast('请输入平台账号', 'danger'); break; }
        if (isNaN(count) || count < 0 || count > 999) { toast('过题数需在 0–999 之间', 'danger'); break; }
        TZ.saveManual(user.id, platform, handle, count);
        TZ.closeModal();
        toast('手动申报已保存（未校验）', 'success');
        TZ.rerender();
        break;
      }
      case 'cf': {
        const user = currentUser();
        if (!user) break;
        const res = TZ.bindCf(user.id, data.handle);
        if (!res.ok) { toast(res.message, 'danger'); break; }
        TZ.closeModal();
        toast('绑定成功，本周 CF 过题 ' + res.count + ' 道', 'success');
        TZ.rerender();
        break;
      }
      case 'llm-credential': {
        const user = currentUser();
        if (!user) break;
        const provider = String(data.provider || '');
        const inputKey = String(data.key || '').trim();
        const model = String(data.model || '').trim().slice(0, 60);
        const existing = TZ.getLlmCredential(user.id);
        const key = inputKey || (existing ? existing.key : '');
        if (!key) { toast('请填写 API Key', 'danger'); break; }
        if (existing && !inputKey && provider !== existing.provider) { toast('更换服务商时需重新填写 API Key', 'danger'); break; }
        if (TZ.hasRunningLlmTask(user.id)) { toast('有进行中的 LLM 造数据任务，任务结束前不能替换凭证', 'danger'); break; }
        const res = TZ.validateLlmKey(provider, key);
        if (!res.ok) { toast('凭证校验失败：' + res.message, 'danger'); break; }
        const now = new Date().toISOString();
        if (existing) {
          existing.provider = provider;
          existing.key = key;
          existing.model = model;
          existing.status = 'valid';
          existing.lastValidatedAt = now;
          existing.updatedAt = now;
        } else {
          TZ.db.counters.credential++;
          TZ.db.llmCredentials.push({
            id: 'llm_' + TZ.db.counters.credential,
            userId: user.id,
            provider: provider,
            model: model,
            key: key,
            status: 'valid',
            lastValidatedAt: now,
            createdAt: now,
            updatedAt: now
          });
        }
        TZ.save();
        TZ.addLog(existing ? '更换 LLM 凭证' : '配置 LLM 凭证', '个人中心', '用户 ' + user.name + ' 配置 ' + provider + ' 凭证（已校验通过）');
        TZ.closeModal();
        toast('凭证校验通过并已保存', 'success');
        TZ.rerender();
        break;
      }
      case 'profile': {
        const user = currentUser();
        if (!user) break;
        const name = String(data.name || '').trim();
        if (!name) { toast('昵称不能为空', 'danger'); break; }
        user.name = name.slice(0, 20);
        user.avatar = user.name.slice(0, 1);
        const avatarUrl = String(data.avatarUrl || '');
        if (avatarUrl) user.avatarUrl = avatarUrl;
        else delete user.avatarUrl;
        user.school = String(data.school || '').trim().slice(0, 40);
        user.email = String(data.email || '').trim();
        TZ.save();
        TZ.closeModal();
        toast('个人资料已更新', 'success');
        TZ.rerender();
        break;
      }
      case 'datagen': {
        const user = currentUser();
        if (!user) break;
        const title = String(data.title || '').trim();
        const statement = String(data.statement || '').trim();
        const stdCode = String(data.stdCode || '');
        if (!title) { toast('请填写任务名称', 'danger'); break; }
        if (!statement) { toast('请填写题目描述', 'danger'); break; }
        if (!stdCode.trim()) { toast('请填写 std 代码', 'danger'); break; }
        const rows = Array.prototype.slice.call(form.querySelectorAll('.var-row'));
        const variables = rows.map(function (row) {
          return {
            name: row.querySelector('[name="var-name"]').value.trim(),
            type: row.querySelector('[name="var-type"]').value,
            min: Number(row.querySelector('[name="var-min"]').value || 0),
            max: Number(row.querySelector('[name="var-max"]').value || 100)
          };
        }).filter(function (v) { return v.name; });
        if (!variables.length) { toast('请至少配置一个数据变量', 'danger'); break; }
        const mode = data.mode === 'llm' ? 'llm' : data.mode === 'llm_direct' ? 'llm_direct' : 'random';
        const specialRequirements = mode === 'random' ? '' : String(data.specialRequirements || '').trim().slice(0, 500);
        TZ.state.datagenMode = mode;
        let llmCred = null;
        if (mode !== 'random') {
          llmCred = TZ.getLlmCredential(user.id);
          if (!llmCred || llmCred.status !== 'valid') { toast('尚未配置有效的 LLM 凭证，请先到个人中心配置', 'danger'); break; }
          const recheck = TZ.validateLlmKey(llmCred.provider, llmCred.key);
          if (!recheck.ok) {
            llmCred.status = 'invalid';
            llmCred.lastValidatedAt = new Date().toISOString();
            TZ.save();
            toast('凭证复检失败：' + recheck.message + '，请更新凭证后重试', 'danger');
            TZ.rerender();
            break;
          }
          if (TZ.hasRunningLlmTask(user.id)) { toast('同一时间只能有 1 个进行中的 LLM 造数据任务，请等待当前任务结束', 'danger'); break; }
        }
        const task = {
          id: TZ.uid('task'),
          userId: user.id,
          title: title,
          status: mode === 'random' ? 'running' : mode === 'llm' ? 'generating' : 'llm_data',
          groupCount: Math.max(1, Math.min(30, Number(data.groupCount) || 10)),
          createdAt: new Date().toISOString(),
          finishedAt: null,
          timeLimit: String(data.timeLimit || '1s'),
          memoryLimit: String(data.memoryLimit || '256MB'),
          statement: statement,
          inputFormat: String(data.inputFormat || ''),
          outputFormat: String(data.outputFormat || ''),
          variables: variables,
          stdCode: stdCode,
          files: [],
          mode: mode,
          llmProvider: llmCred ? llmCred.provider : '',
          llmCredentialId: llmCred ? llmCred.id : '',
          specialRequirements: specialRequirements,
          generatorStatus: mode === 'llm' ? 'building' : 'not_applicable',
          llmDataStatus: mode === 'llm_direct' ? 'generating' : 'not_applicable',
          failureStage: ''
        };
        TZ.db.datagenTasks.unshift(task);
        TZ.save();
        go('#/datagen/task/' + task.id);
        toast(mode === 'llm' ? '任务已提交，LLM 正在构造数据生成器' : mode === 'llm_direct' ? '任务已提交，LLM 正在直接生成数据' : '任务已提交，正在生成数据', 'info');

        const finishTask = function (t) {
          if (!t) return;
          if (t.stdCode.indexOf('// TLE') >= 0 || t.title.indexOf('失败') >= 0) {
            t.status = 'failed';
            t.finishedAt = new Date().toISOString();
            t.failureStage = 'std';
            t.failureReason = 'std 在第 7 组运行超时（超过 1s），任务已终止。';
            t.files = [];
          } else if (t.mode === 'llm' && (t.specialRequirements || '').indexOf('模拟失败') >= 0) {
            t.status = 'failed';
            t.finishedAt = new Date().toISOString();
            t.failureStage = 'generator';
            t.failureReason = 'LLM 生成的数据构造器未通过可执行性校验（第 3 组输出格式不匹配），任务已终止。';
            t.files = [];
          } else if (t.mode === 'llm_direct' && (t.specialRequirements || '').indexOf('模拟失败') >= 0) {
            t.status = 'failed';
            t.finishedAt = new Date().toISOString();
            t.failureStage = 'llm_data';
            t.llmDataStatus = 'invalid';
            t.failureReason = 'LLM 直接生成的数据未通过格式/范围校验（第 3 组：变量 n 超出 [1, 20]），任务已终止。';
            t.files = [];
          } else {
            t.status = 'success';
            t.finishedAt = new Date().toISOString();
            t.failureStage = '';
            if (t.mode === 'llm_direct') t.llmDataStatus = 'ready';
            t.files = TZ.makeDataFiles(t);
          }
          TZ.save();
          if (location.hash.indexOf('#/datagen/task/' + t.id) >= 0) {
            TZ.rerender();
            toast(t.status === 'success' ? '数据生成完成' : '任务失败：' + t.failureReason, t.status === 'success' ? 'success' : 'danger');
          }
        };

        const startGeneration = function () {
          const t = TZ.db.datagenTasks.find(function (x) { return x.id === task.id; });
          if (!t) return;
          const expected = t.mode === 'llm' ? 'generating' : 'llm_data';
          if (t.status !== expected) return;
          const cred = TZ.getLlmCredential(t.userId);
          let credError = '';
          if (!cred) {
            credError = '凭证已被删除';
          } else {
            const chk = TZ.validateLlmKey(cred.provider, cred.key);
            if (!chk.ok) credError = chk.message;
          }
          if (!credError && (t.specialRequirements || '').indexOf('模拟 Key 失效') >= 0) {
            credError = 'API Key 已过期或余额不足';
          }
          if (credError) {
            if (cred) {
              cred.status = 'invalid';
              cred.lastValidatedAt = new Date().toISOString();
            }
            t.status = 'failed';
            t.finishedAt = new Date().toISOString();
            t.failureStage = 'credential';
            t.failureReason = 'LLM 凭证复检失败（' + credError + '），任务已终止。请更新凭证后手动重新提交。';
            t.files = [];
            TZ.save();
            if (location.hash.indexOf('#/datagen/task/' + t.id) >= 0) {
              TZ.rerender();
              toast('任务失败：' + t.failureReason, 'danger');
            }
            return;
          }
          if ((t.specialRequirements || '').indexOf('模拟失败') >= 0) {
            finishTask(t);
            return;
          }
          t.status = 'running';
          if (t.mode === 'llm') t.generatorStatus = 'ready';
          if (t.mode === 'llm_direct') t.llmDataStatus = 'ready';
          TZ.save();
          if (location.hash.indexOf('#/datagen/task/' + t.id) >= 0) {
            TZ.rerender();
            toast(t.mode === 'llm' ? '数据构造器已就绪，开始生成数据' : '数据已通过校验，开始运行 std', 'info');
          }
          setTimeout(function () {
            const cur = TZ.db.datagenTasks.find(function (x) { return x.id === task.id; });
            if (cur && cur.status === 'running') finishTask(cur);
          }, 1400);
        };

        if (mode === 'llm' || mode === 'llm_direct') {
          setTimeout(startGeneration, 1500);
        } else {
          setTimeout(function () {
            const t = TZ.db.datagenTasks.find(function (x) { return x.id === task.id; });
            if (t && t.status === 'running') finishTask(t);
          }, 1600);
        }
        break;
      }
      case 'admin-route': {
        const id = String(data.id || '');
        const title = String(data.title || '').trim();
        if (!title) { toast('路线标题不能为空', 'danger'); break; }
        if (id) {
          const r = TZ.getRoute(id);
          if (!r) break;
          r.title = title;
          r.subtitle = String(data.subtitle || '');
          r.level = String(data.level || '入门');
          r.color = String(data.color || 'indigo');
          r.estimated = String(data.estimated || '');
          r.sort = Number(data.sort) || 1;
          r.status = String(data.status || 'published');
          r.description = String(data.description || '');
          TZ.addLog('编辑路线', '学习路线', '更新《' + r.title + '》');
        } else {
          TZ.db.counters.route++;
          const r = {
            id: 'r' + TZ.db.counters.route,
            title: title,
            subtitle: String(data.subtitle || ''),
            description: String(data.description || ''),
            level: String(data.level || '入门'),
            color: String(data.color || 'indigo'),
            icon: 'route',
            estimated: String(data.estimated || ''),
            status: String(data.status || 'published'),
            sort: Number(data.sort) || TZ.db.routes.length + 1,
            stages: []
          };
          TZ.db.routes.push(r);
          TZ.addLog('新增路线', '学习路线', '创建《' + r.title + '》');
        }
        TZ.closeModal();
        toast('路线已保存', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-stage': {
        const routeId = String(data.routeId || '');
        const id = String(data.id || '');
        const route = TZ.getRoute(routeId);
        if (!route) break;
        const title = String(data.title || '').trim();
        if (!title) { toast('阶段标题不能为空', 'danger'); break; }
        const problemIds = fd.getAll('problemIds');
        const courseRows = Array.prototype.slice.call(form.querySelectorAll('[data-course-row]'));
        const courses = [];
        let courseError = '';
        for (let ci = 0; ci < courseRows.length; ci++) {
          const row = courseRows[ci];
          const courseTitle = String((row.querySelector('[name="courseTitle"]') || {}).value || '').trim();
          const courseUrlRaw = String((row.querySelector('[name="courseUrl"]') || {}).value || '').trim();
          if (!courseTitle && !courseUrlRaw) continue;
          if (!courseTitle) { courseError = '课程标题不能为空'; break; }
          if (!courseUrlRaw) { courseError = '课程《' + courseTitle + '》缺少 B 站链接'; break; }
          const courseUrl = TZ.normalizeBiliUrl(courseUrlRaw);
          if (!courseUrl) { courseError = '课程《' + courseTitle + '》的 B 站链接无效'; break; }
          courses.push({
            id: row.dataset.courseId || ('c' + (++TZ.db.counters.course)),
            title: courseTitle,
            url: courseUrl,
            duration: String((row.querySelector('[name="courseDuration"]') || {}).value || '').trim(),
            source: String((row.querySelector('[name="courseSource"]') || {}).value || '').trim()
          });
        }
        if (courseError) { toast(courseError, 'danger'); break; }
        if (id) {
          const s = route.stages.find(function (x) { return x.id === id; });
          if (!s) break;
          s.title = title;
          s.summary = String(data.summary || '');
          s.duration = String(data.duration || '');
          s.sort = Number(data.sort) || s.sort;
          s.status = String(data.status || 'published');
          s.content = String(data.content || '');
          s.problemIds = problemIds;
          s.courses = courses;
          TZ.addLog('编辑阶段', '阶段管理', '更新《' + s.title + '》');
        } else {
          TZ.db.counters.stage++;
          const s = {
            id: 's' + TZ.db.counters.stage,
            title: title,
            summary: String(data.summary || ''),
            duration: String(data.duration || ''),
            status: String(data.status || 'published'),
            sort: Number(data.sort) || route.stages.length + 1,
            problemIds: problemIds,
            courses: courses,
            content: String(data.content || '')
          };
          route.stages.push(s);
          TZ.addLog('新增阶段', '阶段管理', '在《' + route.title + '》中创建《' + s.title + '》');
        }
        TZ.closeModal();
        toast('阶段已保存', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-oj': {
        const id = String(data.id || '');
        const title = String(data.title || '').trim();
        if (!title) { toast('题目名称不能为空', 'danger'); break; }
        const tags = parseTags(data.tags);
        if (id) {
          const p = TZ.getOjProblem(id);
          if (!p) break;
          p.title = title;
          p.difficulty = String(data.difficulty || '入门');
          p.tags = tags;
          p.timeLimit = String(data.timeLimit || '1s');
          p.memoryLimit = String(data.memoryLimit || '256MB');
          p.status = String(data.status || 'published');
          p.statement = String(data.statement || '');
          p.inputFormat = String(data.inputFormat || '');
          p.outputFormat = String(data.outputFormat || '');
          p.samples = [{ input: String(data.sampleInput || ''), output: String(data.sampleOutput || '') }];
          p.testData = String(data.testData || '');
          TZ.addLog('编辑题目', 'OJ 题库', '更新《' + p.title + '》');
        } else {
          TZ.db.counters.problem++;
          const p = {
            id: 'p' + TZ.db.counters.problem,
            type: 'oj',
            title: title,
            difficulty: String(data.difficulty || '入门'),
            tags: tags,
            source: '站内原创',
            status: String(data.status || 'published'),
            statement: String(data.statement || ''),
            inputFormat: String(data.inputFormat || ''),
            outputFormat: String(data.outputFormat || ''),
            samples: [{ input: String(data.sampleInput || ''), output: String(data.sampleOutput || '') }],
            timeLimit: String(data.timeLimit || '1s'),
            memoryLimit: String(data.memoryLimit || '256MB'),
            accepted: 0,
            submissions: 0,
            stageIds: [],
            testData: String(data.testData || '')
          };
          TZ.db.problems.push(p);
          TZ.addLog('新增题目', 'OJ 题库', '创建《' + p.title + '》');
        }
        TZ.closeModal();
        toast('题目已保存', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-objective': {
        const id = String(data.id || '');
        const title = String(data.title || '').trim();
        if (!title) { toast('题干不能为空', 'danger'); break; }
        const options = ['A', 'B', 'C', 'D'].map(function (k) {
          return { key: k, text: String(data['option' + k] || '') };
        }).filter(function (o) { return o.text; });
        const answer = String(data.answer || '').toUpperCase().split(/[,，\s]+/).filter(Boolean);
        if (!options.length) { toast('请至少填写一个选项', 'danger'); break; }
        if (!answer.length) { toast('请填写正确答案', 'danger'); break; }
        const payload = {
          qtype: String(data.qtype || 'single'),
          title: title,
          difficulty: String(data.difficulty || '入门'),
          tags: parseTags(data.tags),
          status: String(data.status || 'published'),
          options: options,
          answer: answer,
          explanation: String(data.explanation || '')
        };
        if (id) {
          const p = TZ.getObjective(id);
          if (!p) break;
          Object.assign(p, payload);
          TZ.addLog('编辑客观题', '客观题', '更新《' + p.title + '》');
        } else {
          TZ.db.counters.objective++;
          const p = Object.assign({
            id: 'q' + TZ.db.counters.objective,
            type: 'objective',
            source: '站内原创',
            stageIds: []
          }, payload);
          TZ.db.objectives.push(p);
          TZ.addLog('新增客观题', '客观题', '创建《' + p.title + '》');
        }
        TZ.closeModal();
        toast('客观题已保存', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-short': {
        const id = String(data.id || '');
        const title = String(data.title || '').trim();
        if (!title) { toast('题干不能为空', 'danger'); break; }
        const referenceAnswer = String(data.referenceAnswer || '').trim();
        const scoringRubric = String(data.scoringRubric || '').split(/\n+/).map(function (s) { return s.trim(); }).filter(Boolean);
        const status = String(data.status || 'published');
        if (status === 'published' && (!referenceAnswer || !scoringRubric.length)) {
          toast('上架前须配置参考答案与评分要点', 'danger');
          break;
        }
        const keywords = String(data.keywords || '').split(/[,，\s]+/).map(function (s) { return s.trim(); }).filter(Boolean);
        const payload = {
          title: title,
          difficulty: String(data.difficulty || '入门'),
          tags: parseTags(data.tags),
          status: status,
          maxScore: 100,
          referenceAnswer: referenceAnswer,
          scoringRubric: scoringRubric,
          keywords: keywords
        };
        if (id) {
          const p = TZ.getShortAnswer(id);
          if (!p) break;
          Object.assign(p, payload);
          TZ.addLog('编辑简答题', '简答题', '更新《' + p.title + '》');
        } else {
          TZ.db.counters.short++;
          const p = Object.assign({
            id: 'sa' + TZ.db.counters.short,
            type: 'short',
            source: '站内原创',
            stageIds: []
          }, payload);
          TZ.db.shortAnswers.push(p);
          TZ.addLog('新增简答题', '简答题', '创建《' + p.title + '》');
        }
        TZ.closeModal();
        toast('简答题已保存', 'success');
        TZ.rerender();
        break;
      }
      case 'admin-external': {
        const id = String(data.id || '');
        const title = String(data.title || '').trim();
        const url = String(data.url || '').trim();
        if (!title) { toast('题目名称不能为空', 'danger'); break; }
        if (!/^https?:\/\//.test(url)) { toast('请输入合法的 http(s) 链接', 'danger'); break; }
        const payload = {
          title: title,
          platform: String(data.platform || 'Codeforces'),
          url: url,
          difficulty: String(data.difficulty || '入门'),
          tags: parseTags(data.tags),
          status: String(data.status || 'published'),
          source: String(data.platform || 'Codeforces')
        };
        if (id) {
          const p = TZ.getExternal(id);
          if (!p) break;
          Object.assign(p, payload);
          TZ.addLog('编辑外链题', '外链题', '更新《' + p.title + '》');
        } else {
          TZ.db.counters.external++;
          const p = Object.assign({
            id: 'e' + TZ.db.counters.external,
            type: 'external',
            stageIds: []
          }, payload);
          TZ.db.externals.push(p);
          TZ.addLog('新增外链题', '外链题', '创建《' + p.title + '》');
        }
        TZ.closeModal();
        toast('外链题已保存', 'success');
        TZ.rerender();
        break;
      }
    }
  });

  /* ---------- 输入与筛选 ---------- */
  document.addEventListener('input', function (e) {
    const el = e.target;
    if (el.dataset.draftKey) {
      TZ.state.drafts[el.dataset.draftKey] = el.value;
      return;
    }
    if (el.dataset.battleDraft) {
      TZ.state.battleDraft = el.value;
      return;
    }
    if (el.dataset.filter && el.tagName !== 'SELECT') {
      setFilter(el.dataset.filter, el.value);
      scheduleRerender();
    }
  });

  document.addEventListener('change', function (e) {
    const el = e.target;
    if (el.id === 'profile-avatar-file') {
      const file = el.files && el.files[0];
      if (file) handleProfileAvatarFile(file);
      el.value = '';
      return;
    }
    if (el.id === 'language-select') {
      TZ.state.languages[el.dataset.problem] = el.value;
      TZ.rerender();
      return;
    }
    if (el.dataset.filter) {
      setFilter(el.dataset.filter, el.value);
      clearTimeout(filterTimer);
      TZ.rerender();
      return;
    }
    if (el.name === 'mode' && el.closest('[data-datagen-mode-group]')) {
      TZ.state.datagenMode = el.value === 'llm' ? 'llm' : el.value === 'llm_direct' ? 'llm_direct' : 'random';
      const field = document.getElementById('llm-requirements-field');
      const hint = document.getElementById('datagen-mode-hint');
      if (field) field.hidden = TZ.state.datagenMode === 'random';
      if (hint) hint.textContent = TZ.state.datagenMode === 'llm'
        ? 'LLM 会结合题面、数据范围与特殊要求构造生成器；构造器代码不展示。'
        : TZ.state.datagenMode === 'llm_direct'
          ? 'LLM 直接产出约 10 组输入数据，平台逐组校验格式与范围后再运行 std；不生成构造器。'
          : '按数据范围随机生成，稳定且耗时更短。';
      const submitBtn = document.getElementById('datagen-submit-btn');
      if (submitBtn) {
        submitBtn.innerHTML = TZ.emojiIcon('🤖') + ' 提交生成任务';
      }
    }
  });

  /* ---------- 头像拖拽上传 ---------- */
  document.addEventListener('dragover', function (e) {
    const zone = e.target.closest('[data-avatar-drop]');
    if (!zone) return;
    e.preventDefault();
    zone.classList.add('drag-over');
  });

  document.addEventListener('dragleave', function (e) {
    const zone = e.target.closest('[data-avatar-drop]');
    if (zone && !zone.contains(e.relatedTarget)) zone.classList.remove('drag-over');
  });

  document.addEventListener('drop', function (e) {
    const zone = e.target.closest('[data-avatar-drop]');
    if (!zone) return;
    e.preventDefault();
    zone.classList.remove('drag-over');
    const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
    if (file) handleProfileAvatarFile(file);
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      TZ.closeModal();
      const m = document.getElementById('user-menu');
      if (m) m.classList.remove('open');
    }
  });

  /* ---------- 启动 ---------- */
  window.addEventListener('hashchange', TZ.route);
  window.addEventListener('resize', function () {
    // 预留：移动端导航由 CSS 控制
  });

  TZ.route();
})();
