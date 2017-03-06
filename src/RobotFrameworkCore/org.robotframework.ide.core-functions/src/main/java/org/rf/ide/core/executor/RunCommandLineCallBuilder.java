/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.executor;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.rf.ide.core.executor.RobotRuntimeEnvironment.PythonInstallationDirectory;

import com.google.common.base.Joiner;

/**
 * @author Michal Anglart
 */
public class RunCommandLineCallBuilder {

    public static interface IRunCommandLineBuilder {

        public IRunCommandLineBuilder addLocationsToPythonPath(final Collection<String> pythonPathLocations);

        public IRunCommandLineBuilder addLocationsToClassPath(final Collection<String> classPathLocations);

        public IRunCommandLineBuilder addVariableFiles(final Collection<String> varFiles);

        public IRunCommandLineBuilder suitesToRun(final Collection<String> suites);

        public IRunCommandLineBuilder testsToRun(final Collection<String> tests);

        public IRunCommandLineBuilder includeTags(final Collection<String> tags);

        public IRunCommandLineBuilder excludeTags(final Collection<String> tags);

        public IRunCommandLineBuilder addUserArgumentsForInterpreter(final String arguments);

        public IRunCommandLineBuilder addUserArgumentsForRobot(final String arguments);

        public IRunCommandLineBuilder enableDryRun(final boolean shouldEnableDryRun);

        public IRunCommandLineBuilder withProject(final File project);

        public IRunCommandLineBuilder withAdditionalProjectsLocations(
                final Collection<String> additionalProjectsLocations);

        public RunCommandLine build() throws IOException;
    }

    private static class Builder implements IRunCommandLineBuilder {

        private final SuiteExecutor executor;

        private final String executablePath;

        private final int port;

        private final List<String> pythonPath = new ArrayList<>();

        private final List<String> classPath = new ArrayList<>();

        private final List<String> variableFiles = new ArrayList<>();

        private final List<String> suitesToRun = new ArrayList<>();

        private final List<String> testsToRun = new ArrayList<>();

        private final List<String> tagsToInclude = new ArrayList<>();

        private final List<String> tagsToExclude = new ArrayList<>();

        private final List<String> additionalProjectsLocations = new ArrayList<>();

        private File project = null;

        private boolean enableDryRun = false;

        private String robotUserArgs = "";

        private String interpreterUserArgs = "";

        private Builder(final SuiteExecutor executor, final String executablePath, final int port) {
            this.executor = executor;
            this.executablePath = executablePath;
            this.port = port;
        }

        @Override
        public IRunCommandLineBuilder addLocationsToPythonPath(final Collection<String> pythonPathLocations) {
            pythonPath.addAll(pythonPathLocations);
            return this;
        }

        @Override
        public IRunCommandLineBuilder addLocationsToClassPath(final Collection<String> classPathLocations) {
            classPath.addAll(classPathLocations);
            return this;
        }

        @Override
        public IRunCommandLineBuilder addVariableFiles(final Collection<String> varFiles) {
            for (final String varFile : varFiles) {
                variableFiles.add("-V");
                variableFiles.add(varFile);
            }
            return this;
        }

        @Override
        public IRunCommandLineBuilder suitesToRun(final Collection<String> suites) {
            for (final String suite : suites) {
                suitesToRun.add("-s");
                suitesToRun.add(suite);
            }
            return this;
        }

        @Override
        public IRunCommandLineBuilder testsToRun(final Collection<String> tests) {
            for (final String test : tests) {
                testsToRun.add("-t");
                testsToRun.add(test);
            }
            return this;
        }

        @Override
        public IRunCommandLineBuilder includeTags(final Collection<String> tags) {
            for (final String tag : tags) {
                tagsToInclude.add("-i");
                tagsToInclude.add(tag);
            }
            return this;
        }

        @Override
        public IRunCommandLineBuilder excludeTags(final Collection<String> tags) {
            for (final String tag : tags) {
                tagsToExclude.add("-e");
                tagsToExclude.add(tag);
            }
            return this;
        }

        @Override
        public IRunCommandLineBuilder addUserArgumentsForInterpreter(final String arguments) {
            this.interpreterUserArgs = arguments.trim();
            return this;
        }

        @Override
        public IRunCommandLineBuilder addUserArgumentsForRobot(final String arguments) {
            this.robotUserArgs = arguments.trim();
            return this;
        }

        @Override
        public IRunCommandLineBuilder enableDryRun(final boolean shouldEnableDryRun) {
            this.enableDryRun = shouldEnableDryRun;
            return this;
        }

        @Override
        public IRunCommandLineBuilder withProject(final File project) {
            this.project = project;
            return this;
        }

        @Override
        public IRunCommandLineBuilder withAdditionalProjectsLocations(
                final Collection<String> additionalProjectsLocations) {
            this.additionalProjectsLocations.addAll(additionalProjectsLocations);
            return this;
        }

        @Override
        public RunCommandLine build() throws IOException {
            final List<String> cmdLine = new ArrayList<>();

            cmdLine.add(executablePath);
            if (executor == SuiteExecutor.Jython) {
                final String additionalPythonPathLocationForJython = extractAdditionalPythonPathLocationForJython();
                if (!additionalPythonPathLocationForJython.isEmpty()) {
                    cmdLine.add(additionalPythonPathLocationForJython);
                }
                cmdLine.add("-J-cp");
                cmdLine.add(classPath());
            }
            if (!interpreterUserArgs.isEmpty()) {
                cmdLine.add(interpreterUserArgs);
            }
            cmdLine.add("-m");
            cmdLine.add("robot.run");
            if (!pythonPath.isEmpty()) {
                cmdLine.add("-P");
                cmdLine.add(pythonPath());
            }
            cmdLine.addAll(variableFiles);
            cmdLine.addAll(tagsToInclude);
            cmdLine.addAll(tagsToExclude);
            cmdLine.add("--listener");
            cmdLine.add(RobotRuntimeEnvironment.copyScriptFile("TestRunnerAgent.py").toPath() + ":" + port);
            if (enableDryRun) {
                cmdLine.add("--prerunmodifier");
                cmdLine.add(RobotRuntimeEnvironment.copyScriptFile("SuiteVisitorImportProxy.py").toPath().toString());
                cmdLine.add("--runemptysuite");
                cmdLine.add("--dryrun");
                cmdLine.add("--output");
                cmdLine.add("NONE");
                cmdLine.add("--report");
                cmdLine.add("NONE");
                cmdLine.add("--log");
                cmdLine.add("NONE");
            }
            cmdLine.addAll(suitesToRun);
            cmdLine.addAll(testsToRun);
            if (!robotUserArgs.isEmpty()) {
                cmdLine.addAll(ArgumentsConverter
                        .fromJavaArgsToPythonLike(ArgumentsConverter.convertToJavaMainLikeArgs(robotUserArgs)));
            }
            cmdLine.add(project.getAbsolutePath());
            for (final String projectLocation : additionalProjectsLocations) {
                cmdLine.add(projectLocation);
            }
            return new RunCommandLine(cmdLine);
        }

        private String classPath() {
            final String sysPath = System.getenv("CLASSPATH");
            final List<String> wholeClasspath = newArrayList();
            if (sysPath != null && !sysPath.isEmpty()) {
                wholeClasspath.add(sysPath);
            }
            wholeClasspath.addAll(classPath);

            return Joiner.on(RedSystemProperties.getPathsSeparator()).join(wholeClasspath);
        }

        private String pythonPath() {
            return Joiner.on(":").join(pythonPath);
        }

        private String extractAdditionalPythonPathLocationForJython() {
            String additionalPythonPathLocation = "";
            final Path jythonPath = Paths.get(executablePath);
            Path jythonParentPath = jythonPath.getParent();
            if (jythonParentPath == null) {
                final List<PythonInstallationDirectory> pythonInterpreters = RobotRuntimeEnvironment
                        .whereArePythonInterpreters();
                for (final PythonInstallationDirectory pythonInstallationDirectory : pythonInterpreters) {
                    if (pythonInstallationDirectory.getInterpreter() == SuiteExecutor.Jython) {
                        jythonParentPath = pythonInstallationDirectory.toPath();
                        break;
                    }
                }
            }
            if (jythonParentPath != null && jythonParentPath.getFileName() != null
                    && jythonParentPath.getFileName().toString().equalsIgnoreCase("bin")) {
                final Path mainDir = jythonParentPath.getParent();
                final Path sitePackagesDir = Paths.get(mainDir.toString(), "Lib", "site-packages");
                additionalPythonPathLocation = "-J-Dpython.path=" + sitePackagesDir.toString(); // in case of 'robot' folder existing in project
            }
            return additionalPythonPathLocation;
        }
    }

    public static IRunCommandLineBuilder forEnvironment(final RobotRuntimeEnvironment env, final int port) {
        return new Builder(env.getInterpreter(), env.getPythonExecutablePath(), port);
    }

    public static IRunCommandLineBuilder forExecutor(final SuiteExecutor executor, final int port) {
        return new Builder(executor, executor.executableName(), port);
    }

    public static class RunCommandLine {

        private final List<String> commandLine;

        RunCommandLine(final List<String> commandLine) {
            this.commandLine = new ArrayList<>(commandLine);
        }

        public String[] getCommandLine() {
            return commandLine.toArray(new String[0]);
        }

        public String show() {
            return Joiner.on(' ').join(commandLine);
        }
    }
}
