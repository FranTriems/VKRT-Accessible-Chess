package org.vkrt.accessiblechess;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic Spanish voice command grammar; output is NOT permission to mutate the game. */
final class VoiceCommands {
    private VoiceCommands() {}
    static String normalize(String raw) {
        if(raw==null||raw.length()>160)return "";
        String t=Normalizer.normalize(raw.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").replaceAll("[^a-z0-9 ]"," ").trim().replaceAll("\\s+"," ");
        return t;
    }
    static String parse(String raw) {
        String s=normalize(raw);if(s.isEmpty())return "UNKNOWN";
        switch(s) {
            case "ayuda": case "que puedo decir": case "comandos":return "HELP";
            case "presentacion": case "introduccion": case "como se juega":return "INTRO";
            case "tablero": case "leer tablero": case "describir tablero":return "BOARD";
            case "estado": case "turno": case "que turno es":return "STATE";
            case "repetir": case "repite":return "REPEAT";
            case "historial": case "ultimas jugadas":return "HISTORY";
            case "opciones": case "movimientos": case "jugadas legales":case "que puedo mover":return "OPTIONS";
            case "siguientes": case "mas opciones":return "NEXT";
            case "anteriores": case "opciones anteriores":return "PREVIOUS";
            case "confirmar":case "confirmo":case "si confirmar":return "CONFIRM";
            case "cancelar":case "no cancelar":return "CANCEL";
            case "nueva partida":case "reiniciar partida":return "NEW";
            case "deshacer":case "retroceder jugada":return "UNDO";
            case "stockfish":case "sugerencia":case "sugerir jugada":return "SUGGEST";
            case "motor":case "estado del motor":return "ENGINE";
            case "rival basico":return "BASIC";
            case "diagnostico de voz":case "microfono":return "DIAGNOSTICS";
            case "revocar consentimiento":return "REVOKE";
            case "nivel facil":return "LEVEL:1320";
            case "nivel medio":return "LEVEL:1500";
            case "nivel dificil":return "LEVEL:1900";
            case "nivel experto":return "LEVEL:2400";
        }
        Matcher m=Pattern.compile("^(?:opcion |numero )?(uno|dos|tres|cuatro|cinco|seis|[1-6])$").matcher(s);
        if(m.matches()){
            String[] words={"uno","dos","tres","cuatro","cinco","seis"};
            for(int i=0;i<words.length;i++)if(words[i].equals(m.group(1)))return "OPTION:"+(i+1);
            return "OPTION:"+m.group(1);
        }
        // Recognizers may spell ranks as Spanish words instead of digits.
        String numericSquares=s;
        if(s.startsWith("fila ")||s.startsWith("leer fila ")||s.startsWith("casilla ")||
                s.startsWith("origen ")||s.startsWith("destino ")||s.startsWith("desde ")||s.startsWith("a ")){
            String[] words={"uno","dos","tres","cuatro","cinco","seis","siete","ocho"};
            for(int i=0;i<words.length;i++)numericSquares=numericSquares.replaceAll("\\b"+words[i]+"\\b",String.valueOf(i+1));
        }
        m=Pattern.compile("^(?:fila |leer fila )([1-8])$").matcher(numericSquares);
        if(m.matches())return "ROW:"+m.group(1);
        m=Pattern.compile("^(?:casilla |desde |origen )?([a-h]) ?([1-8])$").matcher(numericSquares);
        if(m.matches())return "SQUARE:"+m.group(1)+m.group(2);
        m=Pattern.compile("^(?:a |destino )([a-h]) ?([1-8])$").matcher(numericSquares);
        if(m.matches())return "DEST:"+m.group(1)+m.group(2);
        String move=MoveGrammar.parse(s);
        if(move==null) {
            // Coordinate commands with piece names and explicit mover: no guessing piece or destination.
            String shortMove=s.replaceFirst("^(?:mover |mueve |jugar |juega )","")
                .replaceFirst("^(?:peon |caballo |torre |alfil |dama |rey )(?:de )?","");
            move=MoveGrammar.parse(shortMove);
        }
        if(move!=null) {
            if("cancel".equals(move))return "CANCEL";
            return "MOVE:"+move;
        }
        return "UNKNOWN";
    }
}
