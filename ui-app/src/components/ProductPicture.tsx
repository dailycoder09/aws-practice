import { useState } from 'react';
import { initials, safeImageUrl } from '../lib/product';

interface Props {
  /** From an editable API: anything but an absolute http or https URL is ignored. */
  url?: string | null;
  /** Describes the image. Leave empty when the text next to it already names the product. */
  alt: string;
  /** The product name, for the initials shown when there is no usable image. */
  name: string;
  className?: string;
}

/**
 * A square picture that never changes size, so a grid does not jump while
 * images load. With no usable URL, or when the image fails to load, the same
 * box shows the product's initials. The fallback is drawn locally and makes no
 * request of its own.
 */
export function ProductPicture({ url, alt, name, className = '' }: Props) {
  const src = safeImageUrl(url);
  // Remember which URL failed, not just that one did, so a new URL gets its own try.
  const [failedSrc, setFailedSrc] = useState<string | undefined>(undefined);
  const showImage = src !== undefined && src !== failedSrc;
  const described = alt !== '';

  return (
    <div className={`picture ${className}`.trim()}>
      {showImage ? (
        <img src={src} alt={alt} loading="lazy" decoding="async" onError={() => setFailedSrc(src)} />
      ) : (
        <div
          className="picture-fallback"
          role={described ? 'img' : undefined}
          aria-label={described ? alt : undefined}
          aria-hidden={described ? undefined : true}
        >
          <span aria-hidden="true">{initials(name)}</span>
        </div>
      )}
    </div>
  );
}
