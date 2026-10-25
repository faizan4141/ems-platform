import { EmptyState } from '../components/ui';

export default function NoAccess() {
  return (
    <section className="card">
      <EmptyState title="Nothing here yet"
        text="Your account has no screens in this version. Student and parent views are coming later." />
    </section>
  );
}