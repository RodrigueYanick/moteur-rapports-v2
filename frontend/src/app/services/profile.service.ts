import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PersonaProfileDto } from '../models/profile.model';

export interface ProfileApiResponse<T> {
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private readonly baseUrl = '/api/user/profile';

  constructor(private http: HttpClient) {}

  getProfile(): Observable<ProfileApiResponse<PersonaProfileDto>> {
    return this.http.get<ProfileApiResponse<PersonaProfileDto>>(this.baseUrl);
  }

  updateProfile(profile: PersonaProfileDto): Observable<ProfileApiResponse<PersonaProfileDto>> {
    return this.http.put<ProfileApiResponse<PersonaProfileDto>>(this.baseUrl, profile);
  }
}

