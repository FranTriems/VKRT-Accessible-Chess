package org.vkrt.accessiblechess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Decorative visual board. Never a required touch target; game state remains in the C++ core. */
public final class ChessBoardView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final char[] cells=new char[64];
    private final String white="♔♕♖♗♘♙", black="♚♛♜♝♞♟";
    ChessBoardView(Context c){super(c);for(int i=0;i<64;i++)cells[i]='.';
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);}
    void update(String raw){
        if(raw==null)return;
        String[] items=raw.split("\n");
        for(int i=0;i<64&&i<items.length;i++) if(items[i].length()>3) {
            String square=items[i].substring(0,2);
            int file=square.charAt(0)-'a', rank=square.charAt(1)-'1';
            if(file>=0&&file<8&&rank>=0&&rank<8)cells[(7-rank)*8+file]=items[i].charAt(3);
        }
        invalidate();
    }
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);int max=(int)(getResources().getDisplayMetrics().density*344);
        int height=MeasureSpec.getSize(h);int size=Math.min(Math.min(width,height),max);setMeasuredDimension(size,size);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float size=Math.min(getWidth(),getHeight()), cell=size/8f;
        paint.setStyle(Paint.Style.FILL);
        for(int r=0;r<8;r++)for(int f=0;f<8;f++){
            paint.setColor((r+f)%2==0?0xFFDDE5DB:0xFF477052);
            canvas.drawRect(new RectF(f*cell,r*cell,(f+1)*cell,(r+1)*cell),paint);
            char p=cells[r*8+f];String glyph=pieceGlyph(p);
            if(!glyph.isEmpty()){
                paint.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
                paint.setTextSize(cell*.69f);paint.setTextAlign(Paint.Align.CENTER);
                boolean whitePiece=Character.isUpperCase(p);
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1.5f,cell*.055f));
                paint.setColor(whitePiece?0xFF14241C:0xFFE6F2E8);
                canvas.drawText(glyph,(f+.5f)*cell,(r+.73f)*cell,paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(whitePiece?Color.WHITE:0xFF101713);
                canvas.drawText(glyph,(f+.5f)*cell,(r+.73f)*cell,paint);
            }
        }
    }
    private String pieceGlyph(char c){switch(c){
        case 'K':return "♔";case 'Q':return "♕";case 'R':return "♖";case 'B':return "♗";case 'N':return "♘";case 'P':return "♙";
        case 'k':return "♚";case 'q':return "♛";case 'r':return "♜";case 'b':return "♝";case 'n':return "♞";case 'p':return "♟";
        default:return "";}}
}
