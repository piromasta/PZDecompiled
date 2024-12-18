package zombie.ai.states.animals;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class AnimalAlertedState extends State {
   private static final AnimalAlertedState _instance = new AnimalAlertedState();
   float alertedFor = 0.0F;
   float spottedDist = 0.0F;

   public AnimalAlertedState() {
   }

   public static AnimalAlertedState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      this.alertedFor = 0.0F;
      if (var2.alertedChr != null && var2.alertedChr.getCurrentSquare() != null) {
         this.spottedDist = var2.alertedChr.getCurrentSquare().DistToProper((int)var2.getX(), (int)var2.getY());
      }

   }

   public void execute(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      if (var2.alertedChr != null && var2.alertedChr.getCurrentSquare() != null) {
         this.setTurnAlertedValues(var2, var2.alertedChr);
         if (!GameClient.bClient) {
            if (this.alertedFor > 1000.0F) {
               var2.setIsAlerted(false);
               var2.alertedChr = null;
               var2.setDefaultState();
            }

            this.alertedFor += GameTime.getInstance().getMultiplier();
            if (var2.alertedChr != null && var2.alertedChr.getCurrentSquare() != null) {
               float var3 = var2.alertedChr.getCurrentSquare().DistToProper((int)var2.getX(), (int)var2.getY());
               if (var2.alertedChr != null && var2.alertedChr.getCurrentSquare() != null && (var3 < this.spottedDist - 2.0F || var3 <= 4.0F)) {
                  var2.spottedChr = var2.alertedChr;
                  var2.getBehavior().lastAlerted = 10000.0F;
                  var2.setIsAlerted(false);
                  var2.alertedChr = null;
               }
            }

            if (GameServer.bServer) {
               INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalEvent, var2.getX(), var2.getY(), var2, var2.isAlerted(), var2.alertedChr);
            }

         }
      } else {
         var2.setDefaultState();
      }
   }

   public void setTurnAlertedValues(IsoAnimal var1, IsoMovingObject var2) {
      IsoAnimal.tempVector2.x = var1.getX() - var2.getX();
      IsoAnimal.tempVector2.y = var1.getY() - var2.getY();
      float var3 = IsoAnimal.tempVector2.getDirectionNeg();
      if (var3 < 0.0F) {
         var3 = Math.abs(var3);
      } else {
         var3 = (float)(6.283185307179586 - (double)var3);
      }

      double var4 = Math.toDegrees((double)var3);
      IsoAnimal.tempVector2.x = IsoDirections.reverse(var1.getDir()).ToVector().x;
      IsoAnimal.tempVector2.y = IsoDirections.reverse(var1.getDir()).ToVector().y;
      IsoAnimal.tempVector2.normalize();
      float var6 = IsoAnimal.tempVector2.getDirectionNeg();
      if (var6 < 0.0F) {
         var6 = Math.abs(var6);
      } else {
         var6 = 6.2831855F - var6;
      }

      double var7 = Math.toDegrees((double)var6);
      if ((int)var7 == 360) {
         var7 = 0.0;
      }

      if ((int)var4 == 360) {
         var4 = 0.0;
      }

      boolean var9 = false;
      float var10 = 0.0F;
      int var11;
      if (var4 > var7) {
         var11 = (int)(var4 - var7);
         if (var11 > 180) {
            var11 = 180 - (var11 - 180);
            var10 = (float)var11 / 180.0F - (float)var11 / 180.0F * 2.0F;
         } else {
            var10 = (float)var11 / 180.0F;
         }
      } else {
         var11 = (int)(var7 - var4);
         var10 = (float)var11 / 180.0F - (float)var11 / 180.0F * 2.0F;
      }

      if (GameClient.bClient) {
         var1.setVariable("AlertX", IsoAnimal.tempVector2.set(var2.getX() - var1.getX(), var2.getY() - var1.getY()).getDirection());
         var1.setVariable("AlertY", 0.0F);
      } else {
         var1.setVariable("AlertX", var10);
         var1.setVariable("AlertY", 0.0F);
      }

   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
   }
}
