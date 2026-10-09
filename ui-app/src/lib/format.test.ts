import { formatDateTime, formatNumber, formatPercent, formatPrice, formatUptime, withPeriod } from './format';

describe('formatPrice', () => {
  it('uses a dollar sign, thousands separators and two decimals', () => {
    expect(formatPrice(1234.56)).toBe('$1,234.56');
    expect(formatPrice(1234567.891)).toBe('$1,234,567.89');
  });

  it('pads whole amounts', () => {
    expect(formatPrice(5)).toBe('$5.00');
    expect(formatPrice(0)).toBe('$0.00');
  });

  it('shows a dash when there is no usable number', () => {
    expect(formatPrice(undefined)).toBe('—');
    expect(formatPrice(null)).toBe('—');
    expect(formatPrice(Number.NaN)).toBe('—');
  });
});

describe('formatNumber', () => {
  it('adds thousands separators', () => {
    expect(formatNumber(5010)).toBe('5,010');
    expect(formatNumber(undefined)).toBe('—');
  });
});

describe('formatPercent', () => {
  it('keeps whole numbers whole and rounds the rest to one decimal', () => {
    expect(formatPercent(50)).toBe('50%');
    expect(formatPercent(100)).toBe('100%');
    expect(formatPercent((1 / 3) * 100)).toBe('33.3%');
    expect(formatPercent(0)).toBe('0%');
  });
});

describe('formatUptime', () => {
  it('shows hours, minutes and seconds', () => {
    expect(formatUptime(0)).toBe('00:00:00');
    expect(formatUptime(3_723_000)).toBe('01:02:03');
  });

  it('adds days once there is at least one', () => {
    expect(formatUptime(93_784_000)).toBe('1d 02:03:04');
  });

  it('never goes negative when clocks disagree', () => {
    expect(formatUptime(-5000)).toBe('00:00:00');
  });
});

describe('formatDateTime', () => {
  it('formats a valid timestamp and dashes an invalid one', () => {
    expect(formatDateTime('2026-10-04T08:43:44Z')).toMatch(/\d/);
    expect(formatDateTime('not a date')).toBe('—');
    expect(formatDateTime(undefined)).toBe('—');
  });
});

describe('withPeriod', () => {
  it('adds a full stop only when the text has no closing punctuation', () => {
    expect(withPeriod('Could not reach the server')).toBe('Could not reach the server.');
    expect(withPeriod('Already ends.')).toBe('Already ends.');
    expect(withPeriod('Really?')).toBe('Really?');
  });
});
