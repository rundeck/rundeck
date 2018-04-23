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

package com.dtolabs.rundeck.core.authorization.providers.yaml.model;

import java.util.*;

/**
 * @author greg
 * @since 3/20/17
 */
public class ACLPolicyDoc {
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public By getBy() {
        return by;
    }

    public void setBy(By by) {
        this.by = by;
    }

    public Map<String, List<TypeRule>> getFor() {
        return forSection;
    }

    public void setFor(Map<String, List<TypeRule>> forSection) {
        this.forSection = forSection;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public static class Context {
        private String project;
        private String application;

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        @Override
        public String toString() {
            return "Context{" +
                   (null != project ? "project:'" + project + '\'' : "") +
                   (null != application ? ", application:'" + application + '\'' : "") +
                   '}';
        }
    }

    public static class For {
        private List<TypeRule> job;
        private List<TypeRule> resource;
        private List<TypeRule> node;
        private List<TypeRule> user;
        private List<TypeRule> adhoc;
        private List<TypeRule> project;
        private List<TypeRule> apitoken;
        private List<TypeRule> type;

        public List<TypeRule> getJob() {
            return job;
        }

        public void setJob(List<TypeRule> job) {
            this.job = job;
        }

        public List<TypeRule> getResource() {
            return resource;
        }

        public void setResource(List<TypeRule> resource) {
            this.resource = resource;
        }

        public List<TypeRule> getNode() {
            return node;
        }

        public void setNode(List<TypeRule> node) {
            this.node = node;
        }

        public List<TypeRule> getUser() {
            return user;
        }

        public void setUser(List<TypeRule> user) {
            this.user = user;
        }

        public List<TypeRule> getAdhoc() {
            return adhoc;
        }

        public void setAdhoc(List<TypeRule> adhoc) {
            this.adhoc = adhoc;
        }

        public List<TypeRule> getProject() {
            return project;
        }

        public void setProject(List<TypeRule> project) {
            this.project = project;
        }

        public List<TypeRule> getApitoken() {
            return apitoken;
        }

        public void setApitoken(List<TypeRule> apitoken) {
            this.apitoken = apitoken;
        }

        public Map<String, List<TypeRule>> getRuleSets() {
            Map<String, List<TypeRule>> rules = new HashMap<>();
            if (null != job) {
                rules.put("job", job);
            }
            if (null != resource) {
                rules.put("resource", resource);
            }
            if (null != node) {
                rules.put("node", node);
            }
            if (null != user) {
                rules.put("user", user);
            }
            if (null != adhoc) {
                rules.put("adhoc", adhoc);
            }
            if (null != project) {
                rules.put("project", project);
            }
            if (null != apitoken) {
                rules.put("apitoken", apitoken);
            }
            if (null != type) {
                rules.put("type", type);
            }
            return rules;
        }


        public List<TypeRule> getType() {
            return type;
        }

        public void setType(List<TypeRule> type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "For{" +
                   (job != null ? "job=" + job : "") +
                   (resource != null ? ", resource=" + resource : "") +
                   (node != null ? ", node=" + node : "") +
                   (user != null ? ", user=" + user : "") +
                   (adhoc != null ? ", adhoc=" + adhoc : "") +
                   (project != null ? ", project=" + project : "") +
                   (apitoken != null ? ", apitoken=" + apitoken : "") +
                   (type != null ? ", type=" + type : "") +
                   '}';
        }

        public boolean isEmpty() {
            return getRuleSets().isEmpty();
        }
    }

    public static class TypeRule {
        private Map<String, Object> equals;
        private Map<String, Object> contains;
        private Map<String, Object> subset;
        private Map<String, Object> match;
        private Object allow;
        private Object deny;

        public Map<String, Object> getEquals() {
            return equals;
        }

        public void setEquals(Map<String, Object> equals) {
            this.equals = equals;
        }

        public Map<String, Object> getContains() {
            return contains;
        }

        public void setContains(Map<String, Object> contains) {
            this.contains = contains;
        }

        public Map<String, Object> getSubset() {
            return subset;
        }

        public void setSubset(Map<String, Object> subset) {
            this.subset = subset;
        }

        public Map<String, Object> getMatch() {
            return match;
        }

        public void setMatch(Map<String, Object> match) {
            this.match = match;
        }

        public Object getAllow() {
            return allow;
        }

        public void setAllow(Object allow) {
            this.allow = allow;
        }

        public Object getDeny() {
            return deny;
        }

        public void setDeny(Object deny) {
            this.deny = deny;
        }

        public Set<String> getAllowActions() {
            return getValue(allow);
        }

        private Set<String> getValue(final Object ruleobj) {
            final HashSet<String> actions = new HashSet<>();
            if (ruleobj instanceof String) {
                final String actionStr = (String) ruleobj;
                actions.add(actionStr);
            } else if (ruleobj instanceof List) {
                actions.addAll((List<String>) ruleobj);
            } else {
                return null;
            }
            return actions;
        }

        boolean validRule(final Object ruleobj){
            return ruleobj instanceof String || ruleobj instanceof List;
        }

        public boolean validDeny() {
            return null == deny || validRule(deny);
        }

        public boolean validAllow() {
            return null == allow || validRule(allow);
        }
        public Set<String> getDenyActions() {
            return getValue(deny);
        }


        @Override
        public String toString() {
            return "Rule{" +
                   (equals != null ? " equals=" + equals : "") +
                   (contains != null ? " contains=" + contains : "") +
                   (subset != null ? " subset=" + subset : "") +
                   (match != null ? " match=" + match : "") +
                   (allow != null ? " allow=" + allow : "") +
                   (deny != null ? " deny=" + deny : "") +
                   '}';
        }

        public boolean hasresource() {
            return notEmpty(equals) || notEmpty(contains) || notEmpty(subset) || notEmpty(match);
        }

        public boolean isEmpty() {
            return !hasActions();
        }

        private boolean hasActions() {
            return notEmpty(getAllowActions()) || notEmpty(getDenyActions());
        }

        private boolean isEmpty(final Map val) {
            return null != val && val.size() == 0;
        }
        private boolean notEmpty(final Map val) {
            return null != val && val.size() > 0;
        }

        private boolean notEmpty(final Collection val) {
            return null != val && val.size() > 0;
        }
    }


    public static class By {
        private Object username;
        private Object group;

        public Object getUsername() {
            return username;
        }

        public void setUsername(Object username) {
            this.username = username;
        }

        public Object getGroup() {
            return group;
        }

        public void setGroup(Object group) {
            this.group = group;
        }

        @Override
        public String toString() {
            return "By{" +
                   "username=" + username +
                   ", group=" + group +
                   '}';
        }
    }


    private Context context;
    private String description;
    //    private For forSection;
    private Map<String, List<TypeRule>> forSection;
    private By by;
    private String id;

    @Override
    public String toString() {
        return "Policy{" +
               "context=" + context +
               ", description='" + description + '\'' +
               ", for=" + forSection +
               ", by=" + by +
               ", id='" + id + '\'' +
               '}';
    }
}
