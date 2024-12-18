package zombie.iso;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;

public class NewMapBinaryFile {
   public final boolean m_pot;
   public final int CHUNK_DIM;
   public final int CHUNKS_PER_CELL;
   public final int CELL_DIM;
   private final ArrayList<RoomDef> tempRooms = new ArrayList();
   private final SharedStrings m_sharedStrings = new SharedStrings();

   public NewMapBinaryFile(boolean var1) {
      this.m_pot = var1;
      this.CHUNK_DIM = var1 ? 8 : 10;
      this.CHUNKS_PER_CELL = var1 ? 32 : 30;
      this.CELL_DIM = var1 ? 256 : 300;
   }

   public static void SpawnBasement(String var0, int var1, int var2) throws IOException {
      NewMapBinaryFile var3 = new NewMapBinaryFile(true);
      String var4 = "media/binmap/" + var0 + ".pzby";
      Header var5 = var3.loadHeader(var4);
      IsoPlayer var6 = IsoPlayer.getInstance();
      var6.ensureOnTile();
      IsoChunk var7 = var6.getSquare().chunk;
      int var8 = var7.wx * 8 / IsoCell.CellSizeInSquares;
      int var9 = var7.wy * 8 / IsoCell.CellSizeInSquares;
      IsoMetaCell var10 = IsoWorld.instance.MetaGrid.getCellData(var8, var9);
      if (var10 != null) {
         int var11 = var1 / 8;
         int var12 = var2 / 8;
         int var13 = var1 / 8 + var5.m_width - 1;
         int var14 = var2 / 8 + var5.m_height - 1;
         if (var11 / IsoCell.CellSizeInChunks == var13 / IsoCell.CellSizeInChunks) {
            if (var12 / IsoCell.CellSizeInChunks == var14 / IsoCell.CellSizeInChunks) {
               var3.addBasementRoomsToMetaGrid(var10, var5, var1, var2);

               int var15;
               int var16;
               for(var15 = 0; var15 < var5.m_height; ++var15) {
                  for(var16 = 0; var16 < var5.m_width; ++var16) {
                     ChunkData var17 = var3.loadChunk(var5, var16, var15);
                     var3.setChunkInWorldArb(var17, 0, 0, 0, var1 + var16 * var3.CHUNK_DIM, var2 + var15 * var3.CHUNK_DIM, -var5.m_levels);
                  }
               }

               for(var15 = var11; var15 <= var13; ++var15) {
                  for(var16 = var12; var16 <= var14; ++var16) {
                     IsoChunk var22 = IsoCell.getInstance().getChunk(var15, var16);
                     if (var22 != null) {
                        Iterator var18 = var22.roomLights.iterator();

                        while(var18.hasNext()) {
                           IsoRoomLight var19 = (IsoRoomLight)var18.next();
                           if (!IsoCell.getInstance().roomLights.contains(var19)) {
                              IsoCell.getInstance().roomLights.add(var19);
                           }
                        }
                     }
                  }
               }

               for(var15 = var1; var15 < var1 + var5.m_width * var3.CHUNK_DIM; ++var15) {
                  for(var16 = var2; var16 < var2 + var5.m_height * var3.CHUNK_DIM; ++var16) {
                     IsoGridSquare var23 = IsoCell.getInstance().getGridSquare(var15, var16, 0);
                     if (var23 != null && var23.HasStairsBelow()) {
                        IsoGridSquare var24 = var23;
                        IsoObject[] var25 = (IsoObject[])var23.getObjects().getElements();

                        for(int var20 = 0; var20 < var24.getObjects().size(); ++var20) {
                           IsoObject var21 = var25[var20];
                           if (var21.isFloor()) {
                              var24.DeleteTileObject(var21);
                              break;
                           }
                        }
                     }
                  }
               }

               IsoCell.getInstance().ChunkMap[IsoPlayer.getPlayerIndex()].calculateZExtentsForChunkMap();
            }
         }
      }
   }

   public static void SpawnBasementInChunk(IsoChunk var0, String var1, int var2, int var3, int var4) throws IOException {
      DebugLog.Basement.println("SpawnBasementInChunk : " + var1 + ", at: " + var2 + ", " + var3);
      boolean var5 = var1.startsWith("ba_");
      NewMapBinaryFile var6 = new NewMapBinaryFile(true);
      String var7 = "media/binmap/" + var1 + ".pzby";
      if (var5) {
         var7 = "media/basement_access/" + var1 + ".pzby";
      }

      Header var8 = var6.loadHeader(var7);
      byte var9 = 8;
      int var10 = var0.wx * var9;
      int var11 = var0.wy * var9;
      int var12 = Math.max(var10, var2) - var2;
      int var13 = Math.max(var11, var3) - var3;
      int var14 = Math.min(var10 + var9, var2 + var8.m_width * var6.CHUNK_DIM) - 1 - var2;
      int var15 = Math.min(var11 + var9, var3 + var8.m_height * var6.CHUNK_DIM) - 1 - var3;

      for(int var16 = var13 / var6.CHUNK_DIM; var16 <= var15 / var6.CHUNK_DIM; ++var16) {
         for(int var17 = var12 / var6.CHUNK_DIM; var17 <= var14 / var6.CHUNK_DIM; ++var17) {
            ChunkData var18 = var6.loadChunk(var8, var17, var16);
            var6.addToIsoChunk(var0, var18, var2 + var17 * var6.CHUNK_DIM, var3 + var16 * var6.CHUNK_DIM, var4, var5);
         }
      }

   }

   private boolean addBasementRoomsToMetaGrid(IsoMetaCell var1, Header var2, int var3, int var4) {
      ArrayList var5 = new ArrayList();
      var1.getRoomsIntersecting(var3, var4, var2.m_width * var2.m_file.CHUNK_DIM, var2.m_height * var2.m_file.CHUNK_DIM, var5);
      int var6 = var3 / IsoCell.CellSizeInSquares;
      int var7 = var4 / IsoCell.CellSizeInSquares;
      if (var5.isEmpty()) {
         return false;
      } else {
         BuildingDef var8 = ((RoomDef)var5.get(0)).building;
         int var10000 = var3 - var8.x;
         var10000 = var4 - var8.y;
         ArrayList var11 = var2.m_roomDefList;

         for(int var12 = 0; var12 < var11.size(); ++var12) {
            RoomDef var13 = (RoomDef)var11.get(var12);
            RoomDef var14 = new RoomDef(RoomID.makeID(var1.info.cellX, var1.info.cellY, var1.info.Rooms.size()), var13.name);
            var14.level = var13.level - var2.m_levels;
            Iterator var15 = var13.rects.iterator();

            while(var15.hasNext()) {
               RoomDef.RoomRect var16 = (RoomDef.RoomRect)var15.next();
               var14.rects.add(new RoomDef.RoomRect(var16.x + var3, var16.y + var4, var16.getW(), var16.getH()));
            }

            var14.CalculateBounds();
            var14.building = var8;
            var8.rooms.add(var14);
            var1.addRoom(var14, var6 * IsoCell.CellSizeInSquares, var7 * IsoCell.CellSizeInSquares);
            var1.info.Rooms.put(var14.ID, var14);
            IsoRoom var19 = var14.getIsoRoom();
            var19.createLights(false);
            IsoChunk var20 = IsoCell.getInstance().getChunk(var14.x / 8, var14.y / 8);
            if (var20 != null) {
               Iterator var17 = var19.roomLights.iterator();

               while(var17.hasNext()) {
                  IsoRoomLight var18 = (IsoRoomLight)var17.next();
                  var20.roomLights.add(var18);
               }
            }
         }

         var8.CalculateBounds(new ArrayList(var8.rooms));
         return true;
      }
   }

   private void mergeBuildingsIntoMetaGrid(IsoMetaCell var1, Header var2, int var3, int var4) {
      ArrayList var5 = new ArrayList();
      var1.getRoomsIntersecting(var3 * 8, var4 * 8, var2.m_width * var2.m_file.CHUNK_DIM, var2.m_height * var2.m_file.CHUNK_DIM, var5);

      int var6;
      RoomDef var7;
      for(var6 = 0; var6 < var5.size(); ++var6) {
         var7 = (RoomDef)var5.get(var6);
         var1.info.RoomList.remove(var7);
         var1.info.Rooms.remove(var7.ID);
         var1.info.isoRooms.remove(var7.ID);
         var1.info.Buildings.remove(var7.building);
         var1.info.isoBuildings.remove(var7.building.ID);
      }

      for(var6 = 0; var6 < var2.m_buildingDefList.size(); ++var6) {
         BuildingDef var8 = (BuildingDef)var2.m_buildingDefList.get(var6);
         var8.ID = BuildingID.makeID(var1.getX(), var1.getY(), var1.info.Buildings.size() + var6);
         var1.info.Buildings.add(var8);
      }

      for(var6 = 0; var6 < var2.m_roomDefList.size(); ++var6) {
         var7 = (RoomDef)var2.m_roomDefList.get(var6);
         var7.ID = RoomID.makeID(var1.getX(), var1.getY(), var1.info.RoomList.size() + var6);
         var1.info.RoomList.add(var7);
         var1.info.Rooms.put(var7.ID, var7);
      }

   }

   public Header loadHeader(String var1) throws IOException {
      var1 = ZomboidFileSystem.instance.getString(var1);
      BufferedRandomAccessFile var2 = new BufferedRandomAccessFile(var1, "r", 4096);

      Header var3;
      try {
         var3 = this.loadHeaderInternal(var1, var2);
      } catch (Throwable var6) {
         try {
            var2.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      var2.close();
      return var3;
   }

   private Header loadHeaderInternal(String var1, BufferedRandomAccessFile var2) throws IOException {
      Header var3 = new Header();
      var3.m_file = this;
      var3.m_fileName = var1;
      int var4 = var2.read();
      int var5 = var2.read();
      int var6 = var2.read();
      int var7 = var2.read();
      if (var4 != 80 || var5 != 90 || var6 != 66 || var7 != 89) {
         throw new IOException("unrecognized file format");
      } else {
         var3.m_version = IsoLot.readInt(var2);
         int var8 = IsoLot.readInt(var2);

         int var9;
         for(var9 = 0; var9 < var8; ++var9) {
            String var10 = IsoLot.readString(var2);
            var3.m_usedTileNames.add(this.m_sharedStrings.get(var10.trim()));
         }

         var3.m_width = IsoLot.readInt(var2);
         var3.m_height = IsoLot.readInt(var2);
         var3.m_levels = IsoLot.readInt(var2);
         var9 = IsoLot.readInt(var2);

         int var13;
         int var19;
         for(var19 = 0; var19 < var9; ++var19) {
            String var11 = IsoLot.readString(var2);
            RoomDef var12 = new RoomDef((long)var19, this.m_sharedStrings.get(var11));
            var12.level = IsoLot.readInt(var2);
            var13 = IsoLot.readInt(var2);

            int var14;
            for(var14 = 0; var14 < var13; ++var14) {
               RoomDef.RoomRect var15 = new RoomDef.RoomRect(IsoLot.readInt(var2), IsoLot.readInt(var2), IsoLot.readInt(var2), IsoLot.readInt(var2));
               var12.rects.add(var15);
            }

            var12.CalculateBounds();
            var3.m_roomDefMap.put(var12.ID, var12);
            var3.m_roomDefList.add(var12);
            var14 = IsoLot.readInt(var2);

            for(int var23 = 0; var23 < var14; ++var23) {
               int var16 = IsoLot.readInt(var2);
               int var17 = IsoLot.readInt(var2);
               int var18 = IsoLot.readInt(var2);
               var12.objects.add(new MetaObject(var16, var17 - var12.x, var18 - var12.y, var12));
            }

            var12.bLightsActive = Rand.Next(2) == 0;
         }

         var19 = IsoLot.readInt(var2);

         for(int var20 = 0; var20 < var19; ++var20) {
            BuildingDef var21 = new BuildingDef();
            var9 = IsoLot.readInt(var2);
            var21.ID = (long)var20;

            for(var13 = 0; var13 < var9; ++var13) {
               RoomDef var22 = (RoomDef)var3.m_roomDefMap.get((long)IsoLot.readInt(var2));
               var22.building = var21;
               if (var22.isEmptyOutside()) {
                  var21.emptyoutside.add(var22);
               } else {
                  var21.rooms.add(var22);
               }
            }

            var21.CalculateBounds(this.tempRooms);
            var3.m_buildingDefList.add(var21);
         }

         var3.m_chunkTablePosition = var2.getFilePointer();
         return var3;
      }
   }

   public ChunkData loadChunk(Header var1, int var2, int var3) throws IOException {
      BufferedRandomAccessFile var4 = new BufferedRandomAccessFile(var1.m_fileName, "r", 4096);

      ChunkData var5;
      try {
         var5 = this.loadChunkInner(var4, var1, var2, var3);
      } catch (Throwable var8) {
         try {
            var4.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      var4.close();
      return var5;
   }

   private ChunkData loadChunkInner(RandomAccessFile var1, Header var2, int var3, int var4) throws IOException {
      ChunkData var5 = new ChunkData();
      var5.m_header = var2;
      var5.data = new int[this.CHUNK_DIM][this.CHUNK_DIM][var2.m_levels][];
      int var6 = var4 * var2.m_width + var3;
      var1.seek(var2.m_chunkTablePosition + (long)(var6 * 8));
      int var7 = IsoLot.readInt(var1);
      var1.seek((long)var7);
      int var8 = 0;
      var5.attributes = new int[var2.m_levels][this.CHUNK_DIM * this.CHUNK_DIM];

      int var9;
      int var10;
      int var11;
      int var12;
      for(var9 = 0; var9 < var2.m_levels; ++var9) {
         for(var10 = 0; var10 < this.CHUNK_DIM; ++var10) {
            for(var11 = 0; var11 < this.CHUNK_DIM; ++var11) {
               if (var8 > 0) {
                  --var8;
                  var5.attributes[var9][var10 + var11 * this.CHUNK_DIM] = 0;
               } else {
                  var12 = IsoLot.readInt(var1);
                  if (var12 == -1) {
                     var8 = IsoLot.readInt(var1);
                     if (var8 > 0) {
                        --var8;
                        var5.attributes[var9][var10 + var11 * this.CHUNK_DIM] = 0;
                        continue;
                     }
                  }

                  if (var12 > 1) {
                     var5.attributes[var9][var10 + var11 * this.CHUNK_DIM] = IsoLot.readInt(var1);
                  } else {
                     var5.attributes[var9][var10 + var11 * this.CHUNK_DIM] = 0;
                  }
               }
            }
         }
      }

      var8 = 0;

      for(var9 = 0; var9 < var2.m_levels; ++var9) {
         for(var10 = 0; var10 < this.CHUNK_DIM; ++var10) {
            for(var11 = 0; var11 < this.CHUNK_DIM; ++var11) {
               if (var8 > 0) {
                  --var8;
                  var5.data[var10][var11][var9] = null;
               } else {
                  var12 = IsoLot.readInt(var1);
                  if (var12 == -1) {
                     var8 = IsoLot.readInt(var1);
                     if (var8 > 0) {
                        --var8;
                        var5.data[var10][var11][var9] = null;
                        continue;
                     }
                  }

                  if (var12 > 1) {
                     var5.data[var10][var11][var9] = new int[var12 - 1];

                     for(int var13 = 1; var13 < var12; ++var13) {
                        var5.data[var10][var11][var9][var13 - 1] = IsoLot.readInt(var1);
                     }
                  } else {
                     var5.data[var10][var11][var9] = null;
                  }
               }
            }
         }
      }

      return var5;
   }

   public void setChunkInWorldArb(ChunkData var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      IsoCell var8 = IsoWorld.instance.CurrentCell;

      int var10;
      int var11;
      try {
         this.setChunkInWorldInnerArb(var1, var2, var3, var4, var5, var6, var7);

         for(int var9 = var5; var9 < var5 + var1.m_header.m_file.CHUNK_DIM; ++var9) {
            for(var10 = var6; var10 < var6 + var1.m_header.m_file.CHUNK_DIM; ++var10) {
               for(var11 = var7; var11 < var7 + var1.m_header.m_levels; ++var11) {
                  IsoGridSquare var15 = IsoCell.getInstance().getGridSquare(var9, var10, var11);
                  if (var15 != null) {
                     var15.RecalcAllWithNeighbours(true);
                     if (var15.HasStairsBelow()) {
                        var15.removeUnderground();
                     }
                  }
               }
            }
         }
      } catch (Exception var14) {
         DebugLog.log("Failed to load chunk, blocking out area");
         ExceptionLogger.logException(var14);

         for(var10 = var5; var10 < var5 + var1.m_header.m_file.CHUNK_DIM; ++var10) {
            for(var11 = var6; var11 < var6 + var1.m_header.m_file.CHUNK_DIM; ++var11) {
               for(int var12 = var7; var12 < var7 + var1.m_header.m_levels; ++var12) {
                  IsoGridSquare var13 = IsoCell.getInstance().getGridSquare(var10, var11, var12);
                  if (var13 != null) {
                     var13.RecalcAllWithNeighbours(true);
                  }
               }
            }
         }
      }

   }

   public void setChunkInWorld(ChunkData var1, int var2, int var3, int var4, IsoChunk var5, int var6, int var7) {
      IsoCell var8 = IsoWorld.instance.CurrentCell;
      var6 *= 8;
      var7 *= 8;

      try {
         this.setChunkInWorldInner(var1, var2, var3, var4, var5, var6, var7);
      } catch (Exception var13) {
         DebugLog.log("Failed to load chunk, blocking out area");
         ExceptionLogger.logException(var13);

         for(int var10 = var6 + var2; var10 < var6 + var2 + 8; ++var10) {
            for(int var11 = var7 + var3; var11 < var7 + var3 + 8; ++var11) {
               for(int var12 = var4; var12 < var4 + var1.m_header.m_levels; ++var12) {
                  var5.setSquare(var10 - var6, var11 - var7, var12, (IsoGridSquare)null);
                  var8.setCacheGridSquare(var10, var11, var12, (IsoGridSquare)null);
               }
            }
         }
      }

   }

   private void setChunkInWorldInner(ChunkData var1, int var2, int var3, int var4, IsoChunk var5, int var6, int var7) {
      IsoCell var8 = IsoWorld.instance.CurrentCell;
      new Stack();
      IsoMetaGrid var10 = IsoWorld.instance.getMetaGrid();

      for(int var11 = var6 + var2; var11 < var6 + var2 + 8; ++var11) {
         for(int var12 = var7 + var3; var12 < var7 + var3 + 8; ++var12) {
            for(int var13 = var4; var13 < var4 + var1.m_header.m_levels; ++var13) {
               boolean var14 = false;
               if (var11 < var6 + 8 && var12 < var7 + 8 && var11 >= var6 && var12 >= var7 && var13 >= 0) {
                  int[] var15 = var1.data[var11 - (var6 + var2)][var12 - (var7 + var3)][var13 - var4];
                  if (var15 != null && var15.length != 0) {
                     int var16 = var15.length;
                     IsoGridSquare var17 = var5.getGridSquare(var11 - var6, var12 - var7, var13);
                     if (var17 == null) {
                        var17 = IsoGridSquare.getNew(var8, (SliceY)null, var11, var12, var13);
                        var17.setX(var11);
                        var17.setY(var12);
                        var17.setZ(var13);
                        var5.setSquare(var11 - var6, var12 - var7, var13, var17);
                     }

                     for(int var18 = -1; var18 <= 1; ++var18) {
                        for(int var19 = -1; var19 <= 1; ++var19) {
                           if ((var18 != 0 || var19 != 0) && var18 + var11 - var6 >= 0 && var18 + var11 - var6 < 8 && var19 + var12 - var7 >= 0 && var19 + var12 - var7 < 8) {
                              IsoGridSquare var20 = var5.getGridSquare(var11 + var18 - var6, var12 + var19 - var7, var13);
                              if (var20 == null) {
                                 var20 = IsoGridSquare.getNew(var8, (SliceY)null, var11 + var18, var12 + var19, var13);
                                 var5.setSquare(var11 + var18 - var6, var12 + var19 - var7, var13, var20);
                              }
                           }
                        }
                     }

                     if (var16 > 1 && var13 > IsoCell.MaxHeight) {
                        IsoCell.MaxHeight = var13;
                     }

                     RoomDef var24 = var10.getRoomAt(var11, var12, var13);
                     long var25 = var24 != null ? var24.ID : -1L;
                     var17.setRoomID(var25);
                     var17.ResetIsoWorldRegion();
                     var24 = var10.getEmptyOutsideAt(var11, var12, var13);
                     if (var24 != null) {
                        IsoRoom var21 = var5.getRoom(var24.ID);
                        var17.roofHideBuilding = var21 == null ? null : var21.building;
                     }

                     for(int var26 = 0; var26 < var16; ++var26) {
                        String var22 = (String)var1.m_header.m_usedTileNames.get(var15[var26]);
                        if (!var1.m_header.bFixed2x) {
                           var22 = IsoChunk.Fix2x(var22);
                        }

                        IsoSprite var23 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var22);
                        if (var23 == null) {
                           Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + var22);
                        } else {
                           if (var26 == 0 && var23.getProperties().Is(IsoFlagType.solidfloor) && (!var23.Properties.Is(IsoFlagType.hidewalls) || var15.length > 1)) {
                              var14 = true;
                           }

                           if (var14 && var26 == 0) {
                              var17.getObjects().clear();
                           }

                           CellLoader.DoTileObjectCreation(var23, var23.getType(), var17, var8, var11, var12, var13, var22);
                        }
                     }

                     var17.FixStackableObjects();
                  }
               }
            }
         }
      }

   }

   private void setChunkInWorldInnerArb(ChunkData var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      IsoCell var8 = IsoWorld.instance.CurrentCell;
      IsoMetaGrid var9 = IsoWorld.instance.getMetaGrid();

      for(int var10 = var5; var10 < var5 + this.CHUNK_DIM; ++var10) {
         for(int var11 = var6; var11 < var6 + this.CHUNK_DIM; ++var11) {
            for(int var12 = var7; var12 < var7 + var1.m_header.m_levels; ++var12) {
               boolean var13;
               if (var10 == 12030 && var11 == 2601 && var12 == -1) {
                  var13 = false;
               }

               var13 = false;
               int[] var14 = var1.data[var10 - var5][var11 - var6][var12 - var7];
               if (var14 != null && var14.length != 0) {
                  int var15 = var14.length;
                  IsoGridSquare var16 = IsoCell.getInstance().getOrCreateGridSquare((double)var10, (double)var11, (double)var12);
                  var16.EnsureSurroundNotNull();
                  if (var15 > 1 && var12 > IsoCell.MaxHeight) {
                     IsoCell.MaxHeight = var12;
                  }

                  RoomDef var17 = var9.getRoomAt(var10, var11, var12);
                  long var18 = var17 != null ? var17.ID : -1L;
                  var16.setRoomID(var18);
                  if (var18 != -1L && !var16.getRoom().getBuilding().Rooms.contains(var16.getRoom())) {
                     var16.getRoom().getBuilding().Rooms.add(var16.getRoom());
                  }

                  var16.ResetIsoWorldRegion();

                  for(int var20 = 0; var20 < var15; ++var20) {
                     String var21 = (String)var1.m_header.m_usedTileNames.get(var14[var20]);
                     if (!var1.m_header.bFixed2x) {
                        var21 = IsoChunk.Fix2x(var21);
                     }

                     IsoSprite var22 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var21);
                     if (var22 == null) {
                        Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + var21);
                     } else {
                        if (var20 == 0) {
                           var13 = true;
                        }

                        if (var13 && var20 == 0) {
                           int var23 = var16.getObjects().size();

                           for(int var24 = var23 - 1; var24 >= 0; --var24) {
                              ((IsoObject)var16.getObjects().get(var24)).removeFromWorld();
                              ((IsoObject)var16.getObjects().get(var24)).removeFromSquare();
                           }
                        }

                        if (var21.contains("lighting")) {
                           boolean var25 = false;
                        }

                        CellLoader.DoTileObjectCreation(var22, var22.getType(), var16, var8, var10, var11, var12, var21);
                     }
                  }

                  var16.FixStackableObjects();
               }
            }
         }
      }

   }

   private void addToIsoChunk(IsoChunk var1, ChunkData var2, int var3, int var4, int var5, boolean var6) {
      IsoCell var7 = IsoWorld.instance.getCell();
      IsoMetaGrid var8 = IsoWorld.instance.getMetaGrid();
      byte var9 = 8;
      int var10 = Math.max(var1.wx * var9, var3);
      int var11 = Math.max(var1.wy * var9, var4);
      int var12 = Math.min(var1.wx * var9 + var9, var3 + this.CHUNK_DIM);
      int var13 = Math.min(var1.wy * var9 + var9, var4 + this.CHUNK_DIM);

      for(int var14 = var10; var14 < var12; ++var14) {
         for(int var15 = var11; var15 < var13; ++var15) {
            for(int var16 = var5; var16 < var5 + var2.m_header.m_levels; ++var16) {
               int[] var17 = var2.data[var14 - var3][var15 - var4][var16 - var5];
               if (var17 != null && var17.length != 0) {
                  int var18 = var17.length;
                  IsoGridSquare var19 = var1.getGridSquare(var14 - var1.wx * var9, var15 - var1.wy * var9, var16);
                  if (var19 == null) {
                     var19 = IsoGridSquare.getNew(var7, (SliceY)null, var14, var15, var16);
                     var19.setX(var14);
                     var19.setY(var15);
                     var19.setZ(var16);
                     var1.setSquare(var14 - var1.wx * var9, var15 - var1.wy * var9, var16, var19);
                  }

                  RoomDef var20 = var8.getRoomAt(var14, var15, var16);
                  long var21 = var20 != null ? var20.ID : -1L;
                  var19.setRoomID(var21);
                  int var23 = var2.attributes[var16 - var5][var14 - var3 + (var15 - var4) * this.CHUNK_DIM];
                  boolean var24 = (var23 & 1) != 0;
                  boolean var25 = (var23 & 2) != 0;
                  boolean var26 = (var23 & 4) != 0;
                  int var27;
                  if (!var24 && !var25 && !var26) {
                     var19.getObjects().clear();
                     var19.getSpecialObjects().clear();
                  } else {
                     for(var27 = var19.getObjects().size() - 1; var27 >= 0; --var27) {
                        IsoObject var28 = (IsoObject)var19.getObjects().get(var27);
                        boolean var29 = false;
                        boolean var30 = var28.sprite != null && var28.sprite.solidfloor;
                        boolean var31 = var28.sprite != null && (var28.sprite.getProperties().Is(IsoFlagType.WallW) || var28.sprite.getProperties().Is(IsoFlagType.WallN) || var28.sprite.getProperties().Is(IsoFlagType.WallSE));
                        boolean var32 = !var30 && !var31;
                        if (var24 && var30) {
                           var29 = true;
                        }

                        if (var25 && var31) {
                           var29 = true;
                        }

                        if (var26 && var32) {
                           var29 = true;
                        }

                        if (!var29) {
                           var19.getObjects().remove(var27);
                           var19.getSpecialObjects().remove(var28);
                        }
                     }
                  }

                  for(var27 = 0; var27 < var18; ++var27) {
                     String var34 = (String)var2.m_header.m_usedTileNames.get(var17[var27]);
                     if (!var2.m_header.bFixed2x) {
                        var34 = IsoChunk.Fix2x(var34);
                     }

                     IsoSprite var35 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var34);
                     if (var35 == null) {
                        Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + var34);
                     } else {
                        CellLoader.DoTileObjectCreation(var35, var35.getType(), var19, var7, var14, var15, var16, var34);
                     }
                  }

                  var19.FixStackableObjects();
               }
            }

            IsoGridSquare var33 = var1.getGridSquare(var14 - var1.wx * var9, var15 - var1.wy * var9, var5 + var2.m_header.m_levels);
            this.removeFloorsAboveStairs(var33);
         }
      }

   }

   void removeFloorsAboveStairs(IsoGridSquare var1) {
      if (var1 != null) {
         byte var2 = 8;
         int var3 = var1.chunk.wx * var2;
         int var4 = var1.chunk.wy * var2;
         IsoGridSquare var5 = var1.chunk.getGridSquare(var1.x - var3, var1.y - var4, var1.z - 1);
         if (var5 != null) {
            var5.RecalcProperties();
            if (var5.HasStairs() || var5.hasSlopedSurface()) {
               IsoObject[] var6 = (IsoObject[])var1.getObjects().getElements();
               int var7 = var1.getObjects().size();

               for(int var8 = 0; var8 < var7; ++var8) {
                  IsoObject var9 = var6[var8];
                  if (var9.isFloor()) {
                     var1.getObjects().remove(var9);
                     break;
                  }
               }

            }
         }
      }
   }

   public static final class Header {
      NewMapBinaryFile m_file;
      public int m_width = 0;
      public int m_height = 0;
      public int m_levels = 0;
      public int m_version = 0;
      public final TLongObjectHashMap<RoomDef> m_roomDefMap = new TLongObjectHashMap();
      public final ArrayList<RoomDef> m_roomDefList = new ArrayList();
      public final ArrayList<BuildingDef> m_buildingDefList = new ArrayList();
      public boolean bFixed2x = true;
      protected final ArrayList<String> m_usedTileNames = new ArrayList();
      String m_fileName;
      private long m_chunkTablePosition;

      public Header() {
      }

      public void Dispose() {
         for(int var1 = 0; var1 < this.m_buildingDefList.size(); ++var1) {
            BuildingDef var2 = (BuildingDef)this.m_buildingDefList.get(var1);
            var2.Dispose();
         }

         this.m_roomDefMap.clear();
         this.m_roomDefList.clear();
         this.m_buildingDefList.clear();
         this.m_usedTileNames.clear();
      }
   }

   public static final class ChunkData {
      Header m_header;
      int[][][][] data = null;
      int[][] attributes = null;

      public ChunkData() {
      }
   }
}
