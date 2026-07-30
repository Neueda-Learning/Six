// English locale messages
export default {
  app: {
    title: 'Payments Processing System',
    navList: 'Payments',
    navCreate: 'Create',
    navTrash: 'Recently Deleted',
    navAccounts: 'Account Balances'
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
    moveToTrash: 'Move to Trash',
    moveToTrashSuccess: 'Record moved to recycle bin',
    autoRefreshing: 'List is refreshing automatically',
    columns: {
      index: '#',
      paymentId: 'Payment ID',
      fromAccount: 'From Account',
      toAccount: 'To Account',
      amount: 'Amount',
      status: 'Status',
      remark: 'Remark',
      createdAt: 'Created At',
      actions: 'Actions'
    },
    empty: 'No payments found'
  },
  trash: {
    title: 'Recently Deleted',
    keyword: 'Keyword',
    keywordPlaceholder: 'Search by payment ID or remark',
    search: 'Search',
    reset: 'Reset',
    empty: 'No recently deleted records',
    restore: 'Restore',
    restoreSuccess: 'Record restored',
    confirmDelete: 'Confirm Delete',
    confirmDeleteSuccess: 'Record has been permanently deleted and cannot be restored',
    confirmDeletePrompt: 'Permanently delete this record? It cannot be restored, but it will remain in the database.',
    cancel: 'Cancel',
    retentionHint: 'Recycle bin entries are shown for 30 days by default. After that, they are hidden from the UI but still preserved in the database.',
    columns: {
      paymentId: 'Payment ID',
      fromAccount: 'From Account',
      toAccount: 'To Account',
      amount: 'Amount',
      status: 'Status',
      remark: 'Remark',
      deletedAt: 'Deleted At',
      recoverableUntil: 'Recoverable Until',
      actions: 'Actions'
    }
  },
  accounts: {
    title: 'Account Balances',
    hint: 'This standalone account page shows the latest balance snapshot and supports filtering by account number or owner name.',
    keyword: 'Keyword',
    keywordPlaceholder: 'Search by account number or owner name',
    search: 'Search',
    reset: 'Reset',
    empty: 'No account records found',
    columns: {
      accountNo: 'Account No.',
      ownerName: 'Owner',
      currency: 'Currency',
      status: 'Status',
      balance: 'Current Balance'
    }
  },
  create: {
    title: 'Create Payment',
    fromAccount: 'From Account',
    fromAccountPlaceholder: 'e.g. ACC10001',
    toAccount: 'To Account',
    toAccountPlaceholder: 'e.g. ACC20002',
    checkBalance: 'Check Balance',
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
    refreshStatus: 'Refresh Status',
    autoRefreshing: 'Status is refreshing automatically',
    paymentId: 'Payment ID',
    idempotencyKey: 'Idempotency Key',
    fromAccount: 'From Account',
    toAccount: 'To Account',
    fromAccountBalance: 'Current From Balance',
    toAccountBalance: 'Current To Balance',
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
  },
  // 前端本地化的错误码描述字典：与后端 ErrorCode 枚举一一对应。
  // 用于在错误详情展示时优先使用（而不是直接展示后端固定语言的 message），
  // 从而保证同一个错误码在三种界面语言下都能看到对应语言的描述。
  errors: {
    VALIDATION_FAILED: 'Basic form field validation failed',
    INSUFFICIENT_FUNDS: 'Insufficient available balance in the source account',
    INVALID_ACCOUNT: 'Invalid account format or the account does not exist',
    INVALID_CURRENCY: 'Unsupported or non-compliant currency code',
    INVALID_AMOUNT: 'Amount is zero, negative, or exceeds the single-transaction limit',
    DUPLICATE_PAYMENT: 'Idempotency key conflict; the payment is already being processed',
    INVALID_STATUS_TRANSITION: 'Attempted an illegal or backward status transition',
    PAYMENT_NOT_FOUND: 'The requested payment ID does not exist',
    PROCESSING_ERROR: 'An unexpected backend runtime error occurred',
    NETWORK_ERROR: 'Simulated channel communication timed out and retries were exhausted'
  }
};
