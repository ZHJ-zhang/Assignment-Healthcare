import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { setupPermission } from './directive/permission'
import './styles/index.scss'

const app = createApp(App)

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component as any)
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
setupPermission(app)

app.mount('#app')
