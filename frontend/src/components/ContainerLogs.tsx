import React, { useState, useEffect } from 'react';
import { X, Download, RotateCw } from 'lucide-react';
import { containerApi } from '../services/api';

interface ContainerLogsProps {
  containerId: number;
  containerName: string;
  onClose: () => void;
}

const ContainerLogs: React.FC<ContainerLogsProps> = ({ containerId, containerName, onClose }) => {
  const [logs, setLogs] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lines, setLines] = useState(100);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await containerApi.getContainerLogs(containerId, lines);
      setLogs(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [containerId, lines]);

  const downloadLogs = () => {
    const blob = new Blob([logs.join('\n')], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${containerName}-logs.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-dark-800 rounded-xl w-full max-w-4xl max-h-[80vh] flex flex-col shadow-2xl border border-dark-700">
        <div className="flex items-center justify-between p-4 border-b border-dark-700">
          <div>
            <h2 className="text-xl font-semibold text-white">Container Logs</h2>
            <p className="text-sm text-gray-400 mt-1">{containerName}</p>
          </div>
          
          <div className="flex items-center gap-2">
            <select
              value={lines}
              onChange={(e) => setLines(Number(e.target.value))}
              className="bg-dark-700 text-white px-3 py-2 rounded-lg text-sm border border-dark-600 focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value={50}>Last 50 lines</option>
              <option value={100}>Last 100 lines</option>
              <option value={200}>Last 200 lines</option>
              <option value={500}>Last 500 lines</option>
            </select>
            
            <button
              onClick={fetchLogs}
              disabled={loading}
              className="p-2 hover:bg-dark-700 rounded-lg transition-colors text-gray-400 hover:text-white disabled:opacity-50"
              title="Refresh logs"
            >
              <RotateCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
            </button>
            
            <button
              onClick={downloadLogs}
              disabled={logs.length === 0}
              className="p-2 hover:bg-dark-700 rounded-lg transition-colors text-gray-400 hover:text-white disabled:opacity-50"
              title="Download logs"
            >
              <Download className="w-5 h-5" />
            </button>
            
            <button
              onClick={onClose}
              className="p-2 hover:bg-dark-700 rounded-lg transition-colors text-gray-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-auto p-4 bg-dark-900">
          {loading ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-gray-400">Loading logs...</div>
            </div>
          ) : error ? (
            <div className="text-red-400 bg-red-950/30 border border-red-900 rounded-lg p-4">
              {error}
            </div>
          ) : logs.length === 0 ? (
            <div className="text-gray-500 text-center py-8">
              No logs available
            </div>
          ) : (
            <pre className="text-sm text-green-400 font-mono whitespace-pre-wrap break-all">
              {logs.join('\n')}
            </pre>
          )}
        </div>
      </div>
    </div>
  );
};

export default ContainerLogs;