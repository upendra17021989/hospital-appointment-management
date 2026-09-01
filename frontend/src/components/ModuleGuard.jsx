import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ModuleGuard({ module, children }) {
  const { isModuleEnabled } = useAuth();
  if (module && !isModuleEnabled(module)) return <Navigate to="/dashboard" replace />;
  return children;
}
