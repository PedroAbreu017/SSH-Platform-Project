// src/services/api.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  login: async (username: string, password: string) => {
    const response = await api.post('/auth/login', { username, password });
    return response.data;
  },
  register: async (userData: any) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },
};

// Container API
export const containerApi = {
  getContainers: async () => {
    const response = await api.get('/containers');
    return response.data;
  },
  createContainer: async (containerData: any) => {
    const response = await api.post('/containers', containerData);
    return response.data;
  },
  startContainer: async (id: number) => {
    const response = await api.post(`/containers/${id}/start`);
    return response.data;
  },
  stopContainer: async (id: number) => {
    const response = await api.post(`/containers/${id}/stop`);
    return response.data;
  },
  deleteContainer: async (id: number) => {
    const response = await api.delete(`/containers/${id}`);
    return response.data;
  },
  getContainerLogs: async (id: number, lines: number = 100) => {
    const response = await api.get(`/containers/${id}/logs?lines=${lines}`);
    return response.data;
  },
};

// SSH Keys API
export const sshKeyApi = {
  getSSHKeys: async () => {
    const response = await api.get('/ssh-keys');
    return response.data;
  },
  addSSHKey: async (keyData: { name: string; publicKey: string }) => {
    const response = await api.post('/ssh-keys', keyData);
    return response.data;
  },
  deleteSSHKey: async (id: number) => {
    const response = await api.delete(`/ssh-keys/${id}`);
    return response.data;
  },
  getStats: async () => {
    const response = await api.get('/ssh-keys/stats');
    return response.data;
  },
};

export default api;