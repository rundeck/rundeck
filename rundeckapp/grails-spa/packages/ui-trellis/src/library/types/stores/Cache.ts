export interface Cache<T> {
  get(key: string): T | null;
  set(key: string, value: T): void;
  clear(): void;
}
