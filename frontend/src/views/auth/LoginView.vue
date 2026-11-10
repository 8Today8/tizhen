<script setup lang="ts">
import { reactive } from 'vue'
import { Lock, Trophy, User } from '@element-plus/icons-vue'

const form = reactive({
  username: '',
  password: ''
})
</script>

<template>
  <div class="auth-card">
    <header class="auth-card__head">
      <span class="auth-card__badge">
        <el-icon><Trophy /></el-icon>
        选手入口
      </span>
      <h1>欢迎回来</h1>
      <p class="auth-card__desc">登录后继续学习路线、提交题目，并与对手展开实时对战。</p>
    </header>

    <el-alert
      class="auth-alert"
      title="选手账号功能正在建设中"
      description="当前版本优先开放运营后台登录，选手登录、注册与参赛功能将在账号模块完成后启用。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-form class="auth-form" label-position="top" @submit.prevent>
      <el-form-item label="用户名">
        <el-input
          v-model="form.username"
          :prefix-icon="User"
          placeholder="请输入用户名"
          autocomplete="username"
          disabled
        />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="form.password"
          :prefix-icon="Lock"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          show-password
          disabled
        />
      </el-form-item>
      <el-button class="auth-form__submit" type="primary" size="large" disabled>登录</el-button>
    </el-form>

    <div class="auth-divider">相关入口</div>
    <footer class="auth-card__foot">
      <RouterLink to="/register">注册账号</RouterLink>
      <RouterLink to="/admin/login">管理员登录入口</RouterLink>
    </footer>
  </div>
</template>
