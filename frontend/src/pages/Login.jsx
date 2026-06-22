import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../services/apiClient';
import { API_ENDPOINTS } from '../constants/jobConfig';
import '../styles/Login.css';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmedEmail = email.trim().toLowerCase();
    const trimmedPassword = password.trim();

    if (!trimmedEmail || !trimmedPassword || !EMAIL_PATTERN.test(trimmedEmail)) {
      setError('Please enter a valid email and password.');
      return;
    }

    try {
      const data = await apiClient.post(API_ENDPOINTS.AUTH_LOGIN, {
        email: trimmedEmail,
        password: trimmedPassword,
      });

      login(data, data.token);
      navigate('/');
    } catch (err) {
      // Display backend-provided error message for disabled/pending accounts
      if (err.message && (err.message.includes('disabled') || err.message.includes('not activated'))) {
        setError(err.message);
      } else {
        // Generic message for all other login failures
        setError('Email or password is incorrect.');
      }
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <img
            src="https://static.wixstatic.com/media/4c239c_3d5b277894a042ef8d75ecd3fdacfdda~mv2.png/v1/fill/w_378,h_174,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/RE-GUTTERS_edited.png"
            alt="RE-GUTTERS"
            className="logo"
          />
          <h1>Login</h1>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="you@example.com"
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
          </div>

          <button type="submit" className="submit-button">
            Login
          </button>
        </form>
      </div>
    </div>
  );
}
