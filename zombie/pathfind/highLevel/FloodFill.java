package zombie.pathfind.highLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.utils.BooleanGrid;
import zombie.pathfind.Chunk;
import zombie.pathfind.MoverType;
import zombie.pathfind.PMMover;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.VehicleRect;
import zombie.pathfind.VisibilityGraph;

public final class FloodFill {
   static final int CPW = 8;
   static final PMMover mover = new PMMover();
   final ArrayDeque<Square> m_stack = new ArrayDeque();
   boolean m_thumpable = false;
   private final BooleanGrid m_visited = new BooleanGrid(8, 8);
   final ArrayList<Square> m_choices = new ArrayList(64);
   HLChunkRegion m_region;
   Square[][] m_squares;
   int m_minX;
   int m_minY;
   final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList();

   public FloodFill() {
   }

   void calculate(HLChunkRegion var1, Square[][] var2, Square var3) {
      this.m_region = var1;
      this.m_squares = var2;
      this.m_thumpable = var3.has(131072);
      this.m_minX = this.m_region.getChunk().getMinX();
      this.m_minY = this.m_region.getChunk().getMinY();
      this.initVisibilityGraphs();
      this.push(var3.getX(), var3.getY());

      int var6;
      while((var3 = this.pop()) != null) {
         var6 = var3.getX();

         int var5;
         for(var5 = var3.getY(); this.shouldVisit(var6, var5, var6, var5 - 1); --var5) {
         }

         boolean var7 = false;
         boolean var8 = false;

         while(true) {
            this.m_visited.setValue(this.gridX(var6), this.gridY(var5), true);
            Square var9 = this.m_squares[this.gridX(var6)][this.gridY(var5)];
            if (var9 != null) {
               this.m_choices.add(var9);
            }

            if (!var7 && this.shouldVisit(var6, var5, var6 - 1, var5)) {
               this.push(var6 - 1, var5);
               var7 = true;
            } else if (var7 && !this.shouldVisit(var6, var5, var6 - 1, var5)) {
               var7 = false;
            } else if (var7 && !this.shouldVisit(var6 - 1, var5, var6 - 1, var5 - 1)) {
               this.push(var6 - 1, var5);
            }

            if (!var8 && this.shouldVisit(var6, var5, var6 + 1, var5)) {
               this.push(var6 + 1, var5);
               var8 = true;
            } else if (var8 && !this.shouldVisit(var6, var5, var6 + 1, var5)) {
               var8 = false;
            } else if (var8 && !this.shouldVisit(var6 + 1, var5, var6 + 1, var5 - 1)) {
               this.push(var6 + 1, var5);
            }

            ++var5;
            if (!this.shouldVisit(var6, var5 - 1, var6, var5)) {
               break;
            }
         }
      }

      this.m_region.minX = 2147483647;
      this.m_region.minY = 2147483647;
      this.m_region.maxX = -2147483648;
      this.m_region.maxY = -2147483648;

      for(var6 = 0; var6 < this.m_choices.size(); ++var6) {
         var3 = (Square)this.m_choices.get(var6);
         this.m_region.m_squaresMask.setValue(this.gridX(var3.getX()), this.gridY(var3.getY()), true);
         this.m_region.minX = PZMath.min(this.m_region.minX, var3.getX());
         this.m_region.minY = PZMath.min(this.m_region.minY, var3.getY());
         this.m_region.maxX = PZMath.max(this.m_region.maxX, var3.getX());
         this.m_region.maxY = PZMath.max(this.m_region.maxY, var3.getY());
      }

   }

   boolean shouldVisit(int var1, int var2, int var3, int var4) {
      if (this.gridX(var3) < 8 && this.gridX(var3) >= 0) {
         if (this.gridY(var4) < 8 && this.gridY(var4) >= 0) {
            if (this.m_visited.getValue(this.gridX(var3), this.gridY(var4))) {
               return false;
            } else {
               Square var5 = this.m_squares[this.gridX(var3)][this.gridY(var4)];
               if (this.m_thumpable) {
                  if (var5 == null) {
                     return false;
                  }

                  if (!var5.has(131072)) {
                     return false;
                  }

                  if (!var5.TreatAsSolidFloor()) {
                     return false;
                  }
               } else {
                  if (!this.m_region.m_levelData.canWalkOnSquare(var5)) {
                     return false;
                  }

                  Square var6 = this.m_squares[this.gridX(var1)][this.gridY(var2)];
                  if (this.m_region.m_levelData.isCanPathTransition(var6, var5)) {
                     return false;
                  }
               }

               mover.type = MoverType.Player;
               mover.minLevel = this.m_region.getLevel();
               mover.maxLevel = this.m_region.getLevel();
               return PolygonalMap2.instance.canMoveBetween(mover, var1, var2, this.m_region.getLevel(), var3, var4, this.m_region.getLevel());
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void push(int var1, int var2) {
      Square var3 = this.m_squares[this.gridX(var1)][this.gridY(var2)];
      this.m_stack.push(var3);
   }

   Square pop() {
      return this.m_stack.isEmpty() ? null : (Square)this.m_stack.pop();
   }

   int gridX(int var1) {
      return var1 - this.m_minX;
   }

   int gridY(int var1) {
      return var1 - this.m_minY;
   }

   void initVisibilityGraphs() {
      Chunk var1 = this.m_region.getChunk();
      this.visibilityGraphs.clear();
      PolygonalMap2.instance.getVisibilityGraphsOverlappingChunk(var1, this.m_region.getLevel(), this.visibilityGraphs);

      for(int var2 = 0; var2 < this.visibilityGraphs.size(); ++var2) {
         VisibilityGraph var3 = (VisibilityGraph)this.visibilityGraphs.get(var2);

         for(int var4 = 0; var4 < var3.cluster.rects.size(); ++var4) {
            VehicleRect var5 = (VehicleRect)var3.cluster.rects.get(var4);

            for(int var6 = var5.y - 1; var6 < var5.y + var5.h + 1; ++var6) {
               for(int var7 = var5.x - 1; var7 < var5.x + var5.w + 1; ++var7) {
                  if (var1.contains(var7, var6)) {
                     Square var8 = this.m_squares[this.gridX(var7)][this.gridY(var6)];
                     if (var8 == null || !var8.has(504) && !var8.hasSlopedSurface()) {
                        this.m_visited.setValue(var7 - this.m_minX, var6 - this.m_minY, true);
                     }
                  }
               }
            }
         }
      }

   }

   void reset() {
      this.m_choices.clear();
      this.m_stack.clear();
      this.m_visited.clear();
   }
}
