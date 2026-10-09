import { fireEvent, render, screen } from '@testing-library/react';
import { ProductPicture } from './ProductPicture';

const URL_OK = 'https://placehold.co/800x800/png?text=Alpha';

function image(container: HTMLElement) {
  return container.querySelector('img');
}

describe('ProductPicture', () => {
  it('renders the image with its alt text, lazy and async', () => {
    const { container } = render(<ProductPicture url={URL_OK} alt="Front view" name="Alpha Phone" />);

    const img = screen.getByRole('img', { name: 'Front view' });
    expect(img).toBe(image(container));
    expect(img).toHaveAttribute('src', URL_OK);
    expect(img).toHaveAttribute('loading', 'lazy');
    expect(img).toHaveAttribute('decoding', 'async');
    expect(container.querySelector('.picture-fallback')).toBeNull();
  });

  it('can be decorative with an empty alt', () => {
    const { container } = render(<ProductPicture url={URL_OK} alt="" name="Alpha Phone" />);
    expect(image(container)).toHaveAttribute('alt', '');
  });

  it('falls back to the initials when the image fails to load', () => {
    const { container } = render(<ProductPicture url={URL_OK} alt="Front view" name="Alpha Phone" />);

    fireEvent.error(image(container)!);

    expect(image(container)).toBeNull();
    const fallback = screen.getByRole('img', { name: 'Front view' });
    expect(fallback).toHaveClass('picture-fallback');
    expect(fallback).toHaveTextContent('AP');
  });

  it('shows the initials when there is no URL', () => {
    const { container } = render(<ProductPicture alt="" name="Bravo Lamp" />);

    expect(image(container)).toBeNull();
    const fallback = container.querySelector('.picture-fallback')!;
    expect(fallback).toHaveTextContent('BL');
    // Decorative: the name next to it already says what this is.
    expect(fallback).toHaveAttribute('aria-hidden', 'true');
  });

  it('treats an empty or missing URL as no URL', () => {
    const { container, rerender } = render(<ProductPicture url="" alt="" name="Charlie" />);
    expect(image(container)).toBeNull();
    rerender(<ProductPicture url={null} alt="" name="Charlie" />);
    expect(image(container)).toBeNull();
    expect(container.querySelector('.picture-fallback')).toHaveTextContent('C');
  });

  it.each([
    'javascript:alert(1)',
    'JAVASCRIPT:alert(document.cookie)',
    'data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=',
    'data:text/html,<script>alert(1)</script>',
    'file:///etc/passwd',
    '//evil.example.com/a.png',
    '/relative/a.png',
  ])('refuses to render %s and shows the fallback instead', (url) => {
    const { container } = render(<ProductPicture url={url} alt="Front view" name="Alpha Phone" />);

    expect(image(container)).toBeNull();
    expect(container.querySelector('.picture-fallback')).toHaveTextContent('AP');
  });

  it('makes no request of its own for the fallback', () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const { container } = render(<ProductPicture url="javascript:alert(1)" alt="Front view" name="Alpha Phone" />);

    const fallback = container.querySelector('.picture-fallback')!;
    expect(fetchMock).not.toHaveBeenCalled();
    // Nothing in it can load anything: no images, no inline background URLs.
    expect(fallback.querySelector('img, image, source, video, iframe, object, embed')).toBeNull();
    expect(fallback.outerHTML).not.toMatch(/url\(|src=|href=/i);
  });

  it('gives a new URL its own try after an earlier one failed', () => {
    const { container, rerender } = render(<ProductPicture url={URL_OK} alt="Front view" name="Alpha Phone" />);
    fireEvent.error(image(container)!);
    expect(image(container)).toBeNull();

    rerender(<ProductPicture url="https://placehold.co/800x800/png?text=Two" alt="Side view" name="Alpha Phone" />);
    expect(image(container)).toHaveAttribute('src', 'https://placehold.co/800x800/png?text=Two');
  });

  it('keeps showing the fallback for the URL that failed', () => {
    const { container, rerender } = render(<ProductPicture url={URL_OK} alt="Front view" name="Alpha Phone" />);
    fireEvent.error(image(container)!);

    rerender(<ProductPicture url={URL_OK} alt="Front view again" name="Alpha Phone" />);
    expect(image(container)).toBeNull();
  });
});
