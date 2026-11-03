import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/useAuth';
import { useListUsersQuery } from '../services/usersApi';
import { useGetOnboardingQuery } from '../services/academicApi';
import { useListStudentsQuery } from '../services/studentsApi';
import { useGetAttendanceSummaryQuery } from '../services/attendanceApi';
import { useGetMyTeachingQuery } from '../services/staffApi';
import { Chip, EmptyState, PageHeader } from '../components/ui';
import { useGetMyTodayQuery } from '../services/timetableApi';
import { hhmm } from '../app/timetable';

function TodayLessons() {
  const { data } = useGetMyTodayQuery();
  return (
    <section className="card">
      <h2>Today’s lessons</h2>
      {!data ? <div className="skeleton" style={{ width: '50%' }} /> : data.lessons.length === 0 ? (
        <p className="muted">No lessons scheduled for you today.</p>
      ) : data.lessons.map((l) => (
        <div className="lesson" key={`${l.periodId}`}>
          <span>
            <strong>{hhmm(l.startTime)}</strong> <span className="muted">· {l.periodName}</span><br />
            {l.subjectName} <span className="muted">· {l.className} · {l.sectionName}{l.room ? ` · Room ${l.room}` : ''}</span>
          </span>
          <Link className="btn btn-secondary" to={`/attendance?section=${l.sectionId}`}>Take attendance</Link>
        </div>
      ))}
    </section>
  );
}

function TeacherClasses() {
  const { data: teaching } = useGetMyTeachingQuery();
  const sections = teaching?.sections ?? [];
  return (
    <section className="card">
      <div className="page-header" style={{ marginBottom: 12 }}>
        <h2 style={{ margin: 0 }}>Your classes</h2>
        <Link className="btn btn-primary" to="/attendance">Take attendance</Link>
      </div>
      {teaching && sections.length === 0 ? (
        <EmptyState title="You are not assigned to any class yet" text="Ask a school admin to assign you. Your classes appear here." />
      ) : (
        <ul className="checklist">
          {sections.map((s) => (
            <li key={s.sectionId}>
              <strong>{s.className} · {s.sectionName}</strong>
              <span className="row-actions">
                {s.classTeacher && <Chip tone="info">Class teacher</Chip>}
                {s.subjects.map((x) => <Chip key={x.id} tone="neutral">{x.name}</Chip>)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default function DashboardPage() {
  const { firstName, roles } = useAuth();
  const canManage = roles.includes('TENANT_ADMIN');
  const { data: attendance } = useGetAttendanceSummaryQuery();
  const { data: onboarding } = useGetOnboardingQuery(undefined, { skip: !canManage });
  const { data: users = [], isLoading: usersLoading } = useListUsersQuery(undefined, { skip: !canManage });
  const { data: studentPage } = useListStudentsQuery({ size: 1 });

  const count = (role) => users.filter((u) => u.role === role).length;
  const pendingLogins = users.filter((u) => !u.hasLogin).length;
  const show = (n) => (usersLoading ? '…' : n);

  const kpis = [
    {
      label: 'Attendance today',
      value: !attendance ? '…' : attendance.percentPresent == null ? '—' : `${attendance.percentPresent}%`,
      chip: <Chip tone={attendance && attendance.sectionsTaken >= attendance.sectionsTotal && attendance.sectionsTotal > 0 ? 'ok' : 'warn'}>
        {attendance ? `${attendance.sectionsTaken} of ${attendance.sectionsTotal} classes done` : '…'}
      </Chip>,
    },
    { label: canManage ? 'Students' : 'Your students', value: studentPage ? studentPage.totalElements : '…',
      chip: <Chip tone="info">{canManage ? 'Enrolled' : 'In your classes'}</Chip> },
    ...(canManage ? [
      { label: 'People', value: show(users.length), chip: <Chip tone="info">In your school</Chip> },
      { label: 'Teachers', value: show(count('TEACHER')), chip: <Chip tone="ok">Staff</Chip> },
      { label: 'Logins to set up', value: show(pendingLogins),
        chip: <Chip tone={pendingLogins > 0 ? 'warn' : 'ok'}>{pendingLogins > 0 ? 'Needs attention' : 'All set'}</Chip> },
    ] : []),
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
        action={canManage && <Link className="btn btn-primary" to="/users">Manage people</Link>} />

      <div className="kpis">
        {kpis.map((k) => (
          <div className="card" key={k.label}>
            <div className="muted">{k.label}</div>
            <div className="kpi-value">{k.value}</div>
            {k.chip}
          </div>
        ))}
      </div>

      {!canManage && <TodayLessons />}

      <div className="two-col">
        {canManage ? (
          <section className="card">
            <h2>Getting started</h2>
            <ul className="checklist">
              {steps.map((s) => (
                <li key={s.label}>
                  <span>{s.label}</span>
                  {s.done ? <Chip tone="ok">Done</Chip> : <Link className="btn-link" to={s.to}>Start</Link>}
                </li>
              ))}
            </ul>
          </section>
        ) : <TeacherClasses />}
        <section className="card card-accent">
          <h2>The UniTent network</h2>
          <p className="muted">Share lesson plans, join inter-school events and be discovered by new families. Coming soon.</p>
        </section>
      </div>
    </>
  );
}