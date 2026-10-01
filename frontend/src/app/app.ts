import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiClient, apiErrorMessage } from './core/api-client.service';
import { SessionService } from './core/session.service';
import { Allocation, AuditPage, Customer, DeliveryAddress, Movement, Order, Page, Product, Role, Section, Shipment, Stock, Token, User, Warehouse } from './core/models';
import { allocationsAreValid, passwordIsValid, positiveInteger } from './core/validators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly api = inject(ApiClient);
  private readonly session = inject(SessionService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private sessionEpoch = 0;
  private customerRequest = 0;
  private productRequest = 0;
  private orderRequest = 0;
  private stockRequest = 0;
  private refreshRequest = 0;
  private actionRequest = 0;
  get sessionEndsAt(): number { return this.session.expiresAt(); }
  me: User | null = null;
  section: Section = 'overview';
  busy = false;
  loadingData = false;
  notice = '';
  error = '';
  loginForm = { companyId: 1, username: 'admin', password: '' };
  registerForm = { companyName: '', username: '', password: '', registrationCode: '' };
  registering = false;
  customers: Customer[] = [];
  private customerCache: Record<number, Customer> = {};
  private productCache: Record<number, Product> = {};
  customerPage: Page<Customer> = { items: [], total: 0, page: 0, size: 20 };
  productPage: Page<Product> = { items: [], total: 0, page: 0, size: 20 };
  orderPage: Page<Order> = { items: [], total: 0, page: 0, size: 20 };
  customerSearch = '';
  productSearch = '';
  orderStatusFilter = 'ALL';
  orderCustomerSearch = '';
  customerForm = { name: '', email: '', phone: '' };
  editingCustomerId = 0;
  editingProductId = 0;
  editingWarehouseId = 0;
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
  selectedOrderDetail: Order | null = null;
  productForm = { productName: '', sku: '', price: 0, currency: 'EUR' };
  warehouseForm = { code: '', name: '' };
  receiptForm = { productId: 0, quantity: 1 };
  adjustmentForm = { quantityChange: 0, reason: '' };
  stocktakeForm = { countedQuantity: 0, reason: '' };
  transferForm = { sourceWarehouseId: 0, destinationWarehouseId: 0, productId: 0, quantity: 1 };
  orderLines: { productId: number; quantity: number }[] = [{ productId: 0, quantity: 1 }];
  orderCustomerId = 0;
  addressForm: DeliveryAddress = { recipient: '', street: '', city: '', postalCode: '', country: 'España' };
  allocations: Allocation[] = [];
  shipmentForm = { warehouseId: 0, carrier: '', trackingNumber: '' };
  shipmentQuantities: Record<number, number> = {};
  returnForm = { shipmentItemId: 0, quantity: 1, reason: '' };
  userForm = { username: '', password: '', role: 'SALES' as Role };
  passwordForm = { currentPassword: '', newPassword: '' };

  constructor() {
    this.session.expired$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.logout();
      this.error = 'La sesión ha caducado. Inicia sesión de nuevo.';
      this.changeDetector.markForCheck();
    });
  }

  get canCatalog(): boolean { return this.me?.role === 'ADMIN'; }
  get canStock(): boolean { return this.me?.role === 'ADMIN' || this.me?.role === 'WAREHOUSE_MANAGER'; }
  get canSales(): boolean { return this.me?.role === 'ADMIN' || this.me?.role === 'SALES'; }
  get selectedResetUsername(): string { return this.users.find(u => u.id === this.resetUserId)?.username || ''; }
  get auditPages(): number { return Math.max(1, Math.ceil(this.audit.total / this.audit.size)); }
  get selectedOrder(): Order | undefined {
    return this.selectedOrderDetail?.id === this.selectedOrderId ? this.selectedOrderDetail :
      this.orders.find(o => o.id === this.selectedOrderId);
  }
  get totalAvailable(): number { return this.stocks.reduce((sum, s) => sum + s.available, 0); }
  get activeOrders(): number { return this.orders.filter(o => o.status === 'DRAFT' || o.status === 'CONFIRMED').length; }
  get activeCustomers(): Customer[] {
    const all = new Map<number, Customer>(Object.values(this.customerCache).map(c => [c.id, c]));
    for (const customer of this.customers) all.set(customer.id, customer);
    return [...all.values()].filter(c => c.active);
  }
  get activeProducts(): Product[] {
    const all = new Map<number, Product>(Object.values(this.productCache).map(p => [p.id, p]));
    for (const product of this.products) all.set(product.id, product);
    return [...all.values()].filter(p => p.active);
  }
  get activeWarehouses(): Warehouse[] { return this.warehouses.filter(w => w.active); }
  get remainingByLine(): Record<number, number> {
    const remaining: Record<number, number> = {};
    for (const line of this.selectedOrder?.items || []) {
      const shipped = this.shipments.flatMap(s => s.items || []).filter(i => i.orderItemId === line.id)
        .reduce((total, i) => total + i.quantity, 0);
      remaining[line.id] = Math.max(0, line.quantity - shipped);
    }
    return remaining;
  }
  get allocationsValid(): boolean {
    return allocationsAreValid(this.selectedOrder, this.allocations, this.allStocks);
  }

  private get<T>(url: string): Promise<T> { return this.api.get<T>(url); }
  private post<T>(url: string, body: unknown = {}): Promise<T> { return this.api.post<T>(url, body); }
  private put<T>(url: string, body: unknown): Promise<T> { return this.api.put<T>(url, body); }
  private validate(condition: boolean, message: string): boolean {
    if (condition) return true;
    this.error = message;
    this.changeDetector.markForCheck();
    return false;
  }
  private get companyUrl(): string { return `/api/companies/${this.me!.companyId}`; }
  private orderPageUrl(page: number): string {
    return `${this.companyUrl}/orders/search?page=${page}&size=20&status=${encodeURIComponent(this.orderStatusFilter)}` +
      `&customer=${encodeURIComponent(this.orderCustomerSearch.trim())}`;
  }
  private upsertStock(stock: Stock): void {
    this.allStocks = [...this.allStocks.filter(s => s.id !== stock.id), stock];
    this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
  }
  private replaceOrder(order: Order): void {
    if (this.selectedOrderId === order.id) this.selectedOrderDetail = order;
    this.orders = this.orders.map(o => o.id === order.id ? order : o);
  }
  async login(): Promise<void> {
    if (!this.validate(positiveInteger(this.loginForm.companyId) && !!this.loginForm.username.trim() && !!this.loginForm.password,
      'Introduce el ID de empresa, usuario y contraseña.')) return;
    const epoch = this.sessionEpoch;
    await this.act('', async () => {
      const token = await this.post<Token>('/api/auth/login', {
        ...this.loginForm, username: this.loginForm.username.trim(),
      });
      if (epoch !== this.sessionEpoch) return;
      this.session.start(token.accessToken, token.expiresIn);
      this.loginForm.password = '';
      try {
        const me = await this.get<User>('/api/auth/me');
        if (epoch !== this.sessionEpoch) return;
        this.me = me;
      }
      catch (error) { this.session.clear(); throw error; }
      this.notice = 'Sesión iniciada';
      this.changeDetector.markForCheck();
      void this.refresh();
    });
    this.loginForm.password = '';
  }

  async register(): Promise<void> {
    if (!this.validate(!!this.registerForm.companyName.trim() && !!this.registerForm.username.trim() &&
      !!this.registerForm.registrationCode && passwordIsValid(this.registerForm.password),
      'Completa todos los campos. La contraseña debe tener al menos 14 caracteres y 72 bytes UTF-8 como máximo.')) return;
    await this.act('Empresa creada. Ya puedes iniciar sesión.', async () => {
      const result = await this.post<{ companyId: number; username: string }>('/api/auth/register', {
        ...this.registerForm, companyName: this.registerForm.companyName.trim(), username: this.registerForm.username.trim(),
      });
      this.loginForm = { companyId: result.companyId, username: result.username, password: '' };
      this.registerForm = { companyName: '', username: '', password: '', registrationCode: '' };
      this.registering = false;
    });
    this.registerForm.password = '';
    this.registerForm.registrationCode = '';
  }

  logout(): void {
    this.sessionEpoch++;
    this.customerRequest++; this.productRequest++; this.orderRequest++; this.stockRequest++;
    this.refreshRequest++; this.loadingData = false;
    this.actionRequest++; this.busy = false;
    this.session.clear();
    this.me = null;
    this.products = []; this.customers = []; this.warehouses = []; this.stocks = []; this.allStocks = []; this.orders = []; this.users = [];
    this.productCache = {}; this.customerCache = {};
    this.productPage = { items: [], total: 0, page: 0, size: 20 };
    this.customerPage = { items: [], total: 0, page: 0, size: 20 };
    this.orderPage = { items: [], total: 0, page: 0, size: 20 };
    this.audit = { events: [], total: 0, page: 0, size: 50 };
    this.movements = []; this.shipments = [];
    this.selectedOrderId = 0; this.selectedOrderDetail = null; this.selectedStockId = 0; this.selectedWarehouseId = 0;
    this.allocations = [];
    this.issuedCredentials = null;
    this.resetUserId = 0; this.resetPasswordValue = '';
    this.loginForm.password = '';
    this.passwordForm = { currentPassword: '', newPassword: '' };
    this.userForm.password = '';
    this.section = 'overview';
    this.changeDetector.markForCheck();
  }

  async refresh(): Promise<void> {
    if (!this.me || this.loadingData) return;
    const epoch = this.sessionEpoch;
    const request = ++this.refreshRequest;
    this.loadingData = true;
    this.changeDetector.markForCheck();
    try {
      const warehousePromise = this.get<Warehouse[]>(`${this.companyUrl}/warehouses`).then(warehouses => {
        if (epoch !== this.sessionEpoch) return;
        this.warehouses = warehouses;
        if (!warehouses.some(w => w.id === this.selectedWarehouseId)) this.selectedWarehouseId = warehouses[0]?.id || 0;
        if (!warehouses.some(w => w.id === this.shipmentForm.warehouseId)) this.shipmentForm.warehouseId = warehouses[0]?.id || 0;
        this.changeDetector.markForCheck();
      });
      await Promise.all([
        this.loadProductPage(this.productPage.page), this.loadCustomerPage(this.customerPage.page),
        this.loadOrderPage(this.orderPage.page), warehousePromise,
      ]);
      if (epoch !== this.sessionEpoch) return;
      await this.loadStocks();
      if (this.selectedOrderId) await this.selectOrder(this.selectedOrderId);
      if (this.section === 'audit' && this.canCatalog) await this.loadAudit();
      if (this.section === 'users' && this.canCatalog) await this.loadUsers();
    } catch (error) {
      if (epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally {
      if (request === this.refreshRequest) this.loadingData = false;
      this.changeDetector.markForCheck();
    }
  }

  private async loadUsers(): Promise<void> {
    const epoch = this.sessionEpoch;
    try {
      const users = await this.get<User[]>('/api/users');
      if (epoch === this.sessionEpoch) this.users = users;
    } catch (error) {
      if (epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally { this.changeDetector.markForCheck(); }
  }

  async loadCustomerPage(page = 0): Promise<void> {
    if (!this.me) return;
    const request = ++this.customerRequest;
    const epoch = this.sessionEpoch;
    try {
      const result = await this.get<Page<Customer>>(`${this.companyUrl}/customers?page=${page}&size=20&q=${encodeURIComponent(this.customerSearch.trim())}`);
      if (request !== this.customerRequest || epoch !== this.sessionEpoch) return;
      this.customerPage = result;
      this.customers = result.items;
      for (const customer of result.items) this.customerCache[customer.id] = customer;
    } catch (error) {
      if (request === this.customerRequest && epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally { this.changeDetector.markForCheck(); }
  }
  async loadProductPage(page = 0): Promise<void> {
    if (!this.me) return;
    const request = ++this.productRequest;
    const epoch = this.sessionEpoch;
    try {
      const result = await this.get<Page<Product>>(`${this.companyUrl}/products/search?page=${page}&size=20&q=${encodeURIComponent(this.productSearch.trim())}`);
      if (request !== this.productRequest || epoch !== this.sessionEpoch) return;
      this.productPage = result;
      this.products = result.items;
      for (const product of result.items) this.productCache[product.id] = product;
    } catch (error) {
      if (request === this.productRequest && epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally { this.changeDetector.markForCheck(); }
  }
  async loadOrderPage(page = 0): Promise<void> {
    if (!this.me) return;
    const request = ++this.orderRequest;
    const epoch = this.sessionEpoch;
    try {
      const result = await this.get<Page<Order>>(this.orderPageUrl(page));
      if (request !== this.orderRequest || epoch !== this.sessionEpoch) return;
      this.orderPage = result;
      this.orders = result.items;
    } catch (error) {
      if (request === this.orderRequest && epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally { this.changeDetector.markForCheck(); }
  }

  private async act(message: string, action: () => Promise<void>): Promise<void> {
    if (this.busy) return;
    const epoch = this.sessionEpoch;
    const request = ++this.actionRequest;
    this.busy = true; this.error = ''; this.notice = '';
    try { await action(); if (message && epoch === this.sessionEpoch) this.notice = message; }
    catch (error) { if (epoch === this.sessionEpoch) this.error = apiErrorMessage(error); }
    finally {
      if (request === this.actionRequest) this.busy = false;
      this.changeDetector.markForCheck();
    }
  }

  async changeSection(value: Section): Promise<void> {
    if (value !== 'users') this.issuedCredentials = null;
    this.section = value; this.error = ''; this.notice = '';
    if (value === 'audit' && this.canCatalog) await this.loadAudit();
    if (value === 'users' && this.canCatalog) await this.loadUsers();
    this.changeDetector.markForCheck();
  }

  async loadAudit(page = 0): Promise<void> {
    const epoch = this.sessionEpoch;
    try {
      const audit = await this.get<AuditPage>(`/api/audit-events?page=${page}&size=50`);
      if (epoch === this.sessionEpoch) this.audit = audit;
    } catch (error) {
      if (epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    }
    finally { this.changeDetector.markForCheck(); }
  }

  async createProduct(): Promise<void> {
    if (!this.validate(!!this.productForm.productName.trim() && !!this.productForm.sku.trim() &&
      Number.isFinite(Number(this.productForm.price)) && Number(this.productForm.price) >= 0 &&
      /^[A-Za-z]{3}$/.test(this.productForm.currency), 'Revisa nombre, SKU, precio y moneda del producto.')) return;
    await this.act('Producto creado', async () => {
      const created = await this.post<Product>(`${this.companyUrl}/products`,
        { ...this.productForm, currency: this.productForm.currency.toUpperCase() });
      this.products = [...this.products, created];
      this.productCache[created.id] = created;
      await this.loadProductPage();
      this.productForm = { productName: '', sku: '', price: 0, currency: 'EUR' };
    });
  }
  editProduct(product: Product): void {
    this.editingProductId = product.id;
    this.productForm = { productName: product.productName, sku: product.sku,
      price: product.price, currency: product.currency };
  }
  async saveProduct(): Promise<void> {
    if (!this.validate(!!this.productForm.productName.trim() && !!this.productForm.sku.trim() &&
      Number.isFinite(Number(this.productForm.price)) && Number(this.productForm.price) >= 0 &&
      /^[A-Za-z]{3}$/.test(this.productForm.currency), 'Revisa nombre, SKU, precio y moneda del producto.')) return;
    await this.act('Producto actualizado', async () => {
      await this.put<Product>(`${this.companyUrl}/products/${this.editingProductId}`,
        { ...this.productForm, currency: this.productForm.currency.toUpperCase() });
      this.editingProductId = 0;
      this.productForm = { productName: '', sku: '', price: 0, currency: 'EUR' };
      await this.loadProductPage(this.productPage.page);
    });
  }
  async deactivateProduct(id: number): Promise<void> {
    if (!confirm('¿Desactivar este producto? Los pedidos anteriores seguirán visibles.')) return;
    await this.act('Producto desactivado', async () => {
      await this.post(`${this.companyUrl}/products/${id}/deactivate`);
      delete this.productCache[id];
      await this.loadProductPage(this.productPage.page);
    });
  }
  async createCustomer(): Promise<void> {
    if (!this.validate(!!this.customerForm.name.trim(), 'Introduce el nombre del cliente.')) return;
    await this.act('Cliente creado', async () => {
      const created = await this.post<Customer>(`${this.companyUrl}/customers`, this.customerForm);
      this.customerCache[created.id] = created;
      this.customerForm = { name: '', email: '', phone: '' };
      await this.loadCustomerPage();
    });
  }
  editCustomer(customer: Customer): void {
    this.editingCustomerId = customer.id;
    this.customerForm = { name: customer.name, email: customer.email || '', phone: customer.phone || '' };
  }
  async saveCustomer(): Promise<void> {
    if (!this.validate(!!this.customerForm.name.trim(), 'Introduce el nombre del cliente.')) return;
    await this.act('Cliente actualizado', async () => {
      await this.put<Customer>(`${this.companyUrl}/customers/${this.editingCustomerId}`, this.customerForm);
      this.editingCustomerId = 0;
      this.customerForm = { name: '', email: '', phone: '' };
      await this.loadCustomerPage(this.customerPage.page);
    });
  }
  async deactivateCustomer(id: number): Promise<void> {
    if (!confirm('¿Desactivar este cliente?')) return;
    await this.act('Cliente desactivado', async () => {
      await this.post(`${this.companyUrl}/customers/${id}/deactivate`);
      delete this.customerCache[id];
      await this.loadCustomerPage(this.customerPage.page);
    });
  }
  async createWarehouse(): Promise<void> {
    if (!this.validate(!!this.warehouseForm.code.trim() && !!this.warehouseForm.name.trim(),
      'Introduce el código y el nombre del almacén.')) return;
    await this.act('Almacén creado', async () => {
      const created = await this.post<Warehouse>(`${this.companyUrl}/warehouses`, this.warehouseForm);
      this.warehouses = [...this.warehouses, created];
      if (!this.selectedWarehouseId) this.selectedWarehouseId = created.id;
      if (!this.shipmentForm.warehouseId) this.shipmentForm.warehouseId = created.id;
      this.warehouseForm = { code: '', name: '' };
    });
  }
  editWarehouse(warehouse: Warehouse): void {
    this.editingWarehouseId = warehouse.id;
    this.warehouseForm = { code: warehouse.code, name: warehouse.name };
  }
  async saveWarehouse(): Promise<void> {
    if (!this.validate(!!this.warehouseForm.code.trim() && !!this.warehouseForm.name.trim(),
      'Introduce el código y el nombre del almacén.')) return;
    await this.act('Almacén actualizado', async () => {
      const warehouse = await this.put<Warehouse>(`${this.companyUrl}/warehouses/${this.editingWarehouseId}`, this.warehouseForm);
      this.warehouses = this.warehouses.map(w => w.id === warehouse.id ? warehouse : w);
      this.editingWarehouseId = 0; this.warehouseForm = { code: '', name: '' };
    });
  }
  async deactivateWarehouse(id: number): Promise<void> {
    if (!confirm('¿Desactivar este almacén? Debe estar vacío.')) return;
    await this.act('Almacén desactivado', async () => {
      const warehouse = await this.post<Warehouse>(`${this.companyUrl}/warehouses/${id}/deactivate`);
      this.warehouses = this.warehouses.map(w => w.id === warehouse.id ? warehouse : w);
    });
  }
  async loadStocks(): Promise<void> {
    const request = ++this.stockRequest;
    const epoch = this.sessionEpoch;
    const warehouseId = this.selectedWarehouseId;
    try {
      if (!warehouseId) { this.stocks = []; return; }
      this.stocks = [];
      const stocks = await this.get<Stock[]>(`/api/warehouses/${warehouseId}/inventory`);
      if (request !== this.stockRequest || epoch !== this.sessionEpoch || warehouseId !== this.selectedWarehouseId) return;
      this.allStocks = [...this.allStocks.filter(s => s.warehouseId !== warehouseId), ...stocks];
      this.stocks = stocks;
    } catch (error) {
      if (request === this.stockRequest && epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally { this.changeDetector.markForCheck(); }
  }
  async receiveStock(): Promise<void> {
    if (!this.validate(positiveInteger(this.selectedWarehouseId) && positiveInteger(this.receiptForm.productId) &&
      positiveInteger(this.receiptForm.quantity), 'Selecciona un almacén, producto y una cantidad válida.')) return;
    await this.act('Entrada de stock registrada', async () => {
      const stock = await this.post<Stock>(`/api/warehouses/${this.selectedWarehouseId}/inventory`, this.receiptForm);
      this.upsertStock(stock);
      this.receiptForm = { productId: 0, quantity: 1 };
    });
  }
  async selectStock(id: number): Promise<void> {
    this.selectedStockId = id;
    const epoch = this.sessionEpoch;
    try {
      const movements = await this.get<Movement[]>(`/api/inventory/${id}/movements`);
      if (epoch === this.sessionEpoch && id === this.selectedStockId) this.movements = movements;
    } catch (error) {
      if (epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    }
    finally { this.changeDetector.markForCheck(); }
  }
  async adjustStock(): Promise<void> {
    if (!this.validate(positiveInteger(this.selectedStockId) && Number.isSafeInteger(Number(this.adjustmentForm.quantityChange)) &&
      Number(this.adjustmentForm.quantityChange) !== 0 && !!this.adjustmentForm.reason.trim(),
      'Introduce un ajuste distinto de cero y un motivo.')) return;
    await this.act('Ajuste registrado', async () => {
      const stock = await this.post<Stock>(`/api/inventory/${this.selectedStockId}/adjustments`, this.adjustmentForm);
      this.upsertStock(stock);
      this.adjustmentForm = { quantityChange: 0, reason: '' };
      await this.selectStock(this.selectedStockId);
    });
  }
  async stocktake(): Promise<void> {
    if (!this.validate(positiveInteger(this.selectedStockId) && Number.isSafeInteger(Number(this.stocktakeForm.countedQuantity)) &&
      Number(this.stocktakeForm.countedQuantity) >= 0 && !!this.stocktakeForm.reason.trim(),
      'Introduce una cantidad válida y un motivo para el recuento.')) return;
    await this.act('Recuento registrado', async () => {
      const stock = await this.post<Stock>(`/api/inventory/${this.selectedStockId}/stocktake`, this.stocktakeForm);
      this.upsertStock(stock);
      this.stocktakeForm = { countedQuantity: 0, reason: '' };
      await this.selectStock(this.selectedStockId);
    });
  }
  async transferStock(): Promise<void> {
    if (!this.validate(positiveInteger(this.transferForm.sourceWarehouseId) &&
      positiveInteger(this.transferForm.destinationWarehouseId) &&
      this.transferForm.sourceWarehouseId !== this.transferForm.destinationWarehouseId &&
      positiveInteger(this.transferForm.productId) && positiveInteger(this.transferForm.quantity),
      'Selecciona dos almacenes distintos, un producto y una cantidad válida.')) return;
    await this.act('Traslado registrado', async () => {
      await this.post(`${this.companyUrl}/transfers`, this.transferForm);
      await this.loadStocks();
      this.transferForm = { ...this.transferForm, productId: 0, quantity: 1 };
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
    if (!this.validate(positiveInteger(this.orderCustomerId) && this.orderLines.length > 0 &&
      this.orderLines.every(line => positiveInteger(line.productId) && positiveInteger(line.quantity)) &&
      new Set(this.orderLines.map(line => Number(line.productId))).size === this.orderLines.length &&
      Object.values(this.addressForm).every(value => !!value.trim()),
      'Selecciona cliente y productos sin duplicados, cantidades válidas y una dirección completa.')) return;
    await this.act('Pedido creado', async () => {
      const order = await this.post<Order>(`${this.companyUrl}/orders`, {
        customerId: Number(this.orderCustomerId), deliveryAddress: this.addressForm, items: this.orderLines,
      });
      this.orders = [...this.orders, order];
      this.orderPage.total++;
      this.orderLines = [{ productId: 0, quantity: 1 }];
      this.selectedOrderId = order.id;
      this.selectedOrderDetail = order;
      this.allocations = order.items.map(line =>
        ({ orderItemId: line.id, inventoryId: 0, quantity: line.quantity }));
      this.shipments = [];
      for (const line of order.items) this.shipmentQuantities[line.id] = line.quantity;
    });
  }
  async selectOrder(id: number): Promise<void> {
    if (this.selectedOrderId !== id) this.allocations = [];
    this.selectedOrderId = id;
    const epoch = this.sessionEpoch;
    try {
      const [order, shipments] = await Promise.all([
        this.get<Order>(`/api/orders/${id}`), this.get<Shipment[]>(`/api/orders/${id}/shipments`),
      ]);
      if (epoch !== this.sessionEpoch || id !== this.selectedOrderId) return;
      this.replaceOrder(order);
      this.shipments = shipments;
      if (order.status === 'DRAFT') await this.reloadAllStocks();
      if (order.status === 'DRAFT' && !this.allocations.length) {
        this.allocations = order.items.map(line => ({ orderItemId: line.id, inventoryId: 0, quantity: line.quantity }));
      }
      for (const line of order.items) {
        this.shipmentQuantities[line.id] = this.remainingByLine[line.id];
      }
    } catch (error) {
      if (epoch === this.sessionEpoch) this.error = apiErrorMessage(error);
    } finally { this.changeDetector.markForCheck(); }
  }
  async confirmOrder(): Promise<void> {
    if (!this.validate(this.allocationsValid, 'La reserva debe cubrir todas las líneas sin superar el stock disponible.')) return;
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
    const order = this.selectedOrder;
    const remaining = this.remainingByLine;
    const quantities = order?.items.map(line => Number(this.shipmentQuantities[line.id])) || [];
    if (!this.validate(!!order && positiveInteger(this.shipmentForm.warehouseId) &&
      quantities.some(quantity => quantity > 0) && quantities.every((quantity, index) =>
        Number.isSafeInteger(quantity) && quantity >= 0 && quantity <= remaining[order.items[index].id]),
      'Selecciona un almacén y cantidades entre cero y las unidades pendientes.')) return;
    await this.act('Envío registrado', async () => {
      const items = this.selectedOrder!.items
        .filter(line => Number(this.shipmentQuantities[line.id]) > 0)
        .map(line => ({ orderItemId: line.id, quantity: Number(this.shipmentQuantities[line.id]) }));
      const shipment = await this.post<Shipment>(`/api/orders/${this.selectedOrderId}/shipments`,
        { warehouseId: Number(this.shipmentForm.warehouseId), carrier: this.shipmentForm.carrier,
          trackingNumber: this.shipmentForm.trackingNumber, items });
      this.shipments = [...this.shipments, shipment];
      const [order, stocks] = await Promise.all([
        this.get<Order>(`/api/orders/${this.selectedOrderId}`),
        this.get<Stock[]>(`/api/warehouses/${this.shipmentForm.warehouseId}/inventory`),
      ]);
      this.replaceOrder(order);
      this.allStocks = [...this.allStocks.filter(s => s.warehouseId !== this.shipmentForm.warehouseId), ...stocks];
      this.stocks = this.allStocks.filter(s => s.warehouseId === this.selectedWarehouseId);
      for (const line of order.items) this.shipmentQuantities[line.id] = this.remainingByLine[line.id];
      this.shipmentForm.carrier = ''; this.shipmentForm.trackingNumber = '';
    });
  }
  async returnStock(): Promise<void> {
    const line = this.shipments.flatMap(shipment => shipment.items).find(item => item.id === Number(this.returnForm.shipmentItemId));
    if (!this.validate(!!line && positiveInteger(this.returnForm.quantity) &&
      this.returnForm.quantity <= line.quantity - line.returnedQuantity && !!this.returnForm.reason.trim(),
      'Selecciona una línea enviada, una cantidad pendiente y un motivo.')) return;
    await this.act('Devolución registrada', async () => {
      await this.post(`/api/orders/${this.selectedOrderId}/returns`, this.returnForm);
      this.shipments = await this.get<Shipment[]>(`/api/orders/${this.selectedOrderId}/shipments`);
      await this.reloadAllStocks();
      this.returnForm = { shipmentItemId: 0, quantity: 1, reason: '' };
    });
  }
  async createUser(): Promise<void> {
    if (!this.validate(!!this.userForm.username.trim() && passwordIsValid(this.userForm.password),
      'Introduce un usuario y una contraseña de al menos 14 caracteres y hasta 72 bytes UTF-8.')) return;
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
    if (!this.validate(!!this.passwordForm.currentPassword && passwordIsValid(this.passwordForm.newPassword) &&
      this.passwordForm.currentPassword !== this.passwordForm.newPassword,
      'Introduce la contraseña actual y una nueva de 14 a 72 bytes UTF-8.')) return;
    await this.act('Contraseña actualizada. Inicia sesión de nuevo.', async () => {
      await this.post('/api/users/change-password', this.passwordForm);
      this.passwordForm = { currentPassword: '', newPassword: '' };
      this.logout();
      this.notice = 'Contraseña actualizada. Inicia sesión de nuevo.';
    });
  }
  async resetPassword(): Promise<void> {
    if (!this.validate(positiveInteger(this.resetUserId) && passwordIsValid(this.resetPasswordValue),
      'Selecciona un usuario y una contraseña de al menos 14 caracteres y hasta 72 bytes UTF-8.')) return;
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
  productName(id: number): string { return this.productCache[id]?.productName || this.products.find(p => p.id === id)?.productName || `Producto #${id}`; }
  warehouseName(id: number): string { return this.warehouses.find(w => w.id === id)?.name || `Almacén #${id}`; }
}
