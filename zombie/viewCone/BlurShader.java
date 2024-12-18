package zombie.viewCone;

import org.lwjgl.opengl.GL20;
import zombie.characters.IsoPlayer;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;

public class BlurShader extends Shader {
   private int ScreenInfo;
   private static float[][] floatArrs = new float[25][];

   public BlurShader(String var1) {
      super(var1);
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

   public void startMainThread(TextureDraw var1, int var2) {
      if (var2 >= 0 && var2 < 4) {
         RenderSettings.PlayerRenderSettings var3 = RenderSettings.getInstance().getPlayerSettings(var2);
         IsoPlayer var10000 = IsoPlayer.players[var2];
         PlayerCamera var5 = IsoCamera.cameras[var2];
         if (var1.vars == null) {
            var1.vars = getFreeFloatArray();
            if (var1.vars == null) {
               var1.vars = new float[25];
            }
         }

         var1.vars[0] = (float)IsoCamera.getOffscreenWidth(var2);
         var1.vars[1] = (float)IsoCamera.getOffscreenHeight(var2);
         var1.vars[2] = var5.RightClickX;
         var1.vars[3] = var5.RightClickY;
      }
   }

   public void startRenderThread(TextureDraw var1) {
      GL20.glUniform4f(this.ScreenInfo, var1.vars[0], var1.vars[1], var1.vars[2], var1.vars[3]);
   }

   public void onCompileSuccess(ShaderProgram var1) {
      int var2 = this.getID();
      this.ScreenInfo = GL20.glGetUniformLocation(var2, "ScreenInfo");
   }
}
