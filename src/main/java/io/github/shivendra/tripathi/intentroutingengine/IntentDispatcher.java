package io.github.shivendra.tripathi.intentroutingengine;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.shivendra.tripathi.annotations.JsonIntentHandler;
import io.github.shivendra.tripathi.annotations.JsonIntentMapping;
import io.github.shivendra.tripathi.annotations.JsonParam;
import io.github.shivendra.tripathi.jsonvalidator.JMethodParameters;
import io.github.shivendra.tripathi.scanningengine.AnnotationSet;
import io.github.shivendra.tripathi.scanningengine.ScannerEngine;
import io.github.shivendra.tripathi.wrapper.ScannedDeclaredAnnotation;
import io.github.shivendra.tripathi.wrapper.ScannedMethod;
import io.github.shivendra.tripathi.wrapper.ScannedMethodParameter;

public class IntentDispatcher {

    private final static Class<? extends Annotation> intentMappingMethodAnnoClass = JsonIntentMapping.class;
    private static Class<? extends Annotation> intentHandlerAnnoClass = JsonIntentHandler.class;
    private final static Class<? extends Annotation> intentMappingMethodParameterAnnoClass = JsonParam.class; 

    private final RoutingManager routingManager;

   

    //Scans the provided methods only
    public IntentDispatcher(String ... packages) throws ClassNotFoundException,NoSuchMethodException,IllegalAccessException{

        routingManager = new RoutingManager();

        AnnotationSet methodAnnoSet =  AnnotationSet.allOf(intentMappingMethodAnnoClass);
        AnnotationSet classAnnoSet = AnnotationSet.allOf(intentHandlerAnnoClass) ; 

        ScannerEngine scannerEngine
         = new ScannerEngine(packages)
         .registerMethodRule(methodAnnoSet , classAnnoSet)
         .scan();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        List<ScannedMethod> methods = scannerEngine.getResultsAsMethods(methodAnnoSet,classAnnoSet);

        for(ScannedMethod method : methods ){
            //getting all the parameters of the method
            List<ScannedMethodParameter> parameters = method.getParameters();
            
            //Constructing the method type
            List<Class<?> > paramTypes = new ArrayList<>();

            //JMethodParameters
            JMethodParameters jMethodParameters = new JMethodParameters();
            
            for(ScannedMethodParameter parameter : parameters){
                if(!parameter.hasAnnotation(intentMappingMethodParameterAnnoClass)){
                    throw new MissingAnnotationForParameterException("\""+ intentMappingMethodParameterAnnoClass.getName()
                    +"\"does not exist for some parameter in "+method.getName()+" in class "+method.getDeclaringClassName());
                }

                paramTypes.add(parameter.loadType(loader));
                ScannedDeclaredAnnotation paramAnno = parameter.getDeclaredAnnotation(intentMappingMethodParameterAnnoClass).get();
                String paramName = paramAnno.getDefaultValue().get().asString();

                jMethodParameters.addNewParameter(paramName, parameter.getType().getGenericString());
            }


            //Creating the methodHandle
            MethodHandle handle;

            if(method.isStatic()){
                handle = lookup.findStatic(
                    Class.forName(method.getDeclaringClassName()),
                    method.getName(),
                    MethodType.methodType(
                        method.getReturnType().load(loader),
                        paramTypes.toArray(new Class<?>[0])
                    )
                );
            }else{
                handle = lookup.findVirtual(
                    Class.forName(method.getDeclaringClassName()),
                    method.getName(),
                    MethodType.methodType(
                        method.getReturnType().load(loader),
                        paramTypes.toArray(new Class<?>[0])
                    )
                );
            }

            //Getting the mapping name
            ScannedDeclaredAnnotation scannedAnn = method.getDeclaredAnnotation(intentMappingMethodAnnoClass).get();
            //It is ensured that scannedAnn is not null

            String intentMappingName = scannedAnn.getDefaultValue().get().asString();

            routingManager.addRoute(intentMappingName, handle,jMethodParameters, method.getDeclaringClassName(), method.isStatic());


        }
    }

    //To register the object to be used
    public void registerToUse(Object object){
        routingManager.registerHandlerObject(object);
    }

    //To call the required method automatically
    public void dispatch(JsonNode node ,ObjectMapper mapper) throws Throwable{
        routingManager.invokeIntentMethod(node, mapper);
    }


    //To display the info
    public void displayInfo(){
        System.out.println(routingManager);
    }


}






