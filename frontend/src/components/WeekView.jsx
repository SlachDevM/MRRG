import { useState, useEffect, useRef } from 'react';
import { useDragClickGuard } from '../hooks/useDragClickGuard';
import apiClient from '../services/apiClient';
import { PRIORITY_COLORS, JOB_STATUSES, API_ENDPOINTS } from '../constants/jobConfig';
import {
  dateToISOString,
  isSameDay,
} from '../utils/dateUtils';
import { formatWorkers } from '../utils/permissionUtils';
import '../styles/WeekView.css';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

function ScheduledJobChip({ job, onJobClick, onDragStartRef, getPriorityColor }) {
  const { onMouseDown, onDragStart, onClick } = useDragClickGuard();
  const awaitingConfirmation = job.status === JOB_STATUSES.READY_FOR_CONFIRMATION;
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
      <span className="chip-workers">{formatWorkers(job)}</span>
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
        apiClient.setToken(token);
        const weekStartDate = dateToISOString(weekDays[0]);
        const weekEndDate = dateToISOString(weekEnd);
        const jobs = await apiClient.get(
          `${API_ENDPOINTS.JOBS_SCHEDULED}?weekStart=${weekStartDate}&weekEnd=${weekEndDate}`
        );
        setScheduledJobs(jobs);
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
      const jobDateStr = dateToISOString(dayDate);
      const updated = await apiClient.put(`${API_ENDPOINTS.JOBS}/${job.id}`, {
        jobDate: jobDateStr,
        jobStartHour: job.jobStartHour || '07:50',
        status: JOB_STATUSES.SCHEDULED,
      });

      onJobAssigned?.(job.id);
      setScheduledJobs((prev) => {
        const filtered = prev.filter((j) => j.id !== job.id);
        return [...filtered, updated];
      });
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
    return PRIORITY_COLORS[level] || '#999';
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
