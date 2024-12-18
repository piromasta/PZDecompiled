package zombie.iso.worldgen;

import java.util.List;
import java.util.Map;

public class PrefabStructure {
   private final int[] dimensions;
   private final List<String> tiles;
   private final Map<String, int[][]> schematic;
   private final float zombies;
   private final List<String> categories = List.of("Floor", "FloorFurniture", "FloorOverlay", "Furniture");

   public PrefabStructure(int[] var1, List<String> var2, Map<String, int[][]> var3, float var4) {
      this.dimensions = var1;
      this.tiles = var2;
      this.schematic = var3;
      this.zombies = var4;
   }

   public int getX() {
      return this.dimensions[0];
   }

   public int getY() {
      return this.dimensions[1];
   }

   public List<String> getCategories() {
      return this.categories;
   }

   public boolean hasCategory(String var1) {
      return this.schematic.containsKey(var1);
   }

   public int getTileRef(String var1, int var2, int var3) {
      return ((int[][])this.schematic.get(var1))[var3][var2];
   }

   public String getTile(int var1) {
      return (String)this.tiles.get(var1);
   }

   public float getZombies() {
      return this.zombies;
   }

   public String toString() {
      return String.format("<PrefabStructure@%s | [%s %s] | %s tiles>", Integer.toHexString(this.hashCode()), this.dimensions[0], this.dimensions[1], this.tiles.size());
   }
}
