import { initials } from '../app/format';

export function Chip({ tone = 'neutral', children }) {
  return <span className={`chip chip-${tone}`}>{children}</span>;
}

export function Field({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

export function Avatar({ name }) {
  return <span className="avatar" aria-hidden="true">{initials(name)}</span>;
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function EmptyState({ title, text }) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      <p className="muted">{text}</p>
    </div>
  );
}

export function ErrorNotice({ error }) {
  if (!error) return null;
  return (
    <div className="notice-error" role="alert">
      {error.message}
      {error.requestId && <small> (reference: {error.requestId})</small>}
    </div>
  );
}

export function SkeletonRows({ rows = 4, cols = 4 }) {
  return Array.from({ length: rows }, (_, r) => (
    <tr key={r}>
      {Array.from({ length: cols }, (_, c) => <td key={c}><div className="skeleton" style={{ width: '80%' }} /></td>)}
    </tr>
  ));
}