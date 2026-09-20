# Task Manager — Backend

API REST pour une petite application de gestion de tâches, faite pour un test technique. Stack : Java 25, Spring Boot 4, Spring Data JPA, MySQL, Flyway, JWT.

Le frontend (React + Vite + TypeScript + Tailwind + Redux Toolkit) est dans un dépôt à part, [`recrutement-test-front-web`](../recrutement-test-front-web). Un client mobile Flutter pourra venir plus tard dans son propre dépôt.

## Sommaire

- [Architecture](#architecture)
- [Pourquoi ces choix techniques](#pourquoi-ces-choix-techniques)
- [Modèle de données](#modèle-de-données)
- [Endpoints](#endpoints)
- [Lancer le projet en local](#lancer-le-projet-en-local)
- [Avec Docker](#avec-docker)
- [Tests](#tests)
- [Swagger](#swagger)
- [CI/CD et déploiement GCP](#cicd-et-déploiement-gcp)
- [Frontend](#frontend)
- [À faire](#à-faire)

## Architecture

```
recutement-test/
├── src/main/java/com/test/recutement_test/
│   ├── auth/         # inscription / connexion
│   ├── config/       # sécurité, CORS, OpenAPI
│   ├── exception/    # gestion centralisée des erreurs
│   ├── security/     # JWT + filtre d'authentification
│   ├── task/         # entité Task, repository, specifications, service, controller
│   └── user/         # entité User
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/   # scripts Flyway
├── src/test/java/...    # tests d'intégration MockMvc
├── Dockerfile            # build multi-stage Gradle -> JRE
├── docker-compose.yml     # MySQL + backend en local
└── .github/workflows/ci-cd.yml
```

Découpage par domaine métier (`auth`, `task`, `user`) plutôt que par couche technique globale (`controller/`, `service/`, `repository/`) — plus simple à naviguer quand chaque dossier contient déjà tout ce qui concerne une fonctionnalité.

## Pourquoi ces choix techniques

**Auth en JWT stateless** plutôt qu'en session : pas de session serveur à gérer, et le même token sert pour le web et pour un futur client mobile. Les mots de passe passent par BCrypt.

**Spring Security** avec un filtre JWT custom (`OncePerRequestFilter`) en mode stateless (`SessionCreationPolicy.STATELESS`). Chaque requête sur `/api/tasks/**` est filtrée par `user_id` directement au niveau du repository (`findByIdAndUserId`), donc un utilisateur ne peut pas toucher aux tâches d'un autre — c'est aussi couvert par les tests.

**Recherche et filtrage des tâches** via `Specification` JPA (`TaskSpecifications`), pour combiner statut + recherche texte sans avoir à multiplier les méthodes de repository pour chaque combinaison de filtres.

**MySQL en dev/prod, H2 en mémoire pour les tests** (profil `test`) — tests rapides, aucune dépendance externe pour les faire tourner.

**Flyway** pour les migrations (`db/migration/V1__init_schema.sql`), avec `hibernate.ddl-auto: validate` : le schéma est versionné et explicite, Hibernate vérifie juste qu'il correspond aux entités mais ne le modifie jamais. En profil `test`, H2 tourne en `create-drop` pour aller plus vite.

**MapStruct** pour le mapping entité ↔ DTO (`TaskMapper`, `AuthMapper`), ça évite le mapping manuel et c'est généré à la compilation donc aucun coût à l'exécution. Les mappers ignorent explicitement tout ce qui relève d'une règle métier (statut par défaut, hash du mot de passe, propriétaire de la tâche) — cette logique reste dans les services, pas dans le mapper.

**Gestion d'erreurs centralisée** avec `ApiException` + `@RestControllerAdvice`, pour avoir des réponses JSON homogènes partout (`status`, `error`, `message`, `path`, `fieldErrors`).

**Documentation OpenAPI/Swagger** sur chaque endpoint et chaque DTO, avec un schéma de sécurité Bearer appliqué uniquement là où il faut un token.

**Validation** avec Bean Validation sur les DTOs (records), messages en français.

## Modèle de données

**User** : `id`, `email` (unique), `password` (haché), `fullName`, `createdAt`

**Task** : `id`, `title`, `description`, `status` (`TODO`, `IN_PROGRESS`, `DONE`), `user` (FK), `createdAt`, `updatedAt`

## Endpoints

| Méthode | URL | Auth | Description |
|---|---|:---:|---|
| POST | `/api/auth/register` | non | Inscription → renvoie un JWT |
| POST | `/api/auth/login` | non | Connexion → renvoie un JWT |
| GET | `/api/tasks?status=&search=` | oui | Liste des tâches de l'utilisateur connecté |
| POST | `/api/tasks` | oui | Création d'une tâche |
| PUT | `/api/tasks/{id}` | oui | Modification d'une tâche (doit m'appartenir) |
| DELETE | `/api/tasks/{id}` | oui | Suppression d'une tâche (doit m'appartenir) |

Le token se passe dans l'en-tête `Authorization: Bearer <token>`.

## Lancer le projet en local

Prérequis : Java 25 (via [SDKMAN](https://sdkman.io) par exemple) et MySQL 8+ (ou Docker).

Démarrer une base MySQL locale :

```bash
docker run -d --name task-manager-mysql \
  -e MYSQL_DATABASE=taskmanager \
  -e MYSQL_USER=taskuser \
  -e MYSQL_PASSWORD=taskpass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 mysql:8.4
```

Copier `.env.example` vers `.env` si besoin d'ajuster quelque chose (les valeurs par défaut collent déjà à la commande ci-dessus), puis lancer l'appli :

```bash
./gradlew bootRun
```

L'API démarre sur `http://localhost:8080`. Flyway applique les migrations tout seul au démarrage (création des tables `users`/`tasks`, contraintes, index), rien à faire à la main.

Pour tester rapidement :

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Alice","email":"alice@example.com","password":"password123"}'
```

## Avec Docker

`docker-compose.yml` lance MySQL et le backend (build automatique via le `Dockerfile` multi-stage) :

```bash
docker compose up --build
```

L'API arrive sur `http://localhost:8080`.

> Le `Dockerfile` utilise `eclipse-temurin:25-jdk`/`25-jre`. Si ces images ne sont pas dispo sur votre registre, basculer sur une LTS équivalente (ex. `21`) dans le `Dockerfile` et dans `java.toolchain` de `build.gradle.kts`.

## Tests

```bash
./gradlew test
```

`TaskFlowIntegrationTest` couvre, via MockMvc sur une base H2 en mémoire : inscription/connexion, refus d'inscription en doublon (409), accès refusé sans token (401), création/liste/filtrage/mise à jour/suppression d'une tâche, et l'isolation des tâches entre utilisateurs.

`TaskMapperTest` et `AuthMapperTest` testent directement le mapper généré par MapStruct (sans contexte Spring) : valeur par défaut du statut, champs volontairement ignorés, non-régression sur une mise à jour partielle.

J'ai aussi vérifié à la main qu'un deuxième utilisateur ne peut ni voir, ni modifier, ni supprimer les tâches d'un autre (404 dans tous les cas).

## Swagger

Une fois l'appli lancée :
- UI : `http://localhost:8080/swagger-ui.html`
- JSON OpenAPI : `http://localhost:8080/v3/api-docs`

Cliquer sur **Authorize** et coller `Bearer <token>` récupéré via `/api/auth/login` pour tester les endpoints protégés directement depuis l'interface. Les endpoints sont groupés par tag (Authentification / Tâches), avec les réponses possibles documentées (200/201/204, 400, 401, 404, 409) et le schéma JSON de chaque erreur.

## CI/CD et déploiement GCP

Le pipeline (`.github/workflows/ci-cd.yml`) a 4 étapes qui s'enchaînent : `build` → `test` → `package` → `deploy`.

| Job | Se déclenche | Fait quoi |
|---|---|---|
| `build` | push + PR | `./gradlew assemble`, jar publié en artifact GitHub |
| `test` | push + PR | `./gradlew test`, rapport JUnit publié sur le job |
| `package` | push sur `main` uniquement | build de l'image Docker + push vers Artifact Registry |
| `deploy` | push sur `main`, après `package` | déploiement de l'image sur Cloud Run |

Sur une pull request, seuls `build` et `test` tournent (validation avant merge, pas de déploiement). Sur `main`, tout s'enchaîne automatiquement à chaque push — pas de commande à lancer à la main, c'est GitHub Actions qui déclenche tout. Il n'y a pas de job de lint : le projet est full Java sans linter configuré pour l'instant.

Pour suivre un run : onglet **Actions** du dépôt GitHub.

### Mise en place GCP (à faire une seule fois)

Pour repartir de zéro sur GCP (hiérarchie des ressources, IAM, Cloud Run, Artifact Registry, Secret Manager...), j'ai mis toutes les notes détaillées dans [`PLAN-GCP.pdf`](./PLAN-GCP.pdf) à la racine du dépôt.

```bash
# Variables à adapter
export PROJECT_ID="mon-projet-gcp"
export REGION="europe-west1"
export SA_NAME="github-actions-deployer"

# 1. Activer les APIs nécessaires
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com --project "$PROJECT_ID"

# 2. Créer le dépôt Artifact Registry pour les images Docker
gcloud artifacts repositories create task-manager \
  --repository-format=docker --location="$REGION" \
  --description="Images Task Manager" --project "$PROJECT_ID"

# 3. Créer le compte de service utilisé par GitHub Actions
gcloud iam service-accounts create "$SA_NAME" \
  --display-name="GitHub Actions - Task Manager" --project "$PROJECT_ID"

SA_EMAIL="$SA_NAME@$PROJECT_ID.iam.gserviceaccount.com"

for ROLE in roles/artifactregistry.writer roles/run.admin roles/iam.serviceAccountUser roles/secretmanager.secretAccessor roles/cloudsql.client; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" --role="$ROLE"
done

# 4. Clé JSON pour l'authentification (à mettre dans le secret GitHub GCP_SA_KEY, puis à supprimer en local)
gcloud iam service-accounts keys create sa-key.json --iam-account="$SA_EMAIL"
# En prod, préférer la Workload Identity Federation plutôt qu'une clé longue durée :
# https://github.com/google-github-actions/auth#setting-up-workload-identity-federation

# 5. Secrets applicatifs dans Secret Manager (lus directement par le job "deploy")
echo -n "un-mot-de-passe-fort" | gcloud secrets create DB_PASSWORD --data-file=- --project "$PROJECT_ID"
echo -n "un-secret-jwt-de-32-caracteres-minimum" | gcloud secrets create JWT_SECRET --data-file=- --project "$PROJECT_ID"
gcloud secrets add-iam-policy-binding DB_PASSWORD --member="serviceAccount:$SA_EMAIL" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding JWT_SECRET  --member="serviceAccount:$SA_EMAIL" --role="roles/secretmanager.secretAccessor"

# 6. Le job "deploy" ne passe pas de service_account à deploy-cloudrun@v2, donc la révision
# Cloud Run tourne avec le compte de service Compute Engine par défaut, pas $SA_EMAIL
# (celui-ci ne sert qu'à l'authentification gcloud dans le job). Il faut donc lui donner
# accès aux secrets aussi, sinon la révision plante au démarrage.
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")
COMPUTE_SA="$PROJECT_NUMBER-compute@developer.gserviceaccount.com"
gcloud secrets add-iam-policy-binding DB_PASSWORD --member="serviceAccount:$COMPUTE_SA" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding JWT_SECRET  --member="serviceAccount:$COMPUTE_SA" --role="roles/secretmanager.secretAccessor"
```

### Base de données : Cloud SQL

Le pipeline ne provisionne pas la base, il faut la créer une fois à part :

```bash
export INSTANCE_NAME="task-manager-db"

gcloud sql instances create "$INSTANCE_NAME" \
  --database-version=MYSQL_8_4 --tier=db-f1-micro --region="$REGION" \
  --project "$PROJECT_ID"

gcloud sql databases create taskmanager --instance="$INSTANCE_NAME" --project "$PROJECT_ID"
gcloud sql users create taskuser --instance="$INSTANCE_NAME" --password="un-mot-de-passe-fort" --project "$PROJECT_ID"

# récupérer le connection name (format PROJECT_ID:REGION:INSTANCE_NAME)
gcloud sql instances describe "$INSTANCE_NAME" --project "$PROJECT_ID" --format="value(connectionName)"
```

L'appli se connecte à Cloud SQL via le [Cloud SQL Java Connector](https://github.com/GoogleCloudPlatform/cloud-sql-java-connector) (`mysql-socket-factory-connector-j-8`), activé par le profil Spring `cloud` (`application-cloud.yaml`) : pas d'IP publique à ouvrir, pas de certificat à gérer à la main. Le job `deploy` active ce profil, passe le connection name via `INSTANCE_CONNECTION_NAME` et monte l'accès à l'instance avec `--add-cloudsql-instances`.

Le compte de service de déploiement a besoin du rôle `roles/cloudsql.client` (déjà inclus au-dessus). Penser à ajouter `GCP_CLOUDSQL_INSTANCE` (le connection name récupéré ci-dessus) dans les variables GitHub.

### Secrets et variables GitHub

Dans **Settings → Secrets and variables → Actions** du dépôt :

**Secrets**
| Nom | Contenu |
|---|---|
| `GCP_SA_KEY` | Contenu JSON de `sa-key.json` (étape 4 ci-dessus) |

**Variables**
| Nom | Exemple |
|---|---|
| `GCP_PROJECT_ID` | `mon-projet-gcp` |
| `GCP_REGION` | `europe-west1` |
| `GCP_CLOUDSQL_INSTANCE` | `mon-projet-gcp:europe-west1:task-manager-db` |
| `DB_NAME` | `taskmanager` |
| `DB_USER` | `taskuser` |
| `CORS_ALLOWED_ORIGINS` | URLs autorisées séparées par des virgules, en incluant l'URL Cloud Run du frontend |

`DB_PASSWORD` et `JWT_SECRET` ne sont pas des secrets/variables GitHub : Cloud Run les lit directement dans Secret Manager au démarrage, pour qu'ils ne transitent jamais par les logs CI.

### Ce qui se passe à chaque push sur `main`

1. Build + tests. Si un test casse, tout s'arrête là, pas de `package`/`deploy`.
2. Image Docker construite et poussée sur `${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/task-manager/task-manager-backend`, taguée avec le SHA du commit et `latest`.
3. Déploiement sur Cloud Run de cette image précise, accessible publiquement (`--allow-unauthenticated`), avec `server.port` piloté par la variable `$PORT` que Cloud Run injecte automatiquement.

### En cas de souci

- **Aucun run n'apparaît dans l'onglet Actions après un push sur `main`** : sur un compte/dépôt neuf, GitHub désactive Actions par défaut. Aller dans l'onglet Actions et cliquer sur "Enable Actions on this repository". Si ça refuse, vérifier que l'email et le téléphone du compte sont bien vérifiés (et éventuellement un moyen de paiement renseigné dans Billing) — ça débloque souvent même sans jamais dépasser le quota gratuit.
- **Le job `deploy` échoue avec `Permission denied on secret ... for Revision service account ...-compute@developer.gserviceaccount.com`** : voir l'étape 6 plus haut, le compte de service Compute Engine par défaut doit aussi avoir accès à `DB_PASSWORD` et `JWT_SECRET`.

## Frontend

Le frontend (React 19 + Vite + TypeScript + Tailwind CSS v4 + Redux Toolkit + React Router + Axios) est dans le dépôt séparé [`recrutement-test-front-web`](../recrutement-test-front-web) — voir son README pour l'installation et les détails.

**CORS** : le backend n'autorise que les origines listées dans `CORS_ALLOWED_ORIGINS`. En local, `http://localhost:5173` est autorisé par défaut. Pour un frontend déployé, il faut ajouter son URL à la variable GitHub `CORS_ALLOWED_ORIGINS`, sinon le navigateur bloquera les requêtes même si le backend répond correctement.

## À faire

- [x] Frontend React + Vite + TS + Redux Toolkit (dépôt séparé)
- [x] Dockerfile + CI/CD frontend prêts — reste à configurer les secrets/variables GitHub sur ce dépôt et pousser sur `main` pour le premier déploiement effectif, et à ajouter son URL à `CORS_ALLOWED_ORIGINS` (déjà fait à la main sur le service Cloud Run, mais pas encore dans la variable GitHub)
- [ ] App mobile Flutter (`mobile/`) — bonus
- [x] Pipeline CI/CD + déploiement GCP — bonus
