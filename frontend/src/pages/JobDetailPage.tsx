import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getJob } from "../api";
import { formatLabels, formatWhen, messageOf } from "../format";
import type { K8sJob } from "../types";

export function JobDetailPage() {
  const { name } = useParams();
  const jobName = name ? decodeURIComponent(name) : "";
  const [job, setJob] = useState<K8sJob | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!jobName) {
      setError("Missing job name.");
      setLoading(false);
      return;
    }
    let active = true;
    getJob(jobName)
      .then((next) => {
        if (active) setJob(next);
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
  }, [jobName]);

  if (loading) {
    return (
      <section className="panel">
        <p className="muted">Loading job…</p>
      </section>
    );
  }

  if (!job) {
    return (
      <section className="panel">
        {error && <p className="banner banner-error">{error}</p>}
        <Link className="button-link secondary" to="/jobs">
          Back to jobs
        </Link>
      </section>
    );
  }

  const containers = job.spec?.template?.spec?.containers ?? [];

  return (
    <section className="panel">
      <div className="card-head">
        <h2>{job.metadata?.name ?? "Job"}</h2>
        <Link className="button-link secondary" to="/jobs">
          Back to jobs
        </Link>
      </div>

      {error && <p className="banner banner-error">{error}</p>}

      <dl className="meta-grid">
        <dt>Namespace</dt>
        <dd>{job.metadata?.namespace ?? "—"}</dd>
        <dt>Labels</dt>
        <dd>{formatLabels(job.metadata?.labels)}</dd>
        <dt>Created</dt>
        <dd>{formatWhen(job.metadata?.creationTimestamp)}</dd>
        <dt>Active</dt>
        <dd>{job.status?.active ?? 0}</dd>
        <dt>Succeeded</dt>
        <dd>{job.status?.succeeded ?? 0}</dd>
        <dt>Failed</dt>
        <dd>{job.status?.failed ?? 0}</dd>
        <dt>Started</dt>
        <dd>{formatWhen(job.status?.startTime)}</dd>
        <dt>Completed</dt>
        <dd>{formatWhen(job.status?.completionTime)}</dd>
      </dl>

      {containers.length > 0 && (
        <div className="stack">
          <h4>Containers</h4>
          {containers.map((container) => (
            <div key={container.name ?? container.image} className="subcard">
              <p>
                <strong>{container.name ?? "container"}</strong>
              </p>
              <p className="meta">{container.image ?? "—"}</p>
              {(container.env ?? []).length > 0 && (
                <ul className="env-list">
                  {(container.env ?? []).map((entry) => (
                    <li key={entry.name}>
                      <span>{entry.name}</span>
                      <span>{entry.value ?? "—"}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}

      {(job.status?.conditions ?? []).length > 0 && (
        <div className="stack">
          <h4>Conditions</h4>
          <ul className="env-list">
            {(job.status?.conditions ?? []).map((condition, index) => (
              <li key={`${condition.type}-${index}`}>
                <span>
                  {condition.type} ({condition.status})
                </span>
                <span>{condition.message || condition.reason || "—"}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <details className="raw-json">
        <summary>Raw JSON</summary>
        <pre>{JSON.stringify(job, null, 2)}</pre>
      </details>
    </section>
  );
}
