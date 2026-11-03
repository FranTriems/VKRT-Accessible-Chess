package org.vkrt.accessiblechess;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded, explicit es/en voice grammar. Parsing never authorizes a chess state mutation. */
final class VoiceCommands {
    private VoiceCommands() {}
    static String normalize(String raw) {
        if(raw==null||raw.length()>160)return "";
        return Normalizer.normalize(raw.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").replaceAll("[^a-z0-9 ]"," ").trim().replaceAll("\\s+"," ");
    }
    static String parse(String raw){return parse(raw,"es");}
    static String parse(String raw,String language){
        String s=normalize(raw);if(s.isEmpty())return "UNKNOWN";
        // Cross-language switching is recognized regardless of the chosen locale.
        switch(s){
            case "idioma":case "cambiar idioma":case "lenguaje":case "language":
            case "choose language":case "change language":case "select language":return "LANG_MENU";
            case "idioma ingles":case "cambiar a ingles":case "hablar ingles":case "switch to english":
            case "english language":case "language english":case "speak english":return "LANG:en";
            case "idioma espanol":case "idioma castellano":case "cambiar a espanol":case "hablar espanol":
            case "switch to spanish":case "spanish language":case "language spanish":case "speak spanish":return "LANG:es";
        }
        return "en".equals(language)?english(s):spanish(s);
    }
    private static String english(String s){
        switch(s){
            case "help":case "what can i say":case "commands":case "instructions":return "HELP";
            case "introduction":case "welcome":case "how to play":return "INTRO";
            case "board":case "read board":case "describe board":case "chessboard":return "BOARD";
            case "status":case "state":case "whose turn":case "turn":return "STATE";
            case "repeat":case "say again":return "REPEAT";
            case "history":case "move history":case "last moves":return "HISTORY";
            case "options":case "legal moves":case "moves":case "what can i move":return "OPTIONS";
            case "next":case "next options":case "more options":return "NEXT";
            case "previous":case "previous options":case "back options":return "PREVIOUS";
            case "confirm":case "yes confirm":case "accept":return "CONFIRM";
            case "cancel":case "no cancel":case "reject":return "CANCEL";
            case "new game":case "restart game":case "reset game":return "NEW";
            case "undo":case "undo move":case "take back":return "UNDO";
            case "stockfish":case "suggestion":case "hint":case "suggest a move":return "SUGGEST";
            case "engine":case "engine status":return "ENGINE";
            case "basic opponent":return "BASIC";
            case "voice diagnostics":case "microphone":return "DIAGNOSTICS";
            case "revoke consent":return "REVOKE";
            case "easy level":return "LEVEL:1320";
            case "medium level":return "LEVEL:1500";
            case "hard level":return "LEVEL:1900";
            case "expert level":return "LEVEL:2400";
        }
        Matcher m=Pattern.compile("^(?:(?:option|number) )?(one|two|three|four|five|six|[1-6])$").matcher(s);
        if(m.matches()){
            String[] words={"one","two","three","four","five","six"};
            for(int i=0;i<6;i++)if(words[i].equals(m.group(1)))return "OPTION:"+(i+1);
            return "OPTION:"+m.group(1);
        }
        String expanded=englishNumbers(s);
        m=Pattern.compile("^(?:(?:rank|read rank|row) )([1-8])$").matcher(expanded);
        if(m.matches())return "ROW:"+m.group(1);
        m=Pattern.compile("^(?:(?:square|from|origin) )?([a-h]) ?([1-8])$").matcher(expanded);
        if(m.matches())return "SQUARE:"+m.group(1)+m.group(2);
        m=Pattern.compile("^(?:(?:to|destination) )([a-h]) ?([1-8])$").matcher(expanded);
        if(m.matches())return "DEST:"+m.group(1)+m.group(2);
        String move=MoveGrammar.parseEnglish(s);
        if(move==null)return "UNKNOWN";
        return "cancel".equals(move)?"CANCEL":"MOVE:"+move;
    }
    static String englishNumbers(String s){
        String[] words={"one","two","three","four","five","six","seven","eight"};
        for(int i=0;i<8;i++)s=s.replaceAll("\\b"+words[i]+"\\b",String.valueOf(i+1));
        return s;
    }
    private static String spanish(String s){
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
