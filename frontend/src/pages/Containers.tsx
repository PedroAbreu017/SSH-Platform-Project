// src/pages/Containers.tsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  PlusIcon, 
  ServerIcon, 
  PlayIcon, 
  StopIcon,
  TrashIcon,
  CommandLineIcon,
  EyeIcon,
  ClockIcon 
} from '@heroicons/react/24/outline';
import { containerApi } from '../services/api';
import toast from 'react-hot-toast';
import ContainerLogs from '../components/ContainerLogs';


interface Container {
  id: number;
  name: string;
  image: string;
  status: string;
  running: boolean;
  sshPort: number;
  createdAt: string;
}

const Containers: React.FC = () => {
  const [containers, setContainers] = useState<Container[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    image: 'ubuntu:22.04'
  });
  const [submitting, setSubmitting] = useState(false);
  const [showLogsModal, setShowLogsModal] = useState<{id: number, name: string} | null>(null);

  useEffect(() => {
    loadContainers();
    // Auto-refresh every 30 seconds
    const interval = setInterval(loadContainers, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadContainers = async () => {
    try {
      const data = await containerApi.getContainers();
      setContainers(data);
    } catch (error) {
      toast.error('Failed to load containers');
      console.error('Error loading containers:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateContainer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      toast.error('Please enter a container name');
      return;
    }

    setSubmitting(true);
    try {
      await containerApi.createContainer(formData);
      toast.success('Container created successfully');
      setShowCreateModal(false);
      setFormData({ name: '', image: 'ubuntu:22.04' });
      loadContainers();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to create container';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleContainerAction = async (id: number, action: 'start' | 'stop') => {
    setActionLoading(id);
    try {
      if (action === 'start') {
        await containerApi.startContainer(id);
        toast.success('Container started');
      } else {
        await containerApi.stopContainer(id);
        toast.success('Container stopped');
      }
      loadContainers();
    } catch (error: any) {
      const message = error.response?.data?.message || `Failed to ${action} container`;
      toast.error(message);
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeleteContainer = async (id: number, name: string) => {
    if (!confirm(`Are you sure you want to delete container "${name}"? This action cannot be undone.`)) {
      return;
    }

    setActionLoading(id);
    try {
      await containerApi.deleteContainer(id);
      toast.success('Container deleted successfully');
      loadContainers();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to delete container';
      toast.error(message);
    } finally {
      setActionLoading(null);
    }
  };

  const getStatusColor = (running: boolean) => {
    return running 
      ? 'bg-green-400 animate-pulse' 
      : 'bg-red-400';
  };

  const getStatusText = (running: boolean) => {
    return running ? 'Running' : 'Stopped';
  };

  if (loading) {
    return (
      <div className="animate-fade-in space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-white">Containers</h1>
            <p className="text-gray-400">Manage your Docker containers</p>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <div key={i} className="bg-dark-800 rounded-lg p-6 animate-pulse">
              <div className="h-4 bg-gray-700 rounded w-3/4 mb-2"></div>
              <div className="h-8 bg-gray-700 rounded w-1/2"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
  <div className="animate-fade-in space-y-8">
    {/* Header */}
    <div className="flex justify-between items-center">
      <div>
        <h1 className="text-3xl font-bold text-white">Containers</h1>
        <p className="text-gray-400">
          Manage your Docker containers and SSH access
        </p>
      </div>
      <button
        onClick={() => setShowCreateModal(true)}
        className="flex items-center space-x-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg transition-colors"
      >
        <PlusIcon className="w-5 h-5" />
        <span>Create Container</span>
      </button>
    </div>

    {/* Stats */}
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div className="bg-dark-800 rounded-lg p-6 border border-dark-700">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-gray-400 text-sm">Total Containers</p>
            <p className="text-2xl font-bold text-white">{containers.length}</p>
          </div>
          <ServerIcon className="w-8 h-8 text-primary-400" />
        </div>
      </div>
      <div className="bg-dark-800 rounded-lg p-6 border border-dark-700">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-gray-400 text-sm">Running</p>
            <p className="text-2xl font-bold text-green-400">
              {containers.filter(c => c.running).length}
            </p>
          </div>
          <PlayIcon className="w-8 h-8 text-green-400" />
        </div>
      </div>
      <div className="bg-dark-800 rounded-lg p-6 border border-dark-700">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-gray-400 text-sm">Stopped</p>
            <p className="text-2xl font-bold text-red-400">
              {containers.filter(c => !c.running).length}
            </p>
          </div>
          <StopIcon className="w-8 h-8 text-red-400" />
        </div>
      </div>
    </div>

    {/* Containers Grid */}
    <div className="bg-dark-800 rounded-lg border border-dark-700">
      <div className="p-6 border-b border-dark-700">
        <h2 className="text-xl font-semibold text-white">Your Containers</h2>
      </div>
      <div className="p-6">
        {containers.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <ServerIcon className="w-16 h-16 mx-auto mb-4 opacity-50" />
            <h3 className="text-lg font-medium mb-2">No Containers Found</h3>
            <p className="mb-4">Create your first container to get started</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              Create Container
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {containers.map(container => (
              <div key={container.id} className="bg-dark-700 rounded-lg p-6 border border-dark-600">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center space-x-3">
                    <div className="flex-shrink-0">
                      <div className="w-10 h-10 bg-primary-600 rounded-lg flex items-center justify-center">
                        <ServerIcon className="w-5 h-5 text-white" />
                      </div>
                    </div>
                    <div className="min-w-0">
                      <h3 className="text-white font-medium truncate">{container.name}</h3>
                      <p className="text-sm text-gray-400">{container.image}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className={`w-3 h-3 rounded-full ${getStatusColor(container.running)}`}></div>
                    <span className="text-sm text-gray-400">{getStatusText(container.running)}</span>
                  </div>
                </div>

                <div className="space-y-3 mb-4">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-400">SSH Port:</span>
                    <span className="text-white">{container.sshPort}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-400">Created:</span>
                    <span className="text-white">
                      {new Date(container.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleContainerAction(container.id, container.running ? 'stop' : 'start')}
                      disabled={actionLoading === container.id}
                      className={`p-2 rounded-md transition-colors disabled:opacity-50 ${
                        container.running 
                          ? 'bg-red-600 hover:bg-red-700 text-white' 
                          : 'bg-green-600 hover:bg-green-700 text-white'
                      }`}
                    >
                      {actionLoading === container.id ? (
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                      ) : container.running ? (
                        <StopIcon className="w-4 h-4" />
                      ) : (
                        <PlayIcon className="w-4 h-4" />
                      )}
                    </button>
                    
                    {container.running && (
                      <>
                        <Link
                          to={`/terminal/${container.id}`}
                          className="p-2 bg-primary-600 hover:bg-primary-700 text-white rounded-md transition-colors"
                          title="Open Terminal"
                        >
                          <CommandLineIcon className="w-4 h-4" />
                        </Link>
                        
                        <button
                          onClick={() => setShowLogsModal({id: container.id, name: container.name})}
                          className="p-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
                          title="View Logs"
                        >
                          <EyeIcon className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>

                  <button
                    onClick={() => handleDeleteContainer(container.id, container.name)}
                    disabled={actionLoading === container.id}
                    className="p-2 text-gray-400 hover:text-red-400 hover:bg-red-900/20 rounded-md transition-colors"
                    title="Delete Container"
                  >
                    <TrashIcon className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>

    {/* Create Container Modal */}
    {showCreateModal && (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
        <div className="bg-dark-800 rounded-lg border border-dark-700 w-full max-w-md">
          <div className="p-6 border-b border-dark-700">
            <h2 className="text-xl font-semibold text-white">Create New Container</h2>
            <p className="text-gray-400 mt-1">Deploy a new Docker container with SSH access</p>
          </div>
          <form onSubmit={handleCreateContainer} className="p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Container Name
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                className="w-full p-3 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-400 focus:border-primary-500 focus:outline-none"
                placeholder="e.g., my-ubuntu-server"
                required
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Docker Image
              </label>
              <input
                type="text"
                value={formData.image}
                onChange={(e) => setFormData(prev => ({ ...prev, image: e.target.value }))}
                list="image-suggestions"
                placeholder="e.g., ubuntu:22.04"
                className="w-full px-4 py-3 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                required
              />
              
              <datalist id="image-suggestions">
                <option value="ubuntu:22.04" />
                <option value="ubuntu:20.04" />
                <option value="ubuntu:18.04" />
                <option value="debian:11" />
                <option value="debian:10" />
                <option value="alpine:3.18" />
                <option value="alpine:3.17" />
                <option value="node:18" />
                <option value="node:16" />
                <option value="node:20" />
                <option value="python:3.11" />
                <option value="python:3.10" />
                <option value="python:3.9" />
                <option value="openjdk:17" />
                <option value="openjdk:11" />
                <option value="nginx:alpine" />
                <option value="nginx:latest" />
              </datalist>
              
              <p className="mt-2 text-sm text-gray-400">
                Only whitelisted images are allowed for security
              </p>
            </div>
            
            <div className="flex items-center justify-end space-x-3 pt-4">
              <button
                type="button"
                onClick={() => {
                  setShowCreateModal(false);
                  setFormData({ name: '', image: 'ubuntu:22.04' });
                }}
                className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-6 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={submitting}
              >
                {submitting ? 'Creating...' : 'Create Container'}
              </button>
            </div>
          </form>
        </div>
      </div>
    )}

    {/* Container Logs Modal */}
    {showLogsModal && (
      <ContainerLogs
        containerId={showLogsModal.id}
        containerName={showLogsModal.name}
        onClose={() => setShowLogsModal(null)}
      />
    )}
  </div>
);
};

export default Containers;