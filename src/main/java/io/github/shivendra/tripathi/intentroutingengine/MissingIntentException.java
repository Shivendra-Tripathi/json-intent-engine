package io.github.shivendra.tripathi.intentroutingengine;

public class MissingIntentException extends RuntimeException {
    
    private String what;

    public MissingIntentException(String what){
        this.what=what;
    }

    public String toString(){
        return what;
    }
}
