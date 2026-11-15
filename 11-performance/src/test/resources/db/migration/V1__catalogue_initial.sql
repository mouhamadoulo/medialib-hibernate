-- Premiere version du schema du catalogue.
--
-- Ecrit a la main, versionne, relu en revue de code : c'est toute la difference avec
-- hbm2ddl.auto. Ce fichier ne changera JAMAIS une fois livre — Flyway en garde l'empreinte.

create sequence media_seq start with 1 increment by 50;

create table media (
    id         bigint       not null,
    title      varchar(255),
    loan_count integer,
    primary key (id)
);

create table tag (
    id    bigserial    not null,
    label varchar(255),
    primary key (id)
);
