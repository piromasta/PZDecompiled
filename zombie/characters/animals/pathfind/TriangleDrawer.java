package zombie.characters.animals.pathfind;

import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;

final class TriangleDrawer extends TextureDraw.GenericDrawer {
   final ArrayList<Vector2f> triangles = new ArrayList();
   final TShortArrayList trianglesOnBoundaries = new TShortArrayList();
   int z;
   float camOffX;
   float camOffY;

   TriangleDrawer() {
   }

   void init(MeshList var1) {
      this.triangles.clear();
      this.trianglesOnBoundaries.clear();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         Mesh var3 = var1.get(var2);
         int var4 = this.triangles.size();
         this.triangles.addAll(var3.triangles);

         for(int var5 = 0; var5 < var3.trianglesOnBoundaries.size(); ++var5) {
            this.trianglesOnBoundaries.add((short)(var4 + var3.trianglesOnBoundaries.get(var5)));
         }
      }

      this.z = var1.z;
      this.camOffX = IsoCamera.frameState.OffX;
      this.camOffY = IsoCamera.frameState.OffY;
   }

   public void render() {
      GL11.glDisable(3553);
      GL11.glBegin(4);

      for(int var1 = 0; var1 < this.triangles.size(); ++var1) {
         if (var1 % 3 == 0) {
            if (this.trianglesOnBoundaries.contains((short)var1)) {
               GL11.glColor4f(0.0F, 0.0F, 1.0F, 0.25F);
            } else {
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.25F);
            }
         }

         Vector2f var2 = (Vector2f)this.triangles.get(var1);
         float var3 = IsoUtils.XToScreen(var2.x, var2.y, (float)this.z, 0) - this.camOffX;
         float var4 = IsoUtils.YToScreen(var2.x, var2.y, (float)this.z, 0) - this.camOffY;
         GL11.glVertex3f(var3, var4, (float)this.z);
      }

      GL11.glEnd();
      GL11.glEnable(3553);
   }
}
