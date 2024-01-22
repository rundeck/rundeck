export interface BaseLocalStorageInterface<T> {
    load(): Promise<T>;
    store(data: T): Promise<void>;
}