import { contextVariables } from "../context_variables";
import { getRundeckContext } from "../../rundeckService";

jest.mock("../../rundeckService")
const mockedRundeckContext = getRundeckContext as jest.Mock<unknown>;

describe("contextVariableUtils", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedRundeckContext.mockImplementation(() => {})
  });

  it("returns expected keys by default", () => {
    expect(contextVariables()).toHaveProperty("job");
    expect(contextVariables()).toHaveProperty("execution");
    expect(contextVariables()).toHaveProperty("node");
    expect(contextVariables()).toHaveProperty("error_handler");
    expect(contextVariables()).toHaveProperty("options");
  });


  it("returns 'options' key containing an empty array when rundeck context returns undefined", () => {
    mockedRundeckContext.mockImplementation(() => undefined)
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([]);
  })

  it("returns 'options' key containing an empty array when there's an error getting rundeck context", () => {
    mockedRundeckContext.mockImplementation(() => Error)
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([]);
  })

  it("returns 'options' key containing an empty array when contextVariables.data is undefined in rundeck context", () => {
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([]);
  })

  it("returns 'options' key containing an empty array when contextVariables.data.options is undefined in rundeck context", () => {
    mockedRundeckContext.mockImplementation(() => ({
      data: {}
    }))
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([]);
  })

  it("returns 'options' key containing an empty array when contextVariables.data.options.optionsData is undefined in rundeck context", () => {
    mockedRundeckContext.mockImplementation(() => ({
      data: {
        options: {}
      }
    }))
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([]);
  })

  it("returns 'options' key containing an empty array when contextVariables.data.options.optionsData is an empty array", () => {
    mockedRundeckContext.mockImplementation(() => ({
      data: {
        options: {
          optionsData: []
        }
      }
    }))
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([]);
  })

  it("returns 'options' key with populated data when rundeck context contains data in contextVariables.options.data.optionsData", () => {
    mockedRundeckContext.mockImplementation(() => ({
        data: {
          options: {
            optionsData: [
              {
                name: "a_custom_variable",
                label: "a custom variable",
                description: "a variable representing something not supplied by rundeck by default",
              }
            ]
          }
        }
    }))
    expect(contextVariables()).toHaveProperty("options");
    expect(contextVariables().options).toEqual([
      {
        name: "a_custom_variable",
        title: "a custom variable",
        description: "a variable representing something not supplied by rundeck by default",
        type: "option",
      }
    ]);
  })
});
