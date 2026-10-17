import { vi } from "vitest";
import {
  contextVariables,
  createOptionVariables,
  ContextVariable,
} from "../contextVariables";
import { getRundeckContext } from "../../rundeckService";

vi.mock("../../rundeckService");
const mockedRundeckContext = getRundeckContext as vi.Mock<unknown>;

describe("contextVariables", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedRundeckContext.mockImplementation(() => {});
  });

  it("returns expected keys by default", () => {
    expect(contextVariables()).toHaveProperty("job");
    expect(contextVariables()).toHaveProperty("execution");
    expect(contextVariables()).toHaveProperty("node");
    expect(contextVariables()).toHaveProperty("error_handler");
  });

  it("accepts global as a valid ContextVariable type", () => {
    const globalVar: ContextVariable = {
      name: "myGlobalVar",
      title: "My Global Variable",
      type: "global",
    };
    expect(globalVar.type).toBe("global");
  });
});
describe("createOptionVariables", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedRundeckContext.mockImplementation(() => {});
  });
  it("creates options variables when with input", () => {
    expect(
      createOptionVariables([
        {
          name: "a_custom_variable",
          label: "a custom variable",
          description:
            "a variable representing something not supplied by rundeck by default",
          type: "String",
        },
      ]),
    ).toEqual([
      {
        name: "a_custom_variable",
        title: "a custom variable",
        description:
          "a variable representing something not supplied by rundeck by default",
        type: "option",
      },
    ]);
  });
  it("creates empty variables when empty input", () => {
    expect(createOptionVariables([])).toEqual([]);
  });
});
