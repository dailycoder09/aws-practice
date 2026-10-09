import { useRef, useState, type KeyboardEvent } from 'react';
import type { ProductImage } from '../api/types';
import { ProductPicture } from './ProductPicture';

interface Props {
  images: ProductImage[];
  /** The product name: the alt text when an image has none, and the fallback initials. */
  name: string;
}

/**
 * A large picture and, with more than one image, a strip of thumbnails.
 * Left and Right move between images while focus is inside the gallery.
 * Each picture handles its own load failure, so one broken image shows the
 * fallback tile for that image only.
 */
export function ProductGallery({ images, name }: Props) {
  const count = images.length;
  const [selected, setSelected] = useState(0);
  const thumbs = useRef<(HTMLButtonElement | null)[]>([]);
  // The list can shrink under us (a reload with fewer images): never point past the end.
  const index = Math.min(selected, Math.max(0, count - 1));
  const current: ProductImage | undefined = images[index];

  function select(next: number, moveFocus: boolean) {
    setSelected(next);
    if (moveFocus) thumbs.current[next]?.focus();
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (count < 2) return;
    // Leave Alt+Left (browser back) and friends alone.
    if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
    if (event.key === 'ArrowRight') {
      event.preventDefault();
      select((index + 1) % count, true);
    } else if (event.key === 'ArrowLeft') {
      event.preventDefault();
      select((index - 1 + count) % count, true);
    }
  }

  return (
    <div className="gallery" role="group" aria-label="Product images" onKeyDown={onKeyDown}>
      <ProductPicture
        className="gallery-main"
        url={current?.url}
        alt={current?.alt || name}
        name={name}
      />
      {count > 1 && (
        <ul className="gallery-thumbs">
          {images.map((image, position) => (
            <li key={`${position}-${image.url}`}>
              <button
                type="button"
                className="thumb"
                ref={(element) => {
                  thumbs.current[position] = element;
                }}
                aria-label={`Show image ${position + 1} of ${count}`}
                aria-current={position === index ? 'true' : undefined}
                onClick={() => select(position, false)}
              >
                <ProductPicture url={image.url} alt="" name={name} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
