package org.vkrt.accessiblechess;

/** Android-independent visual phase and history; Spanish R31 compatibility retained. */
public final class BlindFirstUi {
    private BlindFirstUi() {}
    public static String phase(String result,boolean busy,boolean listening,boolean recovery) {
        return phase(result,busy,listening,recovery,false);
    }
    public static String phase(String result,boolean busy,boolean listening,boolean recovery,boolean english) {
        if (recovery) return english?"RECOVER GAME":"RECUPERAR PARTIDA";
        if (listening) return english?"LISTENING":"ESCUCHANDO";
        if (busy) return english?"PROCESSING":"PROCESANDO";
        if (result == null) return english?"MENU":"MENÚ";
        switch (result) {
            case "CHECKMATE": case "STALEMATE": case "DRAW_DEAD":
            case "DRAW_75_MOVE": case "DRAW_5_REPETITION": case "DRAW_CLAIMED":
                return english?"GAME OVER":"FIN DE PARTIDA";
            case "CHECK":return english?"CHECK":"JAQUE";
            case "ONGOING":return english?"YOUR TURN":"TU TURNO";
            default:return english?"MENU":"MENÚ";
        }
    }
    public static boolean urgent(String phase) {
        return "RECUPERAR PARTIDA".equals(phase)||"JAQUE".equals(phase)||
               "RECOVER GAME".equals(phase)||"CHECK".equals(phase);
    }
    public static String preview(String history) {return preview(history,false);}
    public static String preview(String history,boolean english) {
        if (history == null || history.trim().isEmpty()) return english?"No moves yet":"Sin movimientos todavía";
        String s=history.trim().replace('\n',' ');
        return s.length()<=200 ? s : "…"+s.substring(s.length()-199);
    }
}
