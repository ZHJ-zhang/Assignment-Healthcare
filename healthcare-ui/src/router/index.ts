import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { constantRoutes } from './constant'

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 已注册的动态路由对应的移除函数，重新加载菜单时先清掉上一批，避免残留。
 */
const removeDynamicRoutes: Array<() => void> = []
let catchAllAdded = false

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (!userStore.token) {
    if (to.path === '/login') {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return
  }

  if (to.path === '/login') {
    next({ path: '/' })
    return
  }

  if (!userStore.userInfo) {
    try {
      await userStore.loadProfile()
    } catch (e) {
      userStore.reset()
      next({ path: '/login' })
      return
    }
  }

  if (!permissionStore.loaded) {
    try {
      // 清理上一个会话残留的动态路由
      removeDynamicRoutes.splice(0).forEach((remove) => remove())
      const routes = await permissionStore.loadRoutes()
      routes.forEach((route) => {
        removeDynamicRoutes.push(router.addRoute(route))
      })
      if (!catchAllAdded) {
        router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/404' })
        catchAllAdded = true
      }
      next({ ...to, replace: true })
      return
    } catch (e) {
      console.error('动态路由加载失败：', e)
      removeDynamicRoutes.splice(0).forEach((remove) => remove())
      permissionStore.reset()
      // 清掉登录态回到登录页，避免停留在 /404 反复触发守卫形成死循环
      userStore.reset()
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

export default router
