import { useCallback, useEffect, useState, type DependencyList } from 'react';
import { isAbortError } from '../api/client';

export interface AsyncResult<T> {
  /** The last successful result. Kept while a newer request is loading. */
  data: T | undefined;
  /** Set when the latest request failed. Cleared when the next one starts. */
  error: Error | undefined;
  loading: boolean;
  reload: () => void;
}

/**
 * Runs `load` on mount, whenever `deps` change, and when `reload` is called.
 * An older request is aborted when a newer one starts, so results never
 * arrive out of order.
 */
export function useAsync<T>(load: (signal: AbortSignal) => Promise<T>, deps: DependencyList): AsyncResult<T> {
  const [data, setData] = useState<T | undefined>(undefined);
  const [error, setError] = useState<Error | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [attempt, setAttempt] = useState(0);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(undefined);
    load(controller.signal).then(
      (result) => {
        if (controller.signal.aborted) return;
        setData(result);
        setLoading(false);
      },
      (failure: unknown) => {
        if (controller.signal.aborted || isAbortError(failure)) return;
        setError(failure instanceof Error ? failure : new Error(String(failure)));
        setLoading(false);
      },
    );
    return () => controller.abort();
  }, [...deps, attempt]);

  return { data, error, loading, reload };
}
