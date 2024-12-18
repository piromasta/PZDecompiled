package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.vehicles.BaseVehicle;

public final class RBHeatBreakAfternoon extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      var1.bAlarmed = false;
      RoomDef var2 = var1.getRoom("bank");
      if (var2 != null) {
         String var3 = "BankRobber";
         if (Rand.NextBool(2)) {
            var3 = "BankRobberSuit";
         }

         this.addZombies(var1, Rand.Next(3, 5), var3, 0, var2);
         InventoryContainer var4 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
         ItemPickerJava.rollContainerItem(var4, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var4.getType()));
         IsoGridSquare var5 = var2.getFreeSquare();
         if (var5 != null) {
            this.addItemOnGround(var5, var4);
         }

         if (Rand.Next(2) == 0) {
            var4 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
            ItemPickerJava.rollContainerItem(var4, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var4.getType()));
            var5 = var2.getFreeSquare();
            if (var5 != null) {
               this.addItemOnGround(var5, var4);
            }
         }

         if (Rand.Next(2) == 0) {
            var4 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
            ItemPickerJava.rollContainerItem(var4, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var4.getType()));
            var5 = var2.getFreeSquare();
            if (var5 != null) {
               this.addItemOnGround(var5, var4);
            }
         }

         if (Rand.Next(2) == 0) {
            var4 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
            ItemPickerJava.rollContainerItem(var4, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var4.getType()));
            var5 = var2.getFreeSquare();
            if (var5 != null) {
               this.addItemOnGround(var5, var4);
            }
         }

         this.addZombies(var1, Rand.Next(2, 4), "Police", (Integer)null, var2);
      }

      BaseVehicle var6 = this.spawnCarOnNearestNav("Base.StepVan_LouisvilleSWAT", var1);
      if (var6 != null) {
         var6.setAlarmed(false);
         IsoGridSquare var7 = var6.getSquare().getCell().getGridSquare(var6.getSquare().x - 2, var6.getSquare().y - 2, 0);
         ArrayList var8 = this.addZombiesOnSquare(Rand.Next(3, 6), "Police_SWAT", (Integer)null, var7);
         if (!var8.isEmpty()) {
            ((IsoZombie)var8.get(Rand.Next(var8.size()))).addItemToSpawnAtDeath(var6.createVehicleKey());
         }
      }
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else {
         return var1.getRoom("bank") != null;
      }
   }

   public RBHeatBreakAfternoon() {
      this.name = "Bank Robbery";
      this.setChance(10);
      this.setUnique(true);
   }
}
