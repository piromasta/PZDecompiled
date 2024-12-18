package zombie.iso;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.iso.sprite.SkyBox;

public final class PuddlesShader extends Shader {
   private int WaterGroundTex;
   private int PuddlesHM;
   private int WaterTextureReflectionA;
   private int WaterTextureReflectionB;
   private int WaterTime;
   private int WaterOffset;
   private int WaterViewport;
   private int WaterReflectionParam;
   private int PuddlesParams;

   public PuddlesShader(String var1) {
      super(var1);
   }

   protected void onCompileSuccess(ShaderProgram var1) {
      int var2 = var1.getShaderID();
      this.WaterGroundTex = GL20.glGetUniformLocation(var2, "WaterGroundTex");
      this.WaterTextureReflectionA = GL20.glGetUniformLocation(var2, "WaterTextureReflectionA");
      this.WaterTextureReflectionB = GL20.glGetUniformLocation(var2, "WaterTextureReflectionB");
      this.PuddlesHM = GL20.glGetUniformLocation(var2, "PuddlesHM");
      this.WaterTime = GL20.glGetUniformLocation(var2, "WTime");
      this.WaterOffset = GL20.glGetUniformLocation(var2, "WOffset");
      this.WaterViewport = GL20.glGetUniformLocation(var2, "WViewport");
      this.WaterReflectionParam = GL20.glGetUniformLocation(var2, "WReflectionParam");
      this.PuddlesParams = GL20.glGetUniformLocation(var2, "PuddlesParams");
      this.Start();
      if (this.WaterGroundTex != -1) {
         GL20.glUniform1i(this.WaterGroundTex, 0);
      }

      if (this.WaterTextureReflectionA != -1) {
         GL20.glUniform1i(this.WaterTextureReflectionA, 1);
      }

      if (this.WaterTextureReflectionB != -1) {
         GL20.glUniform1i(this.WaterTextureReflectionB, 2);
      }

      if (this.PuddlesHM != -1) {
         GL20.glUniform1i(this.PuddlesHM, 3);
      }

      this.End();
   }

   public void updatePuddlesParams(int var1, int var2) {
      IsoPuddles var3 = IsoPuddles.getInstance();
      SkyBox var4 = SkyBox.getInstance();
      PlayerCamera var5 = SpriteRenderer.instance.getRenderingPlayerCamera(var1);
      GL13.glActiveTexture(33985);
      var4.getTextureCurrent().bind();
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexEnvi(8960, 8704, 7681);
      GL13.glActiveTexture(33986);
      var4.getTexturePrev().bind();
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexEnvi(8960, 8704, 7681);
      GL13.glActiveTexture(33987);
      var3.getHMTexture().bind();
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexEnvi(8960, 8704, 7681);
      GL20.glUniform1f(this.WaterTime, var3.getShaderTime());
      Vector4f var6 = var3.getShaderOffset();
      GL20.glUniform4f(this.WaterOffset, var6.x - 90000.0F, var6.y - 640000.0F, var6.z, var6.w);
      GL20.glUniform4f(this.WaterViewport, (float)IsoCamera.getOffscreenLeft(var1), (float)IsoCamera.getOffscreenTop(var1), (float)var5.OffscreenWidth / var5.zoom, (float)var5.OffscreenHeight / var5.zoom);
      GL20.glUniform1f(this.WaterReflectionParam, var4.getTextureShift());
      GL20.glUniformMatrix4fv(this.PuddlesParams, true, var3.getPuddlesParams(var2));
   }
}
