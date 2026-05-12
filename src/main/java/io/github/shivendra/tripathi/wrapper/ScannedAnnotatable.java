package io.github.shivendra.tripathi.wrapper;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base class for any scanned element that can carry annotations.
 * Provides deep annotation inspection without retaining ClassGraph objects.
 */
public abstract class ScannedAnnotatable {

    /** All declared annotations on this element, keyed by annotation class name. */
    private final Map<String, ScannedDeclaredAnnotation> annotationsByName;

    protected ScannedAnnotatable(List<ScannedDeclaredAnnotation> annotations) {
        Map<String, ScannedDeclaredAnnotation> map = new LinkedHashMap<>();
        if (annotations != null) {
            for (ScannedDeclaredAnnotation a : annotations) {
                map.put(a.getAnnotationClassName(), a);
            }
        }
        this.annotationsByName = Collections.unmodifiableMap(map);
    }

    // -------------------------------------------------------------------------
    // Annotation inspection
    // -------------------------------------------------------------------------

    /** Returns all declared annotations on this element. */
    public List<ScannedDeclaredAnnotation> getDeclaredAnnotations() {
        return List.copyOf(annotationsByName.values());
    }

    /** Returns the {@link ScannedDeclaredAnnotation} for the given annotation class name, if present. */
    public Optional<ScannedDeclaredAnnotation> getDeclaredAnnotation(String annotationClassName) {
        return Optional.ofNullable(annotationsByName.get(annotationClassName));
    }

    /** Returns the {@link ScannedDeclaredAnnotation} for the given annotation class, if present. */
    public Optional<ScannedDeclaredAnnotation> getDeclaredAnnotation(Class<? extends Annotation> annotationClass) {
        return getDeclaredAnnotation(annotationClass.getName());
    }

    /** Returns true if this element is annotated with the given annotation class name. */
    public boolean hasAnnotation(String annotationClassName) {
        return annotationsByName.containsKey(annotationClassName);
    }

    /** Returns true if this element is annotated with the given annotation class. */
    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
        return hasAnnotation(annotationClass.getName());
    }

    /**
     * Loads and returns the live {@link Annotation} instance for the given type,
     * using the provided {@link ClassLoader}. Requires the annotated element to
     * be loadable at runtime.
     */
    public <A extends Annotation> Optional<A> loadAnnotation(Class<A> annotationClass, ClassLoader classLoader)
            throws ClassNotFoundException {
        return getDeclaredAnnotation(annotationClass)
                .map(sda -> sda.loadAnnotationInstance(annotationClass, classLoader));
    }

    /** Returns annotation class names of all annotations on this element. */
    public List<String> getAnnotationClassNames() {
        return List.copyOf(annotationsByName.keySet());
    }

    /** Returns annotations whose class name matches the given simple name (case-sensitive). */
    public List<ScannedDeclaredAnnotation> getAnnotationsBySimpleName(String simpleName) {
        return annotationsByName.values().stream()
                .filter(a -> a.getAnnotationSimpleName().equals(simpleName))
                .collect(Collectors.toUnmodifiableList());
    }
}
