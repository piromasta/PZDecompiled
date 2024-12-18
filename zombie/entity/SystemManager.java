package zombie.entity;

import java.util.Comparator;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.SingleThreadPool;

public final class SystemManager {
   private final SystemUpdateComparator systemUpdateComparator = new SystemUpdateComparator();
   private final SystemUpdateSimulationComparator systemUpdateSimulationComparator = new SystemUpdateSimulationComparator();
   private final SystemRenderComparator systemRenderComparator = new SystemRenderComparator();
   private final Array<EngineSystem> systems = new Array(false, 16);
   private final Array<EngineSystem> updaterSystems = new Array(true, 16);
   private final Array<EngineSystem> simulationUpdaterSystems = new Array(true, 16);
   private final Array<EngineSystem> rendererSystems = new Array(true, 16);
   private final ImmutableArray<EngineSystem> immutableSystems;
   private final ImmutableArray<EngineSystem> immutableUpdaterSystems;
   private final ImmutableArray<EngineSystem> immutableSimulationUpdaterSystems;
   private final ImmutableArray<EngineSystem> immutableRendererSystems;
   private final ObjectMap<Class<?>, EngineSystem> systemsByClass;
   private final Engine engine;
   private final Array<SystemOperation> pendingOperations;
   private final SystemOperationPool systemOperationPool;
   private final SystemMembershipListener systemMembershipListener;
   private final boolean enableDynamicSystems;

   protected SystemManager(Engine var1, boolean var2) {
      this.immutableSystems = new ImmutableArray(this.systems);
      this.immutableUpdaterSystems = new ImmutableArray(this.updaterSystems);
      this.immutableSimulationUpdaterSystems = new ImmutableArray(this.simulationUpdaterSystems);
      this.immutableRendererSystems = new ImmutableArray(this.rendererSystems);
      this.systemsByClass = new ObjectMap();
      this.pendingOperations = new Array(false, 16);
      this.systemOperationPool = new SystemOperationPool();
      this.systemMembershipListener = new SystemMembershipListener();
      this.engine = var1;
      this.enableDynamicSystems = var2;
   }

   void addSystem(EngineSystem var1) {
      if (this.engine.isProcessing()) {
         if (!this.enableDynamicSystems) {
            throw new UnsupportedOperationException("Cannot modify systems while the Engine is processing.");
         }

         this.addSystemDelayed(var1);
      } else {
         this.addSystemInternal(var1);
      }

   }

   void addSystemDelayed(EngineSystem var1) {
      for(int var2 = 0; var2 < this.pendingOperations.size; ++var2) {
         SystemOperation var3 = (SystemOperation)this.pendingOperations.get(var2);
         if (var3.system == var1) {
            var3.valid = false;
         }
      }

      SystemOperation var4 = (SystemOperation)this.systemOperationPool.obtain();
      var4.system = var1;
      var4.type = SystemManager.SystemOperation.Type.Add;
      this.pendingOperations.add(var4);
   }

   void addSystemInternal(EngineSystem var1) {
      Class var2 = var1.getClass();
      EngineSystem var3 = this.getSystem(var2);
      if (var3 == var1) {
         this.updateSystemMembership(var1);
      } else {
         if (var3 != null) {
            this.removeSystem(var3);
         }

         var1.membershipListener = this.systemMembershipListener;
         this.systems.add(var1);
         this.systemsByClass.put(var2, var1);
         this.updateSystemMembership(var1);
         var1.addedToEngineInternal(this.engine);
      }
   }

   void removeSystem(EngineSystem var1) {
      if (this.engine.isProcessing()) {
         if (!this.enableDynamicSystems) {
            throw new UnsupportedOperationException("Cannot modify systems while the Engine is processing.");
         }

         this.removeSystemDelayed(var1);
      } else {
         this.removeSystemInternal(var1);
      }

   }

   void removeSystemDelayed(EngineSystem var1) {
      for(int var2 = 0; var2 < this.pendingOperations.size; ++var2) {
         SystemOperation var3 = (SystemOperation)this.pendingOperations.get(var2);
         if (var3.system == var1) {
            var3.valid = false;
         }
      }

      SystemOperation var4 = (SystemOperation)this.systemOperationPool.obtain();
      var4.system = var1;
      var4.type = SystemManager.SystemOperation.Type.Remove;
      this.pendingOperations.add(var4);
   }

   void removeSystemInternal(EngineSystem var1) {
      if (this.systems.removeValue(var1, true)) {
         var1.membershipListener = null;
         this.systemsByClass.remove(var1.getClass());
         this.updaterSystems.removeValue(var1, true);
         this.simulationUpdaterSystems.removeValue(var1, true);
         this.rendererSystems.removeValue(var1, true);
         var1.removedFromEngineInternal(this.engine);
      }

   }

   void removeAllSystems() {
      while(this.systems.size > 0) {
         this.removeSystem((EngineSystem)this.systems.first());
      }

   }

   <T extends EngineSystem> T getSystem(Class<T> var1) {
      return (EngineSystem)this.systemsByClass.get(var1);
   }

   ImmutableArray<EngineSystem> getSystems() {
      return this.immutableSystems;
   }

   ImmutableArray<EngineSystem> getUpdaterSystems() {
      return this.immutableUpdaterSystems;
   }

   ImmutableArray<EngineSystem> getSimulationUpdaterSystems() {
      return this.immutableSimulationUpdaterSystems;
   }

   ImmutableArray<EngineSystem> getRendererSystems() {
      return this.immutableRendererSystems;
   }

   boolean hasPendingOperations() {
      return this.pendingOperations.size > 0;
   }

   void processPendingOperations() {
      for(int var1 = 0; var1 < this.pendingOperations.size; ++var1) {
         SystemOperation var2 = (SystemOperation)this.pendingOperations.get(var1);
         if (!var2.valid) {
            this.systemOperationPool.free(var2);
         } else {
            switch (var2.type) {
               case Add:
                  this.addSystemInternal(var2.system);
                  break;
               case Remove:
                  this.removeSystemInternal(var2.system);
                  break;
               case UpdateMembership:
                  this.updateSystemMembership(var2.system);
                  break;
               default:
                  throw new AssertionError("Unexpected SystemOperation type");
            }

            this.systemOperationPool.free(var2);
         }
      }

      this.pendingOperations.clear();
   }

   private void updateSystemMembership(EngineSystem var1) {
      boolean var2 = var1.isEnabled();
      boolean var3 = var1.isUpdater();
      if (var2 && var3 && !this.updaterSystems.contains(var1, true)) {
         this.updaterSystems.add(var1);
      } else if ((!var2 || !var3) && this.updaterSystems.contains(var1, true)) {
         this.updaterSystems.removeValue(var1, true);
      }

      boolean var4 = var1.isSimulationUpdater();
      if (var2 && var4 && !this.simulationUpdaterSystems.contains(var1, true)) {
         this.simulationUpdaterSystems.add(var1);
      } else if ((!var2 || !var4) && this.simulationUpdaterSystems.contains(var1, true)) {
         this.simulationUpdaterSystems.removeValue(var1, true);
      }

      boolean var5 = var1.isRenderer();
      if (var2 && var5 && !this.rendererSystems.contains(var1, true)) {
         this.rendererSystems.add(var1);
      } else if ((!var2 || !var5) && this.rendererSystems.contains(var1, true)) {
         this.rendererSystems.removeValue(var1, true);
      }

      this.updaterSystems.sort(this.systemUpdateComparator);
      this.simulationUpdaterSystems.sort(this.systemUpdateSimulationComparator);
      this.rendererSystems.sort(this.systemRenderComparator);
   }

   private static class SystemUpdateComparator implements Comparator<EngineSystem> {
      private SystemUpdateComparator() {
      }

      public int compare(EngineSystem var1, EngineSystem var2) {
         return Integer.compare(var1.getUpdatePriority(), var2.getUpdatePriority());
      }
   }

   private static class SystemUpdateSimulationComparator implements Comparator<EngineSystem> {
      private SystemUpdateSimulationComparator() {
      }

      public int compare(EngineSystem var1, EngineSystem var2) {
         return Integer.compare(var1.getUpdateSimulationPriority(), var2.getUpdateSimulationPriority());
      }
   }

   private static class SystemRenderComparator implements Comparator<EngineSystem> {
      private SystemRenderComparator() {
      }

      public int compare(EngineSystem var1, EngineSystem var2) {
         return Integer.compare(var1.getRenderLastPriority(), var2.getRenderLastPriority());
      }
   }

   private static class SystemOperationPool extends SingleThreadPool<SystemOperation> {
      private SystemOperationPool() {
      }

      protected SystemOperation newObject() {
         return new SystemOperation();
      }
   }

   private class SystemMembershipListener implements EngineSystem.MembershipListener {
      private SystemMembershipListener() {
      }

      public void onMembershipPropertyChanged(EngineSystem var1) {
         for(int var2 = 0; var2 < SystemManager.this.pendingOperations.size; ++var2) {
            SystemOperation var3 = (SystemOperation)SystemManager.this.pendingOperations.get(var2);
            if (var3.system == var1 && var3.valid) {
               return;
            }
         }

         SystemOperation var4 = (SystemOperation)SystemManager.this.systemOperationPool.obtain();
         var4.system = var1;
         var4.type = SystemManager.SystemOperation.Type.UpdateMembership;
         SystemManager.this.pendingOperations.add(var4);
      }
   }

   private static class SystemOperation implements SingleThreadPool.Poolable {
      Type type;
      EngineSystem system;
      boolean valid = true;

      private SystemOperation() {
      }

      public void reset() {
         this.system = null;
         this.valid = true;
      }

      public static enum Type {
         Add,
         Remove,
         UpdateMembership;

         private Type() {
         }
      }
   }
}
