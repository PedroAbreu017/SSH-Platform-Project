// src/pages/SSHKeys.tsx
import React, { useState, useEffect } from 'react';
import { 
  PlusIcon, 
  KeyIcon, 
  TrashIcon, 
  ClipboardDocumentIcon,
  CheckIcon 
} from '@heroicons/react/24/outline';
import { sshKeyApi } from '../services/api';
import toast from 'react-hot-toast';

interface SSHKey {
  id: number;
  name: string;
  publicKeyFingerprint: string;
  publicKeyType: string;
  active: boolean;
  createdAt: string;
}

const SSHKeys: React.FC = () => {
  const [keys, setKeys] = useState<SSHKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [stats, setStats] = useState({ totalKeys: 0, activeKeys: 0, inactiveKeys: 0 });
  const [formData, setFormData] = useState({
    name: '',
    publicKey: ''
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadSSHKeys();
    loadStats();
  }, []);

  const loadSSHKeys = async () => {
    try {
      const data = await sshKeyApi.getSSHKeys();
      setKeys(data);
    } catch (error) {
      toast.error('Failed to load SSH keys');
      console.error('Error loading SSH keys:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const data = await sshKeyApi.getStats();
      setStats(data);
    } catch (error) {
      console.error('Error loading SSH key stats:', error);
    }
  };

  const handleAddKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.publicKey.trim()) {
      toast.error('Please fill all fields');
      return;
    }

    setSubmitting(true);
    try {
      await sshKeyApi.addSSHKey(formData);
      toast.success('SSH key added successfully');
      setShowAddModal(false);
      setFormData({ name: '', publicKey: '' });
      loadSSHKeys();
      loadStats();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to add SSH key';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteKey = async (id: number, name: string) => {
    if (!confirm(`Are you sure you want to delete SSH key "${name}"?`)) return;

    try {
      await sshKeyApi.deleteSSHKey(id);
      toast.success('SSH key deleted successfully');
      loadSSHKeys();
      loadStats();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to delete SSH key';
      toast.error(message);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      toast.success('Copied to clipboard');
    });
  };

  const generateKeyPair = () => {
    const sampleKey = `ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC7vbqajMzHkE2v5/zGj2Qw8xYeF9ZPm1kJlLm2oPqR3sTuVwXyZ1aBcDeFgHiJkLmNoPqR3sTuVwXyZ1aBcDe user@example.com`;
    setFormData(prev => ({ ...prev, publicKey: sampleKey }));
    toast.success('Sample SSH key generated');
  };

  if (loading) {
    return (
      <div className="animate-fade-in">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-white">SSH Keys</h1>
            <p className="text-gray-400">Manage your SSH public keys for secure authentication</p>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
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
          <h1 className="text-3xl font-bold text-white">SSH Keys</h1>
          <p className="text-gray-400">Manage your SSH public keys for secure authentication</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center space-x-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg transition-colors"
        >
          <PlusIcon className="w-5 h-5" />
          <span>Add SSH Key</span>
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-dark-800 rounded-lg p-6 border border-dark-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-400 text-sm">Total Keys</p>
              <p className="text-2xl font-bold text-white">{stats.totalKeys}</p>
            </div>
            <KeyIcon className="w-8 h-8 text-primary-400" />
          </div>
        </div>
        <div className="bg-dark-800 rounded-lg p-6 border border-dark-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-400 text-sm">Active Keys</p>
              <p className="text-2xl font-bold text-green-400">{stats.activeKeys}</p>
            </div>
            <CheckIcon className="w-8 h-8 text-green-400" />
          </div>
        </div>
        <div className="bg-dark-800 rounded-lg p-6 border border-dark-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-400 text-sm">Inactive Keys</p>
              <p className="text-2xl font-bold text-gray-400">{stats.inactiveKeys}</p>
            </div>
            <KeyIcon className="w-8 h-8 text-gray-400" />
          </div>
        </div>
      </div>

      {/* SSH Keys List */}
      <div className="bg-dark-800 rounded-lg border border-dark-700">
        <div className="p-6 border-b border-dark-700">
          <h2 className="text-xl font-semibold text-white">Your SSH Keys</h2>
        </div>
        <div className="p-6">
          {keys.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <KeyIcon className="w-16 h-16 mx-auto mb-4 opacity-50" />
              <h3 className="text-lg font-medium mb-2">No SSH Keys Found</h3>
              <p className="mb-4">Add your first SSH key to start using secure authentication</p>
              <button
                onClick={() => setShowAddModal(true)}
                className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg transition-colors"
              >
                Add SSH Key
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {keys.map(key => (
                <div key={key.id} className="p-4 bg-dark-700 rounded-lg border border-dark-600">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      <div className="flex-shrink-0">
                        <div className="w-10 h-10 bg-purple-600 rounded-lg flex items-center justify-center">
                          <KeyIcon className="w-5 h-5 text-white" />
                        </div>
                      </div>
                      <div className="min-w-0 flex-1">
                        <h3 className="text-white font-medium">{key.name}</h3>
                        <div className="flex items-center space-x-4 mt-1">
                          <span className="text-sm text-gray-400">
                            {key.publicKeyType.toUpperCase()}
                          </span>
                          <span className="text-sm text-gray-400">
                            Added {new Date(key.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                        <div className="flex items-center space-x-2 mt-2">
                          <code className="text-xs text-gray-300 bg-dark-900 px-2 py-1 rounded font-mono">
                            {key.publicKeyFingerprint}
                          </code>
                          <button
                            onClick={() => copyToClipboard(key.publicKeyFingerprint)}
                            className="text-gray-400 hover:text-white transition-colors"
                            title="Copy fingerprint"
                          >
                            <ClipboardDocumentIcon className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-3">
                      <div className={`px-3 py-1 rounded-full text-xs font-medium ${
                        key.active 
                          ? 'bg-green-900 text-green-300 border border-green-800'
                          : 'bg-gray-900 text-gray-400 border border-gray-800'
                      }`}>
                        {key.active ? 'Active' : 'Inactive'}
                      </div>
                      <button
                        onClick={() => handleDeleteKey(key.id, key.name)}
                        className="p-2 text-gray-400 hover:text-red-400 hover:bg-red-900/20 rounded-md transition-colors"
                        title="Delete SSH key"
                      >
                        <TrashIcon className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Add SSH Key Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-dark-800 rounded-lg border border-dark-700 w-full max-w-2xl">
            <div className="p-6 border-b border-dark-700">
              <h2 className="text-xl font-semibold text-white">Add SSH Key</h2>
              <p className="text-gray-400 mt-1">Add a new SSH public key for secure authentication</p>
            </div>
            <form onSubmit={handleAddKey} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Key Name
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                  className="w-full p-3 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-400 focus:border-primary-500 focus:outline-none"
                  placeholder="e.g., My MacBook Pro"
                  required
                />
              </div>
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-sm font-medium text-gray-300">
                    Public Key
                  </label>
                  <button
                    type="button"
                    onClick={generateKeyPair}
                    className="text-sm text-primary-400 hover:text-primary-300 transition-colors"
                  >
                    Generate Sample Key
                  </button>
                </div>
                <textarea
                  value={formData.publicKey}
                  onChange={(e) => setFormData(prev => ({ ...prev, publicKey: e.target.value }))}
                  className="w-full p-3 bg-dark-700 border border-dark-600 rounded-lg text-white placeholder-gray-400 focus:border-primary-500 focus:outline-none font-mono text-sm"
                  placeholder="ssh-rsa AAAAB3NzaC1yc2EAAAA... user@example.com"
                  rows={4}
                  required
                />
                <p className="text-xs text-gray-400 mt-1">
                  Paste your public key (usually found in ~/.ssh/id_rsa.pub)
                </p>
              </div>
              <div className="flex items-center justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false);
                    setFormData({ name: '', publicKey: '' });
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
                  {submitting ? 'Adding...' : 'Add SSH Key'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default SSHKeys;