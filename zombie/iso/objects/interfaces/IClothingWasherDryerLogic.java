package zombie.iso.objects.interfaces;

import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;

public interface IClothingWasherDryerLogic {
   void update();

   void saveChange(String var1, KahluaTable var2, ByteBuffer var3);

   void loadChange(String var1, ByteBuffer var2);

   ItemContainer getContainer();

   boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2);

   boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2);

   boolean isActivated();

   void setActivated(boolean var1);

   void switchModeOn();

   void switchModeOff();
}
