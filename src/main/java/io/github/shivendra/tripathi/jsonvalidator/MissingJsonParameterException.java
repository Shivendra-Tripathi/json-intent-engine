package io.github.shivendra.tripathi.jsonvalidator;

public class MissingJsonParameterException extends RuntimeException {

    private final String what;

    //Constructor
    public MissingJsonParameterException(String what){
        this.what = what;
    }

    public MissingJsonParameterException(String missingParameterName, String json){
        this("The Parameter(\""+missingParameterName+"\") can't be fetched as Json not have Parameter(\""+missingParameterName+"\") in the json : "+json);
    }

    public MissingJsonParameterException(){
        this("The Json Passed has some error(i.e. missing certain needed parameter)");
    }

    @Override
    public String toString(){
        return what;
    }
}
