import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let headers = req.headers;

    const token = this.auth.getToken();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const code = this.auth.currentEntrepriseCode();
    if (code) {
      headers = headers.set('X-Entreprise-Code', code);
    }

    const cloned = req.clone({ headers });

    return next.handle(cloned).pipe(
      catchError((error: HttpErrorResponse) => {
        if ((error.status === 401 || error.status === 403) && !req.url.includes('/api/auth/login')) {
          this.auth.logout();
        }
        return throwError(() => error);
      })
    );
  }
}

