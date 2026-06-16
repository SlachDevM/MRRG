import { useState, useEffect, useRef } from 'react';
import { useDragClickGuard } from '../hooks/useDragClickGuard';
import '../styles/WeekView.css';

const API_BASE = 'http://localhost:4000';
const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

function startOfDay(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

function endOfDay(date) {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d.getTime();
}

function isSameDay(ts, dayDate) {
  if (!ts) return false;
  return startOfDay(new Date(ts)) === startOfDay(dayDate);
}

function formatWorkers(assignedWorkers) {
  if (!assignedWorkers || !assignedWorkers.trim()) {
    return 'No workers assigned';
  }
  return assignedWorkers.split(',').filter(Boolean).join(', ');
}

function ScheduledJobChip({ job, onJobClick, onDragStartRef, getPriorityColor }) {
  const { onMouseDown, onDragStart, onClick } = useDragClickGuard();
  const awaitingConfirmation = job.status === 'READY_FOR_CONFIRMATION';
  const canDrag = !awaitingConfirmation;

  return (
    <div
      className={`scheduled-job-chip${awaitingConfirmation ? ' awaiting-confirmation' : ''}`}
      draggable={canDrag}
      onMouseDown={onMouseDown}
      onDragStart={(e) => {
        if (!canDrag) return;
        onDragStart(e, () => {
          onDragStartRef.current = job;
          e.dataTransfer.effectAllowed = 'move';
          e.dataTransfer.setData('job', JSON.stringify(job));
        });
      }}
      onDragEnd={() => {
        onDragStartRef.current = null;
      }}
      onClick={() => onClick(() => onJobClick?.(job))}
      style={{ borderLeftColor: getPriorityColor(job.priorityLevel) }}
    >
      <span className="chip-time">{job.jobStartHour || '07:50'}</span>
      {awaitingConfirmation && (
        <span className="chip-status-badge">Awaiting confirmation</span>
      )}
      <span className="chip-name">{job.clientName}</span>
      <span className="chip-address">{job.clientAddress}</span>
      <span className="chip-workers">{formatWorkers(job.assignedWorkers)}</span>
    </div>
  );
}

export default function WeekView({
  weekStart,
  onWeekChange,
  onJobAssigned,
  onJobClick,
  token,
  refreshKey = 0,
}) {
  const [scheduledJobs, setScheduledJobs] = useState([]);
  const [dragOverDay, setDragOverDay] = useState(null);
  const draggedJobRef = useRef(null);

  const getWeekDays = () => {
    const days = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(weekStart);
      date.setDate(date.getDate() + i);
      days.push(date);
    }
    return days;
  };

  const weekDays = getWeekDays();
  const weekEnd = weekDays[6];

  useEffect(() => {
    const fetchScheduled = async () => {
      try {
        const weekStartMs = startOfDay(weekDays[0]);
        const weekEndMs = endOfDay(weekEnd);
        const response = await fetch(
          `${API_BASE}/api/jobs/scheduled?weekStart=${weekStartMs}&weekEnd=${weekEndMs}`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        if (response.ok) {
          setScheduledJobs(await response.json());
        } else {
          console.error('Failed to fetch scheduled jobs:', response.status);
        }
      } catch (err) {
        console.error('Failed to fetch scheduled jobs:', err);
      }
    };

    fetchScheduled();
  }, [weekStart, token, refreshKey]);

  const handlePrevWeek = () => {
    const newStart = new Date(weekStart);
    newStart.setDate(newStart.getDate() - 7);
    onWeekChange(newStart);
  };

  const handleNextWeek = () => {
    const newStart = new Date(weekStart);
    newStart.setDate(newStart.getDate() + 7);
    onWeekChange(newStart);
  };

  const handleDragOver = (e, dayIdx) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverDay(dayIdx);
  };

  const handleDragLeave = () => {
    setDragOverDay(null);
  };

  const scheduleJobOnDay = async (job, dayDate) => {
    try {
      const response = await fetch(`${API_BASE}/api/jobs/${job.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          jobDate: startOfDay(dayDate),
          jobStartHour: job.jobStartHour || '07:50',
          status: 'SCHEDULED',
        }),
      });

      if (response.ok) {
        const updated = await response.json();
        onJobAssigned?.(job.id);
        setScheduledJobs((prev) => {
          const filtered = prev.filter((j) => j.id !== job.id);
          return [...filtered, updated];
        });
      }
    } catch (err) {
      console.error('Failed to schedule job:', err);
    }
  };

  const handleDrop = (e, dayDate) => {
    e.preventDefault();
    setDragOverDay(null);

    let job = draggedJobRef.current;
    if (!job) {
      const jobData = e.dataTransfer.getData('job');
      if (jobData) {
        try {
          job = JSON.parse(jobData);
        } catch {
          return;
        }
      }
    }

    if (!job) return;
    draggedJobRef.current = null;
    scheduleJobOnDay(job, dayDate);
  };

  const getPriorityColor = (level) => {
    const colors = { 1: '#28a745', 2: '#ffc107', 3: '#fd7e14', 4: '#dc3545' };
    return colors[level] || '#999';
  };

  const today = new Date();

  return (
    <div className="week-view">
      <div className="week-controls">
        <button type="button" onClick={handlePrevWeek}>← Previous Week</button>
        <span className="week-dates">
          {weekStart.toLocaleDateString()} – {weekEnd.toLocaleDateString()}
        </span>
        <button type="button" onClick={handleNextWeek}>Next Week →</button>
      </div>

      <div className="week-grid">
        {weekDays.map((day, idx) => {
          const dayJobs = scheduledJobs.filter((j) => isSameDay(j.jobDate, day));

          return (
            <div
              key={idx}
              className={`week-day ${day.toDateString() === today.toDateString() ? 'today' : ''} ${dragOverDay === idx ? 'drag-over' : ''}`}
              onDragOver={(e) => handleDragOver(e, idx)}
              onDragLeave={handleDragLeave}
              onDrop={(e) => handleDrop(e, day)}
            >
              <div className="day-header">
                <div className="day-name">{DAYS[idx]}</div>
                <div className="day-date">
                  {day.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                </div>
              </div>
              <div className="day-content">
                {dayJobs.length === 0 && (
                  <div className="drop-zone">Drop jobs here</div>
                )}
                {dayJobs.map((job) => (
                  <ScheduledJobChip
                    key={job.id}
                    job={job}
                    onJobClick={onJobClick}
                    onDragStartRef={draggedJobRef}
                    getPriorityColor={getPriorityColor}
                  />
                ))}
                {dayJobs.length > 0 && <div className="drop-zone-hint">Drop to reschedule</div>}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
