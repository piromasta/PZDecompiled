package zombie.iso.worldgen;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.iso.objects.IsoTree;
import zombie.iso.worldgen.biomes.Biome;
import zombie.iso.worldgen.biomes.BiomeRegistry;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.blending.BlendDirection;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.maps.BiomeMapEntry;
import zombie.iso.worldgen.roads.Road;
import zombie.iso.worldgen.roads.RoadConfig;
import zombie.iso.worldgen.roads.RoadGenerator;
import zombie.iso.worldgen.utils.Direction;
import zombie.iso.worldgen.veins.OreVein;
import zombie.iso.worldgen.veins.Veins;
import zombie.iso.worldgen.zones.WorldGenZone;
import zombie.randomizedWorld.RandomizedWorldBase;

public class WGChunk {
   private final WGSimplexGenerator simplex;
   private final WGTile wgTile;
   private final RandomizedWorldBase randomizedWorldBase;
   private final Map<String, Biome> biomes;
   private final Map<String, Biome> biomesMap;
   private final long seed;
   private final Map<BiomeType.Landscape, List<Double>> landscape;
   private final Map<BiomeType.Plant, List<Double>> plant;
   private final Map<BiomeType.Bush, List<Double>> bush;
   private final Map<BiomeType.Temperature, List<Double>> temperature;
   private final Map<BiomeType.Hygrometry, List<Double>> hygrometry;
   private final Map<BiomeType.OreLevel, List<Double>> oreLevel;
   private final List<StaticModule> staticModules;
   private final Veins veins;
   private final Map<String, Map<Direction, List<TileGroup>>> attachments;
   private final Map<String, List<TileGroup>> similar;
   private final Map<String, Double> priorities;
   private final Map<String, RoadConfig> roadsConfig;
   private final List<RoadGenerator> roadGenerators;

   public WGChunk(long var1) {
      this.seed = var1;
      this.simplex = new WGSimplexGenerator(var1);
      this.wgTile = new WGTile();
      this.randomizedWorldBase = new RandomizedWorldBase();
      this.runLuaOverride();
      KahluaTable var3 = (KahluaTable)LuaManager.env.rawget("worldgen");
      String var4 = var3.rawget("biomes_override") == null ? "biomes" : "biomes_override";
      WGReader var5 = new WGReader();
      this.biomes = (Map)var5.loadBiomes(var3, var4).entrySet().stream().filter((var0) -> {
         return ((Biome)var0.getValue()).generate();
      }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      this.biomesMap = (Map)var5.loadBiomes(var3, "biomes_map").entrySet().stream().filter((var0) -> {
         return ((Biome)var0.getValue()).generate();
      }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      String var6 = var3.rawget("selection_override") == null ? "selection" : "selection_override";
      this.landscape = var5.loadSelection(BiomeType.Landscape.NONE, var3, var6);
      this.plant = var5.loadSelection(BiomeType.Plant.NONE, var3, var6);
      this.bush = var5.loadSelection(BiomeType.Bush.NONE, var3, var6);
      this.temperature = var5.loadSelection(BiomeType.Temperature.NONE, var3, var6);
      this.hygrometry = var5.loadSelection(BiomeType.Hygrometry.NONE, var3, var6);
      this.oreLevel = var5.loadSelection(BiomeType.OreLevel.NONE, var3, var6);
      this.staticModules = var5.loadStaticModules(var3, "static_modules");
      this.veins = new Veins(var5.loadVeinsConfig(var3, "veins"));
      this.attachments = var5.loadAttachments(var3, "attachments");
      this.similar = var5.loadSimilar(var3, "similar");
      this.priorities = var5.loadPriorities(var3, "priorities");
      this.roadsConfig = var5.loadRoadConfig(var3, "roads");
      this.roadGenerators = new ArrayList();
      int var7 = 1000;

      for(Iterator var8 = this.roadsConfig.values().iterator(); var8.hasNext(); var7 += 1000) {
         RoadConfig var9 = (RoadConfig)var8.next();
         this.roadGenerators.add(new RoadGenerator(this.seed, var9, (long)var7));
      }

   }

   public List<RoadGenerator> getRoadGenerators() {
      return this.roadGenerators;
   }

   private void runLuaOverride() {
      String[] var1 = IsoWorld.instance.getMap().split(";");
      String[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String var5 = var2[var4];
         String var6 = ZomboidFileSystem.instance.getString("media/maps/" + var5 + "/WorldGenOverride.lua");
         File var7 = new File(var6);
         if (var7.exists()) {
            LuaManager.RunLua(var6);
         }
      }

   }

   public boolean genRandomChunk(IsoCell var1, IsoChunk var2, int var3, int var4) {
      int var5 = var3 * 8;
      int var6 = var4 * 8;
      int var7 = (var3 + 1) * 8;
      int var8 = (var4 + 1) * 8;
      var2.setMinMaxLevel(0, 0);
      var2.setBlendingDoneFull(true);
      var2.setBlendingDonePartial(false);
      var2.setAttachmentsDoneFull(false);

      for(int var9 = 0; var9 < 5; ++var9) {
         var2.setAttachmentsState(var9, false);
      }

      var2.addModded(ChunkGenerationStatus.WORLDGEN);
      EnumMap var27 = new EnumMap(FeatureType.class);
      FeatureType[] var10 = FeatureType.values();
      int var11 = var10.length;

      int var12;
      for(var12 = 0; var12 < var11; ++var12) {
         FeatureType var13 = var10[var12];
         var27.put(var13, new String[64]);
      }

      Object var28 = new HashSet();

      RoadGenerator var30;
      for(Iterator var29 = this.roadGenerators.iterator(); var29.hasNext(); var28 = var30.getRoads(var3, var4)) {
         var30 = (RoadGenerator)var29.next();
      }

      ((Set)var28).stream().forEach((var0) -> {
         DebugLog.log(String.format("Generating road %s", var0));
      });

      int var14;
      try {
         for(var11 = var5; var11 < var7; ++var11) {
            for(var12 = var6; var12 < var8; ++var12) {
               byte var32 = 0;
               var14 = var11 - var5;
               int var15 = var12 - var6;
               byte var16 = 0;
               IsoGridSquare var17 = var2.getGridSquare(var14, var15, var32);
               if (var17 != null && !var17.getObjects().isEmpty()) {
                  var2.setBlendingDoneFull(false);
                  var2.setBlendingDonePartial(true);
                  var2.setModifDepth(BlendDirection.NORTH, Math.min(var15, var2.getModifDepth(BlendDirection.NORTH)));
                  var2.setModifDepth(BlendDirection.SOUTH, Math.max(var15, var2.getModifDepth(BlendDirection.SOUTH)));
                  var2.setModifDepth(BlendDirection.WEST, Math.min(var14, var2.getModifDepth(BlendDirection.WEST)));
                  var2.setModifDepth(BlendDirection.EAST, Math.max(var14, var2.getModifDepth(BlendDirection.EAST)));
               } else {
                  if (var17 == null) {
                     var17 = IsoGridSquare.getNew(var1, (SliceY)null, var11, var12, var32);
                     var2.setSquare(var14, var15, var32, var17);
                  }

                  var17.setRoomID(-1L);
                  var17.ResetIsoWorldRegion();
                  List var20 = (List)this.staticModules.stream().filter((var2x) -> {
                     return var11 >= var2x.xmin() && var11 <= var2x.xmax() && var12 >= var2x.ymin() && var12 <= var2x.ymax();
                  }).collect(Collectors.toList());
                  Random var21 = WGParams.instance.getRandom(var2.wx * 8 + var11, var2.wy * 8 + var12);
                  if (!var20.isEmpty()) {
                     StaticModule var33 = (StaticModule)var20.get(0);
                     if (var33.biome() != null) {
                        Biome var34 = var33.biome();
                        this.applyBiome(var34, var1, var2, var17, var11, var12, var32, var14, var15, var16, var27, false, var21);
                        this.applyOreVeins(var1, var17, var11, var12, var32, var14, var15, var16, var27, var21);
                     } else {
                        if (var33.prefab() == null) {
                           throw new RuntimeException("Need at least one of 'biome' or 'prefab' in WorldGenOverride.lua/worlgen.static_modules");
                        }

                        IBiome var35 = this.getBiome(var11, var12);
                        PrefabStructure var37 = var33.prefab();
                        this.applyPrefab(var37, var35, var1, var2, var17, var11, var12, var32, var14, var15, var16, var27, var33.xmin(), var33.xmax(), var33.ymin(), var33.ymax(), var21);
                     }
                  } else {
                     boolean var22 = false;
                     Random var23 = WGParams.instance.getRandom(var2.wx * 8 + var11, var2.wy * 8 + var12);
                     Iterator var24 = ((Set)var28).iterator();

                     while(var24.hasNext()) {
                        Road var25 = (Road)var24.next();
                        if (var11 >= Math.min(var25.getA().x, var25.getB().x) && var11 <= Math.max(var25.getA().x, var25.getB().x) && var12 >= Math.min(var25.getA().y, var25.getB().y) && var12 <= Math.max(var25.getA().y, var25.getB().y) && var23.nextDouble() < var25.getProbability()) {
                           this.placeRoad(var25, var1, var17, var11, var12, var32, var14, var15, var16, var27, var23);
                           var22 = true;
                        }
                     }

                     if (!var22) {
                        IBiome var36 = this.getBiome(var11, var12);
                        this.applyBiome(var36, var1, var2, var17, var11, var12, var32, var14, var15, var16, var27, false, var21);
                        this.applyOreVeins(var1, var17, var11, var12, var32, var14, var15, var16, var27, var21);
                     }
                  }
               }
            }
         }

         return true;
      } catch (Exception var26) {
         DebugLog.log("Failed to load chunk, blocking out area");
         ExceptionLogger.logException(var26);

         for(var12 = var5; var12 < var7; ++var12) {
            for(int var31 = var6; var31 < var8; ++var31) {
               for(var14 = 0; var14 < var2.maxLevel + 1; ++var14) {
                  var2.setSquare(var12 - var5, var31 - var6, var14, (IsoGridSquare)null);
               }
            }
         }

         return false;
      }
   }

   public void genMapChunk(IsoCell var1, IsoChunk var2, int var3, int var4) {
      IsoMetaGrid var5 = IsoWorld.instance.getMetaGrid();
      var5.getChunkData(var3, var4);
      BiomeMap var7 = IsoWorld.instance.getBiomeMap();
      int var8 = var3 * 8;
      int var9 = var4 * 8;
      int var10 = (var3 + 1) * 8;
      int var11 = (var4 + 1) * 8;
      EnumMap var12 = new EnumMap(FeatureType.class);
      FeatureType[] var13 = FeatureType.values();
      int var14 = var13.length;

      int var15;
      for(var15 = 0; var15 < var14; ++var15) {
         FeatureType var16 = var13[var15];
         var12.put(var16, new String[64]);
      }

      int[] var36 = var7.getZones(var2.wx, var2.wy);
      if (var36 != null) {
         for(var14 = var8; var14 < var10; ++var14) {
            for(var15 = var9; var15 < var11; ++var15) {
               byte var37 = 0;
               int var17 = var14 - var8;
               int var18 = var15 - var9;
               byte var19 = 0;
               WorldGenZone var20 = this.getWorldGenZoneAt(var14, var15, var37);
               boolean var21 = true;
               if (var20 != null) {
                  var21 = var20.getRocks();
               }

               IsoGridSquare var22 = var2.getGridSquare(var17, var18, var37);
               if (var22 != null) {
                  IsoObject var23 = var22.getFloor();
                  if (var23 != null) {
                     BiomeMapEntry var24 = var7.getEntry(var36[var18 * 8 + var17]);
                     if (var24 != null) {
                        Random var25 = WGParams.instance.getRandom(var2.wx * 8 + var14, var2.wy * 8 + var15);
                        FeatureType[] var26 = FeatureType.values();
                        int var27 = var26.length;

                        for(int var28 = 0; var28 < var27; ++var28) {
                           FeatureType var29 = var26[var28];
                           String var30 = ((String[])var12.get(var29))[var18 * 8 + var17];
                           if (var30 != null) {
                              IsoTree var31 = var22.getTree();
                              IsoObject var32 = var22.getBush();
                              IsoObject var33 = var22.getGrass();
                              switch (var30) {
                                 case "NO_TREE":
                                    var22.DeleteTileObject(var31);
                                    break;
                                 case "NO_BUSH":
                                    var22.DeleteTileObject(var32);
                                    break;
                                 case "NO_GRASS":
                                    var22.DeleteTileObject(var33);
                                    break;
                                 default:
                                    var22.DeleteTileObject(var31);
                                    var22.DeleteTileObject(var32);
                                    var22.DeleteTileObject(var33);
                                    this.wgTile.applyTile(var30, var22, var1, var14, var15, var37, var25);
                              }
                           }
                        }

                        var22.setBiome(this.getMapBiome(var14, var15, var24.biome()));
                        IsoTree var38 = var22.getTree();
                        IsoObject var39 = var22.getBush();
                        String var40 = var23.getSprite().getName();
                        IBiome var41;
                        if (var38 != null || var39 != null) {
                           var41 = this.getMapBiome(var14, var15, var24.biome());
                           if (var41 == null || !WGUtils.instance.canPlace(var41.placement(), var40)) {
                              continue;
                           }

                           if (var38 != null && this.applyBiome(var41, FeatureType.TREE, var1, var2, var22, var14, var15, var37, var17, var18, var19, var12, true, var25)) {
                              var22.DeleteTileObject(var38);
                           }

                           if (var39 != null && this.applyBiome(var41, FeatureType.BUSH, var1, var2, var22, var14, var15, var37, var17, var18, var19, var12, true, var25)) {
                              var22.DeleteTileObject(var39);
                           }
                        }

                        if (var22.getObjects().size() - var22.getGrassLike().size() == 1) {
                           var41 = this.getMapBiome(var14, var15, var24.ore());
                           if (var41 != null && WGUtils.instance.canPlace(var41.placement(), var40) && var21) {
                              this.applyBiome(var41, FeatureType.ORE, var1, var2, var22, var14, var15, var37, var17, var18, var19, var12, true, var25);
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private WorldGenZone getWorldGenZoneAt(int var1, int var2, int var3) {
      IsoMetaCell var4 = IsoWorld.instance.MetaGrid.getCellData(var1 / IsoCell.CellSizeInSquares, var2 / IsoCell.CellSizeInSquares);
      if (var4 != null && var4.worldGenZones != null) {
         ArrayList var5 = var4.worldGenZones;

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            WorldGenZone var7 = (WorldGenZone)var5.get(var6);
            if (var7.contains(var1, var2, var3)) {
               return var7;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void placeRoad(Road var1, IsoCell var2, IsoGridSquare var3, int var4, int var5, int var6, int var7, int var8, int var9, EnumMap<FeatureType, String[]> var10, Random var11) {
      this.wgTile.setTile(var1, var3, var2, var4, var5, var6, var7, var8, var9, var10, var11);
   }

   private void applyBiome(IBiome var1, IsoCell var2, IsoChunk var3, IsoGridSquare var4, int var5, int var6, int var7, int var8, int var9, int var10, EnumMap<FeatureType, String[]> var11, boolean var12, Random var13) {
      this.wgTile.setTiles(var1, var4, var3, var2, var5, var6, var7, var8, var9, var10, var11, var12, var13);
      var4.FixStackableObjects();
      this.generateZombies(var1.zombies(), var4, var13);
   }

   private boolean applyBiome(IBiome var1, FeatureType var2, IsoCell var3, IsoChunk var4, IsoGridSquare var5, int var6, int var7, int var8, int var9, int var10, int var11, EnumMap<FeatureType, String[]> var12, boolean var13, Random var14) {
      if (this.wgTile.setTiles(var1, var2, var5, var4, var3, var6, var7, var8, var9, var10, var11, var12, var14)) {
         var5.FixStackableObjects();
         this.generateZombies(var1.zombies(), var5, var14);
         return true;
      } else {
         return false;
      }
   }

   private void applyOreVeins(IsoCell var1, IsoGridSquare var2, int var3, int var4, int var5, int var6, int var7, int var8, EnumMap<FeatureType, String[]> var9, Random var10) {
      for(int var11 = -10; var11 <= 10; ++var11) {
         for(int var12 = -10; var12 <= 10; ++var12) {
            int var13 = var1.getWorldX() + var11;
            int var14 = var1.getWorldY() + var12;
            List var15 = this.veins.get(var13, var14);
            Iterator var16 = var15.iterator();

            while(var16.hasNext()) {
               OreVein var17 = (OreVein)var16.next();
               if (var17.isValid(var3, var4, var10)) {
                  this.wgTile.setTile(var17, var2, var1, var3, var4, var5, var6, var7, var8, var9, var10);
               }
            }
         }
      }

   }

   private void applyPrefab(PrefabStructure var1, IBiome var2, IsoCell var3, IsoChunk var4, IsoGridSquare var5, int var6, int var7, int var8, int var9, int var10, int var11, EnumMap<FeatureType, String[]> var12, int var13, int var14, int var15, int var16, Random var17) {
      int var18 = Math.abs(var6 - var13) % var1.getX();
      int var19 = Math.abs(var7 - var15) % var1.getY();
      Iterator var20 = var1.getCategories().iterator();

      while(var20.hasNext()) {
         String var21 = (String)var20.next();
         if (var1.hasCategory(var21)) {
            int var22 = var1.getTileRef(var21, var18, var19);
            if (var22 == 0) {
               if (var21.equals("Floor")) {
                  this.wgTile.setTiles(var2, FeatureType.GROUND, var5, var4, var3, var6, var7, var8, var9, var10, var11, var12, var17);
               }
            } else {
               String var23 = var1.getTile(var22 - 1);
               this.wgTile.applyTile(var23, var5, var3, var6, var7, var8, var17);
            }
         }
      }

      var5.FixStackableObjects();
      this.generateZombies(var1.getZombies(), var5, var17);
   }

   private void generateZombies(float var1, IsoGridSquare var2, Random var3) {
      if (var3.nextFloat() < var1) {
         var2.chunk.proceduralZombieSquares.add(var2);
      }

   }

   public void addZombieToSquare(IsoGridSquare var1) {
      try {
         this.randomizedWorldBase.addZombiesOnSquare(1, (String)null, 50, var1);
      } catch (Exception var3) {
         DebugLog.log("Failed to load zombie");
         ExceptionLogger.logException(var3);
      }

   }

   public IBiome getBiome(int var1, int var2) {
      return BiomeRegistry.instance.get(this.biomes, this.simplex.noise((double)var1, (double)var2), this.simplex.selector((double)var1, (double)var2), this.landscape, this.plant, this.bush, this.temperature, this.hygrometry, this.oreLevel);
   }

   public IBiome getMapBiome(int var1, int var2, String var3) {
      return BiomeRegistry.instance.get(this.biomesMap, var3, this.simplex.noise((double)var1, (double)var2), this.simplex.selector((double)var1, (double)var2), this.bush, this.oreLevel);
   }

   public List<TileGroup> getAttachment(String var1, Direction var2) {
      Map var3 = (Map)this.attachments.get(var1);
      return var3 == null ? null : (List)var3.get(var2);
   }

   public boolean areSimilar(String var1, String var2) {
      List var3 = (List)this.similar.get(var1);
      return var3 == null ? false : var3.stream().anyMatch((var1x) -> {
         return var2 != null && var2.equals(var1x.tiles().get(0));
      });
   }

   public boolean priority(String var1, String var2) {
      if (this.priorities.get(var1) != null && this.priorities.get(var2) != null) {
         return (Double)this.priorities.get(var1) < (Double)this.priorities.get(var2);
      } else {
         return false;
      }
   }
}
