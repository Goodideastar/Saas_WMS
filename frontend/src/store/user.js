import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, logout, getUserInfo as fetchUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken, getUserInfo, setUserInfo, removeUserInfo } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const storedUserInfo = getUserInfo()
  const token = ref(getToken() || '')
  const userInfo = ref(storedUserInfo || null)
  const permissions = ref(storedUserInfo?.permissions || [])
  const menuTree = ref(storedUserInfo?.menuTree || [])

  async function loginAction(loginForm) {
    const res = await login(loginForm)
    token.value = res.data.accessToken
    setToken(res.data.accessToken)
    await getUserInfoAction()
  }

  async function getUserInfoAction() {
    const res = await fetchUserInfo()
    userInfo.value = res.data
    permissions.value = res.data.permissions || []
    menuTree.value = res.data.menuTree || []
    setUserInfo(res.data)
  }

  async function logoutAction() {
    try {
      await logout()
    } finally {
      token.value = ''
      userInfo.value = null
      permissions.value = []
      menuTree.value = []
      removeToken()
      removeUserInfo()
    }
  }

  return { token, userInfo, permissions, menuTree, loginAction, getUserInfoAction, logoutAction }
})
