package io.github.shivendra.tripathi.wrapper;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Deep annotation inspection utilities for {@link ScannedAnnotatable} elements.
 *
 * <p>Provides:
 * <ul>
 *   <li>Transitive / recursive meta-annotation search</li>
 *   <li>Finding annotations (or nested annotations) matching arbitrary predicates</li>
 *   <li>Extracting all parameter values of a given name across annotation hierarchies</li>
 *   <li>Checking whether an element is "stereotype-annotated" (i.e. carries an annotation
 *       that is itself meta-annotated transitively)</li>
 * </ul>
 *
 * <p>All methods are static and stateless.
 */
public final class AnnotationInspector {

    // Standard Java meta-annotation class names – traversal stops here to avoid loops
    private static final Set<String> JAVA_META_ANNOTATION_NAMES = Set.of(
            "java.lang.annotation.Retention",
            "java.lang.annotation.Target",
            "java.lang.annotation.Documented",
            "java.lang.annotation.Inherited",
            "java.lang.annotation.Repeatable",
            "java.lang.annotation.Native"
    );

    private AnnotationInspector() { /* utility class */ }

    // -------------------------------------------------------------------------
    // Transitive meta-annotation presence
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code annotatable} carries (directly or transitively through
     * meta-annotations) an annotation whose class name equals {@code targetAnnotationClassName}.
     *
     * <p>Example: {@code @Service} is meta-annotated with {@code @Component}; this returns
     * {@code true} when called with {@code "@Component"} on an element annotated with
     * {@code @Service}.
     */
    public static boolean hasAnnotationTransitively(ScannedAnnotatable annotatable,
                                                    String targetAnnotationClassName) {
        for (ScannedDeclaredAnnotation ann : annotatable.getDeclaredAnnotations()) {
            if (findTransitively(ann, targetAnnotationClassName, new HashSet<>()) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnnotationTransitively(ScannedAnnotatable annotatable,
                                                    Class<? extends Annotation> targetAnnotation) {
        return hasAnnotationTransitively(annotatable, targetAnnotation.getName());
    }

    // -------------------------------------------------------------------------
    // Transitive annotation retrieval
    // -------------------------------------------------------------------------

    /**
     * Finds and returns the first {@link ScannedDeclaredAnnotation} that matches
     * {@code targetAnnotationClassName}, searching the element's direct annotations
     * and their meta-annotation hierarchies (BFS order).
     */
    public static Optional<ScannedDeclaredAnnotation> findAnnotationTransitively(
            ScannedAnnotatable annotatable, String targetAnnotationClassName) {

        for (ScannedDeclaredAnnotation ann : annotatable.getDeclaredAnnotations()) {
            ScannedDeclaredAnnotation found =
                    findTransitively(ann, targetAnnotationClassName, new HashSet<>());
            if (found != null) return Optional.of(found);
        }
        return Optional.empty();
    }

    public static Optional<ScannedDeclaredAnnotation> findAnnotationTransitively(
            ScannedAnnotatable annotatable, Class<? extends Annotation> targetAnnotation) {
        return findAnnotationTransitively(annotatable, targetAnnotation.getName());
    }

    /**
     * Returns all annotations reachable from the element's direct annotations through
     * the meta-annotation hierarchy (BFS), including the direct annotations themselves.
     * Standard Java meta-annotations are excluded from the result but still act as traversal
     * stop-points to prevent infinite loops.
     */
    public static List<ScannedDeclaredAnnotation> collectAllAnnotationsTransitively(
            ScannedAnnotatable annotatable) {
        List<ScannedDeclaredAnnotation> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<ScannedDeclaredAnnotation> queue = new ArrayDeque<>(annotatable.getDeclaredAnnotations());

        while (!queue.isEmpty()) {
            ScannedDeclaredAnnotation current = queue.poll();
            String name = current.getAnnotationClassName();
            if (!visited.add(name)) continue;
            if (!JAVA_META_ANNOTATION_NAMES.contains(name)) {
                result.add(current);
            }
            for (ScannedDeclaredAnnotation meta : current.getMetaAnnotations()) {
                if (!visited.contains(meta.getAnnotationClassName())) {
                    queue.add(meta);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Parameter value extraction across annotation hierarchy
    // -------------------------------------------------------------------------

    /**
     * Collects all {@link ScannedAnnotationValue}s with the given parameter name from
     * every annotation in the transitive meta-annotation hierarchy of the element.
     *
     * <p>Useful for extracting e.g. all {@code "value"} parameters across a composed
     * annotation stack.
     */
    public static List<ScannedAnnotationValue> collectParameterValues(
            ScannedAnnotatable annotatable, String parameterName) {
        List<ScannedAnnotationValue> result = new ArrayList<>();
        for (ScannedDeclaredAnnotation ann : collectAllAnnotationsTransitively(annotatable)) {
            ann.getParameterValue(parameterName).ifPresent(result::add);
        }
        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Predicate-based search inside nested annotation values
    // -------------------------------------------------------------------------

    /**
     * Searches an annotation's parameter values recursively (including nested annotation
     * values and array elements) and returns all {@link ScannedAnnotationValue}s
     * satisfying the predicate.
     */
    public static List<ScannedAnnotationValue> findValuesRecursively(
            ScannedDeclaredAnnotation root, Predicate<ScannedAnnotationValue> predicate) {
        List<ScannedAnnotationValue> result = new ArrayList<>();
        for (ScannedAnnotationValue pv : root.getParameterValues()) {
            collectMatchingValues(pv, predicate, result, new HashSet<>());
        }
        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Annotation attribute helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the string value of the first occurrence of the given parameter name
     * across the transitive meta-annotation hierarchy of the element.
     */
    public static Optional<String> resolveStringAttribute(
            ScannedAnnotatable annotatable,
            String annotationClassName,
            String parameterName) {
        return findAnnotationTransitively(annotatable, annotationClassName)
                .flatMap(a -> a.getParameterValue(parameterName))
                .filter(v -> v.getKind() == ScannedAnnotationValue.Kind.STRING)
                .map(ScannedAnnotationValue::asString);
    }

    // -------------------------------------------------------------------------
    // Element-type convenience methods
    // -------------------------------------------------------------------------

    /**
     * Returns all fields in the given class that carry an annotation which is
     * transitively annotated with {@code metaAnnotationClassName}.
     */
    public static List<ScannedField> getFieldsWithMetaAnnotation(
            ScannedClass scannedClass, String metaAnnotationClassName) {
        List<ScannedField> result = new ArrayList<>();
        for (ScannedField f : scannedClass.getDeclaredFields()) {
            if (hasAnnotationTransitively(f, metaAnnotationClassName)) {
                result.add(f);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns all methods in the given class that carry an annotation which is
     * transitively annotated with {@code metaAnnotationClassName}.
     */
    public static List<ScannedMethod> getMethodsWithMetaAnnotation(
            ScannedClass scannedClass, String metaAnnotationClassName) {
        List<ScannedMethod> result = new ArrayList<>();
        for (ScannedMethod m : scannedClass.getDeclaredMethods()) {
            if (hasAnnotationTransitively(m, metaAnnotationClassName)) {
                result.add(m);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * BFS search for a specific annotation class name within the meta-annotation
     * hierarchy rooted at {@code annotation}.  Returns the matching
     * {@link ScannedDeclaredAnnotation} or {@code null} if not found.
     */
    private static ScannedDeclaredAnnotation findTransitively(
            ScannedDeclaredAnnotation annotation,
            String targetClassName,
            Set<String> visited) {

        Deque<ScannedDeclaredAnnotation> queue = new ArrayDeque<>();
        queue.add(annotation);

        while (!queue.isEmpty()) {
            ScannedDeclaredAnnotation current = queue.poll();
            String name = current.getAnnotationClassName();

            if (!visited.add(name)) continue;

            if (name.equals(targetClassName)) return current;
            if (JAVA_META_ANNOTATION_NAMES.contains(name)) continue;

            queue.addAll(current.getMetaAnnotations());
        }
        return null;
    }

    private static void collectMatchingValues(
            ScannedAnnotationValue value,
            Predicate<ScannedAnnotationValue> predicate,
            List<ScannedAnnotationValue> result,
            Set<ScannedAnnotationValue> visited) {

        if (!visited.add(value)) return;

        if (predicate.test(value)) {
            result.add(value);
        }

        switch (value.getKind()) {
            case ARRAY -> {
                for (ScannedAnnotationValue elem : value.asArray()) {
                    collectMatchingValues(elem, predicate, result, visited);
                }
            }
            case ANNOTATION -> {
                ScannedDeclaredAnnotation nested = value.asNestedAnnotation();
                for (ScannedAnnotationValue pv : nested.getParameterValues()) {
                    collectMatchingValues(pv, predicate, result, visited);
                }
            }
            default -> { /* leaf — already tested above */ }
        }
    }
}
