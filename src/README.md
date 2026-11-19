# khora code

Two Gradle modules:

- `kernel-core`: the log's interface (`khora.kernel.log`) and an in-memory store for tests.
- `log-nats`: the log on NATS JetStream.

## Prerequisites

- Docker, running. The `log-nats` tests start NATS in containers.
- JDK 25 is optional. Gradle downloads one for the build if none is installed (Foojay toolchain resolver).

## Test

From the repository root:

```sh
./gradlew test
```

## Run NATS locally

```sh
docker compose up -d
```

`KHORA_NATS_SYNC` picks the disk sync of the `nats` service (port 4222):

- `always` (default): every write is synced to disk before it is acknowledged.
- `default`: NATS's default, a sync every 2 minutes. A host crash can lose recent acknowledged writes.

```sh
KHORA_NATS_SYNC=default docker compose up -d nats
```

The ports are bound to 127.0.0.1. There is no authentication: this is a development setup.

## Benchmark

The benchmark deletes the log stream on the server it is given. It refuses to run without `--wipe`.
Run it against the dedicated `nats-default-sync` service (port 4223), not against a log you keep:

```sh
docker compose up -d nats-default-sync
./gradlew :log-nats:bench --args='--url nats://localhost:4223 --monitor http://localhost:8223 --wipe'
```

Results are printed and written to `build/bench/`.
