package zombie.iso.worldgen.biomes;

import java.util.List;
import java.util.Map;
import zombie.iso.worldgen.utils.Direction;

public record Feature(List<TileGroup> tileGroups, Map<Direction, List<TileGroup>> attachments, float probability) {
   public Feature(List<TileGroup> tileGroups, Map<Direction, List<TileGroup>> attachments, float probability) {
      this.tileGroups = tileGroups;
      this.attachments = attachments;
      this.probability = probability;
   }

   public List<TileGroup> tileGroups() {
      return this.tileGroups;
   }

   public Map<Direction, List<TileGroup>> attachments() {
      return this.attachments;
   }

   public float probability() {
      return this.probability;
   }
}
