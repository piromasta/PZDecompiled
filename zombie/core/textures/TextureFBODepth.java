package zombie.core.textures;

import gnu.trove.stack.array.TIntArrayStack;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;
import zombie.interfaces.ITexture;

public class TextureFBODepth {
   private static IGLFramebufferObject funcs;
   public static int lastID = 0;
   private static final TIntArrayStack stack = new TIntArrayStack();
   private int id = 0;
   ITexture texture;
   private int depth = 0;
   private int width;
   private int height;
   private static Boolean checked = null;

   public void swapTexture(ITexture var1) {
      assert lastID == this.id;

      if (var1 != null && var1 != this.texture) {
         if (var1.getWidth() == this.width && var1.getHeight() == this.height) {
            if (var1.getID() == -1) {
               var1.bind();
            }

            IGLFramebufferObject var2 = getFuncs();
            var2.glFramebufferTexture2D(var2.GL_FRAMEBUFFER(), var2.GL_DEPTH_ATTACHMENT(), 3553, var1.getID(), 0);
            this.texture = var1;
         }
      }
   }

   public TextureFBODepth(ITexture var1) {
      RenderThread.invokeOnRenderContext(var1, this::init);
   }

   private void init(ITexture var1) {
      int var2 = lastID;
      boolean var7 = false;

      int var10001;
      try {
         var7 = true;
         this.initInternal(var1);
         var7 = false;
      } finally {
         if (var7) {
            IGLFramebufferObject var5 = getFuncs();
            var10001 = var5.GL_FRAMEBUFFER();
            lastID = var2;
            var5.glBindFramebuffer(var10001, var2);
         }
      }

      IGLFramebufferObject var3 = getFuncs();
      var10001 = var3.GL_FRAMEBUFFER();
      lastID = var2;
      var3.glBindFramebuffer(var10001, var2);
   }

   public static IGLFramebufferObject getFuncs() {
      if (funcs == null) {
         checkFBOSupport();
      }

      return funcs;
   }

   public void blitDepth(float var1, float var2, float var3, float var4) {
      GL30C.glBindFramebuffer(36008, this.id);
      GL30C.glBindFramebuffer(36009, lastID);
      GL30C.glBlitFramebuffer(0, 0, this.width, this.height, (int)var1, (int)var2, (int)var3, (int)var4, 256, 9729);
      GL30C.glBindFramebuffer(36160, lastID);
   }

   private void initInternal(ITexture var1) {
      IGLFramebufferObject var2 = getFuncs();

      try {
         PZGLUtil.checkGLErrorThrow("Enter.");
         this.texture = var1;
         this.width = this.texture.getWidth();
         this.height = this.texture.getHeight();
         if (!checkFBOSupport()) {
            throw new RuntimeException("Could not create FBO. FBO's not supported.");
         } else if (this.texture == null) {
            throw new NullPointerException("Could not create FBO. Texture is null.");
         } else {
            this.texture.bind();
            PZGLUtil.checkGLErrorThrow("Binding texture. %s", this.texture);
            GL11.glTexImage2D(3553, 0, 6402, this.texture.getWidthHW(), this.texture.getHeightHW(), 0, 6402, 5126, (IntBuffer)null);
            PZGLUtil.checkGLErrorThrow("glTexImage2D(width: %d, height: %d)", this.texture.getWidthHW(), this.texture.getHeightHW());
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            Texture.lastTextureID = 0;
            GL11.glBindTexture(3553, 0);
            this.id = var2.glGenFramebuffers();
            PZGLUtil.checkGLErrorThrow("glGenFrameBuffers");
            var2.glBindFramebuffer(var2.GL_FRAMEBUFFER(), this.id);
            PZGLUtil.checkGLErrorThrow("glBindFramebuffer(%d)", this.id);
            var2.glFramebufferTexture2D(var2.GL_FRAMEBUFFER(), var2.GL_DEPTH_ATTACHMENT(), 3553, this.texture.getID(), 0);
            PZGLUtil.checkGLErrorThrow("glFramebufferTexture2D texture: %s", this.texture);
            int var3 = var2.glCheckFramebufferStatus(var2.GL_FRAMEBUFFER());
            if (var3 != var2.GL_FRAMEBUFFER_COMPLETE()) {
               if (var3 == var2.GL_FRAMEBUFFER_UNDEFINED()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_UNDEFINED");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_FORMATS");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
               }

               if (var3 == var2.GL_FRAMEBUFFER_UNSUPPORTED()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_UNSUPPORTED");
               }

               if (var3 == var2.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE()) {
                  DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE");
               }

               throw new RuntimeException("Could not create FBO!");
            }
         }
      } catch (Exception var4) {
         var2.glDeleteFramebuffers(this.id);
         var2.glDeleteRenderbuffers(this.depth);
         this.id = 0;
         this.depth = 0;
         this.texture = null;
         throw var4;
      }
   }

   public static boolean checkFBOSupport() {
      if (checked != null) {
         return checked;
      } else if (GL.getCapabilities().OpenGL30) {
         DebugLog.General.debugln("OpenGL 3.0 framebuffer objects supported");
         funcs = new GLFramebufferObject30();
         return checked = Boolean.TRUE;
      } else if (GL.getCapabilities().GL_ARB_framebuffer_object) {
         DebugLog.General.debugln("GL_ARB_framebuffer_object supported");
         funcs = new GLFramebufferObjectARB();
         return checked = Boolean.TRUE;
      } else if (GL.getCapabilities().GL_EXT_framebuffer_object) {
         DebugLog.General.debugln("GL_EXT_framebuffer_object supported");
         if (!GL.getCapabilities().GL_EXT_packed_depth_stencil) {
            DebugLog.General.debugln("GL_EXT_packed_depth_stencil not supported");
         }

         funcs = new GLFramebufferObjectEXT();
         return checked = Boolean.TRUE;
      } else {
         DebugLog.General.debugln("None of OpenGL 3.0, GL_ARB_framebuffer_object or GL_EXT_framebuffer_object are supported, zoom disabled");
         return checked = Boolean.TRUE;
      }
   }

   public void destroy() {
      if (this.id != 0 && this.depth != 0) {
         if (lastID == this.id) {
            lastID = 0;
         }

         RenderThread.invokeOnRenderContext(() -> {
            if (this.texture != null) {
               this.texture.destroy();
               this.texture = null;
            }

            IGLFramebufferObject var1 = getFuncs();
            var1.glDeleteFramebuffers(this.id);
            var1.glDeleteRenderbuffers(this.depth);
            this.id = 0;
            this.depth = 0;
         });
      }
   }

   public void destroyLeaveTexture() {
      if (this.id != 0 && this.depth != 0) {
         RenderThread.invokeOnRenderContext(() -> {
            this.texture = null;
            IGLFramebufferObject var1 = getFuncs();
            var1.glDeleteFramebuffers(this.id);
            var1.glDeleteRenderbuffers(this.depth);
            this.id = 0;
            this.depth = 0;
         });
      }
   }

   public void releaseTexture() {
      IGLFramebufferObject var1 = getFuncs();
      var1.glFramebufferTexture2D(var1.GL_FRAMEBUFFER(), var1.GL_DEPTH_ATTACHMENT(), 3553, 0, 0);
      this.texture = null;
   }

   public void endDrawing() {
      if (stack.size() != 0) {
         lastID = stack.pop();
      } else {
         lastID = 0;
      }

      IGLFramebufferObject var1 = getFuncs();
      var1.glBindFramebuffer(var1.GL_FRAMEBUFFER(), lastID);
   }

   public ITexture getTexture() {
      return this.texture;
   }

   public int getBufferId() {
      return this.id;
   }

   public boolean isDestroyed() {
      return this.texture == null || this.id == 0 || this.depth == 0;
   }

   public void startDrawing() {
      this.startDrawing(false);
   }

   public void startDrawing(boolean var1) {
      stack.push(lastID);
      lastID = this.id;
      IGLFramebufferObject var2 = getFuncs();
      var2.glBindFramebuffer(var2.GL_FRAMEBUFFER(), this.id);
      if (this.texture != null) {
         if (var1) {
            GL11.glClearDepth(1.0);
            GL11.glClear(256);
         }

      }
   }

   public void setTexture(Texture var1) {
      int var2 = lastID;
      IGLFramebufferObject var3 = getFuncs();
      var3.glBindFramebuffer(var3.GL_FRAMEBUFFER(), lastID = this.id);
      this.swapTexture(var1);
      int var10001 = var3.GL_FRAMEBUFFER();
      lastID = var2;
      var3.glBindFramebuffer(var10001, var2);
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public static int getCurrentID() {
      return lastID;
   }

   public static void reset() {
      stack.clear();
      if (lastID != 0) {
         IGLFramebufferObject var0 = getFuncs();
         int var10001 = var0.GL_FRAMEBUFFER();
         lastID = 0;
         var0.glBindFramebuffer(var10001, 0);
      }

   }
}
