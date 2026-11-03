package org.vkrt.accessiblechess;

public final class VoiceBilingualTest {
    static int tests=0;
    private static void eq(String raw,String language,String expected){
        String actual=VoiceCommands.parse(raw,language);tests++;
        if(!expected.equals(actual))throw new AssertionError(raw+" in "+language+" -> "+actual+" expected "+expected);
    }
    private static void noMutation(String raw,String lang){
        String got=VoiceCommands.parse(raw,lang);tests++;
        if(got.startsWith("MOVE:")||got.equals("CONFIRM"))throw new AssertionError("False positive: "+raw+" -> "+got);
    }
    public static void main(String[] args){
        eq("help","en","HELP");eq("what can I say","en","HELP");eq("introduction","en","INTRO");
        eq("board","en","BOARD");eq("rank two","en","ROW:2");eq("read rank eight","en","ROW:8");
        eq("status","en","STATE");eq("last moves","en","HISTORY");eq("repeat","en","REPEAT");
        eq("legal moves","en","OPTIONS");eq("next options","en","NEXT");eq("previous","en","PREVIOUS");
        eq("option two","en","OPTION:2");eq("number six","en","OPTION:6");eq("3","en","OPTION:3");
        eq("confirm","en","CONFIRM");eq("cancel","en","CANCEL");eq("new game","en","NEW");
        eq("undo move","en","UNDO");eq("suggestion","en","SUGGEST");eq("engine status","en","ENGINE");
        eq("microphone","en","DIAGNOSTICS");eq("revoke consent","en","REVOKE");eq("easy level","en","LEVEL:1320");
        eq("square e two","en","SQUARE:e2");eq("from e two","en","SQUARE:e2");
        eq("destination e four","en","DEST:e4");eq("to e4","en","DEST:e4");
        eq("e2 e4","en","MOVE:e2e4");eq("e two e four","en","MOVE:e2e4");
        eq("from e2 to e4","en","MOVE:e2e4");eq("pawn from e2 to e4","en","MOVE:e2e4");
        eq("move knight from g1 to f3","en","MOVE:g1f3");
        eq("a seven to a eight promote to queen","en","MOVE:a7a8q");
        eq("a7 a8 promotion knight","en","MOVE:a7a8n");
        eq("castle kingside","en","MOVE:castle_k");eq("queenside castle","en","MOVE:castle_q");
        eq("language","en","LANG_MENU");eq("idioma","es","LANG_MENU");
        eq("switch to spanish","en","LANG:es");eq("switch to english","es","LANG:en");
        eq("idioma inglés","es","LANG:en");eq("idioma español","en","LANG:es");
        eq("ayuda","es","HELP");eq("opción dos","es","OPTION:2");
        eq("peón de e2 a e4","es","MOVE:e2e4");
        eq("origen e dos","es","SQUARE:e2");eq("destino e cuatro","es","DEST:e4");
        eq("confirmar","es","CONFIRM");eq("enroque corto","es","MOVE:castle_k");
        eq("help","es","UNKNOWN");eq("ayuda","en","UNKNOWN");
        eq("","en","UNKNOWN");eq("a9 a10","en","UNKNOWN");
        noMutation("pawn to queen","en");noMutation("confirm move e2 e4","en");
        if(!"YOUR TURN".equals(BlindFirstUi.phase("ONGOING",false,false,false,true)))throw new AssertionError("english phase");
        if(!"TU TURNO".equals(BlindFirstUi.phase("ONGOING",false,false,false,false)))throw new AssertionError("spanish phase");
        if(!"No moves yet".equals(BlindFirstUi.preview("",true)))throw new AssertionError("english history");
        if(!"Sin movimientos todavía".equals(BlindFirstUi.preview("",false)))throw new AssertionError("spanish history");
        System.out.println("PASS_R34_BILINGUAL_VOICE_COMMANDS_AND_COMPATIBILITY;CASES="+tests);
    }
}
