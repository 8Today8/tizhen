<script setup lang="ts">
import { reactive } from 'vue'
import { Lock, Trophy, User } from '@element-plus/icons-vue'

const form = reactive({
  username: '',
  nickname: '',
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
      <h1>创建账号</h1>
      <p class="auth-card__desc">加入题阵，沿学习路线逐步进阶，在题库和对战中验证成长。</p>
    </header>

    <el-alert
      class="auth-alert"
      title="注册功能正在建设中"
      description="选手注册接口将在账号模块完成后开放，当前页面仅用于预览注册流程与页面样式。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-form class="auth-form" label-position="top" @submit.prevent>
      <el-form-item label="用户名">
        <el-input
          v-model="form.username"
          :prefix-icon="User"
          placeholder="3–20 位字母、数字或下划线"
          disabled
        />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" placeholder="站内展示昵称" disabled />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="form.password"
          :prefix-icon="Lock"
          type="password"
          placeholder="请输入密码"
          show-password
          disabled
        />
      </el-form-item>
      <el-button class="auth-form__submit" type="primary" size="large" disabled>注册</el-button>
    </el-form>

    <div class="auth-divider">已有账号</div>
    <footer class="auth-card__foot">
      <span class="tz-muted">返回登录继续学习</span>
      <RouterLink to="/login">返回登录</RouterLink>
    </footer>
  </div>
</template>
