import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { StatusBadge } from "../components/StatusBadge";
import { formatWhen, messageOf } from "../format";
import { useJobImages } from "../hooks/useJobImages";
import { useTask, useTaskHistory, useUpdateTask, useUpdateTaskStatus } from "../hooks/useTasks";
import type { Priority, Task, TaskStatus } from "../types";

const PRIORITIES: Priority[] = ["HIGH", "MEDIUM", "LOW"];
const STATUSES: TaskStatus[] = ["PENDING", "SUBMITTED", "RUNNING", "COMPLETED", "FAILED", "CANCELED"];

interface TaskForm {
  name: string;
  payload: string;
  priority: Priority;
  status: TaskStatus;
  errorMessage: string;
  imageId: string;
}

function formFromTask(task: Task): TaskForm {
  return {
    name: task.name,
    payload: task.payload ?? "",
    priority: task.priority,
    status: task.status,
    errorMessage: "",
    imageId: task.image ? String(task.image.id) : "",
  };
}

export function TaskDetailPage() {
  const { id } = useParams();
  const taskId = Number(id);
  const validId = Number.isFinite(taskId);
  const { data: task, isPending: taskPending, error: taskError } = useTask(taskId, validId);
  const { data: history = [], error: historyError } = useTaskHistory(taskId, validId);
  const { data: jobImages = [] } = useJobImages();
  const updateTaskMutation = useUpdateTask();
  const updateStatusMutation = useUpdateTaskStatus();

  const [form, setForm] = useState<TaskForm | null>(null);
  const [dirty, setDirty] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const saving = updateTaskMutation.isPending || updateStatusMutation.isPending;
  const loadError = !validId
    ? "Invalid task id."
    : taskError
      ? messageOf(taskError)
      : historyError
        ? messageOf(historyError)
        : null;
  const error = saveError ?? loadError;

  useEffect(() => {
    setDirty(false);
    setForm(null);
    setSaveError(null);
  }, [taskId]);

  useEffect(() => {
    if (!task || dirty) return;
    setForm(formFromTask(task));
  }, [task, dirty]);

  function updateForm(next: TaskForm) {
    setForm(next);
    setDirty(true);
  }

  function resetForm() {
    if (task) {
      setForm(formFromTask(task));
      setDirty(false);
      setSaveError(null);
    }
  }

  async function onSave(event: FormEvent) {
    event.preventDefault();
    if (!task || !form) return;
    const name = form.name.trim();
    if (!name) {
      setSaveError("Task name is required.");
      return;
    }

    const currentImageId = task.image ? String(task.image.id) : "";
    const fieldsChanged =
      name !== task.name ||
      form.payload !== (task.payload ?? "") ||
      form.priority !== task.priority ||
      form.imageId !== currentImageId;
    const statusChanged = form.status !== task.status || form.errorMessage.trim() !== "";

    if (!fieldsChanged && !statusChanged) {
      return;
    }

    setSaveError(null);
    try {
      if (fieldsChanged) {
        await updateTaskMutation.mutateAsync({
          id: task.id,
          body: {
            name,
            payload: form.payload,
            priority: form.priority,
            imageId: form.imageId ? Number(form.imageId) : null,
          },
        });
      }
      if (statusChanged) {
        await updateStatusMutation.mutateAsync({
          id: task.id,
          body: {
            status: form.status,
            errorMessage: form.errorMessage.trim() || null,
          },
        });
      }
      setDirty(false);
    } catch (err: unknown) {
      setSaveError(messageOf(err));
    }
  }

  if (taskPending && !task) {
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
          onChange={(event) => updateForm({ ...form, name: event.target.value })}
          required
        />

        <label htmlFor="edit-payload">Payload</label>
        <textarea
          id="edit-payload"
          rows={4}
          value={form.payload}
          onChange={(event) => updateForm({ ...form, payload: event.target.value })}
        />

        <label htmlFor="edit-priority">Priority</label>
        <select
          id="edit-priority"
          value={form.priority}
          onChange={(event) => updateForm({ ...form, priority: event.target.value as Priority })}
        >
          {PRIORITIES.map((priority) => (
            <option key={priority} value={priority}>
              {priority}
            </option>
          ))}
        </select>

        <label htmlFor="edit-image">Job image</label>
        <select
          id="edit-image"
          value={form.imageId}
          onChange={(event) => updateForm({ ...form, imageId: event.target.value })}
        >
          <option value="">None</option>
          {jobImages.map((image) => (
            <option key={image.id} value={image.id}>
              {image.imageName}
            </option>
          ))}
        </select>

        <label htmlFor="edit-status">Status</label>
        <select
          id="edit-status"
          value={form.status}
          onChange={(event) => updateForm({ ...form, status: event.target.value as TaskStatus })}
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
          onChange={(event) => updateForm({ ...form, errorMessage: event.target.value })}
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
