package zombie.entity;

import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectMap;

public final class EntityBucketManager {
   private final ObjectMap<Family, EntityBucket> buckets = new ObjectMap();
   private final ObjectMap<String, EntityBucket> customBuckets = new ObjectMap();
   private final Array<EntityBucket> bucketsArray = new Array(false, 16);
   private final EntityBucket.RendererBucket rendererBucket;
   private EntityBucket.IsoObjectBucket isoObjectBucket;
   private EntityBucket.InventoryItemBucket inventoryItemBucket;
   private EntityBucket.VehiclePartBucket vehiclePartBucket;
   private int bucketIndex = 0;
   private final ImmutableArray<GameEntity> entities;
   private boolean updating = false;
   private GameEntity currentUpdatingEntity;
   private final BucketsUpdatingInformer bucketsUpdatingInformer = new BucketsUpdatingInformer();

   protected EntityBucketManager(ImmutableArray<GameEntity> var1) {
      this.entities = var1;
      this.rendererBucket = new EntityBucket.RendererBucket(this.bucketIndex++);
      this.bucketsArray.add(this.rendererBucket);
   }

   BucketsUpdatingInformer getBucketsUpdatingInformer() {
      return this.bucketsUpdatingInformer;
   }

   EntityBucket getRendererBucket() {
      return this.rendererBucket;
   }

   EntityBucket getIsoObjectBucket() {
      if (this.isoObjectBucket == null) {
         this.isoObjectBucket = new EntityBucket.IsoObjectBucket(this.bucketIndex++);
         this.bucketsArray.add(this.isoObjectBucket);

         for(int var1 = 0; var1 < this.entities.size(); ++var1) {
            this.isoObjectBucket.updateMembership((GameEntity)this.entities.get(var1));
         }
      }

      return this.isoObjectBucket;
   }

   EntityBucket getInventoryItemBucket() {
      if (this.inventoryItemBucket == null) {
         this.inventoryItemBucket = new EntityBucket.InventoryItemBucket(this.bucketIndex++);
         this.bucketsArray.add(this.inventoryItemBucket);

         for(int var1 = 0; var1 < this.entities.size(); ++var1) {
            this.inventoryItemBucket.updateMembership((GameEntity)this.entities.get(var1));
         }
      }

      return this.inventoryItemBucket;
   }

   EntityBucket getVehiclePartBucket() {
      if (this.vehiclePartBucket == null) {
         this.vehiclePartBucket = new EntityBucket.VehiclePartBucket(this.bucketIndex++);
         this.bucketsArray.add(this.vehiclePartBucket);

         for(int var1 = 0; var1 < this.entities.size(); ++var1) {
            this.vehiclePartBucket.updateMembership((GameEntity)this.entities.get(var1));
         }
      }

      return this.vehiclePartBucket;
   }

   EntityBucket getBucket(Family var1) {
      Object var2 = (EntityBucket)this.buckets.get(var1);
      if (var2 == null) {
         var2 = new EntityBucket.FamilyBucket(this.bucketIndex++, var1);
         this.buckets.put(var1, var2);
         this.bucketsArray.add(var2);

         for(int var3 = 0; var3 < this.entities.size(); ++var3) {
            ((EntityBucket)var2).updateMembership((GameEntity)this.entities.get(var3));
         }
      }

      return (EntityBucket)var2;
   }

   EntityBucket registerCustomBucket(String var1, EntityBucket.EntityValidator var2) {
      if (this.customBuckets.get(var1) != null) {
         throw new IllegalArgumentException("Bucket with identifier '" + var1 + "' already exists.");
      } else {
         EntityBucket.CustomBucket var3 = new EntityBucket.CustomBucket(this.bucketIndex++, var2);
         this.customBuckets.put(var1, var3);
         this.bucketsArray.add(var3);

         for(int var4 = 0; var4 < this.entities.size(); ++var4) {
            var3.updateMembership((GameEntity)this.entities.get(var4));
         }

         return var3;
      }
   }

   EntityBucket getCustomBucket(String var1) {
      return (EntityBucket)this.customBuckets.get(var1);
   }

   void updateBucketMembership(GameEntity var1) {
      this.updating = true;
      this.currentUpdatingEntity = var1;

      try {
         if (this.bucketsArray.size > 0) {
            for(int var2 = 0; var2 < this.bucketsArray.size; ++var2) {
               ((EntityBucket)this.bucketsArray.get(var2)).updateMembership(var1);
            }
         }
      } finally {
         this.updating = false;
         this.currentUpdatingEntity = null;
      }

   }

   protected class BucketsUpdatingInformer implements IBucketInformer {
      protected BucketsUpdatingInformer() {
      }

      public boolean value() {
         return EntityBucketManager.this.updating;
      }

      public GameEntity updatingEntity() {
         return EntityBucketManager.this.currentUpdatingEntity;
      }
   }
}
