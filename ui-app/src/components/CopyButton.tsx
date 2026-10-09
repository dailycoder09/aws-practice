import { useEffect, useRef, useState, type ReactNode } from 'react';

interface Props {
  value: string;
  /** What the button copies, for the tooltip: "hostname", "IP address". */
  what: string;
  className?: string;
  children?: ReactNode;
}

/** Shows a value that copies itself to the clipboard when clicked. */
export function CopyButton({ value, what, className = '', children }: Props) {
  const [copied, setCopied] = useState(false);
  const timer = useRef<number | undefined>(undefined);

  useEffect(() => () => window.clearTimeout(timer.current), []);

  async function copy() {
    try {
      await navigator.clipboard.writeText(value);
    } catch {
      return; // Clipboard blocked (for example on plain http). Nothing useful to do.
    }
    setCopied(true);
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setCopied(false), 1500);
  }

  return (
    <>
      <button
        type="button"
        className={`copy ${copied ? 'copied' : ''} ${className}`.trim()}
        title={copied ? 'Copied' : `Copy ${what}`}
        onClick={copy}
      >
        {children ?? value}
      </button>
      <span className="sr-only" role="status">
        {copied ? `Copied ${what}` : ''}
      </span>
    </>
  );
}
