import apiClient from './apiClient';

const USER_API_ENDPOINTS = {
  USERS_LIST: '/api/users',
  USER_DETAIL: (id) => `/api/users/${id}`,
  USER_UPDATE: (id) => `/api/users/${id}`,
  USER_DEACTIVATE: (id) => `/api/users/${id}/deactivate`,
  USER_REACTIVATE: (id) => `/api/users/${id}/reactivate`,
  USER_RESEND_ACTIVATION: (id) => `/api/users/${id}/resend-activation`,
  USER_INVITE: '/api/users/invitations',
};

export const userApi = {
  /**
   * Get list of all users
   */
  listUsers: () => {
    return apiClient.get(USER_API_ENDPOINTS.USERS_LIST);
  },

  /**
   * Get a specific user by ID
   */
  getUser: (userId) => {
    return apiClient.get(USER_API_ENDPOINTS.USER_DETAIL(userId));
  },

  /**
   * Invite a new user (ADMIN only)
   * @param {Object} request - { name, email, role }
   */
  inviteUser: (request) => {
    return apiClient.post(USER_API_ENDPOINTS.USER_INVITE, request);
  },

  /**
   * Update user name and/or email (ADMIN only)
   * @param {number} userId - User ID
   * @param {Object} request - { name, email }
   */
  updateUser: (userId, request) => {
    return apiClient.put(USER_API_ENDPOINTS.USER_UPDATE(userId), request);
  },

  /**
   * Deactivate a user (ADMIN only)
   */
  deactivateUser: (userId) => {
    return apiClient.post(USER_API_ENDPOINTS.USER_DEACTIVATE(userId), {});
  },

  /**
   * Reactivate a user (ADMIN only)
   */
  reactivateUser: (userId) => {
    return apiClient.post(USER_API_ENDPOINTS.USER_REACTIVATE(userId), {});
  },

  /**
   * Resend activation link to pending user (ADMIN only)
   */
  resendActivation: (userId) => {
    return apiClient.post(USER_API_ENDPOINTS.USER_RESEND_ACTIVATION(userId), {});
  },
};

export default userApi;
