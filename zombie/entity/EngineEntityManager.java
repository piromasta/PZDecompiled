package zombie.entity;

import java.util.Iterator;
import zombie.core.Core;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectSet;
import zombie.entity.util.SingleThreadPool;

public final class EngineEntityManager {
   private final EntityBucketManager bucketManager;
   private final Array<GameEntity> entities = new Array(false, 16);
   private final ObjectSet<GameEntity> entitySet = new ObjectSet();
   private final ImmutableArray<GameEntity> immutableEntities;
   private final Array<EntityOperation> pendingOperations;
   private final EntityOperationPool entityOperationPool;
   private final ComponentOperationHandler componentOperationHandler;
   private final IBooleanInformer delayed;
   private final IBucketInformer bucketsUpdating;
   private final Engine engine;

   protected EngineEntityManager(Engine var1, IBooleanInformer var2) {
      this.immutableEntities = new ImmutableArray(this.entities);
      this.pendingOperations = new Array(false, 16);
      this.entityOperationPool = new EntityOperationPool();
      this.engine = var1;
      this.bucketManager = new EntityBucketManager(this.immutableEntities);
      this.bucketsUpdating = this.bucketManager.getBucketsUpdatingInformer();
      this.delayed = var2;
      this.componentOperationHandler = new ComponentOperationHandler(this.delayed, this.bucketsUpdating, new ComponentOperationListener());
   }

   EntityBucketManager getBucketManager() {
      return this.bucketManager;
   }

   void addEntity(GameEntity var1) {
      if (!this.delayed.value() && !this.bucketsUpdating.value()) {
         this.addEntityInternal(var1);
      } else {
         if (var1.scheduledForEngineRemoval || var1.removingFromEngine) {
            throw new IllegalArgumentException("Entity is scheduled for removal.");
         }

         if (var1.addedToEngine) {
            if (Core.bDebug) {
               throw new IllegalArgumentException("Entity has already been added to Engine.");
            }

            return;
         }

         var1.addedToEngine = true;
         var1.scheduledDelayedAddToEngine = true;
         EntityOperation var2 = (EntityOperation)this.entityOperationPool.obtain();
         var2.entity = var1;
         var2.type = EngineEntityManager.EntityOperation.Type.Add;
         this.pendingOperations.add(var2);
      }

   }

   void removeEntity(GameEntity var1) {
      if (!this.delayed.value() && !this.bucketsUpdating.value()) {
         this.removeEntityInternal(var1);
      } else {
         if (var1.scheduledForEngineRemoval) {
            return;
         }

         var1.scheduledForEngineRemoval = true;
         EntityOperation var2 = (EntityOperation)this.entityOperationPool.obtain();
         var2.entity = var1;
         var2.type = EngineEntityManager.EntityOperation.Type.Remove;
         this.pendingOperations.add(var2);
      }

   }

   void removeAllEntities() {
      this.removeAllEntities(this.immutableEntities);
   }

   void removeAllEntities(ImmutableArray<GameEntity> var1) {
      if (!this.delayed.value() && !this.bucketsUpdating.value()) {
         while(var1.size() > 0) {
            this.removeEntityInternal((GameEntity)var1.first());
         }
      } else {
         GameEntity var3;
         for(Iterator var2 = var1.iterator(); var2.hasNext(); var3.scheduledForEngineRemoval = true) {
            var3 = (GameEntity)var2.next();
         }

         EntityOperation var4 = (EntityOperation)this.entityOperationPool.obtain();
         var4.type = EngineEntityManager.EntityOperation.Type.RemoveAll;
         var4.entities = var1;
         this.pendingOperations.add(var4);
      }

   }

   ImmutableArray<GameEntity> getEntities() {
      return this.immutableEntities;
   }

   boolean hasPendingOperations() {
      return this.pendingOperations.size > 0;
   }

   void processPendingOperations() {
      for(int var1 = 0; var1 < this.pendingOperations.size; ++var1) {
         EntityOperation var2;
         var2 = (EntityOperation)this.pendingOperations.get(var1);
         label20:
         switch (var2.type) {
            case Add:
               this.addEntityInternal(var2.entity);
               break;
            case Remove:
               this.removeEntityInternal(var2.entity);
               break;
            case RemoveAll:
               while(true) {
                  if (var2.entities.size() <= 0) {
                     break label20;
                  }

                  this.removeEntityInternal((GameEntity)var2.entities.first());
               }
            default:
               throw new AssertionError("Unexpected EntityOperation type");
         }

         this.entityOperationPool.free(var2);
      }

      this.pendingOperations.clear();
   }

   void updateOperations() {
      while(this.componentOperationHandler.hasOperationsToProcess() || this.hasPendingOperations()) {
         this.componentOperationHandler.processOperations();
         this.processPendingOperations();
      }

   }

   void addEntityInternal(GameEntity var1) {
      if (this.entitySet.contains(var1)) {
         throw new IllegalArgumentException("Entity is already registered " + var1);
      } else {
         var1.scheduledDelayedAddToEngine = false;
         this.entities.add(var1);
         this.entitySet.add(var1);
         var1.setComponentOperationHandler(this.componentOperationHandler);
         var1.addedToEngine = true;
         this.bucketManager.updateBucketMembership(var1);
         this.engine.onEntityAdded(var1);
      }
   }

   void removeEntityInternal(GameEntity var1) {
      boolean var2 = this.entitySet.remove(var1);
      if (var2) {
         var1.scheduledForEngineRemoval = false;
         var1.removingFromEngine = true;
         this.entities.removeValue(var1, true);
         this.bucketManager.updateBucketMembership(var1);
         var1.setComponentOperationHandler((ComponentOperationHandler)null);
         var1.removingFromEngine = false;
         var1.addedToEngine = false;
         this.engine.onEntityRemoved(var1);
      }

   }

   private static class EntityOperationPool extends SingleThreadPool<EntityOperation> {
      private EntityOperationPool() {
      }

      protected EntityOperation newObject() {
         return new EntityOperation();
      }
   }

   private class ComponentOperationListener implements ComponentOperationHandler.OperationListener {
      private ComponentOperationListener() {
      }

      public void componentsChanged(GameEntity var1) {
         EngineEntityManager.this.bucketManager.updateBucketMembership(var1);
      }
   }

   private static class EntityOperation implements SingleThreadPool.Poolable {
      Type type;
      GameEntity entity;
      ImmutableArray<GameEntity> entities;

      private EntityOperation() {
      }

      public void reset() {
         this.entity = null;
      }

      public static enum Type {
         Add,
         Remove,
         RemoveAll;

         private Type() {
         }
      }
   }
}
