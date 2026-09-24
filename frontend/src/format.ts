export function formatWhen(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${day}-${month}-${year} ${hours}:${minutes}`;
}

export function formatLabels(labels: Record<string, string> | undefined): string {
  if (!labels || Object.keys(labels).length === 0) {
    return "—";
  }
  return Object.entries(labels)
    .map(([key, value]) => `${key}=${value}`)
    .join(", ");
}

export function formatLabelValues(labels: Record<string, string> | undefined): string {
  if (!labels || Object.keys(labels).length === 0) {
    return "—";
  }
  return Object.values(labels)
    .map((value) => (value ? value.charAt(0).toUpperCase() + value.slice(1) : value))
    .join(", ");
}

export function messageOf(err: unknown): string {
  return err instanceof Error ? err.message : "Something went wrong";
}
