package zombie.entity.components.resources;

import java.util.List;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class ResourceUpdateSystem extends EngineSystem {
   private EntityBucket resourcesEntities;

   public ResourceUpdateSystem(int var1) {
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
            for(int var6 = 0; var6 < var1.size(); ++var6) {
               GameEntity var2 = (GameEntity)var1.get(var6);
               if (this.isValidEntity(var2)) {
                  Resources var3 = (Resources)var2.getComponent(ComponentType.Resources);
                  if (var3.isValid()) {
                     List var5 = var3.getResources();

                     for(int var7 = 0; var7 < var5.size(); ++var7) {
                        Resource var4 = (Resource)var5.get(var7);
                        if (var4.getType() == ResourceType.Energy && !var4.isEmpty()) {
                           ResourceEnergy var8 = (ResourceEnergy)var4;
                           if (var4.isAutoDecay() && !var4.isDirty()) {
                              float var9 = var8.getEnergyCapacity() * 0.05F;
                              var8.setEnergyAmount(var8.getEnergyAmount() - var9);
                           }
                        }
                     }

                     if (var3.isDirty()) {
                        var3.resetDirty();
                     }
                  }
               }
            }

         }
      }
   }
}
