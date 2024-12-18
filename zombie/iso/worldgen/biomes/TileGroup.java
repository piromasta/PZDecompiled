package zombie.iso.worldgen.biomes;

import java.util.List;

public record TileGroup(int sx, int sy, List<String> tiles) {
   public TileGroup(int sx, int sy, List<String> tiles) {
      this.sx = sx;
      this.sy = sy;
      this.tiles = tiles;
   }

   public int sx() {
      return this.sx;
   }

   public int sy() {
      return this.sy;
   }

   public List<String> tiles() {
      return this.tiles;
   }
}
