import request from '@/utils/request'

export function getInboundPage(params) {
  return request({
    url: '/inbound/page',
    method: 'get',
    params
  })
}

export function createInbound(data) {
  return request({
    url: '/inbound',
    method: 'post',
    data
  })
}

export function auditInbound(id) {
  return request({
    url: `/inbound/audit/${id}`,
    method: 'put'
  })
}

export function cancelInbound(id) {
  return request({
    url: `/inbound/cancel/${id}`,
    method: 'put'
  })
}

export function getInboundDetail(id) {
  return request({
    url: `/inbound/${id}`,
    method: 'get'
  })
}
