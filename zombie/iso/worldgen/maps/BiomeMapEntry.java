package zombie.iso.worldgen.maps;

public record BiomeMapEntry(int pixel, String biome, String ore, String zone) {
   public BiomeMapEntry(int pixel, String biome, String ore, String zone) {
      this.pixel = pixel;
      this.biome = biome;
      this.ore = ore;
      this.zone = zone;
   }

   public int pixel() {
      return this.pixel;
   }

   public String biome() {
      return this.biome;
   }

   public String ore() {
      return this.ore;
   }

   public String zone() {
      return this.zone;
   }
}
