import { DAYS, slotKey, timeRange } from '../app/timetable';

// renderCell(day, period) returns the cell content (or null). onCellClick makes cells editable buttons.
export default function WeekGrid({ periods, renderCell, onCellClick }) {
  if (periods.length === 0) return null;
  return (
    <div className="table-wrap">
      <table className="table week">
        <thead>
          <tr><th>Period</th>{DAYS.map((d) => <th key={d.n}>{d.short}</th>)}</tr>
        </thead>
        <tbody>
          {periods.map((p) => (p.breakPeriod ? (
            <tr key={p.id} className="break-row">
              <td className="period-head"><strong>{p.name}</strong><div>{timeRange(p)}</div></td>
              <td colSpan={DAYS.length}>Break</td>
            </tr>
          ) : (
            <tr key={p.id}>
              <th className="period-head" scope="row">{p.name}<div className="muted">{timeRange(p)}</div></th>
              {DAYS.map((d) => {
                const content = renderCell(d.n, p);
                return (
                  <td key={slotKey(d.n, p.id)}>
                    {onCellClick ? (
                      <button className={`slot ${content ? 'filled' : ''}`} onClick={() => onCellClick(d.n, p)}
                              aria-label={`${d.label}, ${p.name}`}>{content ?? '+'}</button>
                    ) : (content ? <div className="cell">{content}</div> : null)}
                  </td>
                );
              })}
            </tr>
          )))}
        </tbody>
      </table>
    </div>
  );
}