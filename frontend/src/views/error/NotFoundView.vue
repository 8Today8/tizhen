<template>
  <div class="not-found">
    <span class="not-found__code">404</span>
    <h1>页面不存在</h1>
    <p class="tz-muted">你访问的地址可能已被移动或从未存在。</p>
    <div class="not-found__actions">
      <RouterLink class="not-found__link not-found__link--primary" to="/">返回首页</RouterLink>
      <RouterLink class="not-found__link" to="/problems">去题库看看</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  min-height: 100vh;
  padding: 24px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--tz-color-bg);
  text-align: center;
}

.not-found__code {
  background: linear-gradient(120deg, var(--tz-color-primary), var(--tz-color-accent));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size: clamp(72px, 12vw, 120px);
  font-weight: 700;
  letter-spacing: 0.06em;
}

.not-found h1 {
  margin: 8px 0 10px;
  font-size: 24px;
}

.not-found__actions {
  display: flex;
  margin-top: 26px;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.not-found__link {
  display: inline-flex;
  padding: 10px 22px;
  align-items: center;
  border: 1px solid var(--tz-color-border-strong);
  border-radius: var(--tz-radius-pill);
  background: var(--tz-color-surface);
  color: var(--tz-color-text);
  font-size: 14px;
  text-decoration: none;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.not-found__link:hover {
  border-color: var(--tz-color-primary);
}

.not-found__link--primary {
  border-color: transparent;
  background: var(--tz-color-primary);
  color: #fff;
}

.not-found__link--primary:hover {
  background: var(--el-color-primary-dark-2);
}
</style>
