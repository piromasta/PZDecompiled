package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.ai.states.PlayerGetUpState;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.popman.ObjectPool;
import zombie.seating.SeatingManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelCameraRenderData extends TextureDraw.GenericDrawer {
   private ModelCamera m_camera;
   private float m_angle;
   private boolean m_bUseWorldIso;
   private float m_x;
   private float m_y;
   private float m_z;
   private boolean m_bInVehicle;
   public static final ObjectPool<ModelCameraRenderData> s_pool = new ObjectPool(ModelCameraRenderData::new);

   public ModelCameraRenderData() {
   }

   public ModelCameraRenderData init(ModelCamera var1, ModelManager.ModelSlot var2) {
      IsoMovingObject var3 = var2.model.object;
      IsoGameCharacter var4 = (IsoGameCharacter)Type.tryCastTo(var3, IsoGameCharacter.class);
      this.m_camera = var1;
      this.m_x = var3.getX();
      this.m_y = var3.getY();
      this.m_z = var3.getZ();
      if (var4 == null) {
         this.m_angle = 0.0F;
         this.m_bInVehicle = false;
         this.m_bUseWorldIso = !BaseVehicle.RENDER_TO_TEXTURE;
      } else {
         this.m_bInVehicle = var4.isSeatedInVehicle();
         if (this.m_bInVehicle) {
            this.m_angle = 0.0F;
            BaseVehicle var5 = var4.getVehicle();
            this.m_x = var5.getX();
            this.m_y = var5.getY();
            this.m_z = var5.getZ();
         } else {
            this.m_angle = var4.getAnimationPlayer().getRenderedAngle();
            this.adjustForSittingOnFurniture(var4);
         }

         this.m_bUseWorldIso = true;
      }

      return this;
   }

   private void adjustForSittingOnFurniture(IsoGameCharacter var1) {
      if (var1.isSittingOnFurniture()) {
         IsoObject var2 = var1.getSitOnFurnitureObject();
         if (var2 != null && var2.getSprite() != null && var2.getSprite().tilesetName != null) {
            IsoDirections var3 = var1.getSitOnFurnitureDirection();
            Vector3f var4 = SeatingManager.getInstance().getTranslation(var2.getSprite(), var3.name(), new Vector3f());
            float var5 = var4.x;
            float var6 = var4.y;
            float var7 = var4.z;
            float var8 = 1.0F;
            String var9 = var1.getVariableString("SitOnFurnitureDirection");
            String var10 = "SitOnFurniture" + var9;
            if (var1.isCurrentState(PlayerGetUpState.instance())) {
               String var11 = "";
               if (var1.getVariableBoolean("getUpQuick")) {
                  var11 = "Quick";
               }

               if ("Left".equalsIgnoreCase(var9)) {
                  var11 = var11 + "_L";
               } else if ("Right".equalsIgnoreCase(var9)) {
                  var11 = var11 + "_R";
               }

               var10 = "fromSitOnFurniture" + var11;
               var8 = 0.0F;
            }

            float var14 = SeatingManager.getInstance().getAnimationTrackFraction(var1, var10);
            if (var14 < 0.0F && !var1.getVariableBoolean("SitOnFurnitureStarted")) {
               var8 = 1.0F - var8;
            }

            if (var14 >= 0.0F) {
               float var12;
               float var13;
               if (var1.isCurrentState(PlayerGetUpState.instance())) {
                  var12 = 0.48F;
                  var13 = 0.63F;
                  if (var14 >= var13) {
                     var8 = 1.0F;
                  } else if (var14 >= var12) {
                     var8 = (var14 - var12) / (var13 - var12);
                  } else {
                     var8 = 0.0F;
                  }

                  var8 = 1.0F - var8;
               } else {
                  var12 = 0.27F;
                  var13 = 0.43F;
                  if (var14 >= var12 && var14 <= var13) {
                     var8 = (var14 - var12) / (var13 - var12);
                  } else if (var14 >= var13) {
                     var8 = 1.0F;
                  } else {
                     var8 = 0.0F;
                  }
               }
            }

            this.m_z = PZMath.lerp(this.m_z, (float)var2.square.z + var7 / 2.44949F, var8);
         }
      }
   }

   public ModelCameraRenderData init(ModelCamera var1, float var2, boolean var3, float var4, float var5, float var6, boolean var7) {
      this.m_camera = var1;
      this.m_angle = var2;
      this.m_bUseWorldIso = var3;
      this.m_x = var4;
      this.m_y = var5;
      this.m_z = var6;
      this.m_bInVehicle = var7;
      return this;
   }

   public void render() {
      this.m_camera.m_useAngle = this.m_angle;
      this.m_camera.m_bUseWorldIso = this.m_bUseWorldIso;
      this.m_camera.m_x = this.m_x;
      this.m_camera.m_y = this.m_y;
      this.m_camera.m_z = this.m_z;
      this.m_camera.m_bInVehicle = this.m_bInVehicle;
      ModelCamera.instance = this.m_camera;
   }

   public void postRender() {
      s_pool.release((Object)this);
   }
}
