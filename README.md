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

JitPack:

```kotlin
implementation("com.github.czoeller:openhardwaremonitor-web-client:0.0.2")
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

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
```

## Endpoint Notes

The client accepts either:

- `http://host:port`
- `http://host:port/data.json`

If `/data.json` is missing, it is appended automatically.

## Publishing

Create a Git tag such as `0.0.2`, push it to GitHub, and JitPack will build that tag on demand.
