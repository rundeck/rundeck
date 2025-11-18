export interface Link {
  url?: string;
  title?: string;
  links?: Link[];
  icon?: string;
  iconCss?: string;
  enabled?: boolean;
  separator?: boolean;
  group?: [id: string];
}
