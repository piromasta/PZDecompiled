package zombie.entity;

import zombie.entity.util.Array;
import zombie.entity.util.SingleThreadPool;

public final class ComponentOperationHandler {
   private final OperationListener operationListener;
   private final IBooleanInformer delayed;
   private final IBucketInformer bucketsUpdating;
   private final ComponentOperationPool operationPool = new ComponentOperationPool();
   private final Array<ComponentOperation> operations = new Array();

   protected ComponentOperationHandler(IBooleanInformer var1, IBucketInformer var2, OperationListener var3) {
      this.delayed = var1;
      this.bucketsUpdating = var2;
      this.operationListener = var3;
   }

   void add(GameEntity var1) {
      if (this.bucketsUpdating.value()) {
         throw new IllegalStateException("Cannot perform component operation when buckets are updating.");
      } else {
         if (this.delayed.value()) {
            if (var1.scheduledForBucketUpdate) {
               return;
            }

            var1.scheduledForBucketUpdate = true;
            ComponentOperation var2 = (ComponentOperation)this.operationPool.obtain();
            var2.make(var1);
            this.operations.add(var2);
         } else {
            this.operationListener.componentsChanged(var1);
         }

      }
   }

   void remove(GameEntity var1) {
      if (this.bucketsUpdating.value()) {
         throw new IllegalStateException("Cannot perform component operation when buckets are updating.");
      } else {
         if (this.delayed.value()) {
            if (var1.scheduledForBucketUpdate) {
               return;
            }

            var1.scheduledForBucketUpdate = true;
            ComponentOperation var2 = (ComponentOperation)this.operationPool.obtain();
            var2.make(var1);
            this.operations.add(var2);
         } else {
            this.operationListener.componentsChanged(var1);
         }

      }
   }

   boolean hasOperationsToProcess() {
      return this.operations.size > 0;
   }

   void processOperations() {
      for(int var1 = 0; var1 < this.operations.size; ++var1) {
         ComponentOperation var2 = (ComponentOperation)this.operations.get(var1);
         this.operationListener.componentsChanged(var2.entity);
         var2.entity.scheduledForBucketUpdate = false;
         this.operationPool.free(var2);
      }

      this.operations.clear();
   }

   private static class ComponentOperationPool extends SingleThreadPool<ComponentOperation> {
      private ComponentOperationPool() {
      }

      protected ComponentOperation newObject() {
         return new ComponentOperation();
      }
   }

   interface OperationListener {
      void componentsChanged(GameEntity var1);
   }

   private static class ComponentOperation implements SingleThreadPool.Poolable {
      public GameEntity entity;

      private ComponentOperation() {
      }

      public void make(GameEntity var1) {
         this.entity = var1;
      }

      public void reset() {
         this.entity = null;
      }
   }
}
