package zombie.iso.worldgen.zones;

public record AnimalsPathConfig(String animalType, int count, float chance, int[] points, int[] radius, int[] extension, float extensionChance) {
   public AnimalsPathConfig(String animalType, int count, float chance, int[] points, int[] radius, int[] extension, float extensionChance) {
      this.animalType = animalType;
      this.count = count;
      this.chance = chance;
      this.points = points;
      this.radius = radius;
      this.extension = extension;
      this.extensionChance = extensionChance;
   }

   public int getNameHash() {
      return this.animalType.hashCode();
   }

   public String animalType() {
      return this.animalType;
   }

   public int count() {
      return this.count;
   }

   public float chance() {
      return this.chance;
   }

   public int[] points() {
      return this.points;
   }

   public int[] radius() {
      return this.radius;
   }

   public int[] extension() {
      return this.extension;
   }

   public float extensionChance() {
      return this.extensionChance;
   }
}
