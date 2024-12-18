package zombie.characters.animals.pathfind;

import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Vector2f;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.zones.Zone;
import zombie.vehicles.Clipper;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.WorldMapRenderer;

public final class NestedPaths {
   private static Clipper s_clipper = null;
   private static ByteBuffer s_clipperBuffer = null;
   public final ArrayList<NestedPath> m_paths = new ArrayList();

   public NestedPaths() {
   }

   public void init(Zone var1) {
      for(int var2 = 5; this.generatePolygon(var1.points, var2); var2 += 5) {
      }

   }

   boolean generatePolygon(TIntArrayList var1, int var2) {
      if (s_clipper == null) {
         s_clipper = new Clipper();
      }

      s_clipper.clear();
      if (s_clipperBuffer == null || s_clipperBuffer.capacity() < var1.size() * 8 * 4) {
         s_clipperBuffer = ByteBuffer.allocateDirect(var1.size() * 8 * 4);
      }

      s_clipperBuffer.clear();
      int var3;
      if (this.isClockwise(var1)) {
         for(var3 = this.numPoints(var1) - 1; var3 >= 0; --var3) {
            s_clipperBuffer.putFloat((float)this.getX(var1, var3));
            s_clipperBuffer.putFloat((float)this.getY(var1, var3));
         }
      } else {
         for(var3 = 0; var3 < this.numPoints(var1); ++var3) {
            s_clipperBuffer.putFloat((float)this.getX(var1, var3));
            s_clipperBuffer.putFloat((float)this.getY(var1, var3));
         }
      }

      s_clipper.addPath(this.numPoints(var1), s_clipperBuffer, false);
      var3 = s_clipper.generatePolygons((double)(-var2));
      if (var3 <= 0) {
         return false;
      } else {
         boolean var4 = false;

         for(int var5 = 0; var5 < var3; ++var5) {
            s_clipperBuffer.clear();
            s_clipper.getPolygon(var5, s_clipperBuffer);
            short var6 = s_clipperBuffer.getShort();
            if (var6 >= 3) {
               float[] var7 = new float[var6 * 2];
               float var8 = 3.4028235E38F;
               float var9 = 3.4028235E38F;
               float var10 = 1.4E-45F;
               float var11 = 1.4E-45F;

               for(int var12 = 0; var12 < var6; ++var12) {
                  var7[var12 * 2] = s_clipperBuffer.getFloat();
                  var7[var12 * 2 + 1] = s_clipperBuffer.getFloat();
                  var8 = PZMath.min(var8, var7[var12 * 2]);
                  var9 = PZMath.min(var9, var7[var12 * 2 + 1]);
                  var10 = PZMath.max(var10, var7[var12 * 2]);
                  var11 = PZMath.max(var11, var7[var12 * 2 + 1]);
               }

               if (!(var10 - var8 < 5.0F) && !(var11 - var9 < 5.0F)) {
                  NestedPath var13 = new NestedPath();
                  var13.m_points = var7;
                  var13.m_inset = var2;
                  var13.m_minX = var8;
                  var13.m_minY = var9;
                  var13.m_maxX = var10;
                  var13.m_maxY = var11;
                  var13.m_length = this.getLength(var7);
                  this.m_paths.add(var13);
                  var4 = true;
               }
            }
         }

         return var4;
      }
   }

   boolean isClockwise(TIntArrayList var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < this.numPoints(var1); ++var3) {
         int var4 = this.getX(var1, var3);
         int var5 = this.getY(var1, var3);
         int var6 = this.getX(var1, (var3 + 1) % this.numPoints(var1));
         int var7 = this.getY(var1, (var3 + 1) % this.numPoints(var1));
         var2 += (float)((var6 - var4) * (var7 + var5));
      }

      return (double)var2 > 0.0;
   }

   int numPoints(TIntArrayList var1) {
      return var1.size() / 2;
   }

   int getX(TIntArrayList var1, int var2) {
      return var1.get(var2 * 2);
   }

   int getY(TIntArrayList var1, int var2) {
      return var1.get(var2 * 2 + 1);
   }

   float getLength(float[] var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < var1.length; var3 += 2) {
         float var4 = var1[var3];
         float var5 = var1[var3 + 1];
         float var6 = var1[(var3 + 2) % var1.length];
         float var7 = var1[(var3 + 3) % var1.length];
         var2 += Vector2f.length(var6 - var4, var7 - var5);
      }

      return var2;
   }

   public void render(UIWorldMap var1) {
      Iterator var2 = this.m_paths.iterator();

      while(var2.hasNext()) {
         NestedPath var3 = (NestedPath)var2.next();
         float[] var4 = var3.m_points;

         for(int var5 = 0; var5 < var4.length; var5 += 2) {
            float var6 = var4[var5];
            float var7 = var4[var5 + 1];
            float var8 = var4[(var5 + 2) % var4.length];
            float var9 = var4[(var5 + 3) % var4.length];
            this.drawLine(var1.getAPIv1(), var6, var7, var8, var9, 0.0F, 0.0F, 1.0F, 1.0F);
         }
      }

   }

   public void drawLine(UIWorldMapV1 var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      WorldMapRenderer var10 = var1.getRenderer();
      float var11 = var10.worldToUIX(var2, var3, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      float var12 = var10.worldToUIY(var2, var3, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      float var13 = var10.worldToUIX(var4, var5, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      float var14 = var10.worldToUIY(var4, var5, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      SpriteRenderer.instance.renderline((Texture)null, (int)var11, (int)var12, (int)var13, (int)var14, var6, var7, var8, var9, 1.0F);
   }
}
