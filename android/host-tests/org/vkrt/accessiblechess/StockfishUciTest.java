package org.vkrt.accessiblechess;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
public final class StockfishUciTest {
    private static final String START="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    static void fail(String mode) throws Exception {
        try {call(mode,START,1500,100);throw new AssertionError("Accepted "+mode);}
        catch(IOException expected) {}
    }
    static StockfishUci.Result call(String mode,String fen,int elo,int ms)throws Exception {
        String java=System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
        return StockfishUci.analyzeCommand(Arrays.asList(java,"-cp",System.getProperty("java.class.path"),
            "org.vkrt.accessiblechess.FakeUciEngine",mode),fen,elo,ms);
    }
    public static void main(String[] args)throws Exception {
        StockfishUci.Result good=call("ok",START,1500,100);
        check(good.move.equals("e2e4"),"bestmove");check(good.engine.equals("FakeStockfishTest"),"id name");
        for(String m:new String[]{"bad","none","critical","exit","hang"})fail(m);
        try {call("ok","invalid\nquit",1500,100);throw new AssertionError("fen injection");}catch(IOException expected){}
        try {call("ok",START,1000,100);throw new AssertionError("elo gate");}catch(IOException expected){}
        try {call("ok",START,1500,99999);throw new AssertionError("think gate");}catch(IOException expected){}
        check(call("ok",START,0,100).move.equals("e2e4"),"full strength");
        System.out.println("PASS_UCI_HOST_TRANSPORT_AND_FAILURES");
    }
}
