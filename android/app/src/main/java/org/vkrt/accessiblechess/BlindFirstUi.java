package org.vkrt.accessiblechess;

/** Deterministic, Android-independent phase and short-history presentation. */
public final class BlindFirstUi {
    private BlindFirstUi() {}
    public static String phase(String result,boolean busy,boolean listening,boolean recovery) {
        if (recovery) return "RECUPERAR PARTIDA";
        if (listening) return "ESCUCHANDO";
        if (busy) return "PROCESANDO";
        if (result == null) return "MENÚ";
        switch (result) {
            case "CHECKMATE": case "STALEMATE": case "DRAW_DEAD":
            case "DRAW_75_MOVE": case "DRAW_5_REPETITION": case "DRAW_CLAIMED":
                return "FIN DE PARTIDA";
            case "CHECK":return "JAQUE";
            case "ONGOING":return "TU TURNO";
            default:return "MENÚ";
        }
    }
    public static boolean urgent(String phase) {
        return "RECUPERAR PARTIDA".equals(phase) || "JAQUE".equals(phase);
    }
    public static String preview(String history) {
        if (history == null || history.trim().isEmpty()) return "Sin movimientos todavía";
        String s=history.trim().replace('\n',' ');
        return s.length()<=200 ? s : "…"+s.substring(s.length()-199);
    }
}
