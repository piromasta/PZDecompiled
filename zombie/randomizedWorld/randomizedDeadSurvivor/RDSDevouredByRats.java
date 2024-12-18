package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

public final class RDSDevouredByRats extends RandomizedDeadSurvivorBase {
   public RDSDevouredByRats() {
      this.name = "Devoured By Rats";
      this.setChance(1);
      this.setMinimumDays(30);
      this.setUnique(true);
      this.isRat = true;
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      String var2 = "bedroom";
      int var3 = Rand.Next(3);
      if (var3 == 0) {
         var2 = "kitchen";
      }

      if (var3 == 1) {
         var2 = "livingroom";
      }

      RoomDef var4 = this.getRoomNoKids(var1, var2);
      if (var4 == null) {
         var4 = this.getRoom(var1, "kitchen");
      }

      if (var4 == null) {
         var4 = this.getRoom(var1, "livingroom");
      }

      if (var4 == null) {
         var4 = this.getRoomNoKids(var1, "bedroom");
      }

      if (var4 != null) {
         int var5 = Rand.Next(1, 4);

         for(int var6 = 0; var6 < var5; ++var6) {
            IsoDeadBody var7 = super.createSkeletonCorpse(var4);
            if (var7 != null) {
               var7.getHumanVisual().setSkinTextureIndex(2);
               super.addBloodSplat(var7.getCurrentSquare(), Rand.Next(7, 12));
            }
         }

         ArrayList var14 = new ArrayList();
         int var15 = var4.getIsoRoom().getSquares().size();
         if (var15 > 21) {
            var15 = 21;
         }

         int var8 = var15 / 2;
         if (var8 < 1) {
            var8 = 1;
         }

         if (var8 > 9) {
            var8 = 9;
         }

         int var9 = Rand.Next(var8, var15);

         int var10;
         for(var10 = 0; var10 < var9; ++var10) {
            IsoGridSquare var11 = var4.getFreeUnoccupiedSquare();
            String var12 = "grey";
            if (this.getRoom(var1, "laboratory") != null && !Rand.NextBool(3)) {
               var12 = "white";
            }

            if (var11 != null && var11.isFree(true) && !var14.contains(var11)) {
               IsoAnimal var13;
               if (Rand.NextBool(2)) {
                  var13 = new IsoAnimal(IsoWorld.instance.getCell(), var11.getX(), var11.getY(), var11.getZ(), "rat", var12);
               } else {
                  var13 = new IsoAnimal(IsoWorld.instance.getCell(), var11.getX(), var11.getY(), var11.getZ(), "ratfemale", var12);
               }

               var13.addToWorld();
               var13.randomizeAge();
               if (Rand.NextBool(3)) {
                  var13.setStateEventDelayTimer(0.0F);
               } else {
                  var14.add(var11);
               }
            }
         }

         var10 = Rand.Next(var8, var15);

         for(int var16 = 0; var16 < var10; ++var16) {
            IsoGridSquare var17 = var4.getFreeSquare();
            if (var17 != null && !var17.isOutside() && var17.getRoom() != null && var17.hasRoomDef()) {
               this.addItemOnGround(var17, "Base.Dung_Rat");
            }
         }

         var1.bAlarmed = false;
      }
   }
}
