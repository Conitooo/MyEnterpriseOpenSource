export type Role = 'ADMIN' | 'WAREHOUSE_MANAGER' | 'SALES' | 'VIEWER';
export type Section = 'overview' | 'customers' | 'products' | 'warehouses' | 'inventory' | 'orders' | 'users' | 'audit';
export interface User { id: number; companyId: number; username: string; role: Role; active: boolean }
export interface Customer { id: number; companyId: number; name: string; email: string | null; phone: string | null; active: boolean }
export interface Product { id: number; productName: string; sku: string; price: number; currency: string; active: boolean }
export interface Warehouse { id: number; code: string; name: string; active: boolean }
export interface Stock { id: number; productId: number; warehouseId: number; quantity: number; available: number }
export interface Movement { id: number; type: string; quantityChange: number; reason: string }
export interface OrderLine { id: number; productId: number; productName: string; quantity: number; price: number; currency: string }
export interface DeliveryAddress { recipient: string; street: string; city: string; postalCode: string; country: string }
export interface Order { id: number; customerId: number | null; customerName: string | null; deliveryAddress: DeliveryAddress | null; status: string; items: OrderLine[] }
export interface ShipmentLine { id: number; orderItemId: number; quantity: number; returnedQuantity: number }
export interface Shipment { id: number; orderId: number; warehouseId: number; status: string; carrier: string | null; trackingNumber: string | null; items: ShipmentLine[] }
export interface Token { accessToken: string; expiresIn: number }
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface AuditEvent { id: number; occurredAt: string; actorUserId: number | null; username: string | null; requestId: string; httpMethod: string; route: string; path: string; statusCode: number; durationMs: number }
export interface AuditPage { events: AuditEvent[]; total: number; page: number; size: number }
export interface Allocation { orderItemId: number; inventoryId: number; quantity: number }
