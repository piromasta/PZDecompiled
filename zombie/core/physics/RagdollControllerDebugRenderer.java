package zombie.core.physics;

import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public class RagdollControllerDebugRenderer {
   private static final ArrayList<RagdollController> closestRagdollControllers = new ArrayList();

   public RagdollControllerDebugRenderer() {
   }

   public static void drawIsoDebug(IsoGameCharacter var0, boolean var1, boolean var2, Vector3 var3, RagdollStateData var4) {
      Vector3 var5 = new Vector3();
      Model.BoneToWorldCoords((IsoGameCharacter)var0, 0, var5);
      Vector3f var6 = new Vector3f();
      Vector2 var7 = var0.getForwardDirection();
      var6.set(var7.x, var7.y, 0.0F);
      if (var6.length() != 0.0F) {
         var6.normalise();
      }

      Vector3f var8 = new Vector3f();
      Vector3f.cross(var6, Core._UNIT_Z, var8);
      float var9 = 0.15F;
      float var10 = 0.15F;
      float var11 = 0.15F;
      float var12 = 0.015F;
      float var13 = 0.5F;
      Vector3f var14 = new Vector3f(1.0F, 1.0F, 0.0F);
      Vector3f var15 = new Vector3f(1.0F, 0.0F, 1.0F);
      if (var1) {
         var15 = new Vector3f(0.0F, 1.0F, 0.0F);
      }

      drawIsoPerspectiveSquare(new Vector3(var0.getX(), var0.getY(), var0.getZ()), var9, var10, var11, var12, var6, var8, var14, var13);
      drawIsoPerspectiveSquare(var3, var9, var10, var11, var12, var6, var8, var15, var13);
      Vector3f var16 = new Vector3f(1.0F, 1.0F, 1.0F);
      LineDrawer.addLine(var3.x, var3.y, var3.z, var4.pelvisDirection.x, var4.pelvisDirection.y, var4.pelvisDirection.z, var16.x, var16.y, var16.z, var13);
      if (var2) {
         var16 = new Vector3f(0.0F, 1.0F, 0.0F);
      }

      LineDrawer.addLine(var5.x, var5.y, var5.z, var5.x, var5.y, var5.z + 0.6F, var16.x, var16.y, var16.z, var13);
   }

   private static void drawIsoPerspectiveSquare(Vector3 var0, float var1, float var2, float var3, float var4, Vector3f var5, Vector3f var6, Vector3f var7, float var8) {
      float var9 = var0.x + var5.x * var2;
      float var10 = var0.y + var5.y * var2;
      float var11 = var0.x - var5.x * var3;
      float var12 = var0.y - var5.y * var3;
      float var13 = var0.z;
      float var14 = var6.x * var1;
      float var15 = var6.y * var1;
      float var16 = var9 - var14;
      float var17 = var9 + var14;
      float var18 = var11 - var14;
      float var19 = var11 + var14;
      float var20 = var12 - var15;
      float var21 = var12 + var15;
      float var22 = var10 - var15;
      float var23 = var10 + var15;
      LineDrawer.addLine(var16, var22, var13, var17, var23, var13, var7.x, var7.y, var7.z, var8);
      LineDrawer.addLine(var17, var23, var13, var19, var21, var13, var7.x, var7.y, var7.z, var8);
      LineDrawer.addLine(var19, var21, var13, var18, var20, var13, var7.x, var7.y, var7.z, var8);
      LineDrawer.addLine(var18, var20, var13, var16, var22, var13, var7.x, var7.y, var7.z, var8);
      var13 += var4;
      LineDrawer.addLine(var16, var22, var13, var17, var23, var13, var7.x, var7.y, var7.z, var8);
      LineDrawer.addLine(var17, var23, var13, var19, var21, var13, var7.x, var7.y, var7.z, var8);
      LineDrawer.addLine(var19, var21, var13, var18, var20, var13, var7.x, var7.y, var7.z, var8);
      LineDrawer.addLine(var18, var20, var13, var16, var22, var13, var7.x, var7.y, var7.z, var8);
   }

   public static boolean renderDebugPhysics() {
      return false;
   }

   public static void updateDebug(RagdollController var0) {
   }

   private static void calculateClosestRagdollControllers(RagdollController var0) {
      if (!closestRagdollControllers.contains(var0)) {
         if (closestRagdollControllers.size() < 5 && !closestRagdollControllers.contains(var0)) {
            closestRagdollControllers.add(var0);
         } else {
            IsoGameCharacter var1 = var0.getGameCharacterObject();
            IsoGameCharacter var2 = IsoCamera.getCameraCharacter();
            if (var2 != var1) {
               for(int var3 = 0; var3 < closestRagdollControllers.size(); ++var3) {
                  RagdollController var4 = (RagdollController)closestRagdollControllers.get(var3);
                  if (var4.isFree()) {
                     closestRagdollControllers.remove(var4);
                     return;
                  }

                  if (var1.distToNearestCamCharacter() < var4.getGameCharacterObject().distToNearestCamCharacter()) {
                     closestRagdollControllers.remove(var4);
                     closestRagdollControllers.add(var4);
                     return;
                  }
               }

            }
         }
      }
   }

   public static class DebugDrawSettings {
      private boolean drawRagdollBody = false;
      private boolean drawRagdollBodySinglePart = false;
      private boolean drawRagdollSkeleton = false;
      private boolean drawRagdollSkeletonSinglePart = false;

      public DebugDrawSettings() {
      }
   }
}
