package zombie.entity;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.util.ImmutableArray;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.GameEntityScript;

public class EntityDebugger {
   private final Engine engine;
   private long updateStamp;
   private int updatePeakMilli = 0;
   private int[] updateTimes = new int[60];
   private int updateTimeIdx = 0;
   private int updateTimeMilliAverage = 0;
   private int lifeTimeSaves = 0;
   private int lifeTimeLoads = 0;
   private byte[] bytes_cache;

   EntityDebugger(Engine var1) {
      this.engine = var1;
   }

   void beginUpdate() {
      this.updateStamp = System.nanoTime();
   }

   void endUpdate() {
      int var1 = (int)((System.nanoTime() - this.updateStamp) / 1000000L);
      this.updatePeakMilli = PZMath.max(this.updatePeakMilli, var1);
      this.updateTimes[this.updateTimeIdx] = var1;
      ++this.updateTimeIdx;
      if (this.updateTimeIdx >= this.updateTimes.length) {
         this.updateTimeIdx = 0;
         int var2 = 0;

         for(int var3 = 0; var3 < this.updateTimes.length; ++var3) {
            var2 += this.updateTimes[var3];
         }

         this.updateTimeMilliAverage = var2 / this.updateTimes.length;
      }

   }

   void beginSave(File var1) {
      ++this.lifeTimeSaves;
   }

   void endSave(ByteBuffer var1) {
   }

   void beginLoad(File var1) {
      ++this.lifeTimeLoads;
   }

   void endLoad(ByteBuffer var1) {
   }

   ArrayList<GameEntity> getIsoEntitiesDebug() {
      if (!Core.bDebug) {
         return null;
      } else {
         ImmutableArray var1 = this.engine.getIsoObjectBucket().getEntities();
         ArrayList var2 = new ArrayList();

         for(int var4 = 0; var4 < var1.size(); ++var4) {
            GameEntity var3 = (GameEntity)var1.get(var4);
            if (var3.getGameEntityType() == GameEntityType.IsoObject && var3 instanceof IsoObject && (!var3.hasComponent(ComponentType.SpriteConfig) || ((SpriteConfig)var3.getComponent(ComponentType.SpriteConfig)).isMultiSquareMaster())) {
               var2.add(var3);
            }
         }

         return var2;
      }
   }

   void reloadDebug() throws Exception {
      if (Core.bDebug && !GameClient.bClient && !GameServer.bServer) {
         if (this.engine.isProcessing()) {
            DebugLog.General.println("Cannot reload entities when engine is processing.");
            return;
         }

         ImmutableArray var1 = this.engine.getIsoObjectBucket().getEntities();
         DebugLog.General.println("-- Reloading '" + var1.size() + "' Instantiated IsoObject Entities --");
         if (var1.size() == 0) {
            return;
         }

         byte[] var2 = this.bytes_cache != null ? this.bytes_cache : new byte[2097152];
         this.bytes_cache = var2;
         ByteBuffer var3 = ByteBuffer.wrap(var2);

         for(int var5 = 0; var5 < var1.size(); ++var5) {
            GameEntity var4 = (GameEntity)var1.get(var5);

            try {
               DebugLog.General.println("Reloading: " + var4.getEntityFullTypeDebug());
               this.reloadEntity(var3, var4);
            } catch (Exception var7) {
               var7.printStackTrace();
            }
         }
      }

   }

   void reloadDebugEntity(GameEntity var1) throws Exception {
      if (Core.bDebug && !GameClient.bClient && !GameServer.bServer) {
         if (this.engine.isProcessing()) {
            DebugLog.General.println("Cannot reload entities when engine is processing.");
            return;
         }

         DebugLog.General.println("-- Reloading Entity --");
         if (var1.getGameEntityType() != GameEntityType.IsoObject || !(var1 instanceof IsoObject)) {
            DebugLog.General.println("Failed to reload entity, not IsoObject");
            return;
         }

         byte[] var2 = this.bytes_cache != null ? this.bytes_cache : new byte[2097152];
         this.bytes_cache = var2;
         ByteBuffer var3 = ByteBuffer.wrap(var2);
         this.reloadEntity(var3, var1);
      }

   }

   private void removeEntityComponents(GameEntity var1) {
      if (!this.engine.isProcessing() && !var1.addedToEntityManager && !var1.addedToEngine) {
         if (var1.hasComponents()) {
            for(int var3 = var1.componentSize() - 1; var3 >= 0; --var3) {
               Component var2 = var1.getComponentForIndex(var3);
               var1.releaseComponent(var2);
            }
         }

      } else {
         throw new RuntimeException("Cannot remove components when engine is processing or entity added to manager.");
      }
   }

   private void reloadEntity(ByteBuffer var1, GameEntity var2) throws Exception {
      if (this.engine.isProcessing()) {
         DebugLog.General.println("Cannot reload entities when engine is processing.");
      } else {
         boolean var3 = false;
         if (var2.addedToEntityManager) {
            var3 = true;
            GameEntityManager.UnregisterEntity(var2);
         }

         var1.clear();
         var2.saveEntity(var1);
         var1.flip();
         this.removeEntityComponents(var2);
         var2.loadEntity(var1, 219);
         if (var3) {
            GameEntityManager.RegisterEntity(var2);
         }

      }
   }

   void reloadEntityFromScriptDebug(GameEntity var1) throws Exception {
      if (Core.bDebug && !GameClient.bClient && !GameServer.bServer) {
         if (this.engine.isProcessing()) {
            DebugLog.General.println("Cannot reload entities when engine is processing.");
            return;
         }

         DebugLog.General.println("-- Reloading Entity From Script --");
         if (var1.getGameEntityType() != GameEntityType.IsoObject || !(var1 instanceof IsoObject)) {
            DebugLog.General.println("Failed to reload entity from script, not IsoObject");
            return;
         }

         EntityScriptInfo var2 = (EntityScriptInfo)var1.getComponent(ComponentType.Script);
         if (var2 != null && var2.getScript() != null) {
            boolean var3 = false;
            if (var1.addedToEntityManager) {
               var3 = true;
               GameEntityManager.UnregisterEntity(var1);
            }

            this.removeEntityComponents(var1);
            GameEntityScript var4 = var2.getScript();
            GameEntityFactory.CreateEntityDebugReload(var1, var4, false);
            if (var3) {
               GameEntityManager.RegisterEntity(var1);
            }
         } else {
            DebugLog.General.warn("Failed to reload from script, no script component or component has no valid script.");
         }
      }

   }
}
