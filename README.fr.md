# ASCENT

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · [Español](README.es.md) · **[Français](README.fr.md)** · [Italiano](README.it.md) · [Русский](README.ru.md) · [Svenska](README.sv.md)

Une application Android pour celles et ceux qui débutent en alpinisme d’altitude et en cascade de glace. Le programme est organisé par altitude : chaque leçon passée fait monter d’un cran.

Deux parcours se changent librement. La **haute montagne** compte six unités et 68 leçons, de l’initiation à 5 000 m jusqu’à 8 000 m — de 3 200 m à 8 848 m d’un bout à l’autre. La **randonnée** compte quatre unités et 21 leçons, de la sortie à la journée aux itinéraires longs — de 200 m à 5 630 m. Chaque parcours garde sa progression, ses badges et sa file de révision.

Version actuelle v0.20.18 (versionCode 125). Le travail sur les langues s’est achevé avec la v0.19.24 — les neuf langues traduites correspondent à l’anglais entrée par entrée (10 554 / 10 554). La v0.20 concerne les badges : les quarante succès sont désormais rendus en 3D temps réel au lieu d’images plates, et leurs icônes proviennent des mêmes modèles — les anciennes illustrations générées par IA ont disparu. La fenêtre de déblocage utilise le même moteur et projette des confettis depuis les deux bords de l’écran. Les modèles voyagent dans l’APK : le paquet est passé de 21 Mo à environ 41 Mo. La v0.20.3 corrige une vieille gêne : on descendait dans une liste, on ouvrait un écran secondaire, on revenait, et la liste sautait en haut — elle reste maintenant là où vous l’avez laissée. Les v0.20.6–v0.20.7 poussent le rendu des badges en qualité maximale par défaut : bloom plus fin qui ne s’éteint plus à cause des premières images lentes, déclassement en deux paliers et MSAA 4x avec vérification de capacité et repli silencieux.

## Installation

Le plus simple est l’APK debug déjà compilé des [Releases](https://github.com/Solar-CV/AscentApp/releases). Pousser un tag `v*` fait construire et joindre un APK neuf par GitHub Actions.

- Paquet : `com.ascent.climb`
- Minimum : Android 8.0 (API 26), **arm64-v8a uniquement** — les téléphones uniquement 32 bits ne peuvent pas installer cette version
- Taille : environ 41 Mo (37 Mo de modèles non compressés, environ 10 Mo après empaquetage)
- Autorisations : réseau et état du réseau — pour vérifier les mises à jour de la banque de questions et envoyer un retour. Tout fonctionne hors ligne ; la banque embarquée est toujours là

Transférez l’APK sur le téléphone, autorisez l’installation de sources inconnues, appuyez sur installer. Aucune connexion au premier lancement.

## Ce qu’il y a dedans

### Le parcours

L’écran d’accueil est un itinéraire qui monte depuis le camp de base, découpé en unités d’altitude (six en haute montagne, quatre en randonnée). On passe de l’une à l’autre en balayant horizontalement, deux unités par page. Les leçons se débloquent linéairement : une réussie, la suivante s’ouvre. Les nœuds affichent des étoiles, la leçon en cours porte une indication « commencer », la dernière d’une unité est marquée en or.

Deux pastilles en haut changent de parcours ; un appui ouvre une feuille inférieure. Le changement revient à la première page et le choix est mémorisé.

La progression n’est pas un nombre stocké. Elle est recalculée à partir des leçons terminées : si la courbe d’altitude est ajustée plus tard, les anciennes sauvegardes se recalculent au lieu de rester bloquées.

### Banque de questions

1 533 questions officielles plus 154 copies de révision insérées automatiquement, soit 1 687 éléments affichés. Le volume monte avec l’altitude : une quinzaine de questions par leçon dans l’initiation à 5 000 m, 30 dans les unités techniques à 7 000 m, 8 en randonnée.

Huit types de questions : choix unique, choix multiple, ordonnancement, appariement, texte à trous, calcul, point chaud (appuyer sur un point de l’image) et scénario (un énoncé traité en plusieurs étapes).

À la fin d’une leçon, on reprend directement celles qui ont été ratées. Celles réussies comptent, les autres rejoignent le centre de révision. La révision utilise la répétition espacée à 0, 1, 3, 7, 16 et 35 jours. Les questions critiques pour la sécurité sont pondérées et exigent de confirmer la compréhension avant de continuer.

### Manuel

35 articles longs, de 4 à 8 sections, classés par équipement, technique, médecine, santé, météo, météorologie, décisions, jugement, sécurité, risque et secours en avalanche. Le manuel est filtré par parcours : les pièces neige et glace n’apparaissent qu’en haute montagne, tandis que la médecine d’altitude, l’hypothermie, la navigation, le bivouac et le leadership sont communs aux deux. Les sources sont des documents médicaux et techniques publics, trois dossiers commerciaux de guides, et des transcriptions retravaillées de sept livres de montagne chinois. Par accord produit, les livres chinois ne sont pas cités par leur titre ; les dossiers de guides le sont.

### Entraînement, révision, profil

L’entraînement donne un pourcentage de préparation et l’altitude du sommet visé. La révision place devant les questions échues du jour et les erreurs critiques de sécurité. Le profil réunit l’en-tête, l’altitude actuelle, l’aperçu des capacités, l’accès aux badges, la zone de compte et une entrée de retour. L’engrenage ouvre les réglages, l’avatar la page de profil.

Le profil n’affiche que les badges débloqués. Les 40 sont sur le mur de badges, les verrouillés en aperçu gris, les succès cachés sous un « ? ».

La plupart existent aussi en vrai modèle 3D — cadres métalliques, émaux, relief. Ouvrez la zone de test des succès et touchez un badge pour le voir tourner : glisser pour pivoter, pincer pour zoomer, et il oscille doucement de lui-même quand vous lâchez. Leurs icônes viennent des modèles eux-mêmes : le mur et la vue 3D montrent le même objet ; les anciennes illustrations par IA ont été retirées. Les quarante ont désormais un modèle ; le dessin à la main reste le repli des badges ajoutés plus tard. Débloquer un succès n’importe où dans l’application affiche le même badge 3D, sur un socle clair, avec des rubans qui entrent depuis les deux bords de l’écran.

### Réglages

L’apparence suit le système ou se fige en clair ou sombre, est conservée, la barre d’état suit. La langue suit le système ou se choisit à la main ; interface et contenu changent ensemble, sans redémarrage. La section banque affiche la version courante, cherche des mises à jour et restaure la banque embarquée. Le mode développeur se cache derrière le numéro de version dans « À propos » : sept appuis.

### Retour

Profil → Retour et suggestions, entre la carte de compte et l’avertissement de sécurité. Choisissez un type — problème, idée, correction de contenu —, écrivez le message, laissez éventuellement une adresse pour la réponse et joignez éventuellement les diagnostics. L’envoi part directement au mainteneur via Formspree ; sans réseau, l’écran propose d’envoyer le même texte par e-mail et le copie dans le presse-papiers s’il n’existe aucune application de messagerie.

Les diagnostics sont désactivés par défaut et couvrent la version de l’application, la version d’Android, le modèle d’appareil, la langue, la version de la banque, le parcours en cours, les chiffres de progression et la dernière exception non interceptée. Pas de texte de leçon, pas d’historique de réponses, pas de compte, pas de presse-papiers.

## Dix langues

Les textes d’interface et le contenu des cours sont disponibles en :

简体中文 (source), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

Les dix langues sont complètes. Chacune a sa couche d’entrée, ses résumés de leçon, le corps du manuel et une banque de questions, et chaque jeu de clés correspond à l’anglais entrée par entrée — **10 554 / 10 554**, rien ne manque, rien n’est dupliqué, aucune clé orpheline.

| Couche | Éléments | Les 9 langues traduites |
|---|---|---|
| Entrée (leçons, unités, titres du manuel, sommets) | 117 | 100 % |
| Points clés des leçons | 443 | 100 % |
| Corps du manuel | 378 | 100 % |
| Banque de questions (énoncé, options, explication) | 9 422 | 100 % |

La terminologie suit l’usage montagnard de chaque langue plutôt qu’une traduction littérale : suédois stegjärn / isyxa / standplats, allemand Steigeisen / Pickel, russe кошки / ледоруб, italien corda fissa / bloccante / sosta. Les doses médicales, les niveaux d’avalanche, les pentes et les températures sont transposés tels quels — seul le format des nombres s’adapte, et les trois conventions restent volontairement séparées :

- Anglais, chinois, japonais : `6.5 °C`, `5,000 m`
- Allemand, espagnol, italien : `6,5 °C`, `1.000 m` (virgule décimale, point des milliers)
- Français, suédois : `6,5 °C`, `5 000 m` (virgule décimale, espace des milliers)

L’adresse au lecteur change aussi : l’allemand emploie la forme impersonnelle avec *man*, l’espagnol tutoie avec *tú*, le français vouvoie, le suédois et l’italien tutoient avec *du* / *tu* — les résumés de leçon, eux, restent impersonnels partout.

Si un élément manque dans la langue choisie, l’ordre est : langue choisie → anglais → chinois d’origine. Ce chemin n’est plus qu’un filet de sécurité.

## Programme

**Haute montagne**

| Unité | Altitude | Leçons | Questions | Par leçon |
|---|---|---|---|---|
| 5000M · Initiation | 5 000 m | 16 | 249 | 16 |
| 5000M · Progression | 5 300 m | 12 | 221 | 18 |
| 6000M | 6 000 m | 14 | 316 | 22 |
| 7000M · Camp technique C3 | 7 000 m | 6 | 185 | 31 |
| 7000M · Camp d’altitude C4 | 7 500 m | 10 | 279 | 28 |
| 8000M | 8 000 m | 10 | 248 | 24 |

**Randonnée**

| Unité | Altitude | Leçons | Questions | Par leçon |
|---|---|---|---|---|
| Sorties à la journée | 1 200 m | 5 | 40 | 8 |
| Plusieurs jours et bivouac | 2 500 m | 8 | 79 | 10 |
| Randonnée d’altitude | 3 800 m | 5 | 44 | 8 |
| Itinéraires longs et urgence | 4 500 m | 3 | 26 | 8 |

38 sommets et itinéraires au total : 30 en haute montagne, de l’Aotaina à 5 211 m aux quatorze 8 000, et 8 en randonnée, du sentier de Huihang à l’itinéraire des Trois Cols à l’Everest.

## D’où viennent les contenus

Deux flux principaux alimentent les questions et le manuel.

Les sources publiques comprennent la mise à jour 2024 des recommandations cliniques de la Wilderness Medical Society sur le mal aigu des montagnes, le CDC Yellow Book, des documents techniques de l’American Alpine Institute et de l’American Alpine Club, REI Expert Advice, les normes d’équipement UIAA et CE, et les données d’avalanche d’avalanche.org.

Les trois dossiers de SEVEN SUMMIT TREKS — mal d’altitude, équipement, pratique générale d’expédition — forment le second flux.

Plus tard, des transcriptions corrigées de sept livres de montagne chinois ont été retravaillées pour le manuel. Les connaissances et les cas sont réécrits avec nos mots, avec les références de page ; le texte scanné n’est pas conservé dans le dépôt.

| Source | Utilisée pour |
|---|---|
| WMS / CDC / StatPearls | Questions et manuel de médecine d’altitude |
| AAI / AAC / REI / BMC | Technique neige, glace et progression |
| Normes UIAA / CE | Questions d’équipement |
| avalanche.org | Jugement sur les avalanches |
| Dossiers SEVEN SUMMIT TREKS | Manuel h17–h22 et questions associées |
| 7 livres de montagne chinois | Manuel h23–h31 |

## Limite de sécurité

L’application ne couvre que la théorie et la préparation mentale. Les gestes pratiques — neige et glace, cordes, cascade de glace, secours — doivent être enseignés et évalués sur le terrain par un formateur diplômé.

Rien ici ne constitue un avis médical. Les décisions de médicament et de premiers secours appartiennent à un médecin.

Aucun sommet n’est promis, aucun résultat d’acclimatation n’est garanti.

Les leçons marquées comme nécessitant un formateur le disent clairement une fois la théorie terminée.

## Le compiler soi-même

Toute la chaîne d’outils peut vivre dans le dossier personnel — sans Homebrew, sans Android Studio. Ce que cette machine utilise :

- `~/android-toolchain/jdk` : Temurin OpenJDK 17
- `~/android-toolchain/sdk` : Android SDK avec platforms;android-34, build-tools;34.0.0, platform-tools
- `~/android-toolchain/gradle-8.9` : Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

Le dépôt fournit aussi le wrapper Gradle 8.9, donc une machine sans Gradle fonctionne également :

```bash
cd AscentApp
./gradlew assembleDebug
```

Le résultat atterrit dans `app/build/outputs/apk/debug/app-debug.apk`.

## Structure du projet

```
AscentApp/
├── import/                      Rédaction des questions (Markdown, depuis des livres imprimés)
├── knowledge/                   Base de connaissances des sources imprimées
├── tools/
│   ├── contentcheck/            Vérifications de contenu + export de la banque (JVM pure)
│   ├── i18ncheck.py             Couverture par langue sur les quatre couches
│   ├── qbank/build.mjs          Compilateur Markdown → banque de questions
│   └── poc3d/                   Synchronisation des modèles, détourage des icônes, cuisson de l’émissif
└── app/src/main/
    ├── res/values*/             Textes d’interface pour 10 langues
    ├── assets/
    │   ├── badges/*.glb         40 modèles de badge
    │   └── env/studio_ibl.ktx   Environnement IBL généré par code
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt      Contenu des leçons principales
        │   ├── NewLessons.kt    Leçons du programme récent
        │   ├── i18n/Pack*.kt    Traductions de contenu (entrée / résumé / manuel / banque)
        │   ├── Diagnostics.kt   Journal de plantage + instantané d’environnement pour le retour
        │   ├── FeedbackSender.kt  POST Formspree (HttpURLConnection, aucune dépendance)
        │   ├── BundledBank.kt   Assemblage du pool de leçons et insertion des révisions
        │   └── bank/            Configuration de la banque, décodage JSON, réseau, dépôt
        └── ui/                  Écrans et composants (Badge3D.kt = scène Filament, cadrage, lumières)
```

## Modifier le contenu

Pour ajouter des questions à une leçon existante, modifiez `data/Expansion*.kt`. Pour une nouvelle leçon, écrivez `Lesson(...)` dans `data/NewLessons.kt` et ajoutez son id à l’unité correspondante dans `Curriculum.kt`. L’ordre et l’appartenance aux unités ne vivent que dans `Curriculum.kt`.

Puis lancez les vérifications :

```bash
bash tools/contentcheck/run.sh
python3 tools/i18ncheck.py
```

Le premier script synchronise le contenu de l’application dans un projet JVM pur, exécute la vraie logique d’assemblage et vérifie le nombre de questions, la progression de difficulté, les réponses hors bornes, les questions vides, la logique de déblocage et les définitions de succès, puis exporte `bank/bank.json` et `bank/manifest.json`. Le second rapporte la couverture par langue sur les quatre couches et compare les clés. Aucun des deux n’a besoin d’un émulateur.

Après avoir touché à la banque, incrémentez `BUNDLED_VERSION` dans `data/bank/BankConfig.kt`, sinon les clients croiront que rien n’a changé.

## Mises à jour en ligne de la banque

La banque a trois couches : cache local, embarquée, delta en ligne. L’ordre de lecture est cache → embarquée ; la copie en ligne n’écrase le cache que si elle est plus récente.

Ainsi, un avion, une vallée sans réseau, un serveur mort, un JSON mal formé, un sha256 qui ne correspond pas ou une version plus ancienne laissent tout en l’état. Les mises à jour sont vérifiées en silence après le lancement, au plus toutes les six heures et seulement si une URL est configurée.

Pour publier une nouvelle banque : modifier le contenu → incrémenter `BUNDLED_VERSION` → lancer `contentcheck` → déposer `bank/bank.json` et `bank/manifest.json` sur un serveur ou un CDN → l’application la récupère au lancement suivant, sans nouvelle version.

Les anciennes versions de l’application ne cassent pas face à une banque plus récente. `minAppVersion` dans `manifest.json` bloque les clients trop anciens, les champs inconnus sont ignorés et une question mal formée est sautée plutôt que fatale.

## Changements récents

**v0.20.18 : nouveau diorama de l’Everest**

- Le modèle « Climbing Grand Slam » a été refait pour ses détails : les drapeaux de prière passent à 0,42 × 0,26 mm (invisibles sur téléphone auparavant), leurs cordes reposent sur deux tas de pierres et chaque drapeau est vérifié par rapport à la neige, et les petites pièces échantillonnent directement le maillage du terrain au lieu de lancer des rayons sur toute la scène
- L’icône vient du nouveau rendu hero ; pose et cadrage inchangés

**v0.20.17 : plus de rotation accidentelle**

- Lever un doigt après un pincement était lu comme un glissement à un doigt, avec un point de référence à une main de distance : le badge sautait et ce saut partait dans l’élan. Le geste est maintenant une machine à états : dès qu’un geste à deux doigts commence, le doigt restant est ignoré, et un pincement ne laisse aucun élan
- Rien ne s’anime tout seul quand vous êtes zoomé, et le retour à la face avant se fait en douceur au lieu de sauter

**v0.20.16 : zoom sur les détails**

- Le pincement plafonnait à 1,25×, d’où des noms de sommets et des altitudes illisibles sur téléphone. Il va maintenant jusqu’à 5,5×
- Deux doigts à distance constante : le glissement déplace la vue jusqu’aux coins ; un seul doigt fait toujours pivoter. Un double-tap réinitialise zoom et déplacement, et une courte indication des gestes figure sous le titre du badge

**v0.20.15 : les quarante badges sont en 3D**

- Les six derniers arrivent : Eight K Grad (toque), Final Exam (médaille de sommet avec drapeau), Five Zones (plaque semelle de course), Fourteen Peaks (panorama 2,5:1 des quatorze 8 000), Two Tracks (plateau à deux rails) et Full Route (un bloc de terrain de l’Everest, la seule pièce qui n’est pas une médaille)
- L’icône de chaque badge est désormais le rendu du modèle lui-même : les illustrations dessinées quittent le mur
- Full Route demande sa propre pose : il repose sur son socle, donc on saute la rotation de 90° des médailles plates et on tourne la plaque vers l’objectif ; son rendu est sur gris et non sur noir, le détourage se fait donc contre ce gris
- Modèles 34 → 40 ; paquet 39,4 → 40,6 Mo

**v0.20.14 : les badges s’ouvrent partout**

- Plus besoin de passer par la zone de test développeur : touchez n’importe quel badge débloqué dans « Mon profil » ou sur le mur des badges, il s’ouvre en plein écran. La vue se superpose à l’app au lieu de remplacer la page : au retour vous retrouvez l’écran quitté, position de liste comprise, et la barre du bas est masquée
- « Trail Finish » affichait encore l’icône de remplacement partagée alors que son modèle était livré ; le mappage pointe désormais sur l’icône rendue
- « Two Tracks » avait pour icône la photo d’une personne, un marqueur oublié. Elle utilise désormais une plaque dessinée (émail bleu marine, bord laiton, deux tracés pointillés vers le sommet), et la photo a disparu du paquet

**v0.20.13 : six modèles de badge de plus**

- La bibliothèque passe de 28 à 34 : 6 000 m d’apprentissage, le retour, les traces de sept jours et de trois semaines, vingt courses et un mousqueton en D pour « la sécurité d’abord » ; self_arrest réexporté
- Le détourage des icônes retire désormais aussi le fond *encerclé* par la forme du badge — le trou central du mousqueton et les perforations d’une carte poinçonnée sortaient en disques noirs pleins. Chaque zone fermée est jugée séparément : une couleur uniforme identique au fond compte comme un trou, les dégradés sont conservés. Paquet 38.5 → 39.4 Mo

**v0.20.11 : éclairage et fond de la vue 3D des badges**

- La vue du badge n'avait que deux lumières : le métal ne recevait qu'un seul reflet courbe et le relief restait plat. Elle en a maintenant cinq — clé, zénith, remplissage frontal, contre-jour et un léger rebond par le bas — à éclairage total constant : plus de relief, sans délavage
- Un second fond plus sombre (un coucher de soleil synthwave flouté jusqu'à n'être plus que des aplats) accompagne le mode sombre. La pastille en haut à droite bascule clair / sombre, suit le réglage système par défaut et retient le choix ; seul cet écran est concerné

**v0.20.8 – v0.20.10 : écran de lancement**

- L'écran de lancement passe à l'argile : un disque blanc au modelé resserré et aux bords nets, la marque du sommet, et à la place de l'ancien slogan une citation tirée au hasard chez un alpiniste — Mallory, Whymper, Muir, Boukreev, Viesturs, Tabei, Messner, Lowe ou Bonatti. Il s'efface au bout d'environ 1,7 s, et passe presque inaperçu si les animations sont réduites. Le reste de l'app ne change pas Depuis la v0.20.9, le splash système n'affiche plus aucun logo : le thème de lancement n'est que la même couleur de fond, si bien que l'écran d'argile démarre sur un fond blanc (ou presque noir). Chaque citation est donnée dans sa langue d'origine

**v0.20.6 – v0.20.7 : qualité de rendu des badges**

- Le bloom tourne par défaut en qualité maximale — 256 px, quatre niveaux, MEDIUM. L’ancien déclassement automatique jugeait dès la première image : quelques centaines de millisecondes de compilation de shaders suffisaient à l’éteindre pour toute la session. Il attend désormais 2,5 s, tolère des images à 50 ms (20 fps) sur trois fenêtres consécutives et descend en deux paliers : complet → réduit → coupé
- Le MSAA 4x est actif là où le pilote le permet, la capacité étant vérifiée avant la création de la swap chain ; en cas d’échec, le rendu repasse sans MSAA et l’échec est mémorisé — un réglage d’anticrénelage ne doit jamais empêcher un badge de s’afficher

**v0.20.4 – v0.20.5 : petites corrections**

- Le mode développeur est invisible tant qu’il n’est pas débloqué : toute la section des réglages, y compris « Désactivé · touchez 7 fois le numéro de version », n’est plus dessinée pour l’utilisateur ordinaire. Le compteur de sept appuis fonctionne toujours, mais ne compte plus à voix haute
- La position de défilement survit aussi dans la zone de test des succès : entrer dans la vue 3D d’un badge et revenir, passer des réglages à la zone de test et revenir
- La fenêtre de déblocage ne se ferme plus sur un appui parasite : le premier appui sur le fond ne fait que l’armer et affiche « Touchez à nouveau pour fermer » ; le second ferme. « Accepter » et le geste de retour ferment toujours en une étape

**v0.20.0 – v0.20.3 : badges 3D**

- 34 succès sont rendus en 3D temps réel (Filament 1.75, glTF, métal et émail PBR). Glisser pour pivoter, pincer pour zoomer, une légère oscillation au repos ; le mouvement automatique reste sur la face avant, car le revers d’une médaille est une plaque lisse
- Depuis la v0.20.15, les quarante ont un modèle ; la carte inclinée reste le repli des futurs badges
- Les fenêtres de déblocage et d’aperçu passent par le même moteur, avec un socle clair derrière le modèle (un modèle sombre sur une carte sombre disparaît) et 56 rubans depuis les deux bords. « Réduire les animations » coupe les rubans
- La page de présentation repose sur un paysage très flou, généré par code (27 Ko) : ciel, quatre crêtes, brume, aucune licence photo à gérer
- Sens de glissement inversés après retour ; la scène éclaircie puis légèrement reprise pour que les parties émissives gardent leur contraste
- Les lumières créées dans les modèles survivent enfin : les entités `KHR_lights_punctual` sont ajoutées explicitement à la scène (les entités d’un asset glTF ne les incluent pas) et leur intensité est mise à l’échelle de l’exposition — 54 cd à côté d’une lumière principale à 66 000 lux sont invisibles sinon. `KHR_materials_emissive_strength` est cuit dans `emissiveFactor` à la compilation, et le bloom reste actif, désactivé quand le temps de frame dépasse 28 ms
- La bibliothèque de modèles est passée de 17 à 34 : séries, sans-faute, révision, retrait, fin de randonnée et trois réexports ; paquet 21 → 39 Mo
- **Les icônes de badge viennent maintenant des modèles** : le rendu de face est détouré de son fond noir en icône transparente de 256 px, si bien que le mur, la fenêtre de déblocage et la vue 3D montrent le même objet. Les illustrations générées par IA de ces badges ont été abandonnées
- Gain du spot simplifié à 5× (le spot du modèle vise l’intérieur de la médaille, aucun gain ne le rend visible de face — cela se corrige dans la source Blender), bloom ramené à 0,05
- v0.20.3 : la position d’une liste survit au passage par un écran secondaire (réglages → langue, manuel → article, profil → réglages, révision → leçon), et revenir d’une leçon ne remet plus le pagineur du parcours à la première page

**v0.19.25 – v0.19.30 : travail sur l’application**

- Succès réalignés sur la banque de 89 leçons. `full_route` signifie désormais les 68 leçons de haute montagne — il disait 66 et jugeait selon le parcours ouvert, si bien que finir la randonnée seule donnait le « grand chelem théorique ». `five_zones` devient « six zones », en accord avec les six unités de haute montagne. Cinq succès de randonnée ajoutés, 35 → **40**
- La progression des succès est calculée sur les deux parcours et non sur celui qui est ouvert : changer de parcours ne déplace plus l’objectif
- Icônes de parcours redessinées en silhouettes pleines — sommet enneigé et chaussure de montagne — à la place des caractères `▲` / `≡` de la feuille ; les pastilles du haut portent les mêmes en 14 dp
- Logo de démarrage réduit de 40 %
- Écran de retour dans le profil, qui poste vers Formspree avec repli par e-mail et presse-papiers ; le gestionnaire de plantage garde les cinq dernières exceptions non interceptées dans le stockage privé de l’application
- Profil et accueil comptaient le total des leçons terminées face au nombre de leçons du parcours courant, affichant « 68 / 21 » après un changement ; corrigé

La v0.19 est l’extension de contenu ; la structure de la banque et la logique de notation ne changent pas.

- Leçons 47 → 89 : haute montagne 68 (unités 6000 / 7000 / 8000 m, les quatorze 8 000), plus 21 leçons de randonnée
- Questions 711 → 1 533 (9 422 clés), manuel 31 → 35 articles (ensevelissement et réanimation sous avalanche, le trio avalanche, cordes fixes, franchissement de rivière), banque v15 → v19
- **Neuf langues complètes sur les quatre couches de contenu** : anglais, chinois traditionnel, japonais, russe, allemand, espagnol, français, suédois et italien. Leurs clés correspondent à l’anglais (10 554 / 10 554), rien ne retombe à l’exécution
- L’allemand emploie la forme impersonnelle avec man, l’espagnol tutoie, le français vouvoie, le suédois et l’italien tutoient ; chacun suit ses conventions numériques — allemand, espagnol et italien écrivent 1.000 m, français et suédois 5 000 m, tous avec la virgule décimale
- Les clés orphelines d’anciennes modifications (`q.l2#5.c`, `m15#17.c`, `m16#18.c`, `m16#22.c`, `m16#24.c`) ont été retirées des neuf langues, laissant chaque jeu propre et identique
- Le chinois traditionnel passe par OpenCC s2twp plus une table terminologique, validé par régression contre les traductions existantes (zéro écart) ; le japonais est une vraie traduction, avec un registre pour la banque et un autre pour le manuel
- `tools/i18n/` propose trois commandes (`status.py` / `prep.py` / `apply.py`) pour les neuf langues ; le chinois traditionnel a en plus `tw.py` et `tw_entry.py`
- La banque est répartie en fichiers : un seul fichier Kotlin d’environ 2 500 entrées fait tomber le compilateur, donc chacune des neuf langues traduites est découpée en trois (`Pack*Quiz.kt` / `Quiz2` / `Quiz3`)

De la v0.16 à la v0.18, tout était du travail multilingue ; la structure de la banque et la logique de notation n’ont pas bougé.

- Textes d’interface répartis en 10 jeux `res/values*` ; la langue se fixe à la main ou suit le système, avec effet immédiat
- Nouvelle couche de contenu `data/i18n/`, quatre paquets par langue : entrée, points clés, corps du manuel, banque de questions. 4 727 éléments par langue
- Les quatre couches complètes dans les neuf langues traduites (zh-TW, en, ja, de, fr, es, it, ru, sv) ; les huit autres que l’anglais vérifiées clé par clé contre la source, zéro manquant
- Terminologie alignée sur l’usage de chaque langue ; doses, niveaux d’avalanche, pentes et températures laissés tels quels
- Ajout de `tools/i18ncheck.py` pour voir la couverture des quatre couches en une commande — fini le « traduit, mais une leçon manque »
- Quelques textes corrigés au passage : une virgule avant tiret (140 en suédois, 5 en allemand et en espagnol), deux réponses à trous avec un guillemet en trop et trois décimales françaises

Plus tôt :

- v0.15.1 : nettoyage des textes dans l’application, suppression des marqueurs `**` littéraux, réécriture d’une série d’explications, la page de connexion ne promet plus une synchronisation non implémentée
- v0.15.0 : neuf articles de manuel issus de livres chinois (h23–h31), nouvelle catégorie « leadership en extérieur », 22 → 31 articles
- v0.14.0 : six articles de guide SEVEN SUMMIT TREKS
- v0.13.x : réglages, page de profil, mur de badges ; geste de retour sur les écrans secondaires

## Licence

PolyForm Noncommercial License 1.0.0 — usage non commercial uniquement. L’usage commercial demande une autorisation écrite séparée ; voir `LICENSE` et `NOTICE`.
