package io.github.shivendra.tripathi.scanningengine;

import io.github.shivendra.tripathi.wrapper.ScannedClass;
import io.github.shivendra.tripathi.wrapper.ScannedClassRegistry;
import io.github.shivendra.tripathi.wrapper.ScannedField;
import io.github.shivendra.tripathi.wrapper.ScannedMethod;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fluent query engine built on top of the {@code scanned.*} wrappers.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ScannerEngine engine = new ScannerEngine("com.example")
 *
 *     // Classes annotated with @Handler
 *     .registerClassRule(AnnotationSet.allOf(Handler.class))
 *
 *     // Methods annotated with @Route, inside classes annotated with @Handler
 *     .registerMethodRule(
 *             AnnotationSet.allOf(Route.class),
 *             AnnotationSet.allOf(Handler.class))
 *
 *     // Fields annotated with @Inject (in any class)
 *     .registerFieldRule(AnnotationSet.allOf(Inject.class))
 *
 *     .scan();
 *
 * // Retrieve results
 * List<ScannedClass>  handlers = engine.getResultsAsClasses(AnnotationSet.allOf(Handler.class));
 * List<ScannedMethod> routes   = engine.getResultsAsMethods(
 *                                         AnnotationSet.allOf(Route.class),
 *                                         AnnotationSet.allOf(Handler.class));
 * List<ScannedField>  injected = engine.getResultsAsFields(AnnotationSet.allOf(Inject.class));
 * }</pre>
 *
 * <h2>Key design points</h2>
 * <ul>
 *   <li>Registrations are stored as {@link ScanRule}s before the scan runs.</li>
 *   <li>{@link #scan()} runs ClassGraph once, converts everything to {@link ScannedClass}
 *       objects via {@link ScannedClassRegistry}, then evaluates all rules in a single pass.</li>
 *   <li>Results are immutably stored inside a {@link ScanResult} and also accessible via
 *       the fluent {@code getResultsAs*()} methods on this class.</li>
 *   <li>Calling {@link #scan()} multiple times re-scans from scratch; the previous
 *       {@link ScanResult} is replaced.</li>
 * </ul>
 */
public final class ScannerEngine {

    // Packages to scan (empty = whole classpath)
    private final String[] packages;

    // Ordered registrations per kind
    private final List<ScanRule> classRules  = new ArrayList<>();
    private final List<ScanRule> methodRules = new ArrayList<>();
    private final List<ScanRule> fieldRules  = new ArrayList<>();

    // Set after scan()
    private ScanResult lastScanResult;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** Scans the given package prefixes. */
    public ScannerEngine(String... packages) {
        this.packages = packages.clone();
    }

    /** Scans the entire classpath. */
    public ScannerEngine() {
        this.packages = new String[0];
    }

    // -----------------------------------------------------------------------
    // Registration — class rules
    // -----------------------------------------------------------------------

    /**
     * Registers a rule that collects classes carrying all annotations in
     * {@code elementAnnotations}.
     */
    public ScannerEngine registerClassRule(AnnotationSet elementAnnotations) {
        classRules.add(ScanRule.forClass(elementAnnotations));
        return this;
    }

    /** Shorthand — single annotation, AND mode. */
    @SafeVarargs
    public final ScannerEngine registerClassRule(
            Class<? extends Annotation>... annotations) {
        return registerClassRule(AnnotationSet.allOf(annotations));
    }

    // -----------------------------------------------------------------------
    // Registration — method rules
    // -----------------------------------------------------------------------

    /**
     * Registers a rule that collects methods carrying {@code methodAnnotations}
     * inside classes carrying {@code classAnnotations}.
     *
     * @param classAnnotations pass {@code null} to match methods in any class
     */
    public ScannerEngine registerMethodRule(AnnotationSet methodAnnotations,
                                            AnnotationSet classAnnotations) {
        methodRules.add(ScanRule.forMethod(methodAnnotations, classAnnotations));
        return this;
    }

    /** Collects methods carrying {@code methodAnnotations} regardless of declaring class. */
    public ScannerEngine registerMethodRule(AnnotationSet methodAnnotations) {
        return registerMethodRule(methodAnnotations, null);
    }

    /** Shorthand — method annotation only. */
    @SafeVarargs
    public final ScannerEngine registerMethodRule(
            Class<? extends Annotation>... methodAnnotations) {
        return registerMethodRule(AnnotationSet.allOf(methodAnnotations), null);
    }

    // -----------------------------------------------------------------------
    // Registration — field rules
    // -----------------------------------------------------------------------

    /**
     * Registers a rule that collects fields carrying {@code fieldAnnotations}
     * inside classes carrying {@code classAnnotations}.
     *
     * @param classAnnotations pass {@code null} to match fields in any class
     */
    public ScannerEngine registerFieldRule(AnnotationSet fieldAnnotations,
                                           AnnotationSet classAnnotations) {
        fieldRules.add(ScanRule.forField(fieldAnnotations, classAnnotations));
        return this;
    }

    /** Collects fields carrying {@code fieldAnnotations} regardless of declaring class. */
    public ScannerEngine registerFieldRule(AnnotationSet fieldAnnotations) {
        return registerFieldRule(fieldAnnotations, null);
    }

    /** Shorthand — field annotation only. */
    @SafeVarargs
    public final ScannerEngine registerFieldRule(
            Class<? extends Annotation>... fieldAnnotations) {
        return registerFieldRule(AnnotationSet.allOf(fieldAnnotations), null);
    }

    // -----------------------------------------------------------------------
    // Scan
    // -----------------------------------------------------------------------

    /**
     * Runs the classpath scan and evaluates all registered rules.
     *
     * <p>The method is idempotent in the sense that calling it again re-scans
     * the classpath from scratch and replaces the stored result.
     *
     * @return {@code this} for fluent chaining
     */
    public ScannerEngine scan() {
        // ── 1. Scan classpath ────────────────────────────────────────────────
        ScannedClassRegistry registry = ScannedClassRegistry.scan(packages);

        // ── 2. Evaluate rules ────────────────────────────────────────────────
        Map<RuleKey, List<ScannedClass>>  classMatches  = new LinkedHashMap<>();
        Map<RuleKey, List<ScannedMethod>> methodMatches = new LinkedHashMap<>();
        Map<RuleKey, List<ScannedField>>  fieldMatches  = new LinkedHashMap<>();

        // Pre-initialise empty buckets for every registered rule
        for (ScanRule r : classRules)  classMatches.put(r.toKey(),  new ArrayList<>());
        for (ScanRule r : methodRules) methodMatches.put(r.toKey(), new ArrayList<>());
        for (ScanRule r : fieldRules)  fieldMatches.put(r.toKey(),  new ArrayList<>());

        // ── 3. Single pass over all scanned classes ──────────────────────────
        for (ScannedClass sc : registry.getAllClasses()) {

            Set<String> classAnnNames = sc.getAnnotationClassNames()
                                          .stream()
                                          .collect(Collectors.toCollection(LinkedHashSet::new));

            // --- Class rules -------------------------------------------------
            for (ScanRule rule : classRules) {
                if (rule.getElementAnnotations().matches(classAnnNames)) {
                    classMatches.get(rule.toKey()).add(sc);
                }
            }

            // --- Method rules ------------------------------------------------
            if (!methodRules.isEmpty()) {
                for (ScannedMethod method : sc.getDeclaredMethods()) {
                    Set<String> methodAnnNames = annNamesOf(method);
                    for (ScanRule rule : methodRules) {
                        if (!rule.getElementAnnotations().matches(methodAnnNames)) continue;
                        if (rule.hasDeclaringClassFilter() &&
                            !rule.getDeclaringClassAnnotations().matches(classAnnNames)) continue;
                        methodMatches.get(rule.toKey()).add(method);
                    }
                }
            }

            // --- Field rules -------------------------------------------------
            if (!fieldRules.isEmpty()) {
                for (ScannedField field : sc.getDeclaredFields()) {
                    Set<String> fieldAnnNames = annNamesOf(field);
                    for (ScanRule rule : fieldRules) {
                        if (!rule.getElementAnnotations().matches(fieldAnnNames)) continue;
                        if (rule.hasDeclaringClassFilter() &&
                            !rule.getDeclaringClassAnnotations().matches(classAnnNames)) continue;
                        fieldMatches.get(rule.toKey()).add(field);
                    }
                }
            }
        }

        // ── 4. Freeze and store ──────────────────────────────────────────────
        classMatches.replaceAll((k, v)  -> Collections.unmodifiableList(v));
        methodMatches.replaceAll((k, v) -> Collections.unmodifiableList(v));
        fieldMatches.replaceAll((k, v)  -> Collections.unmodifiableList(v));

        this.lastScanResult = new ScanResult(classMatches, methodMatches, fieldMatches);
        return this;
    }

    // -----------------------------------------------------------------------
    // Result retrieval — fluent (delegates to ScanResult)
    // -----------------------------------------------------------------------

    /**
     * Returns the full {@link ScanResult} from the last {@link #scan()} call.
     *
     * @throws IllegalStateException if {@link #scan()} has not been called yet
     */
    public ScanResult getScanResult() {
        requireScanned();
        return lastScanResult;
    }

    // --- Classes ---

    /** Returns classes matched by the given annotation set. */
    public List<ScannedClass> getResultsAsClasses(AnnotationSet elementAnnotations) {
        requireScanned();
        return lastScanResult.getClasses(elementAnnotations);
    }

    @SafeVarargs
    public final List<ScannedClass> getResultsAsClasses(
            Class<? extends Annotation>... annotations) {
        return getResultsAsClasses(AnnotationSet.allOf(annotations));
    }

    // --- Methods ---

    /**
     * Returns methods matched by {@code methodAnnotations} inside classes matched
     * by {@code classAnnotations}.
     */
    public List<ScannedMethod> getResultsAsMethods(AnnotationSet methodAnnotations,
                                                   AnnotationSet classAnnotations) {
        requireScanned();
        return lastScanResult.getMethods(methodAnnotations, classAnnotations);
    }

    /** Returns methods matched by {@code methodAnnotations} (any declaring class). */
    public List<ScannedMethod> getResultsAsMethods(AnnotationSet methodAnnotations) {
        requireScanned();
        return lastScanResult.getMethods(methodAnnotations);
    }

    // --- Fields ---

    /**
     * Returns fields matched by {@code fieldAnnotations} inside classes matched
     * by {@code classAnnotations}.
     */
    public List<ScannedField> getResultsAsFields(AnnotationSet fieldAnnotations,
                                                 AnnotationSet classAnnotations) {
        requireScanned();
        return lastScanResult.getFields(fieldAnnotations, classAnnotations);
    }

    /** Returns fields matched by {@code fieldAnnotations} (any declaring class). */
    public List<ScannedField> getResultsAsFields(AnnotationSet fieldAnnotations) {
        requireScanned();
        return lastScanResult.getFields(fieldAnnotations);
    }

    // -----------------------------------------------------------------------
    // Diagnostics
    // -----------------------------------------------------------------------

    /** Returns the number of registered class rules. */
    public int classRuleCount()  { return classRules.size(); }

    /** Returns the number of registered method rules. */
    public int methodRuleCount() { return methodRules.size(); }

    /** Returns the number of registered field rules. */
    public int fieldRuleCount()  { return fieldRules.size(); }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void requireScanned() {
        if (lastScanResult == null) {
            throw new IllegalStateException(
                    "scan() has not been called yet. Call scan() before retrieving results.");
        }
    }

    private static Set<String> annNamesOf(io.github.shivendra.tripathi.wrapper.ScannedAnnotatable element) {
        return element.getAnnotationClassNames()
                      .stream()
                      .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
