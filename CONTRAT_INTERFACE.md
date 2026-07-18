\# Contrat d'Interface – Moteur de Rapports Dynamiques



Ce document définit le contrat d'interface entre le backend Spring Boot et le frontend Angular. Toute modification doit être validée par l'ensemble de l'équipe avant implémentation.



\---



\## 1. Modèle de données (Entités principales)



\### 1.1 ReportTemplate

\- `id` : UUID

\- `nom` : String

\- `description` : String (optionnel)

\- `contenuDesign` : JSON (structure de blocs, voir ci-dessous)

\- `statut` : Enum (BROUILLON, PUBLIE, ARCHIVE)

\- `version` : Integer (incrémenté à chaque publication)

\- `dateCreation` : Timestamp

\- `dateModification` : Timestamp



\### 1.2 ReportVariable (obsolete à partir de l'Étape 3, remplacé par le schéma JSON)

\- `id` : UUID

\- `templateId` : FK vers ReportTemplate

\- `nomVariable` : String

\- `type` : Enum (STRING, FLOAT, DATE, BOOLEAN, ARRAY)

\- `obligatoire` : Boolean



\### 1.3 ReportGeneration

\- `id` : UUID

\- `templateId` : FK vers ReportTemplate

\- `dateGeneration` : Timestamp

\- `donneesRecues` : JSON (pour audit)

\- `statutGeneration` : Enum (EN\_COURS, SUCCES, ECHEC)

\- `urlFichierGenere` : String (nullable)



\---



\## 2. Format du design (JSON des blocs)



Le design est un objet JSON avec une propriété `blocs` (tableau). Chaque bloc possède un type et des propriétés.



\### Types de blocs supportés :

\- `titre` : contient `contenu` (string avec variables `{{...}}`), `style` (optionnel)

\- `texte` : idem

\- `tableau` : contient `colonnes` (tableau d'objets `{titre, variable}`), `source` (nom de variable pour la liste), `style` (optionnel)



\### Exemple :

```json

{

&#x20; "blocs": \[

&#x20;   {

&#x20;     "type": "titre",

&#x20;     "contenu": "Facture N°{{numero\_facture}}",

&#x20;     "style": { "fontSize": 16, "bold": true, "align": "center" }

&#x20;   },

&#x20;   {

&#x20;     "type": "texte",

&#x20;     "contenu": "Client : {{nom\_client}}",

&#x20;     "style": { "fontSize": 12 }

&#x20;   },

&#x20;   {

&#x20;     "type": "tableau",

&#x20;     "colonnes": \[

&#x20;       { "titre": "Produit", "variable": "produit" },

&#x20;       { "titre": "Prix", "variable": "prix\_unitaire" }

&#x20;     ],

&#x20;     "source": "lignes\_facture"

&#x20;   }

&#x20; ]

}

