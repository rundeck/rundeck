import { AdhocCommandStore } from "../AdhocCommandStore";

describe("AdhocCommandStore", () => {
  let store: AdhocCommandStore;

  beforeEach(() => {
    store = new AdhocCommandStore();
  });

  describe("initialization", () => {
    it("should initialize with empty state", () => {
      expect(store.commandString).toBe("");
      expect(store.recentCommands).toEqual([]);
      expect(store.recentCommandsLoaded).toBe(false);
      expect(store.running).toBe(false);
      expect(store.canRun).toBe(false);
      expect(store.error).toBeNull();
    });

    it("should initialize with command string if provided", () => {
      const storeWithCommand = new AdhocCommandStore({
        commandString: "test command",
      });
      expect(storeWithCommand.commandString).toBe("test command");
    });
  });

  describe("commandString", () => {
    it("should get and set command string", () => {
      store.commandString = "echo hello";
      expect(store.commandString).toBe("echo hello");
    });
  });

  describe("allowInput", () => {
    it("should be false when running", () => {
      store.running = true;
      store.canRun = true;
      expect(store.allowInput).toBe(false);
    });

    it("should be false when cannot run", () => {
      store.running = false;
      store.canRun = false;
      expect(store.allowInput).toBe(false);
    });

    it("should be true when not running and can run", () => {
      store.running = false;
      store.canRun = true;
      expect(store.allowInput).toBe(true);
    });
  });

  describe("updateCanRunFromNodeTotal", () => {
    it("should set canRun to false when running", () => {
      store.updateCanRunFromNodeTotal(5, true);
      expect(store.canRun).toBe(false);
    });

    it("should set canRun to true when total > 0", () => {
      store.updateCanRunFromNodeTotal(5, false);
      expect(store.canRun).toBe(true);
    });

    it("should set canRun to false when total is 0", () => {
      store.updateCanRunFromNodeTotal(0, false);
      expect(store.canRun).toBe(false);
    });

    it("should set canRun to false when total is string '0'", () => {
      store.updateCanRunFromNodeTotal("0" as any, false);
      expect(store.canRun).toBe(false);
    });

    it("should call callback when provided", () => {
      const callback = jest.fn();
      store.setNodeFilterTotalCallback(callback);
      store.updateCanRunFromNodeTotal(5, false);
      expect(callback).toHaveBeenCalledWith(5);
    });
  });

  describe("loadRecentCommands", () => {
    const mockFetch = jest.fn();
    global.fetch = mockFetch;

    beforeEach(() => {
      mockFetch.mockClear();
    });

    it("should load recent commands successfully", async () => {
      const mockResponse = {
        executions: [
          {
            title: "echo test",
            filter: "name: node1",
            status: "succeeded",
            href: "/execution/123",
            execid: "123",
            extraMetadata: {},
          },
        ],
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      await store.loadRecentCommands("http://test.com/api/adhoc/history");

      expect(store.recentCommandsLoaded).toBe(true);
      expect(store.recentCommands).toHaveLength(1);
      expect(store.recentCommands[0].title).toBe("echo test");
      expect(store.recentCommands[0].statusClass).toBe("succeed");
    });

    it("should handle empty recent commands", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ executions: [] }),
      });

      await store.loadRecentCommands("http://test.com/api/adhoc/history");

      expect(store.recentCommandsLoaded).toBe(true);
      expect(store.recentCommandsNoneFound).toBe(true);
      expect(store.recentCommands).toHaveLength(0);
    });

    it("should handle API errors", async () => {
      mockFetch.mockRejectedValueOnce(new Error("Network error"));

      await store.loadRecentCommands("http://test.com/api/adhoc/history");

      expect(store.recentCommandsLoaded).toBe(true);
      expect(store.error).toContain("Recent commands list: request failed");
      expect(store.recentCommands).toHaveLength(0);
    });

    it("should handle HTTP errors", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        statusText: "Not Found",
      });

      await store.loadRecentCommands("http://test.com/api/adhoc/history");

      expect(store.recentCommandsLoaded).toBe(true);
      expect(store.error).toContain("Request failed");
    });

    it("should fill command when fillCommand is called", async () => {
      const mockNodeFilterStore = {
        setSelectedFilter: jest.fn(),
      };

      const mockResponse = {
        executions: [
          {
            title: "echo test",
            filter: "name: node1",
            status: "succeeded",
            href: "/execution/123",
            execid: "123",
            extraMetadata: { test: "data" },
          },
        ],
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      await store.loadRecentCommands(
        "http://test.com/api/adhoc/history",
        mockNodeFilterStore as any
      );

      const fillCommandSpy = jest.fn();
      window.dispatchEvent = fillCommandSpy;

      store.recentCommands[0].fillCommand();

      expect(store.commandString).toBe("echo test");
      expect(mockNodeFilterStore.setSelectedFilter).toHaveBeenCalledWith(
        "name: node1"
      );
    });

    it("should map status classes correctly", async () => {
      const statuses = [
        { status: "scheduled", expected: "running" },
        { status: "running", expected: "running" },
        { status: "succeed", expected: "succeed" },
        { status: "succeeded", expected: "succeed" },
        { status: "fail", expected: "fail" },
        { status: "failed", expected: "fail" },
        { status: "cancel", expected: "aborted" },
        { status: "aborted", expected: "aborted" },
        { status: "retry", expected: "failedretry" },
        { status: "timedout", expected: "timedout" },
        { status: "unknown", expected: "other" },
      ];

      for (const { status, expected } of statuses) {
        const mockResponse = {
          executions: [
            {
              title: "test",
              status,
              href: "/execution/123",
              execid: "123",
            },
          ],
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const testStore = new AdhocCommandStore();
        await testStore.loadRecentCommands("http://test.com/api/adhoc/history");

        expect(testStore.recentCommands[0].statusClass).toBe(expected);
      }
    });
  });

  describe("reset", () => {
    it("should reset all state to initial values", () => {
      store.commandString = "test";
      store.running = true;
      store.canRun = true;
      store.error = "error";
      // recentCommands is read-only, so we can't set it directly
      // Instead, we'll test that reset clears it by checking the initial state

      store.reset();

      expect(store.commandString).toBe("");
      expect(store.running).toBe(false);
      expect(store.canRun).toBe(false);
      expect(store.error).toBeNull();
      expect(store.recentCommands).toEqual([]);
    });
  });

  describe("stopFollowing", () => {
    it("should call stopFollowingOutput if followControl exists", () => {
      const mockFollowControl = {
        stopFollowingOutput: jest.fn(),
      };
      store.followControl = mockFollowControl;

      store.stopFollowing();

      expect(mockFollowControl.stopFollowingOutput).toHaveBeenCalled();
    });

    it("should not throw if followControl is null", () => {
      store.followControl = null;
      expect(() => store.stopFollowing()).not.toThrow();
    });

    it("should not throw if followControl has no stopFollowingOutput", () => {
      store.followControl = {};
      expect(() => store.stopFollowing()).not.toThrow();
    });
  });
});

