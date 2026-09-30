ALTER TABLE task_history DROP CONSTRAINT IF EXISTS task_history_status_check;

ALTER TABLE task_history
  ADD CONSTRAINT task_history_status_check
  CHECK (status IN ('PENDING', 'SUBMITTED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELED'));
