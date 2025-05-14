package com.rundeck.repository.client

import groovy.transform.CompileStatic

import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CompileStatic
class TestPluginGenerator {

    static File generate(String name, String type, String category, String buildDir,Map meta=[:]) {
        if ("script".equals(type)) {
            return generateScriptPlugin(name, category, buildDir,meta)
        } else {
            return generateJarPlugin(name, category, buildDir,meta)
        }
    }

    static File generateScriptPlugin(String name, String category, String buildDir, Map meta=[:]) {
        //copy resource path "test-plugins/test-plugin.zip" into the target dir with the given name
        def pluginName = name
        File target = new File(buildDir, "${pluginName}.zip")
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(target))
        zipOut.putNextEntry(new ZipEntry(pluginName + "/"))
        zipOut.closeEntry()


        zipOut.putNextEntry(new ZipEntry("${pluginName}/plugin.yaml"))
        String yaml = createPluginYaml(pluginName, category,meta)
//        System.err.println("yaml: ${yaml}")
        zipOut << yaml

        zipOut.closeEntry()

        zipOut.putNextEntry(new ZipEntry("${pluginName}/contents/"))
        zipOut.closeEntry()

        zipOut.putNextEntry(new ZipEntry("${pluginName}/contents/script.sh"))
        TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/zip/plugin-test/contents/script.sh").with { input ->
            zipOut << input
        }
        zipOut.closeEntry()
        zipOut.close()
        target
    }

    static String createPluginYaml(String name, String category,Map extra=[:]) {
        def yaml = TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/zip/plugin-test/plugin.yaml").text
        def data = [
                name: name,
                category: category,
                title: name,
                version:'1.0.0'
        ]+extra
        yaml = yaml.replaceAll(/\$(\w+?)\$/, { m, key -> data[key] })
        yaml
    }

    static File generateJarPlugin(String name, String category, String buildDir, Map meta=[:]) {
        //copy resource path "test-plugins/test-plugin.zip" into the target dir with the given name
        File target = new File(buildDir, "${name.toLowerCase()}.jar")
        def manifest = new Manifest()
        manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Classnames"), "org.rundeck.plugin.nodes.NodeRefreshWorkflowStep")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-File-Version"), meta['version']?:'1.0.0')
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Date"), "2018-05-03T10:58:38-05")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Description"), "Force refresh node list")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-URL"), "http://rundeck.com")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Author"), "Rundeck), Inc.")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Version"), "1.2")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Name"), name)
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Archive"), "true")

        def stream = new JarOutputStream(new FileOutputStream(target), manifest)
        stream.putNextEntry(new ZipEntry("org/rundeck/plugin/nodes/"))
        stream.closeEntry()

        stream.putNextEntry(new ZipEntry("org/rundeck/plugin/nodes/NodeRefreshWorkflowStep.class"))

        TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/java/org/rundeck/plugin/nodes/NodeRefreshWorkflowStep.class").with { input ->
            stream << input
        }
        stream.closeEntry()
        stream.close()

        target
    }

}
