package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSKirstyKormick extends RandomizedZoneStoryBase {
   public RZSKirstyKormick() {
      this.name = "Kirsty Cormick";
      this.chance = 100;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.KirstyKormick.toString());
      this.alwaysDo = true;
   }

   public void randomizeZoneStory(Zone var1) {
      IsoGridSquare var2 = this.getRandomExtraFreeSquare(this, var1);
      this.addZombiesOnSquare(Rand.Next(15, 20), (String)null, (Integer)null, var2);
      ArrayList var3 = this.addZombiesOnSquare(1, "KirstyKormick", 100, var2);
      IsoZombie var4 = (IsoZombie)var3.get(0);
      var4.getHumanVisual().setSkinTextureIndex(4);
      SurvivorDesc var5 = var4.getDescriptor();
      if (var5 != null) {
         var5.setForename("Kirsty");
         var5.setSurname("Cormick");
         var4.addRandomVisualDamages();
         InventoryItem var6 = InventoryItemFactory.CreateItem("Base.PressID");
         if (var6 != null) {
            var6.nameAfterDescriptor(var5);
            var4.addItemToSpawnAtDeath(var6);
         }
      }
   }
}
