import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { DataTable } from "../components/DataTable";
import { StatusBadge } from "../components/StatusBadge";
import { formatWhen, messageOf } from "../format";
import { useCancelTask, useCreateTask, useDeleteTask, useExecuteTask, useTasks } from "../hooks/useTasks";
import { useJobImages } from "../hooks/useJobImages";
import { createAppColumnHelper } from "../table";
import type { Priority, Task } from "../types";

const PRIORITIES: Priority[] = ["HIGH", "MEDIUM", "LOW"];
const columnHelper = createAppColumnHelper<Task>();

export function TaskListPage() {
  const { data: tasks, isPending, error: tasksError } = useTasks();
  const { data: jobImages = [], error: imagesError } = useJobImages();
  const createTaskMutation = useCreateTask();
  const executeTaskMutation = useExecuteTask();
  const cancelTaskMutation = useCancelTask();
  const deleteTaskMutation = useDeleteTask();

  const [createOpen, setCreateOpen] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [createForm, setCreateForm] = useState({
    name: "",
    payload: "",
    priority: "MEDIUM" as Priority,
    imageId: "",
  });

  const creating = createTaskMutation.isPending;
  const busyId =
    (executeTaskMutation.isPending ? executeTaskMutation.variables : null) ??
    (cancelTaskMutation.isPending ? cancelTaskMutation.variables : null) ??
    (deleteTaskMutation.isPending ? deleteTaskMutation.variables : null);
  const error = actionError ?? (tasksError ? messageOf(tasksError) : null);

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

  function openCreate() {
    setCreateError(null);
    setCreateOpen(true);
  }

  function closeCreate() {
    if (creating) return;
    setCreateOpen(false);
    setCreateError(null);
    setCreateForm({ name: "", payload: "", priority: "MEDIUM", imageId: "" });
  }

  async function onCreate(event: FormEvent) {
    event.preventDefault();
    const name = createForm.name.trim();
    if (!name) {
      setCreateError("Task name is required.");
      return;
    }
    setCreateError(null);
    try {
      await createTaskMutation.mutateAsync({
        name,
        payload: createForm.payload,
        priority: createForm.priority,
        imageId: createForm.imageId ? Number(createForm.imageId) : null,
      });
      setCreateForm({ name: "", payload: "", priority: "MEDIUM", imageId: "" });
      setCreateOpen(false);
    } catch (err: unknown) {
      setCreateError(messageOf(err));
    }
  }

  async function onExecute(task: Task) {
    setActionError(null);
    try {
      await executeTaskMutation.mutateAsync(task.id);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    }
  }

  async function onCancel(task: Task) {
    setActionError(null);
    try {
      await cancelTaskMutation.mutateAsync(task.id);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    }
  }

  async function onDelete(task: Task) {
    if (!window.confirm(`Delete task ${task.name}?`)) return;
    setActionError(null);
    try {
      await deleteTaskMutation.mutateAsync(task.id);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    }
  }

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("name", { header: "Name", minSize: 100 }),
        columnHelper.accessor("referenceId", { header: "Reference ID", minSize: 100 }),
        columnHelper.accessor((task) => task.image?.imageName ?? "", {
          id: "imageName",
          header: "Image",
          minSize: 100,
          cell: ({ getValue }) => getValue() || "—",
        }),
        columnHelper.accessor("createdAt", {
          header: "Created",
          minSize: 90,
          cell: ({ getValue }) => formatWhen(getValue()),
        }),
        columnHelper.accessor("updatedAt", {
          header: "Updated",
          minSize: 90,
          cell: ({ getValue }) => formatWhen(getValue()),
        }),
        columnHelper.accessor("finishedAt", {
          header: "Finished",
          minSize: 90,
          cell: ({ getValue }) => formatWhen(getValue()),
        }),
        columnHelper.accessor("priority", { header: "Priority", minSize: 80 }),
        columnHelper.accessor("status", {
          header: "Status",
          minSize: 100,
          cell: ({ getValue }) => <StatusBadge status={getValue()} />,
        }),
        columnHelper.display({
          id: "actions",
          header: "Actions",
          minSize: 300,
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
            {!createError && imagesError && <p className="banner banner-error">{messageOf(imagesError)}</p>}

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

              <label htmlFor="create-image">Job image</label>
              <select
                id="create-image"
                value={createForm.imageId}
                onChange={(event) => setCreateForm({ ...createForm, imageId: event.target.value })}
              >
                <option value="">None</option>
                {jobImages.map((image) => (
                  <option key={image.id} value={image.id}>
                    {image.imageName}
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

      {isPending && !tasks ? (
        <p className="muted">Loading tasks…</p>
      ) : (
        <DataTable
          data={tasks ?? []}
          columns={columns}
          getRowId={(task) => String(task.id)}
          initialSorting={[{ id: "createdAt", desc: true }]}
          searchPlaceholder="Search tasks…"
          emptyMessage={(tasks ?? []).length === 0 ? "No tasks yet." : "No tasks match that search."}
        />
      )}
    </section>
  );
}
