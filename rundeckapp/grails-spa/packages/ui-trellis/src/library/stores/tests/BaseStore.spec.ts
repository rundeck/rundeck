import { BaseStore } from "../utils/BaseStore";
import { CacheFactory } from "../modules/SimpleCache";
import { StorageFactory } from "../BaseLocalStorageStore";

const mockCache = { get: jest.fn(), set: jest.fn(), clear: jest.fn() };
const mockStorage = { load: jest.fn(), save: jest.fn() };

describe("BaseStore", () => {
  let initialState: {
    users: { [key: string]: any };
    posts: { [key: string]: any };
  };
  let store: BaseStore<typeof initialState>;

  beforeEach(() => {
    // Reset mocks
    jest.resetAllMocks();

    // Deep clone the initial state to prevent mutation between tests
    initialState = {
      users: {
        "1": { id: "1", name: "John Doe" },
        "2": { id: "2", name: "Jane Doe" },
      },
      posts: {
        "1": { id: "1", title: "First Post" },
        "2": { id: "2", title: "Second Post" },
      },
    };

    // Create a fresh instance of BaseStore for each test
    store = new BaseStore(initialState);
  });

  it("initializes with the given state", () => {
    expect(store["store"]).toEqual(initialState);
    expect(store["cache"]).toBe(null);
    expect(store["storage"]).toBe(null);
  });

  it("gets item from store", () => {
    const item = store.getItem("users", "1");
    expect(item).toEqual({ id: "1", name: "John Doe" });
  });

  it("initializes with cache when storageKey is provided", () => {
    store = new BaseStore(initialState, "testKey");
    expect(store["cache"]).not.toBe(null);
  });

  it("gets item from cache if available", () => {
    mockCache.get = jest
      .fn()
      .mockReturnValue({ id: "1", name: "Cached John Doe" });

    jest.spyOn(CacheFactory, "getCache").mockImplementation(() => mockCache);

    store = new BaseStore(initialState, "testKey");
    const item = store.getItem("users", "1");
    expect(item).toEqual({ id: "1", name: "Cached John Doe" });
    expect(mockCache.get).toHaveBeenCalledWith("users_1");
  });

  it("initializes with cache and storage when useCacheAndStorage is true", () => {
    store = new BaseStore(initialState, "testKey", true);
    expect(store["cache"]).not.toBe(null);
    expect(store["storage"]).not.toBe(null);
  });

  it("sets item in store and cache", () => {
    jest.spyOn(CacheFactory, "getCache").mockImplementation(() => mockCache);

    store = new BaseStore(initialState, "testKey");
    const newUser = { id: "3", name: "New User" };
    store.setItem("users", newUser);

    expect(store.getItem("users", "3")).toEqual(newUser);
    expect(mockCache.set).toHaveBeenCalledWith("users_3", newUser);
  });

  it("removes item from store and cache", () => {
    jest.spyOn(CacheFactory, "getCache").mockImplementation(() => mockCache);

    store = new BaseStore(initialState, "testKey");
    store.removeItem("users", "1");

    expect(store.getItem("users", "1")).toBeNull();
    expect(mockCache.set).toHaveBeenCalledWith("users_1", null);
  });

  it("updates item in store and cache", () => {
    jest.spyOn(CacheFactory, "getCache").mockImplementation(() => mockCache);

    store = new BaseStore(initialState, "testKey");
    store.updateItem("users", "1", { name: "Updated John Doe" });

    expect(store.getItem("users", "1")).toEqual({
      id: "1",
      name: "Updated John Doe",
    });
    expect(mockCache.set).toHaveBeenCalledWith("users_1", {
      id: "1",
      name: "Updated John Doe",
    });
  });

  it("gets all items from a collection", () => {
    const allUsers = store.getAllItems("users");
    expect(allUsers).toEqual(initialState.users);
  });

  it("searches items in a collection", () => {
    const result = store.searchItems("users", (user) =>
      user.name.includes("John"),
    );
    expect(result).toEqual({ "1": { id: "1", name: "John Doe" } });
  });

  it("gets paginated items from a collection", () => {
    const paginatedUsers = store.getPaginatedItems("users", 2, 1);
    expect(paginatedUsers).toEqual([{ id: "2", name: "Jane Doe" }]);
  });

  it("loads data from localStorage on initialization", () => {
    const storedData = {
      users: {
        "3": { id: "3", name: "Stored User" },
      },
    };
    mockStorage.load.mockReturnValue(storedData);
    jest
      .spyOn(StorageFactory, "getStorage")
      .mockImplementation(() => mockStorage);

    store = new BaseStore(initialState, "testKey", true);

    expect(store.getItem("users", "3")).toEqual({
      id: "3",
      name: "Stored User",
    });
    expect(mockStorage.load).toHaveBeenCalled();
  });

  it("saves data to localStorage when setItem is called", () => {
    jest
      .spyOn(StorageFactory, "getStorage")
      .mockImplementation(() => mockStorage);

    store = new BaseStore(initialState, "testKey", true);
    const newUser = { id: "3", name: "New User" };
    store.setItem("users", newUser);

    expect(mockStorage.save).toHaveBeenCalledWith(
      expect.objectContaining({
        users: expect.objectContaining({
          "3": newUser,
        }),
      }),
    );
  });

  it("saves data to localStorage when removeItem is called", () => {
    jest
      .spyOn(StorageFactory, "getStorage")
      .mockImplementation(() => mockStorage);

    store = new BaseStore(initialState, "testKey", true);
    store.removeItem("users", "1");

    expect(mockStorage.save).toHaveBeenCalledWith(
      expect.objectContaining({
        posts: initialState.posts,
        users: expect.not.objectContaining({
          "1": expect.anything(),
        }),
      }),
    );
  });

  it("saves data to localStorage when updateItem is called", () => {
    jest
      .spyOn(StorageFactory, "getStorage")
      .mockImplementation(() => mockStorage);

    store = new BaseStore(initialState, "testKey", true);
    store.updateItem("users", "1", { name: "Updated John Doe" });

    expect(mockStorage.save).toHaveBeenCalledWith(
      expect.objectContaining({
        users: expect.objectContaining({
          "1": { id: "1", name: "Updated John Doe" },
        }),
      }),
    );
  });

  it("does not use localStorage when useCacheAndStorage is false", () => {
    jest
      .spyOn(StorageFactory, "getStorage")
      .mockImplementation(() => mockStorage);

    store = new BaseStore(initialState, "testKey", false);
    const newUser = { id: "3", name: "New User" };
    store.setItem("users", newUser);

    expect(mockStorage.save).not.toHaveBeenCalled();
  });
});
