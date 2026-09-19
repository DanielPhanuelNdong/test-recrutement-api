# Task Manager — Backend (Spring Boot)

API REST pour une mini application de gestion de tâches, réalisée dans le cadre du test de recrutement (`Java 25` + `Spring Boot 4` + `Spring Data JPA` + `MySQL` + `Flyway` + `JWT`). Le frontend (React + Vite + TypeScript + Tailwind + Redux Toolkit) vit dans un dépôt séparé : [`recrutement-test-front-web`](../recrutement-test-front-web). Le dossier `mobile/` (Flutter) pourra être ajouté en tant que dépôt séparé du même projet.

## Sommaire

- [Architecture](#architecture)
- [Choix techniques](#choix-techniques)
- [Modèle de données](#modèle-de-données)
- [Endpoints de l'API](#endpoints-de-lapi)
- [Installation et exécution](#installation-et-exécution)
- [Lancer avec Docker](#lancer-avec-docker)
- [Tests](#tests)
- [Documentation interactive (Swagger)](#documentation-interactive-swagger)
- [CI/CD (GitHub Actions → GCP Cloud Run)](#cicd-github-actions--gcp-cloud-run)

## Architecture

```
recutement-test/
├── src/main/java/com/test/recutement_test/
│   ├── auth/            # Inscription / connexion (controller, service, DTOs)
│   ├── config/           # Sécurité, CORS, OpenAPI
│   ├── exception/         # Gestion centralisée des erreurs (ApiException + @RestControllerAdvice)
│   ├── security/          # JWT (génération/validation), filtre d'authentification, UserDetailsService
│   ├── task/              # Entité Task, repository, specifications, service, controller, DTOs
│   └── user/              # Entité User (implémente UserDetails), repository
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/       # Scripts Flyway (V1__init_schema.sql, ...)
├── src/test/java/...      # Tests d'intégration MockMvc (flux complet + cas d'erreur)
├── Dockerfile              # Build multi-stage (Gradle -> JRE)
├── docker-compose.yml       # MySQL + backend pour un environnement local complet
├── .github/workflows/ci-cd.yml  # CI/CD GitHub Actions -> Artifact Registry -> Cloud Run
└── build.gradle.kts
```

L'application suit une architecture en couches classique par domaine métier (`auth`, `task`, `user`) plutôt qu'une architecture technique globale (`controller/`, `service/`, `repository/`), pour garder chaque fonctionnalité regroupée.

## Choix techniques

| Sujet | Choix | Justification |
|---|---|---|
| Authentification | JWT stateless (`io.jsonwebtoken` / jjwt) | Pas de session serveur, consommable facilement par le web et le mobile avec le même token |
| Mots de passe | BCrypt (`PasswordEncoder`) | Standard de facto, résistant au brute-force |
| Sécurité | Spring Security 7 + filtre JWT custom (`OncePerRequestFilter`) | Contrôle fin du flux d'authentification, stateless (`SessionCreationPolicy.STATELESS`) |
| Autorisation par ressource | Chaque requête sur `/api/tasks/**` est scindée par `user_id` au niveau repository (`findByIdAndUserId`) | Empêche un utilisateur d'accéder aux tâches d'un autre (vérifié par tests) |
| Recherche / filtrage | `Specification` JPA (`TaskSpecifications`) | Permet de combiner statut + recherche texte de façon optionnelle sans multiplier les méthodes de repository |
| Base de données | MySQL en production/dev, H2 en mémoire pour les tests (`profile: test`) | Tests rapides et isolés, sans dépendance à une base externe |
| Migrations de schéma | Flyway (`db/migration/V1__init_schema.sql`) + `hibernate.ddl-auto: validate` | Le schéma est versionné et explicite (pas de génération automatique en prod) ; Hibernate valide seulement qu'il correspond aux entités, il ne le modifie jamais. Désactivé en profil `test` (H2 utilise `create-drop`) pour des tests rapides et isolés |
| Mapping entité ↔ DTO | MapStruct (`TaskMapper`, `AuthMapper`, beans Spring générés à la compilation) | Élimine le mapping manuel (builders ad-hoc) sans coût à l'exécution (code généré, pas de réflexion). Chaque mapper ignore explicitement les champs relevant d'une règle métier (statut par défaut, mot de passe haché, propriétaire de la tâche) plutôt que de les deviner, cette logique restant dans les services |
| Gestion des erreurs | `ApiException` + `@RestControllerAdvice` central | Réponses JSON homogènes (`status`, `error`, `message`, `path`, `fieldErrors`) |
| Documentation API | springdoc-openapi (Swagger UI) avec `@Tag` / `@Operation` / `@ApiResponses` / `@Schema` sur chaque endpoint et DTO | Documentation interactive et exploitable (description, exemples, codes d'erreur documentés), avec un schéma de sécurité Bearer appliqué uniquement aux endpoints protégés |
| Validation | Bean Validation (`jakarta.validation`) sur les DTOs (records) | Validation déclarative, messages d'erreur en français |

## Modèle de données

**User**
- `id`, `email` (unique), `password` (haché), `fullName`, `createdAt`

**Task**
- `id`, `title`, `description`, `status` (`TODO`, `IN_PROGRESS`, `DONE`), `user` (FK), `createdAt`, `updatedAt`

## Endpoints de l'API

| Méthode | URL | Auth requise | Description |
|---|---|:---:|---|
| POST | `/api/auth/register` | non | Inscription (`fullName`, `email`, `password`) → renvoie un JWT |
| POST | `/api/auth/login` | non | Connexion (`email`, `password`) → renvoie un JWT |
| GET | `/api/tasks?status=&search=` | oui | Liste des tâches de l'utilisateur connecté (filtrage optionnel par statut / recherche titre+description) |
| POST | `/api/tasks` | oui | Création d'une tâche (`title`, `description`, `status?`) |
| PUT | `/api/tasks/{id}` | oui | Modification d'une tâche (doit appartenir à l'utilisateur connecté) |
| DELETE | `/api/tasks/{id}` | oui | Suppression d'une tâche (doit appartenir à l'utilisateur connecté) |

Le token JWT s'envoie dans l'en-tête `Authorization: Bearer <token>`.

## Installation et exécution

### Prérequis

- Java 25 (ex: via [SDKMAN](https://sdkman.io))
- MySQL 8+ (ou Docker)

### 1. Démarrer une base MySQL locale

```bash
docker run -d --name task-manager-mysql \
  -e MYSQL_DATABASE=taskmanager \
  -e MYSQL_USER=taskuser \
  -e MYSQL_PASSWORD=taskpass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 mysql:8.4
```

### 2. Configurer les variables d'environnement (optionnel)

Copier `.env.example` vers `.env` et ajuster si besoin (valeurs par défaut déjà fonctionnelles avec la commande ci-dessus).

### 3. Lancer l'application

```bash
./gradlew bootRun
```

L'API démarre sur `http://localhost:8080`. Au démarrage, Flyway applique automatiquement les migrations de `src/main/resources/db/migration/` sur la base configurée (création des tables `users` et `tasks`, contraintes, index) ; aucune commande manuelle n'est nécessaire.

### 4. Tester rapidement

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Alice","email":"alice@example.com","password":"password123"}'
```

## Lancer avec Docker

Le `docker-compose.yml` fourni démarre MySQL **et** le backend (build automatique de l'image via le `Dockerfile` multi-stage) :

```bash
docker compose up --build
```

L'API est alors exposée sur `http://localhost:8080`.

> Note : le `Dockerfile` utilise `eclipse-temurin:25-jdk`/`25-jre`. Si ces images ne sont pas encore disponibles sur votre registre, utilisez une version LTS équivalente disponible (ex. `21`) en ajustant le `Dockerfile` et le `java.toolchain` de `build.gradle.kts`.

## Tests

```bash
./gradlew test
```

Les tests d'intégration (`TaskFlowIntegrationTest`) couvrent, via `MockMvc` sur une base H2 en mémoire :
- inscription, connexion, refus d'inscription en doublon (409),
- accès refusé sans token (401),
- création / liste / filtrage par statut / recherche / mise à jour / suppression d'une tâche,
- isolation stricte des tâches par utilisateur (vérifiée manuellement également, cf. ci-dessous).

Les tests unitaires `TaskMapperTest` et `AuthMapperTest` vérifient directement le mapper généré par MapStruct (sans contexte Spring) : valeur par défaut du statut, champs volontairement ignorés (id, propriétaire, dates, mot de passe), non-régression de la mise à jour partielle d'une tâche.

Vérification manuelle supplémentaire effectuée pendant le développement : un second utilisateur ne peut ni lister, ni modifier, ni supprimer les tâches d'un autre utilisateur (réponse `404`).

## Documentation interactive (Swagger)

Une fois l'application démarrée :
- Swagger UI : `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON : `http://localhost:8080/v3/api-docs`

Cliquer sur **Authorize** et renseigner `Bearer <token>` obtenu via `/api/auth/login` pour tester les endpoints protégés directement depuis l'interface.

La documentation est volontairement détaillée :
- Endpoints regroupés par tag (**Authentification**, **Tâches**) avec une description du flux d'utilisation.
- Chaque opération a un résumé, une description et la liste de ses réponses possibles (200/201/204, 400, 401, 404, 409), chacune associée au schéma JSON réellement renvoyé (`ErrorResponse` pour les erreurs).
- Chaque champ de DTO a une description et un exemple (`@Schema`), utilisés pour pré-remplir les requêtes d'exemple dans Swagger UI.
- Le cadenas d'authentification n'apparaît que sur les endpoints `/api/tasks/**` : les endpoints `/api/auth/**` restent visiblement publics.

## CI/CD (GitHub Actions → GCP Cloud Run)

> 📘 Vous découvrez GCP ? **[`PLAN-GCP.pdf`](./PLAN-GCP.pdf)** (à la racine du dépôt) explique en détail chaque concept mobilisé ici — hiérarchie des ressources, IAM, Cloud Run, Artifact Registry, Secret Manager, connexion à MySQL — en partant de zéro et en le reliant précisément aux commandes et aux jobs ci-dessous.

Le pipeline (`.github/workflows/ci-cd.yml`) reprend le découpage en étapes d'un `.gitlab-ci.yml` classique, transposé en jobs GitHub Actions :

| Job | Équivalent GitLab CI | Déclenchement | Rôle |
|---|---|---|---|
| `build` | `build_project` (stage `build`) | push + pull request | `./gradlew assemble`, jar publié en artifact |
| `test` | `run_tests` (stage `test`) | push + pull request | `./gradlew test`, rapport JUnit publié sur le job |
| `package` | `package` (stage `package`) | push sur `main` uniquement | Build de l'image Docker (`Dockerfile` multi-stage) et push vers Artifact Registry |
| `deploy` | `deploy` (stage `deploy`, SSH + `docker compose`) | push sur `main` uniquement, après `package` | Déploiement de l'image sur **Cloud Run** via `gcloud`/`deploy-cloudrun` |

Différences volontaires par rapport à la référence GitLab : pas de job `lint_code` (le projet est 100 % Java, sans linter Kotlin-style configuré), et le déploiement cible Cloud Run (`google-github-actions/deploy-cloudrun`) plutôt qu'un VPS joint en SSH.

### Mise en place côté GCP (à faire une fois)

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

# 4a. Authentification par clé JSON (simple, à utiliser pour ce test)
gcloud iam service-accounts keys create sa-key.json --iam-account="$SA_EMAIL"
# -> coller le contenu de sa-key.json dans le secret GitHub GCP_SA_KEY, puis supprimer le fichier local.

# 4b. Alternative recommandée en production : Workload Identity Federation (sans clé longue durée)
# https://github.com/google-github-actions/auth#setting-up-workload-identity-federation

# 5. Secrets applicatifs dans Secret Manager (référencés par le job "deploy")
echo -n "un-mot-de-passe-fort" | gcloud secrets create DB_PASSWORD --data-file=- --project "$PROJECT_ID"
echo -n "un-secret-jwt-de-32-caracteres-minimum" | gcloud secrets create JWT_SECRET --data-file=- --project "$PROJECT_ID"
gcloud secrets add-iam-policy-binding DB_PASSWORD --member="serviceAccount:$SA_EMAIL" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding JWT_SECRET  --member="serviceAccount:$SA_EMAIL" --role="roles/secretmanager.secretAccessor"

# 6. Le job "deploy" ne passe pas de `service_account:` à deploy-cloudrun@v2 : la révision Cloud Run
#    tourne donc avec le compte de service Compute Engine par défaut (PROJECT_NUMBER-compute@developer.gserviceaccount.com),
#    PAS avec $SA_EMAIL (qui ne sert qu'à l'authentification gcloud dans le job, pas à l'exécution du conteneur).
#    Ce compte par défaut doit donc AUSSI avoir accès aux secrets, sinon la révision échoue au démarrage
#    avec "Permission denied on secret ... for Revision service account".
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")
COMPUTE_SA="$PROJECT_NUMBER-compute@developer.gserviceaccount.com"
gcloud secrets add-iam-policy-binding DB_PASSWORD --member="serviceAccount:$COMPUTE_SA" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding JWT_SECRET  --member="serviceAccount:$COMPUTE_SA" --role="roles/secretmanager.secretAccessor"
```

### Base de données : Cloud SQL (MySQL)

La base MySQL n'est pas provisionnée par ce pipeline ; elle doit être créée une fois, séparément :

```bash
export INSTANCE_NAME="task-manager-db"

# 1. Créer l'instance Cloud SQL (MySQL 8.4, la plus petite tier pour un environnement de test)
gcloud sql instances create "$INSTANCE_NAME" \
  --database-version=MYSQL_8_4 --tier=db-f1-micro --region="$REGION" \
  --project "$PROJECT_ID"

# 2. Créer la base et l'utilisateur applicatif
gcloud sql databases create taskmanager --instance="$INSTANCE_NAME" --project "$PROJECT_ID"
gcloud sql users create taskuser --instance="$INSTANCE_NAME" --password="un-mot-de-passe-fort" --project "$PROJECT_ID"

# 3. Récupérer le connection name (format PROJECT_ID:REGION:INSTANCE_NAME)
gcloud sql instances describe "$INSTANCE_NAME" --project "$PROJECT_ID" --format="value(connectionName)"
```

L'application se connecte à Cloud SQL via le [Cloud SQL Java Connector](https://github.com/GoogleCloudPlatform/cloud-sql-java-connector) (dépendance `mysql-socket-factory-connector-j-8`), activé par le profil Spring `cloud` (`application-cloud.yaml`) — pas d'IP publique à ouvrir, pas de certificat TLS à gérer à la main. Le job `deploy` :
- active ce profil (`SPRING_PROFILES_ACTIVE=cloud`),
- passe le connection name via `INSTANCE_CONNECTION_NAME`,
- monte l'accès à l'instance avec le flag `--add-cloudsql-instances`.

Le compte de service de déploiement doit avoir le rôle `roles/cloudsql.client` (déjà inclus dans la boucle IAM ci-dessus). Ajouter `GCP_CLOUDSQL_INSTANCE` (le connection name récupéré à l'étape 3) aux variables GitHub ci-dessous.

### Secrets et variables GitHub à configurer

Dans **Settings → Secrets and variables → Actions** du dépôt :

**Secrets** (`Repository secrets`)
| Nom | Contenu |
|---|---|
| `GCP_SA_KEY` | Contenu JSON de `sa-key.json` (étape 4a ci-dessus) |

**Variables** (`Repository variables`)
| Nom | Exemple |
|---|---|
| `GCP_PROJECT_ID` | `mon-projet-gcp` |
| `GCP_REGION` | `europe-west1` |
| `GCP_CLOUDSQL_INSTANCE` | `mon-projet-gcp:europe-west1:task-manager-db` (connection name de l'instance) |
| `DB_NAME` | `taskmanager` |
| `DB_USER` | `taskuser` |
| `CORS_ALLOWED_ORIGINS` | URLs autorisées, séparées par des virgules (ex. `http://localhost:5173,https://task-manager-frontend-646783674843.europe-west1.run.app`) — inclure l'URL Cloud Run du frontend (cf. [`test-recrutement-front-web`](../recrutement-test-front-web)) |

`DB_PASSWORD` et `JWT_SECRET` ne sont **pas** des variables/secrets GitHub : ils sont lus directement depuis Secret Manager par Cloud Run au démarrage (`--set-secrets`), pour ne jamais transiter par les logs CI.

### Ce que fait le pipeline à chaque push sur `main`

1. Build + tests (échoue le pipeline si un test casse — le `package`/`deploy` ne se déclenchent pas).
2. Image Docker construite et poussée sur `${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/task-manager/task-manager-backend`, taguée avec le SHA du commit et `latest`.
3. Déploiement Cloud Run de cette image précise (traçabilité build → déploiement), service accessible publiquement (`--allow-unauthenticated`), avec `server.port` piloté par la variable `$PORT` que Cloud Run injecte (déjà supporté par `application.yaml`, aucune adaptation nécessaire).

Sur une pull request, seuls les jobs `build` et `test` s'exécutent (validation avant merge, pas de déploiement).

### Dépannage

- **Aucun run n'apparaît dans l'onglet Actions, même après un push sur `main`** : sur un dépôt (ou compte GitHub) neuf/non vérifié, GitHub désactive Actions par défaut par mesure anti-abus. Aller sur l'onglet `Actions` du dépôt : s'il affiche *"Workflows aren't being run on this repository"*, cliquer sur **"Enable Actions on this repository"**. Si le bouton renvoie *"Unable to enable Actions for this repository"*, vérifier sur le compte GitHub propriétaire du dépôt : email vérifié, téléphone vérifié, et/ou moyen de paiement renseigné dans `Settings → Billing and plans` (même sans jamais dépasser le quota gratuit, ça débloque souvent l'activation).
- **Le job `deploy` échoue avec `Permission denied on secret ... for Revision service account ...-compute@developer.gserviceaccount.com`** : voir l'étape 6 de la mise en place GCP ci-dessus — le compte de service Compute Engine par défaut (pas `$SA_EMAIL`) doit aussi avoir `roles/secretmanager.secretAccessor` sur `DB_PASSWORD` et `JWT_SECRET`.

## Frontend

Le frontend (React 19 + Vite + TypeScript + Tailwind CSS v4 + Redux Toolkit + React Router + Axios) vit dans un dépôt séparé : [`recrutement-test-front-web`](../recrutement-test-front-web). Voir le README de ce dépôt pour l'installation, l'architecture et les choix techniques détaillés.

**CORS** : le backend n'autorise que les origines listées dans la variable d'environnement `CORS_ALLOWED_ORIGINS` (cf. [CI/CD](#cicd-github-actions--gcp-cloud-run) ci-dessus). En local, `http://localhost:5173` est autorisé par défaut (voir `application.yaml`). Pour un frontend déployé (Cloud Run, Firebase Hosting, ...), son URL doit être ajoutée à la variable GitHub `CORS_ALLOWED_ORIGINS`, sans quoi le navigateur bloquera les requêtes malgré un backend fonctionnel.

## Prochaines étapes

- [x] Frontend React + Vite + TSX + Redux Toolkit (dépôt séparé [`recrutement-test-front-web`](../recrutement-test-front-web))
- [x] Dockerfile + CI/CD frontend prêts (dépôt [`test-recrutement-front-web`](../recrutement-test-front-web)) — reste à configurer les secrets/variables GitHub sur ce dépôt et pousser sur `main` pour le premier déploiement effectif ; et à ajouter son URL à la variable `CORS_ALLOWED_ORIGINS` ci-dessus (déjà fait en édition directe sur le service Cloud Run, mais pas encore dans la variable GitHub — sera écrasé au prochain déploiement du backend)
- [ ] Application mobile Flutter (`mobile/`) — bonus
- [x] Pipeline CI/CD (GitHub Actions) + déploiement GCP (Cloud Run) — bonus
