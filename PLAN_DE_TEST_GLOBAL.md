# 📋 Cahier & Plan de Recette Global – Moteur de Rapports v2

Ce document est un guide pas-à-pas conçu pour tester l'intégralité de la plateforme **Moteur de Rapports** (Frontend Angular, Backend Spring Boot, Moteur Gotenberg, Stockage MinIO S3, Base PostgreSQL).

> [!IMPORTANT]
> **Règles fondamentales du Moteur de Rapports :**
> 1. **Noms de variables valides** : Utilisez impérativement des variables en `snake_case` (ex: `entreprise_nom`, `rapport_periode`, `total_ttc`). N'utilisez **jamais de points** (`entreprise.nom` est interdit par le validateur regex `/^[a-zA-Z0-9_]+$/`).
> 2. **Retours à la ligne respectés** : Les sauts de ligne saisis dans vos blocs texte ou transmis dans les variables sont fidèlement conservés dans le rendu HTML et dans le PDF généré grâce à `white-space: pre-wrap; word-break: break-word;`.
> 3. **Publication obligatoire pour la génération** : Un modèle doit être au statut **Publié** (`PUBLIE`) via le bouton *Publier* de la barre d'outils pour autoriser la génération de PDF finaux.
> 4. **Enregistrement de document vs Téléchargement direct** :
>    - **Générer le PDF** : compile le document à la volée, le stocke dans MinIO et lance son téléchargement direct.
>    - **Enregistrer le document** (dans l'onglet *Remplir le modèle*) : sauvegarde une instance réutilisable dans la base de données, visible immédiatement dans le menu **Documents** (`/documents`) pour la visionneuse intégrée, l'envoi d'emails et les ré-exports.
> 5. **Données de traitement par lot (Batches)** : Le fichier CSV ou Excel téléversé doit contenir les colonnes correspondant exactement aux variables obligatoires du modèle sélectionné. Pour le modèle *RAPPORT MENSUEL DES VENTES*, utilisez [`donnees_ventes_10_rapports.csv`](file:///d:/programme/Mera_Project/moteur-rapports/donnees_ventes_10_rapports.csv) ou [`donnees_ventes_sample_batch.json`](file:///d:/programme/Mera_Project/moteur-rapports/donnees_ventes_sample_batch.json).

---

## 📑 Sommaire
1. [Étape 0 : Vérification Préalable de l'Environnement Docker](#étape-0--vérification-préalable-de-lenvironnement-docker)
2. [Étape 1 : Authentification & Gestion de Session](#étape-1--authentification--gestion-de-session)
3. [Étape 2 : Configuration de la Feuille de Travail Entreprise](#étape-2--configuration-de-la-feuille-de-travail-entreprise)
4. [Étape 3 : Conception du Modèle dans le Designer](#étape-3--conception-du-modèle-dans-le-designer)
5. [Étape 4 : Publication du Modèle, Remplissage & Génération PDF](#étape-4--publication-du-modèle-remplissage--génération-pdf)
6. [Étape 5 : Gestion des Documents & Stockage MinIO S3](#étape-5--gestion-des-documents--stockage-minio-s3)
7. [Étape 6 : Traitements par Lots (Batches) avec CSV/Excel](#étape-6--traitements-par-lots-batches-avec-csvexcel)
8. [Étape 7 : Connecteurs de Données Distants & Planifications Cron](#étape-7--connecteurs-de-données-distants--planifications-cron)
9. [Étape 8 : Sécurité Cryptographique & Piste d'Audit](#étape-8--sécurité-cryptographique--piste-daudit)
10. [Étape 9 : Validation des Suites de Tests Automatisés](#étape-9--validation-des-suites-de-tests-automatisés)
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

### 1.1 Connexion ou Inscription d'un compte entreprise
1. Rendez-vous sur [http://localhost/auth/login](http://localhost/auth/login) (ou `/auth/register` si vous n'avez pas de compte).
2. Connectez-vous avec vos identifiants (ex: `yandzurodriguez@gmail.com` ou `admin@camdist.cm`).
3. **Vérification** :
   - Redirection fluide vers la bibliothèque `/bibliotheque`.
   - Votre nom et votre badge d'entreprise `CAMDIST` s'affichent dans la barre de navigation.
   - Les appels API incluent automatiquement le header Bearer JWT et `X-Entreprise-Code`.

---

## Étape 2 : Configuration de la Feuille de Travail Entreprise

1. Cliquez sur le menu **Feuille de travail** (ou accédez à [http://localhost/feuille-travail](http://localhost/feuille-travail)).
2. Configurez les propriétés par défaut de l'entreprise :
   - Format de page : `A4`
   - Marges : `10 mm` partout
   - En-tête récurrent : Actif (`CAMEROUN DISTRIBUTION SARL - Rapport Officiel`)
   - Pied de page récurrent : Actif avec numérotation de page automatique (`Page {page} / {pages}`)
3. Cliquez sur **Enregistrer les paramètres**.
4. **Vérification** : Message de confirmation et persistance en base de données.

---

## Étape 3 : Conception du Modèle dans le Designer

### 3.1 Création du Modèle
1. Cliquez sur **Nouveau modèle** dans la bibliothèque.
2. Nom : `RAPPORT MENSUEL DES VENTES`
3. Catégorie : `Rapport d'activité` (ou `Ventes`)
4. Description : `Rapport mensuel détaillé des performances de ventes et chiffres d'affaires`
5. Cliquez sur **Créer le modèle**.

### 3.2 Déclaration des Variables dans l'onglet "Variables"
Dans la barre latérale gauche de l'éditeur, sélectionnez l'onglet **Variables** :

| Nom de variable | Type | Obligatoire | Description |
| :--- | :--- | :--- | :--- |
| `entreprise_nom` | `STRING` | Non | Nom de la société |
| `entreprise_adresse` | `STRING` | Non | Adresse Douala |
| `entreprise_telephone` | `STRING` | Non | Téléphone |
| `entreprise_email` | `STRING` | Non | Email contact |
| `rapport_periode` | `STRING` | Oui | Période (ex: Janvier 2026) |
| `rapport_date` | `STRING` | Oui | Date de génération |
| `stat_ca` | `FLOAT` | Oui | Chiffre d'affaires en FCFA |
| `stat_commandes` | `FLOAT` | Oui | Nombre de commandes |
| `stat_clients` | `FLOAT` | Oui | Nombre de clients |
| `stat_top_produit` | `STRING` | Oui | Produit phare |
| `sous_total` | `FLOAT` | Oui | Sous-total |
| `remise` | `FLOAT` | Non | Remise accordée |
| `tva` | `FLOAT` | Oui | TVA |
| `total_ttc` | `FLOAT` | Oui | Total TTC net |
| `analyse_evolution` | `STRING` | Non | Commentaire commercial |
| `analyse_observations` | `STRING` | Non | Recommandations |
| `ventes` | `ARRAY` | Oui | Tableau dynamique des ventes |

### 3.3 Composition des Blocs avec Retours à la Ligne
Dans l'onglet **Composants** :
1. **Titre** :
   - Type `titre` : `RAPPORT MENSUEL DES VENTES` (Centré, Gras, 20px).
2. **Coordonnées (avec retours à la ligne `\n`)** :
   - Type `texte` :
     ```text
     Nom : {{entreprise_nom}}
     Adresse : {{entreprise_adresse}}
     Tel : {{entreprise_telephone}}
     Email : {{entreprise_email}}
     Periode : {{rapport_periode}}
     Emis le : {{rapport_date}}
     ```
3. **Cartouches KPIs** :
   - 4 blocs textes :
     - `Chiffre d'Affaires : {{stat_ca}} FCFA`
     - `Commandes : {{stat_commandes}}`
     - `Clients actifs : {{stat_clients}}`
     - `Top Produit : {{stat_top_produit}}`
4. **Tableau Dynamique des Ventes** :
   - Bloc `tableau` lié à la source `ventes`.
   - Colonnes :
     - `date` (Titre: Date)
     - `commande` (Titre: N° Commande)
     - `client` (Titre: Client)
     - `produit` (Titre: Produit)
     - `quantite` (Titre: Quantite)
     - `prix_unitaire` (Titre: P.U. FCFA)
     - `total` (Titre: Total FCFA)
5. **Totaux et Synthèse (avec retours à la ligne `\n`)** :
   - Type `texte` :
     ```text
     Sous-total : {{sous_total}} FCFA
     Remise accordée : {{remise}} FCFA
     TVA (19.25%) : {{tva}} FCFA
     TOTAL TTC : {{total_ttc}} FCFA
     ```
   - Type `texte` pour l'analyse :
     ```text
     Évolution : {{analyse_evolution}}
     Observations : {{analyse_observations}}
     ```
6. Cliquez sur l'icône **Enregistrer** (disquette).

---

## Étape 4 : Publication du Modèle, Remplissage & Génération PDF

### 4.1 Publication du Modèle (OBLIGATOIRE)
1. Dans la barre d'outils supérieure de l'éditeur de modèle, cliquez sur le bouton **Publier**.
2. Confirmez la publication.
3. Le badge passe au vert : `PUBLIE`.

### 4.2 Panneau de Remplissage & Test du Modèle
> [!TIP]
> **Où se trouve le panneau de remplissage / test ?**
> Dans la barre latérale gauche de l'éditeur de modèle, cliquez sur l'onglet **Remplir** (icône presse-papiers avec coche `ClipboardList`, située à côté de l'onglet Composants et Variables).
> Vous y avez accès à :
> 1. Le bouton **« Remplir avec l'IA ✨ »** qui génère automatiquement des données réalistes cohérentes.
> 2. Les champs de formulaire pour chaque variable du modèle.
> 3. L'ajout de lignes dans le tableau dynamique `ventes`.
> 4. Le bouton **« Enregistrer le document »** (icône disquette verte) pour persister ce rapport dans `/documents`.
> 5. Le bouton **« Générer le PDF »** (icône imprimante) pour le téléchargement immédiat.

#### Option A : Génération automatique assistée par l'IA
- Cliquez sur le bouton **« Remplir avec l'IA ✨ »**.
- Le service génère un jeu complet de données réalistes en quelques secondes.

#### Option B : Remplissage manuel ou injection de données
Renseignez les champs du formulaire :
- `rapport_periode` : `Janvier 2026`
- `rapport_date` : `31/01/2026`
- `stat_ca` : `12500000`
- `stat_commandes` : `450`
- `stat_clients` : `128`
- `stat_top_produit` : `Huile de Palme 5L`
- `sous_total` : `12500000`
- `remise` : `0`
- `tva` : `2406250`
- `total_ttc` : `14906250`
- `analyse_evolution` : `Croissance de +12% sur le mois`
- `analyse_observations` : `Forte demande sur le littoral`
- Ajoutez au moins 1 ligne dans le tableau `ventes` :
  - `date` : `15/01/2026` | `commande` : `CMD-001` | `client` : `Supermarché Dovv` | `produit` : `Huile de Palme 5L` | `quantite` : `150` | `prix_unitaire` : `6500` | `total` : `975000`

### 4.3 Sauvegarde Permanente du Document
1. Dans le champ **Nom du document** (au-dessus du bouton Enregistrer), saisissez :
   `Rapport Ventes Janvier 2026`
2. Cliquez sur **Enregistrer le document**.
3. **Vérification** : Le document est enregistré avec succès et apparaît dans la liste des documents sauvegardés du panneau.

### 4.4 Génération & Rendu PDF
1. Cliquez sur **Générer le PDF**.
2. **Vérifications attendues** :
   - Le fichier PDF est compilé par Gotenberg et téléchargé immédiatement (`rapport.pdf`).
   - Les **retours à la ligne** saisis dans les blocs textes sont **parfaitement respectés** (chaque information est bien sur sa propre ligne et non collée sur une seule ligne).
   - Le tableau des ventes est mis en page proprement avec ses en-têtes et bordures.
   - Les totaux et indicateurs s'affichent correctement.

---

## Étape 5 : Gestion des Documents & Stockage MinIO S3

### 5.1 Consultation de l'Espace Documents de l'Application
1. Rendez-vous dans le menu **Documents** de la barre latérale gauche ([http://localhost/documents](http://localhost/documents)).
2. **Vérifications** :
   - Votre document `Rapport Ventes Janvier 2026` y est listé avec son badge de modèle `RAPPORT MENSUEL DES VENTES`.
   - **Visionneuse PDF intégrée** : Cliquez sur l'icône **Œil** (Aperçu) pour ouvrir la modal de prévisualisation PDF intégrée.
   - **Téléchargement** : Cliquez sur l'icône **Télécharger** pour récupérer le PDF.
   - **Export Excel** : Cliquez sur l'icône **Tableur** pour exporter les données du rapport en `.xlsx`.
   - **Expédition par Email** : Cliquez sur l'icône **Envoyer** pour tester la modale d'envoi d'email avec destinataire.

### 5.2 Contrôle de l'Object Storage MinIO
1. Ouvrez [http://localhost:9001](http://localhost:9001) dans votre navigateur.
2. Connectez-vous avec `minioadmin` / `minioadmin`.
3. Cliquez sur **Buckets** $\rightarrow$ `rapports-pdf` $\rightarrow$ dossier `entreprises/CAMDIST/reports/`.
4. **Vérification** : Les fichiers PDF générés y sont stockés de manière isolée et chiffrée.

---

## Étape 6 : Traitements par Lots (Batches) avec CSV/Excel

### Objectif
Générer automatiquement une série de rapports en arrière-plan à partir d'un fichier tabulaire.

1. Rendez-vous sur [http://localhost/batches](http://localhost/batches).
2. Cliquez sur **Nouveau traitement par lot**.
3. Sélectionnez le modèle `RAPPORT MENSUEL DES VENTES`.
4. Pour importer les données, vous avez deux méthodes directes :

#### Méthode 1 : Import du fichier CSV conforme
- Cliquez sur **Importer un fichier Excel/CSV**.
- Sélectionnez le fichier préparé pour ce modèle :
  [`donnees_ventes_10_rapports.csv`](file:///d:/programme/Mera_Project/moteur-rapports/donnees_ventes_10_rapports.csv)
- Validez le mapping des colonnes et confirmez l'import.

#### Méthode 2 : Copier-coller du JSON pré-formaté
- Si vous préférez coller le JSON directement dans la boîte de saisie, collez le contenu du fichier [`donnees_ventes_sample_batch.json`](file:///d:/programme/Mera_Project/moteur-rapports/donnees_ventes_sample_batch.json).

5. Laissez le champ Webhook vide (ou renseignez une URL valide commençant par `http://` si vous souhaitez tester un webhook).
6. Cliquez sur **Lancer le traitement par lot**.
7. **Vérifications attendues** :
   - Le lot est créé immédiatement avec le statut `EN_COURS`.
   - La barre de progression avance jusqu'à `100%`.
   - Le statut passe à `TERMINE` (`COMPLETED`) avec **10/10 succès et 0 échec** !
   - Cliquez sur **Télécharger ZIP** pour récupérer l'archive compressée contenant tous les rapports générés.

---

## Étape 7 : Connecteurs de Données Distants & Planifications Cron

### 7.1 Connecteurs de Données (Data Sources)
1. Rendez-vous sur [http://localhost/data-sources](http://localhost/data-sources).
2. **Vérification** : La page charge immédiatement sans boucle infinie.
3. Cliquez sur **Nouveau Connecteur**.
4. Remplissez le formulaire de test :
   - Nom : `API Données Externes Test`
   - Type : `REST_API`
   - Hôte / URL : `https://jsonplaceholder.typicode.com/posts`
   - Port : `443`
5. Cliquez sur **Tester la connexion**.
   - **Vérification** : Indicateur vert « Connexion réussie ».
6. Cliquez sur **Enregistrer**. Le connecteur apparaît dans la liste.

### 7.2 Tâches Planifiées (Schedules)
1. Rendez-vous sur [http://localhost/schedules](http://localhost/schedules).
2. **Vérification** : La page charge immédiatement avec la liste des planifications existantes.
3. Cliquez sur **Nouvelle Planification**.
4. Configurez :
   - Nom : `Génération Mensuelle Ventes`
   - Modèle : `RAPPORT MENSUEL DES VENTES`
   - Expression Cron : `0 0 6 1 * *` (Le 1er du mois à 06h00) ou sélectionnez un préréglage dans la liste déroulante.
   - Format d'export : `PDF`
   - Destinataires : `direction@camdist.cm`
   - Actif : ✅
5. Enregistrez la planification.
6. Cliquez sur le bouton **Exécuter maintenant** (icône Play) :
   - **Vérification** : Notification de succès avec durée d'exécution en millisecondes et mise à jour de l'historique d'exécution.

---

## Étape 8 : Sécurité Cryptographique & Piste d'Audit

### 8.1 Gestion des Certificats de Signature PDF (PAdES)
1. Rendez-vous sur [http://localhost/certificates](http://localhost/certificates).
2. Vérifiez le tableau de bord des certificats numériques pour la signature et le scellement officiel des rapports de l'entreprise.

### 8.2 Piste d'Audit & Gouvernance
1. Rendez-vous sur [http://localhost/audit](http://localhost/audit).
2. **Vérification** :
   - La page s'ouvre sans aucune erreur SQL ni crash backend.
   - Les compteurs statistiques et la liste des journaux d'audit s'affichent avec fluidité.
   - Vous y retrouvez les événements horodatés :
     - `DOCUMENT_GENERATE`
     - `AI_MOCK_DATA_GENERATE`
     - `TEMPLATE_PUBLISH`
     - `BATCH_CREATE`
   - Vous pouvez filtrer par action (`DOCUMENT_GENERATE`, `TEMPLATE_PUBLISH`...) ou consulter les détails d'un événement dans la modale d'inspection JSON.

---

## Étape 9 : Validation des Suites de Tests Automatisés

### 9.1 Tests Unitaires & Intégration Backend (Spring Boot / Maven)
Dans un terminal PowerShell :
```powershell
docker compose exec backend mvn test
```
- **Résultat attendu** : 100% des tests unitaires et d'intégration réussis, 0 échec.

---

## Matrice de Recette & Checklist Finale

- [ ] **Étape 0** : 5/5 conteneurs Docker `Up (healthy)`.
- [ ] **Étape 1** : Connexion utilisateur avec code entreprise `CAMDIST` validée.
- [ ] **Étape 2** : Configuration de la feuille A4 sauvegardée.
- [ ] **Étape 3** : Modèle conçu avec variables conformes (`[a-zA-Z0-9_]+`) et blocs texte multilignes.
- [ ] **Étape 4** : Modèle passé en statut **PUBLIE**, données remplies (via IA ou formulaire) et PDF généré avec respect scrupuleux des retours à la ligne (`\n`).
- [ ] **Étape 5** : Document sauvegardé présent dans `/documents`, visionneuse PDF fonctionnelle et stockage MinIO S3 validé sous `entreprises/CAMDIST/reports/`.
- [ ] **Étape 6** : Batch de 10 rapports avec [`donnees_ventes_10_rapports.csv`](file:///d:/programme/Mera_Project/moteur-rapports/donnees_ventes_10_rapports.csv) exécuté avec 100% de succès.
- [ ] **Étape 7** : Pages `/data-sources` et `/schedules` réactives et connecteur / tâche cron créés.
- [ ] **Étape 8** : Module `/certificates` et piste d'audit `/audit` opérationnels et affichant l'historique complet sans erreur.

