// 中文语言包
export default {
  app: {
    title: '支付处理系统',
    navList: '支付列表',
    navCreate: '创建支付'
  },
  language: {
    label: '语言',
    zh: '中文',
    en: 'English',
    de: 'Deutsch'
  },
  list: {
    title: '支付列表',
    newPayment: '新建支付',
    status: '状态',
    allStatuses: '全部状态',
    keyword: '关键字',
    keywordPlaceholder: '按支付 ID 或备注搜索',
    search: '查询',
    reset: '重置',
    columns: {
      index: '#',
      paymentId: '支付 ID',
      fromAccount: '付款账户',
      toAccount: '收款账户',
      amount: '金额',
      status: '状态',
      remark: '备注',
      createdAt: '创建时间'
    },
    empty: '暂无支付记录'
  },
  create: {
    title: '创建支付',
    fromAccount: '付款账户',
    fromAccountPlaceholder: '例如 ACC10001',
    toAccount: '收款账户',
    toAccountPlaceholder: '例如 ACC20002',
    amount: '金额',
    currency: '币种',
    currencyPlaceholder: '请选择币种',
    remark: '备注',
    remarkPlaceholder: '可选备注，例如 invoice-2026-07',
    idempotencyKey: '幂等键',
    regenerate: '重新生成',
    submit: '提交支付',
    reset: '重置',
    submitSuccess: '支付提交成功',
    validation: {
      fromRequired: '付款账户为必填项',
      toRequired: '收款账户为必填项',
      toDifferent: '收款账户不能与付款账户相同',
      amountRequired: '金额为必填项',
      amountRange: '金额必须介于 0.01 与 1,000,000 之间',
      currencyRequired: '币种为必填项',
      idempotencyRequired: '幂等键为必填项'
    }
  },
  detail: {
    title: '支付详情',
    basicInfo: '基础信息',
    paymentId: '支付 ID',
    idempotencyKey: '幂等键',
    fromAccount: '付款账户',
    toAccount: '收款账户',
    amount: '金额',
    currency: '币种',
    remark: '备注',
    createdAt: '创建时间',
    updatedAt: '更新时间',
    errorCode: '错误码',
    unknown: '未知',
    noErrorMessage: '暂无错误信息',
    statusHistory: '状态变更历史',
    start: '起始'
  },
  http: {
    requestFailed: '请求处理失败',
    networkError: '网络请求异常'
  }
};
