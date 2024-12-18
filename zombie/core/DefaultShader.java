package zombie.core;

import org.lwjgl.opengl.GL20;
import org.lwjglx.opengl.Util;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

public class DefaultShader extends Shader {
   public static boolean isActive;
   private static int DIFFUSE;
   private static float[][] floatArrs = new float[25][];
   private boolean textureActive;
   private boolean textureActiveCached = true;
   private int useTexture;
   private int zDepth;
   private float cachedZ = 0.0F;

   public DefaultShader(String var1) {
      super(var1);
   }

   public void startMainThread(TextureDraw var1, int var2) {
   }

   public void startRenderThread(TextureDraw var1) {
      GL20.glUniform1i(DIFFUSE, 0);
   }

   public void onCompileSuccess(ShaderProgram var1) {
      super.onCompileSuccess(var1);
      int var2 = this.getID();
      DIFFUSE = GL20.glGetUniformLocation(var2, "DIFFUSE");
      this.zDepth = GL20.glGetUniformLocation(var2, "zDepth");
      this.useTexture = GL20.glGetUniformLocation(var2, "useTexture");
   }

   public void setTextureActive(boolean var1) {
      this.textureActive = var1;
      if (this.textureActive != this.textureActiveCached) {
         GL20.glUniform1i(this.useTexture, this.textureActive ? 1 : 0);
         Util.checkGLError();
      }

      this.textureActiveCached = this.textureActive;
   }

   public void setZ(float var1) {
      if (this.cachedZ != var1) {
         GL20.glUniform1f(this.zDepth, var1);
         this.cachedZ = var1;
      }

   }

   public void setChunkDepth(float var1) {
      this.getProgram().setValue("chunkDepth", var1);
   }
}
