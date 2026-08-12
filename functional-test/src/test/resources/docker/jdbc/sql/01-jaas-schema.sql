-- Test fixture for GH-10423: org.eclipse.jetty.security.jaas.spi.JDBCLoginModule
-- reads credentials/roles from these tables, entirely independent of Rundeck's own
-- domain schema (login_user, rduser, ...), which is created afterwards by Liquibase
-- against the same `rundeck` database.

CREATE TABLE jaas_users (
    username VARCHAR(255) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE jaas_roles (
    username VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (username, role)
);

INSERT INTO jaas_users (username, password) VALUES ('jdbctest', 'jdbctest');

INSERT INTO jaas_roles (username, role) VALUES
    ('jdbctest', 'admin'),
    ('jdbctest', 'user'),
    ('jdbctest', 'architect'),
    ('jdbctest', 'build'),
    ('jdbctest', 'deploy');
