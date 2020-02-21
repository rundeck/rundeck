/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package webhooks.importer

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.rundeck.core.projects.ProjectDataImporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import webhooks.exporter.WebhooksProjectExporter

class WebhooksProjectImporter implements ProjectDataImporter {

    private static final Logger logger = LoggerFactory.getLogger(WebhooksProjectImporter)
    public static final String IMPORT_WEBHOOKS = 'importWebhooks'
    public static final String WHK_REGEN_AUTH_TOKENS = 'regenAuthTokens'

    def webhookService

    final String name = 'webhooks'
    final List<String> importFilePatterns = [WebhooksProjectExporter.WEBHOOKS_YAML_FILE]

    final List<Property> importProperties = [
        PropertyBuilder.builder().
            booleanType(WHK_REGEN_AUTH_TOKENS).
            title('Regenerate Webhook Auth Tokens').
            description(
                'Regenerate all webhook auth tokens. If unchecked only webhooks without defined auth tokens will have' +
                ' their auth tokens regenerated.'
            ).
            build()
    ]


    @Override
    List<String> doImport(
        final UserAndRolesAuthContext authContext,
        final String project,
        final Map<String, File> importFile,
        final Map<String, String> importOptions
    ) {
        logger.info("Running import for project webhooks")
        File file = importFile[WebhooksProjectExporter.WEBHOOKS_YAML_FILE]
        if (!file) {
            return ["Import file ${WebhooksProjectExporter.WEBHOOKS_YAML_FILE} was not found"]
        }
        def errors = []
        Yaml yaml = new Yaml(new SafeConstructor())
        def data = yaml.load(new FileReader(file))
        if(data instanceof Map) {
            data.webhooks.each { hook ->
                logger.debug("Attempting to import hook: " + hook.name)
                if (hook.project != project) {
                    hook.project = project //reassign project so that the hook will show up under the new project
                }
                def importResult = webhookService.importWebhook(
                    authContext,
                    hook,
                    importOptions[WHK_REGEN_AUTH_TOKENS] == 'true'
                )
                if (importResult.msg) {
                    logger.debug(importResult.msg)
                } else if (importResult.err) {
                    logger.error(importResult.err)
                    errors.add(importResult.err)
                }
            }
        }
        return errors

    }
}
