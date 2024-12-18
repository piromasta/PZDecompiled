package zombie.randomizedWorld.randomizedZoneStory;

import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

public class RZSHogWild extends RandomizedZoneStoryBase {
   public RZSHogWild() {
      this.name = "Hog Wild";
      this.chance = 1;
      this.minZoneHeight = 2;
      this.minZoneWidth = 2;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      this.cleanAreaForStory(this, var1);
      int var2 = var1.x;
      int var3 = var1.y;
      int var10000;
      if (Rand.Next(2) == 0) {
         var10000 = var2 + var1.getWidth();
      }

      if (Rand.Next(2) == 0) {
         var10000 = var3 + var1.getHeight();
      }

      IsoGridSquare var4 = this.getRandomExtraFreeSquare(this, var1);
      if (var4 != null) {
         IsoDeadBody var5 = this.createSkeletonCorpse(var4);
         if (var5 != null) {
            if (var5.getHumanVisual() != null) {
               var5.getHumanVisual().setSkinTextureIndex(2);
            }

            super.addBloodSplat(var5.getCurrentSquare(), 20);
         }

         String var6 = "largeblack";
         if (Rand.Next(2) == 0) {
            var6 = "landrace";
         }

         String var7 = "boar";
         if (Rand.Next(2) == 0) {
            var7 = "sow";
         }

         IsoAnimal var8 = new IsoAnimal(IsoWorld.instance.getCell(), var4.getX(), var4.getY(), var4.getZ(), var7, var6);
         var8.addToWorld();
         var8.randomizeAge();
         super.addBloodSplat(var8.getCurrentSquare(), Rand.Next(7, 12));
         if (Rand.Next(3) == 0) {
            IsoGridSquare var9 = var4.getAdjacentSquare(IsoDirections.getRandom());
            if (var9.isFree(true)) {
               var6 = "largeblack";
               if (Rand.Next(2) == 0) {
                  var6 = "landrace";
               }

               var7 = "boar";
               if (Rand.Next(2) == 0) {
                  var7 = "sow";
               }

               IsoAnimal var10 = new IsoAnimal(IsoWorld.instance.getCell(), var9.getX(), var9.getY(), var9.getZ(), var7, var6);
               var10.addToWorld();
               var10.randomizeAge();
               super.addBloodSplat(var10.getCurrentSquare(), Rand.Next(7, 12));
            }

            if (Rand.Next(4) == 0) {
               IsoGridSquare var12 = var4.getAdjacentSquare(IsoDirections.getRandom());
               if (var12.isFree(true)) {
                  var6 = "largeblack";
                  if (Rand.Next(2) == 0) {
                     var6 = "landrace";
                  }

                  var7 = "boar";
                  if (Rand.Next(2) == 0) {
                     var7 = "sow";
                  }

                  IsoAnimal var11 = new IsoAnimal(IsoWorld.instance.getCell(), var12.getX(), var12.getY(), var12.getZ(), var7, var6);
                  var11.addToWorld();
                  var11.randomizeAge();
                  super.addBloodSplat(var11.getCurrentSquare(), Rand.Next(7, 12));
               }
            }
         }

      }
   }
}
