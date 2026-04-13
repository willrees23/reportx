# ReportX

Advanced-but-simple moderation reporting plugin for Paper networks with Velocity proxy support. Built on the [Solo](https://github.com/willrees23/solo) framework.

Design document: [PLAN.md](./PLAN.md)

## Status

Early development — Milestone 1 (core foundation). No runnable plugin jar yet.

## Build

Requires:

- JDK 21+
- Maven 3.9+
- Solo installed to your local Maven repo (`mvn install` in the Solo repo)

```
mvn clean verify
```

## Modules

| Module | Purpose |
| --- | --- |
| `reportx-core` | Pure-Java models, repository contracts, event types, utilities. No platform deps. |
| `reportx-storage-sqlite` | JDBC/SQLite implementation of the repository contracts. |
| `reportx-messaging-local` | In-JVM synchronous implementation of the message bus. |

## License

MIT — see [LICENSE](./LICENSE).
