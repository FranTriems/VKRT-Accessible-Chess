package org.vkrt.accessiblechess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Optional, isolated Stockfish 19 UCI adapter. Never owns or modifies a chess position.
 * Always pass its proposal back to VKRT C++20 legal move adjudication.
 */
public final class StockfishUci {
    private StockfishUci() {}
    private static final Pattern UCI = Pattern.compile("[a-h][1-8][a-h][1-8][qrbn]?");
    private static final Pattern FEN = Pattern.compile("[PNBRQKpnbrqk1-8/]+ [wb] (?:-|[KQkq]{1,4}) (?:-|[a-h][36]) [0-9]{1,7} [0-9]{1,7}");
    public static final class Result {
        public final String move;
        public final String engine;
        Result(String move,String engine) {this.move=move;this.engine=engine;}
    }
    public static Result analyze(File engine, String fen, int elo, int thinkMs) throws IOException {
        if(engine==null || !engine.isFile() || !engine.canExecute() || !"libstockfish.so".equals(engine.getName()))
            throw new IOException("Stockfish no está instalado para esta arquitectura");
        return analyzeCommand(Arrays.asList(engine.getAbsolutePath()),fen,elo,thinkMs);
    }
    // Package-private injection point for host-only UCI transport tests; never exposed to UI.
    static Result analyzeCommand(List<String> cmd,String fen,int elo,int thinkMs) throws IOException {
        if(cmd==null||cmd.isEmpty()||fen==null||fen.length()>256||!FEN.matcher(fen).matches())
            throw new IOException("Posición no válida");
        if(elo!=0&&(elo<1320||elo>3190))throw new IOException("Nivel fuera de límites");
        if(thinkMs<100||thinkMs>3000)throw new IOException("Presupuesto de cálculo inválido");
        Process process=new ProcessBuilder(cmd).redirectErrorStream(true).start();
        BlockingQueue<String> lines=new ArrayBlockingQueue<>(4096);
        AtomicReference<IOException> readError=new AtomicReference<>();
        Thread reader=new Thread(()->{
            try(BufferedReader input=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))) {
                String line;
                while((line=input.readLine())!=null) {
                    if(line.length()>4096)throw new IOException("Salida UCI demasiado larga");
                    if(!lines.offer(line))throw new IOException("Salida UCI desbordada");
                }
            } catch(IOException e){readError.set(e);}
        },"vkrt-uci-reader");
        reader.setDaemon(true);reader.start();
        try(BufferedWriter output=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.US_ASCII))){
            final long handshake=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
            send(output,"uci");
            String name="Stockfish";
            while(true) {
                String line=next(lines,readError,process,handshake);
                if(line.equals("uciok"))break;
                if(line.startsWith("id name "))name=line.substring(8,Math.min(line.length(),128));
            }
            send(output,"setoption name Threads value 1");
            send(output,"setoption name Hash value 16");
            send(output,"setoption name Ponder value false");
            send(output,"setoption name MultiPV value 1");
            send(output,"setoption name UCI_LimitStrength value "+(elo==0?"false":"true"));
            if(elo!=0)send(output,"setoption name UCI_Elo value "+elo);
            send(output,"isready");
            long ready=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
            while(!next(lines,readError,process,ready).equals("readyok")){}
            send(output,"ucinewgame");
            send(output,"position fen "+fen);
            send(output,"go movetime "+thinkMs);
            final long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(thinkMs+6500L);
            while(true){
                String line=next(lines,readError,process,deadline);
                if(line.startsWith("bestmove ")) {
                    String[] fields=line.split(" ");
                    if(fields.length<2||!UCI.matcher(fields[1]).matches())throw new IOException("Stockfish no propuso jugada válida");
                    return new Result(fields[1],name);
                }
            }
        } finally {
            process.destroy();
            try {if(!process.waitFor(300,TimeUnit.MILLISECONDS))process.destroyForcibly();}
            catch(InterruptedException e){Thread.currentThread().interrupt();process.destroyForcibly();}
            reader.interrupt();
        }
    }
    private static void send(BufferedWriter writer,String cmd)throws IOException{writer.write(cmd);writer.write('\n');writer.flush();}
    private static String next(BlockingQueue<String> queue,AtomicReference<IOException> error,Process p,long deadline)throws IOException {
        while(System.nanoTime()<deadline){
            if(error.get()!=null)throw error.get();
            try {
                String s=queue.poll(75,TimeUnit.MILLISECONDS);
                if(s!=null){if(s.startsWith("info string CRITICAL ERROR"))throw new IOException("Stockfish rechazó posición");return s;}
                if(!p.isAlive()&&queue.isEmpty())throw new IOException("Motor UCI terminó antes de responder");
            } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Análisis interrumpido",e);}
        }
        throw new IOException("Stockfish excedió el tiempo permitido");
    }
}
