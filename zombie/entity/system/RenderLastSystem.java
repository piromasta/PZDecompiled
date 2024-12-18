package zombie.entity.system;

import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.iso.IsoObject;
import zombie.vehicles.VehiclePart;

public class RenderLastSystem extends EngineSystem {
   EntityBucket nonMetaRenderers;

   public RenderLastSystem(int var1) {
      super(false, false, 2147483647, true, var1);
   }

   public void addedToEngine(Engine var1) {
      this.nonMetaRenderers = var1.getCustomBucket("NonMetaRenderers");
   }

   public void removedFromEngine(Engine var1) {
   }

   public void renderLast() {
      ImmutableArray var1 = this.nonMetaRenderers.getEntities();

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         GameEntity var2 = (GameEntity)var1.get(var3);
         if (var2.isValidEngineEntity() && (var2 instanceof IsoObject || var2 instanceof VehiclePart)) {
            var2.renderlastComponents();
         }
      }

   }
}
