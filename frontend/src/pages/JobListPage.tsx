import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { deleteJob, deleteJobs, listJobs } from "../api";
import { DataTable } from "../components/DataTable";
import { formatLabelValues, formatWhen, messageOf } from "../format";
import { createAppColumnHelper } from "../table";
import type { K8sJob } from "../types";

const columnHelper = createAppColumnHelper<K8sJob>();

export function JobListPage() {
  const [jobs, setJobs] = useState<K8sJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterLabel, setFilterLabel] = useState("");
  const [deleteLabel, setDeleteLabel] = useState("");
  const [busy, setBusy] = useState(false);

  async function load(label = filterLabel) {
    setJobs(await listJobs(label));
  }

  useEffect(() => {
    let active = true;
    listJobs()
      .then((next) => {
        if (active) setJobs(next);
      })
      .catch((err: unknown) => {
        if (active) setError(messageOf(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function onRefresh(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await load(filterLabel);
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  async function onDelete(name: string) {
    if (!window.confirm(`Delete job ${name}?`)) return;
    setBusy(true);
    setError(null);
    try {
      await deleteJob(name);
      await load(filterLabel);
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  async function onDeleteAll(event: FormEvent) {
    event.preventDefault();
    const label = deleteLabel.trim();
    const prompt = label ? `Delete jobs with label name=${label}?` : "Delete all jobs?";
    if (!window.confirm(prompt)) return;
    setBusy(true);
    setError(null);
    try {
      await deleteJobs(label);
      await load(filterLabel);
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor((job) => job.metadata?.name ?? "", {
          id: "name",
          header: "Name",
        }),
        columnHelper.accessor((job) => formatLabelValues(job.metadata?.labels), {
          id: "labels",
          header: "Labels",
        }),
        columnHelper.accessor((job) => job.status?.active ?? 0, {
          id: "active",
          header: "Active",
        }),
        columnHelper.accessor((job) => job.status?.succeeded ?? 0, {
          id: "succeeded",
          header: "Succeeded",
        }),
        columnHelper.accessor((job) => job.status?.failed ?? 0, {
          id: "failed",
          header: "Failed",
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
            cell: ({ row }) => formatWhen(row.original.status?.startTime),
          },
        ),
        columnHelper.display({
          id: "actions",
          header: "Actions",
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

      {loading ? (
        <p className="muted">Loading jobs…</p>
      ) : (
        <DataTable
          data={jobs}
          columns={columns}
          getRowId={(job, index) => job.metadata?.name ?? String(index)}
          initialSorting={[{ id: "started", desc: true }]}
          searchPlaceholder="Search jobs…"
          emptyMessage={jobs.length === 0 ? "No jobs found." : "No jobs match that search."}
        />
      )}
    </section>
  );
}
