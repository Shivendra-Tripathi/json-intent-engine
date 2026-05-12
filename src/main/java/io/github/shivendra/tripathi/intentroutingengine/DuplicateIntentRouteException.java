package io.github.shivendra.tripathi.intentroutingengine;

public class DuplicateIntentRouteException extends RuntimeException {

    private String what;
    public DuplicateIntentRouteException(String what){
        this.what = what;
    }
    
    public DuplicateIntentRouteException(){
        this("Same intent name used for multiple intents");
    }

    // public DuplicateIntentRouteException(String duplicateIntentName){
    //     this("Same intent(\""+duplicateIntentName+"\") used multiple times..");
    // }

    public String toString(){
        return what;
    }
}
