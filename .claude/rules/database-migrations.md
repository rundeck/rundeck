---
globs: ["rundeckapp/grails-app/migrations/**"]
alwaysApply: false
applyTo: "rundeckapp/grails-app/migrations/**"
---

# Database Migration Rules

## Mandatory

1. **NEVER modify existing migrations** without explicit team approval. Create a new migration instead.
2. **Always include rollback** changesets where possible.
3. **Always add preconditions** (`onFail: 'MARK_RAN'`) to make migrations idempotent.
4. **Support all databases**: MySQL/MariaDB, PostgreSQL, H2. Use `dbms` attribute when SQL syntax differs.
5. **Use Liquibase types** for portability (`BIGINT`, `VARCHAR(n)`, `TEXT`, `TIMESTAMP`, `BOOLEAN`).
6. **Test migrations locally** before committing.

## Database-Specific SQL

```xml
<!-- MySQL/MariaDB: CONCAT() -->
<sql dbms="mysql,mariadb">SELECT CONCAT('a', 'b')</sql>

<!-- PostgreSQL: || operator -->
<sql dbms="postgresql">SELECT 'a' || 'b'</sql>

<!-- H2: CONCAT() with CAST -->
<sql dbms="h2">SELECT CONCAT(CAST(col AS VARCHAR), 'b')</sql>
```

## Performance (Large Tables 10M+ rows)

- Index creation can take 30+ minutes and lock writes
- Use `ALGORITHM=INPLACE, LOCK=NONE` (MySQL) or `CREATE INDEX CONCURRENTLY` (PostgreSQL)
- Use optimized GROUP BY instead of loops for data population

## If Modifications Are Needed

1. **STOP** — do not make the change
2. **Document** — explain why the change is needed
3. **Consult** — ask the user/team for approval
4. **Wait** — do not proceed until explicitly authorized

## Before Completing

- [ ] Migration tested locally
- [ ] Rollback tested (if applicable)
- [ ] No modifications to existing migrations
- [ ] Preconditions added for idempotency
- [ ] Multi-database support (MySQL, PostgreSQL, H2)
- [ ] Business reason documented in changeset comments