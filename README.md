# OpenHardwareMonitor Web Client

`openhardwaremonitor-web-client` is a small JVM library for reading telemetry from the OpenHardwareMonitor web server `data.json` endpoint.

It fetches the hardware tree, parses the response, flattens sensors, and exposes a small metrics extractor for common values such as CPU temperature, CPU load, memory load, GPU temperature, and GPU load.

## Features

- JVM library usable from Kotlin and Java
- Reads OpenHardwareMonitor-compatible `/data.json` endpoints
- Tolerant JSON parsing with support for decimal commas like `58,5 °C`
- Typed models for the raw tree and flattened sensor values
- Convenience extraction for common metrics

## Coordinates

GitHub Packages:

```kotlin
implementation("io.github.czoeller:openhardwaremonitor-web-client:0.1.0")
```

## Kotlin Example

```kotlin
import io.github.czoeller.openhardwaremonitor.client.HttpOpenHardwareMonitorClient

val client = HttpOpenHardwareMonitorClient("http://localhost:8085")
val snapshot = client.fetchSnapshot()
val metrics = snapshot.metrics()

println(metrics.cpuTempC)
println(metrics.gpuTempC)
```

## Java Example

```java
import io.github.czoeller.openhardwaremonitor.client.HttpOpenHardwareMonitorClient;
import io.github.czoeller.openhardwaremonitor.client.OpenHardwareMonitorMetrics;
import io.github.czoeller.openhardwaremonitor.client.OpenHardwareMonitorSnapshot;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpOpenHardwareMonitorClient client =
            new HttpOpenHardwareMonitorClient("http://localhost:8085");

        OpenHardwareMonitorSnapshot snapshot = client.fetchSnapshot();
        OpenHardwareMonitorMetrics metrics = snapshot.metrics();

        System.out.println(metrics.getCpuTempC());
        System.out.println(metrics.getGpuTempC());
    }
}
```

## Gradle Setup

GitHub Packages requires authentication:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/czoeller/bongo_cat_monitor")
        credentials {
            username = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .get()
            password = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .get()
        }
    }
}
```

## Endpoint Notes

The client accepts either:

- `http://host:port`
- `http://host:port/data.json`

If `/data.json` is missing, it is appended automatically.

## Publishing

The module publishes:

- main jar
- sources jar
- javadoc jar

GitHub Actions builds and tests the project on pushes and pull requests, and publishes the library to GitHub Packages on GitHub releases or manual workflow dispatch.
