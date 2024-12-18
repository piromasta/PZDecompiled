package zombie.pathfind;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.popman.ObjectPool;
import zombie.vehicles.Clipper;

final class ChunkDataZ {
   public Chunk chunk;
   public final ArrayList<Obstacle> obstacles = new ArrayList();
   public final ArrayList<Node> nodes = new ArrayList();
   public int z;
   static short EPOCH = 0;
   short epoch;
   public static final ObjectPool<ChunkDataZ> pool = new ObjectPool(ChunkDataZ::new);

   ChunkDataZ() {
   }

   public void init(Chunk var1, int var2) {
      this.chunk = var1;
      this.z = var2;
      this.epoch = EPOCH;
      if (PolygonalMap2.instance.clipperThread == null) {
         PolygonalMap2.instance.clipperThread = new Clipper();
      }

      Clipper var3 = PolygonalMap2.instance.clipperThread;
      var3.clear();
      int var4 = var1.wx * 8;
      int var5 = var1.wy * 8;

      int var7;
      for(int var6 = var5 - 2; var6 < var5 + 8 + 2; ++var6) {
         for(var7 = var4 - 2; var7 < var4 + 8 + 2; ++var7) {
            Square var8 = PolygonalMap2.instance.getSquare(var7, var6, var2);
            if (var8 != null && var8.has(512)) {
               if (var8.isReallySolid() || var8.has(128) || var8.has(64) || var8.has(16) || var8.has(8)) {
                  var3.addAABBBevel((float)var7 - 0.3F, (float)var6 - 0.3F, (float)var7 + 1.0F + 0.3F, (float)var6 + 1.0F + 0.3F, 0.19800001F);
               }

               if (var8.has(2) || var8.has(256)) {
                  var3.addAABBBevel((float)var7 - 0.3F, (float)var6 - 0.3F, (float)var7 + 0.3F, (float)var6 + 1.0F + 0.3F, 0.19800001F);
               }

               if (var8.has(4) || var8.has(32)) {
                  var3.addAABBBevel((float)var7 - 0.3F, (float)var6 - 0.3F, (float)var7 + 1.0F + 0.3F, (float)var6 + 0.3F, 0.19800001F);
               }

               Square var9;
               if (var8.has(256)) {
                  var9 = PolygonalMap2.instance.getSquare(var7 + 1, var6, var2);
                  if (var9 != null) {
                     var3.addAABBBevel((float)(var7 + 1) - 0.3F, (float)var6 - 0.3F, (float)(var7 + 1) + 0.3F, (float)var6 + 1.0F + 0.3F, 0.19800001F);
                  }
               }

               if (var8.has(32)) {
                  var9 = PolygonalMap2.instance.getSquare(var7, var6 + 1, var2);
                  if (var9 != null) {
                     var3.addAABBBevel((float)var7 - 0.3F, (float)(var6 + 1) - 0.3F, (float)var7 + 1.0F + 0.3F, (float)(var6 + 1) + 0.3F, 0.19800001F);
                  }
               }
            } else {
               var3.addAABB((float)var7, (float)var6, (float)var7 + 1.0F, (float)var6 + 1.0F);
            }
         }
      }

      ByteBuffer var21 = PolygonalMap2.instance.xyBufferThread;
      var7 = var3.generatePolygons();

      int var10;
      int var11;
      int var22;
      for(var22 = 0; var22 < var7; ++var22) {
         var21.clear();
         var3.getPolygon(var22, var21);
         Obstacle var23 = Obstacle.alloc().init((IsoGridSquare)null);
         this.getEdgesFromBuffer(var21, var23, true);
         var10 = var21.getShort();

         for(var11 = 0; var11 < var10; ++var11) {
            this.getEdgesFromBuffer(var21, var23, false);
         }

         var23.calcBounds();
         this.obstacles.add(var23);
      }

      var22 = var1.wx * 8;
      int var24 = var1.wy * 8;
      var10 = var22 + 8;
      var11 = var24 + 8;
      var22 -= 2;
      var24 -= 2;
      var10 += 2;
      var11 += 2;
      ImmutableRectF var12 = ImmutableRectF.alloc();
      var12.init((float)var22, (float)var24, (float)(var10 - var22), (float)(var11 - var24));
      ImmutableRectF var13 = ImmutableRectF.alloc();

      for(int var14 = 0; var14 < PolygonalMap2.instance.vehicles.size(); ++var14) {
         Vehicle var15 = (Vehicle)PolygonalMap2.instance.vehicles.get(var14);
         VehiclePoly var16 = var15.polyPlusRadius;
         float var17 = Math.min(var16.x1, Math.min(var16.x2, Math.min(var16.x3, var16.x4)));
         float var18 = Math.min(var16.y1, Math.min(var16.y2, Math.min(var16.y3, var16.y4)));
         float var19 = Math.max(var16.x1, Math.max(var16.x2, Math.max(var16.x3, var16.x4)));
         float var20 = Math.max(var16.y1, Math.max(var16.y2, Math.max(var16.y3, var16.y4)));
         var13.init(var17, var18, var19 - var17, var20 - var18);
         if (var12.intersects(var13)) {
            this.addEdgesForVehicle(var15);
         }
      }

      var12.release();
      var13.release();
   }

   private void getEdgesFromBuffer(ByteBuffer var1, Obstacle var2, boolean var3) {
      short var4 = var1.getShort();
      if (var4 < 3) {
         var1.position(var1.position() + var4 * 4 * 2);
      } else {
         EdgeRing var5 = var2.outer;
         if (!var3) {
            var5 = EdgeRing.alloc();
            var5.clear();
            var2.inner.add(var5);
         }

         int var6 = this.nodes.size();

         int var7;
         for(var7 = 0; var7 < var4; ++var7) {
            float var8 = var1.getFloat();
            float var9 = var1.getFloat();
            Node var10 = Node.alloc().init(var8, var9, this.z);
            var10.flags |= 4;
            this.nodes.add(var6, var10);
         }

         Node var12;
         for(var7 = var6; var7 < this.nodes.size() - 1; ++var7) {
            var12 = (Node)this.nodes.get(var7);
            Node var13 = (Node)this.nodes.get(var7 + 1);
            Edge var15 = Edge.alloc().init(var12, var13, var2, var5);
            var5.add(var15);
         }

         Node var11 = (Node)this.nodes.get(this.nodes.size() - 1);
         var12 = (Node)this.nodes.get(var6);
         Edge var14 = Edge.alloc().init(var11, var12, var2, var5);
         var5.add(var14);
      }
   }

   private void addEdgesForVehicle(Vehicle var1) {
      VehiclePoly var2 = var1.polyPlusRadius;
      int var3 = PZMath.fastfloor(var2.z);
      Node var4 = Node.alloc().init(var2.x1, var2.y1, var3);
      Node var5 = Node.alloc().init(var2.x2, var2.y2, var3);
      Node var6 = Node.alloc().init(var2.x3, var2.y3, var3);
      Node var7 = Node.alloc().init(var2.x4, var2.y4, var3);
      var4.flags |= 4;
      var5.flags |= 4;
      var6.flags |= 4;
      var7.flags |= 4;
      Obstacle var8 = Obstacle.alloc().init(var1);
      this.obstacles.add(var8);
      Edge var9 = Edge.alloc().init(var4, var5, var8, var8.outer);
      Edge var10 = Edge.alloc().init(var5, var6, var8, var8.outer);
      Edge var11 = Edge.alloc().init(var6, var7, var8, var8.outer);
      Edge var12 = Edge.alloc().init(var7, var4, var8, var8.outer);
      var8.outer.add(var9);
      var8.outer.add(var10);
      var8.outer.add(var11);
      var8.outer.add(var12);
      var8.calcBounds();
      this.nodes.add(var4);
      this.nodes.add(var5);
      this.nodes.add(var6);
      this.nodes.add(var7);
   }

   public void clear() {
      Node.releaseAll(this.nodes);
      this.nodes.clear();
      Obstacle.releaseAll(this.obstacles);
      this.obstacles.clear();
   }
}
