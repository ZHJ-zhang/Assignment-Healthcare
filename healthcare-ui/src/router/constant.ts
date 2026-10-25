import type { RouteRecordRaw } from 'vue-router'

/**
 * 布局组件。放在这里统一导出，避免 store 与 router 各自 import 造成循环依赖。
 */
export const Layout = () => import('@/layout/index.vue')

/**
 * 固定路由：不依赖后端菜单，任何时候都必须可用。
 * 首页在这里兜底，后端 sys_menu 里同名的"首页"菜单不再重复生成动态路由。
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: 'index',
        name: 'Index',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' }
      }
    ]
  }
]

/**
 * 已经由固定路由提供的访问路径。
 * 同一个路径注册两条路由时 vue-router 只会命中先注册的那条，动态路由必须跳过它们。
 */
export const fixedPaths = ['/index']
