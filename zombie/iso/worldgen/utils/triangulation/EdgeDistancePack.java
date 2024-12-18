package zombie.iso.worldgen.utils.triangulation;

public class EdgeDistancePack implements Comparable<EdgeDistancePack> {
   public Edge2D edge;
   public double distance;

   public EdgeDistancePack(Edge2D var1, double var2) {
      this.edge = var1;
      this.distance = var2;
   }

   public int compareTo(EdgeDistancePack var1) {
      return Double.compare(this.distance, var1.distance);
   }
}
