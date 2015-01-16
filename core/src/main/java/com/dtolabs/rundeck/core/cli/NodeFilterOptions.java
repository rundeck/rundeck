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
* NodeFilterOptions.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 30, 2010 3:54:42 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.util.Map;
import java.util.HashMap;

/**
 * NodeFilterOptions presents common Nodeset filter options for a CLI Tool.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeFilterOptions implements CLIToolOptions {
    /**
     * Short option value for filter exclude precedence option
     */
    static final String FILTER_EXCLUDE_PRECEDENCE_OPT = "Z";

    private boolean argKeepgoing;
    private boolean argKeepgoingSet;
    private int argThreadCount=-1;
    private Map<String, String> includeMap;
    private Map<String, String> excludeMap;
    private String argNodeFilter;
    private String argIncludeNodes;
    private String argExcludeNodes;
    private Boolean argExcludePrecedence;
    /**
     * Long option for filter exclude precedence option
     */
    static final String FILTER_EXCLUDE_PRECEDENCE_LONG = "filter-exclude-precedence";
    public static final String FILTER_INCLUDE = "I";
    public static final String FILTER_EXCLUDE = "X";
    public static final String FILTER_INCLUDE_LONG = "nodes";
    public static final String FILTER_EXCLUDE_LONG = "xnodes";
    public static final String THREADCOUNT = "C";
    public static final String THREADCOUNT_LONG = "threadcount";
    public static final String KEEPGOING = "K";
    public static final String KEEPGOING_LONG = "keepgoing";
    public static final String DONTKEEPGOING = "N";
    public static final String DONTKEEPGOING_LONG = "nokeepgoing";
    public static final String FILTER = "F";
    public static final String FILTER_LONG = "filter";


    /**
     * Create options, specify whether failednodes file option is included
     *
     * @param includeFailednodes if true, include failednodes file option
     */
    public NodeFilterOptions(final boolean includeFailednodes) {
    }

    /**
     * Default constructor, does not configure failednodes file option
     */
    public NodeFilterOptions() {
        this(false);
    }

    public void addOptions(final Options options) {
        options.addOption(THREADCOUNT, THREADCOUNT_LONG, true, "number of threads");
        options.addOption(FILTER, FILTER_LONG, true, "node filter string");
        options.addOption(FILTER_INCLUDE, FILTER_INCLUDE_LONG, true, "include node parameter list (deprecated, use --filter)");
        options.addOption(FILTER_EXCLUDE, FILTER_EXCLUDE_LONG, true, "exclude node parameter list (deprecated, use --filter)");
        options.addOption(KEEPGOING, KEEPGOING_LONG, false, "Continue node dispatch when execution on one node fails");
        options.addOption(DONTKEEPGOING, DONTKEEPGOING_LONG, false,
            "Force early failure if execution on one node fails");
        options.addOption(FILTER_EXCLUDE_PRECEDENCE_OPT,
            FILTER_EXCLUDE_PRECEDENCE_LONG, true,
            "true/false. if true, exclusion filters have precedence over inclusion filters");
    }

    public void parseArgs(final CommandLine cli, final String[] original) {
        if (cli.hasOption(DONTKEEPGOING)) {
            setArgKeepgoing(false);
            argKeepgoingSet = true;
        } else if (cli.hasOption(KEEPGOING)) {
            setArgKeepgoing(true);
            argKeepgoingSet = true;
        }

        if (cli.hasOption(THREADCOUNT)) {
            try {
                setArgThreadCount(Integer.valueOf(cli.getOptionValue(THREADCOUNT)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("threadcount must be an integer");
            }
        }
        setArgNodeFilter(cli.getOptionValue(FILTER));
        final String[] keys = NodeSet.FILTER_KEYS_LIST.toArray(new String[NodeSet.FILTER_KEYS_LIST.size()]);
        excludeMap = parseExcludeArgs(keys, cli);
        includeMap = parseIncludeArgs(keys, cli);
        if (cli.hasOption(FILTER_INCLUDE)) {
            argIncludeNodes = cli.getOptionValue(FILTER_INCLUDE);
        }
        if (cli.hasOption(FILTER_EXCLUDE)) {
            argExcludeNodes = cli.getOptionValue(FILTER_EXCLUDE);
        }
        setArgExcludePrecedence(determineExclusionPrecedenceForArgs(original, cli));
    }

    public void validate(CommandLine cli, String[] original) throws CLIToolOptionsException {
    }

    /**
     * Parse the value of the -X option
     *
     * @param keys keys
     * @param cli cli
     *
     * @return map
     */

    protected Map<String, String> parseExcludeArgs(String[] keys, final CommandLine cli) {
        return parseFilterArgs(keys, cli, FILTER_EXCLUDE);
    }

    /**
     * Parse the value of the -X option.
     *
     * @param keys keys
     * @param cli cli
     *
     * @return map
     */
    protected Map<String, String> parseIncludeArgs(String[] keys, final CommandLine cli) {
        return parseFilterArgs(keys, cli, FILTER_INCLUDE);
    }

    protected static Map<String, String> parseFilterArgs(String[] keys, CommandLine cli, String opt) {
        String[] strings = cli.getOptionValues(opt);
        if (null == strings || strings.length == 0) {
            if (null != cli.getOptionValue(opt)) {
                strings = new String[]{
                    cli.getOptionValue(opt)
                };
            }
        }
        return parseMultiNodeArgs(keys, strings);
    }

    /**
     * Parse the values as key=value pairs, using the set of allowed keys.  If there is only one entry in the values
     * array without a key, then the first key of the allowed keys is used as the default
     *
     * @param keys   allowed keys for the key=value strings, the first key is used as the default key
     * @param values array of key=value strings, or merely 1 value string if the array is size 1
     *
     * @return map of the key to values
     */
    protected static Map<String, String> parseMultiNodeArgs(String[] keys, String[] values) {
        HashMap<String, String> map = new HashMap<String, String>();

        if (null != values && values.length > 0) {
            for (String exclude : values) {
                int i1 = exclude.indexOf("=");
                if (i1 > 0 && i1 <= exclude.length() - 1) {
                    String k = exclude.substring(0, i1);
                    String v = exclude.substring(i1 + 1);
                    map.put(k, v);
                } else if (i1 < 0) {
                    map.put(keys[0], exclude);
                }
            }
        }
        return map;
    }

    /**
     * Create and return a NodeSet representing the input arguments, may be empty.
     *
     * @return nodeset
     */
    public NodeSet getNodeSet() {
        final NodeSet nodeset = new NodeSet();
        final NodeSet.Include include = nodeset.createInclude(includeMap);
        include.setDominant(!isArgExcludePrecedence());
        final NodeSet.Exclude exclude = nodeset.createExclude(excludeMap);
        exclude.setDominant(isArgExcludePrecedence());
        nodeset.setKeepgoing(isArgKeepgoing());
        nodeset.setThreadCount(getArgThreadCount());

        return nodeset;
    }

    /**
     * Return true if a keepgoing option (K or N) has been specified, false otherwise
     *
     * @return true if -K or -N option specified
     */
    public boolean isKeepgoingSet() {
        return argKeepgoingSet;
    }

    /**
     * Return true if exclusion should have precedence in node filter args
     *
     * @param args all commandline args
     * @param cli  parsed CommandLine
     *
     * @return true if --filter-exclusion-precedence is true, or -I is not specified before -X
     */
    static boolean determineExclusionPrecedenceForArgs(String[] args, final CommandLine cli) {
        if (cli.hasOption(FILTER_EXCLUDE_PRECEDENCE_OPT)) {
            return "true".equals(cli.getOptionValue(FILTER_EXCLUDE_PRECEDENCE_OPT));
        } else {
            //determine if -X or -I appears first in args list, and set precendence for first item
            for (int i = 0 ; i < args.length ; i++) {
                String option = args[i];
                if ("-X".equals(option) || "--xnodes".equals(option)) {
                    return true;
                } else if ("-I".equals(option) || "--nodes".equals(option)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getArgIncludeNodes() {
        return argIncludeNodes;
    }

    public String getArgExcludeNodes() {
        return argExcludeNodes;
    }

    public String getArgNodeFilter() {
        return argNodeFilter;
    }

    public void setArgNodeFilter(String argNodeFilter) {
        this.argNodeFilter = argNodeFilter;
    }

    public Boolean isArgExcludePrecedence() {
        return argExcludePrecedence;
    }

    public void setArgExcludePrecedence(boolean argExcludePrecedence) {
        this.argExcludePrecedence = argExcludePrecedence;
    }

    public boolean isArgKeepgoing() {
        return argKeepgoing;
    }

    public void setArgKeepgoing(boolean argKeepgoing) {
        this.argKeepgoing = argKeepgoing;
    }

    public int getArgThreadCount() {
        return argThreadCount;
    }

    public void setArgThreadCount(int argThreadCount) {
        this.argThreadCount = argThreadCount;
    }
}
