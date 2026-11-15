package com.medialib.support;

/**
 * Tag JUnit des tests a grosse volumetrie.
 *
 * <p>Usage : {@code @Tag(Slow.SLOW)}. Ces tests sont exclus du build par defaut
 * (configuration Surefire du pom parent) et reactives par {@code mvn verify -Pslow}.
 */
public final class Slow {

    public static final String SLOW = "slow";

    private Slow() {
    }
}
