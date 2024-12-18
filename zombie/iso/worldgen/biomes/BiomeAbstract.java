package zombie.iso.worldgen.biomes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class BiomeAbstract implements IBiome {
   protected EnumSet<BiomeType.Landscape> landscape = EnumSet.allOf(BiomeType.Landscape.class);
   protected EnumSet<BiomeType.Plant> plant = EnumSet.allOf(BiomeType.Plant.class);
   protected EnumSet<BiomeType.Bush> bush = EnumSet.allOf(BiomeType.Bush.class);
   protected EnumSet<BiomeType.Temperature> temperature = EnumSet.allOf(BiomeType.Temperature.class);
   protected EnumSet<BiomeType.Hygrometry> hygrometry = EnumSet.allOf(BiomeType.Hygrometry.class);
   protected EnumSet<BiomeType.OreLevel> oreLevel = EnumSet.allOf(BiomeType.OreLevel.class);
   protected List<String> placement = new ArrayList();
   protected String name;
   protected String parent;
   protected boolean generate;
   protected Grass grass;
   protected float zombies = 0.0F;

   public BiomeAbstract() {
   }

   public String name() {
      return this.name;
   }

   public abstract Map<FeatureType, List<Feature>> getFeatures();

   public EnumSet<BiomeType.Landscape> landscape() {
      return this.landscape;
   }

   public EnumSet<BiomeType.Plant> plant() {
      return this.plant;
   }

   public EnumSet<BiomeType.Bush> bush() {
      return this.bush;
   }

   public EnumSet<BiomeType.Temperature> temperature() {
      return this.temperature;
   }

   public EnumSet<BiomeType.Hygrometry> hygrometry() {
      return this.hygrometry;
   }

   public EnumSet<BiomeType.OreLevel> oreLevel() {
      return this.oreLevel;
   }

   public List<String> placement() {
      return this.placement;
   }

   public String parent() {
      return this.parent;
   }

   public boolean generate() {
      return this.generate;
   }

   public float zombies() {
      return this.zombies;
   }

   public Grass grass() {
      return this.grass;
   }

   public IBiome landscape(BiomeType.Landscape var1) {
      this.landscape = EnumSet.of(var1);
      return this;
   }

   public IBiome plant(BiomeType.Plant var1) {
      this.plant = EnumSet.of(var1);
      return this;
   }

   public IBiome bush(BiomeType.Bush var1) {
      this.bush = EnumSet.of(var1);
      return this;
   }

   public IBiome temperature(BiomeType.Temperature var1) {
      this.temperature = EnumSet.of(var1);
      return this;
   }

   public IBiome hygrometry(BiomeType.Hygrometry var1) {
      this.hygrometry = EnumSet.of(var1);
      return this;
   }

   public IBiome oreLevel(BiomeType.OreLevel var1) {
      this.oreLevel = EnumSet.of(var1);
      return this;
   }

   public IBiome landscape(EnumSet<BiomeType.Landscape> var1) {
      this.landscape = var1;
      return this;
   }

   public IBiome plant(EnumSet<BiomeType.Plant> var1) {
      this.plant = var1;
      return this;
   }

   public IBiome bush(EnumSet<BiomeType.Bush> var1) {
      this.bush = var1;
      return this;
   }

   public IBiome temperature(EnumSet<BiomeType.Temperature> var1) {
      this.temperature = var1;
      return this;
   }

   public IBiome hygrometry(EnumSet<BiomeType.Hygrometry> var1) {
      this.hygrometry = var1;
      return this;
   }

   public IBiome oreLevel(EnumSet<BiomeType.OreLevel> var1) {
      this.oreLevel = var1;
      return this;
   }

   public IBiome placement(List<String> var1) {
      this.placement = var1;
      return this;
   }

   public IBiome zombies(float var1) {
      this.zombies = var1;
      return this;
   }

   public IBiome grass(Grass var1) {
      this.grass = var1;
      return this;
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         BiomeAbstract var2 = (BiomeAbstract)var1;
         return this.generate == var2.generate && Float.compare(this.zombies, var2.zombies) == 0 && Objects.equals(this.landscape, var2.landscape) && Objects.equals(this.plant, var2.plant) && Objects.equals(this.bush, var2.bush) && Objects.equals(this.temperature, var2.temperature) && Objects.equals(this.hygrometry, var2.hygrometry) && Objects.equals(this.oreLevel, var2.oreLevel) && Objects.equals(this.placement, var2.placement) && Objects.equals(this.name, var2.name) && Objects.equals(this.parent, var2.parent) && Objects.equals(this.grass, var2.grass);
      } else {
         return false;
      }
   }

   public int hashCode() {
      int var1 = Objects.hashCode(this.landscape);
      var1 = 31 * var1 + Objects.hashCode(this.plant);
      var1 = 31 * var1 + Objects.hashCode(this.bush);
      var1 = 31 * var1 + Objects.hashCode(this.temperature);
      var1 = 31 * var1 + Objects.hashCode(this.hygrometry);
      var1 = 31 * var1 + Objects.hashCode(this.oreLevel);
      var1 = 31 * var1 + Objects.hashCode(this.placement);
      var1 = 31 * var1 + Objects.hashCode(this.name);
      var1 = 31 * var1 + Objects.hashCode(this.parent);
      var1 = 31 * var1 + Boolean.hashCode(this.generate);
      var1 = 31 * var1 + Objects.hashCode(this.grass);
      var1 = 31 * var1 + Float.hashCode(this.zombies);
      return var1;
   }
}
