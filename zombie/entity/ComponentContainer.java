package zombie.entity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.entity.util.Array;
import zombie.entity.util.BitSet;
import zombie.entity.util.SnapshotArray;

public final class ComponentContainer {
   private static final ComponentRenderComparator renderComparator = new ComponentRenderComparator();
   private static final boolean ENABLE_POOLING = true;
   private static final int MAX_POOL_SIZE = -1;
   private static final ConcurrentLinkedDeque<ComponentContainer> array_pool = new ConcurrentLinkedDeque();
   private final Array<Component> componentList = new Array(false, 8);
   private final Component[] componentArray;
   private final SnapshotArray<Component> renderersArray;
   private boolean dirtyRenderers;
   private ComponentOperationHandler componentOperationHandler;
   private final BitSet componentBits;
   private final BitSet bucketBits;
   private GameEntity entity;

   public static ComponentContainer Alloc(GameEntity var0) {
      ComponentContainer var1 = (ComponentContainer)array_pool.poll();
      if (var1 == null) {
         var1 = new ComponentContainer();
      }

      var1.entity = var0;
      return var1;
   }

   public static void Release(ComponentContainer var0) {
      if (var0.size() > 0 && Core.bDebug) {
         throw new RuntimeException("Releasing ComponentContainer which has contents that might not have been properly disposed.");
      } else {
         array_pool.offer(var0);
      }
   }

   private ComponentContainer() {
      this.componentArray = new Component[ComponentType.MAX_ID_INDEX];
      this.renderersArray = new SnapshotArray(false, 16, Component.class);
      this.dirtyRenderers = false;
      this.componentBits = new BitSet();
      this.bucketBits = new BitSet();
   }

   BitSet getComponentBits() {
      return this.componentBits;
   }

   BitSet getBucketBits() {
      return this.bucketBits;
   }

   int size() {
      return this.componentList.size;
   }

   int getCapacity() {
      return this.componentArray.length;
   }

   boolean isEmpty() {
      return this.componentList.size == 0;
   }

   ComponentOperationHandler getComponentOperationHandler() {
      return this.componentOperationHandler;
   }

   void setComponentOperationHandler(ComponentOperationHandler var1) {
      this.componentOperationHandler = var1;
   }

   boolean isIdenticalTo(ComponentContainer var1) {
      if (var1 == null) {
         return false;
      } else {
         return var1 == this ? true : this.componentBits.equals(var1.componentBits);
      }
   }

   Component get(ComponentType var1) {
      return this.componentArray[var1.GetID()];
   }

   Component getForIndex(int var1) {
      return (Component)this.componentList.get(var1);
   }

   Component removeIndex(int var1) {
      Component var2 = (Component)this.componentList.get(var1);
      if (var2 != null) {
         this.remove(var2.getComponentType());
      }

      return var2;
   }

   boolean removeComponent(Component var1) {
      if (var1 == null) {
         return false;
      } else {
         Component var2 = this.get(var1.getComponentType());
         if (var2 == var1) {
            this.remove(var1.getComponentType());
            return true;
         } else {
            return false;
         }
      }
   }

   Component remove(ComponentType var1) {
      Component var2 = this.componentArray[var1.GetID()];
      if (var2 != null) {
         this.componentArray[var1.GetID()] = null;
         this.componentList.removeValue(var2, true);
         this.componentBits.clear(var1.GetID());
         if (var2.isRenderLast()) {
            this.renderersArray.removeValue(var2, true);
            this.dirtyRenderers = true;
         }

         if (this.componentOperationHandler != null) {
            this.componentOperationHandler.remove(this.entity);
         }
      }

      return var2;
   }

   boolean contains(ComponentType var1) {
      return this.componentArray[var1.GetID()] != null;
   }

   boolean contains(Component var1) {
      return var1 != null && this.contains(var1.getComponentType());
   }

   void add(Component var1) {
      if (var1 != null) {
         Component var2 = this.get(var1.getComponentType());
         if (var2 != null) {
            if (var2 == var1) {
               return;
            }

            this.remove(var1.getComponentType());
         }

         this.componentArray[var1.getComponentType().GetID()] = var1;
         this.componentList.add(var1);
         this.componentBits.set(var1.getComponentType().GetID());
         if (var1.isRenderLast()) {
            this.renderersArray.add(var1);
            this.dirtyRenderers = true;
         }

         if (this.componentOperationHandler != null) {
            this.componentOperationHandler.add(this.entity);
         }

      }
   }

   void release() {
      if (this.entity == null || !this.entity.addedToEntityManager && !this.entity.addedToEngine) {
         if (!this.bucketBits.isEmpty() && Core.bDebug) {
            throw new IllegalStateException("Entity is still registered to buckets?");
         } else {
            for(int var1 = 0; var1 < this.componentList.size; ++var1) {
               Component var2 = (Component)this.componentList.get(var1);
               var2.setOwner((GameEntity)null);
               ComponentType.ReleaseComponent(var2);
            }

            Arrays.fill(this.componentArray, (Object)null);
            this.componentList.clear();
            this.renderersArray.clear();
            this.componentBits.clear();
            this.bucketBits.clear();
            if (this.componentOperationHandler != null) {
               this.componentOperationHandler = null;
               if (Core.bDebug) {
                  throw new IllegalStateException("ComponentHandler should be null.");
               }
            }

            this.dirtyRenderers = false;
            this.entity = null;
         }
      } else {
         throw new IllegalStateException("Engine should be removed from engine and manager.");
      }
   }

   boolean hasRenderers() {
      return this.renderersArray.size > 0;
   }

   void render() {
      if (this.dirtyRenderers) {
         this.renderersArray.sort(renderComparator);
         this.dirtyRenderers = false;
      }

      if (this.renderersArray.size == 1) {
         ((Component[])this.renderersArray.items)[0].renderlast();
      } else if (this.renderersArray.size > 1) {
         Component[] var1 = (Component[])this.renderersArray.begin();

         try {
            int var2 = this.renderersArray.size;

            for(int var3 = 0; var3 < var2; ++var3) {
               if (var1[var3].isValid()) {
                  var1[var3].renderlast();
               }
            }
         } finally {
            this.renderersArray.end();
         }
      }

   }

   private static class ComponentRenderComparator implements Comparator<Component> {
      private ComponentRenderComparator() {
      }

      public int compare(Component var1, Component var2) {
         return (int)Math.signum((float)(var1.getRenderLastPriority() - var2.getRenderLastPriority()));
      }
   }
}
