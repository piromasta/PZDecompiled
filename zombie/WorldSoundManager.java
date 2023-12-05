package zombie;

import java.util.ArrayList;
import java.util.Stack;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
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
      return this.addSound(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, true, false);
   }

   public WorldSound addSound(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7, float var8, float var9, boolean var10, boolean var11, boolean var12) {
      if (var5 <= 0) {
         return null;
      } else {
         WorldSound var13;
         synchronized(this.SoundList) {
            var13 = this.getNew().init(var1, var2, var3, var4, var5, var6, var7, var8, var9);
            if (var1 == null) {
               var13.sourceIsZombie = var10;
            }

            if (!GameServer.bServer) {
               int var15 = SandboxOptions.instance.Lore.Hearing.getValue();
               if (var15 == 4) {
                  var15 = 1;
               }

               int var16 = (int)PZMath.ceil((float)var5 * this.getHearingMultiplier(var15));
               int var17 = (var2 - var16) / 10;
               int var18 = (var3 - var16) / 10;
               int var19 = (int)Math.ceil((double)(((float)var2 + (float)var16) / 10.0F));
               int var20 = (int)Math.ceil((double)(((float)var3 + (float)var16) / 10.0F));

               for(int var21 = var17; var21 < var19; ++var21) {
                  for(int var22 = var18; var22 < var20; ++var22) {
                     IsoChunk var23 = IsoWorld.instance.CurrentCell.getChunk(var21, var22);
                     if (var23 != null) {
                        var23.SoundList.add(var13);
                     }
                  }
               }
            }

            this.SoundList.add(var13);
            ZombiePopulationManager.instance.addWorldSound(var13, var11);
         }

         if (var11) {
            if (GameClient.bClient) {
               GameClient.instance.sendWorldSound(var13);
            } else if (GameServer.bServer) {
               GameServer.sendWorldSound((WorldSound)var13, (UdpConnection)null);
            }
         }

         if (Core.bDebug && GameClient.bClient) {
            MPDebugInfo.AddDebugSound(var13);
         }

         return var13;
      }
   }

   public WorldSound addSoundRepeating(Object var1, int var2, int var3, int var4, int var5, int var6, boolean var7) {
      WorldSound var8 = this.addSound(var1, var2, var3, var4, var5, var6, var7, 0.0F, 1.0F);
      if (var8 != null) {
         var8.bRepeating = true;
      }

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
            float var12 = IsoUtils.DistanceToSquared((float)var1, (float)var2, (float)var11.x, (float)var11.y);
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
         float var3 = IsoUtils.DistanceToSquared(var2.x, var2.y, (float)var1.x, (float)var1.y);
         float var4 = (float)var1.radius * this.getHearingMultiplier(var2);
         if (var3 > var4 * var4) {
            return 0.0F;
         } else if (var3 < var1.zombieIgnoreDist * var1.zombieIgnoreDist && var2.z == (float)var1.z) {
            return 0.0F;
         } else if (var1.sourceIsZombie) {
            return 0.0F;
         } else {
            IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var1.x, var1.y, var1.z);
            IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare((double)var2.x, (double)var2.y, (double)var2.z);
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

               float var2 = this.getHearingMultiplier(var1);

               for(int var3 = 0; var3 < this.SoundList.size(); ++var3) {
                  WorldSound var4 = (WorldSound)this.SoundList.get(var3);
                  float var5 = (float)var4.radius * var2;

                  for(double var6 = 0.0; var6 < 6.283185307179586; var6 += 0.15707963267948966) {
                     this.DrawIsoLine((float)var4.x + var5 * (float)Math.cos(var6), (float)var4.y + var5 * (float)Math.sin(var6), (float)var4.x + var5 * (float)Math.cos(var6 + 0.15707963267948966), (float)var4.y + var5 * (float)Math.sin(var6 + 0.15707963267948966), (float)var4.z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                  }
               }

               if (!GameServer.bServer) {
                  IsoChunkMap var16 = IsoWorld.instance.CurrentCell.getChunkMap(0);
                  if (var16 != null && !var16.ignore) {
                     for(int var17 = 0; var17 < IsoChunkMap.ChunkGridWidth; ++var17) {
                        for(int var18 = 0; var18 < IsoChunkMap.ChunkGridWidth; ++var18) {
                           IsoChunk var19 = var16.getChunk(var18, var17);
                           if (var19 != null) {
                              for(int var7 = 0; var7 < var19.SoundList.size(); ++var7) {
                                 WorldSound var8 = (WorldSound)var19.SoundList.get(var7);
                                 float var9 = (float)var8.radius * var2;

                                 for(double var10 = 0.0; var10 < 6.283185307179586; var10 += 0.15707963267948966) {
                                    this.DrawIsoLine((float)var8.x + var9 * (float)Math.cos(var10), (float)var8.y + var9 * (float)Math.sin(var10), (float)var8.x + var9 * (float)Math.cos(var10 + 0.15707963267948966), (float)var8.y + var9 * (float)Math.sin(var10 + 0.15707963267948966), (float)var8.z, 0.0F, 1.0F, 1.0F, 1.0F, 1);
                                    float var12 = (float)(var19.wx * 10) + 0.1F;
                                    float var13 = (float)(var19.wy * 10) + 0.1F;
                                    float var14 = (float)((var19.wx + 1) * 10) - 0.1F;
                                    float var15 = (float)((var19.wy + 1) * 10) - 0.1F;
                                    this.DrawIsoLine(var12, var13, var14, var13, (float)var8.z, 0.0F, 1.0F, 1.0F, 1.0F, 1);
                                    this.DrawIsoLine(var14, var13, var14, var15, (float)var8.z, 0.0F, 1.0F, 1.0F, 1.0F, 1);
                                    this.DrawIsoLine(var14, var15, var12, var15, (float)var8.z, 0.0F, 1.0F, 1.0F, 1.0F, 1);
                                    this.DrawIsoLine(var12, var15, var12, var13, (float)var8.z, 0.0F, 1.0F, 1.0F, 1.0F, 1);
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
   }

   private void DrawIsoLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int var10) {
      float var11 = IsoUtils.XToScreenExact(var1, var2, var5, 0);
      float var12 = IsoUtils.YToScreenExact(var1, var2, var5, 0);
      float var13 = IsoUtils.XToScreenExact(var3, var4, var5, 0);
      float var14 = IsoUtils.YToScreenExact(var3, var4, var5, 0);
      LineDrawer.drawLine(var11, var12, var13, var14, var6, var7, var8, var9, var10);
   }

   public float getHearingMultiplier(IsoZombie var1) {
      return var1 == null ? this.getHearingMultiplier(2) : this.getHearingMultiplier(var1.hearing);
   }

   public float getHearingMultiplier(int var1) {
      if (var1 == 1) {
         return 3.0F;
      } else {
         return var1 == 3 ? 0.45F : 1.0F;
      }
   }

   public class WorldSound {
      public Object source = null;
      public int life = 1;
      public int radius;
      public boolean stresshumans;
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
         this.life = 1;
         this.x = var2;
         this.y = var3;
         this.z = var4;
         this.radius = var5;
         this.volume = var6;
         this.stresshumans = var7;
         this.zombieIgnoreDist = var8;
         this.stressMod = var9;
         this.sourceIsZombie = var1 instanceof IsoZombie;
         this.bRepeating = false;
         LuaEventManager.triggerEvent("OnWorldSound", var2, var3, var4, var5, var6, var1);
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
