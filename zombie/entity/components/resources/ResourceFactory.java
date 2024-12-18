package zombie.entity.components.resources;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.ObjectMap;

public class ResourceFactory {
   private static final ObjectMap<ResourceType, ResourcePool> pools = new ObjectMap();
   private static final int initialSize = 512;
   private static final int maxSize = 2147483647;

   public ResourceFactory() {
   }

   static Resource createResource(String var0) {
      try {
         ResourceBlueprint var1 = ResourceBlueprint.Deserialize(var0);
         return createResource(var1);
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   static Resource createResource(ResourceBlueprint var0) {
      Resource var1 = alloc(var0.getType());
      var1.loadBlueprint(var0);
      return var1;
   }

   static Resource createBlancResource(ResourceType var0) {
      return alloc(var0);
   }

   static void releaseResource(Resource var0) {
      release(var0);
   }

   private static <T extends Resource> T alloc(ResourceType var0) {
      ResourcePool var1 = (ResourcePool)pools.get(var0);
      return var1.obtain();
   }

   private static <T extends Resource> void release(T var0) {
      if (var0 == null) {
         throw new IllegalArgumentException("resource cannot be null.");
      } else {
         ResourcePool var1 = (ResourcePool)pools.get(var0.getType());
         if (var1 != null) {
            assert !Core.bDebug || !var1.pool.contains(var0) : "Object already in pool.";

            if (var0.getResourcesComponent() != null) {
               DebugLog.General.error("Resource not removed from Resources Component");
               if (Core.bDebug) {
               }

               var0.getResourcesComponent().removeResource(var0);
            }

            var1.free(var0);
         }
      }
   }

   static {
      pools.put(ResourceType.Item, new ResourcePool<ResourceItem>(512, 2147483647) {
         protected ResourceItem newObject() {
            return new ResourceItem();
         }
      });
      pools.put(ResourceType.Fluid, new ResourcePool<ResourceFluid>(512, 2147483647) {
         protected ResourceFluid newObject() {
            return new ResourceFluid();
         }
      });
      pools.put(ResourceType.Energy, new ResourcePool<ResourceEnergy>(512, 2147483647) {
         protected ResourceEnergy newObject() {
            return new ResourceEnergy();
         }
      });
   }

   private abstract static class ResourcePool<T extends Resource> {
      protected final ConcurrentLinkedDeque<T> pool;
      private final int max;
      private int peak;

      public ResourcePool() {
         this(16, 2147483647);
      }

      public ResourcePool(int var1) {
         this(var1, 2147483647);
      }

      public ResourcePool(int var1, int var2) {
         this.max = var2;
         this.pool = new ConcurrentLinkedDeque();
      }

      public T obtain() {
         Resource var1 = (Resource)this.pool.poll();
         if (var1 == null) {
            var1 = this.newObject();
         }

         return var1;
      }

      public void free(T var1) {
         if (var1 == null) {
            throw new IllegalArgumentException("object cannot be null.");
         } else {
            if (this.pool.size() < this.max) {
               var1.reset();
               this.pool.add(var1);
               this.peak = Math.max(this.peak, this.pool.size());
            } else {
               var1.reset();
            }

         }
      }

      protected abstract T newObject();
   }
}
