package zombie.iso;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

public final class SmokeShader extends Shader {
   private int mvpMatrix;
   private int FireTime;
   private int FireParam;
   private int FireTexture;

   public SmokeShader(String var1) {
      super(var1);
   }

   protected void onCompileSuccess(ShaderProgram var1) {
      int var2 = var1.getShaderID();
      this.FireTexture = GL20.glGetUniformLocation(var2, "FireTexture");
      this.mvpMatrix = GL20.glGetUniformLocation(var2, "mvpMatrix");
      this.FireTime = GL20.glGetUniformLocation(var2, "FireTime");
      this.FireParam = GL20.glGetUniformLocation(var2, "FireParam");
      this.Start();
      if (this.FireTexture != -1) {
         GL20.glUniform1i(this.FireTexture, 0);
      }

      this.End();
   }

   public void updateSmokeParams(TextureDraw var1, int var2, float var3) {
      ParticlesFire var4 = ParticlesFire.getInstance();
      GL13.glActiveTexture(33984);
      var4.getFireSmokeTexture().bind();
      GL11.glTexEnvi(8960, 8704, 7681);
      GL20.glUniformMatrix4fv(this.mvpMatrix, true, var4.getMVPMatrix());
      GL20.glUniform1f(this.FireTime, var3);
      GL20.glUniformMatrix3fv(this.FireParam, true, var4.getParametersFire());
      if (this.FireTexture != -1) {
         GL20.glUniform1i(this.FireTexture, 0);
      }

   }
}
