package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.utils.Converter;
import org.apache.commons.collections.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A list of commandline arguments, with flags to indicate quoting.
 */
public class ExecArgList {
    List<ExecArg> args = new ArrayList<ExecArg>();

    private ExecArgList() {
    }

    private void addArg(ExecArg arg) {
        args.add(arg);
    }

    private void addArg(String arg, boolean quoted) {
        args.add(ExecArg.fromString(arg, quoted));
    }

    private void addArgs(List<String> arg, boolean quoted) {
        for (String s : arg) {
            addArg(s, quoted);
        }
    }

    public static ExecArgList fromStrings(boolean quoted, String... args) {
        return fromStrings(Arrays.asList(args), quoted);
    }

    /**
     * @return an ExecArgList from a list of strings, and a predicate to determine whether the argument needs to be
     * quoted
     *
     * @param quoteDetect predicate
     * @param args args
     */
    public static ExecArgList fromStrings(Predicate quoteDetect, String... args) {
        return builder().args(args, quoteDetect).build();
    }

    /**
     * @return Create an ExecArgList from a list of strings
     *
     * @param strings the strings
     * @param quoted  whether they are each quoted
     */
    public static ExecArgList fromStrings(List<String> strings, boolean quoted) {
        return builder().args(strings, quoted).build();
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
        final ArrayList<String> strings = new ArrayList<String>();
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
    public ArrayList<String> buildCommandForNode(Map<String, Map<String, String>> dataContext, String osFamily) {
        return buildCommandForNode(this, dataContext, osFamily);
    }

    private static ArrayList<String> buildCommandForNode(ExecArgList command, Map<String, Map<String,
            String>> dataContext,
            String osFamily) {
        final Converter<String, String> quote = CLIUtils.argumentQuoteForOperatingSystem(osFamily);
        final Converter<String, String> expand = DataContextUtils.replaceDataReferencesConverter(dataContext,
                new Converter<String, String>() {
                    @Override
                    public String convert(String s) {
                        if (s.startsWith("${option.") && s.endsWith("}")) {
                            return "";
                        }else{
                            return s;
                        }
                    }
                },
                false);

        final ArrayList<String> commandList = new ArrayList<String>();
        CommandVisitor visiter = new CommandVisitor(commandList, quote, expand);
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

        public String convertAndQuote(String s, boolean quoted) {
            String replaced = expand.convert(s);
            if (quote != null) {
                if (quoted || !replaced.equals(s)) {
                    replaced = quote.convert(replaced);
                }
            }
            return replaced;
        }

        @Override
        public void visit(ExecArg arg) {
            if (arg.isList()) {
                CommandVisitor commandVisitor = new CommandVisitor(new ArrayList<String>(), quote, expand);
                for (ExecArg execArg : arg.getList()) {
                    execArg.accept(commandVisitor);
                }
                String join = joinAndQuote(commandVisitor.getCommandList(), arg.isQuoted() ? quote : null);
                getCommandList().add(join);
            } else {
                getCommandList().add(convertAndQuote(arg.getString(), arg.isQuoted()));
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
         * @param arg    argument
         * @param quoted true if it needs to be quoted
         *
         * @return builder
         */
        public Builder arg(String arg, boolean quoted) {
            argList.addArg(arg, quoted);
            return this;
        }

        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         *
         * @return builder
         */
        public Builder args(List<String> args, boolean quoted) {
            argList.addArgs(args, quoted);
            return this;
        }
        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         *
         * @return builder
         */
        public Builder args(List<String> args, Predicate quoted) {
            for (String arg : args) {
                argList.addArg(arg, quoted.evaluate(arg));
            }
            return this;
        }

        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         *
         * @return builder
         */
        public Builder args(String[] args, Predicate quoted) {
            return args(Arrays.asList(args), quoted);
        }

        /**
         * Add a list of args
         *
         * @param args   args
         * @param quoted true if all should be quoted
         *
         * @return builder
         */
        public Builder args(String[] args, boolean quoted) {
            argList.addArgs(Arrays.asList(args), quoted);
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
