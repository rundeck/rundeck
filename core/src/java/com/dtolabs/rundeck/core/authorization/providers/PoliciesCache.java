/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* PoliciesCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Nov 16, 2010 11:26:12 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * PoliciesCache retains PolicyDocument objects for inserted Files, and reloads them if file modification time changes.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PoliciesCache implements Iterable<PoliciesDocument> {
    static final FilenameFilter filenameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".aclpolicy");
        }
    };
    private HashMap<File, PoliciesDocument> cache = new HashMap<File, PoliciesDocument>();
    private HashMap<File, Long> expiry = new HashMap<File, Long>();
    private DocumentBuilder builder;
    private File rootDir;

    public PoliciesCache(File rootDir) throws ParserConfigurationException {
        this.rootDir = rootDir;
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        builder = domFactory.newDocumentBuilder();
    }

    private File[] listDirFiles() {
        return rootDir.listFiles(filenameFilter);
    }

    public synchronized void add(final File file) throws PoliciesParseException {
        PoliciesDocument doc = getDocument(file);
    }

    private PoliciesDocument createEntry(final File file) throws PoliciesParseException {
        try {
            final Document document = builder.parse(file);
            return new PoliciesDocument(document,file);
        } catch (SAXException e) {
            throw new PoliciesParseException(e);
        } catch (IOException e) {
            throw new PoliciesParseException(e);
        }
    }

    public synchronized PoliciesDocument getDocument(final File file) throws PoliciesParseException {
        if(!file.exists()) {
            expiry.remove(file);
            cache.remove(file);
            return null;
        }
        final long lastmod = file.lastModified();
        final Long cachetime = expiry.get(file);
        final PoliciesDocument entry;
        if (null == cachetime || lastmod > cachetime) {
            entry = createEntry(file);
            if (null != entry) {
                expiry.put(file, lastmod);
                cache.put(file, entry);
            }
        } else {
            entry = cache.get(file);
        }
        return entry;
    }

    public Iterator<PoliciesDocument> iterator() {
        return new cacheIterator(Arrays.asList(listDirFiles()).iterator());
    }

    /**
     * Iterator over the PoliciesDocuments for the cache's files.  It skips
     * files that cannot be loaded.
     */
    private class cacheIterator implements Iterator<PoliciesDocument> {
        Iterator<File> intIter;
        private File nextFile;
        private PoliciesDocument nextDocument;

        public cacheIterator(final Iterator<File> intIter) {
            this.intIter = intIter;
            nextFile = this.intIter.hasNext() ? this.intIter.next() : null;
            loadNextDocument();
        }

        private void loadNextDocument() {
            while (hasNextFile() && null == nextDocument) {
                try {
                    nextDocument = getDocument(getNextFile());
                } catch (PoliciesParseException e) {

                }
            }
        }

        private File getNextFile() {
            File next = nextFile;
            nextFile = intIter.hasNext() ? intIter.next() : null;
            return next;
        }

        private PoliciesDocument getNextDocument() {
            PoliciesDocument doc = nextDocument;
            nextDocument=null;
            loadNextDocument();
            return doc;
        }

        public boolean hasNextFile() {
            return null != nextFile;
        }

        public boolean hasNext() {
            return null != nextDocument;
        }

        public PoliciesDocument next() {
            return getNextDocument();
        }

        public void remove() {
        }
    }

}
