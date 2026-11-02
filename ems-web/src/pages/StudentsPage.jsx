import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDebounced } from '../app/useDebounced';
import { useSectionOptions } from '../app/useSectionOptions';
import { fullName, placementLabel } from '../app/students';
import { statusTone } from '../app/format';
import { useListStudentsQuery } from '../services/studentsApi';
import ImportDialog from '../components/ImportDialog';
import StudentForm from '../components/StudentForm';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader, SkeletonRows } from '../components/ui';

const PAGE_SIZE = 20;

export default function StudentsPage() {
  const { options: sectionOptions, isAdmin, ready } = useSectionOptions();
  const [search, setSearch] = useState('');
  const debounced = useDebounced(search);
  const [filter, setFilter] = useState('');          // '', 'unassigned' (admins only), or a section id
  const [page, setPage] = useState(0);
  const [showAdd, setShowAdd] = useState(false);
  const [showImport, setShowImport] = useState(false);

  const noAccess = !isAdmin && ready && sectionOptions.length === 0;
  const { data, isLoading, error } = useListStudentsQuery({
    page, size: PAGE_SIZE, search: debounced,
    sectionId: filter && filter !== 'unassigned' ? filter : '', unassigned: filter === 'unassigned',
  }, { skip: noAccess || (!isAdmin && !ready) });

  const total = data?.totalElements ?? 0;
  const pages = Math.max(data?.totalPages ?? 1, 1);

  if (noAccess) {
    return (
      <>
        <PageHeader title="Students" />
        <section className="card">
          <EmptyState title="You are not assigned to any class yet"
            text="Ask a school admin to assign you to a class. You will then see its students here." />
        </section>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Students" subtitle={isLoading ? 'Loading…' : `${total} ${isAdmin ? 'in your school' : 'in your classes'}`}
        action={isAdmin && (
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => setShowImport(true)}>Import CSV</button>
            <button className="btn btn-primary" onClick={() => setShowAdd((v) => !v)}>
              {showAdd ? 'Close form' : 'Add student'}
            </button>
          </div>
        )} />

      {isAdmin && showAdd && (
        <section className="card">
          <h2>Add a student</h2>
          <StudentForm sectionOptions={sectionOptions} onDone={() => setShowAdd(false)} />
        </section>
      )}

      <section className="card">
        <div className="toolbar">
          <div className="grow">
            <Field label="Search">
              <input type="search" value={search} placeholder="Name or admission number"
                     onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
            </Field>
          </div>
          <Field label="Show">
            <select value={filter} onChange={(e) => { setFilter(e.target.value); setPage(0); }}>
              <option value="">{isAdmin ? 'All students' : 'All my classes'}</option>
              {isAdmin && <option value="unassigned">Not assigned to a section</option>}
              {sectionOptions.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
            </select>
          </Field>
        </div>

        <ErrorNotice error={error} />
        {data && total === 0 && (
          <EmptyState title="No students found"
            text={search || filter ? 'Try a different search or filter.' : 'Add a student or import a CSV file to get started.'} />
        )}
        {(isLoading || total > 0) && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Admission no.</th><th>Name</th><th>Class</th><th>Status</th></tr></thead>
              <tbody>
                {isLoading && <SkeletonRows cols={4} />}
                {data?.content.map((s) => (
                  <tr key={s.id}>
                    <td>{s.admissionNo}</td>
                    <td><Link to={`/students/${s.id}`}><strong>{fullName(s)}</strong></Link></td>
                    <td className="muted">{placementLabel(s)}</td>
                    <td><Chip tone={statusTone[s.status]}>{s.status}</Chip></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {total > PAGE_SIZE && (
          <div className="pager">
            <button className="btn btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span className="muted">Page {page + 1} of {pages}</span>
            <button className="btn btn-secondary" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        )}
      </section>

      {isAdmin && <ImportDialog open={showImport} onClose={() => setShowImport(false)} />}
    </>
  );
}