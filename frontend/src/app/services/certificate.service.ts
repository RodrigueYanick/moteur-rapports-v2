import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompanyCertificate } from '../models/certificate.model';

@Injectable({
  providedIn: 'root'
})
export class CertificateService {
  private readonly baseUrl = '/api/certificates';

  constructor(private http: HttpClient) {}

  getAll(): Observable<CompanyCertificate[]> {
    return this.http.get<CompanyCertificate[]>(this.baseUrl);
  }

  upload(file: File, alias: string, password?: string): Observable<CompanyCertificate> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('alias', alias);
    if (password) {
      formData.append('password', password);
    }
    return this.http.post<CompanyCertificate>(this.baseUrl, formData);
  }

  toggleActive(id: string): Observable<CompanyCertificate> {
    return this.http.patch<CompanyCertificate>(`${this.baseUrl}/${id}/toggle-active`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
