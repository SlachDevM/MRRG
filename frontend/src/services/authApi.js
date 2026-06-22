import apiClient from './apiClient';

const AUTH_ENDPOINTS = {
  ACTIVATE_ACCOUNT: '/api/auth/activate-account',
};

const authApi = {
  activateAccount: (token, password) => {
    return apiClient.post(AUTH_ENDPOINTS.ACTIVATE_ACCOUNT, { token, password });
  },
};

export default authApi;
