package org.vkrt.accessiblechess;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/** Full-screen voice control. In TalkBack, ACTION_CLICK toggles recognition. */
public final class VoiceSurface extends View {
    interface Actions {void down();void up();void accessibilityActivate();void help();void manual();void language();}
    private final Actions actions;
    private boolean pressed=false;
    private boolean english=false;
    void setLanguage(boolean english){this.english=english;setContentDescription(english?
        "Speak chess. Press and hold to talk, release when finished. With TalkBack, double tap to start or stop. Say help for commands.":
        "Hablar ajedrez. Mantené pulsado para hablar y soltá al terminar. Con TalkBack, doble toque para iniciar o detener la escucha. Decí ayuda para instrucciones.");}

    VoiceSurface(Context context,Actions actions){super(context);this.actions=actions;
        setBackgroundColor(Color.TRANSPARENT);setClickable(true);setLongClickable(true);setFocusable(true);
        setLanguage(false);
        setAccessibilityDelegate(new AccessibilityDelegate(){
          @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
            super.onInitializeAccessibilityNodeInfo(host,info);
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000010,english?"Hear help":"Escuchar ayuda"));
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000011,english?"Enter move with keyboard":"Ingresar jugada con teclado"));
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000012,english?"Choose language":"Elegir idioma"));
          }
          @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){
            if(action==0x01000010){actions.help();return true;}
            if(action==0x01000011){actions.manual();return true;}
            if(action==0x01000012){actions.language();return true;}
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
