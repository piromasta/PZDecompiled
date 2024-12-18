package zombie.entity;

import zombie.debug.DebugLog;
import zombie.entity.util.ImmutableArray;

public final class Engine {
   private final SystemManager systemManager = new SystemManager(this, false);
   private final EngineEntityManager entityManager = new EngineEntityManager(this, new EngineDelayedInformer());
   private final EntityBucketManager bucketManager;
   private EntityListener entityListener;
   private boolean processing = false;

   public Engine() {
      this.bucketManager = this.entityManager.getBucketManager();
   }

   public boolean isProcessing() {
      return this.processing;
   }

   void setEntityListener(EntityListener var1) {
      this.entityListener = var1;
   }

   public EntityBucket getRendererBucket() {
      return this.bucketManager.getRendererBucket();
   }

   public EntityBucket getIsoObjectBucket() {
      return this.bucketManager.getIsoObjectBucket();
   }

   public EntityBucket getInventoryItemBucket() {
      return this.bucketManager.getInventoryItemBucket();
   }

   public EntityBucket getVehiclePartBucket() {
      return this.bucketManager.getVehiclePartBucket();
   }

   public EntityBucket getBucket(Family var1) {
      return this.bucketManager.getBucket(var1);
   }

   public EntityBucket registerCustomBucket(String var1, EntityBucket.EntityValidator var2) {
      return this.bucketManager.registerCustomBucket(var1, var2);
   }

   public EntityBucket getCustomBucket(String var1) {
      return this.bucketManager.getCustomBucket(var1);
   }

   boolean addEntity(GameEntity var1) {
      this.entityManager.addEntity(var1);
      return true;
   }

   void removeEntity(GameEntity var1) {
      this.entityManager.removeEntity(var1);
   }

   void removeAllEntities() {
      this.entityManager.removeAllEntities();
   }

   void onEntityAdded(GameEntity var1) {
      if (this.entityListener != null) {
         this.entityListener.onEntityAddedToEngine(var1);
      }

   }

   void onEntityRemoved(GameEntity var1) {
      if (this.entityListener != null) {
         this.entityListener.onEntityRemovedFromEngine(var1);
      }

   }

   void update() {
      if (this.processing) {
         throw new IllegalStateException("Cannot call update() engine is already processing.");
      } else {
         this.processing = true;

         try {
            while(this.systemManager.hasPendingOperations()) {
               this.systemManager.processPendingOperations();
            }

            ImmutableArray var1 = this.systemManager.getUpdaterSystems();

            for(int var2 = 0; var2 < var1.size(); ++var2) {
               EngineSystem var3 = (EngineSystem)var1.get(var2);
               var3.update();
               this.entityManager.updateOperations();
            }
         } finally {
            this.processing = false;
         }

      }
   }

   void updateSimulation() {
      if (this.processing) {
         throw new IllegalStateException("Cannot call simulationUpdate() engine is already processing.");
      } else {
         this.processing = true;

         try {
            while(this.systemManager.hasPendingOperations()) {
               this.systemManager.processPendingOperations();
            }

            ImmutableArray var1 = this.systemManager.getSimulationUpdaterSystems();

            for(int var2 = 0; var2 < var1.size(); ++var2) {
               EngineSystem var3 = (EngineSystem)var1.get(var2);
               var3.updateSimulation();
               this.entityManager.updateOperations();
            }
         } finally {
            this.processing = false;
         }

      }
   }

   void renderLast() {
      if (this.processing) {
         throw new IllegalStateException("Cannot call renderLast() engine is already processing.");
      } else {
         this.processing = true;

         try {
            ImmutableArray var1 = this.systemManager.getRendererSystems();

            for(int var2 = 0; var2 < var1.size(); ++var2) {
               EngineSystem var3 = (EngineSystem)var1.get(var2);
               var3.renderLast();
               this.entityManager.updateOperations();
            }
         } finally {
            this.processing = false;
         }

      }
   }

   ImmutableArray<GameEntity> getEntities() {
      return this.entityManager.getEntities();
   }

   <T extends EngineSystem> T addSystem(T var1) {
      this.systemManager.addSystem(var1);
      return var1;
   }

   void removeSystem(EngineSystem var1) {
      this.systemManager.removeSystem(var1);
   }

   void removeAllSystems() {
      this.systemManager.removeAllSystems();
   }

   public <T extends EngineSystem> T getSystem(Class<T> var1) {
      return this.systemManager.getSystem(var1);
   }

   public ImmutableArray<EngineSystem> getSystems() {
      return this.systemManager.getSystems();
   }

   protected void printSystems() {
      DebugLog.log("=== Engine Registered Systems ===");
      ImmutableArray var1 = this.systemManager.getSystems();

      int var2;
      for(var2 = 0; var2 < var1.size(); ++var2) {
         DebugLog.log("[" + var2 + "] = " + ((EngineSystem)var1.get(var2)).getClass().getSimpleName());
      }

      DebugLog.log("");
      DebugLog.log("- UPDATERS -");
      var1 = this.systemManager.getUpdaterSystems();

      for(var2 = 0; var2 < var1.size(); ++var2) {
         DebugLog.log("[" + var2 + "] = " + ((EngineSystem)var1.get(var2)).getClass().getSimpleName());
      }

      DebugLog.log("");
      DebugLog.log("- RENDERERS -");
      var1 = this.systemManager.getRendererSystems();

      for(var2 = 0; var2 < var1.size(); ++var2) {
         DebugLog.log("[" + var2 + "] = " + ((EngineSystem)var1.get(var2)).getClass().getSimpleName());
      }

      DebugLog.log("=================================");
   }

   private class EngineDelayedInformer implements IBooleanInformer {
      private EngineDelayedInformer() {
      }

      public boolean value() {
         return Engine.this.processing;
      }
   }

   interface EntityListener {
      void onEntityAddedToEngine(GameEntity var1);

      void onEntityRemovedFromEngine(GameEntity var1);
   }
}
