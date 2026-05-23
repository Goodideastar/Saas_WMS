import request from '@/utils/request'

export function getOutboundPage(params) {
  return request({
    url: '/outbound/page',
    method: 'get',
    params
  })
}

export function createOutbound(data) {
  return request({
    url: '/outbound',
    method: 'post',
    data
  })
}

export function auditOutbound(id) {
  return request({
    url: `/outbound/${id}/audit`,
    method: 'put'
  })
}

export function cancelOutbound(id) {
  return request({
    url: `/outbound/${id}/cancel`,
    method: 'put'
  })
}

export function getOutboundDetail(id) {
  return request({
    url: `/outbound/${id}`,
    method: 'get'
  })
}
