import http from './http';

/**
 * 查询账户列表，支持按账户号或户主关键字过滤。
 * @param {Object} params 查询参数：keyword
 */
export function listAccounts(params) {
  return http.get('/accounts', { params });
}

/**
 * 查询指定账户当前余额。
 * @param {string} accountNo 账户号
 */
export function getAccountBalance(accountNo) {
  return http.get(`/accounts/${accountNo}/balance`);
}