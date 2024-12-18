package zombie.characters.animals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.ai.states.animals.AnimalZoneState;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.worldMap.UIWorldMap;

public final class AnimalZones {
   private static AnimalZones instance = null;
   private static final int animalsPerPath = 1;
   private static final LinkedList<AnimalChunk> chunksWithTracks = new LinkedList();
   private final HashSet<AnimalChunk> chunksWithTracksSet = new HashSet();
   private final ArrayList<VirtualAnimal> tempAnimalList = new ArrayList();
   private HashSet<VirtualAnimal> doneVirtualAnimals = new HashSet();

   public AnimalZones() {
   }

   public static AnimalZones getInstance() {
      if (instance == null) {
         instance = new AnimalZones();
      }

      return instance;
   }

   public static void addAnimalChunk(AnimalChunk var0) {
      synchronized(chunksWithTracks) {
         chunksWithTracks.addLast(var0);
      }
   }

   public static void removeAnimalChunk(AnimalChunk var0) {
      synchronized(chunksWithTracks) {
         chunksWithTracks.remove(var0);
      }
   }

   public static void createJunctions(AnimalCell var0) {
      if (!var0.m_bAddedJunctions) {
         var0.m_bAddedJunctions = true;
         IsoMetaGrid var1 = IsoWorld.instance.getMetaGrid();
         IsoMetaCell var2 = var1.getCellData(var0.m_x, var0.m_y);
         if (var2 != null) {
            int var5;
            for(int var3 = 0; var3 < var2.getAnimalZonesSize(); ++var3) {
               AnimalZone var4 = var2.getAnimalZone(var3);

               for(var5 = var3 + 1; var5 < var2.getAnimalZonesSize(); ++var5) {
                  AnimalZone var6 = var2.getAnimalZone(var5);
                  if (var6.isPolyline()) {
                     var4.addJunctionsWithOtherZone(var6);
                  }
               }
            }

            AnimalManagerWorker var12 = AnimalManagerWorker.getInstance();

            for(int var13 = -1; var13 <= 1; ++var13) {
               for(var5 = -1; var5 <= 1; ++var5) {
                  AnimalCell var14 = var12.getCellFromCellPos(var0.m_x + var5, var0.m_y + var13);
                  if (var0 != var14 && var14 != null) {
                     IsoMetaCell var7 = var1.getCellData(var14.m_x, var14.m_y);
                     if (var7 != null) {
                        for(int var8 = 0; var8 < var2.getAnimalZonesSize(); ++var8) {
                           AnimalZone var9 = var2.getAnimalZone(var8);

                           for(int var10 = 0; var10 < var7.getAnimalZonesSize(); ++var10) {
                              AnimalZone var11 = var7.getAnimalZone(var10);
                              var9.addJunctionsWithOtherZone(var11);
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   void createJunctions(AnimalCell var1, AnimalCell var2) {
   }

   void spawnAnimalsInCell(AnimalCell var1) {
      IsoMetaGrid var2 = IsoWorld.instance.getMetaGrid();
      IsoMetaCell var3 = var2.getCellData(var1.m_x, var1.m_y);
      if (var3 != null) {
         for(int var4 = 0; var4 < var3.getAnimalZonesSize(); ++var4) {
            AnimalZone var5 = var3.getAnimalZone(var4);
            this.spawnAnimalsOnZone(var5);
         }

      }
   }

   void spawnAnimalsOnZone(AnimalZone var1) {
      // $FF: Couldn't be decompiled
   }

   static float getClosestZoneDist(float var0, float var1) {
      AnimalManagerWorker var2 = AnimalManagerWorker.getInstance();
      AnimalCell var3 = var2.getCellFromSquarePos((int)var0, (int)var1);
      if (var3 == null) {
         return -1.0F;
      } else {
         IsoMetaGrid var4 = IsoWorld.instance.getMetaGrid();
         IsoMetaCell var5 = var4.getCellData(var3.m_x, var3.m_y);
         if (var5 == null) {
            return -1.0F;
         } else {
            Vector2f var6 = new Vector2f();
            float var7 = 3.4028235E38F;

            for(int var8 = 0; var8 < var5.getAnimalZonesSize(); ++var8) {
               AnimalZone var9 = var5.getAnimalZone(var8);
               float var10 = var9.getClosestPointOnPolyline(var0, var1, var6);
               if (!(var10 < 0.0F)) {
                  float var11 = Vector2f.distance(var0, var1, var6.x, var6.y);
                  if (var11 < var7) {
                     var7 = var11;
                  }
               }
            }

            return var7;
         }
      }
   }

   AnimalZone getClosestZone(float var1, float var2, String var3) {
      AnimalManagerWorker var4 = AnimalManagerWorker.getInstance();
      AnimalCell var5 = var4.getCellFromSquarePos((int)var1, (int)var2);
      if (var5 == null) {
         return null;
      } else {
         IsoMetaGrid var6 = IsoWorld.instance.getMetaGrid();
         IsoMetaCell var7 = var6.getCellData(var5.m_x, var5.m_y);
         if (var7 == null) {
            return null;
         } else {
            Vector2f var8 = new Vector2f();
            AnimalZone var9 = null;
            float var10 = 3.4028235E38F;

            for(int var11 = 0; var11 < var7.getAnimalZonesSize(); ++var11) {
               AnimalZone var12 = var7.getAnimalZone(var11);
               if (var3 == null || var3.equals(var12.m_action)) {
                  float var13 = var12.getClosestPointOnPolyline(var1, var2, var8);
                  if (!(var13 < 0.0F)) {
                     float var14 = Vector2f.distance(var1, var2, var8.x, var8.y);
                     if (var14 < var10) {
                        var10 = var14;
                        var9 = var12;
                     }
                  }
               }
            }

            return var9;
         }
      }
   }

   public void render(UIWorldMap var1, boolean var2, boolean var3) {
      this.renderAnimalCells(var1, var2, var3);
      this.renderIsoAnimals(var1);
      this.renderChunkMapBounds(var1);
   }

   public static void updateVirtualAnimals() {
      synchronized(chunksWithTracks) {
         getInstance().chunksWithTracksSet.clear();
         getInstance().chunksWithTracksSet.addAll(chunksWithTracks);
         getInstance().doneVirtualAnimals.clear();
         Iterator var1 = getInstance().chunksWithTracksSet.iterator();

         while(var1.hasNext()) {
            AnimalChunk var2 = (AnimalChunk)var1.next();
            ArrayList var3 = getInstance().tempAnimalList;
            var3.clear();
            var3.addAll(var2.m_animals);
            var2.updateTracks();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               VirtualAnimal var5 = (VirtualAnimal)var3.get(var4);
               if (var5.isRemoved()) {
                  var2.m_animals.remove(var5);
               } else if (!getInstance().doneVirtualAnimals.contains(var5)) {
                  getInstance().doneVirtualAnimals.add(var5);
                  var5.update();
               }
            }
         }

      }
   }

   private void renderAnimalCells(UIWorldMap var1, boolean var2, boolean var3) {
      AnimalPopulationManager var4 = AnimalPopulationManager.getInstance();
      AnimalManagerWorker var5 = AnimalManagerWorker.getInstance();

      for(int var6 = var4.minY; var6 <= var4.minY + var4.height; ++var6) {
         for(int var7 = var4.minX; var7 <= var4.minX + var4.width; ++var7) {
            AnimalCell var8 = var5.getCellFromCellPos(var7, var6);
            if (var8 != null && var8.m_bLoaded) {
               createJunctions(var8);
               this.renderJunctions(var1, var8);

               for(int var9 = 0; var9 < AnimalPopulationManager.CHUNKS_PER_CELL; ++var9) {
                  for(int var10 = 0; var10 < AnimalPopulationManager.CHUNKS_PER_CELL; ++var10) {
                     AnimalChunk var11 = var8.m_chunks[var10 + var9 * AnimalPopulationManager.CHUNKS_PER_CELL];
                     int var12;
                     float var14;
                     float var15;
                     double var16;
                     String var10000;
                     double var18;
                     double var20;
                     double var22;
                     String var24;
                     if (var2) {
                        for(var12 = 0; var12 < var11.m_animals.size(); ++var12) {
                           VirtualAnimal var13 = (VirtualAnimal)var11.m_animals.get(var12);
                           var14 = var1.getAPI().worldToUIX(var13.getX(), var13.getY());
                           var15 = var1.getAPI().worldToUIY(var13.getX(), var13.getY());
                           var14 = PZMath.floor(var14);
                           var15 = PZMath.floor(var15);
                           var16 = 0.0;
                           var18 = 0.0;
                           var20 = 1.0;
                           var22 = 1.0;
                           var1.DrawTextureScaledColor((Texture)null, (double)var14 - 3.0, (double)var15 - 3.0, 6.0, 6.0, var16, var18, var20, var22);
                           if (var13.m_state != null) {
                              var10000 = var13.migrationGroup;
                              var10000 = Translator.getText("IGUI_MigrationGroup_" + var10000);
                              var24 = var10000 + " x" + var13.m_animals.size() + " " + var13.m_state.getClass().getSimpleName() + (var13.m_bMoveForwardOnZone ? " F" : " B");
                              if (var13.m_state instanceof VirtualAnimalState.StateSleep) {
                                 var24 = var24 + " " + Math.max(0, (int)Math.floor((var13.m_wakeTime - GameTime.getInstance().getWorldAgeHours()) * 60.0) + 1) + " mins";
                              }

                              if (var13.m_state instanceof VirtualAnimalState.StateEat) {
                                 var24 = var24 + " " + ((int)Math.floor((var13.m_eatStartTime + (double)((float)var13.timeToEat / 60.0F) - GameTime.getInstance().getWorldAgeHours()) * 60.0) + 1) + " mins";
                              }

                              if (var13.m_state instanceof VirtualAnimalState.StateFollow) {
                                 var24 = var24 + "\nNext rest: " + var13.getNextSleepPeriod();
                                 var24 = var24 + "\nEat: " + var13.getNextEatPeriod();
                              }

                              var1.DrawTextCentre(var24, (double)var14, (double)var15 + 4.0, 0.0, 0.0, 0.0, 1.0);
                           }
                        }
                     }

                     if (var3) {
                        for(var12 = 0; var12 < var11.m_animalTracks.size(); ++var12) {
                           AnimalTracks var25 = (AnimalTracks)var11.m_animalTracks.get(var12);
                           var14 = var1.getAPI().worldToUIX((float)var25.x, (float)var25.y);
                           var15 = var1.getAPI().worldToUIY((float)var25.x, (float)var25.y);
                           var14 = PZMath.floor(var14);
                           var15 = PZMath.floor(var15);
                           var16 = 0.33;
                           var18 = 0.33;
                           var20 = 0.33;
                           var22 = 1.0;
                           var1.DrawTextureScaledColor((Texture)null, (double)var14 - 3.0, (double)var15 - 3.0, 6.0, 6.0, var16, var18, var20, var22);
                           var10000 = Translator.getText("IGUI_MigrationGroup_" + var25.animalType);
                           var24 = var10000 + " " + AnimalTracks.getTrackStr(var25.trackType);
                           if (var25.dir != null) {
                              var24 = var24 + " " + var25.dir.toString();
                           }

                           var24 = var24 + "\n" + PZMath.fastfloor((float)((GameTime.getInstance().getCalender().getTimeInMillis() - var25.addedTime) / 60000L)) + " mins ago";
                           var1.DrawTextCentre(var24, (double)var14, (double)var15 + 4.0, 0.33, 0.33, 0.33, 1.0);
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public AnimalZoneJunction getClosestJunction(int var1, int var2) {
      AnimalZone var3 = this.getClosestZone((float)var1, (float)var2, (String)null);
      if (var3 != null && var3.m_junctions != null && !var3.m_junctions.isEmpty()) {
         AnimalZoneJunction var4 = (AnimalZoneJunction)var3.m_junctions.get(0);
         double var5 = 100.0;

         for(int var7 = 0; var7 < var3.m_junctions.size(); ++var7) {
            AnimalZoneJunction var8 = (AnimalZoneJunction)var3.m_junctions.get(var7);
            double var9 = Math.sqrt((double)((var8.getY() - var2) * (var8.getY() - var2) + (var8.getX() - var1) * (var8.getX() - var1)));
            if (var9 < var5 && var9 < 30.0) {
               var5 = var9;
               var4 = var8;
            }
         }

         return var4;
      } else {
         return null;
      }
   }

   private void renderJunctions(UIWorldMap var1, AnimalCell var2) {
      IsoMetaGrid var3 = IsoWorld.instance.getMetaGrid();
      IsoMetaCell var4 = var3.getCellData(var2.m_x, var2.m_y);
      if (var4 != null) {
         for(int var5 = 0; var5 < var4.getAnimalZonesSize(); ++var5) {
            AnimalZone var6 = var4.getAnimalZone(var5);
            if (var6.m_junctions != null) {
               for(int var7 = 0; var7 < var6.m_junctions.size(); ++var7) {
                  AnimalZoneJunction var8 = (AnimalZoneJunction)var6.m_junctions.get(var7);
                  float var9 = var1.getAPI().worldToUIX((float)var8.getX() + 0.5F, (float)var8.getY() + 0.5F);
                  float var10 = var1.getAPI().worldToUIY((float)var8.getX() + 0.5F, (float)var8.getY() + 0.5F);
                  var9 = PZMath.floor(var9);
                  var10 = PZMath.floor(var10);
                  double var11 = 0.0;
                  double var13 = 1.0;
                  double var15 = 0.0;
                  double var17 = 1.0;
                  var1.DrawTextureScaledColor((Texture)null, (double)var9 - 3.0, (double)var10 - 3.0, 6.0, 6.0, var11, var13, var15, var17);
                  if (!"Follow".equals(var8.m_zoneOther.m_action) && !StringUtils.isNullOrEmpty(var8.m_zoneOther.m_action)) {
                     var1.DrawTextCentre(var8.m_zoneOther.m_action, (double)var9, (double)var10 + 6.0, 0.0, 0.0, 0.0, 1.0);
                  }
               }
            }
         }

      }
   }

   private void renderIsoAnimals(UIWorldMap var1) {
      ArrayList var2 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoAnimal var4 = (IsoAnimal)Type.tryCastTo((IsoMovingObject)var2.get(var3), IsoAnimal.class);
         if (var4 != null && !var4.isOnHook()) {
            float var5 = var1.getAPI().worldToUIX(var4.getX(), var4.getY());
            float var6 = var1.getAPI().worldToUIY(var4.getX(), var4.getY());
            var5 = PZMath.floor(var5);
            var6 = PZMath.floor(var6);
            double var7 = 0.0;
            double var9 = 0.5;
            double var11 = 1.0;
            double var13 = 1.0;
            if (var4.getCurrentSquare() == null) {
               var7 = 1.0;
               var11 = 0.0;
               var9 = 0.0;
            }

            var1.DrawTextureScaledColor((Texture)null, (double)var5 - 3.0, (double)var6 - 3.0, 6.0, 6.0, var7, var9, var11, var13);
            if (var4.isCurrentState(AnimalZoneState.instance())) {
               HashMap var15 = var4.getStateMachineParams(AnimalZoneState.instance());
               Object var16 = var15.get(1);
               var1.DrawTextCentre(var16.getClass().getSimpleName() + (var4.isMoveForwardOnZone() ? " F" : " B"), (double)var5, (double)var6 + 4.0, 0.0, 0.0, 0.0, 1.0);
            }
         }
      }

   }

   private void renderChunkMapBounds(UIWorldMap var1) {
      IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[0];
      float var3 = var1.getAPI().worldToUIX((float)var2.getWorldXMinTiles(), (float)var2.getWorldYMinTiles());
      float var4 = var1.getAPI().worldToUIY((float)var2.getWorldXMinTiles(), (float)var2.getWorldYMinTiles());
      float var5 = var1.getAPI().worldToUIX((float)var2.getWorldXMaxTiles(), (float)var2.getWorldYMinTiles());
      float var6 = var1.getAPI().worldToUIY((float)var2.getWorldXMaxTiles(), (float)var2.getWorldYMinTiles());
      float var7 = var1.getAPI().worldToUIX((float)var2.getWorldXMaxTiles(), (float)var2.getWorldYMaxTiles());
      float var8 = var1.getAPI().worldToUIY((float)var2.getWorldXMaxTiles(), (float)var2.getWorldYMaxTiles());
      float var9 = var1.getAPI().worldToUIX((float)var2.getWorldXMinTiles(), (float)var2.getWorldYMaxTiles());
      float var10 = var1.getAPI().worldToUIY((float)var2.getWorldXMinTiles(), (float)var2.getWorldYMaxTiles());
      var3 = PZMath.floor(var3);
      var4 = PZMath.floor(var4);
      var5 = PZMath.floor(var5);
      var6 = PZMath.floor(var6);
      var7 = PZMath.floor(var7);
      var8 = PZMath.floor(var8);
      var9 = PZMath.floor(var9);
      var10 = PZMath.floor(var10);
      float var11 = 1.0F;
      float var12 = 1.0F;
      float var13 = 1.0F;
      float var14 = 1.0F;
      byte var15 = 1;
      var1.DrawLine(Texture.getWhite(), (double)var3, (double)var4, (double)var5, (double)var6, (float)var15, (double)var11, (double)var12, (double)var13, (double)var14);
      var1.DrawLine(Texture.getWhite(), (double)var5, (double)var6, (double)var7, (double)var8, (float)var15, (double)var11, (double)var12, (double)var13, (double)var14);
      var1.DrawLine(Texture.getWhite(), (double)var7, (double)var8, (double)var9, (double)var10, (float)var15, (double)var11, (double)var12, (double)var13, (double)var14);
      var1.DrawLine(Texture.getWhite(), (double)var9, (double)var10, (double)var3, (double)var4, (float)var15, (double)var11, (double)var12, (double)var13, (double)var14);
   }

   public static void Reset() {
      chunksWithTracks.clear();
   }
}
