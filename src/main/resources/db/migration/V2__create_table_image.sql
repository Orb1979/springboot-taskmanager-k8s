CREATE TABLE job_image (
  id          BIGSERIAL PRIMARY KEY,
  image_name  VARCHAR(255) NOT NULL UNIQUE,
  description TEXT
);

ALTER TABLE task
  ADD COLUMN image_id BIGINT REFERENCES job_image(id);

CREATE INDEX idx_task_image_id ON task(image_id);
