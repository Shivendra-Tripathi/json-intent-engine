package io.github.shivendra.tripathi.intentroutingengine;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.shivendra.tripathi.jsonvalidator.JMethodParameters;

public class RoutingManager {

    //Bundles all the routing information into a single entity
    public record RouteInfo(MethodHandle methodHandle,
        JMethodParameters jsonWorker,
        String containingClassName,
        Object classObject,
        boolean isStatic
    ) {
    }
    
    private final Map<String , RouteInfo> intentToRouteInfoMap;

    public RoutingManager(){
        intentToRouteInfoMap = new HashMap<>();
        
    }

    public void addRoute(String name, MethodHandle handle,JMethodParameters jsonWorker,String className,boolean isStaticMethod) throws DuplicateIntentRouteException{
        if(intentToRouteInfoMap.containsKey(name))
            throw new DuplicateIntentRouteException("IntentMapping(\""+name+"\") exists at more than one place");
        intentToRouteInfoMap.put(name, new RouteInfo(handle, jsonWorker,className,null,isStaticMethod));
    }

    public void registerHandlerObject(Object classObj){
        String className = classObj.getClass().getName();
        boolean foundAny = intentToRouteInfoMap.values()
                            .stream()
                            .anyMatch(r -> r.containingClassName().equals(className));
                
        if(!foundAny)
            throw new MissingIntentException("No Intent registered/added for the class(\""+className+"\") whose object is provided...");
        
        //Updating the classObject for each of the intent belonging to the class(classObject)
        intentToRouteInfoMap.replaceAll((intentName,oldRouteInfo) -> {
            if(oldRouteInfo.containingClassName().equals(className))
                return new RouteInfo(oldRouteInfo.methodHandle(), oldRouteInfo.jsonWorker(),oldRouteInfo.containingClassName(), classObj,oldRouteInfo.isStatic());
            return oldRouteInfo;
        });
    }
    



    //To invoke from the Json
    public void invokeIntentMethod(JsonNode node, ObjectMapper mapper) throws Throwable{
        String intentName = node.get("intent").asText();
        if(intentName==null || (!intentToRouteInfoMap.containsKey(intentName)) ){
            throw new RuntimeException("Missing the intent parameter in json :"+node);
        }

        RouteInfo routeInfo = intentToRouteInfoMap.get(intentName);

        Object[] parameters = routeInfo.jsonWorker().fetchPassableObjectArray(node, mapper);
        invokeIntentMethod(intentName, parameters);

    }

    //To Invoke the respective Intent
    public void invokeIntentMethod(String intentName , Object [] parameters) throws Throwable{
        //Handling the nullity of Parameters
        if(parameters == null)
            parameters = new Object[0];

        if(!intentToRouteInfoMap.containsKey(intentName))
             throw new MissingIntentException("No Intent registered/added for the : \""+intentName+"\"...");
        
        RouteInfo routeInfo = intentToRouteInfoMap.get(intentName);
        if(!routeInfo.isStatic()){
            if(routeInfo.classObject()==null)
                throw new MissingCallerObjectException("No Calling Object registered/added for the : \""+intentName+"\"...");
            routeInfo.methodHandle().invokeWithArguments(bindArguments(routeInfo.classObject(), parameters));
        }else{
            routeInfo.methodHandle().invokeWithArguments(parameters);
        }

    }

    //HELPER
    private Object[] bindArguments(Object ob, Object [] arr){
        Object [] ret = new Object[arr.length+1];
        ret[0]=ob;
        for(int i=0;i<arr.length;i++)
            ret[i+1]=arr[i];
        return ret;
    }



    public String toString(){
        String res="";
        for(Map.Entry<String,RouteInfo> route : intentToRouteInfoMap.entrySet()){
            res+=route.getKey().toString() + "------->"+route.getValue().toString()+"\n";
        }
        return res;
    }
    
}


//NOTE:Register of Handler Objects must be done after adding Routes

//
///POTENTIAL IMPROVEMENTS
///1) Seperation of Class Object Registry and Intent To MethodHandle Mapping
///2) Binding the MethodHandle to specific Class Object
/// 
/// 
/// 
/// 
/// 

