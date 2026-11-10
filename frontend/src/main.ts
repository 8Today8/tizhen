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

import App from './App.vue'
import router from './router'
import { useTheme } from './composables/useTheme'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

useTheme().initializeTheme()

app.mount('#app')
