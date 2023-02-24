import groovy.sql.GroovyRowResult
import org.h2.jdbc.JdbcBlob

import java.nio.charset.Charset

databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-23") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"storage")
            }
        }
        createTable(tableName: "storage") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "storagePK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: '${text.type}')

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "dir", type: '${varchar2048.type}')

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "name", type: '${varchar1024.type}') {
                constraints(nullable: "false")
            }

            column(name: "namespace", type: '${varchar255.type}')

            column(name: "data", type: '${bytearray.type}')

            column(name: "path_sha", type: '${varchar40.type}') {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.11.0-wizard-id-e4e421", dbms: "postgresql") {
        comment { 'Add id to node-wizard node source configuration' }
        preConditions(onFail: "MARK_RAN"){
            and{
                tableExists (tableName:"storage")
                grailsPrecondition {
                    check {
                        List<GroovyRowResult> projectsWithWizard = sql.rows("select count(1) as num_projs from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")

                        if(projectsWithWizard.get(0).getProperty('num_projs') == 0) fail('No wizard nodes found, skipping this data migration')
                    }
                }
            }

        }
        grailsChange {
            change {
                List<GroovyRowResult> projects = sql.rows("select s.id, s.data from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")
                final String sourcePropSufix = '.type=node-wizard'
                sql.withTransaction {
                    projects.each { projectPropsRecord ->
                        {
                            String projectProps = new String(projectPropsRecord.getProperty('data') as byte[], Charset.forName('UTF-8'))

                            final char sourceIdx = projectProps.charAt(projectProps.indexOf(sourcePropSufix) - 1)
                            final String newSourceProp = 'resources.source.' + sourceIdx + '.config.wizard-id=nodes.yaml\n'

                            sql.executeUpdate("update storage set data = data || ? where id = ?", [newSourceProp.bytes, projectPropsRecord.getProperty('id')])
                        }
                    }
                }
            }
            rollback {

            }
            confirm 'Added resources.source.<sourceIdx>.config.wizard-id=nodes.yaml property for projects with node wizard'
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.11.0-wizard-id-e4e421", dbms: "oracle") {
        comment { 'Add id to node-wizard node source configuration' }
        preConditions(onFail: "MARK_RAN"){
            and{
                tableExists (tableName:"storage")
                grailsPrecondition {
                    check {
                        List<GroovyRowResult> projectsWithWizard = sql.rows("select count(1) as num_projs from storage s where s.name = 'project.properties' and dbms_lob.instr (s.data, utl_raw.cast_to_raw ('.type=node-wizard'), 1, 1) > 0")

                        if(projectsWithWizard.get(0).getProperty('num_projs') == 0) fail('No wizard nodes found, skipping this data migration')
                    }
                }
            }

        }
        grailsChange {
            change {
                List<GroovyRowResult> projects = sql.rows("select s.id, utl_raw.cast_to_varchar2(dbms_lob.substr(s.data)) data from storage s where s.name = 'project.properties' and dbms_lob.instr (s.data, utl_raw.cast_to_raw ('.type=node-wizard'), 1, 1) > 0")
                final String sourcePropSufix = '.type=node-wizard'
                sql.withTransaction {
                    projects.each { projectPropsRecord ->
                        {
                            String projectProps = projectPropsRecord.getProperty('data')

                            final char sourceIdx = projectProps.charAt(projectProps.indexOf(sourcePropSufix) - 1)
                            final String newSourceProp = 'resources.source.' + sourceIdx + '.config.wizard-id=nodes.yaml\n'

                            sql.executeUpdate("update storage set data = utl_raw.cast_to_raw(utl_raw.cast_to_varchar2(dbms_lob.substr(data))) || utl_raw.cast_to_raw(?) where id = ?", [newSourceProp, projectPropsRecord.getProperty('id')])
                        }
                    }
                }
            }
            rollback {

            }
            confirm 'Added resources.source.<sourceIdx>.config.wizard-id=nodes.yaml property for projects with node wizard'
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.11.0-wizard-id-e4e421", dbms: "h2") {
        comment { 'Add id to node-wizard node source configuration' }
        preConditions(onFail: "MARK_RAN"){
            and{
                tableExists (tableName:"storage")
                grailsPrecondition {
                    check {
                        List<GroovyRowResult> projectsWithWizard = sql.rows("select count(1) as num_projs from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")

                        if(projectsWithWizard.get(0).getProperty('num_projs') == 0) fail('No wizard nodes found, skipping this data migration')
                    }
                }
            }

        }
        grailsChange {
            change {
                List<GroovyRowResult> projects = sql.rows("select s.id, s.data from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")

                final String sourcePropSufix = '.type=node-wizard'
                sql.withTransaction {
                    projects.each { projectPropsRecord ->
                        {
                            JdbcBlob projectDataBlob = projectPropsRecord.getProperty('data') as JdbcBlob
                            String projectProps = projectDataBlob.getBinaryStream().getText()

                            final char sourceIdx = projectProps.charAt(projectProps.indexOf(sourcePropSufix) - 1)
                            final String newSourceProp = 'resources.source.' + sourceIdx + '.config.wizard-id=nodes.yaml\n'

                            sql.executeUpdate("update storage set data = concat(data, ?) where id = ?", [newSourceProp, projectPropsRecord.getProperty('id')])
                        }
                    }
                }
            }
            rollback {

            }
            confirm 'Added resources.source.<sourceIdx>.config.wizard-id=nodes.yaml property for projects with node wizard'
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.11.0-wizard-id-e4e421", dbms: "mariadb,mysql") {
        comment { 'Add id to node-wizard node source configuration' }
        preConditions(onFail: "MARK_RAN"){
            and{
                tableExists (tableName:"storage")
                grailsPrecondition {
                    check {
                        List<GroovyRowResult> projectsWithWizard = sql.rows("select count(1) as num_projs from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")

                        if(projectsWithWizard.get(0).getProperty('num_projs') == 0) fail('No wizard nodes found, skipping this data migration')
                    }
                }
            }

        }
        grailsChange {
            change {
                List<GroovyRowResult> projects = sql.rows("select s.id, s.data from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")
                final String sourcePropSufix = '.type=node-wizard'
                sql.withTransaction {
                    projects.each { projectPropsRecord ->
                        {
                            String projectProps = new String(projectPropsRecord.getProperty('data') as byte[], Charset.forName('UTF-8'))

                            final char sourceIdx = projectProps.charAt(projectProps.indexOf(sourcePropSufix) - 1)
                            final String newSourceProp = 'resources.source.' + sourceIdx + '.config.wizard-id=nodes.yaml\n'

                            sql.executeUpdate("update storage set data = concat(data, ?) where id = ?", [newSourceProp, projectPropsRecord.getProperty('id')])
                        }
                    }
                }
            }
            rollback {

            }
            confirm 'Added resources.source.<sourceIdx>.config.wizard-id=nodes.yaml property for projects with node wizard'
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.11.0-wizard-id-e4e421", dbms: "mssql") {
        comment { 'Add id to node-wizard node source configuration' }
        preConditions(onFail: "MARK_RAN"){
            and{
                tableExists (tableName:"storage")
                grailsPrecondition {
                    check {
                        List<GroovyRowResult> projectsWithWizard = sql.rows("select count(1) as num_projs from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")

                        if(projectsWithWizard.get(0).getProperty('num_projs') == 0) fail('No wizard nodes found, skipping this data migration')
                    }
                }
            }

        }
        grailsChange {
            change {
                List<GroovyRowResult> projects = sql.rows("select s.id, s.data from storage s where s.name = 'project.properties' and s.data like '%.type=node-wizard%'")
                final String sourcePropSufix = '.type=node-wizard'
                sql.withTransaction {
                    projects.each { projectPropsRecord ->
                        {
                            String projectProps = new String(projectPropsRecord.getProperty('data') as byte[], Charset.forName('UTF-8'))

                            final char sourceIdx = projectProps.charAt(projectProps.indexOf(sourcePropSufix) - 1)
                            final String newSourceProp = 'resources.source.' + sourceIdx + '.config.wizard-id=nodes.yaml\n'

                            sql.executeUpdate("update storage set data = data + ? where id = ?", [newSourceProp.bytes, projectPropsRecord.getProperty('id')])
                        }
                    }
                }
            }
            rollback {

            }
            confirm 'Added resources.source.<sourceIdx>.config.wizard-id=nodes.yaml property for projects with node wizard'
        }
    }
}