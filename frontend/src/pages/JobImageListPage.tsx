import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { DataTable } from "../components/DataTable";
import { messageOf } from "../format";
import { useCreateJobImage, useDeleteJobImage, useJobImages } from "../hooks/useJobImages";
import { createAppColumnHelper } from "../table";
import type { JobImage } from "../types";

const columnHelper = createAppColumnHelper<JobImage>();

export function JobImageListPage() {
  const { data: images, isPending, error: imagesError } = useJobImages();
  const createImageMutation = useCreateJobImage();
  const deleteImageMutation = useDeleteJobImage();

  const [createOpen, setCreateOpen] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [createForm, setCreateForm] = useState({ imageName: "", description: "" });

  const creating = createImageMutation.isPending;
  const busyId = deleteImageMutation.isPending ? deleteImageMutation.variables : null;
  const error = actionError ?? (imagesError ? messageOf(imagesError) : null);

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
    setCreateForm({ imageName: "", description: "" });
  }

  async function onCreate(event: FormEvent) {
    event.preventDefault();
    const imageName = createForm.imageName.trim();
    if (!imageName) {
      setCreateError("Image name is required.");
      return;
    }
    setCreateError(null);
    try {
      await createImageMutation.mutateAsync({
        imageName,
        description: createForm.description.trim() || null,
      });
      setCreateForm({ imageName: "", description: "" });
      setCreateOpen(false);
    } catch (err: unknown) {
      setCreateError(messageOf(err));
    }
  }

  async function onDelete(image: JobImage) {
    if (!window.confirm(`Delete image ${image.imageName}?`)) return;
    setActionError(null);
    try {
      await deleteImageMutation.mutateAsync(image.id);
    } catch (err: unknown) {
      setActionError(messageOf(err));
    }
  }

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("id", { header: "Id", minSize: 72 }),
        columnHelper.accessor("imageName", { header: "Image name", minSize: 160 }),
        columnHelper.display({
          id: "actions",
          header: "Actions",
          minSize: 180,
          enableSorting: false,
          enableGlobalFilter: false,
          cell: ({ row }) => {
            const image = row.original;
            const busy = busyId === image.id;
            return (
              <div className="table-actions">
                <button type="button" className="danger" disabled={busy} onClick={() => onDelete(image)}>
                  Delete
                </button>
                <Link className="button-link secondary" to={`/job-images/${image.id}`}>
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
        <h2>Job images</h2>
        <button type="button" onClick={openCreate}>
          Create Job Image
        </button>
      </div>

      {error && <p className="banner banner-error">{error}</p>}

      {createOpen && (
        <div className="overlay" onClick={closeCreate} role="presentation">
          <div
            className="overlay-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-job-image-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="card-head">
              <h3 id="create-job-image-title">Create Job Image</h3>
              <button type="button" className="secondary" disabled={creating} onClick={closeCreate}>
                Close
              </button>
            </div>

            {createError && <p className="banner banner-error">{createError}</p>}

            <form className="form-grid" onSubmit={onCreate}>
              <label htmlFor="create-image-name">Image name</label>
              <input
                id="create-image-name"
                value={createForm.imageName}
                onChange={(event) => setCreateForm({ ...createForm, imageName: event.target.value })}
                required
                autoFocus
              />

              <label htmlFor="create-image-description">Description</label>
              <textarea
                id="create-image-description"
                rows={3}
                value={createForm.description}
                onChange={(event) => setCreateForm({ ...createForm, description: event.target.value })}
              />

              <span />
              <div className="button-row">
                <button type="submit" disabled={creating}>
                  {creating ? "Creating…" : "Create"}
                </button>
                <button type="button" className="secondary" disabled={creating} onClick={closeCreate}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isPending && !images ? (
        <p className="muted">Loading job images…</p>
      ) : (
        <DataTable
          data={images ?? []}
          columns={columns}
          getRowId={(image) => String(image.id)}
          initialSorting={[{ id: "id", desc: true }]}
          searchPlaceholder="Search job images…"
          emptyMessage={(images ?? []).length === 0 ? "No job images yet." : "No job images match that search."}
        />
      )}
    </section>
  );
}
