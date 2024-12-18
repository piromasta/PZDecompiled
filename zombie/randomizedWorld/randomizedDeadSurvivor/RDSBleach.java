package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

public final class RDSBleach extends RandomizedDeadSurvivorBase {
   public RDSBleach() {
      this.name = "Suicide by Bleach";
      this.setChance(10);
      this.setMinimumDays(60);
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getLivingRoomOrKitchen(var1);
      IsoDeadBody var3 = RandomizedDeadSurvivorBase.createRandomDeadBody(var2, 0);
      if (var3 != null) {
         InventoryItem var7;
         if (Rand.NextBool(2)) {
            int var4 = Rand.Next(1, 3);

            for(int var5 = 0; var5 < var4; ++var5) {
               InventoryItem var6 = InventoryItemFactory.CreateItem("Base.BleachEmpty");
               ItemSpawner.spawnItem(var6, var3.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
            }

            var3.setPrimaryHandItem(InventoryItemFactory.CreateItem("Base.BleachEmpty"));
         } else {
            var7 = InventoryItemFactory.CreateItem("Base.RatPoison");
            var7.setCurrentUses(var7.getMaxUses() / 2);
            var3.setPrimaryHandItem(var7);
         }

         if (Rand.Next(2) == 0) {
            var7 = InventoryItemFactory.CreateItem("Base.Note");
            if (Rand.Next(2) == 0) {
               ItemSpawner.spawnItem(var7, var3.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
            } else {
               var3.getContainer().addItem(var7);
            }
         }

         var1.bAlarmed = false;
      }
   }
}
