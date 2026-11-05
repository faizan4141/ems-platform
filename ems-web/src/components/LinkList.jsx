import { linkLabel, safeLink } from '../app/homework';

export default function LinkList({ links }) {
  const safe = (links ?? []).map((l) => ({ href: safeLink(l), text: linkLabel(l) })).filter((l) => l.href);
  if (safe.length === 0) return null;
  return (
    <div className="links">
      {safe.map((l) => (
        <a key={l.href} href={l.href} target="_blank" rel="noopener noreferrer">{l.text} ↗</a>
      ))}
    </div>
  );
}