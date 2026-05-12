package io.github.shivendra.tripathi.intentroutingengine;

public class MissingAnnotationForParameterException extends RuntimeException {

    String what;
    public MissingAnnotationForParameterException(String what){
        this.what = what;
    }

    public String toString(){
        return what;
    }
}
