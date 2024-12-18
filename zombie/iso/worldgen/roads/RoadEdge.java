package zombie.iso.worldgen.roads;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Vector2i;
import zombie.iso.worldgen.biomes.TileGroup;

public class RoadEdge {
   public final Vector2i a;
   public final Vector2i b;
   public final Vector2i subnexus;
   public final List<Road> roads;

   public RoadEdge(Vector2i var1, Vector2i var2, List<TileGroup> var3, double var4) {
      boolean var6 = var1.lengthSquared() > var2.lengthSquared();
      this.a = var6 ? var1 : var2;
      this.b = var6 ? var2 : var1;
      this.subnexus = new Vector2i(this.a.x, this.b.y);
      this.roads = new ArrayList();
      this.roads.add(new Road(this.a, this.subnexus, var3, var4));
      this.roads.add(new Road(this.subnexus, this.b, var3, var4));
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         RoadEdge var2 = (RoadEdge)var1;
         return Objects.equals(this.a, var2.a) && Objects.equals(this.b, var2.b);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.a, this.b});
   }
}
