# MySchool — Application Web Distribuée

Système de gestion scolaire basé sur une **architecture microservices**
Spring Boot / Spring Cloud.

> Projet réalisé dans le cadre du module **Applications Web Distribuées (5SE)**

---

## 🏗️ Architecture

Le projet est composé de **5 microservices** :

| Microservice | Port | Type | Rôle |
|---|---|---|---|
| `discovery-service` | 8761 | Infrastructure | Eureka Server — registre des microservices |
| `config-service` | 8888 | Infrastructure | Spring Cloud Config — configurations centralisées |
| `gateway-service` | 8080 | Infrastructure | Spring Cloud Gateway — point d'entrée unique |
| `user-service` | 8082 | Métier | Gestion des étudiants, cours et inscriptions (MySQL + JWT) |
| `notification-service` | 8083 | Métier | Gestion des notifications email (MongoDB + Feign) |

### Schéma de l'architecture


┌─────────────────────────┐
│   Angular Frontend      │
│   (port 4200)           │
└──────────┬──────────────┘
│
▼
┌─────────────────────────┐
│   gateway-service       │
│   (port 8080)           │
└──────────┬──────────────┘
│
┌──────────────────┼──────────────────┐
▼                  ▼                  ▼
┌───────────────────┐  ┌──────────────┐  ┌───────────────────────┐
│  user-service     │  │ discovery    │  │ notification-service  │
│  (port 8082)      │◄─┤ service      ├─►│ (port 8083)           │
│                   │  │ Eureka 8761  │  │                       │
│  └──► MySQL       │  └──────────────┘  │  └──► MongoDB         │
└─────────┬─────────┘                    └──────────┬────────────┘
│                                          │
│   Feign Client (3 scénarios)            │
└──────────────────────────────────────────┘
▲
│
┌──────────┴──────────┐
│   config-service    │
│   (port 8888)       │
└─────────────────────┘


---

## 🛠️ Technologies utilisées

### Backend
- **Spring Boot** 3.2.5
- **Spring Cloud** 2023.0.3
- **Java** 17
- **Maven** (multi-modules)
- **Spring Cloud Gateway** — API Gateway réactif
- **Spring Cloud Netflix Eureka** — Service Discovery
- **Spring Cloud Config** — Configuration centralisée
- **Spring Cloud OpenFeign** — Communication inter-microservices
- **Spring Security + JWT** — Authentification
- **Spring Data JPA** — Persistance MySQL
- **Spring Data MongoDB** — Persistance NoSQL
- **Spring Mail + Thymeleaf** — Emails HTML
- **Lombok** — Réduction du boilerplate

### Bases de données
- **MySQL 8** — `user-service` (étudiants, cours, inscriptions, utilisateurs)
- **MongoDB 8** — `notification-service` (historique des notifications)

### Frontend
- **Angular 21** (projet séparé : `myschool-front`)

---

## 🔄 Communication inter-microservices (Feign)

Le `user-service` communique avec `notification-service` via **OpenFeign**.
Trois scénarios sont implémentés :

| # | Déclencheur | Type de notification |
|---|---|---|
| 1 | Création d'un étudiant | `WELCOME_EMAIL` |
| 2 | Inscription à un cours | `ENROLLMENT_CONFIRMATION` |
| 3 | Cours atteint sa capacité max | `CAPACITY_ALERT` (alerte admin) |

Le client Feign est défini dans :
`user-service/src/main/java/com/myschool/userservice/client/NotificationClient.java`

---

## 🚀 Démarrage du projet

### Prérequis

- **Java 17** installé
- **Maven 3.6+**
- **MySQL 8** (port 3306) avec base `myschool_db`
- **MongoDB 8** (port 27017)
- **Node.js 18+** et Angular CLI (pour le front)

### Ordre de démarrage (IMPORTANT)

⚠️ Les microservices doivent être lancés **dans cet ordre** :

```bash
# 1. Discovery Server (Eureka)
mvn spring-boot:run -pl discovery-service

# 2. Config Server
mvn spring-boot:run -pl config-service

# 3. Gateway
mvn spring-boot:run -pl gateway-service

# 4. User Service
mvn spring-boot:run -pl user-service

# 5. Notification Service
mvn spring-boot:run -pl notification-service
```

### Vérification

- **Eureka Dashboard** : http://localhost:8761 → doit afficher les 4 autres services
- **Config Server** : http://localhost:8888/user-service/default → doit retourner un JSON
- **Gateway health** : http://localhost:8080/actuator/health

---

## 🔐 Sécurité

L'authentification est basée sur **JWT** (JSON Web Tokens) + **BCrypt**
pour le hachage des mots de passe.

### Endpoints publics
- `POST /auth/register` — Inscription
- `POST /auth/login` — Connexion (retourne un token JWT)

### Endpoints protégés (nécessitent le header `Authorization: Bearer <token>`)
- `GET|POST /students/**`
- `GET|POST /courses/**`
- `GET|POST /enrollments/**`
- `GET|POST /notifications/**`

### Exemple d'appel authentifié

```bash
# 1. Login pour récupérer le token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# 2. Utiliser le token
curl -X GET http://localhost:8080/students \
  -H "Authorization: Bearer eyJhbGci..."
```

---

## 📂 Structure du projet


myschool-ms/
├── pom.xml                          # Parent Maven
├── README.md
├── .gitignore
│
├── discovery-service/               # Eureka Server
├── config-service/                  # Config Server
│   └── src/main/resources/config/   # Fichiers .properties centralisés
│
├── gateway-service/                 # API Gateway
│
├── user-service/                    # Microservice métier (MySQL)
│   └── src/main/java/com/myschool/userservice/
│       ├── client/                  # Feign clients (NotificationClient)
│       ├── controller/              # REST controllers
│       ├── service/                 # Logique métier
│       ├── entity/                  # Entités JPA
│       ├── repository/              # Spring Data JPA
│       └── security/                # JWT, filter, config
│
└── notification-service/            # Microservice métier (MongoDB)
└── src/main/java/com/myschool/notificationservice/
├── controller/              # REST controllers
├── service/                 # Envoi emails + sauvegarde Mongo
├── entity/                  # Documents MongoDB
├── repository/              # Spring Data MongoDB
└── client/                  # Feign client vers user-service


---

## 🧪 Tests

### Test manuel via Postman

1. **Register** : `POST http://localhost:8080/auth/register`
2. **Login** : `POST http://localhost:8080/auth/login` → récupérer le token
3. **Créer un étudiant** : `POST http://localhost:8080/students`
   → déclenche scénario Feign #1 (WELCOME_EMAIL)
4. **Créer un cours** : `POST http://localhost:8080/courses`
5. **Inscrire l'étudiant** : `POST http://localhost:8080/enrollments`
   → déclenche scénario Feign #2 (ENROLLMENT_CONFIRMATION)
6. **Remplir le cours** (plusieurs inscriptions)
   → déclenche scénario Feign #3 (CAPACITY_ALERT)

### Vérification des notifications

Ouvrir **MongoDB Compass** → base `myschool_notifications` →
collection `notifications` → voir les documents créés.

---

## 📋 Fonctionnalités implémentées

- ✅ Architecture microservices (5 services)
- ✅ Service Discovery (Eureka)
- ✅ API Gateway (Spring Cloud Gateway)
- ✅ Configuration centralisée (Config Server)
- ✅ Communication inter-services via Feign (3 scénarios)
- ✅ Base de données relationnelle (MySQL) pour les données métier
- ✅ Base de données NoSQL (MongoDB) pour les notifications
- ✅ Authentification JWT avec gestion des rôles
- ✅ Envoi d'emails HTML avec Thymeleaf
- ✅ Export Excel / PDF (Apache POI / OpenPDF)
- ✅ Frontend Angular 21

---

## 👥 Auteurs

- **Skander Hawess** — 4SAE — ESPRIT

---

