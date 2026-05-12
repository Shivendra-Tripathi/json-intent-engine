package io.github.shivendra.tripathi.scanningengine;

import io.github.shivendra.tripathi.wrapper.ScannedClass;
import io.github.shivendra.tripathi.wrapper.ScannedField;
import io.github.shivendra.tripathi.wrapper.ScannedMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of a completed scan produced by {@link ScannerEngine#scan()}.
 *
 * <p>Results are partitioned by {@link RuleKey}. Every registration rule that was
 * added to the engine gets its own bucket of matched elements.
 */
public final class ScanResult {

    private final Map<RuleKey, List<ScannedClass>>  classResults;
    private final Map<RuleKey, List<ScannedMethod>> methodResults;
    private final Map<RuleKey, List<ScannedField>>  fieldResults;

    /** Package-private — constructed only by {@link ScannerEngine}. */
    ScanResult(Map<RuleKey, List<ScannedClass>>  classResults,
               Map<RuleKey, List<ScannedMethod>> methodResults,
               Map<RuleKey, List<ScannedField>>  fieldResults) {
        this.classResults  = Collections.unmodifiableMap(classResults);
        this.methodResults = Collections.unmodifiableMap(methodResults);
        this.fieldResults  = Collections.unmodifiableMap(fieldResults);
    }

    // -----------------------------------------------------------------------
    // Class results
    // -----------------------------------------------------------------------

    /**
     * Returns all matched classes for the given key, or an empty list if no
     * such rule was registered / no matches were found.
     */
    public List<ScannedClass> getClasses(RuleKey key) {
        return classResults.getOrDefault(key, List.of());
    }

    /** Convenience overload that builds the key inline. */
    public List<ScannedClass> getClasses(AnnotationSet elementAnnotations) {
        return getClasses(RuleKey.forClass(elementAnnotations));
    }

    // -----------------------------------------------------------------------
    // Method results
    // -----------------------------------------------------------------------

    /**
     * Returns all matched methods for the given key, or an empty list if no
     * such rule was registered / no matches were found.
     */
    public List<ScannedMethod> getMethods(RuleKey key) {
        return methodResults.getOrDefault(key, List.of());
    }

    /** Convenience: builds a key from method + class annotation sets. */
    public List<ScannedMethod> getMethods(AnnotationSet methodAnnotations,
                                          AnnotationSet classAnnotations) {
        return getMethods(RuleKey.forMethod(methodAnnotations, classAnnotations));
    }

    /** Convenience: builds a key for methods with no class filter. */
    public List<ScannedMethod> getMethods(AnnotationSet methodAnnotations) {
        return getMethods(RuleKey.forMethod(methodAnnotations));
    }

    // -----------------------------------------------------------------------
    // Field results
    // -----------------------------------------------------------------------

    /**
     * Returns all matched fields for the given key, or an empty list if no
     * such rule was registered / no matches were found.
     */
    public List<ScannedField> getFields(RuleKey key) {
        return fieldResults.getOrDefault(key, List.of());
    }

    /** Convenience: builds a key from field + class annotation sets. */
    public List<ScannedField> getFields(AnnotationSet fieldAnnotations,
                                        AnnotationSet classAnnotations) {
        return getFields(RuleKey.forField(fieldAnnotations, classAnnotations));
    }

    /** Convenience: builds a key for fields with no class filter. */
    public List<ScannedField> getFields(AnnotationSet fieldAnnotations) {
        return getFields(RuleKey.forField(fieldAnnotations));
    }

    // -----------------------------------------------------------------------
    // Diagnostics
    // -----------------------------------------------------------------------

    /** All registered class-rule keys. */
    public Set<RuleKey> classRuleKeys()  { return classResults.keySet(); }

    /** All registered method-rule keys. */
    public Set<RuleKey> methodRuleKeys() { return methodResults.keySet(); }

    /** All registered field-rule keys. */
    public Set<RuleKey> fieldRuleKeys()  { return fieldResults.keySet(); }

    /** Total number of matched elements across all rules. */
    public int totalMatchCount() {
        int n = 0;
        for (var v : classResults.values())  n += v.size();
        for (var v : methodResults.values()) n += v.size();
        for (var v : fieldResults.values())  n += v.size();
        return n;
    }

    @Override
    public String toString() {
        return "ScanResult[classes=" + classResults.values().stream().mapToInt(List::size).sum() +
               ", methods=" + methodResults.values().stream().mapToInt(List::size).sum() +
               ", fields="  + fieldResults.values().stream().mapToInt(List::size).sum() + "]";
    }
}
