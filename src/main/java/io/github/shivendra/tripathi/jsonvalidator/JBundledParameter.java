package io.github.shivendra.tripathi.jsonvalidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stores a single Pair <JParameterInfo,JParameterValue>
 */
public class JBundledParameter {
    
    private final JParameterInfo pInfo;
    private Object value;

    public JBundledParameter(JParameterInfo pInfo, Object pValue){
        this.pInfo=pInfo;
        this.value=pValue;
    }

    public static JBundledParameter createFrom(JParameterInfo pInfo){
        return new JBundledParameter(pInfo, null);
    }

    //Getters
    public JParameterInfo getJParameterInfo(){
        return pInfo;
    }

    public Object getValue(){
        return value;
    }

    //Setters
    public void setValueFromJson(JsonNode node, ObjectMapper mapper) throws MissingJsonParameterException {
        value =  pInfo.fetchFrom(node, mapper) ;
    }
}
