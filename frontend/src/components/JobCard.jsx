import { useDragClickGuard } from '../hooks/useDragClickGuard';
import '../styles/JobCard.css';

export default function JobCard({ job, onJobClick }) {
  const { onMouseDown, onDragStart, onClick } = useDragClickGuard();

  const handleDragStart = (e) => {
    onDragStart(e, () => {
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('job', JSON.stringify(job));
    });
  };

  const getPriorityColor = (level) => {
    const colors = { 1: '#28a745', 2: '#ffc107', 3: '#fd7e14', 4: '#dc3545' };
    return colors[level] || '#999';
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Not assigned';
    // jobDate is now a string in format "yyyy-MM-dd"
    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString();
  };

  return (
    <div
      className="job-card"
      draggable
      onMouseDown={onMouseDown}
      onDragStart={handleDragStart}
      onClick={() => onClick(() => onJobClick?.(job))}
      style={{ borderLeftColor: getPriorityColor(job.priorityLevel) }}
    >
      <div className="job-header">
        <h4>{job.clientName}</h4>
        <span className="priority-badge" style={{ backgroundColor: getPriorityColor(job.priorityLevel) }}>
          P{job.priorityLevel}
        </span>
      </div>
      <div className="job-details">
        <p><strong>Phone:</strong> {job.clientPhone}</p>
        <p><strong>Address:</strong> {job.clientAddress}</p>
        <p><strong>Status:</strong> <span className="status-badge">{job.status}</span></p>
        <p><strong>Date:</strong> {formatDate(job.jobDate)}</p>
        <p><strong>Time:</strong> {job.jobStartHour || 'Not set'}</p>
        {job.details && <p><strong>Details:</strong> {job.details}</p>}
        {job.assignedWorkerDetails && job.assignedWorkerDetails.length > 0 && (
          <p><strong>Workers:</strong> {job.assignedWorkerDetails.map(w => w.name).join(', ')}</p>
        )}
      </div>
    </div>
  );
}
