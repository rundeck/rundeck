import { RuleSetLex, Token, TokenType } from "./RuleSetLex";

enum RuleType {
  condition,
  directive,
}

export enum DirectiveType {
  runAtStart = "run-at-start",
  runAfter = "run-after",
  runInSequence = "run-in-sequence",
}

enum ConditionType {
  if = "if",
  unless = "unless",
}

export interface Rule {
  identifiers: Array<string>;
  rules: Array<Condition | Directive>;
}

interface StepRule {
  identifier: string;
  rule: Condition | Directive;
}

export interface Condition {
  type: ConditionType;
  expression: string;
}

export interface Directive {
  type: DirectiveType;
  identifiers?: Array<string>;
}

type ParseFunc = () => ParseFunc;

/**
 * Parses ruleset tokens into step rules
 */
export class RuleSetParser {
  rules: Array<StepRule> = [];
  builder!: RuleBuilder;
  rule?: Condition | Directive;
  pos: number = 0;

  constructor(readonly tokens: Array<Token>) {}

  static ParseRules(rules: string) {
    const lines = rules.split(/\r?\n/).filter((line) => !line.startsWith("#"));

    const ruleSet = new RuleSet();

    for (const line of lines) {
      const lexer = new RuleSetLex(line);
      lexer.lex();
      const parser = new this(lexer.items);
      parser.parse();
      ruleSet.addRule(parser.builder);
    }

    return ruleSet;
  }

  parse() {
    let pFunc: ParseFunc = this.parseStart;

    try {
      while (true) {
        pFunc = pFunc();
      }
    } catch (e) {
      const ex = e as Error;
      if (ex.message != "EOF") throw ex;
    }
  }

  next(): Token | null {
    if (this.pos >= this.tokens.length) return null;

    const t = this.tokens[this.pos];
    this.pos++;
    return t;
  }

  /** Return the next token if it matches a set of types, otherwise throw an error */
  expect(...types: Array<TokenType>): Token {
    const t = this.next();

    if (t == null || types.indexOf(t.type) < 0)
      throw new Error(`Expected one of ${types}`);
    else return t;
  }

  /** Collect a run of tokens matching the type */
  collect(type: TokenType): Array<Token> {
    const tokens: any = [];
    let t = this.next();
    while (t != null && t.type == type) {
      tokens.push(t);
      t = this.next();
    }
    this.backup();
    return tokens;
  }

  backup() {
    this.pos--;
  }

  parseStart = (): ParseFunc => {
    const t = this.next();

    if (!this.builder) this.builder = new RuleBuilder();

    if (t == null) throw new Error("Unexpected EOF");

    switch (t.type) {
      case TokenType.EOF:
        throw new Error("EOF");
      case TokenType.stepIdentifier:
        this.backup();
        return this.parseRuleIdentifiers;
      case TokenType.conditionType:
        this.backup();
        return this.parseCondition;
      case TokenType.directiveType:
        this.backup();
        return this.parseDirective;

      default:
        return this.parseStart;
    }
  };

  parseRuleIdentifiers = (): ParseFunc => {
    const t = this.next();

    if (t == null) throw new Error("Unexpected EOF");

    if (t.type != TokenType.stepIdentifier) {
      this.backup();
      return this.parseStart;
    } else {
      this.builder.addIdentifer(t.string);
      return this.parseRuleIdentifiers;
    }
  };

  parseCondition = (): ParseFunc => {
    const tType = this.expect(TokenType.conditionType);
    const type = tType.string;

    const tExpr = this.expect(TokenType.conditionExpression);
    const expression = tExpr.string;

    this.builder.addRule({
      type: type as any as ConditionType,
      expression,
    });

    return this.parseStart;
  };

  parseDirective = (): ParseFunc => {
    const tType = this.expect(TokenType.directiveType);

    switch (tType.string) {
      case "run-in-sequence":
        this.builder.addRule({
          type: DirectiveType.runInSequence,
        });
        break;
      case "run-at-start":
        this.builder.addRule({
          type: DirectiveType.runAtStart,
        });
        break;
      case "run-after":
        this.builder.addRule({
          type: DirectiveType.runAfter,
          identifiers: this.collect(TokenType.stepIdentifier).map(
            (i) => i.string,
          ),
        });
        break;
      default:
        throw new Error(`Unexpected directive type [${tType.string}]`);
    }

    return this.parseStart;
  };
}

export class RuleSet {
  rules: Array<RuleBuilder> = [];

  constructor() {}

  addRule(rule: RuleBuilder) {
    this.rules.push(rule);
  }

  addRules(rules: RuleBuilder[]) {
    rules.forEach((r) => this.rules.push(r));
  }

  withoutWildcard(): Array<RuleBuilder> {
    return this.rules.filter((r) => r.identifiers[0] != "*");
  }

  withWildcard(): Array<RuleBuilder> {
    return this.rules.filter((r) => r.identifiers[0] == "*");
  }

  /** Returns rules with directives */
  withDirectives(): Array<RuleBuilder> {
    return this.rules.filter((r) => r.hasDirectives());
  }

  /** Returns rules with conditions */
  withConditions(): Array<RuleBuilder> {
    return this.rules.filter((r) => r.hasConditions());
  }
}

export class RuleBuilder {
  identifiers: Array<string> = [];
  rules: Array<Condition | Directive> = [];

  addIdentifer(string: string) {
    this.identifiers.push(string);
    return this;
  }

  addRule(rule: Condition | Directive) {
    this.rules.push(rule);
    return this;
  }

  rule(): Rule {
    return this;
  }

  stepRules(): Array<StepRule> {
    const stepRules: any = [];
    for (const identifier of this.identifiers) {
      for (const rule of this.rules) {
        stepRules.push({
          identifier,
          rule,
        });
      }
    }
    return stepRules;
  }

  toString(): string {
    if (this.identifiers.length == 0)
      throw new Error("Rule has no identifiers!");

    let r = "[";

    for (let x = 0; x < this.identifiers.length; x++) {
      r += this.identifiers[x];
      if (x != this.identifiers.length - 1) r += ",";
    }

    r += "] ";

    for (let x = 0; x < this.rules.length; x++) {
      const rule = this.rules[x];

      if (isCondition(rule)) {
        if (rule.expression.indexOf(" ") >= 0) {
          //quote
          r += '"';
          r += `${rule.type}`;
          r += `:${rule.expression}`;
          r += '"';
        } else {
          r += `${rule.type}`;
          r += `:${rule.expression}`;
        }
      } else if (rule.identifiers) {
        r += `${rule.type}`;
        r += `:${rule.identifiers.join(",")}`;
      } else {
        r += `${rule.type}`;
      }

      if (x != this.rules.length - 1) r += " ";
    }

    return r;
  }

  /** Returns a new rule with just the directives */
  directives(): RuleBuilder {
    const newRule = new RuleBuilder();
    newRule.identifiers = [...this.identifiers];
    newRule.rules = this.rules.filter((r) => isDirective(r));
    return newRule;
  }

  /** Returns a new rule with just the conditions */
  conditions(): RuleBuilder {
    const newRule = new RuleBuilder();
    newRule.identifiers = [...this.identifiers];
    newRule.rules = this.rules.filter((r) => isCondition(r));
    return newRule;
  }

  hasDirectives(): boolean {
    return this.rules.some((r) => isDirective(r));
  }

  hasConditions(): boolean {
    return this.rules.some((r) => isCondition(r));
  }

  getIdentifiers() {
    const ids: string[] = [];
    for (const id of this.identifiers) {
      ids.push(...this.expandRange(id));
    }

    return ids;
  }

  /* Return this rule applied to the given identifier */
  forIdentifier(identifier: string) {
    if (!this.getIdentifiers().includes(identifier))
      throw new Error(
        `This rules does not apply to identifier [${identifier}]`,
      );
    const newRule = new RuleBuilder();
    newRule.identifiers = [identifier];
    newRule.rules = this.rules;
    return newRule;
  }

  expandRange(id: string): Array<string> {
    const parts = id.split("-");
    if (parts.length != 2) return [id];

    const [start, end] = parts.map((p) => Number(p));

    if (isNaN(start) || isNaN(end)) return [id];

    const ids: string[] = [];
    for (let x = start; x <= end; x++) {
      ids.push(x.toString());
    }
    return ids;
  }
}

export function isCondition(rule: Condition | Directive): rule is Condition {
  if (rule.hasOwnProperty("expression")) return true;
  else return false;
}

export function isDirective(rule: Condition | Directive): rule is Directive {
  if (!rule.hasOwnProperty("expression")) return true;
  else return false;
}

export function isRange(identifier: string) {
  let [start, stop] = identifier.split("-");

  if (!start || !stop) return false;

  start = start.replace(/\s+/g, "");
  stop = stop.replace(/\s+/g, "");

  if (!isNaN(Number(start)) && !isNaN(Number(stop))) return true;
  else return false;
}
