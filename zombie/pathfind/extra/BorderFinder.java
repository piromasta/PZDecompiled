package zombie.pathfind.extra;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.worldgen.utils.SquareCoord;

public class BorderFinder {
   private final IsoWorld world;
   private final Position player;
   private final int maxRange;
   private final List<SquareCoord> found = new ArrayList();
   private final List<Queue<Position>> parsed = new ArrayList();
   private final Map<SquareCoord, Position> path = new HashMap();
   private SquareCoord coordinates;
   private int maxDistance;
   private boolean running = true;
   private int tick;

   public BorderFinder(int var1, int var2, int var3, int var4, IsoWorld var5) {
      this.coordinates = new SquareCoord(var1, var2, var3);
      this.world = var5;
      this.player = new Position(this.coordinates, Direction.NORTH, 0, 0, new EnumMap(Direction.class));
      this.maxRange = var4;
   }

   public void run() {
      this.found.clear();
      this.path.clear();
      this.parsed.clear();
      this.parsed.add(new PriorityQueue((var1x, var2) -> {
         return this.player.manhattan(var2.coords()) - this.player.manhattan(var1x.coords());
      }));

      for(int var1 = 1; var1 < 9; ++var1) {
         this.parsed.add(new LinkedList());
      }

      while(this.running) {
         this.oneStep();
         ++this.tick;
      }

      if (DebugOptions.instance.PathfindBorderFinder.getValue()) {
         BorderFinderRenderer.instance.addAllPath(this.path.values());
      }

   }

   private BorderStatus hasWall(IsoGridSquare var1, Direction var2) {
      if (var2 == Direction.NORTH && (var1.getProperties().Is(IsoFlagType.collideN) || var1.getProperties().Is(IsoFlagType.doorN))) {
         return BorderStatus.WALL;
      } else if (var2 != Direction.WEST || !var1.getProperties().Is(IsoFlagType.collideW) && !var1.getProperties().Is(IsoFlagType.doorW)) {
         SquareCoord var3 = Direction.move(var1.getCoords(), var2);
         IsoGridSquare var4;
         if (var2 == Direction.SOUTH) {
            var4 = this.world.CurrentCell.getGridSquare(var3.x(), var3.y(), var3.z());
            if (var4 == null || var4.getProperties().Is(IsoFlagType.collideN) || var4.getProperties().Is(IsoFlagType.doorN)) {
               return BorderStatus.WALL;
            }
         }

         if (var2 == Direction.EAST) {
            var4 = this.world.CurrentCell.getGridSquare(var3.x(), var3.y(), var3.z());
            if (var4 == null || var4.getProperties().Is(IsoFlagType.collideW) || var4.getProperties().Is(IsoFlagType.doorW)) {
               return BorderStatus.WALL;
            }
         }

         if (var3.x() - this.player.coords().x() <= this.maxRange && var3.x() - this.player.coords().x() >= -this.maxRange && var3.y() - this.player.coords().y() <= this.maxRange && var3.y() - this.player.coords().y() >= -this.maxRange) {
            return var1.getCollideMatrix(var2.x(), var2.y(), var2.z()) ? BorderStatus.WALL : BorderStatus.OPEN;
         } else {
            return BorderStatus.OUT_OF_RANGE;
         }
      } else {
         return BorderStatus.WALL;
      }
   }

   private void oneStep() {
      IsoGridSquare var1 = this.world.CurrentCell.getGridSquare(this.coordinates.x(), this.coordinates.y(), this.coordinates.z());
      if (var1 != null) {
         Direction[] var2 = Direction.values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Direction var5 = var2[var4];
            SquareCoord var6 = Direction.move(this.coordinates, var5);
            IsoGridSquare var7 = this.world.CurrentCell.getGridSquare(var6.x(), var6.y(), var6.z());
            if (!this.found.contains(var6) && this.hasWall(var1, var5) == BorderStatus.OPEN && var7 != null) {
               EnumMap var8 = new EnumMap(Direction.class);
               int var9 = 0;
               Direction[] var10 = Direction.values();
               int var11 = var10.length;

               for(int var12 = 0; var12 < var11; ++var12) {
                  Direction var13 = var10[var12];
                  BorderStatus var14 = this.hasWall(var7, var13);
                  var8.put(var13, var14);
                  var9 += var14 != BorderStatus.OPEN ? 1 : 0;
               }

               ((Queue)this.parsed.get(var9)).offer(new Position(var6, var5, this.player.manhattan(var6), this.tick, var8));
               this.found.add(var6);
            }
         }

         for(int var15 = 8; var15 >= 0; --var15) {
            Queue var16 = (Queue)this.parsed.get(var15);
            if (!var16.isEmpty()) {
               Position var17 = (Position)var16.poll();
               if (var15 == 0 && var17.distance() < this.maxDistance) {
                  this.running = false;
               }

               this.maxDistance = PZMath.max(this.maxDistance, var17.distance());
               this.coordinates = var17.coords();
               this.path.put(var17.coords(), var17);
               break;
            }
         }

      }
   }

   public Map<SquareCoord, Position> getPath() {
      return this.path;
   }
}
