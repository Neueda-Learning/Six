// English locale messages
export default {
  app: {
    title: 'Payments Processing System',
    navList: 'Payments',
    navCreate: 'Create'
  },
  language: {
    label: 'Language',
    zh: '中文',
    en: 'English',
    de: 'Deutsch'
  },
  list: {
    title: 'Payment List',
    newPayment: 'New Payment',
    status: 'Status',
    allStatuses: 'All statuses',
    keyword: 'Keyword',
    keywordPlaceholder: 'Search by payment ID or remark',
    search: 'Search',
    reset: 'Reset',
    columns: {
      index: '#',
      paymentId: 'Payment ID',
      fromAccount: 'From Account',
      toAccount: 'To Account',
      amount: 'Amount',
      status: 'Status',
      remark: 'Remark',
      createdAt: 'Created At'
    },
    empty: 'No payments found'
  },
  create: {
    title: 'Create Payment',
    fromAccount: 'From Account',
    fromAccountPlaceholder: 'e.g. ACC10001',
    toAccount: 'To Account',
    toAccountPlaceholder: 'e.g. ACC20002',
    amount: 'Amount',
    currency: 'Currency',
    currencyPlaceholder: 'Select currency',
    remark: 'Remark',
    remarkPlaceholder: 'Optional note, e.g. invoice-2026-07',
    idempotencyKey: 'Idempotency Key',
    regenerate: 'Regenerate',
    submit: 'Submit Payment',
    reset: 'Reset',
    submitSuccess: 'Payment submitted successfully',
    validation: {
      fromRequired: 'From Account is required',
      toRequired: 'To Account is required',
      toDifferent: 'To Account must be different from From Account',
      amountRequired: 'Amount is required',
      amountRange: 'Amount must be between 0.01 and 1,000,000',
      currencyRequired: 'Currency is required',
      idempotencyRequired: 'Idempotency Key is required'
    }
  },
  detail: {
    title: 'Payment Detail',
    basicInfo: 'Basic Information',
    paymentId: 'Payment ID',
    idempotencyKey: 'Idempotency Key',
    fromAccount: 'From Account',
    toAccount: 'To Account',
    amount: 'Amount',
    currency: 'Currency',
    remark: 'Remark',
    createdAt: 'Created At',
    updatedAt: 'Updated At',
    errorCode: 'Error Code',
    unknown: 'UNKNOWN',
    noErrorMessage: 'No error message provided',
    statusHistory: 'Status History',
    start: 'START'
  },
  http: {
    requestFailed: 'Request failed',
    networkError: 'Network request error'
  }
};
