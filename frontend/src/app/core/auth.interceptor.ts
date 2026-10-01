import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const token = session.token();
  // Only same-origin application API paths may receive our bearer token.
  const apiRequest = request.url.startsWith('/api/');
  const outgoing = token && apiRequest
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(outgoing).pipe(catchError(error => {
    if (apiRequest && token && error instanceof HttpErrorResponse && error.status === 401) {
      session.invalidate();
    }
    return throwError(() => error);
  }));
};
