package zombie.iso.worldgen.roads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.joml.Vector2i;
import zombie.iso.worldgen.biomes.TileGroup;

public class RoadNexus {
   private final Vector2i delaunayPoint;
   private final List<Vector2i> delaunayRemotes;
   private final List<RoadEdge> roadEdges;

   public RoadNexus(Vector2i var1, List<Vector2i> var2, List<TileGroup> var3, double var4) {
      this.delaunayPoint = var1;
      this.delaunayRemotes = var2;
      this.roadEdges = new ArrayList();
      Iterator var6 = var2.iterator();

      while(var6.hasNext()) {
         Vector2i var7 = (Vector2i)var6.next();
         this.roadEdges.add(new RoadEdge(this.delaunayPoint, var7, var3, var4));
      }

   }

   public Vector2i getDelaunayPoint() {
      return this.delaunayPoint;
   }

   public List<Vector2i> getDelaunayRemotes() {
      return this.delaunayRemotes;
   }

   public List<RoadEdge> getRoadEdges() {
      return this.roadEdges;
   }
}
