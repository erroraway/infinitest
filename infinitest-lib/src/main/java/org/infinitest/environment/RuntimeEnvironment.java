/*
 * Infinitest, a Continuous Test Runner.
 *
 * Copyright (C) 2010-2013
 * "Ben Rady" <benrady@gmail.com>,
 * "Rod Coffin" <rfciii@gmail.com>,
 * "Ryan Breidenbach" <ryan.breidenbach@gmail.com>
 * "David Gageot" <david@gageot.net>, et al.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.infinitest.environment;

import static com.google.common.collect.Lists.newArrayList;
import static java.io.File.pathSeparator;
import static java.io.File.separator;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import static org.infinitest.util.InfinitestUtils.findClasspathEntryFor;
import static org.infinitest.util.InfinitestUtils.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.infinitest.classloader.ClassPathFileClassLoader;
import org.infinitest.testrunner.TestRunnerProcess;
import org.infinitest.util.InfinitestUtils;

import com.google.common.annotations.VisibleForTesting;

/**
 * Defines the runtime environment for test execution.
 * 
 * @author bjrady
 */
public class RuntimeEnvironment implements ClasspathProvider {
	
	public static final class MissingInfinitestRunnerException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public MissingInfinitestRunnerException(String classpath) {
			super("Could not find infinitest runner in classpath:\n" + classpath);
		}
	}
	
	public static final class MissingInfinitestClassLoaderException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public MissingInfinitestClassLoaderException(String classpath) {
			super("Could not find infinitest class loader in classpath:\n" + classpath);
		}
	}


	public static class JavaHomeException extends RuntimeException {
		private static final long serialVersionUID = -1L;

		JavaHomeException(File javaHome) {
			super("Could not find java executable at " + javaHome.getAbsolutePath());
		}
	}

	private final int heapSize = 256;
	private final File javaHome;
	private final File workingDirectory;
	private final List<File> classOutputDirs;
	private final String projectUnderTestClassPath;
	private final List<String> additionalArgs;
	private final String runnerProcessClassPath;
	private List<File> classDirs;
	private final CustomJvmArgumentsReader customArgumentsReader;
	private final String runnerBootstrapClassPath;

	/**
	 * Creates a new environment for test execution.
	 * 
	 * @param javaHome
	 *            The location of the JDK home directory, similar to the JAVA_HOME
	 *            environment variable.
	 * @param workingDirectory
	 *            The "Current Directory" used to resolve relative paths when
	 *            running tests
	 * @param runnerBootstrapClassPath
	 *            The classpath used for loading the bootstrap classloader (should
	 *            be infinitest-classloader.jar)
	 * @param runnerProcessClassPath
	 *            The classpath containing the Test Runner classpath (should be
	 *            infinitest-runner.jar)
	 * @param classOutputDirs
	 *            A list of class directories containing classes generated by an IDE
	 *            or compiler that should be monitored for changes
	 * @param projectUnderTestClassPath
	 *            The classpath used to launch the tests. It must include the test
	 *            classes and their dependencies (including directories in
	 *            classOutputDirs). it must not include infinitest itself.
	 */
	public RuntimeEnvironment(File javaHome, File workingDirectory, String runnerBootstrapClassPath,
			String runnerProcessClassPath, List<File> classOutputDirs, String projectUnderTestClassPath) {
		this.classOutputDirs = classOutputDirs;
		this.workingDirectory = workingDirectory;
		this.javaHome = javaHome;
		this.runnerBootstrapClassPath = runnerBootstrapClassPath;
		this.runnerProcessClassPath = runnerProcessClassPath;
		this.projectUnderTestClassPath = projectUnderTestClassPath;
		additionalArgs = new ArrayList<>();
		customArgumentsReader = new FileCustomJvmArgumentReader(workingDirectory);
	}

	public List<String> createProcessArguments(ClasspathArgumentBuilder classpathArgumentBuilder) {
		String memorySetting = "-mx" + getHeapSize() + "m";
		List<String> args = new ArrayList<>();
		args.add(getJavaExecutable());
		args.add(memorySetting);
		args.addAll(additionalArgs);
		args.addAll(classpathArgumentBuilder.buildArguments());
		args.addAll(addCustomArguments());
		return args;
	}

	public Map<String, String> createProcessEnvironment() {
		Map<String, String> environment = new HashMap<>();
		// Put only Infinitest runner jar in classpath just to be able to load
		// org.infinitest.testrunner.TestRunnerProcessClassLoader
		environment.put("CLASSPATH", getRunnerBootstrapClassPath());
		return environment;
	}

	private List<String> addCustomArguments() {
		return customArgumentsReader.readCustomArguments();
	}

	@Override
	public String getRunnerFullClassPath() {
		String infinitestJarPath = findInfinitestRunnerJar();
		log(CONFIG, "Found infinitest jar classpath entry at " + infinitestJarPath);
		String runnerFullClassPath = projectUnderTestClassPath + File.pathSeparator + infinitestJarPath;
		validateClasspath(runnerFullClassPath);
		return runnerFullClassPath;
	}

	@VisibleForTesting
	String getRunnerBootstrapClassPath() {
		String entry = findClasspathEntryFor(runnerBootstrapClassPath, ClassPathFileClassLoader.class);
		if (entry == null) {
			throw new MissingInfinitestClassLoaderException(runnerBootstrapClassPath);
		}
		return entry;
	}

	@VisibleForTesting
	String findInfinitestRunnerJar() {
		String runnerClasspathEntry = findClasspathEntryFor(runnerProcessClassPath, TestRunnerProcess.class);
		if (runnerClasspathEntry == null) {
			throw new MissingInfinitestRunnerException(runnerProcessClassPath);
		}
		return runnerClasspathEntry;
	}


	private void validateClasspath(String completeClasspath) {
		for (String entry : getClasspathEntries(completeClasspath)) {
			if (!(new File(getWorkingDirectory(), entry).exists() || new File(entry).exists())) {
				log(WARNING, "Could not find classpath entry [" + entry + "] at file system root or relative to "
						+ "working directory [" + getWorkingDirectory() + "].");
			}
		}
	}

	private List<String> getClasspathEntries(String classpath) {
		return newArrayList(classpath.split(pathSeparator));
	}

	private String getJavaExecutable() {
		File javaExecutable = createJavaExecutableFile("java");
		if (!javaExecutable.exists()) {
			javaExecutable = createJavaExecutableFile("java.exe");
			if (!javaExecutable.exists()) {
				throw new JavaHomeException(javaExecutable);
			}
		}
		return javaExecutable.getAbsolutePath();
	}

	private File createJavaExecutableFile(String fileName) {
		File javaExecutable = new File(javaHome.getAbsolutePath() + separator + "bin" + separator + fileName);
		return javaExecutable;
	}

	/**
	 * The heap size, in megabytes, that will be used when launching the test runner
	 * process.
	 */
	public int getHeapSize() {
		return heapSize;
	}

	/**
	 * The working directory that will be used when launching the test runner
	 * process. That is, if a test run by the core creates a new File object like:
	 * <code>
	 * new File(".");
	 * </code> It will be equal to this directory
	 */
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public void addVMArgs(List<String> newArgs) {
		additionalArgs.addAll(newArgs);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RuntimeEnvironment) {
			RuntimeEnvironment other = (RuntimeEnvironment) obj;
			return other.classOutputDirs.equals(classOutputDirs) && other.workingDirectory.equals(workingDirectory)
					&& other.projectUnderTestClassPath.equals(projectUnderTestClassPath)
					&& other.javaHome.equals(javaHome) && other.additionalArgs.equals(additionalArgs);
		}
		return false;
	}

	@Override
	public int hashCode() {
		// CHECKSTYLE:OFF
		return classOutputDirs.hashCode() ^ additionalArgs.hashCode() ^ javaHome.hashCode()
				^ workingDirectory.hashCode() ^ projectUnderTestClassPath.hashCode();
		// CHECKSTYLE:ON
	}

	@Override
	public List<File> getClassOutputDirs() {
		return classOutputDirs;
	}

	@Override
	public List<File> classDirectoriesInClasspath() {
		// FIXME what is the difference with classOutputDirs
		// RISK Caching this prevents tons of disk access, but we risk caching a
		// bad set of
		// classDirs
		if (classDirs == null) {
			classDirs = new ArrayList<>();
			for (String each : getClasspathEntries(projectUnderTestClassPath)) {
				File classEntry = new File(each);
				if (classEntry.isDirectory()) {
					classDirs.add(classEntry);
				}
			}
		}
		return classDirs;
	}

	public List<String> getRunnerFullClassPathEntries() {
		return getClasspathEntries(getRunnerFullClassPath());
	}
	
	public ClasspathArgumentBuilder createClasspathArgumentBuilder() {
		Integer javaVersion = getJavaVersion();
		
		if (javaVersion != null && javaVersion >= 9) {
			// Argument files are only supported from Java 9
			File classpathFile = createClasspathArgumentFile();

			return new FileClasspathArgumentBuilder(classpathFile);
		} else {
			// We are below Java 9 or could not find the version
			String classpath = getRunnerFullClassPath();
			
			return new SimpleClasspathArgumentBuilder(classpath);
		}
	}
	
	/**
	 * @return The Java major version (e.g 8 for 8.xyz) or null if we could not get the version
	 */
	private Integer getJavaVersion() {
		String javaVersion = null;
		
		try (FileInputStream in = new FileInputStream(new File(javaHome, "release"))) {
			Properties properties = new Properties();
			properties.load(in);
			
			javaVersion = properties.getProperty("JAVA_VERSION");
			int indexOfPoint = javaVersion.indexOf('.');
			String javaMajorVersion;
			if (indexOfPoint == -1) {
				// For the reference OpenJDK build the version is "18"
				javaMajorVersion = javaVersion.substring(1, javaVersion.length() - 1);
			} else {
				// For temurin it is "18.0.2"
				javaMajorVersion = javaVersion.substring(1, indexOfPoint);
			}
			
			return Integer.parseInt(javaMajorVersion);
		} catch (Exception e) {
			log(Level.SEVERE, "Could not get java version (" + javaVersion + ") " + e.getClass() + " " + e.getMessage());
			return null;
		}
	}

	public File createClasspathFile() {
		try {
			File classpathFile = InfinitestUtils.createTempFile("infinitest-", ".classpath");
			classpathFile.deleteOnExit();
			Files.write(classpathFile.toPath(), getRunnerFullClassPathEntries(), StandardCharsets.UTF_8);
			return classpathFile;
		} catch (IOException e) {
			throw new RuntimeException("Error writing classpath file", e);
		}
	}

	public File createClasspathArgumentFile() {
		try {
			File argumentFile = InfinitestUtils.createTempFile("infinitest-", ".cp-argument");
			argumentFile.deleteOnExit();
			String escapedRunnerFullClassPath = escapeClassPathFileContent(getRunnerFullClassPath());
			Files.write(argumentFile.toPath(), Collections.singleton(escapedRunnerFullClassPath), StandardCharsets.UTF_8);
			return argumentFile;
		} catch (IOException e) {
			throw new RuntimeException("Error writing argument file", e);
		}
	}

	protected String escapeClassPathFileContent(String classPath) {
		classPath = classPath.replace("\\", "\\\\");
		
		return "\"" + classPath + "\"";
	}
}
