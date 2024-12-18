package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZJackieJaye extends RandomizedZoneStoryBase {
   public RZJackieJaye() {
      this.name = "JackieJaye";
      this.chance = 100;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.JackieJaye.toString());
      this.alwaysDo = true;
   }

   public void randomizeZoneStory(Zone var1) {
      IsoGridSquare var2 = this.getRandomExtraFreeSquare(this, var1);
      ArrayList var3 = this.addZombiesOnSquare(1, "Jackie_Jaye", 100, var2);
      IsoZombie var4 = (IsoZombie)var3.get(0);
      var4.getHumanVisual().setSkinTextureIndex(1);
      SurvivorDesc var5 = var4.getDescriptor();
      if (var5 != null) {
         var5.setForename("Jackie");
         var5.setSurname("Jaye");
         InventoryItem var6 = InventoryItemFactory.CreateItem("Base.PressID");
         if (var6 != null) {
            var6.nameAfterDescriptor(var5);
            var4.addItemToSpawnAtDeath(var6);
         }
      }
   }
}
