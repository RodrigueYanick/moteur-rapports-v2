# 📋 Cahier & Plan de Recette Global – Moteur de Rapports v2

Ce document est un guide pas-à-pas conçu pour tester l'intégralité de la plateforme **Moteur de Rapports** (Frontend Angular, Backend Spring Boot, Moteur Gotenberg, Stockage MinIO S3, Base PostgreSQL).

---

## 📑 Sommaire
1. [Étape 0 : Vérification Préalable de l'Environnement Docker](#étape-0--vérification-préalable-de-lenvironnement-docker)
2. [Étape 1 : Authentification & Gestion de Session](#étape-1--authentification--gestion-de-session)
3. [Étape 2 : Configuration de la Feuille de Travail Entreprise](#étape-2--configuration-de-la-feuille-de-travail-entreprise)
4. [Étape 3 : Création & Design d'un Modèle de Rapport](#étape-3--création--design-dun-modèle-de-rapport)
5. [Étape 4 : Injection de Données & Génération PDF (Gotenberg)](#étape-4--injection-de-données--génération-pdf-gotenberg)
6. [Étape 5 : Stockage MinIO S3 & Gestion des Documents](#étape-5--stockage-minio-s3--gestion-des-documents)
7. [Étape 6 : Traitements par Lots (Batches) avec CSV/Excel](#étape-6--traitements-par-lots-batches-avec-csvexcel)
8. [Étape 7 : Sources de Données Externes & Planifications (Cron)](#étape-7--sources-de-données-externes--planifications-cron)
9. [Étape 8 : Certificats Numériques & Piste d'Audit](#étape-8--certificats-numériques--piste-daudit)
10. [Étape 9 : Exécution des Suites de Tests Automatisés](#étape-9--exécution-des-suites-de-tests-automatisés)
11. [Matrice de Recette & Checklist Finale](#matrice-de-recette--checklist-finale)

---

## Étape 0 : Vérification Préalable de l'Environnement Docker

### Objectif
S'assurer que les 5 conteneurs sont en ligne, communicants et sains (`healthy`).

### Actions à exécuter dans le terminal :
```powershell
docker compose ps
```

### Résultat Attendu :
| Conteneur | Statut | Port Hôte |
| :--- | :--- | :--- |
| `report-frontend` | `Up` | `80` |
| `report-backend` | `Up (healthy)` | `8082` |
| `report-gotenberg` | `Up (healthy)` | `3000` |
| `report-minio` | `Up (healthy)` | `9000` (API) & `9001` (Web UI) |
| `report-postgres` | `Up (healthy)` | `5455` (interne: `5432`) |

### Tests de connectivité rapide (Smoke Tests) :
- **Frontend** : Ouvrir [http://localhost](http://localhost) dans votre navigateur (affiche la page d'accueil ou de connexion).
- **Backend Health** : Ouvrir [http://localhost:8082/api/health](http://localhost:8082/api/health) (retourne `{"status":"UP"}`).
- **Moteur PDF Gotenberg** : Ouvrir [http://localhost:3000/health](http://localhost:3000/health) (retourne `{"status":"up"}`).
- **Console MinIO** : Ouvrir [http://localhost:9001](http://localhost:9001) (identifiants: `minioadmin` / `minioadmin`).
- **Swagger OpenAPI** : Ouvrir [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html).

---

## Étape 1 : Authentification & Gestion de Session

### 1.1 Inscription d'un nouvel utilisateur
1. Rendez-vous sur [http://localhost/auth/register](http://localhost/auth/register).
2. Remplissez le formulaire :
   - **Nom** : `Administrateur`
   - **Prénom** : `Test`
   - **Email** : `admin@distribution-cameroun.cm`
   - **Mot de passe** : `MotDePasseFort123!`
   - **Nom de l'entreprise** : `CAMEROUN DISTRIBUTION SARL`
   - **Code Entreprise** : `CAMDIST`
3. Cliquez sur **S'inscrire**.
4. **Vérification** : Vous devez être automatiquement redirigé vers la bibliothèque de modèles (`/bibliotheque`) avec un jeton JWT valide stocké dans `localStorage`.

### 1.2 Déconnexion et Reconnexion
1. Cliquez sur l'icône de profil / bouton **Déconnexion** en haut à droite.
2. Vérifiez que vous êtes redirigé vers `/auth/login`.
3. Tentez d'accéder directement à [http://localhost/bibliotheque](http://localhost/bibliotheque) sans session.
   - **Vérification** : Le Guard d'authentification Angular vous renvoie vers `/auth/login`.
4. Connectez-vous avec :
   - **Email** : `admin@distribution-cameroun.cm`
   - **Mot de passe** : `MotDePasseFort123!`
5. **Vérification** : Connexion réussie, affichage du dashboard principal.

---

## Étape 2 : Configuration de la Feuille de Travail Entreprise

### Objectif
Configurer les paramètres d'impression et de mise en page par défaut pour l'entreprise (`CAMDIST`).

1. Cliquez sur le menu de navigation : **Configuration** ou rendez-vous sur [http://localhost/feuille-travail](http://localhost/feuille-travail).
2. Vérifiez et modifiez les paramètres :
   - **Format Papier** : `A4` (Portrait)
   - **Marges** :
     - Haut : `15 mm`
     - Bas : `15 mm`
     - Gauche : `10 mm`
     - Droite : `10 mm`
   - **En-tête (Header)** :
     - Activer : ✅
     - Hauteur : `20 mm`
     - Contenu : `CAMEROUN DISTRIBUTION SARL – Siège Social Douala`
     - Ligne de séparation : ✅
   - **Pied de page (Footer)** :
     - Activer : ✅
     - Hauteur : `15 mm`
     - Numérotation de page : ✅ Format `PAGE_X_SUR_Y` ("Page X sur Y")
     - Contenu : `Document généré automatiquement - Tous droits réservés`
3. Cliquez sur **Enregistrer la configuration**.
4. **Vérification** : Un message de succès apparaît. Rechargez la page (F5) et constatez que les données sont bien persistées en base.

---

## Étape 3 : Création & Design d'un Modèle de Rapport

### 3.1 Création d'un nouveau modèle
1. Accédez à [http://localhost/templates/new](http://localhost/templates/new) ou cliquez sur **Nouveau Modèle**.
2. Remplissez :
   - **Nom du Modèle** : `RAPPORT MENSUEL DES VENTES`
   - **Catégorie** : `VENTES` ou `COMMERCIAL`
   - **Description** : `Rapport mensuel d'activité et synthèse financière pour entreprise de distribution`
   - **Format** : `A4`
3. Cliquez sur **Créer et Ouvrir le Studio**.

### 3.2 Utilisation du Studio Designer
1. Vous êtes redirigé vers l'éditeur visuel : `/templates/<id_du_template>`.
2. **Ajout des blocs de composants** :
   - **Titre principal** : Glissez un composant Titre / En-tête : `RAPPORT MENSUEL DES VENTES`.
   - **Informations En-tête** :
     - Nom de l'entreprise : `{{entreprise.nom}}`
     - Période : `{{rapport.periode}}`
     - Date de génération : `{{rapport.date_generation}}`
   - **Cartes KPIs (4 Indicateurs)** :
     - Chiffre d'Affaires : `{{statistiques.chiffre_affaires}} FCFA`
     - Nombre de commandes : `{{statistiques.commandes}}`
     - Nombre de clients : `{{statistiques.clients}}`
     - Produit phare : `{{statistiques.top_produit}}`
   - **Tableau dynamique des Ventes** :
     - Glissez un composant **Tableau**.
     - Nommez la collection répétée : `ventes`.
     - Définissez les colonnes :
       - `Date` : `{{item.date}}`
       - `N° Commande` : `{{item.commande}}`
       - `Client` : `{{item.client}}`
       - `Produit` : `{{item.produit}}`
       - `Quantité` : `{{item.quantite}}`
       - `Prix Unitaire` : `{{item.prix_unitaire}} FCFA`
       - `Total Ligne` : `{{item.total}} FCFA`
   - **Synthèse Financière** :
     - Sous-total : `{{finances.sous_total}} FCFA`
     - Remise : `{{finances.remise}} FCFA`
     - TVA (19.25%) : `{{finances.tva}} FCFA`
     - **Total TTC** : `{{finances.total_ttc}} FCFA`
3. Cliquez sur **Sauvegarder le modèle** (icône disquette ou bouton Sauvegarder).
4. **Vérification** : Notification de succès, la version s'incrémente ou s'enregistre sans erreur.

---

## Étape 4 : Injection de Données & Génération PDF (Gotenberg)

### 4.1 Injection d'un jeu de données JSON de test
Dans l'onglet **Données de test / Génération** du template, insérez le JSON suivant :

```json
{
  "entreprise": {
    "nom": "CAMEROUN DISTRIBUTION SARL",
    "adresse": "Akwa, Boulevard de la Liberté, Douala",
    "telephone": "+237 699 00 11 22",
    "email": "contact@camdistribution.cm"
  },
  "rapport": {
    "titre": "RAPPORT MENSUEL DES VENTES",
    "periode": "Septembre 2026",
    "date_generation": "04/10/2026"
  },
  "statistiques": {
    "chiffre_affaires": "14 850 000",
    "commandes": "128",
    "clients": "45",
    "top_produit": "Huile de Palme Raffinée 5L"
  },
  "ventes": [
    {
      "date": "02/09/2026",
      "commande": "CMD-2026-089",
      "client": "Supermarché Dovv Yaoundé",
      "produit": "Huile de Palme Raffinée 5L",
      "quantite": 50,
      "prix_unitaire": "8 500",
      "total": "425 000"
    },
    {
      "date": "05/09/2026",
      "commande": "CMD-2026-090",
      "client": "Boutique Le Bon Marché Douala",
      "produit": "Riz Parfumé 25kg",
      "quantite": 30,
      "prix_unitaire": "18 000",
      "total": "540 000"
    },
    {
      "date": "12/09/2026",
      "commande": "CMD-2026-091",
      "client": "Alimentation Générale Bafoussam",
      "produit": "Sucre en Poudre 1kg (Carton de 20)",
      "quantite": 40,
      "prix_unitaire": "14 500",
      "total": "580 000"
    },
    {
      "date": "18/09/2026",
      "commande": "CMD-2026-092",
      "client": "Hôtel Sawa Douala",
      "produit": "Farine de Blé Supérieure 50kg",
      "quantite": 20,
      "prix_unitaire": "24 000",
      "total": "480 000"
    }
  ],
  "finances": {
    "sous_total": "2 025 000",
    "remise": "25 000",
    "tva": "385 000",
    "total_ttc": "2 385 000"
  },
  "analyse": {
    "evolution": "Croissance des ventes de +14% par rapport à août 2026.",
    "top_produits": "1. Huile 5L, 2. Riz 25kg, 3. Farine 50kg.",
    "clients_fissures": "Forte hausse des commandes dans la région du Littoral et de l'Ouest.",
    "observations": "Prévoir un réapprovisionnement d'huile de palme avant la mi-octobre pour éviter toute rupture."
  }
}
```

### 4.2 Test de Génération PDF
1. Cliquez sur **Générer le PDF** ou **Prévisualiser**.
2. **Vérifications visuelles** :
   - Le moteur Gotenberg traite la requête Chromium sans erreur 422 ou 500.
   - Les en-têtes et pieds de page définis à l'Étape 2 s'affichent correctement.
   - La numérotation de page indique bien `Page 1 sur X`.
   - Les devises sont bien formatées avec `FCFA`.
   - Le tableau répète bien les 4 lignes de ventes sans chevauchement.

---

## Étape 5 : Stockage MinIO S3 & Gestion des Documents

### 5.1 Vérification de la liste des documents générés
1. Rendez-vous sur [http://localhost/documents](http://localhost/documents).
2. **Vérification** : Le document fraîchement généré apparaît dans la liste :
   - Statut : `COMPLETED` ou `GENERATED`
   - Date de création : Date et heure courantes
   - Bouton d'action : **Télécharger** / **Visualiser**
3. Cliquez sur **Télécharger** : Le fichier PDF s'ouvre ou se télécharge directement sur votre machine.

### 5.2 Contrôle dans l'Object Storage MinIO
1. Ouvrez [http://localhost:9001](http://localhost:9001) dans un nouvel onglet.
2. Connectez-vous avec `minioadmin` / `minioadmin`.
3. Cliquez sur **Buckets** puis sur `rapports-pdf`.
4. **Vérification** : Le fichier PDF y est stocké avec une clé unique (UUID).

---

## Étape 6 : Traitements par Lots (Batches) avec CSV/Excel

### Objectif
Vérifier la capacité du système à générer plusieurs rapports à la chaîne à partir d'un fichier source tabulaire.

1. Rendez-vous sur [http://localhost/batches](http://localhost/batches).
2. Cliquez sur **Nouveau traitement par lot**.
3. Sélectionnez le modèle `RAPPORT MENSUEL DES VENTES`.
4. Téléversez le fichier exemple présent à la racine du projet :
   - Fichier : `donnees_factures_10_rapports.csv` (ou `.xlsx`).
5. Lancez le traitement.
6. **Vérifications** :
   - La barre de progression s'incrémente de 0% à 100%.
   - Le statut passe à `COMPLETED`.
   - Les 10 rapports générés sont téléchargeables individuellement ou sous forme d'archive ZIP.

---

## Étape 7 : Sources de Données Externes & Planifications (Cron)

### 7.1 Sources de données (Data Sources)
1. Rendez-vous sur [http://localhost/data-sources](http://localhost/data-sources).
2. Cliquez sur **Ajouter une source de données**.
3. Remplissez :
   - Nom : `API Mock Produits`
   - Type : `REST_API`
   - URL : `https://jsonplaceholder.typicode.com/posts` (ou API interne)
   - Méthode : `GET`
4. Cliquez sur **Tester la connexion**.
   - **Vérification** : Statut vert "Connexion réussie".

### 7.2 Tâches planifiées (Schedules)
1. Rendez-vous sur [http://localhost/schedules](http://localhost/schedules).
2. Créez un job planifié :
   - Modèle : `RAPPORT MENSUEL DES VENTES`
   - Expression Cron : `0 0 1 * * ?` (Tous les 1ers du mois à minuit)
   - Statut : `Actif`
3. Enregistrez.
   - **Vérification** : Le job apparaît dans la liste avec sa prochaine date d'exécution calculée.

---

## Étape 8 : Certificats Numériques & Piste d'Audit

### 8.1 Gestion des Certificats de signature
1. Rendez-vous sur [http://localhost/certificates](http://localhost/certificates).
2. Constatez la présence de la gestion des certificats d'entreprise pour la signature électronique des rapports PDF.

### 8.2 Piste d'Audit & Gouvernance
1. Rendez-vous sur [http://localhost/audit](http://localhost/audit).
2. **Vérification** : Consultez les entrées du journal d'audit :
   - Action `USER_LOGIN` : Connexion de l'administrateur
   - Action `TEMPLATE_CREATE` ou `TEMPLATE_UPDATE` : Création et modifications de template
   - Action `REPORT_GENERATE` : Génération du rapport de ventes avec son identifiant et statut
   - L'IP client, l'horodatage et le code entreprise `CAMDIST` sont scrupuleusement enregistrés.

---

## Étape 9 : Exécution des Suites de Tests Automatisés

Pour valider l'intégrité du code source sans régression :

### 9.1 Tests Unitaires & Intégration Backend (Spring Boot 3.5 / JUnit 5)
Ouvrez un terminal dans le dossier `backend` :
```powershell
cd d:\programme\Mera_Project\moteur-rapports\backend
mvn test
```
- **Résultat attendu** : 46 classes de tests exécutées, **247+ tests réussis**, 0 échec, 0 erreur.

### 9.2 Tests E2E Frontend (Playwright)
Ouvrez un terminal dans le dossier `frontend` :
```powershell
cd d:\programme\Mera_Project\moteur-rapports\frontend
npx playwright test
```
- **Résultat attendu** : Exécution des scénarios navigateurs (authentification, studio designer, import Excel, pagination).

---

## Matrice de Recette & Checklist Finale

Cochez chaque case au fur et à mesure de vos validations :

- [ ] **Étape 0** : Tous les conteneurs Docker sont `Up (healthy)`
- [ ] **Étape 1** : Inscription et connexion JWT fonctionnelles
- [ ] **Étape 2** : Configuration de la feuille A4 (marges, header, footer) persistée
- [ ] **Étape 3** : Template de vente créé avec tableau dynamique et calculs
- [ ] **Étape 4** : PDF généré via Gotenberg avec mise en page et montants FCFA
- [ ] **Étape 5** : Fichier PDF sauvegardé dans MinIO et téléchargeable via `/documents`
- [ ] **Étape 6** : Batch de 10 rapports CSV traité avec succès
- [ ] **Étape 7** : Source de données testée et job Cron enregistré
- [ ] **Étape 8** : Traces d'audit enregistrées et consultables sur `/audit`
- [ ] **Étape 9** : 100% des tests unitaires backend et E2E frontend au vert
