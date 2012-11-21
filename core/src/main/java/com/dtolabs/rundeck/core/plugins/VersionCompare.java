/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
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
* VersionCompare.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 6/20/12 5:01 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.util.*;

/**
 * VersionCompare compares version strings for plugin files.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class VersionCompare {
    public Integer maj;
    public String majString;
    public Integer min;
    public String minString;
    public Integer patch;
    public String patchString;
    public String tag;

    /**
     * Compares two version strings and their parsed integer values if available. Returns -1,0 or 1, if
     * value 1 is less than, equal to, or greater than value 2, respectively.  Compares integers if both are available,
     * otherwise compares non-integer as less than integer. if no integers are available, comparse strings, and treats null strings as less than non-null strings.
     */
    public static int comp(Integer v1, String s1, Integer v2, String s2) {
        if (v1 != null && v2 != null) {
            return v1.compareTo(v2);
        }else if(v1!=null){
            return 1;
        }else if(v2!=null){
            return -1;
        } else if (null == s1 && null == s2) {
            return 0;
        } else if (null == s1) {
            return -1;
        } else if (null == s2) {
            return 1;
        } else {
            //compare lexicographically
            return s1.compareTo(s2);
        }
    }

    public int compareTo(VersionCompare b) {
        //compare maj
        int c1 = comp(maj, majString, b.maj, b.majString);
        if (c1 != 0) {
            return c1;
        }
        //min
        int c2 = comp(min, minString, b.min, b.minString);
        if (c2 != 0) {
            return c2;
        }
        //patch
        int c3 = comp(patch, patchString, b.patch, b.patchString);
        if (c3 != 0) {
            return c3;
        }
        //ignore tag
        return 0;
    }
    /**
     * Return true if this verison is at least as big as the given version
     */
    public boolean atLeast(VersionCompare b) {
        return compareTo(b) >= 0;
    }

    /**
     * Return true if this verison is at most as big as the given version
     */
    public boolean atMost(VersionCompare b) {
        return compareTo(b) <= 0;
    }

    /**
     * Return a VersionCompare for the string
     */
    public static VersionCompare forString(final String value) {

        VersionCompare vers = new VersionCompare();
        if (null == value || "".equals(value)) {
            return vers;
        }

        String[] parr1 = value.split("-", 2);
        String[] v1arr = parr1[0].split("\\.", 3);
        if (v1arr.length > 0) {
            vers.majString = v1arr[0];
            try {
                vers.maj = Integer.parseInt(vers.majString);
            } catch (NumberFormatException e) {
            }
        }
        if (v1arr.length > 1) {
            vers.minString = v1arr[1];
            try {
                vers.min = Integer.parseInt(vers.minString);
            } catch (NumberFormatException e) {
            }
        }
        if (v1arr.length > 2) {
            vers.patchString = v1arr[2];
            try {
                vers.patch = Integer.parseInt(vers.patchString);
            } catch (NumberFormatException e) {
            }
        }
        if (parr1.length > 1) {
            vers.tag = parr1[1];
        }
        return vers;
    }

    public static class fileComparator implements Comparator<File>{
        Map<File,VersionCompare> versions;

        public fileComparator(final Map<File, VersionCompare> versions) {
            this.versions = versions;
        }

        public int compare(final File file, final File file1) {
            final VersionCompare v1 = versions.get(file);
            final VersionCompare v2 = versions.get(file1);
            int comp = 0;
            if (null != v1 && null != v2) {
                comp = v1.compareTo(v2);
            } else if (v1 == null) {
                comp = -1;
            } else if (v2 == null) {
                comp = 1;
            }
            if (0 == comp) {
                comp = file.getName().compareTo(file1.getName());
            }
            return comp;
        }
    }
    @Override
    public String toString() {
        return "Vers{" +
               "maj=" + maj +
               ", majString='" + majString + '\'' +
               ", min=" + min +
               ", minString='" + minString + '\'' +
               ", patch=" + patch +
               ", patchString='" + patchString + '\'' +
               ", tag='" + tag + '\'' +
               '}';
    }
}
