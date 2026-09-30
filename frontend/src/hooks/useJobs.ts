import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { deleteJob, deleteJobs, getJob, listJobs } from "../api";
import { LIVE_POLL_INTERVAL_MS, queryClient } from "../queryClient";
import { queryKeys } from "../queryKeys";

export function useJobs(label: string) {
  return useQuery({
    queryKey: queryKeys.jobs.list(label),
    queryFn: () => listJobs(label),
    refetchInterval: LIVE_POLL_INTERVAL_MS,
  });
}

export function fetchJobs(label: string) {
  return queryClient.fetchQuery({
    queryKey: queryKeys.jobs.list(label),
    queryFn: () => listJobs(label),
  });
}

export function useJob(name: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.jobs.detail(name),
    queryFn: () => getJob(name),
    enabled,
    refetchInterval: LIVE_POLL_INTERVAL_MS,
  });
}

function invalidateJobs(queryClient: QueryClient) {
  return queryClient.invalidateQueries({ queryKey: queryKeys.jobs.all });
}

export function useDeleteJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteJob,
    onSuccess: () => invalidateJobs(queryClient),
  });
}

export function useDeleteJobs() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteJobs,
    onSuccess: () => invalidateJobs(queryClient),
  });
}
