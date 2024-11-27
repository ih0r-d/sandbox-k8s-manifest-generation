package com.ih0rd.sandbox.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.*;
import org.junit.jupiter.api.Test;

import io.dekorate.utils.Serialization;
import io.fabric8.certmanager.api.model.v1.Certificate;
import io.fabric8.certmanager.api.model.v1.Issuer;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class SpringBootCertManagerTest {

    private static final String EXPECTED_VOLUME_NAME = "volume-certs";
    private static final String EXPECTED_SECRET_NAME = "tls-secret";
    private static final String HTTPS = "HTTPS";

    @Test
    public void shouldContainCertificate() {
        Certificate certificate = findFirst(Certificate.class);

        assertEquals(EXPECTED_SECRET_NAME, certificate.getSpec().getSecretName());
    }

    @Test
    public void shouldContainSelfSignedIssuer() {
        Issuer issuer = findFirst(Issuer.class);
        assertNotNull(issuer.getSpec().getSelfSigned());
    }

    @Test
    public void shouldContainVolumesAndSchemaInProbesShouldBeHttps() {
        Deployment deployment = findFirst(Deployment.class);

        PodSpec podTemplate = deployment.getSpec().getTemplate().getSpec();
        final List<Volume> volumes = podTemplate.getVolumes();
        final List<Container> containers = podTemplate.getContainers();

        assertTrue(volumes.stream()
                .anyMatch(v -> v.getName().equals(EXPECTED_VOLUME_NAME) && v.getSecret().getSecretName().equals(EXPECTED_SECRET_NAME)));
        assertTrue(containers.stream()
                .allMatch(c -> c.getVolumeMounts().stream().anyMatch(m -> m.getName().equals(EXPECTED_VOLUME_NAME))));
        assertTrue(containers.stream()
                .map(Container::getReadinessProbe)
                .filter(Objects::nonNull)
                .allMatch(probe -> probe.getHttpGet().getScheme().equals(HTTPS)));
        assertTrue(containers.stream()
                .map(Container::getLivenessProbe)
                .filter(Objects::nonNull)
                .allMatch(probe -> probe.getHttpGet().getScheme().equals(HTTPS)));
    }

    @SuppressWarnings("unchecked")
    <T extends HasMetadata> T findFirst(Class<T> clazz) {
        KubernetesList list = Serialization
                .unmarshalAsList(Objects.requireNonNull(SpringBootCertManagerTest.class.getClassLoader().getResourceAsStream("META-INF/dekorate/kubernetes.yml")));
        assertNotNull(list);

        return (T) list.getItems().stream()
                .filter(clazz::isInstance)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }
}
