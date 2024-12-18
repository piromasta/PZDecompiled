package zombie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.MPDebugInfo;
import zombie.popman.ZombiePopulationManager;

public final class WorldSoundManager {
   public static final WorldSoundManager instance = new WorldSoundManager();
   public final ArrayList<WorldSound> SoundList = new ArrayList();
   private final Stack<WorldSound> freeSounds = new Stack();
   private static final ResultBiggestSound resultBiggestSound = new ResultBiggestSound();

   public WorldSoundManager() {
   }

   public void init(IsoCell var1) {
   }

   public void initFrame() {
   }

   public void KillCell() {
      WorldSound var2;
      for(Iterator var1 = this.SoundList.iterator(); var1.hasNext(); var2.source = null) {
         var2 = (WorldSound)var1.next();
      }

      this.freeSounds.addAll(this.SoundList);
      this.SoundList.clear();
   }

   public WorldSound getNew() {
      return this.freeSounds.isEmpty() ? new WorldSound() : (WorldSound)this.freeSounds.pop();
   }

   public WorldSound addSound(Object var1, int var2, int var3, int var4, int var5, int var6) {
      return this.addSound(var1, var2, var3, var4, var5, var6, false, 0.0F, 1.0F);
   }

   public WorldSound addSound(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7) {
      return this.addSound(var1, var2, var3, var4, var5, var6, var7, 0.0F, 1.0F);
   }

   public WorldSound addSound(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9) {
      return this.addSound(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, true, false, false);
   }

   public WorldSound addSoundRepeating(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9) {
      return this.addSound(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, true, false, true);
   }

   public WorldSound addSound(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9, boolean var10, boolean var11, boolean var12) {
      return this.addSound(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, false);
   }

   public WorldSound addSound(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9, boolean var10, boolean var11, boolean var12, boolean var13) {
      if (var5 <= 0) {
         return null;
      } else {
         WorldSound var14;
         synchronized(this.SoundList) {
            var14 = this.getNew().init(var1, var2, var3, var4, var5, var6, var7, var8, var9);
            var14.bRepeating = var13;
            if (var1 == null) {
               var14.sourceIsZombie = var10;
            }

            if (!GameServer.bServer) {
               int var16 = SandboxOptions.instance.Lore.Hearing.getValue();
               if (var16 == 4) {
                  var16 = 1;
               }

               if (var16 == 5) {
                  var16 = 2;
               }

               int var17 = (int)PZMath.ceil((float)var5 * this.getHearingMultiplier(var16));
               int var18 = (var2 - var17) / 8;
               int var19 = (var3 - var17) / 8;
               int var20 = (int)Math.ceil((double)(((float)var2 + (float)var17) / 8.0F));
               int var21 = (int)Math.ceil((double)(((float)var3 + (float)var17) / 8.0F));

               for(int var22 = var18; var22 < var20; ++var22) {
                  for(int var23 = var19; var23 < var21; ++var23) {
                     IsoChunk var24 = IsoWorld.instance.CurrentCell.getChunk(var22, var23);
                     if (var24 != null) {
                        var24.SoundList.add(var14);
                     }
                  }
               }
            }

            this.SoundList.add(var14);
            ZombiePopulationManager.instance.addWorldSound(var14, var11);
         }

         if (var11) {
            if (GameClient.bClient) {
               GameClient.instance.sendWorldSound(var14);
            } else if (GameServer.bServer) {
               GameServer.sendWorldSound(var14, (UdpConnection)null);
            }
         }

         if (Core.bDebug && GameClient.bClient) {
            MPDebugInfo.AddDebugSound(var14);
         }

         return var14;
      }
   }

   public WorldSound addSoundRepeating(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7) {
      WorldSound var8 = this.addSoundRepeating(var1, var2, var3, var4, var5, var6, var7, 0.0F, 1.0F);
      return var8;
   }

   public WorldSound getSoundZomb(IsoZombie var1) {
      IsoChunk var2 = null;
      if (var1.soundSourceTarget == null) {
         return null;
      } else if (var1.getCurrentSquare() == null) {
         return null;
      } else {
         var2 = var1.getCurrentSquare().chunk;
         ArrayList var3 = null;
         if (var2 != null && !GameServer.bServer) {
            var3 = var2.SoundList;
         } else {
            var3 = this.SoundList;
         }

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            WorldSound var5 = (WorldSound)var3.get(var4);
            if (var1.soundSourceTarget == var5.source) {
               return var5;
            }
         }

         return null;
      }
   }

   public WorldSound getSoundAnimal(IsoAnimal var1) {
      IsoChunk var2 = null;
      if (var1.getCurrentSquare() == null) {
         return null;
      } else {
         var2 = var1.getCurrentSquare().chunk;
         ArrayList var3 = null;
         if (var2 != null && !GameServer.bServer) {
            var3 = var2.SoundList;
         } else {
            var3 = this.SoundList;
         }

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            WorldSound var5 = (WorldSound)var3.get(var4);
            if (var5.stresshumans || var5.stressAnimals) {
               return var5;
            }
         }

         return null;
      }
   }

   public ResultBiggestSound getBiggestSoundZomb(int var1, int var2, int var3, boolean var4, IsoZombie var5) {
      float var6 = -1000000.0F;
      WorldSound var7 = null;
      IsoChunk var8 = null;
      if (var5 != null) {
         if (var5.getCurrentSquare() == null) {
            return resultBiggestSound.init((WorldSound)null, 0.0F);
         }

         var8 = var5.getCurrentSquare().chunk;
      }

      ArrayList var9 = null;
      if (var8 != null && !GameServer.bServer) {
         var9 = var8.SoundList;
      } else {
         var9 = this.SoundList;
      }

      for(int var10 = 0; var10 < var9.size(); ++var10) {
         WorldSound var11 = (WorldSound)var9.get(var10);
         if (var11 != null && var11.radius != 0) {
            float var12 = IsoUtils.DistanceToSquared((float)var1, (float)var2, (float)(var3 * 3), (float)var11.x, (float)var11.y, (float)(var11.z * 3));
            float var13 = (float)var11.radius * this.getHearingMultiplier(var5);
            if (!(var12 > var13 * var13) && (!(var12 < var11.zombieIgnoreDist * var11.zombieIgnoreDist) || var3 != var11.z) && (!var4 || !var11.sourceIsZombie)) {
               IsoGridSquare var14 = IsoWorld.instance.CurrentCell.getGridSquare(var11.x, var11.y, var11.z);
               IsoGridSquare var15 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
               float var16 = var12 / (var13 * var13);
               if (var14 != null && var15 != null && var14.getRoom() != var15.getRoom()) {
                  var16 *= 1.2F;
                  if (var15.getRoom() == null || var14.getRoom() == null) {
                     var16 *= 1.4F;
                  }
               }

               var16 = 1.0F - var16;
               if (!(var16 <= 0.0F)) {
                  if (var16 > 1.0F) {
                     var16 = 1.0F;
                  }

                  float var17 = (float)var11.volume * var16;
                  if (var17 > var6) {
                     var6 = var17;
                     var7 = var11;
                  }
               }
            }
         }
      }

      return resultBiggestSound.init(var7, var6);
   }

   public float getSoundAttract(WorldSound var1, IsoZombie var2) {
      if (var1 == null) {
         return 0.0F;
      } else if (var1.radius == 0) {
         return 0.0F;
      } else {
         float var3 = IsoUtils.DistanceToSquared(var2.getX(), var2.getY(), var2.getZ() * 3.0F, (float)var1.x, (float)var1.y, (float)(var1.z * 3));
         float var4 = (float)var1.radius * this.getHearingMultiplier(var2);
         if (var3 > var4 * var4) {
            return 0.0F;
         } else if (var3 < var1.zombieIgnoreDist * var1.zombieIgnoreDist && var2.getZ() == (float)var1.z) {
            return 0.0F;
         } else if (var1.sourceIsZombie) {
            return 0.0F;
         } else {
            IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var1.x, var1.y, var1.z);
            IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare((double)var2.getX(), (double)var2.getY(), (double)var2.getZ());
            float var7 = var3 / (var4 * var4);
            if (var5 != null && var6 != null && var5.getRoom() != var6.getRoom()) {
               var7 *= 1.2F;
               if (var6.getRoom() == null || var5.getRoom() == null) {
                  var7 *= 1.4F;
               }
            }

            var7 = 1.0F - var7;
            if (var7 <= 0.0F) {
               return 0.0F;
            } else {
               if (var7 > 1.0F) {
                  var7 = 1.0F;
               }

               float var8 = (float)var1.volume * var7;
               return var8;
            }
         }
      }
   }

   public float getSoundAttractAnimal(WorldSound var1, IsoAnimal var2) {
      if (var1 == null) {
         return 0.0F;
      } else if (var1.radius == 0) {
         return 0.0F;
      } else {
         float var3 = IsoUtils.DistanceTo(var2.getX(), var2.getY(), var2.getZ() * 3.0F, (float)var1.x, (float)var1.y, (float)(var1.z * 3));
         float var4 = 1.0F;
         if (var2.isWild()) {
            var4 = 3.0F;
         }

         if (var3 > (float)var1.radius * var4) {
            return 0.0F;
         } else if (var3 < var1.zombieIgnoreDist * var1.zombieIgnoreDist && var2.getZ() == (float)var1.z) {
            return 0.0F;
         } else {
            IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var1.x, var1.y, var1.z);
            IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare((double)var2.getX(), (double)var2.getY(), (double)var2.getZ());
            float var7 = var3 / (float)(var1.radius * var1.radius);
            if (var5 != null && var6 != null && var5.getRoom() != var6.getRoom()) {
               var7 *= 1.2F;
               if (var6.getRoom() == null || var5.getRoom() == null) {
                  var7 *= 1.4F;
               }
            }

            var7 = 1.0F - var7;
            if (var7 <= 0.0F) {
               return 0.0F;
            } else {
               if (var7 > 1.0F) {
                  var7 = 1.0F;
               }

               float var8 = (float)var1.volume * var7;
               return var8;
            }
         }
      }
   }

   public float getStressFromSounds(int var1, int var2, int var3) {
      float var4 = 0.0F;

      for(int var5 = 0; var5 < this.SoundList.size(); ++var5) {
         WorldSound var6 = (WorldSound)this.SoundList.get(var5);
         if (var6.stresshumans && var6.radius != 0) {
            float var7 = IsoUtils.DistanceManhatten((float)var1, (float)var2, (float)var6.x, (float)var6.y);
            float var8 = var7 / (float)var6.radius;
            var8 = 1.0F - var8;
            if (!(var8 <= 0.0F)) {
               if (var8 > 1.0F) {
                  var8 = 1.0F;
               }

               float var9 = var8 * var6.stressMod;
               var4 += var9;
            }
         }
      }

      return var4;
   }

   public void update() {
      int var1;
      if (!GameServer.bServer) {
         for(var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
            if (!var2.ignore) {
               for(int var3 = 0; var3 < IsoChunkMap.ChunkGridWidth; ++var3) {
                  for(int var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
                     IsoChunk var5 = var2.getChunk(var4, var3);
                     if (var5 != null) {
                        var5.updateSounds();
                     }
                  }
               }
            }
         }
      }

      var1 = this.SoundList.size();

      for(int var6 = 0; var6 < var1; ++var6) {
         WorldSound var7 = (WorldSound)this.SoundList.get(var6);
         if (var7 != null && var7.life > 0) {
            --var7.life;
         } else {
            this.SoundList.remove(var6);
            this.freeSounds.push(var7);
            --var6;
            --var1;
         }
      }

   }

   public void render() {
      if (Core.bDebug && DebugOptions.instance.WorldSoundRender.getValue()) {
         if (!GameClient.bClient) {
            if (!GameServer.bServer || ServerGUI.isCreated()) {
               int var1 = SandboxOptions.instance.Lore.Hearing.getValue();
               if (var1 == 4) {
                  var1 = 2;
               }

               if (var1 == 5) {
                  var1 = 2;
               }

               float var2 = this.getHearingMultiplier(var1);

               for(int var3 = 0; var3 < this.SoundList.size(); ++var3) {
                  WorldSound var4 = (WorldSound)this.SoundList.get(var3);
                  float var5 = (float)var4.radius * var2;
                  byte var6 = 32;
                  LineDrawer.DrawIsoCircle((float)var4.x, (float)var4.y, (float)var4.z, var5, var6, 1.0F, 1.0F, 1.0F, 1.0F);
               }

               if (!GameServer.bServer) {
                  IsoChunkMap var15 = IsoWorld.instance.CurrentCell.getChunkMap(0);
                  if (var15 != null && !var15.ignore) {
                     for(int var16 = 0; var16 < IsoChunkMap.ChunkGridWidth; ++var16) {
                        for(int var17 = 0; var17 < IsoChunkMap.ChunkGridWidth; ++var17) {
                           IsoChunk var18 = var15.getChunk(var17, var16);
                           if (var18 != null) {
                              for(int var7 = 0; var7 < var18.SoundList.size(); ++var7) {
                                 WorldSound var8 = (WorldSound)var18.SoundList.get(var7);
                                 float var9 = (float)var8.radius * var2;
                                 byte var10 = 32;
                                 LineDrawer.DrawIsoCircle((float)var8.x, (float)var8.y, (float)var8.z, var9, var10, 0.0F, 1.0F, 1.0F, 1.0F);
                                 float var11 = (float)(var18.wx * 8) + 0.1F;
                                 float var12 = (float)(var18.wy * 8) + 0.1F;
                                 float var13 = (float)((var18.wx + 1) * 8) - 0.1F;
                                 float var14 = (float)((var18.wy + 1) * 8) - 0.1F;
                                 LineDrawer.DrawIsoRect(var11, var12, var13 - var11, var14 - var12, var8.z, 0.0F, 1.0F, 1.0F);
                              }
                           }
                        }
                     }

                  }
               }
            }
         }
      }
   }

   public float getHearingMultiplier(IsoZombie var1) {
      return var1 == null ? this.getHearingMultiplier(2) : this.getHearingMultiplier(var1.hearing) * var1.getWornItemsHearingMultiplier() * var1.getWeatherHearingMultiplier();
   }

   public float getHearingMultiplier(int var1) {
      if (var1 == 1) {
         return 3.0F;
      } else {
         return var1 == 3 ? 0.45F : 1.0F;
      }
   }

   public static final class WorldSound {
      public Object source = null;
      public int life = 1;
      public int radius;
      public boolean stresshumans;
      public boolean stressAnimals = false;
      public int volume;
      public int x;
      public int y;
      public int z;
      public float zombieIgnoreDist = 0.0F;
      public boolean sourceIsZombie;
      public float stressMod = 1.0F;
      public boolean bRepeating;

      public WorldSound() {
      }

      public WorldSound init(Object var1, int var2, int var3, int var4, int var5, int var6) {
         return this.init(var1, var2, var3, var4, var5, var6, false, 0.0F, 1.0F);
      }

      public WorldSound init(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7) {
         return this.init(var1, var2, var3, var4, var5, var6, var7, 0.0F, 1.0F);
      }

      public WorldSound init(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9) {
         this.source = var1;
         this.life = 16;
         this.x = var2;
         this.y = var3;
         this.z = var4;
         this.radius = var5;
         this.volume = var6;
         this.stresshumans = var7;
         this.stressAnimals = false;
         this.zombieIgnoreDist = var8;
         this.stressMod = var9;
         this.sourceIsZombie = var1 instanceof IsoZombie;
         this.bRepeating = false;
         LuaEventManager.triggerEvent("OnWorldSound", var2, var3, var4, var5, var6, var1);
         if (!GameClient.bClient) {
            FishSchoolManager.getInstance().addSoundNoise(var2, var3, var5 / 6);
         }

         return this;
      }

      public WorldSound init(boolean var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9) {
         WorldSound var10 = this.init((Object)null, var2, var3, var4, var5, var6, var7, var8, var9);
         var10.sourceIsZombie = var1;
         return var10;
      }
   }

   public static final class ResultBiggestSound {
      public WorldSound sound;
      public float attract;

      public ResultBiggestSound() {
      }

      public ResultBiggestSound init(WorldSound var1, float var2) {
         this.sound = var1;
         this.attract = var2;
         return this;
      }
   }
}
