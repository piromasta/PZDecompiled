package zombie.core.rendering;

import java.util.ArrayList;
import java.util.Iterator;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.Util;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;

public abstract class RenderTarget {
   private static final ArrayList<RenderTarget> ActiveRenderTargets = new ArrayList();
   private static VertexBufferObject FullScreenTri;
   private static VertexBufferObject FullScreenQuad;
   public final String name;
   public int buffer = -1;
   private boolean created = false;

   protected RenderTarget(String var1) {
      this.name = var1;
   }

   public String toString() {
      return String.format("%s: %s", super.toString(), this.name);
   }

   private static VertexBufferObject GetFullScreenTri() {
      if (FullScreenTri == null) {
         VertexBufferObject.VertexFormat var0 = new VertexBufferObject.VertexFormat(2);
         var0.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
         var0.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
         var0.calculate();
         VertexBufferObject.VertexArray var1 = new VertexBufferObject.VertexArray(var0, 3);
         var1.setElement(0, 0, -1.0F, -1.0F, 0.0F);
         var1.setElement(0, 1, 0.0F, 0.0F);
         var1.setElement(1, 0, -1.0F, 3.0F, 0.0F);
         var1.setElement(1, 1, 0.0F, 2.0F);
         var1.setElement(2, 0, 3.0F, -1.0F, 0.0F);
         var1.setElement(2, 1, 2.0F, 0.0F);
         FullScreenTri = new VertexBufferObject(var1, new int[]{0, 1, 2});
      }

      return FullScreenTri;
   }

   private static VertexBufferObject GetFullScreenQuad() {
      if (FullScreenQuad == null) {
         VertexBufferObject.VertexFormat var0 = new VertexBufferObject.VertexFormat(2);
         var0.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
         var0.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
         var0.calculate();
         VertexBufferObject.VertexArray var1 = new VertexBufferObject.VertexArray(var0, 4);
         var1.setElement(0, 0, -1.0F, -1.0F, 0.0F);
         var1.setElement(0, 1, 0.0F, 0.0F);
         var1.setElement(1, 0, -1.0F, 1.0F, 0.0F);
         var1.setElement(1, 1, 0.0F, 1.0F);
         var1.setElement(2, 0, 1.0F, -1.0F, 0.0F);
         var1.setElement(2, 1, 1.0F, 0.0F);
         var1.setElement(3, 0, 1.0F, 1.0F, 0.0F);
         var1.setElement(3, 1, 1.0F, 1.0F);
         FullScreenQuad = new VertexBufferObject(var1, new int[]{0, 1, 2, 1, 3, 2});
      }

      return FullScreenQuad;
   }

   public static RenderTarget GetTarget(String var0) {
      Iterator var1 = ActiveRenderTargets.iterator();

      RenderTarget var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (RenderTarget)var1.next();
      } while(!var2.name.equals(var0));

      return var2;
   }

   public static void UnbindTarget() {
      GL30.glBindFramebuffer(36160, 0);
   }

   public final RenderTarget Create() {
      if (this.created) {
         return this;
      } else {
         this.OnCreate();
         int var1 = GL30.glCheckFramebufferStatus(36160);
         if (var1 != 36053) {
            this.Destroy();
            String var10000;
            switch (var1) {
               case 36054:
                  var10000 = "Incomplete attachment";
                  break;
               case 36055:
                  var10000 = "Incomplete missing attachment";
                  break;
               case 36059:
                  var10000 = "Incomplete draw buffer";
                  break;
               case 36060:
                  var10000 = "Incomplete read buffer";
                  break;
               case 36061:
                  var10000 = "Format combination is unsupported";
                  break;
               case 36182:
                  var10000 = "Number of samples do not match";
                  break;
               case 36264:
                  var10000 = "Texture not layered or from different target";
                  break;
               default:
                  var10000 = "Unknown error";
            }

            String var2 = var10000;
            throw new OpenGLException("Failed to create framebuffer - " + var2);
         } else {
            Util.checkGLError();
            this.created = true;
            ActiveRenderTargets.add(this);
            return this;
         }
      }
   }

   public final void Destroy() {
      this.OnDestroy();
      ActiveRenderTargets.remove(this);
      this.created = false;
   }

   public void BindRead() {
      assert this.buffer != -1;

      GL30.glBindFramebuffer(36008, this.buffer);
   }

   public void BindDraw() {
      assert this.buffer != -1;

      GL30.glBindFramebuffer(36009, this.buffer);
      GL30.glViewport(0, 0, this.GetWidth(), this.GetHeight());
   }

   protected static int GetFormatType(int var0) {
      char var10000;
      switch (var0) {
         case 32852:
         case 32859:
         case 33189:
         case 33322:
         case 33324:
         case 33332:
         case 33338:
         case 36214:
         case 36215:
            var10000 = 5123;
            break;
         case 33191:
         case 33334:
         case 33340:
         case 36208:
         case 36209:
            var10000 = 5125;
            break;
         case 33325:
         case 33327:
         case 34842:
         case 34843:
            var10000 = 5131;
            break;
         case 33326:
         case 33328:
         case 34836:
         case 34837:
         case 36012:
            var10000 = 5126;
            break;
         case 33329:
         case 33335:
         case 36238:
         case 36239:
         case 36756:
         case 36757:
         case 36758:
         case 36759:
            var10000 = 5120;
            break;
         case 33331:
         case 33337:
         case 36232:
         case 36233:
         case 36760:
         case 36761:
         case 36762:
         case 36763:
            var10000 = 5122;
            break;
         case 33333:
         case 33339:
         case 36226:
         case 36227:
            var10000 = 5124;
            break;
         case 35056:
            var10000 = '蓺';
            break;
         case 36013:
            var10000 = '趭';
            break;
         default:
            var10000 = 5121;
      }

      return var10000;
   }

   protected static int GetInternalFormat(int var0) {
      short var10000;
      switch (var0) {
         case 32852:
         case 32859:
         case 33322:
         case 33324:
         case 33332:
         case 33338:
         case 36214:
         case 36215:
            var10000 = 5123;
            break;
         case 33325:
         case 33327:
         case 34842:
         case 34843:
            var10000 = 5131;
            break;
         case 33326:
         case 33328:
         case 34836:
         case 34837:
            var10000 = 5126;
            break;
         case 33329:
         case 33335:
         case 36238:
         case 36239:
         case 36756:
         case 36757:
         case 36758:
         case 36759:
            var10000 = 5120;
            break;
         case 33331:
         case 33337:
         case 36232:
         case 36233:
         case 36760:
         case 36761:
         case 36762:
         case 36763:
            var10000 = 5122;
            break;
         case 33333:
         case 33339:
         case 36226:
         case 36227:
            var10000 = 5124;
            break;
         case 33334:
         case 33340:
         case 36208:
         case 36209:
            var10000 = 5125;
            break;
         default:
            var10000 = 6408;
      }

      return var10000;
   }

   public abstract int GetWidth();

   public abstract int GetHeight();

   protected abstract void OnCreate();

   protected abstract void OnDestroy();

   public abstract void BindTexture();

   public void Blit(RenderTarget var1) {
      this.Blit(var1, (Shader)null);
   }

   public void Blit(RenderTarget var1, Shader var2) {
      if (var2 == null) {
         var2 = ShaderManager.instance.getOrCreateShader("blit", false, false);
      }

      if (var1 == null) {
         GL30.glBindFramebuffer(36009, 0);
      } else {
         var1.BindDraw();
      }

      GL20.glUseProgram(var2.getShaderProgram().getShaderID());
      GL20.glActiveTexture(33984);
      this.BindTexture();
      DrawFullScreenTri();
   }

   public static void DrawFullScreenTri() {
      DrawVBO(GetFullScreenTri());
   }

   public static void DrawFullScreenQuad() {
      DrawVBO(GetFullScreenQuad());
   }

   private static void DrawVBO(VertexBufferObject var0) {
      GL11.glPushAttrib(8);
      GL11.glPushClientAttrib(2);
      GL11.glDisable(2884);
      var0.Draw((Shader)null);
      GL11.glPopClientAttrib();
      GL11.glPopAttrib();
   }
}
