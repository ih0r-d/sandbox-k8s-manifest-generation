package com.ih0rd.sandbox.kubernetes;

import io.dekorate.helm.model.Chart;
import io.dekorate.helm.model.HelmDependency;
import io.dekorate.helm.model.ValuesSchema;
import io.dekorate.helm.model.ValuesSchemaProperty;
import io.fabric8.certmanager.api.model.v1.Certificate;
import io.fabric8.certmanager.api.model.v1.Issuer;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DekorateK8sResourcesTest {
    private static final Integer EXPECTED_K8S_ITEMS = 6;
    private static List<String> EXPECTED_HOST_NAME_1;
    private static List<String> EXPECTED_HOST_NAME_2;
    private static String EXPECTED_VOLUME_NAME;
    private static String EXPECTED_SECRET_NAME;

    public static KubernetesList K8S_LIST;

    private static String CHART_NAME = "myChart";
    private static String CHART_OUTPUT_LOCATION;
    private static String ROOT_CONFIG_NAME;

    @BeforeAll
    static void setupAll() {
        K8S_LIST = SharedUtils.loadKubernetesResources();
        EXPECTED_HOST_NAME_1 = List.of("test.com");
        EXPECTED_HOST_NAME_2 = List.of("foo.org", "bar.com");
        EXPECTED_VOLUME_NAME = "volume-certs";
        EXPECTED_SECRET_NAME = "tls-secret";
        CHART_NAME = "myChart";
        CHART_OUTPUT_LOCATION = "META-INF/dekorate/helm/kubernetes/" + CHART_NAME;
        ROOT_CONFIG_NAME = "app";
    }

    @Test
    public void shouldContainConfigMap() {
        assertNotNull(K8S_LIST);
        assertEquals(7, K8S_LIST.getItems().size());
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
        assertEquals(7, K8S_LIST.getItems().size());

        final Deployment deployment = SharedUtils.findFirst(Deployment.class);
        assertNotNull(deployment);

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();
        assertNotNull(container);
        assertEquals("foo/bar:baz", container.getImage());
    }

    @Test
    public void shouldContainHostAliases() {
        assertNotNull(K8S_LIST);
        assertEquals(7, K8S_LIST.getItems().size());
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

        final List<Probe> readinessProbe = containers.stream().map(Container::getReadinessProbe).toList();
        assertNotNull(readinessProbe);
        assertFalse(readinessProbe.isEmpty());

        final List<Probe> livenessProbe = containers.stream().map(Container::getLivenessProbe).toList();
        assertNotNull(livenessProbe);
        assertFalse(livenessProbe.isEmpty());
    }


    @Test
    public void shouldHelmManifestsBeGenerated() throws IOException {
        Chart chart = SharedUtils.read(CHART_OUTPUT_LOCATION +"/Chart.yaml", Chart.class);
        assertNotNull(chart, "Chart is null!");
        assertEquals("v2", chart.getApiVersion());
        // Should be the same as in `dekorate.helm.chart` from properties.
        assertEquals(CHART_NAME, chart.getName());
        // Should contain expected dependencies
        final List<HelmDependency> dependencies = chart.getDependencies();
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        assertEquals("dependencyNameA", dependencies.getFirst().getName());
        assertEquals("dependencyNameA", dependencies.getFirst().getAlias());
        assertEquals("0.0.1", dependencies.get(0).getVersion());
        assertEquals("http://localhost:8080", dependencies.get(0).getRepository());
        assertEquals("dependencyNameB", dependencies.get(1).getName());
        assertEquals("app", dependencies.get(1).getAlias());
        assertEquals("dependencyNameC", dependencies.get(2).getName());
        assertEquals("app.database.enabled", dependencies.get(2).getCondition());
        assertEquals(2, dependencies.get(2).getTags().length);
        assertEquals("web", dependencies.get(2).getTags()[0]);
        assertEquals("frontend", dependencies.get(2).getTags()[1]);
        // Values.yaml manifest
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/values.yaml"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/values.dev.yaml"));
        // templates
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/templates/deployment.yaml"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/templates/ingress.yaml"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/templates/service.yaml"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/templates/_helpers.tpl"));
        // charts folder
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/charts"));
        // notes
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/templates/NOTES.txt"));
        // optional resources
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/LICENSE"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/README.md"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/values.schema.json"));
        // crds
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/crds"));
        assertNotNull(SharedUtils.loadKubernetesResources(CHART_OUTPUT_LOCATION + "/crds/crontabs.stable.example.com.yaml"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void chartsShouldContainExpectedData() throws IOException {
        Map<String, Object> chart = SharedUtils.read(CHART_OUTPUT_LOCATION + "/Chart.yaml", Map.class);
        assertNotNull(chart, "Chart.yaml is null!");

        assertTrue(chart.containsKey("annotations"), "Does not contain `annotations` from the user Charts.yml!");
        assertEquals(CHART_NAME, chart.get("name"), "The name was not replaced with the generated value!");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void valuesShouldContainExpectedData() throws IOException {
        Map<String, Object> values = SharedUtils.read(CHART_OUTPUT_LOCATION + "/values.yaml", Map.class);
        assertNotNull(values, "Values is null!");

        assertNotNull(values.containsKey(ROOT_CONFIG_NAME), "Does not contain `" + ROOT_CONFIG_NAME + "`");
        assertNotNull(values.get(ROOT_CONFIG_NAME) instanceof Map, "Value `" + ROOT_CONFIG_NAME + "` is not a map!");

        // Rootless properties
        assertEquals("rootless-property", values.get("prop"));

        Map<String, Object> app = (Map<String, Object>) values.get(ROOT_CONFIG_NAME);
        // Should contain image
        assertNotNull(app.get("image"));
        // Should contain replicas
        assertEquals(3, app.get("replicas"));
        // Should NOT contain notFound: as this property is ignored
        assertNull(app.get("notFound"));
        // Should contain vcsUrl with the overridden value from properties
        assertEquals("Overridden", app.get("vcsUrl"));
        // Should map ports:
        Map<String, Object> ports = (Map<String, Object>) app.get("ports");
        assertNotNull(ports);
        assertEquals(8080, ports.get("http"));
        // Should include health check properties:
        // 1. tcp socket action
        Map<String, Object> livenessValues = (Map<String, Object>) app.get("livenessProbe");
        assertProbe(livenessValues, 11, 31);
        Map<String, Object> tcpSocketValues = (Map<String, Object>) livenessValues.get("tcpSocket");
        assertEquals("1111", tcpSocketValues.get("port"));
        assertEquals("my-service", tcpSocketValues.get("host"));
        // 2. http get action
        Map<String, Object> readinessValues = (Map<String, Object>) app.get("readinessProbe");
        assertProbe(readinessValues, 10, 30);
        Map<String, Object> httpGetValues = (Map<String, Object>) readinessValues.get("httpGet");
        assertEquals("/readiness", httpGetValues.get("path"));
        // It should be null because it's now mapped with app.ports.http;
        assertNull(httpGetValues.get("port"));
        assertEquals("HTTP", httpGetValues.get("scheme"));
        // 3. exec action
        Map<String, Object> startupValues = (Map<String, Object>) app.get("startupProbe");
        assertProbe(startupValues, 12, 32);
        Map<String, Object> execValues = (Map<String, Object>) startupValues.get("exec");
        List<String> command = (List<String>) execValues.get("command");
        assertEquals(2, command.size());
        assertEquals("command1", command.get(0));
        assertEquals("command2", command.get(1));
        // 4. helm expression
//        assertEquals(SharedUtils.readString("expected-ingress.yaml"), SharedUtils.readString(CHART_OUTPUT_LOCATION + "/templates/ingress.yaml"));
//        assertEquals(SharedUtils.readString("expected-configmap.yaml"), SharedUtils.readString(CHART_OUTPUT_LOCATION + "/templates/configmap.yaml"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void valuesShouldContainExpectedDataInDevProfile() throws IOException {
        Map<String, Object> values = SharedUtils.read(CHART_OUTPUT_LOCATION + "/values.dev.yaml", Map.class);
        assertNotNull(values, "Values is null!");

        assertTrue(values.containsKey(ROOT_CONFIG_NAME), "Does not contain `" + ROOT_CONFIG_NAME + "`");
        assertInstanceOf(Map.class, values.get(ROOT_CONFIG_NAME), "Value `" + ROOT_CONFIG_NAME + "` is not a map!");
        Map<String, Object> helmExampleValues = (Map<String, Object>) values.get(ROOT_CONFIG_NAME);

        // Should contain image
        assertNotNull(helmExampleValues.get("image"));
        // Should contain replicas
        assertEquals(3, helmExampleValues.get("replicas"));
        // Should NOT contain notFound: as this property is ignored
        assertNull(helmExampleValues.get("notFound"));
        // Should contain vcsUrl with the value from properties
        assertEquals("Only for DEV!", helmExampleValues.get("vcsUrl"));
        // Should contain ingress with the value from properties
        assertEquals("my-test-host", helmExampleValues.get("host"));
    }

    @Test
    public void valuesFileShouldContainDependencyValues() throws IOException {
        ValuesSchema schema = SharedUtils.read(CHART_OUTPUT_LOCATION + "/values.schema.json", ValuesSchema.class);
        // From properties
        assertEquals("My Values", schema.getTitle());
        // From the provided values schema json
        assertEquals(2, schema.getRequired().size());
        Iterator<String> requirements = schema.getRequired().iterator();
        assertEquals("protocol", requirements.next());
        assertEquals("port", requirements.next());
        ValuesSchemaProperty image = schema.getProperties().get("image");
        assertNotNull(image);
        assertEquals("Container Image", image.getDescription());
        assertEquals(2, image.getProperties().size());
        // From config references
        ValuesSchemaProperty app = schema.getProperties().get("app");
        assertNotNull(app);
        ValuesSchemaProperty replicas = app.getProperties().get("replicas");
        assertNotNull(replicas);
        assertEquals(3, replicas.getMinimum());
        assertEquals(5, replicas.getMaximum());
        assertEquals("Overwrite default description!", replicas.getDescription());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateValuesSchemaFile() throws IOException {
        Map<String, Object> values = SharedUtils.read(CHART_OUTPUT_LOCATION + "/values.yaml", Map.class);
        Map<String, Object> dependencyA = (Map<String, Object>) values.get("dependencyNameA");
        assertEquals("aValue", dependencyA.get("config"));

        Map<String, Object> dependencyApp = (Map<String, Object>) values.get("app");
        Map<String, Object> config = (Map<String, Object>) dependencyApp.get("config");
        assertEquals("John", config.get("user"));
        assertEquals("mysql", config.get("database"));
    }

    private void assertProbe(Map<String, Object> probeValues, int expectedTimeoutSeconds, int expectedPeriodSeconds) {
        assertEquals(3, probeValues.get("failureThreshold"));
        assertEquals(expectedTimeoutSeconds, probeValues.get("timeoutSeconds"));
        assertEquals(expectedPeriodSeconds, probeValues.get("periodSeconds"));
        assertEquals(1, probeValues.get("successThreshold"));
        assertEquals(0, probeValues.get("initialDelaySeconds"));
    }


}
