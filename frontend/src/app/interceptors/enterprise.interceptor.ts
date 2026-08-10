import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class EntrepriseInterceptor implements HttpInterceptor {
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const code = localStorage.getItem('entrepriseCode');
        if (code) {
        const cloned = req.clone({ setHeaders: { 'X-Entreprise-Code': code } });
        return next.handle(cloned);
        }
        // En l'absence de code, on laisse passer la requête sans header
        return next.handle(req);
    }
}