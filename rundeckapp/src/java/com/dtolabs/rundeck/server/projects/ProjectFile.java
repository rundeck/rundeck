package com.dtolabs.rundeck.server.projects;

/**
 * Created by greg on 3/18/16.
 */
public class ProjectFile {
    private String project;
    private String path;

    public static ProjectFile of(final String project, final String path) {
        return new ProjectFile(project, path);
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private ProjectFile(final String project, final String path) {
        this.project = project;
        this.path = path;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ProjectFile that = (ProjectFile) o;

        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        return path != null ? path.equals(that.path) : that.path == null;

    }

    @Override
    public int hashCode() {
        int result = project != null ? project.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
