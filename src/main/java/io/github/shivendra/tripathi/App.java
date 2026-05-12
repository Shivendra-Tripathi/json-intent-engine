package io.github.shivendra.tripathi;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.shivendra.tripathi.annotations.JsonIntentHandler;
import io.github.shivendra.tripathi.annotations.JsonIntentMapping;
import io.github.shivendra.tripathi.annotations.JsonParam;
import io.github.shivendra.tripathi.intentroutingengine.IntentDispatcher;

@JsonIntentHandler("user-handler")
public class App {

    // =========================
    // SIMPLE STRING
    // =========================
    @JsonIntentMapping("hello")
    public void hello(
            @JsonParam("name") String name
    ) {

        System.out.println(
                "Hello " + name
        );
    }

    // =========================
    // MULTIPLE PARAMETERS
    // =========================
    @JsonIntentMapping("user-info")
    public void userInfo(

            @JsonParam("name")
            String name,

            @JsonParam("age")
            int age
    ) {

        System.out.println(
                "Name : " + name
        );

        System.out.println(
                "Age  : " + age
        );
    }

    // =========================
    // BOOLEAN + DOUBLE
    // =========================
    @JsonIntentMapping("account-status")
    public void accountStatus(

            @JsonParam("active")
            boolean active,

            @JsonParam("balance")
            double balance
    ) {

        System.out.println(
                "Active  : " + active
        );

        System.out.println(
                "Balance : " + balance
        );
    }

    // =========================
    // LIST TEST
    // =========================
    @JsonIntentMapping("list-test")
    public void listTest(

            @JsonParam("names")
            List<String> names
    ) {

        System.out.println(
                "Names : " + names
        );
    }

    // =========================
    // MAP TEST
    // =========================
    @JsonIntentMapping("map-test")
    public void mapTest(

            @JsonParam("scores")
            Map<String,Integer> scores
    ) {

        System.out.println(
                "Scores : " + scores
        );
    }

    // =========================
    // STATIC METHOD TEST
    // =========================
    @JsonIntentMapping("static-test")
    public static void staticMethod(

            @JsonParam("msg")
            String msg
    ) {

        System.out.println(
                "Static Message : " + msg
        );
    }

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args)
            throws Throwable {

        ObjectMapper mapper =
                new ObjectMapper();

        IntentDispatcher dispatcher =
                new IntentDispatcher(
                        "io.github.shivendra.tripathi"
                );

        dispatcher.registerToUse(new App());

        // =========================
        // TEST 1
        // =========================
        String json1 = """
        {
            "intent":"hello",
            "name":"Shivendra"
        }
        """;

        dispatcher.dispatch(
                mapper.readTree(json1),
                mapper
        );

        System.out.println(
                "----------------"
        );

        // =========================
        // TEST 2
        // =========================
        String json2 = """
        {
            "intent":"user-info",
            "name":"Shiva",
            "age":21
        }
        """;

        dispatcher.dispatch(
                mapper.readTree(json2),
                mapper
        );

        System.out.println(
                "----------------"
        );

        // =========================
        // TEST 3
        // =========================
        String json3 = """
        {
            "intent":"account-status",
            "active":true,
            "balance":5421.75
        }
        """;

        dispatcher.dispatch(
                mapper.readTree(json3),
                mapper
        );

        System.out.println(
                "----------------"
        );

        // =========================
        // TEST 4
        // =========================
        String json4 = """
        {
            "intent":"list-test",
            "names":[
                "A",
                "B",
                "C"
            ]
        }
        """;

        dispatcher.dispatch(
                mapper.readTree(json4),
                mapper
        );

        System.out.println(
                "----------------"
        );

        // =========================
        // TEST 5
        // =========================
        String json5 = """
        {
            "intent":"map-test",
            "scores":{
                "math":90,
                "science":85
            }
        }
        """;

        dispatcher.dispatch(
                mapper.readTree(json5),
                mapper
        );

        System.out.println(
                "----------------"
        );

        // =========================
        // TEST 6
        // =========================
        String json6 = """
        {
            "intent":"static-test",
            "msg":"STATIC CALL WORKED"
        }
        """;

        dispatcher.dispatch(
                mapper.readTree(json6),
                mapper
        );

        System.out.println(
                "----------------"
        );

        dispatcher.displayInfo();
    }
}