package zombie.core.opengl;

import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.textures.TextureDraw;

public class SDFShader extends Shader {
   private int sdfThreshold;
   private int sdfShadow;
   private int sdfOutlineThick;
   private int sdfOutlineColor;
   private float threshold = 0.0F;

   public SDFShader(String var1) {
      super(var1);
   }

   protected void onCompileSuccess(ShaderProgram var1) {
      int var2 = var1.getShaderID();
      this.sdfThreshold = GL20.glGetUniformLocation(var2, "sdfThreshold");
      this.sdfShadow = GL20.glGetUniformLocation(var2, "sdfShadow");
      this.sdfOutlineThick = GL20.glGetUniformLocation(var2, "sdfOutlineThick");
      this.sdfOutlineColor = GL20.glGetUniformLocation(var2, "sdfOutlineColor");
   }

   public void startRenderThread(TextureDraw var1) {
   }

   public void updateThreshold(float var1) {
      IndieGL.ShaderUpdate1f(this.getID(), this.sdfThreshold, var1);
   }

   public void updateOutline(float var1, float var2, float var3, float var4, float var5) {
      IndieGL.ShaderUpdate1f(this.getID(), this.sdfOutlineThick, (1.0F - var1) / 2.0F);
      IndieGL.ShaderUpdate4f(this.getID(), this.sdfOutlineColor, var2, var3, var4, var5);
   }

   public void updateShadow(float var1) {
      IndieGL.ShaderUpdate1f(this.getID(), this.sdfShadow, var1);
   }
}
