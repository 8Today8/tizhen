<script setup lang="ts">
import { Link, Lock, TopRight, TrendCharts, Trophy } from '@element-plus/icons-vue'

const XCPC_RATING_URL = 'https://hei-maom.github.io/xcpcrating/'
</script>

<template>
  <div class="xcpc-page">
    <section class="xcpc-hero">
      <div class="xcpc-hero__copy">
        <span class="xcpc-eyebrow">XCPC · EXTERNAL SERVICE</span>
        <div class="xcpc-hero__title-row">
          <h1>XCPC 评分与获奖记录</h1>
          <span class="xcpc-badge">外部服务</span>
        </div>
        <p>
          查看 ICPC / CCPC 等赛事的评分与获奖记录。题阵不复制、不修改外部数据，
          仅提供统一的站内说明与外部入口。
        </p>
        <div class="xcpc-hero__actions">
          <a
            class="xcpc-primary-link"
            :href="XCPC_RATING_URL"
            target="_blank"
            rel="noopener noreferrer"
          >
            打开 XCPC Rating
            <el-icon><TopRight /></el-icon>
          </a>
          <RouterLink class="xcpc-secondary-link" to="/">返回首页</RouterLink>
        </div>
        <p class="xcpc-hero__hint">
          <el-icon><Link /></el-icon>
          外链将在新窗口打开，题阵的当前页面不会丢失。
        </p>
      </div>

      <div class="xcpc-hero__visual" aria-hidden="true">
        <div class="xcpc-medal">
          <el-icon><Trophy /></el-icon>
          <span>XCPC</span>
        </div>
        <p>RATING &amp; AWARDS</p>
      </div>
    </section>

    <section class="xcpc-section">
      <div class="xcpc-section__head">
        <div>
          <h2>页面说明</h2>
          <p>说明页只做信息指引，评分与获奖数据以外部服务为准。</p>
        </div>
        <span class="xcpc-section__tag">站内说明页</span>
      </div>

      <div class="xcpc-grid">
        <article class="xcpc-card xcpc-card--amber">
          <span class="xcpc-card__icon"><el-icon><Trophy /></el-icon></span>
          <h3>获奖记录</h3>
          <p>在外部服务查看 ICPC / CCPC 等赛事的获奖信息与队伍记录。</p>
        </article>

        <article class="xcpc-card xcpc-card--violet">
          <span class="xcpc-card__icon"><el-icon><TrendCharts /></el-icon></span>
          <h3>评分排名</h3>
          <p>查看 XCPC Rating 与历史变化，了解当前评分区间与排名情况。</p>
        </article>

        <article class="xcpc-card xcpc-card--mint">
          <span class="xcpc-card__icon"><el-icon><Lock /></el-icon></span>
          <h3>数据边界</h3>
          <p>首期不自建获奖数据、不做评分模型，也不抓取或改动外部记录。</p>
        </article>
      </div>
    </section>

    <section class="xcpc-note">
      <span class="xcpc-note__mark">i</span>
      <p>
        XCPC Rating 由外部服务提供。点击主按钮后将在新窗口打开该站点，
        查询与登录均在外部站点完成；题阵不保存你的外部账号信息。
      </p>
    </section>
  </div>
</template>
