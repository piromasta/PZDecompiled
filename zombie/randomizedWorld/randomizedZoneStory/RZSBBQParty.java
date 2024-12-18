package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;

public class RZSBBQParty extends RandomizedZoneStoryBase {
   public RZSBBQParty() {
      this.name = "BBQ Party";
      this.chance = 10;
      this.minZoneHeight = 12;
      this.minZoneWidth = 12;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = RZSForestCamp.getCoolerClutter();
      IsoGridSquare var5 = getSq(var2, var3, var1.z);
      String var6 = "appliances_cooking_01_35";
      if (Rand.NextBool(2)) {
         IsoBarbecue var7 = new IsoBarbecue(IsoWorld.instance.getCell(), var5, (IsoSprite)IsoSpriteManager.instance.NamedMap.get("appliances_cooking_01_35"));
         var5.getObjects().add(var7);
      } else {
         this.cleanSquareAndNeighbors(var5);
         this.addCookingPit(var5);
      }

      int var12 = Rand.Next(1, 4);

      for(int var8 = 0; var8 < var12; ++var8) {
         this.addTileObject(this.getRandomExtraFreeSquare(this, var1), "furniture_seating_outdoor_01_" + Rand.Next(16, 20));
      }

      InventoryContainer var13 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Cooler");
      int var9 = Rand.Next(4, 8);

      int var10;
      for(var10 = 0; var10 < var9; ++var10) {
         var13.getItemContainer().AddItem((String)var4.get(Rand.Next(var4.size())));
      }

      this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var13);
      var9 = Rand.Next(3, 7);

      for(var10 = 0; var10 < var9; ++var10) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), this.getBBQClutterItem());
      }

      var10 = Rand.Next(2, 7);

      for(int var11 = 0; var11 < var10; ++var11) {
         this.addZombiesOnSquare(1, "Tourist", (Integer)null, this.getRandomExtraFreeSquare(this, var1));
      }

      this.addZombiesOnSquare(1, "Meat_Master", (Integer)null, this.getRandomExtraFreeSquare(this, var1));
   }
}
