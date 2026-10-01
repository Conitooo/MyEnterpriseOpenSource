import { Allocation, Order, Stock } from './models';

export function positiveInteger(value: number): boolean {
  return Number.isSafeInteger(Number(value)) && Number(value) > 0;
}

export function passwordIsValid(value: string): boolean {
  const bytes = new TextEncoder().encode(value).length;
  return value.length >= 14 && bytes <= 72;
}

export function allocationsAreValid(order: Order | undefined, allocations: Allocation[], stocks: Stock[]): boolean {
  if (!order?.items.length || allocations.length < order.items.length) return false;
  const stockById = new Map(stocks.map(stock => [stock.id, stock]));
  const reserved = new Map<number, number>();
  for (const line of order.items) {
    const rows = allocations.filter(row => row.orderItemId === line.id);
    if (!rows.length || new Set(rows.map(row => Number(row.inventoryId))).size !== rows.length) return false;
    if (rows.reduce((sum, row) => sum + Number(row.quantity), 0) !== line.quantity) return false;
    for (const row of rows) {
      const stock = stockById.get(Number(row.inventoryId));
      if (!stock || stock.productId !== line.productId || !positiveInteger(row.quantity)) return false;
      reserved.set(stock.id, (reserved.get(stock.id) || 0) + Number(row.quantity));
    }
  }
  if (allocations.some(row => !order.items.some(line => line.id === row.orderItemId))) return false;
  return [...reserved].every(([id, quantity]) => quantity <= stockById.get(id)!.available);
}
