import { expect, test } from '@playwright/test'

test('首页展示品牌、导航与模块入口', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('link', { name: /题阵/ }).first()).toBeVisible()
  await expect(page.getByRole('navigation', { name: '主导航' }).getByRole('link', { name: '题库' })).toBeVisible()
  await expect(page.getByRole('heading', { level: 1 })).toContainText('学习路径')
  await expect(page.getByRole('link', { name: /学习路线/ }).first()).toBeVisible()
})

test('未实现页面展示占位说明', async ({ page }) => {
  await page.goto('/problems')

  await expect(page.getByRole('heading', { level: 1, name: '题库' })).toBeVisible()
  await expect(page.getByText('工程骨架已就绪')).toBeVisible()
})
