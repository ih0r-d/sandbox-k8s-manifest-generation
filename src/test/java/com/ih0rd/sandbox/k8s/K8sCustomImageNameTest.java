package com.ih0rd.sandbox.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.dekorate.utils.Serialization;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.Objects;

public class K8sCustomImageNameTest {

  @Test
  public void shouldUseCustomImageName() {
    KubernetesList list = Serialization.unmarshalAsList(Objects.requireNonNull(K8sCustomImageNameTest.class.getClassLoader().getResourceAsStream("META-INF/dekorate/kubernetes.yml")));
    assertNotNull(list);
    assertEquals(6, list.getItems().size());
    Deployment deployment = (Deployment) list.getItems().getFirst();
    assertNotNull(deployment);
    Container container = deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();
    assertNotNull(container);
    assertEquals("foo/bar:baz", container.getImage());
  }
}
