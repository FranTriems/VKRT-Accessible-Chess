package org.vkrt.accessiblechess;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/** Full-screen voice control. In TalkBack, ACTION_CLICK toggles recognition. */
public final class VoiceSurface extends View {
    interface Actions {void down();void up();void accessibilityActivate();void help();void manual();}
    private final Actions actions;
    private boolean pressed=false;
    VoiceSurface(Context context,Actions actions){super(context);this.actions=actions;
        setBackgroundColor(Color.TRANSPARENT);setClickable(true);setLongClickable(true);setFocusable(true);
        setContentDescription("Hablar ajedrez. Mantené pulsado para hablar y soltá al terminar. " +
          "Con TalkBack, doble toque para iniciar o detener la escucha. Comando ayuda para recibir instrucciones.");
        setAccessibilityDelegate(new AccessibilityDelegate(){
          @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
            super.onInitializeAccessibilityNodeInfo(host,info);
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000010,"Escuchar ayuda"));
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000011,"Ingresar jugada con teclado"));
          }
          @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){
            if(action==0x01000010){actions.help();return true;}
            if(action==0x01000011){actions.manual();return true;}
            return super.performAccessibilityAction(host,action,args);
          }
        });
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:pressed=true;actions.down();return true;
            case MotionEvent.ACTION_UP:if(pressed){pressed=false;actions.up();}return true;
            case MotionEvent.ACTION_CANCEL:if(pressed){pressed=false;actions.up();}return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();actions.accessibilityActivate();return true;}
    @Override public boolean performLongClick(){super.performLongClick();actions.accessibilityActivate();return true;}
}
