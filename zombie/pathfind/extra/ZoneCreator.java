package zombie.pathfind.extra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.worldgen.utils.SquareCoord;

public class ZoneCreator {
   private static final Comparator<Pos> COMPARATOR_X_Y = Comparator.comparing(Pos::isDone).thenComparing(Pos::x).thenComparing(Pos::y);
   private static final Comparator<Pos> COMPARATOR_Y_X = Comparator.comparing(Pos::isDone).thenComparing(Pos::y).thenComparing(Pos::x);
   private static final Comparator<Rectangle> COMPARATOR_REC_X = Comparator.comparing(Rectangle::xmin).thenComparing(Rectangle::xmax).thenComparing(Rectangle::ymin);

   public ZoneCreator() {
   }

   public List<Rectangle> generateRectangles(Collection<Position> var1) {
      ArrayList var2 = new ArrayList();
      LinkedList var3 = new LinkedList();
      var1.forEach((var1x) -> {
         var3.add(new Pos(var1x));
      });
      var3.sort(COMPARATOR_Y_X);

      int var5;
      int var8;
      int var16;
      while(!((Pos)var3.get(0)).isDone()) {
         int var4 = 0;
         var5 = 2147483647;
         int var6 = -2147483647;

         while(true) {
            Pos var7 = (Pos)var3.get(var4);
            var5 = Math.min(var5, var7.x());
            var6 = Math.max(var6, var7.x());
            if (var7.isWall(Direction.EAST) != BorderStatus.OPEN) {
               var16 = ((Pos)var3.get(0)).y();
               var8 = ((Pos)var3.get(0)).y();

               for(int var9 = var5; var9 <= var6; ++var9) {
                  for(int var10 = var16; var10 <= var8; ++var10) {
                     List var13 = var3.stream().filter((var2x) -> {
                        return var2x.x() == var9 && var2x.y() == var10;
                     }).toList();
                     if (!var13.isEmpty()) {
                        ((Pos)var13.get(0)).setDone(true);
                     }
                  }
               }

               var2.add(new Rectangle(var5, var6, var16, var8, 0));
               var3.sort(COMPARATOR_Y_X);
               break;
            }

            ++var4;
         }
      }

      ArrayList var14 = new ArrayList();
      var2.sort(COMPARATOR_REC_X);

      for(var5 = 0; var5 < var2.size(); ++var5) {
         Rectangle var15 = (Rectangle)var2.get(var5);
         var16 = ((Rectangle)var2.get(var5)).ymax();

         for(var8 = var5 + 1; var8 < var2.size(); ++var8) {
            Rectangle var17 = (Rectangle)var2.get(var8);
            if (var17.xmin() != var15.xmin() || var17.xmax() != var15.xmax() || var17.ymin() != var15.ymin() + (var8 - var5)) {
               var14.add(new Rectangle(var15.xmin(), var15.xmax() + 1, var15.ymin(), var16 + 1, 0));
               var5 = var8 - 1;
               break;
            }

            var16 = ((Rectangle)var2.get(var8)).ymax();
            if (var8 == var2.size() - 1) {
               var14.add(new Rectangle(var15.xmin(), var15.xmax() + 1, var15.ymin(), var16 + 1, 0));
               return var14;
            }
         }
      }

      return var14;
   }

   public void generateZones(List<Rectangle> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         Rectangle var3 = (Rectangle)var2.next();
         String var4 = String.format("Zone_%s_%s_%s_%s_%s", var3.xmin(), var3.ymin(), var3.xmax(), var3.ymax(), var3.z());
         DesignationZoneAnimal var5 = new DesignationZoneAnimal(var4, var3.xmin(), var3.ymin(), var3.z(), var3.xmax(), var3.ymax());
         var5.createSurroundingFence();
      }

   }

   public static class Pos {
      private final SquareCoord coords;
      private final EnumMap<Direction, BorderStatus> walls;
      private boolean done;

      public Pos(Position var1) {
         this.coords = var1.coords();
         this.walls = var1.walls();
         this.done = false;
      }

      public Pos(SquareCoord var1, EnumMap<Direction, BorderStatus> var2) {
         this.coords = var1;
         this.walls = var2;
         this.done = false;
      }

      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 != null && this.getClass() == var1.getClass()) {
            Pos var2 = (Pos)var1;
            return Objects.equals(this.coords, var2.coords);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hashCode(this.coords);
      }

      public int x() {
         return this.coords.x();
      }

      public int y() {
         return this.coords.y();
      }

      public SquareCoord coords() {
         return this.coords;
      }

      public boolean isDone() {
         return this.done;
      }

      public void setDone(boolean var1) {
         this.done = var1;
      }

      public BorderStatus isWall(Direction var1) {
         return (BorderStatus)this.walls.get(var1);
      }
   }

   public static record Rectangle(int xmin, int xmax, int ymin, int ymax, int z) {
      public Rectangle(int xmin, int xmax, int ymin, int ymax, int z) {
         this.xmin = xmin;
         this.xmax = xmax;
         this.ymin = ymin;
         this.ymax = ymax;
         this.z = z;
      }

      public int xmin() {
         return this.xmin;
      }

      public int xmax() {
         return this.xmax;
      }

      public int ymin() {
         return this.ymin;
      }

      public int ymax() {
         return this.ymax;
      }

      public int z() {
         return this.z;
      }
   }
}
