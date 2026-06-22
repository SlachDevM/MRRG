import apiClient from './apiClient';

const AUTH_ENDPOINTS = {
  ACTIVATE_ACCOUNT: '/api/auth/activate-account',
  VALIDATE_ACTIVATION_TOKEN: '/api/auth/activation-token/validate',
};

const authApi = {
  validateActivationToken: (token) => {
    const query = new URLSearchParams({ token }).toString();
    return apiClient.get(`${AUTH_ENDPOINTS.VALIDATE_ACTIVATION_TOKEN}?${query}`);
  },

  activateAccount: (token, password) => {
    return apiClient.post(AUTH_ENDPOINTS.ACTIVATE_ACCOUNT, { token, password });
  },
};

export default authApi;
