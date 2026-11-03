package org.vkrt.accessiblechess;
import java.io.BufferedReader;
import java.io.InputStreamReader;
public final class FakeUciEngine {
    public static void main(String[] args)throws Exception {
        String mode=args.length>0?args[0]:"ok";
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        String line;
        while((line=in.readLine())!=null){
            if(line.equals("uci")){System.out.println("id name FakeStockfishTest");System.out.println("uciok");System.out.flush();}
            if(line.equals("isready")){System.out.println("readyok");System.out.flush();}
            if(line.startsWith("go movetime ")){
                if(mode.equals("exit"))System.exit(3);
                if(mode.equals("bad"))System.out.println("bestmove a1a9");
                else if(mode.equals("critical"))System.out.println("info string CRITICAL ERROR malformed input");
                else if(mode.equals("none"))System.out.println("bestmove (none)");
                else if(mode.equals("hang")){Thread.sleep(11000);return;}
                else System.out.println("bestmove e2e4 ponder e7e5");
                System.out.flush();return;
            }
        }
    }
}
