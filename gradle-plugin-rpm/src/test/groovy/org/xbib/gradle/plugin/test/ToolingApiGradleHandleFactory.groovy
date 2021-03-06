package org.xbib.gradle.plugin.test

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

import java.util.concurrent.TimeUnit

class ToolingApiGradleHandleFactory implements GradleHandleFactory {

    private final boolean fork

    private final String version

    private final Integer daemonMaxIdleTimeInSeconds

    ToolingApiGradleHandleFactory(boolean fork, String version, Integer daemonMaxIdleTimeInSeconds = null) {
        this.fork = fork
        this.version = version
        this.daemonMaxIdleTimeInSeconds = daemonMaxIdleTimeInSeconds
    }

    @Override
    GradleHandle start(File projectDir, List<String> arguments, List<String> jvmArguments = []) {
        GradleConnector connector = createGradleConnector(projectDir)
        boolean forkedProcess = isForkedProcess()
        connector.embedded(!forkedProcess)
        if (daemonMaxIdleTimeInSeconds != null) {
            connector.daemonMaxIdleTime(daemonMaxIdleTimeInSeconds, TimeUnit.SECONDS)
        }
        ProjectConnection connection = connector.connect();
        BuildLauncher launcher = createBuildLauncher(connection, arguments, jvmArguments)
        createGradleHandle(connection, launcher, forkedProcess)
    }

    private GradleConnector createGradleConnector(File projectDir) {
        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(projectDir);
        configureGradleVersion(connector, projectDir)
        connector
    }

    private void configureGradleVersion(GradleConnector connector, File projectDir) {
        if (version != null) {
            connector.useGradleVersion(version)
        } else {
            configureWrapperDistributionIfUsed(connector, projectDir)
        }
    }

    private static void configureWrapperDistributionIfUsed(GradleConnector connector, File projectDir) {
        File target = projectDir.absoluteFile
        while (target != null) {
            URI distribution = prepareDistributionURI(target)
            if (distribution) {
                connector.useDistribution(distribution)
                return
            }
            target = target.parentFile
        }
    }

    private static URI prepareDistributionURI(File target) {
        File propertiesFile = new File(target, "gradle/wrapper/gradle-wrapper.properties")
        if (propertiesFile.exists()) {
            Properties properties = new Properties()
            propertiesFile.withInputStream {
                properties.load(it)
            }
            URI source = new URI(properties.getProperty("distributionUrl"))
            return source.getScheme() == null ? (new File(propertiesFile.getParentFile(), source.getSchemeSpecificPart())).toURI() : source;
        }
        return null
    }

    private boolean isForkedProcess() {
        fork
    }

    private static BuildLauncher createBuildLauncher(ProjectConnection connection, List<String> arguments,
                                                     List<String> jvmArguments) {
        BuildLauncher launcher = connection.newBuild();
        launcher.withArguments(arguments as String[]);
        launcher.setJvmArguments(jvmArguments as String[])
        launcher
    }

    private GradleHandle createGradleHandle(ProjectConnection connection, BuildLauncher launcher, boolean forkedProcess) {
        GradleHandleBuildListener toolingApiBuildListener =
                new ToolingApiBuildListener(connection)
        BuildLauncherBackedGradleHandle buildLauncherBackedGradleHandle =
                new BuildLauncherBackedGradleHandle(launcher, forkedProcess)
        buildLauncherBackedGradleHandle.registerBuildListener(toolingApiBuildListener)
        buildLauncherBackedGradleHandle
    }

    private class ToolingApiBuildListener implements GradleHandleBuildListener {
        private final ProjectConnection connection

        ToolingApiBuildListener(ProjectConnection connection) {
            this.connection = connection
        }

        @Override
        void buildStarted() {
        }

        @Override
        void buildFinished() {
            connection.close()
        }
    }
}