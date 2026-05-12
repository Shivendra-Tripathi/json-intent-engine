package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Central registry that owns all {@link ScannedClass} objects produced from a
 * single ClassGraph scan.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Run the scan (or accept an externally-provided {@link ScanResult})</li>
 *   <li>Convert every {@link ClassInfo} → {@link ScannedClass} and cache it</li>
 *   <li>Release the {@link ScanResult} so no ClassGraph memory is retained</li>
 *   <li>Provide rich query APIs for downstream consumers</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * ScannedClassRegistry registry = ScannedClassRegistry.scan("com.example");
 *
 * // Find all classes annotated with @Service
 * List<ScannedClass> services =
 *         registry.getClassesAnnotatedWith("com.example.annotations.Service");
 *
 * // Deep annotation inspection
 * services.forEach(sc ->
 *     sc.getDeclaredAnnotation("com.example.annotations.Service")
 *       .flatMap(a -> a.getParameterValue("name"))
 *       .ifPresent(v -> System.out.println(sc.getSimpleName() + " → " + v.asString())));
 * }</pre>
 */
public final class ScannedClassRegistry {

    /** All scanned classes, keyed by binary name. */
    private final Map<String, ScannedClass> classesByName;

    // -------------------------------------------------------------------------
    // Construction / scanning
    // -------------------------------------------------------------------------

    private ScannedClassRegistry(Map<String, ScannedClass> classesByName) {
        this.classesByName = Collections.unmodifiableMap(classesByName);
    }

    /**
     * Runs a ClassGraph scan over the specified package prefixes and builds the registry.
     *
     * @param acceptPackages zero or more package prefixes; if empty, the whole classpath is scanned
     */
    public static ScannedClassRegistry scan(String... acceptPackages) {
        ClassGraph cg = new ClassGraph()
                .enableAllInfo()
                .enableExternalClasses();

        if (acceptPackages.length > 0) {
            cg = cg.acceptPackages(acceptPackages);
        }

        try (ScanResult result = cg.scan()) {
            return fromScanResult(result);
        }
    }

    /**
     * Builds the registry from an externally-provided {@link ScanResult}.
     * The caller is responsible for closing the {@code ScanResult} afterwards.
     *
     * @param result an open ClassGraph scan result
     */
    public static ScannedClassRegistry fromScanResult(ScanResult result) {
        Map<String, ScannedClass> map = new LinkedHashMap<>();
        for (ClassInfo ci : result.getAllClasses()) {
            try {
                ScannedClass sc = ScannedClass.from(ci);
                map.put(sc.getClassName(), sc);
            } catch (Exception e) {
                // Skip classes that fail to convert (e.g. corrupted bytecode)
                System.err.println("[ScannedClassRegistry] Skipping " + ci.getName()
                        + " – " + e.getMessage());
            }
        }
        return new ScannedClassRegistry(map);
    }

    // -------------------------------------------------------------------------
    // Basic lookups
    // -------------------------------------------------------------------------

    /** Returns the {@link ScannedClass} for the given binary class name, if present. */
    public Optional<ScannedClass> getClass(String binaryClassName) {
        return Optional.ofNullable(classesByName.get(binaryClassName));
    }

    /** Returns the {@link ScannedClass} for the given class literal, if present. */
    public Optional<ScannedClass> getClass(Class<?> clazz) {
        return getClass(clazz.getName());
    }

    /** Returns all scanned classes. */
    public Collection<ScannedClass> getAllClasses() {
        return classesByName.values();
    }

    /** Returns the total number of scanned classes. */
    public int size() { return classesByName.size(); }

    // -------------------------------------------------------------------------
    // Annotation-based queries
    // -------------------------------------------------------------------------

    /** Returns all classes that carry the given annotation (by binary name). */
    public List<ScannedClass> getClassesAnnotatedWith(String annotationClassName) {
        return classesByName.values().stream()
                .filter(sc -> sc.hasAnnotation(annotationClassName))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns all classes that carry the given annotation. */
    public List<ScannedClass> getClassesAnnotatedWith(Class<? extends Annotation> annotationClass) {
        return getClassesAnnotatedWith(annotationClass.getName());
    }

    /**
     * Returns all classes that carry an annotation which is itself meta-annotated
     * with {@code metaAnnotationClassName}.  Useful for finding classes that use
     * composed / stereotype annotations.
     */
    public List<ScannedClass> getClassesWithAnnotationMetaAnnotatedWith(String metaAnnotationClassName) {
        return classesByName.values().stream()
                .filter(sc -> sc.getDeclaredAnnotations().stream()
                        .anyMatch(a -> a.hasMetaAnnotation(metaAnnotationClassName)))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<ScannedClass> getClassesWithAnnotationMetaAnnotatedWith(
            Class<? extends Annotation> metaAnnotationClass) {
        return getClassesWithAnnotationMetaAnnotatedWith(metaAnnotationClass.getName());
    }

    // -------------------------------------------------------------------------
    // Kind-based queries
    // -------------------------------------------------------------------------

    /** All interfaces. */
    public List<ScannedClass> getInterfaces() {
        return filter(ScannedClass::isInterface);
    }

    /** All enums. */
    public List<ScannedClass> getEnums() {
        return filter(ScannedClass::isEnum);
    }

    /** All annotation types. */
    public List<ScannedClass> getAnnotationTypes() {
        return filter(ScannedClass::isAnnotation);
    }

    /** All concrete (non-abstract, non-interface) classes. */
    public List<ScannedClass> getConcreteClasses() {
        return filter(ScannedClass::isConcrete);
    }

    // -------------------------------------------------------------------------
    // Hierarchy queries
    // -------------------------------------------------------------------------

    /** Returns all classes that directly extend the given class (by binary name). */
    public List<ScannedClass> getDirectSubclasses(String superclassName) {
        return classesByName.values().stream()
                .filter(sc -> sc.getSuperclassName()
                        .map(superclassName::equals).orElse(false))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns all classes that directly implement the given interface (by binary name). */
    public List<ScannedClass> getDirectImplementors(String interfaceName) {
        return classesByName.values().stream()
                .filter(sc -> sc.getInterfaceNames().contains(interfaceName))
                .collect(Collectors.toUnmodifiableList());
    }

    // -------------------------------------------------------------------------
    // Field / method queries across all classes
    // -------------------------------------------------------------------------

    /** Returns all fields across all scanned classes that carry the given annotation. */
    public List<ScannedField> getAllFieldsAnnotatedWith(String annotationClassName) {
        List<ScannedField> result = new ArrayList<>();
        for (ScannedClass sc : classesByName.values()) {
            result.addAll(sc.getFieldsAnnotatedWith(annotationClassName));
        }
        return Collections.unmodifiableList(result);
    }

    public List<ScannedField> getAllFieldsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        return getAllFieldsAnnotatedWith(annotationClass.getName());
    }

    /** Returns all methods across all scanned classes that carry the given annotation. */
    public List<ScannedMethod> getAllMethodsAnnotatedWith(String annotationClassName) {
        List<ScannedMethod> result = new ArrayList<>();
        for (ScannedClass sc : classesByName.values()) {
            result.addAll(sc.getMethodsAnnotatedWith(annotationClassName));
        }
        return Collections.unmodifiableList(result);
    }

    public List<ScannedMethod> getAllMethodsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        return getAllMethodsAnnotatedWith(annotationClass.getName());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<ScannedClass> filter(Predicate<ScannedClass> predicate) {
        return classesByName.values().stream()
                .filter(predicate)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toString() {
        return "ScannedClassRegistry[" + classesByName.size() + " classes]";
    }
}
