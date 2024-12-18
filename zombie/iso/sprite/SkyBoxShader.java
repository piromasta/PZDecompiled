package zombie.iso.sprite;

import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

final class SkyBoxShader extends Shader {
   private int SkyBoxTime;
   private int SkyBoxParamCloudCount;
   private int SkyBoxParamCloudSize;
   private int SkyBoxParamSunLight;
   private int SkyBoxParamSunColor;
   private int SkyBoxParamSkyHColour;
   private int SkyBoxParamSkyLColour;
   private int SkyBoxParamCloudLight;
   private int SkyBoxParamStars;
   private int SkyBoxParamFog;
   private int SkyBoxParamWind;

   public SkyBoxShader(String var1) {
      super(var1);
   }

   public void startRenderThread(TextureDraw var1) {
      SkyBox var2 = SkyBox.getInstance();
      GL20.glUniform1f(this.SkyBoxTime, (float)var2.getShaderTime());
      GL20.glUniform1f(this.SkyBoxParamCloudCount, var2.getShaderCloudCount());
      GL20.glUniform1f(this.SkyBoxParamCloudSize, var2.getShaderCloudSize());
      GL20.glUniform3f(this.SkyBoxParamSunLight, var2.getShaderSunLight().x, var2.getShaderSunLight().y, var2.getShaderSunLight().z);
      GL20.glUniform3f(this.SkyBoxParamSunColor, var2.getShaderSunColor().r, var2.getShaderSunColor().g, var2.getShaderSunColor().b);
      GL20.glUniform3f(this.SkyBoxParamSkyHColour, var2.getShaderSkyHColour().r, var2.getShaderSkyHColour().g, var2.getShaderSkyHColour().b);
      GL20.glUniform3f(this.SkyBoxParamSkyLColour, var2.getShaderSkyLColour().r, var2.getShaderSkyLColour().g, var2.getShaderSkyLColour().b);
      GL20.glUniform1f(this.SkyBoxParamCloudLight, var2.getShaderCloudLight());
      GL20.glUniform1f(this.SkyBoxParamStars, var2.getShaderStars());
      GL20.glUniform1f(this.SkyBoxParamFog, var2.getShaderFog());
      GL20.glUniform3f(this.SkyBoxParamWind, var2.getShaderWind().x, var2.getShaderWind().y, var2.getShaderWind().z);
   }

   public void onCompileSuccess(ShaderProgram var1) {
      int var2 = this.getID();
      this.SkyBoxTime = GL20.glGetUniformLocation(var2, "SBTime");
      this.SkyBoxParamCloudCount = GL20.glGetUniformLocation(var2, "SBParamCloudCount");
      this.SkyBoxParamCloudSize = GL20.glGetUniformLocation(var2, "SBParamCloudSize");
      this.SkyBoxParamSunLight = GL20.glGetUniformLocation(var2, "SBParamSunLight");
      this.SkyBoxParamSunColor = GL20.glGetUniformLocation(var2, "SBParamSunColour");
      this.SkyBoxParamSkyHColour = GL20.glGetUniformLocation(var2, "SBParamSkyHColour");
      this.SkyBoxParamSkyLColour = GL20.glGetUniformLocation(var2, "SBParamSkyLColour");
      this.SkyBoxParamCloudLight = GL20.glGetUniformLocation(var2, "SBParamCloudLight");
      this.SkyBoxParamStars = GL20.glGetUniformLocation(var2, "SBParamStars");
      this.SkyBoxParamFog = GL20.glGetUniformLocation(var2, "SBParamFog");
      this.SkyBoxParamWind = GL20.glGetUniformLocation(var2, "SBParamWind");
   }
}
