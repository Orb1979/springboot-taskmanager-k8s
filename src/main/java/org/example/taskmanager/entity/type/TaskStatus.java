package org.example.taskmanager.entity.type;

/*
PENDING   → created in DB, not yet submitted to k8s
RUNNING   → pod observed as actually running (needs a watch/poll)
COMPLETED → job finished successfully
FAILED    → job creation failed, OR job ran and failed
CANCELLED → job got canceled by user
*/

public enum TaskStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELED }