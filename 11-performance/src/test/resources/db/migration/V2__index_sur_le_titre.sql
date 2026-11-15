-- Deuxieme version : un index sur le titre.
--
-- Exactement le genre de changement qu'hbm2ddl.auto ne sait pas faire et qu'un ORM n'a pas a
-- decider. Le mapping Hibernate est identique avant et apres cette migration.

create index idx_media_title on media (title);
