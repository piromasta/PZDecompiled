package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.iso.worldgen.biomes.Biome;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.Grass;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.maps.BiomeMapEntry;
import zombie.iso.worldgen.roads.RoadConfig;
import zombie.iso.worldgen.utils.Direction;
import zombie.iso.worldgen.veins.OreVeinConfig;
import zombie.iso.worldgen.zones.AnimalsPathConfig;

public class WGReader {
   public WGReader() {
   }

   public Map<String, Biome> loadBiomes(KahluaTable var1, String var2) {
      KahluaTable var3 = (KahluaTable)var1.rawget(var2);
      HashMap var4 = new HashMap();
      KahluaTableIterator var5 = var3.iterator();

      while(var5.advance()) {
         Object var6 = var5.getKey();
         KahluaTable var7 = (KahluaTable)var5.getValue();
         var4.put(var6.toString(), this.loadBiome(var6.toString(), var7));
      }

      var4.forEach((var1x, var2x) -> {
         ArrayList var3 = new ArrayList(List.of(var2x));

         while(true) {
            Biome var4x = (Biome)var3.get(0);
            if (var4x.parent() == null || var4x.parent().isEmpty()) {
               if (var3.size() == 1) {
                  return;
               } else {
                  HashMap var17 = new HashMap();
                  EnumSet var5 = ((Biome)var3.get(0)).temperature();
                  EnumSet var6 = ((Biome)var3.get(0)).plant();
                  EnumSet var7 = ((Biome)var3.get(0)).bush();
                  EnumSet var8 = ((Biome)var3.get(0)).landscape();
                  EnumSet var9 = ((Biome)var3.get(0)).hygrometry();
                  EnumSet var10 = ((Biome)var3.get(0)).oreLevel();
                  List var11 = ((Biome)var3.get(0)).placement();
                  float var12 = ((Biome)var3.get(0)).zombies();
                  Grass var13 = ((Biome)var3.get(0)).grass();

                  Biome var15;
                  for(Iterator var14 = var3.iterator(); var14.hasNext(); var13 = var15.grass()) {
                     var15 = (Biome)var14.next();
                     Map var16 = var15.getFeatures();
                     if (var16 != null) {
                        if (var16.get(FeatureType.GROUND) != null) {
                           var17.put(FeatureType.GROUND, (List)var16.get(FeatureType.GROUND));
                        }

                        if (var16.get(FeatureType.TREE) != null) {
                           var17.put(FeatureType.TREE, (List)var16.get(FeatureType.TREE));
                        }

                        if (var16.get(FeatureType.PLANT) != null) {
                           var17.put(FeatureType.PLANT, (List)var16.get(FeatureType.PLANT));
                        }

                        if (var16.get(FeatureType.BUSH) != null) {
                           var17.put(FeatureType.BUSH, (List)var16.get(FeatureType.BUSH));
                        }

                        if (var16.get(FeatureType.ORE) != null) {
                           var17.put(FeatureType.ORE, (List)var16.get(FeatureType.ORE));
                        }
                     }

                     if (!Objects.equals(var15.temperature(), EnumSet.allOf(BiomeType.Temperature.class))) {
                        var5 = var15.temperature();
                     }

                     if (!Objects.equals(var15.plant(), EnumSet.allOf(BiomeType.Plant.class))) {
                        var6 = var15.plant();
                     }

                     if (!Objects.equals(var15.bush(), EnumSet.allOf(BiomeType.Bush.class))) {
                        var7 = var15.bush();
                     }

                     if (!Objects.equals(var15.landscape(), EnumSet.allOf(BiomeType.Landscape.class))) {
                        var8 = var15.landscape();
                     }

                     if (!Objects.equals(var15.hygrometry(), EnumSet.allOf(BiomeType.Hygrometry.class))) {
                        var9 = var15.hygrometry();
                     }

                     if (!Objects.equals(var15.oreLevel(), EnumSet.allOf(BiomeType.OreLevel.class))) {
                        var10 = var15.oreLevel();
                     }

                     if (!var15.placement().isEmpty()) {
                        var11 = var15.placement();
                     }

                     if ((double)var15.zombies() >= 0.0) {
                        var12 = var15.zombies();
                     }
                  }

                  var4.put(var1x, new Biome(var1x, var2x.parent(), var2x.generate(), var17, var8, var6, var7, var5, var9, var10, var12, var11, var13));
                  return;
               }
            }

            var3.add(0, (Biome)var4.get(var4x.parent()));
         }
      });
      return var4;
   }

   public Biome loadBiome(String var1, KahluaTable var2) {
      if (var2 == null) {
         return null;
      } else {
         String var3 = (String)var2.rawget("parent");
         KahluaTable var4 = (KahluaTable)var2.rawget("features");
         Map var5 = this.loadBiomeFeatures(var4);
         KahluaTable var6 = (KahluaTable)var2.rawget("params");
         EnumSet var7 = this.loadBiomeType(BiomeType.Landscape.NONE, var6.rawget("landscape"));
         EnumSet var8 = this.loadBiomeType(BiomeType.Plant.NONE, var6.rawget("plant"));
         EnumSet var9 = this.loadBiomeType(BiomeType.Bush.NONE, var6.rawget("bush"));
         EnumSet var10 = this.loadBiomeType(BiomeType.Temperature.NONE, var6.rawget("temperature"));
         EnumSet var11 = this.loadBiomeType(BiomeType.Hygrometry.NONE, var6.rawget("hygrometry"));
         EnumSet var12 = this.loadBiomeType(BiomeType.OreLevel.NONE, var6.rawget("ore_level"));
         List var13 = this.loadList((KahluaTable)var6.rawget("placement"));
         KahluaTable var14 = (KahluaTable)var6.rawget("grass");
         Grass var15;
         if (var14 != null) {
            float var16 = this.loadDouble(var14.rawget("fernChance"), 0.7).floatValue();
            float var17 = this.loadDouble(var14.rawget("noGrassDiv"), 3.0).floatValue();
            List var18 = this.loadList((KahluaTable)var14.rawget("noGrassStages")).isEmpty() ? List.of(0.4) : this.loadList((KahluaTable)var14.rawget("noGrassStages"));
            List var19 = this.loadList((KahluaTable)var14.rawget("grassStages")).isEmpty() ? List.of(0.33, 0.5) : this.loadList((KahluaTable)var14.rawget("grassStages"));
            var15 = new Grass(var16, var17, var18, var19);
         } else {
            var15 = new Grass(0.7F, 3.0F, List.of(0.4), List.of(0.33, 0.5));
         }

         Double var20 = this.loadDouble(var6.rawget("zombies"), -1.0);
         boolean var21 = this.loadBoolean(var6.rawget("generate"), true);
         return new Biome(var1, var3, var21, var5, var7, var8, var9, var10, var11, var12, var20.floatValue(), var13, var15);
      }
   }

   public <T extends Enum<T>> Map<T, List<Double>> loadSelection(T var1, KahluaTable var2, String var3) {
      KahluaTable var4 = (KahluaTable)var2.rawget(var3);
      KahluaTableIterator var5 = ((KahluaTable)var4.rawget(BiomeType.keys.get(var1.getDeclaringClass()))).iterator();
      HashMap var6 = new HashMap();

      while(var5.advance()) {
         Enum var7 = Enum.valueOf(var1.getDeclaringClass(), (String)var5.getKey());
         List var8 = this.loadList((KahluaTable)var5.getValue());
         var6.put(var7, var8);
      }

      return var6;
   }

   private Map<FeatureType, List<Feature>> loadBiomeFeatures(KahluaTable var1) {
      if (var1 == null) {
         return null;
      } else {
         HashMap var2 = new HashMap();
         KahluaTableIterator var3 = var1.iterator();

         while(var3.advance()) {
            String var4 = (String)var3.getKey();
            KahluaTable var5 = (KahluaTable)var3.getValue();
            ArrayList var6 = new ArrayList();
            KahluaTableIterator var7 = var5.iterator();

            while(var7.advance()) {
               Object var8 = ((KahluaTable)var7.getValue()).rawget("f");
               Object var9 = ((KahluaTable)var7.getValue()).rawget("p");
               if (var8 == null || var9 == null) {
                  throw new RuntimeException(String.format("Features not found or probability absents | %s | %s", var8, var9));
               }

               List var10 = this.loadFeatures((KahluaTable)((KahluaTable)var8).rawget("main"));
               Map var11 = this.loadAttachments((KahluaTable)((KahluaTable)var8).rawget("attach"));
               Double var12 = (Double)var9;
               var6.add(new Feature(var10, var11, var12.floatValue()));
            }

            var2.put(FeatureType.valueOf(var4), var6);
         }

         return var2;
      }
   }

   private List<TileGroup> loadFeatures(KahluaTable var1) {
      ArrayList var2 = new ArrayList();
      KahluaTableIterator var3 = var1.iterator();

      while(true) {
         while(var3.advance()) {
            Object var4 = var3.getValue();
            if (var4 instanceof String) {
               var2.add(new TileGroup(1, 1, List.of((String)var4)));
            } else {
               if (!(var4 instanceof KahluaTable)) {
                  throw new RuntimeException("Only strings and tables in there!");
               }

               int var5 = 0;
               int var6 = 0;
               ArrayList var7 = new ArrayList();
               KahluaTableIterator var8 = ((KahluaTable)var4).iterator();

               while(var8.advance()) {
                  ++var6;
                  List var9 = this.loadList((KahluaTable)var8.getValue());
                  var5 = var9.size();
                  var7.addAll(var9);
               }

               var2.add(new TileGroup(var5, var6, var7));
            }
         }

         return var2;
      }
   }

   public Map<String, Map<Direction, List<TileGroup>>> loadAttachments(KahluaTable var1, String var2) {
      HashMap var3 = new HashMap();
      KahluaTable var4 = (KahluaTable)var1.rawget(var2);
      KahluaTableIterator var5 = var4.iterator();

      while(var5.advance()) {
         var3.put((String)var5.getKey(), this.loadAttachments((KahluaTable)var5.getValue()));
      }

      return var3;
   }

   private Map<Direction, List<TileGroup>> loadAttachments(KahluaTable var1) {
      HashMap var2 = new HashMap();
      if (var1 != null) {
         Direction[] var3 = Direction.values();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            Direction var6 = var3[var5];
            KahluaTable var7 = (KahluaTable)var1.rawget(var6.config);
            if (var7 != null) {
               var2.put(var6, this.loadFeatures(var7));
            }
         }
      }

      return var2;
   }

   public Map<String, List<TileGroup>> loadSimilar(KahluaTable var1, String var2) {
      HashMap var3 = new HashMap();
      KahluaTable var4 = (KahluaTable)var1.rawget(var2);
      KahluaTableIterator var5 = var4.iterator();

      while(var5.advance()) {
         var3.put((String)var5.getKey(), this.loadFeatures((KahluaTable)var5.getValue()));
      }

      return var3;
   }

   public Map<String, Double> loadPriorities(KahluaTable var1, String var2) {
      HashMap var3 = new HashMap();
      KahluaTable var4 = (KahluaTable)var1.rawget(var2);
      KahluaTableIterator var5 = var4.iterator();

      while(var5.advance()) {
         var3.put((String)var5.getValue(), (Double)var5.getKey());
      }

      return var3;
   }

   private <T extends Enum<T>> EnumSet<T> loadBiomeType(T var1, Object var2) {
      if (var2 == null) {
         return null;
      } else {
         KahluaTable var3 = (KahluaTable)var2;
         KahluaTableIterator var4 = var3.iterator();
         ArrayList var5 = new ArrayList();

         while(var4.advance()) {
            var5.add(Enum.valueOf(var1.getDeclaringClass(), (String)var4.getValue()));
         }

         return var5.isEmpty() ? EnumSet.of(var1) : EnumSet.copyOf(var5);
      }
   }

   public List<StaticModule> loadStaticModules(KahluaTable var1, String var2) {
      KahluaTable var3 = (KahluaTable)var1.rawget(var2);
      if (var3 == null) {
         return new ArrayList();
      } else {
         ArrayList var4 = new ArrayList();
         KahluaTableIterator var5 = var3.iterator();

         while(var5.advance()) {
            KahluaTable var6 = (KahluaTable)var5.getValue();
            KahluaTable var7 = (KahluaTable)var6.rawget("position");
            Double var8 = this.loadDouble(var7.rawget("xmin"), -1.7976931348623157E308);
            Double var9 = this.loadDouble(var7.rawget("xmax"), 1.7976931348623157E308);
            Double var10 = this.loadDouble(var7.rawget("ymin"), -1.7976931348623157E308);
            Double var11 = this.loadDouble(var7.rawget("ymax"), 1.7976931348623157E308);
            Biome var12 = this.loadBiome("", (KahluaTable)var6.rawget("biome"));
            PrefabStructure var13 = this.loadPrefab((KahluaTable)var6.rawget("prefab"));
            if (var12 == null && var13 == null) {
               throw new RuntimeException("Need at least one of 'biome' or 'prefab' in WorldGenOverride.lua/worlgen.static_modules");
            }

            var4.add(new StaticModule(var12, var13, var8.intValue(), var9.intValue(), var10.intValue(), var11.intValue()));
         }

         return var4;
      }
   }

   private PrefabStructure loadPrefab(KahluaTable var1) {
      if (var1 == null) {
         return null;
      } else {
         KahluaTable var2 = (KahluaTable)var1.rawget("dimensions");
         KahluaTableIterator var3 = var2.iterator();

         int[] var4;
         int var5;
         for(var4 = new int[2]; var3.advance(); var4[var5] = ((Double)var3.getValue()).intValue()) {
            var5 = ((Double)var3.getKey()).intValue() - 1;
         }

         List var14 = this.loadList((KahluaTable)var1.rawget("tiles"));
         KahluaTable var6 = (KahluaTable)var1.rawget("schematic");
         KahluaTableIterator var7 = var6.iterator();
         HashMap var8 = new HashMap();

         while(var7.advance()) {
            String var9 = (String)var7.getKey();
            KahluaTable var10 = (KahluaTable)var7.getValue();
            KahluaTableIterator var11 = var10.iterator();

            int[][] var12;
            int var13;
            for(var12 = new int[var4[1]][var4[0]]; var11.advance(); var12[var13] = Stream.of(((String)var11.getValue()).split(",")).mapToInt(Integer::parseInt).toArray()) {
               var13 = ((Double)var11.getKey()).intValue() - 1;
            }

            var8.put(var9, var12);
         }

         Double var15 = this.loadDouble(var1.rawget("zombies"), 0.0);
         return new PrefabStructure(var4, var14, var8, var15.floatValue());
      }
   }

   public Map<String, OreVeinConfig> loadVeinsConfig(KahluaTable var1, String var2) {
      KahluaTable var3 = (KahluaTable)var1.rawget(var2);
      if (var3 == null) {
         return new HashMap();
      } else {
         HashMap var4 = new HashMap();
         KahluaTableIterator var5 = var3.iterator();

         while(var5.advance()) {
            String var6 = (String)var5.getKey();
            KahluaTable var7 = (KahluaTable)var5.getValue();
            KahluaTable var8 = (KahluaTable)var7.rawget("feature");
            List var9 = this.loadFeatures((KahluaTable)((KahluaTable)var8.rawget("f")).rawget("main"));
            KahluaTable var10 = (KahluaTable)var7.rawget("arms");
            int var11 = this.loadInteger(var10.rawget("amount_min"), 3);
            int var12 = this.loadInteger(var10.rawget("amount_max"), 6);
            int var13 = this.loadInteger(var10.rawget("distance_min"), 100);
            int var14 = this.loadInteger(var10.rawget("distance_max"), 400);
            int var15 = this.loadInteger(var10.rawget("delta_angle"), 5);
            float var16 = this.loadDouble(var10.rawget("p"), 0.25).floatValue();
            KahluaTable var17 = (KahluaTable)var7.rawget("center");
            int var18 = this.loadInteger(var17.rawget("radius"), 5);
            float var19 = this.loadDouble(var17.rawget("p"), 0.5).floatValue();
            float var20 = this.loadDouble(var7.rawget("p"), 0.01).floatValue();
            var4.put(var6, new OreVeinConfig(var9, var18, var19, var11, var12, var13, var14, var15, var16, var20));
         }

         return var4;
      }
   }

   public Map<String, RoadConfig> loadRoadConfig(KahluaTable var1, String var2) {
      KahluaTable var3 = (KahluaTable)var1.rawget(var2);
      if (var3 == null) {
         return new HashMap();
      } else {
         HashMap var4 = new HashMap();
         KahluaTableIterator var5 = var3.iterator();

         while(var5.advance()) {
            String var6 = (String)var5.getKey();
            KahluaTable var7 = (KahluaTable)var5.getValue();
            KahluaTable var8 = (KahluaTable)var7.rawget("feature");
            List var9 = this.loadFeatures((KahluaTable)((KahluaTable)var8.rawget("f")).rawget("main"));
            double var10 = this.loadDouble(var8.rawget("p"), 1.0);
            double var12 = this.loadDouble(var7.rawget("filter_edge"), 5.0E8);
            double var14 = this.loadDouble(var7.rawget("p"), 5.0E-4);
            var4.put(var6, new RoadConfig(var9, var10, var14, var12));
         }

         return var4;
      }
   }

   public List<AnimalsPathConfig> loadAnimalsPath(KahluaTable var1) {
      ArrayList var2 = new ArrayList();
      KahluaTableIterator var3 = var1.iterator();

      while(var3.advance()) {
         Object var4 = var3.getKey();
         KahluaTable var5 = (KahluaTable)var3.getValue();
         Object var6 = var5.rawget("animal");
         Object var7 = var5.rawget("count");
         Object var8 = var5.rawget("chance");
         Object var9 = var5.rawget("points");
         Object var10 = var5.rawget("radius");
         Object var11 = var5.rawget("extension");
         Object var12 = var5.rawget("extension_chance");
         String var13 = this.loadString(var6, (String)null);
         int var14 = this.loadInteger(var7, 1);
         Double var15 = this.loadDouble(var8, 0.0);
         List var16 = var9 instanceof KahluaTable ? this.loadList((KahluaTable)var9) : List.of(this.loadDouble(var9, -1.0));
         List var17 = var10 instanceof KahluaTable ? this.loadList((KahluaTable)var10) : List.of(this.loadDouble(var10, -1.0));
         List var18 = var11 instanceof KahluaTable ? this.loadList((KahluaTable)var11) : List.of(this.loadDouble(var11, -1.0));
         Double var19 = this.loadDouble(var12, 1.0);
         int[] var20 = var16.stream().mapToInt(Double::intValue).toArray();
         int[] var21 = var17.stream().mapToInt(Double::intValue).toArray();
         int[] var22 = var18.stream().mapToInt(Double::intValue).toArray();
         if (var13 != null) {
            var2.add(new AnimalsPathConfig(var13, var14, var15.floatValue(), var20, var21, var22, var19.floatValue()));
         }
      }

      return var2;
   }

   public Map<Integer, BiomeMapEntry> loadBiomeMapConfig(KahluaTable var1) {
      HashMap var2 = new HashMap();
      KahluaTableIterator var3 = var1.iterator();

      while(var3.advance()) {
         KahluaTable var4 = (KahluaTable)var3.getValue();
         int var5 = this.loadInteger(var4.rawget("pixel"), 0);
         String var6 = this.loadString(var4.rawget("biome"), (String)null);
         String var7 = this.loadString(var4.rawget("ore"), (String)null);
         String var8 = this.loadString(var4.rawget("zone"), (String)null);
         var2.put(var5, new BiomeMapEntry(var5, var6, var7, var8));
      }

      return var2;
   }

   private <T> T[] loadArray(KahluaTable var1) {
      return this.loadList(var1).toArray();
   }

   private <T> List<T> loadList(KahluaTable var1) {
      ArrayList var2 = new ArrayList();
      if (var1 == null) {
         return var2;
      } else {
         KahluaTableIterator var3 = var1.iterator();

         while(var3.advance()) {
            var2.add(var3.getValue());
         }

         return var2;
      }
   }

   private int loadInteger(Object var1, int var2) {
      return var1 == null ? var2 : ((Double)var1).intValue();
   }

   private Double loadDouble(Object var1, Double var2) {
      return var1 == null ? var2 : (Double)var1;
   }

   private String loadString(Object var1, String var2) {
      return var1 == null ? var2 : (String)var1;
   }

   private boolean loadBoolean(Object var1, boolean var2) {
      return var1 == null ? var2 : (Boolean)var1;
   }
}
