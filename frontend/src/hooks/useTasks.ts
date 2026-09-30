import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelTask,
  createTask,
  deleteTask,
  executeTask,
  getTask,
  getTaskHistory,
  getTasks,
  updateTask,
  updateTaskStatus,
} from "../api";
import { LIVE_POLL_INTERVAL_MS } from "../queryClient";
import { queryKeys } from "../queryKeys";
import type { TaskRequest, TaskStatusUpdate } from "../types";

export function useTasks() {
  return useQuery({
    queryKey: queryKeys.tasks.all,
    queryFn: getTasks,
    refetchInterval: LIVE_POLL_INTERVAL_MS,
  });
}

export function useTask(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.tasks.detail(id),
    queryFn: () => getTask(id),
    enabled,
    refetchInterval: LIVE_POLL_INTERVAL_MS,
  });
}

export function useTaskHistory(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.tasks.history(id),
    queryFn: () => getTaskHistory(id),
    enabled,
    refetchInterval: LIVE_POLL_INTERVAL_MS,
  });
}

function invalidateTasks(queryClient: QueryClient) {
  return queryClient.invalidateQueries({ queryKey: queryKeys.tasks.all });
}

function invalidateTasksAndJobs(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.tasks.all }),
    queryClient.invalidateQueries({ queryKey: queryKeys.jobs.all }),
  ]);
}

export function useCreateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createTask,
    onSuccess: () => invalidateTasks(queryClient),
  });
}

export function useUpdateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: TaskRequest }) => updateTask(id, body),
    onSuccess: () => invalidateTasks(queryClient),
  });
}

export function useUpdateTaskStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: TaskStatusUpdate }) => updateTaskStatus(id, body),
    onSuccess: () => invalidateTasks(queryClient),
  });
}

export function useExecuteTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: executeTask,
    onSuccess: () => invalidateTasksAndJobs(queryClient),
  });
}

export function useCancelTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: cancelTask,
    onSuccess: () => invalidateTasksAndJobs(queryClient),
  });
}

export function useDeleteTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteTask,
    onSuccess: () => invalidateTasks(queryClient),
  });
}
