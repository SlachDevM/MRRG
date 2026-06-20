export function timestampToDateInput(timestamp) {
  if (!timestamp) return '';
  // This is kept for other date fields that are actually timestamps (createdAt, etc)
  // Do NOT use for jobDate - use the date string directly
  const d = new Date(timestamp);
  const offset = d.getTimezoneOffset();
  const local = new Date(d.getTime() - offset * 60 * 1000);
  return local.toISOString().split('T')[0];
}

export function dateInputToTimestamp(dateStr) {
  if (!dateStr) return null;
  // This is kept for backward compatibility with timestamp-based dates
  // Do NOT use for jobDate - use the date string directly
  const [year, month, day] = dateStr.split('-').map(Number);
  const d = new Date(year, month - 1, day);
  return d.getTime();
}

export function startOfDay(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

export function endOfDay(date) {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d.getTime();
}

export function isSameDay(dateStr, dayDate) {
  if (!dateStr) return false;
  // dateStr is now a string in format "yyyy-MM-dd"
  const [year, month, day] = dateStr.split('-').map(Number);
  const dateToCompare = new Date(year, month - 1, day);
  return dateToCompare.toDateString() === dayDate.toDateString();
}

export function dateToISOString(date) {
  if (!date) return null;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatJobTypeLabel(type) {
  return type.replace(/_/g, ' ');
}

export function getMonday(date) {
  const d = new Date(date);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  return new Date(d.setDate(diff));
}
