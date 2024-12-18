package zombie.entity.components.resources;

import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class LogisticsSystem extends EngineSystem {
   private EntityBucket resourcesEntities;

   public LogisticsSystem(int var1) {
      super(false, true, var1);
   }

   public void addedToEngine(Engine var1) {
      this.resourcesEntities = var1.getBucket(Family.all(ComponentType.Resources).get());
   }

   public void removedFromEngine(Engine var1) {
   }

   private boolean isValidEntity(GameEntity var1) {
      return var1.isEntityValid() && var1.isValidEngineEntity();
   }

   public void updateSimulation() {
      if (!GameClient.bClient) {
         ImmutableArray var1 = this.resourcesEntities.getEntities();
         if (var1.size() != 0) {
            for(int var4 = 0; var4 < var1.size(); ++var4) {
               GameEntity var2 = (GameEntity)var1.get(var4);
               if (this.isValidEntity(var2)) {
                  Resources var3 = (Resources)var2.getComponent(ComponentType.Resources);
                  if (!var3.isValid()) {
                  }
               }
            }

         }
      }
   }
}
