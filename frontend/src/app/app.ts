import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

type Role = 'ADMIN' | 'WAREHOUSE_MANAGER' | 'SALES' | 'VIEWER';
type Section = 'overview' | 'products' | 'warehouses' | 'inventory' | 'orders' | 'users' | 'audit';
interface User { id: number; companyId: number; username: string; role: Role; active: boolean }
interface Product { id: number; productName: string; sku: string; price: number; currency: string }
interface Warehouse { id: number; code: string; name: string }
interface Stock { id: number; productId: number; warehouseId: number; quantity: number; available: number }
interface Movement { id: number; type: string; quantityChange: number; reason: string }
interface OrderLine { id: number; productId: number; quantity: number; price: number; currency: string }
interface Order { id: number; status: string; items: OrderLine[] }
interface Shipment { id: number; orderId: number; warehouseId: number; status: string }
interface Token { accessToken: string }
interface AuditEvent { id: number; occurredAt: string; actorUserId: number | null; username: string | null; requestId: string; httpMethod: string; route: string; path: string; statusCode: number; durationMs: number }
interface AuditPage { events: AuditEvent[]; total: number; page: number; size: number }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly changeDetector = inject(ChangeDetectorRef);
  token = sessionStorage.getItem('meos_token') || '';
  me: User | null = null;
  section: Section = 'overview';
  busy = false;
  notice = '';
  error = '';
  loginForm = { companyId: 1, username: 'admin', password: '' };
  products: Product[] = [];
  warehouses: Warehouse[] = [];
  stocks: Stock[] = [];
  allStocks: Stock[] = [];
  orders: Order[] = [];
  users: User[] = [];
  audit: AuditPage = { events: [], total: 0, page: 0, size: 50 };
  issuedCredentials: { companyId: number; username: string; password: string; role: Role } | null = null;
  resetUserId = 0;
  resetPasswordValue = '';
  movements: Movement[] = [];
  shipments: Shipment[] = [];
  selectedWarehouseId = 0;
  selectedStockId = 0;
  selectedOrderId = 0;
  productForm = { productName: '', sku: '', price: 0, currency: 'EUR' };
  warehouseForm = { code: '', name: '' };
  receiptForm = { productId: 0, quantity: 1 };
  adjustmentForm = { quantityChange: 0, reason: '' };
  orderLines: { productId: number; quantity: number }[] = [{ productId: 0, quantity: 1 }];
  allocations: { orderItemId: number; inventoryId: number; quantity: number }[] = [];
  shipmentForm = { warehouseId: 0 };
  shipmentQuantities: Record<number, number> = {};
  userForm = { username: '', password: '', role: 'SALES' as Role };
  passwordForm = { currentPassword: '', newPassword: '' };

  get canCatalog(): boolean { return this.me?.role === 'ADMIN'; }
  get canStock(): boolean { return this.me?.role === 'ADMIN' || this.me?.role === 'WAREHOUSE_MANAGER'; }
  get canSales(): boolean { return this.me?.role === 'ADMIN' || this.me?.role === 'SALES'; }
  get selectedResetUsername(): string { return this.users.find(u => u.id === this.resetUserId)?.username || ''; }
  get auditPages(): number { return Math.max(1, Math.ceil(this.audit.total / this.audit.size)); }
  get selectedOrder(): Order | undefined { return this.orders.find(o => o.id === this.selectedOrderId); }
  get totalAvailable(): number { return this.stocks.reduce((sum, s) => sum + s.available, 0); }
  get activeOrders(): number { return this.orders.filter(o => o.status === 'DRAFT' || o.status === 'CONFIRMED').length; }
  get allocationsValid(): boolean {
    const order = this.selectedOrder;
    if (!order) return false;
    const complete = order.items.every(line => {
      const rows = this.allocationsFor(line.id);
      return rows.length > 0 && rows.every(a => a.inventoryId > 0 && a.quantity > 0) &&
        rows.reduce((sum, a) => sum + Number(a.quantity), 0) === line.quantity &&
        new Set(rows.map(a => a.inventoryId)).size === rows.length;
    });
    if (!complete) return false;
    const totals = new Map<number, number>();
    for (const allocation of this.allocations) {
      const stock = this.allStocks.find(s => s.id === Number(allocation.inventoryId));
      const line = order.items.find(i => i.id === allocation.orderItemId);
      if (!stock || !line || stock.productId !== line.productId) return false;
      totals.set(stock.id, (totals.get(stock.id) || 0) + Number(allocation.quantity));
    }
    return [...totals].every(([id, quantity]) => quantity <= this.allStocks.find(s => s.id === id)!.available);
  }

  async ngOnInit(): Promise<void> {
    if (!this.token) return;
    try { await this.refresh(); }
    catch (e) {
      if ((e as HttpErrorResponse).status === 401) this.logout();
      else this.error = 'No se pudieron cargar los datos. Comprueba la conexión y actualiza.';
    } finally { this.changeDetector.markForCheck(); }
  }

  private async get<T>(url: string): Promise<T> { return firstValueFrom(this.http.get<T>(url)); }
  private async post<T>(url: string, body: unknown = {}): Promise<T> { return firstValueFrom(this.http.post<T>(url, body)); }
  private get companyUrl(): string { return `/api/companies/${this.me!.companyId}`; }
  private upsertStock(stock: Stock): void {
    this.allStocks = [...this.allStocks.filter(s => s.id !== stock.id), stock];
    this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
  }
  private replaceOrder(order: Order): void {
    this.orders = this.orders.map(o => o.id === order.id ? order : o);
  }

  async login(): Promise<void> {
    await this.act('Sesión iniciada', async () => {
      const token = await this.post<Token>('/api/auth/login', {
        ...this.loginForm, username: this.loginForm.username.trim(),
      });
      this.token = token.accessToken;
      sessionStorage.setItem('meos_token', this.token);
      this.loginForm.password = '';
      await this.refresh();
    });
  }

  logout(): void {
    sessionStorage.removeItem('meos_token');
    this.token = '';
    this.me = null;
    this.products = []; this.warehouses = []; this.stocks = []; this.allStocks = []; this.orders = []; this.users = [];
    this.selectedOrderId = 0; this.selectedStockId = 0; this.selectedWarehouseId = 0;
    this.allocations = [];
    this.issuedCredentials = null;
    this.resetUserId = 0; this.resetPasswordValue = '';
    this.section = 'overview';
    this.changeDetector.markForCheck();
  }

  async refresh(): Promise<void> {
    try {
      this.me = await this.get<User>('/api/auth/me');
      const [products, warehouses, orders, users] = await Promise.all([
        this.get<Product[]>(`${this.companyUrl}/products`),
        this.get<Warehouse[]>(`${this.companyUrl}/warehouses`),
        this.get<Order[]>(`${this.companyUrl}/orders`),
        this.canCatalog ? this.get<User[]>('/api/users') : Promise.resolve([]),
      ]);
      this.products = products; this.warehouses = warehouses; this.orders = orders; this.users = users;
      if (!warehouses.some(w => w.id === this.selectedWarehouseId)) this.selectedWarehouseId = warehouses[0]?.id || 0;
      if (!warehouses.some(w => w.id === this.shipmentForm.warehouseId)) this.shipmentForm.warehouseId = warehouses[0]?.id || 0;
      this.allStocks = (await Promise.all(warehouses.map(w =>
        this.get<Stock[]>(`/api/warehouses/${w.id}/inventory`)))).flat();
      this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
      if (this.selectedOrderId) await this.selectOrder(this.selectedOrderId);
      if (this.section === 'audit' && this.canCatalog) await this.loadAudit();
    } finally { this.changeDetector.markForCheck(); }
  }

  private async act(message: string, action: () => Promise<void>): Promise<void> {
    this.busy = true; this.error = ''; this.notice = '';
    try { await action(); this.notice = message; }
    catch (e) {
      const response = e as HttpErrorResponse;
      const body = response.error;
      this.error = typeof body === 'string' ? body : body?.error || body?.message || body?.detail || response.message || 'Error inesperado';
      if (response.status === 401 && this.token) this.logout();
    } finally {
      this.busy = false;
      this.changeDetector.markForCheck();
    }
  }

  async changeSection(value: Section): Promise<void> {
    if (value !== 'users') this.issuedCredentials = null;
    this.section = value; this.error = ''; this.notice = '';
    if (value === 'audit' && this.canCatalog) {
      try { await this.loadAudit(); }
      catch { this.error = 'No se pudo cargar la auditoría.'; }
    }
    this.changeDetector.markForCheck();
  }

  async loadAudit(page = 0): Promise<void> {
    try { this.audit = await this.get<AuditPage>(`/api/audit-events?page=${page}&size=50`); }
    finally { this.changeDetector.markForCheck(); }
  }

  async createProduct(): Promise<void> {
    await this.act('Producto creado', async () => {
      const created = await this.post<Product>(`${this.companyUrl}/products`,
        { ...this.productForm, currency: this.productForm.currency.toUpperCase() });
      this.products = [...this.products, created];
      this.productForm = { productName: '', sku: '', price: 0, currency: 'EUR' };
    });
  }
  async createWarehouse(): Promise<void> {
    await this.act('Almacén creado', async () => {
      const created = await this.post<Warehouse>(`${this.companyUrl}/warehouses`, this.warehouseForm);
      this.warehouses = [...this.warehouses, created];
      if (!this.selectedWarehouseId) this.selectedWarehouseId = created.id;
      if (!this.shipmentForm.warehouseId) this.shipmentForm.warehouseId = created.id;
      this.warehouseForm = { code: '', name: '' };
    });
  }
  async loadStocks(): Promise<void> {
    try {
      if (!this.selectedWarehouseId) { this.stocks = []; return; }
      const stocks = await this.get<Stock[]>(`/api/warehouses/${this.selectedWarehouseId}/inventory`);
      this.allStocks = [...this.allStocks.filter(s => s.warehouseId !== this.selectedWarehouseId), ...stocks];
      this.stocks = stocks;
    } finally { this.changeDetector.markForCheck(); }
  }
  async receiveStock(): Promise<void> {
    await this.act('Entrada de stock registrada', async () => {
      const stock = await this.post<Stock>(`/api/warehouses/${this.selectedWarehouseId}/inventory`, this.receiptForm);
      this.upsertStock(stock);
      this.receiptForm = { productId: 0, quantity: 1 };
    });
  }
  async selectStock(id: number): Promise<void> {
    this.selectedStockId = id;
    try { this.movements = await this.get<Movement[]>(`/api/inventory/${id}/movements`); }
    finally { this.changeDetector.markForCheck(); }
  }
  async adjustStock(): Promise<void> {
    await this.act('Ajuste registrado', async () => {
      const stock = await this.post<Stock>(`/api/inventory/${this.selectedStockId}/adjustments`, this.adjustmentForm);
      this.upsertStock(stock);
      this.adjustmentForm = { quantityChange: 0, reason: '' };
      await this.selectStock(this.selectedStockId);
    });
  }
  addOrderLine(): void { this.orderLines.push({ productId: 0, quantity: 1 }); }
  removeOrderLine(index: number): void { this.orderLines.splice(index, 1); }
  allocationsFor(orderItemId: number) { return this.allocations.filter(a => a.orderItemId === orderItemId); }
  addAllocation(orderItemId: number): void { this.allocations.push({ orderItemId, inventoryId: 0, quantity: 1 }); }
  removeAllocation(item: { orderItemId: number; inventoryId: number; quantity: number }): void {
    this.allocations = this.allocations.filter(a => a !== item);
  }
  async createOrder(): Promise<void> {
    await this.act('Pedido creado', async () => {
      const order = await this.post<Order>(`${this.companyUrl}/orders`, { items: this.orderLines });
      this.orders = [...this.orders, order];
      this.orderLines = [{ productId: 0, quantity: 1 }];
      this.selectedOrderId = order.id;
      this.allocations = order.items.map(line =>
        ({ orderItemId: line.id, inventoryId: 0, quantity: line.quantity }));
      this.shipments = [];
      for (const line of order.items) this.shipmentQuantities[line.id] = line.quantity;
    });
  }
  async selectOrder(id: number): Promise<void> {
    if (this.selectedOrderId !== id) this.allocations = [];
    this.selectedOrderId = id;
    try {
      const [order, shipments] = await Promise.all([
        this.get<Order>(`/api/orders/${id}`), this.get<Shipment[]>(`/api/orders/${id}/shipments`),
      ]);
      this.replaceOrder(order);
      this.shipments = shipments;
      if (order.status === 'DRAFT' && !this.allocations.length) {
        this.allocations = order.items.map(line => ({ orderItemId: line.id, inventoryId: 0, quantity: line.quantity }));
      }
      for (const line of order.items) {
        if (!(line.id in this.shipmentQuantities)) this.shipmentQuantities[line.id] = line.quantity;
      }
    } finally { this.changeDetector.markForCheck(); }
  }
  async confirmOrder(): Promise<void> {
    await this.act('Pedido confirmado y stock reservado', async () => {
      const allocated = this.allocations.map(a => ({ ...a }));
      const order = await this.post<Order>(`/api/orders/${this.selectedOrderId}/confirm`, {
        allocations: this.allocations.map(a => ({ orderItemId: a.orderItemId, inventoryId: Number(a.inventoryId), quantity: Number(a.quantity) })),
      });
      this.replaceOrder(order);
      const reserved = new Map<number, number>();
      for (const item of allocated) reserved.set(Number(item.inventoryId),
        (reserved.get(Number(item.inventoryId)) || 0) + Number(item.quantity));
      this.allStocks = this.allStocks.map(s => ({ ...s, available: s.available - (reserved.get(s.id) || 0) }));
      this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
      this.allocations = [];
    });
  }
  async cancelOrder(): Promise<void> {
    await this.act('Pedido cancelado', async () => {
      const wasConfirmed = this.selectedOrder?.status === 'CONFIRMED';
      const order = await this.post<Order>(`/api/orders/${this.selectedOrderId}/cancel`);
      this.replaceOrder(order);
      if (wasConfirmed) await this.reloadAllStocks();
    });
  }
  async shipOrder(): Promise<void> {
    await this.act('Envío registrado', async () => {
      const items = this.selectedOrder!.items
        .filter(line => Number(this.shipmentQuantities[line.id]) > 0)
        .map(line => ({ orderItemId: line.id, quantity: Number(this.shipmentQuantities[line.id]) }));
      const shipment = await this.post<Shipment>(`/api/orders/${this.selectedOrderId}/shipments`,
        { warehouseId: Number(this.shipmentForm.warehouseId), items });
      this.shipments = [...this.shipments, shipment];
      const [order, stocks] = await Promise.all([
        this.get<Order>(`/api/orders/${this.selectedOrderId}`),
        this.get<Stock[]>(`/api/warehouses/${this.shipmentForm.warehouseId}/inventory`),
      ]);
      this.replaceOrder(order);
      this.allStocks = [...this.allStocks.filter(s => s.warehouseId !== this.shipmentForm.warehouseId), ...stocks];
      this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
    });
  }
  async createUser(): Promise<void> {
    await this.act('Usuario creado', async () => {
      const password = this.userForm.password;
      const user = await this.post<User>('/api/users', this.userForm);
      this.users = [...this.users, user];
      this.issuedCredentials = { companyId: user.companyId, username: user.username,
        password, role: user.role };
      this.userForm = { username: '', password: '', role: 'SALES' };
    });
  }
  async deactivateUser(id: number): Promise<void> {
    if (!confirm('¿Desactivar este usuario?')) return;
    await this.act('Usuario desactivado', async () => {
      const user = await this.post<User>(`/api/users/${id}/deactivate`);
      this.users = this.users.map(u => u.id === id ? user : u);
    });
  }
  async changePassword(): Promise<void> {
    await this.act('Contraseña actualizada. Inicia sesión de nuevo.', async () => {
      await this.post('/api/users/change-password', this.passwordForm);
      this.passwordForm = { currentPassword: '', newPassword: '' };
      this.logout();
    });
  }
  async resetPassword(): Promise<void> {
    await this.act('Contraseña restablecida. Comparte estas credenciales por un canal seguro.', async () => {
      const password = this.resetPasswordValue;
      const user = await this.post<User>(`/api/users/${this.resetUserId}/reset-password`,
        { newPassword: password });
      this.users = this.users.map(u => u.id === user.id ? user : u);
      this.issuedCredentials = { companyId: user.companyId, username: user.username,
        password, role: user.role };
      this.resetUserId = 0; this.resetPasswordValue = '';
    });
  }
  private async reloadAllStocks(): Promise<void> {
    this.allStocks = (await Promise.all(this.warehouses.map(w =>
      this.get<Stock[]>(`/api/warehouses/${w.id}/inventory`)))).flat();
    this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
  }
  productName(id: number): string { return this.products.find(p => p.id === id)?.productName || `Producto #${id}`; }
  warehouseName(id: number): string { return this.warehouses.find(w => w.id === id)?.name || `Almacén #${id}`; }
}
