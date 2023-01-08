package me.chrommob.skidfuscatormaven;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mojo(name = "skidfuscate", defaultPhase = LifecyclePhase.PACKAGE)
public class SkidFuscatorMaven extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;
//
    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File basedir;

    @Parameter(defaultValue = "${maven.repo.local}", readonly = true)
    private File mavenRepo;

    @Override
    public void execute() {
        if (mavenRepo == null) {
            mavenRepo = new File(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
        }
        File output = new File(basedir + File.separator + "target");
        File skidfuscatorFolder = new File(basedir + File.separator + "skidfuscator");
        File skidfuscatorJar = new File(basedir +  File.separator + "skidfuscator", "skidfuscator.jar");
        File manualLibs = new File(skidfuscatorFolder + File.separator + "manualLibs");
        if (!skidfuscatorJar.exists()) {
            throw new RuntimeException("Skifuscator not found in: " + skidfuscatorJar.getAbsolutePath());
        }
        if (output.listFiles() == null || Objects.requireNonNull(output.listFiles()).length == 0) {
            throw  new RuntimeException("No output file to obfuscate.");
        }
        for (File file : Objects.requireNonNull(skidfuscatorFolder.listFiles())) {
            if (!file.getName().equals("skidfuscator.jar") && !file.getName().equals("manualLibs")) {
                deleteDirectory(file);
            }
        }
        Set<File> compileLibs = new HashSet<>();
        List<Dependency> dependencies = project.getDependencies();
        for (Dependency dependency : dependencies) {
            if (!dependency.getType().equals("jar"))
                return;
            compileLibs.add(getJar(dependency, mavenRepo));
        }
        if (manualLibs.listFiles() != null) {
            compileLibs.addAll(Arrays.asList(Objects.requireNonNull(manualLibs.listFiles())));
        }
        new File(skidfuscatorFolder + File.separator + "libs").mkdirs();
        for (File lib : compileLibs) {
            try {
                Files.copy(lib.toPath(), new File(skidfuscatorFolder + File.separator + "libs" + File.separator + lib.getName()).toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (File outPutFile : Objects.requireNonNull(output.listFiles())) {
            if (!outPutFile.getName().contains(".jar")) {
                continue;
            }
            String name = outPutFile.getName().replaceAll(".jar", "");
            File outputFolder = new File(skidfuscatorFolder + File.separator + name);
            outputFolder.mkdirs();
            try {
                Files.copy(outPutFile.toPath(), new File(outputFolder + File.separator + outPutFile.getName()).toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Obfuscating: " + outPutFile.getName());
            Process process;
            try {
                process = Runtime.getRuntime().exec("java -jar " + skidfuscatorJar.getAbsolutePath() + " " + outputFolder + File.separator + outPutFile.getName() + " -li=" + new File(skidfuscatorFolder + File.separator + "libs"), new String[0], outputFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("File successfully obfuscated.");
        }
    }
    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private File getJar(Dependency dependency, File mavenFolder) {
        String[] split = dependency.getGroupId().split("\\.");
        File file = mavenFolder;
        for (String s : split) {
            file = new File(file + File.separator + s);
        }
        file = new File(file + File.separator + dependency.getArtifactId() + File.separator + dependency.getVersion());
        File[] files = file.listFiles();
        if (files == null) {
            throw new RuntimeException("Could not find dependency: " + dependency);
        }
        for (File f : files) {
            if (f.getName().endsWith(".jar")) {
                return f;
            }
        }
        throw new RuntimeException("Could not find dependency: " + dependency);
    }
}