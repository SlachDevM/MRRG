import { useState, useEffect } from 'react';
import '../styles/JobModal.css';
import apiClient from '../services/apiClient';
import {
  JOB_TYPES,
  PRIORITY_LEVELS,
  JOB_STATUSES,
  EMPTY_JOB_FORM,
  API_ENDPOINTS,
} from '../constants/jobConfig';
import {
  formatJobTypeLabel,
} from '../utils/dateUtils';
import {
  toPhotoSrc,
  extractBase64,
  photoEntryFromBase64,
  parsePhotosFromJob,
  photosToPayload,
  sanitizeClientName,
} from '../utils/photoUtils';
import { getJobPermissions } from '../utils/permissionUtils';

function PhotoGallery({ title, photoType, photos, canUpload, onUpload, onDelete, onDownload }) {
  return (
    <div className="photo-group">
      <p className="photo-group-label">{title}</p>
      {photos.length > 0 && (
        <div className="photo-grid">
          {photos.map((photo, index) => (
            <div key={`${title}-${index}`} className="photo-thumb">
              <button
                type="button"
                className="photo-action photo-download"
                onClick={() => onDownload(photo, photoType, index)}
                aria-label="Download photo"
                title="Download"
              >
                <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
                  <path
                    d="M12 3v10m0 0l-4-4m4 4l4-4M5 19h14"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>
              {canUpload && (
                <button
                  type="button"
                  className="photo-action photo-delete"
                  onClick={() => onDelete(index)}
                  aria-label="Delete photo"
                  title="Delete"
                >
                  ×
                </button>
              )}
              <img src={photo.preview} alt={`${title} ${index + 1}`} className="photo-preview" />
            </div>
          ))}
        </div>
      )}
      {canUpload && (
        <input
          type="file"
          accept="image/*"
          multiple
          className="photo-file-input"
          onChange={onUpload}
        />
      )}
    </div>
  );
}

export default function JobModal({
  isOpen,
  onClose,
  onSuccess,
  token,
  job = null,
  prefilledDate = null,
  canManage = false,
  currentUserName = '',
  currentUserId = null,
}) {
  const isEdit = Boolean(job?.id);
  const [form, setForm] = useState(EMPTY_JOB_FORM);
  const [selectedTypes, setSelectedTypes] = useState([]);
  const [selectedWorkers, setSelectedWorkers] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [jobStatus, setJobStatus] = useState(null);
  const [beforePhotos, setBeforePhotos] = useState([]);
  const [afterPhotos, setAfterPhotos] = useState([]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [callbackFixing, setCallbackFixing] = useState(false);
  const [loadingJob, setLoadingJob] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    const loadWorkers = async () => {
      if (!canManage) return;
      try {
        const workers = await apiClient.get(API_ENDPOINTS.USERS_WORKERS);
        setWorkers(workers);
      } catch (err) {
        console.error('Failed to load workers:', err);
      }
    };

    loadWorkers();
  }, [isOpen, canManage]);

  useEffect(() => {
    if (!isOpen) return;

    const resetForm = () => {
      setForm({
        ...EMPTY_JOB_FORM,
        jobDate: prefilledDate || '',
      });
      setSelectedTypes([]);
      setSelectedWorkers([]);
      setBeforePhotos([]);
      setAfterPhotos([]);
      setError('');
    };

    if (!isEdit) {
      resetForm();
      return;
    }

    const loadJob = async () => {
      setLoadingJob(true);
      try {
        const fullJob = await apiClient.get(`${API_ENDPOINTS.JOBS}/${job.id}`);

        setForm({
          clientName: fullJob.clientName || '',
          clientPhone: fullJob.clientPhone || '',
          clientAddress: fullJob.clientAddress || '',
          details: fullJob.details || '',
          notes: fullJob.notes || '',
          priorityLevel: String(fullJob.priorityLevel || 1),
          jobDate: prefilledDate || fullJob.jobDate || '',
          jobStartHour: fullJob.jobStartHour || '07:50',
        });
        setSelectedTypes(fullJob.jobTypes ? fullJob.jobTypes.split(',').filter(Boolean) : []);
        // Load assigned worker IDs from backend (format: "1,3,7")
        // Convert to numbers for comparison
        setSelectedWorkers(
          fullJob.assignedWorkers ? fullJob.assignedWorkers.split(',').filter(Boolean).map(Number) : []
        );
        setJobStatus(fullJob.status || null);
        setBeforePhotos(parsePhotosFromJob(fullJob, 'beforePhotos', 'beforePhoto'));
        setAfterPhotos(parsePhotosFromJob(fullJob, 'afterPhotos', 'afterPhoto'));
      } catch (err) {
        console.error(err);
        setError('Failed to load job details.');
      } finally {
        setLoadingJob(false);
      }
    };

    loadJob();
  }, [isOpen, isEdit, job?.id, prefilledDate]);

  apiClient.setToken(token);

  if (!isOpen) return null;

  const isAssignedWorker = currentUserId && selectedWorkers.includes(currentUserId);
  const permissions = getJobPermissions(job, { role: canManage ? 'MANAGER' : 'WORKER' }, isAssignedWorker);
  const {
    isArchived,
    isDone,
    isInProgress,
    isCallbackOnly,
    awaitingConfirmation,
    isCoreReadOnly,
    canUploadPhotos,
    canEditNotes,
    canCallbackFix,
    canComplete,
    canConfirm,
    canArchive,
  } = permissions;

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const toggleJobType = (type) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
  };

  const toggleWorker = (workerId) => {
    setSelectedWorkers((prev) =>
      prev.includes(workerId) ? prev.filter((id) => id !== workerId) : [...prev, workerId]
    );
  };

  const handlePhotoChange = (e, type) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    e.target.value = '';

    const setter = type === 'before' ? setBeforePhotos : setAfterPhotos;

    files.forEach((file) => {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = reader.result;
        const base64 = dataUrl.split(',')[1];
        setter((prev) => [...prev, { preview: dataUrl, base64 }]);
      };
      reader.readAsDataURL(file);
    });
  };

  const persistPhotoLists = async (nextBefore, nextAfter, includeNotes = false) => {
    const payload = {
      beforePhotos: photosToPayload(nextBefore),
      afterPhotos: photosToPayload(nextAfter),
    };
    if (includeNotes) {
      payload.notes = form.notes.trim() || null;
    }

    try {
      const savedJob = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}`, payload);
      setBeforePhotos(parsePhotosFromJob(savedJob, 'beforePhotos', 'beforePhoto'));
      setAfterPhotos(parsePhotosFromJob(savedJob, 'afterPhotos', 'afterPhoto'));
      onSuccess(savedJob);
      return savedJob;
    } catch (err) {
      throw new Error('Failed to save photos.');
    }
  };

  const handleDeletePhoto = async (type, index) => {
    if (!window.confirm('Delete this photo?')) {
      return;
    }

    const nextBefore =
      type === 'before' ? beforePhotos.filter((_, photoIndex) => photoIndex !== index) : beforePhotos;
    const nextAfter =
      type === 'after' ? afterPhotos.filter((_, photoIndex) => photoIndex !== index) : afterPhotos;

    if (type === 'before') {
      setBeforePhotos(nextBefore);
    } else {
      setAfterPhotos(nextAfter);
    }

    if (!isEdit) {
      return;
    }

    setError('');
    setSubmitting(true);
    try {
      await persistPhotoLists(nextBefore, nextAfter);
    } catch (err) {
      console.error(err);
      setError('Failed to delete photo.');
      if (type === 'before') {
        setBeforePhotos(beforePhotos);
      } else {
        setAfterPhotos(afterPhotos);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const downloadPhoto = (photo, photoType, index) => {
    const clientPart = sanitizeClientName(form.clientName);
    const typePart = photoType === 'before' ? 'Before' : 'After';
    const link = document.createElement('a');
    link.href = photo.preview;
    link.download = `${clientPart}_${typePart}_${index + 1}.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleClose = () => {
    setError('');
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (isCallbackOnly && canManage) {
      setSubmitting(true);
      try {
        const payload = {
          details: form.details.trim() || null,
          notes: form.notes.trim() || null,
          beforePhotos: photosToPayload(beforePhotos),
          afterPhotos: photosToPayload(afterPhotos),
        };

        const savedJob = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}`, payload);
        onSuccess(savedJob);
        onClose();
      } catch (err) {
        console.error(err);
        setError('Failed to save changes.');
      } finally {
        setSubmitting(false);
      }
      return;
    }

    if (!form.clientName.trim() || !form.clientPhone.trim() || !form.clientAddress.trim()) {
      setError('Client name, phone, and address are required.');
      return;
    }

    if (selectedTypes.length === 0) {
      setError('Select at least one job type.');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        clientName: form.clientName.trim(),
        clientPhone: form.clientPhone.trim(),
        clientAddress: form.clientAddress.trim(),
        jobTypes: selectedTypes.join(','),
        priorityLevel: parseInt(form.priorityLevel, 10),
        details: form.details.trim() || null,
        notes: form.notes.trim() || null,
      };

      if (canManage) {
        if (form.jobDate) {
          payload.jobDate = form.jobDate;
          payload.jobStartHour = form.jobStartHour || '07:50';
        }
        payload.assignedWorkers = selectedWorkers.join(',');
      }

      if (isEdit) {
        payload.beforePhotos = photosToPayload(beforePhotos);
        payload.afterPhotos = photosToPayload(afterPhotos);
      }

      const savedJob = isEdit
        ? await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}`, payload)
        : await apiClient.post(API_ENDPOINTS.JOBS, payload);

      onSuccess(savedJob);
      onClose();
    } catch (err) {
      console.error(err);
      setError(isEdit ? 'Failed to update job.' : 'Failed to create job.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSavePhotos = async () => {
    setError('');
    setSubmitting(true);
    try {
      await persistPhotoLists(beforePhotos, afterPhotos, true);
      onClose();
    } catch (err) {
      console.error(err);
      setError('Failed to save changes.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCompleteJob = async () => {
    if (!window.confirm('Mark this job as complete?')) {
      return;
    }

    setError('');
    setCompleting(true);
    try {
      const savedJob = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}/complete`, {});
      onSuccess(savedJob);
      onClose();
    } catch (err) {
      console.error(err);
      setError('Failed to complete job.');
    } finally {
      setCompleting(false);
    }
  };

  const handleArchiveJob = async () => {
    if (!window.confirm('Archive this job? It will be removed from the schedule.')) {
      return;
    }

    setError('');
    setArchiving(true);
    try {
      const savedJob = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}/archive`, {});
      onSuccess(savedJob);
      onClose();
    } catch (err) {
      console.error(err);
      setError('Failed to archive job.');
    } finally {
      setArchiving(false);
    }
  };

  const handleCallbackFix = async () => {
    setError('');
    setCallbackFixing(true);
    try {
      const payload = {};
      if (form.jobDate) {
        payload.jobDate = form.jobDate;
        payload.jobStartHour = form.jobStartHour || '07:50';
      }

      const savedJob = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}/callback-fix`, payload);
      onSuccess(savedJob);
      onClose();
    } catch (err) {
      console.error(err);
      setError('Failed to callback fix job.');
    } finally {
      setCallbackFixing(false);
    }
  };

  const handleConfirmJob = async () => {
    if (!window.confirm('Confirm this job as done? It will be removed from the schedule.')) {
      return;
    }

    setError('');
    setConfirming(true);
    try {
      const savedJob = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}/confirm`, {});
      onSuccess(savedJob);
      onClose();
    } catch (err) {
      console.error(err);
      setError('Failed to confirm job.');
    } finally {
      setConfirming(false);
    }
  };

  return (
    <div className="job-modal-overlay" onClick={handleClose}>
      <div className="job-modal" onClick={(e) => e.stopPropagation()}>
        <h3>
          {isEdit
            ? isArchived
              ? 'Archived Job'
              : isDone
                ? 'Done Job'
                : 'Edit Job'
            : 'Create Job'}
        </h3>

        {isCallbackOnly && (
          <p className="job-modal-status-banner job-modal-archived-banner">
            {isDone
              ? 'This job is done. You can still update details, notes, and photos. Use Callback Fix to reschedule, or Archive to move it to archived jobs.'
              : 'This job is archived. You can still update details, notes, and photos. Use Callback Fix to send it back to the schedule or pending pool.'}
          </p>
        )}

        {awaitingConfirmation && (
          <p className="job-modal-status-banner">
            {canManage
              ? 'This job was marked complete by a worker. Review and confirm when ready.'
              : 'Waiting for manager confirmation.'}
          </p>
        )}

        {loadingJob ? (
          <p className="job-modal-loading">Loading job...</p>
        ) : (
          <form onSubmit={handleSubmit}>
            <label>
              Client Name *
              <input
                name="clientName"
                value={form.clientName}
                onChange={handleChange}
                placeholder="John Smith"
                required
                disabled={isCoreReadOnly}
              />
            </label>

            <label>
              Phone *
              <input
                name="clientPhone"
                value={form.clientPhone}
                onChange={handleChange}
                placeholder="0412 345 678"
                required
                disabled={isCoreReadOnly}
              />
            </label>

            <label>
              Address *
              <input
                name="clientAddress"
                value={form.clientAddress}
                onChange={handleChange}
                placeholder="123 Main St, Sydney"
                required
                disabled={isCoreReadOnly}
              />
            </label>

            <fieldset className="job-types-fieldset">
              <legend>Job Types *</legend>
              <div className="job-types-grid">
                {JOB_TYPES.map((type) => (
                  <label key={type} className="job-type-option">
                    <input
                      type="checkbox"
                      checked={selectedTypes.includes(type)}
                      onChange={() => toggleJobType(type)}
                      disabled={isCoreReadOnly}
                    />
                    {formatJobTypeLabel(type)}
                  </label>
                ))}
              </div>
            </fieldset>

            <label>
              Priority
              <select name="priorityLevel" value={form.priorityLevel} onChange={handleChange} disabled={isCoreReadOnly}>
                {PRIORITY_LEVELS.map((level) => (
                  <option key={level.value} value={level.value}>{level.label}</option>
                ))}
              </select>
            </label>

            <label>
              Details
              <textarea
                name="details"
                value={form.details}
                onChange={handleChange}
                placeholder="Additional job details..."
                rows={3}
                disabled={!canManage}
              />
            </label>

            <label>
              Notes
              <textarea
                name="notes"
                value={form.notes}
                onChange={handleChange}
                placeholder="Internal notes..."
                rows={2}
                disabled={!canEditNotes}
              />
            </label>

            {(canManage || isEdit) && !isCallbackOnly && (
              <>
                <div className="job-modal-section">
                  <h4>Schedule</h4>
                  <div className="job-modal-row">
                    <label>
                      Start Date
                      <input
                        type="date"
                        name="jobDate"
                        value={form.jobDate}
                        onChange={handleChange}
                        disabled={isCoreReadOnly}
                      />
                    </label>
                    <label>
                      Start Hour
                      <input
                        type="time"
                        name="jobStartHour"
                        value={form.jobStartHour}
                        onChange={handleChange}
                        disabled={isCoreReadOnly}
                      />
                    </label>
                  </div>
                </div>

                <fieldset className="job-types-fieldset">
                  <legend>Assign Workers</legend>
                  {!canManage && selectedWorkers.length > 0 ? (
                    <p className="job-modal-hint">{workers.filter(w => selectedWorkers.includes(w.id)).map(w => w.name).join(', ')}</p>
                  ) : workers.length === 0 ? (
                    <p className="job-modal-hint">No workers available.</p>
                  ) : (
                    <div className="job-types-grid">
                      {workers.map((worker) => (
                        <label key={worker.id} className="job-type-option">
                          <input
                            type="checkbox"
                            checked={selectedWorkers.includes(worker.id)}
                            onChange={() => toggleWorker(worker.id)}
                            disabled={isCoreReadOnly}
                          />
                          {worker.name}
                        </label>
                      ))}
                    </div>
                  )}
                </fieldset>
              </>
            )}

            {isEdit && (canUploadPhotos || beforePhotos.length > 0 || afterPhotos.length > 0) && (
              <div className="job-modal-section">
                <h4>Photos</h4>
                <PhotoGallery
                  title="Before Photos"
                  photoType="before"
                  photos={beforePhotos}
                  canUpload={canUploadPhotos}
                  onUpload={(e) => handlePhotoChange(e, 'before')}
                  onDelete={(index) => handleDeletePhoto('before', index)}
                  onDownload={downloadPhoto}
                />
                <PhotoGallery
                  title="After Photos"
                  photoType="after"
                  photos={afterPhotos}
                  canUpload={canUploadPhotos}
                  onUpload={(e) => handlePhotoChange(e, 'after')}
                  onDelete={(index) => handleDeletePhoto('after', index)}
                  onDownload={downloadPhoto}
                />
              </div>
            )}

            {canCallbackFix && (
              <div className="job-modal-section callback-fix-section">
                <h4>Callback Fix</h4>
                <p className="job-modal-hint">
                  Optionally pick a new date to put the job back on the week schedule, or leave empty
                  to send it to the pending pool as To Be Fixed.
                </p>
                <div className="job-modal-row">
                  <label>
                    New Date (optional)
                    <input
                      type="date"
                      name="jobDate"
                      value={form.jobDate}
                      onChange={handleChange}
                    />
                  </label>
                  <label>
                    Start Hour
                    <input
                      type="time"
                      name="jobStartHour"
                      value={form.jobStartHour}
                      onChange={handleChange}
                      disabled={!form.jobDate}
                    />
                  </label>
                </div>
              </div>
            )}

            {error && <p className="job-modal-error">{error}</p>}

            <div className="job-modal-actions">
              {canCallbackFix && (
                <button
                  type="button"
                  className="callback-fix-btn"
                  onClick={handleCallbackFix}
                  disabled={callbackFixing || loadingJob}
                >
                  {callbackFixing ? 'Processing...' : 'Callback Fix'}
                </button>
              )}
              {canArchive && (
                <button
                  type="button"
                  className="archive-job-btn"
                  onClick={handleArchiveJob}
                  disabled={submitting || loadingJob || completing || confirming || archiving}
                >
                  {archiving ? 'Archiving...' : 'Archive'}
                </button>
              )}
              {canConfirm && (
                <button
                  type="button"
                  className="confirm-job-btn"
                  onClick={handleConfirmJob}
                  disabled={submitting || loadingJob || completing || confirming}
                >
                  {confirming ? 'Confirming...' : 'Confirmed'}
                </button>
              )}
              {!canManage && isEdit && isAssignedWorker && jobStatus === JOB_STATUSES.SCHEDULED && (
                <p className="job-modal-hint" style={{ color: '#ff9800', fontWeight: 'bold' }}>
                  📸 Add photos or notes to start the job (move to In Progress)
                </p>
              )}
              {canComplete && (
                <button
                  type="button"
                  className="complete-job-btn"
                  onClick={handleCompleteJob}
                  disabled={submitting || loadingJob || completing || confirming}
                >
                  {completing ? 'Completing...' : 'Complete Job'}
                </button>
              )}
              {!canManage && isEdit && canEditNotes && (
                <button
                  type="button"
                  className="save-job-btn"
                  onClick={handleSavePhotos}
                  disabled={submitting || loadingJob || completing || confirming}
                >
                  {submitting ? 'Saving...' : 'Save'}
                </button>
              )}
              {canManage && (
                <button type="submit" className="save-job-btn" disabled={submitting || loadingJob || confirming || archiving || callbackFixing}>
                  {submitting
                    ? isEdit
                      ? 'Saving...'
                      : 'Creating...'
                    : isEdit
                      ? 'Save Job'
                      : 'Create Job'}
                </button>
              )}
              <button type="button" className="cancel-job-btn" onClick={handleClose} disabled={submitting || completing || confirming || archiving || callbackFixing}>
                {canManage ? 'Cancel' : 'Close'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
