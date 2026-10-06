# 📋 Cahier & Plan de Recette Global – Moteur de Rapports v2

Ce document est un guide pas-à-pas conçu pour tester l'intégralité de la plateforme **Moteur de Rapports** (Frontend Angular, Backend Spring Boot, Moteur Gotenberg, Stockage MinIO S3, Base PostgreSQL).

> [!IMPORTANT]
> **Règle essentielle sur les variables et les données :**
> 1. **Pas de points dans les noms de variables** : Le moteur de validation de l'application (`DataValidatorService` et le validateur Angular `/^[a-zA-Z0-9_]+$/`) utilise des clés de premier niveau en `snake_case` (ex: `entreprise_nom`, `rapport_periode`, `total_ttc`). N'utilisez pas `entreprise.nom` ni `rapport.periode`.
> 2. **Tableaux dynamiques** : Le tableau se lie à une variable source de type `ARRAY` (ex: `ventes`). Chaque colonne renseigne directement la propriété de l'objet (ex: `date`, `client`, `quantite`, `prix_unitaire`, `total`), sans préfixe `{{item.}}`.
> 3. **Publication obligatoire** : Un modèle doit obligatoirement être **Publié** (`PUBLIE`) via le bouton *Publier* de l'éditeur avant de pouvoir générer un document. Un modèle en statut *Brouillon* (`BROUILLON`) refuse la génération avec une erreur 400/422.
> 4. **Valeurs numériques** : Les montants et quantités dans le JSON doivent être des nombres purs (ex: `14850000`, `8500`) et non des chaînes avec espaces (ex: `"14 850 000"`), car le backend valide le type `FLOAT` avec `Double.parseDouble()`. Le symbole `FCFA` s'ajoute dans le modèle ou par formule.

---

## 📑 Sommaire
1. [Étape 0 : Vérification Préalable de l'Environnement Docker](#étape-0--vérification-préalable-de-lenvironnement-docker)
2. [Étape 1 : Authentification & Gestion de Session](#étape-1--authentification--gestion-de-session)
3. [Étape 2 : Configuration de la Feuille de Travail Entreprise](#étape-2--configuration-de-la-feuille-de-travail-entreprise)
4. [Étape 3 : Création & Design d'un Modèle de Rapport](#étape-3--création--design-dun-modèle-de-rapport)
5. [Étape 4 : Publication du Modèle & Génération PDF (Gotenberg)](#étape-4--publication-du-modèle--génération-pdf-gotenberg)
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
- **Frontend** : Ouvrir [http://localhost](http://localhost) dans votre navigateur.
- **Backend Health** : Ouvrir [http://localhost:8082/api/health](http://localhost:8082/api/health) (retourne `{"status":"UP"}`).
- **Moteur PDF Gotenberg** : Ouvrir [http://localhost:3000/health](http://localhost:3000/health) (retourne `{"status":"up"}`).
- **Console MinIO** : Ouvrir [http://localhost:9001](http://localhost:9001) (identifiants: `minioadmin` / `minioadmin`).
- **Swagger OpenAPI** : Ouvrir [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html).

---

## Étape 1 : Authentification & Gestion de Session

### 1.1 Inscription d'un compte entreprise
1. Rendez-vous sur [http://localhost/auth/register](http://localhost/auth/register).
2. Renseignez :
   - **Nom** : `Administrateur`
   - **Prénom** : `Test`
   - **Email** : `admin@distribution-cameroun.cm`
   - **Mot de passe** : `MotDePasseFort123!`
   - **Nom de l'entreprise** : `CAMEROUN DISTRIBUTION SARL`
   - **Code Entreprise** : `CAMDIST`
3. Cliquez sur **S'inscrire**.
4. **Vérification** : Redirection vers `/bibliotheque`, l'en-tête affiche l'utilisateur connecté et son code entreprise `CAMDIST`.

### 1.2 Déconnexion & Contrôle du Guard
1. Cliquez sur le bouton de déconnexion en haut à droite.
2. Tentez d'accéder directement à [http://localhost/bibliotheque](http://localhost/bibliotheque).
   - **Vérification** : Redirection automatique vers `/auth/login`.
3. Reconnectez-vous avec `admin@distribution-cameroun.cm` et `MotDePasseFort123!`.
   - **Vérification** : Connexion réussie et accès rétabli.

---

## Étape 2 : Configuration de la Feuille de Travail Entreprise

### Objectif
Définir les en-têtes, pieds de page et marges par défaut pour tous les futurs rapports de l'entreprise `CAMDIST`.

1. Accédez à [http://localhost/feuille-travail](http://localhost/feuille-travail).
2. Renseignez les paramètres suivants :
   - **Format Papier** : `A4`
   - **Orientation** : `Portrait`
   - **Marges** :
     - Haut : `15 mm`
     - Bas : `15 mm`
     - Gauche : `10 mm`
     - Droite : `10 mm`
   - **En-tête (Header)** :
     - Activer : ✅
     - Hauteur : `20 mm`
     - Contenu : `CAMEROUN DISTRIBUTION SARL – Siège Social Akwa Douala`
     - Ligne de séparation : ✅
   - **Pied de page (Footer)** :
     - Activer : ✅
     - Hauteur : `15 mm`
     - Numérotation de page : ✅ Format `PAGE_X_SUR_Y` ("Page X sur Y")
     - Contenu : `Document généré automatiquement - Tous droits réservés`
3. Cliquez sur **Enregistrer la configuration**.
4. **Vérification** : Un message de confirmation s'affiche et les données sont conservées après un rechargement de page (F5).

---

## Étape 3 : Création & Design d'un Modèle de Rapport

### 3.1 Création du Modèle
1. Rendez-vous sur [http://localhost/templates/new](http://localhost/templates/new).
2. Remplissez :
   - **Nom du Modèle** : `RAPPORT MENSUEL DES VENTES`
   - **Catégorie** : `VENTES`
   - **Description** : `Rapport mensuel des ventes et synthèse financière pour entreprise de distribution`
   - **Format** : `A4`
3. Cliquez sur **Créer et Ouvrir le Studio**.

### 3.2 Déclaration des Variables dans l'onglet "Variables"
Dans la barre latérale gauche de l'éditeur, basculez sur l'onglet **Variables** et ajoutez les variables requises (le formulaire valide le motif `/^[a-zA-Z0-9_]+$/`) :

| Nom de variable | Type | Obligatoire | Description |
| :--- | :--- | :--- | :--- |
| `entreprise_nom` | `STRING` | Non | Nom de la société |
| `entreprise_adresse` | `STRING` | Non | Adresse à Douala |
| `entreprise_telephone` | `STRING` | Non | Téléphone |
| `entreprise_email` | `STRING` | Non | Email contact |
| `rapport_periode` | `STRING` | Oui | Période (ex: Septembre 2026) |
| `rapport_date` | `STRING` | Oui | Date de génération |
| `stat_ca` | `FLOAT` | Oui | Chiffre d'affaires en FCFA |
| `stat_commandes` | `FLOAT` | Oui | Nombre de commandes |
| `stat_clients` | `FLOAT` | Oui | Nombre de clients |
| `stat_top_produit` | `STRING` | Non | Produit le plus vendu |
| `sous_total` | `FLOAT` | Oui | Sous-total en FCFA |
| `remise` | `FLOAT` | Non | Montant remise en FCFA |
| `tva` | `FLOAT` | Oui | Montant TVA (19.25%) |
| `total_ttc` | `FLOAT` | Oui | Montant Total Net TTC |
| `analyse_evolution` | `STRING` | Non | Commentaire sur l'évolution |
| `analyse_observations` | `STRING` | Non | Recommandations |
| `ventes` | `ARRAY` | Oui | Collection dynamique des ventes |

### 3.3 Composition des Blocs sur la Page
Dans l'onglet **Composants** (ou Bibliothèque) :

1. **En-tête & Coordonnées** :
   - Ajoutez un bloc **Titre** : `RAPPORT MENSUEL DES VENTES` (Centré, Gras, taille 20).
   - Ajoutez un bloc **Texte** pour les coordonnées :
     ```text
     {{entreprise_nom}}
     {{entreprise_adresse}} | Tél : {{entreprise_telephone}} | Email : {{entreprise_email}}
     Période : {{rapport_periode}} | Émis le : {{rapport_date}}
     ```

2. **Indicateurs Clés (KPIs)** :
   - Ajoutez 4 blocs **Texte** ou cartouches pour afficher :
     - `Chiffre d'Affaires : {{stat_ca}} FCFA`
     - `Commandes : {{stat_commandes}}`
     - `Clients actifs : {{stat_clients}}`
     - `Top Produit : {{stat_top_produit}}`

3. **Tableau Dynamique des Ventes** :
   - Ajoutez un bloc **Tableau**.
   - Dans le panneau de droite (Éditeur de bloc), cochez/sélectionnez le **Mode Dynamique**.
   - **Source (variable Tableau)** : Sélectionnez `ventes`.
   - Configurez les colonnes :
     - Colonne 1 $\rightarrow$ Titre : `Date` \| Variable : `date`
     - Colonne 2 $\rightarrow$ Titre : `N° Commande` \| Variable : `commande`
     - Colonne 3 $\rightarrow$ Titre : `Client` \| Variable : `client`
     - Colonne 4 $\rightarrow$ Titre : `Produit` \| Variable : `produit`
     - Colonne 5 $\rightarrow$ Titre : `Quantité` \| Variable : `quantite`
     - Colonne 6 $\rightarrow$ Titre : `P.U. (FCFA)` \| Variable : `prix_unitaire`
     - Colonne 7 $\rightarrow$ Titre : `Total (FCFA)` \| Variable : `total` *(ou laissez vide avec Formule : `quantite * prix_unitaire`)*

4. **Synthèse Financière & Analyse** :
   - Ajoutez un bloc **Texte** pour les totaux :
     ```text
     Sous-total : {{sous_total}} FCFA
     Remise accordée : {{remise}} FCFA
     TVA (19.25%) : {{tva}} FCFA
     TOTAL TTC : {{total_ttc}} FCFA
     ```
   - Ajoutez un bloc **Texte** pour l'analyse :
     ```text
     Évolution : {{analyse_evolution}}
     Observations : {{analyse_observations}}
     ```

5. **Sauvegarde** :
   - Cliquez sur **Enregistrer** (icône disquette en haut).
   - **Vérification** : Message "Enregistré avec succès".

---

## Étape 4 : Publication du Modèle & Génération PDF (Gotenberg)

### 4.1 Publication du Modèle (OBLIGATOIRE)
> [!WARNING]
> Avant de générer, le modèle doit être publié !
1. Cliquez sur le bouton **Publier** dans la barre d'outils supérieure de l'éditeur de modèle.
2. Confirmez la publication.
3. **Vérification** : Le badge du statut passe de `BROUILLON` à `PUBLIE`.

### 4.2 Injection du Jeu de Données de Test Conforme
Dans le panneau de génération / test du template, utilisez le payload JSON valide ci-dessous :

```json
{
  "entreprise_nom": "CAMEROUN DISTRIBUTION SARL",
  "entreprise_adresse": "Akwa, Boulevard de la Liberté, Douala",
  "entreprise_telephone": "+237 699 00 11 22",
  "entreprise_email": "contact@camdistribution.cm",
  "rapport_periode": "Septembre 2026",
  "rapport_date": "04/10/2026",
  "stat_ca": 14850000,
  "stat_commandes": 128,
  "stat_clients": 45,
  "stat_top_produit": "Huile de Palme Raffinée 5L",
  "sous_total": 2025000,
  "remise": 25000,
  "tva": 385000,
  "total_ttc": 2385000,
  "analyse_evolution": "Croissance des ventes de +14% par rapport au mois précédent.",
  "analyse_observations": "Prévoir un réapprovisionnement avant la mi-octobre pour éviter les ruptures.",
  "ventes": [
    {
      "date": "02/09/2026",
      "commande": "CMD-2026-089",
      "client": "Supermarché Dovv Yaoundé",
      "produit": "Huile de Palme Raffinée 5L",
      "quantite": 50,
      "prix_unitaire": 8500,
      "total": 425000
    },
    {
      "date": "05/09/2026",
      "commande": "CMD-2026-090",
      "client": "Boutique Le Bon Marché Douala",
      "produit": "Riz Parfumé 25kg",
      "quantite": 30,
      "prix_unitaire": 18000,
      "total": 540000
    },
    {
      "date": "12/09/2026",
      "commande": "CMD-2026-091",
      "client": "Alimentation Générale Bafoussam",
      "produit": "Sucre en Poudre 1kg (Carton de 20)",
      "quantite": 40,
      "prix_unitaire": 14500,
      "total": 580000
    },
    {
      "date": "18/09/2026",
      "commande": "CMD-2026-092",
      "client": "Hôtel Sawa Douala",
      "produit": "Farine de Blé Supérieure 50kg",
      "quantite": 20,
      "prix_unitaire": 24000,
      "total": 480000
    }
  ]
}
```

### 4.3 Génération & Rendu PDF
1. Cliquez sur **Générer le PDF** (ou **Générer avec ces données**).
2. **Vérifications attendues** :
   - Aucune erreur 422 ni 400 dans la console navigateur.
   - Le PDF s'affiche dans la prévisualisation ou se télécharge directement (`rapport.pdf`).
   - Le moteur Gotenberg applique la typographie propre et les largeurs de colonnes.
   - L'en-tête et le pied de page d'entreprise configurés à l'Étape 2 apparaissent avec `Page 1 sur 1`.

---

## Étape 5 : Stockage MinIO S3 & Gestion des Documents

### 5.1 Consultation de l'Historique dans l'Application
1. Cliquez sur le menu **Documents** ou accédez à [http://localhost/documents](http://localhost/documents).
2. **Vérification** :
   - Le document généré apparaît avec le nom du modèle `RAPPORT MENSUEL DES VENTES`.
   - Le statut est `SUCCES` ou `COMPLETED`.
   - Cliquez sur **Télécharger** : Le fichier PDF binaire est restitué instantanément.

### 5.2 Contrôle de l'Object Storage MinIO
1. Ouvrez [http://localhost:9001](http://localhost:9001) dans un nouvel onglet.
2. Connectez-vous avec `minioadmin` / `minioadmin`.
3. Cliquez sur **Buckets** puis sur `rapports-pdf`.
4. Ouvrez le dossier `entreprises/CAMDIST/reports/`.
5. **Vérification** : Le fichier `.pdf` y est présent, attestant de l'isolation multi-tenant des données dans MinIO S3.

---

## Étape 6 : Traitements par Lots (Batches) avec CSV/Excel

### Objectif
Vérifier l'importation de fichiers tabulaires pour générer plusieurs rapports à la chaîne en arrière-plan.

1. Rendez-vous sur [http://localhost/batches](http://localhost/batches).
2. Cliquez sur **Nouveau traitement par lot**.
3. Sélectionnez le modèle `RAPPORT MENSUEL DES VENTES` (ou `Facture commerciale`).
4. Téléversez le fichier exemple fourni à la racine du projet :
   - Fichier : [`donnees_factures_10_rapports.csv`](file:///d:/programme/Mera_Project/moteur-rapports/donnees_factures_10_rapports.csv) (ou `.xlsx`).
5. Lancez le traitement.
6. **Vérifications** :
   - La barre de progression avance jusqu'à 100%.
   - Le statut du lot passe à `TERMINE` (`COMPLETED`).
   - Les rapports générés peuvent être téléchargés unitairement ou en archive ZIP.

---

## Étape 7 : Sources de Données Externes & Planifications (Cron)

### 7.1 Connecteurs de Données (Data Sources)
1. Rendez-vous sur [http://localhost/data-sources](http://localhost/data-sources).
2. Cliquez sur **Ajouter une source de données**.
3. Paramètres de test :
   - Nom : `API Produits Externes`
   - Type : `REST_API`
   - URL : `https://jsonplaceholder.typicode.com/posts`
   - Méthode : `GET`
4. Cliquez sur **Tester la connexion**.
   - **Vérification** : Indicateur vert « Connexion réussie ».

### 7.2 Tâches Planifiées (Schedules)
1. Rendez-vous sur [http://localhost/schedules](http://localhost/schedules).
2. Cliquez sur **Nouvelle planification**.
3. Paramètres :
   - Modèle : `RAPPORT MENSUEL DES VENTES`
   - Expression Cron : `0 0 1 * * ?` (Chaque 1er jour du mois à minuit)
   - Actif : ✅
4. Enregistrez.
   - **Vérification** : Le job apparaît dans la liste avec sa prochaine échéance calculée.

---

## Étape 8 : Certificats Numériques & Piste d'Audit

### 8.1 Gestion des Certificats de Signature
1. Rendez-vous sur [http://localhost/certificates](http://localhost/certificates).
2. Vérifiez la présence du module de gestion des certificats PKCS#12 pour la signature cryptographique des PDF d'entreprise.

### 8.2 Piste d'Audit & Gouvernance
1. Rendez-vous sur [http://localhost/audit](http://localhost/audit).
2. **Vérification** : Examinez le journal des événements immuables :
   - Événement `USER_LOGIN` lors de votre connexion.
   - Événements `TEMPLATE_CREATE` et `TEMPLATE_PUBLISH`.
   - Événement `DOCUMENT_GENERATE` avec l'ID de la génération et le code entreprise `CAMDIST`.
   - Toutes les entrées comportent l'adresse IP, le nom de l'utilisateur et la date exacte.

---

## Étape 9 : Exécution des Suites de Tests Automatisés

Pour valider le bon fonctionnement global au niveau du code :

### 9.1 Tests Unitaires & Intégration Backend (Spring Boot / JUnit 5)
Dans un terminal PowerShell :
```powershell
cd d:\programme\Mera_Project\moteur-rapports\backend
mvn test
```
- **Résultat attendu** : 46 classes de tests exécutées, **247+ tests réussis**, 0 échec, 0 erreur.

### 9.2 Tests E2E Frontend (Playwright)
Dans un terminal PowerShell :
```powershell
cd d:\programme\Mera_Project\moteur-rapports\frontend
npx playwright test
```
- **Résultat attendu** : Validation des scénarios E2E navigateurs (Auth, Designer, Imports, Pagination).

---

## Matrice de Recette & Checklist Finale

- [ ] **Étape 0** : 5/5 conteneurs Docker `Up (healthy)`
- [ ] **Étape 1** : Inscription et connexion JWT sous le code `CAMDIST` validées
- [ ] **Étape 2** : Configuration de la feuille A4 sauvegardée et persistée
- [ ] **Étape 3** : Modèle créé avec variables plates (`[a-zA-Z0-9_]+`) et tableau dynamique lié à `ventes`
- [ ] **Étape 4** : Modèle passé en statut **PUBLIE** et PDF généré avec succès via Gotenberg (sans erreur 422)
- [ ] **Étape 5** : Fichier PDF archivé dans MinIO (`entreprises/CAMDIST/reports/`) et téléchargeable depuis `/documents`
- [ ] **Étape 6** : Batch de 10 rapports CSV traité à 100%
- [ ] **Étape 7** : Source REST connectée et job Cron créé
- [ ] **Étape 8** : Audit trail vérifié dans `/audit`
- [ ] **Étape 9** : Suites de tests automatisés validées (Backend + Frontend)
