interface Props {
  /** Zero-based, as in the API. */
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  disabled?: boolean;
  onPrevious: () => void;
  onNext: () => void;
}

export function Pager({ page, totalPages, first, last, disabled = false, onPrevious, onNext }: Props) {
  return (
    <nav className="pager" aria-label="Pagination">
      <button type="button" className="btn" onClick={onPrevious} disabled={disabled || first}>
        Previous
      </button>
      {totalPages > 0 && (
        <span className="mono" aria-live="polite">
          Page {page + 1} of {totalPages}
        </span>
      )}
      <button type="button" className="btn" onClick={onNext} disabled={disabled || last}>
        Next
      </button>
    </nav>
  );
}
