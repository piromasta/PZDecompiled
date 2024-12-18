package zombie.iso.worldgen.biomes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Biome extends BiomeAbstract {
   public static Biome DEFAULT_BIOME = new DefaultBiome();
   private final Map<FeatureType, List<Feature>> features;

   public Biome(String var1, String var2, boolean var3, Map<FeatureType, List<Feature>> var4, EnumSet<BiomeType.Landscape> var5, EnumSet<BiomeType.Plant> var6, EnumSet<BiomeType.Bush> var7, EnumSet<BiomeType.Temperature> var8, EnumSet<BiomeType.Hygrometry> var9, EnumSet<BiomeType.OreLevel> var10, float var11, List<String> var12, Grass var13) {
      this.name = var1;
      this.parent = var2;
      this.generate = var3;
      this.features = var4;
      if (var5 != null) {
         this.landscape = var5;
      }

      if (var6 != null) {
         this.plant = var6;
      }

      if (var7 != null) {
         this.bush = var7;
      }

      if (var8 != null) {
         this.temperature = var8;
      }

      if (var9 != null) {
         this.hygrometry = var9;
      }

      if (var10 != null) {
         this.oreLevel = var10;
      }

      this.placement = var12;
      this.zombies = var11;
      this.grass = var13;
   }

   public String toString() {
      return String.format("<Biome@%s | %s | %s | %s | %s | %s | %s>", Integer.toHexString(this.hashCode()), this.landscape, this.plant, this.temperature, this.landscape, this.oreLevel, this.zombies);
   }

   public Map<FeatureType, List<Feature>> getFeatures() {
      return this.features;
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         if (!super.equals(var1)) {
            return false;
         } else {
            Biome var2 = (Biome)var1;
            return this.features.equals(var2.features);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int var1 = super.hashCode();
      var1 = 31 * var1 + this.features.hashCode();
      return var1;
   }

   private static class DefaultBiome extends Biome {
      public DefaultBiome() {
         super("DEFAULT", (String)null, true, defaultMap(), EnumSet.of(BiomeType.Landscape.NONE), EnumSet.of(BiomeType.Plant.NONE), EnumSet.of(BiomeType.Bush.NONE), EnumSet.of(BiomeType.Temperature.NONE), EnumSet.of(BiomeType.Hygrometry.NONE), EnumSet.of(BiomeType.OreLevel.NONE), 0.0F, List.of(), new Grass(0.0F, 1.0F, List.of(0.4), List.of(0.33, 0.5)));
      }

      private static Map<FeatureType, List<Feature>> defaultMap() {
         HashMap var0 = new HashMap();
         List var1 = List.of("carpentry_02_58");
         ArrayList var2 = new ArrayList();
         var2.add(new Feature(List.of(new TileGroup(1, 1, var1)), new HashMap(), 1.0F));
         var0.put(FeatureType.GROUND, var2);
         return var0;
      }
   }
}
