import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { DataTable } from "../components/DataTable";
import { formatLabelValues, formatWhen, messageOf } from "../format";
import { fetchJobs, useDeleteJob, useDeleteJobs, useJobs } from "../hooks/useJobs";
import { createAppColumnHelper } from "../table";
import type { K8sJob } from "../types";

const columnHelper = createAppColumnHelper<K8sJob>();

export function JobListPage() {
  const [filterLabel, setFilterLabel] = useState("");
  const [appliedLabel, setAppliedLabel] = useState("");
  const [deleteLabel, setDeleteLabel] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data: jobs, isPending, error: jobsError } = useJobs(appliedLabel);
  const deleteJobMutation = useDeleteJob();
  const deleteJobsMutation = useDeleteJobs();

  const busy = refreshing || deleteJobMutation.isPending || deleteJobsMutation.isPending;
  const error = actionError ?? (jobsError ? messageOf(jobsError) : null);

  async function onRefresh(event: FormEvent) {
    event.preventDefault();
    setRefreshing(true);
    setActionError(null);
    try {
      const next = filterLabel;
      setAppliedLabel(next);
      await fetchJobs(next);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    } finally {
      setRefreshing(false);
    }
  }

  async function onDelete(name: string) {
    if (!window.confirm(`Delete job ${name}?`)) return;
    setActionError(null);
    try {
      await deleteJobMutation.mutateAsync(name);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    }
  }

  async function onDeleteAll(event: FormEvent) {
    event.preventDefault();
    const label = deleteLabel.trim();
    const prompt = label ? `Delete jobs with label name=${label}?` : "Delete all jobs?";
    if (!window.confirm(prompt)) return;
    setActionError(null);
    try {
      await deleteJobsMutation.mutateAsync(label);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    }
  }

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor((job) => job.metadata?.name ?? "", {
          id: "name",
          header: "Name",
          minSize: 96,
        }),
        columnHelper.accessor((job) => formatLabelValues(job.metadata?.labels), {
          id: "labels",
          header: "Labels",
          minSize: 120,
        }),
        columnHelper.accessor((job) => job.status?.active ?? 0, {
          id: "active",
          header: "Active",
          minSize: 72,
        }),
        columnHelper.accessor((job) => job.status?.succeeded ?? 0, {
          id: "succeeded",
          header: "Succeeded",
          minSize: 88,
        }),
        columnHelper.accessor((job) => job.status?.failed ?? 0, {
          id: "failed",
          header: "Failed",
          minSize: 72,
        }),
        columnHelper.accessor(
          (job) => {
            const started = job.status?.startTime ?? "";
            const created = job.metadata?.creationTimestamp ?? "";
            return started > created ? started : created;
          },
          {
            id: "started",
            header: "Started",
            minSize: 88,
            cell: ({ row }) => formatWhen(row.original.status?.startTime),
          },
        ),
        columnHelper.display({
          id: "actions",
          header: "Actions",
          minSize: 96,
          enableSorting: false,
          enableGlobalFilter: false,
          cell: ({ row }) => {
            const name = row.original.metadata?.name ?? "";
            return (
              <div className="table-actions">
                <Link
                  className="button-link secondary"
                  to={`/jobs/${encodeURIComponent(name)}`}
                  aria-disabled={!name || busy}
                >
                  Details
                </Link>
                <button
                  type="button"
                  className="danger"
                  disabled={busy || !name}
                  onClick={() => onDelete(name)}
                >
                  Delete
                </button>
              </div>
            );
          },
        }),
      ]),
    [busy],
  );

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Jobs</h2>
      </div>

      {error && <p className="banner banner-error">{error}</p>}

      <form className="job-action-row" onSubmit={onRefresh}>
        <label htmlFor="job-filter-label">Filter by label</label>
        <input
          id="job-filter-label"
          value={filterLabel}
          placeholder="Value of label key name, e.g. worker"
          onChange={(event) => setFilterLabel(event.target.value)}
        />
        <button type="submit" disabled={busy}>
          {busy ? "Working…" : "Refresh"}
        </button>
      </form>

      <form className="job-action-row" onSubmit={onDeleteAll}>
        <label htmlFor="job-delete-label">Delete by label</label>
        <input
          id="job-delete-label"
          value={deleteLabel}
          placeholder="Optional. Empty deletes every job"
          onChange={(event) => setDeleteLabel(event.target.value)}
        />
        <button type="submit" className="danger" disabled={busy}>
          Delete jobs
        </button>
      </form>

      {isPending && !jobs ? (
        <p className="muted">Loading jobs…</p>
      ) : (
        <DataTable
          data={jobs ?? []}
          columns={columns}
          getRowId={(job, index) => job.metadata?.name ?? String(index)}
          initialSorting={[{ id: "started", desc: true }]}
          searchPlaceholder="Search jobs…"
          emptyMessage={(jobs ?? []).length === 0 ? "No jobs found." : "No jobs match that search."}
        />
      )}
    </section>
  );
}
