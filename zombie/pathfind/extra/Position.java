package zombie.pathfind.extra;

import java.util.EnumMap;
import zombie.iso.worldgen.utils.SquareCoord;

public record Position(SquareCoord coords, Direction direction, int distance, int tick, EnumMap<Direction, BorderStatus> walls) {
   public Position(SquareCoord coords, Direction direction, int distance, int tick, EnumMap<Direction, BorderStatus> walls) {
      this.coords = coords;
      this.direction = direction;
      this.distance = distance;
      this.tick = tick;
      this.walls = walls;
   }

   public int x() {
      return this.coords.x();
   }

   public int y() {
      return this.coords.y();
   }

   public int z() {
      return this.coords.z();
   }

   public int manhattan(SquareCoord var1) {
      return Math.abs(this.coords.x() - var1.x()) + Math.abs(this.coords.y() - var1.y());
   }

   public BorderStatus isWall(Direction var1) {
      return (BorderStatus)this.walls.get(var1);
   }

   public SquareCoord coords() {
      return this.coords;
   }

   public Direction direction() {
      return this.direction;
   }

   public int distance() {
      return this.distance;
   }

   public int tick() {
      return this.tick;
   }

   public EnumMap<Direction, BorderStatus> walls() {
      return this.walls;
   }
}
