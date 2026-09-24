import { DesignBlock } from '../../models/design-block.model';

export interface RenderContext {
  mockData: Record<string, any>;
  replaceVars: (text: string, mockData: Record<string, any>) => string;
  escape: (text: string) => string;
}

export interface BlockHtmlRenderer {
  supports(type: string): boolean;
  render(block: DesignBlock, context: RenderContext): string;
}

