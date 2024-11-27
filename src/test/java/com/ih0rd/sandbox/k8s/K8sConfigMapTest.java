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
import io.fabric8.kubernetes.api.model.Volume;
import org.junit.jupiter.api.Test;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class K8sConfigMapTest {

  @Test
  public void shouldContainConfigMap() {
    KubernetesList list = Serialization.unmarshalAsList(Objects.requireNonNull(K8sConfigMapTest.class.getClassLoader().getResourceAsStream("META-INF/dekorate/kubernetes.yml")));
    assertNotNull(list);
    assertEquals(6, list.getItems().size());
    Deployment deployment = (Deployment) list.getItems().getFirst();
    assertEquals("Deployment", deployment.getKind());
    final Volume firstVolume = deployment.getSpec().getTemplate().getSpec().getVolumes().getFirst();
    assertNotNull(firstVolume);
    assertEquals("bar-volume", firstVolume.getName());
    assertEquals("foo-map", firstVolume.getConfigMap().getName());
  }
}
