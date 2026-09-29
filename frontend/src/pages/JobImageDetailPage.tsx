import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getJobImage, updateJobImage } from "../api";
import { messageOf } from "../format";
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
  const [image, setImage] = useState<JobImage | null>(null);
  const [form, setForm] = useState<JobImageForm | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load(idToLoad: number) {
    const next = await getJobImage(idToLoad);
    setImage(next);
    setForm(formFromImage(next));
  }

  useEffect(() => {
    if (!Number.isFinite(imageId)) {
      setError("Invalid job image id.");
      setLoading(false);
      return;
    }
    let active = true;
    load(imageId)
      .catch((err: unknown) => {
        if (active) setError(messageOf(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [imageId]);

  function resetForm() {
    if (image) {
      setForm(formFromImage(image));
      setError(null);
    }
  }

  async function onSave(event: FormEvent) {
    event.preventDefault();
    if (!image || !form) return;
    const imageName = form.imageName.trim();
    if (!imageName) {
      setError("Image name is required.");
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await updateJobImage(image.id, {
        imageName,
        description: form.description.trim() || null,
      });
      await load(image.id);
    } catch (err: unknown) {
      setError(messageOf(err));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
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

      <dl className="meta-grid">shou
        <dt>Id</dt>
        <dd>{image.id}</dd>
      </dl>

      <form className="form-grid" onSubmit={onSave}>
        <label htmlFor="edit-image-name">Image name</label>
        <input
          id="edit-image-name"
          value={form.imageName}
          onChange={(event) => setForm({ ...form, imageName: event.target.value })}
          required
        />

        <label htmlFor="edit-image-description">Description</label>
        <textarea
          id="edit-image-description"
          rows={4}
          value={form.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })}
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
