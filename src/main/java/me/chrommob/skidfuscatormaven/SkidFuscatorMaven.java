package me.chrommob.skidfuscatormaven;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Mojo(name = "skidfuscate")
public class SkidFuscatorMaven extends AbstractMojo {
    @Parameter(defaultValue = "3")
    public int maxDepth;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    //
    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File basedir;

    @Parameter(defaultValue = "${maven.repo.local}", readonly = true)
    private File mavenRepo;

    private File skidfuscatorFolder;
    private File skidfuscatorJar;
    private File manualLibs;

    @Override
    public void execute() {
        System.out.println("Max depth: " + maxDepth);
        if (mavenRepo == null) {
            mavenRepo = new File(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
        }
        File output = new File(basedir + File.separator + "target");
        skidfuscatorFolder = new File(basedir + File.separator + "skidfuscator");
        skidfuscatorJar = new File(basedir + File.separator + "skidfuscator", "skidfuscator.jar");
        File configFile = new File(basedir + File.separator + "skidfuscator", "config.txt");
        if (!skidfuscatorJar.exists()) {
            throw new RuntimeException("Skidfuscator not found in: " + skidfuscatorJar.getAbsolutePath());
        }
        if (output.listFiles() == null || Objects.requireNonNull(output.listFiles()).length == 0) {
            throw new RuntimeException("No output file to obfuscate.");
        }
        for (File file : Objects.requireNonNull(skidfuscatorFolder.listFiles())) {
            if (!file.getName().equals("skidfuscator.jar") && !file.getName().equals("manualLibs") && !file.getName().equals("config.txt")) {
                deleteDirectory(file);
            }
        }
        Set<File> compileLibs = new HashSet<>();
        DependencyFinder dependencyFinder = new DependencyFinder(mavenRepo, skidfuscatorFolder, maxDepth);
        List<Dependency> dependencies = project.getDependencies();
        for (Dependency dependency : dependencies) {
            if (!dependency.getType().equals("jar"))
                return;
            System.out.println("Found dependency: " + dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
            me.chrommob.skidfuscatormaven.Dependency dep = new me.chrommob.skidfuscatormaven.Dependency(dependencyFinder, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), Collections.singleton("https://repo1.maven.org/maven2/"), 0);
            compileLibs.addAll(dep.getFiles());
        }
        new File(skidfuscatorFolder + File.separator + "libs").mkdirs();
        for (File lib : compileLibs) {
            if (lib == null)
                continue;
            try {
                Files.copy(lib.toPath(), new File(skidfuscatorFolder + File.separator + "libs" + File.separator + lib.getName()).toPath());
            } catch (IOException ignored) {
            }
        }
        manualLibs = new File(skidfuscatorFolder + File.separator + "manualLibs");
        if (manualLibs.listFiles() != null && Objects.requireNonNull(manualLibs.listFiles()).length > 0) {
            for (File lib : Objects.requireNonNull(manualLibs.listFiles())) {
                try {
                    Files.copy(lib.toPath(), new File(skidfuscatorFolder + File.separator + "libs" + File.separator + lib.getName()).toPath());
                } catch (IOException ignored) {
                }
            }
        }
        for (File lib: Objects.requireNonNull(new File(skidfuscatorFolder + File.separator + "libs").listFiles())) {
            if (lib.getName().endsWith(".jar")) {
                try {
                    ZipFile zipFile = new ZipFile(lib);
                    //Check if the jar is a valid jar
                    ZipEntry entry = zipFile.getEntry("annotations");
                    if (entry != null) {
                        System.out.println("Removing " + lib.getName() + " because it is has annotations.");
                        zipFile.close();
                        Files.delete(lib.toPath());
                    }
                    zipFile.close();
                } catch (ZipException e) {
                    System.out.println("Deleting " + lib.getName() + " because it is not a valid jar.");
                    try {
                        Files.delete(lib.toPath());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        for (File outPutFile : Objects.requireNonNull(output.listFiles())) {
            if (!outPutFile.getName().contains(".jar")) {
                continue;
            }
            System.out.println("Obfuscating: " + outPutFile.getName());
            String name = outPutFile.getName().replaceAll(".jar", "");
            File outputFolder = new File(skidfuscatorFolder + File.separator + name);
            outputFolder.mkdirs();
            try {
                Files.copy(outPutFile.toPath(), new File(outputFolder + File.separator + outPutFile.getName()).toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String command = "java -jar " + skidfuscatorJar.getAbsolutePath() + " obfuscate " + outputFolder + File.separator + outPutFile.getName() + " -li=" + new File(skidfuscatorFolder + File.separator + "libs");
            List<String> args = new ArrayList<>(Arrays.asList(command.split(" ")));
            if (configFile.exists()) {
                args.add("-cfg=" + configFile.getAbsolutePath());
            }
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            System.out.println("Running command: " + args);
            Process process;
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Scanner s = new Scanner(process.getInputStream());
            while (s.hasNextLine()) {
                String line = s.nextLine();
                System.out.println(line);
            }
            s.close();

            System.out.println("ERRORS:");

            s = new Scanner(process.getErrorStream());
            while (s.hasNextLine()) {
                String line = s.nextLine();
                System.out.println(line);
            }
            s.close();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Obfuscated: " + outPutFile.getName());
        }
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}