import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';

describe('reserva dividida de un pedido', () => {
  let app: App;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    app = TestBed.runInInjectionContext(() => new App());
    app.orders = [{ id: 1, status: 'DRAFT', items: [
      { id: 7, productId: 5, quantity: 5, price: 10, currency: 'EUR' },
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
    app = TestBed.runInInjectionContext(() => new App());
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
