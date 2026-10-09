import { inventoryLevel, stockLevel } from './stock';

describe('stockLevel', () => {
  it.each([
    [0, 'out'],
    [1, 'low'],
    [20, 'low'],
    [21, 'ok'],
    [215, 'ok'],
  ])('quantity %i is %s with the default threshold', (quantity, expected) => {
    expect(stockLevel(quantity)).toBe(expected);
  });

  it('is unknown when there is no number', () => {
    expect(stockLevel(undefined)).toBe('unknown');
    expect(stockLevel(null)).toBe('unknown');
    expect(stockLevel(Number.NaN)).toBe('unknown');
  });

  it('treats a negative quantity as out of stock', () => {
    expect(stockLevel(-3)).toBe('out');
  });

  it('honours a custom threshold', () => {
    expect(stockLevel(5, 5)).toBe('low');
    expect(stockLevel(6, 5)).toBe('ok');
  });
});

describe('inventoryLevel', () => {
  it('uses the item reorder threshold', () => {
    expect(inventoryLevel({ quantityOnHand: 0, reorderThreshold: 10 })).toBe('out');
    expect(inventoryLevel({ quantityOnHand: 10, reorderThreshold: 10 })).toBe('low');
    expect(inventoryLevel({ quantityOnHand: 11, reorderThreshold: 10 })).toBe('ok');
  });

  it('can call 30 units fine for one item and low for another', () => {
    expect(inventoryLevel({ quantityOnHand: 30, reorderThreshold: 5 })).toBe('ok');
    expect(inventoryLevel({ quantityOnHand: 30, reorderThreshold: 50 })).toBe('low');
  });

  it('is out at zero even when the threshold is zero', () => {
    expect(inventoryLevel({ quantityOnHand: 0, reorderThreshold: 0 })).toBe('out');
  });
});
