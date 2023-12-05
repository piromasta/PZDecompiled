package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.properties.PropertyContainer;
import zombie.debug.DebugLog;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWheelieBin;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoDirectionFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class CellLoader {
   public static final ArrayDeque<IsoObject> isoObjectCache = new ArrayDeque();
   public static final ArrayDeque<IsoTree> isoTreeCache = new ArrayDeque();
   static int wanderX = 0;
   static int wanderY = 0;
   static IsoRoom wanderRoom = null;
   static final HashSet<String> missingTiles = new HashSet();
   public static final HashMap<IsoSprite, IsoSprite> smashedWindowSpriteMap = new HashMap();

   public CellLoader() {
   }

   public static void DoTileObjectCreation(IsoSprite var0, IsoObjectType var1, IsoGridSquare var2, IsoCell var3, int var4, int var5, int var6, String var7) throws NumberFormatException {
      Object var8 = null;
      if (var2 != null) {
         boolean var9 = false;
         if (smashedWindowSpriteMap.containsKey(var0)) {
            var0 = (IsoSprite)smashedWindowSpriteMap.get(var0);
            var1 = var0.getType();
            var9 = true;
         }

         PropertyContainer var10 = var0.getProperties();
         IsoObject var11;
         if (var0.solidfloor && var10.Is(IsoFlagType.diamondFloor) && !var10.Is(IsoFlagType.transparentFloor)) {
            var11 = var2.getFloor();
            if (var11 != null && var11.getProperties().Is(IsoFlagType.diamondFloor)) {
               var11.clearAttachedAnimSprite();
               var11.setSprite(var0);
               return;
            }
         }

         if (var1 != IsoObjectType.doorW && var1 != IsoObjectType.doorN) {
            float var13;
            if (var1 == IsoObjectType.lightswitch) {
               var8 = new IsoLightSwitch(var3, var2, var0, var2.getRoomID());
               AddObject(var2, (IsoObject)var8);
               GameClient.instance.objectSyncReq.putRequest(var2, (IsoObject)var8);
               if (((IsoObject)var8).sprite.getProperties().Is("lightR")) {
                  float var27 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightR")) / 255.0F;
                  float var24 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightG")) / 255.0F;
                  var13 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightB")) / 255.0F;
                  int var25 = 10;
                  if (((IsoObject)var8).sprite.getProperties().Is("LightRadius") && Integer.parseInt(((IsoObject)var8).sprite.getProperties().Val("LightRadius")) > 0) {
                     var25 = Integer.parseInt(((IsoObject)var8).sprite.getProperties().Val("LightRadius"));
                  }

                  IsoLightSource var26 = new IsoLightSource(((IsoObject)var8).square.getX(), ((IsoObject)var8).square.getY(), ((IsoObject)var8).square.getZ(), var27, var24, var13, var25);
                  var26.bActive = true;
                  var26.bHydroPowered = true;
                  var26.switches.add((IsoLightSwitch)var8);
                  ((IsoLightSwitch)var8).lights.add(var26);
               } else {
                  ((IsoLightSwitch)var8).lightRoom = true;
               }
            } else if (var1 != IsoObjectType.curtainN && var1 != IsoObjectType.curtainS && var1 != IsoObjectType.curtainE && var1 != IsoObjectType.curtainW) {
               if (!var0.getProperties().Is(IsoFlagType.windowW) && !var0.getProperties().Is(IsoFlagType.windowN)) {
                  if (var0.getProperties().Is(IsoFlagType.container) && var0.getProperties().Val("container").equals("barbecue")) {
                     var8 = new IsoBarbecue(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                  } else if (var0.getProperties().Is(IsoFlagType.container) && var0.getProperties().Val("container").equals("fireplace")) {
                     var8 = new IsoFireplace(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                  } else if ("IsoCombinationWasherDryer".equals(var0.getProperties().Val("IsoType"))) {
                     var8 = new IsoCombinationWasherDryer(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                  } else if (var0.getProperties().Is(IsoFlagType.container) && var0.getProperties().Val("container").equals("clothingdryer")) {
                     var8 = new IsoClothingDryer(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                  } else if (var0.getProperties().Is(IsoFlagType.container) && var0.getProperties().Val("container").equals("clothingwasher")) {
                     var8 = new IsoClothingWasher(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                  } else if (var0.getProperties().Is(IsoFlagType.container) && var0.getProperties().Val("container").equals("woodstove")) {
                     var8 = new IsoFireplace(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                  } else if (!var0.getProperties().Is(IsoFlagType.container) || !var0.getProperties().Val("container").equals("stove") && !var0.getProperties().Val("container").equals("microwave")) {
                     if (var1 == IsoObjectType.jukebox) {
                        var8 = new IsoJukebox(var3, var2, var0);
                        ((IsoObject)var8).OutlineOnMouseover = true;
                        AddObject(var2, (IsoObject)var8);
                     } else if (var1 == IsoObjectType.radio) {
                        var8 = new IsoRadio(var3, var2, var0);
                        AddObject(var2, (IsoObject)var8);
                     } else {
                        String var19;
                        if (var0.getProperties().Is("signal")) {
                           var19 = var0.getProperties().Val("signal");
                           if ("radio".equals(var19)) {
                              var8 = new IsoRadio(var3, var2, var0);
                           } else if ("tv".equals(var19)) {
                              var8 = new IsoTelevision(var3, var2, var0);
                           }

                           AddObject(var2, (IsoObject)var8);
                        } else {
                           if (var0.getProperties().Is(IsoFlagType.WallOverlay)) {
                              var11 = null;
                              if (var0.getProperties().Is(IsoFlagType.attachedSE)) {
                                 var11 = var2.getWallSE();
                              } else if (var0.getProperties().Is(IsoFlagType.attachedW)) {
                                 var11 = var2.getWall(false);
                              } else if (var0.getProperties().Is(IsoFlagType.attachedN)) {
                                 var11 = var2.getWall(true);
                              } else {
                                 for(int var22 = var2.getObjects().size() - 1; var22 >= 0; --var22) {
                                    IsoObject var23 = (IsoObject)var2.getObjects().get(var22);
                                    if (var23.sprite.getProperties().Is(IsoFlagType.cutW) || var23.sprite.getProperties().Is(IsoFlagType.cutN)) {
                                       var11 = var23;
                                       break;
                                    }
                                 }
                              }

                              if (var11 != null) {
                                 if (var11.AttachedAnimSprite == null) {
                                    var11.AttachedAnimSprite = new ArrayList(4);
                                 }

                                 var11.AttachedAnimSprite.add(IsoSpriteInstance.get(var0));
                              } else {
                                 IsoObject var16 = IsoObject.getNew();
                                 var16.sx = 0.0F;
                                 var16.sprite = var0;
                                 var16.square = var2;
                                 AddObject(var2, var16);
                              }

                              return;
                           }

                           if (var0.getProperties().Is(IsoFlagType.FloorOverlay)) {
                              var11 = var2.getFloor();
                              if (var11 != null) {
                                 if (var11.AttachedAnimSprite == null) {
                                    var11.AttachedAnimSprite = new ArrayList(4);
                                 }

                                 var11.AttachedAnimSprite.add(IsoSpriteInstance.get(var0));
                              }
                           } else if (IsoMannequin.isMannequinSprite(var0)) {
                              var8 = new IsoMannequin(var3, var2, var0);
                              AddObject(var2, (IsoObject)var8);
                           } else if (var1 == IsoObjectType.tree) {
                              if (var0.getName() != null && var0.getName().startsWith("vegetation_trees")) {
                                 var11 = var2.getFloor();
                                 if (var11 == null || var11.getSprite() == null || var11.getSprite().getName() == null || !var11.getSprite().getName().startsWith("blends_natural")) {
                                    DebugLog.log("ERROR: removed tree at " + var2.x + "," + var2.y + "," + var2.z + " because floor is not blends_natural");
                                    return;
                                 }
                              }

                              var8 = IsoTree.getNew();
                              ((IsoObject)var8).sprite = var0;
                              ((IsoObject)var8).square = var2;
                              ((IsoObject)var8).sx = 0.0F;
                              ((IsoTree)var8).initTree();

                              for(int var20 = 0; var20 < var2.getObjects().size(); ++var20) {
                                 IsoObject var21 = (IsoObject)var2.getObjects().get(var20);
                                 if (var21 instanceof IsoTree) {
                                    var2.getObjects().remove(var20);
                                    var21.reset();
                                    isoTreeCache.push((IsoTree)var21);
                                    break;
                                 }
                              }

                              AddObject(var2, (IsoObject)var8);
                           } else {
                              if ((var0.CurrentAnim.Frames.isEmpty() || ((IsoDirectionFrame)var0.CurrentAnim.Frames.get(0)).getTexture(IsoDirections.N) == null) && !GameServer.bServer) {
                                 if (!missingTiles.contains(var7)) {
                                    if (Core.bDebug) {
                                       DebugLog.General.error("CellLoader> missing tile " + var7);
                                    }

                                    missingTiles.add(var7);
                                 }

                                 var0.LoadFramesNoDirPageSimple(Core.bDebug ? "media/ui/missing-tile-debug.png" : "media/ui/missing-tile.png");
                                 if (var0.CurrentAnim.Frames.isEmpty() || ((IsoDirectionFrame)var0.CurrentAnim.Frames.get(0)).getTexture(IsoDirections.N) == null) {
                                    return;
                                 }
                              }

                              var19 = GameServer.bServer ? null : ((IsoDirectionFrame)var0.CurrentAnim.Frames.get(0)).getTexture(IsoDirections.N).getName();
                              boolean var12 = true;
                              if (!GameServer.bServer && var19.contains("TileObjectsExt") && (var19.contains("_5") || var19.contains("_6") || var19.contains("_7") || var19.contains("_8"))) {
                                 var8 = new IsoWheelieBin(var3, var4, var5, var6);
                                 if (var19.contains("_5")) {
                                    ((IsoObject)var8).dir = IsoDirections.S;
                                 }

                                 if (var19.contains("_6")) {
                                    ((IsoObject)var8).dir = IsoDirections.W;
                                 }

                                 if (var19.contains("_7")) {
                                    ((IsoObject)var8).dir = IsoDirections.N;
                                 }

                                 if (var19.contains("_8")) {
                                    ((IsoObject)var8).dir = IsoDirections.E;
                                 }

                                 var12 = false;
                              }

                              if (var12) {
                                 var8 = IsoObject.getNew();
                                 ((IsoObject)var8).sx = 0.0F;
                                 ((IsoObject)var8).sprite = var0;
                                 ((IsoObject)var8).square = var2;
                                 AddObject(var2, (IsoObject)var8);
                                 if (((IsoObject)var8).sprite.getProperties().Is("lightR")) {
                                    var13 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightR"));
                                    float var14 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightG"));
                                    float var15 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightB"));
                                    var3.getLamppostPositions().add(new IsoLightSource(((IsoObject)var8).square.getX(), ((IsoObject)var8).square.getY(), ((IsoObject)var8).square.getZ(), var13, var14, var15, 8));
                                 }
                              }
                           }
                        }
                     }
                  } else {
                     var8 = new IsoStove(var3, var2, var0);
                     AddObject(var2, (IsoObject)var8);
                     GameClient.instance.objectSyncReq.putRequest(var2, (IsoObject)var8);
                  }
               } else {
                  var8 = new IsoWindow(var3, var2, var0, var0.getProperties().Is(IsoFlagType.windowN));
                  if (var9) {
                     ((IsoWindow)var8).setSmashed(true);
                  }

                  AddSpecialObject(var2, (IsoObject)var8);
                  GameClient.instance.objectSyncReq.putRequest(var2, (IsoObject)var8);
               }
            } else {
               boolean var18 = Integer.parseInt(var7.substring(var7.lastIndexOf("_") + 1)) % 8 <= 3;
               var8 = new IsoCurtain(var3, var2, var0, var1 == IsoObjectType.curtainN || var1 == IsoObjectType.curtainS, var18);
               AddSpecialObject(var2, (IsoObject)var8);
               GameClient.instance.objectSyncReq.putRequest(var2, (IsoObject)var8);
            }
         } else {
            IsoDoor var17 = new IsoDoor(var3, var2, var0, var1 == IsoObjectType.doorN);
            var8 = var17;
            AddSpecialObject(var2, var17);
            if (var0.getProperties().Is("DoubleDoor")) {
               var17.Locked = false;
               var17.lockedByKey = false;
            }

            if (var0.getProperties().Is("GarageDoor")) {
               var17.Locked = !var17.IsOpen();
               var17.lockedByKey = false;
            }

            GameClient.instance.objectSyncReq.putRequest(var2, var17);
         }

         if (var8 != null) {
            ((IsoObject)var8).tile = var7;
            ((IsoObject)var8).createContainersFromSpriteProperties();
            if (((IsoObject)var8).sprite.getProperties().Is(IsoFlagType.vegitation)) {
               ((IsoObject)var8).tintr = 0.7F + (float)Rand.Next(30) / 100.0F;
               ((IsoObject)var8).tintg = 0.7F + (float)Rand.Next(30) / 100.0F;
               ((IsoObject)var8).tintb = 0.7F + (float)Rand.Next(30) / 100.0F;
            }
         }

      }
   }

   public static boolean LoadCellBinaryChunk(IsoCell var0, int var1, int var2, IsoChunk var3) {
      int var4 = var1;
      int var5 = var2;
      String var6 = "world_" + var1 / 30 + "_" + var2 / 30 + ".lotpack";
      if (!IsoLot.InfoFileNames.containsKey(var6)) {
         DebugLog.log("LoadCellBinaryChunk: NO SUCH LOT " + var6);
         return false;
      } else {
         File var7 = new File((String)IsoLot.InfoFileNames.get(var6));
         if (var7.exists()) {
            IsoLot var8 = null;

            try {
               var8 = IsoLot.get(var4 / 30, var5 / 30, var1, var2, var3);
               var0.PlaceLot(var8, 0, 0, 0, var3, var1, var2);
            } finally {
               if (var8 != null) {
                  IsoLot.put(var8);
               }

            }

            return true;
         } else {
            DebugLog.log("LoadCellBinaryChunk: NO SUCH LOT " + var6);
            return false;
         }
      }
   }

   public static IsoCell LoadCellBinaryChunk(IsoSpriteManager var0, int var1, int var2) throws IOException {
      wanderX = 0;
      wanderY = 0;
      wanderRoom = null;
      wanderX = 0;
      wanderY = 0;
      IsoCell var3 = new IsoCell(300, 300);
      int var4 = IsoPlayer.numPlayers;
      byte var12 = 1;
      if (!GameServer.bServer) {
         if (GameClient.bClient) {
            WorldStreamer.instance.requestLargeAreaZip(var1, var2, IsoChunkMap.ChunkGridWidth / 2 + 2);
            IsoChunk.bDoServerRequests = false;
         }

         for(int var5 = 0; var5 < var12; ++var5) {
            var3.ChunkMap[var5].setInitialPos(var1, var2);
            IsoPlayer.assumedPlayer = var5;
            IsoChunkMap var10001 = var3.ChunkMap[var5];
            int var6 = var1 - IsoChunkMap.ChunkGridWidth / 2;
            var10001 = var3.ChunkMap[var5];
            int var7 = var2 - IsoChunkMap.ChunkGridWidth / 2;
            var10001 = var3.ChunkMap[var5];
            int var8 = var1 + IsoChunkMap.ChunkGridWidth / 2 + 1;
            var10001 = var3.ChunkMap[var5];
            int var9 = var2 + IsoChunkMap.ChunkGridWidth / 2 + 1;

            for(int var10 = var6; var10 < var8; ++var10) {
               for(int var11 = var7; var11 < var9; ++var11) {
                  if (IsoWorld.instance.getMetaGrid().isValidChunk(var10, var11)) {
                     var3.ChunkMap[var5].LoadChunk(var10, var11, var10 - var6, var11 - var7);
                  }
               }
            }
         }
      }

      IsoPlayer.assumedPlayer = 0;
      LuaEventManager.triggerEvent("OnPostMapLoad", var3, var1, var2);
      ConnectMultitileObjects(var3);
      return var3;
   }

   private static void RecurseMultitileObjects(IsoCell var0, IsoGridSquare var1, IsoGridSquare var2, ArrayList<IsoPushableObject> var3) {
      Iterator var4 = var2.getMovingObjects().iterator();
      IsoPushableObject var5 = null;
      boolean var6 = false;

      while(var4 != null && var4.hasNext()) {
         IsoMovingObject var7 = (IsoMovingObject)var4.next();
         if (var7 instanceof IsoPushableObject var8) {
            int var9 = var1.getX() - var2.getX();
            int var10 = var1.getY() - var2.getY();
            int var11;
            if (var10 != 0 && var7.sprite.getProperties().Is("connectY")) {
               var11 = Integer.parseInt(var7.sprite.getProperties().Val("connectY"));
               if (var11 == var10) {
                  var8.connectList = var3;
                  var3.add(var8);
                  var5 = var8;
                  var6 = false;
                  break;
               }
            }

            if (var9 != 0 && var7.sprite.getProperties().Is("connectX")) {
               var11 = Integer.parseInt(var7.sprite.getProperties().Val("connectX"));
               if (var11 == var9) {
                  var8.connectList = var3;
                  var3.add(var8);
                  var5 = var8;
                  var6 = true;
                  break;
               }
            }
         }
      }

      if (var5 != null) {
         int var12;
         IsoGridSquare var13;
         if (var5.sprite.getProperties().Is("connectY") && var6) {
            var12 = Integer.parseInt(var5.sprite.getProperties().Val("connectY"));
            var13 = var0.getGridSquare(var5.getCurrentSquare().getX(), var5.getCurrentSquare().getY() + var12, var5.getCurrentSquare().getZ());
            RecurseMultitileObjects(var0, var5.getCurrentSquare(), var13, var5.connectList);
         }

         if (var5.sprite.getProperties().Is("connectX") && !var6) {
            var12 = Integer.parseInt(var5.sprite.getProperties().Val("connectX"));
            var13 = var0.getGridSquare(var5.getCurrentSquare().getX() + var12, var5.getCurrentSquare().getY(), var5.getCurrentSquare().getZ());
            RecurseMultitileObjects(var0, var5.getCurrentSquare(), var13, var5.connectList);
         }
      }

   }

   private static void ConnectMultitileObjects(IsoCell var0) {
      Iterator var1 = var0.getObjectList().iterator();

      while(var1 != null && var1.hasNext()) {
         IsoMovingObject var2 = (IsoMovingObject)var1.next();
         if (var2 instanceof IsoPushableObject var3) {
            if ((var2.sprite.getProperties().Is("connectY") || var2.sprite.getProperties().Is("connectX")) && var3.connectList == null) {
               var3.connectList = new ArrayList();
               var3.connectList.add(var3);
               int var4;
               IsoGridSquare var5;
               if (var2.sprite.getProperties().Is("connectY")) {
                  var4 = Integer.parseInt(var2.sprite.getProperties().Val("connectY"));
                  var5 = var0.getGridSquare(var2.getCurrentSquare().getX(), var2.getCurrentSquare().getY() + var4, var2.getCurrentSquare().getZ());
                  if (var5 == null) {
                     boolean var6 = false;
                  }

                  RecurseMultitileObjects(var0, var3.getCurrentSquare(), var5, var3.connectList);
               }

               if (var2.sprite.getProperties().Is("connectX")) {
                  var4 = Integer.parseInt(var2.sprite.getProperties().Val("connectX"));
                  var5 = var0.getGridSquare(var2.getCurrentSquare().getX() + var4, var2.getCurrentSquare().getY(), var2.getCurrentSquare().getZ());
                  RecurseMultitileObjects(var0, var3.getCurrentSquare(), var5, var3.connectList);
               }
            }
         }
      }

   }

   private static void AddObject(IsoGridSquare var0, IsoObject var1) {
      int var2 = var0.placeWallAndDoorCheck(var1, var0.getObjects().size());
      if (var2 != var0.getObjects().size() && var2 >= 0 && var2 <= var0.getObjects().size()) {
         var0.getObjects().add(var2, var1);
      } else {
         var0.getObjects().add(var1);
      }

   }

   private static void AddSpecialObject(IsoGridSquare var0, IsoObject var1) {
      int var2 = var0.placeWallAndDoorCheck(var1, var0.getObjects().size());
      if (var2 != var0.getObjects().size() && var2 >= 0 && var2 <= var0.getObjects().size()) {
         var0.getObjects().add(var2, var1);
      } else {
         var0.getObjects().add(var1);
         var0.getSpecialObjects().add(var1);
      }

   }
}
