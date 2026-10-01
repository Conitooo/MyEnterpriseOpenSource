import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, takeUntil } from 'rxjs';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);

  get<T>(path: string): Promise<T> { return firstValueFrom(this.http.get<T>(path).pipe(takeUntil(this.session.ended$))); }
  post<T>(path: string, body: unknown = {}): Promise<T> { return firstValueFrom(this.http.post<T>(path, body).pipe(takeUntil(this.session.ended$))); }
  put<T>(path: string, body: unknown): Promise<T> { return firstValueFrom(this.http.put<T>(path, body).pipe(takeUntil(this.session.ended$))); }
}

/** Keep server internals out of the UI while preserving actionable validation feedback. */
export function apiErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return 'No se pudo completar la operación. Inténtalo de nuevo.';
  if (error.status === 0) return 'No hay conexión con el servidor. Comprueba tu red e inténtalo de nuevo.';
  if (error.status === 401) return 'Credenciales incorrectas o sesión caducada.';
  if (error.status === 403) return 'No tienes permisos para realizar esta operación.';
  if (error.status === 404) return 'El recurso ya no está disponible. Actualiza los datos.';
  if (error.status === 429) return 'Demasiadas solicitudes. Espera un momento e inténtalo de nuevo.';
  if (error.status >= 500) return 'El servidor no pudo completar la operación. Inténtalo más tarde.';
  const body = error.error;
  const detail = typeof body === 'object' && body !== null
    ? body.message ?? body.detail ?? body.error : undefined;
  return typeof detail === 'string' && detail.length <= 300 && !/[\r\n<>]/.test(detail)
    ? detail : 'Revisa los datos e inténtalo de nuevo.';
}
