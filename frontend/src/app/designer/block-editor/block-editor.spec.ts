import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach } from 'vitest';
import { BlockEditor } from './block-editor';
import { DesignBlock } from '../models/design-block.model';

describe('BlockEditor', () => {
  let component: BlockEditor;
  let fixture: ComponentFixture<BlockEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlockEditor],
    }).compileComponents();

    fixture = TestBed.createComponent(BlockEditor);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Signature block handling', () => {
    const testBlock: DesignBlock = {
      id: 'sig-1',
      type: 'signature',
      rotation: 0,
      x: 10,
      y: 10,
      signatureMode: 'CADRE_VIERGE',
      signatureMentionLegale: 'Lu et approuvé',
      signatureSignataireNom: 'Jean Dupont',
      signatureSignataireQualite: 'Directeur',
      signatureDate: '2026-09-27',
      signatureAfficherCadre: true,
      signatureCadrePointille: true,
    };

    beforeEach(async () => {
      fixture.componentRef.setInput('block', testBlock);
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('should patch form with signature values on block input', () => {
      expect(component.form.get('signatureMode')?.value).toBe('CADRE_VIERGE');
      expect(component.form.get('signatureMentionLegale')?.value).toBe('Lu et approuvé');
      expect(component.form.get('signatureSignataireNom')?.value).toBe('Jean Dupont');
      expect(component.form.get('signatureSignataireQualite')?.value).toBe('Directeur');
      expect(component.form.get('signatureDate')?.value).toBe('2026-09-27');
      expect(component.form.get('signatureAfficherCadre')?.value).toBe(true);
      expect(component.form.get('signatureCadrePointille')?.value).toBe(true);
    });

    it('should emit updated block with signature properties on save', () => {
      let emitted: any = null;
      component.updated.subscribe((b) => (emitted = b));

      component.form.patchValue({
        signatureMentionLegale: 'Bon pour accord',
        signatureSignataireNom: 'Marie Curie',
      });
      component.save();

      expect(emitted).toBeTruthy();
      expect(emitted.signatureMentionLegale).toBe('Bon pour accord');
      expect(emitted.signatureSignataireNom).toBe('Marie Curie');
    });

    it('should change signature mode and clear pad', () => {
      component.form.get('signatureMode')?.setValue('MANUSCRITE');
      component.onSignatureModeChange();

      expect(component.form.get('signatureMode')?.value).toBe('MANUSCRITE');

      // Test clear pad
      component.block!.signatureDataUrl = 'data:image/png;base64,fake';
      component.clearSigPad();
      expect(component.block!.signatureDataUrl).toBeUndefined();
      expect(component.form.get('signatureImageUrl')?.value).toBe('');
    });

    it('should handle drawing lifecycle methods without throwing', () => {
      expect(() => component.stopSigDrawing()).not.toThrow();

      const fakeMouseEvent = {
        preventDefault: () => {},
        clientX: 50,
        clientY: 50,
      } as any;

      expect(() => component.startSigDrawing(fakeMouseEvent)).not.toThrow();
      expect(() => component.drawSig(fakeMouseEvent)).not.toThrow();
      expect(() => component.stopSigDrawing()).not.toThrow();
    });
  });
});
