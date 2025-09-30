import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';

const Terminal: React.FC = () => {
  const { containerId } = useParams<{ containerId: string }>();
  const [output, setOutput] = useState<string[]>([
    'SSH Platform Terminal v1.0 (Simulated)',
    `Connected to container ${containerId || 'demo'}`,
    'Type "help" for available commands',
    ''
  ]);
  const [input, setInput] = useState('');
  const terminalRef = useRef<HTMLDivElement>(null);

  const commands: Record<string, () => string[]> = {
    help: () => [
      'Available commands:',
      '  help     - Show this help message',
      '  clear    - Clear terminal',
      '  ls       - List files',
      '  pwd      - Print working directory',
      '  whoami   - Show current user',
      '  date     - Show current date',
      '  echo     - Echo text',
      '  exit     - Close terminal',
      ''
    ],
    clear: () => {
      setOutput([]);
      return [];
    },
    ls: () => ['Documents/', 'Downloads/', 'Projects/', 'workspace/', 'README.md', 'config.json', ''],
    pwd: () => ['/home/user', ''],
    whoami: () => ['root', ''],
    date: () => [new Date().toLocaleString(), ''],
    exit: () => ['Goodbye!', ''],
  };

  const handleCommand = (cmd: string) => {
    const trimmed = cmd.trim();
    setOutput(prev => [...prev, `$ ${trimmed}`]);

    if (!trimmed) {
      setOutput(prev => [...prev, '']);
      return;
    }

    const [command, ...args] = trimmed.split(' ');

    if (command === 'echo') {
      setOutput(prev => [...prev, args.join(' '), '']);
    } else if (commands[command]) {
      const result = commands[command]();
      setOutput(prev => [...prev, ...result]);
    } else {
      setOutput(prev => [...prev, `bash: ${command}: command not found`, '']);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleCommand(input);
    setInput('');
  };

  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [output]);

  return (
    <div className="h-full flex flex-col bg-gray-900">
      <div className="bg-dark-800 border-b border-dark-700 px-4 py-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-white font-semibold">
              Terminal - Container {containerId || 'Demo'}
            </h1>
            <p className="text-gray-400 text-sm">Simulated SSH terminal</p>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse"></div>
            <span className="text-sm text-green-400">Connected</span>
          </div>
        </div>
      </div>

      <div
        ref={terminalRef}
        className="flex-1 overflow-y-auto p-4 font-mono text-sm text-green-400 bg-gray-900"
      >
        {output.map((line, index) => (
          <div key={index} className="whitespace-pre-wrap">{line}</div>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="bg-dark-800 border-t border-dark-700 p-4">
        <div className="flex items-center space-x-2 font-mono">
          <span className="text-green-400">root@container:~$</span>
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            className="flex-1 bg-transparent text-green-400 outline-none"
            placeholder="Type a command..."
            autoFocus
          />
        </div>
      </form>
      
      <div className="bg-dark-800 border-t border-dark-700 px-4 py-2">
        <div className="text-xs text-gray-400">
          Note: This is a simulated terminal. Real SSH connection coming in v2.0
        </div>
      </div>
    </div>
  );
};

export default Terminal;