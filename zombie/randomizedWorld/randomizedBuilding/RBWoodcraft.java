package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

public final class RBWoodcraft extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null && this.roomValid(var6)) {
                  RBBasic.doWoodcraftStuff(var6);
               }
            }
         }
      }

      RoomDef var8 = var1.getRoom("woodcraftset");
      if (var8 != null) {
         ArrayList var9 = this.addZombies(var1, 1, "Woodcut", 0, var8);
         IsoZombie var10 = (IsoZombie)var9.get(0);
         var10.getHumanVisual().setSkinTextureIndex(1);
         SurvivorDesc var11 = var10.getDescriptor();
         if (var11 != null) {
            var11.setForename("Notdean");
            var11.setSurname("Doe");
            InventoryItem var7 = InventoryItemFactory.CreateItem("Base.Hammer");
            if (var7 != null) {
               var10.addItemToSpawnAtDeath(var7);
            }
         }
      }
   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && "woodcraftset".equals(var1.getRoom().getName());
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return var1.getRoom("woodcraftset") != null;
   }

   public RBWoodcraft() {
      this.name = "Woodcraft";
      this.reallyAlwaysForce = true;
      this.setAlwaysDo(true);
   }
}
