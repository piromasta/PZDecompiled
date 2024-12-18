package zombie.iso.worldgen.biomes;

import java.util.List;

public record Grass(float fernChance, float noGrassDiv, List<Double> noGrassStages, List<Double> grassStages) {
   public static Grass DEFAULT = new Grass(0.3F, 12.0F, List.of(0.4), List.of(0.33, 0.5));

   public Grass(float fernChance, float noGrassDiv, List<Double> noGrassStages, List<Double> grassStages) {
      this.fernChance = fernChance;
      this.noGrassDiv = noGrassDiv;
      this.noGrassStages = noGrassStages;
      this.grassStages = grassStages;
   }

   public float fernChance() {
      return this.fernChance;
   }

   public float noGrassDiv() {
      return this.noGrassDiv;
   }

   public List<Double> noGrassStages() {
      return this.noGrassStages;
   }

   public List<Double> grassStages() {
      return this.grassStages;
   }
}
