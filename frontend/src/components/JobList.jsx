import { useState, useEffect, useMemo } from 'react';
import JobCard from './JobCard';
import { sortPoolJobs } from '../utils/jobUtils';
import '../styles/JobList.css';

export default function JobList({ jobs, onJobClick }) {
  const sortedJobs = useMemo(() => sortPoolJobs(jobs), [jobs]);

  return (
    <div className="job-list">
      {sortedJobs.length === 0 ? (
        <div className="no-jobs">No pending or to-be-fixed jobs.</div>
      ) : (
        <div className="job-cards">
          {sortedJobs.map((job) => (
            <JobCard key={job.id} job={job} onJobClick={onJobClick} />
          ))}
        </div>
      )}
    </div>
  );
}
