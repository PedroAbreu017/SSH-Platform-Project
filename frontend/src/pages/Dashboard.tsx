// src/pages/Dashboard.tsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  ServerIcon, 
  KeyIcon, 
  CommandLineIcon,
  PlayIcon,
  StopIcon,
  EyeIcon,
  ChartBarIcon 
} from '@heroicons/react/24/outline';
import { containerApi, sshKeyApi } from '../services/api';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface Container {
  id: number;
  name: string;
  status: string;
  running: boolean;
  sshPort: number;
  image: string;
  createdAt: string;
}

interface SSHKey {
  id: number;
  name: string;
  publicKeyType: string;
  active: boolean;
  createdAt: string;
}

interface DashboardStats {
  totalContainers: number;
  runningContainers: number;
  totalSSHKeys: number;
  activeSSHKeys: number;
}

const Dashboard: React.FC<{ user: User }> = ({ user }) => {
  const [containers, setContainers] = useState<Container[]>([]);
  const [sshKeys, setSSHKeys] = useState<SSHKey[]>([]);
  const [stats, setStats] = useState<DashboardStats>({
    totalContainers: 0,
    runningContainers: 0,
    totalSSHKeys: 0,
    activeSSHKeys: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
    // Auto-refresh every 30 seconds
    const interval = setInterval(loadDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadDashboardData = async () => {
    try {
      const [containersData, sshKeysData, sshStatsData] = await Promise.all([
        containerApi.getContainers(),
        sshKeyApi.getSSHKeys(),
        sshKeyApi.getStats()
      ]);

      setContainers(containersData.slice(0, 5)); // Show only recent 5
      setSSHKeys(sshKeysData.slice(0, 5)); // Show only recent 5
      
      setStats({
        totalContainers: containersData.length,
        runningContainers: containersData.filter((c: Container) => c.running).length,
        totalSSHKeys: sshStatsData.totalKeys,
        activeSSHKeys: sshStatsData.activeKeys
      });
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleContainerAction = async (containerId: number, action: 'start' | 'stop') => {
    try {
      if (action === 'start') {
        await containerApi.startContainer(containerId);
      } else {
        await containerApi.stopContainer(containerId);
      }
      loadDashboardData(); // Refresh data
    } catch (error) {
      console.error(`Error ${action}ing container:`, error);
    }
  };

  if (loading) {
    return (
      <div className="animate-fade-in">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="bg-dark-800 rounded-lg p-6 animate-pulse">
              <div className="h-4 bg-gray-700 rounded w-3/4 mb-2"></div>
              <div className="h-8 bg-gray-700 rounded w-1/2"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  const StatCard = ({ title, value, icon: Icon, trend, color = "primary" }: any) => (
    <div className="bg-dark-800 rounded-lg p-6 border border-dark-700 hover:border-dark-600 transition-colors">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-gray-400 text-sm">{title}</p>
          <p className={`text-2xl font-bold text-${color}-400`}>{value}</p>
          {trend && <p className="text-green-400 text-sm flex items-center mt-1">↗ {trend}</p>}
        </div>
        <Icon className={`w-8 h-8 text-${color}-400`} />
      </div>
    </div>
  );

  return (
    <div className="animate-fade-in space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">
          Welcome back, {user.username}
        </h1>
        <p className="text-gray-400">
          Manage your containers and SSH keys from a unified platform
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Containers"
          value={stats.totalContainers}
          icon={ServerIcon}
          trend="+12%"
          color="primary"
        />
        <StatCard
          title="Running Containers"
          value={stats.runningContainers}
          icon={PlayIcon}
          color="green"
        />
        <StatCard
          title="SSH Keys"
          value={stats.totalSSHKeys}
          icon={KeyIcon}
          color="purple"
        />
        <StatCard
          title="Active Keys"
          value={stats.activeSSHKeys}
          icon={ChartBarIcon}
          color="blue"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Containers */}
        <div className="bg-dark-800 rounded-lg border border-dark-700">
          <div className="p-6 border-b border-dark-700">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold text-white">Recent Containers</h2>
              <Link 
                to="/containers"
                className="text-primary-400 hover:text-primary-300 text-sm font-medium"
              >
                View All
              </Link>
            </div>
          </div>
          <div className="p-6 space-y-4">
            {containers.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <ServerIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>No containers found</p>
                <Link 
                  to="/containers" 
                  className="text-primary-400 hover:text-primary-300 text-sm"
                >
                  Create your first container
                </Link>
              </div>
            ) : (
              containers.map(container => (
                <div key={container.id} className="flex items-center justify-between p-4 bg-dark-700 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <div className={`w-3 h-3 rounded-full ${container.running ? 'bg-green-400 animate-pulse-slow' : 'bg-red-400'}`}></div>
                    <div>
                      <h3 className="font-medium text-white">{container.name}</h3>
                      <p className="text-sm text-gray-400">{container.image} • Port {container.sshPort}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleContainerAction(container.id, container.running ? 'stop' : 'start')}
                      className={`p-2 rounded-md transition-colors ${
                        container.running 
                          ? 'bg-red-600 hover:bg-red-700 text-white' 
                          : 'bg-green-600 hover:bg-green-700 text-white'
                      }`}
                    >
                      {container.running ? (
                        <StopIcon className="w-4 h-4" />
                      ) : (
                        <PlayIcon className="w-4 h-4" />
                      )}
                    </button>
                    <Link
                      to={`/terminal/${container.id}`}
                      className="p-2 bg-primary-600 hover:bg-primary-700 text-white rounded-md transition-colors"
                    >
                      <CommandLineIcon className="w-4 h-4" />
                    </Link>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Recent SSH Keys */}
        <div className="bg-dark-800 rounded-lg border border-dark-700">
          <div className="p-6 border-b border-dark-700">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold text-white">SSH Keys</h2>
              <Link 
                to="/ssh-keys"
                className="text-primary-400 hover:text-primary-300 text-sm font-medium"
              >
                Manage Keys
              </Link>
            </div>
          </div>
          <div className="p-6 space-y-4">
            {sshKeys.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <KeyIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>No SSH keys found</p>
                <Link 
                  to="/ssh-keys" 
                  className="text-primary-400 hover:text-primary-300 text-sm"
                >
                  Add your first SSH key
                </Link>
              </div>
            ) : (
              sshKeys.map(key => (
                <div key={key.id} className="flex items-center justify-between p-4 bg-dark-700 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <KeyIcon className="w-5 h-5 text-purple-400" />
                    <div>
                      <h3 className="font-medium text-white">{key.name}</h3>
                      <p className="text-sm text-gray-400">{key.publicKeyType.toUpperCase()} • {new Date(key.createdAt).toLocaleDateString()}</p>
                    </div>
                  </div>
                  <div className={`px-2 py-1 rounded-full text-xs ${
                    key.active 
                      ? 'bg-green-900 text-green-300 border border-green-800' 
                      : 'bg-gray-900 text-gray-400 border border-gray-800'
                  }`}>
                    {key.active ? 'Active' : 'Inactive'}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-dark-800 rounded-lg border border-dark-700 p-6">
        <h2 className="text-xl font-semibold text-white mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            to="/containers"
            className="flex items-center space-x-3 p-4 bg-dark-700 hover:bg-dark-600 rounded-lg transition-colors group"
          >
            <ServerIcon className="w-6 h-6 text-primary-400 group-hover:text-primary-300" />
            <div>
              <h3 className="font-medium text-white">Manage Containers</h3>
              <p className="text-sm text-gray-400">Create, start, stop containers</p>
            </div>
          </Link>
          <Link
            to="/ssh-keys"
            className="flex items-center space-x-3 p-4 bg-dark-700 hover:bg-dark-600 rounded-lg transition-colors group"
          >
            <KeyIcon className="w-6 h-6 text-purple-400 group-hover:text-purple-300" />
            <div>
              <h3 className="font-medium text-white">SSH Keys</h3>
              <p className="text-sm text-gray-400">Add and manage SSH keys</p>
            </div>
          </Link>
          <Link
            to="/terminal"
            className="flex items-center space-x-3 p-4 bg-dark-700 hover:bg-dark-600 rounded-lg transition-colors group"
          >
            <CommandLineIcon className="w-6 h-6 text-green-400 group-hover:text-green-300" />
            <div>
              <h3 className="font-medium text-white">Terminal</h3>
              <p className="text-sm text-gray-400">Access SSH terminal</p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;