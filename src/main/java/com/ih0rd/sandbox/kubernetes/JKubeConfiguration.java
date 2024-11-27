package com.ih0rd.sandbox.kubernetes;


import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

@Configuration
public class JKubeConfiguration {
    @Value("${app.name:jkube-helm-example}")
    private String appName;

    @Value("${target.dir:target}")
    private String targetDir;



    @Bean
    public Boolean generateKubernetesResourcesDuringBuild() throws IOException {
        generateKubernetesResources();
        return true;
    }

    @Bean
    public Template helmValuesTemplate() {
        return new TemplateBuilder()
                .addToParameters(new ParameterBuilder().withName("serviceAccountName").withValue("default").build())
                .addToParameters(new ParameterBuilder().withName("labelsAppName").withValue(appName).build())
                .addToParameters(new ParameterBuilder()
                        .withName("containerName").withValue("{{ .Chart.Name }}").build())
                .addToParameters(new ParameterBuilder().withName("containerImagePullPolicy").withValue("Always").build())
                .addToParameters(new ParameterBuilder().withName("volumeName").withValue("config-map-volume").build())
                .addToParameters(new ParameterBuilder().withName("serviceType").withValue("NodePort").build())
                .addToParameters(new ParameterBuilder().withName("configMapName").withValue(appName + "-config-map").build())
                .addToParameters(new ParameterBuilder().withName("helmBuildLogFilename").withValue("helm-generate.log").build())
                .build();
    }

    public void generateKubernetesResources() throws IOException {
        Path projectDir = getResourcePath();
        File target = Paths.get(targetDir).toFile();
        File kubernetesHelmInputDir = new File(target, "kubernetes");

        if (!kubernetesHelmInputDir.exists()) {
            FileUtils.forceMkdir(kubernetesHelmInputDir);
        }

        Files.walkFileTree(projectDir, new SimpleFileVisitor<>() {
            @NotNull
            @Override
            public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                File destinationFile = new File(kubernetesHelmInputDir, file.getFileName().toString());
                if (!destinationFile.exists()) {
                    FileUtils.copyFile(file.toFile(), destinationFile);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder()
                .addToItems(
                        new ConfigMapBuilder()
                                .withNewMetadata().withName(appName + "-config-map").addToLabels("app", appName).endMetadata()
                                .addToData("helm-generate.log", String.format("Chart generated: %s\n", Instant.now()))
                                .build()
                )
                .addToItems(new ServiceBuilder()
                        .withNewMetadata().withName(appName + "-service").addToLabels("app", appName).endMetadata()
                        .withNewSpec()
                        .addToSelector("app", appName)
                        .addNewPort().withPort(8080).withNewTargetPort(8080).endPort()
                        .withType("NodePort")
                        .endSpec()
                        .build());

        // Запис у файл
        File yamlFile = new File(kubernetesHelmInputDir, "kubernetes.yml");
        if (!yamlFile.exists()) { // Перевірка перед записом
            FileUtils.write(yamlFile, Serialization.asYaml(kubernetesListBuilder.build()), StandardCharsets.UTF_8);
        }
    }

    @Bean
    public HelmConfig helmConfig(Template helmValuesTemplate) {
        Path projectDir = getResourcePath();
        File target = projectDir.resolve(targetDir).toFile();

        return HelmConfig.builder()
                .chart(appName + "-chart")
                .version("1.33.7")
                .parameterTemplates(Collections.singletonList(helmValuesTemplate))
                .sourceDir(new File(target, "").getAbsolutePath())
                .outputDir(new File(target, "").getAbsolutePath())
                .tarballOutputDir(target.getAbsolutePath())
                .chartExtension("tar.gz")
                .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
                .build();
    }

    private Path getResourcePath() {
        try {
            return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("jkube")).toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Resource not found: jkube", e);
        }
    }
}

