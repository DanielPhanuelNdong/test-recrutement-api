# Task Manager — Backend (Spring Boot)

API REST pour une mini application de gestion de tâches, réalisée dans le cadre du test de recrutement. Ce dépôt contient pour l'instant le **backend** (`Java 25` + `Spring Boot 4` + `Spring Data JPA` + `MySQL` + `Flyway` + `JWT`). Les dossiers `frontend/` (React + Vite + TSX) et `mobile/` (Flutter) seront ajoutés en tant que sous-projets du même dépôt.

## Sommaire

- [Architecture](#architecture)
- [Choix techniques](#choix-techniques)
- [Modèle de données](#modèle-de-données)
- [Endpoints de l'API](#endpoints-de-lapi)
- [Installation et exécution](#installation-et-exécution)
- [Lancer avec Docker](#lancer-avec-docker)
- [Tests](#tests)
- [Documentation interactive (Swagger)](#documentation-interactive-swagger)

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

## Prochaines étapes

- [ ] Frontend React + Vite + TSX (`frontend/`)
- [ ] Application mobile Flutter (`mobile/`) — bonus
- [ ] Pipeline CI/CD (GitHub Actions) + déploiement GCP (Cloud Run) — bonus
