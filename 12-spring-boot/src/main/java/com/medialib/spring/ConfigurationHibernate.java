package com.medialib.spring;

import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Comment on branche un reglage Hibernate sous Spring Boot.
 *
 * <p>Aux chapitres precedents, on passait un {@code Map} de reglages a
 * {@code StandardServiceRegistryBuilder}. Ici, Boot construit la {@code SessionFactory} a
 * notre place : le point d'accroche est un {@link HibernatePropertiesCustomizer}.
 *
 * <p>C'est l'illustration exacte du propos du chapitre — rien n'a disparu, mais tout passe
 * desormais par un point d'extension qu'il faut connaitre.
 *
 * <p>Detail qui coute une demi-heure a qui migre : Spring Boot 4 a reorganise ses classes
 * d'auto-configuration en paquets par module. {@code HibernatePropertiesCustomizer} n'est plus
 * dans {@code org.springframework.boot.autoconfigure.orm.jpa} mais dans
 * {@code org.springframework.boot.hibernate.autoconfigure}. De meme, {@code @DataJpaTest} vit
 * desormais dans {@code org.springframework.boot.data.jpa.test.autoconfigure} et
 * {@code @AutoConfigureMockMvc} dans {@code org.springframework.boot.webmvc.test.autoconfigure}.
 */
@Configuration(proxyBeanMethods = false)
public class ConfigurationHibernate {

    @Bean
    public CompteurSql compteurSql() {
        return new CompteurSql();
    }

    @Bean
    public HibernatePropertiesCustomizer brancheLeCompteur(CompteurSql compteur) {
        return (Map<String, Object> proprietes) ->
                proprietes.put(AvailableSettings.STATEMENT_INSPECTOR, compteur);
    }
}
