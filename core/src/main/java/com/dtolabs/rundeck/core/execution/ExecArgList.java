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

package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.utils.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A list of commandline arguments, with flags to indicate quoting.
 */
public class ExecArgList {
    List<ExecArg> args = new ArrayList<>();

    private ExecArgList() {
    }

    private void addArg(ExecArg arg) {
        args.add(arg);
    }

    private void addArg(String arg, boolean quoted, boolean featureQuotingBackwardCompatible) {
        args.add(ExecArg.fromString(arg, quoted, featureQuotingBackwardCompatible));
    }

    private void addArgs(List<String> arg, boolean quoted, boolean featureQuotingBackwardCompatible) {
        for (String s : arg) {
            addArg(s, quoted, featureQuotingBackwardCompatible);
        }
    }

    public static ExecArgList fromStrings(boolean featureQuotingBackwardCompatible, boolean quoted, String... args) {
        return fromStrings(featureQuotingBackwardCompatible, Arrays.asList(args), quoted);
    }

    /**
     * @return an ExecArgList from a list of strings, and a predicate to determine whether the argument needs to be
     * quoted
     *
     * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
     * @param quoteDetect predicate
     * @param args args
     */
    public static ExecArgList fromStrings(boolean featureQuotingBackwardCompatible, Predicate quoteDetect, String... args) {
        return builder().args(args, quoteDetect, featureQuotingBackwardCompatible).build();
    }

    /**
     * @return an ExecArgList from a list of strings, and a predicate to determine whether the argument needs to be
     * quoted
     *
     * @param quoteDetect predicate
     * @param args args
     */
    public static ExecArgList fromStrings(Predicate quoteDetect, String... args) {
        return fromStrings(false, quoteDetect, args);
    }

    /**
     * @return Create an ExecArgList from a list of strings
     *
     * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
     * @param strings the strings
     * @param quoted  whether they are each quoted
     */
    public static ExecArgList fromStrings(boolean featureQuotingBackwardCompatible, List<String> strings, boolean quoted) {
        return builder().args(strings, quoted, featureQuotingBackwardCompatible).build();
    }

    /**
     * @return the list of ExecArg objects
     */
    public List<ExecArg> getList() {
        return args;
    }

    /**
     * @return Flatten the list of arguments into an array
     */
    public String[] asFlatStringArray() {
        ArrayList<String> strings = asFlatStringList();
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * @return Flatten the list of arguments into a list
     */
    public ArrayList<String> asFlatStringList() {
        final ArrayList<String> strings = new ArrayList<>();
        for (ExecArg arg : getList()) {
            arg.accept(new ExecArg.Visitor() {
                @Override
                public void visit(ExecArg arg) {
                    if (arg.isList()) {
                        for (ExecArg execArg : arg.getList()) {
                            execArg.accept(this);
                        }
                    } else {
                        strings.add(arg.getString());
                    }
                }
            });
        }
        return strings;
    }

    /**
     * @return Join a list of strings and then quote the entire string, if specified
     *
     * @param commandList1 list of commands
     * @param quote quote converter
     */
    public static String joinAndQuote(List<String> commandList1, Converter<String, String> quote) {
        String join = DataContextUtils.join(commandList1, " ");
        if (null != quote) {
            join = quote.convert(join);
        }
        return join;
    }

    /**
     * Generate the quoted and expanded argument list, by expanding property values given the data context, and quoting
     * for the given OS
     *
     * @param dataContext property value data context
     * @param osFamily    OS family to determine quoting
     *
     * @return list of strings
     */
    @Deprecated
    public ArrayList<String> buildCommandForNode(Map<String, Map<String, String>> dataContext, String osFamily) {
        return buildCommandForNode(this, dataContext, osFamily);
    }

    private static ArrayList<String> buildCommandForNode(
            ExecArgList command,
            Map<String, Map<String, String>> dataContext,
            String osFamily
    )
    {
        final Converter<String, String> quote = CLIUtils.argumentQuoteForOperatingSystem(osFamily);
        final Converter<String, String> expand = DataContextUtils.replaceDataReferencesConverter(
                dataContext,
                DataContextUtils.replaceMissingOptionsWithBlank,
                false
        );

        final ArrayList<String> commandList = new ArrayList<>();
        CommandVisitor visiter = new CommandVisitor(commandList, quote, expand);
        command.visitWith(visiter);
        return commandList;
    }

    /**
     * Generate the quoted and expanded argument list, by expanding property values given the data context, and quoting
     * for the given OS or escaping characters when the command interpreter is specified as CMD.
     *
     * @param sharedContext       property value data context
     * @param nodeName            node name
     * @param osFamily            OS family to determine quoting
     * @param commandInterpreter  command interpreter to use for quoting
     *
     * @return list of strings
     */
    public ArrayList<String> buildCommandForNode(
            MultiDataContext<ContextView, DataContext> sharedContext,
            String nodeName, String osFamily, String commandInterpreter
    )
    {
        return buildCommandForNode(this, sharedContext, nodeName, osFamily, commandInterpreter);
    }

    /**
     * Generate the quoted and expanded argument list, by expanding property values given the data context, and quoting
     * for the given OS or escaping characters when the command interpreter is specified as CMD.
     *
     * @param command             ExecArgList to build
     * @param sharedContext       property value data context
     * @param nodeName            node name
     * @param osFamily            OS family to determine quoting
     * @param commandInterpreter  command interpreter to use for quoting
     *
     * @return list of strings
     */
    private static ArrayList<String> buildCommandForNode(
            ExecArgList command,
            MultiDataContext<ContextView, DataContext> sharedContext,
            String nodeName,
            String osFamily,
            String commandInterpreter
    )
    {

        final ArrayList<String> commandList = new ArrayList<>();
        CommandVisitor visiter = new CommandVisitor(
                commandList,
                CLIUtils.argumentQuoteForOperatingSystem(osFamily, commandInterpreter),
                str -> SharedDataContextUtils.replaceDataReferences(
                        str,
                        sharedContext,
                        //add node name to qualifier to read node-data first
                        ContextView.node(nodeName),
                        ContextView::nodeStep,
                        DataContextUtils.replaceMissingOptionsWithBlank,
                        false,
                        false
                )
        );
        command.visitWith(visiter);
        return commandList;
    }

    /**
     * Visits ExecArgs and populates a list
     */
    private static class CommandVisitor implements ExecArg.Visitor {
        private final ArrayList<String> commandList;
        final Converter<String, String> quote;
        final Converter<String, String> expand;

        private CommandVisitor(ArrayList<String> commandList, Converter<String, String> quote, Converter<String,
                String> expand) {
            this.commandList = commandList;
            this.quote = quote;
            this.expand = expand;
        }

        public String convertAndQuote(String s, boolean quoted, boolean featureQuotingBackwardCompatible) {
            String replaced = expand.convert(s);
            if (quote != null && quoted || featureQuotingBackwardCompatible && !replaced.equals(s)) {
                replaced = quote.convert(replaced);
            }
            return replaced;
        }

        @Override
        public void visit(ExecArg arg) {
            if (arg.isList()) {
                CommandVisitor commandVisitor = new CommandVisitor(new ArrayList<>(), quote, expand);
                for (ExecArg execArg : arg.getList()) {
                    execArg.accept(commandVisitor);
                }
                String join = joinAndQuote(commandVisitor.getCommandList(), arg.isQuoted() ? quote : null);
                getCommandList().add(join);
            } else {
                getCommandList().add(convertAndQuote(arg.getString(), arg.isQuoted(), arg.isFeatureQuotingBackwardCompatible()));
            }
        }


        public ArrayList<String> getCommandList() {
            return commandList;
        }
    }

    /**
     * @return a Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Visit with a visitor
     *
     * @param visitor visitor
     */
    public void visitWith(ExecArg.Visitor visitor) {
        for (ExecArg arg : getList()) {
            arg.accept(visitor);
        }
    }

    /**
     * ExecArg.List subclass that is built from a builder
     */
    private static class ArgListBuilder extends ExecArg.ListArg {
        private Builder builder;
        boolean built = false;

        private ArgListBuilder(Builder builder) {
            this.builder = builder;
        }

        private void finishBuild() {
            if (!built) {
                setArgs(builder.build());
                built = true;
            }
        }

        @Override
        protected ExecArgList getArgs() {
            finishBuild();
            return super.getArgs();
        }
    }

    /**
     * Builder class
     */
    public static class Builder {
        private ExecArgList argList;
        private Builder parent;

        public Builder() {
            argList = new ExecArgList();
        }

        /**
         * Add a string arg
         *
         * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
         * @param arg    argument
         * @param quoted true if it needs to be quoted
         *
         * @return builder
         */
        public Builder arg(String arg, boolean quoted, boolean featureQuotingBackwardCompatible) {
            argList.addArg(arg, quoted, featureQuotingBackwardCompatible);
            return this;
        }

        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
         *
         * @return builder
         */
        public Builder args(List<String> args, boolean quoted, boolean featureQuotingBackwardCompatible) {
            argList.addArgs(args, quoted, featureQuotingBackwardCompatible);
            return this;
        }
        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
         *
         * @return builder
         */
        public Builder args(List<String> args, Predicate quoted, boolean featureQuotingBackwardCompatible) {
            for (String arg : args) {
                argList.addArg(arg, quoted.test(arg), featureQuotingBackwardCompatible);
            }
            return this;
        }

        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
         *
         * @return builder
         */
        public Builder args(String[] args, Predicate quoted, boolean featureQuotingBackwardCompatible) {
            return args(Arrays.asList(args), quoted, featureQuotingBackwardCompatible);
        }

        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         * @param featureQuotingBackwardCompatible indicates whether to use old quoting behavior < 3.4.1
         *
         * @return builder
         */
        public Builder args(String[] args, boolean quoted, boolean featureQuotingBackwardCompatible) {
            argList.addArgs(Arrays.asList(args), quoted, featureQuotingBackwardCompatible);
            return this;
        }

        /**
         * Start a buidler for a sublist of args
         *
         * @param quoted true if the list should be quoted
         *
         * @return a builder for a sublist
         */
        public Builder subList(boolean quoted) {
            Builder subbuild = new Builder();
            ArgListBuilder arg = new ArgListBuilder(subbuild);
            arg.setQuoted(quoted);
            argList.addArg(arg);
            subbuild.parent = this;
            return subbuild;
        }

        /**
         * Return the parent builder from a sublist builder.
         *
         * @return parent of this sublist
         */
        public Builder parent() {
            if (null != parent) {
                return parent;
            }
            throw new IllegalStateException("no parent builder");
        }

        /**
         * @return Build the ExecArgList
         */
        public ExecArgList build() {
            return argList;
        }
    }

    @Override
    public String toString() {
        return asFlatStringList().toString();
    }
}
