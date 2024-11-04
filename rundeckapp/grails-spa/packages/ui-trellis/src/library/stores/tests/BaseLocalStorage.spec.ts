import {
  BaseLocalStorageStore,
  StorageFactory,
} from "../BaseLocalStorageStore";

describe("BaseLocalStorageStore", () => {
  let store: BaseLocalStorageStore<any>;
  const testKey = "testKey";

  beforeEach(() => {
    store = new BaseLocalStorageStore(testKey);
    localStorage.clear();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should load data from localStorage", async () => {
    const testData = { foo: "bar" };
    localStorage.setItem(testKey, JSON.stringify(testData));

    const result = await store.load();
    expect(result).toEqual(testData);
  });

  it("should return an empty object if no data in localStorage", async () => {
    const result = await store.load();
    expect(result).toEqual({});
  });

  it("should handle JSON parse errors when loading", async () => {
    localStorage.setItem(testKey, "invalid JSON");
    const consoleSpy = jest.spyOn(console, "warn").mockImplementation();

    const result = await store.load();

    expect(result).toEqual({});
    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining(
        `Failed to load data from localStorage for key ${testKey}:`,
      ),
      expect.any(Error),
    );
    expect(localStorage.getItem(testKey)).toBeNull();
  });

  it("should save data to localStorage", async () => {
    const testData = { foo: "bar" };
    await store.save(testData);

    const storedData = localStorage.getItem(testKey);
    expect(JSON.parse(storedData)).toEqual(testData);
  });

  it("should handle errors when saving", async () => {
    const consoleSpy = jest.spyOn(console, "warn").mockImplementation();
    jest.spyOn(localStorage, "setItem").mockImplementation(() => {
      throw new Error("Storage full");
    });

    const testData = { foo: "bar" };
    await store.save(testData);

    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining(
        `Error saving data to localStorage for key "${testKey}":`,
      ),
      expect.any(Error),
    );
  });
});

describe("StorageFactory", () => {
  it("should create a new storage instance if it doesn't exist", () => {
    const storage1 = StorageFactory.getStorage("storage1");
    expect(storage1).toBeInstanceOf(BaseLocalStorageStore);
  });

  it("should return the same instance for the same key", () => {
    const storage1 = StorageFactory.getStorage("storage1");
    const storage1Again = StorageFactory.getStorage("storage1");
    expect(storage1).toBe(storage1Again);
  });

  it("should create different instances for different keys", () => {
    const storage1 = StorageFactory.getStorage("storage1");
    const storage2 = StorageFactory.getStorage("storage2");
    expect(storage1).not.toBe(storage2);
  });
});
