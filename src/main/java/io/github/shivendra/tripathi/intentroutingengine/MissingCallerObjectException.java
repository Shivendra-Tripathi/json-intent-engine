package io.github.shivendra.tripathi.intentroutingengine;

public class MissingCallerObjectException extends Exception {
    
    private String what;
    public MissingCallerObjectException(String what){
        this.what=what;
    }

    public String toString(){
        return what;
    }
}
