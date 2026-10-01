import { Injectable, OnDestroy, computed, signal } from '@angular/core';
import { Subject } from 'rxjs';

/** Access tokens stay in memory so a reload or a closed tab cannot recover them. */
@Injectable({ providedIn: 'root' })
export class SessionService implements OnDestroy {
  private readonly accessToken = signal('');
  private readonly expiry = signal(0);
  private readonly expiredSubject = new Subject<void>();
  private readonly endedSubject = new Subject<void>();
  private timer: ReturnType<typeof setTimeout> | undefined;

  readonly token = this.accessToken.asReadonly();
  readonly expiresAt = this.expiry.asReadonly();
  readonly authenticated = computed(() => !!this.accessToken() && this.expiry() > Date.now());
  readonly expired$ = this.expiredSubject.asObservable();
  readonly ended$ = this.endedSubject.asObservable();

  start(token: string, expiresIn: number): void {
    if (!token || !Number.isFinite(expiresIn) || expiresIn <= 0) {
      throw new Error('La respuesta de acceso no es válida.');
    }
    this.clear();
    const expiry = Date.now() + expiresIn * 1000;
    this.accessToken.set(token);
    this.expiry.set(expiry);
    this.timer = setTimeout(() => {
      this.clear();
      this.expiredSubject.next();
    }, Math.max(0, expiry - Date.now()));
  }

  clear(): void {
    const hadToken = !!this.accessToken();
    if (this.timer) clearTimeout(this.timer);
    this.timer = undefined;
    this.accessToken.set('');
    this.expiry.set(0);
    if (hadToken) this.endedSubject.next();
  }

  invalidate(): void {
    if (!this.accessToken()) return;
    this.clear();
    this.expiredSubject.next();
  }

  ngOnDestroy(): void { this.clear(); }
}
