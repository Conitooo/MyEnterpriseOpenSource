import { HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { appConfig } from './app.config';

describe('JWT interceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    requests.verify();
    sessionStorage.clear();
  });

  it('sends the bearer token to backend API routes', () => {
    sessionStorage.setItem('meos_token', 'test-token');
    http.get('/api/products').subscribe();
    const request = requests.expectOne('/api/products');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    request.flush([]);
  });

  it('does not send the token to unrelated URLs', () => {
    sessionStorage.setItem('meos_token', 'test-token');
    http.get('https://example.org/public').subscribe();
    const request = requests.expectOne('https://example.org/public');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
