export const queryKeys = {
  tasks: {
    all: ["tasks"] as const,
    detail: (id: number) => ["tasks", id] as const,
    history: (id: number) => ["tasks", id, "history"] as const,
  },
  jobImages: {
    all: ["job-images"] as const,
    detail: (id: number) => ["job-images", id] as const,
  },
  jobs: {
    all: ["jobs"] as const,
    list: (label: string) => ["jobs", label] as const,
    detail: (name: string) => ["jobs", "detail", name] as const,
  },
};
