package me.chrommob.skidfuscatormaven;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class Dependency {
    private final DependencyFinder finder;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Set<String> repositories;

    private Set<Dependency> subDependencies;
    private int depth;

    public Dependency(DependencyFinder dependencyFinder, String groupId, String artifactId, String version, Set<String> repositories, int depth) {
        this.finder = dependencyFinder;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.repositories = repositories;
        this.depth = depth;
    }

    public Set<File> getFiles() {
        depth++;
        if (depth >= finder.getMaxDepth()) {
            System.out.println("Max depth reached for " + this);
            return new HashSet<>();
        }
        Set<File> files = new HashSet<>();
        File dep = getFile();
        if (dep == null) {
            return files;
        }
        files.add(dep);
        if (subDependencies == null || subDependencies.isEmpty()) {
            return files;
        }
        for (Dependency dependency : subDependencies) {
            files.addAll(dependency.getFiles());
        }
        return files;
    }

    private File getFile() {
        if (finder.isFound(toString())) {
            return null;
        }
        DependencyResponse response = finder.getDependency(this);
        if (response == null) {
            return null;
        } else {
            finder.addDependency(toString());
            subDependencies = response.getSubDependencies();
            return response.getDependency();
        }

    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public Set<String> getRepositories() {
        return repositories;
    }

    public int getDepth() {
        return depth;
    }
}
