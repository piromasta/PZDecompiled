package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

public class RZSForestCampEaten extends RandomizedZoneStoryBase {
   public RZSForestCampEaten() {
      this.name = "Forest Camp Eaten";
      this.chance = 10;
      this.minZoneHeight = 6;
      this.minZoneWidth = 10;
      this.minimumDays = 30;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = RZSForestCamp.getForestClutter();
      ArrayList var5 = RZSForestCamp.getCoolerClutter();
      ArrayList var6 = RZSForestCamp.getFireClutter();
      IsoGridSquare var7 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      this.cleanSquareAndNeighbors(var7);
      this.addCampfireOrPit(var7);
      this.addItemOnGround(getSq(var2, var3, var1.z), (String)var6.get(Rand.Next(var6.size())));
      int var8 = 0;
      byte var9 = 0;
      this.addRandomTentNorthSouth(var2 - 4, var3 + var9 - 2, var1.z);
      var8 += Rand.Next(1, 3);
      this.addRandomTentNorthSouth(var2 - 3 + var8, var3 + var9 - 2, var1.z);
      var8 += Rand.Next(1, 3);
      this.addRandomTentNorthSouth(var2 - 2 + var8, var3 + var9 - 2, var1.z);
      if (Rand.NextBool(1)) {
         var8 += Rand.Next(1, 3);
         this.addRandomTentNorthSouth(var2 - 1 + var8, var3 + var9 - 2, var1.z);
      }

      if (Rand.NextBool(2)) {
         var8 += Rand.Next(1, 3);
         this.addRandomTentNorthSouth(var2 + var8, var3 + var9 - 2, var1.z);
      }

      InventoryContainer var10 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Cooler");
      int var11 = Rand.Next(2, 5);

      int var12;
      for(var12 = 0; var12 < var11; ++var12) {
         var10.getItemContainer().AddItem((String)var5.get(Rand.Next(var5.size())));
      }

      this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var10);
      var11 = Rand.Next(3, 7);

      for(var12 = 0; var12 < var11; ++var12) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), (String)var4.get(Rand.Next(var4.size())));
      }

      String var18 = "Camper";
      if (Rand.NextBool(2)) {
         var18 = "Backpacker";
      }

      ArrayList var13 = this.addZombiesOnSquare(1, var18, (Integer)null, this.getRandomExtraFreeSquare(this, var1));
      IsoZombie var14 = var13.isEmpty() ? null : (IsoZombie)var13.get(0);
      int var15 = Rand.Next(3, 7);
      IsoDeadBody var16 = null;

      for(int var17 = 0; var17 < var15; ++var17) {
         var16 = createRandomDeadBody(this.getRandomExtraFreeSquare(this, var1), (IsoDirections)null, Rand.Next(5, 10), 0, var18);
         if (var16 != null) {
            this.addBloodSplat(var16.getSquare(), 10);
         }
      }

      var16 = createRandomDeadBody(getSq(var2, var3 + 3, var1.z), (IsoDirections)null, Rand.Next(5, 10), 0, var18);
      if (var16 != null) {
         this.addBloodSplat(var16.getSquare(), 10);
         if (var14 != null) {
            var14.faceLocationF(var16.getX(), var16.getY());
            var14.setX(var16.getX() + 1.0F);
            var14.setY(var16.getY());
            var14.setEatBodyTarget(var16, true);
         }
      }

   }
}
