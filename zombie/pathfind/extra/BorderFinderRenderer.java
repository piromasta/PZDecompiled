package zombie.pathfind.extra;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;

public class BorderFinderRenderer {
   public static final BorderFinderRenderer instance = new BorderFinderRenderer();
   private final Set<Position> path = new HashSet();
   private final Object renderLock = new Object();

   private BorderFinderRenderer() {
   }

   public void addAllPath(Collection<Position> var1) {
      this.path.addAll(var1);
   }

   public void render() {
      if (DebugOptions.instance.PathfindBorderFinder.getValue()) {
         Iterator var1 = this.path.iterator();

         while(var1.hasNext()) {
            Position var2 = (Position)var1.next();
            LineDrawer.addLine((float)var2.coords().x() + 0.45F, (float)var2.coords().y() + 0.45F, (float)var2.coords().z(), (float)var2.coords().x() + 0.55F, (float)var2.coords().y() + 0.55F, (float)var2.coords().z(), 0.5F, 1.0F, 0.5F, (String)null, false);
            Direction[] var3 = Direction.values();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               Direction var6 = var3[var5];
               float var7 = 0.0F;
               float var8 = 0.0F;
               float var9 = 0.0F;
               if (var2.walls().get(var6) == BorderStatus.OUT_OF_RANGE) {
                  var7 = 1.0F;
               } else {
                  var9 = 1.0F;
               }

               if (var2.walls().get(var6) != BorderStatus.OPEN) {
                  float var10000;
                  switch (var6) {
                     case NORTH:
                     case SOUTH:
                        var10000 = 0.0F;
                        break;
                     case WEST:
                        var10000 = 0.2F;
                        break;
                     case EAST:
                        var10000 = 0.8F;
                        break;
                     default:
                        var10000 = 0.5F;
                  }

                  float var10 = var10000;
                  switch (var6) {
                     case NORTH:
                     case SOUTH:
                        var10000 = 1.0F;
                        break;
                     case WEST:
                        var10000 = 0.2F;
                        break;
                     case EAST:
                        var10000 = 0.8F;
                        break;
                     default:
                        var10000 = 0.5F;
                  }

                  float var11 = var10000;
                  switch (var6) {
                     case NORTH:
                        var10000 = 0.2F;
                        break;
                     case SOUTH:
                        var10000 = 0.8F;
                        break;
                     case WEST:
                     case EAST:
                        var10000 = 0.0F;
                        break;
                     default:
                        var10000 = 0.5F;
                  }

                  float var12 = var10000;
                  switch (var6) {
                     case NORTH:
                        var10000 = 0.2F;
                        break;
                     case SOUTH:
                        var10000 = 0.8F;
                        break;
                     case WEST:
                     case EAST:
                        var10000 = 1.0F;
                        break;
                     default:
                        var10000 = 0.5F;
                  }

                  float var13 = var10000;
                  LineDrawer.addLine((float)var2.coords().x() + var10, (float)var2.coords().y() + var12, (float)var2.coords().z(), (float)var2.coords().x() + var11, (float)var2.coords().y() + var13, (float)var2.coords().z(), var7, var8, var9, (String)null, false);
               }
            }
         }
      }

   }
}
