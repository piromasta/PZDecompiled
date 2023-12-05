package zombie.worldMap;

import java.util.ArrayList;

public class StrokeGeometry {
   static Point s_firstPoint = null;
   static Point s_lastPoint = null;
   static final double EPSILON = 1.0E-4;

   public StrokeGeometry() {
   }

   static Point newPoint(double var0, double var2) {
      if (s_firstPoint == null) {
         return new Point(var0, var2);
      } else {
         Point var4 = s_firstPoint;
         s_firstPoint = s_firstPoint.next;
         if (s_lastPoint == var4) {
            s_lastPoint = null;
         }

         var4.next = null;
         return var4.set(var0, var2);
      }
   }

   static void release(Point var0) {
      if (var0.next == null && var0 != s_lastPoint) {
         var0.next = s_firstPoint;
         s_firstPoint = var0;
         if (s_lastPoint == null) {
            s_lastPoint = var0;
         }

      }
   }

   static void release(ArrayList<Point> var0) {
      for(int var1 = 0; var1 < var0.size(); ++var1) {
         release((Point)var0.get(var1));
      }

   }

   static ArrayList<Point> getStrokeGeometry(Point[] var0, Attrs var1) {
      if (var0.length < 2) {
         return null;
      } else {
         String var2 = var1.cap;
         String var3 = var1.join;
         float var4 = var1.width / 2.0F;
         float var5 = var1.miterLimit;
         ArrayList var6 = new ArrayList();
         ArrayList var7 = new ArrayList();
         boolean var8 = false;
         if (var0.length == 2) {
            var3 = "bevel";
            createTriangles(var0[0], StrokeGeometry.Point.Middle(var0[0], var0[1]), var0[1], var6, var4, var3, var5);
         } else {
            int var9;
            for(var9 = 0; var9 < var0.length - 1; ++var9) {
               if (var9 == 0) {
                  var7.add(var0[0]);
               } else if (var9 == var0.length - 2) {
                  var7.add(var0[var0.length - 1]);
               } else {
                  var7.add(StrokeGeometry.Point.Middle(var0[var9], var0[var9 + 1]));
               }
            }

            for(var9 = 1; var9 < var7.size(); ++var9) {
               createTriangles((Point)var7.get(var9 - 1), var0[var9], (Point)var7.get(var9), var6, var4, var3, var5);
            }
         }

         if (!var8) {
            Point var10;
            Point var15;
            if (var2.equals("round")) {
               var15 = (Point)var6.get(0);
               var10 = (Point)var6.get(1);
               Point var11 = var0[1];
               Point var12 = (Point)var6.get(var6.size() - 1);
               Point var13 = (Point)var6.get(var6.size() - 3);
               Point var14 = var0[var0.length - 2];
               createRoundCap(var0[0], var15, var10, var11, var6);
               createRoundCap(var0[var0.length - 1], var12, var13, var14, var6);
            } else if (var2.equals("square")) {
               var15 = (Point)var6.get(var6.size() - 1);
               var10 = (Point)var6.get(var6.size() - 3);
               createSquareCap((Point)var6.get(0), (Point)var6.get(1), StrokeGeometry.Point.Sub(var0[0], var0[1]).normalize().scalarMult(StrokeGeometry.Point.Sub(var0[0], (Point)var6.get(0)).length()), var6);
               createSquareCap(var15, var10, StrokeGeometry.Point.Sub(var0[var0.length - 1], var0[var0.length - 2]).normalize().scalarMult(StrokeGeometry.Point.Sub(var10, var0[var0.length - 1]).length()), var6);
            }
         }

         return var6;
      }
   }

   static void createSquareCap(Point var0, Point var1, Point var2, ArrayList<Point> var3) {
      var3.add(var0);
      var3.add(StrokeGeometry.Point.Add(var0, var2));
      var3.add(StrokeGeometry.Point.Add(var1, var2));
      var3.add(var1);
      var3.add(StrokeGeometry.Point.Add(var1, var2));
      var3.add(var0);
   }

   static void createRoundCap(Point var0, Point var1, Point var2, Point var3, ArrayList<Point> var4) {
      double var5 = StrokeGeometry.Point.Sub(var0, var1).length();
      double var7 = Math.atan2(var2.y - var0.y, var2.x - var0.x);
      double var9 = Math.atan2(var1.y - var0.y, var1.x - var0.x);
      double var11 = var7;
      if (var9 > var7) {
         if (var9 - var7 >= 3.141492653589793) {
            var9 -= 6.283185307179586;
         }
      } else if (var7 - var9 >= 3.141492653589793) {
         var7 -= 6.283185307179586;
      }

      double var13 = var9 - var7;
      if (Math.abs(var13) >= 3.141492653589793 && Math.abs(var13) <= 3.1416926535897933) {
         Point var15 = StrokeGeometry.Point.Sub(var0, var3);
         if (var15.x == 0.0) {
            if (var15.y > 0.0) {
               var13 = -var13;
            }
         } else if (var15.x >= -1.0E-4) {
            var13 = -var13;
         }
      }

      int var19 = (int)(Math.abs(var13 * var5) / 7.0);
      ++var19;
      double var16 = var13 / (double)var19;

      for(int var18 = 0; var18 < var19; ++var18) {
         var4.add(newPoint(var0.x, var0.y));
         var4.add(newPoint(var0.x + var5 * Math.cos(var11 + var16 * (double)var18), var0.y + var5 * Math.sin(var11 + var16 * (double)var18)));
         var4.add(newPoint(var0.x + var5 * Math.cos(var11 + var16 * (double)(1 + var18)), var0.y + var5 * Math.sin(var11 + var16 * (double)(1 + var18))));
      }

   }

   static double signedArea(Point var0, Point var1, Point var2) {
      return (var1.x - var0.x) * (var2.y - var0.y) - (var2.x - var0.x) * (var1.y - var0.y);
   }

   static Point lineIntersection(Point var0, Point var1, Point var2, Point var3) {
      double var4 = var1.y - var0.y;
      double var6 = var0.x - var1.x;
      double var8 = var3.y - var2.y;
      double var10 = var2.x - var3.x;
      double var12 = var4 * var10 - var8 * var6;
      if (var12 > -1.0E-4 && var12 < 1.0E-4) {
         return null;
      } else {
         double var14 = var4 * var0.x + var6 * var0.y;
         double var16 = var8 * var2.x + var10 * var2.y;
         double var18 = (var10 * var14 - var6 * var16) / var12;
         double var20 = (var4 * var16 - var8 * var14) / var12;
         return newPoint(var18, var20);
      }
   }

   static void createTriangles(Point var0, Point var1, Point var2, ArrayList<Point> var3, float var4, String var5, float var6) {
      Point var7 = StrokeGeometry.Point.Sub(var1, var0);
      Point var8 = StrokeGeometry.Point.Sub(var2, var1);
      var7.perpendicular();
      var8.perpendicular();
      if (signedArea(var0, var1, var2) > 0.0) {
         var7.invert();
         var8.invert();
      }

      var7.normalize();
      var8.normalize();
      var7.scalarMult((double)var4);
      var8.scalarMult((double)var4);
      Point var9 = lineIntersection(StrokeGeometry.Point.Add(var7, var0), StrokeGeometry.Point.Add(var7, var1), StrokeGeometry.Point.Add(var8, var2), StrokeGeometry.Point.Add(var8, var1));
      Point var10 = null;
      double var11 = 1.7976931348623157E308;
      if (var9 != null) {
         var10 = StrokeGeometry.Point.Sub(var9, var1);
         var11 = var10.length();
      }

      double var13 = (double)((int)(var11 / (double)var4));
      Point var15 = StrokeGeometry.Point.Sub(var0, var1);
      double var16 = var15.length();
      Point var18 = StrokeGeometry.Point.Sub(var1, var2);
      double var19 = var18.length();
      if (!(var11 > var16) && !(var11 > var19)) {
         var3.add(StrokeGeometry.Point.Add(var0, var7));
         var3.add(StrokeGeometry.Point.Sub(var0, var7));
         var3.add(StrokeGeometry.Point.Sub(var1, var10));
         var3.add(StrokeGeometry.Point.Add(var0, var7));
         var3.add(StrokeGeometry.Point.Sub(var1, var10));
         var3.add(StrokeGeometry.Point.Add(var1, var7));
         if (var5.equals("round")) {
            Point var21 = StrokeGeometry.Point.Add(var1, var7);
            Point var22 = StrokeGeometry.Point.Add(var1, var8);
            Point var23 = StrokeGeometry.Point.Sub(var1, var10);
            var3.add(var21);
            var3.add(var1);
            var3.add(var23);
            createRoundCap(var1, var21, var22, var23, var3);
            var3.add(var1);
            var3.add(var22);
            var3.add(var23);
         } else {
            if (var5.equals("bevel") || var5.equals("miter") && var13 >= (double)var6) {
               var3.add(StrokeGeometry.Point.Add(var1, var7));
               var3.add(StrokeGeometry.Point.Add(var1, var8));
               var3.add(StrokeGeometry.Point.Sub(var1, var10));
            }

            if (var5.equals("miter") && var13 < (double)var6) {
               var3.add(var9);
               var3.add(StrokeGeometry.Point.Add(var1, var7));
               var3.add(StrokeGeometry.Point.Add(var1, var8));
            }
         }

         var3.add(StrokeGeometry.Point.Add(var2, var8));
         var3.add(StrokeGeometry.Point.Sub(var1, var10));
         var3.add(StrokeGeometry.Point.Add(var1, var8));
         var3.add(StrokeGeometry.Point.Add(var2, var8));
         var3.add(StrokeGeometry.Point.Sub(var1, var10));
         var3.add(StrokeGeometry.Point.Sub(var2, var8));
      } else {
         var3.add(StrokeGeometry.Point.Add(var0, var7));
         var3.add(StrokeGeometry.Point.Sub(var0, var7));
         var3.add(StrokeGeometry.Point.Add(var1, var7));
         var3.add(StrokeGeometry.Point.Sub(var0, var7));
         var3.add(StrokeGeometry.Point.Add(var1, var7));
         var3.add(StrokeGeometry.Point.Sub(var1, var7));
         if (var5.equals("round")) {
            createRoundCap(var1, StrokeGeometry.Point.Add(var1, var7), StrokeGeometry.Point.Add(var1, var8), var2, var3);
         } else if (var5.equals("bevel") || var5.equals("miter") && var13 >= (double)var6) {
            var3.add(var1);
            var3.add(StrokeGeometry.Point.Add(var1, var7));
            var3.add(StrokeGeometry.Point.Add(var1, var8));
         } else if (var5.equals("miter") && var13 < (double)var6 && var9 != null) {
            var3.add(StrokeGeometry.Point.Add(var1, var7));
            var3.add(var1);
            var3.add(var9);
            var3.add(StrokeGeometry.Point.Add(var1, var8));
            var3.add(var1);
            var3.add(var9);
         }

         var3.add(StrokeGeometry.Point.Add(var2, var8));
         var3.add(StrokeGeometry.Point.Sub(var1, var8));
         var3.add(StrokeGeometry.Point.Add(var1, var8));
         var3.add(StrokeGeometry.Point.Add(var2, var8));
         var3.add(StrokeGeometry.Point.Sub(var1, var8));
         var3.add(StrokeGeometry.Point.Sub(var2, var8));
      }

   }

   public static final class Point {
      double x;
      double y;
      Point next;

      Point() {
         this.x = 0.0;
         this.y = 0.0;
      }

      Point(double var1, double var3) {
         this.x = var1;
         this.y = var3;
      }

      Point set(double var1, double var3) {
         this.x = var1;
         this.y = var3;
         return this;
      }

      Point scalarMult(double var1) {
         this.x *= var1;
         this.y *= var1;
         return this;
      }

      Point perpendicular() {
         double var1 = this.x;
         this.x = -this.y;
         this.y = var1;
         return this;
      }

      Point invert() {
         this.x = -this.x;
         this.y = -this.y;
         return this;
      }

      double length() {
         return Math.sqrt(this.x * this.x + this.y * this.y);
      }

      Point normalize() {
         double var1 = this.length();
         this.x /= var1;
         this.y /= var1;
         return this;
      }

      double angle() {
         return this.y / this.x;
      }

      static double Angle(Point var0, Point var1) {
         return Math.atan2(var1.x - var0.x, var1.y - var0.y);
      }

      static Point Add(Point var0, Point var1) {
         return StrokeGeometry.newPoint(var0.x + var1.x, var0.y + var1.y);
      }

      static Point Sub(Point var0, Point var1) {
         return StrokeGeometry.newPoint(var0.x - var1.x, var0.y - var1.y);
      }

      static Point Middle(Point var0, Point var1) {
         return Add(var0, var1).scalarMult(0.5);
      }
   }

   static class Attrs {
      String cap = "butt";
      String join = "bevel";
      float width = 1.0F;
      float miterLimit = 10.0F;

      Attrs() {
      }
   }
}
