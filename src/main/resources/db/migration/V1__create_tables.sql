CREATE TABLE task (
  id            BIGSERIAL PRIMARY KEY,
  reference_id  UUID NOT NULL UNIQUE,
  name          VARCHAR(50) NOT NULL,
  payload       JSONB,
  created_at    TIMESTAMP NOT NULL DEFAULT now(),
  updated_at    TIMESTAMP NOT NULL DEFAULT now(),
  finished_at   TIMESTAMP,
  priority      VARCHAR(10) NOT NULL CHECK (priority IN ('HIGH','MEDIUM','LOW')),
  status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUBMITTED', 'RUNNING','COMPLETED','FAILED','CANCELED'))
);

CREATE TABLE task_history (
  id            BIGSERIAL PRIMARY KEY,
  created_at    TIMESTAMP NOT NULL DEFAULT now(),
  status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED','CANCELED')),
  error_message TEXT,
  task_id       BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE
);

CREATE INDEX idx_task_history_task_id ON task_history(task_id);
CREATE INDEX idx_task_status ON task(status);