export default function ErrorMessage({ error }) {
  if (!error) return null;
  return (
    <div className="error" role="alert">
      {error.message}
      {error.requestId && <small> (reference: {error.requestId})</small>}
    </div>
  );
}