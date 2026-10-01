import { HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { appConfig } from './app.config';
import { SessionService } from './core/session.service';

describe('JWT interceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let session: SessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
  });

  afterEach(() => {
    requests.verify();
    session.clear();
  });

  it('sends the bearer token to backend API routes', () => {
    session.start('test-token', 900);
    http.get('/api/products').subscribe();
    const request = requests.expectOne('/api/products');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    request.flush([]);
  });

  it('does not send the token to unrelated URLs', () => {
    session.start('test-token', 900);
    http.get('https://example.org/public').subscribe();
    const request = requests.expectOne('https://example.org/public');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('invalidates the session after an authenticated 401 response', () => {
    session.start('test-token', 900);
    http.get('/api/private').subscribe({ error: () => undefined });
    requests.expectOne('/api/private').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(session.token()).toBe('');
  });

  it('does not persist the access token in browser storage', () => {
    session.start('private-token', 900);
    expect(sessionStorage.getItem('meos_token')).toBeNull();
    expect(localStorage.getItem('meos_token')).toBeNull();
  });
});
