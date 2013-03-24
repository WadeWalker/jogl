/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package jogamp.newt.driver.android.event;

import jogamp.newt.Debug;
import android.view.MotionEvent;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.NEWTEvent;

public class AndroidNewtEventFactory {
    private static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Android.MouseEvent");
    private static final boolean DEBUG_KEY_EVENT = Debug.debug("Android.KeyEvent");
    
    /** API Level 12: {@link android.view.MotionEvent#ACTION_SCROLL} = {@value} */
    private static final int ACTION_SCROLL = 8;

    private static final com.jogamp.newt.event.MouseEvent.PointerType aToolType2PointerType(int aToolType) {
        switch( aToolType ) {
            case MotionEvent.TOOL_TYPE_FINGER:
                return com.jogamp.newt.event.MouseEvent.PointerType.Touch;
            case MotionEvent.TOOL_TYPE_MOUSE:
                return com.jogamp.newt.event.MouseEvent.PointerType.Mouse;
            case MotionEvent.TOOL_TYPE_STYLUS:
            case MotionEvent.TOOL_TYPE_ERASER:
                return com.jogamp.newt.event.MouseEvent.PointerType.Pen;
            default:
                return com.jogamp.newt.event.MouseEvent.PointerType.Undefined;       
        }
    }
    
    private static final short aMotionEventType2Newt(int aType) {
        switch( aType ) {
            case android.view.MotionEvent.ACTION_DOWN: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED; 
            case android.view.MotionEvent.ACTION_UP: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED; 
            case android.view.MotionEvent.ACTION_MOVE: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED; 
            case android.view.MotionEvent.ACTION_CANCEL: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED; 
            case android.view.MotionEvent.ACTION_OUTSIDE: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED; 
            case android.view.MotionEvent.ACTION_POINTER_DOWN: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED; 
            case android.view.MotionEvent.ACTION_POINTER_UP: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED; 
            // case ACTION_HOVER_MOVE
            case ACTION_SCROLL:  // API Level 12 !
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED; 
            // case ACTION_HOVER_ENTER
            // case ACTION_HOVER_EXIT
        }
        return (short)0;
    }
    
    private static final short aAccessibilityEventType2Newt(int aType) {
        switch( aType ) {
            case android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED: 
                return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS;
        }
        return (short)0;
    }

    private static final short aKeyEventType2NewtEventType(int androidKeyAction) {
        switch(androidKeyAction) {
            case android.view.KeyEvent.ACTION_DOWN: 
            case android.view.KeyEvent.ACTION_MULTIPLE:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED;
            case android.view.KeyEvent.ACTION_UP:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED;
        }
        return (short)0;
    }
    
    private static final short aKeyCode2NewtKeyCode(int androidKeyCode, boolean inclSysKeys) {
        if(android.view.KeyEvent.KEYCODE_0 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_9) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_0 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_0 ) ); 
        }
        if(android.view.KeyEvent.KEYCODE_A <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_Z) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_A + ( androidKeyCode - android.view.KeyEvent.KEYCODE_A ) ); 
        }
        if(android.view.KeyEvent.KEYCODE_F1 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_F12) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_F1 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_F1 ) ); 
        }
        if(android.view.KeyEvent.KEYCODE_NUMPAD_0 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_NUMPAD_9) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_NUMPAD0 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0 ) ); 
        }           
        switch(androidKeyCode) {
            case android.view.KeyEvent.KEYCODE_COMMA: return com.jogamp.newt.event.KeyEvent.VK_COMMA; 
            case android.view.KeyEvent.KEYCODE_PERIOD: return com.jogamp.newt.event.KeyEvent.VK_PERIOD;
            case android.view.KeyEvent.KEYCODE_ALT_LEFT: return com.jogamp.newt.event.KeyEvent.VK_ALT;
            case android.view.KeyEvent.KEYCODE_ALT_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_ALT_GRAPH;
            case android.view.KeyEvent.KEYCODE_SHIFT_LEFT: return com.jogamp.newt.event.KeyEvent.VK_SHIFT;
            case android.view.KeyEvent.KEYCODE_SHIFT_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_SHIFT;
            case android.view.KeyEvent.KEYCODE_TAB: return com.jogamp.newt.event.KeyEvent.VK_TAB;
            case android.view.KeyEvent.KEYCODE_SPACE: return com.jogamp.newt.event.KeyEvent.VK_SPACE;
            case android.view.KeyEvent.KEYCODE_ENTER: return com.jogamp.newt.event.KeyEvent.VK_ENTER;
            case android.view.KeyEvent.KEYCODE_DEL: return com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE;
            case android.view.KeyEvent.KEYCODE_MINUS: return com.jogamp.newt.event.KeyEvent.VK_MINUS;
            case android.view.KeyEvent.KEYCODE_EQUALS: return com.jogamp.newt.event.KeyEvent.VK_EQUALS;
            case android.view.KeyEvent.KEYCODE_LEFT_BRACKET: return com.jogamp.newt.event.KeyEvent.VK_LEFT_PARENTHESIS;
            case android.view.KeyEvent.KEYCODE_RIGHT_BRACKET: return com.jogamp.newt.event.KeyEvent.VK_RIGHT_PARENTHESIS;
            case android.view.KeyEvent.KEYCODE_BACKSLASH: return com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH;
            case android.view.KeyEvent.KEYCODE_SEMICOLON: return com.jogamp.newt.event.KeyEvent.VK_SEMICOLON;
            // case android.view.KeyEvent.KEYCODE_APOSTROPHE: ??
            case android.view.KeyEvent.KEYCODE_SLASH: return com.jogamp.newt.event.KeyEvent.VK_SLASH;
            case android.view.KeyEvent.KEYCODE_AT: return com.jogamp.newt.event.KeyEvent.VK_AT;
            // case android.view.KeyEvent.KEYCODE_MUTE: ??
            case android.view.KeyEvent.KEYCODE_PAGE_UP: return com.jogamp.newt.event.KeyEvent.VK_PAGE_UP;
            case android.view.KeyEvent.KEYCODE_PAGE_DOWN: return com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN;
            case android.view.KeyEvent.KEYCODE_ESCAPE: return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
            case android.view.KeyEvent.KEYCODE_CTRL_LEFT: return com.jogamp.newt.event.KeyEvent.VK_CONTROL;
            case android.view.KeyEvent.KEYCODE_CTRL_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_CONTROL; // ??
            case android.view.KeyEvent.KEYCODE_BACK: 
                if( inclSysKeys ) {
                    // Note that manual mapping is performed, based on the keyboard state.
                    // I.e. we map to VK_KEYBOARD_INVISIBLE if keyboard was visible and now becomes invisible!
                    // Otherwise we map to VK_ESCAPE, and if not consumed by user, the application will be terminated.
                    return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
                }
                break;
            case android.view.KeyEvent.KEYCODE_HOME:
                if( inclSysKeys ) {
                    // If not consumed by user, the application will be 'paused',
                    // i.e. resources (GLEventListener) pulled before surface gets destroyed!
                    return com.jogamp.newt.event.KeyEvent.VK_HOME;
                }
                break;
        }        
        return (short)0;
    }
    
    private static final int aKeyModifiers2Newt(int androidMods) {
        int newtMods = 0;
        if ((androidMods & android.view.KeyEvent.META_SYM_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((androidMods & android.view.KeyEvent.META_SHIFT_ON) != 0)   newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((androidMods & android.view.KeyEvent.META_ALT_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        
        return newtMods;
    }
    
    public static com.jogamp.newt.event.WindowEvent createWindowEvent(android.view.accessibility.AccessibilityEvent event, com.jogamp.newt.Window newtSource) {
        final int aType = event.getEventType();
        final short nType = aAccessibilityEventType2Newt(aType);
        
        if( (short)0 != nType) {
            return new com.jogamp.newt.event.WindowEvent(nType, ((null==newtSource)?null:(Object)newtSource), event.getEventTime());
        }
        return null; // no mapping ..
    }

    
    public static com.jogamp.newt.event.KeyEvent createKeyEvent(android.view.KeyEvent aEvent, com.jogamp.newt.Window newtSource, boolean inclSysKeys) {
        final com.jogamp.newt.event.KeyEvent res;
        final short newtType = aKeyEventType2NewtEventType(aEvent.getAction());        
        if( (short)0 != newtType) {
            final short newtKeyCode = aKeyCode2NewtKeyCode(aEvent.getKeyCode(), inclSysKeys);
            res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        } else {
            res = null;
        }
        if(DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent0: "+aEvent+" -> "+res);
        }
        return res;
    }

    public static com.jogamp.newt.event.KeyEvent createKeyEvent(android.view.KeyEvent aEvent, short newtType, com.jogamp.newt.Window newtSource, boolean inclSysKeys) {
        final short newtKeyCode = aKeyCode2NewtKeyCode(aEvent.getKeyCode(), inclSysKeys);
        final com.jogamp.newt.event.KeyEvent res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        if(DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent1: newtType "+NEWTEvent.toHexString(newtType)+", "+aEvent+" -> "+res);
        }
        return res;
    }
    
    public static com.jogamp.newt.event.KeyEvent createKeyEvent(android.view.KeyEvent aEvent, short newtKeyCode, short newtType, com.jogamp.newt.Window newtSource) {
        final com.jogamp.newt.event.KeyEvent res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        if(DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent2: newtType "+NEWTEvent.toHexString(newtType)+", "+aEvent+" -> "+res);
        }
        return res;
    }
    
    private static com.jogamp.newt.event.KeyEvent createKeyEventImpl(android.view.KeyEvent aEvent, short newtType, short newtKeyCode, com.jogamp.newt.Window newtSource) {
        if( (short)0 != newtType && (short)0 != newtKeyCode ) {
            final Object src = null==newtSource ? null : newtSource;
            final long unixTime = System.currentTimeMillis() + ( aEvent.getEventTime() - android.os.SystemClock.uptimeMillis() );
            final int newtMods = aKeyModifiers2Newt(aEvent.getMetaState());
            
            return new com.jogamp.newt.event.KeyEvent(
                                newtType, src, unixTime, newtMods, newtKeyCode, newtKeyCode, (char) aEvent.getUnicodeChar());
        }
        return null;
    }
    
    private static float maxPressure = 0.7f; // experienced maximum value (Amazon HD = 0.8f)
    
    /**
     * Dynamic calibration of maximum MotionEvent pressure, starting from 0.7f
     * <p>
     * Specification says no pressure is 0.0f and
     * normal pressure is 1.0f, where &gt; 1.0f denominates very high pressure.
     * </p>
     * <p>
     * Some devices exceed this spec, or better, most devices do.
     * <ul>
     *   <li>Asus TF2*: Pressure always &gt; 1.0f</li>
     *   <li>Amazon HD: Pressure always &le; 0.8f</li>
     * </ul>
     * </p>
     *   
     * @return
     */
    public static float getMaxPressure() {
        return maxPressure;
    }
    
    private final int touchSlop, touchSlop2x, touchSlopSquare, doubleTapSlop, doubleTapSlopSquare;
    
    public AndroidNewtEventFactory(android.content.Context context, android.os.Handler handler) {
        final android.view.ViewConfiguration configuration = android.view.ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        touchSlop2x = 2*touchSlop;
        touchSlopSquare = touchSlop * touchSlop;         
        doubleTapSlop = configuration.getScaledDoubleTapSlop();
        doubleTapSlopSquare = doubleTapSlop * doubleTapSlop;  
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("GestureListener     touchSlop (scaled) "+touchSlop);                               
            System.err.println("GestureListener doubleTapSlop (scaled) "+doubleTapSlop);                               
        }
    }
            
    private static void collectPointerData(MotionEvent e, int eIdx, int dIdx, final int[] x, final int[] y, final float[] pressure, short[] pointerIds, final com.jogamp.newt.event.MouseEvent.PointerType[] pointerTypes) {
        x[dIdx] = (int)e.getX(eIdx);
        y[dIdx] = (int)e.getY(eIdx);
        pressure[dIdx] = e.getPressure(eIdx);
        pointerIds[dIdx] = (short)e.getPointerId(eIdx);
        if( pressure[dIdx] > maxPressure ) {
            maxPressure = pressure[dIdx];
        }
        pointerTypes[dIdx] = aToolType2PointerType( e.getToolType(eIdx) );   
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("createMouseEvent: ptr-data["+eIdx+" -> "+dIdx+"] "+x[dIdx]+"/"+y[dIdx]+", pressure "+pressure[dIdx]+", id "+pointerIds[dIdx]+", type "+pointerTypes[dIdx]);
        }
    }
    
    public com.jogamp.newt.event.MouseEvent[] createMouseEvents(boolean isOnTouchEvent, 
                                                                android.view.MotionEvent event, com.jogamp.newt.Window newtSource) {
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("createMouseEvent: isOnTouchEvent "+isOnTouchEvent+", "+event);                               
        }

        if( event.getPressure() > maxPressure ) {
            maxPressure = event.getPressure();
        }
        
        //
        // Prefilter Android Event (Gesture, ..) and determine final type
        //
        final int aType;
        final short nType;
        float[] rotationXY = null;
        int rotationSource = 0; // 1 - Gesture, 2 - ACTION_SCROLL        
        {
            final int aType0 = event.getActionMasked();
            if( isOnTouchEvent ) {
                boolean action = false;
                switch ( aType0 ) {
                    case MotionEvent.ACTION_DOWN:
                        action = true;
                        // fall through intended
                    case MotionEvent.ACTION_POINTER_DOWN:
                        gesture2FingerScrl.onDown(event, action);
                        break;
                    case MotionEvent.ACTION_UP:
                        action = true;
                        // fall through intended
                    case MotionEvent.ACTION_POINTER_UP:
                        gesture2FingerScrl.onUp(event, action);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        gesture2FingerScrl.onActionMove(event);
                        break;
                }
            }
                        
            if( gesture2FingerScrl.isWithinGesture() ) {
                rotationXY = gesture2FingerScrl.getScrollDistanceXY();
                aType = ACTION_SCROLL; // 8
                rotationSource = 1;
            } else {
                aType = aType0;
            }
            nType = aMotionEventType2Newt(aType);            
        }
        
        if( (short)0 != nType ) {            
            final short clickCount = 1;
            int modifiers = 0;
            
            if( null == rotationXY && AndroidVersion.SDK_INT >= 12 && ACTION_SCROLL == aType ) { // API Level 12
                rotationXY = new float[] { event.getAxisValue(android.view.MotionEvent.AXIS_X),
                                           event.getAxisValue(android.view.MotionEvent.AXIS_Y) };
                rotationSource = 2;
            }
            
            final float rotation;
            final float rotationScale = touchSlop;
            if( null != rotationXY ) {
                final float _rotation;
                if( rotationXY[0]*rotationXY[0] > rotationXY[1]*rotationXY[1] ) {
                    // Horizontal
                    modifiers |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
                    _rotation = rotationXY[0];
                } else {
                    // Vertical
                    _rotation = rotationXY[1];
                }
                rotation =  _rotation / rotationScale;                
                if(DEBUG_MOUSE_EVENT) {
                    System.err.println("createMouseEvent: Scroll "+rotationXY[0]+"/"+rotationXY[1]+" -> "+_rotation+" / "+rotationScale+" -> "+rotation+" scaled -- mods "+modifiers+", source "+rotationSource);
                }      
            } else {
                rotation = 0.0f;
            }
            
            //
            // Determine newt-button and whether dedicated pointer is pressed
            //
            final int pIndex;
            final short button;
            switch( aType ) {
                case android.view.MotionEvent.ACTION_POINTER_DOWN:
                case android.view.MotionEvent.ACTION_POINTER_UP: {
                        pIndex = event.getActionIndex();
                        final int b = event.getPointerId(pIndex) + 1; // FIXME: Assumption that Pointer-ID starts w/ 0 !
                        if( com.jogamp.newt.event.MouseEvent.BUTTON1 <= b && b <= com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER ) {
                            button = (short)b;
                        } else {
                            button = com.jogamp.newt.event.MouseEvent.BUTTON1;
                        }
                    }
                    break;
                default: {
                        pIndex = 0;
                        button = com.jogamp.newt.event.MouseEvent.BUTTON1;
                    }
            }
            final int pCount = event.getPointerCount(); // all
            
            //
            // Collect common data
            //
            final int[] x = new int[pCount];
            final int[] y = new int[pCount];
            final float[] pressure = new float[pCount];
            final short[] pointerIds = new short[pCount];
            final com.jogamp.newt.event.MouseEvent.PointerType[] pointerTypes = new com.jogamp.newt.event.MouseEvent.PointerType[pCount];
            if( 0 < pCount ) {
                if(DEBUG_MOUSE_EVENT) {
                    System.err.println("createMouseEvent: collect ptr-data [0.."+(pCount-1)+", count "+pCount+", action "+pIndex+"], aType "+aType+", button "+button+", twoFingerScrollGesture "+gesture2FingerScrl);
                }
                int j = 0; 
                // Always put action-pointer data at index 0
                collectPointerData(event, pIndex, j++, x, y, pressure, pointerIds, pointerTypes);
                for(int i=0; i < pCount; i++) {
                    if( pIndex != i ) {
                        collectPointerData(event, i, j++, x, y, pressure, pointerIds, pointerTypes);
                    }
                }
            }
            
            if(null!=newtSource) {
                if(newtSource.isPointerConfined()) {
                    modifiers |= InputEvent.CONFINED_MASK;
                }
                if(!newtSource.isPointerVisible()) {
                    modifiers |= InputEvent.INVISIBLE_MASK;
                }
            }
                                
            final Object src = (null==newtSource)?null:(Object)newtSource;
            final long unixTime = System.currentTimeMillis() + ( event.getEventTime() - android.os.SystemClock.uptimeMillis() );
            
            final com.jogamp.newt.event.MouseEvent me1 = new com.jogamp.newt.event.MouseEvent(
                           nType,  src, unixTime,
                           modifiers, x, y, pressure, maxPressure, pointerTypes, pointerIds, 
                           clickCount, button, rotation, rotationScale);
            
            if( com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED == nType ) {
                return new com.jogamp.newt.event.MouseEvent[] { me1, 
                    new com.jogamp.newt.event.MouseEvent(
                           com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED, 
                           src, unixTime, modifiers, x, y, pressure, maxPressure, pointerTypes, pointerIds, 
                           clickCount, button, rotation, rotationScale) };
            } else {
                return new com.jogamp.newt.event.MouseEvent[] { me1 };
            }
        } 
        return null; // no mapping ..
    }
        
    static interface GestureHandler {
        /** Returns true if within the gesture */ 
        public boolean isWithinGesture();
        /** Returns distance of the last consecutive double-tab scrolling. */ 
        public float[] getScrollDistanceXY();
        
        public void onUp(android.view.MotionEvent e, boolean action);
        public void onDown(android.view.MotionEvent e, boolean action);
        public void onActionMove(android.view.MotionEvent e);
    }
    
    /**
     * Criteria related to Android parameter:
     *    - ScaledDoubleTapSlop:
     *       - Max 2 finger distance
     *
     *    - ScaledTouchSlop:
     *       - Min. movement w/ 2 pointer withing ScaledDoubleTapSlop starting 'scroll' mode
     *       - Max. change of finger distance in respect to initiating 2-finger distance (2x ScaledTouchSlop)
     *       - Max. distance growth in respect to initiating 2-finger distance.
     */
    private final GestureHandler gesture2FingerScrl = new GestureHandler() {
        private final float[] scrollDistance = new float[] { 0f, 0f };
        private int dStartDist = 0;
        private float dDownY = 0;
        private float dDownX = 0;
        private float dLastY = 0;
        private float dLastX = 0;
        private int pointerDownCount = 0;
        private boolean dDownScroll = false;
        
        public String toString() {
            return "Gesture2FingerScrl[in "+dDownScroll+", pc "+pointerDownCount+"]";
        }
        
        private final int getSquareDistance(float x1, float y1, float x2, float y2) {
            final int deltaX = (int) x1 - (int) x2;
            final int deltaY = (int) y1 - (int) y2;
            return deltaX * deltaX + deltaY * deltaY;
        }
        
        public boolean isWithinGesture() {
            return dDownScroll;
        }
        @Override
        public final float[] getScrollDistanceXY() {
            return scrollDistance;
        }
        
        @Override
        public void onDown(android.view.MotionEvent e, boolean action) {
            pointerDownCount = e.getPointerCount();
            if( 2 == pointerDownCount ) {
                final int sqDist = getSquareDistance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
                final boolean isDistWithinDoubleTapSlop = sqDist < doubleTapSlopSquare;
                if(DEBUG_MOUSE_EVENT) {
                    System.err.println(this+".onDown: action "+action+", dist "+Math.sqrt(sqDist)+", distWithin2DTSlop "+isDistWithinDoubleTapSlop+", "+e);
                }
                if( isDistWithinDoubleTapSlop ) {
                    dDownX = e.getX();
                    dDownY = e.getY();
                    dLastX = dDownX;
                    dLastY = dDownY;
                    dStartDist = (int)Math.sqrt(sqDist);
                }
            }
        }
        
        @Override
        public void onUp(android.view.MotionEvent e, boolean action) {
            pointerDownCount = e.getPointerCount();
            if(DEBUG_MOUSE_EVENT) {
                System.err.println(this+".onrUp: action "+action+", "+e);
            }
            if( 2 >= pointerDownCount ) {
                dDownScroll = false;
            }
            pointerDownCount--; // lifted now!
            if(!dDownScroll) {
                dDownY = 0f;
                dDownX = 0f;
                dLastY = 0f;
                dLastX = 0f;
            }
        }
     
        @Override
        public void onActionMove(android.view.MotionEvent e) {
            pointerDownCount = e.getPointerCount();
            if( 2 <= pointerDownCount ) {
                final int sqDist = getSquareDistance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
                final int dist = (int)Math.sqrt(sqDist);
                final float x = e.getX();
                final float y = e.getY();
                final boolean isDistWithinDoubleTapSlop = sqDist < doubleTapSlopSquare;
                final boolean isDistDeltaWithinTouchSlop = Math.abs( dStartDist - dist ) <= touchSlop2x &&
                                                                       dist - dStartDist <= touchSlop; 
                if( !isDistWithinDoubleTapSlop || !isDistDeltaWithinTouchSlop ) {
                    dDownScroll = false;
                } else  if( !dDownScroll ) {  
                    final int dX = (int) (x - dDownX);
                    final int dY = (int) (y - dDownY);
                    final int d = (dX * dX) + (dY * dY);
                    dDownScroll = d > touchSlopSquare;
                }
                scrollDistance[0] = dLastX - x;
                scrollDistance[1] = dLastY - y;
                dLastX = x;
                dLastY = y;
                if(DEBUG_MOUSE_EVENT) {
                    System.err.println(this+".onActionMove: startDist "+dStartDist+", dist "+dist+", distWithin2DTSlop "+isDistWithinDoubleTapSlop+", distDeltaWithinTSlop "+isDistDeltaWithinTouchSlop+", d "+scrollDistance[0]+"/"+scrollDistance[1]+", "+e);
                }
            }
        }
    };
}

