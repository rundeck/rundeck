import "jest";

import { RuleSetLex } from "../RuleSetLex";
import { RuleSetParser } from "../RuleSetParser";

describe("RuleSetLexer", () => {
  it("Lexes basic", () => {
    const rule = "[1,two,3] run-after:1,2 if:option.foo==BAR";
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual([
      "1",
      "two",
      "3",
      "run-after",
      "1",
      "2",
      "if",
      "option.foo==BAR",
      "EOF",
    ]);
  });

  it("Lexes non step directive with condition", () => {
    const rule = "[2-4] run-in-sequence unless:option.a==a";

    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual([
      "2-4",
      "run-in-sequence",
      "unless",
      "option.a==a",
      "EOF",
    ]);
  });

  it("lexes multiple conditions", () => {
    const rule = "[*] if:option.foo==BAR unless:option.fizz==BAZZ";
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual([
      "*",
      "if",
      "option.foo==BAR",
      "unless",
      "option.fizz==BAZZ",
      "EOF",
    ]);
  });
  it("parses", () => {
    const rule =
      "[*,2,3] if:option.foo==BAR unless:option.fizz==BAZZ run-after:1 run-at-start";
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const parser = new RuleSetParser(lexer.items);

    parser.parse();
  });
  it("parses quoted identifiers", () => {
    const rule = `"three four" "run-after:one two, three four"`;
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual([
      "three four",
      "run-after",
      "one two",
      "three four",
      "EOF",
    ]);
  });
  it("parses quoted conditions", () => {
    const rule = `[1] "if: option.select != really bad"`;
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual(["1", "if", "option.select != really bad", "EOF"]);
  });
  it("parses ranges", () => {
    const rule = `[1-5] run-in-sequence`;
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual(["1-5", "run-in-sequence", "EOF"]);
  });

  it("trims identifiers", () => {
    const rule = "[1-2,4-7 , 9 - 11 ]";
    const lexer = new RuleSetLex(rule);

    lexer.lex();

    const tokens = lexer.items.map((i) => i.string);

    expect(tokens).toEqual(["1-2", "4-7", "9 - 11", "EOF"]);
  });
});
