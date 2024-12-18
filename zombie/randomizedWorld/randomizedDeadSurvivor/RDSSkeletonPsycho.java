package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

public final class RDSSkeletonPsycho extends RandomizedDeadSurvivorBase {
   public RDSSkeletonPsycho() {
      this.name = "Skeleton Psycho";
      this.setChance(1);
      this.setMinimumDays(120);
      this.setUnique(true);
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getRoomNoKids(var1, "bedroom");
      if (var2 == null) {
         var2 = this.getRoom(var1, "kitchen");
      }

      if (var2 == null) {
         var2 = this.getRoom(var1, "livingroom");
      }

      if (var2 != null) {
         int var3 = Rand.Next(3, 7);

         for(int var4 = 0; var4 < var3; ++var4) {
            IsoDeadBody var5 = super.createSkeletonCorpse(var2);
            if (var5 != null) {
               super.addBloodSplat(var5.getCurrentSquare(), Rand.Next(7, 12));
            }
         }

         ArrayList var7 = super.addZombies(var1, 1, "MadScientist", (Integer)null, var2);
         if (!var7.isEmpty()) {
            InventoryContainer var8 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_DoctorBag");
            ItemPickerJava.rollContainerItem(var8, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get("Bag_BurglarBag"));
            this.addItemOnGround(var1.getFreeSquareInRoom(), var8);

            for(int var6 = 0; var6 < 8; ++var6) {
               ((IsoZombie)var7.get(0)).addBlood((BloodBodyPartType)null, false, true, false);
            }

            var1.bAlarmed = false;
         }
      }
   }
}
