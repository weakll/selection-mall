import request from './request'

export const addToCart = (skuId, skuNum) => request.get(`/order/cart/auth/addToCart/${skuId}/${skuNum}`)
export const getCartList = () => request.get('/order/cart/auth/cartList')
export const deleteCart = (skuId) => request.delete(`/order/cart/auth/deleteCart/${skuId}`)
export const checkCart = (skuId, isChecked) => request.get(`/order/cart/auth/checkCart/${skuId}/${isChecked}`)
export const allCheckCart = (isChecked) => request.get(`/order/cart/auth/allCheckCart/${isChecked}`)
export const clearCart = () => request.get('/order/cart/auth/clearCart')
