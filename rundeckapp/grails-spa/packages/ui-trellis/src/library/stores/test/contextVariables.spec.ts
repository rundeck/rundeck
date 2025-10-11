import {contextVariables, createOptionVariables} from '../contextVariables'
import { getRundeckContext } from "../../rundeckService";

jest.mock("../../rundeckService")
const mockedRundeckContext = getRundeckContext as jest.Mock<unknown>;

describe("contextVariables", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedRundeckContext.mockImplementation(() => {
    })
  });

  it("returns expected keys by default", () => {
    expect(contextVariables()).toHaveProperty("job");
    expect(contextVariables()).toHaveProperty("execution");
    expect(contextVariables()).toHaveProperty("node");
    expect(contextVariables()).toHaveProperty("error_handler");
  });

});
describe('createOptionVariables',()=>{
  beforeEach(() => {
    jest.clearAllMocks();
    mockedRundeckContext.mockImplementation(() => {
    })
  });
  it("creates options variables when with input", () => {
    expect(createOptionVariables([{
      name: "a_custom_variable",
      label: "a custom variable",
      description: "a variable representing something not supplied by rundeck by default",
      type: "String",
    }])).toEqual([
      {
        name: "a_custom_variable",
        title: "a custom variable",
        description: "a variable representing something not supplied by rundeck by default",
        type: "option",
      }
    ]);
  })
  it("creates empty variables when empty input", () => {
    expect(createOptionVariables([])).toEqual([]);
  })
});
