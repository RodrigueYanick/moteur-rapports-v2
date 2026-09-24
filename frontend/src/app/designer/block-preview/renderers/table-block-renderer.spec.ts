import { TableBlockRenderer } from './table-block-renderer';
import { DesignBlock } from '../../models/design-block.model';
import { RenderContext } from './block-html-renderer.interface';

describe('TableBlockRenderer', () => {
  let renderer: TableBlockRenderer;
  let context: RenderContext;

  beforeEach(() => {
    renderer = new TableBlockRenderer();
    context = {
      mockData: {
        factures: [
          { ref: 'FAC-001', montant: 100 },
          { ref: 'FAC-002', montant: 200 }
        ]
      },
      replaceVars: (val: string, data: any) => val,
      escape: (val: string) => val || ''
    };
  });

  it('devrait générer thead, th et tbody avec répétition et évitement de coupure par défaut sur tableau dynamique', () => {
    const block: DesignBlock = {
      id: 'table-1',
      type: 'tableau',
      x: 0,
      y: 0,
      largeurBox: 300,
      hauteurBox: 200,
      rotation: 0,
      source: 'factures',
      colonnes: [
        { variable: 'ref', titre: 'Référence' },
        { variable: 'montant', titre: 'Montant' }
      ]
    };

    const html = renderer.render(block, context);

    expect(html).toContain('<thead><tr style="page-break-inside:avoid;break-inside:avoid;">');
    expect(html).toContain('<th');
    expect(html).toContain('Référence');
    expect(html).toContain('<tbody>');
    expect(html).toContain('<tr style="page-break-inside:avoid;break-inside:avoid;">');
    expect(html).toContain('FAC-001');
  });

  it('devrait désactiver l\'évitement de coupure si eviterCoupureLignes est false', () => {
    const block: DesignBlock = {
      id: 'table-2',
      type: 'tableau',
      x: 0,
      y: 0,
      largeurBox: 300,
      hauteurBox: 200,
      rotation: 0,
      source: 'factures',
      eviterCoupureLignes: false,
      colonnes: [
        { variable: 'ref', titre: 'Référence' }
      ]
    };

    const html = renderer.render(block, context);

    expect(html).toContain('<thead><tr style="">');
    expect(html).not.toContain('page-break-inside:avoid');
  });

  it('devrait générer thead et tbody sur tableau statique avec repeterEnTeteChaquePage', () => {
    const block: DesignBlock = {
      id: 'table-static',
      type: 'tableau',
      x: 0,
      y: 0,
      largeurBox: 300,
      hauteurBox: 150,
      rotation: 0,
      repeterEnTeteChaquePage: true,
      eviterCoupureLignes: true,
      lignes: [
        [{ value: 'Col A' }, { value: 'Col B' }],
        [{ value: 'Val 1' }, { value: 'Val 2' }]
      ]
    };

    const html = renderer.render(block, context);

    expect(html).toContain('<thead><tr style="page-break-inside:avoid;break-inside:avoid;">');
    expect(html).toContain('<th');
    expect(html).toContain('Col A');
    expect(html).toContain('<tbody>');
    expect(html).toContain('<tr style="page-break-inside:avoid;break-inside:avoid;">');
    expect(html).toContain('Val 1');
  });

  it('devrait omettre thead sur tranches secondaires si repeterEnTeteChaquePage est false', () => {
    const block: any = {
      id: 'table-sliced',
      type: 'tableau',
      x: 0,
      y: 0,
      largeurBox: 300,
      hauteurBox: 200,
      rotation: 0,
      source: 'factures',
      repeterEnTeteChaquePage: false,
      _tableSlice: { startRow: 2, endRow: 4 },
      colonnes: [
        { variable: 'ref', titre: 'Référence' }
      ]
    };

    const html = renderer.render(block, context);

    expect(html).not.toContain('<thead>');
    expect(html).toContain('<tbody>');
  });
});
