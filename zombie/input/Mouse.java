package zombie.input;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Cursor;
import zombie.GameTime;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;

public final class Mouse {
   protected static int x;
   protected static int y;
   private static float TimeRightPressed = 0.0F;
   private static final float TIME_RIGHT_PRESSED_SECONDS = 0.15F;
   public static final int BTN_OFFSET = 10000;
   public static final int BTN_0 = 10000;
   public static final int BTN_1 = 10001;
   public static final int BTN_2 = 10002;
   public static final int BTN_3 = 10003;
   public static final int BTN_4 = 10004;
   public static final int BTN_5 = 10005;
   public static final int BTN_6 = 10006;
   public static final int BTN_7 = 10007;
   public static final int LMB = 10000;
   public static final int RMB = 10001;
   public static final int MMB = 10002;
   public static boolean[] m_buttonDownStates;
   public static boolean[] m_buttonPrevStates;
   public static long lastActivity;
   public static int wheelDelta;
   private static final MouseStateCache s_mouseStateCache = new MouseStateCache();
   public static boolean[] UICaptured = new boolean[10];
   static Cursor blankCursor;
   static Cursor defaultCursor;
   private static boolean isCursorVisible = true;
   private static Texture mouseCursorTexture = null;

   public Mouse() {
   }

   public static int getWheelState() {
      return wheelDelta;
   }

   public static int getButtonCount() {
      return s_mouseStateCache.getState().getButtonCount();
   }

   public static synchronized int getXA() {
      return x;
   }

   public static synchronized int getYA() {
      return y;
   }

   public static synchronized int getX() {
      return (int)((float)x * Core.getInstance().getZoom(0));
   }

   public static synchronized int getY() {
      return (int)((float)y * Core.getInstance().getZoom(0));
   }

   public static boolean isButtonDown(int var0) {
      return m_buttonDownStates != null ? m_buttonDownStates[var0] : false;
   }

   public static boolean wasButtonDown(int var0) {
      return m_buttonPrevStates != null ? m_buttonPrevStates[var0] : false;
   }

   public static boolean isButtonPressed(int var0) {
      if (m_buttonDownStates != null && m_buttonPrevStates != null) {
         return !m_buttonPrevStates[var0] && m_buttonDownStates[var0];
      } else {
         return false;
      }
   }

   public static boolean isButtonReleased(int var0) {
      if (m_buttonDownStates != null && m_buttonPrevStates != null) {
         return m_buttonPrevStates[var0] && !m_buttonDownStates[var0];
      } else {
         return false;
      }
   }

   public static void UIBlockButtonDown(int var0) {
      UICaptured[var0] = true;
   }

   public static boolean isButtonDownUICheck(int var0) {
      if (m_buttonDownStates == null) {
         return false;
      } else {
         boolean var1 = m_buttonDownStates[var0];
         if (!var1) {
            UICaptured[var0] = false;
         } else if (UICaptured[var0]) {
            return false;
         }

         return var0 == 1 ? isRightDelay() : var1;
      }
   }

   public static boolean isRightDelay() {
      if (!UICaptured[1] && m_buttonDownStates != null && m_buttonDownStates[1]) {
         return TimeRightPressed >= 0.15F;
      } else {
         return false;
      }
   }

   public static boolean isLeftDown() {
      return isButtonDown(0);
   }

   public static boolean isLeftPressed() {
      return isButtonPressed(0);
   }

   public static boolean isLeftReleased() {
      return isButtonReleased(0);
   }

   public static boolean isLeftUp() {
      return !isButtonDown(0);
   }

   public static boolean isMiddleDown() {
      return isButtonDown(2);
   }

   public static boolean isMiddlePressed() {
      return isButtonPressed(2);
   }

   public static boolean isMiddleReleased() {
      return isButtonReleased(2);
   }

   public static boolean isMiddleUp() {
      return !isButtonDown(2);
   }

   public static boolean isRightDown() {
      return isButtonDown(1);
   }

   public static boolean isRightPressed() {
      return isButtonPressed(1);
   }

   public static boolean isRightReleased() {
      return isButtonReleased(1);
   }

   public static boolean isRightUp() {
      return !isButtonDown(1);
   }

   public static synchronized void update() {
      MouseState var0 = s_mouseStateCache.getState();
      if (!var0.isCreated()) {
         s_mouseStateCache.swap();

         try {
            org.lwjglx.input.Mouse.create();
         } catch (LWJGLException var5) {
            var5.printStackTrace();
         }

      } else {
         int var1 = x;
         int var2 = y;
         x = var0.getX();
         y = Core.getInstance().getScreenHeight() - var0.getY() - 1;
         wheelDelta = var0.getDWheel();
         var0.resetDWheel();
         boolean var3 = var1 != x || var2 != y || wheelDelta != 0;
         if (m_buttonDownStates == null) {
            m_buttonDownStates = new boolean[var0.getButtonCount()];
         }

         if (m_buttonPrevStates == null) {
            m_buttonPrevStates = new boolean[var0.getButtonCount()];
         }

         int var4;
         for(var4 = 0; var4 < m_buttonDownStates.length; ++var4) {
            m_buttonPrevStates[var4] = m_buttonDownStates[var4];
         }

         for(var4 = 0; var4 < m_buttonDownStates.length; ++var4) {
            if (m_buttonDownStates[var4] != var0.isButtonDown(var4)) {
               var3 = true;
            }

            m_buttonDownStates[var4] = var0.isButtonDown(var4);
         }

         if (m_buttonDownStates[1]) {
            TimeRightPressed += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
         } else {
            TimeRightPressed = 0.0F;
         }

         if (var3) {
            lastActivity = System.currentTimeMillis();
         }

         s_mouseStateCache.swap();
      }
   }

   public static void poll() {
      s_mouseStateCache.poll();
   }

   public static synchronized void setXY(int var0, int var1) {
      s_mouseStateCache.getState().setCursorPosition(var0, Core.getInstance().getOffscreenHeight(0) - 1 - var1);
   }

   public static Cursor loadCursor(String var0) throws LWJGLException {
      File var1 = ZomboidFileSystem.instance.getMediaFile("ui/" + var0);
      BufferedImage var2 = null;

      try {
         var2 = ImageIO.read(var1);
         int var3 = var2.getWidth();
         int var4 = var2.getHeight();
         int[] var5 = new int[var3 * var4];

         for(int var6 = 0; var6 < var5.length; ++var6) {
            int var7 = var6 % var3;
            int var8 = var4 - 1 - var6 / var3;
            var5[var6] = var2.getRGB(var7, var8);
         }

         IntBuffer var11 = BufferUtils.createIntBuffer(var3 * var4);
         var11.put(var5);
         var11.rewind();
         byte var12 = 1;
         byte var13 = 1;
         Cursor var9 = new Cursor(var3, var4, var12, var13, 1, var11, (IntBuffer)null);
         return var9;
      } catch (Exception var10) {
         return null;
      }
   }

   public static void initCustomCursor() {
      if (blankCursor == null) {
         try {
            blankCursor = loadCursor("cursor_blank.png");
            defaultCursor = loadCursor("cursor_white.png");
         } catch (LWJGLException var2) {
            var2.printStackTrace();
         }
      }

      if (defaultCursor != null) {
         try {
            org.lwjglx.input.Mouse.setNativeCursor(defaultCursor);
         } catch (LWJGLException var1) {
            var1.printStackTrace();
         }

      }
   }

   public static void setCursorVisible(boolean var0) {
      isCursorVisible = var0;
   }

   public static boolean isCursorVisible() {
      return isCursorVisible;
   }

   public static void renderCursorTexture() {
      if (isCursorVisible()) {
         if (mouseCursorTexture == null) {
            mouseCursorTexture = Texture.getSharedTexture("media/ui/cursor_white.png");
         }

         if (mouseCursorTexture != null && mouseCursorTexture.isReady()) {
            int var0 = getXA();
            int var1 = getYA();
            byte var2 = 1;
            byte var3 = 1;
            SpriteRenderer.instance.render(mouseCursorTexture, (float)(var0 - var2), (float)(var1 - var3), (float)mouseCursorTexture.getWidth(), (float)mouseCursorTexture.getHeight(), 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
         }
      }
   }
}
