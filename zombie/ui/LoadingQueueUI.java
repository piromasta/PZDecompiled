package zombie.ui;

import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;

public class LoadingQueueUI extends UIElement {
   private String strLoadingQueue = Translator.getText("UI_GameLoad_LoadingQueue");
   private String strQueuePlace = Translator.getText("UI_GameLoad_PlaceInQueue");
   private static int placeInQueue = -1;
   private Texture arrowBG = null;
   private Texture arrowFG = null;
   private double timerMultiplierAnim = 0.0;
   private int animOffset = -1;

   public LoadingQueueUI() {
      this.arrowBG = Texture.getSharedTexture("media/ui/ArrowRight_Disabled.png");
      this.arrowFG = Texture.getSharedTexture("media/ui/ArrowRight.png");
      placeInQueue = -1;
      this.onresize();
   }

   public void update() {
   }

   public void onresize() {
      this.x = 288.0;
      this.y = 101.0;
      this.width = (float)((double)Core.getInstance().getScreenWidth() - 2.0 * this.x);
      this.height = (float)((double)Core.getInstance().getScreenHeight() - 2.0 * this.y);
   }

   public void render() {
      this.onresize();
      double var1 = 0.4000000059604645;
      double var3 = 0.4000000059604645;
      double var5 = 0.4000000059604645;
      double var7 = 1.0;
      this.DrawTextureScaledColor((Texture)null, 0.0, 0.0, 1.0, (double)this.height, var1, var3, var5, var7);
      this.DrawTextureScaledColor((Texture)null, 1.0, 0.0, (double)this.width - 2.0, 1.0, var1, var3, var5, var7);
      this.DrawTextureScaledColor((Texture)null, (double)this.width - 1.0, 0.0, 1.0, (double)this.height, var1, var3, var5, var7);
      this.DrawTextureScaledColor((Texture)null, 1.0, (double)this.height - 1.0, (double)this.width - 2.0, 1.0, var1, var3, var5, var7);
      this.DrawTextureScaledColor((Texture)null, 1.0, 1.0, (double)this.width - 2.0, (double)(this.height - 2.0F), 0.0, 0.0, 0.0, 0.5);
      TextManager.instance.DrawStringCentre(UIFont.Large, this.x + (double)(this.width / 2.0F), this.y + 60.0, this.strLoadingQueue, 1.0, 1.0, 1.0, 1.0);
      this.DrawTextureColor(this.arrowBG, (double)((this.width - (float)this.arrowBG.getWidth()) / 2.0F - 15.0F), 120.0, 1.0, 1.0, 1.0, 1.0);
      this.DrawTextureColor(this.arrowBG, (double)((this.width - (float)this.arrowBG.getWidth()) / 2.0F), 120.0, 1.0, 1.0, 1.0, 1.0);
      this.DrawTextureColor(this.arrowBG, (double)((this.width - (float)this.arrowBG.getWidth()) / 2.0F + 15.0F), 120.0, 1.0, 1.0, 1.0, 1.0);
      this.timerMultiplierAnim += UIManager.getMillisSinceLastRender();
      if (this.timerMultiplierAnim <= 500.0) {
         this.animOffset = -2147483648;
      } else if (this.timerMultiplierAnim <= 1000.0) {
         this.animOffset = -15;
      } else if (this.timerMultiplierAnim <= 1500.0) {
         this.animOffset = 0;
      } else if (this.timerMultiplierAnim <= 2000.0) {
         this.animOffset = 15;
      } else {
         this.timerMultiplierAnim = 0.0;
      }

      if (this.animOffset != -2147483648) {
         this.DrawTextureColor(this.arrowFG, (double)((this.width - (float)this.arrowBG.getWidth()) / 2.0F + (float)this.animOffset), 120.0, 1.0, 1.0, 1.0, 1.0);
      }

      if (placeInQueue >= 0) {
         TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + 180.0, String.format(this.strQueuePlace, placeInQueue), 1.0, 1.0, 1.0, 1.0);
      }

   }

   public void setPlaceInQueue(int var1) {
      placeInQueue = var1;
   }
}
