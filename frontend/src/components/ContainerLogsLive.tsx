import React, { useState, useEffect, useRef } from 'react';
import { X, Download, Pause, Play, Trash2 } from 'lucide-react';

interface ContainerLogsLiveProps {
  containerId: number;
  containerName: string;
  onClose: () => void;
}

const ContainerLogsLive: React.FC<ContainerLogsLiveProps> = ({ 
  containerId, 
  containerName, 
  onClose 
}) => {
  const [logs, setLogs] = useState<string[]>([]);
  const [connected, setConnected] = useState(false);
  const [paused, setPaused] = useState(false);
  const [error, setError] = useState('');
  const wsRef = useRef<WebSocket | null>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const pausedLogsRef = useRef<string[]>([]);

  useEffect(() => {
    connectWebSocket();
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [containerId]);

  useEffect(() => {
    if (!paused) {
      scrollToBottom();
    }
  }, [logs, paused]);

  const connectWebSocket = () => {
    const ws = new WebSocket(`ws://localhost:8080/ws/logs/${containerId}`);
    
    ws.onopen = () => {
      console.log('WebSocket connected');
      setConnected(true);
      setError('');
    };

    ws.onmessage = (event) => {
      const logLine = event.data;
      
      if (logLine.startsWith('ERROR:')) {
        setError(logLine);
        return;
      }

      if (logLine === 'STREAM_COMPLETE') {
        setConnected(false);
        return;
      }

      if (paused) {
        pausedLogsRef.current.push(logLine);
      } else {
        setLogs(prev => [...prev, logLine]);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setError('WebSocket connection error');
      setConnected(false);
    };

    ws.onclose = () => {
      console.log('WebSocket closed');
      setConnected(false);
    };

    wsRef.current = ws;
  };

  const scrollToBottom = () => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const togglePause = () => {
    if (paused) {
      // Resume: add all paused logs
      if (pausedLogsRef.current.length > 0) {
        setLogs(prev => [...prev, ...pausedLogsRef.current]);
        pausedLogsRef.current = [];
      }
    }
    setPaused(!paused);
  };

  const clearLogs = () => {
    setLogs([]);
    pausedLogsRef.current = [];
  };

  const downloadLogs = () => {
    const allLogs = [...logs, ...pausedLogsRef.current];
    const blob = new Blob([allLogs.join('\n')], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${containerName}-live-logs.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-dark-800 rounded-xl w-full max-w-5xl max-h-[85vh] flex flex-col shadow-2xl border border-dark-700">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-dark-700">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-semibold text-white">Live Container Logs</h2>
              <div className="flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${connected ? 'bg-green-400 animate-pulse' : 'bg-red-400'}`}></div>
                <span className={`text-sm ${connected ? 'text-green-400' : 'text-red-400'}`}>
                  {connected ? 'Connected' : 'Disconnected'}
                </span>
              </div>
            </div>
            <p className="text-sm text-gray-400 mt-1">{containerName}</p>
          </div>
          
          <div className="flex items-center gap-2">
            <button
              onClick={togglePause}
              className="p-2 hover:bg-dark-700 rounded-lg transition-colors text-gray-400 hover:text-white"
              title={paused ? 'Resume' : 'Pause'}
            >
              {paused ? <Play className="w-5 h-5" /> : <Pause className="w-5 h-5" />}
            </button>
            
            <button
              onClick={clearLogs}
              className="p-2 hover:bg-dark-700 rounded-lg transition-colors text-gray-400 hover:text-white"
              title="Clear logs"
            >
              <Trash2 className="w-5 h-5" />
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

        {/* Status bar */}
        {paused && (
          <div className="bg-yellow-900/30 border-b border-yellow-700 px-4 py-2 flex items-center justify-between">
            <div className="flex items-center gap-2 text-yellow-400 text-sm">
              <Pause className="w-4 h-4" />
              <span>Paused - {pausedLogsRef.current.length} new logs buffered</span>
            </div>
            <button
              onClick={togglePause}
              className="text-yellow-400 hover:text-yellow-300 text-sm font-medium"
            >
              Resume to see updates
            </button>
          </div>
        )}

        {error && (
          <div className="bg-red-900/30 border-b border-red-700 px-4 py-2">
            <p className="text-red-400 text-sm">{error}</p>
          </div>
        )}

        {/* Logs Content */}
        <div className="flex-1 overflow-auto p-4 bg-dark-900">
          {logs.length === 0 ? (
            <div className="text-gray-500 text-center py-8">
              {connected ? 'Waiting for logs...' : 'No logs available'}
            </div>
          ) : (
            <pre className="text-sm text-green-400 font-mono whitespace-pre-wrap break-all">
              {logs.join('\n')}
              <div ref={logsEndRef} />
            </pre>
          )}
        </div>

        {/* Footer */}
        <div className="bg-dark-800 border-t border-dark-700 px-4 py-2">
          <div className="text-xs text-gray-400">
            {logs.length} lines • Real-time streaming via WebSocket
          </div>
        </div>
      </div>
    </div>
  );
};

export default ContainerLogsLive;