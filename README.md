# K8s Manifest Generation

This repository provides examples and insights into generating Kubernetes manifests with Java, using **Dekorate** and **JKube**.

---

## Agenda

- [Dekorate](#dekorate)
    - [Configuration Dekorate Overview](#configuration-dekorate-overview)
    - [Custom Docker Image Name](#custom-docker-image-name)
    - [Host Aliases](#host-aliases)
    - [Spring Boot with Cert Manager](#spring-boot-with-cert-manager)
- [JKube](#jkube)
    - [Configuration JKube Overview](#configuration-jkube-overview)

---

## Dekorate

### Configuration Dekorate Overview
* [Documentation] (https://dekorate.io/docs/overview)
* [Github] (https://github.com/eclipse-jkube/jkube)
* [DekorateConfig](src/main/java/com/ih0rd/sandbox/kubernetes/DekorateConfiguration.java)

### Custom Docker Image Name

An example that demonstrates the use of `@DockerBuild` in order to add a custom image name.
To access the `@DockerBuild` annotation you just need to have the following dependency in your
class path:

    <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>docker-annotations</artifactId>
      <version>4.1.4</version>
    </dependency>

Or a project already containing it like:

    <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>kubernetes-annotations</artifactId>
      <version>4.1.4</version>
    </dependency>

By default, the docker image name used is a combination of:

- group
- name
- version

As extracted by the environment or explicitly configured by the user. So the image name is something like: `${group}/${name}:${version}`.
If you need to use a custom name without having to edit any of the properties above, you can use the `image` property of the `@DockerBuild` annotation.

```
@KubernetesApplication
@DockerBuild(image="foo/bar:baz")
public class Main {

  public static void main(String[] args) {
     //do stuff
  }
}
```

Users that configure dekorate in the annotation less fashion (via application.properties or application.yaml), can:

```
dekorate.docker.image=foo/bar:baz
```

or

```
dekorate:
  docker:
    image: foo/bar:baz
```

You can find the generated deployment under: `target/classes/META-INF/dekorate/kubernetes.yml` that should look like:

```---
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app.kubernetes.io/name: kubernetes-with-custom-image-name-example
    app.kubernetes.io/version: 0.12-SNAPSHOT
  name: kubernetes-with-custom-image-name-example
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: kubernetes-with-custom-image-name-example
      app.kubernetes.io/version: 0.12-SNAPSHOT
  template:
    metadata:
      labels:
        app.kubernetes.io/name: kubernetes-with-custom-image-name-example
        app.kubernetes.io/version: 0.12-SNAPSHOT
    spec:
      containers:
        image: foo/bar:baz
        imagePullPolicy: IfNotPresent
        name: kubernetes-with-custom-image-name-example

```

### Host Aliases
An example that demonstrates the use of `@KubernetesApplication` in order to add hostAliases property to a deployment.
To access the `@KubernetesApplication` annotation you just need to have the following dependency in your
class path:

    <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>kubernetes-annotations</artifactId>
      <version>4.1.4</version>
    </dependency>

To add the hostAliases section of the Deployment specification you need pass the `hostAliases` parameter containing the ip address and the list of hostnames (comma separated values) to the `@KubernetesApplication` in the Spring Boot annotated class. The code would look as follow:

```
@KubernetesApplication(hostAliases = {@HostAlias(ip = "127.0.0.1", hostnames = "foo.org,bar.com")})
@SpringBootApplication
public class Main {

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }
}
```
You can pass multiple `@HostAlias` annotation depending on your needs.

You can find the generated deployment under: `target/classes/META-INF/dekorate/kubernetes.yml` that should look like:
```---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app.kubernetes.io/name: kubernetes-with-hostaliases-example
    app.kubernetes.io/version: 0.12-SNAPSHOT
  name: kubernetes-example-with-hostaliases
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: kubernetes-with-hostaliases-example
        app.kubernetes.io/version: 0.12-SNAPSHOT
    spec:
      hostAliases:
      - hostnames:
        - foo.org
        - bar.com
        ip: 127.0.0.1
      - hostnames:
        - test.com
        ip: 10.0.0.1
```



### Spring Boot with Cert Manager
The purpose of this Spring Boot example is to demonstrate how we can configure an HTTPS/TLS microservice using the Cert-Manager Dekorate extension.

Apart from the Dekorate Kubernetes starter dependency, the application must declare the Cert-Manager dekorate dependency part of the pom.xml file:

    <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>certmanager-annotations</artifactId>
      <version>4.1.4</version>
    </dependency>

## Enable HTTPS transport

To enable the HTTPS/TLS transport in Spring Boot, we need to add the following properties:

```
server.port=8443
server.ssl.enabled=true
```

Next, we need to configure the Java PKCS12 Keystore properties that our Spring Boot application will use to get the Server certificate signed and the private key:

```
server.ssl.key-store-type=PKCS12
server.ssl.key-store=/path/to/keystore.p12
server.ssl.key-store-password=the password
```

Where is the `keystore.p12` Keystore and what is its password? This is where Cert-Manager comes to play. This Java Keystore will be generated by the Cert-Manager on the Kubernetes platform as a secret resource. Then, what we need to do is to mount a volume from this generated secret, so the Spring Boot application can read the generated Keystore by Cert-Manager. In the following sections, we'll see how we can instruct Cert-Manager to generate the Keystore and how to configure the application to mount the secret and use it.

## Generate a Self-Signed Certificate and the Keystore

Let's configure the different properties to request the generation of the Self-Signed certificate and the `keystore.p12` PKCS12 Keystore file:

```
dekorate.certificate.secret-name=tls-secret
dekorate.certificate.self-signed.enabled=true
dekorate.certificate.keystores.pkcs12.create=true
# the secret name of the password:
dekorate.certificate.keystores.pkcs12.passwordSecretRef.name=pkcs12-pass 
dekorate.certificate.keystores.pkcs12.passwordSecretRef.key=password
```

Using this configuration, Dekorate will create the `Certificate` and `Issuer` resources that, once installed on the Kubernetes platform, will be used by the Certificate Manager to generate a self-signed certificate and the Keystore files within the secret `tls-secret`.

**NOTE**: As the Keystore file (pkcs12, ...) is password protected, this is then the reason why we have to create a secret including the needed password. For that purpose, we are going to create, part of the file `src/main/resources/k8s/common.yml`, a secret named "pkcs12-pass". The data field will include the key password where the string `supersecret` will be encoded in base64:

```yaml
---
apiVersion: v1
kind: Secret
metadata:
  name: pkcs12-pass
data:
  # "supersecret" in base64:
  password: c3VwZXJzZWNyZXQ=
type: Opaque
```

To tell to Dekorate where it can find the file `src/main/resources/k8s/common.yml`, we will then set the property `dekorate.options.input-path` to specify the name folder under `src/main/resources` where they can be found (e.g. k8s):

```
dekorate.options.input-path=k8s
```

At this point, when we install the generated resources by Dekorate on the Kubernetes platform, Certificate Manager will generate the generated PKCS12 Keystore file named `keystore.p12` within the secret `tls-secret`. Also, the Cert-Manager Dekorate extension will configure the Spring Boot application to automatically mount a volume using this secret `tls-secret` at the path `/etc/certs` (it can be configured using `dekorate.certificate.volume-mount-path`). Therefore, what we need to do next is to simply map the Keystore file and password into the Spring Boot properties `server.ssl.key-store` and `server.ssl.key-store-password`:

```
dekorate.kubernetes.env-vars[0].name=SERVER_SSL_KEY_STORE
dekorate.kubernetes.env-vars[0].value=/etc/certs/keystore.p12
dekorate.kubernetes.env-vars[1].name=SERVER_SSL_KEY_STORE_PASSWORD
dekorate.kubernetes.env-vars[1].secret=pkcs12-pass
dekorate.kubernetes.env-vars[1].value=password
```

## Run the application in Kubernetes

First, make sure you have access to a Kubernetes cluster and that the [Cert-Manager](https://cert-manager.io/docs/installation/) is deployed.

For generate the manifests and push the application container image to our container registry:

```
./mvnw clean package -Ddekorate.push=true -Ddekorate.docker.registry=<your container registry. Example: quay.io> -Ddekorate.docker.group=<the container group. Example: user>
```

After executing the above command, the generated manifests, which are available at this path `target/classes/META-INF/dekorate/kubernetes.yml`, can be installed as such:

```
kubectl apply -f target/classes/META-INF/dekorate/kubernetes.yml
```

After a few moments, we should be able to see the secret resource named `tls-secret` that the `Cert-Manager` has created like the pkcs12 keystore file:

```
kubectl get secret/tls-secret -o yaml | grep keystore.p12
```

After some minutes, our application should be up and running:

```
kubectl get pods
```

Output:

```
NAME                                                    READY   STATUS    RESTARTS   AGE
spring-boot-with-certmanager-example-566546987c-nj94n   1/1     Running   0          2m23s
```

Let's try it out by port-forwarding the port 8443:

```
kubectl port-forward spring-boot-with-certmanager-example-566546987c-nj94n 8443:8443
```

Then, if we browse to `https://localhost:8443/`, we should see `Hello world from HTTPS!`!


---

## JKube

### Configuration JKube Overview
* [Documentation] (https://eclipse.dev/jkube/docs/)
* [Github] (https://github.com/dekorateio/dekorate/)
* [JKubeConfig](src/main/java/com/ih0rd/sandbox/kubernetes/JKubeConfiguration.java)
