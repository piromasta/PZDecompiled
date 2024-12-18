package zombie.entity;

import zombie.entity.util.ImmutableArray;
import zombie.inventory.InventoryItem;

public class InventoryItemSystem extends EngineSystem {
   EntityBucket itemEntities;

   public InventoryItemSystem(int var1) {
      super(true, false, var1);
   }

   public void addedToEngine(Engine var1) {
      this.itemEntities = var1.getInventoryItemBucket();
   }

   public void removedFromEngine(Engine var1) {
   }

   public void update() {
      ImmutableArray var1 = this.itemEntities.getEntities();

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         GameEntity var2 = (GameEntity)var1.get(var3);
         InventoryItem var4 = (InventoryItem)var2;
         if (var4.getEquipParent() == null || var4.getEquipParent().isDead()) {
            GameEntityManager.UnregisterEntity(var2);
         }
      }

   }
}
