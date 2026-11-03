package org.vkrt.accessiblechess;

public final class MoveGrammarTest {
    private static void check(String input,String expected) {
        String actual=MoveGrammar.parse(input);
        if(expected==null?actual!=null:!expected.equals(actual))
            throw new AssertionError("Grammar fixture: "+input);
    }
    public static void main(String[] args) {
        check("e2e4","e2e4");check(" E2 E4 ","e2e4");
        check("de eco dos a eco cuatro","e2e4");
        check("a siete a a ocho promoción caballo","a7a8n");
        check("e7 e8 dama","e7e8q");check("hache dos hache cuatro","h2h4");
        check("enroque corto","castle_k");check("enroque largo","castle_q");
        check("cancelar","cancel");
        for(String s:new String[]{"","e2e4 ahora","no e2 e4","e2 e4 e7 e5","caballo e2 e4",
            "e9e4","e2e4;","e2e4 rey","e7e8k","e2 e4 y confirma", "x".repeat(121)})check(s,null);
        for(String p:new String[]{"q","r","b","n"})check("a7a8"+p,"a7a8"+p);
        System.out.println("PASS strict Spanish coordinate grammar, promotions, cancellation, rejection");
    }
}
