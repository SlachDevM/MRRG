import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import authApi from '../services/authApi';
import '../styles/ActivateAccount.css';

export default function ActivateAccount() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token')?.trim() ?? '';

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!token) {
      setError('Activation link is invalid. Please use the link from your invitation email.');
      return;
    }

    const trimmedPassword = password.trim();
    const trimmedConfirm = confirmPassword.trim();

    if (!trimmedPassword || !trimmedConfirm) {
      setError('Please enter and confirm your password.');
      return;
    }

    if (trimmedPassword !== trimmedConfirm) {
      setError('Passwords do not match.');
      return;
    }

    try {
      setLoading(true);
      await authApi.activateAccount(token, trimmedPassword);
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Account activation failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="activate-container">
      <div className="activate-card">
        <div className="activate-header">
          <img
            src="https://static.wixstatic.com/media/4c239c_3d5b277894a042ef8d75ecd3fdacfdda~mv2.png/v1/fill/w_378,h_174,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/RE-GUTTERS_edited.png"
            alt="RE-GUTTERS"
            className="logo"
          />
          <h1>Activate Account</h1>
        </div>

        {success ? (
          <div className="activate-success">
            <p>Your account has been activated. You can now log in.</p>
            <Link to="/login" className="activate-link-button">
              Go to Login
            </Link>
          </div>
        ) : (
          <>
            {!token && (
              <div className="error-message">
                Activation link is invalid. Please use the link from your invitation email.
              </div>
            )}

            {error && <div className="error-message">{error}</div>}

            <form onSubmit={handleSubmit} noValidate>
              <div className="form-group">
                <label htmlFor="password">Password</label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  disabled={!token || loading}
                  placeholder="Choose a password"
                  autoComplete="new-password"
                />
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm Password</label>
                <input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  disabled={!token || loading}
                  placeholder="Confirm your password"
                  autoComplete="new-password"
                />
              </div>

              <button
                type="submit"
                className="submit-button"
                disabled={!token || loading}
              >
                {loading ? 'Activating...' : 'Activate Account'}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
