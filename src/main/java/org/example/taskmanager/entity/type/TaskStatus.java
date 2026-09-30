package org.example.taskmanager.entity.type;

/*
PENDING   → created in DB, not yet submitted to k8s (*1)
SUBMITTED → Kubernetes accepted the Job (*2)
RUNNING   → pod observed as actually running (kafka event from pod (*3)
COMPLETED → job finished successfully (*3)
FAILED    → job creation failed, OR job ran and failed (*3, *2)
CANCELLED → job got canceled by user (*2)

*1 from backend (entity)
*2 from backend (service)
*3 kafka event from k8s pod

When a task is COMPLETED, you can not restart it.
When a task is CANCELLED, it won't accept any new kafka events.
In both test cases if you want to restart it, you would need to send it to PENDING
*/

public enum TaskStatus { PENDING, SUBMITTED, RUNNING, COMPLETED, FAILED, CANCELED }