package zombie.iso.worldgen.biomes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import zombie.debug.DebugLog;

public class BiomeRegistry {
   public static BiomeRegistry instance = new BiomeRegistry();

   private BiomeRegistry() {
   }

   public IBiome get(Map<String, Biome> var1, double[] var2, double var3, Map<BiomeType.Landscape, List<Double>> var5, Map<BiomeType.Plant, List<Double>> var6, Map<BiomeType.Bush, List<Double>> var7, Map<BiomeType.Temperature, List<Double>> var8, Map<BiomeType.Hygrometry, List<Double>> var9, Map<BiomeType.OreLevel, List<Double>> var10) {
      BiomeType.Landscape var11 = this.getLandscape(var2, var5);
      BiomeType.Plant var12 = this.getPlant(var2, var6);
      BiomeType.Bush var13 = this.getBush(var2, var7);
      BiomeType.Temperature var14 = this.getTemperature(var2, var8);
      BiomeType.Hygrometry var15 = this.getHygrometry(var2, var9);
      BiomeType.OreLevel var16 = this.getOre(var2, var10);
      List var17 = (List)var1.values().stream().filter((var1x) -> {
         return var1x.landscape().contains(var11);
      }).filter((var1x) -> {
         return var1x.plant().contains(var12);
      }).filter((var1x) -> {
         return var1x.bush().contains(var8);
      }).filter((var1x) -> {
         return var1x.temperature().contains(var14);
      }).filter((var1x) -> {
         return var1x.hygrometry().contains(var15);
      }).filter((var1x) -> {
         return var1x.oreLevel().contains(var9);
      }).collect(Collectors.toList());
      return (IBiome)(var17.isEmpty() ? Biome.DEFAULT_BIOME : (IBiome)var17.get((int)(var3 * (double)var17.size())));
   }

   public IBiome get(Map<String, Biome> var1, String var2, double[] var3, double var4, Map<BiomeType.Bush, List<Double>> var6, Map<BiomeType.OreLevel, List<Double>> var7) {
      if (var2 == null) {
         return null;
      } else {
         BiomeType.Bush var8 = this.getBush(var3, var6);
         BiomeType.OreLevel var9 = this.getOre(var3, var7);
         List var10 = (List)var1.values().stream().filter((var1x) -> {
            return var1x.name().startsWith(var2);
         }).filter((var1x) -> {
            return var1x.bush().contains(var8);
         }).filter((var1x) -> {
            return var1x.oreLevel().contains(var9);
         }).collect(Collectors.toList());
         if (var10.isEmpty()) {
            DebugLog.log(String.format("ERROR: No match for %s", var2));
            return Biome.DEFAULT_BIOME;
         } else {
            return (IBiome)var10.get((int)(var4 * (double)var10.size()));
         }
      }
   }

   private BiomeType.Landscape getLandscape(double[] var1, Map<BiomeType.Landscape, List<Double>> var2) {
      BiomeType.Landscape var3;
      if (var1[0] >= (Double)((List)var2.get(BiomeType.Landscape.FOREST)).get(0) && var1[0] < (Double)((List)var2.get(BiomeType.Landscape.FOREST)).get(1)) {
         var3 = BiomeType.Landscape.FOREST;
      } else if (var1[0] >= (Double)((List)var2.get(BiomeType.Landscape.LIGHT_FOREST)).get(0) && var1[0] < (Double)((List)var2.get(BiomeType.Landscape.LIGHT_FOREST)).get(1)) {
         var3 = BiomeType.Landscape.LIGHT_FOREST;
      } else if (var1[0] >= (Double)((List)var2.get(BiomeType.Landscape.PLAIN)).get(0) && var1[0] < (Double)((List)var2.get(BiomeType.Landscape.PLAIN)).get(1)) {
         var3 = BiomeType.Landscape.PLAIN;
      } else {
         var3 = BiomeType.Landscape.NONE;
      }

      return var3;
   }

   private BiomeType.Plant getPlant(double[] var1, Map<BiomeType.Plant, List<Double>> var2) {
      BiomeType.Plant var3;
      if (var1[1] >= (Double)((List)var2.get(BiomeType.Plant.FLOWER)).get(0) && var1[1] < (Double)((List)var2.get(BiomeType.Plant.FLOWER)).get(1)) {
         var3 = BiomeType.Plant.FLOWER;
      } else if (var1[1] >= (Double)((List)var2.get(BiomeType.Plant.GRASS)).get(0) && var1[1] < (Double)((List)var2.get(BiomeType.Plant.GRASS)).get(1)) {
         var3 = BiomeType.Plant.GRASS;
      } else {
         var3 = BiomeType.Plant.NONE;
      }

      return var3;
   }

   private BiomeType.Bush getBush(double[] var1, Map<BiomeType.Bush, List<Double>> var2) {
      BiomeType.Bush var3;
      if (var1[1] >= (Double)((List)var2.get(BiomeType.Bush.DRY)).get(0) && var1[1] < (Double)((List)var2.get(BiomeType.Bush.DRY)).get(1)) {
         var3 = BiomeType.Bush.DRY;
      } else if (var1[1] >= (Double)((List)var2.get(BiomeType.Bush.REGULAR)).get(0) && var1[1] < (Double)((List)var2.get(BiomeType.Bush.REGULAR)).get(1)) {
         var3 = BiomeType.Bush.REGULAR;
      } else if (var1[1] >= (Double)((List)var2.get(BiomeType.Bush.FAT)).get(0) && var1[1] < (Double)((List)var2.get(BiomeType.Bush.FAT)).get(1)) {
         var3 = BiomeType.Bush.FAT;
      } else {
         var3 = BiomeType.Bush.NONE;
      }

      return var3;
   }

   private BiomeType.Temperature getTemperature(double[] var1, Map<BiomeType.Temperature, List<Double>> var2) {
      BiomeType.Temperature var3;
      if (var1[2] >= (Double)((List)var2.get(BiomeType.Temperature.HOT)).get(0) && var1[2] < (Double)((List)var2.get(BiomeType.Temperature.HOT)).get(1)) {
         var3 = BiomeType.Temperature.HOT;
      } else if (var1[2] >= (Double)((List)var2.get(BiomeType.Temperature.MEDIUM)).get(0) && var1[2] < (Double)((List)var2.get(BiomeType.Temperature.MEDIUM)).get(1)) {
         var3 = BiomeType.Temperature.MEDIUM;
      } else if (var1[2] >= (Double)((List)var2.get(BiomeType.Temperature.COLD)).get(0) && var1[2] < (Double)((List)var2.get(BiomeType.Temperature.COLD)).get(1)) {
         var3 = BiomeType.Temperature.COLD;
      } else {
         var3 = BiomeType.Temperature.NONE;
      }

      return var3;
   }

   private BiomeType.Hygrometry getHygrometry(double[] var1, Map<BiomeType.Hygrometry, List<Double>> var2) {
      BiomeType.Hygrometry var3;
      if (var1[3] >= (Double)((List)var2.get(BiomeType.Hygrometry.FLOODING)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.Hygrometry.FLOODING)).get(1)) {
         var3 = BiomeType.Hygrometry.FLOODING;
      } else if (var1[3] >= (Double)((List)var2.get(BiomeType.Hygrometry.RAIN)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.Hygrometry.RAIN)).get(1)) {
         var3 = BiomeType.Hygrometry.RAIN;
      } else if (var1[3] >= (Double)((List)var2.get(BiomeType.Hygrometry.DRY)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.Hygrometry.DRY)).get(1)) {
         var3 = BiomeType.Hygrometry.DRY;
      } else {
         var3 = BiomeType.Hygrometry.NONE;
      }

      return var3;
   }

   private BiomeType.OreLevel getOre(double[] var1, Map<BiomeType.OreLevel, List<Double>> var2) {
      BiomeType.OreLevel var3;
      if (var1[3] >= (Double)((List)var2.get(BiomeType.OreLevel.VERY_HIGH)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.OreLevel.VERY_HIGH)).get(1)) {
         var3 = BiomeType.OreLevel.VERY_HIGH;
      } else if (var1[3] >= (Double)((List)var2.get(BiomeType.OreLevel.HIGH)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.OreLevel.HIGH)).get(1)) {
         var3 = BiomeType.OreLevel.HIGH;
      } else if (var1[3] >= (Double)((List)var2.get(BiomeType.OreLevel.MEDIUM)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.OreLevel.MEDIUM)).get(1)) {
         var3 = BiomeType.OreLevel.MEDIUM;
      } else if (var1[3] >= (Double)((List)var2.get(BiomeType.OreLevel.LOW)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.OreLevel.LOW)).get(1)) {
         var3 = BiomeType.OreLevel.LOW;
      } else if (var1[3] >= (Double)((List)var2.get(BiomeType.OreLevel.VERY_LOW)).get(0) && var1[3] < (Double)((List)var2.get(BiomeType.OreLevel.VERY_LOW)).get(1)) {
         var3 = BiomeType.OreLevel.VERY_LOW;
      } else {
         var3 = BiomeType.OreLevel.NONE;
      }

      return var3;
   }
}
