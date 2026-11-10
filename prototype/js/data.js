/* ============================================================
 * 题阵 · 交互原型数据层
 * 纯前端 mock：localStorage 持久化，无服务端逻辑
 * ============================================================ */
(function () {
  const DB_KEY = 'tizhen_proto_v1';
  const TZ = (window.TZ = window.TZ || {});

  TZ.WEEK = '2026-W46';
  TZ.WEEK_LABEL = '2026 第 46 周 · 11/09–11/15';

  function D(m, d, h, min) {
    return new Date(2026, m - 1, d, h || 0, min || 0, 0, 0).toISOString();
  }

  function makeFiles(count) {
    return Array.from({ length: count }, function (_, i) {
      const a = i + 1;
      const b = (i + 2) * 3;
      const input = a + ' ' + b + '\n';
      const output = a + b + '\n';
      return {
        index: i + 1,
        input: input,
        output: output,
        inputSize: input.length + 12,
        outputSize: output.length + 4,
        stdTime: 11 + i * 5,
        status: 'OK'
      };
    });
  }

  const users = [
    { id: 'u_demo', name: '演示选手', school: '示例大学', avatar: '演', phone: '13800000000', email: 'demo@tizhen.dev', cfHandle: 'demo_cf', cfVerified: true, status: 'active', joinedAt: D(9, 1, 10, 0) },
    { id: 'u_02', name: '林深', school: 'A 大学', avatar: '林', phone: '13800000002', email: 'linshen@tizhen.dev', cfHandle: 'tourist', cfVerified: true, status: 'active', joinedAt: D(9, 2, 11, 0) },
    { id: 'u_03', name: '周野', school: 'B 大学', avatar: '周', phone: '13800000003', email: 'zhouye@tizhen.dev', cfHandle: 'Benq', cfVerified: true, status: 'active', joinedAt: D(9, 3, 12, 0) },
    { id: 'u_04', name: '苏晚', school: 'C 大学', avatar: '苏', phone: '13800000004', email: 'suwan@tizhen.dev', cfHandle: 'neal', cfVerified: true, status: 'active', joinedAt: D(9, 4, 13, 0) },
    { id: 'u_05', name: '陈一', school: 'D 大学', avatar: '陈', phone: '13800000005', email: 'chenyi@tizhen.dev', cfHandle: 'ecnerwala', cfVerified: true, status: 'active', joinedAt: D(9, 5, 14, 0) },
    { id: 'u_06', name: '顾北', school: 'E 大学', avatar: '顾', phone: '13800000006', email: 'gubei@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 6, 15, 0) },
    { id: 'u_07', name: '江离', school: 'F 大学', avatar: '江', phone: '13800000007', email: 'jiangli@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 7, 16, 0) },
    { id: 'u_08', name: '许知', school: 'G 大学', avatar: '许', phone: '13800000008', email: 'xuzhi@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 8, 17, 0) },
    { id: 'u_09', name: '沈舟', school: 'H 大学', avatar: '沈', phone: '13800000009', email: 'shenzhou@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 9, 18, 0) },
    { id: 'u_10', name: '陆星', school: 'I 大学', avatar: '陆', phone: '13800000010', email: 'luxing@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 10, 9, 0) },
    { id: 'u_11', name: '白露', school: 'J 大学', avatar: '白', phone: '13800000011', email: 'bailu@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 11, 10, 0) },
    { id: 'u_12', name: '程野', school: 'K 大学', avatar: '程', phone: '13800000012', email: 'chengye@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 12, 11, 0) },
    { id: 'u_13', name: '韩川', school: 'L 大学', avatar: '韩', phone: '13800000013', email: 'hanchuan@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 13, 12, 0) },
    { id: 'u_14', name: '叶青', school: 'M 大学', avatar: '叶', phone: '13800000014', email: 'yeqing@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 14, 13, 0) },
    { id: 'u_15', name: '宋辞', school: 'N 大学', avatar: '宋', phone: '13800000015', email: 'songci@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 15, 14, 0) },
    { id: 'u_16', name: '唐宁', school: 'O 大学', avatar: '唐', phone: '13800000016', email: 'tangning@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 16, 15, 0) },
    { id: 'u_17', name: '方圆', school: 'P 大学', avatar: '方', phone: '13800000017', email: 'fangyuan@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 17, 16, 0) },
    { id: 'u_18', name: '夏木', school: 'Q 大学', avatar: '夏', phone: '13800000018', email: 'xiamu@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 18, 17, 0) },
    { id: 'u_19', name: '洛河', school: 'R 大学', avatar: '洛', phone: '13800000019', email: 'luohe@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 19, 18, 0) },
    { id: 'u_20', name: '时雨', school: 'S 大学', avatar: '时', phone: '13800000020', email: 'shiyu@tizhen.dev', cfHandle: '', cfVerified: false, status: 'active', joinedAt: D(9, 20, 9, 0) },
    { id: 'u_21', name: '初九', school: 'T 大学', avatar: '初', phone: '13800000021', email: 'chujiu@tizhen.dev', cfHandle: '', cfVerified: false, status: 'disabled', joinedAt: D(9, 21, 10, 0) }
  ];

  /* 演示账号：用户名取邮箱前缀，统一密码 123456；u_demo 即 demo / 123456 */
  users.forEach(function (u) {
    if (!u.username) u.username = (u.email || '').split('@')[0] || u.id;
    if (!u.password) u.password = '123456';
  });

  /* 预置 LLM 服务商：原型模拟，不发起真实请求 */
  TZ.LLM_PROVIDERS = ['DeepSeek', '通义千问', '智谱 AI', 'OpenAI'];

  /* 选手 LLM 造数据凭证；原型把演示 Key 存在本机 localStorage，页面只展示脱敏尾号 */
  const llmCredentials = [
    {
      id: 'llm_01',
      userId: 'u_demo',
      provider: 'DeepSeek',
      model: 'deepseek-chat',
      key: 'sk-demo-tizhen-9f3a',
      status: 'valid',
      lastValidatedAt: D(11, 9, 9, 30),
      createdAt: D(11, 9, 9, 20),
      updatedAt: D(11, 9, 9, 30)
    }
  ];

  const routes = [
    {
      id: 'r1',
      title: 'C++ 与 STL 基础',
      subtitle: '从语法到常用容器，打牢竞赛代码基础',
      description: '面向刚接触程序设计竞赛的选手，覆盖 C++ 基础语法、STL 常用容器与算法，配合站内练习与 Codeforces 经典题，建立稳定的编码手感。',
      level: '入门',
      color: 'indigo',
      icon: 'code',
      estimated: '约 6 小时',
      status: 'published',
      sort: 1,
      stages: [
        {
          id: 's1',
          title: 'C++ 基础语法与输入输出',
          summary: '快速回顾变量、循环、函数与 IO 加速，写出第一份稳定的竞赛代码。',
          duration: '60 分钟',
          status: 'published',
          sort: 1,
          problemIds: ['p1', 'q1', 'e1'],
          courses: [
            { id: 'c1', title: '【一听就懂】C++ 基本输入和输出', url: 'https://www.bilibili.com/video/BV14z421m7eU', duration: '16:39' },
            { id: 'c2', title: '【C 与 C++ 编程入门】第 3 课：基础语法、输入输出与变量', url: 'https://www.bilibili.com/video/BV1QCWp6yEC3', duration: '32:29' }
          ],
          content: '<h3>学习目标</h3><p>掌握竞赛代码的基本结构、输入输出方式和常见坑点，能够独立完成一道 A+B 难度的题目。</p><h4>核心知识点</h4><ul><li>头文件、命名空间与 <code>main</code> 函数</li><li><code>cin/cout</code> 与 <code>scanf/printf</code> 的选择</li><li>关闭同步流：<code>ios::sync_with_stdio(false); cin.tie(nullptr);</code></li><li>常见数据类型范围与溢出</li></ul><h4>代码模板</h4><pre><code>#include &lt;bits/stdc++.h&gt;\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    long long a, b;\n    cin &gt;&gt; a &gt;&gt; b;\n    cout &lt;&lt; a + b &lt;&lt; \'\\n\';\n    return 0;\n}</code></pre><h4>练习建议</h4><p>先完成站内 A+B，再尝试 Codeforces 4A，最后用客观题检查对 vector 复杂度的理解。</p><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        },
        {
          id: 's2',
          title: 'STL 容器与复杂度',
          summary: '掌握 vector、map、set 的适用场景，学会用复杂度分析选择容器。',
          duration: '90 分钟',
          status: 'published',
          sort: 2,
          problemIds: ['p2', 'q2', 'e2'],
          courses: [
            { id: 'c3', title: 'C++ STL 容器系统讲解', url: 'https://www.bilibili.com/video/BV1fY4Bz4Eyn', duration: '32:50' },
            { id: 'c4', title: '27 C++ STL 容器 unordered_map', url: 'https://www.bilibili.com/video/BV19f421U7Uc', duration: '17:18' }
          ],
          content: '<h3>学习目标</h3><p>能够根据操作类型和数据规模，在 vector、map、set、unordered_map 之间做出合理选择。</p><h4>核心知识点</h4><ul><li>vector：随机访问、尾部插入均摊 O(1)，中间插入 O(n)</li><li>map / set：有序，单次操作 O(log n)</li><li>unordered_map：平均 O(1)，需要注意哈希冲突与常数</li><li>迭代器失效的常见场景</li></ul><h4>复杂度速查</h4><pre><code>vector  push_back 均摊 O(1)   随机访问 O(1)\nmap     插入/查找 O(log n)   有序遍历\nset     插入/查找 O(log n)   自动去重\nunordered_map 平均 O(1)      最坏 O(n)</code></pre><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        },
        {
          id: 's3',
          title: '迭代器与常用算法',
          summary: '熟悉 sort、lower_bound、reverse 等算法，写出更简洁的代码。',
          duration: '80 分钟',
          status: 'published',
          sort: 3,
          problemIds: ['p3', 'q3', 'e3'],
          courses: [
            { id: 'c5', title: '【一听就懂】C++ 中迭代器的用法', url: 'https://www.bilibili.com/video/BV1ExSRYEEFy', duration: '49:33' },
            { id: 'c6', title: '19. 什么是迭代器', url: 'https://www.bilibili.com/video/BV1WA4m1N7BF', duration: '16:44' }
          ],
          content: '<h3>学习目标</h3><p>熟练使用 STL 算法减少手写代码量，并理解迭代器区间左闭右开的约定。</p><h4>核心知识点</h4><ul><li><code>sort(begin, end)</code> 与自定义比较器</li><li><code>lower_bound/upper_bound</code> 的返回值与边界</li><li><code>reverse</code>、<code>unique</code>、<code>accumulate</code></li><li>lambda 表达式捕获与排序</li></ul><h4>示例</h4><pre><code>vector&lt;int&gt; a = {3, 1, 4, 1, 5};\nsort(a.begin(), a.end());\nauto it = lower_bound(a.begin(), a.end(), 4);\ncout &lt;&lt; (it - a.begin()) &lt;&lt; \'\\n\';</code></pre><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        },
        {
          id: 's4',
          title: '基础综合练习',
          summary: '用四道综合题把语法、容器和算法串起来，完成第一轮手速训练。',
          duration: '120 分钟',
          status: 'published',
          sort: 4,
          problemIds: ['p4', 'q5', 'e4'],
          courses: [
            { id: 'c7', title: 'C++ 算法竞赛基础课程', url: 'https://www.bilibili.com/video/BV1xu41177kV', duration: '246:21' },
            { id: 'c8', title: '算法竞赛的 C++ 语言入门知识体系概述', url: 'https://www.bilibili.com/video/BV1bv411p7U5', duration: '17:00' }
          ],
          content: '<h3>学习目标</h3><p>在限时环境中独立完成基础题，形成读题、建模、编码、调试的完整流程。</p><h4>训练方式</h4><ul><li>每题先花 2 分钟读题并写下关键约束</li><li>编码完成后至少手算一组边界数据</li><li>提交后记录错误原因，进入错题本</li></ul><h4>本阶段题目</h4><p>站内最短路、二分复杂度客观题，以及牛客基础题单。完成后可以进入「动态规划专项」。</p><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        }
      ]
    },
    {
      id: 'r2',
      title: '动态规划专项',
      subtitle: '从线性 DP 到区间 DP，建立状态设计思维',
      description: '围绕竞赛中最常见的动态规划模型，按「线性 DP → 背包 → 区间 DP → 综合」推进，每阶段配套站内题与外部经典题。',
      level: '进阶',
      color: 'cyan',
      icon: 'layers',
      estimated: '约 8 小时',
      status: 'published',
      sort: 2,
      stages: [
        {
          id: 's5',
          title: '线性 DP 与最大子段和',
          summary: '理解状态、转移、边界三要素，掌握最大子段和与 LIS。',
          duration: '100 分钟',
          status: 'published',
          sort: 1,
          problemIds: ['p2', 'q3', 'e5'],
          courses: [
            { id: 'c9', title: '0x11 动态规划之线性 DP：4 道经典例题', url: 'https://www.bilibili.com/video/BV1vy421874o', duration: '29:24' },
            { id: 'c10', title: '动态规划 · 最大子段和', url: 'https://www.bilibili.com/video/BV1WG4y1x73P', duration: '19:53' }
          ],
          content: '<h3>学习目标</h3><p>能够把「以 i 结尾」的状态定义转化为转移方程，并正确处理负数和空段。</p><h4>核心模型</h4><ul><li>最大子段和：<code>f[i] = max(a[i], f[i-1] + a[i])</code></li><li>最长上升子序列：O(n²) 与 O(n log n) 两种做法</li><li>状态初始化与边界处理</li></ul><h4>思考题</h4><p>如果要求子段长度至少为 k，状态应该如何调整？</p><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        },
        {
          id: 's6',
          title: '01 背包与滚动数组',
          summary: '掌握 01 背包的状态定义、倒序枚举与空间优化。',
          duration: '110 分钟',
          status: 'published',
          sort: 2,
          problemIds: ['p3', 'q6', 'e1'],
          courses: [
            { id: 'c11', title: '01 背包一维数组优化', url: 'https://www.bilibili.com/video/BV1QU4y1g7ky', duration: '13:51' },
            { id: 'c12', title: '01 背包问题 · 滚动数组的自我修养', url: 'https://www.bilibili.com/video/BV13S7czSEWs', duration: '11:35' }
          ],
          content: '<h3>学习目标</h3><p>理解「选或不选」的决策过程，能够写出 01 背包的一维滚动数组实现。</p><h4>核心转移</h4><pre><code>for (int i = 1; i &lt;= n; i++)\n    for (int j = V; j &gt;= w[i]; j--)\n        f[j] = max(f[j], f[j - w[i]] + v[i]);</code></pre><h4>常见错误</h4><ul><li>内层循环正序，导致同一物品被重复选择</li><li>忽略容量上界与下标越界</li></ul><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        },
        {
          id: 's7',
          title: '区间 DP 与石子合并',
          summary: '从小区间推向大区间，掌握环形与链式区间 DP 的基本套路。',
          duration: '120 分钟',
          status: 'published',
          sort: 3,
          problemIds: ['p5', 'q1', 'e2'],
          courses: [
            { id: 'c13', title: '手动模拟石子合并 · 讲透区间动态规划', url: 'https://www.bilibili.com/video/BV1ov4y1y7et', duration: '18:17' },
            { id: 'c14', title: '合并石子：区间 DP 入门经典题型', url: 'https://www.bilibili.com/video/BV1Nq1nBmEST', duration: '05:40' }
          ],
          content: '<h3>学习目标</h3><p>掌握区间 DP 的枚举顺序：先枚举区间长度，再枚举左端点，最后枚举分割点。</p><h4>核心模板</h4><pre><code>for (int len = 2; len &lt;= n; len++)\n    for (int l = 1; l + len - 1 &lt;= n; l++) {\n        int r = l + len - 1;\n        for (int k = l; k &lt; r; k++)\n            f[l][r] = min(f[l][r], f[l][k] + f[k+1][r] + sum(l, r));\n    }</code></pre><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        },
        {
          id: 's8',
          title: '图论与 DP 综合',
          summary: '把拓扑排序、最短路与 DP 结合，完成本路线的综合挑战。',
          duration: '130 分钟',
          status: 'published',
          sort: 4,
          problemIds: ['p6', 'q4', 'e3'],
          courses: [
            { id: 'c15', title: '数据结构 · 拓扑排序和逆拓扑排序', url: 'https://www.bilibili.com/video/BV17d4y1H7HY', duration: '04:55' },
            { id: 'c16', title: '图论 · 最短路 Floyd 求传递闭包', url: 'https://www.bilibili.com/video/BV1924y1Y7gL', duration: '05:42' }
          ],
          content: '<h3>学习目标</h3><p>能够在 DAG 上按拓扑序进行 DP，并识别题目中的阶段划分。</p><h4>核心知识点</h4><ul><li>拓扑排序与入度数组</li><li>DAG 上的最长路 / 方案数 DP</li><li>最短路与 DP 的区别与联系</li></ul><h4>综合任务</h4><p>完成站内拓扑排序题，并在 CF 158A 上练习快速读题与边界处理。</p><p class="source-note">内容来源：公开学习资料整理，仅用于本地演示。</p>'
        }
      ]
    }
  ];

  const problems = [
    {
      id: 'p1', type: 'oj', title: 'A + B Problem', difficulty: '入门',
      tags: ['C++ 与 STL 基础', '模拟'], source: '站内原创', status: 'published',
      statement: '<p>输入两个整数 <code>a</code> 和 <code>b</code>，输出它们的和。</p>',
      inputFormat: '<p>一行两个整数 <code>a</code> 和 <code>b</code>，满足 <code>|a|, |b| ≤ 10<sup>9</sup></code>。</p>',
      outputFormat: '<p>输出一个整数，表示 <code>a + b</code>。</p>',
      samples: [{ input: '1 2', output: '3' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 128, submissions: 260, stageIds: ['s1'],
      testData: '1 2\n3 4\n-5 8\n1000000000 1000000000'
    },
    {
      id: 'p2', type: 'oj', title: '最大子段和', difficulty: '基础',
      tags: ['C++ 与 STL 基础', '动态规划'], source: '站内原创', status: 'published',
      statement: '<p>给定长度为 <code>n</code> 的整数序列，选择一个非空连续子段，使其元素之和最大，输出这个最大和。</p>',
      inputFormat: '<p>第一行一个整数 <code>n</code>（<code>1 ≤ n ≤ 2×10<sup>5</sup></code>）。</p><p>第二行 <code>n</code> 个整数，绝对值不超过 <code>10<sup>9</sup></code>。</p>',
      outputFormat: '<p>输出一个整数，表示最大子段和。</p>',
      samples: [{ input: '5\n-2 1 -3 4 -1', output: '4' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 96, submissions: 210, stageIds: ['s2', 's5'],
      testData: '5\n-2 1 -3 4 -1\n3\n-1 -2 -3\n6\n1 2 3 4 5 6'
    },
    {
      id: 'p3', type: 'oj', title: '01 背包', difficulty: '基础',
      tags: ['动态规划', '背包'], source: '站内原创', status: 'published',
      statement: '<p>有 <code>n</code> 件物品和一个容量为 <code>V</code> 的背包。第 <code>i</code> 件物品的重量是 <code>w<sub>i</sub></code>，价值是 <code>v<sub>i</sub></code>，每件物品只能选一次。求背包能装下的最大价值。</p>',
      inputFormat: '<p>第一行两个整数 <code>n, V</code>（<code>1 ≤ n ≤ 1000</code>，<code>1 ≤ V ≤ 10000</code>）。</p><p>接下来 <code>n</code> 行，每行两个整数 <code>w<sub>i</sub>, v<sub>i</sub></code>。</p>',
      outputFormat: '<p>输出一个整数，表示最大总价值。</p>',
      samples: [{ input: '3 5\n1 2\n2 4\n3 4', output: '8' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 74, submissions: 190, stageIds: ['s3', 's6'],
      testData: '3 5\n1 2\n2 4\n3 4\n4 6\n2 3\n1 2\n4 5\n5 6'
    },
    {
      id: 'p4', type: 'oj', title: '单源最短路', difficulty: '进阶',
      tags: ['C++ 与 STL 基础', '图论', '最短路'], source: '站内原创', status: 'published',
      statement: '<p>给定一张 <code>n</code> 个点、<code>m</code> 条边的有向图，边权非负。求从起点 <code>s</code> 到每个点的最短距离；若不可达，输出 <code>-1</code>。</p>',
      inputFormat: '<p>第一行三个整数 <code>n, m, s</code>（<code>1 ≤ n ≤ 10<sup>5</sup></code>，<code>1 ≤ m ≤ 2×10<sup>5</sup></code>）。</p><p>接下来 <code>m</code> 行，每行三个整数 <code>u, v, w</code>，表示一条从 <code>u</code> 到 <code>v</code>、权值为 <code>w</code> 的边。</p>',
      outputFormat: '<p>输出一行 <code>n</code> 个整数，表示起点到每个点的最短距离。</p>',
      samples: [{ input: '4 4 1\n1 2 2\n1 3 5\n2 3 1\n3 4 3', output: '0 2 3 6' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 52, submissions: 150, stageIds: ['s4'],
      testData: '4 4 1\n1 2 2\n1 3 5\n2 3 1\n3 4 3\n5 4 1\n1 2 1\n2 3 1\n3 4 1\n4 5 1'
    },
    {
      id: 'p5', type: 'oj', title: '石子合并', difficulty: '进阶',
      tags: ['动态规划', '区间 DP'], source: '站内原创', status: 'published',
      statement: '<p>有 <code>n</code> 堆石子排成一排，每次只能合并相邻两堆，得分为两堆石子数之和。求把所有石子合并成一堆的最小总得分。</p>',
      inputFormat: '<p>第一行一个整数 <code>n</code>（<code>1 ≤ n ≤ 300</code>）。</p><p>第二行 <code>n</code> 个整数，表示每堆石子数。</p>',
      outputFormat: '<p>输出一个整数，表示最小总得分。</p>',
      samples: [{ input: '4\n1 3 5 2', output: '22' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 31, submissions: 88, stageIds: ['s7'],
      testData: '4\n1 3 5 2\n3\n1 1 1\n5\n4 2 6 3 1'
    },
    {
      id: 'p6', type: 'oj', title: '拓扑排序', difficulty: '基础',
      tags: ['图论', '拓扑排序'], source: '站内原创', status: 'published',
      statement: '<p>给定一张有向图，输出任意一个拓扑序；如果图中存在环，输出 <code>-1</code>。</p>',
      inputFormat: '<p>第一行两个整数 <code>n, m</code>（<code>1 ≤ n ≤ 10<sup>5</sup></code>，<code>0 ≤ m ≤ 2×10<sup>5</sup></code>）。</p><p>接下来 <code>m</code> 行，每行两个整数 <code>u, v</code>，表示一条从 <code>u</code> 到 <code>v</code> 的有向边。</p>',
      outputFormat: '<p>输出一行 <code>n</code> 个整数，表示拓扑序；若存在环，输出 <code>-1</code>。</p>',
      samples: [{ input: '4 3\n1 2\n1 3\n3 4', output: '1 2 3 4' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 44, submissions: 102, stageIds: ['s8'],
      testData: '4 3\n1 2\n1 3\n3 4\n3 3\n1 2\n2 3\n3 1'
    },
    {
      id: 'p7', type: 'oj', title: '快速幂', difficulty: '基础',
      tags: ['动态规划', '数学', '快速幂'], source: '站内原创', status: 'published',
      statement: '<p>计算 <code>a<sup>b</sup> mod p</code>。</p>',
      inputFormat: '<p>一行三个整数 <code>a, b, p</code>（<code>1 ≤ a, p ≤ 10<sup>9</sup></code>，<code>0 ≤ b ≤ 10<sup>18</sup></code>）。</p>',
      outputFormat: '<p>输出一个整数，表示答案。</p>',
      samples: [{ input: '2 10 1000', output: '24' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 61, submissions: 120, stageIds: ['s3'],
      testData: '2 10 1000\n3 0 7\n5 1000000000000000000 1000000007'
    },
    {
      id: 'p8', type: 'oj', title: '并查集', difficulty: '基础',
      tags: ['C++ 与 STL 基础', '图论', '并查集'], source: '站内原创', status: 'published',
      statement: '<p>初始有 <code>n</code> 个独立元素。执行 <code>m</code> 次合并操作，求最终连通块数量。</p>',
      inputFormat: '<p>第一行两个整数 <code>n, m</code>（<code>1 ≤ n ≤ 10<sup>5</sup></code>，<code>0 ≤ m ≤ 2×10<sup>5</sup></code>）。</p><p>接下来 <code>m</code> 行，每行两个整数 <code>u, v</code>，表示合并 <code>u</code> 和 <code>v</code> 所在集合。</p>',
      outputFormat: '<p>输出一个整数，表示连通块数量。</p>',
      samples: [{ input: '5 3\n1 2\n2 3\n4 5', output: '2' }],
      timeLimit: '1s', memoryLimit: '256MB',
      accepted: 58, submissions: 105, stageIds: ['s4'],
      testData: '5 3\n1 2\n2 3\n4 5\n4 2\n1 2\n3 4'
    }
  ];

  const objectives = [
    {
      id: 'q1', type: 'objective', qtype: 'single', title: 'STL 中 vector 的 push_back 平均时间复杂度是？',
      difficulty: '入门', tags: ['C++ 与 STL 基础'], source: '站内原创', status: 'published',
      options: [{ key: 'A', text: 'O(1)' }, { key: 'B', text: 'O(log n)' }, { key: 'C', text: 'O(n)' }, { key: 'D', text: 'O(n log n)' }],
      answer: ['A'],
      explanation: 'vector 在尾部插入时，若容量足够则为 O(1)；扩容时虽然需要搬移元素，但均摊到每次插入仍是 O(1)。',
      stageIds: ['s1', 's7']
    },
    {
      id: 'q2', type: 'objective', qtype: 'single', title: '关于 map 与 unordered_map，下列说法正确的是？',
      difficulty: '基础', tags: ['C++ 与 STL 基础'], source: '站内原创', status: 'published',
      options: [{ key: 'A', text: 'map 的查找复杂度是 O(1)' }, { key: 'B', text: 'unordered_map 平均查找复杂度是 O(1)，但最坏可达 O(n)' }, { key: 'C', text: 'unordered_map 会保持键的有序性' }, { key: 'D', text: 'map 不允许自定义比较器' }],
      answer: ['B'],
      explanation: 'map 基于平衡树，单次操作 O(log n)；unordered_map 基于哈希表，平均 O(1)，最坏 O(n)，且不保证顺序。',
      stageIds: ['s2']
    },
    {
      id: 'q3', type: 'objective', qtype: 'multiple', title: '在 01 背包问题中，下列哪些状态定义是合理的？',
      difficulty: '进阶', tags: ['动态规划', '背包'], source: '站内原创', status: 'published',
      options: [{ key: 'A', text: 'f[i][j] 表示前 i 件物品、容量不超过 j 的最大价值' }, { key: 'B', text: 'f[i][j] 表示前 i 件物品、恰好装满容量 j 的最大价值' }, { key: 'C', text: 'f[j] 表示容量不超过 j 的最大价值（滚动数组）' }, { key: 'D', text: 'f[i] 表示第 i 件物品的价值' }],
      answer: ['A', 'B', 'C'],
      explanation: 'A、B、C 都是常见的合法状态设计；D 只描述单件物品，不能支撑决策转移。',
      stageIds: ['s3', 's5']
    },
    {
      id: 'q4', type: 'objective', qtype: 'judge', title: 'Dijkstra 算法可以正确处理带负权边的图。',
      difficulty: '基础', tags: ['图论', '最短路'], source: '站内原创', status: 'published',
      options: [{ key: 'A', text: '正确' }, { key: 'B', text: '错误' }],
      answer: ['B'],
      explanation: 'Dijkstra 依赖「已确定点的距离不会再被更新」这一性质，负权边会破坏该性质，应使用 Bellman-Ford 或 SPFA。',
      stageIds: ['s8']
    },
    {
      id: 'q5', type: 'objective', qtype: 'single', title: '在有序数组上二分查找的时间复杂度是？',
      difficulty: '入门', tags: ['C++ 与 STL 基础'], source: '站内原创', status: 'published',
      options: [{ key: 'A', text: 'O(1)' }, { key: 'B', text: 'O(log n)' }, { key: 'C', text: 'O(n)' }, { key: 'D', text: 'O(n log n)' }],
      answer: ['B'],
      explanation: '每次比较后搜索区间减半，因此复杂度为 O(log n)。',
      stageIds: ['s4']
    },
    {
      id: 'q6', type: 'objective', qtype: 'multiple', title: '下列哪些 STL 容器支持随机访问（下标访问）？',
      difficulty: '基础', tags: ['C++ 与 STL 基础'], source: '站内原创', status: 'published',
      options: [{ key: 'A', text: 'vector' }, { key: 'B', text: 'deque' }, { key: 'C', text: 'array' }, { key: 'D', text: 'list' }],
      answer: ['A', 'B', 'C'],
      explanation: 'vector、deque、array 都支持随机访问；list 是双向链表，只能顺序访问。',
      stageIds: ['s6']
    }
  ];

  const shortAnswers = [
    {
      id: 'sa1', type: 'short', title: '简述 vector 的扩容机制，并说明 push_back 的均摊复杂度为什么是 O(1)。',
      difficulty: '入门', tags: ['C++ 与 STL 基础'], source: '站内原创', status: 'published', stageIds: ['s1'],
      maxScore: 100,
      referenceAnswer: 'vector 内部使用一段连续内存。当 size 达到 capacity 时，会申请一块更大的连续内存（常见实现按 1.5 倍或 2 倍扩容），把原有元素搬移到新内存后再释放旧内存。单次扩容需要 O(n) 搬移，但扩容间隔随容量增长，把搬移代价均摊到每次 push_back 上仍是 O(1)。',
      scoringRubric: [
        '说明 vector 使用连续内存（15 分）',
        '说明容量不足时申请更大内存并搬移元素（35 分）',
        '说明常见扩容倍数或增长策略（20 分）',
        '解释均摊复杂度为 O(1) 的推导（30 分）'
      ],
      keywords: ['连续内存', '扩容', '搬移', '均摊', 'O(1)', 'capacity']
    },
    {
      id: 'sa2', type: 'short', title: '简述 Dijkstra 算法为什么不能处理带负权边的图。',
      difficulty: '基础', tags: ['图论', '最短路'], source: '站内原创', status: 'published', stageIds: ['s8'],
      maxScore: 100,
      referenceAnswer: 'Dijkstra 基于贪心：每次取出当前距离最小的未确定点，并认为它的距离已经最终确定。这个性质依赖边权非负——若存在负权边，之后可能出现一条经过其他点、总权更小的路径，使已确定的最短距离被更新，贪心前提被破坏。带负权边时应使用 Bellman-Ford 或 SPFA。',
      scoringRubric: [
        '指出 Dijkstra 的贪心策略与「最短距离已确定」的性质（30 分）',
        '说明非负边权是该性质成立的前提（25 分）',
        '给出负权边导致已确定距离被更新的反例思路（30 分）',
        '指出替代算法（15 分）'
      ],
      keywords: ['贪心', '负权', '非负', '确定', 'Bellman', 'SPFA']
    },
    {
      id: 'sa3', type: 'short', title: '简述 01 背包使用一维数组时，内层容量循环为什么必须倒序枚举。',
      difficulty: '进阶', tags: ['动态规划', '背包'], source: '站内原创', status: 'published', stageIds: ['s6'],
      maxScore: 100,
      referenceAnswer: '一维数组 f[j] 表示容量不超过 j 的最大价值。若 j 正序枚举，f[j - w[i]] 可能在本轮已经被第 i 件物品更新，相当于同一件物品被重复选择，退化为完全背包。倒序枚举时 f[j - w[i]] 仍是上一轮（前 i-1 件物品）的状态，保证每件物品只选一次。',
      scoringRubric: [
        '说明 f[j] 的状态含义（20 分）',
        '指出正序枚举会重复选择同一件物品（30 分）',
        '说明倒序枚举读取的是上一轮状态（35 分）',
        '说明正序会退化为完全背包（15 分）'
      ],
      keywords: ['倒序', '正序', '重复', '上一轮', '完全背包', '一件']
    }
  ];

  const externals = [
    { id: 'e1', type: 'external', title: 'Codeforces 4A · Watermelon', platform: 'Codeforces', url: 'https://codeforces.com/problemset/problem/4/A', difficulty: '入门', tags: ['C++ 与 STL 基础'], source: 'Codeforces', status: 'published', stageIds: ['s1', 's6'] },
    { id: 'e2', type: 'external', title: 'Codeforces 1A · Theatre Square', platform: 'Codeforces', url: 'https://codeforces.com/problemset/problem/1/A', difficulty: '入门', tags: ['C++ 与 STL 基础'], source: 'Codeforces', status: 'published', stageIds: ['s2', 's7'] },
    { id: 'e3', type: 'external', title: 'Codeforces 158A · Next Round', platform: 'Codeforces', url: 'https://codeforces.com/problemset/problem/158/A', difficulty: '入门', tags: ['C++ 与 STL 基础'], source: 'Codeforces', status: 'published', stageIds: ['s3', 's8'] },
    { id: 'e4', type: 'external', title: '牛客 · 基础算法题单', platform: '牛客', url: 'https://www.nowcoder.com/exam/oj', difficulty: '基础', tags: ['C++ 与 STL 基础'], source: '牛客', status: 'published', stageIds: ['s4'] },
    { id: 'e5', type: 'external', title: '洛谷 P1001 · A+B Problem', platform: '洛谷', url: 'https://www.luogu.com.cn/problem/P1001', difficulty: '入门', tags: ['动态规划'], source: '洛谷', status: 'published', stageIds: ['s5'] },
    { id: 'e6', type: 'external', title: 'Codeforces 231A · Team', platform: 'Codeforces', url: 'https://codeforces.com/problemset/problem/231/A', difficulty: '入门', tags: ['C++ 与 STL 基础'], source: 'Codeforces', status: 'published', stageIds: ['s3'] }
  ];

  const allProblems = problems.concat(objectives, shortAnswers, externals);

  const leaderboard = [
    { userId: 'u_02', inSystem: 18, cf: 32, inSystemAllTime: 214, cfAllTime: 486 },
    { userId: 'u_03', inSystem: 15, cf: 28, inSystemAllTime: 183, cfAllTime: 402 },
    { userId: 'u_04', inSystem: 20, cf: 20, inSystemAllTime: 320, cfAllTime: 410 },
    { userId: 'u_05', inSystem: 12, cf: 25, inSystemAllTime: 145, cfAllTime: 318 },
    { userId: 'u_06', inSystem: 22, cf: 8, inSystemAllTime: 300, cfAllTime: 150 },
    { userId: 'u_07', inSystem: 16, cf: 10, inSystemAllTime: 158, cfAllTime: 172 },
    { userId: 'u_08', inSystem: 14, cf: 12, inSystemAllTime: 132, cfAllTime: 188 },
    { userId: 'u_09', inSystem: 10, cf: 9, inSystemAllTime: 96, cfAllTime: 142 },
    { userId: 'u_10', inSystem: 8, cf: 11, inSystemAllTime: 88, cfAllTime: 134 },
    { userId: 'u_demo', inSystem: 6, cf: 12, inSystemAllTime: 64, cfAllTime: 156 },
    { userId: 'u_11', inSystem: 9, cf: 8, inSystemAllTime: 82, cfAllTime: 118 },
    { userId: 'u_12', inSystem: 7, cf: 9, inSystemAllTime: 74, cfAllTime: 126 },
    { userId: 'u_13', inSystem: 6, cf: 8, inSystemAllTime: 58, cfAllTime: 104 },
    { userId: 'u_14', inSystem: 5, cf: 9, inSystemAllTime: 52, cfAllTime: 96 },
    { userId: 'u_15', inSystem: 8, cf: 6, inSystemAllTime: 76, cfAllTime: 88 },
    { userId: 'u_16', inSystem: 4, cf: 8, inSystemAllTime: 48, cfAllTime: 74 },
    { userId: 'u_17', inSystem: 6, cf: 5, inSystemAllTime: 55, cfAllTime: 68 },
    { userId: 'u_18', inSystem: 3, cf: 7, inSystemAllTime: 36, cfAllTime: 62 },
    { userId: 'u_19', inSystem: 5, cf: 4, inSystemAllTime: 41, cfAllTime: 54 },
    { userId: 'u_20', inSystem: 2, cf: 6, inSystemAllTime: 28, cfAllTime: 48 },
    { userId: 'u_21', inSystem: 3, cf: 3, inSystemAllTime: 32, cfAllTime: 40 }
  ];

  const manualDeclarations = [
    { id: 'm_02', userId: 'u_02', platform: '牛客', handle: 'linshen', count: 6, week: TZ.WEEK, createdAt: D(11, 9, 8, 10) },
    { id: 'm_03', userId: 'u_03', platform: '洛谷', handle: 'zhouye', count: 5, week: TZ.WEEK, createdAt: D(11, 9, 8, 20) },
    { id: 'm_04', userId: 'u_04', platform: 'AtCoder', handle: 'suwan', count: 4, week: TZ.WEEK, createdAt: D(11, 9, 8, 30) },
    { id: 'm_05', userId: 'u_05', platform: '牛客', handle: 'chenyi', count: 2, week: TZ.WEEK, createdAt: D(11, 9, 8, 40) },
    { id: 'm_06', userId: 'u_06', platform: '牛客', handle: 'gubei', count: 6, week: TZ.WEEK, createdAt: D(11, 9, 8, 50) },
    { id: 'm_07', userId: 'u_07', platform: '洛谷', handle: 'jiangli', count: 8, week: TZ.WEEK, createdAt: D(11, 9, 9, 0) },
    { id: 'm_08', userId: 'u_08', platform: '牛客', handle: 'xuzhi', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 9, 10) },
    { id: 'm_09', userId: 'u_09', platform: 'AtCoder', handle: 'shenzhou', count: 5, week: TZ.WEEK, createdAt: D(11, 9, 9, 20) },
    { id: 'm_10', userId: 'u_10', platform: '洛谷', handle: 'luxing', count: 2, week: TZ.WEEK, createdAt: D(11, 9, 9, 30) },
    { id: 'm_demo', userId: 'u_demo', platform: '牛客', handle: 'demo_nk', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 9, 40) },
    { id: 'm_11', userId: 'u_11', platform: '牛客', handle: 'bailu', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 9, 50) },
    { id: 'm_12', userId: 'u_12', platform: '洛谷', handle: 'chengye', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 10, 0) },
    { id: 'm_13', userId: 'u_13', platform: 'AtCoder', handle: 'hanchuan', count: 4, week: TZ.WEEK, createdAt: D(11, 9, 10, 10) },
    { id: 'm_14', userId: 'u_14', platform: '牛客', handle: 'yeqing', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 10, 20) },
    { id: 'm_15', userId: 'u_15', platform: '洛谷', handle: 'songci', count: 2, week: TZ.WEEK, createdAt: D(11, 9, 10, 30) },
    { id: 'm_16', userId: 'u_16', platform: '牛客', handle: 'tangning', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 10, 40) },
    { id: 'm_17', userId: 'u_17', platform: '洛谷', handle: 'fangyuan', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 10, 50) },
    { id: 'm_18', userId: 'u_18', platform: 'AtCoder', handle: 'xiamu', count: 3, week: TZ.WEEK, createdAt: D(11, 9, 11, 0) },
    { id: 'm_19', userId: 'u_19', platform: '牛客', handle: 'luohe', count: 2, week: TZ.WEEK, createdAt: D(11, 9, 11, 10) },
    { id: 'm_20', userId: 'u_20', platform: '洛谷', handle: 'shiyu', count: 2, week: TZ.WEEK, createdAt: D(11, 9, 11, 20) },
    { id: 'm_21', userId: 'u_21', platform: '牛客', handle: 'chujiu', count: 2, week: TZ.WEEK, createdAt: D(11, 9, 11, 30) }
  ];

  /* 历史周手动申报：用于演示总榜手动申报累计 */
  const manualHistory = [
    ['u_02', 5], ['u_03', 4], ['u_04', 6], ['u_05', 5], ['u_06', 4], ['u_07', 2],
    ['u_08', 3], ['u_09', 4], ['u_10', 3], ['u_demo', 2], ['u_11', 2], ['u_12', 3],
    ['u_13', 2], ['u_14', 4], ['u_15', 3], ['u_16', 2], ['u_17', 3], ['u_18', 2],
    ['u_19', 2], ['u_20', 3], ['u_21', 2]
  ].map(function (item, i) {
    return {
      id: 'mh_' + String(i + 1).padStart(2, '0'),
      userId: item[0],
      platform: '牛客',
      handle: '',
      count: item[1],
      week: '2026-W45',
      createdAt: D(11, 2, 10, 0)
    };
  });

  const submissions = [
    { id: 'sub_01', userId: 'u_demo', problemId: 'p1', type: 'oj', language: 'C++17', code: 'int main() { int a,b; cin>>a>>b; cout<<a+b; }', verdict: 'AC', time: '12ms', memory: '1.8MB', detail: '通过全部测试点', createdAt: D(11, 9, 10, 0) },
    { id: 'sub_02', userId: 'u_demo', problemId: 'p2', type: 'oj', language: 'C++17', code: '// wa\nint main() {}', verdict: 'WA', time: '14ms', memory: '2.1MB', detail: '答案错误（第 3 组数据）', createdAt: D(11, 9, 10, 20) },
    { id: 'sub_03', userId: 'u_demo', problemId: 'p2', type: 'oj', language: 'C++17', code: 'int main() { /* AC */ }', verdict: 'AC', time: '16ms', memory: '2.0MB', detail: '通过全部测试点', createdAt: D(11, 9, 10, 35) },
    { id: 'sub_04', userId: 'u_demo', problemId: 'p3', type: 'oj', language: 'C++17', code: '// wa\nint main() {}', verdict: 'WA', time: '9ms', memory: '1.5MB', detail: '答案错误（第 5 组数据）', createdAt: D(11, 8, 21, 0) },
    { id: 'sub_05', userId: 'u_demo', problemId: 'p3', type: 'oj', language: 'C++17', code: 'int main() { /* AC */ }', verdict: 'AC', time: '11ms', memory: '1.6MB', detail: '通过全部测试点', createdAt: D(11, 9, 11, 0) },
    { id: 'sub_06', userId: 'u_demo', problemId: 'p4', type: 'oj', language: 'C++17', code: 'int main() { /* dijkstra */ }', verdict: 'AC', time: '23ms', memory: '3.2MB', detail: '通过全部测试点', createdAt: D(11, 9, 11, 30) },
    { id: 'sub_07', userId: 'u_demo', problemId: 'p5', type: 'oj', language: 'C++17', code: 'int main() { /* interval dp */ }', verdict: 'AC', time: '18ms', memory: '2.4MB', detail: '通过全部测试点', createdAt: D(11, 9, 12, 0) },
    { id: 'sub_08', userId: 'u_demo', problemId: 'p6', type: 'oj', language: 'C++17', code: 'int main() { /* topo */ }', verdict: 'AC', time: '15ms', memory: '2.2MB', detail: '通过全部测试点', createdAt: D(11, 9, 12, 30) },
    { id: 'sub_09', userId: 'u_demo', problemId: 'q1', type: 'objective', language: '-', code: '', verdict: 'WA', time: '-', memory: '-', detail: '选择 B，正确答案 A', createdAt: D(11, 9, 13, 0) },
    { id: 'sub_10', userId: 'u_demo', problemId: 'q2', type: 'objective', language: '-', code: '', verdict: 'WA', time: '-', memory: '-', detail: '选择 C，正确答案 B', createdAt: D(11, 8, 9, 0) },
    { id: 'sub_11', userId: 'u_demo', problemId: 'sa1', type: 'short', language: '文本作答', code: '', verdict: 'AC', time: '-', memory: '-', detail: 'AI 判分 78 分', aiScore: 78, aiFeedback: '回答基本覆盖扩容机制与均摊结论，但扩容倍数的描述不够明确。', gradingStatus: 'graded', gradedAt: D(11, 9, 9, 20), answerText: 'vector 用连续内存存储元素，size 等于 capacity 时会申请更大的空间并把旧元素搬过去，所以每次插入平均下来还是 O(1)。', createdAt: D(11, 9, 9, 20) },
    { id: 'sub_12', userId: 'u_demo', problemId: 'sa2', type: 'short', language: '文本作答', code: '', verdict: 'WA', time: '-', memory: '-', detail: 'AI 判分 52 分', aiScore: 52, aiFeedback: '只提到负权边会影响结果，没有说明贪心策略和最短距离已确定这两个关键点。', gradingStatus: 'graded', gradedAt: D(11, 8, 20, 10), answerText: '因为负权边会让最短路变成负的，Dijkstra 算不出来。', createdAt: D(11, 8, 20, 10) }
  ];

  const progress = {
    u_demo: {
      s1: { completed: true, completedAt: D(11, 8, 10, 0) },
      s2: { completed: true, completedAt: D(11, 8, 15, 0) }
    }
  };

  const externalCompletions = {
    u_demo: { e1: { done: true, source: 'CF 自动识别', at: D(11, 8, 11, 0) } }
  };

  const wrongBook = [
    { id: 'w_01', userId: 'u_demo', problemId: 'p2', latestError: 'WA', count: 1, lastAt: D(11, 9, 10, 20) },
    { id: 'w_02', userId: 'u_demo', problemId: 'p3', latestError: 'WA', count: 1, lastAt: D(11, 8, 21, 0) },
    { id: 'w_03', userId: 'u_demo', problemId: 'q1', latestError: 'WA', count: 1, lastAt: D(11, 9, 13, 0) },
    { id: 'w_04', userId: 'u_demo', problemId: 'q2', latestError: 'WA', count: 1, lastAt: D(11, 8, 9, 0) },
    { id: 'w_05', userId: 'u_demo', problemId: 'sa2', latestError: 'WA', latestScore: 52, count: 1, lastAt: D(11, 8, 20, 10) }
  ];

  const battles = [
    { id: 'b_01', room: 'TZ-2048', userId: 'u_demo', opponentId: 'u_02', opponentName: '林深', problemId: 'p1', result: 'win', startedAt: D(11, 8, 20, 15), endedAt: D(11, 8, 20, 27), duration: '12:34' },
    { id: 'b_02', room: 'TZ-1987', userId: 'u_demo', opponentId: 'u_03', opponentName: '周野', problemId: 'p3', result: 'loss', startedAt: D(11, 7, 19, 20), endedAt: D(11, 7, 19, 48), duration: '28:10' },
    { id: 'b_03', room: 'TZ-1776', userId: 'u_demo', opponentId: 'u_04', opponentName: '苏晚', problemId: 'p2', result: 'draw', startedAt: D(11, 6, 21, 0), endedAt: D(11, 6, 21, 30), duration: '30:00' }
  ];

  const datagenTasks = [
    {
      id: 'd_01', userId: 'u_demo', title: '最大子段和 · 随机数据', status: 'success', groupCount: 10, mode: 'random',
      createdAt: D(11, 8, 14, 20), finishedAt: D(11, 8, 14, 22), timeLimit: '1s', memoryLimit: '256MB',
      statement: '给定长度为 n 的整数序列，求最大子段和。',
      inputFormat: '第一行 n，第二行 n 个整数。',
      outputFormat: '一个整数，表示最大子段和。',
      variables: [{ name: 'n', type: 'int', min: 1, max: 20 }, { name: 'a', type: 'int', min: -100, max: 100 }],
      stdCode: '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    long long ans = -1e18, sum = 0;\n    for (int i = 0; i < n; i++) {\n        long long x; cin >> x;\n        sum = max(x, sum + x);\n        ans = max(ans, sum);\n    }\n    cout << ans << "\\n";\n    return 0;\n}',
      files: makeFiles(10)
    },
    {
      id: 'd_02', userId: 'u_demo', title: '01 背包 · 边界数据', status: 'failed', groupCount: 10, mode: 'random',
      createdAt: D(11, 7, 10, 5), finishedAt: D(11, 7, 10, 7), timeLimit: '1s', memoryLimit: '256MB',
      statement: '01 背包问题。',
      inputFormat: '第一行 n, V，接下来 n 行 w, v。',
      outputFormat: '一个整数，表示最大价值。',
      variables: [{ name: 'n', type: 'int', min: 1, max: 50 }, { name: 'V', type: 'int', min: 1, max: 100 }],
      stdCode: '#include <bits/stdc++.h>\n// TLE\nint main() { return 0; }',
      files: [],
      failureStage: 'std',
      failureReason: 'std 在第 7 组运行超时（超过 1s），任务已终止。'
    },
    {
      id: 'd_03', userId: 'u_02', title: '图论 · 随机连通图', status: 'success', groupCount: 10, mode: 'random',
      createdAt: D(11, 8, 16, 40), finishedAt: D(11, 8, 16, 42), timeLimit: '2s', memoryLimit: '256MB',
      statement: '给定一张无向图，求连通块数量。',
      inputFormat: '第一行 n, m，接下来 m 行 u, v。',
      outputFormat: '一个整数，表示连通块数量。',
      variables: [{ name: 'n', type: 'int', min: 1, max: 30 }, { name: 'm', type: 'int', min: 0, max: 50 }],
      stdCode: '#include <bits/stdc++.h>\nusing namespace std;\n// std\nint main() { return 0; }',
      files: makeFiles(10)
    },
    {
      id: 'd_04', userId: 'u_demo', title: '图论 · 链状与星形边界', status: 'success', groupCount: 10, mode: 'llm', llmProvider: 'DeepSeek',
      createdAt: D(11, 9, 10, 10), finishedAt: D(11, 9, 10, 12), timeLimit: '2s', memoryLimit: '256MB',
      statement: '给定一张 n 个点的无向图，求连通块数量。',
      inputFormat: '第一行 n, m，接下来 m 行 u, v。',
      outputFormat: '一个整数，表示连通块数量。',
      variables: [{ name: 'n', type: 'int', min: 1, max: 50 }, { name: 'm', type: 'int', min: 0, max: 100 }],
      specialRequirements: '包含全孤立点、一条链、一个星形图，并覆盖 n=1 与空边集。',
      stdCode: '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n, m; cin >> n >> m;\n    cout << max(1, n - m) << "\\n";\n    return 0;\n}',
      files: makeFiles(10)
    },
    {
      id: 'd_05', userId: 'u_demo', title: 'DP · 卡精度边界', status: 'failed', groupCount: 10, mode: 'llm', llmProvider: '智谱 AI',
      createdAt: D(11, 9, 9, 30), finishedAt: D(11, 9, 9, 32), timeLimit: '1s', memoryLimit: '256MB',
      statement: '给定序列，求平均数并保留 6 位小数。',
      inputFormat: '第一行 n，第二行 n 个整数。',
      outputFormat: '一个实数。',
      variables: [{ name: 'n', type: 'int', min: 1, max: 20 }],
      specialRequirements: '包含极大值与极小值混合、结果接近精度边界的用例。',
      stdCode: '#include <bits/stdc++.h>\nusing namespace std;\nint main() { return 0; }',
      files: [],
      failureStage: 'generator',
      failureReason: 'LLM 生成的数据构造器未通过可执行性校验（第 3 组输出格式不匹配），任务已终止。'
    },
    {
      id: 'd_06', userId: 'u_demo', title: '最短路 · LLM 直接生成数据', status: 'success', groupCount: 10, mode: 'llm_direct', llmProvider: 'DeepSeek',
      createdAt: D(11, 9, 11, 10), finishedAt: D(11, 9, 11, 12), timeLimit: '1s', memoryLimit: '256MB',
      statement: '给定一张 n 个点、m 条边的有向图，边权非负，求起点到每个点的最短路。',
      inputFormat: '第一行 n, m, s，接下来 m 行 u, v, w。',
      outputFormat: '一行 n 个整数，表示最短距离。',
      variables: [{ name: 'n', type: 'int', min: 1, max: 100 }, { name: 'm', type: 'int', min: 0, max: 300 }],
      specialRequirements: '包含不可达点、单点图与链状图，边权覆盖 0 与较大值。',
      stdCode: '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n, m, s; cin >> n >> m >> s;\n    return 0;\n}',
      llmDataStatus: 'ready',
      generatorStatus: 'not_applicable',
      failureStage: '',
      files: makeFiles(10)
    }
  ];

  const admins = [
    { id: 'a_01', username: 'admin', password: 'admin', name: '平台超管', role: 'super', avatar: '超', lastLoginAt: D(11, 9, 9, 0) },
    { id: 'a_02', username: 'operator', password: 'operator', name: '内容运营', role: 'operator', avatar: '运', lastLoginAt: D(11, 9, 8, 30) }
  ];

  const logs = [
    { id: 'l_01', adminId: 'a_01', adminName: '平台超管', role: 'super', action: '新增题目', target: 'OJ 题库', detail: '创建题目《快速幂》', createdAt: D(11, 9, 9, 10) },
    { id: 'l_02', adminId: 'a_02', adminName: '内容运营', role: 'operator', action: '编辑路线', target: '学习路线', detail: '更新《动态规划专项》阶段排序', createdAt: D(11, 9, 8, 40) },
    { id: 'l_03', adminId: 'a_01', adminName: '平台超管', role: 'super', action: '刷新 CF 数据', target: '榜单管理', detail: '模拟同步 CF 周数据', createdAt: D(11, 9, 8, 0) },
    { id: 'l_04', adminId: 'a_02', adminName: '内容运营', role: 'operator', action: '禁用用户', target: '用户管理', detail: '禁用用户 初九', createdAt: D(11, 8, 18, 20) },
    { id: 'l_05', adminId: 'a_01', adminName: '平台超管', role: 'super', action: '登录后台', target: '系统', detail: '超级管理员登录', createdAt: D(11, 8, 9, 0) }
  ];

  const seed = {
    version: 7,
    currentUserId: null,
    currentAdminId: null,
    users: users,
    routes: routes,
    problems: problems,
    objectives: objectives,
    shortAnswers: shortAnswers,
    externals: externals,
    leaderboard: leaderboard,
    manualDeclarations: manualDeclarations.concat(manualHistory),
    submissions: submissions,
    progress: progress,
    externalCompletions: externalCompletions,
    wrongBook: wrongBook,
    battles: battles,
    datagenTasks: datagenTasks,
    battleRoom: null,
    admins: admins,
    logs: logs,
    lastCfSync: D(11, 9, 8, 0),
    llmCredentials: llmCredentials,
    counters: { route: 2, stage: 8, problem: 8, objective: 6, short: 3, external: 6, submission: 12, task: 6, battle: 3, log: 5, manual: 42, user: 21, course: 16, credential: 1 }
  };

  function clone(obj) {
    return JSON.parse(JSON.stringify(obj));
  }

  TZ.load = function () {
    try {
      const raw = localStorage.getItem(DB_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && parsed.version === seed.version) return parsed;
      }
    } catch (e) {}
    return clone(seed);
  };

  TZ.save = function () {
    try { localStorage.setItem(DB_KEY, JSON.stringify(TZ.db)); } catch (e) {}
  };

  TZ.reset = function () {
    try { localStorage.removeItem(DB_KEY); } catch (e) {}
    TZ.db = clone(seed);
    TZ.save();
  };

  TZ.db = TZ.load();

  /* ---------- 查询辅助 ---------- */
  TZ.getUser = function (id) {
    return TZ.db.users.find(function (u) { return u.id === id; });
  };
  TZ.getRoute = function (id) {
    return TZ.db.routes.find(function (r) { return r.id === id; });
  };
  TZ.getStage = function (id) {
    for (const r of TZ.db.routes) {
      const s = r.stages.find(function (x) { return x.id === id; });
      if (s) return { stage: s, route: r };
    }
    return null;
  };
  TZ.getProblem = function (id) {
    return TZ.db.problems.find(function (p) { return p.id === id; })
      || TZ.db.objectives.find(function (p) { return p.id === id; })
      || TZ.db.shortAnswers.find(function (p) { return p.id === id; })
      || TZ.db.externals.find(function (p) { return p.id === id; });
  };
  TZ.getAnyProblem = TZ.getProblem;
  TZ.getOjProblem = function (id) {
    return TZ.db.problems.find(function (p) { return p.id === id; });
  };
  TZ.getObjective = function (id) {
    return TZ.db.objectives.find(function (p) { return p.id === id; });
  };
  TZ.getShortAnswer = function (id) {
    return TZ.db.shortAnswers.find(function (p) { return p.id === id; });
  };
  TZ.getExternal = function (id) {
    return TZ.db.externals.find(function (p) { return p.id === id; });
  };
  TZ.getLlmCredential = function (userId) {
    return TZ.db.llmCredentials.find(function (c) { return c.userId === userId; }) || null;
  };
  TZ.maskLlmKey = function (key) {
    const raw = String(key || '');
    if (!raw) return '';
    return '••••' + raw.slice(-4);
  };
  TZ.validateLlmKey = function (provider, key) {
    const trimmed = String(key || '').trim();
    if (!provider) return { ok: false, message: '请选择 LLM 服务商' };
    if (TZ.LLM_PROVIDERS.indexOf(provider) < 0) return { ok: false, message: '该服务商不在预置列表中' };
    if (trimmed.length < 8) return { ok: false, message: 'API Key 格式不正确，至少 8 个字符' };
    if (/模拟失败|invalid|bad.?key/i.test(trimmed)) return { ok: false, message: 'API Key 无效，请检查后重试' };
    if (/expired|expire|欠费/i.test(trimmed)) return { ok: false, message: 'API Key 已过期或余额不足' };
    return { ok: true };
  };
  TZ.hasRunningLlmTask = function (userId) {
    return TZ.db.datagenTasks.some(function (t) {
      return t.userId === userId
        && (t.mode === 'llm' || t.mode === 'llm_direct')
        && ['generating', 'llm_data', 'running'].indexOf(t.status) >= 0;
    });
  };
  TZ.currentUser = function () {
    return TZ.db.currentUserId ? TZ.getUser(TZ.db.currentUserId) : null;
  };
  TZ.currentAdmin = function () {
    return TZ.db.currentAdminId ? TZ.db.admins.find(function (a) { return a.id === TZ.db.currentAdminId; }) : null;
  };
  TZ.isSuper = function () {
    const a = TZ.currentAdmin();
    return !!(a && a.role === 'super');
  };
  TZ.uid = function (prefix) {
    const key = prefix === 'sub' ? 'submission' : prefix;
    TZ.db.counters[key] = (TZ.db.counters[key] || 0) + 1;
    return prefix + '_' + String(TZ.db.counters[key]).padStart(2, '0') + '_' + Math.random().toString(36).slice(2, 6);
  };

  /* ---------- 榜单计算（周榜 / 总榜） ---------- */
  TZ.getManualTotal = function (userId) {
    return TZ.db.manualDeclarations
      .filter(function (m) { return m.userId === userId && m.week === TZ.WEEK; })
      .reduce(function (sum, m) { return sum + Number(m.count || 0); }, 0);
  };
  TZ.getManualAllTime = function (userId) {
    return TZ.db.manualDeclarations
      .filter(function (m) { return m.userId === userId; })
      .reduce(function (sum, m) { return sum + Number(m.count || 0); }, 0);
  };
  TZ.getManualBreakdown = function (userId) {
    return TZ.db.manualDeclarations.filter(function (m) { return m.userId === userId && m.week === TZ.WEEK; });
  };
  TZ.getLeaderboard = function (mode) {
    const allTime = mode === 'all';
    const rows = TZ.db.leaderboard.map(function (entry) {
      const user = TZ.getUser(entry.userId);
      const manual = allTime ? TZ.getManualAllTime(entry.userId) : TZ.getManualTotal(entry.userId);
      const inSystem = allTime ? Number(entry.inSystemAllTime || 0) : Number(entry.inSystem || 0);
      const cf = allTime ? Number(entry.cfAllTime || 0) : Number(entry.cf || 0);
      return {
        userId: entry.userId,
        user: user,
        mode: allTime ? 'all' : 'week',
        inSystem: inSystem,
        cf: cf,
        manual: manual,
        total: inSystem + cf + manual
      };
    }).filter(function (r) { return r.user; });

    rows.sort(function (a, b) {
      if (b.total !== a.total) return b.total - a.total;
      if (b.inSystem !== a.inSystem) return b.inSystem - a.inSystem;
      return a.user.name.localeCompare(b.user.name, 'zh');
    });

    rows.forEach(function (r, i) {
      if (i > 0 && rows[i - 1].total === r.total && rows[i - 1].inSystem === r.inSystem) {
        r.rank = rows[i - 1].rank;
      } else {
        r.rank = i + 1;
      }
    });
    return rows;
  };
  TZ.getMyRankRow = function (mode) {
    const user = TZ.currentUser();
    if (!user) return null;
    return TZ.getLeaderboard(mode).find(function (r) { return r.userId === user.id; }) || null;
  };

  /* ---------- 学习进度 ---------- */
  TZ.getProgress = function (userId, stageId) {
    const p = TZ.db.progress[userId] && TZ.db.progress[userId][stageId];
    return p || { completed: false };
  };
  TZ.routeProgress = function (userId, route) {
    if (!route) return { done: 0, total: 0, percent: 0 };
    const done = route.stages.filter(function (s) {
      return TZ.getProgress(userId, s.id).completed;
    }).length;
    return { done: done, total: route.stages.length, percent: route.stages.length ? Math.round(done / route.stages.length * 100) : 0 };
  };
  TZ.toggleStage = function (userId, stageId) {
    if (!TZ.db.progress[userId]) TZ.db.progress[userId] = {};
    const cur = TZ.getProgress(userId, stageId);
    TZ.db.progress[userId][stageId] = cur.completed
      ? { completed: false }
      : { completed: true, completedAt: new Date().toISOString() };
    TZ.save();
  };

  /* ---------- 外链题完成 ---------- */
  TZ.isExternalDone = function (userId, problemId) {
    return !!(TZ.db.externalCompletions[userId] && TZ.db.externalCompletions[userId][problemId] && TZ.db.externalCompletions[userId][problemId].done);
  };
  TZ.setExternalDone = function (userId, problemId, done, source) {
    if (!TZ.db.externalCompletions[userId]) TZ.db.externalCompletions[userId] = {};
    if (done) {
      TZ.db.externalCompletions[userId][problemId] = { done: true, source: source || '手动标记', at: new Date().toISOString() };
    } else {
      delete TZ.db.externalCompletions[userId][problemId];
    }
    TZ.save();
  };

  /* ---------- 提交 / 判题 ---------- */
  TZ.judgeCode = function (code) {
    const c = (code || '').toLowerCase();
    if (!c.trim() || c.indexOf('compile_error') >= 0) {
      return { verdict: 'CE', detail: '编译错误：代码为空或存在语法问题', time: '0ms', memory: '0KB' };
    }
    if (c.indexOf('// tle') >= 0 || c.indexOf('while(true)') >= 0 || c.indexOf('for(;;)') >= 0) {
      return { verdict: 'TLE', detail: '运行超时（第 4 组数据）', time: '2000ms', memory: '2.1MB' };
    }
    if (c.indexOf('// wa') >= 0 || c.indexOf('wrong_answer') >= 0) {
      return { verdict: 'WA', detail: '答案错误（第 3 组数据）', time: '14ms', memory: '1.9MB' };
    }
    if (c.indexOf('runtime_error') >= 0 || c.indexOf('segfault') >= 0) {
      return { verdict: 'RE', detail: '运行时错误（段错误）', time: '8ms', memory: '1.2MB' };
    }
    return { verdict: 'AC', detail: '通过全部测试点', time: '15ms', memory: '2.4MB' };
  };
  /* 简答题 AI 判分模拟：按关键词覆盖与篇幅给出 0–100 分与评语，不调用真实模型 */
  TZ.gradeShortAnswer = function (p, text) {
    const answer = String(text || '').trim();
    if (!answer) return { ok: false, message: '请先填写答案' };
    if (/模拟失败|grading_error/i.test(answer)) {
      return { ok: false, message: 'AI 判分服务暂时不可用，请稍后重新提交。' };
    }
    const lower = answer.toLowerCase();
    const keywords = p.keywords || [];
    const hits = keywords.filter(function (k) { return lower.indexOf(String(k).toLowerCase()) >= 0; });
    const missed = keywords.filter(function (k) { return hits.indexOf(k) < 0; });
    let score;
    if (answer.length < 20) {
      score = 40 + (answer.length % 16);
    } else {
      score = 58 + hits.length * 8 + Math.min(10, Math.floor(answer.length / 40));
    }
    if (hits.length <= 1 && answer.length >= 20) score = Math.min(score, 59);
    score = Math.max(0, Math.min(100, score));
    const passed = score >= 60;
    let feedback;
    if (score >= 85) {
      feedback = '回答完整，覆盖了主要评分要点，表述也比较清楚。';
    } else if (score >= 60) {
      feedback = '回答基本正确，覆盖了部分评分要点，但论证或关键术语还不够完整。';
    } else if (answer.length < 20) {
      feedback = '答案过于简略，未能覆盖评分要点，建议展开说明关键机制与理由。';
    } else {
      feedback = '回答没有覆盖核心评分要点，建议对照参考答案补充关键概念与推理过程。';
    }
    if (hits.length) feedback += '已覆盖：' + hits.join('、') + '。';
    if (missed.length && score < 85) feedback += '仍可补充：' + missed.slice(0, 3).join('、') + '。';
    return { ok: true, score: score, passed: passed, feedback: feedback, hits: hits, missed: missed };
  };

  TZ.hasPriorAC = function (userId, problemId) {
    return TZ.db.submissions.some(function (s) {
      return s.userId === userId && s.problemId === problemId && s.type === 'oj' && s.verdict === 'AC';
    });
  };
  TZ.bumpInSystem = function (userId) {
    let entry = TZ.db.leaderboard.find(function (e) { return e.userId === userId; });
    if (!entry) {
      entry = { userId: userId, inSystem: 0, cf: 0, inSystemAllTime: 0, cfAllTime: 0 };
      TZ.db.leaderboard.push(entry);
    }
    entry.inSystem = Number(entry.inSystem || 0) + 1;
    entry.inSystemAllTime = Number(entry.inSystemAllTime || 0) + 1;
    TZ.save();
  };
  TZ.addWrong = function (userId, problemId, error, meta) {
    const extra = meta || {};
    let row = TZ.db.wrongBook.find(function (w) { return w.userId === userId && w.problemId === problemId; });
    if (row) {
      row.count += 1;
      row.latestError = error;
      if (extra.latestScore != null) row.latestScore = extra.latestScore;
      row.lastAt = new Date().toISOString();
    } else {
      TZ.db.wrongBook.unshift({
        id: 'w_' + Date.now().toString(36),
        userId: userId,
        problemId: problemId,
        latestError: error,
        latestScore: extra.latestScore != null ? extra.latestScore : null,
        count: 1,
        lastAt: new Date().toISOString()
      });
    }
    TZ.save();
  };
  TZ.removeWrong = function (userId, problemId) {
    TZ.db.wrongBook = TZ.db.wrongBook.filter(function (w) {
      return !(w.userId === userId && w.problemId === problemId);
    });
    TZ.save();
  };
  TZ.saveSubmission = function (sub) {
    TZ.db.submissions.unshift(sub);
    TZ.save();
  };
  TZ.getSubmissions = function (userId, problemId) {
    return TZ.db.submissions.filter(function (s) {
      return s.userId === userId && (!problemId || s.problemId === problemId);
    });
  };

  /* ---------- 外部账号 ---------- */
  TZ.CF_PRESET = {
    'demo_cf': 12,
    'tourist': 32,
    'Benq': 28,
    'neal': 20,
    'ecnerwala': 25,
    'Petr': 18,
    'maroonrk': 22
  };
  /* 演示用 CF 历史累计过题数 */
  TZ.CF_ALLTIME_PRESET = {
    'demo_cf': 156,
    'tourist': 3120,
    'Benq': 2800,
    'neal': 1960,
    'ecnerwala': 2450,
    'Petr': 1780,
    'maroonrk': 2150
  };
  TZ.bindCf = function (userId, handle) {
    const user = TZ.getUser(userId);
    if (!user) return { ok: false, message: '用户不存在' };
    handle = (handle || '').trim();
    if (!handle) return { ok: false, message: '请输入 Codeforces Handle' };
    if (Object.prototype.hasOwnProperty.call(TZ.CF_PRESET, handle) === false) {
      return { ok: false, message: '校验失败：该 Handle 不在演示白名单中' };
    }
    const taken = TZ.db.users.find(function (u) { return u.id !== userId && u.cfHandle === handle; });
    if (taken) return { ok: false, message: '该 Handle 已被其他选手绑定' };
    user.cfHandle = handle;
    user.cfVerified = true;
    let entry = TZ.db.leaderboard.find(function (e) { return e.userId === userId; });
    if (!entry) {
      entry = { userId: userId, inSystem: 0, cf: 0, inSystemAllTime: 0, cfAllTime: 0 };
      TZ.db.leaderboard.push(entry);
    }
    entry.cf = TZ.CF_PRESET[handle];
    entry.cfAllTime = Math.max(Number(entry.cfAllTime || 0), TZ.CF_ALLTIME_PRESET[handle] || entry.cf);
    TZ.save();
    return { ok: true, count: entry.cf };
  };
  TZ.unbindCf = function (userId) {
    const user = TZ.getUser(userId);
    if (!user) return;
    user.cfHandle = '';
    user.cfVerified = false;
    TZ.save();
  };
  TZ.saveManual = function (userId, platform, handle, count) {
    let row = TZ.db.manualDeclarations.find(function (m) {
      return m.userId === userId && m.week === TZ.WEEK && m.platform === platform;
    });
    if (row) {
      row.handle = handle;
      row.count = Number(count);
      row.createdAt = new Date().toISOString();
    } else {
      TZ.db.manualDeclarations.push({
        id: TZ.uid('manual'),
        userId: userId,
        platform: platform,
        handle: handle,
        count: Number(count),
        week: TZ.WEEK,
        createdAt: new Date().toISOString()
      });
    }
    TZ.save();
  };
  TZ.deleteManual = function (id) {
    TZ.db.manualDeclarations = TZ.db.manualDeclarations.filter(function (m) { return m.id !== id; });
    TZ.save();
  };

  /* ---------- 造数据 ---------- */
  TZ.makeDataFiles = function (task) {
    const vars = (task.variables && task.variables.length) ? task.variables : [{ name: 'n', type: 'int', min: 1, max: 100 }];
    const count = Math.max(1, Math.min(30, Number(task.groupCount) || 10));
    return Array.from({ length: count }, function (_, i) {
      const values = vars.map(function (v) {
        const min = Number(v.min || 0);
        const max = Number(v.max || 100);
        if (v.type === 'double') {
          return (min + Math.random() * (max - min)).toFixed(3);
        }
        const lo = Math.min(min, max);
        const hi = Math.max(min, max);
        return String(Math.floor(lo + Math.random() * (hi - lo + 1)));
      });
      const input = values.join(' ') + '\n';
      const output = String(values.reduce(function (s, v) { return s + Number(v); }, 0)) + '\n';
      return {
        index: i + 1,
        input: input,
        output: output,
        inputSize: input.length + 16,
        outputSize: output.length + 6,
        stdTime: Math.floor(10 + Math.random() * 60),
        status: 'OK'
      };
    });
  };
  TZ.bundleTask = function (task) {
    let text = '题阵 · 造数据任务 ' + task.title + '\n';
    text += '任务编号：' + task.id + '\n';
    text += '生成时间：' + TZ.fmtDate(task.createdAt) + '\n';
    text += '数据组数：' + task.files.length + '\n\n';
    task.files.forEach(function (f) {
      text += '===== 第 ' + f.index + ' 组输入 =====\n' + f.input + '\n';
      text += '===== 第 ' + f.index + ' 组输出 =====\n' + f.output + '\n\n';
    });
    return text;
  };

  /* ---------- 操作日志 ---------- */
  TZ.addLog = function (action, target, detail) {
    const admin = TZ.currentAdmin();
    if (!admin) return;
    TZ.db.logs.unshift({
      id: TZ.uid('log'),
      adminId: admin.id,
      adminName: admin.name,
      role: admin.role,
      action: action,
      target: target,
      detail: detail || '',
      createdAt: new Date().toISOString()
    });
    TZ.save();
  };

  /* ---------- 通用 ---------- */
  TZ.fmtDate = function (iso, withTime) {
    if (!iso) return '-';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '-';
    const pad = function (n) { return String(n).padStart(2, '0'); };
    const base = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    return withTime === false ? base : base + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  };
  TZ.fmtRelative = function (iso) {
    if (!iso) return '-';
    const diff = Date.now() - new Date(iso).getTime();
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前';
    return TZ.fmtDate(iso, false);
  };
  TZ.download = function (filename, content) {
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  };
  TZ.copyText = function (text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(text);
    }
    return new Promise(function (resolve) {
      const ta = document.createElement('textarea');
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      try { document.execCommand('copy'); } catch (e) {}
      document.body.removeChild(ta);
      resolve();
    });
  };
})();
