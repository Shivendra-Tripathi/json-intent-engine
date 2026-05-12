package io.github.shivendra.tripathi.scanningengine;

import java.util.Objects;

/**
 * Immutable descriptor for a single registration rule inside {@link ScannerEngine}.
 *
 * <p>A rule captures:
 * <ul>
 *   <li>{@link RuleKind} – whether it targets classes, methods, or fields</li>
 *   <li>{@code elementAnnotations}   – annotation filter applied to the element itself</li>
 *   <li>{@code declaringClassAnnotations} – (methods / fields only) additional filter
 *       applied to the element's declaring class; {@code null} means "any class"</li>
 * </ul>
 */
public final class ScanRule {

    public enum RuleKind { CLASS, METHOD, FIELD }

    private final RuleKind      kind;
    private final AnnotationSet elementAnnotations;
    private final AnnotationSet declaringClassAnnotations; // nullable

    // -----------------------------------------------------------------------
    // Factories
    // -----------------------------------------------------------------------

    /** Rule that targets classes carrying {@code elementAnnotations}. */
    public static ScanRule forClass(AnnotationSet elementAnnotations) {
        return new ScanRule(RuleKind.CLASS, elementAnnotations, null);
    }

    /**
     * Rule that targets methods carrying {@code methodAnnotations}
     * inside classes carrying {@code classAnnotations}.
     *
     * @param classAnnotations pass {@code null} to match methods in any class
     */
    public static ScanRule forMethod(AnnotationSet methodAnnotations,
                                     AnnotationSet classAnnotations) {
        return new ScanRule(RuleKind.METHOD, methodAnnotations, classAnnotations);
    }

    /**
     * Rule that targets fields carrying {@code fieldAnnotations}
     * inside classes carrying {@code classAnnotations}.
     *
     * @param classAnnotations pass {@code null} to match fields in any class
     */
    public static ScanRule forField(AnnotationSet fieldAnnotations,
                                    AnnotationSet classAnnotations) {
        return new ScanRule(RuleKind.FIELD, fieldAnnotations, classAnnotations);
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    private ScanRule(RuleKind kind, AnnotationSet elementAnnotations,
                     AnnotationSet declaringClassAnnotations) {
        this.kind                     = Objects.requireNonNull(kind);
        this.elementAnnotations       = Objects.requireNonNull(elementAnnotations);
        this.declaringClassAnnotations = declaringClassAnnotations; // nullable
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public RuleKind      getKind()                      { return kind; }
    public AnnotationSet getElementAnnotations()        { return elementAnnotations; }

    /** May be {@code null} – means "match elements in any class". */
    public AnnotationSet getDeclaringClassAnnotations() { return declaringClassAnnotations; }

    public boolean hasDeclaringClassFilter()            { return declaringClassAnnotations != null; }

    // -----------------------------------------------------------------------
    // Key used to look up results later
    // -----------------------------------------------------------------------

    /**
     * Returns a canonical {@link RuleKey} that consumers use when calling
     * {@code engine.getResultsAs*(...)}.
     */
    public RuleKey toKey() {
        return new RuleKey(kind, elementAnnotations, declaringClassAnnotations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScanRule that)) return false;
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
        String base = kind + "[" + elementAnnotations + "]";
        if (declaringClassAnnotations != null) {
            base += " in class[" + declaringClassAnnotations + "]";
        }
        return base;
    }
}
