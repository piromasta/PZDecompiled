package zombie.randomizedWorld.randomizedZoneStory;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

public class RZSRangerSmith extends RandomizedZoneStoryBase {
   public RZSRangerSmith() {
      this.name = "Ranger Smith";
      this.chance = 1;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      this.cleanAreaForStory(this, var1);
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      IsoGridSquare var4 = getSq(var2, var3, var1.z);
      if (var4 != null) {
         IsoDeadBody var5 = createRandomDeadBody(var4, (IsoDirections)null, 100, 0, "Ranger");
         if (var5 != null) {
            this.addBloodSplat(var4, 100);

            for(int var6 = 0; var6 < var5.getWornItems().size(); ++var6) {
               if (var5.getWornItems().get(var6).getItem() instanceof Clothing) {
                  ((Clothing)var5.getWornItems().get(var6).getItem()).randomizeCondition(0, 25, 50, 50);
               }
            }
         }

         IsoGridSquare var8 = var4.getAdjacentSquare(IsoDirections.getRandom());
         if (var8 != null) {
            InventoryContainer var7 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_PicnicBasket");
            if (var7 != null) {
               this.addItemOnGround(var8, var7);
               ItemPickerJava.rollContainerItem(var7, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var7.getType()));
            }
         }
      }

   }
}
