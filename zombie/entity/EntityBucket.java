package zombie.entity;

import java.util.Comparator;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.util.Array;
import zombie.entity.util.BitSet;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectSet;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.vehicles.VehiclePart;

public abstract class EntityBucket {
   private final Array<GameEntity> entities = new Array(false, 16);
   private final ImmutableArray<GameEntity> immutableEntities;
   private final Array<BucketListenerData> listeners = new Array(true, 16);
   private final ObjectSet<IBucketListener> listenerSet = new ObjectSet();
   private final BucketListenerComparator listenerComparator = new BucketListenerComparator();
   private final int index;
   private boolean verbose = false;

   private EntityBucket(int var1) {
      this.immutableEntities = new ImmutableArray(this.entities);
      this.index = var1;
   }

   public final int getIndex() {
      return this.index;
   }

   public final ImmutableArray<GameEntity> getEntities() {
      return this.immutableEntities;
   }

   public final void setVerbose(boolean var1) {
      this.verbose = var1;
   }

   protected abstract boolean acceptsEntity(GameEntity var1);

   final void updateMembership(GameEntity var1) {
      BitSet var2 = var1.getBucketBits();
      boolean var3 = var2.get(this.index);
      boolean var4 = this.acceptsEntity(var1);
      DebugLogStream var10000;
      long var10001;
      if (Core.bDebug && this.verbose) {
         var10000 = DebugLog.Entity;
         var10001 = var1.getEntityNetID();
         var10000.println("testing entity = " + var10001 + ", type=" + var1.getGameEntityType() + ", contains=" + var3 + ", accepts=" + var4 + ", removing=" + var1.removingFromEngine);
      }

      int var5;
      if (!var1.removingFromEngine && !var3 && var4) {
         if (Core.bDebug && this.verbose) {
            var10000 = DebugLog.Entity;
            var10001 = var1.getEntityNetID();
            var10000.println("adding entity = " + var10001 + ", type=" + var1.getGameEntityType());
         }

         if (Core.bDebug && GameEntityManager.DEBUG_MODE && this.entities.contains(var1, true)) {
            throw new RuntimeException("Entity already exists in bucket.");
         }

         this.entities.add(var1);
         var2.set(this.index);
         if (Core.bDebug && this.verbose) {
            var10000 = DebugLog.Entity;
            boolean var6 = var2.get(this.index);
            var10000.println("bits = " + var6);
         }

         if (this.listeners.size > 0) {
            for(var5 = 0; var5 < this.listeners.size; ++var5) {
               ((BucketListenerData)this.listeners.get(var5)).listener.onBucketEntityAdded(this, var1);
            }
         }
      } else if (var3 && (var1.removingFromEngine || !var4)) {
         if (Core.bDebug && this.verbose) {
            var10000 = DebugLog.Entity;
            var10001 = var1.getEntityNetID();
            var10000.println("removing entity = " + var10001 + ", type=" + var1.getGameEntityType());
         }

         if (Core.bDebug && GameEntityManager.DEBUG_MODE && !this.entities.contains(var1, true)) {
            throw new RuntimeException("Entity should exist in bucket but does not.");
         }

         this.entities.removeValue(var1, true);
         var2.clear(this.index);
         if (this.listeners.size > 0) {
            for(var5 = 0; var5 < this.listeners.size; ++var5) {
               ((BucketListenerData)this.listeners.get(var5)).listener.onBucketEntityRemoved(this, var1);
            }
         }
      }

   }

   public final void addListener(int var1, IBucketListener var2) {
      if (!this.listenerSet.contains(var2)) {
         BucketListenerData var3 = new BucketListenerData();
         var3.listener = var2;
         var3.priority = var1;
         this.listeners.add(var3);
         this.listeners.sort(this.listenerComparator);
      }
   }

   public final void removeListener(IBucketListener var1) {
      if (this.listenerSet.remove(var1)) {
         for(int var2 = 0; var2 < this.listeners.size; ++var2) {
            if (((BucketListenerData)this.listeners.get(var2)).listener == var1) {
               this.listeners.removeIndex(var2);
               break;
            }
         }
      }

   }

   private static class BucketListenerComparator implements Comparator<BucketListenerData> {
      private BucketListenerComparator() {
      }

      public int compare(BucketListenerData var1, BucketListenerData var2) {
         return Integer.compare(var1.priority, var2.priority);
      }
   }

   private static class BucketListenerData {
      public IBucketListener listener;
      public int priority;

      private BucketListenerData() {
      }
   }

   protected static class CustomBucket extends EntityBucket {
      private final EntityValidator validator;

      protected CustomBucket(int var1, EntityValidator var2) {
         super(var1);
         this.validator = var2;
      }

      protected final boolean acceptsEntity(GameEntity var1) {
         return this.validator.acceptsEntity(var1);
      }
   }

   public interface EntityValidator {
      boolean acceptsEntity(GameEntity var1);
   }

   protected static class FamilyBucket extends EntityBucket {
      private final Family family;

      protected FamilyBucket(int var1, Family var2) {
         super(var1);
         this.family = var2;
      }

      protected final boolean acceptsEntity(GameEntity var1) {
         return this.family.matches(var1);
      }
   }

   protected static class VehiclePartBucket extends EntityBucket {
      protected VehiclePartBucket(int var1) {
         super(var1);
      }

      protected final boolean acceptsEntity(GameEntity var1) {
         return var1 instanceof VehiclePart;
      }
   }

   protected static class InventoryItemBucket extends EntityBucket {
      protected InventoryItemBucket(int var1) {
         super(var1);
      }

      protected final boolean acceptsEntity(GameEntity var1) {
         return var1 instanceof InventoryItem;
      }
   }

   protected static class IsoObjectBucket extends EntityBucket {
      protected IsoObjectBucket(int var1) {
         super(var1);
      }

      protected final boolean acceptsEntity(GameEntity var1) {
         return var1 instanceof IsoObject;
      }
   }

   protected static class RendererBucket extends EntityBucket {
      protected RendererBucket(int var1) {
         super(var1);
      }

      protected final boolean acceptsEntity(GameEntity var1) {
         return var1.hasRenderers();
      }
   }
}
