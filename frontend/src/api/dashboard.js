import request from '@/utils/request'

export function getTodaySummary() {
  return request({
    url: '/dashboard/today-summary',
    method: 'get'
  })
}

export function getLast7DaysTrend() {
  return request({
    url: '/dashboard/last-7-days-trend',
    method: 'get'
  })
}

export function getAlertStats() {
  return request({
    url: '/dashboard/alert-stats',
    method: 'get'
  })
}

export function getTopProducts() {
  return request({
    url: '/dashboard/top-products',
    method: 'get'
  })
}

export function getWarehouseDistribution() {
  return request({
    url: '/dashboard/warehouse-distribution',
    method: 'get'
  })
}
