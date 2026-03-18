# cloud-native-gcp-demo

A production-grade cloud-native application built with **Spring Boot 4.0.3** and **Google Cloud Platform**, developed as a focused 7-day sprint. Each day adds a new layer of real-world cloud infrastructure — all designed to run within GCP's free tier or at minimal cost (~€3–5 for the week).

> **Background:** Built by a Senior Java Engineer (9 years, Spring Boot / JEE / PostgreSQL) to demonstrate hands-on GCP cloud-native skills for new opportunities in the EU market.

---

## Architecture overview

```
┌─────────────────────────────────────────────────────────────┐
│                    GCP Project                              │
│                                                             │
│  ┌─────────────────┐        ┌──────────────────────────┐   │
│  │ Artifact        │        │ Cloud Run                │   │
│  │ Registry        │───────▶│                          │   │
│  │                 │        │  order-service           │   │
│  │ order-service   │        │  event-processor         │   │
│  │ event-processor │        │  (0–3 instances each)    │   │
│  └─────────────────┘        └──────────┬───────────────┘   │
│                                        │                    │
│  ┌──────────────┐   ┌──────────────┐   │ ┌──────────────┐  │
│  │ Cloud SQL    │   │  Pub/Sub     │   │ │   Secret     │  │
│  │ PostgreSQL   │   │  Topics /    │◀──┘ │   Manager    │  │
│  │ (db-f1-micro)│   │  Subs        │     │              │  │
│  └──────────────┘   └──────────────┘     └──────────────┘  │
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   │
│  │    Cloud     │   │    Cloud     │   │    Cloud     │   │
│  │   Logging    │   │  Monitoring  │   │    Trace     │   │
│  └──────────────┘   └──────────────┘   └──────────────┘   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Cloud Build  →  Artifact Registry  →  Cloud Run    │  │
│  │                    CI/CD pipeline                    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Repository structure

```
cloud-native-gcp-demo/
├── order-service/          # Producer — REST API, Cloud SQL backed
│   ├── src/
│   ├── pom.xml
│   └── cloudbuild.yaml
├── event-processor/        # Consumer — Pub/Sub push handler
│   ├── src/
│   └── pom.xml
├── helm/                   # Helm chart for GKE deployment (Day 6)
│   └── order-service/
├── infra/                  # gcloud provisioning commands
│   ├── setup.sh
│   ├── teardown.sh
│   └── startup.sh
└── README.md
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 (Spring Framework 7, Java 21 minimum) |
| Build | Maven + Jib (no Dockerfile) |
| Base image | `gcr.io/distroless/java21-debian12` |
| Container registry | GCP Artifact Registry |
| Compute | Cloud Run (serverless) |
| Database | Cloud SQL — PostgreSQL 15 |
| DB connectivity | Cloud SQL Java Connector (no public IP) |
| Secrets | Secret Manager |
| Messaging | Pub/Sub (push subscriptions) |
| Observability | Cloud Logging · Cloud Monitoring · Cloud Trace |
| Tracing | Micrometer Tracing |
| CI/CD | Cloud Build |
| Kubernetes | GKE Autopilot + Helm |
| IAM | Dedicated service accounts, least-privilege |

---

## Cost design

This project is designed to run at near-zero cost against GCP free tier:

| Service | Free tier | Usage |
|---|---|---|
| Cloud Run | 2M requests/month, 360k vCPU-seconds | Always on — €0 |
| Cloud Build | 120 min/day | ~2 min per build — €0 |
| Pub/Sub | 10GB/month | Negligible — €0 |
| Secret Manager | 6 active versions free | €0 |
| Cloud Logging / Monitoring / Trace | Generous free tiers | €0 |
| Artifact Registry | First 0.5GB free | ~150MB images — €0 |
| Cloud SQL | **Not free** — €0.10/hr | Session-only — ~€1–2 total |
| GKE Autopilot | **Not free** — ~€0.08/hr | Day 6 only, deleted same day — ~€0.40 |

**Total estimated week spend: €3–5.**

A budget alert is configured at €10 (see `infra/setup.sh`). If you receive the €5 alert email, something is running that should have been torn down.

---

## Quick start

### Prerequisites

- Java 21 (`sdk install java 21.0.3-tem` via SDKMAN)
- `gcloud` CLI authenticated (`gcloud init`)
- Maven 3.9+

### Run locally

```bash
cd order-service
./mvnw spring-boot:run

curl http://localhost:8080/
curl http://localhost:8080/api/orders
curl http://localhost:8080/actuator/health
```

### Deploy to Cloud Run

```bash
export PROJECT_ID=cloudnative-suhas-demo
export REGION=europe-west1
export REPO=demo-repo

# Build and push image (no Docker daemon required)
./mvnw compile jib:build

# Deploy
gcloud run deploy order-service \
  --image=${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/order-service:latest \
  --region=$REGION \
  --platform=managed \
  --service-account=order-service-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --allow-unauthenticated \
  --memory=512Mi \
  --min-instances=0 \
  --max-instances=3
```

### Session teardown (run every time you stop working)

```bash
./infra/teardown.sh
```

This stops Cloud SQL and deletes any GKE clusters. Cloud Run scales to zero automatically — no action needed.

---

## Day-by-day progress

### Day 1 — GCP foundations + Cloud Run ✅

**What was built:**
- GCP project with billing alert at €10
- Artifact Registry repository for Docker images
- Dedicated IAM service account with least-privilege roles (`logging.logWriter`, `monitoring.metricWriter`, `cloudtrace.agent`)
- Spring Boot **4.0.3** app with REST endpoints and Actuator health check (Spring Framework 7, Java 21 baseline)
- Containerised with **Jib 3.4.4** — no Dockerfile, no Docker daemon, distroless base image
- Deployed to Cloud Run: scales to zero, 512Mi memory, 0–3 instances
- `teardown.sh` and `startup.sh` committed to repo

**Key design decisions:**
- `gcr.io/distroless/java21-debian12` as base image: no shell, no package manager, dramatically reduced attack surface vs `eclipse-temurin`
- `${PORT:8080}` in `application.properties`: Cloud Run injects the `PORT` env var — hardcoding `8080` works by coincidence but breaks when Cloud Run changes port assignment
- `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`: JVM respects the container's 512Mi memory limit rather than seeing the host machine's RAM
- Service account per service: never run on the default compute SA, which has broad project-level permissions

**Endpoints:**

| Method | Path | Description |
|---|---|---|
| GET | `/` | Service info |
| GET | `/api/orders` | List all orders |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/actuator/health` | Health check (used by Cloud Run) |

**Image tags:**
- `:latest` — moves on every push, used for active development
- `:1.0.0` — frozen at Day 1 state, used for rollback

---

### Day 2 — Cloud SQL + Secret Manager 🔜

*Coming soon — persistent storage with zero plaintext credentials.*

> **Spring Boot 4 note:** Ships with Hibernate 7. `@GeneratedValue` strategy defaults and some `@Column` behaviours differ from Hibernate 6. Will be documented here when Day 2 is complete.

---

### Day 3 — Pub/Sub async messaging 🔜

*Coming soon — event-driven architecture with push subscriptions.*

---

### Day 4 — Observability 🔜

*Coming soon — structured logging, custom metrics, distributed tracing.*

---

### Day 5 — CI/CD with Cloud Build 🔜

*Coming soon — push-to-deploy pipeline.*

---

### Day 6 — GKE Autopilot + Helm 🔜

*Coming soon — Kubernetes deployment with Helm chart and HPA.*

---

### Day 7 — Polish + interview prep 🔜

*Coming soon — final cleanup and documentation.*

---

## IAM design

Each service runs under a dedicated service account following least-privilege:

| Service account | Service | Roles |
|---|---|---|
| `order-service-sa` | order-service (Cloud Run) | `logging.logWriter`, `monitoring.metricWriter`, `cloudtrace.agent` |

Roles are added incrementally as each day's features require them. No service account has `Editor` or `Owner` — those are only held by the human operator account during setup.

---

## Connecting the dots — GCP vs what I already knew

This project deliberately maps each GCP service to a production problem already solved in prior work:

| Prior experience | GCP equivalent | Why the mapping holds |
|---|---|---|
| MQTT broker (Identpro WMS) | **Pub/Sub** | Both are async pub/sub. Pub/Sub adds managed durability, dead-letter queues, and push-to-HTTP — no consumer polling needed |
| PostgreSQL + index tuning | **Cloud SQL** | Same PostgreSQL engine. Cloud SQL Connector replaces VPN/bastion host for secure connectivity |
| Jenkins / GitHub Actions | **Cloud Build** | Same pipeline concept — trigger on git push, build, push artefact, deploy |
| Docker + Kubernetes | **Cloud Run + GKE Autopilot** | Cloud Run = k8s without the cluster management. Autopilot = managed node pools |
| Manual secret rotation | **Secret Manager** | Centralised secret store with IAM-gated access and automatic version history |
| Application logs (Splunk / ELK) | **Cloud Logging** | Structured JSON logs auto-indexed. `logging.structured.format.console=gcp` in Spring Boot is all that's needed |

---

## Infrastructure scripts

### `infra/setup.sh`
Full one-time project provisioning: project creation, billing link, API enablement, Artifact Registry, IAM service accounts, budget alert.

### `infra/teardown.sh`
Safe session teardown. Stops Cloud SQL, deletes GKE cluster if exists. Cloud Run scales to zero automatically.

### `infra/startup.sh`
Session startup. Starts Cloud SQL when needed (Days 2–5). Prints current service status.

---

## License

MIT