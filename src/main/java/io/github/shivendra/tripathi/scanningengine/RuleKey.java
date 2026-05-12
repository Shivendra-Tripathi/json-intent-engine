package io.github.shivendra.tripathi.scanningengine;

import java.util.Objects;

/**
 * Immutable key used to look up scan results from {@link ScannerEngine}.
 *
 * <p>A {@code RuleKey} uniquely identifies a registration rule and is the
 * parameter passed to {@code engine.getResultsAsClasses(...)},
 * {@code engine.getResultsAsMethods(...)}, etc.
 *
 * <p>Instances are obtained either directly from {@link ScanRule#toKey()} or
 * via the static convenience factories on this class.
 *
 * <pre>{@code
 * RuleKey key = RuleKey.forMethod(
 *         AnnotationSet.allOf(Route.class),
 *         AnnotationSet.allOf(Handler.class));
 *
 * List<ScannedMethod> methods = engine.getResultsAsMethods(key);
 * }</pre>
 */
public final class RuleKey {

    private final ScanRule.RuleKind kind;
    private final AnnotationSet     elementAnnotations;
    private final AnnotationSet     declaringClassAnnotations; // nullable

    // -----------------------------------------------------------------------
    // Package-private constructor (created by ScanRule#toKey())
    // -----------------------------------------------------------------------

    RuleKey(ScanRule.RuleKind kind,
            AnnotationSet elementAnnotations,
            AnnotationSet declaringClassAnnotations) {
        this.kind                     = Objects.requireNonNull(kind);
        this.elementAnnotations       = Objects.requireNonNull(elementAnnotations);
        this.declaringClassAnnotations = declaringClassAnnotations;
    }

    // -----------------------------------------------------------------------
    // Public convenience factories
    // -----------------------------------------------------------------------

    /** Key for a class rule. */
    public static RuleKey forClass(AnnotationSet elementAnnotations) {
        return ScanRule.forClass(elementAnnotations).toKey();
    }

    /** Key for a method rule. */
    public static RuleKey forMethod(AnnotationSet methodAnnotations,
                                    AnnotationSet classAnnotations) {
        return ScanRule.forMethod(methodAnnotations, classAnnotations).toKey();
    }

    /** Key for a method rule with no class filter. */
    public static RuleKey forMethod(AnnotationSet methodAnnotations) {
        return ScanRule.forMethod(methodAnnotations, null).toKey();
    }

    /** Key for a field rule. */
    public static RuleKey forField(AnnotationSet fieldAnnotations,
                                   AnnotationSet classAnnotations) {
        return ScanRule.forField(fieldAnnotations, classAnnotations).toKey();
    }

    /** Key for a field rule with no class filter. */
    public static RuleKey forField(AnnotationSet fieldAnnotations) {
        return ScanRule.forField(fieldAnnotations, null).toKey();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public ScanRule.RuleKind getKind()                      { return kind; }
    public AnnotationSet     getElementAnnotations()        { return elementAnnotations; }
    public AnnotationSet     getDeclaringClassAnnotations() { return declaringClassAnnotations; }

    // -----------------------------------------------------------------------
    // Equality / hashing — must mirror ScanRule equality
    // -----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleKey that)) return false;
        return kind == that.kind &&
               elementAnnotations.equals(that.elementAnnotations) &&
               Objects.equals(declaringClassAnnotations, that.declaringClassAnnotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, elementAnnotations, declaringClassAnnotations);
    }

    @Override
    public String toString() {
        String s = "RuleKey." + kind + "[" + elementAnnotations + "]";
        if (declaringClassAnnotations != null) s += " in[" + declaringClassAnnotations + "]";
        return s;
    }
}
