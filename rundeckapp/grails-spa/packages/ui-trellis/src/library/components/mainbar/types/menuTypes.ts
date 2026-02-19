export interface Link {
  url?: string;
  title?: string;
  links?: Link[];
  iconCss?: string;
  enabled?: boolean;
  separator?: boolean;
  group?: [id: string];
}
