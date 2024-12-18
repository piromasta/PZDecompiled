package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.textures.ColorInfo;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.tileDepth.TileDepthModifier;

public final class IsoWallBloodSplat {
   private static final ColorInfo info = new ColorInfo();
   public float worldAge;
   public IsoSprite sprite;

   public IsoWallBloodSplat() {
   }

   public IsoWallBloodSplat(float var1, IsoSprite var2) {
      this.worldAge = var1;
      this.sprite = var2;
   }

   public void render(float var1, float var2, float var3, ColorInfo var4) {
      if (this.sprite != null) {
         if (!this.sprite.hasNoTextures()) {
            int var5 = Core.TileScale;
            int var6 = 32 * var5;
            int var7 = 96 * var5;
            if (IsoSprite.globalOffsetX == -1.0F) {
               IsoSprite.globalOffsetX = -IsoCamera.frameState.OffX;
               IsoSprite.globalOffsetY = -IsoCamera.frameState.OffY;
            }

            float var8 = IsoSprite.globalOffsetX;
            float var9 = IsoSprite.globalOffsetY;
            if (FBORenderChunkManager.instance.isCaching()) {
               var8 = FBORenderChunkManager.instance.getXOffset();
               var9 = FBORenderChunkManager.instance.getYOffset();
               var1 -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wx * 8);
               var2 -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wy * 8);
            }

            float var10 = IsoUtils.XToScreen(var1, var2, var3, 0);
            float var11 = IsoUtils.YToScreen(var1, var2, var3, 0);
            var10 -= (float)var6;
            var11 -= (float)var7;
            var10 += var8;
            var11 += var9;
            if (!PerformanceSettings.FBORenderChunk) {
               label60: {
                  if (!(var10 >= (float)IsoCamera.frameState.OffscreenWidth) && !(var10 + (float)(64 * var5) <= 0.0F)) {
                     if (!(var11 >= (float)IsoCamera.frameState.OffscreenHeight) && !(var11 + (float)(128 * var5) <= 0.0F)) {
                        break label60;
                     }

                     return;
                  }

                  return;
               }
            }

            info.r = 0.7F * var4.r;
            info.g = 0.9F * var4.g;
            info.b = 0.9F * var4.b;
            info.a = 0.4F;
            float var12 = (float)GameTime.getInstance().getWorldAgeHours();
            float var13 = var12 - this.worldAge;
            ColorInfo var10000;
            if (var13 >= 0.0F && var13 < 72.0F) {
               float var14 = 1.0F - var13 / 72.0F;
               var10000 = info;
               var10000.r *= 0.2F + var14 * 0.8F;
               var10000 = info;
               var10000.g *= 0.2F + var14 * 0.8F;
               var10000 = info;
               var10000.b *= 0.2F + var14 * 0.8F;
               var10000 = info;
               var10000.a *= 0.25F + var14 * 0.75F;
            } else {
               var10000 = info;
               var10000.r *= 0.2F;
               var10000 = info;
               var10000.g *= 0.2F;
               var10000 = info;
               var10000.b *= 0.2F;
               var10000 = info;
               var10000.a *= 0.25F;
            }

            info.a = Math.max(info.a, 0.15F);
            if (PerformanceSettings.FBORenderChunk) {
               SpriteRenderer.instance.StartShader(SceneShaderStore.DefaultShaderID, IsoCamera.frameState.playerIndex);
               IndieGL.enableDepthTest();
               IndieGL.glDepthMask(false);
               IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
            }

            this.sprite.render((IsoObject)null, var1, var2, var3, IsoDirections.N, (float)var6, (float)var7, info, false, TileDepthModifier.instance);
            if (PerformanceSettings.FBORenderChunk) {
               IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
            }

         }
      }
   }

   public void save(ByteBuffer var1) {
      var1.putFloat(this.worldAge);
      var1.putInt(this.sprite.ID);
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.worldAge = var1.getFloat();
      int var3 = var1.getInt();
      this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, var3);
   }
}
