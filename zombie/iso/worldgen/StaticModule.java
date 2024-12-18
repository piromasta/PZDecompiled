package zombie.iso.worldgen;

import zombie.iso.worldgen.biomes.Biome;

public record StaticModule(Biome biome, PrefabStructure prefab, int xmin, int xmax, int ymin, int ymax) {
   public StaticModule(Biome biome, PrefabStructure prefab, int xmin, int xmax, int ymin, int ymax) {
      this.biome = biome;
      this.prefab = prefab;
      this.xmin = xmin;
      this.xmax = xmax;
      this.ymin = ymin;
      this.ymax = ymax;
   }

   public Biome biome() {
      return this.biome;
   }

   public PrefabStructure prefab() {
      return this.prefab;
   }

   public int xmin() {
      return this.xmin;
   }

   public int xmax() {
      return this.xmax;
   }

   public int ymin() {
      return this.ymin;
   }

   public int ymax() {
      return this.ymax;
   }
}
