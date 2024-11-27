package com.ih0rd.sandbox.k8s;

import io.dekorate.docker.annotation.DockerBuild;
import io.dekorate.docker.config.DockerBuildConfig;
import io.dekorate.docker.config.DockerBuildConfigBuilder;
import io.dekorate.helm.annotation.HelmChart;
import io.dekorate.helm.config.HelmChartConfig;
import io.dekorate.helm.config.HelmChartConfigBuilder;
import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.KubernetesApplication;
import io.dekorate.kubernetes.config.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@KubernetesApplication(
        configMapVolumes = @io.dekorate.kubernetes.annotation.ConfigMapVolume(volumeName = "bar-volume", configMapName = "foo-map"),
        hostAliases = {
                @io.dekorate.kubernetes.annotation.HostAlias(ip = "127.0.0.1", hostnames = "foo.org,bar.com"),
                @io.dekorate.kubernetes.annotation.HostAlias(ip = "10.0.0.1", hostnames = "test.com")
        }
)
@DockerBuild(image = "foo/bar:baz")
@HelmChart(name = "sandbox-k8s-manifest-generation")
public class DekorateConfiguration {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.application.version}")
    private String appVersion;

    @Value("${server.port}")
    private int serverPort;

    @Bean
    public DockerBuildConfig dockerBuildConfig() {
        return new DockerBuildConfigBuilder()
                .withName(appName)
                .withVersion(appVersion)
                .build();
    }

    @Bean
    public KubernetesConfig kubernetesConfig() {
        var port = Port.newBuilder().withName("http").withContainerPort(serverPort).build();
        var label = new Label("appVersion", appVersion, new String[]{});
        var livenessProbe = new Probe("/health/liveness", "", "", "", 5, 10, 3, 200, 500);
        var readinessProbe = new Probe("/health/readiness", "", "", "", 5, 10, 3, 200, 500);
        var resourceRequirements = ResourceRequirements.newBuilder().withCpu("1m").withMemory("128Mi").build();
        var limitResources = ResourceRequirements.newBuilder().withCpu("5m").withMemory("512Mi").build();
        return new KubernetesConfigBuilder()
                .withName(appName)
                .withLabels(label)
                .withPorts(port)
                .withLivenessProbe(livenessProbe)
                .withReadinessProbe(readinessProbe)
                .withReplicas(3)
                .withRequestResources(resourceRequirements)
                .withLimitResources(limitResources)
                .withImagePullPolicy(ImagePullPolicy.Always)
//                .withConfigMapVolumes(configMapVolume())
                .build();
    }

    @Bean
    public HelmChartConfig helmChartConfig() {
        return new HelmChartConfigBuilder().withName(appName).withVersion(appVersion).withDescription("Demo application helm chart").withKeywords("Demo", "java", "spring boot").withCreateValuesSchemaFile(true).withCreateTarFile(true).withCreateReadmeFile(true).build();
    }

//    @Bean
//    public ConfigMapVolume configMapVolume() {
//        return new ConfigMapVolume("foo-map",
//                "bar-volume",
//                42,
//                true,
//                new Item[]{
//                        new Item("config-key1", "/path/to/key1", 0),
//                        new Item("config-key2", "/path/to/key2", 0)
//                });
//    }
}
