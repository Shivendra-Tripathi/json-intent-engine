package io.github.shivendra.tripathi.scanningengine;

import io.github.shivendra.tripathi.wrapper.*;

import java.util.List;
import java.util.Optional;

/**
 * Comprehensive demo of {@link ScannerEngine}.
 *
 * Assumes these annotations exist in the scanned packages:
 *
 *   @Handler(value = "...")          — marks a controller class
 *   @Route(path = "...", method = "GET") — marks a handler method
 *   @Inject                          — marks an injectable field
 *   @Validated                       — marks a method whose parameters are validated
 *   @NotNull                         — marks a method parameter
 */
public class ScannerEngineExample {

    // ── Annotation names ────────────────────────────────────────────────────
    private static final String HANDLER   = "sample.annotations.Handler";
    private static final String ROUTE     = "sample.annotations.Route";
    private static final String INJECT    = "sample.annotations.Inject";
    private static final String VALIDATED = "sample.annotations.Validated";
    private static final String NOT_NULL  = "sample.annotations.NotNull";

    public static void main(String[] args) {

        // ── 1. Build annotation sets ────────────────────────────────────────
        AnnotationSet handlerSet   = AnnotationSet.allOfNames(HANDLER);
        AnnotationSet routeSet     = AnnotationSet.allOfNames(ROUTE);
        AnnotationSet injectSet    = AnnotationSet.allOfNames(INJECT);
        AnnotationSet validatedSet = AnnotationSet.allOfNames(VALIDATED);

        // ── 2. Configure and scan ───────────────────────────────────────────
        ScannerEngine engine = new ScannerEngine("sample", "com.example")

            // Rule A: all @Handler classes
            .registerClassRule(handlerSet)

            // Rule B: @Route methods inside @Handler classes
            .registerMethodRule(routeSet, handlerSet)

            // Rule C: @Validated methods anywhere (no class filter)
            .registerMethodRule(validatedSet)

            // Rule D: @Inject fields inside @Handler classes
            .registerFieldRule(injectSet, handlerSet)

            // Rule E: @Inject fields anywhere
            .registerFieldRule(injectSet)

            .scan();   // ← runs ClassGraph exactly once

        // ── 3. Retrieve the ScanResult (optional — see direct getters below) ─
        ScanResult result = engine.getScanResult();
        System.out.println("Scan complete: " + result);
        System.out.println();

        // ── 4. Fetch @Handler classes ────────────────────────────────────────
        List<ScannedClass> handlerClasses = engine.getResultsAsClasses(handlerSet);
        // Alternatively via RuleKey:
        // List<ScannedClass> handlerClasses =
        //     result.getClasses(RuleKey.forClass(handlerSet));

        System.out.println("=== @Handler classes (" + handlerClasses.size() + ") ===");
        for (ScannedClass sc : handlerClasses) {

            Optional<ScannedDeclaredAnnotation> handlerAnn =
                    sc.getDeclaredAnnotation(HANDLER);

            String handlerName = handlerAnn
                    .flatMap(a -> a.getParameterValue("value"))
                    .filter(v -> v.getKind() == ScannedAnnotationValue.Kind.STRING)
                    .map(ScannedAnnotationValue::asString)
                    .orElse(sc.getSimpleName());

            System.out.printf("  [%s]  %s%n", handlerName, sc.getClassName());
        }
        System.out.println();

        // ── 5. Fetch @Route methods in @Handler classes ──────────────────────
        List<ScannedMethod> routeMethods = engine.getResultsAsMethods(routeSet, handlerSet);
        // Alternatively:
        // List<ScannedMethod> routeMethods =
        //     result.getMethods(routeSet, handlerSet);

        System.out.println("=== @Route methods (" + routeMethods.size() + ") ===");
        for (ScannedMethod method : routeMethods) {

            ScannedDeclaredAnnotation routeAnn =
                    method.getDeclaredAnnotation(ROUTE).orElseThrow();

            String path       = routeAnn.getParameterValue("path")
                                        .map(ScannedAnnotationValue::asString)
                                        .orElse("/");
            String httpMethod = routeAnn.getParameterValue("method")
                                        .map(ScannedAnnotationValue::asString)
                                        .orElse("GET");

            System.out.printf("  [%s] %-30s -> %s#%s(%s)%n",
                    httpMethod,
                    path,
                    method.getDeclaringClassName(),
                    method.getName(),
                    parameterSummary(method));
        }
        System.out.println();

        // ── 6. Fetch @Validated methods (no class filter) ────────────────────
        List<ScannedMethod> validatedMethods = engine.getResultsAsMethods(validatedSet);

        System.out.println("=== @Validated methods (" + validatedMethods.size() + ") ===");
        for (ScannedMethod method : validatedMethods) {
            System.out.printf("  %s#%s%n",
                    method.getDeclaringClassName(), method.getName());

            // Show which parameters have @NotNull
            List<ScannedMethodParameter> notNullParams =
                    method.getParametersAnnotatedWith(NOT_NULL);
            notNullParams.forEach(p ->
                System.out.printf("    @NotNull param[%d] %s %s%n",
                        p.getIndex(),
                        p.getTypeName(),
                        p.getName() != null ? p.getName() : ""));
        }
        System.out.println();

        // ── 7. Fetch @Inject fields in @Handler classes ──────────────────────
        List<ScannedField> injectedInHandlers =
                engine.getResultsAsFields(injectSet, handlerSet);

        System.out.println("=== @Inject fields in @Handler classes ("
                + injectedInHandlers.size() + ") ===");
        for (ScannedField field : injectedInHandlers) {
            System.out.printf("  %s  %s.%s%n",
                    field.getTypeName(),
                    field.getDeclaringClassName(),
                    field.getName());
        }
        System.out.println();

        // ── 8. Fetch @Inject fields anywhere ────────────────────────────────
        List<ScannedField> allInjected = engine.getResultsAsFields(injectSet);

        System.out.println("=== All @Inject fields (" + allInjected.size() + ") ===");
        allInjected.forEach(f ->
            System.out.printf("  %s  %s.%s%n",
                    f.getTypeName(), f.getDeclaringClassName(), f.getName()));
        System.out.println();

        // ── 9. RuleKey-based access (most explicit, best for storing keys) ───
        RuleKey routeInHandlerKey = RuleKey.forMethod(routeSet, handlerSet);
        List<ScannedMethod> sameRoutes = result.getMethods(routeInHandlerKey);
        System.out.println("Routes via RuleKey: " + sameRoutes.size());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String parameterSummary(ScannedMethod method) {
        return method.getParameters().stream()
                .map(p -> p.getType().getTypeName()
                          + (p.getName() != null ? " " + p.getName() : ""))
                .reduce((a, b) -> a + ", ")
                .orElse("");
    }
}
