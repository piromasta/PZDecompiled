package zombie.iso.worldgen.biomes;

import java.util.Map;

public class BiomeType {
   public static Map<Class<?>, String> keys = Map.of(Hygrometry.class, "hygrometry", Landscape.class, "landscape", Plant.class, "plant", Bush.class, "bush", Temperature.class, "temperature", OreLevel.class, "ore_level");

   public BiomeType() {
   }

   public static enum Hygrometry {
      FLOODING,
      RAIN,
      DRY,
      NONE;

      private Hygrometry() {
      }
   }

   public static enum Landscape {
      LIGHT_FOREST,
      FOREST,
      PLAIN,
      NONE;

      private Landscape() {
      }
   }

   public static enum Plant {
      FLOWER,
      GRASS,
      NONE;

      private Plant() {
      }
   }

   public static enum Bush {
      DRY,
      REGULAR,
      FAT,
      NONE;

      private Bush() {
      }
   }

   public static enum Temperature {
      COLD,
      MEDIUM,
      HOT,
      NONE;

      private Temperature() {
      }
   }

   public static enum OreLevel {
      VERY_LOW,
      LOW,
      MEDIUM,
      HIGH,
      VERY_HIGH,
      NONE;

      private OreLevel() {
      }
   }
}
