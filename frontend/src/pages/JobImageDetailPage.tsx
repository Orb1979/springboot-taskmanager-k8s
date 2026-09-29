import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { messageOf } from "../format";
import { useJobImage, useUpdateJobImage } from "../hooks/useJobImages";
import type { JobImage } from "../types";

interface JobImageForm {
  imageName: string;
  description: string;
}

function formFromImage(image: JobImage): JobImageForm {
  return {
    imageName: image.imageName,
    description: image.description ?? "",
  };
}

export function JobImageDetailPage() {
  const { id } = useParams();
  const imageId = Number(id);
  const validId = Number.isFinite(imageId);
  const { data: image, isPending, error: imageError } = useJobImage(imageId, validId);
  const updateImageMutation = useUpdateJobImage();

  const [form, setForm] = useState<JobImageForm | null>(null);
  const [dirty, setDirty] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const saving = updateImageMutation.isPending;
  const loadError = !validId ? "Invalid job image id." : imageError ? messageOf(imageError) : null;
  const error = saveError ?? loadError;

  useEffect(() => {
    setDirty(false);
    setForm(null);
    setSaveError(null);
  }, [imageId]);

  useEffect(() => {
    if (!image || dirty) return;
    setForm(formFromImage(image));
  }, [image, dirty]);

  function updateForm(next: JobImageForm) {
    setForm(next);
    setDirty(true);
  }

  function resetForm() {
    if (image) {
      setForm(formFromImage(image));
      setDirty(false);
      setSaveError(null);
    }
  }

  async function onSave(event: FormEvent) {
    event.preventDefault();
    if (!image || !form) return;
    const imageName = form.imageName.trim();
    if (!imageName) {
      setSaveError("Image name is required.");
      return;
    }

    setSaveError(null);
    try {
      await updateImageMutation.mutateAsync({
        id: image.id,
        body: {
          imageName,
          description: form.description.trim() || null,
        },
      });
      setDirty(false);
    } catch (err: unknown) {
      setSaveError(messageOf(err));
    }
  }

  if (isPending && !image) {
    return (
      <section className="panel">
        <p className="muted">Loading job image…</p>
      </section>
    );
  }

  if (!image || !form) {
    return (
      <section className="panel">
        {error && <p className="banner banner-error">{error}</p>}
        <Link className="button-link secondary" to="/job-images">
          Back to job images
        </Link>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Job image {image.imageName}</h2>
      </div>

      {error && <p className="banner banner-error">{error}</p>}

      <dl className="meta-grid">
        <dt>Id</dt>
        <dd>{image.id}</dd>
      </dl>

      <form className="form-grid" onSubmit={onSave}>
        <label htmlFor="edit-image-name">Image name</label>
        <input
          id="edit-image-name"
          value={form.imageName}
          onChange={(event) => updateForm({ ...form, imageName: event.target.value })}
          required
        />

        <label htmlFor="edit-image-description">Description</label>
        <textarea
          id="edit-image-description"
          rows={4}
          value={form.description}
          onChange={(event) => updateForm({ ...form, description: event.target.value })}
        />

        <span />
        <div className="button-row">
          <button type="submit" disabled={saving}>
            {saving ? "Saving…" : "Save"}
          </button>
          <button type="button" className="secondary" disabled={saving} onClick={resetForm}>
            Reset
          </button>
          <Link className="button-link secondary" to="/job-images">
            Back to job images
          </Link>
        </div>
      </form>
    </section>
  );
}
