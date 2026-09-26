import request from './request'

export const getIndexData = () => request.get('/product/index')
export const getProductList = (page, limit, params = {}) => request.get(`/product/${page}/${limit}`, { params })
export const getProductDetail = (skuId) => request.get(`/product/item/${skuId}`)
export const getCategoryTree = () => request.get('/product/category/findCategoryTree')
