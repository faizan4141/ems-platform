import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { hasAnyRole } from '../auth/roles';

export default function RequireRole({ roles, children }) {
  const auth = useAuth();
  if (!hasAnyRole(auth.roles, roles)) {
    return <Navigate to="/" replace />;
  }
  return children;
}