import axios from "axios";

import { api } from "../../../../../library/services/api";
import {
  getConfigMetaForExecutionMode,
  getNodeSummary,
  getNodes,
  getNodeResources,
  getNodeTags,
} from "../services/nodeServices";
import { getAppLinks, getRundeckContext } from "../../../../../library";

// Mock all external dependencies
jest.mock("axios");
jest.mock("../../../../../library/services/api", () => ({
  api: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

jest.mock("../../../../../library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    projectName: "TestProject",
    rdBase: "http://localhost:4440/",
    apiVersion: "41",
  })),
  getAppLinks: jest.fn().mockImplementation(() => ({
    frameworkNodeSummaryAjax: "/framework/nodeSummary",
    frameworkNodesQueryAjax: "/framework/nodesQuery",
  })),
}));

describe("Node Services", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getConfigMetaForExecutionMode", () => {
    const mockConfigMeta = [{ key: "executionMode", value: "active" }];

    it("should fetch config meta successfully", async () => {
      (axios.request as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockConfigMeta,
      });

      const result = await getConfigMetaForExecutionMode("TestProject");

      expect(result).toEqual(mockConfigMeta);
      expect(axios.request).toHaveBeenCalledWith({
        method: "GET",
        headers: {
          "x-rundeck-ajax": "true",
        },
        url: "http://localhost:4440/api/41/project/TestProject/meta?meta=config",
        validateStatus: expect.any(Function),
      });
    });

    it("should throw error for non-successful response", async () => {
      (axios.request as jest.Mock).mockResolvedValue({
        status: 400,
        data: { error: "Bad Request" },
      });

      await expect(
        getConfigMetaForExecutionMode("TestProject"),
      ).rejects.toEqual({
        message: "Error fetching execution mode: 400",
        response: expect.any(Object),
      });
    });

    it("should accept custom meta parameter", async () => {
      (axios.request as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockConfigMeta,
      });

      await getConfigMetaForExecutionMode("TestProject", "customMeta");

      expect(axios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: expect.stringContaining("meta=customMeta"),
        }),
      );
    });
  });

  describe("getNodeSummary", () => {
    const mockSummary = { total: 10, active: 8 };

    it("should fetch node summary successfully", async () => {
      (axios.get as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockSummary,
      });

      const result = await getNodeSummary();

      expect(result).toEqual(mockSummary);
      expect(axios.get).toHaveBeenCalledWith(
        "/framework/nodeSummary",
        expect.any(Object),
      );
    });

    it("should throw error for failed request", async () => {
      (axios.get as jest.Mock).mockRejectedValue({
        message: "Network Error",
        response: { status: 500 },
      });

      await expect(getNodeSummary()).rejects.toEqual({
        message: "Error: Network Error",
        response: expect.any(Object),
      });
    });
  });

  describe("getNodes", () => {
    const mockNodes = {
      allnodes: [
        { id: "node1", name: "Node 1" },
        { id: "node2", name: "Node 2" },
      ],
    };

    it("should fetch nodes successfully", async () => {
      (axios.request as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockNodes,
      });

      const result = await getNodes(
        { filter: "tags:production" },
        "/nodes/query",
      );

      expect(result).toEqual(mockNodes);
      expect(axios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "GET",
          headers: { "x-rundeck-ajax": "true" },
        }),
      );
    });

    it("should handle error response with message", async () => {
      (axios.request as jest.Mock).mockResolvedValue({
        status: 400,
        data: { message: "Invalid filter" },
      });

      await expect(getNodes({}, "/nodes/query")).rejects.toEqual({
        message: "Error: Invalid filter",
        response: expect.any(Object),
      });
    });

    it("should handle error response without message", async () => {
      (axios.request as jest.Mock).mockResolvedValue({
        status: 500,
        data: {},
      });

      await expect(getNodes({}, "/nodes/query")).rejects.toEqual({
        message: "Error: 500",
        response: expect.any(Object),
      });
    });
  });

  describe("getNodeResources", () => {
    const mockNodes = {
      node1: { name: "node1", tags: ["prod"] },
      node2: { name: "node2", tags: ["dev"] },
    };

    it("should fetch node resources successfully", async () => {
      (api.get as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockNodes,
      });

      const result = await getNodeResources({ filter: "tags:production" });

      expect(result).toEqual(mockNodes);
      expect(api.get).toHaveBeenCalledWith("project/TestProject/resources", {
        params: { filter: "tags:production" },
      });
    });

    it("should throw error for non-200 response", async () => {
      (api.get as jest.Mock).mockResolvedValue({
        status: 400,
        data: { message: "Bad Request" },
      });

      await expect(getNodeResources()).rejects.toEqual({
        message: "Bad Request",
        response: expect.any(Object),
      });
    });

    it("should use provided project parameter", async () => {
      (api.get as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockNodes,
      });

      await getNodeResources({}, "CustomProject");

      expect(api.get).toHaveBeenCalledWith(
        "project/CustomProject/resources",
        expect.any(Object),
      );
    });
  });

  describe("getNodeTags", () => {
    const mockTags = {
      tags: [
        { name: "prod", nodeCount: 5 },
        { name: "dev", nodeCount: 3 },
      ],
    };

    it("should fetch node tags successfully", async () => {
      (api.get as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockTags,
      });

      const result = await getNodeTags();

      expect(result).toEqual(mockTags);
      expect(api.get).toHaveBeenCalledWith("project/TestProject/nodes/tags");
    });

    it("should throw error for non-200 response", async () => {
      (api.get as jest.Mock).mockResolvedValue({
        status: 400,
        data: { message: "Bad Request" },
      });

      await expect(getNodeTags()).rejects.toEqual({
        message: "Bad Request",
        response: expect.any(Object),
      });
    });

    it("should use provided project parameter", async () => {
      (api.get as jest.Mock).mockResolvedValue({
        status: 200,
        data: mockTags,
      });

      await getNodeTags("CustomProject");

      expect(api.get).toHaveBeenCalledWith("project/CustomProject/nodes/tags");
    });
  });
});
