package zombie.iso.worldgen.utils.triangulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class DelaunayTriangulator {
   private List<Vector2D> pointSet;
   private TriangleSoup triangleSoup;

   public DelaunayTriangulator(List<Vector2D> var1) {
      this.pointSet = var1;
      this.triangleSoup = new TriangleSoup();
   }

   public void triangulate() throws NotEnoughPointsException {
      this.triangleSoup = new TriangleSoup();
      if (this.pointSet != null && this.pointSet.size() >= 3) {
         double var1 = 0.0;

         Vector2D var4;
         for(Iterator var3 = this.getPointSet().iterator(); var3.hasNext(); var1 = Math.max(Math.max(var4.x, var4.y), var1)) {
            var4 = (Vector2D)var3.next();
         }

         var1 *= 16.0;
         Vector2D var18 = new Vector2D(0.0, 3.0 * var1);
         var4 = new Vector2D(3.0 * var1, 0.0);
         Vector2D var5 = new Vector2D(-3.0 * var1, -3.0 * var1);
         Triangle2D var6 = new Triangle2D(var18, var4, var5);
         this.triangleSoup.add(var6);

         for(int var7 = 0; var7 < this.pointSet.size(); ++var7) {
            Triangle2D var8 = this.triangleSoup.findContainingTriangle((Vector2D)this.pointSet.get(var7));
            Triangle2D var14;
            if (var8 == null) {
               Edge2D var9 = this.triangleSoup.findNearestEdge((Vector2D)this.pointSet.get(var7));
               Triangle2D var10 = this.triangleSoup.findOneTriangleSharing(var9);
               Triangle2D var11 = this.triangleSoup.findNeighbour(var10, var9);
               Vector2D var12 = var10.getNoneEdgeVertex(var9);
               Vector2D var13 = var11.getNoneEdgeVertex(var9);
               this.triangleSoup.remove(var10);
               this.triangleSoup.remove(var11);
               var14 = new Triangle2D(var9.a, var12, (Vector2D)this.pointSet.get(var7));
               Triangle2D var15 = new Triangle2D(var9.b, var12, (Vector2D)this.pointSet.get(var7));
               Triangle2D var16 = new Triangle2D(var9.a, var13, (Vector2D)this.pointSet.get(var7));
               Triangle2D var17 = new Triangle2D(var9.b, var13, (Vector2D)this.pointSet.get(var7));
               this.triangleSoup.add(var14);
               this.triangleSoup.add(var15);
               this.triangleSoup.add(var16);
               this.triangleSoup.add(var17);
               this.legalizeEdge(var14, new Edge2D(var9.a, var12), (Vector2D)this.pointSet.get(var7));
               this.legalizeEdge(var15, new Edge2D(var9.b, var12), (Vector2D)this.pointSet.get(var7));
               this.legalizeEdge(var16, new Edge2D(var9.a, var13), (Vector2D)this.pointSet.get(var7));
               this.legalizeEdge(var17, new Edge2D(var9.b, var13), (Vector2D)this.pointSet.get(var7));
            } else {
               Vector2D var19 = var8.a;
               Vector2D var20 = var8.b;
               Vector2D var21 = var8.c;
               this.triangleSoup.remove(var8);
               Triangle2D var22 = new Triangle2D(var19, var20, (Vector2D)this.pointSet.get(var7));
               Triangle2D var23 = new Triangle2D(var20, var21, (Vector2D)this.pointSet.get(var7));
               var14 = new Triangle2D(var21, var19, (Vector2D)this.pointSet.get(var7));
               this.triangleSoup.add(var22);
               this.triangleSoup.add(var23);
               this.triangleSoup.add(var14);
               this.legalizeEdge(var22, new Edge2D(var19, var20), (Vector2D)this.pointSet.get(var7));
               this.legalizeEdge(var23, new Edge2D(var20, var21), (Vector2D)this.pointSet.get(var7));
               this.legalizeEdge(var14, new Edge2D(var21, var19), (Vector2D)this.pointSet.get(var7));
            }
         }

         this.triangleSoup.removeTrianglesUsing(var6.a);
         this.triangleSoup.removeTrianglesUsing(var6.b);
         this.triangleSoup.removeTrianglesUsing(var6.c);
      } else {
         throw new NotEnoughPointsException("Less than three points in point set.");
      }
   }

   private void legalizeEdge(Triangle2D var1, Edge2D var2, Vector2D var3) {
      Triangle2D var4 = this.triangleSoup.findNeighbour(var1, var2);
      if (var4 != null && var4.isPointInCircumcircle(var3)) {
         this.triangleSoup.remove(var1);
         this.triangleSoup.remove(var4);
         Vector2D var5 = var4.getNoneEdgeVertex(var2);
         Triangle2D var6 = new Triangle2D(var5, var2.a, var3);
         Triangle2D var7 = new Triangle2D(var5, var2.b, var3);
         this.triangleSoup.add(var6);
         this.triangleSoup.add(var7);
         this.legalizeEdge(var6, new Edge2D(var5, var2.a), var3);
         this.legalizeEdge(var7, new Edge2D(var5, var2.b), var3);
      }

   }

   public void shuffle() {
      Collections.shuffle(this.pointSet);
   }

   public void shuffle(int[] var1) {
      ArrayList var2 = new ArrayList();

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2.add((Vector2D)this.pointSet.get(var1[var3]));
      }

      this.pointSet = var2;
   }

   public List<Vector2D> getPointSet() {
      return this.pointSet;
   }

   public List<Triangle2D> getTriangles() {
      return this.triangleSoup.getTriangles();
   }

   public List<Edge2D> getEdges() {
      HashSet var1 = new HashSet();
      Iterator var2 = this.triangleSoup.getTriangles().iterator();

      while(var2.hasNext()) {
         Triangle2D var3 = (Triangle2D)var2.next();
         var1.add(new Edge2D(var3.a, var3.b));
         var1.add(new Edge2D(var3.b, var3.c));
         var1.add(new Edge2D(var3.a, var3.c));
      }

      return var1.stream().toList();
   }
}
