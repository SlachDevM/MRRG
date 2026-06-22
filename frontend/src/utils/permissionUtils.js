import { JOB_STATUSES } from '../constants/jobConfig';

export function getJobPermissions(job, user, isAssignedWorker) {
  const canManage = user?.role === 'MANAGER' || user?.role === 'ADMIN';
  const isArchived = job?.status === JOB_STATUSES.ARCHIVED;
  const isDone = job?.status === JOB_STATUSES.DONE;
  const isInProgress = job?.status === JOB_STATUSES.IN_PROGRESS;
  const isCallbackOnly = isArchived || isDone;
  const awaitingConfirmation = job?.status === JOB_STATUSES.READY_FOR_CONFIRMATION;

  return {
    canManage,
    isAssignedWorker,
    isArchived,
    isDone,
    isInProgress,
    isCallbackOnly,
    awaitingConfirmation,
    isCoreReadOnly: !canManage || isCallbackOnly,
    canUploadPhotos: canManage || (job?.id && isAssignedWorker && !isCallbackOnly),
    canEditNotes: canManage || (job?.id && isAssignedWorker && !isCallbackOnly),
    canCallbackFix: job?.id && canManage && isCallbackOnly,
    canComplete:
      job?.id &&
      !canManage &&
      isAssignedWorker &&
      (job?.status === JOB_STATUSES.IN_PROGRESS || job?.status === JOB_STATUSES.TO_BE_FIXED),
    canConfirm: job?.id && canManage && job?.status === JOB_STATUSES.READY_FOR_CONFIRMATION,
    canArchive: job?.id && canManage && !isArchived,
    canDrag: !awaitingConfirmation,
  };
}

export function formatWorkers(jobOrAssignedWorkers) {
  if (jobOrAssignedWorkers && typeof jobOrAssignedWorkers === 'object') {
    const job = jobOrAssignedWorkers;
    if (job.assignedWorkerDetails?.length > 0) {
      return job.assignedWorkerDetails.map((worker) => worker.name).join(', ');
    }
    if (!job.assignedWorkers?.trim()) {
      return 'No workers assigned';
    }
    return job.assignedWorkers.split(',').filter(Boolean).join(', ');
  }

  const assignedWorkers = jobOrAssignedWorkers;
  if (!assignedWorkers || !assignedWorkers.trim()) {
    return 'No workers assigned';
  }
  return assignedWorkers.split(',').filter(Boolean).join(', ');
}
