export type Priority = "HIGH" | "MEDIUM" | "LOW";

export type TaskStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELED";

export interface TaskHistory {
  id: number;
  taskId: number;
  createdAt: string;
  status: TaskStatus;
  errorMessage: string | null;
}

export interface Task {
  id: number;
  referenceId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
  payload: string | null;
  priority: Priority;
  status: TaskStatus;
  taskHistory: TaskHistory[];
}

export interface TaskRequest {
  name: string;
  payload: string;
  priority: Priority;
}

export interface TaskStatusUpdate {
  status: TaskStatus;
  errorMessage: string | null;
}

export interface K8sContainer {
  name?: string;
  image?: string;
  env?: Array<{ name?: string; value?: string }>;
}

export interface K8sJob {
  metadata?: {
    name?: string;
    namespace?: string;
    labels?: Record<string, string>;
    creationTimestamp?: string;
  };
  spec?: {
    template?: {
      spec?: {
        containers?: K8sContainer[];
      };
    };
  };
  status?: {
    active?: number;
    succeeded?: number;
    failed?: number;
    startTime?: string;
    completionTime?: string;
    conditions?: Array<{
      type?: string;
      status?: string;
      reason?: string;
      message?: string;
    }>;
  };
}
