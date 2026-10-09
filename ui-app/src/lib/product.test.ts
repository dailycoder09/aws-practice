import { formatPrice } from './format';
import { formatDiscount, formatSavings, initials, parseProductId, safeImageUrl, savings, stockStatus } from './product';

describe('safeImageUrl', () => {
  it('accepts absolute http and https URLs', () => {
    expect(safeImageUrl('https://placehold.co/800x800/png?text=Alpha')).toBe('https://placehold.co/800x800/png?text=Alpha');
    expect(safeImageUrl('http://images.example.com/a.jpg')).toBe('http://images.example.com/a.jpg');
    expect(safeImageUrl('  https://example.com/a.png  ')).toBe('https://example.com/a.png');
  });

  it.each([
    'javascript:alert(1)',
    ' JaVaScRiPt:alert(1)',
    'java\tscript:alert(1)',
    'data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=',
    'file:///etc/passwd',
    'ftp://example.com/a.png',
    'blob:https://example.com/1234',
    '//example.com/a.png',
    '/images/a.png',
    'a.png',
    'not a url',
    '',
    '   ',
  ])('ignores %j', (value) => {
    expect(safeImageUrl(value)).toBeUndefined();
  });

  it('ignores a missing value', () => {
    expect(safeImageUrl(undefined)).toBeUndefined();
    expect(safeImageUrl(null)).toBeUndefined();
  });
});

describe('initials', () => {
  it('uses the first letters of up to two words', () => {
    expect(initials('Alpha Phone')).toBe('AP');
    expect(initials('Alpha Phone Pro Max')).toBe('AP');
    expect(initials('alpha')).toBe('A');
  });

  it('ignores extra spacing and leading punctuation', () => {
    expect(initials('  alpha   phone ')).toBe('AP');
    expect(initials('- (Alpha) Phone')).toBe('AP');
  });

  it('keeps digits and non-Latin letters', () => {
    expect(initials('4K Monitor')).toBe('4M');
    expect(initials('Ünïcode Straße')).toBe('ÜS');
  });

  it('falls back to a question mark when there is nothing to use', () => {
    expect(initials('')).toBe('?');
    expect(initials('   ')).toBe('?');
    expect(initials('--- ...')).toBe('?');
    expect(initials(undefined)).toBe('?');
  });
});

describe('formatDiscount', () => {
  it('formats a whole percent', () => {
    expect(formatDiscount(20)).toBe('20% off');
    expect(formatDiscount(12.6)).toBe('13% off');
  });

  it('is empty when there is no discount', () => {
    expect(formatDiscount(undefined)).toBeUndefined();
    expect(formatDiscount(null)).toBeUndefined();
    expect(formatDiscount(0)).toBeUndefined();
    expect(formatDiscount(-5)).toBeUndefined();
    expect(formatDiscount(Number.NaN)).toBeUndefined();
  });
});

describe('savings', () => {
  it('derives the amount and percent when the MRP is above the price', () => {
    expect(savings(799.99, 999.99)).toEqual({ amount: 200, percent: 20 });
  });

  it('prefers the percent the server sent', () => {
    expect(savings(80, 100, 21)).toEqual({ amount: 20, percent: 21 });
  });

  it('derives the percent when the server sent none or zero', () => {
    expect(savings(75, 100, 0)).toEqual({ amount: 25, percent: 25 });
    expect(savings(75, 100, null)).toEqual({ amount: 25, percent: 25 });
  });

  it('rounds the amount to cents', () => {
    expect(savings(0.1, 0.3)?.amount).toBe(0.2);
  });

  it('is empty unless the MRP really is above the price', () => {
    expect(savings(100, 100)).toBeUndefined();
    expect(savings(100, 90)).toBeUndefined();
    expect(savings(100, undefined)).toBeUndefined();
    expect(savings(100, null)).toBeUndefined();
    expect(savings(100, Number.NaN)).toBeUndefined();
    expect(savings(100, 100.001)).toBeUndefined(); // less than a cent
  });
});

describe('formatSavings', () => {
  it('words the saving with the percent', () => {
    expect(formatSavings({ amount: 200, percent: 20 }, formatPrice)).toBe('You save $200.00 (20% off)');
  });

  it('leaves the percent out when there is none to show', () => {
    expect(formatSavings({ amount: 0.5, percent: 0 }, formatPrice)).toBe('You save $0.50');
  });
});

describe('stockStatus', () => {
  it.each([
    [42, 'ok', 'In stock (42)'],
    [21, 'ok', 'In stock (21)'],
    [1234, 'ok', 'In stock (1,234)'],
    [20, 'low', 'Only 20 left'],
    [7, 'low', 'Only 7 left'],
    [1, 'low', 'Only 1 left'],
    [0, 'out', 'Out of stock'],
    [-2, 'out', 'Out of stock'],
    [null, 'unknown', 'Stock unavailable'],
    [undefined, 'unknown', 'Stock unavailable'],
  ])('quantity %s is %s: %s', (quantity, level, text) => {
    expect(stockStatus(quantity)).toEqual({ level, text });
  });
});

describe('parseProductId', () => {
  it('accepts positive whole numbers', () => {
    expect(parseProductId('1')).toBe(1);
    expect(parseProductId('4021')).toBe(4021);
  });

  it.each(['abc', '', '0', '-1', '1.5', '1e3', '007', ' 1', '1 ', '12abc', '99999999999999999999', undefined])(
    'rejects %j',
    (value) => {
      expect(parseProductId(value)).toBeUndefined();
    },
  );
});
