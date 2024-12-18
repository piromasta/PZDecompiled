package zombie.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleType;
import zombie.core.Core;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.weather.ClimateManager;
import zombie.meta.Meta;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehiclePart;

public final class LightingJNI {
   public static final int ROOM_SPAWN_DIST = 50;
   public static boolean init = false;
   public static final int[][] ForcedVis = new int[][]{{-1, 0, -1, -1, 0, -1, 1, -1, 1, 0, -2, -2, -1, -2, 0, -2, 1, -2, 2, -2}, {-1, 1, -1, 0, -1, -1, 0, -1, 1, -1, -2, 0, -2, -1, -2, -2, -1, -2, 0, -2}, {0, 1, -1, 1, -1, 0, -1, -1, 0, -1, -2, 2, -2, 1, -2, 0, -2, -1, -2, -2}, {1, 1, 0, 1, -1, 1, -1, 0, -1, -1, 0, 2, -1, 2, -2, 2, -2, 1, -2, 0}, {1, 0, 1, 1, 0, 1, -1, 1, -1, 0, 2, 2, 1, 2, 0, 2, -1, 2, -2, 2}, {-1, 1, 0, 1, 1, 1, 1, 0, 1, -1, 2, 0, 2, 1, 2, 2, 1, 2, 0, 2}, {0, 1, 1, 1, 1, 0, 1, -1, 0, -1, 2, -2, 2, -1, 2, 0, 2, 1, 2, 2}, {-1, -1, 0, -1, 1, -1, 1, 0, 1, 1, 0, -2, 1, -2, 2, -2, 2, -1, 2, 0}};
   private static final ArrayList<IsoGameCharacter.TorchInfo> torches = new ArrayList();
   private static final ArrayList<IsoGameCharacter.TorchInfo> activeTorches = new ArrayList();
   private static final ArrayList<IsoLightSource> JNILights = new ArrayList();
   private static final int[] updateCounter = new int[4];
   private static boolean bWasElecShut = false;
   private static boolean bWasNight = false;
   private static final Vector2 tempVector2 = new Vector2();
   private static final int MAX_PLAYERS = 256;
   private static final int MAX_LIGHTS_PER_PLAYER = 4;
   private static final int MAX_LIGHTS_PER_VEHICLE = 10;
   private static final ArrayList<InventoryItem> tempItems = new ArrayList();
   private static CompletableFuture<Void> checkLightsFuture;

   public LightingJNI() {
   }

   public static void doInvalidateGlobalLights(int var0) {
      ++Core.dirtyGlobalLightsCount;
   }

   public static void init() {
      if (!init) {
         String var0 = "";
         if ("1".equals(System.getProperty("zomboid.debuglibs.lighting"))) {
            DebugLog.log("***** Loading debug version of Lighting");
            var0 = "d";
         }

         try {
            if (System.getProperty("os.name").contains("OS X")) {
               System.loadLibrary("Lighting");
            } else if (System.getProperty("os.name").startsWith("Win")) {
               if (System.getProperty("sun.arch.data.model").equals("64")) {
                  System.loadLibrary("Lighting64" + var0);
               } else {
                  System.loadLibrary("Lighting32" + var0);
               }
            } else if (System.getProperty("sun.arch.data.model").equals("64")) {
               System.loadLibrary("Lighting64");
            } else {
               System.loadLibrary("Lighting32");
            }

            for(int var1 = 0; var1 < 4; ++var1) {
               updateCounter[var1] = -1;
            }

            configure(0.005F);
            init = true;
         } catch (UnsatisfiedLinkError var4) {
            var4.printStackTrace();

            try {
               Thread.sleep(3000L);
            } catch (InterruptedException var3) {
            }

            System.exit(1);
         }

      }
   }

   private static int getTorchIndexById(int var0) {
      for(int var1 = 0; var1 < torches.size(); ++var1) {
         IsoGameCharacter.TorchInfo var2 = (IsoGameCharacter.TorchInfo)torches.get(var1);
         if (var2.id == var0) {
            return var1;
         }
      }

      return -1;
   }

   private static void checkTorch(IsoPlayer var0, InventoryItem var1, int var2) {
      int var3 = getTorchIndexById(var2);
      IsoGameCharacter.TorchInfo var4;
      if (var3 == -1) {
         var4 = IsoGameCharacter.TorchInfo.alloc();
         torches.add(var4);
      } else {
         var4 = (IsoGameCharacter.TorchInfo)torches.get(var3);
      }

      var4.set(var0, var1);
      if (var4.id == 0) {
         var4.id = var2;
      }

      updateTorch(var4.id, var4.x, var4.y, var4.z + 32.0F, var4.angleX, var4.angleY, var4.dist, var4.strength, var4.bCone, var4.dot, var4.focusing);
      activeTorches.add(var4);
   }

   private static int checkPlayerTorches(IsoPlayer var0, int var1) {
      ArrayList var2 = tempItems;
      var2.clear();
      var0.getActiveLightItems(var2);
      int var3 = Math.min(var2.size(), 4);

      for(int var4 = 0; var4 < var3; ++var4) {
         checkTorch(var0, (InventoryItem)var2.get(var4), var1 * 4 + var4 + 1);
      }

      return var3;
   }

   private static void clearPlayerTorches(int var0, int var1) {
      for(int var2 = var1; var2 < 4; ++var2) {
         int var3 = var0 * 4 + var2 + 1;
         int var4 = getTorchIndexById(var3);
         if (var4 != -1) {
            IsoGameCharacter.TorchInfo var5 = (IsoGameCharacter.TorchInfo)torches.get(var4);
            removeTorch(var5.id);
            var5.id = 0;
            IsoGameCharacter.TorchInfo.release(var5);
            torches.remove(var4);
            break;
         }
      }

   }

   private static void checkTorch(VehiclePart var0, int var1) {
      VehicleLight var2 = var0.getLight();
      if (var2 != null && var2.getActive()) {
         IsoGameCharacter.TorchInfo var5 = null;

         for(int var6 = 0; var6 < torches.size(); ++var6) {
            var5 = (IsoGameCharacter.TorchInfo)torches.get(var6);
            if (var5.id == var1) {
               break;
            }

            var5 = null;
         }

         if (var5 == null) {
            var5 = IsoGameCharacter.TorchInfo.alloc();
            torches.add(var5);
         }

         var5.set(var0);
         if (var5.id == 0) {
            var5.id = var1;
         }

         updateTorch(var5.id, var5.x, var5.y, var5.z + 32.0F, var5.angleX, var5.angleY, var5.dist, var5.strength, var5.bCone, var5.dot, var5.focusing);
         activeTorches.add(var5);
      } else {
         for(int var3 = 0; var3 < torches.size(); ++var3) {
            IsoGameCharacter.TorchInfo var4 = (IsoGameCharacter.TorchInfo)torches.get(var3);
            if (var4.id == var1) {
               removeTorch(var4.id);
               var4.id = 0;
               IsoGameCharacter.TorchInfo.release(var4);
               torches.remove(var3--);
            }
         }
      }

   }

   private static void checkLights() {
      if (IsoWorld.instance.CurrentCell != null) {
         if (GameClient.bClient) {
            IsoGenerator.updateSurroundingNow();
         }

         boolean var0 = IsoWorld.instance.isHydroPowerOn();
         Stack var1 = IsoWorld.instance.CurrentCell.getLamppostPositions();

         int var2;
         IsoLightSource var3;
         int var5;
         boolean var13;
         for(var2 = 0; var2 < var1.size(); ++var2) {
            var3 = (IsoLightSource)var1.get(var2);
            IsoChunk var4 = IsoWorld.instance.CurrentCell.getChunkForGridSquare(var3.x, var3.y, var3.z);
            if (var4 != null && var3.chunk != null && var3.chunk != var4) {
               var3.life = 0;
            }

            if (var3.life != 0 && var3.isInBounds()) {
               if (var3.bHydroPowered) {
                  if (var3.switches.isEmpty()) {
                     assert false;

                     var13 = var0;
                     if (!var0) {
                        IsoGridSquare var16 = IsoWorld.instance.CurrentCell.getGridSquare(var3.x, var3.y, var3.z);
                        var13 = var16 != null && var16.haveElectricity();
                     }

                     if (var3.bActive != var13) {
                        var3.bActive = var13;
                        GameTime.instance.lightSourceUpdate = 100.0F;
                     }
                  } else {
                     IsoLightSwitch var6 = (IsoLightSwitch)var3.switches.get(0);
                     var13 = var6.canSwitchLight();
                     if (var6.bStreetLight && (GameTime.getInstance().getNight() < 0.5F || !IsoWorld.instance.isHydroPowerOn())) {
                        var13 = false;
                     }

                     if (var3.bActive && !var13) {
                        var3.bActive = false;
                        GameTime.instance.lightSourceUpdate = 100.0F;
                     } else if (!var3.bActive && var13 && var6.isActivated()) {
                        var3.bActive = true;
                        GameTime.instance.lightSourceUpdate = 100.0F;
                     }
                  }
               }

               float var20 = 2.0F;
               if (var3.ID == 0) {
                  var3.ID = IsoLightSource.NextID++;
                  if (var3.life != -1) {
                     addTempLight(var3.ID, var3.x, var3.y, var3.z + 32, var3.radius, var3.r, var3.g, var3.b, (int)((float)(var3.life * PerformanceSettings.getLockFPS()) / 30.0F));
                     var1.remove(var2--);
                  } else {
                     var3.rJNI = var3.r;
                     var3.gJNI = var3.g;
                     var3.bJNI = var3.b;
                     var3.bActiveJNI = var3.bActive;
                     JNILights.add(var3);
                     addLight(var3.ID, var3.x, var3.y, var3.z + 32, PZMath.min(var3.radius, 20), PZMath.clamp(var3.r * var20, 0.0F, 1.0F), PZMath.clamp(var3.g * var20, 0.0F, 1.0F), PZMath.clamp(var3.b * var20, 0.0F, 1.0F), var3.localToBuilding == null ? -1 : var3.localToBuilding.ID, var3.bActive);
                  }
               } else {
                  if (var3.r != var3.rJNI || var3.g != var3.gJNI || var3.b != var3.bJNI) {
                     var3.rJNI = var3.r;
                     var3.gJNI = var3.g;
                     var3.bJNI = var3.b;
                     setLightColor(var3.ID, PZMath.clamp(var3.r * var20, 0.0F, 1.0F), PZMath.clamp(var3.g * var20, 0.0F, 1.0F), PZMath.clamp(var3.b * var20, 0.0F, 1.0F));
                  }

                  if (var3.bActiveJNI != var3.bActive) {
                     var3.bActiveJNI = var3.bActive;
                     setLightActive(var3.ID, var3.bActive);
                  }
               }
            } else {
               var1.remove(var2);
               if (var3.ID != 0) {
                  var5 = var3.ID;
                  var3.ID = 0;
                  JNILights.remove(var3);
                  removeLight(var5);
                  GameTime.instance.lightSourceUpdate = 100.0F;
               }

               --var2;
            }
         }

         int var9;
         for(var2 = 0; var2 < JNILights.size(); ++var2) {
            var3 = (IsoLightSource)JNILights.get(var2);
            if (!var1.contains(var3)) {
               var9 = var3.ID;
               var3.ID = 0;
               JNILights.remove(var2--);
               removeLight(var9);
            }
         }

         ArrayList var11 = IsoWorld.instance.CurrentCell.roomLights;

         int var8;
         for(var8 = 0; var8 < var11.size(); ++var8) {
            IsoRoomLight var10 = (IsoRoomLight)var11.get(var8);
            if (!var10.isInBounds()) {
               var11.remove(var8--);
               if (var10.ID != 0) {
                  var5 = var10.ID;
                  var10.ID = 0;
                  removeRoomLight(var5);
                  GameTime.instance.lightSourceUpdate = 100.0F;
               }
            } else {
               var10.bActive = var10.room.def.bLightsActive;
               if (!var0) {
                  var13 = false;

                  for(int var18 = 0; !var13 && var18 < var10.room.lightSwitches.size(); ++var18) {
                     IsoLightSwitch var7 = (IsoLightSwitch)var10.room.lightSwitches.get(var18);
                     if (var7.square != null && var7.square.haveElectricity()) {
                        var13 = true;
                     }
                  }

                  if (!var13 && var10.bActive) {
                     var10.bActive = false;
                     if (var10.bActiveJNI) {
                        IsoGridSquare.RecalcLightTime = -1.0F;
                        if (PerformanceSettings.FBORenderChunk) {
                           ++Core.dirtyGlobalLightsCount;
                        }

                        GameTime.instance.lightSourceUpdate = 100.0F;
                     }
                  } else if (var13 && var10.bActive && !var10.bActiveJNI) {
                     IsoGridSquare.RecalcLightTime = -1.0F;
                     if (PerformanceSettings.FBORenderChunk) {
                        ++Core.dirtyGlobalLightsCount;
                     }

                     GameTime.instance.lightSourceUpdate = 100.0F;
                  }
               }

               if (var10.ID == 0) {
                  var10.ID = 100000 + IsoRoomLight.NextID++;
                  addRoomLight(var10.ID, var10.room.building.ID, RoomID.getIndex(var10.room.def.ID), var10.x, var10.y, var10.z + 32, var10.width, var10.height, var10.bActive);
                  var10.bActiveJNI = var10.bActive;
                  GameTime.instance.lightSourceUpdate = 100.0F;
               } else if (var10.bActiveJNI != var10.bActive) {
                  setRoomLightActive(var10.ID, var10.bActive);
                  var10.bActiveJNI = var10.bActive;
                  GameTime.instance.lightSourceUpdate = 100.0F;
               }
            }
         }

         activeTorches.clear();
         if (GameClient.bClient) {
            ArrayList var12 = GameClient.instance.getPlayers();

            for(var9 = 0; var9 < var12.size(); ++var9) {
               IsoPlayer var21 = (IsoPlayer)var12.get(var9);
               checkPlayerTorches(var21, var21.OnlineID + 1);
            }
         } else {
            for(var8 = 0; var8 < IsoPlayer.numPlayers; ++var8) {
               IsoPlayer var14 = IsoPlayer.players[var8];
               if (var14 != null && !var14.isDead() && (var14.getVehicle() == null || var14.isAiming())) {
                  var5 = checkPlayerTorches(var14, var8);
                  clearPlayerTorches(var8, var5);
               } else {
                  clearPlayerTorches(var8, 0);
               }
            }
         }

         for(var8 = 0; var8 < IsoWorld.instance.CurrentCell.getVehicles().size(); ++var8) {
            BaseVehicle var15 = (BaseVehicle)IsoWorld.instance.CurrentCell.getVehicles().get(var8);
            if (var15.VehicleID != -1) {
               for(var5 = 0; var5 < var15.getLightCount(); ++var5) {
                  VehiclePart var19 = var15.getLightByIndex(var5);
                  checkTorch(var19, 1024 + var15.VehicleID * 10 + var5);
               }
            }
         }

         for(var8 = 0; var8 < torches.size(); ++var8) {
            IsoGameCharacter.TorchInfo var17 = (IsoGameCharacter.TorchInfo)torches.get(var8);
            if (!activeTorches.contains(var17)) {
               removeTorch(var17.id);
               var17.id = 0;
               IsoGameCharacter.TorchInfo.release(var17);
               torches.remove(var8--);
            }
         }

      }
   }

   public static float calculateVisionCone(IsoGameCharacter var0) {
      float var1;
      if (var0.getVehicle() == null) {
         var1 = -0.2F;
         var1 -= var0.getStats().fatigue - 0.6F;
         if (var1 > -0.2F) {
            var1 = -0.2F;
         }

         if (var0.getStats().fatigue >= 1.0F) {
            var1 -= 0.2F;
         }

         if (var0.getMoodles().getMoodleLevel(MoodleType.Drunk) >= 2) {
            var1 -= var0.getStats().Drunkenness * 0.002F;
         }

         if (var0.getMoodles().getMoodleLevel(MoodleType.Panic) == 4) {
            var1 -= 0.2F;
         }

         if (var0.isInARoom()) {
            var1 -= 0.2F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
         } else {
            var1 -= 0.7F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
         }

         if (var1 < -0.9F) {
            var1 = -0.9F;
         }

         if (var0.Traits.EagleEyed.isSet()) {
            var1 += 0.2F * ClimateManager.getInstance().getDayLightStrength();
         }

         if (var0.Traits.NightVision.isSet()) {
            var1 += 0.2F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
         }

         DebugLog.Lightning.debugln("cone 1 %f", var1);
         var1 *= var0.getWornItemsVisionModifier();
         DebugLog.Lightning.debugln("cone 2 %f", var1);
         if (var1 > 0.0F) {
            var1 = 0.0F;
         } else if (var1 < -0.9F) {
            var1 = -0.9F;
         }

         DebugLog.Lightning.debugln("cone 3 %f", var1);
      } else {
         var1 = 0.8F - 3.0F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
         if (var0.Traits.NightVision.isSet()) {
            var1 += 0.2F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
         }

         var1 *= var0.getWornItemsVisionModifier();
         if (var1 > 1.0F) {
            var1 = 1.0F;
         }

         if (var0.getVehicle().getHeadlightsOn() && var0.getVehicle().getHeadlightCanEmmitLight() && var1 < -0.8F) {
            var1 = -0.8F;
         } else if (var1 < -0.95F) {
            var1 = -0.95F;
         }
      }

      return var1;
   }

   public static float calculateRearZombieDistance(IsoGameCharacter var0) {
      return var0.getSeeNearbyCharacterDistance();
   }

   public static void updatePlayer(int var0) {
      IsoPlayer var1 = IsoPlayer.players[var0];
      if (var1 != null) {
         float var2 = var1.getStats().fatigue - 0.6F;
         if (var2 < 0.0F) {
            var2 = 0.0F;
         }

         var2 *= 2.5F;
         float var3 = 2.0F;
         if (var1.Traits.HardOfHearing.isSet()) {
            --var3;
         }

         if (var1.Traits.KeenHearing.isSet()) {
            var3 += 3.0F;
         }

         var3 *= var1.getWornItemsHearingMultiplier();
         float var4 = calculateVisionCone(var1);
         Vector2 var5 = var1.getLookVector(tempVector2);
         BaseVehicle var6 = var1.getVehicle();
         if (var6 != null && !var1.isAiming() && !var1.isLookingWhileInVehicle() && var6.isDriver(var1) && var6.getCurrentSpeedKmHour() < -1.0F) {
            var5.rotate(3.1415927F);
         }

         playerSet(var1.getX(), var1.getY(), var1.getZ() + 32.0F, var5.x, var5.y, false, var1.ReanimatedCorpse != null, var1.isGhostMode(), var1.Traits.ShortSighted.isSet(), var2, var3, var4);
      }

   }

   public static void updateChunk(int var0, IsoChunk var1) {
      chunkBeginUpdate(var1.wx, var1.wy, var1.getMinLevel() + 32, var1.getMaxLevel() + 32);

      for(int var2 = var1.getMinLevel(); var2 <= var1.getMaxLevel(); ++var2) {
         IsoChunkLevel var3 = var1.getLevelData(var2);
         if (var3.lightCheck[var0]) {
            var3.lightCheck[var0] = false;
            chunkLevelBeginUpdate(var2 + 32);

            for(int var4 = 0; var4 < 8; ++var4) {
               for(int var5 = 0; var5 < 8; ++var5) {
                  IsoGridSquare var6 = var3.squares[var5 + var4 * 8];
                  if (var6 != null) {
                     squareBeginUpdate(var5, var4, var2 + 32);
                     int var7 = var6.visionMatrix;
                     if (var6.isSeen(var0)) {
                        var7 |= 1 << 27 + var0;
                     }

                     boolean var8 = var6.getOpenAir();
                     if (var8) {
                        var6.lightLevel = GameTime.getInstance().getSkyLightLevel();
                     }

                     boolean var9 = var6.Has(IsoObjectType.stairsTN) || var6.Has(IsoObjectType.stairsMN) || var6.Has(IsoObjectType.stairsTW) || var6.Has(IsoObjectType.stairsMW);
                     int var10 = 0;

                     int var11;
                     for(var11 = 0; var11 < 8; ++var11) {
                        IsoDirections var12 = IsoDirections.fromIndex(var11);
                        if (var6.testVisionAdjacent(var12.dx(), var12.dy(), 0, true, false) != LosUtil.TestResults.Blocked) {
                           var10 |= 1 << var11;
                        }
                     }

                     squareSet(var10, var6.testVisionAdjacent(0, 0, 1, true, false) != LosUtil.TestResults.Blocked, var6.testVisionAdjacent(0, 0, -1, true, false) != LosUtil.TestResults.Blocked, var9, var7, var6.getRoom() == null ? -1 : var6.getBuilding().ID, var6.getRoom() == null ? -1 : RoomID.getIndex(var6.getRoomID()), var6.lightLevel, var8);

                     for(var11 = 0; var11 < var6.getSpecialObjects().size(); ++var11) {
                        IsoObject var19 = (IsoObject)var6.getSpecialObjects().get(var11);
                        if (var19 instanceof IsoCurtain) {
                           IsoCurtain var22 = (IsoCurtain)var19;
                           int var25 = 0;
                           if (var22.getType() == IsoObjectType.curtainW) {
                              var25 |= 4;
                           } else if (var22.getType() == IsoObjectType.curtainN) {
                              var25 |= 8;
                           } else if (var22.getType() == IsoObjectType.curtainE) {
                              var25 |= 16;
                           } else if (var22.getType() == IsoObjectType.curtainS) {
                              var25 |= 32;
                           }

                           squareAddCurtain(var25, var22.open);
                        } else {
                           boolean var14;
                           IsoBarricade var15;
                           IsoBarricade var16;
                           if (!(var19 instanceof IsoDoor)) {
                              if (var19 instanceof IsoThumpable) {
                                 IsoThumpable var20 = (IsoThumpable)var19;
                                 var14 = var20.getSprite().getProperties().Is("doorTrans");
                                 if (var20.isDoor && var20.open) {
                                    var14 = true;
                                 }

                                 squareAddThumpable(var20.north, var20.open, var20.isDoor, var14);
                                 IsoThumpable var23 = (IsoThumpable)var19;
                                 boolean var24 = false;
                                 IsoBarricade var17 = var23.getBarricadeOnSameSquare();
                                 IsoBarricade var18 = var23.getBarricadeOnOppositeSquare();
                                 if (var17 != null) {
                                    var24 |= var17.isBlockVision();
                                 }

                                 if (var18 != null) {
                                    var24 |= var18.isBlockVision();
                                 }

                                 squareAddWindow(var23.north, var23.open, var24);
                              } else if (var19 instanceof IsoWindow) {
                                 IsoWindow var21 = (IsoWindow)var19;
                                 var14 = false;
                                 var15 = var21.getBarricadeOnSameSquare();
                                 var16 = var21.getBarricadeOnOppositeSquare();
                                 if (var15 != null) {
                                    var14 |= var15.isBlockVision();
                                 }

                                 if (var16 != null) {
                                    var14 |= var16.isBlockVision();
                                 }

                                 squareAddWindow(var21.north, var21.open, var14);
                              }
                           } else {
                              IsoDoor var13 = (IsoDoor)var19;
                              var14 = var13.sprite != null && var13.sprite.getProperties().Is("doorTrans");
                              if (var13.open) {
                                 var14 = true;
                              } else {
                                 var14 = var14 && (var13.HasCurtains() == null || var13.isCurtainOpen());
                              }

                              var15 = var13.getBarricadeOnSameSquare();
                              var16 = var13.getBarricadeOnOppositeSquare();
                              if (var15 != null && var15.isBlockVision()) {
                                 var14 = false;
                              }

                              if (var16 != null && var16.isBlockVision()) {
                                 var14 = false;
                              }

                              if (var13.IsOpen() && IsoDoor.getGarageDoorIndex(var13) != -1) {
                                 var14 = true;
                              }

                              squareAddDoor(var13.north, var13.open, var14);
                           }
                        }
                     }

                     squareEndUpdate();
                  } else {
                     squareSetNull(var5, var4, var2 + 32);
                  }
               }
            }

            chunkLevelEndUpdate();
         }
      }

      chunkEndUpdate();
   }

   public static void preUpdate() {
      if (DebugOptions.instance.ThreadLighting.getValue()) {
         checkLightsFuture = CompletableFuture.runAsync(LightingJNI::checkLights, PZForkJoinPool.commonPool());
      }

   }

   public static void update() {
      if (IsoWorld.instance != null && IsoWorld.instance.CurrentCell != null) {
         GameProfiler var10000;
         if (checkLightsFuture != null) {
            var10000 = GameProfiler.getInstance();
            CompletableFuture var10002 = checkLightsFuture;
            Objects.requireNonNull(var10002);
            var10000.invokeAndMeasure("checkLights", var10002::join);
         } else {
            GameProfiler.getInstance().invokeAndMeasure("checkLights", LightingJNI::checkLights);
         }

         checkLightsFuture = null;
         GameTime var0 = GameTime.getInstance();
         RenderSettings var1 = RenderSettings.getInstance();
         boolean var2 = IsoWorld.instance.isHydroPowerOn();
         boolean var3 = GameTime.getInstance().getNight() < 0.5F;
         if (var2 != bWasElecShut || var3 != bWasNight) {
            bWasElecShut = var2;
            bWasNight = var3;
            IsoGridSquare.RecalcLightTime = -1.0F;
            if (PerformanceSettings.FBORenderChunk) {
               ++Core.dirtyGlobalLightsCount;
            }

            var0.lightSourceUpdate = 100.0F;
         }

         for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
            IsoChunkMap var5 = IsoWorld.instance.CurrentCell.ChunkMap[var4];
            if (var5 != null && !var5.ignore) {
               RenderSettings.PlayerRenderSettings var6 = var1.getPlayerSettings(var4);
               stateBeginUpdate(var4, var5.getWorldXMin(), var5.getWorldYMin(), IsoChunkMap.ChunkGridWidth, IsoChunkMap.ChunkGridWidth);
               updatePlayer(var4);
               stateEndFrame(var6.getRmod(), var6.getGmod(), var6.getBmod(), var6.getAmbient(), var6.getNight(), var6.getViewDistance(), var0.getViewDistMax(), LosUtil.cachecleared[var4], var0.lightSourceUpdate, GameTime.getInstance().getSkyLightLevel());
               if (LosUtil.cachecleared[var4]) {
                  LosUtil.cachecleared[var4] = false;
                  IsoWorld.instance.CurrentCell.invalidatePeekedRoom(var4);
               }

               for(int var7 = 0; var7 < IsoChunkMap.ChunkGridWidth; ++var7) {
                  for(int var8 = 0; var8 < IsoChunkMap.ChunkGridWidth; ++var8) {
                     IsoChunk var9 = var5.getChunk(var8, var7);
                     if (var9 != null && var9.bLoaded) {
                        if (var9.lightCheck[var4]) {
                           updateChunk(var4, var9);
                           var9.lightCheck[var4] = false;
                        }

                        var9.bLightingNeverDone[var4] = !chunkLightingDone(var9.wx, var9.wy);
                     }
                  }
               }

               stateEndUpdate();
               updateCounter[var4] = stateUpdateCounter(var4);
               if (var0.lightSourceUpdate > 0.0F && IsoPlayer.players[var4] != null) {
                  IsoPlayer.players[var4].dirtyRecalcGridStackTime = 20.0F;
               }
            }
         }

         var10000 = GameProfiler.getInstance();
         Integer var10 = updateCounter[0];
         Boolean var10003 = var0.lightSourceUpdate > 0.0F;
         DeadBodyAtlas var10004 = DeadBodyAtlas.instance;
         Objects.requireNonNull(var10004);
         var10000.invokeAndMeasure("DeadBodyAtlas", var10, var10003, var10004::lightingUpdate);
         var0.lightSourceUpdate = 0.0F;
      }
   }

   public static void getTorches(ArrayList<IsoGameCharacter.TorchInfo> var0) {
      var0.addAll(torches);
   }

   public static int getUpdateCounter(int var0) {
      return updateCounter[var0];
   }

   public static void stop() {
      torches.clear();
      JNILights.clear();
      destroy();

      for(int var0 = 0; var0 < updateCounter.length; ++var0) {
         updateCounter[var0] = -1;
      }

      bWasElecShut = false;
      bWasNight = false;
      IsoLightSource.NextID = 1;
      IsoRoomLight.NextID = 1;
   }

   public static native void configure(float var0);

   public static native void scrollLeft(int var0);

   public static native void scrollRight(int var0);

   public static native void scrollUp(int var0);

   public static native void scrollDown(int var0);

   public static native void stateBeginUpdate(int var0, int var1, int var2, int var3, int var4);

   public static native void stateEndFrame(float var0, float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, float var8, int var9);

   public static native void stateEndUpdate();

   public static native int stateUpdateCounter(int var0);

   public static native void teleport(int var0, int var1, int var2);

   public static native void DoLightingUpdateNew(long var0, boolean var2);

   public static native boolean WaitingForMain();

   public static native void playerSet(float var0, float var1, float var2, float var3, float var4, boolean var5, boolean var6, boolean var7, boolean var8, float var9, float var10, float var11);

   public static native boolean chunkLightingDone(int var0, int var1);

   public static native boolean getChunkDirty(int var0, int var1, int var2, int var3);

   public static native void chunkBeginUpdate(int var0, int var1, int var2, int var3);

   public static native void chunkEndUpdate();

   public static native void chunkLevelBeginUpdate(int var0);

   public static native void chunkLevelEndUpdate();

   public static native void squareSetNull(int var0, int var1, int var2);

   public static native void squareBeginUpdate(int var0, int var1, int var2);

   public static native void squareSet(int var0, boolean var1, boolean var2, boolean var3, int var4, int var5, int var6, int var7, boolean var8);

   public static native void squareAddCurtain(int var0, boolean var1);

   public static native void squareAddDoor(boolean var0, boolean var1, boolean var2);

   public static native void squareAddThumpable(boolean var0, boolean var1, boolean var2, boolean var3);

   public static native void squareAddWindow(boolean var0, boolean var1, boolean var2);

   public static native void squareEndUpdate();

   public static native int getVertLight(int var0, int var1, int var2, int var3, int var4);

   public static native float getLightInfo(int var0, int var1, int var2, int var3, int var4);

   public static native float getDarkMulti(int var0, int var1, int var2, int var3);

   public static native float getTargetDarkMulti(int var0, int var1, int var2, int var3);

   public static native boolean getSeen(int var0, int var1, int var2, int var3);

   public static native boolean getCanSee(int var0, int var1, int var2, int var3);

   public static native boolean getCouldSee(int var0, int var1, int var2, int var3);

   public static native boolean getSquareLighting(int var0, int var1, int var2, int var3, int[] var4);

   public static native boolean getSquareDirty(int var0, int var1, int var2, int var3);

   public static native void addLight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, int var8, boolean var9);

   public static native void addTempLight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, int var8);

   public static native void removeLight(int var0);

   public static native void setLightActive(int var0, boolean var1);

   public static native void setLightColor(int var0, float var1, float var2, float var3);

   public static native void addRoomLight(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8);

   public static native void removeRoomLight(int var0);

   public static native void setRoomLightActive(int var0, boolean var1);

   public static native void updateTorch(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8, float var9, int var10);

   public static native void removeTorch(int var0);

   public static native void destroy();

   public static final class JNILighting implements IsoGridSquare.ILighting {
      private static final int RESULT_LIGHTS_PER_SQUARE = 6;
      private static final int[] lightInts = new int[49];
      private static final byte VIS_SEEN = 1;
      private static final byte VIS_CAN_SEE = 2;
      private static final byte VIS_COULD_SEE = 4;
      private int playerIndex;
      private final IsoGridSquare square;
      private ColorInfo lightInfo = new ColorInfo();
      private byte vis;
      private float cacheDarkMulti;
      private float cacheTargetDarkMulti;
      private final int[] cacheVertLight = new int[8];
      private int updateTick = -1;
      private int lightsCount;
      private IsoGridSquare.ResultLight[] lights;
      private int lightLevel;
      static int notDirty = 0;
      static int dirty = 0;

      public JNILighting(int var1, IsoGridSquare var2) {
         this.playerIndex = var1;
         this.square = var2;
         this.cacheDarkMulti = 0.0F;
         this.cacheTargetDarkMulti = 0.0F;

         for(int var3 = 0; var3 < 8; ++var3) {
            this.cacheVertLight[var3] = -16777216;
         }

      }

      public int lightverts(int var1) {
         return this.cacheVertLight[var1];
      }

      public float lampostTotalR() {
         return 0.0F;
      }

      public float lampostTotalG() {
         return 0.0F;
      }

      public float lampostTotalB() {
         return 0.0F;
      }

      public boolean bSeen() {
         this.update();
         return (this.vis & 1) != 0;
      }

      public boolean bCanSee() {
         this.update();
         return (this.vis & 2) != 0;
      }

      public boolean bCouldSee() {
         this.update();
         return (this.vis & 4) != 0;
      }

      public float darkMulti() {
         return this.cacheDarkMulti;
      }

      public float targetDarkMulti() {
         return this.cacheTargetDarkMulti;
      }

      public ColorInfo lightInfo() {
         this.update();
         return this.lightInfo;
      }

      public void lightverts(int var1, int var2) {
         throw new IllegalStateException();
      }

      public void lampostTotalR(float var1) {
         throw new IllegalStateException();
      }

      public void lampostTotalG(float var1) {
         throw new IllegalStateException();
      }

      public void lampostTotalB(float var1) {
         throw new IllegalStateException();
      }

      public void bSeen(boolean var1) {
         if (var1) {
            this.vis = (byte)(this.vis | 1);
         } else {
            this.vis &= -2;
         }

      }

      public void bCanSee(boolean var1) {
         throw new IllegalStateException();
      }

      public void bCouldSee(boolean var1) {
         throw new IllegalStateException();
      }

      public void darkMulti(float var1) {
         throw new IllegalStateException();
      }

      public void targetDarkMulti(float var1) {
         throw new IllegalStateException();
      }

      public int resultLightCount() {
         return this.lightsCount;
      }

      public IsoGridSquare.ResultLight getResultLight(int var1) {
         return this.lights[var1];
      }

      public void reset() {
         this.updateTick = -1;
         Arrays.fill(this.cacheVertLight, -16777216);
         this.vis = 0;
         this.cacheDarkMulti = 0.0F;
         this.cacheTargetDarkMulti = 0.0F;
         this.lightLevel = 0;
         this.lightInfo.set(0.0F, 0.0F, 0.0F, 1.0F);
      }

      private void update() {
         if (PerformanceSettings.FBORenderChunk) {
            this.updateFBORenderChunk();
         } else if (this.playerIndex == -1 || LightingJNI.updateCounter[this.playerIndex] != -1) {
            if (this.playerIndex == -1 || this.updateTick != LightingJNI.updateCounter[this.playerIndex] && LightingJNI.getSquareDirty(this.playerIndex, this.square.x, this.square.y, this.square.z + 32) && LightingJNI.getSquareLighting(this.playerIndex, this.square.x, this.square.y, this.square.z + 32, lightInts)) {
               IsoPlayer var1 = null;
               if (this.playerIndex != -1) {
                  var1 = IsoPlayer.players[this.playerIndex];
               }

               boolean var2 = (this.vis & 1) != 0;
               int var3 = 0;
               this.vis = (byte)(lightInts[var3++] & 7);
               this.lightInfo.r = (float)(lightInts[var3] & 255) / 255.0F;
               this.lightInfo.g = (float)(lightInts[var3] >> 8 & 255) / 255.0F;
               this.lightInfo.b = (float)(lightInts[var3++] >> 16 & 255) / 255.0F;
               this.lightInfo.a = 1.0F;
               this.cacheDarkMulti = (float)lightInts[var3++] / 100000.0F;
               this.cacheTargetDarkMulti = (float)lightInts[var3++] / 100000.0F;
               this.lightLevel = lightInts[var3++];
               this.square.lightLevel = this.lightLevel;
               float var4 = 1.0F;
               float var5 = 1.0F;
               int var6;
               int var7;
               if (var1 != null) {
                  var6 = this.square.z - PZMath.fastfloor(var1.getZ());
                  if (var6 == -1) {
                     var4 = 1.0F;
                     var5 = 0.85F;
                  } else if (var6 < -1) {
                     var4 = 0.85F;
                     var5 = 0.85F;
                  }

                  if ((this.vis & 2) == 0 && (this.vis & 4) != 0) {
                     var7 = PZMath.fastfloor(var1.getX());
                     int var8 = PZMath.fastfloor(var1.getY());
                     int var9 = this.square.x - var7;
                     int var10 = this.square.y - var8;
                     if (var1.dir != IsoDirections.Max && Math.abs(var9) <= 2 && Math.abs(var10) <= 2) {
                        int[] var11 = LightingJNI.ForcedVis[var1.dir.index()];

                        for(int var12 = 0; var12 < var11.length; var12 += 2) {
                           if (var9 == var11[var12] && var10 == var11[var12 + 1]) {
                              this.vis = (byte)(this.vis | 2);
                              break;
                           }
                        }
                     }
                  }
               }

               float var13;
               float var15;
               float var16;
               for(var6 = 0; var6 < 4; ++var6) {
                  var7 = lightInts[var3++];
                  var13 = (float)(var7 & 255) * var5;
                  var15 = (float)((var7 & '\uff00') >> 8) * var5;
                  var16 = (float)((var7 & 16711680) >> 16) * var5;
                  this.cacheVertLight[var6] = (int)var13 << 0 | (int)var15 << 8 | (int)var16 << 16 | -16777216;
               }

               for(var6 = 4; var6 < 8; ++var6) {
                  var7 = lightInts[var3++];
                  var13 = (float)(var7 & 255) * var4;
                  var15 = (float)((var7 & '\uff00') >> 8) * var4;
                  var16 = (float)((var7 & 16711680) >> 16) * var4;
                  this.cacheVertLight[var6] = (int)var13 << 0 | (int)var15 << 8 | (int)var16 << 16 | -16777216;
               }

               this.lightsCount = lightInts[var3++];

               for(var6 = 0; var6 < this.lightsCount; ++var6) {
                  if (this.lights == null) {
                     this.lights = new IsoGridSquare.ResultLight[6];
                  }

                  if (this.lights[var6] == null) {
                     this.lights[var6] = new IsoGridSquare.ResultLight();
                  }

                  this.lights[var6].id = lightInts[var3++];
                  this.lights[var6].x = lightInts[var3++];
                  this.lights[var6].y = lightInts[var3++];
                  this.lights[var6].z = lightInts[var3++] - 32;
                  this.lights[var6].radius = lightInts[var3++];
                  var7 = lightInts[var3++];
                  this.lights[var6].r = (float)(var7 & 255) / 255.0F;
                  this.lights[var6].g = (float)(var7 >> 8 & 255) / 255.0F;
                  this.lights[var6].b = (float)(var7 >> 16 & 255) / 255.0F;
                  this.lights[var6].flags = var7 >> 24 & 255;
               }

               if (this.playerIndex == -1) {
                  return;
               }

               this.updateTick = LightingJNI.updateCounter[this.playerIndex];
               if ((this.vis & 1) != 0) {
                  if (var2 && this.square.getRoom() != null && this.square.getRoom().def != null && !this.square.getRoom().def.bExplored) {
                     boolean var14 = true;
                  }

                  this.square.checkRoomSeen(this.playerIndex);
                  if (!var2) {
                     assert !GameServer.bServer;

                     if (!GameClient.bClient) {
                        Meta.instance.dealWithSquareSeen(this.square);
                     }
                  }
               } else if (this.square.getRoom() != null && this.square.getRoom().def != null && !this.square.getRoom().def.bExplored && IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), (float)this.square.x + 0.5F, (float)this.square.y + 0.5F) < 3.0F) {
                  this.square.checkRoomSeen(this.playerIndex);
               }
            }

         }
      }

      private void updateFBORenderChunk() {
         if (this.square.chunk != null) {
            if (LightingJNI.updateCounter[this.playerIndex] != -1) {
               if (this.updateTick != LightingJNI.updateCounter[this.playerIndex]) {
                  if (!LightingJNI.getSquareDirty(this.playerIndex, this.square.x, this.square.y, this.square.z + 32)) {
                     ++notDirty;
                  } else {
                     ++dirty;
                     if (LightingJNI.getSquareLighting(this.playerIndex, this.square.x, this.square.y, this.square.z + 32, lightInts)) {
                        IsoPlayer var1 = IsoPlayer.players[this.playerIndex];
                        boolean var2 = (this.vis & 2) != 0;
                        boolean var3 = (this.vis & 4) != 0;
                        boolean var4 = (this.vis & 1) != 0;
                        int var5 = 0;
                        this.vis = (byte)(lightInts[var5++] & 7);
                        int var6 = (int)(this.lightInfo.r * 255.0F);
                        int var7 = (int)(this.lightInfo.g * 255.0F);
                        int var8 = (int)(this.lightInfo.b * 255.0F);
                        int var9 = (int)(this.cacheDarkMulti * 1000.0F);
                        int var10 = (int)(this.cacheTargetDarkMulti * 1000.0F);
                        int var11 = this.lightLevel;
                        this.lightInfo.r = (float)(lightInts[var5] & 255) / 255.0F;
                        this.lightInfo.g = (float)(lightInts[var5] >> 8 & 255) / 255.0F;
                        this.lightInfo.b = (float)(lightInts[var5++] >> 16 & 255) / 255.0F;
                        this.lightInfo.a = 1.0F;
                        this.cacheDarkMulti = (float)lightInts[var5++] / 100000.0F;
                        this.cacheTargetDarkMulti = (float)lightInts[var5++] / 100000.0F;
                        this.lightLevel = lightInts[var5++];
                        this.square.lightLevel = this.lightLevel;
                        int var12;
                        int var13;
                        int var14;
                        int var15;
                        int var17;
                        if (var1 != null && (this.vis & 2) == 0 && (this.vis & 4) != 0) {
                           var12 = PZMath.fastfloor(var1.getX());
                           var13 = PZMath.fastfloor(var1.getY());
                           var14 = this.square.x - var12;
                           var15 = this.square.y - var13;
                           if (var1.dir != IsoDirections.Max && Math.abs(var14) <= 2 && Math.abs(var15) <= 2) {
                              int[] var16 = LightingJNI.ForcedVis[var1.dir.index()];

                              for(var17 = 0; var17 < var16.length; var17 += 2) {
                                 if (var14 == var16[var17] && var15 == var16[var17 + 1]) {
                                    this.vis = (byte)(this.vis | 2);
                                    break;
                                 }
                              }
                           }
                        }

                        var12 = this.cacheVertLight[0];
                        var13 = this.cacheVertLight[1];
                        var14 = this.cacheVertLight[2];
                        var15 = this.cacheVertLight[3];
                        int var37 = this.cacheVertLight[4];
                        var17 = this.cacheVertLight[5];
                        int var18 = this.cacheVertLight[6];
                        int var19 = this.cacheVertLight[7];

                        int var20;
                        for(var20 = 0; var20 < 8; ++var20) {
                           this.cacheVertLight[var20] = lightInts[var5++];
                        }

                        var20 = (int)(this.lightInfo.r * 255.0F);
                        int var21 = (int)(this.lightInfo.g * 255.0F);
                        int var22 = (int)(this.lightInfo.b * 255.0F);
                        int var23 = (int)(this.cacheDarkMulti * 1000.0F);
                        int var24 = (int)(this.cacheTargetDarkMulti * 1000.0F);
                        int var25 = this.lightLevel;
                        int var26 = this.cacheVertLight[0];
                        int var27 = this.cacheVertLight[1];
                        int var28 = this.cacheVertLight[2];
                        int var29 = this.cacheVertLight[3];
                        int var30 = this.cacheVertLight[4];
                        int var31 = this.cacheVertLight[5];
                        int var32 = this.cacheVertLight[6];
                        int var33 = this.cacheVertLight[7];
                        FBORenderLevels var34 = this.square.chunk.getRenderLevels(this.playerIndex);
                        if (var23 == var9 && var24 == var10 && var25 == var11 && var20 == var6 && var21 == var7 && var22 == var8) {
                           if (var26 == var12 && var27 == var13 && var28 == var14 && var29 == var15 && var30 == var37 && var31 == var17 && var32 == var18 && var33 == var19) {
                              if (var3 != ((this.vis & 4) != 0)) {
                                 FBORenderCutaways.getInstance().squareChanged(this.square);
                              }
                           } else if (!DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
                              var34.invalidateLevel(this.square.z, FBORenderChunk.DIRTY_LIGHTING);
                           }
                        } else if (!DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
                           var34.invalidateLevel(this.square.z, FBORenderChunk.DIRTY_LIGHTING);
                        }

                        this.lightsCount = lightInts[var5++];

                        for(int var35 = 0; var35 < this.lightsCount; ++var35) {
                           if (this.lights == null) {
                              this.lights = new IsoGridSquare.ResultLight[6];
                           }

                           if (this.lights[var35] == null) {
                              this.lights[var35] = new IsoGridSquare.ResultLight();
                           }

                           this.lights[var35].id = lightInts[var5++];
                           this.lights[var35].x = lightInts[var5++];
                           this.lights[var35].y = lightInts[var5++];
                           this.lights[var35].z = lightInts[var5++] - 32;
                           this.lights[var35].radius = lightInts[var5++];
                           int var36 = lightInts[var5++];
                           this.lights[var35].r = (float)(var36 & 255) / 255.0F;
                           this.lights[var35].g = (float)(var36 >> 8 & 255) / 255.0F;
                           this.lights[var35].b = (float)(var36 >> 16 & 255) / 255.0F;
                           this.lights[var35].flags = var36 >> 24 & 255;
                        }

                        if (this.updateTick == -1 && var34.isOnScreen(this.square.z)) {
                           var34.invalidateLevel(this.square.z, FBORenderChunk.DIRTY_LIGHTING);
                        }

                        this.updateTick = LightingJNI.updateCounter[this.playerIndex];
                        if ((this.vis & 1) != 0) {
                           this.square.checkRoomSeen(this.playerIndex);
                           if (!var4) {
                              assert !GameServer.bServer;

                              if (!GameClient.bClient) {
                                 Meta.instance.dealWithSquareSeen(this.square);
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
