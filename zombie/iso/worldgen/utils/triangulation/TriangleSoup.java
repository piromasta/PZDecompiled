package zombie.iso.worldgen.utils.triangulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class TriangleSoup {
   private List<Triangle2D> triangleSoup = new ArrayList();

   public TriangleSoup() {
   }

   public void add(Triangle2D var1) {
      this.triangleSoup.add(var1);
   }

   public void remove(Triangle2D var1) {
      this.triangleSoup.remove(var1);
   }

   public List<Triangle2D> getTriangles() {
      return this.triangleSoup;
   }

   public Triangle2D findContainingTriangle(Vector2D var1) {
      Iterator var2 = this.triangleSoup.iterator();

      Triangle2D var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (Triangle2D)var2.next();
      } while(!var3.contains(var1));

      return var3;
   }

   public Triangle2D findNeighbour(Triangle2D var1, Edge2D var2) {
      Iterator var3 = this.triangleSoup.iterator();

      Triangle2D var4;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         var4 = (Triangle2D)var3.next();
      } while(!var4.isNeighbour(var2) || var4 == var1);

      return var4;
   }

   public Triangle2D findOneTriangleSharing(Edge2D var1) {
      Iterator var2 = this.triangleSoup.iterator();

      Triangle2D var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (Triangle2D)var2.next();
      } while(!var3.isNeighbour(var1));

      return var3;
   }

   public Edge2D findNearestEdge(Vector2D var1) {
      ArrayList var2 = new ArrayList();
      Iterator var3 = this.triangleSoup.iterator();

      while(var3.hasNext()) {
         Triangle2D var4 = (Triangle2D)var3.next();
         var2.add(var4.findNearestEdge(var1));
      }

      EdgeDistancePack[] var5 = new EdgeDistancePack[var2.size()];
      var2.toArray(var5);
      Arrays.sort(var5);
      return var5[0].edge;
   }

   public void removeTrianglesUsing(Vector2D var1) {
      ArrayList var2 = new ArrayList();
      Iterator var3 = this.triangleSoup.iterator();

      while(var3.hasNext()) {
         Triangle2D var4 = (Triangle2D)var3.next();
         if (var4.hasVertex(var1)) {
            var2.add(var4);
         }
      }

      this.triangleSoup.removeAll(var2);
   }
}
