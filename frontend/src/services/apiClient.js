import API_CONFIG from '../config/apiConfig';

class ApiClient {
  constructor(baseURL, token = null) {
    this.baseURL = baseURL;
    this.token = token;
  }

  setToken(token) {
    this.token = token;
  }

  getHeaders() {
    const headers = {
      'Content-Type': 'application/json',
    };
    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }
    return headers;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const config = {
      ...options,
      headers: {
        ...this.getHeaders(),
        ...options.headers,
      },
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        // Try to extract error message from response body
        let message = `API request failed: ${response.status}`;
        try {
          const contentType = response.headers.get('content-type');
          if (contentType && contentType.includes('application/json')) {
            const errorData = await response.json();
            if (errorData.message) {
              message = errorData.message;
            } else if (errorData.detail) {
              message = errorData.detail;
            }
          } else {
            const text = await response.text();
            if (text) message = text;
          }
        } catch (e) {
          // If parsing fails, use default message
        }

        const error = new Error(message);
        error.status = response.status;
        error.statusText = response.statusText;

        if (response.status === 401) {
          error.type = 'UNAUTHORIZED';
        } else if (response.status === 403) {
          error.type = 'FORBIDDEN';
        } else if (response.status === 404) {
          error.type = 'NOT_FOUND';
        } else if (response.status === 400) {
          error.type = 'BAD_REQUEST';
        } else if (response.status >= 500) {
          error.type = 'SERVER_ERROR';
        }

        throw error;
      }

      // Handle 204 No Content and other empty responses
      if (response.status === 204 || response.headers.get('content-length') === '0') {
        return null;
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const data = await response.json();
        return data;
      }

      return null;
    } catch (err) {
      if (err instanceof TypeError) {
        err.type = 'NETWORK_ERROR';
      }
      throw err;
    }
  }

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  put(endpoint, data) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  }
}

const apiClient = new ApiClient(API_CONFIG.BASE_URL);

export default apiClient;
