package zombie.iso.worldgen.roads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.utils.ChunkCoord;
import zombie.iso.worldgen.utils.triangulation.DelaunayTriangulator;
import zombie.iso.worldgen.utils.triangulation.Edge2D;
import zombie.iso.worldgen.utils.triangulation.NotEnoughPointsException;
import zombie.iso.worldgen.utils.triangulation.Vector2D;

public class RoadGenerator {
   private final long seed;
   private final long offset;
   private final RoadConfig config;
   private final List<Vector2D> roadDelaunayPoints;
   private final List<Edge2D> roadDelaunayEdges;
   private final List<Edge2D> roadDelaunayEdgesFiltered;
   private final List<RoadNexus> roadNexus;
   private final Map<ChunkCoord, Set<RoadEdge>> roadEdges;

   public RoadGenerator(long var1, RoadConfig var3, long var4) {
      this.seed = var1;
      this.offset = var4;
      this.config = var3;
      this.roadDelaunayPoints = this.genRoadDelaunayPoints();
      this.roadDelaunayEdges = this.genRoadDelaunayEdges(this.roadDelaunayPoints);
      this.roadDelaunayEdgesFiltered = this.filterEdges(this.roadDelaunayEdges);
      this.roadNexus = this.genRoadNexus(this.roadDelaunayPoints, this.roadDelaunayEdgesFiltered);
      this.roadEdges = this.getRoadEdgesMap(this.roadNexus);
   }

   private List<Vector2D> genRoadDelaunayPoints() {
      HashSet var1 = new HashSet();

      for(int var2 = IsoWorld.instance.MetaGrid.minX; var2 <= IsoWorld.instance.MetaGrid.maxX; ++var2) {
         for(int var3 = IsoWorld.instance.MetaGrid.minY; var3 <= IsoWorld.instance.MetaGrid.maxY; ++var3) {
            Random var4 = WGParams.instance.getRandom(var2, var3, this.offset);
            if ((double)var4.nextFloat() < this.config.probability()) {
               var1.add(new Vector2D((double)(var2 * IsoCell.CellSizeInSquares + (int)((float)IsoCell.CellSizeInSquares * var4.nextFloat())), (double)(var3 * IsoCell.CellSizeInSquares + (int)((float)IsoCell.CellSizeInSquares * var4.nextFloat()))));
            }
         }
      }

      var1.add(new Vector2D(-1.0, 9895.0));
      var1.add(new Vector2D(12598.0, 901.0));
      return var1.stream().toList();
   }

   private List<Edge2D> genRoadDelaunayEdges(List<Vector2D> var1) {
      DelaunayTriangulator var2 = new DelaunayTriangulator(var1);

      try {
         var2.triangulate();
      } catch (NotEnoughPointsException var4) {
         throw new RuntimeException("Not enough points in triangulation", var4);
      }

      return var2.getEdges();
   }

   private List<Edge2D> filterEdges(List<Edge2D> var1) {
      return var1.stream().filter((var1x) -> {
         return var1x.magSqrt() < this.config.filter();
      }).toList();
   }

   private List<RoadNexus> genRoadNexus(List<Vector2D> var1, List<Edge2D> var2) {
      ArrayList var3 = new ArrayList();

      Vector2i var6;
      ArrayList var7;
      label33:
      for(Iterator var4 = var1.iterator(); var4.hasNext(); var3.add(new RoadNexus(var6, var7, this.config.tiles(), this.config.probaRoads()))) {
         Vector2D var5 = (Vector2D)var4.next();
         var6 = new Vector2i((int)var5.x, (int)var5.y);
         var7 = new ArrayList();
         Iterator var8 = var2.iterator();

         while(true) {
            while(true) {
               if (!var8.hasNext()) {
                  continue label33;
               }

               Edge2D var9 = (Edge2D)var8.next();
               if ((int)var9.a.x == var6.x && (int)var9.a.y == var6.y) {
                  var7.add(new Vector2i((int)var9.b.x, (int)var9.b.y));
               } else if ((int)var9.b.x == var6.x && (int)var9.b.y == var6.y) {
                  var7.add(new Vector2i((int)var9.a.x, (int)var9.a.y));
               }
            }
         }
      }

      return var3;
   }

   public List<RoadNexus> getRoadNexus() {
      return this.roadNexus;
   }

   private List<RoadEdge> getRoadEdges(List<RoadNexus> var1) {
      ArrayList var2 = new ArrayList();
      Iterator var3 = var1.iterator();

      while(var3.hasNext()) {
         RoadNexus var4 = (RoadNexus)var3.next();
         var2.addAll(var4.getRoadEdges());
      }

      return var2;
   }

   private Map<ChunkCoord, Set<RoadEdge>> getRoadEdgesMap(List<RoadNexus> var1) {
      HashMap var2 = new HashMap();
      List var3 = this.getRoadEdges(var1);
      Iterator var4 = var3.iterator();

      while(var4.hasNext()) {
         RoadEdge var5 = (RoadEdge)var4.next();
         ChunkCoord var6 = new ChunkCoord(PZMath.fastfloor((float)var5.a.x / 8.0F), PZMath.fastfloor((float)var5.a.y / 8.0F));
         ChunkCoord var7 = new ChunkCoord(PZMath.fastfloor((float)var5.b.x / 8.0F), PZMath.fastfloor((float)var5.b.y / 8.0F));
         ChunkCoord var8 = new ChunkCoord(PZMath.fastfloor((float)var5.subnexus.x / 8.0F), PZMath.fastfloor((float)var5.subnexus.y / 8.0F));
         Object var9 = (Set)var2.get(var6);
         Object var10 = (Set)var2.get(var7);
         Object var11 = (Set)var2.get(var8);
         if (var9 == null) {
            var9 = new HashSet();
         }

         if (var10 == null) {
            var10 = new HashSet();
         }

         if (var11 == null) {
            var11 = new HashSet();
         }

         ((Set)var9).add(var5);
         ((Set)var10).add(var5);
         ((Set)var11).add(var5);
         var2.put(var6, var9);
         var2.put(var7, var10);
         var2.put(var8, var11);
      }

      return var2;
   }

   public Set<Road> getRoads(int var1, int var2) {
      HashSet var3 = new HashSet();

      int var4;
      Set var5;
      Iterator var6;
      RoadEdge var7;
      Iterator var8;
      Road var9;
      for(var4 = IsoWorld.instance.MetaGrid.minX * IsoCell.CellSizeInChunks; var4 <= IsoWorld.instance.MetaGrid.maxX * IsoCell.CellSizeInChunks; ++var4) {
         var5 = (Set)this.roadEdges.get(new ChunkCoord(var4, var2));
         if (var5 != null) {
            var6 = var5.iterator();

            while(var6.hasNext()) {
               var7 = (RoadEdge)var6.next();
               var8 = var7.roads.iterator();

               while(var8.hasNext()) {
                  var9 = (Road)var8.next();
                  if (var9.getDirection() == RoadDirection.WE && Math.min(var9.getCA().x(), var9.getCB().x()) <= var1 && Math.max(var9.getCA().x(), var9.getCB().x()) >= var1) {
                     var3.add(var9);
                  }
               }
            }
         }
      }

      for(var4 = IsoWorld.instance.MetaGrid.minY * IsoCell.CellSizeInChunks; var4 <= IsoWorld.instance.MetaGrid.maxY * IsoCell.CellSizeInChunks; ++var4) {
         var5 = (Set)this.roadEdges.get(new ChunkCoord(var1, var4));
         if (var5 != null) {
            var6 = var5.iterator();

            while(var6.hasNext()) {
               var7 = (RoadEdge)var6.next();
               var8 = var7.roads.iterator();

               while(var8.hasNext()) {
                  var9 = (Road)var8.next();
                  if (var9.getDirection() == RoadDirection.NS && Math.min(var9.getCA().y(), var9.getCB().y()) <= var2 && Math.max(var9.getCA().y(), var9.getCB().y()) >= var2) {
                     var3.add(var9);
                  }
               }
            }
         }
      }

      return var3;
   }
}
