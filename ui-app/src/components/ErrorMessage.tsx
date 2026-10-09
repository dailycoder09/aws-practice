import { errorMessage } from '../api/client';
import { withPeriod } from '../lib/format';

interface Props {
  /** What we were trying to do: "Could not load products". */
  what: string;
  error: unknown;
  /** What the person can do about it. */
  hint?: string;
  onRetry?: () => void;
}

export function ErrorMessage({ what, error, hint, onRetry }: Props) {
  return (
    <div className="problem" role="alert">
      <p>
        <strong>{what}.</strong> {withPeriod(errorMessage(error))}
        {hint ? ` ${hint}` : ''}
      </p>
      {onRetry && (
        <button type="button" className="btn" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
