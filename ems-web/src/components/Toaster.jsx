import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { dismissToast } from '../features/ui/uiSlice';

function ToastItem({ toast }) {
  const dispatch = useDispatch();
  useEffect(() => {
    const timer = setTimeout(() => dispatch(dismissToast(toast.id)), 5000);
    return () => clearTimeout(timer);
  }, [dispatch, toast.id]);

  return (
    <div className={`toast toast-${toast.tone}`} role="status">
      <span>{toast.text}</span>
      <button className="btn-link" onClick={() => dispatch(dismissToast(toast.id))}>Dismiss</button>
    </div>
  );
}

export default function Toaster() {
  const toasts = useSelector((state) => state.ui.toasts);
  return <div className="toasts" aria-live="polite">{toasts.map((t) => <ToastItem key={t.id} toast={t} />)}</div>;
}