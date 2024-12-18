package zombie.characters.animals.pathfind;

import org.joml.Vector2f;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;

public final class NestedPathWanderer {
   public NestedPaths m_paths;
   public NestedPath m_path;
   public float m_x;
   public float m_y;
   boolean m_bMoveForwardOnPath = true;
   float m_switchPathTimer = 0.0F;

   public NestedPathWanderer() {
   }

   void pickAnotherPath() {
      boolean var1 = Rand.NextBool(2);
      int var2 = this.m_paths.m_paths.indexOf(this.m_path);
      Vector2f var3 = new Vector2f();
      int var4;
      NestedPath var5;
      if (var1) {
         for(var4 = var2 - 1; var4 >= 0; --var4) {
            var5 = (NestedPath)this.m_paths.m_paths.get(var4);
            if (var5.m_inset == this.m_path.m_inset - 5) {
               var5.getClosestPointOn(this.m_x, this.m_y, var3);
               if (Vector2f.distance(this.m_x, this.m_y, var3.x, var3.y) < 10.0F) {
                  this.m_path = var5;
                  return;
               }
            }
         }
      } else {
         for(var4 = var2 + 1; var4 < this.m_paths.m_paths.size(); ++var4) {
            var5 = (NestedPath)this.m_paths.m_paths.get(var4);
            if (var5.m_inset == this.m_path.m_inset + 5) {
               var5.getClosestPointOn(this.m_x, this.m_y, var3);
               if (Vector2f.distance(this.m_x, this.m_y, var3.x, var3.y) < 10.0F) {
                  this.m_path = var5;
                  return;
               }
            }
         }
      }

   }

   void moveAlongPath(float var1) {
      Vector2f var2 = new Vector2f();
      float var3 = this.m_path.getClosestPointOn(this.m_x, this.m_y, var2);
      float var5 = this.m_path.getLength();
      float var4;
      if (this.m_bMoveForwardOnPath) {
         var4 = var3 + var1 / var5;
         if (var4 >= 1.0F) {
            var4 %= 1.0F;
         }
      } else {
         var4 = var3 - var1 / var5;
         if (var4 <= 0.0F) {
            var4 = (var4 + 1.0F) % 1.0F;
         }
      }

      this.m_path.getPointOn(var4, var2);
      this.m_x = var2.x;
      this.m_y = var2.y;
   }

   public void render(UIWorldMap var1) {
      if (++this.m_switchPathTimer >= 90.0F) {
         this.pickAnotherPath();
         this.m_switchPathTimer = 0.0F;
      }

      this.moveAlongPath(1.0F);
      this.drawRect(var1, this.m_x - 1.0F, this.m_y - 1.0F, 2.0F, 2.0F, 0.0F, 1.0F, 0.0F, 1.0F);
   }

   public void drawLine(UIWorldMap var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      WorldMapRenderer var10 = var1.getAPIv1().getRenderer();
      float var11 = var10.worldToUIX(var2, var3, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      float var12 = var10.worldToUIY(var2, var3, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      float var13 = var10.worldToUIX(var4, var5, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      float var14 = var10.worldToUIY(var4, var5, var10.getDisplayZoomF(), var10.getCenterWorldX(), var10.getCenterWorldY(), var10.getModelViewMatrix(), var10.getProjectionMatrix());
      SpriteRenderer.instance.renderline((Texture)null, (int)var11, (int)var12, (int)var13, (int)var14, var6, var7, var8, var9, 1.0F);
   }

   public void drawRect(UIWorldMap var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      this.drawLine(var1, var2, var3, var2 + var4, var3, var6, var7, var8, var9);
      this.drawLine(var1, var2 + var4, var3, var2 + var4, var3 + var5, var6, var7, var8, var9);
      this.drawLine(var1, var2, var3 + var5, var2 + var4, var3 + var5, var6, var7, var8, var9);
      this.drawLine(var1, var2, var3, var2, var3 + var5, var6, var7, var8, var9);
   }
}
