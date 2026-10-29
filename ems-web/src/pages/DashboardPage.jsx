import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/useAuth';
import { useListUsersQuery } from '../services/usersApi';
import { Chip, PageHeader } from '../components/ui';
import { useGetOnboardingQuery } from '../services/academicApi';
import { useListStudentsQuery } from '../services/studentsApi';

export default function DashboardPage() {
  const { firstName, roles } = useAuth();
  const canManage = roles.includes('TENANT_ADMIN');
  const { data: onboarding } = useGetOnboardingQuery(undefined, { skip: !canManage });
  const { data: studentPage } = useListStudentsQuery({ size: 1 });
  const { data: users = [], isLoading } = useListUsersQuery();
  const count = (role) => users.filter((u) => u.role === role).length;
  const pendingLogins = users.filter((u) => !u.hasLogin).length;
  const show = (n) => (isLoading ? '…' : n);

  const kpis = [
    { label: 'People', value: show(users.length), chip: <Chip tone="info">In your school</Chip> },
    { label: 'Teachers', value: show(count('TEACHER')), chip: <Chip tone="ok">Staff</Chip> },
    { label: 'Students', value: studentPage ? studentPage.totalElements : '…', chip: <Chip tone="info">Enrolled</Chip> },
    { label: 'Logins to set up', value: show(pendingLogins),
      chip: <Chip tone={pendingLogins > 0 ? 'warn' : 'ok'}>{pendingLogins > 0 ? 'Needs attention' : 'All set'}</Chip> },
  ];

  const steps = [
    { label: 'Set up your school profile', done: onboarding?.profileDone, to: '/setup' },
    { label: 'Add an academic year', done: onboarding?.academicYearDone, to: '/setup' },
    { label: 'Create classes and sections', done: onboarding?.classesDone, to: '/setup' },
    { label: 'Invite your first teacher', done: count('TEACHER') > 0, to: '/users' },
    { label: 'Add your students', done: onboarding?.studentsDone, to: '/students' },
  ];

  return (
    <>
      <PageHeader title={`Hello, ${firstName || 'there'}`} subtitle="Here is your school at a glance."
        action={<Link className="btn btn-primary" to="/users">Manage people</Link>} />

      <div className="kpis">
        {kpis.map((k) => (
          <div className="card" key={k.label}>
            <div className="muted">{k.label}</div>
            <div className="kpi-value">{k.value}</div>
            {k.chip}
          </div>
        ))}
      </div>

      <div className="two-col">
        {canManage && (
          <section className="card">
            <h2>Getting started</h2>
            <ul className="checklist">
              {steps.map((s) => (
                <li key={s.label}>
                  <span>{s.label}</span>
                  {s.done ? <Chip tone="ok">Done</Chip>
                    : s.to ? <Link className="btn-link" to={s.to}>Start</Link>
                    : <Chip tone="neutral">Soon</Chip>}
                </li>
              ))}
            </ul>
          </section>
        )}
        <section className="card card-accent">
          <h2>The UniTent network</h2>
          <p className="muted">Share lesson plans, join inter-school events and be discovered by new families. Coming soon.</p>
        </section>
      </div>
    </>
  );
}