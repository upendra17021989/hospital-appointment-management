import React from 'react';
import { useRole } from '../hooks/useRole';

const RoleGuard = ({
  roles = [],
  requireAll = false,
  fallback = null,
  children
}) => {
  const { hasRole, hasAnyRole } = useRole();

  // If no roles specified, allow access
  if (roles.length === 0) {
    return children;
  }

  // Check role access
  const hasAccess = requireAll
    ? roles.every(role => hasRole(role))
    : hasAnyRole(roles);

  if (!hasAccess) {
    return fallback || (
      <div className="alert alert-error">
        <div className="alert-icon">🚫</div>
        <div>
          <div className="alert-title">Access Denied</div>
          <div className="alert-message">
            You don't have permission to access this resource.
          </div>
        </div>
      </div>
    );
  }

  return children;
};

export default RoleGuard;