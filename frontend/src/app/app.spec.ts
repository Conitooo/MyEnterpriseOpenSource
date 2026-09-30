import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';

describe('reserva dividida de un pedido', () => {
  let app: App;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    app = TestBed.createComponent(App).componentInstance;
    app.orders = [{ id: 1, customerId: 1, customerName: 'Buyer', deliveryAddress: null, status: 'DRAFT', items: [
      { id: 7, productId: 5, productName: 'Widget', quantity: 5, price: 10, currency: 'EUR' },
    ] }];
    app.selectedOrderId = 1;
    app.allStocks = [
      { id: 11, productId: 5, warehouseId: 1, quantity: 3, available: 3 },
      { id: 12, productId: 5, warehouseId: 2, quantity: 4, available: 4 },
    ];
  });

  it('permite repartir una línea entre dos almacenes', () => {
    app.allocations = [
      { orderItemId: 7, inventoryId: 11, quantity: 2 },
      { orderItemId: 7, inventoryId: 12, quantity: 3 },
    ];
    expect(app.allocationsValid).toBe(true);
  });

  it('rechaza exceso de unidades y duplicados', () => {
    app.allocations = [
      { orderItemId: 7, inventoryId: 11, quantity: 4 },
      { orderItemId: 7, inventoryId: 12, quantity: 1 },
    ];
    expect(app.allocationsValid).toBe(false);
    app.allocations[0].quantity = 2;
    app.allocations[1].inventoryId = 11;
    app.allocations[1].quantity = 3;
    expect(app.allocationsValid).toBe(false);
  });
});

describe('acceso de usuarios', () => {
  let app: App;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    app = TestBed.createComponent(App).componentInstance;
    http = TestBed.inject(HttpTestingController);
    app.me = { id: 1, companyId: 7, username: 'admin', role: 'ADMIN', active: true };
  });

  it('crea un usuario, conserva las credenciales temporales y evita recargar todo el panel', async () => {
    app.userForm = { username: 'pruebas', password: 'SecretPassword2026!', role: 'VIEWER' };
    const action = app.createUser();
    const request = http.expectOne('/api/users');
    expect(request.request.body.username).toBe('pruebas');
    request.flush({ id: 2, companyId: 7, username: 'pruebas', role: 'VIEWER', active: true });
    await action;
    expect(app.users).toHaveLength(1);
    expect(app.issuedCredentials?.password).toBe('SecretPassword2026!');
    expect(app.userForm.password).toBe('');
    http.verify();
  });

  it('restablece la contraseña y limpia la clave del formulario', async () => {
    app.users = [{ id: 2, companyId: 7, username: 'pruebas', role: 'VIEWER', active: true }];
    app.resetUserId = 2;
    app.resetPasswordValue = 'ReplacementPassword2026!';
    const action = app.resetPassword();
    const request = http.expectOne('/api/users/2/reset-password');
    expect(request.request.body).toEqual({ newPassword: 'ReplacementPassword2026!' });
    request.flush(app.users[0]);
    await action;
    expect(app.resetPasswordValue).toBe('');
    expect(app.issuedCredentials?.username).toBe('pruebas');
    http.verify();
  });
});

describe('actualización de la pantalla de acceso', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(), provideZonelessChangeDetection(),
    ] });
    http = TestBed.inject(HttpTestingController);
  });

  it('muestra el panel al terminar el login sin necesitar otro clic', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.loginForm = { companyId: 1, username: 'pruebas', password: 'SecretPassword2026!' };
    fixture.detectChanges();

    const login = app.login();
    http.expectOne('/api/auth/login').flush({ accessToken: 'test-token' });
    await new Promise(resolve => setTimeout(resolve, 0));
    http.expectOne('/api/auth/me').flush({
      id: 2, companyId: 1, username: 'pruebas', role: 'VIEWER', active: true,
    });
    await new Promise(resolve => setTimeout(resolve, 0));
    http.expectOne('/api/companies/1/products/search?page=0&size=20&q=').flush({ items: [], total: 0, page: 0, size: 20 });
    http.expectOne('/api/companies/1/customers?page=0&size=20&q=').flush({ items: [], total: 0, page: 0, size: 20 });
    http.expectOne('/api/companies/1/warehouses').flush([]);
    http.expectOne('/api/companies/1/orders/search?page=0&size=20').flush({ items: [], total: 0, page: 0, size: 20 });
    await login;
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Buenos días, pruebas.');
    expect(fixture.nativeElement.querySelector('.login-shell')).toBeNull();
    http.verify();
  });

  it('muestra un error de login sin necesitar otro clic', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.loginForm = { companyId: 1, username: 'pruebas', password: 'incorrecta' };
    fixture.detectChanges();

    const login = app.login();
    http.expectOne('/api/auth/login').flush({ error: 'Invalid credentials' },
      { status: 401, statusText: 'Unauthorized' });
    await login;
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.login-shell .alert.error')?.textContent)
      .toContain('Invalid credentials');
    http.verify();
  });
});

describe('nuevos flujos operativos', () => {
  let app: App;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    app = TestBed.createComponent(App).componentInstance;
    http = TestBed.inject(HttpTestingController);
    app.me = { id: 1, companyId: 7, username: 'owner', role: 'ADMIN', active: true };
  });

  it('envía cliente y dirección al crear un pedido', async () => {
    app.orderCustomerId = 3;
    app.addressForm = { recipient: 'Buyer', street: 'Street 1', city: 'Madrid', postalCode: '28001', country: 'Spain' };
    app.orderLines = [{ productId: 5, quantity: 2 }];
    const action = app.createOrder();
    const request = http.expectOne('/api/companies/7/orders');
    expect(request.request.body.customerId).toBe(3);
    expect(request.request.body.deliveryAddress.street).toBe('Street 1');
    request.flush({ id: 9, customerId: 3, customerName: 'Buyer', deliveryAddress: app.addressForm,
      status: 'DRAFT', items: [{ id: 11, productId: 5, productName: 'Widget', quantity: 2, price: 10, currency: 'EUR' }] });
    await action;
    expect(app.selectedOrderId).toBe(9);
    http.verify();
  });

  it('calcula las unidades aún pendientes tras un envío parcial', () => {
    app.orders = [{ id: 9, customerId: 3, customerName: 'Buyer', deliveryAddress: null,
      status: 'CONFIRMED', items: [{ id: 11, productId: 5, productName: 'Widget', quantity: 5, price: 10, currency: 'EUR' }] }];
    app.selectedOrderId = 9;
    app.shipments = [{ id: 2, orderId: 9, warehouseId: 1, status: 'SHIPPED', carrier: null,
      trackingNumber: null, items: [{ id: 20, orderItemId: 11, quantity: 2, returnedQuantity: 0 }] }];
    expect(app.remainingByLine[11]).toBe(3);
  });

  it('registra una empresa y limpia el código del formulario', async () => {
    app.registerForm = { companyName: 'Nueva', username: 'owner', password: 'ExamplePassword2026!',
      registrationCode: 'local-development-registration-code' };
    const action = app.register();
    const request = http.expectOne('/api/auth/register');
    expect(request.request.body.companyName).toBe('Nueva');
    request.flush({ companyId: 13, username: 'owner' });
    await action;
    expect(app.loginForm.companyId).toBe(13);
    expect(app.registerForm.registrationCode).toBe('');
    http.verify();
  });
});
