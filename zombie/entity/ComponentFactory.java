package zombie.entity;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.Null;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.reflect.ClassReflection;
import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;

public class ComponentFactory {
   private final ObjectMap<Class<?>, ComponentPool> pools;
   private final int initialSize;
   private final int maxSize;

   public ComponentFactory() {
      this(1024, 2147483647);
   }

   public ComponentFactory(int var1, int var2) {
      this.pools = new ObjectMap();
      this.initialSize = var1;
      this.maxSize = var2;
   }

   public <T extends Component> T alloc(Class<T> var1) {
      ComponentPool var2 = (ComponentPool)this.pools.get(var1);
      if (var2 == null) {
         var2 = new ComponentPool(var1, this.initialSize, this.maxSize);
         this.pools.put(var1, var2);
      }

      return var2.obtain();
   }

   public <T extends Component> void release(T var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("component cannot be null.");
      } else {
         ComponentPool var2 = (ComponentPool)this.pools.get(var1.getClass());
         if (var2 != null) {
            assert !Core.bDebug || !var2.pool.contains(var1) : "Object already in pool.";

            if (var1.owner != null) {
               DebugLog.General.error("Owner not removed?");
               if (Core.bDebug) {
                  throw new RuntimeException("Owner not removed");
               }

               var1.owner.removeComponent(var1);
            }

            var2.free(var1);
         }
      }
   }

   private static class ComponentPool<T extends Component> {
      protected final ConcurrentLinkedDeque<T> pool;
      private final Constructor constructor;
      private final int max;
      private int peak;

      public ComponentPool(Class<T> var1) {
         this(var1, 16, 2147483647);
      }

      public ComponentPool(Class<T> var1, int var2) {
         this(var1, var2, 2147483647);
      }

      public ComponentPool(Class<T> var1, int var2, int var3) {
         this.max = var3;
         this.pool = new ConcurrentLinkedDeque();
         this.constructor = this.findConstructor(var1);
         if (this.constructor == null) {
            throw new RuntimeException("Class cannot be created (missing no-arg constructor): " + var1.getName());
         }
      }

      @Null
      private Constructor findConstructor(Class<T> var1) {
         try {
            return ClassReflection.getConstructor(var1, (Class[])null);
         } catch (Exception var5) {
            try {
               Constructor var3 = ClassReflection.getDeclaredConstructor(var1, (Class[])null);
               var3.setAccessible(true);
               return var3;
            } catch (ReflectionException var4) {
               return null;
            }
         }
      }

      public T obtain() {
         Component var1 = (Component)this.pool.poll();
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

      protected T newObject() {
         try {
            return (Component)this.constructor.newInstance((Object[])null);
         } catch (Exception var2) {
            throw new RuntimeException("Unable to create new instance: " + this.constructor.getDeclaringClass().getName(), var2);
         }
      }
   }
}
