import request from '@/utils/request'

export function getWarehousePage(params) {
  return request({
    url: '/warehouse/page',
    method: 'get',
    params
  })
}

export function getWarehouseList() {
  return request({
    url: '/warehouse/list',
    method: 'get'
  })
}

export function addWarehouse(data) {
  return request({
    url: '/warehouse',
    method: 'post',
    data
  })
}

export function updateWarehouse(data) {
  return request({
    url: '/warehouse',
    method: 'put',
    data
  })
}

export function deleteWarehouse(id) {
  return request({
    url: `/warehouse/${id}`,
    method: 'delete'
  })
}
