// 中文语言包
export default {
  app: {
    title: '支付处理系统',
    navList: '支付列表',
    navCreate: '创建支付',
    navTrash: '最近删除',
    navAccounts: '账户余额'
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
    moveToTrash: '移到回收站',
    moveToTrashSuccess: '记录已移入回收站',
    autoRefreshing: '列表自动刷新中',
    columns: {
      index: '#',
      paymentId: '支付 ID',
      fromAccount: '付款账户',
      toAccount: '收款账户',
      amount: '金额',
      status: '状态',
      remark: '备注',
      createdAt: '创建时间',
      actions: '操作'
    },
    empty: '暂无支付记录'
  },
  trash: {
    title: '最近删除',
    keyword: '关键字',
    keywordPlaceholder: '按支付 ID 或备注搜索',
    search: '查询',
    reset: '重置',
    empty: '最近删除中暂无记录',
    restore: '恢复',
    restoreSuccess: '记录已恢复',
    confirmDelete: '确认删除',
    confirmDeleteSuccess: '记录已永久删除，无法恢复',
    confirmDeletePrompt: '确认永久删除这条记录吗？删除后将无法恢复，但数据库仍会保留。',
    cancel: '取消',
    retentionHint: '回收站默认保留 30 天，超期后不再展示但数据库仍保留记录。',
    columns: {
      paymentId: '支付 ID',
      fromAccount: '付款账户',
      toAccount: '收款账户',
      amount: '金额',
      status: '状态',
      remark: '备注',
      deletedAt: '删除时间',
      recoverableUntil: '可恢复至',
      actions: '操作'
    }
  },
  accounts: {
    title: '账户余额',
    hint: '独立账户页会展示当前系统中的账户余额快照，可按账户号或户主名称过滤。',
    keyword: '关键字',
    keywordPlaceholder: '按账户号或户主名称搜索',
    search: '查询',
    reset: '重置',
    empty: '暂无账户记录',
    columns: {
      accountNo: '账户号',
      ownerName: '户主',
      currency: '货币',
      status: '状态',
      balance: '当前余额'
    }
  },
  create: {
    title: '创建支付',
    fromAccount: '付款账户',
    fromAccountPlaceholder: '例如 ACC10001',
    toAccount: '收款账户',
    toAccountPlaceholder: '例如 ACC20002',
    checkBalance: '查询余额',
    amount: '金额',
    currency: '货币',
    currencyPlaceholder: '请选择货币',
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
    refreshStatus: '刷新状态',
    autoRefreshing: '状态自动刷新中',
    paymentId: '支付 ID',
    idempotencyKey: '幂等键',
    fromAccount: '付款账户',
    toAccount: '收款账户',
    fromAccountBalance: '付款账户当前余额',
    toAccountBalance: '收款账户当前余额',
    amount: '金额',
    currency: '货币',
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
  },
  // 前端本地化的错误码描述字典：与后端 ErrorCode 枚举一一对应。
  // 用于在错误详情展示时优先使用（而不是直接展示后端固定语言的 message），
  // 从而保证同一个错误码在三种界面语言下都能看到对应语言的描述。
  errors: {
    VALIDATION_FAILED: '基础表单字段格式校验失败',
    INSUFFICIENT_FUNDS: '付款账户可用余额不足',
    INVALID_ACCOUNT: '账户格式非法或账户不存在',
    INVALID_CURRENCY: '不支持的或不合规的货币代码',
    INVALID_AMOUNT: '金额为零、负数或超过单笔限额',
    DUPLICATE_PAYMENT: '幂等键冲突，该支付正在处理中',
    INVALID_STATUS_TRANSITION: '企图越级或逆向流转状态',
    PAYMENT_NOT_FOUND: '检索的支付记录不存在',
    PROCESSING_ERROR: '后端运行时发生非预期异常',
    NETWORK_ERROR: '模拟通道通信超时且重试次数耗尽'
  }
};
