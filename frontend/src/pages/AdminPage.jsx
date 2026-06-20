import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import JobModal from '../components/JobModal';
import NotificationButton from '../components/NotificationButton';
import apiClient from '../services/apiClient';
import jobApi from '../services/jobApi';
import { API_ENDPOINTS } from '../constants/jobConfig';
import '../styles/AdminPage.css';
import '../styles/Dashboard.css';
import '../styles/JobModal.css';

const formatJobDate = (dateStr) => {
  if (!dateStr) return '-';
  const [year, month, day] = dateStr.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return date.toLocaleDateString();
};



export default function AdminPage() {

  const navigate = useNavigate();

  const { auth, logout } = useAuth();

  const [doneJobs, setDoneJobs] = useState([]);

  const [archivedJobs, setArchivedJobs] = useState([]);

  const [loading, setLoading] = useState(true);

  const [activeTab, setActiveTab] = useState('done');

  const [showJobModal, setShowJobModal] = useState(false);

  const [selectedJob, setSelectedJob] = useState(null);



  useEffect(() => {
    if (!auth || (auth.user.role !== 'MANAGER' && auth.user.role !== 'ADMIN')) {
      navigate('/');
      return;
    }
    apiClient.setToken(auth.token);
    fetchJobs();
  }, [auth, navigate]);

  const fetchJobs = async () => {
    try {
      const [done, archived] = await Promise.all([
        apiClient.get(API_ENDPOINTS.JOBS_DONE),
        apiClient.get(API_ENDPOINTS.JOBS_ARCHIVED),
      ]);

      setDoneJobs(done);
      setArchivedJobs(archived);
    } catch (err) {
      console.error('Failed to fetch jobs:', err);
    } finally {
      setLoading(false);
    }
  };



  const openCreateModal = () => {

    setSelectedJob(null);

    setShowJobModal(true);

  };



  const openEditModal = (job) => {

    setSelectedJob(job);

    setShowJobModal(true);

  };



  const handleJobSaved = (savedJob) => {

    if (savedJob.status === 'DONE') {

      setDoneJobs((prev) => {

        const exists = prev.some((j) => j.id === savedJob.id);

        if (exists) return prev.map((j) => (j.id === savedJob.id ? savedJob : j));

        return [savedJob, ...prev];

      });

      setArchivedJobs((prev) => prev.filter((j) => j.id !== savedJob.id));

    } else if (savedJob.status === 'ARCHIVED') {

      setArchivedJobs((prev) => {

        const exists = prev.some((j) => j.id === savedJob.id);

        if (exists) return prev.map((j) => (j.id === savedJob.id ? savedJob : j));

        return [savedJob, ...prev];

      });

      setDoneJobs((prev) => prev.filter((j) => j.id !== savedJob.id));

    } else if (savedJob.status === 'SCHEDULED' || savedJob.status === 'TO_BE_FIXED') {

      setArchivedJobs((prev) => prev.filter((j) => j.id !== savedJob.id));

      setDoneJobs((prev) => prev.filter((j) => j.id !== savedJob.id));

    } else {

      fetchJobs();

    }

  };



  const handleArchive = async (e, jobId) => {
    e.stopPropagation();

    try {
      const savedJob = await jobApi.archiveJob(jobId);
      setDoneJobs((prev) => prev.filter((j) => j.id !== jobId));
      setArchivedJobs((prev) => [savedJob, ...prev.filter((j) => j.id !== jobId)]);
    } catch (err) {
      console.error('Failed to archive job:', err);
    }
  };



  const handleLogout = () => {

    logout();

    navigate('/login');

  };



  if (loading) {

    return <div className="loading">Loading...</div>;

  }



  return (

    <div className="admin-page">

      <header className="admin-header">

        <button type="button" className="back-btn" onClick={() => navigate('/')}>

          ← Back to Dashboard

        </button>

        <h1>Admin Panel</h1>

        <div className="admin-header-actions">
          <NotificationButton />

          <button type="button" className="create-job-btn" onClick={openCreateModal}>

            + Create Job

          </button>

          <button type="button" className="logout-btn" onClick={handleLogout}>

            Logout

          </button>

        </div>

      </header>



      <main className="admin-content">

        <div className="tab-navigation">

          <button

            type="button"

            className={`tab-button ${activeTab === 'done' ? 'active' : ''}`}

            onClick={() => setActiveTab('done')}

          >

            Done Jobs ({doneJobs.length})

          </button>

          <button

            type="button"

            className={`tab-button ${activeTab === 'archived' ? 'active' : ''}`}

            onClick={() => setActiveTab('archived')}

          >

            Archived Jobs ({archivedJobs.length})

          </button>

        </div>



        {activeTab === 'done' && (

          <div className="jobs-table-container">

            {doneJobs.length === 0 ? (

              <p className="no-jobs">No completed jobs yet.</p>

            ) : (

              <table className="jobs-table">

                <thead>

                  <tr>

                    <th>Client</th>

                    <th>Phone</th>

                    <th>Address</th>

                    <th>Date</th>

                    <th>Priority</th>

                    <th>Actions</th>

                  </tr>

                </thead>

                <tbody>

                  {doneJobs.map((job) => (

                    <tr key={job.id} className="clickable-row" onClick={() => openEditModal(job)}>

                      <td>{job.clientName}</td>

                      <td>{job.clientPhone}</td>

                      <td>{job.clientAddress}</td>

                      <td>{formatJobDate(job.jobDate)}</td>

                      <td><span className="priority">{job.priorityLevel}</span></td>

                      <td>

                        <button

                          type="button"

                          className="action-btn archive-btn"

                          onClick={(e) => handleArchive(e, job.id)}

                        >

                          Archive

                        </button>

                      </td>

                    </tr>

                  ))}

                </tbody>

              </table>

            )}

          </div>

        )}



        {activeTab === 'archived' && (

          <div className="jobs-table-container">

            {archivedJobs.length === 0 ? (

              <p className="no-jobs">No archived jobs yet.</p>

            ) : (

              <table className="jobs-table">

                <thead>

                  <tr>

                    <th>Client</th>

                    <th>Phone</th>

                    <th>Address</th>

                    <th>Priority</th>

                  </tr>

                </thead>

                <tbody>

                  {archivedJobs.map((job) => (

                    <tr key={job.id} className="clickable-row" onClick={() => openEditModal(job)}>

                      <td>{job.clientName}</td>

                      <td>{job.clientPhone}</td>

                      <td>{job.clientAddress}</td>

                      <td><span className="priority">{job.priorityLevel}</span></td>

                    </tr>

                  ))}

                </tbody>

              </table>

            )}

          </div>

        )}

      </main>



      <JobModal

        isOpen={showJobModal}

        onClose={() => {

          setShowJobModal(false);

          setSelectedJob(null);

        }}

        onSuccess={handleJobSaved}

        token={auth.token}

        job={selectedJob}

        canManage

      />

    </div>

  );

}


