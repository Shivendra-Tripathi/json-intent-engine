package io.github.shivendra.tripathi.jsonvalidator;

import java.util.LinkedHashMap;
import java.util.Map;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stores the map <String annotatedName , JBundledParameter actualParameter  >
 */
public class JMethodParameters {
    
    private final LinkedHashMap< String , JParameterInfo > annotatedNameToParams;

    public JMethodParameters(LinkedHashMap<String,JParameterInfo> map){
        this.annotatedNameToParams = map;
    }

    public JMethodParameters(){
        this.annotatedNameToParams = new LinkedHashMap<>();
    }

    public static JMethodParameters createFrom(LinkedHashMap<String,JParameterInfo> map){
        return new JMethodParameters(map);
    }

    //Adding new parameter
    public void addNewParameter(String name, String type ){
        if(annotatedNameToParams.containsKey(name)){
            throw new RuntimeException("Duplicate parameter name ");
        }
        annotatedNameToParams.put(name, new JParameterInfo(name, type));
    }


    //Getter
    public Map<String,JParameterInfo> getMap(){
        return annotatedNameToParams;
    }


    //To cache all the values from the Json Object
    public Object[] fetchPassableObjectArray(JsonNode node,ObjectMapper mapper){
        Object [] arr = new Object[annotatedNameToParams.size()] ;
        int i=0;
        for(JParameterInfo param : annotatedNameToParams.values()){
            arr[i++] = param.fetchFrom(node,mapper);
        }
        return arr;
    }

}
