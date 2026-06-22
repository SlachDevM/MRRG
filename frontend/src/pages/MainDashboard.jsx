import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import WeekView from '../components/WeekView';
import JobList from '../components/JobList';
import JobModal from '../components/JobModal';
import NotificationButton from '../components/NotificationButton';
import apiClient from '../services/apiClient';
import { JOB_STATUSES, API_ENDPOINTS } from '../constants/jobConfig';
import { getMonday } from '../utils/dateUtils';
import '../styles/Dashboard.css';
import '../styles/JobModal.css';

export default function MainDashboard() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { auth, logout } = useAuth();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showJobModal, setShowJobModal] = useState(false);
  const [selectedJob, setSelectedJob] = useState(null);
  const [prefilledDate, setPrefilledDate] = useState(null);
  const [weekRefreshKey, setWeekRefreshKey] = useState(0);
  const [selectedWeekStart, setSelectedWeekStart] = useState(() => getMonday(new Date()));

  const canManage = auth?.user?.role === 'MANAGER' || auth?.user?.role === 'ADMIN';

  useEffect(() => {
    if (!auth?.token) {
      navigate('/login');
      return;
    }
    apiClient.setToken(auth.token);
    fetchJobs();
  }, [auth, navigate]);

  useEffect(() => {
    const jobId = searchParams.get('jobId');
    if (!jobId || !auth?.token) return;

    const openJobFromParam = async () => {
      try {
        const job = await apiClient.get(`${API_ENDPOINTS.JOBS}/${jobId}`);
        setSelectedJob(job);
        setShowJobModal(true);
      } catch (err) {
        console.error('Failed to open job from notification:', err);
      } finally {
        setSearchParams({}, { replace: true });
      }
    };

    openJobFromParam();
  }, [searchParams, auth?.token, setSearchParams]);

  const fetchJobs = async () => {
    try {
      const data = await apiClient.get(API_ENDPOINTS.JOBS_PENDING);
      setJobs(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to fetch jobs:', err);
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {

    setSelectedJob(null);

    setPrefilledDate(null);

    setShowJobModal(true);

  };



  const openEditModal = (job) => {

    setSelectedJob(job);

    setPrefilledDate(null);

    setShowJobModal(true);

  };



  const handleJobSaved = (savedJob) => {
    if (savedJob.status === JOB_STATUSES.PENDING || savedJob.status === JOB_STATUSES.TO_BE_FIXED) {
      setJobs((prev) => {
        const exists = prev.some((j) => j.id === savedJob.id);
        if (exists) {
          return prev.map((j) => (j.id === savedJob.id ? savedJob : j));
        }
        return [savedJob, ...prev];
      });
    } else if (savedJob.status === JOB_STATUSES.ARCHIVED) {
      setJobs((prev) => prev.filter((j) => j.id !== savedJob.id));
    } else if (savedJob.status !== JOB_STATUSES.READY_FOR_CONFIRMATION) {
      setJobs((prev) => prev.filter((j) => j.id !== savedJob.id));
    }

    setWeekRefreshKey((k) => k + 1);
  };



  const handleJobAssigned = () => {

    fetchJobs();

    setWeekRefreshKey((k) => k + 1);

  };



  const handleLogout = () => {

    logout();

    navigate('/login');

  };



  if (loading) {

    return <div className="loading">Loading dashboard...</div>;

  }



  return (

    <div className="dashboard">

      <header className="dashboard-header">

        <img

          src="https://static.wixstatic.com/media/4c239c_3d5b277894a042ef8d75ecd3fdacfdda~mv2.png/v1/fill/w_378,h_174,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/RE-GUTTERS_edited.png"

          alt="RE-GUTTERS"

          className="dashboard-logo"

        />

        <div className="header-info">

          <span>Welcome, {auth?.user?.name ?? 'User'}</span>

          <span className="role-badge">{auth?.user?.role}</span>

        </div>

        <div className="header-actions">
          {canManage && (
            <button type="button" className="create-job-btn" onClick={openCreateModal}>
              + Create Job
            </button>
          )}

          {canManage && (
            <button type="button" className="users-btn" onClick={() => navigate('/users')}>
              👥 Users
            </button>
          )}

          {canManage && (
            <button type="button" className="admin-btn" onClick={() => navigate('/admin')}>
              ⚙️ Admin
            </button>
          )}

          <NotificationButton />

          <button type="button" className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>

      </header>



      <main className="dashboard-content">

        <div className="week-section">

          <h2>Weekly Schedule</h2>

          <WeekView

            weekStart={selectedWeekStart}

            onWeekChange={setSelectedWeekStart}

            onJobAssigned={handleJobAssigned}

            onJobClick={openEditModal}

            token={auth.token}

            refreshKey={weekRefreshKey}

          />

        </div>



        <div className="jobs-section">

          <h2>Pending & To Be Fixed Jobs</h2>

          <JobList jobs={jobs} onJobClick={openEditModal} />

        </div>

      </main>



      <JobModal

        isOpen={showJobModal}

        onClose={() => {

          setShowJobModal(false);

          setSelectedJob(null);

          setPrefilledDate(null);

        }}

        onSuccess={handleJobSaved}

        token={auth.token}

        job={selectedJob}

        prefilledDate={prefilledDate}

        canManage={canManage}

        currentUserName={auth?.user?.name ?? ''}
        currentUserId={auth?.user?.id}

      />

    </div>

  );

}


