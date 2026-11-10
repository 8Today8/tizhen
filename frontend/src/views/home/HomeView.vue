<script setup lang="ts">
const modules = [
  {
    title: '学习路线',
    description: '路线 → 阶段 → 课程 → 阶段题目，按既定节奏推进。',
    to: '/learning',
    tone: 'violet'
  },
  {
    title: '题库',
    description: 'OJ、客观题、简答题与外链题统一浏览与训练。',
    to: '/problems',
    tone: 'mint'
  },
  {
    title: '榜单',
    description: '站内首次 AC、CF 数据与外部成绩申报合成周榜与总榜。',
    to: '/leaderboard',
    tone: 'amber'
  },
  {
    title: '1v1 对战',
    description: '邀请制房间、同题竞速，先 AC 者胜。',
    to: '/battle',
    tone: 'rose'
  },
  {
    title: 'AI 造数据',
    description: '随机生成、LLM 构造器、LLM 直接生成三种模式。',
    to: '/datagen',
    tone: 'violet'
  },
  {
    title: '学习记录',
    description: '阶段进度、提交历史与错题复习集中管理。',
    to: '/records',
    tone: 'mint'
  }
]
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="hero__content">
        <span class="hero__eyebrow">ALGORITHM TRAINING GROUND</span>
        <h1>把学习路径、刷题与对战<br />组织成一套持续的训练系统。</h1>
        <p>
          题阵围绕学习路线与阶段目标组织题目，配合榜单与 1v1 对战，
          让训练节奏、反馈与竞争保持在同一个工作台里。
        </p>
        <div class="hero__actions">
          <RouterLink class="hero__link hero__link--primary" to="/learning">进入学习路线</RouterLink>
          <RouterLink class="hero__link" to="/battle">了解 1v1 对战</RouterLink>
        </div>
      </div>

      <div class="hero__panel">
        <div class="hero__panel-head">
          <span class="hero__dot"></span>
          <span>工程状态</span>
        </div>
        <dl class="hero__stats">
          <div>
            <dt>后端模块</dt>
            <dd>M01 – M09</dd>
          </div>
          <div>
            <dt>数据表</dt>
            <dd>24</dd>
          </div>
          <div>
            <dt>当前阶段</dt>
            <dd>基础工程</dd>
          </div>
        </dl>
        <p class="hero__note">
          Flyway 管理表结构，Spring Session 承载会话，
          SPA 通过统一 API 信封与后端通信。
        </p>
      </div>
    </section>

    <section class="section">
      <div class="section__head">
        <h2>功能模块</h2>
        <p class="tz-muted">页面骨架已就位，业务功能将按模块逐步接入。</p>
      </div>

      <div class="module-grid">
        <RouterLink
          v-for="item in modules"
          :key="item.to"
          :to="item.to"
          class="module-card"
          :class="`module-card--${item.tone}`"
        >
          <span class="module-card__badge"></span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <span class="module-card__more">查看 →</span>
        </RouterLink>
      </div>
    </section>

    <section class="section">
      <div class="tz-card note-card">
        <div>
          <h2>开发状态</h2>
          <p class="tz-muted">
            本版本完成了前后端基础工程：多模块后端、统一响应与异常、MyBatis-Plus、
            Redis 会话、安全基线、迁移脚本，以及前端路由、布局、请求封装与主题。
          </p>
        </div>
        <el-tag type="warning" effect="plain" size="large">骨架版本</el-tag>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 56px;
}

.hero {
  display: grid;
  padding: 56px;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 1fr);
  gap: 48px;
  align-items: center;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-lg);
  background: var(--tz-hero-gradient);
  box-shadow: var(--tz-shadow-md);
}

.hero__eyebrow {
  color: var(--tz-color-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.hero h1 {
  margin: 16px 0 18px;
  font-size: clamp(28px, 3.6vw, 44px);
  line-height: 1.25;
  letter-spacing: -0.01em;
}

.hero p {
  max-width: 560px;
  margin: 0;
  color: var(--tz-color-text-secondary);
  line-height: 1.8;
}

.hero__actions {
  display: flex;
  margin-top: 30px;
  flex-wrap: wrap;
  gap: 12px;
}

.hero__link {
  display: inline-flex;
  padding: 12px 24px;
  align-items: center;
  border: 1px solid var(--tz-color-border-strong);
  border-radius: var(--tz-radius-pill);
  background: var(--tz-color-surface);
  color: var(--tz-color-text);
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    transform 0.18s ease;
}

.hero__link:hover {
  transform: translateY(-1px);
  border-color: var(--tz-color-primary);
}

.hero__link--primary {
  border-color: transparent;
  background: var(--tz-color-primary);
  color: #fff;
}

.hero__link--primary:hover {
  background: var(--el-color-primary-dark-2);
}

.hero__panel {
  padding: 24px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-md);
  background: var(--tz-color-surface);
  box-shadow: var(--tz-shadow-sm);
}

.hero__panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--tz-color-text-secondary);
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.hero__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--tz-color-mint);
  box-shadow: 0 0 0 4px var(--tz-color-mint-soft);
}

.hero__stats {
  display: grid;
  margin: 22px 0;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.hero__stats dt {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.hero__stats dd {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 600;
}

.hero__note {
  margin: 0;
  padding-top: 18px;
  border-top: 1px dashed var(--tz-color-border);
  color: var(--tz-color-text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.section__head {
  display: flex;
  margin-bottom: 20px;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.section__head h2 {
  margin: 0;
  font-size: 22px;
}

.module-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.module-card {
  position: relative;
  display: flex;
  padding: 24px;
  flex-direction: column;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-md);
  background: var(--tz-color-surface);
  color: var(--tz-color-text);
  text-decoration: none;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.module-card:hover {
  transform: translateY(-3px);
  border-color: var(--tz-color-border-strong);
  box-shadow: var(--tz-shadow-md);
}

.module-card__badge {
  width: 34px;
  height: 6px;
  margin-bottom: 18px;
  border-radius: 999px;
  background: var(--tz-color-primary);
}

.module-card--mint .module-card__badge {
  background: var(--tz-color-mint);
}

.module-card--amber .module-card__badge {
  background: var(--tz-color-warning);
}

.module-card--rose .module-card__badge {
  background: var(--tz-color-danger);
}

.module-card h3 {
  margin: 0 0 8px;
  font-size: 17px;
}

.module-card p {
  margin: 0 0 20px;
  color: var(--tz-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.module-card__more {
  margin-top: auto;
  color: var(--tz-color-primary);
  font-size: 13px;
  font-weight: 500;
}

.note-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.note-card h2 {
  margin: 0 0 8px;
  font-size: 18px;
}

.note-card p {
  max-width: 760px;
  margin: 0;
  line-height: 1.8;
}

@media (max-width: 1000px) {
  .hero {
    padding: 36px;
    grid-template-columns: 1fr;
  }

  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 26px 22px;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }

  .hero__stats {
    grid-template-columns: 1fr;
  }

  .note-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
