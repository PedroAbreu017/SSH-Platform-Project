// src/pages/Login.tsx
import React, { useState } from 'react';
import { CommandLineIcon, EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { authApi } from '../services/api';
import toast from 'react-hot-toast';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface LoginProps {
  onLogin: (token: string, user: User) => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await authApi.login(formData.username, formData.password);
      const user: User = {
        id: response.user.id,
        username: response.user.username,
        email: response.user.email,
        role: response.user.role
      };
      
      onLogin(response.accessToken, user);
      toast.success(`Welcome back, ${user.username}!`);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Login failed';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = () => {
    setFormData({
      username: 'testuser2',
      password: 'password123'
    });
    toast.success('Demo credentials loaded');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-dark-900 via-dark-800 to-dark-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <div className="mx-auto h-16 w-16 bg-primary-600 rounded-xl flex items-center justify-center mb-4">
            <CommandLineIcon className="h-8 w-8 text-white" />
          </div>
          <h2 className="text-3xl font-bold text-white">SSH Platform</h2>
          <p className="mt-2 text-gray-400">
            Sign in to manage your containers and SSH keys
          </p>
        </div>

        <div className="bg-dark-800 border border-dark-700 rounded-lg p-8 space-y-6">
          {/* Demo Notice */}
          <div className="bg-primary-900/20 border border-primary-700/30 rounded-lg p-4">
            <p className="text-sm text-primary-300">
              🚀 Demo Mode: Use the demo credentials or create your own account
            </p>
            <button
              onClick={handleDemoLogin}
              className="mt-2 text-xs text-primary-400 hover:text-primary-300 underline"
            >
              Load demo credentials
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Username
              </label>
              <input
                type="text"
                value={formData.username}
                onChange={(e) => setFormData(prev => ({ ...prev, username: e.target.value }))}
                className="w-full p-3 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-400 focus:border-primary-500 focus:outline-none transition-colors"
                placeholder="Enter your username"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={(e) => setFormData(prev => ({ ...prev, password: e.target.value }))}
                  className="w-full p-3 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-400 focus:border-primary-500 focus:outline-none transition-colors pr-10"
                  placeholder="Enter your password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-white transition-colors"
                >
                  {showPassword ? (
                    <EyeSlashIcon className="w-5 h-5" />
                  ) : (
                    <EyeIcon className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-primary-600 hover:bg-primary-700 disabled:bg-primary-800 disabled:cursor-not-allowed text-white font-medium py-3 px-4 rounded-lg transition-colors flex items-center justify-center"
            >
              {loading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          <div className="text-center">
            <p className="text-sm text-gray-400">
              Don't have an account?{' '}
              <a href="#" className="text-primary-400 hover:text-primary-300 transition-colors">
                Contact your administrator
              </a>
            </p>
          </div>
        </div>

        {/* Features Preview */}
        <div className="grid grid-cols-3 gap-4 text-center">
          <div className="space-y-2">
            <div className="w-8 h-8 bg-primary-600/20 rounded-lg mx-auto flex items-center justify-center">
              <CommandLineIcon className="w-4 h-4 text-primary-400" />
            </div>
            <p className="text-xs text-gray-400">SSH Access</p>
          </div>
          <div className="space-y-2">
            <div className="w-8 h-8 bg-green-600/20 rounded-lg mx-auto flex items-center justify-center">
              <div className="w-3 h-3 bg-green-400 rounded-full"></div>
            </div>
            <p className="text-xs text-gray-400">Live Monitoring</p>
          </div>
          <div className="space-y-2">
            <div className="w-8 h-8 bg-purple-600/20 rounded-lg mx-auto flex items-center justify-center">
              <div className="w-4 h-4 border-2 border-purple-400 rounded"></div>
            </div>
            <p className="text-xs text-gray-400">Secure Keys</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;