package com.ih0rd.sandbox.k8s;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import io.dekorate.testing.annotation.Inject;
import io.dekorate.testing.annotation.KubernetesIntegrationTest;
import io.dekorate.testing.annotation.Named;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KubernetesIntegrationTest
public class SpringBootOnK8sIntegrationTest {

    @Inject
    private KubernetesClient client;

    @Inject
    private KubernetesList list;

    @Inject
    @Named("spring-boot-on-kubernetes-example")
    Pod pod;

    @Test
    public void shouldRespondWithHelloWorld() throws IOException {
        Assertions.assertNotNull(client);
        Assertions.assertNotNull(list);
        System.out.println("Using pod:" + pod.getMetadata().getName());
        System.out.println("Forwarding port");
        try (LocalPortForward p = client.services().withName("spring-boot-on-kubernetes-example").portForward(8080)) {
            assertTrue(p.isAlive());
            final URI uri = URI.create("http://localhost:" + p.getLocalPort() + "/");
            final Optional<HttpResponse<String>> httpResponse = fetchHttpResponse(uri);
            assertTrue(httpResponse.isPresent());
            assertEquals(200, httpResponse.get().statusCode());
            assertEquals("Hello World", httpResponse.get().body());
        }
    }

    private Optional<HttpResponse<String>> fetchHttpResponse(URI uri) {
        try (final HttpClient client = HttpClient.newBuilder().build()) {
            final HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
            return Optional.of(client.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException | InterruptedException e) {
            return Optional.empty();
        }
    }
}
