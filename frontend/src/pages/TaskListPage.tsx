import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { cancelTask, createTask, deleteTask, executeTask, getTasks } from "../api";
import { DataTable } from "../components/DataTable";
import { StatusBadge } from "../components/StatusBadge";
import { formatWhen, messageOf } from "../format";
import { createAppColumnHelper } from "../table";
import type { Priority, Task } from "../types";

const PRIORITIES: Priority[] = ["HIGH", "MEDIUM", "LOW"];
const columnHelper = createAppColumnHelper<Task>();

export function TaskListPage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [createForm, setCreateForm] = useState({
    name: "",
    payload: "",
    priority: "MEDIUM" as Priority,
  });

  async function loadTasks() {
    setTasks(await getTasks());
  }

  useEffect(() => {
    if (!createOpen) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        closeCreate();
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [createOpen, creating]);

  useEffect(() => {
    let active = true;
    getTasks()
      .then((next) => {
        if (active) setTasks(next);
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

  function openCreate() {
    setCreateError(null);
    setCreateOpen(true);
  }

  function closeCreate() {
    if (creating) return;
    setCreateOpen(false);
    setCreateError(null);
    setCreateForm({ name: "", payload: "", priority: "MEDIUM" });
  }

  async function onCreate(event: FormEvent) {
    event.preventDefault();
    const name = createForm.name.trim();
    if (!name) {
      setCreateError("Task name is required.");
      return;
    }
    setCreating(true);
    setCreateError(null);
    try {
      await createTask({
        name,
        payload: createForm.payload,
        priority: createForm.priority,
      });
      setCreateForm({ name: "", payload: "", priority: "MEDIUM" });
      setCreateOpen(false);
      await loadTasks();
    } catch (err: unknown) {
      setCreateError(messageOf(err));
    } finally {
      setCreating(false);
    }
  }

  async function onExecute(task: Task) {
    setBusyId(task.id);
    setError(null);
    try {
      await executeTask(task.id);
      await loadTasks();
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setBusyId(null);
    }
  }

  async function onCancel(task: Task) {
    setBusyId(task.id);
    setError(null);
    try {
      await cancelTask(task.id);
      await loadTasks();
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setBusyId(null);
    }
  }

  async function onDelete(task: Task) {
    if (!window.confirm(`Delete task ${task.name}?`)) return;
    setBusyId(task.id);
    setError(null);
    try {
      await deleteTask(task.id);
      await loadTasks();
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setBusyId(null);
    }
  }

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("referenceId", { header: "Reference ID" }),
        columnHelper.accessor("name", { header: "Name" }),
        columnHelper.accessor("createdAt", {
          header: "Created",
          cell: ({ getValue }) => formatWhen(getValue()),
        }),
        columnHelper.accessor("updatedAt", {
          header: "Updated",
          cell: ({ getValue }) => formatWhen(getValue()),
        }),
        columnHelper.accessor("finishedAt", {
          header: "Finished",
          cell: ({ getValue }) => formatWhen(getValue()),
        }),
        columnHelper.accessor("priority", { header: "Priority" }),
        columnHelper.accessor("status", {
          header: "Status",
          cell: ({ getValue }) => <StatusBadge status={getValue()} />,
        }),
        columnHelper.display({
          id: "actions",
          header: "Actions",
          enableSorting: false,
          enableGlobalFilter: false,
          cell: ({ row }) => {
            const task = row.original;
            const busy = busyId === task.id;
            return (
              <div className="table-actions">
                <button type="button" className="success" disabled={busy} onClick={() => onExecute(task)}>
                  Execute
                </button>
                <button type="button" className="warn" disabled={busy} onClick={() => onCancel(task)}>
                  Cancel
                </button>
                <button type="button" className="danger" disabled={busy} onClick={() => onDelete(task)}>
                  Delete
                </button>
                <Link className="button-link secondary" to={`/tasks/${task.id}`}>
                  Details
                </Link>
              </div>
            );
          },
        }),
      ]),
    [busyId],
  );

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Tasks</h2>
        <button type="button" onClick={openCreate}>
          Create Task
        </button>
      </div>

      {error && <p className="banner banner-error">{error}</p>}

      {createOpen && (
        <div className="overlay" onClick={closeCreate} role="presentation">
          <div
            className="overlay-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-task-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="card-head">
              <h3 id="create-task-title">Create Task</h3>
              <button type="button" className="secondary" disabled={creating} onClick={closeCreate}>
                Close
              </button>
            </div>

            {createError && <p className="banner banner-error">{createError}</p>}

            <form className="form-grid" onSubmit={onCreate}>
              <label htmlFor="create-name">Name</label>
              <input
                id="create-name"
                value={createForm.name}
                onChange={(event) => setCreateForm({ ...createForm, name: event.target.value })}
                required
                autoFocus
              />

              <label htmlFor="create-payload">Payload</label>
              <textarea
                id="create-payload"
                rows={3}
                value={createForm.payload}
                onChange={(event) => setCreateForm({ ...createForm, payload: event.target.value })}
              />

              <label htmlFor="create-priority">Priority</label>
              <select
                id="create-priority"
                value={createForm.priority}
                onChange={(event) =>
                  setCreateForm({ ...createForm, priority: event.target.value as Priority })
                }
              >
                {PRIORITIES.map((priority) => (
                  <option key={priority} value={priority}>
                    {priority}
                  </option>
                ))}
              </select>

              <span />
              <div className="button-row">
                <button type="submit" disabled={creating}>
                  {creating ? "Creating…" : "Create task"}
                </button>
                <button type="button" className="secondary" disabled={creating} onClick={closeCreate}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading tasks…</p>
      ) : (
        <DataTable
          data={tasks}
          columns={columns}
          getRowId={(task) => String(task.id)}
          initialSorting={[{ id: "createdAt", desc: true }]}
          searchPlaceholder="Search tasks…"
          emptyMessage={tasks.length === 0 ? "No tasks yet." : "No tasks match that search."}
        />
      )}
    </section>
  );
}
