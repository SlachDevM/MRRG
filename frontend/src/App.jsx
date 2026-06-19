import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import Login from './pages/Login';
import MainDashboard from './pages/MainDashboard';
import AdminPage from './pages/AdminPage';
import NotificationPage from './pages/NotificationPage';
import UserManagementPage from './pages/UserManagementPage';
import './App.css';

function PrivateRoute({ children }) {
  const { auth, loading } = useAuth();

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return auth ? children : <Navigate to="/login" />;
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <NotificationProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              path="/"
              element={
                <PrivateRoute>
                  <MainDashboard />
                </PrivateRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <PrivateRoute>
                  <AdminPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/notifications"
              element={
                <PrivateRoute>
                  <NotificationPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/users"
              element={
                <PrivateRoute>
                  <UserManagementPage />
                </PrivateRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </NotificationProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
