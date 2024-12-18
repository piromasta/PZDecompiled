package zombie.iso.sprite;

import java.util.Objects;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;

public final class IsoReticle {
   private static IsoReticle instance = null;
   private boolean defaultColor = true;
   private boolean hasValidTarget = false;
   private int targetChance = 0;
   private int targetAimPenalty = 0;
   private float currentCrosshairOffset = 60.0F;
   private float targetCrosshairOffset = 60.0F;
   private float crosshairMinOffset = 15.0F;
   private float crosshairMaxOffset = 80.0F;
   private float crosshairDefaultOffset = 60.0F;
   private IsoReticleShader isoReticleShader = null;

   public static IsoReticle getInstance() {
      if (instance == null) {
         instance = new IsoReticle();
      }

      return instance;
   }

   private IsoReticle() {
      RenderThread.invokeOnRenderContext(this::createShader);
      if (this.isoReticleShader != null) {
         int var2 = 0;

         while(true) {
            Objects.requireNonNull(this.isoReticleShader);
            Object[] var10001;
            String var1;
            if (var2 >= 21) {
               var2 = 0;

               while(true) {
                  Objects.requireNonNull(this.isoReticleShader);
                  if (var2 >= 7) {
                     var2 = 0;

                     while(true) {
                        Objects.requireNonNull(this.isoReticleShader);
                        if (var2 >= 3) {
                           return;
                        }

                        for(int var3 = 0; var3 < 4; ++var3) {
                           var10001 = new Object[]{var2, var3};
                           var1 = "media/ui/Reticle/crosshair" + String.format("%d%d", var10001) + ".png";
                           this.isoReticleShader.crosshairTextures[var2][var3] = Texture.getSharedTexture(var1);
                        }

                        ++var2;
                     }
                  }

                  var10001 = new Object[]{var2};
                  var1 = "media/ui/Reticle/targetReticle" + String.format("%02d", var10001) + ".png";
                  this.isoReticleShader.targetTexture[var2] = Texture.getSharedTexture(var1);
                  ++var2;
               }
            }

            var10001 = new Object[]{var2};
            var1 = "media/ui/Reticle/aimCircle" + String.format("%02d", var10001) + ".png";
            this.isoReticleShader.aimTexture[var2] = Texture.getSharedTexture(var1);
            ++var2;
         }
      }
   }

   private void createShader() {
      this.isoReticleShader = new IsoReticleShader();
   }

   public void setAimColor(ColorInfo var1) {
      this.isoReticleShader.aimColorInfo.set(var1);
   }

   public void setReticleColor(ColorInfo var1) {
      this.isoReticleShader.reticleColorInfo.set(var1);
   }

   public void setChance(int var1) {
      this.targetChance = var1;
   }

   public void setAimPenalty(int var1) {
      this.targetAimPenalty = var1;
   }

   public void hasTarget(boolean var1) {
      this.hasValidTarget = var1;
   }

   public void render(int var1) {
      if (Core.getInstance().displayCursor) {
         if (Core.getInstance().getOffscreenBuffer() != null) {
            IsoPlayer var2 = IsoPlayer.players[var1];
            if (var2 != null && !var2.isDead() && var2.isAiming() && var2.PlayerIndex == 0 && var2.JoypadBind == -1) {
               if (!GameTime.isGamePaused()) {
                  if (this.isoReticleShader != null && this.isoReticleShader.isCompiled()) {
                     float var3 = 1.0F;
                     if (Core.getInstance().getOptionReticleCameraZoom()) {
                        var3 = 1.0F / Core.getInstance().getZoom(var1);
                     }

                     int var4 = (int)((float)this.isoReticleShader.aimTexture[Core.getInstance().getOptionAimTextureIndex()].getWidth() / 16.0F * var3);
                     int var5 = (int)((float)this.isoReticleShader.aimTexture[Core.getInstance().getOptionAimTextureIndex()].getHeight() / 16.0F * var3);
                     this.isoReticleShader.m_screenX = Mouse.getXA() - var4 / 2;
                     this.isoReticleShader.m_screenY = Mouse.getYA() - var5 / 2;
                     this.isoReticleShader.setWidth(var4);
                     this.isoReticleShader.setHeight(var5);
                     int var6 = IsoCamera.getScreenLeft(var1);
                     int var7 = IsoCamera.getScreenTop(var1);
                     int var8 = IsoCamera.getScreenWidth(var1);
                     int var9 = IsoCamera.getScreenHeight(var1);
                     this.crosshairMaxOffset = (float)var8 * ((float)Core.getInstance().getOptionMaxCrosshairOffset() / 100.0F);
                     switch (Core.getInstance().getOptionReticleMode()) {
                        case 0:
                        default:
                           this.targetChance = PZMath.clamp(this.targetChance, 0, 100);
                           float var10 = 1.0F - (float)this.targetChance / 100.0F;
                           this.targetCrosshairOffset = this.crosshairMinOffset + (this.crosshairMaxOffset - this.crosshairMinOffset) * var10;
                           this.crosshairMinOffset = 5.0F;
                           break;
                        case 1:
                           this.targetAimPenalty = PZMath.clamp(this.targetAimPenalty, 0, 100);
                           float var11 = (float)this.targetAimPenalty / 100.0F;
                           this.targetCrosshairOffset = this.crosshairMinOffset + (this.crosshairMaxOffset - this.crosshairMinOffset) * var11;
                           this.crosshairMinOffset = 15.0F;
                     }

                     this.currentCrosshairOffset = PZMath.lerp(this.currentCrosshairOffset, this.targetCrosshairOffset, 0.1F);
                     IndieGL.glBlendFunc(770, 771);
                     int var12 = Core.getInstance().getOptionCrosshairTextureIndex();
                     SpriteRenderer.instance.renderClamped(this.isoReticleShader.crosshairTextures[var12][0], this.isoReticleShader.m_screenX - (int)this.currentCrosshairOffset, this.isoReticleShader.m_screenY, var4, var5, var6, var7, var8, var9, 1.0F, 1.0F, 1.0F, this.isoReticleShader.alpha, (Consumer)null);
                     SpriteRenderer.instance.renderClamped(this.isoReticleShader.crosshairTextures[var12][2], this.isoReticleShader.m_screenX + (int)this.currentCrosshairOffset, this.isoReticleShader.m_screenY, var4, var5, var6, var7, var8, var9, 1.0F, 1.0F, 1.0F, this.isoReticleShader.alpha, (Consumer)null);
                     SpriteRenderer.instance.renderClamped(this.isoReticleShader.crosshairTextures[var12][1], this.isoReticleShader.m_screenX, this.isoReticleShader.m_screenY - (int)this.currentCrosshairOffset, var4, var5, var6, var7, var8, var9, 1.0F, 1.0F, 1.0F, this.isoReticleShader.alpha, (Consumer)null);
                     SpriteRenderer.instance.renderClamped(this.isoReticleShader.crosshairTextures[var12][3], this.isoReticleShader.m_screenX, this.isoReticleShader.m_screenY + (int)this.currentCrosshairOffset, var4, var5, var6, var7, var8, var9, 1.0F, 1.0F, 1.0F, this.isoReticleShader.alpha, (Consumer)null);
                     if (Core.getInstance().getOptionShowAimTexture()) {
                        SpriteRenderer.instance.StartShader(this.isoReticleShader.getID(), var1);
                        SpriteRenderer.instance.renderClamped(this.isoReticleShader.aimTexture[Core.getInstance().getOptionAimTextureIndex()], this.isoReticleShader.m_screenX, this.isoReticleShader.m_screenY, var4, var5, var6, var7, var8, var9, this.isoReticleShader.aimColorInfo.r, this.isoReticleShader.aimColorInfo.g, this.isoReticleShader.aimColorInfo.b, this.isoReticleShader.alpha, this.isoReticleShader);
                        SpriteRenderer.instance.EndShader();
                     }

                     if (Core.getInstance().getOptionShowValidTargetReticleTexture() && this.hasValidTarget) {
                        SpriteRenderer.instance.renderClamped(this.isoReticleShader.targetTexture[Core.getInstance().getOptionValidTargetReticleTextureIndex()], this.isoReticleShader.m_screenX, this.isoReticleShader.m_screenY, var4, var5, var6, var7, var8, var9, this.isoReticleShader.reticleColorInfo.r, this.isoReticleShader.reticleColorInfo.g, this.isoReticleShader.reticleColorInfo.b, this.isoReticleShader.alpha, (Consumer)null);
                     }

                     if (Core.getInstance().getOptionShowReticleTexture()) {
                        SpriteRenderer.instance.renderClamped(this.isoReticleShader.targetTexture[Core.getInstance().getOptionReticleTextureIndex()], this.isoReticleShader.m_screenX, this.isoReticleShader.m_screenY, var4, var5, var6, var7, var8, var9, this.isoReticleShader.reticleColorInfo.r, this.isoReticleShader.reticleColorInfo.g, this.isoReticleShader.reticleColorInfo.b, this.isoReticleShader.alpha, (Consumer)null);
                     }

                     if (this.defaultColor) {
                        this.isoReticleShader.aimColorInfo.set(0.5F, 0.5F, 0.5F, this.isoReticleShader.alpha);
                        this.defaultColor = false;
                     }

                     this.targetChance = 0;
                     this.targetAimPenalty = 100;
                  }
               }
            }
         }
      }
   }

   private static class IsoReticleShader extends Shader implements Consumer<TextureDraw> {
      private final int maxAimTextures = 21;
      private final int maxTargetTextures = 7;
      private final int maxCrosshairTextures = 3;
      private float alpha = 1.0F;
      private Texture[] aimTexture = new Texture[21];
      private Texture[] targetTexture = new Texture[7];
      private Texture[][] crosshairTextures = new Texture[3][4];
      private Texture textureWorld;
      private int m_screenX;
      private int m_screenY;
      private ColorInfo aimColorInfo = new ColorInfo();
      private ColorInfo reticleColorInfo = new ColorInfo();

      IsoReticleShader() {
         super("isoreticle");
      }

      public void startMainThread(TextureDraw var1, int var2) {
         this.alpha = Core.getInstance().getIsoCursorAlpha();
         this.textureWorld = Core.getInstance().OffscreenBuffer.getTexture(var2);
      }

      public void startRenderThread(TextureDraw var1) {
         this.getProgram().setValue("aimTexture", this.aimTexture[Core.getInstance().getOptionAimTextureIndex()], 0);
         this.getProgram().setValue("red", this.aimColorInfo.r);
         this.getProgram().setValue("green", this.aimColorInfo.g);
         this.getProgram().setValue("blue", this.aimColorInfo.b);
         this.getProgram().setValue("alpha", this.alpha);
         SpriteRenderer.ringBuffer.shaderChangedTexture1();
         GL11.glEnable(3042);
      }

      public void accept(TextureDraw var1) {
         byte var2 = 0;
         int var3 = (int)var1.x0 - this.m_screenX;
         int var4 = (int)var1.y0 - this.m_screenY;
         int var5 = this.m_screenX + this.getWidth() - (int)var1.x2;
         int var6 = this.m_screenY + this.getHeight() - (int)var1.y2;
         this.m_screenX += var3;
         this.m_screenY += var4;
         this.setWidth(this.getWidth() - (var3 + var5));
         this.setHeight(this.getHeight() - (var4 + var6));
         float var7 = (float)this.textureWorld.getWidthHW();
         float var8 = (float)this.textureWorld.getHeightHW();
         float var9 = (float)(IsoCamera.getScreenTop(var2) + IsoCamera.getScreenHeight(var2) - (this.m_screenY + this.getHeight()));
         var1.tex1 = this.textureWorld;
         var1.tex1_u0 = (float)this.m_screenX / var7;
         var1.tex1_v3 = var9 / var8;
         var1.tex1_u1 = (float)(this.m_screenX + this.getWidth()) / var7;
         var1.tex1_v2 = var9 / var8;
         var1.tex1_u2 = (float)(this.m_screenX + this.getWidth()) / var7;
         var1.tex1_v1 = (var9 + (float)this.getHeight()) / var8;
         var1.tex1_u3 = (float)this.m_screenX / var7;
         var1.tex1_v0 = (var9 + (float)this.getHeight()) / var8;
      }
   }
}
