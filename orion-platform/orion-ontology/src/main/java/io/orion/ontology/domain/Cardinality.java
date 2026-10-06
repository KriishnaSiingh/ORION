package io.orion.ontology.domain;

/** Declaration order matters: later values are looser. Used to allow widening but never narrowing. */
public enum Cardinality {
    ONE_TO_ONE,      // each source has at most one target and vice versa
    ONE_TO_MANY,     // a source may have many targets; each target has at most one source
    MANY_TO_MANY
}
