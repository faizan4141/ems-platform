import { useSelector } from 'react-redux';
import { logout } from './initAuth';

export function useAuth() {
  const auth = useSelector((state) => state.auth);
  return { ...auth, logout };
}