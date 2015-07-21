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

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * PoliciesCache retains PolicyDocument objects for inserted Files, and reloads them if file modification time changes.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PoliciesCache implements Iterable<PolicyCollection> {
    static final long DIR_LIST_CHECK_DELAY = Long.getLong(PoliciesCache.class.getName()+".DirListCheckDelay", 60000);
    static final long FILE_CHECK_DELAY = Long.getLong(PoliciesCache.class.getName() + ".FileCheckDelay", 60000);
    private final static Logger logger = Logger.getLogger(PoliciesCache.class);
    
    static final FilenameFilter filenameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".aclpolicy");
        }
    };
    
    private Set<File> warned = new HashSet<File>();
    private Map<String, CacheItem> cache = new HashMap<>();
    private SourceProvider provider;

    private PoliciesCache(final SourceProvider provider) {
        this.provider = provider;
    }

    private static class CacheItem{
        PolicyCollection policyCollection;
        Long cacheTime;
        Long modTime;

        private CacheItem(PolicyCollection policyCollection, Long modTime) {
            this.policyCollection = policyCollection;
            this.modTime = modTime;
            this.cacheTime=System.currentTimeMillis();
        }

        public void touch(Long time) {
            this.cacheTime = time;
        }
    }

    private PolicyCollection createEntry(final YamlSource source) throws PoliciesParseException {
        try {
            return YamlProvider.policiesFromSource(source);
        } catch (ParserException e1) {
            throw new PoliciesParseException("YAML syntax error: " + e1.toString(), e1);
        }catch (Exception e1) {
            throw new PoliciesParseException(e1);
        }
    }

    /**
     * @param source source
     * @return collection
     * @throws PoliciesParseException
     */
    public synchronized PolicyCollection getDocument(final CacheableYamlSource source) throws PoliciesParseException {
//        cacheTotal++;
        CacheItem entry = cache.get(source.getIdentity());

        long checkTime = System.currentTimeMillis();
        if (null == entry || ((checkTime - entry.cacheTime) > FILE_CHECK_DELAY)) {
            final long lastmod = source.getLastModified().getTime();
            if (null == entry || lastmod > entry.modTime) {
                    if (!source.isValid()) {
                        CacheItem remove = cache.remove(source.getIdentity());
                        entry = null;
//                        cacheRemove++;
                    } else {
//                        cacheMiss++;
                        PolicyCollection entry1 = createEntry(source);
                        if (null != entry1) {
                            entry = new CacheItem(entry1, lastmod);
                            cache.put(source.getIdentity(), entry);
                        } else {
                            cache.remove(source.getIdentity());
                            entry = null;
                        }
                    }
            }else{
//                cacheUnmodifiedHit++;
                entry.touch(checkTime);
            }
        }else{
//            cacheHit++;
        }
        return null != entry ? entry.policyCollection : null;
    }

    public Iterator<PolicyCollection> iterator() {
        return new cacheIterator(provider.getSourceIterator());
    }

    /**
     * Create a cache from a single file source
     * @param singleFile file
     * @return cache
     */
    public static PoliciesCache fromFile(File singleFile) {
        return new PoliciesCache(new FileProvider(singleFile));
    }

    /**
     * Create a cache from a directory source
     * @param rootDir base director
     * @return cache
     */
    public static PoliciesCache fromDir(File rootDir) {
        return new PoliciesCache(new DirProvider(rootDir));
    }
    /**
     * Create a cache from cacheable sources
     * @param sources source
     * @return cache
     */
    public static PoliciesCache fromSources(final Iterable<CacheableYamlSource> sources) {
        return new PoliciesCache(
                new SourceProvider() {
                    @Override
                    public Iterator<CacheableYamlSource> getSourceIterator() {
                        return sources.iterator();
                    }
                }
        );
    }
    private static class DirProvider implements SourceProvider{
        private File rootDir;

        public DirProvider(final File rootDir) {
            this.rootDir = rootDir;
        }

        long lastDirListCheckTime=0;
        private File[] lastDirList;
        private File[] listDirFiles() {
            if(System.currentTimeMillis()-lastDirListCheckTime > DIR_LIST_CHECK_DELAY) {
                doListDir();
            }
            return lastDirList;
        }

        private void doListDir() {
            lastDirList = rootDir.listFiles(filenameFilter);
            lastDirListCheckTime = System.currentTimeMillis();
        }
        public Iterator<CacheableYamlSource> getSourceIterator() {
            return asSources(listDirFiles());
        }
    }
    private static class FileProvider implements SourceProvider{
        private File file;

        public FileProvider(final File file) {
            this.file = file;
        }

        @Override
        public Iterator<CacheableYamlSource> getSourceIterator() {
            return asSources(new File[]{file});
        }
    }


    private static Iterator<CacheableYamlSource> asSources(final File[] files) {
        ArrayList<CacheableYamlSource> list = new ArrayList<>();
        if(null!=files) {
            for (File file : files) {
                list.add(YamlProvider.sourceFromFile(file));
            }
        }
        return list.iterator();
    }

    private Map<CacheableYamlSource, Long> cooldownset = Collections.synchronizedMap(new HashMap<CacheableYamlSource, Long>());
    /**
     * Iterator over the PolicyCollections for the cache's sources.  It skips
     * sources that are no longer valid
     */
    private class cacheIterator implements Iterator<PolicyCollection> {
        Iterator<CacheableYamlSource> intIter;
        private CacheableYamlSource nextSource;
        private PolicyCollection nextPolicyCollection;

        public cacheIterator(final Iterator<CacheableYamlSource> intIter) {
            this.intIter = intIter;
            nextSource = this.intIter.hasNext() ? this.intIter.next() : null;
            loadNextSource();
        }

        private void loadNextSource() {
            while (hasNextFile() && null == nextPolicyCollection) {
                CacheableYamlSource newNextSource = getNextSource();
                Long aLong = cooldownset.get(newNextSource);
                if (null != aLong && newNextSource.getLastModified().getTime() == aLong) {
                    logger.debug("Skip parsing of: " + newNextSource + ". Reason: parse error cooldown until modified");
                    continue;
                } else if (null != aLong) {
                    //clear
                    cooldownset.remove(newNextSource);
                }
                try {
                    nextPolicyCollection = getDocument(newNextSource);
                } catch (PoliciesParseException e) {
                    logger.error("ERROR unable to parse aclpolicy: " + newNextSource + ". Reason: " + e.getMessage());
                    logger.debug("ERROR unable to parse aclpolicy: " + newNextSource + ". Reason: " + e.getMessage(), e);
                    cache.remove(newNextSource.getIdentity());
                    cooldownset.put(newNextSource, newNextSource.getLastModified().getTime());
                }
            }
        }

        private CacheableYamlSource getNextSource() {
            CacheableYamlSource next = nextSource;
            nextSource = intIter.hasNext() ? intIter.next() : null;
            return next;
        }

        private PolicyCollection getNextPolicyCollection() {
            PolicyCollection doc = nextPolicyCollection;
            nextPolicyCollection =null;
            loadNextSource();
            return doc;
        }

        public boolean hasNextFile() {
            return null != nextSource;
        }

        public boolean hasNext() {
            return null != nextPolicyCollection;
        }

        public PolicyCollection next() {
            return getNextPolicyCollection();
        }

        public void remove() {
        }
    }

}
