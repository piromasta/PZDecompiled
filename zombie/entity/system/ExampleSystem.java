package zombie.entity.system;

import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.IBucketListener;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.util.ImmutableArray;

public class ExampleSystem extends EngineSystem {
   EntityBucket craftEntities;
   EntityBucket nonMetaEntities;
   int exampleCounter = 0;

   public ExampleSystem(int var1) {
      super(false, true, var1);
   }

   public void addedToEngine(Engine var1) {
      this.craftEntities = var1.getBucket(Family.all(ComponentType.CraftLogic).get());
      this.nonMetaEntities = var1.getCustomBucket("NonMetaEntities");
      this.nonMetaEntities.addListener(0, new CraftEntityBucketListener());
   }

   public void removedFromEngine(Engine var1) {
   }

   public void update() {
      ImmutableArray var1 = this.nonMetaEntities.getEntities();

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         GameEntity var2 = (GameEntity)var1.get(var3);
      }

   }

   public void updateSimulation() {
      ImmutableArray var1 = this.craftEntities.getEntities();

      for(int var4 = 0; var4 < var1.size(); ++var4) {
         GameEntity var2 = (GameEntity)var1.get(var4);
         CraftLogic var3 = (CraftLogic)var2.getComponent(ComponentType.CraftLogic);
      }

   }

   public void renderLast() {
   }

   private class CraftEntityBucketListener implements IBucketListener {
      private CraftEntityBucketListener() {
      }

      public void onBucketEntityAdded(EntityBucket var1, GameEntity var2) {
         ExampleSystem var10000 = ExampleSystem.this;
         var10000.exampleCounter += 100;
      }

      public void onBucketEntityRemoved(EntityBucket var1, GameEntity var2) {
         ExampleSystem var10000 = ExampleSystem.this;
         var10000.exampleCounter -= 100;
      }
   }
}
