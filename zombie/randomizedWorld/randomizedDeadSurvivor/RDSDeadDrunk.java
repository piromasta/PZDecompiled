package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

public final class RDSDeadDrunk extends RandomizedDeadSurvivorBase {
   final ArrayList<String> alcoholList = new ArrayList();

   public RDSDeadDrunk() {
      this.name = "Dead Drunk";
      this.setChance(10);
      this.alcoholList.add("Base.Whiskey");
      this.alcoholList.add("Base.WhiskeyEmpty");
      this.alcoholList.add("Base.Wine");
      this.alcoholList.add("Base.WineEmpty");
      this.alcoholList.add("Base.Wine2");
      this.alcoholList.add("Base.Wine2Empty");
      this.alcoholList.add("Base.WineBox");
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getLivingRoomOrKitchen(var1);
      IsoDeadBody var3 = RandomizedDeadSurvivorBase.createRandomDeadBody(var2, 0);
      if (var3 != null) {
         int var4 = Rand.Next(2, 4);

         for(int var5 = 0; var5 < var4; ++var5) {
            InventoryItem var6 = InventoryItemFactory.CreateItem((String)this.alcoholList.get(Rand.Next(0, this.alcoholList.size())));
            ItemSpawner.spawnItem(var6, var3.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
            var1.bAlarmed = false;
         }

         var3.setPrimaryHandItem(InventoryItemFactory.CreateItem("Base.WhiskeyEmpty"));
      }
   }
}
