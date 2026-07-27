// 该文件用于统一封装支付模块相关的后端接口调用，页面组件只需引入本文件的方法，不直接操作 Axios。
import http from './http';

/**
 * 创建支付（幂等接口）。
 * 若请求体中的 idempotencyKey 与已存在的支付相同，后端会直接返回已存在的支付记录（HTTP 200）。
 * @param {Object} payload 创建支付请求体：idempotencyKey/fromAccount/toAccount/amount/currency/remark
 */
export function createPayment(payload) {
  return http.post('/payments', payload);
}

/**
 * 按支付主键 ID 查询支付详情。
 * @param {number|string} id 支付主键 ID
 */
export function getPaymentById(id) {
  return http.get(`/payments/${id}`);
}

/**
 * 查询指定支付的状态变更历史（审计时间线）。
 * @param {number|string} id 支付主键 ID
 */
export function getPaymentHistory(id) {
  return http.get(`/payments/${id}/history`);
}

/**
 * 分页查询支付列表，支持按状态与关键字筛选。
 * @param {Object} params 查询参数：status/keyword/page/size
 */
export function listPayments(params) {
  return http.get('/payments', { params });
}

/**
 * 手工推进/修改支付状态（用于课程演示与测试，不代表真实生产流程）。
 * @param {number|string} id 支付主键 ID
 * @param {Object} payload 请求体：targetStatus/errorCode/errorMessage/remark
 */
export function updatePaymentStatus(id, payload) {
  return http.patch(`/payments/${id}/status`, payload);
}

