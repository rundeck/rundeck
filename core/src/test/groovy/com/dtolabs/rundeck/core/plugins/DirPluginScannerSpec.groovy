/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.utils.FileUtils
import com.dtolabs.rundeck.core.utils.cache.FileCache
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 3/8/17
 */
class DirPluginScannerSpec extends Specification {
    File testdir;

    def setup() throws Exception {
        testdir = new File("build/TestDirPluginScanner");
        FileUtils.deleteDir(testdir);
        testdir.mkdirs();
    }

    def cleanup() throws Exception {
        FileUtils.deleteDir(testdir);
    }


    public static class TestScanner extends DirPluginScanner {
        Map<File, String> versions;

        TestScanner(File extdir, FileCache<ProviderLoader> filecache, long rescanIntervalMs) {
            super(extdir, filecache);
        }

        @Override
        public boolean isValidPluginFile(File file) {
            return file.getName().contains("_");
        }

        @Override
        public FileFilter getFileFilter() {
            return null;
        }

        public ProviderLoader createLoader(File file) {
            return new TestDirPluginScanner.loader(file);
        }


        public ProviderLoader createCacheItemForFile(File file) {
            return new TestDirPluginScanner.loader(file);
        }

        @Override
        protected String getVersionForFile(File file) {
            if (null != versions) {
                return versions.get(file);
            }
            return null;
        }
    }

    @Unroll
    def "resolve provider conflict"() {
        given:
        File basedir = new File(testdir, "testResolveProviderConflict");
        basedir.mkdirs();
        final FileCache<ProviderLoader> loaderFileCache = new FileCache<ProviderLoader>();

        //scan interval set to 60 seconds
        TestScanner scanner = new TestScanner(basedir, loaderFileCache, 60 * 1000);
        when:
        File testfile1 = new File(basedir, file1name);
        File testfile2 = new File(basedir, file2name);
        def files = ['1': testfile1, '2': testfile2]


        ArrayList<FileCache.MemoFile> arr = new ArrayList<>();
        arr.add(FileCache.memoize(testfile1));
        arr.add(FileCache.memoize(testfile2));
        scanner.versions = versions.collectEntries { [files[it.key], it.value] };

        final File file = scanner.resolveProviderConflict(arr);
        then:
        file?.name == expect

        where:
        file1name | file2name | versions                         | expect
        'test1'   | 'test2'   | [:]                              | null
        'test1'   | 'test2'   | ['2': '1.0']                     | 'test2'
        'test1'   | 'test2'   | ['1': '1.0']                     | 'test1'
        'test1'   | 'test2'   | ['1': '1.0', '2': '1.0']         | 'test2'
        'test1'   | 'test2'   | ['1': '1.0', '2': '1.1']         | 'test2'
        'test1'   | 'test2'   | ['1': '1.2', '2': '1.1']         | 'test1'
        'test1'   | 'test2'   | ['1': '1.1-abc', '2': '1.1-def'] | 'test2'
        'test1'   | 'test2'   | ['1': '1.1-ghi', '2': '1.1-def'] | 'test2'

    }
}
