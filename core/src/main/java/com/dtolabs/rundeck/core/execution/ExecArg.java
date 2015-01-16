package com.dtolabs.rundeck.core.execution;

import java.util.List;

/**
 * Represents a command-line argument, that may need to be quoted/escaped before use, and may contain a list of other
 * arguments to be quoted.
 */
public abstract class ExecArg {

    /**
     * @return true if this arg contains a list of other args
     *
     */
    public abstract boolean isList();

    private boolean quoted = true;

    /**
     * @return the sublist of args, if {@link #isList()} returns true, null otherwise
     */
    public abstract List<ExecArg> getList();

    /**
     * @return the string value of this argument if {@link #isList()} returns false, null otherwise
     */
    public abstract String getString();

    /**
     * Accept a visitor
     *
     * @param converter visitor
     */
    public abstract void accept(Visitor converter);

    /**
     * @return true if this arg should be quoted
     */
    public boolean isQuoted() {
        return quoted;
    }

    /**
     * Set whether this arg should be quoted
     *
     * @param quoted quoted
     */
    void setQuoted(boolean quoted) {
        this.quoted = quoted;
    }

    /**
     * Visitor to visit args
     */
    public static interface Visitor {
        public void visit(ExecArg arg);
    }

    /**
     * Represents a single argument string
     */
    static class StringArg extends ExecArg {

        final String arg;

        public StringArg(String arg, boolean quoted) {
            this.arg = arg;
            setQuoted(quoted);
        }


        @Override
        public String toString() {
            return arg;
        }

        @Override
        public String getString() {
            return arg;
        }

        @Override
        public boolean isList() {
            return false;
        }

        @Override
        public List<ExecArg> getList() {
            return null;
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Represents a sub list of arguments
     */
    static class ListArg extends ExecArg {

        private ExecArgList args;

        ListArg() {
            args = null;
        }


        @Override
        public boolean isList() {
            return true;
        }

        @Override
        public String getString() {
            return null;
        }

        @Override
        public List<ExecArg> getList() {
            return getArgs().getList();
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }

        protected ExecArgList getArgs() {
            return args;
        }

        protected void setArgs(ExecArgList list) {
            this.args = list;
        }

        @Override
        public String toString() {
            return "[" +
                    args +
                    ']';
        }
    }

    public static ExecArg fromString(String arg, boolean quoted) {
        return new StringArg(arg, quoted);
    }
}
