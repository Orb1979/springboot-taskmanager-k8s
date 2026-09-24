import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getTask, getTaskHistory, updateTask, updateTaskStatus } from "../api";
import { StatusBadge } from "../components/StatusBadge";
import { formatWhen, messageOf } from "../format";
import type { Priority, Task, TaskHistory, TaskStatus } from "../types";

const PRIORITIES: Priority[] = ["HIGH", "MEDIUM", "LOW"];
const STATUSES: TaskStatus[] = ["PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELED"];

interface TaskForm {
  name: string;
  payload: string;
  priority: Priority;
  status: TaskStatus;
  errorMessage: string;
}

function formFromTask(task: Task): TaskForm {
  return {
    name: task.name,
    payload: task.payload ?? "",
    priority: task.priority,
    status: task.status,
    errorMessage: "",
  };
}

export function TaskDetailPage() {
  const { id } = useParams();
  const taskId = Number(id);
  const [task, setTask] = useState<Task | null>(null);
  const [history, setHistory] = useState<TaskHistory[]>([]);
  const [form, setForm] = useState<TaskForm | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load(idToLoad: number) {
    const [nextTask, nextHistory] = await Promise.all([getTask(idToLoad), getTaskHistory(idToLoad)]);
    setTask(nextTask);
    setHistory(nextHistory);
    setForm(formFromTask(nextTask));
  }

  useEffect(() => {
    if (!Number.isFinite(taskId)) {
      setError("Invalid task id.");
      setLoading(false);
      return;
    }
    let active = true;
    load(taskId)
      .catch((err: unknown) => {
        if (active) setError(messageOf(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [taskId]);

  function resetForm() {
    if (task) {
      setForm(formFromTask(task));
      setError(null);
    }
  }

  async function onSave(event: FormEvent) {
    event.preventDefault();
    if (!task || !form) return;
    const name = form.name.trim();
    if (!name) {
      setError("Task name is required.");
      return;
    }

    const fieldsChanged =
      name !== task.name || form.payload !== (task.payload ?? "") || form.priority !== task.priority;
    const statusChanged = form.status !== task.status || form.errorMessage.trim() !== "";

    if (!fieldsChanged && !statusChanged) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      if (fieldsChanged) {
        await updateTask(task.id, {
          name,
          payload: form.payload,
          priority: form.priority,
        });
      }
      if (statusChanged) {
        await updateTaskStatus(task.id, {
          status: form.status,
          errorMessage: form.errorMessage.trim() || null,
        });
      }
      await load(task.id);
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <section className="panel">
        <p className="muted">Loading task…</p>
      </section>
    );
  }

  if (!task || !form) {
    return (
      <section className="panel">
        {error && <p className="banner banner-error">{error}</p>}
        <Link className="button-link secondary" to="/tasks">
          Back to tasks
        </Link>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Task {task.name}</h2>
      </div>

      {error && <p className="banner banner-error">{error}</p>}

      <dl className="meta-grid">
        <dt>Reference ID</dt>
        <dd>{task.referenceId}</dd>
        <dt>Created</dt>
        <dd>{formatWhen(task.createdAt)}</dd>
        <dt>Updated</dt>
        <dd>{formatWhen(task.updatedAt)}</dd>
        <dt>Finished</dt>
        <dd>{formatWhen(task.finishedAt)}</dd>
      </dl>

      <form className="form-grid" onSubmit={onSave}>
        <label htmlFor="edit-name">Name</label>
        <input
          id="edit-name"
          value={form.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })}
          required
        />

        <label htmlFor="edit-payload">Payload</label>
        <textarea
          id="edit-payload"
          rows={4}
          value={form.payload}
          onChange={(event) => setForm({ ...form, payload: event.target.value })}
        />

        <label htmlFor="edit-priority">Priority</label>
        <select
          id="edit-priority"
          value={form.priority}
          onChange={(event) => setForm({ ...form, priority: event.target.value as Priority })}
        >
          {PRIORITIES.map((priority) => (
            <option key={priority} value={priority}>
              {priority}
            </option>
          ))}
        </select>

        <label htmlFor="edit-status">Status</label>
        <select
          id="edit-status"
          value={form.status}
          onChange={(event) => setForm({ ...form, status: event.target.value as TaskStatus })}
        >
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>

        <label htmlFor="edit-error">Error</label>
        <input
          id="edit-error"
          value={form.errorMessage}
          placeholder="Optional error message"
          onChange={(event) => setForm({ ...form, errorMessage: event.target.value })}
        />

        <span />
        <div className="button-row">
          <button type="submit" disabled={saving}>
            {saving ? "Saving…" : "Save"}
          </button>
          <button type="button" className="secondary" disabled={saving} onClick={resetForm}>
            Reset
          </button>
          <Link className="button-link secondary" to="/tasks">
            Back to tasks
          </Link>
        </div>
      </form>

      <div className="history">
        <h3>History</h3>
        {history.length === 0 ? (
          <p className="muted">No history entries.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>When</th>
                  <th>Status</th>
                  <th>Error</th>
                </tr>
              </thead>
              <tbody>
                {history.map((entry) => (
                  <tr key={entry.id}>
                    <td>{entry.id}</td>
                    <td>{formatWhen(entry.createdAt)}</td>
                    <td>
                      <StatusBadge status={entry.status} />
                    </td>
                    <td>{entry.errorMessage || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
