import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createJobImage,
  deleteJobImage,
  getJobImage,
  getJobImages,
  updateJobImage,
} from "../api";
import { queryKeys } from "../queryKeys";
import type { JobImageRequest } from "../types";

export function useJobImages() {
  return useQuery({
    queryKey: queryKeys.jobImages.all,
    queryFn: getJobImages,
  });
}

export function useJobImage(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.jobImages.detail(id),
    queryFn: () => getJobImage(id),
    enabled,
  });
}

function invalidateJobImagesAndTasks(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.jobImages.all }),
    queryClient.invalidateQueries({ queryKey: queryKeys.tasks.all }),
  ]);
}

export function useCreateJobImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createJobImage,
    onSuccess: () => invalidateJobImagesAndTasks(queryClient),
  });
}

export function useUpdateJobImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: JobImageRequest }) => updateJobImage(id, body),
    onSuccess: () => invalidateJobImagesAndTasks(queryClient),
  });
}

export function useDeleteJobImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteJobImage,
    onSuccess: () => invalidateJobImagesAndTasks(queryClient),
  });
}
