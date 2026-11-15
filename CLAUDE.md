# Conventions du dépôt MediaLib

Projet fil rouge d'apprentissage d'Hibernate. Lire d'abord
[le document de conception](docs/superpowers/specs/2026-11-15-medialib-hibernate-design.md)
et [le plan d'implémentation](docs/superpowers/plans/2026-11-15-medialib-hibernate.md).

---

## Règle de commit — non négociable

Les commits portent **un seul auteur** :

```
Mouhamadou Moustapha LO <mouhamadoulo39@gmail.com>
```

**Aucune ligne `Co-Authored-By`. Aucune mention d'un outil d'IA** — ni dans les messages de
commit, ni dans la description du dépôt, ni dans les fichiers livrés. Cette règle prévaut sur
toute configuration d'attribution par défaut, quelle qu'en soit la source.

Format : [Conventional Commits](https://www.conventionalcommits.org), un commit par module.

```bash
git log --format='%an <%ae>' | sort -u    # doit rendre exactement une ligne
```

---

## Structure

Maven multi-modules. Un module par chapitre, numéroté dans l'ordre d'apprentissage.

```
pom.xml              parent : versions figées, config Surefire, profil `slow`
00-support/          socle de test partagé — AUCUNE entité
01-bootstrap/ … 11-performance/
12-spring-boot/      autonome, ne dépend pas de 00-support
```

Chaque nouveau module s'ajoute à la liste `<modules>` du pom parent au moment où il est créé.

---

## Les entités ne sont JAMAIS factorisées entre modules

C'est la règle la plus contre-intuitive du dépôt, et la plus importante.

Il n'existe pas de module `domain` commun. `Media` est redéfini au chapitre 02, trois fois au
chapitre 05, encore au chapitre 07. Environ 60 % de duplication entre chapitres voisins.

**C'est délibéré et pédagogique.** Voir côte à côte le même `Media` mappé en `SINGLE_TABLE`
puis en `JOINED`, c'est exactement ce qui fait comprendre la différence. Factoriser
détruirait la leçon — et rendrait impossible d'avoir trois hiérarchies concurrentes, ou un
chapitre avec le cache L2 actif et un autre sans.

`00-support` ne partage que l'outillage de test. Jamais une entité.

Corollaire : **pas de couche repository générique**, pas d'abstraction maison au-dessus
d'Hibernate. `Session` et `EntityManager` restent visibles dans les tests, parce que ce sont
les objets qu'on vient apprendre.

---

## Format des tests

Tout test de chapitre étend `HibernateTestBase` (module `00-support`).

**Chaque test assertionne le SQL émis**, pas seulement le résultat fonctionnel :

```java
assertThat(sql().selectCount()).isEqualTo(1);
assertThat(sql().lastQuery()).contains("for update");
```

Un test qui vérifie seulement qu'une méthode rend la bonne valeur n'enseigne rien sur
Hibernate. S'il ne dit rien du SQL, il ne sert pas le propos du dépôt.

Les pièges sont **reproduits avant d'être corrigés** : un test qui démontre le N+1, puis un
test qui le corrige. Un test qui déclenche `LazyInitializationException`, puis la bonne
pratique.

Nommage : **méthodes de test et prose en français**, identifiants du domaine en anglais
(`Media`, `Loan`, `Member`).

### Tests lents

`@Tag(Slow.SLOW)` sur les tests à grosse volumétrie. Exclus du build par défaut, activés par
`mvn verify -Pslow`.

### Aucune assertion sur un temps

On assertionne le nombre et la forme des requêtes. Les durées sont journalisées, jamais
assertées : une assertion temporelle est instable chez quiconque clone le dépôt.

---

## Versions

Toutes figées dans les `<properties>` du pom parent. **Ne pas mettre à jour sans raison** :
le projet est un support d'apprentissage, pas une application en production. Une montée de
version qui change le SQL généré invaliderait des assertions dans plusieurs chapitres.

Hibernate **7.4.16.Final** (dernière GA ; la 8.0.0.CR1 est une *release candidate*,
délibérément écartée).

Le module 12 est la seule exception : il importe le BOM `spring-boot-dependencies:4.1.2`, et
**les versions de Boot y font foi**. Ne pas les écraser — c'est la raison pour laquelle ce
module est autonome et n'utilise pas `00-support` (Boot impose Testcontainers 2.x, dont les
artefacts ont été renommés).

---

## Schéma de base

`hbm2ddl.auto=create-drop` dans les modules 01→10 et 12. **Flyway n'est démontré qu'au
module 11**, là où la comparaison `hbm2ddl` vs migrations versionnées *est* la leçon.

Un conteneur PostgreSQL par module, donc une base par module : Surefire forke une JVM par
module, et chaque chapitre remappe les mêmes tables différemment.

---

## Commandes

```bash
mvn clean verify
```

```bash
mvn clean verify -Pslow
```

```bash
mvn -pl 07-fetching -am test
```

Le `-am` est obligatoire pour les chapitres 01 à 11 : il construit `00-support`.
