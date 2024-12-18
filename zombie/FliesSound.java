package zombie;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;

public final class FliesSound {
   public static int maxCorpseCount = 25;
   public static final FliesSound instance = new FliesSound();
   private static final IsoGridSquare[] tempSquares = new IsoGridSquare[64];
   private final PlayerData[] playerData = new PlayerData[4];
   private final ArrayList<FadeEmitter> fadeEmitters = new ArrayList();
   private float fliesVolume = -1.0F;

   public FliesSound() {
      for(int var1 = 0; var1 < this.playerData.length; ++var1) {
         this.playerData[var1] = new PlayerData();
      }

   }

   public void Reset() {
      for(int var1 = 0; var1 < this.playerData.length; ++var1) {
         this.playerData[var1].Reset();
      }

   }

   public void update() {
      if (SandboxOptions.instance.DecayingCorpseHealthImpact.getValue() != 1) {
         int var1;
         for(var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoPlayer var2 = IsoPlayer.players[var1];
            if (var2 != null && var2.getCurrentSquare() != null) {
               this.playerData[var1].update(var2);
            }
         }

         for(var1 = 0; var1 < this.fadeEmitters.size(); ++var1) {
            FadeEmitter var3 = (FadeEmitter)this.fadeEmitters.get(var1);
            if (var3.update()) {
               this.fadeEmitters.remove(var1--);
            }
         }

      }
   }

   public void render() {
      byte var1 = 8;
      IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[0];

      for(int var3 = 0; var3 < IsoChunkMap.ChunkGridWidth; ++var3) {
         for(int var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
            IsoChunk var5 = var2.getChunk(var4, var3);
            if (var5 != null) {
               ChunkData var6 = var5.corpseData;
               if (var6 != null) {
                  int var7 = PZMath.fastfloor(IsoPlayer.players[0].getZ());
                  ChunkLevelData var8 = var6.levelData[var7 + 32];

                  for(int var9 = 0; var9 < var8.emitters.length; ++var9) {
                     FadeEmitter var10 = var8.emitters[var9];
                     if (var10 != null && var10.emitter != null) {
                        this.paintSquare(var10.sq.x, var10.sq.y, var10.sq.z, 0.0F, 1.0F, 0.0F, 1.0F);
                     }

                     if (var8.refCount[var9] > 0) {
                        this.paintSquare(var5.wx * var1 + var1 / 2, var5.wy * var1 + var1 / 2, 0, 0.0F, 0.0F, 1.0F, 1.0F);
                     }
                  }

                  IsoBuilding var11 = IsoPlayer.players[0].getCurrentBuilding();
                  if (var11 != null && var8.buildingCorpseCount != null && var8.buildingCorpseCount.containsKey(var11)) {
                     this.paintSquare(var5.wx * var1 + var1 / 2, var5.wy * var1 + var1 / 2, var7, 1.0F, 0.0F, 0.0F, 1.0F);
                  }
               }
            }
         }
      }

   }

   private void paintSquare(int var1, int var2, int var3, float var4, float var5, float var6, float var7) {
      int var8 = Core.TileScale;
      int var9 = (int)IsoUtils.XToScreenExact((float)var1, (float)(var2 + 1), (float)var3, 0);
      int var10 = (int)IsoUtils.YToScreenExact((float)var1, (float)(var2 + 1), (float)var3, 0);
      SpriteRenderer.instance.renderPoly((float)var9, (float)var10, (float)(var9 + 32 * var8), (float)(var10 - 16 * var8), (float)(var9 + 64 * var8), (float)var10, (float)(var9 + 32 * var8), (float)(var10 + 16 * var8), var4, var5, var6, var7);
   }

   public void chunkLoaded(IsoChunk var1) {
      if (var1.corpseData == null) {
         var1.corpseData = new ChunkData(var1.wx, var1.wy);
      }

      var1.corpseData.wx = var1.wx;
      var1.corpseData.wy = var1.wy;
      var1.corpseData.Reset();
   }

   public void corpseAdded(int var1, int var2, int var3) {
      if (var3 >= -32 && var3 <= 31) {
         ChunkData var4 = this.getChunkData(var1, var2);
         if (var4 != null) {
            var4.corpseAdded(var1, var2, var3);

            for(int var5 = 0; var5 < this.playerData.length; ++var5) {
               if (var4.levelData[var3 + 32].refCount[var5] > 0) {
                  this.playerData[var5].forceUpdate = true;
               }
            }

         }
      } else {
         DebugLog.General.error("invalid z-coordinate %d,%d,%d", var1, var2, var3);
      }
   }

   public void corpseRemoved(int var1, int var2, int var3) {
      if (var3 >= -32 && var3 <= 31) {
         ChunkData var4 = this.getChunkData(var1, var2);
         if (var4 != null) {
            var4.corpseRemoved(var1, var2, var3);

            for(int var5 = 0; var5 < this.playerData.length; ++var5) {
               if (var4.levelData[var3 + 32].refCount[var5] > 0) {
                  this.playerData[var5].forceUpdate = true;
               }
            }

         }
      } else {
         DebugLog.General.error("invalid z-coordinate %d,%d,%d", var1, var2, var3);
      }
   }

   public int getCorpseCount(IsoGameCharacter var1) {
      return var1 != null && var1.getCurrentSquare() != null ? this.getCorpseCount(PZMath.fastfloor(var1.getX()) / 8, PZMath.fastfloor(var1.getY()) / 8, PZMath.fastfloor(var1.getZ()), var1.getBuilding()) : 0;
   }

   private int getCorpseCount(int var1, int var2, int var3, IsoBuilding var4) {
      int var5 = 0;

      int var6;
      int var7;
      for(var6 = -1; var6 <= 1; ++var6) {
         for(var7 = -1; var7 <= 1; ++var7) {
            ChunkData var8 = this.getChunkData((var1 + var7) * 8, (var2 + var6) * 8);
            if (var8 != null) {
               ChunkLevelData var9 = var8.levelData[var3 + 32];
               if (var4 == null) {
                  var5 += var9.corpseCount;
               } else if (var9.buildingCorpseCount != null) {
                  Integer var10 = (Integer)var9.buildingCorpseCount.get(var4);
                  if (var10 != null) {
                     var5 += var10;
                  }
               }

               if (var5 >= maxCorpseCount) {
                  return var5;
               }
            }
         }
      }

      if (SandboxOptions.instance.ZombieHealthImpact.getValue()) {
         var6 = var1 * 8;
         var7 = var2 * 8;
         byte var12 = 12;

         for(int var13 = -1 * var12; var13 <= var12; ++var13) {
            for(int var14 = -1 * var12; var14 <= var12; ++var14) {
               IsoGridSquare var11 = IsoWorld.instance.getCell().getGridSquare(var6 + var14, var7 + var13, var3);
               if (var11 != null) {
                  if (var4 == null && var11.getBuilding() == null) {
                     var5 += var11.getZombieCount();
                  } else if (var4 == var11.getBuilding()) {
                     var5 += var11.getZombieCount();
                  }

                  if (var5 >= maxCorpseCount) {
                     return var5;
                  }
               }
            }
         }
      }

      return var5;
   }

   private ChunkData getChunkData(int var1, int var2) {
      IsoChunk var3 = IsoWorld.instance.CurrentCell.getChunkForGridSquare(var1, var2, 0);
      return var3 != null ? var3.corpseData : null;
   }

   private class PlayerData {
      int wx = -1;
      int wy = -1;
      int z = -1;
      IsoBuilding building = null;
      boolean forceUpdate = false;

      PlayerData() {
      }

      boolean isSameLocation(IsoPlayer var1) {
         IsoGridSquare var2 = var1.getCurrentSquare();
         if (var2 != null && var2.getBuilding() != this.building) {
            return false;
         } else {
            return PZMath.fastfloor(var1.getX()) / 8 == this.wx && PZMath.fastfloor(var1.getY()) / 8 == this.wy && PZMath.fastfloor(var1.getZ()) == this.z;
         }
      }

      void update(IsoPlayer var1) {
         if (this.forceUpdate || !this.isSameLocation(var1)) {
            this.forceUpdate = false;
            int var2 = this.wx;
            int var3 = this.wy;
            int var4 = this.z;
            IsoGridSquare var5 = var1.getCurrentSquare();
            this.wx = var5.getX() / 8;
            this.wy = var5.getY() / 8;
            this.z = var5.getZ();
            this.building = var5.getBuilding();

            int var6;
            int var7;
            ChunkData var8;
            ChunkLevelData var9;
            for(var6 = -1; var6 <= 1; ++var6) {
               for(var7 = -1; var7 <= 1; ++var7) {
                  var8 = FliesSound.this.getChunkData((this.wx + var7) * 8, (this.wy + var6) * 8);
                  if (var8 != null) {
                     var9 = var8.levelData[this.z + 32];
                     var9.update(this.wx + var7, this.wy + var6, this.z, var1);
                  }
               }
            }

            if (var4 != -1) {
               for(var6 = -1; var6 <= 1; ++var6) {
                  for(var7 = -1; var7 <= 1; ++var7) {
                     var8 = FliesSound.this.getChunkData((var2 + var7) * 8, (var3 + var6) * 8);
                     if (var8 != null) {
                        var9 = var8.levelData[var4 + 32];
                        var9.deref(var1);
                     }
                  }
               }

            }
         }
      }

      void Reset() {
         this.wx = this.wy = this.z = -1;
         this.building = null;
         this.forceUpdate = false;
      }
   }

   private class FadeEmitter {
      private static final float FADE_IN_RATE = 0.01F;
      private static final float FADE_OUT_RATE = -0.01F;
      BaseSoundEmitter emitter = null;
      float volume = 1.0F;
      float targetVolume = 1.0F;
      IsoGridSquare sq = null;

      private FadeEmitter() {
      }

      boolean update() {
         if (this.emitter == null) {
            return true;
         } else {
            if (this.volume < this.targetVolume) {
               this.volume += 0.01F * GameTime.getInstance().getThirtyFPSMultiplier();
               if (this.volume >= this.targetVolume) {
                  this.volume = this.targetVolume;
                  return true;
               }
            } else {
               this.volume += -0.01F * GameTime.getInstance().getThirtyFPSMultiplier();
               if (this.volume <= 0.0F) {
                  this.volume = 0.0F;
                  this.emitter.stopAll();
                  this.emitter = null;
                  return true;
               }
            }

            this.emitter.setVolumeAll(this.volume);
            return false;
         }
      }

      void Reset() {
         this.emitter = null;
         this.volume = 1.0F;
         this.targetVolume = 1.0F;
         this.sq = null;
      }
   }

   public class ChunkData {
      private int wx;
      private int wy;
      private final ChunkLevelData[] levelData = new ChunkLevelData[64];

      private ChunkData(int var2, int var3) {
         this.wx = var2;
         this.wy = var3;

         for(int var4 = 0; var4 < this.levelData.length; ++var4) {
            this.levelData[var4] = FliesSound.this.new ChunkLevelData();
         }

      }

      private void corpseAdded(int var1, int var2, int var3) {
         IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
         IsoBuilding var5 = var4 == null ? null : var4.getBuilding();
         int var6 = var1 - this.wx * 8;
         int var7 = var2 - this.wy * 8;
         this.levelData[var3 + 32].corpseAdded(var6, var7, var5);
      }

      private void corpseRemoved(int var1, int var2, int var3) {
         IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
         IsoBuilding var5 = var4 == null ? null : var4.getBuilding();
         int var6 = var1 - this.wx * 8;
         int var7 = var2 - this.wy * 8;
         this.levelData[var3 + 32].corpseRemoved(var6, var7, var5);
      }

      private void Reset() {
         for(int var1 = 0; var1 < this.levelData.length; ++var1) {
            this.levelData[var1].Reset();
         }

      }
   }

   private class ChunkLevelData {
      int corpseCount = 0;
      HashMap<IsoBuilding, Integer> buildingCorpseCount = null;
      final int[] refCount = new int[4];
      final FadeEmitter[] emitters = new FadeEmitter[4];

      ChunkLevelData() {
      }

      void corpseAdded(int var1, int var2, IsoBuilding var3) {
         if (var3 == null) {
            ++this.corpseCount;
         } else {
            if (this.buildingCorpseCount == null) {
               this.buildingCorpseCount = new HashMap();
            }

            Integer var4 = (Integer)this.buildingCorpseCount.get(var3);
            if (var4 == null) {
               this.buildingCorpseCount.put(var3, 1);
            } else {
               this.buildingCorpseCount.put(var3, var4 + 1);
            }
         }

      }

      void corpseRemoved(int var1, int var2, IsoBuilding var3) {
         if (var3 == null) {
            --this.corpseCount;
         } else if (this.buildingCorpseCount != null) {
            Integer var4 = (Integer)this.buildingCorpseCount.get(var3);
            if (var4 != null) {
               if (var4 > 1) {
                  this.buildingCorpseCount.put(var3, var4 - 1);
               } else {
                  this.buildingCorpseCount.remove(var3);
               }
            }
         }

      }

      IsoGridSquare calcSoundPos(int var1, int var2, int var3, IsoBuilding var4) {
         byte var5 = 8;
         IsoChunk var6 = IsoWorld.instance.CurrentCell.getChunkForGridSquare(var1 * var5, var2 * var5, var3);
         if (var6 == null) {
            return null;
         } else {
            int var7 = 0;

            for(int var8 = 0; var8 < var5; ++var8) {
               for(int var9 = 0; var9 < var5; ++var9) {
                  IsoGridSquare var10 = var6.getGridSquare(var9, var8, var3);
                  if (var10 != null && !var10.getStaticMovingObjects().isEmpty() && var10.getBuilding() == var4) {
                     FliesSound.tempSquares[var7++] = var10;
                  }
               }
            }

            if (var7 > 0) {
               return FliesSound.tempSquares[var7 / 2];
            } else {
               return null;
            }
         }
      }

      void update(int var1, int var2, int var3, IsoPlayer var4) {
         int var10002 = this.refCount[var4.PlayerIndex]++;
         int var5 = FliesSound.this.getCorpseCount(var1, var2, var3, var4.getCurrentBuilding());
         if ((double)BodyDamage.getSicknessFromCorpsesRate(var5) > ZomboidGlobals.FoodSicknessDecrease) {
            IsoBuilding var6 = var4.getCurrentBuilding();
            IsoGridSquare var7 = this.calcSoundPos(var1, var2, var3, var6);
            if (var7 == null) {
               return;
            }

            if (this.emitters[var4.PlayerIndex] == null) {
               this.emitters[var4.PlayerIndex] = FliesSound.this.new FadeEmitter();
            }

            FadeEmitter var8 = this.emitters[var4.PlayerIndex];
            if (var8.emitter == null) {
               var8.emitter = IsoWorld.instance.getFreeEmitter((float)var7.x, (float)var7.y, (float)var3);
               var8.emitter.playSoundLoopedImpl("CorpseFlies");
               var8.emitter.setVolumeAll(0.0F);
               var8.volume = 0.0F;
               FliesSound.this.fadeEmitters.add(var8);
            } else {
               var8.sq.setHasFlies(false);
               var8.emitter.setPos((float)var7.x, (float)var7.y, (float)var3);
               if (var8.targetVolume != 1.0F && !FliesSound.this.fadeEmitters.contains(var8)) {
                  FliesSound.this.fadeEmitters.add(var8);
               }
            }

            var8.targetVolume = 1.0F;
            var8.sq = var7;
            var7.setHasFlies(true);
         } else {
            FadeEmitter var9 = this.emitters[var4.PlayerIndex];
            if (var9 != null && var9.emitter != null) {
               if (!FliesSound.this.fadeEmitters.contains(var9)) {
                  FliesSound.this.fadeEmitters.add(var9);
               }

               var9.targetVolume = 0.0F;
               var9.sq.setHasFlies(false);
            }
         }

      }

      void deref(IsoPlayer var1) {
         int var2 = var1.PlayerIndex;
         int var10002 = this.refCount[var2]--;
         if (this.refCount[var2] <= 0) {
            if (this.emitters[var2] != null && this.emitters[var2].emitter != null) {
               if (!FliesSound.this.fadeEmitters.contains(this.emitters[var2])) {
                  FliesSound.this.fadeEmitters.add(this.emitters[var2]);
               }

               this.emitters[var2].targetVolume = 0.0F;
               this.emitters[var2].sq.setHasFlies(false);
            }

         }
      }

      void Reset() {
         this.corpseCount = 0;
         if (this.buildingCorpseCount != null) {
            this.buildingCorpseCount.clear();
         }

         for(int var1 = 0; var1 < 4; ++var1) {
            this.refCount[var1] = 0;
            if (this.emitters[var1] != null) {
               this.emitters[var1].Reset();
            }
         }

      }
   }
}
