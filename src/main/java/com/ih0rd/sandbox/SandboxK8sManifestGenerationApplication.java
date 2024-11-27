package com.ih0rd.sandbox;

import io.dekorate.docker.annotation.DockerBuild;
import io.dekorate.helm.annotation.HelmChart;
import io.dekorate.kubernetes.annotation.ConfigMapVolume;
import io.dekorate.kubernetes.annotation.KubernetesApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SandboxK8sManifestGenerationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandboxK8sManifestGenerationApplication.class, args);
    }

}
