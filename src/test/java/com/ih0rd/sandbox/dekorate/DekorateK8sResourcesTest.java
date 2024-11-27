package com.ih0rd.sandbox.dekorate;

import io.fabric8.certmanager.api.model.v1.Certificate;
import io.fabric8.certmanager.api.model.v1.Issuer;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DekorateK8sResourcesTest {
    private static final Integer EXPECTED_K8S_ITEMS = 6;
    private static List<String> EXPECTED_HOST_NAME_1;
    private static List<String> EXPECTED_HOST_NAME_2;
    private static  String EXPECTED_VOLUME_NAME;
    private static  String EXPECTED_SECRET_NAME ;
    private static  String HTTPS_SCHEMA ;

    public static KubernetesList K8S_LIST;

    @BeforeAll
    static void setupAll() {
        K8S_LIST = SharedUtils.loadKubernetesResources();
        EXPECTED_HOST_NAME_1 = List.of("test.com");
        EXPECTED_HOST_NAME_2 = List.of("foo.org", "bar.com");
        EXPECTED_VOLUME_NAME = "volume-certs";
        EXPECTED_SECRET_NAME = "tls-secret";
        HTTPS_SCHEMA = "HTTPS";
    }

    @Test
    public void shouldContainConfigMap() {
        assertNotNull(K8S_LIST);
        assertEquals(EXPECTED_K8S_ITEMS, K8S_LIST.getItems().size());
        Deployment deployment = SharedUtils.findFirst(Deployment.class);
        assertEquals("Deployment", deployment.getKind());
        final Volume firstVolume = deployment.getSpec().getTemplate().getSpec().getVolumes().getFirst();
        assertNotNull(firstVolume);
        assertEquals("bar-volume", firstVolume.getName());
        assertEquals("foo-map", firstVolume.getConfigMap().getName());
    }

    @Test
    public void shouldUseCustomImageName() {
        assertNotNull(K8S_LIST);
        assertEquals(EXPECTED_K8S_ITEMS, K8S_LIST.getItems().size());

        final Deployment deployment = SharedUtils.findFirst(Deployment.class);
        assertNotNull(deployment);

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();
        assertNotNull(container);
        assertEquals("foo/bar:baz", container.getImage());
    }

    @Test
    public void shouldContainHostAliases() {
        assertNotNull(K8S_LIST);
        assertEquals(SharedUtils.EXPECTED_K8S_ITEMS, K8S_LIST.getItems().size());
        final Deployment deployment = SharedUtils.findFirst(Deployment.class);

        final List<HostAlias> hostAliases = deployment.getSpec().getTemplate().getSpec().getHostAliases();
        assertNotNull(hostAliases);
        assertEquals(2, hostAliases.size());

        assertTrue(hostAliases.stream().anyMatch(a -> a.getIp().equals("10.0.0.1")));
        assertTrue(hostAliases.stream().anyMatch(a -> a.getIp().equals("127.0.0.1")));

        assertTrue(hostAliases.stream().anyMatch(a -> a.getHostnames().containsAll(EXPECTED_HOST_NAME_1)));
        assertTrue(hostAliases.stream().anyMatch(a -> a.getHostnames().containsAll(EXPECTED_HOST_NAME_2)));
    }

    @Test
    public void shouldContainCertificate() {
        Certificate certificate = SharedUtils.findFirst(Certificate.class);
        assertNotNull(certificate);
        assertEquals(EXPECTED_SECRET_NAME, certificate.getSpec().getSecretName());
    }

    @Test
    public void shouldContainSelfSignedIssuer() {
        Issuer issuer = SharedUtils.findFirst(Issuer.class);
        assertNotNull(issuer);
        assertNotNull(issuer.getSpec().getSelfSigned());
    }

    @Test
    public void shouldContainVolumesAndSchemaInProbesShouldBeHttps() {
        Deployment deployment = SharedUtils.findFirst(Deployment.class);
        assertNotNull(deployment);
        PodSpec podTemplate = deployment.getSpec().getTemplate().getSpec();
        assertNotNull(podTemplate);
        final List<Volume> volumes = podTemplate.getVolumes();
        final List<Container> containers = podTemplate.getContainers();

        assertTrue(volumes.stream()
                .anyMatch(v -> v.getName().equals(EXPECTED_VOLUME_NAME) && v.getSecret().getSecretName().equals(EXPECTED_SECRET_NAME)));
        assertTrue(containers.stream()
                .allMatch(c -> c.getVolumeMounts().stream().anyMatch(m -> m.getName().equals(EXPECTED_VOLUME_NAME))));
        assertTrue(containers.stream()
                .map(Container::getReadinessProbe)
                .filter(Objects::nonNull)
                .allMatch(probe -> probe.getHttpGet().getScheme().equals(HTTPS_SCHEMA)));
        assertTrue(containers.stream()
                .map(Container::getLivenessProbe)
                .filter(Objects::nonNull)
                .allMatch(probe -> probe.getHttpGet().getScheme().equals(HTTPS_SCHEMA)));
    }
}
