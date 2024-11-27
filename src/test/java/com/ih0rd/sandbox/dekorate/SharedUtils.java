package com.ih0rd.sandbox.dekorate;

import io.dekorate.utils.Serialization;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SharedUtils {

    private static final String K8S_MANIFEST = "META-INF/dekorate/kubernetes.yml";

    public static final Integer EXPECTED_K8S_ITEMS = 6;

    public static KubernetesList loadKubernetesResources() {
        return Serialization.unmarshalAsList(Objects.requireNonNull(SharedUtils.class.getClassLoader().getResourceAsStream(K8S_MANIFEST)));
    }

    public static InputStream loadKubernetesResources(String path) {
        return SharedUtils.class.getClassLoader().getResourceAsStream(path);
    }

    @SuppressWarnings("unchecked")
    public static <T extends HasMetadata> T findFirst(Class<T> clazz) {
        KubernetesList list = loadKubernetesResources();
        assertNotNull(list);

        return (T) list.getItems().stream()
                .filter(clazz::isInstance)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    public static Optional<HttpResponse<String>> fetchHttpResponse(URI uri) {
        try (final HttpClient client = HttpClient.newBuilder().build()) {
            final HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
            return Optional.of(client.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException | InterruptedException e) {
            return Optional.empty();
        }
    }

    public static <T> T read(String path, Class<T> clazz) throws IOException {
        return Serialization.yamlMapper().readValue(SharedUtils.class.getClassLoader()
                .getResourceAsStream(path), clazz);
    }

    public static String readString(String path) {
        return Objects.requireNonNull(new BufferedReader(
                new InputStreamReader(SharedUtils.loadKubernetesResources(path), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining(System.lineSeparator())));
    }
}
