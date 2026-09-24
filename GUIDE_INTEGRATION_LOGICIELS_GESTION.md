# 📑 Guide d'Intégration Industriel du Moteur de Rapports dans les Logiciels de Gestion (ERP, CRM, Facturation, RH)

> **Document de Référence Technique & Architectural**  
> Ce guide détaille l'ensemble des mécanismes, protocoles réseau, contrats d'interfaces et bonnes pratiques pour connecter le **Moteur de Rapports Dynamiques** à n'importe quel logiciel de gestion tiers (SaaS, On-Premise, ERP propriétaire, CRM, etc.).

---

## Sommaire

1. [Introduction & Principes Fondamentaux](#1-introduction--principes-fondamentaux)
2. [Les 3 Modes d'Intégration](#2-les-3-modes-dintégration)
   - [Mode 1 : Headless API REST (100% Back-Office / Invisible)](#mode-1--headless-api-rest-100-back-office--invisible)
   - [Mode 2 : Web Component SDK `<report-designer-widget>` (Atelier Visuel Embarqué)](#mode-2--web-component-sdk-report-designer-widget-atelier-visuel-embarqué)
   - [Mode 3 : Intégration iFrame Sécurisée](#mode-3--intégration-iframe-sécurisée)
3. [Sécurité, Authentification & Cloisonnement Multi-Tenant](#3-sécurité-authentification--cloisonnement-multi-tenant)
4. [Protocole d'Envoi & Formats de Données Admissibles](#4-protocole-denvoi--formats-de-données-admissibles)
   - [Formats acceptés : JSON, Excel (.xlsx), CSV](#formats-acceptés--json-excel-xlsx-csv)
   - [Typage strict et validation de schéma](#typage-strict-et-validation-de-schéma)
   - [Expressions logiques et calculs SpEL](#expressions-logiques-et-calculs-spel)
5. [Mécanisme de Routage : Comment le Moteur sait quoi produire](#5-mécanisme-de-routage--comment-le-moteur-sait-quoi-produire)
   - [1. Résolution du modèle cible (UUID)](#1-résolution-du-modèle-cible-uuid)
   - [2. Sélection du format (PDF, HTML, Excel)](#2-sélection-du-format-pdf-html-excel)
   - [3. Décision du volume (1 document vs N documents en lot)](#3-décision-du-volume-1-document-vs-n-documents-en-lot)
6. [Génération Industrielle par Lot (Batch Engine) & Webhooks](#6-génération-industrielle-par-lot-batch-engine--webhooks)
   - [Cycle de vie asynchrone](#cycle-de-vie-asynchrone)
   - [Webhooks signés cryptographiquement (HMAC-SHA256)](#webhooks-signés-cryptographiquement-hmac-sha256)
   - [Archive groupée ZIP](#archive-groupée-zip)
7. [Norme Factur-X / ZUGFeRD (Facturation Électronique)](#7-norme-factur-x--zugferd-facturation-électronique)
8. [Stockage, Persistance & Récupération des Fichiers (S3 / MinIO)](#8-stockage-persistance--récupération-des-fichiers-s3--minio)
9. [Exemples Concrets de Code d'Intégration (Multi-Langages)](#9-exemples-concrets-de-code-dintégration-multi-langages)
   - [PHP (cURL / Guzzle)](#php-curl--guzzle)
   - [Python (requests)](#python-requests)
   - [Node.js / TypeScript (fetch / axios)](#nodejs--typescript-fetch--axios)
   - [Java (HttpClient / RestTemplate / WebClient)](#java-httpclient--resttemplate--webclient)
10. [FAQ Approfondie & Anticipation des Défis Techniques](#10-faq-approfondie--anticipation-des-défis-techniques)

---

## 1. Introduction & Principes Fondamentaux

Le **Moteur de Rapports** a été bâti autour du paradigme **Headless-First & Embeddable Designer** :
* **Découplage Total** : La logique de composition visuelle des modèles (WYSIWYG) est totalement séparée de l'exécution de génération.
* **Autonomie Complète** : Le moteur n'impose aucun schéma rigide à vos bases de données ; il reçoit des variables en entrée et produit des flux prêts à être imprimés ou archivés.
* **Double Nature** :
  1. Il peut être utilisé comme un **microservice invisible de compilation de documents** (aucun écran affiché à l'utilisateur final).
  2. Il peut être utilisé comme un **éditeur de gabarits intégré** (permettant à vos clients de personnaliser eux-mêmes leurs entêtes, logos et couleurs directement depuis votre logiciel).

```
+-----------------------------------------------------------------------------------+
|                            VOTRE LOGICIEL DE GESTION                              |
|           (ERP, CRM, Logiciel de Facturation, Paie, WMS, E-commerce)              |
+-----------------------------------------------------------------------------------+
         |                                                 |
         |  Mode 1: Requêtes API REST (JSON)               |  Mode 2: Web Component
         |  (Génération silencieuse PDF / Excel)           |  (Studio visuel dans l'ERP)
         v                                                 v
+-----------------------------------------------------------------------------------+
|                        MOTEUR DE RAPPORTS (V2)                                    |
|                                                                                   |
|  +---------------------------+   +---------------------------------------------+  |
|  |     Moteur Backend        |   |           Moteur Frontend                   |  |
|  |   Spring Boot 3 / Java 21 |   |         Angular 21 Custom Element           |  |
|  |   - Gotenberg (Chromium)  |   |   <report-designer-widget>                  |  |
|  |   - Factur-X / ZUGFeRD    |   |   - Virtual Paper Canvas                    |  |
|  |   - Apache POI (Excel)    |   |   - Drag & Drop, Guides magnétiques         |  |
|  |   - S3 Storage / MinIO    |   |   - Formulaire de test temps réel           |  |
|  +---------------------------+   +---------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Les 3 Modes d'Intégration

### Mode 1 : Headless API REST (100% Back-Office / Invisible)
C'est le mode le plus courant pour un logiciel de gestion :
* Vos utilisateurs restent dans vos interfaces familières.
* Lorsqu'un utilisateur clique sur **"Télécharger la facture n° 1042"**, votre serveur backend appelle notre moteur en HTTP POST avec les données de la commande.
* Notre moteur répond immédiatement avec le flux binaire du PDF (`Content-Type: application/pdf`).
* Votre ERP retransmet directement ce flux au navigateur de l'utilisateur ou l'enregistre dans son propre stockage.

### Mode 2 : Web Component SDK `<report-designer-widget>` (Atelier Visuel Embarqué)
Si vous voulez offrir à vos utilisateurs la possibilité de concevoir ou personnaliser leurs propres factures, devis ou bons de commande :
* Vous intégrez le composant web standardisé W3C Custom Element `<report-designer-widget>`.
* Il fonctionne dans **n'importe quel framework frontend** (React, Vue, Svelte, Angular, PHP natif, Blade, Twig) :
```html
<!-- Import unique du bundle JavaScript du widget -->
<script type="module" src="https://rapports.votre-domaine.com/widget.js"></script>

<!-- Insertion de la balise dans votre page -->
<report-designer-widget
  template-id="8b62c140-5421-4f1b-85d1-678c187bc732"
  api-base-url="https://rapports.votre-domaine.com"
  auth-token="VOTRE_JWT_TOKEN"
  theme="light">
</report-designer-widget>

<script>
  const widget = document.querySelector('report-designer-widget');
  
  // Écoute de l'événement de sauvegarde
  widget.addEventListener('save', (event) => {
    console.log('Modèle sauvegardé avec succès :', event.detail);
    alert('Votre modèle a été mis à jour !');
  });

  // Écoute de la fermeture du designer
  widget.addEventListener('close', () => {
    window.location.href = '/mes-parametres';
  });
</script>
```
* **Isolation complète** : Grâce à l'encapsulation Shadow/Emulated, aucun style CSS de votre ERP ne perturbera le designer, et inversement.

### Mode 3 : Intégration iFrame Sécurisée
Pour les architectures SaaS isolées ou les portails partenaires :
* Intégration d'une iFrame pointant vers `https://rapports.votre-domaine.com/templates/{id}?token=...`.
* Communication bidirectionnelle sécurisée via `window.postMessage`.

---

## 3. Sécurité, Authentification & Cloisonnement Multi-Tenant

Toutes les requêtes vers le moteur doivent être authentifiées :

```http
Authorization: Bearer <VOTRE_JETON_JWT>
Content-Type: application/json
```

### Mécanisme de Cloisonnement Multi-Tenant
Chaque appel est intercepté par le filtre de sécurité Spring Boot (`JwtAuthenticationFilter`) :
1. Le jeton JWT contient obligatoirement l'attribut `codeEntreprise` (ex: `CORP-ACME`).
2. À chaque requête (`GET`, `POST`, `PUT`, `DELETE`), le moteur vérifie que le modèle (`ReportTemplate`) ou le document (`Document`) appartient bien au `codeEntreprise` de l'appelant.
3. **Sécurité absolue** : Une entreprise A ne peut sous aucun prétexte accéder, modifier ou générer un modèle appartenant à une entreprise B. Une tentative retourne un code HTTP `403 Forbidden` ou `404 Not Found`.

---

## 4. Protocole d'Envoi & Formats de Données Admissibles

### Formats acceptés : JSON, Excel (.xlsx), CSV

#### 1. JSON (Format Universel pour les API)
C'est le format standard transmis dans le corps de la requête HTTP (`body`).

```json
{
  "numero_facture": "FAC-2026-089",
  "date_facture": "2026-09-24",
  "client_nom": "Cabinet Dupont & Associés",
  "client_adresse": "14 Avenue Montaigne, 75008 Paris",
  "total_ht": 4500.00,
  "taux_tva": 20.0,
  "total_ttc": 5400.00,
  "lignes_articles": [
    { "designation": "Audit de sécurité applicative", "quantite": 3, "prix_unitaire": 1000.00, "total": 3000.00 },
    { "designation": "Refonte architecture cloud", "quantite": 2, "prix_unitaire": 750.00, "total": 1500.00 }
  ]
}
```

#### 2. Tableur Excel (.xlsx) & Fichier CSV (.csv)
* Utilisé principalement pour des imports en masse ou des générations manuelles.
* L'assistant intégré effectue une réconciliation automatique des noms de colonnes : si la colonne s'appelle `Nom Client` ou `client_nom`, le moteur mappe automatiquement la valeur sur `{{ client_nom }}`.

### Typage strict et validation de schéma
Le moteur supporte les types de variables suivants :
* `STRING` : Texte simple ou multi-lignes.
* `FLOAT` / `INTEGER` : Nombres décimaux et entiers avec formatage monétaire possible.
* `DATE` : Dates ISO-8601 (`YYYY-MM-DD`), automatiquement traduisibles en français (`24 septembre 2026`).
* `BOOLEAN` : Vrai/Faux, permettant d'activer des blocs conditionnels.
* `ARRAY` : Liste d'objets pour les tableaux répétitifs (factures, bulletins, bons).
* `IMAGE` : URL distante sécurisée (`https://.../signature.png`) ou chaîne encodée en `data:image/png;base64,...`.
* `CALCULEE` : Champs calculés dynamiquement par le moteur (ex: `total_ht * 0.20`).

> ⚠️ **Validation de schéma** : Si une variable obligatoire est absente du JSON envoyé, le moteur refuse la génération avec une erreur explicite HTTP `400 Bad Request` détaillant les champs manquants :
> ```json
> {
>   "timestamp": "2026-09-24T17:15:00Z",
>   "status": 400,
>   "error": "Validation Error",
>   "message": "Champs obligatoires manquants : [numero_facture, total_ttc]"
> }
> ```

### Expressions logiques et calculs SpEL
Le moteur embarque un évaluateur d'expressions Spring SpEL sécurisé. Dans vos modèles, vous pouvez utiliser :
* **Conditions d'affichage** : Un bloc avec condition `total_ttc > 5000` ne s'affichera que si le montant dépasse 5 000 €.
* **Formatages automatiques** : `{{ #formatMoney(total_ttc) }}` $\rightarrow$ `5 400,00 €`.
* **Dates formatées** : `{{ #formatDate(date_facture, 'dd/MM/yyyy') }}` $\rightarrow$ `24/09/2026`.

---

## 5. Mécanisme de Routage : Comment le Moteur sait quoi produire

Le moteur est piloté de façon prédictive et déterministe grâce à **l'URL appelée** et **la structure du payload JSON**.

```
                           DEMANDE DE L'ERP
                                   |
                +------------------+------------------+
                |                                     |
       Appel Unitaire (1 doc)                Appel par Lot (N docs)
     POST /api/templates/{id}/...         POST /api/templates/{id}/batch
                |                                     |
     +----------+----------+                          |
     |          |          |                          v
/generate  /preview-html  /export-excel       Génération Asynchrone
     |          |          |                  Multi-thread
     v          v          v                          |
  1 PDF       1 HTML     1 Excel (.xlsx)              v
                                              N documents générés
                                              + Webhook de rappel
                                              + batch_{id}.zip
```

### 1. Résolution du modèle cible (UUID)
L'ERP spécifie l'identifiant du modèle directement dans l'URL :
`https://rapports.domaine.com/api/templates/{templateId}/...`
* Le moteur charge le gabarit depuis PostgreSQL.
* Il valide que `statut == 'PUBLIE'`.

### 2. Sélection du format (PDF, HTML, Excel)

| Format souhaité | Endpoint à appeler | Type MIME retourné |
| :--- | :--- | :--- |
| **Document PDF finalisé** | `POST /api/templates/{id}/generate` | `application/pdf` |
| **Aperçu HTML brut** | `POST /api/templates/{id}/preview-html` | `text/html; charset=UTF-8` |
| **Tableur Excel dynamique** | `POST /api/templates/{id}/export-excel` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |

### 3. Décision du volume (1 document vs N documents en lot)

* **Cas A : 1 Document** :
  L'ERP appelle l'endpoint `/generate`. Le corps est un **objet JSON simple** :
  ```json
  POST /api/templates/3fa85f64-5717-4562-b3fc-2c963f66afa6/generate
  {
    "client_nom": "Entreprise ABC",
    "total_ttc": 1200.00
  }
  ```
  $\rightarrow$ Réponse synchrone immédiate (le PDF binaire est retourné dans la réponse).

* **Cas B : 10 ou 1 000 Documents (Traitement par Lot)** :
  L'ERP appelle l'endpoint `/batch`. Le corps contient un **tableau d'items** (`items`) :
  ```json
  POST /api/templates/3fa85f64-5717-4562-b3fc-2c963f66afa6/batch
  {
    "webhookUrl": "https://erp.monentreprise.com/api/webhooks/rapports",
    "webhookSecret": "mon_secret_hmac_12345",
    "items": [
      { "customId": "FAC-001", "data": { "client_nom": "Client 1", "total_ttc": 150.0 } },
      { "customId": "FAC-002", "data": { "client_nom": "Client 2", "total_ttc": 320.0 } },
      { "customId": "FAC-010", "data": { "client_nom": "Client 10", "total_ttc": 980.0 } }
    ]
  }
  ```
  $\rightarrow$ Réponse immédiate `202 ACCEPTED` avec les identifiants du lot, tandis que le traitement se déroule en arrière-plan.

---

## 6. Génération Industrielle par Lot (Batch Engine) & Webhooks

### Cycle de vie asynchrone
1. L'ERP soumet le lot $\rightarrow$ Le moteur enregistre un enregistrement `ReportBatch` avec statut `EN_COURS`.
2. Le pool de threads asynchrones (`AsyncBatchProcessor`) prend en charge les éléments en parallèle.
3. Chaque rapport est compilé, converti en PDF via Gotenberg et déposé dans le bucket S3/MinIO.
4. Dès que tous les éléments sont traités, le statut bascule à `TERMINE`.

### Webhooks signés cryptographiquement (HMAC-SHA256)
Dès la fin du lot, le moteur déclenche un appel HTTP POST vers l'URL configurée (`webhookUrl`).

#### En-têtes HTTP envoyés au Webhook :
```http
POST /api/webhooks/rapports HTTP/1.1
Host: erp.monentreprise.com
Content-Type: application/json
X-Signature-SHA256: 7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069
X-Batch-ID: b9a1d4f2-5813-4a1e-8419-74d12c82a101
```

#### Corps JSON de notification :
```json
{
  "batchId": "b9a1d4f2-5813-4a1e-8419-74d12c82a101",
  "templateId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "statut": "TERMINE",
  "totalItems": 10,
  "itemsSucces": 10,
  "itemsErreur": 0,
  "zipDownloadUrl": "/api/batches/b9a1d4f2-5813-4a1e-8419-74d12c82a101/download-zip",
  "items": [
    { "customId": "FAC-001", "statut": "TERMINE", "documentUrl": "/api/documents/doc-1/pdf" },
    { "customId": "FAC-002", "statut": "TERMINE", "documentUrl": "/api/documents/doc-2/pdf" }
  ]
}
```

#### Vérification de la signature dans votre ERP (Exemple PHP) :
```php
$payload = file_get_contents('php://input');
$signatureRecue = $_SERVER['HTTP_X_SIGNATURE_SHA256'];
$secret = 'mon_secret_hmac_12345';

$signatureCalculee = hash_hmac('sha256', $payload, $secret);

if (!hash_equals($signatureCalculee, $signatureRecue)) {
    http_response_code(401);
    die('Signature de webhook invalide !');
}
// Signature valide -> Déclencher l'impression ou la mise à jour en base
```

### Archive groupée ZIP
L'ERP peut télécharger d'un coup l'ensemble des documents du lot compressés dans un fichier `.zip` via :
`GET /api/batches/{id}/download-zip`

---

## 7. Norme Factur-X / ZUGFeRD (Facturation Électronique)

Le moteur supporte nativement la réglementation européenne de facturation électronique (directive EN 16931).

### Comment ça marche ?
1. Un modèle configuré avec la catégorie `VENTES` ou `FACTURE` génère un fichier conforme au standard **PDF/A-3**.
2. Le moteur génère automatiquement en arrière-plan le flux XML structuré conforme à la norme **CII (Cross Industry Invoice) / UBL**.
3. Ce fichier XML `factur-x.xml` est incorporé dans les pièces jointes du PDF (`embedded files`).
4. **Résultat** : Le document est lisible par un être humain (PDF visuel soigné) **ET** directement intégrable de façon automatisée par les plateformes de dématérialisation partenaires (PDP) et le portail public de facturation (PPF).

---

## 8. Stockage, Persistance & Récupération des Fichiers (S3 / MinIO)

Tous les documents générés sont pérennisés :
* **Connecteur S3 Cloud-Native** : Compatible avec AWS S3, Google Cloud Storage, Scaleway, OVH Cloud ou MinIO On-Premise.
* **URLs Pré-signées sécurisées** : Vous pouvez configurer la délivrance de liens de téléchargement temporaires avec expiration (ex: lien valable 15 minutes).
* **Historique des générations** : Accessible à tout moment via `GET /api/templates/{id}/generations` pour des besoins d'audit ou de réimpression.

---

## 9. Exemples Concrets de Code d'Intégration (Multi-Langages)

Voici comment déclencher la génération d'un PDF depuis votre langage favori :

### PHP (cURL / Guzzle)

```php
<?php
$templateId = '3fa85f64-5717-4562-b3fc-2c963f66afa6';
$jwtToken   = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';

$data = [
    'numero_facture' => 'FAC-2026-999',
    'client_nom'     => 'Acme International',
    'total_ttc'      => 1450.00
];

$ch = curl_init("http://moteur-rapports:8082/api/templates/{$templateId}/generate");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    "Authorization: Bearer {$jwtToken}"
]);

$pdfContent = curl_exec($ch);
$httpCode   = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 200) {
    // Sauvegarder ou envoyer au client
    file_put_contents(__DIR__ . '/facture.pdf', $pdfContent);
    echo "Facture générée avec succès !";
} else {
    echo "Erreur de génération : HTTP {$httpCode}";
}
```

### Python (requests)

```python
import requests

template_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
api_url = f"http://moteur-rapports:8082/api/templates/{template_id}/generate"

headers = {
    "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "Content-Type": "application/json"
}

payload = {
    "numero_facture": "FAC-2026-999",
    "client_nom": "Acme International",
    "total_ttc": 1450.00
}

response = requests.post(api_url, json=payload, headers=headers)

if response.status_code == 200:
    with open("facture.pdf", "wb") as f:
        f.write(response.content)
    print("PDF sauvegardé avec succès !")
else:
    print(f"Erreur {response.status_code} : {response.text}")
```

### Node.js / TypeScript (fetch / axios)

```typescript
import axios from 'axios';
import * as fs from 'fs';

async function genererFacture() {
  const templateId = '3fa85f64-5717-4562-b3fc-2c963f66afa6';
  const url = `http://moteur-rapports:8082/api/templates/${templateId}/generate`;

  const response = await axios.post(
    url,
    {
      numero_facture: 'FAC-2026-999',
      client_nom: 'Acme International',
      total_ttc: 1450.00,
    },
    {
      headers: {
        Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        'Content-Type': 'application/json',
      },
      responseType: 'arraybuffer', // Important pour recevoir le PDF binaire
    }
  );

  fs.writeFileSync('facture.pdf', response.data);
  console.log('Facture enregistrée !');
}

genererFacture().catch(console.error);
```

### Java (HttpClient / RestTemplate / WebClient)

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RapportClient {
    public static void main(String[] args) throws Exception {
        String templateId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String jsonPayload = """
            {
                "numero_facture": "FAC-2026-999",
                "client_nom": "Acme International",
                "total_ttc": 1450.00
            }
            """;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://moteur-rapports:8082/api/templates/" + templateId + "/generate"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        Path destination = Paths.get("facture.pdf");
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(destination));

        if (response.statusCode() == 200) {
            System.out.println("Fichier téléchargé : " + destination.toAbsolutePath());
        }
    }
}
```

---

## 10. FAQ Approfondie & Anticipation des Défis Techniques

### Q1 : Que se passe-t-il si 1 document sur 500 contient une erreur dans un lot ?
Le moteur est **résilient aux pannes partielles** :
* Si le document n° 42 contient des données invalides, il est marqué en statut `EN_ERREUR` avec le message de cause détaillé.
* **Le reste du lot continue normalement** : les 499 autres documents sont générés et archivés avec succès.
* L'ERP peut consulter le détail via `GET /api/batches/{id}/items` et déclencher une réévaluation ciblée des seuls éléments en échec via `POST /api/batches/{id}/retry-failed`.

### Q2 : Quelle est la performance et la cadence maximale de génération ?
* Grâce au moteur Gotenberg conteneurisé (Chromium headless optimisé) et au multithreading Spring Boot :
  * Une facture synchrone standard (1 page avec logo et tableau) est générée en **120 à 250 millisecondes**.
  * Un lot de 100 factures en parallèle est traité en environ **8 à 15 secondes**.
  * Vous pouvez dimensionner Gotenberg horizontalement (clusters de conteneurs Docker) si vous devez générer des centaines de milliers de documents par heure.

### Q3 : Comment gérer la personnalisation par sous-client (White-Label / Marque blanche) ?
Chaque entreprise peut définir ses valeurs globales dans `/feuille-travail` (logo officiel, police de caractères, marges, mentions légales en pied de page). Ces paramètres s'appliquent automatiquement à tous les modèles générés pour cette entreprise, sans avoir à les redéfinir manuellement dans chaque template.

### Q4 : Comment mettre à jour un modèle en production sans impacter les documents du passé ?
Le moteur gère un **arbre de généalogie de versions immuables** :
* Quand un modèle passe de `v1` à `v2` (nouvelle maquette, nouveau logo), la version `v1` est archivée mais conservée.
* Les anciens documents générés l'année précédente restent rattachés à la `v1` et ne bougent jamais.
* Les nouveaux appels pointant sur l'UUID principal reçoivent automatiquement le rendu de la `v2`.

