import type { TaskStatus } from "../types";

export function StatusBadge({ status }: { status: TaskStatus }) {
  return <span className={`badge status-${status}`}>{status}</span>;
}
