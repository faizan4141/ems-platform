import { Navigate } from 'react-router-dom';
import { useAuth } from '../features/auth/useAuth';
import { hasAnyRole } from '../auth/roles';

export default function RequireRole({ roles, children }) {
  const auth = useAuth();
  return hasAnyRole(auth.roles, roles) ? children : <Navigate to="/" replace />;
}