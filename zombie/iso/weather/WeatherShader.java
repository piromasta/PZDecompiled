package zombie.iso.weather;

import org.lwjgl.opengl.GL20;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;
import zombie.iso.SearchMode;

public class WeatherShader extends Shader {
   public int timeOfDay = 0;
   private int PixelOffset;
   private int PixelSize;
   private int bloom;
   private int timer;
   private int BlurStrength;
   private int TextureSize;
   private int Zoom;
   private int Light;
   private int LightIntensity;
   private int NightValue;
   private int Exterior;
   private int NightVisionGoggles;
   private int DesaturationVal;
   private int FogMod;
   private int SearchModeID;
   private int ScreenInfo;
   private int ParamInfo;
   private int VarInfo;
   private int DrunkFactor;
   private int BlurFactor;
   private int timerVal;
   private int timerWrap;
   private float timerWrapVal = -1.0F;
   private boolean bAlt = false;
   public static final int texdVarsSize = 25;
   private static float[][] floatArrs = new float[5][];

   public WeatherShader(String var1) {
      super(var1);
   }

   public void startMainThread(TextureDraw var1, int var2) {
      if (var2 >= 0 && var2 < 4) {
         RenderSettings.PlayerRenderSettings var3 = RenderSettings.getInstance().getPlayerSettings(var2);
         IsoPlayer var4 = IsoPlayer.players[var2];
         boolean var5 = var3.isExterior();
         float var6 = GameTime.instance.TimeOfDay / 12.0F - 1.0F;
         if (Math.abs(var6) > 0.8F && var4 != null && var4.Traits.NightVision.isSet() && !var4.isWearingNightVisionGoggles()) {
            var6 *= 0.8F;
         }

         int var7 = Core.getInstance().getOffscreenWidth(var2);
         int var8 = Core.getInstance().getOffscreenHeight(var2);
         if (var1.vars == null) {
            var1.vars = getFreeFloatArray();
            if (var1.vars == null) {
               var1.vars = new float[25];
            }
         }

         var1.vars[0] = var3.getBlendColor().r;
         var1.vars[1] = var3.getBlendColor().g;
         var1.vars[2] = var3.getBlendColor().b;
         var1.vars[3] = var3.getBlendIntensity();
         var1.vars[4] = var3.getDesaturation();
         var1.vars[5] = var3.isApplyNightVisionGoggles() ? 1.0F : 0.0F;
         SearchMode.PlayerSearchMode var9 = SearchMode.getInstance().getSearchModeForPlayer(var2);
         var1.vars[6] = var9.getShaderBlur();
         var1.vars[7] = var9.getShaderRadius();
         var1.vars[8] = (float)IsoCamera.getOffscreenLeft(var2);
         var1.vars[9] = (float)IsoCamera.getOffscreenTop(var2);
         PlayerCamera var10 = IsoCamera.cameras[var2];
         var1.vars[10] = (float)IsoCamera.getOffscreenWidth(var2);
         var1.vars[11] = (float)IsoCamera.getOffscreenHeight(var2);
         var1.vars[12] = var10.RightClickX;
         var1.vars[13] = var10.RightClickY;
         var1.vars[14] = Core.getInstance().getZoom(var2);
         var1.vars[15] = Core.TileScale == 2 ? 64.0F : 32.0F;
         var1.vars[16] = var9.getShaderGradientWidth() * var1.vars[15] / 2.0F;
         var1.vars[17] = var9.getShaderDesat();
         var1.vars[18] = var9.isShaderEnabled() ? 1.0F : 0.0F;
         var1.vars[19] = var9.getShaderDarkness();
         var1.vars[22] = var3.getDrunkFactor();
         var1.vars[23] = var3.getBlurFactor();
         var1.flipped = var3.isExterior();
         var1.f1 = var3.getDarkness();
         var1.col0 = var7;
         var1.col1 = var8;
         var1.col2 = Core.getInstance().getOffscreenTrueWidth();
         var1.col3 = Core.getInstance().getOffscreenTrueHeight();
         var1.bSingleCol = Core.getInstance().getZoom(var2) > 2.0F || (double)Core.getInstance().getZoom(var2) < 2.0 && Core.getInstance().getZoom(var2) >= 1.75F;
      }
   }

   public void startRenderThread(TextureDraw var1) {
      float var2 = var1.f1;
      boolean var3 = var1.flipped;
      int var4 = var1.col0;
      int var5 = var1.col1;
      int var6 = var1.col2;
      int var7 = var1.col3;
      float var8 = var1.bSingleCol ? 1.0F : 0.0F;
      GL20.glUniform1f(this.getWidth(), (float)var4);
      GL20.glUniform1f(this.getHeight(), (float)var5);
      GL20.glUniform1f(this.NightValue, var2);
      if (var1.vars != null) {
         GL20.glUniform3f(this.Light, var1.vars[0], var1.vars[1], var1.vars[2]);
         GL20.glUniform1f(this.LightIntensity, var1.vars[3]);
         GL20.glUniform1f(this.DesaturationVal, var1.vars[4]);
         GL20.glUniform1f(this.NightVisionGoggles, var1.vars[5]);
      }

      GL20.glUniform1f(this.Exterior, var3 ? 1.0F : 0.0F);
      GL20.glUniform1f(this.timer, (float)(this.timerVal / 2));
      GL20.glUniform1f(this.timerWrap, this.timerWrapVal);
      if (PerformanceSettings.getLockFPS() >= 60) {
         if (this.bAlt) {
            ++this.timerVal;
         }

         this.bAlt = !this.bAlt;
      } else {
         this.timerVal += 2;
      }

      this.timerWrapVal = 1.0F - 2.0F * ((float)this.timerVal / 2.14748365E9F);
      float var9 = 0.0F;
      float var10 = 0.0F;
      float var11 = 1.0F / (float)var4;
      float var12 = 1.0F / (float)var5;
      GL20.glUniform2f(this.TextureSize, (float)var6, (float)var7);
      GL20.glUniform1f(this.Zoom, var8);
      GL20.glUniform4f(this.SearchModeID, var1.vars[6], var1.vars[7], var1.vars[8], var1.vars[9]);
      GL20.glUniform4f(this.ScreenInfo, var1.vars[10], var1.vars[11], var1.vars[12], var1.vars[13]);
      GL20.glUniform4f(this.ParamInfo, var1.vars[14], var1.vars[15], var1.vars[16], var1.vars[17]);
      GL20.glUniform4f(this.VarInfo, var1.vars[18], var1.vars[19], var1.vars[20], var1.vars[21]);
      GL20.glUniform1f(this.DrunkFactor, var1.vars[22]);
      GL20.glUniform1f(this.BlurFactor, var1.vars[23]);
   }

   public void onCompileSuccess(ShaderProgram var1) {
      int var2 = this.getID();
      this.timeOfDay = GL20.glGetUniformLocation(var2, "TimeOfDay");
      this.bloom = GL20.glGetUniformLocation(var2, "BloomVal");
      this.PixelOffset = GL20.glGetUniformLocation(var2, "PixelOffset");
      this.PixelSize = GL20.glGetUniformLocation(var2, "PixelSize");
      this.BlurStrength = GL20.glGetUniformLocation(var2, "BlurStrength");
      this.setWidth(GL20.glGetUniformLocation(var2, "bgl_RenderedTextureWidth"));
      this.setHeight(GL20.glGetUniformLocation(var2, "bgl_RenderedTextureHeight"));
      this.timer = GL20.glGetUniformLocation(var2, "timer");
      this.TextureSize = GL20.glGetUniformLocation(var2, "TextureSize");
      this.Zoom = GL20.glGetUniformLocation(var2, "Zoom");
      this.Light = GL20.glGetUniformLocation(var2, "Light");
      this.LightIntensity = GL20.glGetUniformLocation(var2, "LightIntensity");
      this.NightValue = GL20.glGetUniformLocation(var2, "NightValue");
      this.Exterior = GL20.glGetUniformLocation(var2, "Exterior");
      this.NightVisionGoggles = GL20.glGetUniformLocation(var2, "NightVisionGoggles");
      this.DesaturationVal = GL20.glGetUniformLocation(var2, "DesaturationVal");
      this.FogMod = GL20.glGetUniformLocation(var2, "FogMod");
      this.SearchModeID = GL20.glGetUniformLocation(var2, "SearchMode");
      this.ScreenInfo = GL20.glGetUniformLocation(var2, "ScreenInfo");
      this.ParamInfo = GL20.glGetUniformLocation(var2, "ParamInfo");
      this.VarInfo = GL20.glGetUniformLocation(var2, "VarInfo");
      this.DrunkFactor = GL20.glGetUniformLocation(var2, "DrunkFactor");
      this.BlurFactor = GL20.glGetUniformLocation(var2, "BlurFactor");
      this.timerWrap = GL20.glGetUniformLocation(var2, "timerWrap");
   }

   public void postRender(TextureDraw var1) {
      if (var1.vars != null) {
         returnFloatArray(var1.vars);
         var1.vars = null;
      }

   }

   private static float[] getFreeFloatArray() {
      for(int var0 = 0; var0 < floatArrs.length; ++var0) {
         if (floatArrs[var0] != null) {
            float[] var1 = floatArrs[var0];
            floatArrs[var0] = null;
            return var1;
         }
      }

      return new float[25];
   }

   private static void returnFloatArray(float[] var0) {
      for(int var1 = 0; var1 < floatArrs.length; ++var1) {
         if (floatArrs[var1] == null) {
            floatArrs[var1] = var0;
            break;
         }
      }

   }
}
