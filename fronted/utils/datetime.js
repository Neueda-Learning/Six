export function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  if (typeof value !== 'string') {
    return String(value);
  }

  return value.replace('T', ' ').replace(/\.\d+$/, '');
}