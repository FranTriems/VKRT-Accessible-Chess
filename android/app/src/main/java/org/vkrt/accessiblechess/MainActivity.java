package org.vkrt.accessiblechess;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AtomicFile;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Android framework only. Native state and disk operations run on a serialized worker. */
public final class MainActivity extends Activity {
    static { System.loadLibrary("vkrt_chess_jni"); }
    private static native String[] nativeRequest(String state,String action,String argument);
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final int MIC_REQUEST=7, MAX_SAVE=16384;
    // Native snapshot fields: save, FEN, result, legal UCI, squares, history, PGN, proposal, claim.
    private String[] snapshot;
    private TextView status,feedback,phaseLabel,historyPreview,microphoneIndicator;
    private Button voiceButton;
    private EditText input;
    private LinearLayout root;
    private ChessBoardView board;
    private VoiceSurface voiceSurface;
    private String pendingMove="",pendingBase="",pendingAction="",selectedOrigin="";
    private int choicePage=0;
    private boolean holdRequested=false,pressHeld=false,stopAfterReady=false,speechReady=false,stopSent=false,introPending=false;
    private long pressStarted=0L;
    private TextToSpeech tts;
    private AccessibilityManager accessibility;
    private boolean active,busy=true,ttsReady,listening,recovery,permissionOnDevice,pendingPermission;
    private int generation,voiceGeneration;
    private float speechRate=1f,speechVolume=0.8f;
    private int stockfishElo=1500,stockfishTimeMs=650;
    private String lastNarration="";
    private SpeechRecognizer recognizer;
    private AlertDialog dialog;
    private AtomicFile saveFile;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        saveFile=new AtomicFile(new File(getFilesDir(),"game.vkrt"));
        accessibility=(AccessibilityManager)getSystemService(Context.ACCESSIBILITY_SERVICE);
        speechRate=getPreferences(MODE_PRIVATE).getFloat("rate",1f);
        speechVolume=getPreferences(MODE_PRIVATE).getFloat("volume",0.8f);
        stockfishElo=getPreferences(MODE_PRIVATE).getInt("stockfish_elo",1500);
        stockfishTimeMs=getPreferences(MODE_PRIVATE).getInt("stockfish_ms",650);
        // R31: one interactive voice surface. Everything beneath is visual/read-only.
        FrameLayout frame=new FrameLayout(this);
        frame.setBackgroundColor(0xFF030606);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(18));
        frame.addView(root,new FrameLayout.LayoutParams(-1,-1));
        TextView heading=text("AJEDREZ POR VOZ",23);
        heading.setTypeface(null,Typeface.BOLD);
        heading.setTextColor(0xFFEAF4EC);
        heading.setGravity(Gravity.CENTER);
        TextView subtitle=text("VKRT · BLIND CHESS",12);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(0xFF7DAF91);
        board=new ChessBoardView(this);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,0,1.0f);
        bp.setMargins(0,dp(5),0,dp(12));
        root.addView(board,bp);
        phaseLabel=label("CARGANDO",21,0xFF8CE8A6);
        phaseLabel.setTypeface(null,Typeface.BOLD);
        phaseLabel.setGravity(Gravity.CENTER);
        root.addView(phaseLabel);
        status=label("Preparando partida",16,0xFFEAF4EC);
        status.setGravity(Gravity.CENTER);
        root.addView(status);
        feedback=label("Bienvenido. Mantené pulsado para hablar.",17,0xFFF4F1DF);
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(dp(4),dp(13),dp(4),dp(12));
        feedback.setMaxLines(4);
        feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(feedback);
        historyPreview=label("Sin movimientos todavía",13,0xFFAACAB5);
        historyPreview.setMaxLines(2);
        historyPreview.setGravity(Gravity.CENTER);
        root.addView(historyPreview);
        microphoneIndicator=label("MANTENÉ PULSADO PARA HABLAR · SOLTÁ PARA ENVIAR",13,0xFF88EBAA);
        microphoneIndicator.setGravity(Gravity.CENTER);
        root.addView(microphoneIndicator);
        // The full-screen surface is the ONLY interactive content of the activity.
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        voiceSurface=new VoiceSurface(this,new VoiceSurface.Actions(){
            public void down(){onPressDown();}
            public void up(){onPressUp();}
            public void accessibilityActivate(){onAccessibleVoice();}
            public void help(){announce(contextHelp());}
            public void manual(){guarded(()->showManualEntry());}
        });
        frame.addView(voiceSurface,new FrameLayout.LayoutParams(-1,-1));
        setContentView(frame);
        input=new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        if(state!=null)input.setText(state.getString("draft",""));
        refreshHome();
        tts=new TextToSpeech(this,code->{
            if(code!=TextToSpeech.SUCCESS||tts==null){
                runOnUiThread(()->{if(introPending)showIntro();});return;
            }
            // Never select a voice that declares a network requirement.
            if(tts.getVoices()!=null)for(Voice voice:tts.getVoices())
                if("es".equals(voice.getLocale().getLanguage())&&!voice.isNetworkConnectionRequired()) {
                    ttsReady=tts.setVoice(voice)==TextToSpeech.SUCCESS;break;
                }
            runOnUiThread(()->{if(introPending)showIntro();});
        });
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size) {
        TextView view=new TextView(this);view.setText(value);view.setTextColor(Color.WHITE);
        view.setTextSize(size);view.setPadding(0,0,0,dp(12));root.addView(view);return view;
    }
    private TextView label(String title,int size,int color){
        TextView text=new TextView(this);text.setText(title);text.setTextSize(size);
        text.setTextColor(color);return text;
    }
    private LinearLayout card(int bg,int stroke,int radius){
        LinearLayout block=new LinearLayout(this);block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(16),dp(14),dp(16),dp(14));
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setColor(bg);shape.setCornerRadius(dp(radius));shape.setStroke(dp(1),stroke);
        block.setBackground(shape);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));
        root.addView(block,lp);return block;
    }
    private Button largeAction(String title,int bg,int border){
        Button button=new Button(this);button.setText(title);button.setTextSize(18);
        button.setTypeface(null,Typeface.BOLD);button.setTextColor(Color.WHITE);
        button.setAllCaps(false);button.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setColor(bg);shape.setCornerRadius(dp(16));shape.setStroke(dp(2),border);
        button.setBackground(shape);button.setMinHeight(dp(104));
        return button;
    }
    private Button smallAction(String title,int bg){
        Button button=new Button(this);button.setText(title);button.setAllCaps(false);
        button.setTextSize(16);button.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setColor(bg);shape.setCornerRadius(dp(12));shape.setStroke(dp(1),0xFF718096);
        button.setBackground(shape);button.setMinHeight(dp(54));return button;
    }
    private void guarded(Runnable action){
        if(busy||snapshot==null){announce("Esperá a que termine la operación.");return;}
        if(listening){announce("Detené la escucha antes de usar otra opción.");return;}
        action.run();
    }
    private void refreshHome(){
        if(phaseLabel==null||status==null)return;
        String result=snapshot==null?"":""+snapshot[2];
        String phase=BlindFirstUi.phase(result,busy,listening,recovery);
        phaseLabel.setText(phase);
        phaseLabel.setTextColor(BlindFirstUi.urgent(phase)?0xFFFBBF24:0xFF86E6A8);
        if(snapshot!=null){
            status.setText(stateText());
            if(historyPreview!=null)historyPreview.setText(BlindFirstUi.preview(snapshot[5]));
            if(board!=null)board.update(snapshot[4]);
            if(voiceSurface!=null)voiceSurface.setContentDescription(
                "Ajedrez por voz. "+stateText()+". Mantené pulsado para hablar; con TalkBack, doble toque para escuchar. Decí ayuda para comandos. " +
                (pendingMove.isEmpty()?"":"Propuesta pendiente, decí confirmar o cancelar."));
        }
        if(voiceButton!=null){
            voiceButton.setText(listening?"■  Detener\nescucha":"🎙  Escuchar");
            voiceButton.setContentDescription(listening?"Detener escucha de voz":"Escuchar movimiento de ajedrez");
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
            bg.setColor(listening?0xFF7F1D1D:0xFF14532D);
            bg.setCornerRadius(dp(16));bg.setStroke(dp(2),listening?0xFFEF4444:0xFF22C55E);
            voiceButton.setBackground(bg);
        }
        if(microphoneIndicator!=null&&!listening)
            microphoneIndicator.setText("MANTENÉ PULSADO PARA HABLAR · SOLTÁ PARA ENVIAR");
    }
    private void onPressDown(){
        if(listening){cancelSpeech();announce("Escucha cancelada.");return;}
        pressHeld=true;pressStarted=android.os.SystemClock.uptimeMillis();
        guarded(()->requestVoice(true));
    }
    private void onPressUp(){
        if(!pressHeld)return;pressHeld=false;
        if(!listening||!holdRequested)return;
        // A short accidental tap becomes a normal one-shot listening session.
        if(!VoiceUxPolicy.shouldStopOnRelease(android.os.SystemClock.uptimeMillis()-pressStarted))return;
        if(!speechReady){stopAfterReady=true;return;}
        stopVoiceInput();
    }
    private void onAccessibleVoice(){
        if(listening){stopVoiceInput();return;}
        guarded(()->requestVoice(false));
    }
    private void stopVoiceInput(){
        if(!listening||stopSent||recognizer==null)return;
        stopSent=true;
        try{recognizer.stopListening();}catch(RuntimeException failure){cancelSpeech();announce("Error cerrando escucha. Volvé a intentarlo.");}
    }
    private String introduction(){
        return "Bienvenida a VKRT Ajedrez por Voz. El tablero es visual, pero no necesitás verlo ni tocar casillas. " +
            "Mantené la pantalla pulsada, decí un comando y soltá. Con TalkBack, doble toque para iniciar o detener escucha. " +
            "Decí ayuda para instrucciones. Podés decir e2 e4, opciones, tablero o nueva partida. " +
            "Cada jugada requiere confirmar por voz. "+stateText();
    }
    private void showIntro(){
        if(!introPending||snapshot==null||!active)return;
        if(!ttsReady && (accessibility==null||!accessibility.isEnabled())){
            feedback.setText(introduction());return;
        }
        introPending=false;
        getPreferences(MODE_PRIVATE).edit().putBoolean("intro_done",true).apply();
        announce(introduction());
    }
    private String contextHelp(){
        if(!pendingMove.isEmpty())return "Hay una jugada pendiente: "+spokenMove(pendingMove)+". Decí confirmar o cancelar.";
        if(!pendingAction.isEmpty())return "Hay una acción pendiente. Decí confirmar o cancelar.";
        if(!selectedOrigin.isEmpty())return "Origen "+selectedOrigin+". Decí destino seguido de la casilla, por ejemplo destino e4. O decí cancelar.";
        return "Mantené pulsado para hablar y soltá para enviar. Decí una jugada como e2 e4, " +
            "o peón de e2 a e4. También opciones, opción uno, siguientes, anteriores, casilla e2, " +
            "tablero, fila dos, estado, historial, sugerencia, nueva partida, deshacer, " +
            "presentación, diagnóstico de voz y revocar consentimiento. Para mover, siempre confirmás o cancelás. " +
            "Si el micrófono no funciona, con TalkBack abrí las acciones de accesibilidad de esta superficie " +
            "y elegí ingresar jugada con teclado.";
    }
    private String readRow(int rank){
        String[] list=squares();StringBuilder b=new StringBuilder("Fila "+rank+". ");
        for(int f=0;f<8;f++){String sq="abcdefgh".substring(f,f+1)+rank;
            for(String item:list)if(item.startsWith(sq+":")){b.append(item).append(". ");break;}}
        return b.toString();
    }
    private void readOptions(){
        String[] moves=legal();if(moves.length==0){announce("No hay jugadas legales.");return;}
        int pageSize=6,start=choicePage*pageSize;
        if(start>=moves.length){choicePage=0;start=0;}
        StringBuilder b=new StringBuilder("Jugadas disponibles, página "+(choicePage+1)+". ");
        String[] ordinal={"uno","dos","tres","cuatro","cinco","seis"};
        for(int i=start;i<Math.min(start+pageSize,moves.length);i++)b.append("Opción ").append(ordinal[i-start]).append(": ").append(spokenMove(moves[i])).append(". ");
        b.append("Decí opción y número.");if(start+pageSize<moves.length)b.append(" Hay más: decí siguientes.");
        announce(b.toString());
    }
    private void askAction(String action,String message){
        pendingMove="";pendingBase="";pendingAction=action;
        announce(message+" Decí confirmar o cancelar.");
    }
    private void handleVoice(String transcript){
        if(!active||snapshot==null)return;
        String command=VoiceCommands.parse(transcript);
        if(command.equals("HELP")){announce(contextHelp());return;}
        if(command.equals("INTRO")){announce(introduction());return;}
        if(command.equals("REPEAT")){String repeat=lastNarration;announce(repeat.isEmpty()?contextHelp():repeat);return;}
        if(command.equals("CANCEL")){pendingMove="";pendingBase="";pendingAction="";selectedOrigin="";announce("Cancelado. La partida no cambió.");return;}
        if(command.equals("CONFIRM")){
            if(!pendingMove.isEmpty()){
                if(!pendingBase.equals(snapshot[0])){pendingMove="";announce("La posición cambió. Pedí otra jugada.");return;}
                String move=pendingMove;pendingMove="";pendingBase="";request("play",move,true);return;
            }
            String action=pendingAction;pendingAction="";
            if(action.equals("reset")){request("reset","",true);return;}
            if(action.equals("undo")){request("undo","",true);return;}
            announce("No hay ninguna jugada o acción para confirmar.");return;
        }
        if(!pendingMove.isEmpty()||!pendingAction.isEmpty()){
            announce("Tenés una confirmación pendiente. Decí confirmar, cancelar o ayuda.");return;
        }
        if(command.equals("STATE")){announce(stateText());return;}
        if(command.equals("BOARD")){announce(boardText());return;}
        if(command.equals("HISTORY")){announce(snapshot[5].isEmpty()?"Sin movimientos todavía.":snapshot[5]);return;}
        if(command.equals("NEW")){askAction("reset","¿Empezar nueva partida y reemplazar la partida guardada?");return;}
        if(command.equals("UNDO")){askAction("undo","¿Deshacer la última jugada?");return;}
        if(command.equals("ENGINE")){announce(stockfishStatus());return;}
        if(command.equals("SUGGEST")){suggestStockfish();return;}
        if(command.equals("BASIC")){request("simple","",false);return;}
        if(command.equals("DIAGNOSTICS")){announce(voiceDiagnosticText());return;}
        if(command.equals("REVOKE")){
            getPreferences(MODE_PRIVATE).edit().remove("remote_voice_consent").apply();
            announce("Consentimiento de reconocimiento externo revocado. Se volverá a solicitar antes de usarlo.");return;
        }
        if(command.startsWith("LEVEL:")){
            stockfishElo=Integer.parseInt(command.substring(6));saveSettings();
            announce("Nivel de Stockfish actualizado. Su fuerza real no está certificada.");return;
        }
        if(command.equals("OPTIONS")){choicePage=0;readOptions();return;}
        if(command.equals("NEXT")){if((choicePage+1)*6<legal().length)choicePage++;readOptions();return;}
        if(command.equals("PREVIOUS")){choicePage=Math.max(0,choicePage-1);readOptions();return;}
        if(command.startsWith("OPTION:")){
            int option=Integer.parseInt(command.substring(7)),i=choicePage*6+option-1;
            String[] moves=legal();
            if(i>=0&&i<moves.length&&option<=6){request("propose",moves[i],false);return;}
            announce("Esa opción no está disponible. Decí opciones.");return;
        }
        if(command.startsWith("ROW:")){announce(readRow(Integer.parseInt(command.substring(4))));return;}
        if(command.startsWith("SQUARE:")||command.startsWith("DEST:")){
            String square=command.substring(command.indexOf(':')+1);
            if(!selectedOrigin.isEmpty()&&(command.startsWith("DEST:")||command.startsWith("SQUARE:"))){
                String move=selectedOrigin+square;selectedOrigin="";proposeText(move);return;
            }
            if(command.startsWith("DEST:")){announce("Primero indicá origen, por ejemplo origen e2.");return;}
            selectedOrigin=square;
            announce("Origen "+square+". "+squareDescription(square)+". Decí destino y casilla. Ejemplo: destino e4.");return;
        }
        if(command.startsWith("MOVE:")){
            String value=command.substring(5);
            if(value.equals("castle_k")||value.equals("castle_q")){
                proposeText(value.equals("castle_k")?"enroque corto":"enroque largo");return;
            }
            selectedOrigin="";proposeText(value);return;
        }
        announce("No entendí ese comando. Decí ayuda, opciones o una jugada como e2 e4.");
    }
    private String squareDescription(String square){
        for(String entry:squares())if(entry.startsWith(square+":"))return entry;
        return "Casilla no encontrada";
    }
    private String voiceDiagnosticText(){
        boolean mic=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        boolean service=SpeechRecognizer.isRecognitionAvailable(this);
        boolean local=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        return "Permiso de micrófono: "+(mic?"concedido":"pendiente")+". Servicio de voz: "+(service?"disponible":"no disponible")+
            ". Reconocimiento local: "+(local?"disponible":"no disponible")+
            ". El servicio no local puede enviar audio a terceros; se solicita consentimiento una vez.";
    }
    private void showManualEntry(){
        EditText editor=new EditText(this);
        editor.setSingleLine(true);editor.setText(input.getText());editor.setTextSize(20);
        editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        editor.setHint("e2 e4");
        editor.setContentDescription("Ingresar movimiento en coordenadas, ejemplo e2 e4");
        closeDialog();
        dialog=new AlertDialog.Builder(this).setTitle("Ingresar jugada")
            .setMessage("Escribí origen y destino, por ejemplo e2 e4. La jugada se revisará antes de aplicarse.")
            .setView(editor)
            .setPositiveButton("Revisar jugada",(d,w)->{
                String typed=editor.getText().toString();input.setText(typed);proposeText(typed);
            }).setNegativeButton("Cancelar",null).create();dialog.show();
    }
    private void showActions(){
        showList("Opciones de ajedrez",new String[]{
            "Ingresar jugada sin micrófono", "Explorar las 64 casillas", "Elegir movimiento legal",
            "Leer tablero", "Historial completo y PGN", "Motores y análisis",
            "Herramientas de partida", "Voz, narración y ayuda"},i->{
            switch(i){
                case 0:showManualEntry();break;
                case 1:showList("Explorar casillas",squares(),this::selectSquare);break;
                case 2:showLegal();break;
                case 3:showNarration("Tablero",boardText());break;
                case 4:showList("Historial",new String[]{"Leer historial", "Ver y copiar PGN"},v->{
                    if(v==0)showNarration("Historial",snapshot[5].isEmpty()?"Sin movimientos.":snapshot[5]);
                    else showNarration("PGN",snapshot[6]);});break;
                case 5:showEngines();break;
                case 6:showGameTools();break;
                case 7:showAccessibilityTools();break;
            }
        });
    }
    private void showEngines(){
        showList("Motores y análisis",new String[]{"Stockfish: proponer jugada",
            "Cambiar nivel Stockfish", "Cambiar tiempo de análisis", "Comprobar Stockfish", "Rival básico VKRT"},i->{
            switch(i){
                case 0:suggestStockfish();break;
                case 1:showList("Nivel de Stockfish",new String[]{"1320 aproximado","1500 aproximado","1900 aproximado","2400 aproximado","Máxima fuerza"},v->{
                    stockfishElo=new int[]{1320,1500,1900,2400,0}[v];saveSettings();announce("Nivel elegido. La fuerza real no está certificada.");});break;
                case 2:showList("Tiempo de cálculo",new String[]{"300 ms","650 ms","1500 ms","3000 ms"},v->{
                    stockfishTimeMs=new int[]{300,650,1500,3000}[v];saveSettings();announce("Tiempo actualizado.");});break;
                case 3:announce(stockfishStatus());break;
                case 4:request("simple","",false);break;
            }
        });
    }
    private void showGameTools(){
        showList("Herramientas de partida",new String[]{"Deshacer",
            "Reclamar tablas", "Nueva partida", "Repetir estado y turno"},i->{
            switch(i){
                case 0:confirm("Deshacer","Restaurar el estado anterior.",()->request("undo","",true));break;
                case 1:if(!"1".equals(snapshot[8]))announce("No se pueden reclamar tablas en esta posición.");
                    else confirm("Reclamar tablas","Finalizar por cincuenta movimientos o triple repetición.",()->request("claim","",true));break;
                case 2:confirm("Nueva partida","La partida actual será reemplazada.",()->request("reset","",true));break;
                case 3:announce(stateText());break;
            }
        });
    }
    private void showAccessibilityTools(){
        showList("Accesibilidad y voz",new String[]{"Cómo jugar sin micrófono",
            "Diagnóstico de voz", "Repetir última narración", "Volumen de voz", "Velocidad de voz"},i->{
            switch(i){
                case 0:showNarration("Ayuda", "La pantalla mantiene dos botones principales: nueva partida y escuchar. " +
                    "En Más opciones podés ingresar e2 e4, explorar casillas, seleccionar jugadas legales o usar Stockfish. " +
                    "Siempre revisá antes de confirmar. Si TalkBack está activo, la aplicación no reproduce voz propia.");break;
                case 1:showVoiceDiagnostics();break;
                case 2:announce(lastNarration);break;
                case 3:showList("Volumen de aplicación",new String[]{"Silencio","Bajo","Medio","Alto"},v->{
                    speechVolume=new float[]{0f,0.3f,0.6f,1f}[v];saveSettings();announce("Volumen actualizado. TalkBack tiene volumen propio.");});break;
                case 4:showList("Velocidad",new String[]{"Lenta","Normal","Rápida"},v->{
                    speechRate=new float[]{0.75f,1f,1.3f}[v];saveSettings();announce("Velocidad actualizada.");});break;
            }
        });
    }
    private void showVoiceDiagnostics(){showNarration("Estado de voz",voiceDiagnosticText());}
    private void saveSettings() {
        getPreferences(MODE_PRIVATE).edit().putFloat("rate",speechRate).putFloat("volume",speechVolume)
            .putInt("stockfish_elo",stockfishElo).putInt("stockfish_ms",stockfishTimeMs).apply();
    }
    private interface Selection {void choose(int index);}
    private void showList(String title,String[] items,Selection select) {
        closeDialog();
        dialog=new AlertDialog.Builder(this).setTitle(title).setItems(items,(d,i)->select.choose(i))
            .setNegativeButton("Cerrar",null).create();dialog.show();
    }
    private void closeDialog() {if(dialog!=null){dialog.dismiss();dialog=null;}}
    private void confirm(String title,String description,Runnable yes) {
        closeDialog();dialog=new AlertDialog.Builder(this).setTitle(title).setMessage(description)
            .setPositiveButton("Confirmar",(d,w)->yes.run()).setNegativeButton("Cancelar",(d,w)->announce("Cancelado.")).create();
        dialog.show();
    }
    private String stateText() {
        if(snapshot==null)return "Cargando.";
        String side=snapshot[1].contains(" w ")?"blancas":"negras",state;
        switch(snapshot[2]) {
            case "CHECKMATE":state="Jaque mate. Ganan las "+(side.equals("blancas")?"negras":"blancas")+". Partida terminada.";break;
            case "STALEMATE":state="Tablas por ahogado. Partida terminada.";break;
            case "DRAW_DEAD":state="Tablas por material sin mate posible. Partida terminada.";break;
            case "DRAW_75_MOVE":state="Tablas por setenta y cinco movimientos. Partida terminada.";break;
            case "DRAW_5_REPETITION":state="Tablas por cinco repeticiones. Partida terminada.";break;
            case "DRAW_CLAIMED":state="Tablas reclamadas. Partida terminada.";break;
            case "CHECK":state="Turno de "+side+". Jaque.";break;
            default:state="Turno de "+side+". En juego.";
        }
        if("1".equals(snapshot[8]))state+=" Se pueden reclamar tablas.";
        return state;
    }
    private void announce(String message) {
        if(!active||message==null||message.isEmpty())return;
        if(listening)cancelSpeech();
        lastNarration=message;
        // Exactly one route: accessible live region OR app TTS.
        feedback.setText(message);
        if(tts!=null)tts.stop();
        boolean screenReader=accessibility!=null&&accessibility.isEnabled();
        if(screenReader&&voiceSurface!=null)voiceSurface.announceForAccessibility(message);
        if(!screenReader&&!listening&&ttsReady&&tts!=null) {
            tts.setSpeechRate(speechRate);Bundle params=new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,speechVolume);
            tts.speak(message.substring(0,Math.min(message.length(),TextToSpeech.getMaxSpeechInputLength())),
                TextToSpeech.QUEUE_FLUSH,params,"chess");
        }
    }
    private String piece(char p) {
        if(p=='.')return "vacía";
        String name;
        switch(Character.toLowerCase(p)) {
            case 'k':name="rey";break;case 'q':name="dama";break;case 'r':name="torre";break;
            case 'b':name="alfil";break;case 'n':name="caballo";break;default:name="peón";
        }
        return name+" de las "+(Character.isUpperCase(p)?"blancas":"negras");
    }
    private String[] squares() {
        String[] raw=snapshot[4].split("\n"),labels=new String[64];
        for(int i=0;i<64;i++)labels[i]=raw[i].substring(0,2)+": "+piece(raw[i].charAt(3));
        return labels;
    }
    private String boardText() {return String.join(". ",squares());}
    private void selectSquare(int index) {
        String square=snapshot[4].split("\n")[index].substring(0,2);
        String old=input.getText().toString().trim();
        if(old.matches("[a-h][1-8]")) {input.setText(old+" "+square);proposeText(input.getText().toString());}
        else {input.setText(square);announce(squares()[index]+". Origen elegido. Explorá otra casilla para elegir destino.");}
    }
    private String[] legal() {return snapshot[3].isEmpty()?new String[0]:snapshot[3].split("\n");}
    private void showLegal() {
        String[] moves=legal();
        if(moves.length==0){announce("La partida terminó; no hay jugadas disponibles.");return;}
        String[] labels=new String[moves.length];for(int i=0;i<moves.length;i++)labels[i]=spokenMove(moves[i]);
        showList("Elegir jugada legal para revisar",labels,i->request("propose",moves[i],false));
    }
    private String spokenMove(String m) {
        String s="De "+m.substring(0,2)+" a "+m.substring(2,4);
        if(m.length()==5) {
            String p=m.substring(4);
            s+=", promoción a "+(p.equals("q")?"dama":p.equals("r")?"torre":p.equals("b")?"alfil":"caballo");
        }
        return s;
    }
    private void proposeText(String raw) {
        String m=MoveGrammar.parse(raw);
        if(m==null){announce("No se entendió una jugada. Usá coordenadas como e2 e4.");return;}
        if(m.equals("cancel")){closeDialog();announce("Cancelado.");return;}
        if(m.startsWith("castle_"))m=(snapshot[1].contains(" w ")?"e1":"e8")+
            (m.endsWith("k")?(snapshot[1].contains(" w ")?"g1":"g8"):(snapshot[1].contains(" w ")?"c1":"c8"));
        final String move=m;
        if(m.length()==4) {
            List<String> promotions=new ArrayList<>();
            for(String candidate:legal())if(candidate.length()==5&&candidate.startsWith(move))promotions.add(candidate);
            if(!promotions.isEmpty()) {
                String[] labels=new String[promotions.size()];
                for(int i=0;i<labels.length;i++)labels[i]=spokenMove(promotions.get(i));
                showList("Elegir promoción",labels,i->request("propose",promotions.get(i),false));return;
            }
        }
        request("propose",move,false);
    }
    private void showProposal(String[] proposed) {
        String[] parts=proposed[7].split(" ",2);if(parts.length!=2){announce("Propuesta inválida.");return;}
        pendingMove=parts[0];pendingBase=snapshot[0];pendingAction="";
        String detail=spokenMove(pendingMove)+". Notación "+parts[1]+".";
        if(parts[1].endsWith("#"))detail+=" Jaque mate.";
        else if(parts[1].endsWith("+"))detail+=" Jaque.";
        announce("Propuesta: "+detail+" Decí confirmar o cancelar. La partida todavía no cambió.");
    }
    private void showNarration(String title,String body) {
        closeDialog();
        ScrollView scroll=new ScrollView(this);TextView content=new TextView(this);
        content.setText(body);content.setTextSize(20);content.setPadding(24,16,24,16);
        content.setTextIsSelectable(true);scroll.addView(content);
        dialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setPositiveButton("Leer desde el comienzo",(d,w)->announce(body))
            .setNegativeButton("Cerrar",null).create();dialog.show();
    }
    private File stockfishExecutable() {
        return new File(getApplicationInfo().nativeLibraryDir,"libstockfish.so");
    }
    private String stockfishStatus(){
        File binary=stockfishExecutable();
        return binary.isFile()&&binary.canExecute()
            ? "Stockfish está empaquetado. Aún requiere prueba real en este dispositivo. El núcleo VKRT sigue controlando cada jugada."
            : "Stockfish no está incluido en esta edición. El motor de ajedrez VKRT sigue funcionando sin conexión.";
    }
    private void suggestStockfish(){
        if(!"ONGOING".equals(snapshot[2])&&!"CHECK".equals(snapshot[2])){
            announce("La partida terminó. No se puede pedir una nueva jugada.");return;
        }
        final File binary=stockfishExecutable();
        if(!binary.isFile()||!binary.canExecute()){announce(stockfishStatus());return;}
        busy=true;cancelSpeech();refreshHome();
        final int ticket=generation;final String old=snapshot[0];final String fen=snapshot[1];
        final int elo=stockfishElo,ms=stockfishTimeMs;
        WORKER.execute(()->{
            try {
                StockfishUci.Result best=StockfishUci.analyze(binary,fen,elo,ms);
                // Always re-adjudicate with the C++20 game. Engine cannot mutate state.
                String[] proposed=nativeRequest(old,"propose",best.move);
                runOnUiThread(()->{
                    if(!active||ticket!=generation)return;
                    busy=false;refreshHome();
                    if(snapshot==null||!old.equals(snapshot[0])){
                        announce("La posición cambió. Descartada la sugerencia antigua.");return;
                    }
                    announce("Stockfish propone "+spokenMove(best.move)+". Revisá antes de confirmar.");
                    showProposal(proposed);
                });
            } catch(Exception|LinkageError error){
                runOnUiThread(()->{
                    if(!active||ticket!=generation)return;
                    busy=false;refreshHome();announce("Stockfish no pudo analizar esta posición. No se movió ninguna pieza. Podés usar el rival simple o jugar normalmente.");
                });
            }
        });
    }
    private void request(String action,String argument,boolean mutation) {
        if(!active||busy||snapshot==null)return;
        if(recovery&&!action.equals("reset")) {announce("El archivo guardado no es válido. Elegí Nueva partida para empezar; el archivo se conserva hasta confirmar.");return;}
        busy=true;cancelSpeech();refreshHome();
        final int ticket=generation;final String old=snapshot[0];
        WORKER.execute(()->{
            try {
                String[] result=nativeRequest(action.equals("reset")?"":old,action.equals("reset")?"snapshot":action,argument);
                if(mutation)writeSave(result[0]);
                runOnUiThread(()->{
                    if(!active||ticket!=generation)return;busy=false;refreshHome();
                    if(action.equals("propose")||action.equals("simple")) {showProposal(result);return;}
                    snapshot=result;recovery=false;pendingMove="";pendingBase="";pendingAction="";selectedOrigin="";choicePage=0;input.setText("");status.setText(stateText());refreshHome();
                    refreshHome();root.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    announce((action.equals("play")?spokenMove(argument)+". SAN "+result[7].split(" ",2)[1]+". ":action.equals("undo")?"Deshecho. ":"")+stateText());
                });
            } catch(Exception|LinkageError error) {
                runOnUiThread(()->{
                    if(!active||ticket!=generation)return;busy=false;refreshHome();
                    announce("No se aplicó la acción. Revisá la legalidad, el final de partida, el límite de 2048 jugadas o el almacenamiento. Podés volver a intentar.");
                });
            }
        });
    }
    private String readSave() throws Exception {
        if(!saveFile.getBaseFile().exists()&&!new File(saveFile.getBaseFile()+".bak").exists())return "";
        try(DataInputStream stream=new DataInputStream(saveFile.openRead())) {
            if(stream.readInt()!=0x564b5231)throw new IOException("header");
            int length=stream.readInt();if(length<1||length>MAX_SAVE)throw new IOException("size");
            byte[] expected=new byte[32],data=new byte[length];stream.readFully(expected);stream.readFully(data);
            if(stream.read()!=-1||!MessageDigest.isEqual(expected,MessageDigest.getInstance("SHA-256").digest(data)))throw new IOException("checksum");
            return new String(data,StandardCharsets.US_ASCII);
        }
    }
    private void writeSave(String saved) throws Exception {
        byte[] bytes=saved.getBytes(StandardCharsets.US_ASCII);
        if(bytes.length>MAX_SAVE)throw new IOException("size");
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        FileOutputStream stream=null;
        try {
            stream=saveFile.startWrite();DataOutputStream data=new DataOutputStream(stream);
            data.writeInt(0x564b5231);data.writeInt(bytes.length);data.write(digest);data.write(bytes);data.flush();
            stream.getFD().sync();saveFile.finishWrite(stream);
        } catch(Exception error) {if(stream!=null)saveFile.failWrite(stream);throw error;}
    }
    private void load() {
        busy=true;refreshHome();final int ticket=++generation;
        WORKER.execute(()->{
            String[] loaded;boolean damaged=false;
            try {loaded=nativeRequest(readSave(),"snapshot","");}
            catch(Exception|LinkageError error) {
                damaged=true;
                try {loaded=nativeRequest("","snapshot","");}
                catch(Exception|LinkageError fatal) {
                    runOnUiThread(()->{if(active&&ticket==generation){busy=true;announce("No se pudo iniciar el motor nativo.");}});return;
                }
            }
            final String[] result=loaded;final boolean invalid=damaged;
            runOnUiThread(()->{
                if(!active||ticket!=generation)return;snapshot=result;busy=false;recovery=invalid;status.setText(stateText());refreshHome();
                if(invalid)announce("No se pudo validar la partida guardada. Se conserva hasta confirmar Nueva partida.");
                else if(!getPreferences(MODE_PRIVATE).getBoolean("intro_done",false)){
                    introPending=true;feedback.setText(introduction());
                    if(ttsReady||(accessibility!=null&&accessibility.isEnabled()))showIntro();
                } else announce(stateText()+" Partida local lista. Decí ayuda cuando quieras.");
            });
        });
    }
    private void requestVoice() {requestVoice(false);}
    private void requestVoice(boolean fromHold) {
        // Voice must remain available during corrupted-save recovery, especially to say Nueva partida.
        if(tts!=null)tts.stop();
        boolean local=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        if(!local&&!SpeechRecognizer.isRecognitionAvailable(this)){
            announce("No hay reconocimiento de voz disponible. Con TalkBack, usá la acción ingresar jugada con teclado.");return;
        }
        if(!local&&!getPreferences(MODE_PRIVATE).getBoolean("remote_voice_consent",false)){
            // The OS service may send audio to its provider. Consent is requested ONCE, not per utterance.
            confirm("Consentimiento de reconocimiento",
                "No hay reconocedor exclusivamente local. El servicio Android puede enviar audio a un proveedor externo. " +
                "La aplicación no guarda grabaciones. ¿Autorizar este servicio para futuras escuchas?",
                ()->{getPreferences(MODE_PRIVATE).edit().putBoolean("remote_voice_consent",true).apply();permission(false,false);});
            return;
        }
        permission(local,fromHold);
    }
    private void permission(boolean local,boolean fromHold) {
        permissionOnDevice=local;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            if(getPreferences(MODE_PRIVATE).getBoolean("mic_denied",false)){
                announce("Permiso de micrófono denegado anteriormente. Activá Micrófono en ajustes de Android para esta aplicación. Con TalkBack podés ingresar por teclado.");return;
            }
            pendingPermission=true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);
        } else startVoice(local,fromHold);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==MIC_REQUEST&&active&&pendingPermission) {
            pendingPermission=false;
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){
                getPreferences(MODE_PRIVATE).edit().putBoolean("mic_denied",false).apply();
                startVoice(permissionOnDevice,false);
            } else {
                getPreferences(MODE_PRIVATE).edit().putBoolean("mic_denied",true).apply();
                announce("Micrófono denegado. Podés habilitarlo en ajustes de Android o ingresar jugada con teclado usando TalkBack.");
            }
        }
    }
    private void startVoice(boolean local,boolean fromHold) {
        if(!active||busy)return;
        cancelSpeech();if(tts!=null)tts.stop();
        holdRequested=fromHold;stopAfterReady=false;stopSent=false;speechReady=false;
        final int ticket=++voiceGeneration;
        try {
            recognizer=local&&Build.VERSION.SDK_INT>=31?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);
            listening=true;refreshHome();
            microphoneIndicator.setText("PREPARANDO MICRÓFONO…");
            recognizer.setRecognitionListener(new RecognitionListener() {
                private boolean current(){return active&&listening&&ticket==voiceGeneration;}
                public void onReadyForSpeech(Bundle p){if(current()){
                    speechReady=true;microphoneIndicator.setText("ESCUCHANDO. HABLÁ AHORA.");
                    voiceSurface.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    if(stopAfterReady)voiceSurface.postDelayed(()->{if(current())stopVoiceInput();},350);
                }}
                public void onBeginningOfSpeech(){}
                public void onRmsChanged(float value){}
                public void onBufferReceived(byte[] bytes){}
                public void onEndOfSpeech(){}
                public void onPartialResults(Bundle values){}
                public void onEvent(int type,Bundle values){}
                public void onError(int error){if(!current())return;cancelSpeech();
                    announce("Falló el reconocimiento de voz, código "+error+". Decí ayuda en el próximo intento o usá entrada por teclado desde TalkBack.");}
                public void onResults(Bundle result){
                    if(!current())return;
                    ArrayList<String> matches=result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    float[] confidence=result.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                    cancelSpeech();
                    if(matches==null||matches.isEmpty()||(confidence!=null&&confidence.length>0&&confidence[0]>=0&&confidence[0]<0.6f)){
                        announce("No pude reconocer la voz con seguridad. Repetí el comando.");return;
                    }
                    handleVoice(matches.get(0));
                }
            });
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-AR");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
            recognizer.startListening(intent);
            voiceSurface.postDelayed(()->{if(listening&&ticket==voiceGeneration){cancelSpeech();announce("Tiempo de escucha agotado. Volvé a mantener pulsado.");}},15000);
        }catch(RuntimeException error){cancelSpeech();announce("No se pudo iniciar el reconocimiento. Revisá el servicio de voz de Android.");}
    }
    private void cancelSpeech() {
        listening=false;holdRequested=false;stopAfterReady=false;stopSent=false;speechReady=false;++voiceGeneration;
        if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(RuntimeException ignored){}recognizer=null;}
        refreshHome();
    }
    @Override protected void onStart(){super.onStart();active=true;load();}
    @Override protected void onStop(){
        active=false;++generation;cancelSpeech();closeDialog();if(tts!=null)tts.stop();super.onStop();
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putString("draft",input.getText().toString());super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){cancelSpeech();if(tts!=null){tts.stop();tts.shutdown();tts=null;}super.onDestroy();}
}
