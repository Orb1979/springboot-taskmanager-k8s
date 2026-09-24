import type { K8sJob, Task, TaskHistory, TaskRequest, TaskStatusUpdate } from "./types";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  if (init?.body != null) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers });
  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!response.ok) {
    throw new Error(readError(text, response.status));
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

function readError(text: string, status: number): string {
  if (!text) {
    return `Request failed (${status})`;
  }
  try {
    const body = JSON.parse(text) as {
      message?: string;
      detail?: string;
      error?: string;
      title?: string;
    };
    return body.message || body.detail || body.error || body.title || `Request failed (${status})`;
  } catch {
    return text;
  }
}

export function getTasks(): Promise<Task[]> {
  return request<Task[]>("/api/v1/tasks");
}

export function getTask(id: number): Promise<Task> {
  return request<Task>(`/api/v1/tasks/${id}`);
}

export function createTask(body: TaskRequest): Promise<Task> {
  return request<Task>("/api/v1/tasks", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateTask(id: number, body: TaskRequest): Promise<Task> {
  return request<Task>(`/api/v1/tasks/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function updateTaskStatus(id: number, body: TaskStatusUpdate): Promise<Task> {
  return request<Task>(`/api/v1/tasks/${id}/status`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function executeTask(taskId: number): Promise<Task> {
  return request<Task>(`/api/v1/execute/${taskId}`);
}

export function cancelTask(taskId: number): Promise<void> {
  return request<void>(`/api/v1/execute/${taskId}`, { method: "DELETE" });
}

export function deleteTask(id: number): Promise<void> {
  return request<void>(`/api/v1/tasks/${id}`, { method: "DELETE" });
}

export function getTaskHistory(taskId: number): Promise<TaskHistory[]> {
  return request<TaskHistory[]>(`/api/v1/tasks/${taskId}/history`);
}

export function listJobs(label?: string): Promise<K8sJob[]> {
  const query = label?.trim() ? `?label=${encodeURIComponent(label.trim())}` : "";
  return request<K8sJob[]>(`/api/kubernetes/jobs${query}`);
}

export function getJob(name: string): Promise<K8sJob> {
  return request<K8sJob>(`/api/kubernetes/jobs/${encodeURIComponent(name)}`);
}

export function deleteJob(name: string): Promise<void> {
  return request<void>(`/api/kubernetes/jobs/${encodeURIComponent(name)}`, {
    method: "DELETE",
  });
}

export function deleteJobs(label?: string): Promise<void> {
  const query = label?.trim() ? `?label=${encodeURIComponent(label.trim())}` : "";
  return request<void>(`/api/kubernetes/jobs${query}`, { method: "DELETE" });
}
