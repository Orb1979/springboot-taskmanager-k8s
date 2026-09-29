import { QueryClient } from "@tanstack/react-query";

export const LIVE_POLL_INTERVAL_MS = 5_000;

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5_000,
      refetchOnWindowFocus: true,
      refetchOnReconnect: true,
    },
  },
});
