<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft, Moon, Sunny } from '@element-plus/icons-vue'

import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const { mode, toggleTheme } = useTheme()
</script>

<template>
  <div class="auth-layout">
    <section class="auth-brand">
      <div class="auth-brand__mesh" aria-hidden="true"></div>
      <div class="auth-brand__glow auth-brand__glow--primary" aria-hidden="true"></div>
      <div class="auth-brand__glow auth-brand__glow--accent" aria-hidden="true"></div>

      <RouterLink class="auth-brand__logo" to="/">
        <span class="auth-brand__mark">阵</span>
        <span class="auth-brand__name">题阵</span>
      </RouterLink>

      <div class="auth-brand__inner">
        <p class="auth-brand__eyebrow">ALGORITHM TRAINING PLATFORM</p>
        <h1>从路线到赛场，<br /><span>把每一次提交</span><br />都变成进步。</h1>
        <p class="auth-brand__desc">
          学习路线、阶段课程、题库判题与 1v1 对战协同运作，让成长路径清晰可见。
        </p>
        <ul class="auth-brand__features">
          <li><span>01</span>路线与课程</li>
          <li><span>02</span>题库与判题</li>
          <li><span>03</span>1v1 实时对战</li>
        </ul>
      </div>

      <footer class="auth-brand__footer">
        <span>让每一步成长都有迹可循</span>
        <span class="auth-brand__version">TIZHEN · v0.8</span>
      </footer>
    </section>

    <section class="auth-panel">
      <header class="auth-panel__topbar">
        <RouterLink class="auth-mobile-brand" to="/">
          <span class="auth-mobile-brand__mark">阵</span>
          <strong>题阵</strong>
        </RouterLink>

        <div class="auth-panel__actions">
          <el-tooltip :content="mode === 'light' ? '切换到暗淡主题' : '切换到明亮主题'">
            <el-button
              circle
              plain
              :icon="mode === 'light' ? Moon : Sunny"
              aria-label="切换主题"
              @click="toggleTheme"
            />
          </el-tooltip>
          <button class="auth-back" type="button" aria-label="返回首页" @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回首页</span>
          </button>
        </div>
      </header>

      <div class="auth-panel__body">
        <RouterView />
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-layout {
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  grid-template-columns: minmax(0, 1.08fr) minmax(440px, 0.92fr);
  background: var(--tz-color-bg);
}

.auth-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(32px, 4.5vw, 64px);
  overflow: hidden;
  isolation: isolate;
  background: var(--tz-auth-brand-bg);
}

.auth-brand__mesh {
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image: radial-gradient(var(--tz-color-border-strong) 0.8px, transparent 0.8px);
  background-size: 24px 24px;
  opacity: 0.34;
  mask-image: linear-gradient(118deg, rgba(0, 0, 0, 0.9) 8%, transparent 72%);
  -webkit-mask-image: linear-gradient(118deg, rgba(0, 0, 0, 0.9) 8%, transparent 72%);
}

.auth-brand__glow {
  position: absolute;
  z-index: -1;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.auth-brand__glow--primary {
  top: -180px;
  right: -140px;
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, var(--tz-color-primary-soft) 0%, transparent 68%);
}

.auth-brand__glow--accent {
  right: -120px;
  bottom: -220px;
  width: 480px;
  height: 480px;
  background: radial-gradient(circle, var(--tz-color-accent-soft) 0%, transparent 70%);
  opacity: 0.9;
}

.auth-brand__logo {
  position: relative;
  z-index: 1;
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 12px;
  color: var(--tz-color-text);
  text-decoration: none;
}

.auth-brand__mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 13px;
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  box-shadow: 0 10px 24px rgba(115, 87, 232, 0.28);
  color: #fff;
  font-size: 21px;
  font-weight: 700;
}

.auth-brand__name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.auth-brand__inner {
  position: relative;
  z-index: 1;
  max-width: 580px;
  margin: auto 0;
  padding: 48px 0;
}

.auth-brand__eyebrow {
  display: flex;
  margin: 0 0 18px;
  align-items: center;
  gap: 10px;
  color: var(--tz-color-text-muted);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.auth-brand__eyebrow::before {
  content: '';
  width: 30px;
  height: 1px;
  background: currentColor;
}

.auth-brand h1 {
  margin: 0;
  font-size: clamp(36px, 4.1vw, 58px);
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.14;
}

.auth-brand h1 span {
  background: linear-gradient(100deg, var(--tz-color-primary), var(--tz-color-accent));
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
}

.auth-brand__desc {
  max-width: 460px;
  margin: 24px 0 0;
  color: var(--tz-color-text-secondary);
  font-size: 14px;
  line-height: 1.85;
}

.auth-brand__features {
  display: flex;
  padding: 0;
  margin: 30px 0 0;
  flex-wrap: wrap;
  gap: 10px;
  list-style: none;
}

.auth-brand__features li {
  display: inline-flex;
  padding: 9px 14px;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-pill);
  background: color-mix(in srgb, var(--tz-color-surface) 58%, transparent);
  backdrop-filter: blur(8px);
  color: var(--tz-color-text-secondary);
  font-size: 12px;
}

.auth-brand__features span {
  color: var(--tz-color-primary);
  font-family: var(--tz-font-mono);
  font-size: 10px;
  font-weight: 600;
}

.auth-brand__footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.auth-brand__version {
  font-family: var(--tz-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.auth-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 26px 40px 36px;
}

.auth-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(62% 48% at 100% 0%, var(--tz-color-primary-soft) 0%, transparent 72%);
  opacity: 0.72;
  pointer-events: none;
}

.auth-panel__topbar {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.auth-mobile-brand {
  display: none;
  align-items: center;
  gap: 10px;
  color: var(--tz-color-text);
  text-decoration: none;
}

.auth-mobile-brand__mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.auth-mobile-brand strong {
  font-size: 18px;
  letter-spacing: 0.1em;
}

.auth-panel__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-back {
  display: inline-flex;
  height: 34px;
  padding: 0 14px;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-pill);
  background: var(--tz-color-surface);
  color: var(--tz-color-text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  transition:
    border-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.auth-back:hover {
  border-color: var(--tz-color-border-strong);
  color: var(--tz-color-text);
  transform: translateY(-1px);
}

.auth-panel__body {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  padding: 28px 0;
  align-items: center;
  justify-content: center;
}

@media (max-width: 1100px) {
  .auth-layout {
    grid-template-columns: minmax(0, 1fr) minmax(420px, 0.9fr);
  }

  .auth-brand {
    padding: 40px;
  }

  .auth-brand__inner {
    padding: 32px 0;
  }
}

@media (max-width: 900px) {
  .auth-layout {
    display: block;
  }

  .auth-brand {
    display: none;
  }

  .auth-panel {
    min-height: 100vh;
    min-height: 100dvh;
    padding: 18px 20px 28px;
  }

  .auth-panel__topbar {
    justify-content: space-between;
  }

  .auth-mobile-brand {
    display: inline-flex;
  }

  .auth-panel__body {
    padding: 26px 0 16px;
  }
}

@media (max-width: 480px) {
  .auth-panel {
    padding: 14px;
  }

  .auth-panel__body {
    padding: 20px 0 10px;
  }

  .auth-back span {
    display: none;
  }

  .auth-back {
    width: 34px;
    padding: 0;
    justify-content: center;
  }
}
</style>
