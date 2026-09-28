import request from './request'

export const getTrade = () => request.get('/order/orderInfo/auth/trade')
export const buyNow = (skuId) => request.get(`/order/orderInfo/auth/buy/${skuId}`)
export const submitOrder = (data) => request.post('/order/orderInfo/auth/submitOrder', data)
export const getOrderList = (page, limit, orderStatus) => request.get(`/order/orderInfo/auth/${page}/${limit}`, { params: { orderStatus } })
export const getOrderByNo = (orderNo) => request.get(`/order/orderInfo/auth/getOrderInfoByOrderNo/${orderNo}`)
export const directPay = (orderNo) => request.get(`/order/alipay/directPay/${orderNo}`)
export const cancelOrder = (orderNo) => request.post(`/order/orderInfo/auth/cancel/${orderNo}`)
