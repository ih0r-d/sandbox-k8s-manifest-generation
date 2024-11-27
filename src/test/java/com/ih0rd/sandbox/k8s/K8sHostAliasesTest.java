/**
 * Copyright 2018 The original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ih0rd.sandbox.k8s;

import io.dekorate.utils.Serialization;
import org.junit.jupiter.api.Test;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class K8sHostAliasesTest {

  @Test
  public void shouldContainHostAliases() {
    List<String> expectedHostName1 = Collections.singletonList("test.com");
    List<String> expectedHostName2 = List.of("foo.org", "bar.com");
    KubernetesList list = Serialization.unmarshalAsList(Objects.requireNonNull(K8sHostAliasesTest.class.getClassLoader().getResourceAsStream("META-INF/dekorate/kubernetes.yml")));
    assertNotNull(list);
    assertEquals(6, list.getItems().size());
    Deployment deployment = (Deployment) list.getItems().getFirst();

    assertEquals(2, deployment.getSpec().getTemplate().getSpec().getHostAliases().size());

    assertTrue(deployment.getSpec().getTemplate().getSpec().getHostAliases().stream().anyMatch(a -> a.getIp().equals("10.0.0.1")));
    assertTrue(deployment.getSpec().getTemplate().getSpec().getHostAliases().stream().anyMatch(a -> a.getIp().equals("127.0.0.1")));

    assertTrue(deployment.getSpec().getTemplate().getSpec().getHostAliases().stream().anyMatch(a -> a.getHostnames().containsAll(expectedHostName1)));
    assertTrue(deployment.getSpec().getTemplate().getSpec().getHostAliases().stream().anyMatch(a -> a.getHostnames().containsAll(expectedHostName2)));
  }
}
