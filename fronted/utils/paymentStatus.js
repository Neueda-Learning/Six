export const PAYMENT_STATUS_OPTIONS = ['CREATED', 'VALIDATED', 'SENT', 'COMPLETED', 'FAILED'];

export function getPaymentStatusLabel(status, t, te) {
  const key = `paymentStatus.${status}`;
  if (status && typeof te === 'function' && te(key)) {
    return t(key);
  }
  return status || '-';
}