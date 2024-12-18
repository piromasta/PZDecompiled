package zombie.ui;

import java.util.HashMap;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoUtils;
import zombie.world.Wind;

public class LoadingQueueUI extends UIElement {
   private String strLoadingQueue = Translator.getText("UI_GameLoad_LoadingQueue");
   private String strQueuePlace = Translator.getText("UI_GameLoad_PlaceInQueue");
   private static int placeInQueue = -1;
   private static HashMap<String, Object> serverInformation = new HashMap();
   private double timerServerInformationAnim = 0.0;
   private Texture arrowBG = null;
   private Texture arrowFG = null;
   private Texture[] moons = new Texture[8];
   private Texture[] windsock = new Texture[6];
   private double timerMultiplierAnim = 0.0;
   private int animOffset = -1;

   public LoadingQueueUI() {
      this.arrowBG = Texture.getSharedTexture("media/ui/ArrowRight_Disabled.png");
      this.arrowFG = Texture.getSharedTexture("media/ui/ArrowRight.png");

      int var1;
      for(var1 = 0; var1 < 8; ++var1) {
         this.moons[var1] = Texture.getSharedTexture("media/ui/queue/moonN" + (var1 + 1) + ".png");
      }

      for(var1 = 0; var1 < 6; ++var1) {
         this.windsock[var1] = Texture.getSharedTexture("media/ui/queue/windsock" + (var1 + 1) + ".png");
      }

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

      if (serverInformation != null) {
         try {
            this.timerServerInformationAnim += UIManager.getMillisSinceLastRender();
            if (this.timerServerInformationAnim / 40000.0 > 1.0) {
               this.timerServerInformationAnim -= 40000.0;
            }

            float var9 = IsoUtils.smoothstep(0.0F, 2000.0F, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(10000.0F, 8000.0F, (float)this.timerServerInformationAnim);
            float var10 = IsoUtils.smoothstep(10000.0F, 12000.0F, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(20000.0F, 18000.0F, (float)this.timerServerInformationAnim);
            float var11 = IsoUtils.smoothstep(20000.0F, 22000.0F, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(30000.0F, 28000.0F, (float)this.timerServerInformationAnim);
            float var12 = IsoUtils.smoothstep(30000.0F, 32000.0F, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(40000.0F, 38000.0F, (float)this.timerServerInformationAnim);
            if (var9 > 0.0F) {
               int var13 = 240;
               float var14 = 0.5F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, String.format(Translator.getText("UI_GameLoad_PlayerPopulation"), serverInformation.get("countPlayers"), serverInformation.get("maxPlayers")), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               var13 += 30;
               var14 += 0.1F;
               Integer var15 = (Integer)serverInformation.get("ZombiesKilledToday");
               if (var15 == 0) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, Translator.getText("UI_GameLoad_zombieKilledToday0"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               } else if (var15 == 1) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, Translator.getText("UI_GameLoad_zombieKilledToday1"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               } else {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, String.format(Translator.getText("UI_GameLoad_zombieKilledTodayN"), var15), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               }

               var13 += 30;
               var14 += 0.1F;
               Integer var16 = (Integer)serverInformation.get("ZombifiedPlayersToday");
               if (var16 == 0) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, Translator.getText("UI_GameLoad_zombifiedPlayersToday0"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               } else if (var16 == 1) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, Translator.getText("UI_GameLoad_zombifiedPlayersToday1"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               } else {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, String.format(Translator.getText("UI_GameLoad_zombifiedPlayersTodayN"), var16), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               }

               var13 += 30;
               var14 += 0.1F;
               Integer var17 = (Integer)serverInformation.get("BurnedCorpsesToday");
               if (var17 == 0) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, Translator.getText("UI_GameLoad_burnedZombiesToday0"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               } else if (var17 == 1) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, Translator.getText("UI_GameLoad_burnedZombiesToday1"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               } else {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var13, String.format(Translator.getText("UI_GameLoad_burnedZombiesTodayN"), var17), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var14 - 0.2F, var14, var9));
               }
            }

            float var21;
            int var22;
            if (var10 > 0.0F) {
               var21 = 0.5F;
               var22 = 240;
               Byte var23 = (Byte)serverInformation.get("Hour");
               Byte var25 = (Byte)serverInformation.get("Minutes");
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, String.format(Translator.getText("UI_GameLoad_time"), var23, var25), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var10));
               var22 += 30;
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, String.format("%d/%d/%d", serverInformation.get("Month"), serverInformation.get("Day"), serverInformation.get("Year")), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var10));
               var22 += 30;
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, String.format(Translator.getText("UI_GameLoad_temperature"), serverInformation.get("Temperature")), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var10));
               var22 += 30;
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, String.format(Translator.getText("UI_GameLoad_humidity"), (Float)serverInformation.get("Humidity") * 100.0F), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var10));
            }

            if (var11 > 0.0F) {
               var21 = 0.5F;
               short var26 = 240;
               float var24 = (Float)serverInformation.get("WindspeedKph");
               int var27 = Wind.getBeaufortNumber(var24);
               String var28 = Wind.getName(var27);
               String var18 = Wind.getDescription(var27);
               this.DrawTextureScaled(this.windsock[Wind.getWindsockSegments(var24)], (double)((this.width - 100.0F) / 2.0F), (double)var26, 100.0, 100.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
               var22 = var26 + 130;
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, String.format(Translator.getText("UI_GameLoad_windSpeed"), var28, (int)Wind.getWindKnots(var24), (int)var24), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
               var22 += 30;
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, var18, 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
               var22 += 30;
               var21 += 0.1F;
               float var19 = (Float)serverInformation.get("Fog");
               if ((double)var19 < 0.2) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, Translator.getText("UI_GameLoad_fogNo"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
               } else if ((double)var19 > 0.8) {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, Translator.getText("UI_GameLoad_fogMedium"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
               } else {
                  TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, Translator.getText("UI_GameLoad_fogHeavy"), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
               }

               var22 += 30;
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + (double)var22, Translator.getText("UI_GameLoad_season" + serverInformation.get("SeasonId")), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var11));
            }

            if (var12 > 0.0F) {
               var21 = 0.5F;
               this.DrawTextureScaled(this.moons[Math.max(0, Math.min(7, (Byte)serverInformation.get("Moon")))], (double)((this.width - 100.0F) / 2.0F), 240.0, 100.0, 100.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var12));
               var21 += 0.1F;
               TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0F), this.y + 240.0 + 30.0 + 100.0, Translator.getText("UI_GameLoad_moon" + serverInformation.get("Moon")), 1.0, 1.0, 1.0, (double)IsoUtils.smoothstep(var21 - 0.2F, var21, var12));
            }
         } catch (Exception var20) {
            DebugLog.General.printException(var20, "LoadingQueueUI render failed", LogSeverity.Error);
         }
      }

   }

   public void setPlaceInQueue(int var1) {
      placeInQueue = var1;
   }

   public void setServerInformation(HashMap<String, Object> var1) {
      serverInformation.clear();
      serverInformation.putAll(var1);
   }
}
