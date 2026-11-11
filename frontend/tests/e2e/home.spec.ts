import { expect, test } from '@playwright/test'

test('首页展示品牌、导航与模块入口', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('link', { name: /题阵/ }).first()).toBeVisible()
  await expect(page.getByRole('navigation', { name: '主导航' }).getByRole('link', { name: '题库' })).toBeVisible()
  await expect(page.getByRole('heading', { level: 1 })).toContainText('学习路径')
  await expect(page.getByRole('link', { name: /学习路线/ }).first()).toBeVisible()
})

test('未登录访问受限页面会跳转登录页', async ({ page }) => {
  await page.goto('/problems')

  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('heading', { level: 1, name: '欢迎回来' })).toBeVisible()
})
