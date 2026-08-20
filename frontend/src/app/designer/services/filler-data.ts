import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Variable } from '../../models/variable.model';

@Injectable({ providedIn: 'root' })
export class FillerDataService {
  private valuesSubject = new BehaviorSubject<Record<string, any>>({});
  public values$ = this.valuesSubject.asObservable();

  constructor() {}

  setValues(values: Record<string, any>): void {
    this.valuesSubject.next(values);
  }

  updateValue(key: string, value: any): void {
    const current = { ...this.valuesSubject.value, [key]: value };
    this.valuesSubject.next(current);
  }

  getValues(): Record<string, any> {
    return this.valuesSubject.value;
  }

  reset(): void {
    this.valuesSubject.next({});
  }
}