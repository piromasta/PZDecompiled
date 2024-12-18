package zombie.iso.worldgen;

import java.util.Iterator;
import java.util.List;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.roads.RoadEdge;
import zombie.iso.worldgen.roads.RoadGenerator;
import zombie.iso.worldgen.roads.RoadNexus;
import zombie.worldMap.UIWorldMap;

public class WGDebug {
   private static WGDebug instance = new WGDebug();

   private WGDebug() {
   }

   public static WGDebug getInstance() {
      return instance;
   }

   public void renderRoads(UIWorldMap var1) {
      for(int var2 = 0; var2 < IsoWorld.instance.getWgChunk().getRoadGenerators().size(); ++var2) {
         RoadGenerator var3 = (RoadGenerator)IsoWorld.instance.getWgChunk().getRoadGenerators().get(var2);
         Iterator var4 = var3.getRoadNexus().iterator();

         while(var4.hasNext()) {
            RoadNexus var5 = (RoadNexus)var4.next();
            Vector2i var6 = var5.getDelaunayPoint();
            List var7 = var5.getDelaunayRemotes();
            List var8 = var5.getRoadEdges();
            double var9 = 0.1 * (double)var2;
            double var11 = 1.0;
            double var13 = 0.0;
            double var15 = 1.0;
            float var17 = PZMath.floor(var1.getAPI().worldToUIX((float)var6.x, (float)var6.y));
            float var18 = PZMath.floor(var1.getAPI().worldToUIY((float)var6.x, (float)var6.y));
            var1.DrawTextureScaledColor((Texture)null, (double)var17 - 3.0, (double)var18 - 3.0, 6.0, 6.0, var9, var11, var13, var15);
            Iterator var19 = var7.iterator();

            float var21;
            float var22;
            float var23;
            float var24;
            while(var19.hasNext()) {
               Vector2i var20 = (Vector2i)var19.next();
               var21 = PZMath.floor(var1.getAPI().worldToUIX((float)var20.x, (float)var20.y));
               var22 = PZMath.floor(var1.getAPI().worldToUIY((float)var20.x, (float)var20.y));
               var23 = PZMath.floor(var1.getAPI().worldToUIX((float)var6.x, (float)var6.y));
               var24 = PZMath.floor(var1.getAPI().worldToUIY((float)var6.x, (float)var6.y));
               var1.DrawLine((Texture)null, (double)var21, (double)var22, (double)var23, (double)var24, 0.5F, var9, var11, var13, var15);
            }

            Iterator var27 = var8.iterator();

            while(var27.hasNext()) {
               RoadEdge var10 = (RoadEdge)var27.next();
               float var28 = PZMath.floor(var1.getAPI().worldToUIX((float)var10.subnexus.x, (float)var10.subnexus.y));
               float var12 = PZMath.floor(var1.getAPI().worldToUIY((float)var10.subnexus.x, (float)var10.subnexus.y));
               var13 = 0.1 * (double)var2;
               var15 = 0.0;
               double var29 = 1.0;
               double var30 = 1.0;
               var1.DrawTextureScaledColor((Texture)null, (double)var28 - 3.0, (double)var12 - 3.0, 6.0, 6.0, var13, var15, var29, var30);
               var21 = PZMath.floor(var1.getAPI().worldToUIX((float)var10.a.x, (float)var10.a.y));
               var22 = PZMath.floor(var1.getAPI().worldToUIY((float)var10.a.x, (float)var10.a.y));
               var23 = PZMath.floor(var1.getAPI().worldToUIX((float)var10.subnexus.x, (float)var10.subnexus.y));
               var24 = PZMath.floor(var1.getAPI().worldToUIY((float)var10.subnexus.x, (float)var10.subnexus.y));
               float var25 = PZMath.floor(var1.getAPI().worldToUIX((float)var10.b.x, (float)var10.b.y));
               float var26 = PZMath.floor(var1.getAPI().worldToUIY((float)var10.b.x, (float)var10.b.y));
               var1.DrawLine((Texture)null, (double)var21, (double)var22, (double)var23, (double)var24, 0.5F, var13, var15, var29, var30);
               var1.DrawLine((Texture)null, (double)var23, (double)var24, (double)var25, (double)var26, 0.5F, var13, var15, var29, var30);
            }
         }
      }

   }
}
