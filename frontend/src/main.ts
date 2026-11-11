import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/tokens.css'
import './styles/base.css'
import './styles/admin.css'
import './styles/themes/light.css'
import './styles/themes/dusk.css'
import './styles/auth.css'
import './styles/learning.css'
import './styles/problem.css'
import './styles/leaderboard.css'
import './styles/battle.css'
import './styles/datagen.css'
import './styles/xcpc.css'
import './styles/profile.css'
import './styles/records.css'

import App from './App.vue'
import router from './router'
import { useTheme } from './composables/useTheme'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

useTheme().initializeTheme()

app.mount('#app')
