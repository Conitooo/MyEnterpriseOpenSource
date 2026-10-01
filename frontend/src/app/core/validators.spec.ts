import { describe, expect, it } from 'vitest';
import { Order, Stock } from './models';
import { allocationsAreValid, passwordIsValid, positiveInteger } from './validators';

const order: Order = {
  id: 1, customerId: 2, customerName: 'Cliente', deliveryAddress: null, status: 'DRAFT',
  items: [{ id: 11, productId: 3, productName: 'Producto', quantity: 5, price: 10, currency: 'EUR' }],
};
const stocks: Stock[] = [
  { id: 20, productId: 3, warehouseId: 1, quantity: 3, available: 3 },
  { id: 21, productId: 3, warehouseId: 2, quantity: 4, available: 4 },
];

describe('validación de formularios y reservas', () => {
  it('acepta solo enteros positivos seguros', () => {
    expect(positiveInteger(2)).toBe(true);
    expect(positiveInteger(1.5)).toBe(false);
    expect(positiveInteger(Number.NaN)).toBe(false);
    expect(positiveInteger(0)).toBe(false);
  });

  it('respeta el límite de bytes UTF-8 de las contraseñas', () => {
    expect(passwordIsValid('Password2026!xyz')).toBe(true);
    expect(passwordIsValid('a'.repeat(13))).toBe(false);
    expect(passwordIsValid('é'.repeat(40))).toBe(false);
  });

  it('valida una reserva dividida y bloquea stock, duplicados y líneas ajenas', () => {
    const valid = [
      { orderItemId: 11, inventoryId: 20, quantity: 2 },
      { orderItemId: 11, inventoryId: 21, quantity: 3 },
    ];
    expect(allocationsAreValid(order, valid, stocks)).toBe(true);
    expect(allocationsAreValid(order, [{ ...valid[0], quantity: 4 }, valid[1]], stocks)).toBe(false);
    expect(allocationsAreValid(order, [valid[0], { ...valid[1], inventoryId: 20 }], stocks)).toBe(false);
    expect(allocationsAreValid(order, [...valid, { orderItemId: 99, inventoryId: 21, quantity: 1 }], stocks)).toBe(false);
  });
});
