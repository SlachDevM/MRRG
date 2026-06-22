# Administrator Manual

## Introduction

This manual describes the administrative workflows available in MRRG.

It is intended for managers and administrators responsible for scheduling work, managing employees and monitoring job progress.

The screenshots shown throughout this document illustrate the current version of the application. Minor interface changes may occur over time while the underlying workflows remain unchanged.

---

## User Management

Administrators are responsible for creating and maintaining user accounts.

Employees cannot register themselves.

Each new account is created by an administrator and remains in the `PENDING_ACTIVATION` state until the employee completes the account activation process from the Android application.

Disabling a user automatically removes them from all non-final jobs and from the list of assignable workers. Their assignments are preserved on completed and archived jobs to maintain an accurate historical record.

Administrators can:

* create new users;
* update user information;
* deactivate existing accounts;
* reactivate disabled accounts;
* resend activation emails.

<img width="2844" height="1220" alt="image" src="https://github.com/user-attachments/assets/66dc3c62-e8d4-4db9-8007-ad9eb1c7a243" />

---

## Scheduling Work

Jobs are created and scheduled through the management interface.

Scheduling consists of assigning the appropriate employee, selecting the planned work date and defining the expected working period.

Once scheduled, the assigned employee automatically receives the job through the Android application. 

A job can be scheduled without assigning any employee. This allows managers to reserve a time slot before deciding which member(s) of the team will perform the work.

Managers may reschedule jobs whenever operational requirements change.

<img width="2850" height="1546" alt="image" src="https://github.com/user-attachments/assets/6e19a70f-a4d2-48da-af97-585a9f83eb01" />

---

## Managing Jobs

Jobs move through a predefined business workflow.

```text
Pending

↓

Scheduled

↓

In Progress

↓

Waiting Manager Validation

↓

Completed

↓

Archived
```
The workflow is primarily driven by field operations:

- uploading the first before photo starts the job (Scheduled → In Progress);
- adding notes never changes the job status;
- at least one after photo is required before the job can be submitted for manager validation;

Each status represents a business milestone and ensures that work progresses in a controlled and predictable manner.

Managers monitor this progression from the dashboard and intervene only when business decisions are required.

Each stage reflects the current operational state of the job and helps ensure consistent tracking throughout its lifecycle.

---

## Validating Completed Work

Once the work is finished, they upload one or more after photos and explicitly submit the job for manager validation.

Managers review the submitted information before marking the job as completed.

Validation confirms that the work satisfies business requirements before it becomes part of the company's archive.

The sequence below illustrates the complete validation workflow performed by a manager.

<img width="522" height="568" alt="image" src="https://github.com/user-attachments/assets/93dbc290-0f9c-4eb4-8c8c-f5afa59336ff" />
<img width="1262" height="550" alt="image" src="https://github.com/user-attachments/assets/fe9efd4b-db85-4dcc-b660-b42d417e2785" />
<img width="1260" height="1136" alt="image" src="https://github.com/user-attachments/assets/b2d7083d-786b-4fb3-a6b6-3bd3f4d8d70f" />
<img width="904" height="336" alt="image" src="https://github.com/user-attachments/assets/7e3d0588-c728-4278-b22d-66f2f9c98eab" />
<img width="2854" height="918" alt="image" src="https://github.com/user-attachments/assets/8c9d29fd-f2b2-471e-840d-9031dc9017e9" />

---

## Archive and Callbacks

Completed jobs first appear in the Done Jobs section. Once the work has been reviewed and no further action is expected, the job can be moved to the Archive.

Archived jobs remain available for historical reference and can be restored whenever additional work or customer callbacks are required.

Restoring a job preserves the complete history of the original work while allowing managers to continue the workflow without creating duplicate records.

When a callback is created, all assigned workers are automatically removed. This allows managers to assign the most appropriate employee(s) for the follow-up work instead of automatically notifying the original team.

<img width="2844" height="942" alt="image" src="https://github.com/user-attachments/assets/61bc3bf1-d713-428e-81d8-8512f626d9ba" />
<img width="1256" height="600" alt="image" src="https://github.com/user-attachments/assets/1c3a538f-637c-4c73-b933-fcd920f04167" />
<img width="1250" height="572" alt="image" src="https://github.com/user-attachments/assets/2a31c7a0-36ac-40d1-b458-d3441e5fae41" />


---

## Notifications

MRRG provides notifications for important business events.

Typical notifications include:

* newly assigned work;
* schedule changes;
* completed jobs awaiting validation.

Notifications help managers and employees remain informed without requiring constant manual communication. They complement existing business workflows but do not replace direct communication when immediate action is required.

<img width="2846" height="1254" alt="image" src="https://github.com/user-attachments/assets/83f91d51-64e2-45b4-9575-d7ce9896ebc6" />

---

## Good Practices

To maintain accurate operational records:

* schedule jobs before assigning field workers;
* validate completed work promptly;
* archive completed jobs only after verification;
* deactivate accounts instead of deleting users;
* restore archived jobs for callbacks instead of creating new jobs;
* keep user information up to date;
* keep job information accurate and up to date;

Following these practices helps maintain accurate business records, improves operational consistency and ensures reliable historical data for future reference.
