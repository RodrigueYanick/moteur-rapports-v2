package com.rapports.moteur.init;

import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.repository.ReportTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicTemplatePackSeeder implements CommandLineRunner {

    private final ReportTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        log.info("Vérification et initialisation du Pack de Modèles Publics officiels...");

        List<ReportTemplate> existingPublicTemplates = templateRepository.findByCodeEntrepriseIsNull();

        seedCvCanadian(existingPublicTemplates);
        seedCvAts(existingPublicTemplates);
        seedCvEuro(existingPublicTemplates);
        seedRapportStage(existingPublicTemplates);
        seedFactureFreelance(existingPublicTemplates);
    }

    private void seedCvCanadian(List<ReportTemplate> existing) {
        String name = "CV Canadien Professionnel (Norme Officielle)";
        if (existing.stream().anyMatch(t -> t.getNom().equalsIgnoreCase(name))) {
            return;
        }

        log.info("Seeding template public : {}", name);
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "can-hdr-title",
                  "type": "titre",
                  "contenu": "{{nom_complet}}",
                  "x": 40,
                  "y": 40,
                  "largeurBox": 736,
                  "hauteurBox": 45,
                  "style": { "fontSize": 28, "fontWeight": "bold", "color": "#0f172a", "textAlign": "center" }
                },
                {
                  "id": "can-hdr-subtitle",
                  "type": "texte",
                  "contenu": "{{titre_professionnel}}",
                  "x": 40,
                  "y": 88,
                  "largeurBox": 736,
                  "hauteurBox": 25,
                  "style": { "fontSize": 16, "fontWeight": "600", "color": "#0284c7", "textAlign": "center" }
                },
                {
                  "id": "can-hdr-contact",
                  "type": "texte",
                  "contenu": "{{ville_province}} • {{telephone}} • {{email}} • {{linkedin}}",
                  "x": 40,
                  "y": 115,
                  "largeurBox": 736,
                  "hauteurBox": 20,
                  "style": { "fontSize": 11, "color": "#475569", "textAlign": "center" }
                },
                {
                  "id": "can-sep-1",
                  "type": "ligne",
                  "x": 40,
                  "y": 142,
                  "largeurBox": 736,
                  "hauteurBox": 2,
                  "style": { "color": "#cbd5e1" }
                },
                {
                  "id": "can-sec-profil-title",
                  "type": "titre",
                  "contenu": "PROFIL PROFESSIONNEL & OBJECTIF",
                  "x": 40,
                  "y": 155,
                  "largeurBox": 736,
                  "hauteurBox": 25,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "1px solid #0284c7" }
                },
                {
                  "id": "can-sec-profil-text",
                  "type": "texte",
                  "contenu": "{{profil_sommaire}}",
                  "x": 40,
                  "y": 185,
                  "largeurBox": 736,
                  "hauteurBox": 60,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-sec-skills-title",
                  "type": "titre",
                  "contenu": "COMPÉTENCES CLÉS & EXPERTISE",
                  "x": 40,
                  "y": 255,
                  "largeurBox": 736,
                  "hauteurBox": 25,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "1px solid #0284c7" }
                },
                {
                  "id": "can-sec-skills-text",
                  "type": "texte",
                  "contenu": "{{competences_cles}}",
                  "x": 40,
                  "y": 285,
                  "largeurBox": 736,
                  "hauteurBox": 55,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-sec-exp-title",
                  "type": "titre",
                  "contenu": "EXPÉRIENCES PROFESSIONNELLES (RÉALISATIONS QUANTIFIÉES)",
                  "x": 40,
                  "y": 350,
                  "largeurBox": 736,
                  "hauteurBox": 25,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "1px solid #0284c7" }
                },
                {
                  "id": "can-exp-1-head",
                  "type": "texte",
                  "contenu": "<b>{{poste_1}}</b> | {{entreprise_1}} — {{ville_1}} ({{dates_1}})",
                  "x": 40,
                  "y": 380,
                  "largeurBox": 736,
                  "hauteurBox": 22,
                  "style": { "fontSize": 12, "color": "#0f172a" }
                },
                {
                  "id": "can-exp-1-desc",
                  "type": "texte",
                  "contenu": "{{realisations_1}}",
                  "x": 40,
                  "y": 405,
                  "largeurBox": 736,
                  "hauteurBox": 110,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-exp-2-head",
                  "type": "texte",
                  "contenu": "<b>{{poste_2}}</b> | {{entreprise_2}} — {{ville_2}} ({{dates_2}})",
                  "x": 40,
                  "y": 525,
                  "largeurBox": 736,
                  "hauteurBox": 22,
                  "style": { "fontSize": 12, "color": "#0f172a" }
                },
                {
                  "id": "can-exp-2-desc",
                  "type": "texte",
                  "contenu": "{{realisations_2}}",
                  "x": 40,
                  "y": 550,
                  "largeurBox": 736,
                  "hauteurBox": 105,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-sec-edu-title",
                  "type": "titre",
                  "contenu": "FORMATION ACADÉMIQUE & DIPLÔMES",
                  "x": 40,
                  "y": 665,
                  "largeurBox": 736,
                  "hauteurBox": 25,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "1px solid #0284c7" }
                },
                {
                  "id": "can-sec-edu-text",
                  "type": "texte",
                  "contenu": "{{formations_et_diplomes}}",
                  "x": 40,
                  "y": 695,
                  "largeurBox": 736,
                  "hauteurBox": 80,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "nom_complet": { "type": "string", "title": "Nom Complet", "default": "Alexandre Tremblay" },
            "titre_professionnel": { "type": "string", "title": "Titre du Poste Visé", "default": "Ingénieur DevOps Senior & Architecte Cloud" },
            "ville_province": { "type": "string", "title": "Ville et Province", "default": "Montréal, QC" },
            "telephone": { "type": "string", "title": "Téléphone", "default": "+1 (514) 890-1234" },
            "email": { "type": "string", "title": "Courriel", "default": "alexandre.tremblay@email.ca" },
            "linkedin": { "type": "string", "title": "Profil LinkedIn", "default": "linkedin.com/in/alex-tremblay" },
            "profil_sommaire": { "type": "string", "title": "Sommaire professionnel", "default": "Ingénieur DevOps chevronné cumulant 7 ans d'expérience dans l'orchestration de pipelines CI/CD, l'architecture Kubernetes résiliente et la réduction de 40% des temps de déploiement pour des applications financières critiques." },
            "competences_cles": { "type": "string", "title": "Compétences clés", "default": "• Cloud & Conteneurs : AWS, GCP, Docker, Kubernetes, Terraform\\n• CI/CD & Observabilité : GitLab CI, GitHub Actions, Prometheus, Grafana, Datadog\\n• Sécurité & Conformité : DevSecOps, SonarQube, ISO 27001, Vault\\n• Méthodologies : Agile Scrum, SRE, Chaos Engineering" },
            "poste_1": { "type": "string", "title": "Poste récent", "default": "Lead DevOps & SRE" },
            "entreprise_1": { "type": "string", "title": "Entreprise 1", "default": "Banque Nationale du Canada" },
            "ville_1": { "type": "string", "title": "Ville 1", "default": "Montréal, QC" },
            "dates_1": { "type": "string", "title": "Période 1", "default": "2022 - Présent" },
            "realisations_1": { "type": "string", "title": "Réalisations 1", "default": "• Conception et déploiement de clusters EKS multi-régions garantissant 99.99% de disponibilité pour 2.5M d'utilisateurs actifs.\\n• Automatisation complète de l'infrastructure via Terraform, réduisant le délai de mise en production de 3 semaines à 45 minutes.\\n• Réduction de 32% des coûts annuels d'infrastructure cloud grâce à l'optimisation des instances spot et du provisionnement dynamique." },
            "poste_2": { "type": "string", "title": "Poste précédent", "default": "Ingénieur Cloud & CI/CD" },
            "entreprise_2": { "type": "string", "title": "Entreprise 2", "default": "CGI Inc." },
            "ville_2": { "type": "string", "title": "Ville 2", "default": "Québec, QC" },
            "dates_2": { "type": "string", "title": "Période 2", "default": "2019 - 2022" },
            "realisations_2": { "type": "string", "title": "Réalisations 2", "default": "• Migration de 40+ microservices monolithiques vers des conteneurs Docker managés.\\n• Implémentation de tests de sécurité statiques (SAST) bloquant 100% des vulnérabilités critiques avant mise en prod.\\n• Mentorat technique de 6 jeunes ingénieurs juniors aux pratiques DevOps modernes." },
            "formations_et_diplomes": { "type": "string", "title": "Formations & Certifications", "default": "• Baccalauréat en Génie Logiciel — École Polytechnique de Montréal (2019)\\n• AWS Certified Solutions Architect - Professional (2023)\\n• Certified Kubernetes Administrator (CKA - Linux Foundation, 2022)" }
          },
          "required": ["nom_complet", "titre_professionnel", "email"]
        }
        """;

        ReportTemplate t = ReportTemplate.builder()
                .nom(name)
                .description("Format officiel canadien 100% conforme aux standards québécois et fédéraux : zéro photo, zéro critère discriminatoire, axé sur les résultats chiffrés.")
                .categorie(Categorie.CV_CANADIAN)
                .formatPapier("Letter")
                .modePagination(PaginationMode.FIXED)
                .statut(TemplateStatus.PUBLIE)
                .version(1)
                .codeEntreprise(null)
                .contenuDesign(designJson)
                .schema(schemaJson)
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        templateRepository.save(t);
    }

    private void seedCvAts(List<ReportTemplate> existing) {
        String name = "CV US ATS Clean (100% Parsing Garanti)";
        if (existing.stream().anyMatch(t -> t.getNom().equalsIgnoreCase(name))) {
            return;
        }

        log.info("Seeding template public : {}", name);
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "ats-name",
                  "type": "titre",
                  "contenu": "{{full_name}}",
                  "x": 35,
                  "y": 35,
                  "largeurBox": 746,
                  "hauteurBox": 35,
                  "style": { "fontSize": 24, "fontWeight": "bold", "color": "#000000", "textAlign": "center" }
                },
                {
                  "id": "ats-contact",
                  "type": "texte",
                  "contenu": "{{phone}} | {{email}} | {{linkedin_url}} | {{location}}",
                  "x": 35,
                  "y": 75,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 10, "color": "#222222", "textAlign": "center" }
                },
                {
                  "id": "ats-sep-1",
                  "type": "ligne",
                  "x": 35,
                  "y": 98,
                  "largeurBox": 746,
                  "hauteurBox": 1,
                  "style": { "color": "#000000" }
                },
                {
                  "id": "ats-sec1-t",
                  "type": "titre",
                  "contenu": "PROFESSIONAL SUMMARY",
                  "x": 35,
                  "y": 108,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-sec1-c",
                  "type": "texte",
                  "contenu": "{{summary_text}}",
                  "x": 35,
                  "y": 130,
                  "largeurBox": 746,
                  "hauteurBox": 55,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-sec2-t",
                  "type": "titre",
                  "contenu": "TECHNICAL SKILLS",
                  "x": 35,
                  "y": 192,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-sec2-c",
                  "type": "texte",
                  "contenu": "{{skills_text}}",
                  "x": 35,
                  "y": 214,
                  "largeurBox": 746,
                  "hauteurBox": 50,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-sec3-t",
                  "type": "titre",
                  "contenu": "PROFESSIONAL EXPERIENCE",
                  "x": 35,
                  "y": 272,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-job-1",
                  "type": "texte",
                  "contenu": "<b>{{role_1}}</b> — {{company_1}}, {{location_1}} ({{dates_1}})",
                  "x": 35,
                  "y": 296,
                  "largeurBox": 746,
                  "hauteurBox": 18,
                  "style": { "fontSize": 11, "color": "#000000" }
                },
                {
                  "id": "ats-job-1-bullets",
                  "type": "texte",
                  "contenu": "{{bullets_1}}",
                  "x": 35,
                  "y": 316,
                  "largeurBox": 746,
                  "hauteurBox": 110,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-job-2",
                  "type": "texte",
                  "contenu": "<b>{{role_2}}</b> — {{company_2}}, {{location_2}} ({{dates_2}})",
                  "x": 35,
                  "y": 435,
                  "largeurBox": 746,
                  "hauteurBox": 18,
                  "style": { "fontSize": 11, "color": "#000000" }
                },
                {
                  "id": "ats-job-2-bullets",
                  "type": "texte",
                  "contenu": "{{bullets_2}}",
                  "x": 35,
                  "y": 455,
                  "largeurBox": 746,
                  "hauteurBox": 110,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-sec4-t",
                  "type": "titre",
                  "contenu": "EDUCATION & CERTIFICATIONS",
                  "x": 35,
                  "y": 575,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-sec4-c",
                  "type": "texte",
                  "contenu": "{{education_text}}",
                  "x": 35,
                  "y": 597,
                  "largeurBox": 746,
                  "hauteurBox": 70,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "full_name": { "type": "string", "title": "Full Name", "default": "Sarah Jenkins" },
            "phone": { "type": "string", "title": "Phone", "default": "+1 (415) 555-0192" },
            "email": { "type": "string", "title": "Email", "default": "sarah.jenkins@techmail.com" },
            "linkedin_url": { "type": "string", "title": "LinkedIn URL", "default": "linkedin.com/in/sjenkins" },
            "location": { "type": "string", "title": "Location", "default": "San Francisco, CA" },
            "summary_text": { "type": "string", "title": "Summary", "default": "Results-driven Software Engineer with 6+ years of experience building high-throughput microservices, scalable distributed architectures, and modern cloud native applications. Proven track record of boosting API response times by 45% and reducing infrastructure costs." },
            "skills_text": { "type": "string", "title": "Skills", "default": "Languages: Java, Python, TypeScript, Go | Frameworks: Spring Boot, React, Node.js | Databases: PostgreSQL, MongoDB, Redis | Cloud & DevOps: AWS, Docker, Kubernetes, CI/CD, Git" },
            "role_1": { "type": "string", "title": "Role 1", "default": "Senior Backend Engineer" },
            "company_1": { "type": "string", "title": "Company 1", "default": "Stripe" },
            "location_1": { "type": "string", "title": "Location 1", "default": "San Francisco, CA" },
            "dates_1": { "type": "string", "title": "Dates 1", "default": "03/2021 - Present" },
            "bullets_1": { "type": "string", "title": "Bullet Points 1", "default": "• Architected high-availability payment routing service handling $20M+ in daily transaction volume with 99.999% uptime.\\n• Spearheaded migration from monolithic service to gRPC microservices, decreasing latency by 35ms per request.\\n• Automated deployment validation pipelines with GitHub Actions, eliminating production regression incidents." },
            "role_2": { "type": "string", "title": "Role 2", "default": "Software Engineer" },
            "company_2": { "type": "string", "title": "Company 2", "default": "Salesforce" },
            "location_2": { "type": "string", "title": "Location 2", "default": "San Francisco, CA" },
            "dates_2": { "type": "string", "title": "Dates 2", "default": "08/2018 - 02/2021" },
            "bullets_2": { "type": "string", "title": "Bullet Points 2", "default": "• Engineered enterprise CRM reporting module serving 500,000+ daily active enterprise users.\\n• Refactored SQL queries and introduced Redis cache layer, reducing database CPU load by 40%.\\n• Collaborated closely with Product Managers and UX designers to roll out 8 major customer-facing features on schedule." },
            "education_text": { "type": "string", "title": "Education & Certifications", "default": "• B.S. in Computer Science — University of California, Berkeley (2018)\\n• AWS Certified Developer - Associate (2022)\\n• Oracle Certified Professional Java SE 17 Developer" }
          },
          "required": ["full_name", "email", "phone"]
        }
        """;

        ReportTemplate t = ReportTemplate.builder()
                .nom(name)
                .description("Format strict US Letter 1 page 100% optimisé pour les robots recruteurs ATS (Workday, Taleo, Greenhouse). Typographie linéaire et lecture sans friction.")
                .categorie(Categorie.CV_ATS)
                .formatPapier("Letter")
                .modePagination(PaginationMode.FIXED)
                .statut(TemplateStatus.PUBLIE)
                .version(1)
                .codeEntreprise(null)
                .contenuDesign(designJson)
                .schema(schemaJson)
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        templateRepository.save(t);
    }

    private void seedCvEuro(List<ReportTemplate> existing) {
        String name = "CV Européen Moderne & Dynamique";
        if (existing.stream().anyMatch(t -> t.getNom().equalsIgnoreCase(name))) {
            return;
        }

        log.info("Seeding template public : {}", name);
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "euro-side-bg",
                  "type": "rectangle",
                  "x": 0,
                  "y": 0,
                  "largeurBox": 260,
                  "hauteurBox": 1123,
                  "style": { "backgroundColor": "#1e293b", "color": "#ffffff" }
                },
                {
                  "id": "euro-side-name",
                  "type": "titre",
                  "contenu": "{{nom_complet}}",
                  "x": 25,
                  "y": 50,
                  "largeurBox": 210,
                  "hauteurBox": 55,
                  "style": { "fontSize": 20, "fontWeight": "bold", "color": "#ffffff", "textAlign": "center" }
                },
                {
                  "id": "euro-side-role",
                  "type": "texte",
                  "contenu": "{{titre_poste}}",
                  "x": 25,
                  "y": 110,
                  "largeurBox": 210,
                  "hauteurBox": 30,
                  "style": { "fontSize": 12, "color": "#38bdf8", "textAlign": "center", "fontWeight": "600" }
                },
                {
                  "id": "euro-side-contact-t",
                  "type": "titre",
                  "contenu": "CONTACT",
                  "x": 25,
                  "y": 160,
                  "largeurBox": 210,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#94a3b8", "borderBottom": "1px solid #334155" }
                },
                {
                  "id": "euro-side-contact-c",
                  "type": "texte",
                  "contenu": "📧 {{email}}\\n📱 {{telephone}}\\n📍 {{adresse_ville}}\\n🔗 {{portfolio_link}}",
                  "x": 25,
                  "y": 188,
                  "largeurBox": 210,
                  "hauteurBox": 90,
                  "style": { "fontSize": 10, "color": "#e2e8f0", "lineHeight": "1.7" }
                },
                {
                  "id": "euro-side-skills-t",
                  "type": "titre",
                  "contenu": "COMPÉTENCES",
                  "x": 25,
                  "y": 295,
                  "largeurBox": 210,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#94a3b8", "borderBottom": "1px solid #334155" }
                },
                {
                  "id": "euro-side-skills-c",
                  "type": "texte",
                  "contenu": "{{competences_liste}}",
                  "x": 25,
                  "y": 322,
                  "largeurBox": 210,
                  "hauteurBox": 140,
                  "style": { "fontSize": 10, "color": "#e2e8f0", "lineHeight": "1.6" }
                },
                {
                  "id": "euro-side-lang-t",
                  "type": "titre",
                  "contenu": "LANGUES",
                  "x": 25,
                  "y": 480,
                  "largeurBox": 210,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#94a3b8", "borderBottom": "1px solid #334155" }
                },
                {
                  "id": "euro-side-lang-c",
                  "type": "texte",
                  "contenu": "{{langues_liste}}",
                  "x": 25,
                  "y": 508,
                  "largeurBox": 210,
                  "hauteurBox": 80,
                  "style": { "fontSize": 10, "color": "#e2e8f0", "lineHeight": "1.6" }
                },
                {
                  "id": "euro-main-about-t",
                  "type": "titre",
                  "contenu": "À PROPOS DE MOI",
                  "x": 285,
                  "y": 45,
                  "largeurBox": 480,
                  "hauteurBox": 25,
                  "style": { "fontSize": 14, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "2px solid #0284c7" }
                },
                {
                  "id": "euro-main-about-c",
                  "type": "texte",
                  "contenu": "{{a_propos}}",
                  "x": 285,
                  "y": 76,
                  "largeurBox": 480,
                  "hauteurBox": 70,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "euro-main-exp-t",
                  "type": "titre",
                  "contenu": "PARCOURS PROFESSIONNEL",
                  "x": 285,
                  "y": 160,
                  "largeurBox": 480,
                  "hauteurBox": 25,
                  "style": { "fontSize": 14, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "2px solid #0284c7" }
                },
                {
                  "id": "euro-exp-1",
                  "type": "texte",
                  "contenu": "<b>{{poste_1}}</b> — {{entreprise_1}} ({{dates_1}})<br/><span style='color:#64748b;font-size:10px;'>{{ville_1}}</span><br/>{{desc_1}}",
                  "x": 285,
                  "y": 192,
                  "largeurBox": 480,
                  "hauteurBox": 115,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.4" }
                },
                {
                  "id": "euro-exp-2",
                  "type": "texte",
                  "contenu": "<b>{{poste_2}}</b> — {{entreprise_2}} ({{dates_2}})<br/><span style='color:#64748b;font-size:10px;'>{{ville_2}}</span><br/>{{desc_2}}",
                  "x": 285,
                  "y": 320,
                  "largeurBox": 480,
                  "hauteurBox": 115,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.4" }
                },
                {
                  "id": "euro-main-edu-t",
                  "type": "titre",
                  "contenu": "DIPLÔMES & CERTIFICATIONS",
                  "x": 285,
                  "y": 450,
                  "largeurBox": 480,
                  "hauteurBox": 25,
                  "style": { "fontSize": 14, "fontWeight": "bold", "color": "#0f172a", "borderBottom": "2px solid #0284c7" }
                },
                {
                  "id": "euro-edu-c",
                  "type": "texte",
                  "contenu": "{{formations_texte}}",
                  "x": 285,
                  "y": 482,
                  "largeurBox": 480,
                  "hauteurBox": 120,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "nom_complet": { "type": "string", "title": "Nom Complet", "default": "Thomas Lefebvre" },
            "titre_poste": { "type": "string", "title": "Poste", "default": "Chef de Projet Digital & Data" },
            "email": { "type": "string", "title": "Email", "default": "thomas.lefebvre@pro.fr" },
            "telephone": { "type": "string", "title": "Téléphone", "default": "+33 6 12 34 56 78" },
            "adresse_ville": { "type": "string", "title": "Ville", "default": "Paris, France" },
            "portfolio_link": { "type": "string", "title": "Portfolio / LinkedIn", "default": "linkedin.com/in/tlefebvre" },
            "competences_liste": { "type": "string", "title": "Compétences", "default": "• Pilotage Scrum / Agile\\n• Analyse Data (SQL, Tableau)\\n• Architecture Cloud (AWS)\\n• Gestion Budgétaire & ROI\\n• Management transversal" },
            "langues_liste": { "type": "string", "title": "Langues", "default": "• Français : Langue maternelle\\n• Anglais : Courant (TOEIC 940)\\n• Espagnol : Professionnel" },
            "a_propos": { "type": "string", "title": "À propos", "default": "Professionnel du digital passionné par la valorisation des données et la transformation agile. Fort de 8 ans d'expérience dans le déploiement de solutions SaaS à fort impact économique pour des comptes du CAC 40." },
            "poste_1": { "type": "string", "title": "Poste 1", "default": "Senior Digital Product Owner" },
            "entreprise_1": { "type": "string", "title": "Entreprise 1", "default": "Capgemini Invent" },
            "ville_1": { "type": "string", "title": "Ville 1", "default": "Paris" },
            "dates_1": { "type": "string", "title": "Dates 1", "default": "2021 - Présent" },
            "desc_1": { "type": "string", "title": "Description 1", "default": "• Conduite de 4 projets d'automatisation des rapports financiers avec une équipe de 12 développeurs.\\n• Augmentation de 28% de la satisfaction utilisateur dès le premier trimestre post-lancement." },
            "poste_2": { "type": "string", "title": "Poste 2", "default": "Chef de Projet Informatique" },
            "entreprise_2": { "type": "string", "title": "Entreprise 2", "default": "Sopra Steria" },
            "ville_2": { "type": "string", "title": "Ville 2", "default": "Lyon" },
            "dates_2": { "type": "string", "title": "Dates 2", "default": "2018 - 2021" },
            "desc_2": { "type": "string", "title": "Description 2", "default": "• Supervision de la refonte des portails clients B2B pour le secteur bancaire et assurantiel.\\n• Respect strict des jalons contractuels et des normes de sécurité RGPD." },
            "formations_texte": { "type": "string", "title": "Formations", "default": "• Master 2 Management des Systèmes d'Information — Université Paris-Dauphine (2018)\\n• Licence Économie & Gestion — Sorbonne Université (2016)\\n• Certification Scrum Master PSM II (Scrum.org)" }
          },
          "required": ["nom_complet", "titre_poste", "email"]
        }
        """;

        ReportTemplate t = ReportTemplate.builder()
                .nom(name)
                .description("Design européen contemporain au format A4 avec barre latérale sombre, hiérarchie visuelle raffinée et sections équilibrées pour recruteurs exigeants.")
                .categorie(Categorie.CV_EUROPEEN)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .statut(TemplateStatus.PUBLIE)
                .version(1)
                .codeEntreprise(null)
                .contenuDesign(designJson)
                .schema(schemaJson)
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        templateRepository.save(t);
    }

    private void seedRapportStage(List<ReportTemplate> existing) {
        String name = "Rapport de Stage & PFE Universitaire Officiel";
        if (existing.stream().anyMatch(t -> t.getNom().equalsIgnoreCase(name))) {
            return;
        }

        log.info("Seeding template public : {}", name);
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page de Garde",
              "blocs": [
                {
                  "id": "stg-univ-header",
                  "type": "texte",
                  "contenu": "<b>{{universite}}</b> — {{faculte_departement}}",
                  "x": 40,
                  "y": 45,
                  "largeurBox": 714,
                  "hauteurBox": 30,
                  "style": { "fontSize": 14, "color": "#475569", "textAlign": "center" }
                },
                {
                  "id": "stg-sep-top",
                  "type": "ligne",
                  "x": 150,
                  "y": 85,
                  "largeurBox": 494,
                  "hauteurBox": 2,
                  "style": { "color": "#0284c7" }
                },
                {
                  "id": "stg-doc-type",
                  "type": "texte",
                  "contenu": "RAPPORT DE STAGE DE FIN D'ÉTUDES",
                  "x": 40,
                  "y": 180,
                  "largeurBox": 714,
                  "hauteurBox": 30,
                  "style": { "fontSize": 16, "letterSpacing": "3px", "fontWeight": "bold", "color": "#0284c7", "textAlign": "center" }
                },
                {
                  "id": "stg-doc-title",
                  "type": "titre",
                  "contenu": "{{titre_sujet}}",
                  "x": 40,
                  "y": 225,
                  "largeurBox": 714,
                  "hauteurBox": 70,
                  "style": { "fontSize": 26, "fontWeight": "800", "color": "#0f172a", "textAlign": "center", "lineHeight": "1.3" }
                },
                {
                  "id": "stg-doc-subtitle",
                  "type": "texte",
                  "contenu": "{{sous_titre}}",
                  "x": 40,
                  "y": 305,
                  "largeurBox": 714,
                  "hauteurBox": 40,
                  "style": { "fontSize": 13, "color": "#64748b", "textAlign": "center" }
                },
                {
                  "id": "stg-box-cadre",
                  "type": "rectangle",
                  "x": 80,
                  "y": 420,
                  "largeurBox": 634,
                  "hauteurBox": 240,
                  "style": { "backgroundColor": "#f8fafc", "border": "1px solid #e2e8f0", "borderRadius": "8px" }
                },
                {
                  "id": "stg-meta-etudiant",
                  "type": "texte",
                  "contenu": "<b>Présenté par :</b><br/>{{nom_etudiant}}<br/>Filière : {{filiere_promo}}",
                  "x": 110,
                  "y": 445,
                  "largeurBox": 260,
                  "hauteurBox": 70,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-meta-entreprise",
                  "type": "texte",
                  "contenu": "<b>Organisme d'accueil :</b><br/>{{entreprise_accueil}}<br/>Lieu : {{lieu_stage}}",
                  "x": 410,
                  "y": 445,
                  "largeurBox": 270,
                  "hauteurBox": 70,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-meta-tuteur-acad",
                  "type": "texte",
                  "contenu": "<b>Tuteur académique :</b><br/>{{tuteur_universitaire}}",
                  "x": 110,
                  "y": 535,
                  "largeurBox": 260,
                  "hauteurBox": 60,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-meta-tuteur-ent",
                  "type": "texte",
                  "contenu": "<b>Maître de stage en entreprise :</b><br/>{{maitre_stage}}",
                  "x": 410,
                  "y": 535,
                  "largeurBox": 270,
                  "hauteurBox": 60,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-footer-year",
                  "type": "texte",
                  "contenu": "Année Universitaire {{annee_universitaire}}",
                  "x": 40,
                  "y": 780,
                  "largeurBox": 714,
                  "hauteurBox": 25,
                  "style": { "fontSize": 12, "fontWeight": "600", "color": "#475569", "textAlign": "center" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "universite": { "type": "string", "title": "Université / École", "default": "Université Claude Bernard Lyon 1" },
            "faculte_departement": { "type": "string", "title": "Département", "default": "Faculté des Sciences & Technologies — Département Informatique" },
            "titre_sujet": { "type": "string", "title": "Titre du Stage", "default": "Conception et Optimisation d'un Moteur Hybride de Génération Documentaire Haute Disponibilité" },
            "sous_titre": { "type": "string", "title": "Sous-titre / Spécialité", "default": "Mémoire de fin d'études en vue de l'obtention du Diplôme d'Ingénieur en Génie Logiciel" },
            "nom_etudiant": { "type": "string", "title": "Nom de l'étudiant", "default": "Camille Mercier" },
            "filiere_promo": { "type": "string", "title": "Filière & Promo", "default": "Master 2 Génie Logiciel (Promo 2026)" },
            "entreprise_accueil": { "type": "string", "title": "Entreprise d'accueil", "default": "Mera Tech Solutions SAS" },
            "lieu_stage": { "type": "string", "title": "Lieu du stage", "default": "Lyon, France" },
            "tuteur_universitaire": { "type": "string", "title": "Tuteur Universitaire", "default": "Pr. Robert Vandevelde" },
            "maitre_stage": { "type": "string", "title": "Maître de Stage", "default": "M. Marc Delorme (Lead Architect)" },
            "annee_universitaire": { "type": "string", "title": "Année Universitaire", "default": "2025 - 2026" }
          },
          "required": ["universite", "titre_sujet", "nom_etudiant", "entreprise_accueil"]
        }
        """;

        ReportTemplate t = ReportTemplate.builder()
                .nom(name)
                .description("Page de garde universitaire officielle pour mémoires, PFE, thèses et rapports de stage. Conforme aux chartes académiques européennes.")
                .categorie(Categorie.STAGE_ACADEMIQUE)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .statut(TemplateStatus.PUBLIE)
                .version(1)
                .codeEntreprise(null)
                .contenuDesign(designJson)
                .schema(schemaJson)
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        templateRepository.save(t);
    }

    private void seedFactureFreelance(List<ReportTemplate> existing) {
        String name = "Facture & Devis Freelance Conforme";
        if (existing.stream().anyMatch(t -> t.getNom().equalsIgnoreCase(name))) {
            return;
        }

        log.info("Seeding template public : {}", name);
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "inv-emetteur",
                  "type": "texte",
                  "contenu": "<b>{{emetteur_nom}}</b><br/>{{emetteur_activite}}<br/>SIRET : {{emetteur_siret}}<br/>{{emetteur_adresse}}<br/>{{emetteur_email}} | {{emetteur_telephone}}",
                  "x": 40,
                  "y": 40,
                  "largeurBox": 320,
                  "hauteurBox": 90,
                  "style": { "fontSize": 11, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "inv-title-box",
                  "type": "titre",
                  "contenu": "FACTURE",
                  "x": 450,
                  "y": 35,
                  "largeurBox": 304,
                  "hauteurBox": 35,
                  "style": { "fontSize": 26, "fontWeight": "bold", "color": "#0284c7", "textAlign": "right" }
                },
                {
                  "id": "inv-meta-dates",
                  "type": "texte",
                  "contenu": "N° Facture : <b>{{numero_facture}}</b><br/>Date d'émission : {{date_facture}}<br/>Date d'échéance : {{date_echeance}}",
                  "x": 450,
                  "y": 75,
                  "largeurBox": 304,
                  "hauteurBox": 55,
                  "style": { "fontSize": 11, "color": "#475569", "textAlign": "right", "lineHeight": "1.5" }
                },
                {
                  "id": "inv-client-box",
                  "type": "rectangle",
                  "x": 450,
                  "y": 140,
                  "largeurBox": 304,
                  "hauteurBox": 85,
                  "style": { "backgroundColor": "#f8fafc", "border": "1px solid #e2e8f0", "borderRadius": "6px" }
                },
                {
                  "id": "inv-client-text",
                  "type": "texte",
                  "contenu": "<b>FACTURER À :</b><br/><b>{{client_nom}}</b><br/>{{client_adresse}}<br/>N° TVA / Ref : {{client_reference}}",
                  "x": 465,
                  "y": 150,
                  "largeurBox": 275,
                  "hauteurBox": 70,
                  "style": { "fontSize": 11, "color": "#1e293b", "lineHeight": "1.4" }
                },
                {
                  "id": "inv-sep-mid",
                  "type": "ligne",
                  "x": 40,
                  "y": 240,
                  "largeurBox": 714,
                  "hauteurBox": 1,
                  "style": { "color": "#cbd5e1" }
                },
                {
                  "id": "inv-items-table",
                  "type": "tableau",
                  "source": "{{lignes_facture}}",
                  "x": 40,
                  "y": 255,
                  "largeurBox": 714,
                  "hauteurBox": 160,
                  "style": {
                    "headerBackground": "#0f172a",
                    "headerColor": "#ffffff",
                    "borderColor": "#e2e8f0",
                    "fontSize": 11
                  }
                },
                {
                  "id": "inv-totals-box",
                  "type": "rectangle",
                  "x": 450,
                  "y": 435,
                  "largeurBox": 304,
                  "hauteurBox": 110,
                  "style": { "backgroundColor": "#f1f5f9", "borderRadius": "6px" }
                },
                {
                  "id": "inv-totals-text",
                  "type": "texte",
                  "contenu": "Total HT : <b>{{total_ht}} €</b><br/>TVA (20%) : <b>{{total_tva}} €</b><br/><span style='font-size:14px;color:#0284c7;'>Total TTC : <b>{{total_ttc}} €</b></span>",
                  "x": 470,
                  "y": 448,
                  "largeurBox": 265,
                  "hauteurBox": 85,
                  "style": { "fontSize": 12, "color": "#0f172a", "textAlign": "right", "lineHeight": "1.7" }
                },
                {
                  "id": "inv-bank-box",
                  "type": "texte",
                  "contenu": "<b>Règlement par virement bancaire :</b><br/>IBAN : {{iban}}<br/>BIC : {{bic}}<br/>Titulaire : {{emetteur_nom}}",
                  "x": 40,
                  "y": 445,
                  "largeurBox": 380,
                  "hauteurBox": 75,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "inv-legal-notices",
                  "type": "texte",
                  "contenu": "Mentions légales : En cas de retard de paiement, indemnité forfaitaire pour frais de recouvrement de 40 € (art. D.441-5 C. com.) et pénalités de retard au taux légal BCE majoré de 10 points. Pas d'escompte pour paiement anticipé.",
                  "x": 40,
                  "y": 720,
                  "largeurBox": 714,
                  "hauteurBox": 40,
                  "style": { "fontSize": 8, "color": "#94a3b8", "textAlign": "center", "lineHeight": "1.3" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "emetteur_nom": { "type": "string", "title": "Nom ou Société", "default": "Studio Apex Consulting (Julien Laurent)" },
            "emetteur_activite": { "type": "string", "title": "Activité", "default": "Conseil en Systèmes Informatiques & Cloud" },
            "emetteur_siret": { "type": "string", "title": "SIRET", "default": "892 411 902 00018" },
            "emetteur_adresse": { "type": "string", "title": "Adresse Émetteur", "default": "12 Rue de la République, 69002 Lyon" },
            "emetteur_email": { "type": "string", "title": "Email Émetteur", "default": "julien@apex-consulting.fr" },
            "emetteur_telephone": { "type": "string", "title": "Téléphone", "default": "+33 6 45 78 90 12" },
            "numero_facture": { "type": "string", "title": "Numéro de Facture", "default": "FA-2026-089" },
            "date_facture": { "type": "string", "title": "Date d'émission", "default": "15/10/2026" },
            "date_echeance": { "type": "string", "title": "Date d'échéance", "default": "15/11/2026" },
            "client_nom": { "type": "string", "title": "Nom Client", "default": "Acme Global Solutions SAS" },
            "client_adresse": { "type": "string", "title": "Adresse Client", "default": "45 Avenue des Champs-Élysées, 75008 Paris" },
            "client_reference": { "type": "string", "title": "Ref Client / TVA", "default": "FR 89 423 789 123" },
            "total_ht": { "type": "string", "title": "Total HT", "default": "4 250,00" },
            "total_tva": { "type": "string", "title": "Total TVA", "default": "850,00" },
            "total_ttc": { "type": "string", "title": "Total TTC", "default": "5 100,00" },
            "iban": { "type": "string", "title": "IBAN", "default": "FR76 3000 4000 0100 1234 5678 901" },
            "bic": { "type": "string", "title": "BIC", "default": "BNPAFRPPXXX" }
          },
          "required": ["emetteur_nom", "numero_facture", "client_nom", "total_ttc"]
        }
        """;

        ReportTemplate t = ReportTemplate.builder()
                .nom(name)
                .description("Facture freelance professionnelle avec tableau d'honoraires, calculs de TVA, coordonnées bancaires IBAN/BIC et mentions légales obligatoires françaises.")
                .categorie(Categorie.FACTURE_DEVIS)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .statut(TemplateStatus.PUBLIE)
                .version(1)
                .codeEntreprise(null)
                .contenuDesign(designJson)
                .schema(schemaJson)
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        templateRepository.save(t);
    }
}

