// src/components/Sidebar.tsx
import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  HomeIcon,
  ServerIcon,
  KeyIcon,
  CommandLineIcon,
  UserIcon,
  ArrowRightOnRectangleIcon,
  XMarkIcon,
  Bars3Icon
} from '@heroicons/react/24/outline';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface SidebarProps {
  user: User;
  isOpen: boolean;
  onToggle: () => void;
  onLogout: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ user, isOpen, onToggle, onLogout }) => {
  const location = useLocation();

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: HomeIcon },
    { name: 'Containers', href: '/containers', icon: ServerIcon },
    { name: 'SSH Keys', href: '/ssh-keys', icon: KeyIcon },
    { name: 'Terminal', href: '/terminal', icon: CommandLineIcon },
  ];

  const isActive = (path: string) => location.pathname === path;

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={onToggle}
        />
      )}

      {/* Sidebar */}
      <div className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-dark-900 border-r border-dark-700 transform transition-transform duration-300 ease-in-out
        lg:translate-x-0 lg:static lg:inset-0
        ${isOpen ? 'translate-x-0' : '-translate-x-full lg:w-20'}
      `}>
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-dark-700">
            <div className={`flex items-center space-x-3 ${!isOpen && 'lg:justify-center lg:space-x-0'}`}>
              <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <CommandLineIcon className="w-5 h-5 text-white" />
              </div>
              {(isOpen || window.innerWidth < 1024) && (
                <span className="text-white font-bold text-lg">SSH Platform</span>
              )}
            </div>
            <button
              onClick={onToggle}
              className="text-gray-400 hover:text-white lg:hidden"
            >
              <XMarkIcon className="w-6 h-6" />
            </button>
            <button
              onClick={onToggle}
              className="text-gray-400 hover:text-white hidden lg:block"
            >
              {isOpen ? (
                <XMarkIcon className="w-5 h-5" />
              ) : (
                <Bars3Icon className="w-5 h-5" />
              )}
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-2">
            {navigation.map((item) => {
              const active = isActive(item.href);
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`
                    flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors
                    ${!isOpen && 'lg:justify-center lg:space-x-0'}
                    ${active 
                      ? 'bg-primary-600 text-white' 
                      : 'text-gray-400 hover:text-white hover:bg-dark-800'
                    }
                  `}
                  title={!isOpen ? item.name : ''}
                >
                  <item.icon className="w-5 h-5 flex-shrink-0" />
                  {(isOpen || window.innerWidth < 1024) && (
                    <span>{item.name}</span>
                  )}
                </Link>
              );
            })}
          </nav>

          {/* User Profile */}
          <div className="border-t border-dark-700 p-4">
            <div className={`flex items-center space-x-3 ${!isOpen && 'lg:justify-center lg:space-x-0'}`}>
              <div className="w-8 h-8 bg-gray-600 rounded-full flex items-center justify-center">
                <UserIcon className="w-4 h-4 text-white" />
              </div>
              {(isOpen || window.innerWidth < 1024) && (
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-white truncate">
                    {user.username}
                  </p>
                  <p className="text-xs text-gray-400 truncate">
                    {user.email}
                  </p>
                </div>
              )}
            </div>
            {(isOpen || window.innerWidth < 1024) && (
              <button
                onClick={onLogout}
                className="flex items-center space-x-2 w-full mt-3 px-3 py-2 text-sm text-gray-400 hover:text-white hover:bg-dark-800 rounded-lg transition-colors"
              >
                <ArrowRightOnRectangleIcon className="w-4 h-4" />
                <span>Sign out</span>
              </button>
            )}
            {!isOpen && window.innerWidth >= 1024 && (
              <button
                onClick={onLogout}
                className="flex justify-center w-full mt-3 p-2 text-gray-400 hover:text-white hover:bg-dark-800 rounded-lg transition-colors"
                title="Sign out"
              >
                <ArrowRightOnRectangleIcon className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Sidebar;