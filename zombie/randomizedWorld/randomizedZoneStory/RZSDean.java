package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSDean extends RandomizedZoneStoryBase {
   public RZSDean() {
      this.name = "Dean";
      this.chance = 1;
      this.minZoneHeight = 10;
      this.minZoneWidth = 10;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      this.cleanAreaForStory(this, var1);
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      IsoGridSquare var4 = getSq(var2, var3, var1.z);
      if (var4 != null) {
         this.cleanSquareAndNeighbors(var4);
         this.addSimpleCookingPit(var4);
         this.addItemOnGround(var4, "Base.TinCanEmpty");
         int var5 = Rand.Next(3);
         switch (var5) {
            case 0:
               this.addItemOnGround(var4, "Base.PanForged");
               break;
            case 1:
               this.addItemOnGround(var4, "Base.PotForged");
               break;
            case 2:
               this.addItemOnGround(var4, "Base.MetalCup");
         }

         int var6 = Rand.Next(0, 1);
         int var7 = Rand.Next(0, 1);
         if (Rand.NextBool(2)) {
            this.addShelterWestEast(var2 + var6 - 2, var3 + var7, var1.z);
         } else {
            this.addShelterNorthSouth(var2 + var6, var3 + var7 - 2, var1.z);
         }

         this.addItemOnGround(this.getRandomFreeSquare(this, var1), getOldShelterClutterItem());
         this.addItemOnGround(this.getRandomFreeSquare(this, var1), getOldShelterClutterItem());
         this.getRandomExtraFreeSquare(this, var1);
         ArrayList var9 = this.addZombiesOnSquare(1, "Dean", 0, this.getRandomExtraFreeSquare(this, var1));
         IsoZombie var10 = (IsoZombie)var9.get(0);
         var10.getHumanVisual().setSkinTextureIndex(1);
         SurvivorDesc var11 = var10.getDescriptor();
         if (var11 != null) {
            var11.setForename("Dean");
            var11.setSurname("Porch");
         }
      }
   }
}
