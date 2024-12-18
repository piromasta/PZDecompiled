package zombie.iso.fboRenderChunk;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.Mouse;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoDoor;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.popman.ObjectPool;

public final class FBORenderCutaways {
   private static FBORenderCutaways instance;
   public static final byte CLDSF_NONE = 0;
   public static final byte CLDSF_SHOULD_RENDER = 1;
   public IsoCell cell;
   private final PerPlayerData[] perPlayerData = new PerPlayerData[4];
   private final HashSet<IsoChunk> invalidatedChunks = new HashSet();
   private final ArrayList<PointOfInterest> pointOfInterest = new ArrayList();
   private final ObjectPool<PointOfInterest> pointOfInterestStore = new ObjectPool(PointOfInterest::new);
   private final Rectangle buildingRectTemp = new Rectangle();
   public static final ObjectPool<CutawayWall> s_cutawayWallPool = new ObjectPool(CutawayWall::new);
   public static final ObjectPool<SlopedSurface> s_slopedSurfacePool = new ObjectPool(SlopedSurface::new);

   public static FBORenderCutaways getInstance() {
      if (instance == null) {
         instance = new FBORenderCutaways();
      }

      return instance;
   }

   private FBORenderCutaways() {
      for(int var1 = 0; var1 < this.perPlayerData.length; ++var1) {
         this.perPlayerData[var1] = new PerPlayerData();
      }

   }

   public boolean checkPlayerRoom(int var1) {
      boolean var2 = false;
      IsoGridSquare var3 = IsoCamera.frameState.CamCharacterSquare;
      if (var3 != null) {
         long var4 = var3.getRoomID();
         if (var4 == -1L && getInstance().isRoofRoomSquare(var3)) {
            var4 = var3.associatedBuilding.getRoofRoomID(var3.z);
         }

         PerPlayerData var6 = this.perPlayerData[var1];
         if (var6.lastPlayerRoomID != -1L && var4 == -1L) {
            var2 = true;
            var6.lastPlayerRoomID = -1L;
         } else if (var4 != -1L && var6.lastPlayerRoomID != var4) {
            var2 = true;
            var6.lastPlayerRoomID = var4;
         }
      }

      return var2;
   }

   public boolean checkExteriorWalls(ArrayList<IsoChunk> var1) {
      IsoGridSquare var2 = IsoCamera.frameState.CamCharacterSquare;
      if (var2 == null) {
         return false;
      } else {
         int var3 = IsoCamera.frameState.playerIndex;
         int var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
         float var5 = Core.getInstance().getZoom(var3);
         boolean var6 = false;

         for(int var7 = 0; var7 < var1.size(); ++var7) {
            IsoChunk var8 = (IsoChunk)var1.get(var7);
            FBORenderLevels var9 = var8.getRenderLevels(var3);
            if (var9.isOnScreen(var4)) {
               ChunkLevelData var10 = var8.getCutawayData().getDataForLevel(var4);
               if (var10.m_adjacentChunkLoadedCounter != var8.m_adjacentChunkLoadedCounter) {
                  var10.m_adjacentChunkLoadedCounter = var8.m_adjacentChunkLoadedCounter;
                  var10.m_orphanStructures.m_adjacentChunkLoadedCounter = var8.m_adjacentChunkLoadedCounter;
                  var8.getCutawayData().recreateLevel(var4);
                  var6 = true;
               } else if (var9.isDirty(var4, FBORenderChunk.DIRTY_OBJECT_ADD | FBORenderChunk.DIRTY_OBJECT_REMOVE, var5)) {
                  var8.getCutawayData().recreateLevel(var4);
                  var6 = true;
               }

               var6 |= this.checkOrphanStructures(var3, var8);
               if (!var10.m_exteriorWalls.isEmpty()) {
                  boolean var11 = false;

                  for(int var12 = 0; var12 < var10.m_exteriorWalls.size(); ++var12) {
                     CutawayWall var13 = (CutawayWall)var10.m_exteriorWalls.get(var12);
                     if (var13.shouldCutawayFence()) {
                        if (!var13.isPlayerInRange(var3, FBORenderCutaways.PlayerInRange.True)) {
                           var13.setPlayerInRange(var3, FBORenderCutaways.PlayerInRange.True);
                           var13.setPlayerCutawayFlag(var3, true);
                           var11 = true;
                        }
                     } else if (!var13.isPlayerInRange(var3, FBORenderCutaways.PlayerInRange.False)) {
                        var13.setPlayerInRange(var3, FBORenderCutaways.PlayerInRange.False);
                        var13.setPlayerCutawayFlag(var3, false);
                        var11 = true;
                     }
                  }

                  if (var11) {
                     var9.invalidateLevel(var4, FBORenderChunk.DIRTY_CUTAWAYS);
                  }
               }
            }
         }

         return var6;
      }
   }

   public boolean checkSlopedSurfaces(ArrayList<IsoChunk> var1) {
      IsoGridSquare var2 = IsoCamera.frameState.CamCharacterSquare;
      if (var2 == null) {
         return false;
      } else {
         int var3 = IsoCamera.frameState.playerIndex;
         int var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
         boolean var5 = false;

         for(int var6 = 0; var6 < var1.size(); ++var6) {
            IsoChunk var7 = (IsoChunk)var1.get(var6);
            FBORenderLevels var8 = var7.getRenderLevels(var3);
            if (var8.isOnScreen(var4)) {
               ChunkLevelData var9 = var7.getCutawayData().getDataForLevel(var4);
               if (!var9.m_slopedSurfaces.isEmpty()) {
                  boolean var10 = false;

                  for(int var11 = 0; var11 < var9.m_slopedSurfaces.size(); ++var11) {
                     SlopedSurface var12 = (SlopedSurface)var9.m_slopedSurfaces.get(var11);
                     if (var12.shouldCutaway()) {
                        if (!var12.isPlayerInRange(var3, FBORenderCutaways.PlayerInRange.True)) {
                           var12.setPlayerInRange(var3, FBORenderCutaways.PlayerInRange.True);
                           var12.setPlayerCutawayFlag(var3, true);
                           var10 = true;
                        }
                     } else if (!var12.isPlayerInRange(var3, FBORenderCutaways.PlayerInRange.False)) {
                        var12.setPlayerInRange(var3, FBORenderCutaways.PlayerInRange.False);
                        var12.setPlayerCutawayFlag(var3, false);
                        var10 = true;
                     }
                  }

                  if (var10) {
                     var8.invalidateLevel(var4, FBORenderChunk.DIRTY_CUTAWAYS);
                  }
               }
            }
         }

         return var5;
      }
   }

   public void squareChanged(IsoGridSquare var1) {
      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         this.perPlayerData[var2].checkSquare = null;
      }

   }

   public boolean checkOccludedRooms(int var1, ArrayList<IsoChunk> var2) {
      IsoGridSquare var3 = IsoCamera.frameState.CamCharacterSquare;
      if (var3 == null) {
         return false;
      } else {
         PerPlayerData var4 = this.perPlayerData[var1];
         if (var4.checkSquare == var3) {
            return false;
         } else {
            var4.checkSquare = var3;
            int var5 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
            boolean var6 = false;

            for(int var7 = 0; var7 < var2.size(); ++var7) {
               IsoChunk var8 = (IsoChunk)var2.get(var7);
               FBORenderLevels var9 = var8.getRenderLevels(var1);
               if (var9.isOnScreen(var5)) {
                  ChunkLevelData var10 = var8.getCutawayData().getDataForLevel(var5);
                  if (!var10.m_allWalls.isEmpty()) {
                     boolean var11 = false;

                     for(int var12 = 0; var12 < var10.m_allWalls.size(); ++var12) {
                        CutawayWall var13 = (CutawayWall)var10.m_allWalls.get(var12);
                        if (var13.shouldCutawayBuilding(var1)) {
                           if (var13.isPlayerInRange(var1, FBORenderCutaways.PlayerInRange.True)) {
                              int var14 = var13.calculateOccludedSquaresMaskForSeenRooms(var1);
                              if (var14 != var13.occludedSquaresMaskForSeenRooms[var1]) {
                                 var13.occludedSquaresMaskForSeenRooms[var1] = var14;
                                 var6 = true;
                                 var11 = true;
                              }
                           } else {
                              var13.setPlayerInRange(var1, FBORenderCutaways.PlayerInRange.True);
                              var13.occludedSquaresMaskForSeenRooms[var1] = var13.calculateOccludedSquaresMaskForSeenRooms(var1);
                              var6 = true;
                              var11 = true;
                           }
                        } else if (!var13.isPlayerInRange(var1, FBORenderCutaways.PlayerInRange.False)) {
                           var13.setPlayerInRange(var1, FBORenderCutaways.PlayerInRange.False);
                           var6 = true;
                           var11 = true;
                        }
                     }

                     if (var11) {
                        var9.invalidateLevel(var5, FBORenderChunk.DIRTY_CUTAWAYS);
                     }
                  }
               }
            }

            return var6;
         }
      }
   }

   boolean checkOrphanStructures(int var1, IsoChunk var2) {
      FBORenderLevels var3 = var2.getRenderLevels(var1);
      boolean var4 = false;

      for(int var5 = PZMath.max(1, var2.minLevel); var5 <= var2.maxLevel; ++var5) {
         ChunkLevelData var6 = var2.getCutawayData().getDataForLevel(var5);
         OrphanStructures var7 = var6.m_orphanStructures;
         if (var7.hasOrphanStructures) {
            if (var7.shouldCutaway()) {
               if (!var7.isPlayerInRange(var1, FBORenderCutaways.PlayerInRange.True)) {
                  var7.setPlayerInRange(var1, FBORenderCutaways.PlayerInRange.True);
                  var4 = true;
                  var3.invalidateLevel(var5, FBORenderChunk.DIRTY_CUTAWAYS);
               }
            } else if (!var7.isPlayerInRange(var1, FBORenderCutaways.PlayerInRange.False)) {
               var7.setPlayerInRange(var1, FBORenderCutaways.PlayerInRange.False);
               var4 = true;
               var3.invalidateLevel(var5, FBORenderChunk.DIRTY_CUTAWAYS);
            }
         }
      }

      return var4;
   }

   public void doCutawayVisitSquares(int var1, ArrayList<IsoChunk> var2) {
      PerPlayerData var3 = this.perPlayerData[var1];
      var3.lastCutawayVisitorResults.clear();
      var3.lastCutawayVisitorResults.addAll(var3.cutawayVisitorResultsNorth);
      var3.lastCutawayVisitorResults.addAll(var3.cutawayVisitorResultsWest);
      var3.cutawayVisitorResultsNorth.clear();
      var3.cutawayVisitorResultsWest.clear();
      var3.cutawayVisitorVisitedNorth.clear();
      var3.cutawayVisitorVisitedWest.clear();
      int var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      var3.cutawayWalls.clear();

      for(int var5 = 0; var5 < var2.size(); ++var5) {
         IsoChunk var6 = (IsoChunk)var2.get(var5);
         if (var4 >= var6.minLevel && var4 <= var6.maxLevel) {
            FBORenderLevels var7 = var6.getRenderLevels(var1);
            if (var7.isOnScreen(var4)) {
               ChunkLevelData var8 = var6.getCutawayDataForLevel(var4);
               var3.cutawayWalls.addAll(var8.m_allWalls);
            }
         }
      }

      IsoChunkMap var15 = this.cell.ChunkMap[var1];
      long var16 = System.currentTimeMillis();

      int var17;
      for(var17 = 0; var17 < this.pointOfInterest.size(); ++var17) {
         PointOfInterest var9 = (PointOfInterest)this.pointOfInterest.get(var17);
         if (var9.z == var4) {
            IsoGridSquare var10 = var15.getGridSquare(var9.x, var9.y, var9.z);
            if (var10 != null && !var3.cutawayVisitorVisitedNorth.contains(var10) && !var3.cutawayVisitorVisitedWest.contains(var10)) {
               this.doCutawayVisitSquares(var10, var16, var2);
            }
         }
      }

      this.invalidatedChunks.clear();

      FBORenderLevels var11;
      IsoGridSquare var18;
      IsoChunk var20;
      for(var17 = 0; var17 < var3.lastCutawayVisitorResults.size(); ++var17) {
         var18 = (IsoGridSquare)var3.lastCutawayVisitorResults.get(var17);
         var18.setPlayerCutawayFlag(var1, 0, var16);
         var20 = var18.getChunk();
         if (var20 != null && !this.invalidatedChunks.contains(var20)) {
            this.invalidatedChunks.add(var20);
            var11 = var20.getRenderLevels(var1);
            var11.invalidateLevel(var18.z, FBORenderChunk.DIRTY_CUTAWAYS);
            if (!var20.IsOnScreen(false)) {
               var20.getCutawayData().invalidateOccludedSquaresMaskForSeenRooms(var1, var18.z);
            }
         }
      }

      IsoChunk var19;
      for(var17 = 0; var17 < var2.size(); ++var17) {
         var19 = (IsoChunk)var2.get(var17);
         if (var4 >= var19.minLevel && var4 <= var19.maxLevel) {
            FBORenderLevels var22 = var19.getRenderLevels(var1);
            if (var22.isOnScreen(var4)) {
               ChunkLevelData var23 = var19.getCutawayDataForLevel(var4);

               for(int var12 = 0; var12 < var23.m_exteriorWalls.size(); ++var12) {
                  CutawayWall var13 = (CutawayWall)var23.m_exteriorWalls.get(var12);
                  if (!var13.isPlayerInRange(var1, FBORenderCutaways.PlayerInRange.False)) {
                     var13.setVisitedSquares(var3);
                  }
               }
            }
         }
      }

      Iterator var21 = var3.cutawayVisitorResultsNorth.iterator();

      while(var21.hasNext()) {
         var18 = (IsoGridSquare)var21.next();
         var18.addPlayerCutawayFlag(var1, 1, var16);
         var20 = var18.getChunk();
         if (var20 != null && !this.invalidatedChunks.contains(var20)) {
            this.invalidatedChunks.add(var20);
            var11 = var20.getRenderLevels(var1);
            var11.invalidateLevel(var18.z, FBORenderChunk.DIRTY_CUTAWAYS);
         }
      }

      var21 = var3.cutawayVisitorResultsWest.iterator();

      while(var21.hasNext()) {
         var18 = (IsoGridSquare)var21.next();
         var18.addPlayerCutawayFlag(var1, 2, var16);
         var20 = var18.getChunk();
         if (var20 != null && !this.invalidatedChunks.contains(var20)) {
            this.invalidatedChunks.add(var20);
            var11 = var20.getRenderLevels(var1);
            var11.invalidateLevel(var18.z, FBORenderChunk.DIRTY_CUTAWAYS);
         }
      }

      var21 = this.invalidatedChunks.iterator();

      while(var21.hasNext()) {
         var19 = (IsoChunk)var21.next();
         ChunkLevelData var24 = var19.getCutawayData().getDataForLevel(var4);
         if (var24 != null) {
            boolean var25 = this.hasAnyCutawayWalls(var1, var19, var4, (byte)1, 0, 0, 0, 7);
            if (var25 != var24.bHasCutawayNorthWallsOnWestEdge) {
               var24.bHasCutawayNorthWallsOnWestEdge = var25;
               this.invalidateChunk(var1, var15, var19.wx - 1, var19.wy, var4);
            }

            boolean var26 = this.hasAnyCutawayWalls(var1, var19, var4, (byte)1, 7, 0, 7, 7);
            if (var26 != var24.bHasCutawayNorthWallsOnEastEdge) {
               var24.bHasCutawayNorthWallsOnEastEdge = var26;
               this.invalidateChunk(var1, var15, var19.wx + 1, var19.wy, var4);
            }

            boolean var27 = this.hasAnyCutawayWalls(var1, var19, var4, (byte)2, 0, 0, 7, 0);
            if (var27 != var24.bHasCutawayWestWallsOnNorthEdge) {
               var24.bHasCutawayWestWallsOnNorthEdge = var27;
               this.invalidateChunk(var1, var15, var19.wx, var19.wy - 1, var4);
            }

            boolean var14 = this.hasAnyCutawayWalls(var1, var19, var4, (byte)2, 0, 7, 7, 7);
            if (var14 != var24.bHasCutawayWestWallsOnSouthEdge) {
               var24.bHasCutawayWestWallsOnSouthEdge = var27;
               this.invalidateChunk(var1, var15, var19.wx, var19.wy + 1, var4);
            }
         }
      }

   }

   private void invalidateChunk(int var1, IsoChunkMap var2, int var3, int var4, int var5) {
      IsoChunk var6 = var2.getChunk(var3 - var2.getWorldXMin(), var4 - var2.getWorldYMin());
      if (var6 != null) {
         FBORenderLevels var7 = var6.getRenderLevels(var1);
         var7.invalidateLevel(var5, FBORenderChunk.DIRTY_CUTAWAYS);
      }
   }

   private boolean hasAnyCutawayWalls(int var1, IsoChunk var2, int var3, byte var4, int var5, int var6, int var7, int var8) {
      long var9 = 0L;

      for(int var11 = var6; var11 <= var8; ++var11) {
         for(int var12 = var5; var12 <= var7; ++var12) {
            IsoGridSquare var13 = var2.getGridSquare(var12, var11, var3);
            if (var13 != null && (var13.getPlayerCutawayFlag(var1, var9) & var4) != 0) {
               return true;
            }
         }
      }

      return false;
   }

   private void doCutawayVisitSquares(IsoGridSquare var1, long var2, ArrayList<IsoChunk> var4) {
      this.cutawayVisit(var1, var2, var4);
   }

   private boolean IsCutawaySquare(CutawayWall var1, IsoGridSquare var2, IsoGridSquare var3, long var4) {
      int var6 = IsoCamera.frameState.playerIndex;
      if (var3 == null) {
         return false;
      } else if (var2.getZ() != var3.getZ()) {
         return false;
      } else {
         if (var2.getRoom() != null && var3.getRoom() != null && var2.getBuilding() != var3.getRoom().building) {
         }

         ArrayList var7 = (ArrayList)this.cell.tempPlayerCutawayRoomIDs.get(var6);
         if (var7.isEmpty()) {
            return this.IsCollapsibleBuildingSquare(var3);
         } else if (this.isCutawayDueToPeeking(var1, var3)) {
            return true;
         } else {
            for(int var8 = 0; var8 < var7.size(); ++var8) {
               long var9 = (Long)var7.get(var8);
               if (var1.occludedRoomIDs.contains(var9)) {
                  int var11 = var1.isHorizontal() ? var3.x - var1.x1 : var3.y - var1.y1;
                  return (var1.occludedSquaresMaskForSeenRooms[var6] & 1 << var11) != 0;
               }
            }

            return false;
         }
      }
   }

   private boolean isCutawayDueToPeeking(CutawayWall var1, IsoGridSquare var2) {
      int var3 = IsoCamera.frameState.playerIndex;
      long var4 = this.cell.playerWindowPeekingRoomId[var3];
      if (var4 == -1L) {
         return false;
      } else {
         int var6 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterX);
         int var7 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterY);
         IsoObject var8;
         IsoObject var9;
         IsoObject var10;
         IsoObject var11;
         if (var1.isHorizontal()) {
            if ((var7 == var1.y1 - 1 || var7 == var1.y1) && var2.Is("GarageDoor")) {
               var8 = var2.getGarageDoor(true);
               if (var8 != null) {
                  var9 = var8;

                  for(var10 = var8; var9 != null; var9 = var11) {
                     var11 = IsoDoor.getGarageDoorPrev(var9);
                     if (var11 == null) {
                        break;
                     }
                  }

                  while(var10 != null) {
                     var11 = IsoDoor.getGarageDoorNext(var10);
                     if (var11 == null) {
                        break;
                     }

                     var10 = var11;
                  }

                  return (float)var2.x >= var9.getX() && (float)var2.x <= var10.getX();
               }
            }

            if ((var7 == var1.y1 - 1 || var7 == var1.y1) && var2.x >= var6 && var2.x <= var6 + 1) {
               return true;
            }
         } else {
            if ((var6 == var1.x1 - 1 || var6 == var1.x1) && var2.Is("GarageDoor")) {
               var8 = var2.getGarageDoor(true);
               if (var8 != null) {
                  var9 = var8;

                  for(var10 = var8; var9 != null; var9 = var11) {
                     var11 = IsoDoor.getGarageDoorPrev(var9);
                     if (var11 == null) {
                        break;
                     }
                  }

                  while(var10 != null) {
                     var11 = IsoDoor.getGarageDoorNext(var10);
                     if (var11 == null) {
                        break;
                     }

                     var10 = var11;
                  }

                  return (float)var2.y >= var9.getY() && (float)var2.y <= var10.getY();
               }
            }

            if ((var6 == var1.x1 - 1 || var6 == var1.x1) && var2.y >= var7 && var2.y <= var7 + 1) {
               return true;
            }
         }

         return false;
      }
   }

   private void cutawayVisit(IsoGridSquare var1, long var2, ArrayList<IsoChunk> var4) {
      int var5 = IsoCamera.frameState.playerIndex;
      IsoChunkMap var6 = this.cell.ChunkMap[var5];
      if (var6 != null && !var6.ignore) {
         PerPlayerData var7 = this.perPlayerData[var5];

         for(int var8 = 0; var8 < var7.cutawayWalls.size(); ++var8) {
            CutawayWall var9 = (CutawayWall)var7.cutawayWalls.get(var8);
            int var10 = var9.chunkLevelData.m_level;
            int var11;
            IsoGridSquare var12;
            boolean var13;
            ChunkLevelData var14;
            if (var9.isHorizontal()) {
               for(var11 = var9.x1; var11 < var9.x2; ++var11) {
                  var12 = var6.getGridSquare(var11, var9.y1, var1.z);
                  if (var12 != null) {
                     var13 = var7.cutawayVisitorVisitedNorth.contains(var12);
                     if (!var13) {
                        var7.cutawayVisitorVisitedNorth.add(var12);
                     }

                     var14 = var12.chunk.getCutawayDataForLevel(var10);
                     if (var14.shouldRenderSquare(var5, var12) && !var12.getObjects().isEmpty() && !var13 && this.IsCutawaySquare(var9, var1, var12, var2)) {
                        var7.cutawayVisitorResultsNorth.add(var12);
                        if (var12.Is(IsoFlagType.WallSE)) {
                           var7.cutawayVisitorResultsWest.add(var12);
                        }
                     }
                  }
               }
            } else {
               for(var11 = var9.y1; var11 < var9.y2; ++var11) {
                  var12 = this.cell.getGridSquare(var9.x1, var11, var1.z);
                  if (var12 != null) {
                     var13 = var7.cutawayVisitorVisitedWest.contains(var12);
                     if (!var13) {
                        var7.cutawayVisitorVisitedWest.add(var12);
                     }

                     var14 = var12.chunk.getCutawayDataForLevel(var10);
                     if (var14.shouldRenderSquare(var5, var12) && !var12.getObjects().isEmpty() && !var13 && this.IsCutawaySquare(var9, var1, var12, var2)) {
                        var7.cutawayVisitorResultsWest.add(var12);
                        if (var12.Is(IsoFlagType.WallSE)) {
                           var7.cutawayVisitorResultsNorth.add(var12);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public boolean CalculateBuildingsToCollapse() {
      int var1 = IsoCamera.frameState.playerIndex;
      PerPlayerData var2 = this.perPlayerData[var1];
      BuildingsToCollapse var3 = var2.buildingsToCollapse;
      var3.buildingsToCollapse.clear();
      ArrayList var4 = new ArrayList();
      boolean var5 = false;

      int var9;
      for(int var6 = 0; var6 < this.pointOfInterest.size(); ++var6) {
         PointOfInterest var7 = (PointOfInterest)this.pointOfInterest.get(var6);
         IsoGridSquare var8 = this.cell.getGridSquare(var7.x, var7.y, var7.z);
         this.cell.GetBuildingsInFrontOfCharacter(var4, var8, false);
         var5 |= this.cell.bOccludedByOrphanStructureFlag;
         if (var4.isEmpty()) {
            this.cell.GetBuildingsInFrontOfCharacter(var4, var8, true);
            var5 |= this.cell.bOccludedByOrphanStructureFlag;
         }

         for(var9 = 0; var9 < var4.size(); ++var9) {
            if (!var3.buildingsToCollapse.contains(((IsoBuilding)var4.get(var9)).def)) {
               var3.buildingsToCollapse.add(((IsoBuilding)var4.get(var9)).def);
            }
         }
      }

      this.cell.bOccludedByOrphanStructureFlag = var5;
      long var12 = this.cell.playerWindowPeekingRoomId[var1];
      if (var12 != -1L) {
         IsoRoom var13 = IsoWorld.instance.MetaGrid.getRoomByID(var12);
         BuildingDef var15 = var13.building.getDef();
         if (!var3.buildingsToCollapse.contains(var15)) {
            var3.buildingsToCollapse.add(var15);
         }
      }

      boolean var14 = var3.tempLastBuildingsToCollapse.size() != var3.buildingsToCollapse.size();
      if (!var14) {
         for(var9 = 0; var9 < var3.tempLastBuildingsToCollapse.size(); ++var9) {
            BuildingDef var10 = (BuildingDef)var3.tempLastBuildingsToCollapse.get(var9);
            if (var3.buildingsToCollapse.get(var9) != var10) {
               var14 = true;
               break;
            }
         }
      }

      if (var14) {
         var9 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);

         BuildingDef var11;
         int var16;
         for(var16 = 0; var16 < var3.tempLastBuildingsToCollapse.size(); ++var16) {
            var11 = (BuildingDef)var3.tempLastBuildingsToCollapse.get(var16);
            if (!var3.buildingsToCollapse.contains(var11)) {
               var11.invalidateOverlappedChunkLevelsAbove(var1, var9, FBORenderChunk.DIRTY_CUTAWAYS | FBORenderChunk.DIRTY_REDO_CUTAWAYS);
            }
         }

         for(var16 = 0; var16 < var3.buildingsToCollapse.size(); ++var16) {
            var11 = (BuildingDef)var3.buildingsToCollapse.get(var16);
            if (!var3.tempLastBuildingsToCollapse.contains(var11)) {
               var11.invalidateOverlappedChunkLevelsAbove(var1, var9, FBORenderChunk.DIRTY_CUTAWAYS | FBORenderChunk.DIRTY_REDO_CUTAWAYS);
            }
         }
      }

      var3.tempLastBuildingsToCollapse.clear();
      var3.tempLastBuildingsToCollapse.addAll(var3.buildingsToCollapse);
      return var14;
   }

   public void checkHiddenBuildingLevels() {
      int var1 = IsoCamera.frameState.playerIndex;
      int var2 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      PerPlayerData var3 = this.perPlayerData[var1];
      BuildingsToCollapse var4 = var3.buildingsToCollapse;

      for(int var5 = 0; var5 < var4.buildingsToCollapse.size(); ++var5) {
         BuildingDef var6 = (BuildingDef)var4.buildingsToCollapse.get(var5);
         if (var4.maxVisibleLevel.containsKey(var6)) {
            if (var4.maxVisibleLevel.get(var6) != var2) {
               int var7 = PZMath.min(var4.maxVisibleLevel.get(var6), var2);
               var4.maxVisibleLevel.put(var6, var2);
               var6.invalidateOverlappedChunkLevelsAbove(var1, var7, FBORenderChunk.DIRTY_CUTAWAYS);
            }
         } else {
            var4.maxVisibleLevel.put(var6, var2);
            var6.invalidateOverlappedChunkLevelsAbove(var1, var2, FBORenderChunk.DIRTY_CUTAWAYS);
         }
      }

   }

   public boolean CanBuildingSquareOccludePlayer(IsoGridSquare var1, int var2) {
      PerPlayerData var3 = this.perPlayerData[var2];
      BuildingsToCollapse var4 = var3.buildingsToCollapse;

      for(int var5 = 0; var5 < var4.buildingsToCollapse.size(); ++var5) {
         BuildingDef var6 = (BuildingDef)var4.buildingsToCollapse.get(var5);
         int var7 = var6.getX();
         int var8 = var6.getY();
         int var9 = var6.getX2() - var7;
         int var10 = var6.getY2() - var8;
         this.buildingRectTemp.setBounds(var7 - 1, var8 - 1, var9 + 2, var10 + 2);
         if (this.buildingRectTemp.contains(var1.getX(), var1.getY())) {
            return true;
         }
      }

      return false;
   }

   public IsoObject getFirstMultiLevelObject(IsoGridSquare var1) {
      if (var1 == null) {
         return null;
      } else if (!var1.Is("SpriteGridPos")) {
         return null;
      } else {
         IsoObject[] var2 = (IsoObject[])var1.getObjects().getElements();
         int var3 = 0;

         for(int var4 = var1.getObjects().size(); var3 < var4; ++var3) {
            IsoObject var5 = var2[var3];
            IsoSpriteGrid var6 = var5.getSpriteGrid();
            if (var6 != null && var6.getLevels() > 1) {
               return var5;
            }
         }

         return null;
      }
   }

   public boolean isForceRenderSquare(int var1, IsoGridSquare var2) {
      int var3 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      PerPlayerData var4 = this.perPlayerData[var1];
      BuildingsToCollapse var5 = var4.buildingsToCollapse;
      if (var2.associatedBuilding != null && var5.buildingsToCollapse.contains(var2.associatedBuilding) && var2.z > var3) {
         IsoObject var6 = this.getFirstMultiLevelObject(var2);
         if (var6 != null) {
            IsoSprite var7 = var6.getSprite();
            if (var2.z - var6.getSpriteGrid().getSpriteGridPosZ(var7) <= var3) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldHideElevatedFloor(int var1, IsoObject var2) {
      if (var2 != null && var2.getProperties() != null) {
         if (!var2.getProperties().Is(IsoFlagType.FloorHeightOneThird) && !var2.getProperties().Is(IsoFlagType.FloorHeightTwoThirds)) {
            return false;
         } else {
            IsoGridSquare var3 = var2.getSquare();
            if (var3 == null) {
               return false;
            } else {
               int var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
               if (var3.z != var4) {
                  return false;
               } else {
                  PerPlayerData var5 = this.perPlayerData[var1];
                  BuildingsToCollapse var6 = var5.buildingsToCollapse;
                  return var3.associatedBuilding != null && var6.buildingsToCollapse.contains(var3.associatedBuilding);
               }
            }
         }
      } else {
         return false;
      }
   }

   public boolean shouldRenderBuildingSquare(int var1, IsoGridSquare var2) {
      int var3 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      PerPlayerData var4 = this.perPlayerData[var1];
      BuildingsToCollapse var5 = var4.buildingsToCollapse;
      if (var2.associatedBuilding != null && var5.buildingsToCollapse.contains(var2.associatedBuilding) && var2.z > var3) {
         IsoObject var8 = this.getFirstMultiLevelObject(var2);
         if (var8 != null) {
            IsoSprite var9 = var8.getSprite();
            if (var2.z - var8.getSpriteGrid().getSpriteGridPosZ(var9) <= var3) {
               return true;
            }
         }

         return false;
      } else if (var2.z > var3 && var3 < 0) {
         return false;
      } else {
         if (var2.z > var3) {
            ChunkLevelData var6 = var2.chunk.getCutawayDataForLevel(var2.z);
            if (var6.m_orphanStructures.m_adjacentChunkLoadedCounter != var2.chunk.m_adjacentChunkLoadedCounter) {
               var6.m_orphanStructures.m_adjacentChunkLoadedCounter = var2.chunk.m_adjacentChunkLoadedCounter;
               var6.m_orphanStructures.calculate(var2.chunk);
            }

            OrphanStructures var7 = var6.m_orphanStructures;
            if (var7.hasOrphanStructures && var7.isPlayerInRange(var1, FBORenderCutaways.PlayerInRange.True) && var7.isOrphanStructureSquare(var2)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean IsCollapsibleBuildingSquare(IsoGridSquare var1) {
      if (var1.getProperties().Is(IsoFlagType.forceRender)) {
         return false;
      } else {
         for(int var2 = 0; var2 < 4; ++var2) {
            IsoPlayer var3 = IsoPlayer.players[var2];
            if (var3 != null && var3.getCurrentSquare() != null) {
               PerPlayerData var4 = this.perPlayerData[var2];
               BuildingsToCollapse var5 = var4.buildingsToCollapse;
               int var6 = var5.buildingsToCollapse.size();

               for(int var7 = 0; var7 < var6; ++var7) {
                  BuildingDef var8 = (BuildingDef)var5.buildingsToCollapse.get(var7);
                  if (this.cell.collapsibleBuildingSquareAlgorithm(var8, var1, var3.getSquare())) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   public void CalculatePointsOfInterest() {
      this.pointOfInterestStore.releaseAll(this.pointOfInterest);
      this.pointOfInterest.clear();
      int var1 = IsoCamera.frameState.playerIndex;
      IsoPlayer var2 = IsoPlayer.players[var1];
      this.AddPointOfInterest(var2.getX(), var2.getY(), var2.getZ());
      int var3;
      if (var1 == 0 && var2.isAiming()) {
         var3 = Mouse.getX();
         int var4 = Mouse.getY() + 52 * Core.TileScale;
         float var5 = IsoUtils.XToIso((float)var3, (float)var4, (float)((int)var2.getZ()));
         float var6 = IsoUtils.YToIso((float)var3, (float)var4, (float)((int)var2.getZ()));
         this.AddPointOfInterest(var5, var6, var2.getZ());
      }

      if (var2.getCurrentSquare() != null) {
         this.cell.gridSquaresTempLeft.clear();
         this.cell.gridSquaresTempRight.clear();
         this.cell.GetSquaresAroundPlayerSquare(var2, var2.getCurrentSquare(), this.cell.gridSquaresTempLeft, this.cell.gridSquaresTempRight);

         IsoGridSquare var7;
         for(var3 = 0; var3 < this.cell.gridSquaresTempLeft.size(); ++var3) {
            var7 = (IsoGridSquare)this.cell.gridSquaresTempLeft.get(var3);
            if (var7.isCouldSee(var1) && (var7.getBuilding() == null || var7.getBuilding() == var2.getBuilding())) {
               this.AddPointOfInterest((float)var7.x, (float)var7.y, (float)var7.z);
               if (DebugOptions.instance.FBORenderChunk.RenderMustSeeSquares.getValue()) {
                  LineDrawer.addRect((float)var7.x, (float)var7.y, (float)var7.z, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
               }
            }
         }

         for(var3 = 0; var3 < this.cell.gridSquaresTempRight.size(); ++var3) {
            var7 = (IsoGridSquare)this.cell.gridSquaresTempRight.get(var3);
            if (var7.isCouldSee(var1) && (var7.getBuilding() == null || var7.getBuilding() == var2.getBuilding())) {
               this.AddPointOfInterest((float)var7.x, (float)var7.y, (float)var7.z);
               if (DebugOptions.instance.FBORenderChunk.RenderMustSeeSquares.getValue()) {
                  LineDrawer.addRect((float)var7.x, (float)var7.y, (float)var7.z, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
               }
            }
         }
      }

   }

   private void AddPointOfInterest(float var1, float var2, float var3) {
      PointOfInterest var4 = (PointOfInterest)this.pointOfInterestStore.alloc();
      var4.x = PZMath.fastfloor(var1);
      var4.y = PZMath.fastfloor(var2);
      var4.z = PZMath.fastfloor(var3);
      this.pointOfInterest.add(var4);
   }

   public boolean isRoofRoomSquare(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else if (var1.getZ() == 0) {
         return false;
      } else if (var1.getRoomID() != -1L) {
         return false;
      } else if (var1.associatedBuilding == null) {
         return false;
      } else {
         return var1.TreatAsSolidFloor();
      }
   }

   private static final class PerPlayerData {
      long lastPlayerRoomID = -1L;
      final HashSet<IsoGridSquare> cutawayVisitorResultsNorth = new HashSet();
      final HashSet<IsoGridSquare> cutawayVisitorResultsWest = new HashSet();
      final ArrayList<IsoGridSquare> lastCutawayVisitorResults = new ArrayList();
      final HashSet<IsoGridSquare> cutawayVisitorVisitedNorth = new HashSet();
      final HashSet<IsoGridSquare> cutawayVisitorVisitedWest = new HashSet();
      IsoGridSquare checkSquare = null;
      final ArrayList<CutawayWall> cutawayWalls = new ArrayList();
      private final BuildingsToCollapse buildingsToCollapse = new BuildingsToCollapse();

      private PerPlayerData() {
      }
   }

   public static final class ChunkLevelsData {
      public final IsoChunk m_chunk;
      public final TIntObjectHashMap<ChunkLevelData> m_levelData = new TIntObjectHashMap();

      public ChunkLevelsData(IsoChunk var1) {
         this.m_chunk = var1;
      }

      public ChunkLevelData getDataForLevel(int var1) {
         if (var1 >= -32 && var1 <= 31) {
            int var2 = var1 + 32;
            ChunkLevelData var3 = (ChunkLevelData)this.m_levelData.get(var2);
            if (var3 == null) {
               var3 = new ChunkLevelData(var1);
               var3.m_levelsData = this;
               this.m_levelData.put(var2, var3);
            }

            return var3;
         } else {
            return null;
         }
      }

      public void recreateLevel(int var1) {
         this.recreateLevel_ExteriorWalls(var1);
         this.recreateLevel_AllWalls(var1);
         this.recreateLevel_SlopedSurfaces(var1);
         if (var1 > 0) {
            ChunkLevelData var2 = this.getDataForLevel(var1);
            var2.m_orphanStructures.calculate(this.m_chunk);
         }

      }

      public void recreateLevel_ExteriorWalls(int var1) {
         ChunkLevelData var2 = this.getDataForLevel(var1);
         this.clearPlayerCutawayFlags(var1, var2.m_exteriorWalls);
         FBORenderCutaways.s_cutawayWallPool.releaseAll(var2.m_exteriorWalls);
         var2.m_exteriorWalls.clear();
         if (var1 >= this.m_chunk.minLevel && var1 <= this.m_chunk.maxLevel) {
            IsoGridSquare[] var3 = this.m_chunk.squares[this.m_chunk.squaresIndexOfLevel(var1)];
            byte var4 = 8;

            int var5;
            CutawayWall var6;
            int var7;
            IsoGridSquare var8;
            for(var5 = 0; var5 < var4; ++var5) {
               var6 = null;

               for(var7 = 0; var7 < var4; ++var7) {
                  var8 = var3[var7 + var5 * var4];
                  if (var8 != null && var8.getWall(true) != null && (var8.Is(IsoFlagType.WallN) || var8.Is(IsoFlagType.WallNW) || var8.Is(IsoFlagType.DoorWallN) || var8.Is(IsoFlagType.WindowN)) && !this.isAdjacentToRoom(var8, IsoDirections.N)) {
                     if (var6 == null) {
                        var6 = (CutawayWall)FBORenderCutaways.s_cutawayWallPool.alloc();
                        var6.chunkLevelData = var2;
                        var6.x1 = var8.x;
                        var6.y1 = var8.y;
                        Arrays.fill(var6.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
                        var6.occludedRoomIDs.resetQuick();
                        Arrays.fill(var6.occludedSquaresMaskForSeenRooms, 0);
                     }
                  } else if (var6 != null) {
                     var6.x2 = this.m_chunk.wx * var4 + var7;
                     var6.y2 = this.m_chunk.wy * var4 + var5;
                     var2.m_exteriorWalls.add(var6);
                     var6 = null;
                  }
               }

               if (var6 != null) {
                  var6.x2 = this.m_chunk.wx * var4 + var4;
                  var6.y2 = this.m_chunk.wy * var4 + var5;
                  var2.m_exteriorWalls.add(var6);
               }
            }

            for(var5 = 0; var5 < var4; ++var5) {
               var6 = null;

               for(var7 = 0; var7 < var4; ++var7) {
                  var8 = var3[var5 + var7 * var4];
                  if (var8 != null && var8.getWall(false) != null && (var8.Is(IsoFlagType.WallW) || var8.Is(IsoFlagType.WallNW) || var8.Is(IsoFlagType.DoorWallW) || var8.Is(IsoFlagType.WindowW)) && !this.isAdjacentToRoom(var8, IsoDirections.W)) {
                     if (var6 == null) {
                        var6 = (CutawayWall)FBORenderCutaways.s_cutawayWallPool.alloc();
                        var6.chunkLevelData = var2;
                        var6.x1 = var8.x;
                        var6.y1 = var8.y;
                        Arrays.fill(var6.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
                        var6.occludedRoomIDs.resetQuick();
                        Arrays.fill(var6.occludedSquaresMaskForSeenRooms, 0);
                     }
                  } else if (var6 != null) {
                     var6.x2 = this.m_chunk.wx * var4 + var5;
                     var6.y2 = this.m_chunk.wy * var4 + var7;
                     var2.m_exteriorWalls.add(var6);
                     var6 = null;
                  }
               }

               if (var6 != null) {
                  var6.x2 = this.m_chunk.wx * var4 + var5;
                  var6.y2 = this.m_chunk.wy * var4 + var4;
                  var2.m_exteriorWalls.add(var6);
               }
            }

         }
      }

      public void recreateLevel_AllWalls(int var1) {
         ChunkLevelData var2 = this.getDataForLevel(var1);
         this.clearPlayerCutawayFlags(var1, var2.m_allWalls);
         FBORenderCutaways.s_cutawayWallPool.releaseAll(var2.m_allWalls);
         var2.m_allWalls.clear();
         if (var1 >= this.m_chunk.minLevel && var1 <= this.m_chunk.maxLevel) {
            IsoGridSquare[] var3 = this.m_chunk.squares[this.m_chunk.squaresIndexOfLevel(var1)];
            byte var4 = 8;

            int var5;
            CutawayWall var6;
            int var7;
            IsoGridSquare var8;
            boolean var9;
            for(var5 = 0; var5 < var4; ++var5) {
               var6 = null;

               for(var7 = 0; var7 < var4; ++var7) {
                  var8 = var3[var7 + var5 * var4];
                  var9 = var8 != null && (var8.getWall(true) != null || var8.Is(IsoFlagType.WindowN)) && (var8.Is(IsoFlagType.WallN) || var8.Is(IsoFlagType.WallNW) || var8.Is(IsoFlagType.DoorWallN) || var8.Is(IsoFlagType.WindowN));
                  if (!var9 && var8 != null) {
                     var9 |= var8.getGarageDoor(true) != null;
                  }

                  if (var9) {
                     if (var6 == null) {
                        var6 = (CutawayWall)FBORenderCutaways.s_cutawayWallPool.alloc();
                        var6.chunkLevelData = var2;
                        var6.x1 = var8.x;
                        var6.y1 = var8.y;
                        Arrays.fill(var6.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
                        var6.occludedRoomIDs.resetQuick();
                        Arrays.fill(var6.occludedSquaresMaskForSeenRooms, 0);
                     }
                  } else if (var6 != null) {
                     var6.x2 = this.m_chunk.wx * var4 + var7;
                     var6.y2 = this.m_chunk.wy * var4 + var5;
                     var6.calculateOccludedRooms();
                     var2.m_allWalls.add(var6);
                     var6 = null;
                  }
               }

               if (var6 != null) {
                  var6.x2 = this.m_chunk.wx * var4 + var4;
                  var6.y2 = this.m_chunk.wy * var4 + var5;
                  var6.calculateOccludedRooms();
                  var2.m_allWalls.add(var6);
               }
            }

            for(var5 = 0; var5 < var4; ++var5) {
               var6 = null;

               for(var7 = 0; var7 < var4; ++var7) {
                  var8 = var3[var5 + var7 * var4];
                  var9 = var8 != null && (var8.getWall(false) != null || var8.Is(IsoFlagType.WindowW)) && (var8.Is(IsoFlagType.WallW) || var8.Is(IsoFlagType.WallNW) || var8.Is(IsoFlagType.DoorWallW) || var8.Is(IsoFlagType.WindowW));
                  if (!var9 && var8 != null) {
                     var9 |= var8.getGarageDoor(false) != null;
                  }

                  if (var9) {
                     if (var6 == null) {
                        var6 = (CutawayWall)FBORenderCutaways.s_cutawayWallPool.alloc();
                        var6.chunkLevelData = var2;
                        var6.x1 = var8.x;
                        var6.y1 = var8.y;
                        Arrays.fill(var6.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
                        var6.occludedRoomIDs.resetQuick();
                        Arrays.fill(var6.occludedSquaresMaskForSeenRooms, 0);
                     }
                  } else if (var6 != null) {
                     var6.x2 = this.m_chunk.wx * var4 + var5;
                     var6.y2 = this.m_chunk.wy * var4 + var7;
                     var6.calculateOccludedRooms();
                     var2.m_allWalls.add(var6);
                     var6 = null;
                  }
               }

               if (var6 != null) {
                  var6.x2 = this.m_chunk.wx * var4 + var5;
                  var6.y2 = this.m_chunk.wy * var4 + var4;
                  var6.calculateOccludedRooms();
                  var2.m_allWalls.add(var6);
               }
            }

         }
      }

      public void recreateLevel_SlopedSurfaces(int var1) {
         ChunkLevelData var2 = this.getDataForLevel(var1);
         this.clearPlayerCutawayFlags2(var1, var2.m_slopedSurfaces);
         FBORenderCutaways.s_slopedSurfacePool.releaseAll(var2.m_slopedSurfaces);
         var2.m_slopedSurfaces.clear();
         if (var1 >= this.m_chunk.minLevel && var1 <= this.m_chunk.maxLevel) {
            IsoGridSquare[] var3 = this.m_chunk.squares[this.m_chunk.squaresIndexOfLevel(var1)];
            byte var4 = 8;

            int var5;
            SlopedSurface var6;
            int var7;
            IsoGridSquare var8;
            for(var5 = 0; var5 < var4; ++var5) {
               var6 = null;

               for(var7 = 0; var7 < var4; ++var7) {
                  var8 = var3[var7 + var5 * var4];
                  if (var8 != null && (var8.getSlopedSurfaceDirection() == IsoDirections.W || var8.getSlopedSurfaceDirection() == IsoDirections.E)) {
                     if (var6 == null) {
                        var6 = (SlopedSurface)FBORenderCutaways.s_slopedSurfacePool.alloc();
                        var6.chunkLevelData = var2;
                        var6.x1 = var8.x;
                        var6.y1 = var8.y;
                        Arrays.fill(var6.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
                     }
                  } else if (var6 != null) {
                     var6.x2 = this.m_chunk.wx * var4 + var7;
                     var6.y2 = this.m_chunk.wy * var4 + var5;
                     var2.m_slopedSurfaces.add(var6);
                     var6 = null;
                  }
               }

               if (var6 != null) {
                  var6.x2 = this.m_chunk.wx * var4 + var4;
                  var6.y2 = this.m_chunk.wy * var4 + var5;
                  var2.m_slopedSurfaces.add(var6);
               }
            }

            for(var5 = 0; var5 < var4; ++var5) {
               var6 = null;

               for(var7 = 0; var7 < var4; ++var7) {
                  var8 = var3[var5 + var7 * var4];
                  if (var8 != null && (var8.getSlopedSurfaceDirection() == IsoDirections.N || var8.getSlopedSurfaceDirection() == IsoDirections.S)) {
                     if (var6 == null) {
                        var6 = (SlopedSurface)FBORenderCutaways.s_slopedSurfacePool.alloc();
                        var6.chunkLevelData = var2;
                        var6.x1 = var8.x;
                        var6.y1 = var8.y;
                        Arrays.fill(var6.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
                     }
                  } else if (var6 != null) {
                     var6.x2 = this.m_chunk.wx * var4 + var5;
                     var6.y2 = this.m_chunk.wy * var4 + var7;
                     var2.m_slopedSurfaces.add(var6);
                     var6 = null;
                  }
               }

               if (var6 != null) {
                  var6.x2 = this.m_chunk.wx * var4 + var5;
                  var6.y2 = this.m_chunk.wy * var4 + var4;
                  var2.m_slopedSurfaces.add(var6);
               }
            }

         }
      }

      void clearPlayerCutawayFlags(int var1, ArrayList<CutawayWall> var2) {
         int var3 = 0;

         int var4;
         for(var4 = 0; var4 < var2.size(); ++var4) {
            CutawayWall var5 = (CutawayWall)var2.get(var4);

            for(int var6 = 0; var6 < 4; ++var6) {
               if (var5.isPlayerInRange(var6, FBORenderCutaways.PlayerInRange.True)) {
                  var5.setPlayerCutawayFlag(var6, false);
                  var3 |= 1 << var6;
               }
            }
         }

         if (var3 != 0) {
            for(var4 = 0; var4 < 4; ++var4) {
               if ((var3 & 1 << var4) != 0) {
                  this.m_chunk.getRenderLevels(var4).invalidateLevel(var1, FBORenderChunk.DIRTY_CUTAWAYS);
               }
            }
         }

      }

      void clearPlayerCutawayFlags2(int var1, ArrayList<SlopedSurface> var2) {
         int var3 = 0;

         int var4;
         for(var4 = 0; var4 < var2.size(); ++var4) {
            SlopedSurface var5 = (SlopedSurface)var2.get(var4);

            for(int var6 = 0; var6 < 4; ++var6) {
               if (var5.isPlayerInRange(var6, FBORenderCutaways.PlayerInRange.True)) {
                  var5.setPlayerCutawayFlag(var6, false);
                  var3 |= 1 << var6;
               }
            }
         }

         if (var3 != 0) {
            for(var4 = 0; var4 < 4; ++var4) {
               if ((var3 & 1 << var4) != 0) {
                  this.m_chunk.getRenderLevels(var4).invalidateLevel(var1, FBORenderChunk.DIRTY_CUTAWAYS);
               }
            }
         }

      }

      boolean isAdjacentToRoom(IsoGridSquare var1, IsoDirections var2) {
         IsoGridSquare var3 = var1.getAdjacentSquare(var2);
         if (var3 != null && (var3.getBuilding() != null || var3.roofHideBuilding != null || this.hasRoomBelow(var3))) {
            return true;
         } else {
            return var1.getRoom() != null || var1.roofHideBuilding != null || this.hasRoomBelow(var1);
         }
      }

      boolean hasRoomBelow(IsoGridSquare var1) {
         if (var1 != null && var1.chunk != null) {
            if (var1.getZ() == 0) {
               return false;
            } else {
               for(int var2 = var1.z - 1; var2 >= var1.chunk.minLevel; --var2) {
                  IsoGridSquare var3 = var1.getCell().getGridSquare(var1.x, var1.y, var2);
                  if (var3 != null && (var3.getBuilding() != null || var3.roofHideBuilding != null)) {
                     return true;
                  }
               }

               return false;
            }
         } else {
            return false;
         }
      }

      public void invalidateOccludedSquaresMaskForSeenRooms(int var1, int var2) {
         ChunkLevelData var3 = this.getDataForLevel(var2);

         for(int var4 = 0; var4 < var3.m_allWalls.size(); ++var4) {
            CutawayWall var5 = (CutawayWall)var3.m_allWalls.get(var4);
            var5.occludedSquaresMaskForSeenRooms[var1] = 0;
         }

      }

      public void removeFromWorld() {
         Iterator var1 = this.m_levelData.valueCollection().iterator();

         while(var1.hasNext()) {
            ChunkLevelData var2 = (ChunkLevelData)var1.next();
            var2.removeFromWorld();
         }

      }

      public void debugRender(int var1) {
         ChunkLevelData var2 = this.getDataForLevel(var1);
         var2.debugRender();
      }
   }

   public static final class ChunkLevelData {
      public ChunkLevelsData m_levelsData;
      public final int m_level;
      public int m_adjacentChunkLoadedCounter = 0;
      public final ArrayList<CutawayWall> m_exteriorWalls = new ArrayList();
      public final ArrayList<CutawayWall> m_allWalls = new ArrayList();
      public final OrphanStructures m_orphanStructures = new OrphanStructures();
      public final ArrayList<SlopedSurface> m_slopedSurfaces = new ArrayList();
      public final byte[][] m_squareFlags = new byte[4][64];
      public boolean bHasCutawayNorthWallsOnWestEdge = false;
      public boolean bHasCutawayNorthWallsOnEastEdge = false;
      public boolean bHasCutawayWestWallsOnNorthEdge = false;
      public boolean bHasCutawayWestWallsOnSouthEdge = false;
      public final long[] m_occludingSquares = new long[4];

      ChunkLevelData(int var1) {
         this.m_level = var1;
         this.m_orphanStructures.chunkLevelData = this;
      }

      public boolean shouldRenderSquare(int var1, IsoGridSquare var2) {
         if (var2 != null && var2.chunk != null && var2.z == this.m_level) {
            int var3 = var2.x - var2.chunk.wx * 8;
            int var4 = var2.y - var2.chunk.wy * 8;
            return (this.m_squareFlags[var1][var3 + var4 * 8] & 1) != 0;
         } else {
            return false;
         }
      }

      public boolean calculateOccludingSquares(int var1, int var2, int var3, int var4, int var5, int[] var6) {
         int var7 = var4 - var2 + 1;
         long var8 = this.m_occludingSquares[var1];
         this.m_occludingSquares[var1] = 0L;
         IsoChunk var10 = this.m_levelsData.m_chunk;
         IsoGridSquare[] var11 = var10.getSquaresForLevel(this.m_level);

         for(int var12 = 0; var12 < 64; ++var12) {
            IsoGridSquare var13 = var11[var12];
            if (var13 != null) {
               int var14 = var13.x - var13.z * 3 - var2;
               int var15 = var13.y - var13.z * 3 - var3;
               if (var14 >= 0 && var15 >= 0 && var14 <= var4 - var2 && var15 <= var5 - var3 && this.shouldRenderSquare(var1, var13) && (var13.getVisionMatrix(0, 0, -1) || var10.getGridSquare(var12 % 8, var12 / 8, var13.z - 1) == null)) {
                  int var16 = var14 + var15 * var7;
                  var6[var16] = PZMath.max(var6[var16], var13.z);
                  int var17 = var12 % 8;
                  int var18 = var12 / 8;
                  int var19 = 1 << var17 + var18 * 8;
                  long[] var10000 = this.m_occludingSquares;
                  var10000[var1] |= (long)var19;
               }
            }
         }

         return this.m_occludingSquares[var1] != var8;
      }

      void removeFromWorld() {
         this.m_adjacentChunkLoadedCounter = 0;
         FBORenderCutaways.s_cutawayWallPool.releaseAll(this.m_exteriorWalls);
         this.m_exteriorWalls.clear();
         FBORenderCutaways.s_cutawayWallPool.releaseAll(this.m_allWalls);
         this.m_allWalls.clear();

         for(int var1 = 0; var1 < 4; ++var1) {
            Arrays.fill(this.m_squareFlags[var1], (byte)0);
            this.m_occludingSquares[var1] = 0L;
         }

         this.m_orphanStructures.resetForStore();
         this.bHasCutawayNorthWallsOnWestEdge = false;
         this.bHasCutawayNorthWallsOnEastEdge = false;
         this.bHasCutawayWestWallsOnNorthEdge = false;
         this.bHasCutawayWestWallsOnSouthEdge = false;
      }

      void debugRender() {
         ArrayList var1 = this.m_allWalls;

         int var2;
         int var4;
         IsoGridSquare var5;
         float var6;
         float var7;
         float var8;
         for(var2 = 0; var2 < var1.size(); ++var2) {
            CutawayWall var3 = (CutawayWall)var1.get(var2);
            if (var3.isHorizontal()) {
               for(var4 = var3.x1; var4 < var3.x2; ++var4) {
                  var5 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var3.y1, this.m_level);
                  var6 = 0.0F;
                  var7 = 1.0F;
                  var8 = 0.0F;
                  if (var5 != null && (var5.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 1) != 0) {
                     var6 = 1.0F;
                  }

                  if (var3.isPlayerInRange(IsoCamera.frameState.playerIndex, FBORenderCutaways.PlayerInRange.True)) {
                     var8 = 1.0F;
                  }

                  LineDrawer.addLine((float)var4 + (var4 == var3.x1 ? 0.05F : 0.0F), (float)var3.y1, (float)this.m_level, (float)(var4 + 1) - (var4 == var3.x2 - 1 ? 0.05F : 0.0F), (float)var3.y2, (float)this.m_level, var6, var7, var8, 1.0F);
               }
            } else {
               for(var4 = var3.y1; var4 < var3.y2; ++var4) {
                  var5 = IsoWorld.instance.CurrentCell.getGridSquare(var3.x1, var4, this.m_level);
                  var6 = 0.0F;
                  var7 = 1.0F;
                  var8 = 0.0F;
                  if (var5 != null && (var5.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 2) != 0) {
                     var6 = 1.0F;
                  }

                  if (var3.isPlayerInRange(IsoCamera.frameState.playerIndex, FBORenderCutaways.PlayerInRange.True)) {
                     var8 = 1.0F;
                  }

                  LineDrawer.addLine((float)var3.x1, (float)var4 + (var4 == var3.y1 ? 0.05F : 0.0F), (float)this.m_level, (float)var3.x1, (float)(var4 + 1) - (var4 == var3.y2 - 1 ? 0.05F : 0.0F), (float)this.m_level, var6, var7, var8, 1.0F);
               }
            }
         }

         for(var2 = 0; var2 < this.m_slopedSurfaces.size(); ++var2) {
            SlopedSurface var9 = (SlopedSurface)this.m_slopedSurfaces.get(var2);
            if (var9.isHorizontal()) {
               for(var4 = var9.x1; var4 < var9.x2; ++var4) {
                  var5 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var9.y1, this.m_level);
                  var6 = 0.0F;
                  var7 = 1.0F;
                  var8 = 0.0F;
                  if (var5 != null && (var5.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 1) != 0) {
                     var6 = 1.0F;
                  }

                  LineDrawer.addLine((float)var4 + (var4 == var9.x1 ? 0.05F : 0.0F), (float)var9.y1, (float)this.m_level, (float)(var4 + 1) - (var4 == var9.x2 - 1 ? 0.05F : 0.0F), (float)var9.y2, (float)this.m_level, var6, var7, var8, 1.0F);
               }
            } else {
               for(var4 = var9.y1; var4 < var9.y2; ++var4) {
                  var5 = IsoWorld.instance.CurrentCell.getGridSquare(var9.x1, var4, this.m_level);
                  var6 = 0.0F;
                  var7 = 1.0F;
                  var8 = 0.0F;
                  if (var5 != null && (var5.getPlayerCutawayFlag(IsoCamera.frameState.playerIndex, 0L) & 2) != 0) {
                     var6 = 1.0F;
                  }

                  if (var9.isPlayerInRange(IsoCamera.frameState.playerIndex, FBORenderCutaways.PlayerInRange.True)) {
                     var8 = 1.0F;
                  }

                  LineDrawer.addLine((float)var9.x1, (float)var4 + (var4 == var9.y1 ? 0.05F : 0.0F), (float)this.m_level, (float)var9.x1, (float)(var4 + 1) - (var4 == var9.y2 - 1 ? 0.05F : 0.0F), (float)this.m_level, var6, var7, var8, 1.0F);
               }
            }
         }

      }
   }

   public static final class OrphanStructures {
      ChunkLevelData chunkLevelData;
      final PlayerInRange[] playerInRange = new PlayerInRange[4];
      boolean hasOrphanStructures = false;
      int m_adjacentChunkLoadedCounter = 0;

      public OrphanStructures() {
      }

      void calculate(IsoChunk var1) {
         Arrays.fill(this.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
         this.hasOrphanStructures = false;
         if (this.chunkLevelData.m_level >= var1.minLevel && this.chunkLevelData.m_level <= var1.maxLevel) {
            int var2 = var1.squaresIndexOfLevel(this.chunkLevelData.m_level);
            IsoGridSquare[] var3 = var1.squares[var2];

            for(int var4 = 0; var4 < var3.length; ++var4) {
               IsoGridSquare var5 = var3[var4];
               if (this.isOrphanStructureSquare(var5)) {
                  this.hasOrphanStructures = true;
                  break;
               }
            }

         }
      }

      boolean isOrphanStructureSquare(IsoGridSquare var1) {
         if (var1 == null) {
            return false;
         } else {
            IsoBuilding var2 = var1.getBuilding();
            if (var2 == null) {
               var2 = var1.roofHideBuilding;
               if (var2 != null && var2.isEntirelyEmptyOutside()) {
                  return true;
               }
            }

            IsoGridSquare var4;
            for(int var3 = var1.getZ() - 1; var3 >= 0 && var2 == null; --var3) {
               var4 = var1.getCell().getGridSquare(var1.x, var1.y, var3);
               if (var4 != null) {
                  var2 = var4.getBuilding();
                  if (var2 == null) {
                     var2 = var4.roofHideBuilding;
                  }
               }
            }

            if (var2 != null) {
               return false;
            } else if (var1.associatedBuilding != null) {
               return false;
            } else if (var1.getPlayerBuiltFloor() != null) {
               return true;
            } else {
               IsoGridSquare var6 = var1.nav[IsoDirections.N.index()];
               if (var6 != null && var6.getBuilding() == null) {
                  if (var6.getPlayerBuiltFloor() != null) {
                     return true;
                  }

                  if (var6.HasStairsBelow()) {
                     return true;
                  }
               }

               var4 = var1.nav[IsoDirections.W.index()];
               if (var4 != null && var4.getBuilding() == null) {
                  if (var4.getPlayerBuiltFloor() != null) {
                     return true;
                  }

                  if (var4.HasStairsBelow()) {
                     return true;
                  }
               }

               if (var1.Is(IsoFlagType.WallSE)) {
                  IsoGridSquare var5 = var1.nav[IsoDirections.NW.index()];
                  if (var5 != null && var5.getBuilding() == null) {
                     if (var5.getPlayerBuiltFloor() != null) {
                        return true;
                     }

                     if (var5.HasStairsBelow()) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      }

      boolean shouldCutaway() {
         if (IsoWorld.instance.CurrentCell.bOccludedByOrphanStructureFlag) {
            return this.chunkLevelData.m_level > PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
         } else {
            return false;
         }
      }

      void setPlayerInRange(int var1, PlayerInRange var2) {
         this.playerInRange[var1] = var2;
      }

      boolean isPlayerInRange(int var1, PlayerInRange var2) {
         return this.playerInRange[var1] == var2;
      }

      void resetForStore() {
         Arrays.fill(this.playerInRange, FBORenderCutaways.PlayerInRange.Unset);
         this.hasOrphanStructures = false;
         this.m_adjacentChunkLoadedCounter = 0;
      }
   }

   public static final class CutawayWall {
      ChunkLevelData chunkLevelData;
      public int x1;
      public int y1;
      public int x2;
      public int y2;
      final PlayerInRange[] playerInRange = new PlayerInRange[4];
      final TLongArrayList occludedRoomIDs = new TLongArrayList();
      final int[] occludedSquaresMaskForSeenRooms = new int[4];
      static final int[] NORTH_WALL_DXY = new int[]{-1, -1, -2, -2, -3, -3, 0, -1, -1, -2, -2, -3};
      static final int[] WEST_WALL_DXY = new int[]{-1, -1, -2, -2, -3, -3, -1, 0, -2, -1, -3, -2};

      public CutawayWall() {
      }

      boolean isHorizontal() {
         return this.y1 == this.y2;
      }

      void calculateOccludedRooms() {
         IsoCell var1 = IsoWorld.instance.CurrentCell;
         int var2;
         int var3;
         IsoGridSquare var4;
         long var5;
         if (this.isHorizontal()) {
            for(var2 = this.x1; var2 < this.x2; ++var2) {
               for(var3 = 0; var3 < NORTH_WALL_DXY.length - 1; var3 += 2) {
                  var4 = var1.getGridSquare(var2 + NORTH_WALL_DXY[var3], this.y1 + NORTH_WALL_DXY[var3 + 1], this.chunkLevelData.m_level);
                  if (var4 != null) {
                     var5 = var4.getRoomID();
                     if (var5 == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(var4)) {
                        var5 = var4.associatedBuilding.getRoofRoomID(var4.z);
                     }

                     if (var5 != -1L && !this.occludedRoomIDs.contains(var5)) {
                        this.occludedRoomIDs.add(var5);
                     }
                  }
               }
            }
         } else {
            for(var2 = this.y1; var2 < this.y2; ++var2) {
               for(var3 = 0; var3 < WEST_WALL_DXY.length - 1; var3 += 2) {
                  var4 = var1.getGridSquare(this.x1 + WEST_WALL_DXY[var3], var2 + WEST_WALL_DXY[var3 + 1], this.chunkLevelData.m_level);
                  if (var4 != null) {
                     var5 = var4.getRoomID();
                     if (var5 == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(var4)) {
                        var5 = var4.associatedBuilding.getRoofRoomID(var4.z);
                     }

                     if (var5 != -1L && !this.occludedRoomIDs.contains(var5)) {
                        this.occludedRoomIDs.add(var5);
                     }
                  }
               }
            }
         }

      }

      /** @deprecated */
      @Deprecated
      boolean isSquareOccludingRoom(IsoGridSquare var1, long var2) {
         int var4 = IsoCamera.frameState.playerIndex;
         IsoCell var5 = IsoWorld.instance.CurrentCell;

         for(int var6 = 1; var6 <= 3; ++var6) {
            IsoGridSquare var7 = var5.getGridSquare(var1.x - var6, var1.y - var6, var1.z);
            if (var7 != null && var7.getRoomID() == var2 && var7.isCouldSee(var4)) {
               return true;
            }
         }

         return false;
      }

      boolean shouldCutawayFence() {
         int var1 = 1;
         if (IsoCamera.frameState.playerIndex == 0 && IsoPlayer.players[0].isAiming()) {
            var1 = 2;
         }

         var1 = Math.min(var1, FBORenderCutaways.instance.pointOfInterest.size());

         for(int var2 = 0; var2 < FBORenderCutaways.instance.pointOfInterest.size(); ++var2) {
            PointOfInterest var3 = (PointOfInterest)FBORenderCutaways.instance.pointOfInterest.get(var2);
            if (var2 >= var1) {
               if (this.shouldCutawayFence(var3.getSquare(), 3)) {
                  return true;
               }
            } else if (this.shouldCutawayFence(var3.getSquare(), 6)) {
               return true;
            }
         }

         return false;
      }

      boolean shouldCutawayFence(IsoGridSquare var1, int var2) {
         if (var1 == null) {
            return false;
         } else if (!var1.isCanSee(IsoCamera.frameState.playerIndex)) {
            return false;
         } else {
            assert var1.z == this.chunkLevelData.m_level;

            if (this.isHorizontal()) {
               return var1.y < this.y1 && var1.y >= this.y1 - var2;
            } else {
               return var1.x < this.x1 && var1.x >= this.x1 - var2;
            }
         }
      }

      boolean shouldCutawayBuilding(int var1) {
         ArrayList var2 = (ArrayList)FBORenderCutaways.instance.cell.tempPlayerCutawayRoomIDs.get(var1);

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            long var4 = (Long)var2.get(var3);
            if (this.occludedRoomIDs.contains(var4)) {
               return true;
            }
         }

         return false;
      }

      int calculateOccludedSquaresMask(int var1, long var2) {
         if (!this.occludedRoomIDs.contains(var2)) {
            return 0;
         } else {
            int var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
            IsoChunkMap var5 = IsoWorld.instance.CurrentCell.getChunkMap(var1);
            int var6 = 0;
            int var7;
            int var8;
            IsoGridSquare var9;
            long var10;
            if (this.isHorizontal()) {
               for(var7 = this.x1; var7 < this.x2; ++var7) {
                  for(var8 = 0; var8 < NORTH_WALL_DXY.length - 1; var8 += 2) {
                     var9 = var5.getGridSquare(var7 + NORTH_WALL_DXY[var8], this.y1 + NORTH_WALL_DXY[var8 + 1], var4);
                     if (var9 != null && !var9.getObjects().isEmpty() && var9.isCouldSee(var1)) {
                        var10 = var9.getRoomID();
                        if (var10 == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(var9)) {
                           var10 = var9.associatedBuilding.getRoofRoomID(var4);
                        }

                        if (var10 == var2) {
                           var6 |= 1 << var7 - this.x1;
                           break;
                        }
                     }
                  }
               }
            } else {
               for(var7 = this.y1; var7 < this.y2; ++var7) {
                  for(var8 = 0; var8 < WEST_WALL_DXY.length - 1; var8 += 2) {
                     var9 = var5.getGridSquare(this.x1 + WEST_WALL_DXY[var8], var7 + WEST_WALL_DXY[var8 + 1], var4);
                     if (var9 != null && !var9.getObjects().isEmpty() && var9.isCouldSee(var1)) {
                        var10 = var9.getRoomID();
                        if (var10 == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(var9)) {
                           var10 = var9.associatedBuilding.getRoofRoomID(var4);
                        }

                        if (var10 == var2) {
                           var6 |= 1 << var7 - this.y1;
                           break;
                        }
                     }
                  }
               }
            }

            return var6;
         }
      }

      int calculateOccludedSquaresMaskForSeenRooms(int var1) {
         int var2 = 0;
         ArrayList var3 = (ArrayList)FBORenderCutaways.instance.cell.tempPlayerCutawayRoomIDs.get(var1);

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            long var5 = (Long)var3.get(var4);
            var2 |= this.calculateOccludedSquaresMask(var1, var5);
         }

         return var2;
      }

      void setPlayerInRange(int var1, PlayerInRange var2) {
         this.playerInRange[var1] = var2;
      }

      boolean isPlayerInRange(int var1, PlayerInRange var2) {
         return this.playerInRange[var1] == var2;
      }

      void setPlayerCutawayFlag(int var1, boolean var2) {
         int var3;
         IsoGridSquare var4;
         if (this.isHorizontal()) {
            for(var3 = this.x1; var3 < this.x2; ++var3) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare(var3, this.y1, this.chunkLevelData.m_level);
               if (var4 != null) {
                  if (var2) {
                     var4.addPlayerCutawayFlag(var1, 1, 0L);
                  } else {
                     var4.clearPlayerCutawayFlag(var1, 1, 0L);
                  }
               }
            }
         } else {
            for(var3 = this.y1; var3 < this.y2; ++var3) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare(this.x1, var3, this.chunkLevelData.m_level);
               if (var4 != null) {
                  if (var2) {
                     var4.addPlayerCutawayFlag(var1, 2, 0L);
                  } else {
                     var4.clearPlayerCutawayFlag(var1, 2, 0L);
                  }
               }
            }
         }

      }

      void setVisitedSquares(PerPlayerData var1) {
         int var2;
         IsoGridSquare var3;
         if (this.isHorizontal()) {
            for(var2 = this.x1; var2 < this.x2; ++var2) {
               var3 = IsoWorld.instance.CurrentCell.getGridSquare(var2, this.y1, this.chunkLevelData.m_level);
               if (var3 != null) {
                  var1.cutawayVisitorResultsNorth.add(var3);
               }
            }
         } else {
            for(var2 = this.y1; var2 < this.y2; ++var2) {
               var3 = IsoWorld.instance.CurrentCell.getGridSquare(this.x1, var2, this.chunkLevelData.m_level);
               if (var3 != null) {
                  var1.cutawayVisitorResultsWest.add(var3);
               }
            }
         }

      }
   }

   static enum PlayerInRange {
      Unset,
      True,
      False;

      private PlayerInRange() {
      }
   }

   public static final class SlopedSurface {
      ChunkLevelData chunkLevelData;
      public int x1;
      public int y1;
      public int x2;
      public int y2;
      final PlayerInRange[] playerInRange = new PlayerInRange[4];

      public SlopedSurface() {
      }

      boolean isHorizontal() {
         return this.x2 > this.x1;
      }

      void setPlayerInRange(int var1, PlayerInRange var2) {
         this.playerInRange[var1] = var2;
      }

      boolean isPlayerInRange(int var1, PlayerInRange var2) {
         return this.playerInRange[var1] == var2;
      }

      boolean shouldCutaway() {
         int var1 = 1;
         if (IsoCamera.frameState.playerIndex == 0 && IsoPlayer.players[0].isAiming()) {
            var1 = 2;
         }

         var1 = Math.min(var1, FBORenderCutaways.instance.pointOfInterest.size());

         for(int var2 = 0; var2 < FBORenderCutaways.instance.pointOfInterest.size(); ++var2) {
            PointOfInterest var3 = (PointOfInterest)FBORenderCutaways.instance.pointOfInterest.get(var2);
            if (var2 >= var1) {
               if (this.shouldCutaway(var3.getSquare(), 3)) {
                  return true;
               }
            } else if (this.shouldCutaway(var3.getSquare(), 6)) {
               return true;
            }
         }

         return false;
      }

      boolean shouldCutaway(IsoGridSquare var1, int var2) {
         if (var1 == null) {
            return false;
         } else if (IsoCamera.frameState.CamCharacterSquare != null && IsoCamera.frameState.CamCharacterSquare.hasSlopedSurface()) {
            return false;
         } else if (!var1.isCanSee(IsoCamera.frameState.playerIndex)) {
            return false;
         } else {
            assert var1.z == this.chunkLevelData.m_level;

            if (this.isHorizontal()) {
               return var1.y < this.y1 && var1.y >= this.y1 - var2;
            } else {
               return var1.x < this.x1 && var1.x >= this.x1 - var2 && var1.y >= this.y1 && var1.y < this.y2;
            }
         }
      }

      void setPlayerCutawayFlag(int var1, boolean var2) {
         int var3;
         IsoGridSquare var4;
         if (this.isHorizontal()) {
            for(var3 = this.x1; var3 <= this.x2; ++var3) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare(var3, this.y1, this.chunkLevelData.m_level);
               if (var4 != null) {
                  if (var2) {
                     var4.addPlayerCutawayFlag(var1, 1, 0L);
                  } else {
                     var4.clearPlayerCutawayFlag(var1, 1, 0L);
                  }
               }
            }
         } else {
            for(var3 = this.y1; var3 <= this.y2; ++var3) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare(this.x1, var3, this.chunkLevelData.m_level);
               if (var4 != null) {
                  if (var2) {
                     var4.addPlayerCutawayFlag(var1, 2, 0L);
                  } else {
                     var4.clearPlayerCutawayFlag(var1, 2, 0L);
                  }
               }
            }
         }

      }
   }

   public static final class PointOfInterest {
      public int x;
      public int y;
      public int z;

      public PointOfInterest() {
      }

      public IsoGridSquare getSquare() {
         return IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
      }
   }

   private static final class BuildingsToCollapse {
      final ArrayList<BuildingDef> buildingsToCollapse = new ArrayList();
      final ArrayList<BuildingDef> tempLastBuildingsToCollapse = new ArrayList();
      final TObjectIntHashMap<BuildingDef> maxVisibleLevel = new TObjectIntHashMap();

      private BuildingsToCollapse() {
      }
   }
}
