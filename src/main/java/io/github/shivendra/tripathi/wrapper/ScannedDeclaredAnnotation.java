package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper for ClassGraph's {@link AnnotationInfo}.
 *
 * <p>Stores:
 * <ul>
 *   <li>The annotation's fully-qualified class name</li>
 *   <li>All parameter values ({@link ScannedAnnotationValue}) including nested annotations</li>
 *   <li>Meta-annotations (annotations <em>on</em> the annotation class) as further
 *       {@code ScannedDeclaredAnnotation} instances – useful for deep annotation inspection</li>
 * </ul>
 *
 * <p>No ClassGraph type is retained after construction.
 */
public final class ScannedDeclaredAnnotation {

    private final String annotationClassName;
    private final String annotationSimpleName;

    /** Ordered map: parameter name → value */
    private final Map<String, ScannedAnnotationValue> parameterValues;

    /** Meta-annotations declared on the annotation type itself, keyed by class name */
    private final Map<String, ScannedDeclaredAnnotation> metaAnnotations;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private ScannedDeclaredAnnotation(String annotationClassName,
                                      String annotationSimpleName,
                                      Map<String, ScannedAnnotationValue> parameterValues,
                                      Map<String, ScannedDeclaredAnnotation> metaAnnotations) {
        this.annotationClassName = Objects.requireNonNull(annotationClassName);
        this.annotationSimpleName = Objects.requireNonNull(annotationSimpleName);
        this.parameterValues  = Collections.unmodifiableMap(parameterValues);
        this.metaAnnotations  = Collections.unmodifiableMap(metaAnnotations);
    }

    /**
     * Factory: create from a ClassGraph {@link AnnotationInfo}.
     * This is the only method that touches ClassGraph types.
     */
    public static ScannedDeclaredAnnotation from(AnnotationInfo annotationInfo) {
        String className   = annotationInfo.getName();
        String simpleName  = simpleNameOf(className);

        // --- parameter values ---
        Map<String, ScannedAnnotationValue> params = new LinkedHashMap<>();
        if (annotationInfo.getParameterValues() != null) {
            for (AnnotationParameterValue apv : annotationInfo.getParameterValues()) {
                ScannedAnnotationValue sav = ScannedAnnotationValue.from(apv);
                params.put(apv.getName(), sav);
            }
        }

        // --- meta-annotations (annotations on the annotation type) ---
        Map<String, ScannedDeclaredAnnotation> metas = new LinkedHashMap<>();
        if (annotationInfo.getClassInfo() != null &&
                annotationInfo.getClassInfo().getAnnotationInfo() != null) {
            for (AnnotationInfo meta : annotationInfo.getClassInfo().getAnnotationInfo()) {
                ScannedDeclaredAnnotation scannedMeta = from(meta);
                metas.put(scannedMeta.getAnnotationClassName(), scannedMeta);
            }
        }

        return new ScannedDeclaredAnnotation(className, simpleName, params, metas);
    }

    // -------------------------------------------------------------------------
    // Core accessors
    // -------------------------------------------------------------------------

    /** Fully-qualified annotation type name, e.g. {@code "com.example.MyAnnotation"}. */
    public String getAnnotationClassName()  { return annotationClassName; }

    /** Simple class name, e.g. {@code "MyAnnotation"}. */
    public String getAnnotationSimpleName() { return annotationSimpleName; }

    // -------------------------------------------------------------------------
    // Parameter values
    // -------------------------------------------------------------------------

    /** All parameter values declared on this annotation use-site. */
    public List<ScannedAnnotationValue> getParameterValues() {
        return List.copyOf(parameterValues.values());
    }

    /** Returns the value of the named parameter, if present. */
    public Optional<ScannedAnnotationValue> getParameterValue(String parameterName) {
        return Optional.ofNullable(parameterValues.get(parameterName));
    }

    /**
     * Convenience: returns the raw Java object for the {@code value()} parameter
     * (the default parameter name for single-element annotations).
     */
    public Optional<ScannedAnnotationValue> getDefaultValue() {
        return getParameterValue("value");
    }

    /** Convenience: returns the string value of {@code value()}, or empty. */
    public Optional<String> getStringValue() {
        return getDefaultValue()
                .filter(v -> v.getKind() == ScannedAnnotationValue.Kind.STRING)
                .map(ScannedAnnotationValue::asString);
    }

    /** Returns all parameter names declared on this annotation use-site. */
    public List<String> getParameterNames() {
        return List.copyOf(parameterValues.keySet());
    }

    // -------------------------------------------------------------------------
    // Meta-annotation inspection
    // -------------------------------------------------------------------------

    /** Annotations declared on the annotation class itself (meta-annotations). */
    public List<ScannedDeclaredAnnotation> getMetaAnnotations() {
        return List.copyOf(metaAnnotations.values());
    }

    /** Returns true if the annotation type is itself annotated with the given annotation. */
    public boolean hasMetaAnnotation(String metaAnnotationClassName) {
        return metaAnnotations.containsKey(metaAnnotationClassName);
    }

    /** Returns true if the annotation type is itself annotated with the given annotation. */
    public boolean hasMetaAnnotation(Class<? extends Annotation> metaAnnotationClass) {
        return hasMetaAnnotation(metaAnnotationClass.getName());
    }

    /** Returns the meta-annotation of the given name, if present. */
    public Optional<ScannedDeclaredAnnotation> getMetaAnnotation(String metaAnnotationClassName) {
        return Optional.ofNullable(metaAnnotations.get(metaAnnotationClassName));
    }

    public Optional<ScannedDeclaredAnnotation> getMetaAnnotation(Class<? extends Annotation> metaAnnotationClass) {
        return getMetaAnnotation(metaAnnotationClass.getName());
    }

    // -------------------------------------------------------------------------
    // Reflection loading
    // -------------------------------------------------------------------------

    /**
     * Loads the annotation class using the given {@link ClassLoader}.
     *
     * @throws ClassNotFoundException if the annotation class cannot be found
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Annotation> loadAnnotationClass(ClassLoader classLoader)
            throws ClassNotFoundException {
        return (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);
    }

    /**
     * Returns the live annotation instance cast to {@code A} from the supplied
     * annotated element's annotation array.
     *
     * <p>NOTE: this method does NOT instantiate the annotation itself — the caller
     * must supply it from the reflective element (field/method/class).  The helper
     * exists primarily so higher-level wrappers (ScannedField, ScannedMethod, etc.)
     * can delegate to it.
     *
     * @param annotationClass the annotation type
     * @param classLoader     class-loader to resolve the type
     * @return the annotation instance, or {@code null} if unavailable
     */
    // @SuppressWarnings("unchecked")
    public <A extends Annotation> A loadAnnotationInstance(Class<A> annotationClass,
                                                           ClassLoader classLoader) {
        // Resolution only; the caller's reflective element provides the actual instance
        return null; // See ScannedField / ScannedMethod / ScannedClass for full implementations
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String simpleNameOf(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot >= 0 ? binaryName.substring(dot + 1) : binaryName;
    }

    @Override
    public String toString() {
        return "@" + annotationSimpleName +
               (parameterValues.isEmpty() ? "" : parameterValues.toString());
    }
}
