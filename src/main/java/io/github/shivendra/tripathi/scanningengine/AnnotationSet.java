package io.github.shivendra.tripathi.scanningengine;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An immutable, order-preserving set of annotation class names used as a
 * registration key and match criterion inside {@link ScannerEngine}.
 *
 * <p>An {@code AnnotationSet} may optionally be set to <em>match-any</em>
 * (OR semantics) or <em>match-all</em> (AND semantics, the default).
 *
 * <pre>{@code
 * // Matches elements that have BOTH @Foo AND @Bar
 * AnnotationSet.allOf(Foo.class, Bar.class)
 *
 * // Matches elements that have @Foo OR @Bar (or both)
 * AnnotationSet.anyOf(Foo.class, Bar.class)
 * }</pre>
 */
public final class AnnotationSet {

    public enum MatchMode { ALL, ANY }

    private final Set<String> annotationClassNames;
    private final MatchMode   matchMode;

    // -----------------------------------------------------------------------
    // Factories
    // -----------------------------------------------------------------------

    /** Creates an AND-mode set from class literals. */
    @SafeVarargs
    public static AnnotationSet allOf(Class<? extends Annotation>... annotations) {
        return new AnnotationSet(namesOf(annotations), MatchMode.ALL);
    }

    /** Creates an OR-mode set from class literals. */
    @SafeVarargs
    public static AnnotationSet anyOf(Class<? extends Annotation>... annotations) {
        return new AnnotationSet(namesOf(annotations), MatchMode.ANY);
    }

    /** Creates an AND-mode set from binary class name strings. */
    public static AnnotationSet allOfNames(String... names) {
        return new AnnotationSet(Set.of(names), MatchMode.ALL);
    }

    /** Creates an OR-mode set from binary class name strings. */
    public static AnnotationSet anyOfNames(String... names) {
        return new AnnotationSet(Set.of(names), MatchMode.ANY);
    }

    /** Creates an AND-mode set from a collection of binary class names. */
    public static AnnotationSet allOfNames(Collection<String> names) {
        return new AnnotationSet(new LinkedHashSet<>(names), MatchMode.ALL);
    }

    /** Creates an OR-mode set from a collection of binary class names. */
    public static AnnotationSet anyOfNames(Collection<String> names) {
        return new AnnotationSet(new LinkedHashSet<>(names), MatchMode.ANY);
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    private AnnotationSet(Set<String> names, MatchMode matchMode) {
        this.annotationClassNames = Collections.unmodifiableSet(new LinkedHashSet<>(names));
        this.matchMode            = Objects.requireNonNull(matchMode);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public Set<String> getAnnotationClassNames() { return annotationClassNames; }
    public MatchMode   getMatchMode()             { return matchMode; }
    public boolean     isEmpty()                  { return annotationClassNames.isEmpty(); }
    public int         size()                     { return annotationClassNames.size(); }

    // -----------------------------------------------------------------------
    // Matching
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the given set of annotation class names satisfies
     * this {@code AnnotationSet}'s match criterion.
     *
     * @param presentAnnotations the annotation class names present on the element
     */
    public boolean matches(Set<String> presentAnnotations) {
        if (annotationClassNames.isEmpty()) return true; // empty set = wildcard
        return switch (matchMode) {
            case ALL -> presentAnnotations.containsAll(annotationClassNames);
            case ANY -> annotationClassNames.stream().anyMatch(presentAnnotations::contains);
        };
    }

    // -----------------------------------------------------------------------
    // Equality / hashing (used as map key)
    // -----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotationSet that)) return false;
        return matchMode == that.matchMode &&
               annotationClassNames.equals(that.annotationClassNames);
    }

    @Override
    public int hashCode() { return Objects.hash(annotationClassNames, matchMode); }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @SafeVarargs
    private static Set<String> namesOf(Class<? extends Annotation>... classes) {
        return Arrays.stream(classes)
                .map(Class::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return matchMode + "(" + annotationClassNames + ")";
    }
}
