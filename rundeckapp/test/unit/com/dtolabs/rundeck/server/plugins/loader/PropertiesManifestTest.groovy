/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.server.plugins.loader

import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * PropertiesManifestTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-18
 */
@RunWith(JUnit4)
class PropertiesManifestTest {

    @Test
    public void filename(){
        def manifest = new PropertiesManifest(['plugin.filename': 'test.jar'] as Properties)
        Assert.assertNotNull(manifest.fileName)
        Assert.assertEquals('test.jar',manifest.fileName)
    }
    @Test
    public void name(){
        def manifest = new PropertiesManifest(['plugin.name': 'a test'] as Properties)
        Assert.assertNotNull(manifest.name)
        Assert.assertEquals('a test',manifest.name)
    }
    @Test
    public void description(){
        def manifest = new PropertiesManifest(['plugin.description': 'a test'] as Properties)
        Assert.assertNotNull(manifest.description)
        Assert.assertEquals('a test',manifest.description)
    }
    @Test
    public void url(){
        def manifest = new PropertiesManifest(['plugin.url': 'a url'] as Properties)
        Assert.assertNotNull(manifest.url)
        Assert.assertEquals('a url',manifest.url)
    }
    @Test
    public void author(){
        def manifest = new PropertiesManifest(['plugin.author': 'a author'] as Properties)
        Assert.assertNotNull(manifest.author)
        Assert.assertEquals('a author',manifest.author)
    }
    @Test
    public void version(){
        def manifest = new PropertiesManifest(['plugin.version': 'a version'] as Properties)
        Assert.assertNotNull(manifest.version)
        Assert.assertEquals('a version',manifest.version)
    }
    @Test
    public void prefixMissing(){
        def manifest = new PropertiesManifest('a.b.c.d.e.f.',['plugin.version': 'a version'] as Properties)
        Assert.assertNull(manifest.version)
    }
    @Test
    public void prefix(){
        def manifest = new PropertiesManifest('a.b.c.d.e.f.',['a.b.c.d.e.f.plugin.version': 'a version'] as Properties)
        Assert.assertNotNull(manifest.version)
        Assert.assertEquals('a version', manifest.version)
    }
}
