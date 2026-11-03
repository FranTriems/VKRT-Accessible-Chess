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
    private boolean english=false;
    private String L(String es,String en){return english?en:es;}
    private String languageTag(){return english?"en-US":"es-AR";}
    private void selectOfflineTts(){
        ttsReady=false;
        if(tts==null || tts.getVoices()==null)return;
        Voice fallback=null;
        for(Voice v:tts.getVoices()){
            if(v.isNetworkConnectionRequired() || !v.getLocale().getLanguage().equals(english?"en":"es"))continue;
            if(v.getLocale().toLanguageTag().equalsIgnoreCase(languageTag())){fallback=v;break;}
            if(fallback==null)fallback=v;
        }
        if(fallback!=null)ttsReady=tts.setVoice(fallback)==TextToSpeech.SUCCESS;
    }
    private void changeLanguage(String code){
        if(!"en".equals(code) && !"es".equals(code))return;
        if(listening)cancelSpeech();
        english="en".equals(code);
        getPreferences(MODE_PRIVATE).edit().putString("ui_language",code).apply();
        pendingMove="";pendingBase="";pendingAction="";selectedOrigin="";choicePage=0;
        if(tts!=null){tts.stop();selectOfflineTts();}
        refreshHome();
        announce(L("Idioma cambiado a español. Comandos disponibles: ayuda, opciones, e2 e4 y confirmar. ",
                    "Language changed to English. Available commands: help, legal moves, e2 e4 and confirm. ")+stateText());
    }
    private void showLanguagePicker(){
        // Non-voice fallback for TalkBack; the main screen remains a single voice control.
        showList(L("Elegir idioma","Choose language"),new String[]{"Español (Argentina)","English (United States)"},
                 index->changeLanguage(index==0?"es":"en"));
    }
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
        String savedLanguage=getPreferences(MODE_PRIVATE).getString("ui_language","");
        english="en".equals(savedLanguage) || (savedLanguage.isEmpty() &&
            !getPreferences(MODE_PRIVATE).getBoolean("intro_done",false) &&
            "en".equals(Locale.getDefault().getLanguage()));
        // R34: one interactive voice surface; localized speech and TalkBack. Everything beneath is visual/read-only.
        FrameLayout frame=new FrameLayout(this);
        frame.setBackgroundColor(0xFF030606);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(18));
        frame.addView(root,new FrameLayout.LayoutParams(-1,-1));
        TextView heading=text(L("AJEDREZ POR VOZ","VOICE CHESS"),23);
        heading.setTypeface(null,Typeface.BOLD);
        heading.setTextColor(0xFFEAF4EC);
        heading.setGravity(Gravity.CENTER);
        TextView subtitle=text(L("VKRT · BLIND CHESS","VKRT · BLIND CHESS"),12);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(0xFF7DAF91);
        board=new ChessBoardView(this);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,0,1.0f);
        bp.setMargins(0,dp(5),0,dp(12));
        root.addView(board,bp);
        phaseLabel=label(L("CARGANDO","LOADING"),21,0xFF8CE8A6);
        phaseLabel.setTypeface(null,Typeface.BOLD);
        phaseLabel.setGravity(Gravity.CENTER);
        root.addView(phaseLabel);
        status=label(L("Preparando partida","Preparing game"),16,0xFFEAF4EC);
        status.setGravity(Gravity.CENTER);
        root.addView(status);
        feedback=label(L("Bienvenido. Mantené pulsado para hablar.","Welcome. Press and hold to speak."),17,0xFFF4F1DF);
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(dp(4),dp(13),dp(4),dp(12));
        feedback.setMaxLines(4);
        feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(feedback);
        historyPreview=label(L("Sin movimientos todavía","No moves yet"),13,0xFFAACAB5);
        historyPreview.setMaxLines(2);
        historyPreview.setGravity(Gravity.CENTER);
        root.addView(historyPreview);
        microphoneIndicator=label(L("MANTENÉ PULSADO PARA HABLAR · SOLTÁ PARA ENVIAR","PRESS AND HOLD TO TALK · RELEASE TO SEND"),13,0xFF88EBAA);
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
            public void language(){if(!listening)showLanguagePicker();}
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
            // Only use installed, non-network TTS voices matching the selected language.
            selectOfflineTts();
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
        if(busy||snapshot==null){announce(L("Esperá a que termine la operación.","Please wait until the operation finishes."));return;}
        if(listening){announce(L("Detené la escucha antes de usar otra opción.","Stop listening before using another action."));return;}
        action.run();
    }
    private void refreshHome(){
        if(phaseLabel==null||status==null)return;
        String result=snapshot==null?"":""+snapshot[2];
        String phase=BlindFirstUi.phase(result,busy,listening,recovery,english);
        phaseLabel.setText(phase);
        phaseLabel.setTextColor(BlindFirstUi.urgent(phase)?0xFFFBBF24:0xFF86E6A8);
        if(voiceSurface!=null)voiceSurface.setLanguage(english);
        if(snapshot!=null){
            status.setText(stateText());
            if(historyPreview!=null)historyPreview.setText(BlindFirstUi.preview(snapshot[5],english));
            if(board!=null)board.update(snapshot[4]);
            if(voiceSurface!=null)voiceSurface.setContentDescription(
                L("Ajedrez por voz. ","Voice chess. ")+stateText()+L(". Mantené pulsado para hablar; con TalkBack, doble toque para escuchar. Decí ayuda para comandos. ",". Hold the screen to talk; with TalkBack, double tap to listen. Say help for commands. ") +
                (pendingMove.isEmpty()?"":L("Propuesta pendiente, decí confirmar o cancelar.","Move pending. Say confirm or cancel.")));
        }
        if(voiceButton!=null){
            voiceButton.setText(listening?L("■  Detener\nescucha","■  Stop\nlistening"):L("🎙  Escuchar","🎙  Listen"));
            voiceButton.setContentDescription(listening?L("Detener escucha de voz","Stop voice recognition"):L("Escuchar movimiento de ajedrez","Listen for a chess move"));
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
            bg.setColor(listening?0xFF7F1D1D:0xFF14532D);
            bg.setCornerRadius(dp(16));bg.setStroke(dp(2),listening?0xFFEF4444:0xFF22C55E);
            voiceButton.setBackground(bg);
        }
        if(microphoneIndicator!=null&&!listening)
            microphoneIndicator.setText(L("MANTENÉ PULSADO PARA HABLAR · SOLTÁ PARA ENVIAR","PRESS AND HOLD TO TALK · RELEASE TO SEND"));
    }
    private void onPressDown(){
        if(listening){cancelSpeech();announce(L("Escucha cancelada.","Listening canceled."));return;}
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
        try{recognizer.stopListening();}catch(RuntimeException failure){cancelSpeech();announce(L("Error cerrando escucha. Volvé a intentarlo.","Error stopping recognition. Please try again."));}
    }
    private String introduction(){
        return L("Bienvenida a VKRT Ajedrez por Voz. El tablero es visual, pero no necesitás verlo ni tocar casillas. ","Welcome to VKRT Voice Chess. You can see the board, but you never need to look at it or touch a square. ") +
            L("Mantené la pantalla pulsada, decí un comando y soltá. Con TalkBack, doble toque para iniciar o detener escucha. ","Press and hold anywhere to speak, then release. With TalkBack, double tap to start or stop listening. ") +
            L("Decí ayuda para instrucciones. Podés decir e2 e4, opciones, tablero o nueva partida. ","Say help for instructions. You can say e2 e4, legal moves, board or new game. ") +
            L("Cada jugada requiere confirmar por voz. Decí idioma para elegir español o inglés. ","Every move requires spoken confirmation. Say language to choose English or Spanish. ")+stateText();
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
        if(!pendingMove.isEmpty())return L("Hay una jugada pendiente: ","You have a pending move: ")+spokenMove(pendingMove)+L(". Decí confirmar o cancelar.",". Say confirm or cancel.");
        if(!pendingAction.isEmpty())return L("Hay una acción pendiente. Decí confirmar o cancelar.","An action is pending. Say confirm or cancel.");
        if(!selectedOrigin.isEmpty())return L("Origen ","From ")+selectedOrigin+L(". Decí destino seguido de la casilla, por ejemplo destino e4. O decí cancelar.",". Say destination followed by a square, such as destination e4, or cancel.");
        return L("Mantené pulsado para hablar y soltá para enviar. Decí una jugada como e2 e4, ","Press and hold to speak, release to send. Say e2 e4, ") +
            L("o peón de e2 a e4. También opciones, opción uno, siguientes, anteriores, casilla e2, ","or pawn from e2 to e4. Also say legal moves, option one, next, previous, square e2, ") +
            L("tablero, fila dos, estado, historial, sugerencia, nueva partida, deshacer, ","board, rank two, status, history, suggestion, new game, undo, ") +
            L("presentación, diagnóstico de voz y revocar consentimiento. Para mover, siempre confirmás o cancelás. ","introduction, voice diagnostics, revoke consent and language. Always confirm or cancel a proposed move. ") +
            L("Si el micrófono no funciona, con TalkBack abrí las acciones de accesibilidad de esta superficie ","If your microphone does not work, open the accessibility actions of this surface with TalkBack ") +
            L("y elegí ingresar jugada con teclado.","and select enter move with keyboard.");
    }
    private String readRow(int rank){
        String[] list=squares();StringBuilder b=new StringBuilder(L("Fila ","Rank ")+rank+". ");
        for(int f=0;f<8;f++){String sq="abcdefgh".substring(f,f+1)+rank;
            for(String item:list)if(item.startsWith(sq+":")){b.append(item).append(". ");break;}}
        return b.toString();
    }
    private void readOptions(){
        String[] moves=legal();if(moves.length==0){announce(L("No hay jugadas legales.","No legal moves are available."));return;}
        int pageSize=6,start=choicePage*pageSize;
        if(start>=moves.length){choicePage=0;start=0;}
        StringBuilder b=new StringBuilder(L("Jugadas disponibles, página ","Legal moves, page ")+(choicePage+1)+". ");
        String[] ordinal=english?new String[]{"one","two","three","four","five","six"}:new String[]{"uno","dos","tres","cuatro","cinco","seis"};
        for(int i=start;i<Math.min(start+pageSize,moves.length);i++)b.append(L("Opción ","Option ")).append(ordinal[i-start]).append(": ").append(spokenMove(moves[i])).append(". ");
        b.append(L("Decí opción y número.","Say option and a number."));if(start+pageSize<moves.length)b.append(L(" Hay más: decí siguientes."," For more moves, say next."));
        announce(b.toString());
    }
    private void askAction(String action,String message){
        pendingMove="";pendingBase="";pendingAction=action;
        announce(message+L(" Decí confirmar o cancelar."," Say confirm or cancel."));
    }
    private void handleVoice(String transcript){
        if(!active||snapshot==null)return;
        String command=VoiceCommands.parse(transcript,english?"en":"es");
        if(command.equals("LANG_MENU")){showLanguagePicker();return;}
        if(command.equals("LANG:en")){changeLanguage("en");return;}
        if(command.equals("LANG:es")){changeLanguage("es");return;}
        if(command.equals("HELP")){announce(contextHelp());return;}
        if(command.equals("INTRO")){announce(introduction());return;}
        if(command.equals("REPEAT")){String repeat=lastNarration;announce(repeat.isEmpty()?contextHelp():repeat);return;}
        if(command.equals("CANCEL")){pendingMove="";pendingBase="";pendingAction="";selectedOrigin="";announce(L("Cancelado. La partida no cambió.","Canceled. The game was not changed."));return;}
        if(command.equals("CONFIRM")){
            if(!pendingMove.isEmpty()){
                if(!pendingBase.equals(snapshot[0])){pendingMove="";announce(L("La posición cambió. Pedí otra jugada.","The position changed. Please request another move."));return;}
                String move=pendingMove;pendingMove="";pendingBase="";request("play",move,true);return;
            }
            String action=pendingAction;pendingAction="";
            if(action.equals("reset")){request("reset","",true);return;}
            if(action.equals("undo")){request("undo","",true);return;}
            announce(L("No hay ninguna jugada o acción para confirmar.","There is no action or move to confirm."));return;
        }
        if(!pendingMove.isEmpty()||!pendingAction.isEmpty()){
            announce(L("Tenés una confirmación pendiente. Decí confirmar, cancelar o ayuda.","A confirmation is pending. Say confirm, cancel or help."));return;
        }
        if(command.equals("STATE")){announce(stateText());return;}
        if(command.equals("BOARD")){announce(boardText());return;}
        if(command.equals("HISTORY")){announce(snapshot[5].isEmpty()?L("Sin movimientos todavía.","No moves yet."):snapshot[5]);return;}
        if(command.equals("NEW")){askAction("reset",L("¿Empezar nueva partida y reemplazar la partida guardada?","Start a new game and replace the saved game?"));return;}
        if(command.equals("UNDO")){askAction("undo",L("¿Deshacer la última jugada?","Undo the last move?"));return;}
        if(command.equals("ENGINE")){announce(stockfishStatus());return;}
        if(command.equals("SUGGEST")){suggestStockfish();return;}
        if(command.equals("BASIC")){request("simple","",false);return;}
        if(command.equals("DIAGNOSTICS")){announce(voiceDiagnosticText());return;}
        if(command.equals("REVOKE")){
            getPreferences(MODE_PRIVATE).edit().remove("remote_voice_consent").apply();
            announce(L("Consentimiento de reconocimiento externo revocado. Se volverá a solicitar antes de usarlo.","Consent for external recognition revoked. Permission will be requested before using it again."));return;
        }
        if(command.startsWith("LEVEL:")){
            stockfishElo=Integer.parseInt(command.substring(6));saveSettings();
            announce(L("Nivel de Stockfish actualizado. Su fuerza real no está certificada.","Stockfish level updated. Its actual strength is not certified."));return;
        }
        if(command.equals("OPTIONS")){choicePage=0;readOptions();return;}
        if(command.equals("NEXT")){if((choicePage+1)*6<legal().length)choicePage++;readOptions();return;}
        if(command.equals("PREVIOUS")){choicePage=Math.max(0,choicePage-1);readOptions();return;}
        if(command.startsWith("OPTION:")){
            int option=Integer.parseInt(command.substring(7)),i=choicePage*6+option-1;
            String[] moves=legal();
            if(i>=0&&i<moves.length&&option<=6){request("propose",moves[i],false);return;}
            announce(L("Esa opción no está disponible. Decí opciones.","That option is unavailable. Say legal moves."));return;
        }
        if(command.startsWith("ROW:")){announce(readRow(Integer.parseInt(command.substring(4))));return;}
        if(command.startsWith("SQUARE:")||command.startsWith("DEST:")){
            String square=command.substring(command.indexOf(':')+1);
            if(!selectedOrigin.isEmpty()&&(command.startsWith("DEST:")||command.startsWith("SQUARE:"))){
                String move=selectedOrigin+square;selectedOrigin="";proposeText(move);return;
            }
            if(command.startsWith("DEST:")){announce(L("Primero indicá origen, por ejemplo origen e2.","Please give the origin first, for example from e2."));return;}
            selectedOrigin=square;
            announce(L("Origen ","From ")+square+". "+squareDescription(square)+L(". Decí destino y casilla. Ejemplo: destino e4.",". Say destination followed by a square, such as destination e4."));return;
        }
        if(command.startsWith("MOVE:")){
            String value=command.substring(5);
            if(value.equals("castle_k")||value.equals("castle_q")){
                proposeText(value.equals("castle_k")?L("enroque corto","castle kingside"):L("enroque largo","castle queenside"));return;
            }
            selectedOrigin="";proposeText(value);return;
        }
        announce(L("No entendí ese comando. Decí ayuda, opciones o una jugada como e2 e4.","I did not understand. Say help, legal moves, or a move such as e2 e4."));
    }
    private String squareDescription(String square){
        for(String entry:squares())if(entry.startsWith(square+":"))return entry;
        return L("Casilla no encontrada","Square not found");
    }
    private String voiceDiagnosticText(){
        boolean mic=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        boolean service=SpeechRecognizer.isRecognitionAvailable(this);
        boolean local=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        return L("Idioma de voz: ","Speech language: ")+languageTag()+L(". Voz local instalada: ",". Installed offline voice: ")+
            (ttsReady?L("disponible", "available"):L("no disponible", "unavailable")) + ". " +
            L("Permiso de micrófono: ","Microphone permission: ")+(mic?L("concedido","granted"):L("pendiente","pending"))+L(". Servicio de voz: ",". Speech recognition service: ")+(service?L("disponible","available"):L("no disponible","unavailable"))+
            L(". Reconocimiento local: ",". On-device recognition: ")+(local?L("disponible","available"):L("no disponible","unavailable"))+
            L(". El servicio no local puede enviar audio a terceros; se solicita consentimiento una vez.",". Non-local recognition can send audio to the service provider. Consent is requested once.");
    }
    private void showManualEntry(){
        EditText editor=new EditText(this);
        editor.setSingleLine(true);editor.setText(input.getText());editor.setTextSize(20);
        editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        editor.setHint("e2 e4");
        editor.setContentDescription(L("Ingresar movimiento en coordenadas, ejemplo e2 e4","Enter coordinates, for example e2 e4"));
        closeDialog();
        dialog=new AlertDialog.Builder(this).setTitle(L("Ingresar jugada","Enter move"))
            .setMessage(L("Escribí origen y destino, por ejemplo e2 e4. La jugada se revisará antes de aplicarse.","Type from and to squares, for example e2 e4. The move is reviewed before being played."))
            .setView(editor)
            .setPositiveButton(L("Revisar jugada","Review move"),(d,w)->{
                String typed=editor.getText().toString();input.setText(typed);proposeText(typed);
            }).setNegativeButton(L("Cancelar","Cancel"),null).create();dialog.show();
    }
    private void showActions(){
        showList(L("Opciones de ajedrez","Chess options"),new String[]{
            L("Ingresar jugada sin micrófono","Enter move without a microphone"), L("Explorar las 64 casillas","Explore all 64 squares"), L("Elegir movimiento legal","Choose a legal move"),
            L("Leer tablero","Read board"), L("Historial completo y PGN","Complete move history and PGN"), L("Motores y análisis","Engines and analysis"),
            L("Herramientas de partida","Game tools"), L("Voz, narración y ayuda","Speech, narration and help")},i->{
            switch(i){
                case 0:showManualEntry();break;
                case 1:showList(L("Explorar casillas","Explore squares"),squares(),this::selectSquare);break;
                case 2:showLegal();break;
                case 3:showNarration(L("Tablero","Board"),boardText());break;
                case 4:showList(L("Historial","History"),new String[]{L("Leer historial","Read history"), L("Ver y copiar PGN","View and copy PGN")},v->{
                    if(v==0)showNarration(L("Historial","History"),snapshot[5].isEmpty()?L("Sin movimientos.","No moves."):snapshot[5]);
                    else showNarration("PGN",snapshot[6]);});break;
                case 5:showEngines();break;
                case 6:showGameTools();break;
                case 7:showAccessibilityTools();break;
            }
        });
    }
    private void showEngines(){
        showList(L("Motores y análisis","Engines and analysis"),new String[]{L("Stockfish: proponer jugada","Stockfish: suggest move"),
            L("Cambiar nivel Stockfish","Change Stockfish level"), L("Cambiar tiempo de análisis","Change analysis time"), L("Comprobar Stockfish","Check Stockfish"), L("Rival básico VKRT","Basic VKRT opponent")},i->{
            switch(i){
                case 0:suggestStockfish();break;
                case 1:showList(L("Nivel de Stockfish","Stockfish level"),new String[]{L("1320 aproximado","Approximate 1320"),L("1500 aproximado","Approximate 1500"),L("1900 aproximado","Approximate 1900"),L("2400 aproximado","Approximate 2400"),L("Máxima fuerza","Maximum strength")},v->{
                    stockfishElo=new int[]{1320,1500,1900,2400,0}[v];saveSettings();announce(L("Nivel elegido. La fuerza real no está certificada.","Level selected. Actual playing strength is not certified."));});break;
                case 2:showList(L("Tiempo de cálculo","Analysis time"),new String[]{"300 ms","650 ms","1500 ms","3000 ms"},v->{
                    stockfishTimeMs=new int[]{300,650,1500,3000}[v];saveSettings();announce(L("Tiempo actualizado.","Analysis time updated."));});break;
                case 3:announce(stockfishStatus());break;
                case 4:request("simple","",false);break;
            }
        });
    }
    private void showGameTools(){
        showList(L("Herramientas de partida","Game tools"),new String[]{L("Deshacer","Undo"),
            L("Reclamar tablas","Claim draw"), L("Nueva partida","New game"), L("Repetir estado y turno","Repeat status and turn")},i->{
            switch(i){
                case 0:confirm(L("Deshacer","Undo"),L("Restaurar el estado anterior.","Restore the previous state."),()->request("undo","",true));break;
                case 1:if(!"1".equals(snapshot[8]))announce(L("No se pueden reclamar tablas en esta posición.","A draw cannot be claimed from this position."));
                    else confirm(L("Reclamar tablas","Claim draw"),L("Finalizar por cincuenta movimientos o triple repetición.","Claim a draw by fifty moves or threefold repetition."),()->request("claim","",true));break;
                case 2:confirm(L("Nueva partida","New game"),L("La partida actual será reemplazada.","The current game will be replaced."),()->request("reset","",true));break;
                case 3:announce(stateText());break;
            }
        });
    }
    private void showAccessibilityTools(){
        showList(L("Accesibilidad y voz","Accessibility and voice"),new String[]{L("Cómo jugar sin micrófono","How to play without a microphone"),
            L("Diagnóstico de voz","Voice diagnostics"), L("Repetir última narración","Repeat last narration"), L("Volumen de voz","Voice volume"), L("Velocidad de voz","Speech speed"), L("Elegir idioma","Choose language")},i->{
            switch(i){
                case 0:showNarration(L("Ayuda","Help"), L("La pantalla mantiene dos botones principales: nueva partida y escuchar. ","The full screen is a single voice surface. ") +
                    L("En Más opciones podés ingresar e2 e4, explorar casillas, seleccionar jugadas legales o usar Stockfish. ","Use spoken commands to enter e2 e4, explore squares, select legal moves or ask Stockfish. ") +
                    L("Siempre revisá antes de confirmar. Si TalkBack está activo, la aplicación no reproduce voz propia.","Always review before confirming. When TalkBack is active, the app does not play a second voice."));break;
                case 1:showVoiceDiagnostics();break;
                case 2:announce(lastNarration);break;
                case 3:showList(L("Volumen de aplicación","App speech volume"),new String[]{L("Silencio","Silent"),L("Bajo","Low"),L("Medio","Medium"),L("Alto","High")},v->{
                    speechVolume=new float[]{0f,0.3f,0.6f,1f}[v];saveSettings();announce(L("Volumen actualizado. TalkBack tiene volumen propio.","Volume updated. TalkBack has its own speech volume."));});break;
                case 4:showList(L("Velocidad","Speech speed"),new String[]{L("Lenta","Slow"),L("Normal","Normal"),L("Rápida","Fast")},v->{
                    speechRate=new float[]{0.75f,1f,1.3f}[v];saveSettings();announce(L("Velocidad actualizada.","Speech speed updated."));});break;
                case 5:showLanguagePicker();break;
            }
        });
    }
    private void showVoiceDiagnostics(){showNarration(L("Estado de voz","Voice status"),voiceDiagnosticText());}
    private void saveSettings() {
        getPreferences(MODE_PRIVATE).edit().putFloat("rate",speechRate).putFloat("volume",speechVolume)
            .putInt("stockfish_elo",stockfishElo).putInt("stockfish_ms",stockfishTimeMs).apply();
    }
    private interface Selection {void choose(int index);}
    private void showList(String title,String[] items,Selection select) {
        closeDialog();
        dialog=new AlertDialog.Builder(this).setTitle(title).setItems(items,(d,i)->select.choose(i))
            .setNegativeButton(L("Cerrar","Close"),null).create();dialog.show();
    }
    private void closeDialog() {if(dialog!=null){dialog.dismiss();dialog=null;}}
    private void confirm(String title,String description,Runnable yes) {
        closeDialog();dialog=new AlertDialog.Builder(this).setTitle(title).setMessage(description)
            .setPositiveButton(L("Confirmar","Confirm"),(d,w)->yes.run()).setNegativeButton(L("Cancelar","Cancel"),(d,w)->announce(L("Cancelado.","Canceled."))).create();
        dialog.show();
    }
    private String stateText() {
        if(snapshot==null)return L("Cargando.","Loading.");
        boolean white=snapshot[1].contains(" w ");
        String side=L(white?"blancas":"negras",white?"White":"Black");
        String winner=L(white?"negras":"blancas",white?"Black":"White");
        String state;
        switch(snapshot[2]) {
            case "CHECKMATE":state=L("Jaque mate. Ganan las ","Checkmate. ")+winner+L(". Partida terminada."," wins. Game over.");break;
            case "STALEMATE":state=L("Tablas por ahogado. Partida terminada.","Draw by stalemate. Game over.");break;
            case "DRAW_DEAD":state=L("Tablas por material sin mate posible. Partida terminada.","Draw by insufficient mating material. Game over.");break;
            case "DRAW_75_MOVE":state=L("Tablas por setenta y cinco movimientos. Partida terminada.","Draw by the 75-move rule. Game over.");break;
            case "DRAW_5_REPETITION":state=L("Tablas por cinco repeticiones. Partida terminada.","Draw by fivefold repetition. Game over.");break;
            case "DRAW_CLAIMED":state=L("Tablas reclamadas. Partida terminada.","Draw claimed. Game over.");break;
            case "CHECK":state=L("Turno de ","Turn: ")+side+L(". Jaque.",". Check.");break;
            default:state=L("Turno de ","Turn: ")+side+L(". En juego.",". Game in progress.");
        }
        if("1".equals(snapshot[8]))state+=L(" Se pueden reclamar tablas."," A draw may be claimed.");
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
        if(p=='.')return L("vacía","empty");
        String name;
        switch(Character.toLowerCase(p)) {
            case 'k':name=L("rey","king");break;case 'q':name=L("dama","queen");break;
            case 'r':name=L("torre","rook");break;case 'b':name=L("alfil","bishop");break;
            case 'n':name=L("caballo","knight");break;default:name=L("peón","pawn");
        }
        return english?(Character.isUpperCase(p)?"White ":"Black ")+name:
            name+" de las "+(Character.isUpperCase(p)?"blancas":"negras");
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
        else {input.setText(square);announce(squares()[index]+L(". Origen elegido. Explorá otra casilla para elegir destino.",". Origin selected. Choose another square as the destination."));}
    }
    private String[] legal() {return snapshot[3].isEmpty()?new String[0]:snapshot[3].split("\n");}
    private void showLegal() {
        String[] moves=legal();
        if(moves.length==0){announce(L("La partida terminó; no hay jugadas disponibles.","The game has ended; there are no legal moves."));return;}
        String[] labels=new String[moves.length];for(int i=0;i<moves.length;i++)labels[i]=spokenMove(moves[i]);
        showList(L("Elegir jugada legal para revisar","Choose legal move to review"),labels,i->request("propose",moves[i],false));
    }
    private String spokenMove(String m) {
        String s=L("De ","From ")+m.substring(0,2)+L(" a "," to ")+m.substring(2,4);
        if(m.length()==5) {
            String p=m.substring(4);
            s+=L(", promoción a ",", promotion to ")+(p.equals("q")?L("dama","queen"):p.equals("r")?L("torre","rook"):p.equals("b")?L("alfil","bishop"):L("caballo","knight"));
        }
        return s;
    }
    private void proposeText(String raw) {
        String m=english?MoveGrammar.parseEnglish(raw):MoveGrammar.parse(raw);
        if(m==null){announce(L("No se entendió una jugada. Usá coordenadas como e2 e4.","I did not recognize a move. Use coordinates such as e2 e4."));return;}
        if(m.equals("cancel")){closeDialog();announce(L("Cancelado.","Canceled."));return;}
        if(m.startsWith("castle_"))m=(snapshot[1].contains(" w ")?"e1":"e8")+
            (m.endsWith("k")?(snapshot[1].contains(" w ")?"g1":"g8"):(snapshot[1].contains(" w ")?"c1":"c8"));
        final String move=m;
        if(m.length()==4) {
            List<String> promotions=new ArrayList<>();
            for(String candidate:legal())if(candidate.length()==5&&candidate.startsWith(move))promotions.add(candidate);
            if(!promotions.isEmpty()) {
                String[] labels=new String[promotions.size()];
                for(int i=0;i<labels.length;i++)labels[i]=spokenMove(promotions.get(i));
                showList(L("Elegir promoción","Choose promotion"),labels,i->request("propose",promotions.get(i),false));return;
            }
        }
        request("propose",move,false);
    }
    private void showProposal(String[] proposed) {
        String[] parts=proposed[7].split(" ",2);if(parts.length!=2){announce(L("Propuesta inválida.","Invalid proposal."));return;}
        pendingMove=parts[0];pendingBase=snapshot[0];pendingAction="";
        String detail=spokenMove(pendingMove)+L(". Notación ",". Notation ")+parts[1]+".";
        if(parts[1].endsWith("#"))detail+=L(" Jaque mate."," Checkmate.");
        else if(parts[1].endsWith("+"))detail+=L(" Jaque."," Check.");
        announce(L("Propuesta: ","Proposed move: ")+detail+L(" Decí confirmar o cancelar. La partida todavía no cambió."," Say confirm or cancel. The game has not changed yet."));
    }
    private void showNarration(String title,String body) {
        closeDialog();
        ScrollView scroll=new ScrollView(this);TextView content=new TextView(this);
        content.setText(body);content.setTextSize(20);content.setPadding(24,16,24,16);
        content.setTextIsSelectable(true);scroll.addView(content);
        dialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setPositiveButton(L("Leer desde el comienzo","Read from the beginning"),(d,w)->announce(body))
            .setNegativeButton(L("Cerrar","Close"),null).create();dialog.show();
    }
    private File stockfishExecutable() {
        return new File(getApplicationInfo().nativeLibraryDir,"libstockfish.so");
    }
    private String stockfishStatus(){
        File binary=stockfishExecutable();
        return binary.isFile()&&binary.canExecute()
            ? L("Stockfish está empaquetado. Aún requiere prueba real en este dispositivo. El núcleo VKRT sigue controlando cada jugada.","Stockfish is packaged. It still requires a real-device execution test. VKRT validates every move.")
            : L("Stockfish no está incluido en esta edición. El motor de ajedrez VKRT sigue funcionando sin conexión.","Stockfish is not included in this edition. The VKRT chess core works without it.");
    }
    private void suggestStockfish(){
        if(!"ONGOING".equals(snapshot[2])&&!"CHECK".equals(snapshot[2])){
            announce(L("La partida terminó. No se puede pedir una nueva jugada.","The game is over. A new move cannot be requested."));return;
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
                        announce(L("La posición cambió. Descartada la sugerencia antigua.","The position changed. The previous suggestion was discarded."));return;
                    }
                    announce(L("Stockfish propone ","Stockfish suggests ")+spokenMove(best.move)+L(". Revisá antes de confirmar.",". Review before confirming."));
                    showProposal(proposed);
                });
            } catch(Exception|LinkageError error){
                runOnUiThread(()->{
                    if(!active||ticket!=generation)return;
                    busy=false;refreshHome();announce(L("Stockfish no pudo analizar esta posición. No se movió ninguna pieza. Podés usar el rival simple o jugar normalmente.","Stockfish could not analyze this position. No piece moved. You can use the basic opponent or continue normally."));
                });
            }
        });
    }
    private void request(String action,String argument,boolean mutation) {
        if(!active||busy||snapshot==null)return;
        if(recovery&&!action.equals("reset")) {announce(L("El archivo guardado no es válido. Elegí Nueva partida para empezar; el archivo se conserva hasta confirmar.","The saved game is invalid. Choose new game; the file is preserved until you confirm."));return;}
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
                    announce((action.equals("play")?spokenMove(argument)+L(". SAN ",". SAN ")+result[7].split(" ",2)[1]+". ":action.equals("undo")?L("Deshecho. ","Undone. "):"")+stateText());
                });
            } catch(Exception|LinkageError error) {
                runOnUiThread(()->{
                    if(!active||ticket!=generation)return;busy=false;refreshHome();
                    announce(L("No se aplicó la acción. Revisá la legalidad, el final de partida, el límite de 2048 jugadas o el almacenamiento. Podés volver a intentar.","Action not applied. Check move legality, game-over state, the 2048-move limit or storage, and try again."));
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
                    runOnUiThread(()->{if(active&&ticket==generation){busy=true;announce(L("No se pudo iniciar el motor nativo.","The native chess engine could not be started."));}});return;
                }
            }
            final String[] result=loaded;final boolean invalid=damaged;
            runOnUiThread(()->{
                if(!active||ticket!=generation)return;snapshot=result;busy=false;recovery=invalid;status.setText(stateText());refreshHome();
                if(invalid)announce(L("No se pudo validar la partida guardada. Se conserva hasta confirmar Nueva partida.","The saved game could not be validated. It will remain until you confirm new game."));
                else if(!getPreferences(MODE_PRIVATE).getBoolean("intro_done",false)){
                    introPending=true;feedback.setText(introduction());
                    if(ttsReady||(accessibility!=null&&accessibility.isEnabled()))showIntro();
                } else announce(stateText()+L(" Partida local lista. Decí ayuda cuando quieras."," Local game ready. Say help at any time."));
            });
        });
    }
    private void requestVoice() {requestVoice(false);}
    private void requestVoice(boolean fromHold) {
        // Voice must remain available during corrupted-save recovery, especially to say Nueva partida.
        if(tts!=null)tts.stop();
        boolean local=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        if(!local&&!SpeechRecognizer.isRecognitionAvailable(this)){
            announce(L("No hay reconocimiento de voz disponible. Con TalkBack, usá la acción ingresar jugada con teclado.","Speech recognition is unavailable. With TalkBack, use the enter move with keyboard action."));return;
        }
        if(!local&&!getPreferences(MODE_PRIVATE).getBoolean("remote_voice_consent",false)){
            // The OS service may send audio to its provider. Consent is requested ONCE, not per utterance.
            confirm(L("Consentimiento de reconocimiento","Recognition consent"),
                L("No hay reconocedor exclusivamente local. El servicio Android puede enviar audio a un proveedor externo. ","No on-device-only recognizer is available. The Android speech service may send audio to a third party. ") +
                L("La aplicación no guarda grabaciones. ¿Autorizar este servicio para futuras escuchas?","This app does not store recordings. Authorize this service for future listening?"),
                ()->{getPreferences(MODE_PRIVATE).edit().putBoolean("remote_voice_consent",true).apply();permission(false,false);});
            return;
        }
        permission(local,fromHold);
    }
    private void permission(boolean local,boolean fromHold) {
        permissionOnDevice=local;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            if(getPreferences(MODE_PRIVATE).getBoolean("mic_denied",false)){
                announce(L("Permiso de micrófono denegado anteriormente. Activá Micrófono en ajustes de Android para esta aplicación. Con TalkBack podés ingresar por teclado.","Microphone permission was previously denied. Enable it in Android app settings. TalkBack can enter moves via keyboard."));return;
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
                announce(L("Micrófono denegado. Podés habilitarlo en ajustes de Android o ingresar jugada con teclado usando TalkBack.","Microphone permission denied. Enable it in Android settings, or enter a move with the TalkBack keyboard action."));
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
            microphoneIndicator.setText(L("PREPARANDO MICRÓFONO…","PREPARING MICROPHONE…"));
            recognizer.setRecognitionListener(new RecognitionListener() {
                private boolean current(){return active&&listening&&ticket==voiceGeneration;}
                public void onReadyForSpeech(Bundle p){if(current()){
                    speechReady=true;microphoneIndicator.setText(L("ESCUCHANDO. HABLÁ AHORA.","LISTENING. SPEAK NOW."));
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
                    announce(L("Falló el reconocimiento de voz, código ","Speech recognition failed, code ")+error+L(". Decí ayuda en el próximo intento o usá entrada por teclado desde TalkBack.",". Say help on your next attempt or use the TalkBack keyboard option."));}
                public void onResults(Bundle result){
                    if(!current())return;
                    ArrayList<String> matches=result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    float[] confidence=result.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                    cancelSpeech();
                    if(matches==null||matches.isEmpty()||(confidence!=null&&confidence.length>0&&confidence[0]>=0&&confidence[0]<0.6f)){
                        announce(L("No pude reconocer la voz con seguridad. Repetí el comando.","I could not recognize the command confidently. Please repeat it."));return;
                    }
                    handleVoice(matches.get(0));
                }
            });
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,languageTag());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
            recognizer.startListening(intent);
            voiceSurface.postDelayed(()->{if(listening&&ticket==voiceGeneration){cancelSpeech();announce(L("Tiempo de escucha agotado. Volvé a mantener pulsado.","Listening timed out. Press and hold again."));}},15000);
        }catch(RuntimeException error){cancelSpeech();announce(L("No se pudo iniciar el reconocimiento. Revisá el servicio de voz de Android.","Could not start speech recognition. Check your Android speech service."));}
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
