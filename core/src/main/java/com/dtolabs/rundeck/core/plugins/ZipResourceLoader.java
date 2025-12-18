/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.utils.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * Created by greg on 8/26/16.
 */
public class ZipResourceLoader implements PluginResourceLoader {
    File cacheDir;
    File zipFile;
    List<String> resourcesList;
    String resourcesBasedir;
    List<String> basedirListing;
    Properties assetManifest; // Grails 7: asset-pipeline manifest for hashed filenames

    public ZipResourceLoader(
            final File cacheDir,
            final File zipFile,
            final List<String> resourcesList,
            final String resourcesBasedir
    )
    {
        this.cacheDir = cacheDir;
        this.zipFile = zipFile;
        this.resourcesList = resourcesList;
        this.resourcesBasedir = resourcesBasedir;
    }

    public List<String> listResources() {
        return Arrays.asList(getResourceNames());
    }

    /**
     * Get the list of resources in the jar
     */
    public String[] getResourceNames() {
        if (null != resourcesList) {
            return resourcesList.toArray(new String[resourcesList.size()]);
        }
        if (null != basedirListing) {
            return basedirListing.toArray(new String[basedirListing.size()]);
        }
        String[] list = listZipPath(zipFile, resourcesBasedir);
        if (null != list) {
            basedirListing = Arrays.asList(list);
            return list;
        }
        return new String[0];
    }

    /**
     * Get the main attributes for the jar file
     */
    private static String[] listZipPath(final File file, String path) {
        //debug("listJarPath: " + file + ", " + path);
        ArrayList<String> strings = new ArrayList<>();
        try {
            try (final JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file))) {
                ZipEntry nextJarEntry = jarInputStream.getNextEntry();
                while (nextJarEntry != null) {
                    if (nextJarEntry.getName().startsWith(path + "/")) {
                        if (!nextJarEntry.getName().endsWith("/")) {
                            strings.add(nextJarEntry.getName().substring(path.length() + 1));
                        }
                    }
                    nextJarEntry=jarInputStream.getNextEntry();
                }
            }
        } catch (IOException e) {
            return null;
        }

        return strings.toArray(new String[strings.size()]);
    }

    public InputStream openResourceStreamFor(final String path) throws PluginException, IOException {
        if (null == cacheDir) {
            throw new PluginException(String.format(
                    "Resource path %s was not found in the file: %s",
                    path,
                    zipFile.getAbsolutePath()
            ));
        }
        
        // Grails 7: Try resolving path via asset manifest for hashed filenames
        String resolvedPath = resolveAssetPath(path);
        
        File resfile = new File(cacheDir, resolvedPath);
        if (!resfile.isFile()) {
            throw new PluginException(String.format(
                    "Resource path %s (resolved to %s) did not exist in the file: %s",
                    path,
                    resolvedPath,
                    zipFile.getAbsolutePath()
            ));
        }
        return new FileInputStream(resfile);
    }
    
    /**
     * Resolve asset path via manifest.properties for Grails 7 hashed filenames.
     * Falls back to original path if manifest not found or path not in manifest.
     * 
     * Handles subdirectory paths: plugin.yaml may reference "js/common.js" while
     * manifest.properties contains "common.js=common-HASH.js". Asset-pipeline puts
     * hashed files at the root of resources/, not in subdirectories.
     * 
     * @param path Original path from plugin.yaml (e.g., "js/common.js" or "common.js")
     * @return Resolved path (e.g., "common-HASH.js") or original path if not in manifest
     */
    private String resolveAssetPath(String path) {
        // Load manifest on first use (lazy initialization)
        if (assetManifest == null) {
            assetManifest = new Properties();
            File manifestFile = new File(cacheDir, "manifest.properties");
            if (manifestFile.isFile()) {
                try (FileInputStream fis = new FileInputStream(manifestFile)) {
                    assetManifest.load(fis);
                } catch (IOException e) {
                    // Manifest load failed, continue without it (backwards compatible)
                }
            }
        }
        
        // Try direct lookup first (for backwards compatibility with pre-Grails 7 plugins)
        String resolved = assetManifest.getProperty(path);
        if (resolved != null) {
            return resolved;
        }
        
        // Try stripping subdirectory for Grails 7 asset-pipeline manifests
        // Example: "js/common.js" -> lookup "common.js" in manifest -> "common-HASH.js"
        // Note: Asset-pipeline places hashed files at ROOT of resources/, so we DON'T re-add subdir
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            String filename = path.substring(lastSlash + 1); // "common.js"
            String hashedFilename = assetManifest.getProperty(filename);
            if (hashedFilename != null) {
                return hashedFilename; // "common-HASH.js" (at root, no subdir prefix)
            }
        }
        
        // Fallback: return original path (backwards compatible for non-hashed assets)
        return path;
    }

    /**
     * Extract resources return the extracted files
     *
     * @return the collection of extracted files
     */
    public void extractResources() throws IOException {
        if (!cacheDir.isDirectory()) {
            if (!cacheDir.mkdirs()) {
                //debug("Failed to create cachedJar dir for dependent resources: " + cachedir);
            }
        }
        if (null != resourcesList) {
            //debug("jar manifest lists resources: " + resourcesList + " for file: " + pluginJar);

            extractJarContents(resourcesList, cacheDir);
            for (final String s : resourcesList) {
                File libFile = new File(cacheDir, s);
                libFile.deleteOnExit();
            }
        } else {
            //debug("using resources dir: " + resDir + " for file: " + pluginJar);
            ZipUtil.extractZip(zipFile.getAbsolutePath(), cacheDir, resourcesBasedir, resourcesBasedir);
        }
    }

    /**
     * Extract specific entries from the jar to a destination directory. Creates the
     * destination directory if it does not exist
     *
     * @param entries the entries to extract
     * @param destdir destination directory
     */
    private void extractJarContents(final List<String> entries, final File destdir) throws IOException {
        if (!destdir.exists()) {
            if (!destdir.mkdir()) {
                //log.warn("Unable to create cache dir for plugin: " + destdir.getAbsolutePath());
            }
        }

        //debug("extracting lib files from jar: " + pluginJar);
        for (final String path : entries) {
            //debug("Expand zip " + pluginJar.getAbsolutePath() + " to dir: " + destdir + ", file: " + path);
            ZipUtil.extractZipFile(zipFile.getAbsolutePath(), destdir, path);
        }

    }
}
