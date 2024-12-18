package zombie.iso.worldgen.roads;

import java.util.List;
import zombie.iso.worldgen.biomes.TileGroup;

public record RoadConfig(List<TileGroup> tiles, double probaRoads, double probability, double filter) {
   public RoadConfig(List<TileGroup> tiles, double probaRoads, double probability, double filter) {
      this.tiles = tiles;
      this.probaRoads = probaRoads;
      this.probability = probability;
      this.filter = filter;
   }

   public List<TileGroup> tiles() {
      return this.tiles;
   }

   public double probaRoads() {
      return this.probaRoads;
   }

   public double probability() {
      return this.probability;
   }

   public double filter() {
      return this.filter;
   }
}
