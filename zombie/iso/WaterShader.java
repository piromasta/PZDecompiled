package zombie.iso;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.sprite.SkyBox;

public final class WaterShader extends Shader {
   private int WaterGroundTex;
   private int WaterTextureReflectionA;
   private int WaterTextureReflectionB;
   private int WaterTime;
   private int WaterOffset;
   private int WaterViewport;
   private int WaterReflectionParam;
   private int WaterParamWind;
   private int WaterParamWindSpeed;
   private int WaterParamRainIntensity;

   public WaterShader(String var1) {
      super(var1);
   }

   protected void onCompileSuccess(ShaderProgram var1) {
      int var2 = var1.getShaderID();
      this.WaterGroundTex = GL20.glGetUniformLocation(var2, "WaterGroundTex");
      this.WaterTextureReflectionA = GL20.glGetUniformLocation(var2, "WaterTextureReflectionA");
      this.WaterTextureReflectionB = GL20.glGetUniformLocation(var2, "WaterTextureReflectionB");
      this.WaterTime = GL20.glGetUniformLocation(var2, "WTime");
      this.WaterOffset = GL20.glGetUniformLocation(var2, "WOffset");
      this.WaterViewport = GL20.glGetUniformLocation(var2, "WViewport");
      this.WaterReflectionParam = GL20.glGetUniformLocation(var2, "WReflectionParam");
      this.WaterParamWind = GL20.glGetUniformLocation(var2, "WParamWind");
      this.WaterParamWindSpeed = GL20.glGetUniformLocation(var2, "WParamWindSpeed");
      this.WaterParamRainIntensity = GL20.glGetUniformLocation(var2, "WParamRainIntensity");
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

      this.End();
   }

   public void startMainThread(TextureDraw var1, int var2) {
      IsoWater var3 = IsoWater.getInstance();
      SkyBox var4 = SkyBox.getInstance();
      var1.u0 = var3.getWaterWindX();
      var1.u1 = var3.getWaterWindY();
      var1.u2 = var3.getWaterWindSpeed();
      var1.u3 = IsoPuddles.getInstance().getRainIntensity();
      var1.v0 = var3.getShaderTime();
      var1.v1 = var4.getTextureShift();
   }

   public void updateWaterParams(TextureDraw var1, int var2) {
      IsoWater var3 = IsoWater.getInstance();
      SkyBox var4 = SkyBox.getInstance();
      PlayerCamera var5 = SpriteRenderer.instance.getRenderingPlayerCamera(var2);
      GL13.glActiveTexture(33984);
      GL11.glEnable(3553);
      var3.getTextureBottom().bind();
      GL11.glTexEnvi(8960, 8704, 7681);
      GL13.glActiveTexture(33985);
      GL11.glEnable(3553);
      var4.getTextureCurrent().bind();
      Texture.lastTextureID = -1;
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexEnvi(8960, 8704, 7681);
      GL13.glActiveTexture(33986);
      GL11.glEnable(3553);
      var4.getTexturePrev().bind();
      Texture.lastTextureID = -1;
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexEnvi(8960, 8704, 7681);
      GL20.glUniform1f(this.WaterTime, var1.v0);
      Vector4f var6 = var3.getShaderOffset();
      GL20.glUniform4f(this.WaterOffset, var6.x - 90000.0F, var6.y - 640000.0F, var6.z, var6.w);
      GL20.glUniform4f(this.WaterViewport, (float)IsoCamera.getOffscreenLeft(var2), (float)IsoCamera.getOffscreenTop(var2), (float)var5.OffscreenWidth / var5.zoom, (float)var5.OffscreenHeight / var5.zoom);
      GL20.glUniform1f(this.WaterReflectionParam, var1.v1);
      GL20.glUniform2f(this.WaterParamWind, var1.u0, var1.u1);
      GL20.glUniform1f(this.WaterParamWindSpeed, var1.u2);
      GL20.glUniform1f(this.WaterParamRainIntensity, var1.u3);
   }
}
