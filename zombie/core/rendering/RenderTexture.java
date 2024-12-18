package zombie.core.rendering;

import org.lwjgl.opengl.GL44;
import org.lwjgl.util.Rectangle;
import org.lwjglx.opengl.Util;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public class RenderTexture extends RenderTarget {
   private static VertexBufferObject FullScreenTri;
   private Descriptor descriptor;
   public int colour = -1;
   public int depth = -1;
   public int stencil = -1;
   public int width = 0;
   public int height = 0;
   public int length = 0;
   public int colourFormat = 35907;
   public int depthFormat = 0;
   public boolean depthAsTexture = false;
   public int wrappingMode = 33071;

   public RenderTexture(String var1) {
      super(var1);
      this.descriptor = new Descriptor(var1);
   }

   public RenderTexture(Descriptor var1) {
      super(var1.name);
      this.width = var1.width;
      this.height = var1.height;
      this.length = var1.length;
      this.colourFormat = var1.colourFormat;
      this.depthFormat = var1.depthFormat;
      this.depthAsTexture = var1.depthAsTexture;
      this.wrappingMode = var1.wrappingMode;
      this.descriptor = new Descriptor(var1);
   }

   public int GetWidth() {
      return this.width;
   }

   public int GetHeight() {
      return this.height;
   }

   protected void OnCreate() {
      if (this.width == 0) {
         this.width = Core.width;
      }

      if (this.height == 0) {
         this.height = Core.height;
      }

      this.buffer = GL44.glGenFramebuffers();
      GL44.glBindFramebuffer(36160, this.buffer);
      if (this.colourFormat != 0) {
         this.CreateColourTexture();
      }

      if (this.depthFormat != 0) {
         this.CreateDepthTexture();
      }

   }

   protected void OnDestroy() {
      if (this.colour != -1) {
         GL44.glDeleteTextures(this.colour);
      }

      if (this.depth != -1) {
         if (this.depthAsTexture) {
            GL44.glDeleteTextures(this.depth);
         } else {
            GL44.glDeleteRenderbuffers(this.depth);
         }
      }

      if (this.stencil != -1) {
         GL44.glDeleteTextures(this.stencil);
      }

      GL44.glDeleteFramebuffers(this.buffer);
      this.colour = -1;
      this.depth = -1;
      this.stencil = -1;
      this.buffer = -1;
   }

   public void BindRead() {
      super.BindRead();
      GL44.glReadBuffer(36064);
   }

   public void BindDraw() {
      super.BindDraw();
      GL44.glDrawBuffer(36064);
   }

   public void BindTexture() {
      GL44.glBindTexture(3553, this.colour);
   }

   public void BindDepth() {
      assert this.depthAsTexture && this.depth > 0;

      GL44.glBindTexture(3553, this.depth);
   }

   public void BindStencil() {
      assert this.depthAsTexture && this.stencil > 0;

      GL44.glBindTexture(3553, this.stencil);
   }

   private void Copy(Descriptor var1) {
      this.width = var1.width;
      this.height = var1.height;
      this.length = var1.length;
      this.colourFormat = var1.colourFormat;
      this.depthFormat = var1.depthFormat;
      this.depthAsTexture = var1.depthAsTexture;
      this.wrappingMode = var1.wrappingMode;
   }

   public void CopyTexture(RenderTarget var1) {
      int var2 = 0;
      int var3 = this.width;
      int var4 = this.height;
      if (var1 != null) {
         var2 = var1.buffer;
         var3 = var1.GetWidth();
         var4 = var1.GetHeight();
      }

      GL44.glBindFramebuffer(36008, this.buffer);
      GL44.glBindFramebuffer(36009, var2);
      GL44.glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, var3, var4, 16384, 9728);
   }

   public void CopyTexture(RenderTarget var1, Rectangle var2, Rectangle var3) {
      GL44.glBindFramebuffer(36008, this.buffer);
      GL44.glBindFramebuffer(36009, var1 == null ? 0 : var1.buffer);
      GL44.glBlitFramebuffer(var2.getX(), var2.getY(), var2.getX() + var2.getWidth(), var2.getY() + var2.getHeight(), var3.getX(), var3.getY(), var3.getX() + var3.getWidth(), var3.getY() + var3.getHeight(), 16384, 9728);
   }

   public RenderTarget Recreate() {
      if (this.buffer == -1) {
         this.descriptor.Copy(this);
         return this.Create();
      } else {
         int var1 = this.width == 0 ? Core.width : this.width;
         int var2 = this.height == 0 ? Core.height : this.height;
         GL44.glBindFramebuffer(36160, this.buffer);
         if (this.colour >= 0) {
            if (this.colourFormat == 0) {
               GL44.glFramebufferTexture(36160, 36064, 0, 0);
               GL44.glDeleteTextures(this.colour);
               this.colour = -1;
            } else if (this.colourFormat != this.descriptor.colourFormat || this.descriptor.width != var1 || this.descriptor.height != var2) {
               int var3 = this.length == 0 ? 3553 : '谚';
               GL44.glBindTexture(var3, this.colour);
               int var4 = GetFormatType(this.colourFormat);
               GL44.glTexStorage2D(var3, this.length, this.colourFormat, var1, var2);
            }
         } else if (this.colourFormat != 0) {
            this.CreateColourTexture();
         }

         if (this.depth >= 0) {
            if (this.depthFormat == 0) {
               if (this.depthAsTexture) {
                  GL44.glFramebufferTexture(36160, 36096, 0, 0);
                  GL44.glDeleteTextures(this.colour);
               } else {
                  GL44.glFramebufferRenderbuffer(36160, 36096, 36161, 0);
                  GL44.glDeleteRenderbuffers(this.depth);
               }

               this.depth = -1;
            } else if (this.depthFormat != this.descriptor.depthFormat || this.width != var1 || this.height != var2) {
               GL44.glBindRenderbuffer(36161, this.depth);
               GL44.glRenderbufferStorage(36161, this.depthFormat, this.width, this.height);
            }
         }

         this.width = var1;
         this.height = var2;
         this.descriptor.Copy(this);
         return this;
      }
   }

   private void AttachTexture(int var1, int var2) {
      GL44.glFramebufferTexture2D(36160, var1, 3553, var2, 0);
   }

   private int CreateTextureOrBuffer(int var1, int var2, boolean var3, int var4) {
      int var5;
      if (var3) {
         var5 = GL44.glGenTextures();
         GL44.glBindTexture(3553, var5);
         GL44.glTexStorage2D(3553, 1, var1, this.width, this.height);
         Util.checkGLError();
         GL44.glTexParameteri(3553, 10240, var4);
         GL44.glTexParameteri(3553, 10241, var4);
         GL44.glTexParameteri(3553, 10242, this.wrappingMode);
         GL44.glTexParameteri(3553, 10243, this.wrappingMode);
         Util.checkGLError();
         this.AttachTexture(var2, var5);
      } else {
         var5 = GL44.glGenRenderbuffers();
         GL44.glBindRenderbuffer(36161, var5);
         GL44.glRenderbufferStorage(36161, var1, this.width, this.height);
         GL44.glFramebufferRenderbuffer(36160, var2, 36161, var5);
      }

      Util.checkGLError();
      return var5;
   }

   private void CreateColourTexture() {
      this.colour = this.CreateTextureOrBuffer(this.colourFormat, 36064, true, 9729);
      Util.checkGLError();
   }

   private void CreateDepthTexture() {
      if (this.depthFormat != 35056 && this.depthFormat != 36013) {
         this.depth = this.CreateTextureOrBuffer(this.depthFormat, 36096, this.depthAsTexture, 9728);
      } else {
         this.depth = this.CreateTextureOrBuffer(this.depthFormat, 33306, this.depthAsTexture, 9728);
         Util.checkGLError();
         if (this.depthAsTexture) {
            this.stencil = GL44.glGenTextures();
            Util.checkGLError();
            GL44.glTextureView(this.stencil, 3553, this.depth, this.depthFormat, 0, 1, 0, 1);
            Util.checkGLError();
            GL44.glBindTexture(3553, this.stencil);
            Util.checkGLError();
            GL44.glTexParameteri(3553, 37098, 6401);
            Util.checkGLError();
         }
      }

      Util.checkGLError();
   }

   public static RenderTexture GetTarget(String var0, boolean var1) {
      Object var2 = GetTarget(var0);

      assert var2 == null || var2 instanceof RenderTexture;

      if (var2 == null && var1) {
         var2 = new RenderTexture(var0);
      }

      return (RenderTexture)var2;
   }

   public static RenderTexture GetTexture(Descriptor var0) {
      RenderTarget var1 = GetTarget(var0.name);

      assert var1 == null || var1 instanceof RenderTexture;

      if (var1 == null) {
         return new RenderTexture(var0);
      } else {
         RenderTexture var2 = (RenderTexture)var1;
         if (var2.width != var0.width || var2.height != var0.height || var2.colourFormat != var0.colourFormat || var2.depthFormat != var0.depthFormat) {
            var2.Copy(var0);
            var2.Recreate();
         }

         return var2;
      }
   }

   public static class Descriptor {
      public final String name;
      public int width = 0;
      public int height = 0;
      public int length = 0;
      public int colourFormat = 35907;
      public int depthFormat = 0;
      public boolean depthAsTexture = false;
      public int wrappingMode = 33071;

      public Descriptor(String var1) {
         this.name = var1;
      }

      public Descriptor(Descriptor var1) {
         this.name = var1.name;
         this.width = var1.width;
         this.height = var1.height;
         this.length = var1.length;
         this.colourFormat = var1.colourFormat;
         this.depthFormat = var1.depthFormat;
         this.depthAsTexture = var1.depthAsTexture;
         this.wrappingMode = var1.wrappingMode;
      }

      private void Copy(RenderTexture var1) {
         this.width = var1.width;
         this.height = var1.height;
         this.length = var1.length;
         this.colourFormat = var1.colourFormat;
         this.depthFormat = var1.depthFormat;
         this.depthAsTexture = var1.depthAsTexture;
         this.wrappingMode = var1.wrappingMode;
      }
   }
}
