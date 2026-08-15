import request from '@/utils/request'

export function getProductPage(params) {
  return request({
    url: '/product/page',
    method: 'get',
    params
  })
}

export function addProduct(data) {
  return request({
    url: '/product',
    method: 'post',
    data
  })
}

export function updateProduct(data) {
  return request({
    url: '/product',
    method: 'put',
    data
  })
}

export function deleteProduct(id) {
  return request({
    url: `/product/${id}`,
    method: 'delete'
  })
}

export function adjustStock(data) {
  return request({
    url: '/product/adjustStock',
    method: 'post',
    data
  })
}

export function batchAdjustStock(data) {
  return request({
    url: '/product/batchAdjustStock',
    method: 'post',
    data
  })
}
