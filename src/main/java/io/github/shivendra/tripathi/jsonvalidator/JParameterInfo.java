package io.github.shivendra.tripathi.jsonvalidator;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JParameterInfo {
    
    /**
     * Contains all the information needed for json parameter
     * Stores the parameter information needed for validation.
     * Stores the parameter name as String.
     */
    private final String name;  //Name of the Parameter Annotated by Certain Annotation(@JParam("..."))
    private final String type;
    private final boolean optional;

   static final Map<String, Class<?>> TYPES = Map.ofEntries(
    Map.entry("int", int.class),
    Map.entry("boolean", boolean.class),
    Map.entry("short", short.class),
    Map.entry("byte", byte.class),
    Map.entry("char", char.class),
    Map.entry("float", float.class),
    Map.entry("double", double.class),
    Map.entry("long", long.class),

    Map.entry("java.lang.Integer", Integer.class),
    Map.entry("java.lang.Boolean", Boolean.class),
    Map.entry("java.lang.Short", Short.class),
    Map.entry("java.lang.Byte", Byte.class),
    Map.entry("java.lang.Character", Character.class),
    Map.entry("java.lang.Float", Float.class),
    Map.entry("java.lang.Double", Double.class),
    Map.entry("java.lang.Long", Long.class),
    Map.entry("java.lang.String",String.class)
  
    );
    //==============================================================  

    public JParameterInfo(String name,String fullyQualifiedTypeName){
        optional = fullyQualifiedTypeName.startsWith("java.util.Optional<") ?
                    true    :   false;
        this.name=name;
        this.type=fullyQualifiedTypeName; 
    }

    // private JParameterInfo(String name , String fullyQualifiedTypeName , boolean isOptional){
    //     this.name = name;
    //     this.type = fullyQualifiedTypeName;
    //     optional = isOptional;
    // }
    //===============================================================
    

    //===============================================================
    //                          GETTERS
    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    public boolean isOptional(){
        return optional;
    }

    //===============================================================

    //===============================================================
    //                     FETCHERS
    public Object fetchFrom(JsonNode node , ObjectMapper mapper){

        //Validating that parameter must exist
        if(!node.has(name) && !optional)
            throw new MissingJsonParameterException(name, node.toString());

        //Must be present in the node
        if(!optional && node.has(name)){
            //Now testing whether the Type is simple type or not
            if(TYPES.containsKey(type)){
                    return fetchSimpleTypeHelper(node,mapper);
            }else{
                return fetchComplexTypeHandler(node,mapper);
            }
        }
        else{
            //Must be optional type
            return fetchOptionalTypeHandler(node,mapper);
        }
    }
    //=====================================================================

    //=====================================================================
    //                          PRIVATE HELPERS
    private Object fetchSimpleTypeHelper(JsonNode node , ObjectMapper mapper){
           Object  value = mapper.convertValue(node.get(name)
            , TYPES.get(type));

            return value;
    }

    private Object fetchComplexTypeHandler(JsonNode node,ObjectMapper mapper){
        JavaType javaType = mapper.getTypeFactory()
        .constructFromCanonical(type);
        Object value = mapper.convertValue(node.get(name), javaType);

        return value;
    }

    private Object fetchOptionalTypeHandler(JsonNode node, ObjectMapper mapper){
        
        Object value = null;

        String tempType=type;
        String optionalStartString = "java.util.Optional<";
        int counts=0;
       while(tempType.startsWith(optionalStartString)){
        counts+=1;
        tempType=tempType.substring(optionalStartString.length(),tempType.length()-1);
       }

       if(node.has(name)){
        JavaType jtype  = mapper.getTypeFactory()
                        .constructFromCanonical(tempType);
        value = mapper.convertValue(node.get(name), jtype);
       }

       //Handling Optional Nesting
       while(counts-->0){
        value=Optional.ofNullable(value);
       }

       return value;
    }
    //===============================================================
}
