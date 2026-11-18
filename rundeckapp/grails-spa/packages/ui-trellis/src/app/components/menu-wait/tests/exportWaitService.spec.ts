import {
  pollExportWait,
  EXPORT_WAIT_REQUEST_FAILED,
} from "../services/exportWaitService";
import { api } from "../../../../library/services/api";

jest.mock("../../../../library/services/api", () => ({
  api: { get: jest.fn() },
}));

describe("exportWaitService", () => {
  beforeEach(() => jest.clearAllMocks());

  it("returns the response data from the poll url", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      status: 200,
      data: { ready: true },
    });

    const result = await pollExportWait("/project/p/exportWait/t.json");

    expect(result).toEqual({ ready: true });
    expect(api.get).toHaveBeenCalledWith("/project/p/exportWait/t.json", {
      baseURL: "",
    });
  });

  it("throws with the status text on a client error status", async () => {
    (api.get as jest.Mock).mockResolvedValue({
      status: 404,
      statusText: "Not Found",
      data: {},
    });

    await expect(pollExportWait("/x")).rejects.toThrow("Not Found");
  });

  it("throws the HTTP status text when axios rejects on a 5xx", async () => {
    (api.get as jest.Mock).mockRejectedValue({
      response: { status: 500, statusText: "Internal Server Error" },
    });

    await expect(pollExportWait("/x")).rejects.toThrow("Internal Server Error");
  });

  it("throws a generic message on a network error without a response", async () => {
    (api.get as jest.Mock).mockRejectedValue(new Error("Network Error"));

    await expect(pollExportWait("/x")).rejects.toThrow(
      EXPORT_WAIT_REQUEST_FAILED,
    );
  });

  it("throws the sentinel when a 4xx has no status text", async () => {
    (api.get as jest.Mock).mockResolvedValue({ status: 404, data: {} });

    await expect(pollExportWait("/x")).rejects.toThrow(
      EXPORT_WAIT_REQUEST_FAILED,
    );
  });

  it.each([
    ["an HTML string", "<html>login</html>"],
    ["null", null],
    ["an array", []],
  ])("throws the sentinel when the body is %s", async (_label, body) => {
    (api.get as jest.Mock).mockResolvedValue({ status: 200, data: body });

    await expect(pollExportWait("/x")).rejects.toThrow(
      EXPORT_WAIT_REQUEST_FAILED,
    );
  });
});
