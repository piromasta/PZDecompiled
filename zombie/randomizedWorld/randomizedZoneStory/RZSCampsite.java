package zombie.randomizedWorld.randomizedZoneStory;

import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSCampsite extends RandomizedZoneStoryBase {
   public RZSCampsite() {
      this.name = "Campsite";
      this.chance = 5;
      this.minZoneHeight = 9;
      this.minZoneWidth = 9;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public void randomizeZoneStory(Zone var1) {
      this.cleanAreaForStory(this, var1);
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      boolean var4 = false;
      boolean var5 = false;
      int var6 = 0;
      IsoGridSquare var7 = getSq(var2, var3, var1.z);
      if (var7 != null) {
         this.cleanSquareAndNeighbors(var7);
         int var8 = Rand.Next(3);
         Object var9 = null;
         switch (var8) {
            case 0:
               this.addCampfire(var7);
               break;
            case 1:
               this.addSimpleCookingPit(var7);
               break;
            case 2:
               this.addCookingPit(var7);
               var5 = true;
         }

         if (Rand.NextBool(2)) {
            this.addItemOnGround(var7, "Base.TinCanEmpty");
         }

         byte var10 = 0;
         byte var11 = 0;
         IsoGridSquare var12 = null;
         boolean var13 = !Rand.NextBool(3);
         if (Rand.NextBool(3)) {
            var4 = true;
            var12 = getSq(var2 - 1, var3 - 2 + var10, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_58", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_20", true);
            }

            var12 = getSq(var2, var3 - 2 + var10, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_59", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_21", true);
            }
         }

         if (Rand.NextBool(3)) {
            var4 = true;
            var12 = getSq(var2 - 1, var3 + 2 + var10, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_58", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_20", true);
            }

            var12 = getSq(var2, var3 + 2 + var10, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_59", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_21", true);
            }
         }

         if (Rand.NextBool(3)) {
            var4 = true;
            var12 = getSq(var2 - 2 + var11, var3, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_57", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_23", true);
            }

            var12 = getSq(var2 - 2 + var11, var3 + 1, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_56", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_22", true);
            }
         }

         if (Rand.NextBool(3)) {
            var4 = true;
            var12 = getSq(var2 + 2 + var11, var3, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_57", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_23", true);
            }

            var12 = getSq(var2 + 2 + var11, var3 + 1, var1.z);
            if (var13) {
               this.addTileObject(var12, "crafted_02_56", true);
            } else {
               this.addTileObject(var12, "furniture_seating_outdoor_01_22", true);
            }
         }

         if (var6 < 2 && Rand.NextBool(3)) {
            ++var6;
            this.addTileObject(getSq(var2 - 1, var3 - 4, var1.z), "camping_01_28", true);
            this.addTileObject(getSq(var2, var3 - 4, var1.z), "camping_01_29", true);
         }

         if (var6 < 2 && Rand.NextBool(3)) {
            ++var6;
            this.addTileObject(getSq(var2 - 4, var3, var1.z), "camping_01_35", true);
            this.addTileObject(getSq(var2 - 4, var3 + 1, var1.z), "camping_01_34", true);
         }

         if (var6 < 2 && Rand.NextBool(3)) {
            ++var6;
            this.addTileObject(getSq(var2 - 1, var3 + 4, var1.z), "camping_01_28", true);
            this.addTileObject(getSq(var2, var3 + 4, var1.z), "camping_01_29", true);
         }

         if (var6 < 2 && Rand.NextBool(3)) {
            ++var6;
            this.addTileObject(getSq(var2 + 4, var3, var1.z), "camping_01_35", true);
            this.addTileObject(getSq(var2 + 4, var3 + 1, var1.z), "camping_01_34", true);
         }

         if ((var5 || var4) && Rand.NextBool(3)) {
            var12 = this.getRandomExtraFreeSquare(this, var1);
            if (!var12.isAdjacentTo(var7)) {
               this.addTileObject(var12, "camping_01_6" + (4 + Rand.Next(4)), true);
            }
         }

         if (var5 && var4 && Rand.NextBool(3)) {
            var12 = this.getRandomExtraFreeSquare(this, var1);
            if (!var12.isAdjacentTo(var7)) {
               this.addTileObject(var12, "trashcontainers_01_16", true);
            }
         }

      }
   }
}
