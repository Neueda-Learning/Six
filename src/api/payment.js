// 该文件用于定义前端模块骨架，后续需完成页面逻辑、接口调用与状态处理。
import http from './http';

export function createPayment(payload) {
  //todo POST /api/payments
  return http.post('/payments', payload);
}

export function getPaymentById(id) {
  //todo GET /api/payments/{id}
  return http.get(`/payments/${id}`);
}

export function getPaymentHistory(id) {
  //todo GET /api/payments/{id}/history
  return http.get(`/payments/${id}/history`);
}

export function listPayments(params) {
  //todo GET /api/payments
  return http.get('/payments', { params });
}

export function updatePaymentStatus(id, payload) {
  //todo PATCH /api/payments/{id}/status
  return http.patch(`/payments/${id}/status`, payload);
}
