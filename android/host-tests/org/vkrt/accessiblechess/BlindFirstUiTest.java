package org.vkrt.accessiblechess;
public final class BlindFirstUiTest {
    static void eq(String want,String actual){if(!want.equals(actual))throw new AssertionError(want+" != "+actual);}
    public static void main(String[] args){
        eq("ESCUCHANDO",BlindFirstUi.phase("ONGOING",false,true,false));
        eq("PROCESANDO",BlindFirstUi.phase("ONGOING",true,false,false));
        eq("TU TURNO",BlindFirstUi.phase("ONGOING",false,false,false));
        eq("JAQUE",BlindFirstUi.phase("CHECK",false,false,false));
        eq("FIN DE PARTIDA",BlindFirstUi.phase("CHECKMATE",false,false,false));
        eq("RECUPERAR PARTIDA",BlindFirstUi.phase("ONGOING",false,false,true));
        eq("Sin movimientos todavía",BlindFirstUi.preview(""));
        if(BlindFirstUi.preview("z".repeat(500)).length()>200)throw new AssertionError("unbounded history");
        if(!BlindFirstUi.urgent("JAQUE"))throw new AssertionError("check urgency");
        System.out.println("PASS_R30_BLIND_FIRST_UI_MODEL");
    }
}
