package zombie.core.opengl;

import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.Util;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;

public class PZGLUtil {
   private static int SeverityVerbosity = 37191;
   static int test = 0;

   public PZGLUtil() {
   }

   public static void checkGLErrorThrow(String var0, Object... var1) throws OpenGLException {
      int var2 = GL11.glGetError();
      if (var2 != 0) {
         ++test;
         throw new OpenGLException(createErrorMessage(var2, var0, var1));
      }
   }

   private static String createErrorMessage(int var0, String var1, Object... var2) {
      String var3 = System.lineSeparator();
      return "  GL Error code (" + var0 + ") encountered." + var3 + "  Error translation: " + createErrorMessage(var0) + var3 + "  While performing: " + String.format(var1, var2);
   }

   private static String createErrorMessage(int var0) {
      String var1 = Util.translateGLErrorString(var0);
      return var1 + " (" + var0 + ")";
   }

   public static boolean checkGLError(boolean var0) {
      try {
         Util.checkGLError();
         return true;
      } catch (OpenGLException var2) {
         RenderThread.logGLException(var2, var0);
         return false;
      }
   }

   public static void InitGLDebugging() {
      GL43.glEnable(37600);
      GL11.glEnable(33346);
      GL43.glDebugMessageCallback(PZGLUtil::glDebugOutput, 0L);
      GL43.glDebugMessageControl(4352, 4352, 4352, (IntBuffer)null, true);
   }

   private static void glDebugOutput(int var0, int var1, int var2, int var3, int var4, long var5, long var7) {
      if (var3 <= SeverityVerbosity && (var3 != 33387 || SeverityVerbosity == var3)) {
         String var10000;
         switch (var0) {
            case 33350:
               var10000 = "API";
               break;
            case 33351:
               var10000 = "Window System";
               break;
            case 33352:
               var10000 = "Shader Compiler";
               break;
            case 33353:
               var10000 = "Third Party";
               break;
            case 33354:
               var10000 = "Application";
               break;
            case 33355:
               var10000 = "Other";
               break;
            default:
               var10000 = "";
         }

         String var9 = var10000;
         switch (var1) {
            case 33356:
               var10000 = "Error";
               break;
            case 33357:
               var10000 = "Deprecated Behaviour";
               break;
            case 33358:
               var10000 = "Undefined Behaviour";
               break;
            case 33359:
               var10000 = "Portability";
               break;
            case 33360:
               var10000 = "Performance";
               break;
            case 33361:
               var10000 = "Other";
               break;
            case 33362:
            case 33363:
            case 33364:
            case 33365:
            case 33366:
            case 33367:
            case 33368:
            case 33369:
            case 33370:
            case 33371:
            case 33372:
            case 33373:
            case 33374:
            case 33375:
            case 33376:
            case 33377:
            case 33378:
            case 33379:
            case 33380:
            case 33381:
            case 33382:
            case 33383:
            default:
               var10000 = "";
               break;
            case 33384:
               var10000 = "Marker";
               break;
            case 33385:
               var10000 = "Push Group";
               break;
            case 33386:
               var10000 = "Pop Group";
         }

         String var10 = var10000;
         switch (var3) {
            case 33387:
               var10000 = "Notification";
               break;
            case 37190:
               var10000 = "High";
               break;
            case 37191:
               var10000 = "Medium";
               break;
            case 37192:
               var10000 = "Low";
               break;
            default:
               var10000 = "";
         }

         String var11 = var10000;
         String var12 = MemoryUtil.memASCII(var5);
         DebugLogStream var15;
         Consumer var16;
         switch (var3) {
            case 37190:
               var15 = DebugLog.General;
               Objects.requireNonNull(var15);
               var16 = var15::error;
               break;
            case 37191:
            case 37192:
               var15 = DebugLog.General;
               Objects.requireNonNull(var15);
               var16 = var15::warn;
               break;
            default:
               var15 = DebugLog.General;
               Objects.requireNonNull(var15);
               var16 = var15::print;
         }

         Consumer var13 = var16;
         switch (var3) {
            case 37190:
               var10000 = "ERROR";
               break;
            case 37191:
            case 37192:
               var10000 = "WARN";
               break;
            default:
               var10000 = "INFO";
         }

         String var14 = var10000;
         var13.accept(var14 + " : OpenGL: Source: " + var9 + " Type: " + var10 + " Severity: " + var11 + " Message: " + var12);
      }
   }

   public static void printGLState(PrintStream var0) {
      int var1 = GL11.glGetInteger(2979);
      var0.println("DEBUG: GL_MODELVIEW_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(2980);
      var0.println("DEBUG: GL_PROJECTION_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(2981);
      var0.println("DEBUG: GL_TEXTURE_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(2992);
      var0.println("DEBUG: GL_ATTRIB_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(2993);
      var0.println("DEBUG: GL_CLIENT_ATTRIB_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3381);
      var0.println("DEBUG: GL_MAX_ATTRIB_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3382);
      var0.println("DEBUG: GL_MAX_MODELVIEW_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3383);
      var0.println("DEBUG: GL_MAX_NAME_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3384);
      var0.println("DEBUG: GL_MAX_PROJECTION_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3385);
      var0.println("DEBUG: GL_MAX_TEXTURE_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3387);
      var0.println("DEBUG: GL_MAX_CLIENT_ATTRIB_STACK_DEPTH= " + var1);
      var1 = GL11.glGetInteger(3440);
      var0.println("DEBUG: GL_NAME_STACK_DEPTH= " + var1);
   }

   public static void loadMatrix(Matrix4f var0) {
      var0.get(Model.m_staticReusableFloatBuffer);
      Model.m_staticReusableFloatBuffer.position(16);
      Model.m_staticReusableFloatBuffer.flip();
      GL11.glLoadMatrixf(Model.m_staticReusableFloatBuffer);
   }

   public static void multMatrix(Matrix4f var0) {
      var0.get(Model.m_staticReusableFloatBuffer);
      Model.m_staticReusableFloatBuffer.position(16);
      Model.m_staticReusableFloatBuffer.flip();
      GL11.glMultMatrixf(Model.m_staticReusableFloatBuffer);
   }

   public static void loadMatrix(int var0, Matrix4f var1) {
      GL11.glMatrixMode(var0);
      loadMatrix(var1);
   }

   public static void multMatrix(int var0, Matrix4f var1) {
      GL11.glMatrixMode(var0);
      multMatrix(var1);
   }

   public static void pushAndLoadMatrix(int var0, Matrix4f var1) {
      MatrixStack var2 = var0 == 5888 ? Core.getInstance().modelViewMatrixStack : Core.getInstance().projectionMatrixStack;
      Matrix4f var3 = var2.alloc().set(var1);
      var2.push(var3);
   }

   public static void pushAndMultMatrix(int var0, Matrix4f var1) {
      MatrixStack var2 = var0 == 5888 ? Core.getInstance().modelViewMatrixStack : Core.getInstance().projectionMatrixStack;
      if (!var2.isEmpty()) {
         Matrix4f var3 = var2.alloc().set(var2.peek()).mul(var1);
         var2.push(var3);
      }
   }

   public static void popMatrix(int var0) {
      MatrixStack var1 = var0 == 5888 ? Core.getInstance().modelViewMatrixStack : Core.getInstance().projectionMatrixStack;
      var1.pop();
   }
}
