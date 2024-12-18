package zombie.audio;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import zombie.AmbientStreamManager;
import zombie.GameSounds;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.popman.ObjectPool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class FMODAmbientWalls {
   public static boolean ENABLE = true;
   private static FMODAmbientWalls instance;
   private final Vector2 tempVector2 = new Vector2();
   private final ObjectPool<ObjectWithDistance> m_objectPool = new ObjectPool(ObjectWithDistance::new);
   private final ArrayList<ObjectWithDistance> m_objects = new ArrayList();
   private final Slot[] m_slots;
   private final ObjectPool<ObjectAmbientEmitters.DoorLogic> m_DoorLogicPool = new ObjectPool(ObjectAmbientEmitters.DoorLogic::new);
   private final ObjectPool<ObjectAmbientEmitters.WindowLogic> m_WindowLogicPool = new ObjectPool(ObjectAmbientEmitters.WindowLogic::new);
   private final ObjectPool<OpenWallLogic> m_OpenWallLogicPool = new ObjectPool(OpenWallLogic::new);
   private final HashMap<String, Integer> m_instanceCounts = new HashMap();
   private final Comparator<ObjectWithDistance> comp = (var0, var1x) -> {
      return Float.compare(var0.distSq, var1x.distSq);
   };

   public static FMODAmbientWalls getInstance() {
      if (instance == null) {
         instance = new FMODAmbientWalls();
      }

      return instance;
   }

   private FMODAmbientWalls() {
      byte var1 = 16;
      this.m_slots = (Slot[])PZArrayUtil.newInstance(Slot.class, var1, Slot::new);
   }

   public void update() {
      if (ENABLE) {
         this.addObjectsFromChunks();
      }

      this.updateEmitters();
   }

   void addObjectsFromChunks() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
         if (!var2.ignore) {
            IsoPlayer var3 = IsoPlayer.players[var1];
            if (var3 != null) {
               int var4 = IsoChunkMap.ChunkGridWidth / 2;
               int var5 = IsoChunkMap.ChunkGridWidth / 2;

               for(int var6 = -1; var6 <= 1; ++var6) {
                  for(int var7 = -1; var7 <= 1; ++var7) {
                     this.addObjectsFromChunkLevel(var1, var4 + var7, var5 + var6);
                  }
               }
            }
         }
      }

   }

   private void addObjectsFromChunkLevel(int var1, int var2, int var3) {
      IsoChunkMap var4 = IsoWorld.instance.getCell().getChunkMap(var1);
      IsoChunk var5 = var4.getChunk(var2, var3);
      if (var5 != null) {
         int var6 = PZMath.fastfloor(IsoPlayer.players[var1].getZ());
         IsoChunkLevel var7 = var5.getLevelData(var6);
         if (var7 != null) {
            FMODAmbientWallLevelData var8 = var7.m_fmodAmbientWallLevelData;
            if (var8 == null) {
               var8 = var7.m_fmodAmbientWallLevelData = FMODAmbientWallLevelData.alloc().init(var7);
            }

            var8.checkDirty();

            for(int var9 = 0; var9 < var8.m_walls.size(); ++var9) {
               FMODAmbientWallLevelData.FMODAmbientWall var10 = (FMODAmbientWallLevelData.FMODAmbientWall)var8.m_walls.get(var9);
               this.addObjects(var10);
            }

         }
      }
   }

   private void addObjects(FMODAmbientWallLevelData.FMODAmbientWall var1) {
      int var2 = var1.owner.m_chunkLevel.getLevel();
      int var3;
      IsoGridSquare var4;
      ObjectWithDistance var5;
      if (var1.isHorizontal()) {
         for(var3 = var1.x1; var3 < var1.x2; ++var3) {
            var4 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var1.y1, var2);
            if (var4 != null) {
               var5 = (ObjectWithDistance)this.m_objectPool.alloc();
               var5.x = var4.x;
               var5.y = var4.y;
               var5.z = var4.z;
               var5.bNorth = true;
               var5.setObjectAndLogic(var4);
               this.m_objects.add(var5);
            }
         }
      } else {
         for(var3 = var1.y1; var3 < var1.y2; ++var3) {
            var4 = IsoWorld.instance.CurrentCell.getGridSquare(var1.x1, var3, var2);
            if (var4 != null) {
               var5 = (ObjectWithDistance)this.m_objectPool.alloc();
               var5.x = var4.x;
               var5.y = var4.y;
               var5.z = var4.z;
               var5.bNorth = false;
               var5.setObjectAndLogic(var4);
               this.m_objects.add(var5);
            }
         }
      }

   }

   private void updateEmitters() {
      int var1;
      for(var1 = 0; var1 < this.m_slots.length; ++var1) {
         this.m_slots[var1].playing = false;
      }

      if (this.m_objects.isEmpty()) {
         this.stopNotPlaying();
      } else {
         ObjectWithDistance var2;
         for(var1 = 0; var1 < this.m_objects.size(); ++var1) {
            var2 = (ObjectWithDistance)this.m_objects.get(var1);
            var2.getEmitterPosition(this.tempVector2);
            var2.distSq = this.getClosestListener(this.tempVector2.x, this.tempVector2.y, (float)var2.z);
            if (var2.distSq > 64.0F || !var2.shouldPlay()) {
               this.m_objects.remove(var1--);
               this.m_objectPool.release((Object)var2);
            }
         }

         this.m_objects.sort(this.comp);
         this.m_instanceCounts.clear();

         for(var1 = 0; var1 < this.m_objects.size(); ++var1) {
            var2 = (ObjectWithDistance)this.m_objects.get(var1);
            if (var2.logic != null) {
               String var3 = var2.logic.getSoundName();
               GameSound var4 = GameSounds.getSound(var3);
               if (var4 != null && !var4.clips.isEmpty()) {
                  GameSoundClip var5 = (GameSoundClip)var4.clips.get(0);
                  if (!StringUtils.isNullOrWhitespace(var5.event)) {
                     var3 = var5.event;
                  }
               }

               int var9 = (Integer)this.m_instanceCounts.getOrDefault(var3, 0);
               if (var9 >= 3) {
                  this.m_objects.remove(var1--);
                  this.m_objectPool.release((Object)var2);
               } else {
                  this.m_instanceCounts.put(var3, var9 + 1);
               }
            }
         }

         var1 = Math.min(this.m_objects.size(), this.m_slots.length);

         int var6;
         ObjectWithDistance var7;
         int var8;
         Slot var10;
         for(var6 = 0; var6 < var1; ++var6) {
            var7 = (ObjectWithDistance)this.m_objects.get(var6);
            var8 = this.getExistingSlot(var7);
            if (var8 != -1) {
               var10 = this.m_slots[var8];
               this.m_objects.remove(var6--);
               --var1;
               if (var10.owd != null) {
                  this.m_objectPool.release((Object)var10.owd);
               }

               var10.playSound(var7);
            }
         }

         for(var6 = 0; var6 < var1; ++var6) {
            var7 = (ObjectWithDistance)this.m_objects.get(var6);
            var8 = this.getExistingSlot(var7);
            if (var8 == -1) {
               var8 = this.getFreeSlot();
               var10 = this.m_slots[var8];
               if (var10.owd != null) {
                  if (var10.emitter != null && !var10.emitter.isPlaying(var7.logic.getSoundName())) {
                     var10.stopPlaying();
                  }

                  this.m_objectPool.release((Object)var10.owd);
                  var10.owd = null;
               }

               this.m_objects.remove(var6--);
               --var1;
               var10.playSound(var7);
            }
         }

         this.stopNotPlaying();
         this.m_objectPool.releaseAll(this.m_objects);
         this.m_objects.clear();
      }
   }

   float getClosestListener(float var1, float var2, float var3) {
      float var4 = 3.4028235E38F;

      for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
         IsoPlayer var6 = IsoPlayer.players[var5];
         if (var6 != null && var6.getCurrentSquare() != null) {
            float var7 = var6.getX();
            float var8 = var6.getY();
            float var9 = var6.getZ();
            float var10 = IsoUtils.DistanceToSquared(var7, var8, var9 * 3.0F, var1, var2, var3 * 3.0F);
            var10 *= PZMath.pow(var6.getHearDistanceModifier(), 2.0F);
            if (var10 < var4) {
               var4 = var10;
            }
         }
      }

      return var4;
   }

   int getExistingSlot(ObjectWithDistance var1) {
      for(int var2 = 0; var2 < this.m_slots.length; ++var2) {
         ObjectWithDistance var3 = this.m_slots[var2].owd;
         if (var3 != null && var1.x == var3.x && var1.y == var3.y && var1.z == var3.z && var1.bNorth == var3.bNorth) {
            return var2;
         }
      }

      return -1;
   }

   int getFreeSlot() {
      for(int var1 = 0; var1 < this.m_slots.length; ++var1) {
         if (!this.m_slots[var1].playing) {
            return var1;
         }
      }

      return -1;
   }

   void stopNotPlaying() {
      for(int var1 = 0; var1 < this.m_slots.length; ++var1) {
         Slot var2 = this.m_slots[var1];
         if (!var2.playing) {
            var2.stopPlaying();
            var2.owd = null;
         }
      }

   }

   public void squareChanged(IsoGridSquare var1) {
      if (var1 != null && var1.getChunk() != null) {
         IsoChunkLevel var2 = var1.getChunk().getLevelData(var1.getZ());
         if (var2 != null && var2.m_fmodAmbientWallLevelData != null) {
            var2.m_fmodAmbientWallLevelData.bDirty = true;
         }
      }

   }

   public void render() {
      if (DebugOptions.instance.AmbientWallEmittersRender.getValue()) {
         int var6;
         int var7;
         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
            if (!var2.ignore) {
               IsoPlayer var3 = IsoPlayer.players[var1];
               if (var3 != null) {
                  int var4 = PZMath.fastfloor(var3.getZ());
                  int var5 = IsoChunkMap.ChunkGridWidth / 2;
                  var6 = IsoChunkMap.ChunkGridWidth / 2;

                  for(var7 = -1; var7 <= 1; ++var7) {
                     for(int var8 = -1; var8 <= 1; ++var8) {
                        IsoChunk var9 = var2.getChunk(var5 + var8, var6 + var7);
                        if (var9 != null) {
                           IsoChunkLevel var10 = var9.getLevelData(var4);
                           if (var10 != null) {
                              FMODAmbientWallLevelData var11 = var10.m_fmodAmbientWallLevelData;
                              if (var11 != null) {
                                 for(int var12 = 0; var12 < var11.m_walls.size(); ++var12) {
                                    FMODAmbientWallLevelData.FMODAmbientWall var13 = (FMODAmbientWallLevelData.FMODAmbientWall)var11.m_walls.get(var12);
                                    LineDrawer.addLine((float)var13.x1, (float)var13.y1, (float)var4, (float)var13.x2, (float)var13.y2, (float)var4, 1.0F, 0.0F, 0.0F, 1.0F);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         Slot[] var14 = this.m_slots;
         int var15 = var14.length;

         for(int var16 = 0; var16 < var15; ++var16) {
            Slot var17 = var14[var16];
            if (var17.owd != null) {
               ObjectWithDistance var18 = var17.owd;
               var6 = var18.bNorth ? 1 : 0;
               var7 = 1 - var6;
               LineDrawer.addLine((float)var18.x, (float)var18.y, (float)var18.z, (float)(var18.x + var6), (float)(var18.y + var7), (float)var18.z, 0.0F, 1.0F, 0.0F, 1.0F);
            }
         }

      }
   }

   static final class Slot {
      ObjectWithDistance owd;
      BaseSoundEmitter emitter = null;
      long instance = 0L;
      boolean playing = false;

      Slot() {
      }

      void playSound(ObjectWithDistance var1) {
         this.owd = var1;
         ObjectAmbientEmitters.PerObjectLogic var2 = var1.logic;
         if (this.emitter == null) {
            this.emitter = (BaseSoundEmitter)(Core.SoundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
         }

         Vector2 var3 = var1.getEmitterPosition(FMODAmbientWalls.getInstance().tempVector2);
         this.emitter.setPos(var3.getX(), var3.getY(), (float)var1.z);
         String var4 = var2.getSoundName();
         if (!this.emitter.isPlaying(var4)) {
            this.emitter.stopAll();
            FMODSoundEmitter var5 = (FMODSoundEmitter)Type.tryCastTo(this.emitter, FMODSoundEmitter.class);
            if (var5 != null) {
               var5.clearParameters();
            }

            this.instance = this.emitter.playSoundImpl(var4, (IsoObject)null);
            var2.startPlaying(this.emitter, this.instance);
         }

         var2.checkParameters(this.emitter, this.instance);
         this.playing = true;
         this.emitter.tick();
      }

      void stopPlaying() {
         if (this.emitter != null && this.instance != 0L) {
            ObjectAmbientEmitters.PerObjectLogic var1 = this.owd.logic;
            var1.stopPlaying(this.emitter, this.instance);
            if (this.emitter.hasSustainPoints(this.instance)) {
               this.emitter.triggerCue(this.instance);
               this.instance = 0L;
            } else {
               this.emitter.stopAll();
               this.instance = 0L;
            }
         }
      }
   }

   static final class ObjectWithDistance {
      int x;
      int y;
      int z;
      boolean bNorth;
      IsoObject object;
      ObjectAmbientEmitters.PerObjectLogic logic;
      float distSq;

      ObjectWithDistance() {
      }

      Vector2 getEmitterPosition(Vector2 var1) {
         IsoObject var3 = this.object;
         if (var3 instanceof IsoDoor var4) {
            return var4.getFacingPosition(var1);
         } else {
            var3 = this.object;
            if (var3 instanceof IsoWindow var2) {
               return var2.getFacingPosition(var1);
            } else {
               return this.bNorth ? var1.set((float)this.x + 0.5F, (float)this.y) : var1.set((float)this.x, (float)this.y + 0.5F);
            }
         }
      }

      void setObjectAndLogic(IsoGridSquare var1) {
         this.release();
         this.object = var1.getDoor(this.bNorth);
         if (this.object != null) {
            this.logic = ((ObjectAmbientEmitters.DoorLogic)FMODAmbientWalls.getInstance().m_DoorLogicPool.alloc()).init(this.object);
         } else {
            this.object = var1.getWindow(this.bNorth);
            if (this.object != null) {
               this.logic = ((ObjectAmbientEmitters.WindowLogic)FMODAmbientWalls.getInstance().m_WindowLogicPool.alloc()).init(this.object);
            } else {
               this.logic = ((OpenWallLogic)FMODAmbientWalls.getInstance().m_OpenWallLogicPool.alloc()).init(this.object);
            }
         }
      }

      boolean shouldPlay() {
         return this.object != null && this.object.getObjectIndex() == -1 ? false : this.logic.shouldPlaySound();
      }

      void release() {
         ObjectAmbientEmitters.PerObjectLogic var4 = this.logic;
         if (var4 instanceof ObjectAmbientEmitters.DoorLogic var1) {
            FMODAmbientWalls.getInstance().m_DoorLogicPool.release((Object)var1);
         } else {
            var4 = this.logic;
            if (var4 instanceof ObjectAmbientEmitters.WindowLogic var2) {
               FMODAmbientWalls.getInstance().m_WindowLogicPool.release((Object)var2);
            } else {
               var4 = this.logic;
               if (var4 instanceof OpenWallLogic var3) {
                  FMODAmbientWalls.getInstance().m_OpenWallLogicPool.release((Object)var3);
               }
            }
         }

         this.object = null;
         this.logic = null;
      }
   }

   public static final class OpenWallLogic extends ObjectAmbientEmitters.PerObjectLogic {
      public OpenWallLogic() {
      }

      public boolean shouldPlaySound() {
         return AmbientStreamManager.instance.isParameterInsideTrue();
      }

      public String getSoundName() {
         return "OpenWallAmbience";
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         this.parameterValue1 = 0.0F / 0.0F;
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
         this.setParameterValue1(var1, var2, "DoorWindowOpen", 1.0F);
      }
   }
}
