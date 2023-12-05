package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public class IsoClothingWasher extends IsoObject {
   private final ClothingWasherLogic m_logic = new ClothingWasherLogic(this);

   public IsoClothingWasher(IsoCell var1) {
      super(var1);
   }

   public IsoClothingWasher(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      super(var1, var2, var3);
   }

   public String getObjectName() {
      return "ClothingWasher";
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.m_logic.load(var1, var2, var3);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      this.m_logic.save(var1, var2);
   }

   public void update() {
      this.m_logic.update();
   }

   public void addToWorld() {
      IsoCell var1 = this.getCell();
      var1.addToProcessIsoObject(this);
   }

   public void removeFromWorld() {
      super.removeFromWorld();
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      this.m_logic.saveChange(var1, var2, var3);
   }

   public void loadChange(String var1, ByteBuffer var2) {
      this.m_logic.loadChange(var1, var2);
   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      return this.m_logic.isItemAllowedInContainer(var1, var2);
   }

   public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2) {
      return this.m_logic.isRemoveItemAllowedFromContainer(var1, var2);
   }

   public boolean isActivated() {
      return this.m_logic.isActivated();
   }

   public void setActivated(boolean var1) {
      this.m_logic.setActivated(var1);
   }
}
