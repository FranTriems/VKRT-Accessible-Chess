package org.vkrt.accessiblechess;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deliberately small coordinate grammar. Every result still requires native legality and confirmation. */
final class MoveGrammar {
    private static final Pattern MOVE = Pattern.compile(
        "^(?:de )?([a-h]) ?([1-8])(?: a)? ?([a-h]) ?([1-8])(?: ?(?:promocion )?(q|r|b|n|dama|torre|alfil|caballo))?$");
    private MoveGrammar() {}
    static String parse(String raw) {
        if (raw == null || raw.length() > 120) return null;
        String s = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").trim().replaceAll("\\s+", " ");
        if (s.equals("cancelar")) return "cancel";
        if (s.equals("enroque corto")) return "castle_k";
        if (s.equals("enroque largo")) return "castle_q";
        String[] words = s.split(" ");
        String[] from = {"uno","dos","tres","cuatro","cinco","seis","siete","ocho",
                         "alfa","bravo","charlie","delta","eco","foxtrot","golf","hotel","hache"};
        String[] to = {"1","2","3","4","5","6","7","8","a","b","c","d","e","f","g","h","h"};
        for (int i=0;i<words.length;i++) for(int j=0;j<from.length;j++)
            if(words[i].equals(from[j])) {words[i]=to[j];break;}
        Matcher match = MOVE.matcher(String.join(" ", words));
        if(!match.matches())return null;
        String promotion=match.group(5);
        if(promotion==null)promotion="";
        else if(promotion.equals("dama"))promotion="q";
        else if(promotion.equals("torre"))promotion="r";
        else if(promotion.equals("alfil"))promotion="b";
        else if(promotion.equals("caballo"))promotion="n";
        return match.group(1)+match.group(2)+match.group(3)+match.group(4)+promotion;
    }
}
