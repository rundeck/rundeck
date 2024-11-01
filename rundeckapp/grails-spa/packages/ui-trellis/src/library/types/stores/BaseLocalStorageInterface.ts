export interface BaseLocalStorageInterface<T> {
  load(): Promise<T> | null;
  save(data: T): void;
}
