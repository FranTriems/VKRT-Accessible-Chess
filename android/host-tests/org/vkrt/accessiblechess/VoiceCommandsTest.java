package org.vkrt.accessiblechess;

public final class VoiceCommandsTest {
    private static int n;
    private static void eq(String input,String expected){
        String actual=VoiceCommands.parse(input);
        if(!expected.equals(actual))throw new AssertionError(input+" => "+actual+" expected "+expected);n++;
    }
    public static void main(String[] args){
        eq("ayuda","HELP");eq("¿Qué puedo decir?","HELP");eq("Presentación","INTRO");
        eq("opciones","OPTIONS");eq("opción dos","OPTION:2");eq("número 5","OPTION:5");
        eq("siguientes","NEXT");eq("anteriores","PREVIOUS");
        eq("tablero","BOARD");eq("fila 2","ROW:2");eq("leer fila 8","ROW:8");eq("fila dos","ROW:2");
        eq("casilla e2","SQUARE:e2");eq("origen g1","SQUARE:g1");eq("origen e dos","SQUARE:e2");
        eq("destino e4","DEST:e4");eq("destino e cuatro","DEST:e4");eq("e2 e4","MOVE:e2e4");
        eq("mover e2 a e4","MOVE:e2e4");eq("peón de e2 a e4","MOVE:e2e4");
        eq("caballo de g1 a f3","MOVE:g1f3");eq("enroque corto","MOVE:castle_k");
        eq("deshacer","UNDO");eq("confirmar","CONFIRM");eq("cancelar","CANCEL");
        eq("sugerencia","SUGGEST");eq("motor","ENGINE");
        eq("revocar consentimiento","REVOKE");
        eq("e2 e9","UNKNOWN");eq("mover cualquier pieza","UNKNOWN");
        eq("confirmar cambiar","UNKNOWN");eq(null,"UNKNOWN");
        if(VoiceUxPolicy.shouldStopOnRelease(449)||!VoiceUxPolicy.shouldStopOnRelease(450))throw new AssertionError("Hold threshold");
        System.out.println("PASS_R31_VOICE_COMMANDS_AND_HOLD_POLICY;CASES="+n);
    }
}
