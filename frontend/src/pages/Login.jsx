import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/Login.css';

const API_BASE = 'http://localhost:4000';
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [role, setRole] = useState('EMPLOYEE');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmedEmail = email.trim();
    const trimmedPassword = password.trim();

    if (!isRegister) {
      if (!trimmedEmail || !trimmedPassword || !EMAIL_PATTERN.test(trimmedEmail)) {
        setError('Please enter a valid email and password.');
        return;
      }
    }

    const endpoint = isRegister ? '/api/auth/register' : '/api/auth/login';
    const body = isRegister
      ? { email: trimmedEmail, password: trimmedPassword, name, role }
      : { email: trimmedEmail, password: trimmedPassword };

    try {
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        if (!isRegister) {
          setError('Email or password is incorrect.');
          return;
        }
        const message = await response.text();
        setError(message || 'Registration failed.');
        return;
      }

      const data = await response.json();
      login(data, data.token);
      navigate('/');
    } catch (err) {
      setError('Connection error: ' + err.message);
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
          <h1>{isRegister ? 'Create Account' : 'Login'}</h1>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} noValidate={!isRegister}>
          {isRegister && (
            <>
              <div className="form-group">
                <label>Name</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  placeholder="Full name"
                />
              </div>

              <div className="form-group">
                <label>Role</label>
                <select value={role} onChange={(e) => setRole(e.target.value)}>
                  <option value="EMPLOYEE">Employee</option>
                  <option value="MANAGER">Manager</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
            </>
          )}

          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required={isRegister}
              placeholder="you@example.com"
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required={isRegister}
              placeholder="••••••••"
            />
          </div>

          <button type="submit" className="submit-button">
            {isRegister ? 'Register' : 'Login'}
          </button>
        </form>

        <div className="toggle-auth">
          {isRegister ? (
            <p>
              Already have an account? <button onClick={() => setIsRegister(false)}>Login</button>
            </p>
          ) : (
            <p>
              Don't have an account? <button onClick={() => setIsRegister(true)}>Register</button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
