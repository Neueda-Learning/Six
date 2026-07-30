export function getAccountStatusLabel(status, t, te) {
  const key = `accountStatus.${status}`;
  if (status && typeof te === 'function' && te(key)) {
    return t(key);
  }
  return status || '-';
}