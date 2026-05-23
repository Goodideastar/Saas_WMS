import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/403.vue'),
    meta: { title: '无权限' }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const asyncRoutes = [
  {
    path: '/',
    component: () => import('@/components/Layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'DataBoard' }
      },
      {
        path: 'product/list',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '货品管理', icon: 'Box', permission: 'product:list' }
      },
      {
        path: 'inbound/list',
        name: 'InboundList',
        component: () => import('@/views/inbound/InboundList.vue'),
        meta: { title: '入库管理', icon: 'Download', permission: 'inbound:query' }
      },
      {
        path: 'outbound/list',
        name: 'OutboundList',
        component: () => import('@/views/outbound/OutboundList.vue'),
        meta: { title: '出库管理', icon: 'Upload', permission: 'outbound:query' }
      },
      {
        path: 'alert/index',
        name: 'AlertList',
        component: () => import('@/views/alert/AlertList.vue'),
        meta: { title: '库存预警', icon: 'Warning', permission: 'alert:query' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...constantRoutes, ...asyncRoutes]
})

let hasAddedRoutes = false

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  document.title = to.meta?.title ? `${to.meta.title} - WMS` : 'WMS 仓储管理系统'

  const token = getToken()
  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      const userStore = useUserStore()
      if (hasAddedRoutes) {
        next()
      } else {
        try {
          if (!userStore.userInfo) {
            await userStore.getUserInfoAction()
          }
          next({ ...to, replace: true })
          hasAddedRoutes = true
        } catch (error) {
          userStore.logoutAction()
          next(`/login?redirect=${to.path}`)
        }
      }
    }
  } else {
    if (to.path === '/login') {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
