package zombie.ui;

import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;

public final class ActionProgressBar extends UIElement {
   Texture background = Texture.getSharedTexture("BuildBar_Bkg");
   Texture foreground = Texture.getSharedTexture("BuildBar_Bar");
   float deltaValue = 1.0F;
   float animationProgress = 0.0F;
   public int delayHide = 0;
   private final int offsetX;
   private final int offsetY;

   public ActionProgressBar(int var1, int var2) {
      this.offsetX = var1;
      this.offsetY = var2;
      this.width = (float)this.background.getWidth();
      this.height = (float)this.background.getHeight();
      this.followGameWorld = true;
   }

   public void render() {
      if (this.isVisible() && UIManager.VisibleAllUI) {
         this.DrawUVSliceTexture(this.background, 0.0, 0.0, (double)this.background.getWidth(), (double)this.background.getHeight(), Color.white, 0.0, 0.0, 1.0, 1.0);
         if (this.deltaValue == 1.0F / 0.0F) {
            if (this.animationProgress < 0.5F) {
               this.DrawUVSliceTexture(this.foreground, 3.0, 0.0, (double)this.foreground.getWidth(), (double)this.foreground.getHeight(), Color.white, 0.0, 0.0, (double)(this.animationProgress * 2.0F), 1.0);
            } else {
               this.DrawUVSliceTexture(this.foreground, 3.0, 0.0, (double)this.foreground.getWidth(), (double)this.foreground.getHeight(), Color.white, (double)((this.animationProgress - 0.5F) * 2.0F), 0.0, 1.0, 1.0);
            }
         } else {
            this.DrawUVSliceTexture(this.foreground, 3.0, 0.0, (double)this.foreground.getWidth(), (double)this.foreground.getHeight(), Color.white, 0.0, 0.0, (double)this.deltaValue, 1.0);
         }

      }
   }

   public void setValue(float var1) {
      this.deltaValue = var1;
   }

   public float getValue() {
      return this.deltaValue;
   }

   public void update(int var1) {
      if (this.deltaValue == 1.0F / 0.0F) {
         this.animationProgress += 0.02F * GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 60.0F;
         if (this.animationProgress > 1.0F) {
            this.animationProgress = 0.0F;
         }

         this.setVisible(true);
         this.updateScreenPos(var1);
         this.delayHide = 2;
      } else {
         if (this.getValue() > 0.0F && this.getValue() < 1.0F) {
            this.setVisible(true);
            this.delayHide = 2;
         } else if (this.isVisible() && this.delayHide > 0 && --this.delayHide == 0) {
            this.setVisible(false);
         }

         if (!UIManager.VisibleAllUI) {
            this.setVisible(false);
         }

         if (this.isVisible()) {
            this.updateScreenPos(var1);
         }

      }
   }

   private void updateScreenPos(int var1) {
      IsoPlayer var2 = IsoPlayer.players[var1];
      if (var2 != null) {
         float var3 = IsoUtils.XToScreen(var2.getX(), var2.getY(), var2.getZ(), 0);
         float var4 = IsoUtils.YToScreen(var2.getX(), var2.getY(), var2.getZ(), 0);
         var3 = var3 - IsoCamera.getOffX() - var2.offsetX;
         var4 = var4 - IsoCamera.getOffY() - var2.offsetY;
         var4 -= (float)(128 / (2 / Core.TileScale));
         var3 /= Core.getInstance().getZoom(var1);
         var4 /= Core.getInstance().getZoom(var1);
         var3 -= this.width / 2.0F;
         var4 -= this.height;
         if (var2.getUserNameHeight() > 0) {
            var4 -= (float)(var2.getUserNameHeight() + 2);
         }

         this.setX((double)(var3 + (float)this.offsetX));
         this.setY((double)(var4 + (float)this.offsetY));
      }
   }
}
