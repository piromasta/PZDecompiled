package zombie.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.components.crafting.CraftLogicSystem;
import zombie.entity.components.crafting.FurnaceLogicSystem;
import zombie.entity.components.crafting.MashingLogicSystem;
import zombie.entity.components.resources.LogisticsSystem;
import zombie.entity.components.resources.ResourceUpdateSystem;
import zombie.entity.meta.MetaTagComponent;
import zombie.entity.system.RenderLastSystem;
import zombie.entity.util.LongMap;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class GameEntityManager {
   private static boolean VERBOSE = true;
   public static boolean DEBUG_MODE = false;
   private static final boolean DEBUG_DISABLE_SAVE = false;
   private static final String saveFile = "entity_data.bin";
   private static File cacheFile;
   private static ByteBuffer cacheByteBuffer;
   private static final LongMap<GameEntity> idToEntityMap = new LongMap();
   private static Engine engine;
   private static EntityDebugger debugger;
   private static final ArrayDeque<MetaEntity> delayedReleaseMetaEntities = new ArrayDeque();
   private static boolean initialized = false;
   private static boolean bWasMultiplayer = false;
   public static final int bbBlockSize = 1048576;

   protected GameEntityManager() {
   }

   public static void Init(int var0) {
      if (engine != null) {
         DebugLog.General.warn("Previous engine not disposed!");
         engine = null;
      }

      cacheFile = ZomboidFileSystem.instance.getFileInCurrentSave("entity_data.bin");
      engine = new Engine();
      DEBUG_MODE = Core.bDebug;
      if (Core.bDebug) {
         debugger = new EntityDebugger(engine);
      }

      int var1 = 0;
      int var2 = var1++;
      int var3 = var1++;
      int var4 = var1++;
      int var5 = var1++;
      int var6 = var1++;
      int var7 = var1++;
      int var8 = var1++;
      int var9 = var1++;
      byte var11 = 0;
      var1 = var11 + 1;
      engine.setEntityListener(new EngineEntityListener());
      CustomBuckets.initializeCustomBuckets(engine);
      engine.addSystem(new CraftLogicSystem(var5));
      engine.addSystem(new UsingPlayerUpdateSystem(var2));
      engine.addSystem(new InventoryItemSystem(var3));
      engine.addSystem(new MetaEntitySystem(var8));
      engine.addSystem(new LogisticsSystem(var4));
      engine.addSystem(new ResourceUpdateSystem(var9));
      engine.addSystem(new MashingLogicSystem(var7));
      engine.addSystem(new FurnaceLogicSystem(var6));
      engine.addSystem(new RenderLastSystem(var11));
      if (Core.bDebug) {
      }

      bWasMultiplayer = GameClient.bClient || GameServer.bServer;
      initialized = true;
      load(var0);
   }

   public static void Update() {
      if (debugger != null) {
         debugger.beginUpdate();
      }

      engine.update();
      EntitySimulation.update();
      int var0 = EntitySimulation.getSimulationTicksThisFrame();
      if (var0 > 0) {
         for(int var1 = 0; var1 < var0; ++var1) {
            engine.updateSimulation();
         }
      }

      MetaEntity var2;
      if (delayedReleaseMetaEntities.size() > 0) {
         while((var2 = (MetaEntity)delayedReleaseMetaEntities.poll()) != null) {
            MetaEntity.release(var2);
         }
      }

      if (debugger != null) {
         debugger.endUpdate();
      }

   }

   public static boolean isEngineProcessing() {
      return engine != null && engine.isProcessing();
   }

   public static void RenderLast() {
      engine.renderLast();
   }

   public static void Reset() {
      initialized = false;
      engine = null;
      debugger = null;
      idToEntityMap.clear();
      EntitySimulation.reset();
      if (cacheByteBuffer != null) {
         cacheByteBuffer.clear();
      }

      cacheFile = null;
      delayedReleaseMetaEntities.clear();
   }

   public static GameEntity GetEntity(long var0) {
      synchronized(idToEntityMap) {
         return (GameEntity)idToEntityMap.get(var0);
      }
   }

   static void RegisterEntity(GameEntity var0) {
      if (var0 != null && var0.hasComponents()) {
         if (var0.componentSize() != 1 || !var0.hasComponent(ComponentType.Script)) {
            if (!GameServer.bServer && !GameClient.bClient) {
               if (VERBOSE) {
                  DebugLogStream var10000 = DebugLog.Entity;
                  long var10001 = var0.getEntityNetID();
                  var10000.println("Registering entity id = " + var10001 + " [type:" + var0.getGameEntityType() + ", name:" + var0.getEntityFullTypeDebug() + ", comps: " + var0.componentSize() + "]");
               }

               boolean var12 = false;
               long var2 = var0.getEntityNetID();
               if (var0 instanceof IsoObject && var0.hasComponent(ComponentType.MetaTag)) {
                  MetaTagComponent var4 = (MetaTagComponent)var0.removeComponent(ComponentType.MetaTag);
                  var2 = var4.getStoredID();
                  var12 = true;
               }

               GameEntity var13;
               synchronized(idToEntityMap) {
                  var13 = (GameEntity)idToEntityMap.get(var2);
               }

               if (var12) {
                  if (!(var13 instanceof MetaEntity)) {
                     return;
                  }

                  if (VERBOSE) {
                     DebugLog.Entity.println("IsoObject Entity respawn - " + var0.getEntityNetID() + " loading from MetaEntity...");
                  }

                  MetaEntity var5 = (MetaEntity)var13;

                  for(int var7 = var5.componentSize() - 1; var7 >= 0; --var7) {
                     Component var6 = var5.getComponentForIndex(var7);
                     var5.removeComponent(var6);
                     if (var0.hasComponent(var6.getComponentType())) {
                        var0.releaseComponent(var6.getComponentType());
                     }

                     var0.addComponent(var6);
                  }

                  var0.connectComponents();
                  UnregisterEntity(var13);
               } else if (var13 != null) {
                  return;
               }

               engine.addEntity(var0);
               long var14 = var0.getEntityNetID();
               synchronized(idToEntityMap) {
                  idToEntityMap.put(var14, var0);
               }

               var0.addedToEntityManager = true;
            } else {
               long var1 = var0.getEntityNetID();
               if (var1 == -1L) {
                  throw new RuntimeException("getEntityNetID returned -1");
               } else {
                  synchronized(idToEntityMap) {
                     idToEntityMap.put(var1, var0);
                  }

                  var0.addedToEntityManager = true;
                  var0.addedToEngine = true;
               }
            }
         }
      }
   }

   static void UnregisterEntity(GameEntity var0) {
      UnregisterEntity(var0, false);
   }

   static void UnregisterEntity(GameEntity var0, boolean var1) {
      if (var0 != null && var0.addedToEntityManager) {
         if (!GameServer.bServer && !GameClient.bClient && !bWasMultiplayer) {
            if (VERBOSE) {
               DebugLogStream var10000 = DebugLog.Entity;
               long var10001 = var0.getEntityNetID();
               var10000.println("Unregistering entity id = " + var10001 + " [type:" + var0.getGameEntityType() + ", name:" + var0.getEntityFullTypeDebug() + ", tryOffloadToMeta:" + var1 + "]");
            }

            long var3 = var0.getEntityNetID();
            GameEntity var11;
            synchronized(idToEntityMap) {
               var11 = (GameEntity)idToEntityMap.remove(var3);
            }

            if (var11 != null) {
               if (var11 != var0) {
                  throw new RuntimeException("Stored entity mismatch");
               } else {
                  engine.removeEntity(var0);
                  if (var1 && var0 instanceof IsoObject && ComponentType.bitsRunInMeta.intersects(var0.getComponentBits())) {
                     if (VERBOSE) {
                        DebugLog.Entity.println("IsoObject Entity despawn - " + var0.getEntityNetID() + " saving to MetaEntity...");
                     }

                     boolean var5 = false;

                     Component var6;
                     for(int var7 = var0.componentSize() - 1; var7 >= 0; --var7) {
                        var6 = var0.getComponentForIndex(var7);
                        if (var6.getComponentType().isRunInMeta() && var6.isQualifiesForMetaStorage()) {
                           var5 = true;
                           break;
                        }
                     }

                     if (!var5) {
                        if (VERBOSE) {
                           DebugLog.Entity.println("IsoObject Entity despawn - ignoring meta storage for entity: " + var0.getEntityNetID() + ", no components actually require meta updating right now...");
                        }
                     } else {
                        MetaEntity var12 = MetaEntity.alloc(var0);

                        for(int var8 = var0.componentSize() - 1; var8 >= 0; --var8) {
                           var6 = var0.getComponentForIndex(var8);
                           var0.removeComponent(var6);
                           var12.addComponent(var6);
                        }

                        var12.connectComponents();
                        RegisterEntity(var12);
                        MetaTagComponent var13 = (MetaTagComponent)ComponentType.MetaTag.CreateComponent();
                        var13.setStoredID(var12.getEntityNetID());
                        var0.addComponent(var13);
                     }
                  }

                  var0.addedToEntityManager = false;
                  if (var0 instanceof MetaEntity) {
                     if (!var0.addedToEngine && !var0.scheduledForEngineRemoval && !var0.removingFromEngine) {
                        MetaEntity.release((MetaEntity)var0);
                     } else {
                        delayedReleaseMetaEntities.add((MetaEntity)var0);
                     }
                  }

               }
            }
         } else {
            long var2 = var0.getEntityNetID();
            synchronized(idToEntityMap) {
               idToEntityMap.remove(var2);
            }

            var0.addedToEntityManager = false;
            var0.addedToEngine = false;
         }
      }
   }

   static void onEntityAddedToEngine(GameEntity var0) {
   }

   static void onEntityRemovedFromEngine(GameEntity var0) {
   }

   public static void checkEntityIDChange(GameEntity var0, long var1, long var3) {
      if (var0 != null) {
         if (var1 != -1L) {
            if (var1 != var3) {
               synchronized(idToEntityMap) {
                  GameEntity var6 = (GameEntity)idToEntityMap.get(var1);
                  if (var6 == var0) {
                     idToEntityMap.remove(var1);
                     idToEntityMap.put(var3, var0);
                  } else {
                     boolean var7 = true;
                  }

               }
            }
         }
      }
   }

   public static ByteBuffer ensureCapacity(ByteBuffer var0, int var1) {
      var1 = PZMath.max(var1, 1048576);
      if (var0 == null) {
         return ByteBuffer.allocate(var1 + 1048576);
      } else {
         if (var0.capacity() < var1 + 1048576) {
            ByteBuffer var2 = var0;
            var0 = ByteBuffer.allocate(var1 + 1048576);
            var0.put(var2.array(), 0, var2.position());
         }

         return var0;
      }
   }

   public static void Save() {
      if (initialized && engine != null) {
         if (!Core.getInstance().isNoSave()) {
            if (Core.bDebug) {
            }

            try {
               if (VERBOSE) {
                  DebugLog.Entity.println("Saving GameEntityManager...");
               }

               if (debugger != null) {
                  debugger.beginSave(cacheFile);
               }

               ByteBuffer var0 = ensureCapacity(cacheByteBuffer, 1048576);
               var0.clear();
               MetaEntitySystem var1 = (MetaEntitySystem)engine.getSystem(MetaEntitySystem.class);
               var0 = var1.saveMetaEntities(var0);

               try {
                  FileOutputStream var2 = new FileOutputStream(cacheFile);

                  try {
                     var2.getChannel().truncate(0L);
                     var2.write(var0.array(), 0, var0.position());
                  } catch (Throwable var6) {
                     try {
                        var2.close();
                     } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                     }

                     throw var6;
                  }

                  var2.close();
               } catch (Exception var7) {
                  ExceptionLogger.logException(var7);
                  return;
               }

               cacheByteBuffer = var0;
               if (debugger != null) {
                  debugger.endSave(cacheByteBuffer);
               }
            } catch (Exception var8) {
               ExceptionLogger.logException(var8);
            }

         }
      } else {
         throw new UnsupportedOperationException("Can not save when manager not initialized.");
      }
   }

   private static void load(int var0) {
      if (initialized && engine != null) {
         if (!Core.getInstance().isNoSave()) {
            if (Core.bDebug) {
            }

            try {
               if (VERBOSE) {
                  DebugLog.Entity.println("Loading GameEntityManager...");
               }

               if (!cacheFile.exists()) {
                  if (VERBOSE) {
                     DebugLog.Entity.println("- skipping entity loading, no file -");
                  }

                  return;
               }

               if (debugger != null) {
                  debugger.beginLoad(cacheFile);
               }

               ByteBuffer var1;
               try {
                  FileInputStream var2 = new FileInputStream(cacheFile);

                  try {
                     var1 = ensureCapacity(cacheByteBuffer, (int)cacheFile.length());
                     var1.clear();
                     int var3 = var2.read(var1.array());
                     var1.limit(PZMath.max(var3, 0));
                  } catch (Throwable var6) {
                     try {
                        var2.close();
                     } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                     }

                     throw var6;
                  }

                  var2.close();
               } catch (Exception var7) {
                  ExceptionLogger.logException(var7);
                  return;
               }

               MetaEntitySystem var9 = (MetaEntitySystem)engine.getSystem(MetaEntitySystem.class);
               var9.loadMetaEntities(var1, var0);
               cacheByteBuffer = var1;
               if (debugger != null) {
                  debugger.endLoad(cacheByteBuffer);
               }
            } catch (Exception var8) {
               ExceptionLogger.logException(var8);
            }

         }
      } else {
         throw new UnsupportedOperationException("Can not load when manager not initialized.");
      }
   }

   public static ArrayList<GameEntity> getIsoEntitiesDebug() {
      return debugger != null ? debugger.getIsoEntitiesDebug() : null;
   }

   public static void reloadDebug() throws Exception {
      if (debugger != null) {
         debugger.reloadDebug();
      }

   }

   public static void reloadDebugEntity(GameEntity var0) throws Exception {
      if (debugger != null) {
         debugger.reloadDebugEntity(var0);
      }

   }

   public static void reloadEntityFromScriptDebug(GameEntity var0) throws Exception {
      if (debugger != null) {
         debugger.reloadEntityFromScriptDebug(var0);
      }

   }

   private static class EngineEntityListener implements Engine.EntityListener {
      private EngineEntityListener() {
      }

      public void onEntityAddedToEngine(GameEntity var1) {
         GameEntityManager.onEntityAddedToEngine(var1);
      }

      public void onEntityRemovedFromEngine(GameEntity var1) {
         GameEntityManager.onEntityRemovedFromEngine(var1);
      }
   }
}
