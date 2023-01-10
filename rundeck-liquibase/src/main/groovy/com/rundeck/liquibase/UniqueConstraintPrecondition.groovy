package com.rundeck.liquibase

import groovy.sql.Sql

/**
 * Class used to validate unique constraints. This is a temporary class and it must be removed until
 * grail database migration plugin incorporates the method <i>uniqueConstraintExists()</i> from Liquibase.
 */
class UniqueConstraintPrecondition {

    static boolean exists(String dbName, Sql sql, String tableName, String constraintName) {
        DBMS dbms = processDbName(dbName)
        return dbms.validate(sql,tableName, constraintName)
    }

    static DBMS processDbName(String dbName) {
        return dbName.toUpperCase().replace(' ', '_') as DBMS
    }

    static enum DBMS {
        MYSQL {
            @Override
            boolean validate(Sql sql, String tableName,  String constraintName) {
                return validateUniqueConstraint(sql, tableName, constraintName)
            }
        },
        ORACLE {
            @Override
            boolean validate(Sql sql, String tableName, String constraintName) {
                tableName = tableName.toUpperCase()
                constraintName = constraintName.toUpperCase()
                String query = """
                    SELECT COUNT(DISTINCT CONSTRAINT_NAME) AS res
                    FROM USER_CONSTRAINTS
                    WHERE TABLE_NAME = '${tableName}' 
                    AND CONSTRAINT_NAME = '${constraintName}'
                    AND CONSTRAINT_TYPE = 'U'
                """
                def result = sql.firstRow(query).res
                return result > 0
            }
        },
        MARIADB {
            @Override
            boolean validate(Sql sql, String tableName, String constraintName) {
                return validateUniqueConstraint(sql, tableName, constraintName)
            }
        },
        POSTGRESQL {
            @Override
            boolean validate(Sql sql, String tableName, String constraintName) {
                tableName = tableName.toLowerCase()
                constraintName = constraintName.toLowerCase()
                return validateUniqueConstraint(sql, tableName, constraintName)
            }
        },
        MICROSOFT_SQL_SERVER {
            @Override
            boolean validate(Sql sql, String tableName, String constraintName) {
                return validateUniqueConstraint(sql, tableName, constraintName)
            }
        },
        H2 {
            @Override
            boolean validate(Sql sql, String tableName, String constraintName) {
                tableName = tableName.toUpperCase()
                constraintName = constraintName.toUpperCase()
                return validateUniqueConstraint(sql, tableName, constraintName)
            }
        }

        abstract boolean validate(Sql sql, String tableName, String constraintName)

        /**
         * Validates unique constraint from Mysql, MariaDb, Postgresql and MSSql
         * @param sql
         * @param tableName
         * @param constraintName
         * @return
         */
        boolean validateUniqueConstraint(Sql sql, String tableName, String constraintName) {
            String query = """
                    SELECT COUNT(distinct CONSTRAINT_NAME) AS res
                    FROM information_schema.TABLE_CONSTRAINTS
                    WHERE table_name = '${tableName}' 
                    AND CONSTRAINT_NAME = '${constraintName}'
                    AND constraint_type = 'UNIQUE'
                """
            def result = sql.firstRow(query).res
            return result > 0
        }
    }
}
