import request from '@/utils/request'

export function getAlertPage(params) {
  return request({
    url: '/alert/page',
    method: 'get',
    params
  })
}

export function handleAlert(data) {
  return request({
    url: '/alert/handle',
    method: 'put',
    data
  })
}

export function getAlertStats() {
  return request({
    url: '/alert/stats',
    method: 'get'
  })
}
