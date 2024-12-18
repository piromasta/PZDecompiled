package zombie.iso.worldgen.roads;

import java.util.List;
import java.util.Objects;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.utils.ChunkCoord;

public class Road {
   private final Vector2i a;
   private final Vector2i b;
   private final ChunkCoord ca;
   private final ChunkCoord cb;
   private final RoadDirection direction;
   private final List<TileGroup> tileGroups;
   private final double probability;

   public Road(Vector2i var1, Vector2i var2, List<TileGroup> var3, double var4) {
      boolean var6 = var1.lengthSquared() > var2.lengthSquared();
      this.a = var6 ? var1 : var2;
      this.b = var6 ? var2 : var1;
      this.ca = new ChunkCoord(PZMath.fastfloor((float)this.a.x / 8.0F), PZMath.fastfloor((float)this.a.y / 8.0F));
      this.cb = new ChunkCoord(PZMath.fastfloor((float)this.b.x / 8.0F), PZMath.fastfloor((float)this.b.y / 8.0F));
      this.direction = var1.x == var2.x ? RoadDirection.NS : RoadDirection.WE;
      this.tileGroups = var3;
      this.probability = var4;
   }

   public Vector2i getA() {
      return this.a;
   }

   public Vector2i getB() {
      return this.b;
   }

   public ChunkCoord getCA() {
      return this.ca;
   }

   public ChunkCoord getCB() {
      return this.cb;
   }

   public RoadDirection getDirection() {
      return this.direction;
   }

   public List<TileGroup> getSingleFeatures() {
      return this.tileGroups;
   }

   public double getProbability() {
      return this.probability;
   }

   public String toString() {
      return String.format("Road{ a=(%d, %d), b=(%d, %d), direction=%s, tiles=%s }", this.a.x, this.a.y, this.b.x, this.b.y, this.direction, this.tileGroups);
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         Road var2 = (Road)var1;
         return Objects.equals(this.a, var2.a) && Objects.equals(this.b, var2.b) && this.direction == var2.direction;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.a, this.b, this.direction});
   }
}
