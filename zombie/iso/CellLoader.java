package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import zombie.ChunkMapFilenames;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.GameEntityFactory;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.enums.ChunkGenerationStatus;
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
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
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
   public static final HashMap<IsoSprite, IsoSprite> glassRemovedWindowSpriteMap = new HashMap();
   public static final HashMap<IsoSprite, IsoSprite> smashedWindowSpriteMap = new HashMap();

   public CellLoader() {
   }

   public static void DoTileObjectCreation(IsoSprite var0, IsoObjectType var1, IsoGridSquare var2, IsoCell var3, int var4, int var5, int var6, String var7) throws NumberFormatException {
      Object var8 = null;
      if (var2 != null) {
         boolean var9 = false;
         boolean var10 = false;
         if (glassRemovedWindowSpriteMap.containsKey(var0)) {
            var0 = (IsoSprite)glassRemovedWindowSpriteMap.get(var0);
            var1 = var0.getType();
            var9 = true;
         } else if (smashedWindowSpriteMap.containsKey(var0)) {
            var0 = (IsoSprite)smashedWindowSpriteMap.get(var0);
            var1 = var0.getType();
            var10 = true;
         }

         PropertyContainer var11 = var0.getProperties();
         boolean var12 = var0.getProperties().valueEquals("container", "stove") || var0.getProperties().valueEquals("container", "toaster") || var0.getProperties().valueEquals("container", "coffeemaker");
         IsoObject var13;
         if (var0.solidfloor && var11.Is(IsoFlagType.diamondFloor) && !var11.Is(IsoFlagType.transparentFloor)) {
            var13 = var2.getFloor();
            if (var13 != null && var13.getProperties().Is(IsoFlagType.diamondFloor)) {
               var13.clearAttachedAnimSprite();
               var13.setSprite(var0);
               return;
            }
         }

         if (var1 != IsoObjectType.doorW && var1 != IsoObjectType.doorN) {
            if (var1 == IsoObjectType.lightswitch) {
               var8 = new IsoLightSwitch(var3, var2, var0, var2.getRoomID());
               AddObject(var2, (IsoObject)var8);
               if (((IsoObject)var8).sprite.getProperties().Is("lightR")) {
                  float var29 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightR")) / 255.0F;
                  float var25 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightG")) / 255.0F;
                  float var26 = Float.parseFloat(((IsoObject)var8).sprite.getProperties().Val("lightB")) / 255.0F;
                  int var16 = 10;
                  if (((IsoObject)var8).sprite.getProperties().Is("LightRadius") && Integer.parseInt(((IsoObject)var8).sprite.getProperties().Val("LightRadius")) > 0) {
                     var16 = Integer.parseInt(((IsoObject)var8).sprite.getProperties().Val("LightRadius"));
                  }

                  IsoLightSource var17 = new IsoLightSource(((IsoObject)var8).square.getX(), ((IsoObject)var8).square.getY(), ((IsoObject)var8).square.getZ(), var29, var25, var26, var16);
                  var17.bActive = true;
                  var17.bHydroPowered = true;
                  var17.switches.add((IsoLightSwitch)var8);
                  ((IsoLightSwitch)var8).lights.add(var17);
               } else {
                  ((IsoLightSwitch)var8).lightRoom = true;
               }
            } else {
               boolean var22;
               if (var1 != IsoObjectType.curtainN && var1 != IsoObjectType.curtainS && var1 != IsoObjectType.curtainE && var1 != IsoObjectType.curtainW) {
                  if (!var0.getProperties().Is(IsoFlagType.windowW) && !var0.getProperties().Is(IsoFlagType.windowN)) {
                     if (!var0.getProperties().Is(IsoFlagType.WindowW) && !var0.getProperties().Is(IsoFlagType.WindowN)) {
                        if (var0.getProperties().Is(IsoFlagType.container) && (var0.getProperties().Val("container").equals("barbecue") || var0.getProperties().Val("container").equals("barbecuepropane"))) {
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
                        } else if (!var0.getProperties().Is(IsoFlagType.container) || !var12 && !var0.getProperties().Val("container").equals("microwave")) {
                           if (var1 == IsoObjectType.jukebox) {
                              var8 = new IsoJukebox(var3, var2, var0);
                              ((IsoObject)var8).OutlineOnMouseover = true;
                              AddObject(var2, (IsoObject)var8);
                           } else if (var1 == IsoObjectType.radio) {
                              var8 = new IsoRadio(var3, var2, var0);
                              AddObject(var2, (IsoObject)var8);
                           } else if (var0.getProperties().Is("signal")) {
                              String var28 = var0.getProperties().Val("signal");
                              if ("radio".equals(var28)) {
                                 var8 = new IsoRadio(var3, var2, var0);
                              } else if ("tv".equals(var28)) {
                                 var8 = new IsoTelevision(var3, var2, var0);
                              }

                              AddObject(var2, (IsoObject)var8);
                           } else {
                              if (var0.getProperties().Is(IsoFlagType.WallOverlay)) {
                                 Object var27 = null;
                                 if (var0.getProperties().Is(IsoFlagType.attachedSE)) {
                                    var27 = var2.getWallSE();
                                 } else if (var0.getProperties().Is(IsoFlagType.attachedW)) {
                                    var27 = var2.getWall(false);
                                    if (var27 == null) {
                                       var27 = var2.getWindow(false);
                                       if (var27 != null && (((IsoObject)var27).getProperties() == null || !((IsoObject)var27).getProperties().Is(IsoFlagType.WindowW))) {
                                          var27 = null;
                                       }
                                    }

                                    if (var27 == null) {
                                       var27 = var2.getGarageDoor(false);
                                    }
                                 } else if (var0.getProperties().Is(IsoFlagType.attachedN)) {
                                    var27 = var2.getWall(true);
                                    if (var27 == null) {
                                       var27 = var2.getWindow(true);
                                       if (var27 != null && (((IsoObject)var27).getProperties() == null || !((IsoObject)var27).getProperties().Is(IsoFlagType.WindowN))) {
                                          var27 = null;
                                       }
                                    }

                                    if (var27 == null) {
                                       var27 = var2.getGarageDoor(true);
                                    }
                                 } else {
                                    for(int var23 = var2.getObjects().size() - 1; var23 >= 0; --var23) {
                                       IsoObject var15 = (IsoObject)var2.getObjects().get(var23);
                                       if (var15.sprite.getProperties().Is(IsoFlagType.cutW) || var15.sprite.getProperties().Is(IsoFlagType.cutN)) {
                                          var27 = var15;
                                          break;
                                       }
                                    }
                                 }

                                 if (var27 != null) {
                                    if (((IsoObject)var27).AttachedAnimSprite == null) {
                                       ((IsoObject)var27).AttachedAnimSprite = new ArrayList(4);
                                    }

                                    ((IsoObject)var27).AttachedAnimSprite.add(IsoSpriteInstance.get(var0));
                                 } else {
                                    IsoObject var20 = IsoObject.getNew();
                                    var20.sx = 0.0F;
                                    var20.sprite = var0;
                                    var20.square = var2;
                                    AddObject(var2, var20);
                                 }

                                 return;
                              }

                              if (var0.getProperties().Is(IsoFlagType.FloorOverlay)) {
                                 var13 = var2.getFloor();
                                 if (var13 != null) {
                                    if (var13.AttachedAnimSprite == null) {
                                       var13.AttachedAnimSprite = new ArrayList(4);
                                    }

                                    var13.AttachedAnimSprite.add(IsoSpriteInstance.get(var0));
                                 }
                              } else if (IsoMannequin.isMannequinSprite(var0)) {
                                 var8 = new IsoMannequin(var3, var2, var0);
                                 AddObject(var2, (IsoObject)var8);
                              } else if (var1 != IsoObjectType.tree) {
                                 if (var0.hasNoTextures() && !var0.invisible && !GameServer.bServer) {
                                    if (!missingTiles.contains(var7)) {
                                       if (Core.bDebug) {
                                          DebugLog.General.error("CellLoader> missing tile " + var7);
                                       }

                                       missingTiles.add(var7);
                                    }

                                    var0.LoadSingleTexture(Core.bDebug ? "media/ui/missing-tile-debug.png" : "media/ui/missing-tile.png");
                                    if (var0.hasNoTextures()) {
                                       return;
                                    }
                                 }

                                 var22 = true;
                                 if (var22) {
                                    var8 = IsoObject.getNew();
                                    ((IsoObject)var8).sx = 0.0F;
                                    ((IsoObject)var8).sprite = var0;
                                    ((IsoObject)var8).square = var2;
                                    AddObject(var2, (IsoObject)var8);
                                 }
                              } else {
                                 if (var0.getName() != null && var0.getName().startsWith("vegetation_trees")) {
                                    var13 = var2.getFloor();
                                    if (var13 == null || var13.getSprite() == null || var13.getSprite().getName() == null || !var13.getSprite().getName().startsWith("blends_natural")) {
                                       DebugLog.log("ERROR: removed tree at " + var2.x + "," + var2.y + "," + var2.z + " because floor is not blends_natural");
                                       return;
                                    }
                                 }

                                 var8 = IsoTree.getNew();
                                 ((IsoObject)var8).sprite = var0;
                                 ((IsoObject)var8).square = var2;
                                 ((IsoObject)var8).sx = 0.0F;
                                 ((IsoTree)var8).initTree();

                                 for(int var24 = 0; var24 < var2.getObjects().size(); ++var24) {
                                    IsoObject var14 = (IsoObject)var2.getObjects().get(var24);
                                    if (var14 instanceof IsoTree) {
                                       var2.getObjects().remove(var24);
                                       var14.reset();
                                       synchronized(isoTreeCache) {
                                          isoTreeCache.push((IsoTree)var14);
                                          break;
                                       }
                                    }
                                 }

                                 AddObject(var2, (IsoObject)var8);
                              }
                           }
                        } else {
                           var8 = new IsoStove(var3, var2, var0);
                           AddObject(var2, (IsoObject)var8);
                        }
                     } else {
                        var8 = new IsoWindowFrame(var3, var2, var0, var0.getProperties().Is(IsoFlagType.WindowN));
                        AddObject(var2, (IsoObject)var8);
                     }
                  } else {
                     var8 = new IsoWindow(var3, var2, var0, var0.getProperties().Is(IsoFlagType.windowN));
                     if (var9) {
                        ((IsoWindow)var8).setSmashed(true);
                        ((IsoWindow)var8).setGlassRemoved(true);
                     } else if (var10) {
                        ((IsoWindow)var8).setSmashed(true);
                     }

                     AddSpecialObject(var2, (IsoObject)var8);
                  }
               } else {
                  var22 = Integer.parseInt(var7.substring(var7.lastIndexOf("_") + 1)) % 8 <= 3;
                  var8 = new IsoCurtain(var3, var2, var0, var1 == IsoObjectType.curtainN || var1 == IsoObjectType.curtainS, var22);
                  AddSpecialObject(var2, (IsoObject)var8);
               }
            }
         } else {
            IsoDoor var21 = new IsoDoor(var3, var2, var0, var1 == IsoObjectType.doorN);
            var8 = var21;
            AddSpecialObject(var2, var21);
            if (var0.getProperties().Is("DoubleDoor")) {
               var21.Locked = false;
               var21.lockedByKey = false;
            }

            if (var0.getProperties().Is("GarageDoor")) {
               var21.Locked = !var21.IsOpen();
               var21.lockedByKey = false;
            }
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
      int var4 = PZMath.fastfloor((float)var1 / (float)IsoCell.CellSizeInChunks);
      int var5 = PZMath.fastfloor((float)var2 / (float)IsoCell.CellSizeInChunks);
      String var6 = "world_" + var4 + "_" + var5 + ".lotpack";
      if (!IsoLot.InfoFileNames.containsKey(var6)) {
         return false;
      } else {
         File var7 = new File((String)IsoLot.InfoFileNames.get(var6));
         if (var7.exists()) {
            IsoLot var8 = null;

            boolean var12;
            try {
               LotHeader var9 = (LotHeader)IsoLot.InfoHeaders.get(ChunkMapFilenames.instance.getHeader(var4, var5));
               var8 = IsoLot.get(var9.mapFiles, var4, var5, var1, var2, var3);
               if (var8.info == null) {
                  boolean var23 = true;
                  return var23;
               }

               boolean[] var10 = new boolean[64];
               int var11 = var0.PlaceLot(var8, 0, 0, var8.minLevel, var3, var1, var2, var10);
               if (var11 > 0) {
                  var3.addModded((ChunkGenerationStatus)var9.mapFiles.InfoFileModded.get(var6));
               }

               if (var11 != 64) {
                  for(int var24 = var8.info.mapFiles.priority + 1; var24 < IsoLot.MapFiles.size(); ++var24) {
                     MapFiles var13 = (MapFiles)IsoLot.MapFiles.get(var24);
                     if (var13.hasCell(var4, var5)) {
                        IsoLot var14 = null;

                        try {
                           var14 = IsoLot.get(var13, var4, var5, var1, var2, var3);
                           var11 = var0.PlaceLot(var14, 0, 0, var14.minLevel, var3, var1, var2, var10);
                           if (var11 > 0) {
                              var3.addModded((ChunkGenerationStatus)var13.InfoFileModded.get(var6));
                           }

                           if (var11 == 64) {
                              return true;
                           }
                        } finally {
                           if (var14 != null) {
                              IsoLot.put(var14);
                           }

                        }
                     }
                  }

                  return true;
               }

               var12 = true;
            } finally {
               if (var8 != null) {
                  IsoLot.put(var8);
               }

            }

            return var12;
         } else {
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
      IsoCell var3 = new IsoCell(IsoCell.CellSizeInSquares, IsoCell.CellSizeInSquares);
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
            int var6 = var1 - IsoChunkMap.ChunkGridWidth / 2;
            int var7 = var2 - IsoChunkMap.ChunkGridWidth / 2;
            int var8 = var1 + IsoChunkMap.ChunkGridWidth / 2 + 1;
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
      GameEntityFactory.CreateIsoEntityFromCellLoading(var1);
      int var2 = var0.placeWallAndDoorCheck(var1, var0.getObjects().size());
      if (var2 != var0.getObjects().size() && var2 >= 0 && var2 <= var0.getObjects().size()) {
         var0.getObjects().add(var2, var1);
      } else {
         var0.getObjects().add(var1);
      }

   }

   private static void AddSpecialObject(IsoGridSquare var0, IsoObject var1) {
      GameEntityFactory.CreateIsoEntityFromCellLoading(var1);
      int var2 = var0.placeWallAndDoorCheck(var1, var0.getObjects().size());
      if (var2 != var0.getObjects().size() && var2 >= 0 && var2 <= var0.getObjects().size()) {
         var0.getObjects().add(var2, var1);
      } else {
         var0.getObjects().add(var1);
         var0.getSpecialObjects().add(var1);
      }

   }
}
