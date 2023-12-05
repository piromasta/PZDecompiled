package zombie.vehicles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBOLines;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.Mouse;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class UI3DScene extends UIElement {
   private final ArrayList<SceneObject> m_objects = new ArrayList();
   private View m_view;
   private TransformMode m_transformMode;
   private int m_view_x;
   private int m_view_y;
   private final Vector3f m_viewRotation;
   private int m_zoom;
   private int m_zoomMax;
   private int m_gridDivisions;
   private GridPlane m_gridPlane;
   private final Matrix4f m_projection;
   private final Matrix4f m_modelView;
   private long VIEW_CHANGE_TIME;
   private long m_viewChangeTime;
   private final Quaternionf m_modelViewChange;
   private boolean m_bDrawGrid;
   private boolean m_bDrawGridAxes;
   private boolean m_bDrawGridPlane;
   private final CharacterSceneModelCamera m_CharacterSceneModelCamera;
   private final VehicleSceneModelCamera m_VehicleSceneModelCamera;
   private static final ObjectPool<SetModelCamera> s_SetModelCameraPool = new ObjectPool(SetModelCamera::new);
   private final StateData[] m_stateData;
   private Gizmo m_gizmo;
   private final RotateGizmo m_rotateGizmo;
   private final ScaleGizmo m_scaleGizmo;
   private final TranslateGizmo m_translateGizmo;
   private final Vector3f m_gizmoPos;
   private final Vector3f m_gizmoRotate;
   private SceneObject m_gizmoParent;
   private SceneObject m_gizmoOrigin;
   private SceneObject m_gizmoChild;
   private final OriginAttachment m_originAttachment;
   private final OriginBone m_originBone;
   private final OriginGizmo m_originGizmo;
   private float m_gizmoScale;
   private String m_selectedAttachment;
   private final ArrayList<PositionRotation> m_axes;
   private final OriginBone m_highlightBone;
   private static final ObjectPool<PositionRotation> s_posRotPool = new ObjectPool(PositionRotation::new);
   private final ArrayList<AABB> m_aabb;
   private static final ObjectPool<AABB> s_aabbPool = new ObjectPool(AABB::new);
   private final ArrayList<Box3D> m_box3D;
   private static final ObjectPool<Box3D> s_box3DPool = new ObjectPool(Box3D::new);
   final Vector3f tempVector3f;
   final Vector4f tempVector4f;
   final int[] m_viewport;
   private final float GRID_DARK;
   private final float GRID_LIGHT;
   private float GRID_ALPHA;
   private final int HALF_GRID;
   private static final VBOLines vboLines = new VBOLines();
   private static final ThreadLocal<ObjectPool<Ray>> TL_Ray_pool = ThreadLocal.withInitial(RayObjectPool::new);
   private static final ThreadLocal<ObjectPool<Plane>> TL_Plane_pool = ThreadLocal.withInitial(PlaneObjectPool::new);
   static final float SMALL_NUM = 1.0E-8F;

   public UI3DScene(KahluaTable var1) {
      super(var1);
      this.m_view = UI3DScene.View.Right;
      this.m_transformMode = UI3DScene.TransformMode.Local;
      this.m_view_x = 0;
      this.m_view_y = 0;
      this.m_viewRotation = new Vector3f();
      this.m_zoom = 3;
      this.m_zoomMax = 10;
      this.m_gridDivisions = 1;
      this.m_gridPlane = UI3DScene.GridPlane.YZ;
      this.m_projection = new Matrix4f();
      this.m_modelView = new Matrix4f();
      this.VIEW_CHANGE_TIME = 350L;
      this.m_modelViewChange = new Quaternionf();
      this.m_bDrawGrid = true;
      this.m_bDrawGridAxes = false;
      this.m_bDrawGridPlane = false;
      this.m_CharacterSceneModelCamera = new CharacterSceneModelCamera();
      this.m_VehicleSceneModelCamera = new VehicleSceneModelCamera();
      this.m_stateData = new StateData[3];
      this.m_rotateGizmo = new RotateGizmo();
      this.m_scaleGizmo = new ScaleGizmo();
      this.m_translateGizmo = new TranslateGizmo();
      this.m_gizmoPos = new Vector3f();
      this.m_gizmoRotate = new Vector3f();
      this.m_gizmoParent = null;
      this.m_gizmoOrigin = null;
      this.m_gizmoChild = null;
      this.m_originAttachment = new OriginAttachment(this);
      this.m_originBone = new OriginBone(this);
      this.m_originGizmo = new OriginGizmo(this);
      this.m_gizmoScale = 1.0F;
      this.m_selectedAttachment = null;
      this.m_axes = new ArrayList();
      this.m_highlightBone = new OriginBone(this);
      this.m_aabb = new ArrayList();
      this.m_box3D = new ArrayList();
      this.tempVector3f = new Vector3f();
      this.tempVector4f = new Vector4f();
      this.m_viewport = new int[]{0, 0, 0, 0};
      this.GRID_DARK = 0.1F;
      this.GRID_LIGHT = 0.2F;
      this.GRID_ALPHA = 1.0F;
      this.HALF_GRID = 5;

      for(int var2 = 0; var2 < this.m_stateData.length; ++var2) {
         this.m_stateData[var2] = new StateData();
         this.m_stateData[var2].m_gridPlaneDrawer = new GridPlaneDrawer(this);
         this.m_stateData[var2].m_overlaysDrawer = new OverlaysDrawer();
      }

   }

   SceneObject getSceneObjectById(String var1, boolean var2) {
      for(int var3 = 0; var3 < this.m_objects.size(); ++var3) {
         SceneObject var4 = (SceneObject)this.m_objects.get(var3);
         if (var4.m_id.equalsIgnoreCase(var1)) {
            return var4;
         }
      }

      if (var2) {
         throw new NullPointerException("scene object \"" + var1 + "\" not found");
      } else {
         return null;
      }
   }

   <C> C getSceneObjectById(String var1, Class<C> var2, boolean var3) {
      for(int var4 = 0; var4 < this.m_objects.size(); ++var4) {
         SceneObject var5 = (SceneObject)this.m_objects.get(var4);
         if (var5.m_id.equalsIgnoreCase(var1)) {
            if (var5.getClass() == var2) {
               return var2.cast(var5);
            }

            if (var3) {
               throw new ClassCastException("scene object \"" + var1 + "\" is " + var5.getClass().getSimpleName() + " expected " + var2.getSimpleName());
            }
         }
      }

      if (var3) {
         throw new NullPointerException("scene object \"" + var1 + "\" not found");
      } else {
         return null;
      }
   }

   public void render() {
      if (this.isVisible()) {
         super.render();
         IndieGL.glClear(256);
         StateData var1 = this.stateDataMain();
         this.calcMatrices(this.m_projection, this.m_modelView);
         var1.m_projection.set(this.m_projection);
         long var2 = System.currentTimeMillis();
         float var4;
         if (this.m_viewChangeTime + this.VIEW_CHANGE_TIME > var2) {
            var4 = (float)(this.m_viewChangeTime + this.VIEW_CHANGE_TIME - var2) / (float)this.VIEW_CHANGE_TIME;
            Quaternionf var5 = allocQuaternionf().setFromUnnormalized(this.m_modelView);
            var1.m_modelView.set(this.m_modelViewChange.slerp(var5, 1.0F - var4));
            releaseQuaternionf(var5);
         } else {
            var1.m_modelView.set(this.m_modelView);
         }

         var1.m_zoom = this.m_zoom;
         if (this.m_bDrawGridPlane) {
            SpriteRenderer.instance.drawGeneric(var1.m_gridPlaneDrawer);
         }

         PZArrayUtil.forEach((List)var1.m_objectData, SceneObjectRenderData::release);
         var1.m_objectData.clear();

         for(int var11 = 0; var11 < this.m_objects.size(); ++var11) {
            SceneObject var12 = (SceneObject)this.m_objects.get(var11);
            if (var12.m_visible) {
               if (var12.m_autoRotate) {
                  var12.m_autoRotateAngle = (float)((double)var12.m_autoRotateAngle + UIManager.getMillisSinceLastRender() / 30.0);
                  if (var12.m_autoRotateAngle > 360.0F) {
                     var12.m_autoRotateAngle = 0.0F;
                  }
               }

               SceneObjectRenderData var6 = var12.renderMain();
               if (var6 != null) {
                  var1.m_objectData.add(var6);
               }
            }
         }

         var4 = (float)(Mouse.getXA() - this.getAbsoluteX().intValue());
         float var13 = (float)(Mouse.getYA() - this.getAbsoluteY().intValue());
         var1.m_gizmo = this.m_gizmo;
         if (this.m_gizmo != null) {
            var1.m_gizmoTranslate.set(this.m_gizmoPos);
            var1.m_gizmoRotate.set(this.m_gizmoRotate);
            var1.m_gizmoTransform.translation(this.m_gizmoPos);
            var1.m_gizmoTransform.rotateXYZ(this.m_gizmoRotate.x * 0.017453292F, this.m_gizmoRotate.y * 0.017453292F, this.m_gizmoRotate.z * 0.017453292F);
            var1.m_gizmoAxis = this.m_gizmo.hitTest(var4, var13);
         }

         var1.m_gizmoChildTransform.identity();
         var1.m_selectedAttachmentIsChildAttachment = this.m_gizmoChild != null && this.m_gizmoChild.m_attachment != null && this.m_gizmoChild.m_attachment.equals(this.m_selectedAttachment);
         if (this.m_gizmoChild != null) {
            this.m_gizmoChild.getLocalTransform(var1.m_gizmoChildTransform);
         }

         var1.m_gizmoOriginTransform.identity();
         var1.m_hasGizmoOrigin = this.m_gizmoOrigin != null;
         if (this.m_gizmoOrigin != null && this.m_gizmoOrigin != this.m_gizmoParent) {
            this.m_gizmoOrigin.getGlobalTransform(var1.m_gizmoOriginTransform);
         }

         var1.m_gizmoParentTransform.identity();
         if (this.m_gizmoParent != null) {
            this.m_gizmoParent.getGlobalTransform(var1.m_gizmoParentTransform);
         }

         var1.m_overlaysDrawer.init();
         SpriteRenderer.instance.drawGeneric(var1.m_overlaysDrawer);
         Vector3f var7;
         Vector3f var14;
         if (this.m_bDrawGrid) {
            var14 = this.uiToScene(var4, var13, 0.0F, this.tempVector3f);
            if (this.m_view == UI3DScene.View.UserDefined) {
               var7 = allocVector3f();
               switch (this.m_gridPlane) {
                  case XY:
                     var7.set(0.0F, 0.0F, 1.0F);
                     break;
                  case XZ:
                     var7.set(0.0F, 1.0F, 0.0F);
                     break;
                  case YZ:
                     var7.set(1.0F, 0.0F, 0.0F);
               }

               Vector3f var8 = allocVector3f().set(0.0F);
               Plane var9 = allocPlane().set(var7, var8);
               releaseVector3f(var7);
               releaseVector3f(var8);
               Ray var10 = this.getCameraRay(var4, (float)this.screenHeight() - var13, allocRay());
               if (intersect_ray_plane(var9, var10, var14) != 1) {
                  var14.set(0.0F);
               }

               releasePlane(var9);
               releaseRay(var10);
            }

            var14.x = (float)Math.round(var14.x * this.gridMult()) / this.gridMult();
            var14.y = (float)Math.round(var14.y * this.gridMult()) / this.gridMult();
            var14.z = (float)Math.round(var14.z * this.gridMult()) / this.gridMult();
            this.DrawText(UIFont.Small, String.valueOf(var14.x), (double)(this.width - 200.0F), 10.0, 1.0, 0.0, 0.0, 1.0);
            this.DrawText(UIFont.Small, String.valueOf(var14.y), (double)(this.width - 150.0F), 10.0, 0.0, 1.0, 0.0, 1.0);
            this.DrawText(UIFont.Small, String.valueOf(var14.z), (double)(this.width - 100.0F), 10.0, 0.0, 0.5, 1.0, 1.0);
         }

         float var17;
         if (this.m_gizmo == this.m_rotateGizmo && this.m_rotateGizmo.m_trackAxis != UI3DScene.Axis.None) {
            var14 = this.m_rotateGizmo.m_startXfrm.getTranslation(allocVector3f());
            float var16 = this.sceneToUIX(var14.x, var14.y, var14.z);
            var17 = this.sceneToUIY(var14.x, var14.y, var14.z);
            LineDrawer.drawLine(var16, var17, var4, var13, 0.5F, 0.5F, 0.5F, 1.0F, 1);
            releaseVector3f(var14);
         }

         if (this.m_highlightBone.m_boneName != null) {
            Matrix4f var15 = this.m_highlightBone.getGlobalTransform(allocMatrix4f());
            this.m_highlightBone.m_character.getGlobalTransform(allocMatrix4f()).mul(var15, var15);
            var7 = var15.getTranslation(allocVector3f());
            var17 = this.sceneToUIX(var7.x, var7.y, var7.z);
            float var18 = this.sceneToUIY(var7.x, var7.y, var7.z);
            LineDrawer.drawCircle(var17, var18, 10.0F, 16, 1.0F, 1.0F, 1.0F);
            releaseVector3f(var7);
            releaseMatrix4f(var15);
         }

      }
   }

   private float gridMult() {
      return (float)(100 * this.m_gridDivisions);
   }

   private float zoomMult() {
      return (float)Math.exp((double)((float)this.m_zoom * 0.2F)) * 160.0F / Math.max(1.82F, 1.0F);
   }

   private static Matrix4f allocMatrix4f() {
      return (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();
   }

   private static void releaseMatrix4f(Matrix4f var0) {
      ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var0);
   }

   private static Quaternionf allocQuaternionf() {
      return (Quaternionf)((BaseVehicle.QuaternionfObjectPool)BaseVehicle.TL_quaternionf_pool.get()).alloc();
   }

   private static void releaseQuaternionf(Quaternionf var0) {
      ((BaseVehicle.QuaternionfObjectPool)BaseVehicle.TL_quaternionf_pool.get()).release(var0);
   }

   private static Ray allocRay() {
      return (Ray)((ObjectPool)TL_Ray_pool.get()).alloc();
   }

   private static void releaseRay(Ray var0) {
      ((ObjectPool)TL_Ray_pool.get()).release((Object)var0);
   }

   private static Plane allocPlane() {
      return (Plane)((ObjectPool)TL_Plane_pool.get()).alloc();
   }

   private static void releasePlane(Plane var0) {
      ((ObjectPool)TL_Plane_pool.get()).release((Object)var0);
   }

   private static Vector2 allocVector2() {
      return (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
   }

   private static void releaseVector2(Vector2 var0) {
      ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var0);
   }

   private static Vector3f allocVector3f() {
      return (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();
   }

   private static void releaseVector3f(Vector3f var0) {
      ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var0);
   }

   public Object fromLua0(String var1) {
      switch (var1) {
         case "clearAABBs":
            s_aabbPool.release((List)this.m_aabb);
            this.m_aabb.clear();
            return null;
         case "clearAxes":
            s_posRotPool.release((List)this.m_axes);
            this.m_axes.clear();
            return null;
         case "clearBox3Ds":
            s_box3DPool.release((List)this.m_box3D);
            this.m_box3D.clear();
            return null;
         case "clearGizmoRotate":
            this.m_gizmoRotate.set(0.0F);
            return null;
         case "clearHighlightBone":
            this.m_highlightBone.m_boneName = null;
            return null;
         case "getGizmoPos":
            return this.m_gizmoPos;
         case "getGridMult":
            return BoxedStaticValues.toDouble((double)this.gridMult());
         case "getView":
            return this.m_view.name();
         case "getViewRotation":
            return this.m_viewRotation;
         case "getModelCount":
            int var4 = 0;

            for(int var5 = 0; var5 < this.m_objects.size(); ++var5) {
               if (this.m_objects.get(var5) instanceof SceneModel) {
                  ++var4;
               }
            }

            return BoxedStaticValues.toDouble((double)var4);
         case "stopGizmoTracking":
            if (this.m_gizmo != null) {
               this.m_gizmo.stopTracking();
            }

            return null;
         default:
            throw new IllegalArgumentException("unhandled \"" + var1 + "\"");
      }
   }

   public Object fromLua1(String var1, Object var2) {
      int var5;
      Vector3f var14;
      SceneObject var19;
      AnimationPlayer var21;
      SceneCharacter var22;
      AnimationMultiTrack var28;
      switch (var1) {
         case "createCharacter":
            var19 = this.getSceneObjectById((String)var2, false);
            if (var19 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            SceneCharacter var25 = new SceneCharacter(this, (String)var2);
            this.m_objects.add(var25);
            return var25;
         case "createVehicle":
            var19 = this.getSceneObjectById((String)var2, false);
            if (var19 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            SceneVehicle var24 = new SceneVehicle(this, (String)var2);
            this.m_objects.add(var24);
            return null;
         case "getCharacterAnimationDuration":
            var22 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21 = var22.m_animatedModel.getAnimationPlayer();
            if (var21 == null) {
               return null;
            } else {
               var28 = var21.getMultiTrack();
               if (var28 != null && !var28.getTracks().isEmpty()) {
                  return KahluaUtil.toDouble((double)((AnimationTrack)var28.getTracks().get(0)).getDuration());
               }

               return null;
            }
         case "getCharacterAnimationTime":
            var22 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21 = var22.m_animatedModel.getAnimationPlayer();
            if (var21 == null) {
               return null;
            } else {
               var28 = var21.getMultiTrack();
               if (var28 != null && !var28.getTracks().isEmpty()) {
                  return KahluaUtil.toDouble((double)((AnimationTrack)var28.getTracks().get(0)).getCurrentTimeValue());
               }

               return null;
            }
         case "getModelScript":
            var5 = 0;

            for(int var17 = 0; var17 < this.m_objects.size(); ++var17) {
               SceneModel var27 = (SceneModel)Type.tryCastTo((SceneObject)this.m_objects.get(var17), SceneModel.class);
               if (var27 != null && var5++ == ((Double)var2).intValue()) {
                  return var27.m_modelScript;
               }
            }

            return null;
         case "getObjectAutoRotate":
            var19 = this.getSceneObjectById((String)var2, true);
            return var19.m_autoRotate ? Boolean.TRUE : Boolean.FALSE;
         case "getObjectParent":
            var19 = this.getSceneObjectById((String)var2, true);
            return var19.m_parent == null ? null : var19.m_parent.m_id;
         case "getObjectParentAttachment":
            var19 = this.getSceneObjectById((String)var2, true);
            return var19.m_parentAttachment;
         case "getObjectRotation":
            var19 = this.getSceneObjectById((String)var2, true);
            return var19.m_rotate;
         case "getObjectTranslation":
            var19 = this.getSceneObjectById((String)var2, true);
            return var19.m_translate;
         case "getVehicleScript":
            SceneVehicle var23 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            return var23.m_script;
         case "isCharacterFemale":
            var22 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            return var22.m_animatedModel.isFemale();
         case "isObjectVisible":
            var19 = this.getSceneObjectById((String)var2, true);
            return var19.m_visible ? Boolean.TRUE : Boolean.FALSE;
         case "removeModel":
            SceneModel var18 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            this.m_objects.remove(var18);
            Iterator var16 = this.m_objects.iterator();

            while(var16.hasNext()) {
               SceneObject var26 = (SceneObject)var16.next();
               if (var26.m_parent == var18) {
                  var26.m_attachment = null;
                  var26.m_parent = null;
                  var26.m_parentAttachment = null;
               }
            }

            return null;
         case "setDrawGrid":
            this.m_bDrawGrid = (Boolean)var2;
            return null;
         case "setDrawGridAxes":
            this.m_bDrawGridAxes = (Boolean)var2;
            return null;
         case "setDrawGridPlane":
            this.m_bDrawGridPlane = (Boolean)var2;
            return null;
         case "setGizmoOrigin":
            switch ((String)var2) {
               case "none":
                  this.m_gizmoParent = null;
                  this.m_gizmoOrigin = null;
                  this.m_gizmoChild = null;
               default:
                  return null;
            }
         case "setGizmoPos":
            var14 = (Vector3f)var2;
            if (!this.m_gizmoPos.equals(var14)) {
               this.m_gizmoPos.set(var14);
            }

            return null;
         case "setGizmoRotate":
            var14 = (Vector3f)var2;
            if (!this.m_gizmoRotate.equals(var14)) {
               this.m_gizmoRotate.set(var14);
            }

            return null;
         case "setGizmoScale":
            this.m_gizmoScale = Math.max(((Double)var2).floatValue(), 0.01F);
            return null;
         case "setGizmoVisible":
            var13 = (String)var2;
            this.m_rotateGizmo.m_visible = "rotate".equalsIgnoreCase(var13);
            this.m_scaleGizmo.m_visible = "scale".equalsIgnoreCase(var13);
            this.m_translateGizmo.m_visible = "translate".equalsIgnoreCase(var13);
            switch (var13) {
               case "rotate":
                  this.m_gizmo = this.m_rotateGizmo;
                  break;
               case "scale":
                  this.m_gizmo = this.m_scaleGizmo;
                  break;
               case "translate":
                  this.m_gizmo = this.m_translateGizmo;
                  break;
               default:
                  this.m_gizmo = null;
            }

            return null;
         case "setGridMult":
            this.m_gridDivisions = PZMath.clamp(((Double)var2).intValue(), 1, 100);
            return null;
         case "setGridPlane":
            this.m_gridPlane = UI3DScene.GridPlane.valueOf((String)var2);
            return null;
         case "setMaxZoom":
            this.m_zoomMax = PZMath.clamp(((Double)var2).intValue(), 1, 20);
            return null;
         case "setSelectedAttachment":
            this.m_selectedAttachment = (String)var2;
            return null;
         case "setTransformMode":
            this.m_transformMode = UI3DScene.TransformMode.valueOf((String)var2);
            return null;
         case "setZoom":
            this.m_zoom = PZMath.clamp(((Double)var2).intValue(), 1, this.m_zoomMax);
            this.calcMatrices(this.m_projection, this.m_modelView);
            return null;
         case "setView":
            View var12 = this.m_view;
            this.m_view = UI3DScene.View.valueOf((String)var2);
            if (var12 != this.m_view) {
               long var15 = System.currentTimeMillis();
               if (this.m_viewChangeTime + this.VIEW_CHANGE_TIME < var15) {
                  this.m_modelViewChange.setFromUnnormalized(this.m_modelView);
               }

               this.m_viewChangeTime = var15;
            }

            this.calcMatrices(this.m_projection, this.m_modelView);
            return null;
         case "zoom":
            var5 = -((Double)var2).intValue();
            float var6 = (float)(Mouse.getXA() - this.getAbsoluteX().intValue());
            float var7 = (float)(Mouse.getYA() - this.getAbsoluteY().intValue());
            float var8 = this.uiToSceneX(var6, var7);
            float var9 = this.uiToSceneY(var6, var7);
            this.m_zoom = PZMath.clamp(this.m_zoom + var5, 1, this.m_zoomMax);
            this.calcMatrices(this.m_projection, this.m_modelView);
            float var10 = this.uiToSceneX(var6, var7);
            float var11 = this.uiToSceneY(var6, var7);
            this.m_view_x = (int)((float)this.m_view_x - (var10 - var8) * this.zoomMult());
            this.m_view_y = (int)((float)this.m_view_y + (var11 - var9) * this.zoomMult());
            this.calcMatrices(this.m_projection, this.m_modelView);
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", var1, var2));
      }
   }

   public Object fromLua2(String var1, Object var2, Object var3) {
      int var6;
      int var7;
      SceneObject var17;
      String var18;
      SceneModel var19;
      SceneCharacter var21;
      AnimationPlayer var22;
      ModelAttachment var26;
      SceneModel var27;
      AnimationMultiTrack var33;
      switch (var1) {
         case "addAttachment":
            var19 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            if (var19.m_modelScript.getAttachmentById((String)var3) != null) {
               throw new IllegalArgumentException("model script \"" + var2 + "\" already has attachment named \"" + var3 + "\"");
            }

            var26 = new ModelAttachment((String)var3);
            var19.m_modelScript.addAttachment(var26);
            return var26;
         case "addBoneAxis":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var18 = (String)var3;
            PositionRotation var41 = var21.getBoneAxis(var18, (PositionRotation)s_posRotPool.alloc());
            this.m_axes.add(var41);
            return null;
         case "applyDeltaRotation":
            Vector3f var43 = (Vector3f)var2;
            Vector3f var35 = (Vector3f)var3;
            Quaternionf var40 = allocQuaternionf().rotationXYZ(var43.x * 0.017453292F, var43.y * 0.017453292F, var43.z * 0.017453292F);
            Quaternionf var38 = allocQuaternionf().rotationXYZ(var35.x * 0.017453292F, var35.y * 0.017453292F, var35.z * 0.017453292F);
            var40.mul(var38);
            var40.getEulerAnglesXYZ(var43);
            releaseQuaternionf(var40);
            releaseQuaternionf(var38);
            var43.mul(57.295776F);
            var43.x = (float)Math.floor((double)(var43.x + 0.5F));
            var43.y = (float)Math.floor((double)(var43.y + 0.5F));
            var43.z = (float)Math.floor((double)(var43.z + 0.5F));
            return var43;
         case "createModel":
            var17 = this.getSceneObjectById((String)var2, false);
            if (var17 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            } else {
               ModelScript var32 = ScriptManager.instance.getModelScript((String)var3);
               if (var32 == null) {
                  throw new NullPointerException("model script \"" + var3 + "\" not found");
               } else {
                  Model var39 = ModelManager.instance.getLoadedModel((String)var3);
                  if (var39 == null) {
                     throw new NullPointerException("model \"" + var3 + "\" not found");
                  }

                  var27 = new SceneModel(this, (String)var2, var32, var39);
                  this.m_objects.add(var27);
                  return null;
               }
            }
         case "dragGizmo":
            float var42 = ((Double)var2).floatValue();
            float var29 = ((Double)var3).floatValue();
            if (this.m_gizmo == null) {
               throw new NullPointerException("gizmo is null");
            }

            this.m_gizmo.updateTracking(var42, var29);
            return null;
         case "dragView":
            var6 = ((Double)var2).intValue();
            var7 = ((Double)var3).intValue();
            this.m_view_x -= var6;
            this.m_view_y -= var7;
            this.calcMatrices(this.m_projection, this.m_modelView);
            return null;
         case "getCharacterAnimationKeyframeTimes":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var22 = var21.m_animatedModel.getAnimationPlayer();
            if (var22 == null) {
               return null;
            } else {
               var33 = var22.getMultiTrack();
               if (var33 != null && !var33.getTracks().isEmpty()) {
                  AnimationTrack var37 = (AnimationTrack)var33.getTracks().get(0);
                  AnimationClip var10 = var37.getClip();
                  if (var10 == null) {
                     return null;
                  }

                  if (var3 == null) {
                     var3 = new ArrayList();
                  }

                  ArrayList var11 = (ArrayList)var3;
                  var11.clear();
                  Keyframe[] var12 = var10.getKeyframes();

                  for(int var13 = 0; var13 < var12.length; ++var13) {
                     Keyframe var14 = var12[var13];
                     Double var15 = KahluaUtil.toDouble((double)var14.Time);
                     if (!var11.contains(var15)) {
                        var11.add(var15);
                     }
                  }

                  return var11;
               }

               return null;
            }
         case "removeAttachment":
            var19 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var26 = var19.m_modelScript.getAttachmentById((String)var3);
            if (var26 == null) {
               throw new IllegalArgumentException("model script \"" + var2 + "\" attachment \"" + var3 + "\" not found");
            }

            var19.m_modelScript.removeAttachment(var26);
            return null;
         case "setCharacterAlpha":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_animatedModel.setAlpha(((Double)var3).floatValue());
            return null;
         case "setCharacterAnimate":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_animatedModel.setAnimate((Boolean)var3);
            return null;
         case "setCharacterAnimationClip":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            AnimationSet var25 = AnimationSet.GetAnimationSet(var21.m_animatedModel.GetAnimSetName(), false);
            if (var25 == null) {
               return null;
            } else {
               AnimState var36 = var25.GetState(var21.m_animatedModel.getState());
               if (var36 != null && !var36.m_Nodes.isEmpty()) {
                  AnimNode var34 = (AnimNode)var36.m_Nodes.get(0);
                  var34.m_AnimName = (String)var3;
                  var21.m_animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                  var21.m_animatedModel.getAdvancedAnimator().SetState(var36.m_Name);
                  return null;
               }

               return null;
            }
         case "setCharacterAnimationSpeed":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            AnimationMultiTrack var23 = var21.m_animatedModel.getAnimationPlayer().getMultiTrack();
            if (var23.getTracks().isEmpty()) {
               return null;
            }

            ((AnimationTrack)var23.getTracks().get(0)).SpeedDelta = PZMath.clamp(((Double)var3).floatValue(), 0.0F, 10.0F);
            return null;
         case "setCharacterAnimationTime":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_animatedModel.setTrackTime(((Double)var3).floatValue());
            var22 = var21.m_animatedModel.getAnimationPlayer();
            if (var22 == null) {
               return null;
            } else {
               var33 = var22.getMultiTrack();
               if (var33 != null && !var33.getTracks().isEmpty()) {
                  ((AnimationTrack)var33.getTracks().get(0)).setCurrentTimeValue(((Double)var3).floatValue());
                  return null;
               }

               return null;
            }
         case "setCharacterAnimSet":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var18 = (String)var3;
            if (!var18.equals(var21.m_animatedModel.GetAnimSetName())) {
               var21.m_animatedModel.setAnimSetName(var18);
               var21.m_animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
               ActionGroup var30 = ActionGroup.getActionGroup(var21.m_animatedModel.GetAnimSetName());
               ActionContext var31 = var21.m_animatedModel.getActionContext();
               if (var30 != var31.getGroup()) {
                  var31.setGroup(var30);
               }

               var21.m_animatedModel.getAdvancedAnimator().SetState(var31.getCurrentStateName(), PZArrayUtil.listConvert(var31.getChildStates(), (var0) -> {
                  return var0.name;
               }));
            }

            return null;
         case "setCharacterClearDepthBuffer":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_bClearDepthBuffer = (Boolean)var3;
            return null;
         case "setCharacterFemale":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            boolean var20 = (Boolean)var3;
            if (var20 != var21.m_animatedModel.isFemale()) {
               var21.m_animatedModel.setOutfitName("Naked", var20, false);
            }

            return null;
         case "setCharacterShowBones":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_bShowBones = (Boolean)var3;
            return null;
         case "setCharacterUseDeferredMovement":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_bUseDeferredMovement = (Boolean)var3;
            return null;
         case "setGizmoOrigin":
            SceneVehicle var9;
            switch ((String)var2) {
               case "centerOfMass":
                  this.m_gizmoParent = (SceneObject)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "chassis":
                  var9 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoParent = var9;
                  this.m_originGizmo.m_translate.set(var9.m_script.getCenterOfMassOffset());
                  this.m_originGizmo.m_rotate.zero();
                  this.m_gizmoOrigin = this.m_originGizmo;
                  this.m_gizmoChild = null;
                  break;
               case "character":
                  SceneCharacter var28 = (SceneCharacter)this.getSceneObjectById((String)var3, SceneCharacter.class, true);
                  this.m_gizmoParent = var28;
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "model":
                  var27 = (SceneModel)this.getSceneObjectById((String)var3, SceneModel.class, true);
                  this.m_gizmoParent = var27;
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "vehicleModel":
                  var9 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoParent = var9;
                  this.m_originGizmo.m_translate.set(var9.m_script.getModel().getOffset());
                  this.m_originGizmo.m_rotate.zero();
                  this.m_gizmoOrigin = this.m_originGizmo;
                  this.m_gizmoChild = null;
            }

            return null;
         case "setCharacterState":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var21.m_animatedModel.setState((String)var3);
            return null;
         case "setHighlightBone":
            var21 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var18 = (String)var3;
            this.m_highlightBone.m_character = var21;
            this.m_highlightBone.m_boneName = var18;
            return null;
         case "setModelUseWorldAttachment":
            var19 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var19.m_useWorldAttachment = (Boolean)var3;
            return null;
         case "setModelWeaponRotationHack":
            var19 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var19.m_weaponRotationHack = (Boolean)var3;
            return null;
         case "setObjectAutoRotate":
            var17 = this.getSceneObjectById((String)var2, true);
            var17.m_autoRotate = (Boolean)var3;
            if (!var17.m_autoRotate) {
               var17.m_autoRotateAngle = 0.0F;
            }

            return null;
         case "setObjectVisible":
            var17 = this.getSceneObjectById((String)var2, true);
            var17.m_visible = (Boolean)var3;
            return null;
         case "setVehicleScript":
            SceneVehicle var16 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            var16.setScriptName((String)var3);
            return null;
         case "testGizmoAxis":
            var6 = ((Double)var2).intValue();
            var7 = ((Double)var3).intValue();
            if (this.m_gizmo == null) {
               return "None";
            }

            return this.m_gizmo.hitTest((float)var6, (float)var7).toString();
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\"", var1, var2, var3));
      }
   }

   public Object fromLua3(String var1, Object var2, Object var3, Object var4) {
      float var7;
      float var8;
      float var9;
      switch (var1) {
         case "addAxis":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            this.m_axes.add(((PositionRotation)s_posRotPool.alloc()).set(var7, var8, var9));
            return null;
         case "pickCharacterBone":
            SceneCharacter var12 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            return var12.pickBone(var8, var9);
         case "setGizmoOrigin":
            switch ((String)var2) {
               case "bone":
                  SceneCharacter var10 = (SceneCharacter)this.getSceneObjectById((String)var3, SceneCharacter.class, true);
                  this.m_gizmoParent = var10;
                  this.m_originBone.m_character = var10;
                  this.m_originBone.m_boneName = (String)var4;
                  this.m_gizmoOrigin = this.m_originBone;
                  this.m_gizmoChild = null;
               default:
                  return null;
            }
         case "setGizmoXYZ":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            this.m_gizmoPos.set(var7, var8, var9);
            return null;
         case "startGizmoTracking":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            Axis var13 = UI3DScene.Axis.valueOf((String)var4);
            if (this.m_gizmo != null) {
               this.m_gizmo.startTracking(var7, var8, var13);
            }

            return null;
         case "setViewRotation":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            var7 %= 360.0F;
            var8 %= 360.0F;
            var9 %= 360.0F;
            this.m_viewRotation.set(var7, var8, var9);
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4));
      }
   }

   public Object fromLua4(String var1, Object var2, Object var3, Object var4, Object var5) {
      SceneObject var15;
      switch (var1) {
         case "setGizmoOrigin":
            switch ((String)var2) {
               case "attachment":
                  SceneObject var18 = this.getSceneObjectById((String)var3, true);
                  this.m_gizmoParent = this.getSceneObjectById((String)var4, true);
                  this.m_originAttachment.m_object = this.m_gizmoParent;
                  this.m_originAttachment.m_attachmentName = (String)var5;
                  this.m_gizmoOrigin = this.m_originAttachment;
                  this.m_gizmoChild = var18;
               default:
                  return null;
            }
         case "setObjectParent":
            var15 = this.getSceneObjectById((String)var2, true);
            var15.m_translate.zero();
            var15.m_rotate.zero();
            var15.m_attachment = (String)var3;
            var15.m_parent = this.getSceneObjectById((String)var4, false);
            var15.m_parentAttachment = (String)var5;
            if (var15.m_parent != null && var15.m_parent.m_parent == var15) {
               var15.m_parent.m_parent = null;
            }

            return null;
         case "setObjectPosition":
            var15 = this.getSceneObjectById((String)var2, true);
            var15.m_translate.set(((Double)var3).floatValue(), ((Double)var4).floatValue(), ((Double)var5).floatValue());
            return null;
         case "setPassengerPosition":
            SceneCharacter var8 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            SceneVehicle var9 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
            VehicleScript.Passenger var10 = var9.m_script.getPassengerById((String)var4);
            if (var10 == null) {
               return null;
            }

            VehicleScript.Position var11 = var10.getPositionById((String)var5);
            if (var11 != null) {
               this.tempVector3f.set(var9.m_script.getModel().getOffset());
               this.tempVector3f.add(var11.getOffset());
               Vector3f var10000 = this.tempVector3f;
               var10000.z *= -1.0F;
               var8.m_translate.set(this.tempVector3f);
               var8.m_rotate.set(var11.rotate);
               var8.m_parent = var9;
               if (var8.m_animatedModel != null) {
                  String var12 = "inside".equalsIgnoreCase(var11.getId()) ? "player-vehicle" : "player-editor";
                  if (!var12.equals(var8.m_animatedModel.GetAnimSetName())) {
                     var8.m_animatedModel.setAnimSetName(var12);
                     var8.m_animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                     ActionGroup var13 = ActionGroup.getActionGroup(var8.m_animatedModel.GetAnimSetName());
                     ActionContext var14 = var8.m_animatedModel.getActionContext();
                     if (var13 != var14.getGroup()) {
                        var14.setGroup(var13);
                     }

                     var8.m_animatedModel.getAdvancedAnimator().SetState(var14.getCurrentStateName(), PZArrayUtil.listConvert(var14.getChildStates(), (var0) -> {
                        return var0.name;
                     }));
                  }
               }
            }

            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4));
      }
   }

   public Object fromLua6(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7) {
      float var13;
      float var14;
      float var15;
      float var16;
      float var17;
      float var18;
      switch (var1) {
         case "addAABB":
            var16 = ((Double)var2).floatValue();
            var17 = ((Double)var3).floatValue();
            var18 = ((Double)var4).floatValue();
            var13 = ((Double)var5).floatValue();
            var14 = ((Double)var6).floatValue();
            var15 = ((Double)var7).floatValue();
            this.m_aabb.add(((AABB)s_aabbPool.alloc()).set(var16, var17, var18, var13, var14, var15, 1.0F, 1.0F, 1.0F));
            return null;
         case "addAxis":
            var16 = ((Double)var2).floatValue();
            var17 = ((Double)var3).floatValue();
            var18 = ((Double)var4).floatValue();
            var13 = ((Double)var5).floatValue();
            var14 = ((Double)var6).floatValue();
            var15 = ((Double)var7).floatValue();
            this.m_axes.add(((PositionRotation)s_posRotPool.alloc()).set(var16, var17, var18, var13, var14, var15));
            return null;
         case "addBox3D":
            Vector3f var10 = (Vector3f)var2;
            Vector3f var11 = (Vector3f)var3;
            Vector3f var12 = (Vector3f)var4;
            var13 = ((Double)var5).floatValue();
            var14 = ((Double)var6).floatValue();
            var15 = ((Double)var7).floatValue();
            this.m_box3D.add(((Box3D)s_box3DPool.alloc()).set(var10.x, var10.y, var10.z, var11.x, var11.y, var11.z, var12.x, var12.y, var12.z, var13, var14, var15));
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5, var6, var7));
      }
   }

   public Object fromLua9(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7, Object var8, Object var9, Object var10) {
      switch (var1) {
         case "addAABB":
            float var13 = ((Double)var2).floatValue();
            float var14 = ((Double)var3).floatValue();
            float var15 = ((Double)var4).floatValue();
            float var16 = ((Double)var5).floatValue();
            float var17 = ((Double)var6).floatValue();
            float var18 = ((Double)var7).floatValue();
            float var19 = ((Double)var8).floatValue();
            float var20 = ((Double)var9).floatValue();
            float var21 = ((Double)var10).floatValue();
            this.m_aabb.add(((AABB)s_aabbPool.alloc()).set(var13, var14, var15, var16, var17, var18, var19, var20, var21));
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5, var6, var7, var8, var9, var10));
      }
   }

   private int screenWidth() {
      return (int)this.width;
   }

   private int screenHeight() {
      return (int)this.height;
   }

   public float uiToSceneX(float var1, float var2) {
      float var3 = var1 - (float)this.screenWidth() / 2.0F;
      var3 += (float)this.m_view_x;
      var3 /= this.zoomMult();
      return var3;
   }

   public float uiToSceneY(float var1, float var2) {
      float var3 = var2 - (float)this.screenHeight() / 2.0F;
      var3 *= -1.0F;
      var3 -= (float)this.m_view_y;
      var3 /= this.zoomMult();
      return var3;
   }

   public Vector3f uiToScene(float var1, float var2, float var3, Vector3f var4) {
      this.uiToScene((Matrix4f)null, var1, var2, var3, var4);
      switch (this.m_view) {
         case Left:
         case Right:
            var4.x = 0.0F;
            break;
         case Top:
         case Bottom:
            var4.y = 0.0F;
            break;
         case Front:
         case Back:
            var4.z = 0.0F;
      }

      return var4;
   }

   public Vector3f uiToScene(Matrix4f var1, float var2, float var3, float var4, Vector3f var5) {
      var3 = (float)this.screenHeight() - var3;
      Matrix4f var6 = allocMatrix4f();
      var6.set(this.m_projection);
      var6.mul(this.m_modelView);
      if (var1 != null) {
         var6.mul(var1);
      }

      var6.invert();
      this.m_viewport[2] = this.screenWidth();
      this.m_viewport[3] = this.screenHeight();
      var6.unprojectInv(var2, var3, var4, this.m_viewport, var5);
      releaseMatrix4f(var6);
      return var5;
   }

   public float sceneToUIX(float var1, float var2, float var3) {
      this.tempVector4f.set(var1, var2, var3, 1.0F);
      Matrix4f var4 = allocMatrix4f();
      var4.set(this.m_projection);
      var4.mul(this.m_modelView);
      this.m_viewport[2] = this.screenWidth();
      this.m_viewport[3] = this.screenHeight();
      var4.project(var1, var2, var3, this.m_viewport, this.tempVector3f);
      releaseMatrix4f(var4);
      return this.tempVector3f.x();
   }

   public float sceneToUIY(float var1, float var2, float var3) {
      this.tempVector4f.set(var1, var2, var3, 1.0F);
      Matrix4f var4 = allocMatrix4f();
      var4.set(this.m_projection);
      var4.mul(this.m_modelView);
      int[] var5 = new int[]{0, 0, this.screenWidth(), this.screenHeight()};
      var4.project(var1, var2, var3, var5, this.tempVector3f);
      releaseMatrix4f(var4);
      return (float)this.screenHeight() - this.tempVector3f.y();
   }

   private void renderGridXY(int var1) {
      int var2;
      int var3;
      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboLines.addLine((float)var2 + (float)var3 / (float)var1, -5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 5.0F, 0.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboLines.addLine(-5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboLines.addLine((float)var2, -5.0F, 0.0F, (float)var2, 5.0F, 0.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboLines.addLine(-5.0F, (float)var2, 0.0F, 5.0F, (float)var2, 0.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      if (this.m_bDrawGridAxes) {
         byte var4 = 0;
         vboLines.addLine(-5.0F, 0.0F, (float)var4, 5.0F, 0.0F, (float)var4, 1.0F, 0.0F, 0.0F, this.GRID_ALPHA);
         var4 = 0;
         vboLines.addLine(0.0F, -5.0F, (float)var4, 0.0F, 5.0F, (float)var4, 0.0F, 1.0F, 0.0F, this.GRID_ALPHA);
      }

   }

   private void renderGridXZ(int var1) {
      int var2;
      int var3;
      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboLines.addLine((float)var2 + (float)var3 / (float)var1, 0.0F, -5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 5.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboLines.addLine(-5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboLines.addLine((float)var2, 0.0F, -5.0F, (float)var2, 0.0F, 5.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboLines.addLine(-5.0F, 0.0F, (float)var2, 5.0F, 0.0F, (float)var2, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      if (this.m_bDrawGridAxes) {
         byte var4 = 0;
         vboLines.addLine(-5.0F, 0.0F, (float)var4, 5.0F, 0.0F, (float)var4, 1.0F, 0.0F, 0.0F, this.GRID_ALPHA);
         var4 = 0;
         vboLines.addLine((float)var4, 0.0F, -5.0F, (float)var4, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, this.GRID_ALPHA);
      }

   }

   private void renderGridYZ(int var1) {
      int var2;
      int var3;
      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboLines.addLine(0.0F, (float)var2 + (float)var3 / (float)var1, -5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 5.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboLines.addLine(0.0F, -5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 5.0F, (float)var2 + (float)var3 / (float)var1, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboLines.addLine(0.0F, (float)var2, -5.0F, 0.0F, (float)var2, 5.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboLines.addLine(0.0F, -5.0F, (float)var2, 0.0F, 5.0F, (float)var2, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      if (this.m_bDrawGridAxes) {
         byte var4 = 0;
         vboLines.addLine(0.0F, -5.0F, (float)var4, 0.0F, 5.0F, (float)var4, 0.0F, 1.0F, 0.0F, this.GRID_ALPHA);
         var4 = 0;
         vboLines.addLine((float)var4, 0.0F, -5.0F, (float)var4, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, this.GRID_ALPHA);
      }

   }

   private void renderGrid() {
      vboLines.setLineWidth(1.0F);
      this.GRID_ALPHA = 1.0F;
      long var1 = System.currentTimeMillis();
      if (this.m_viewChangeTime + this.VIEW_CHANGE_TIME > var1) {
         float var3 = (float)(this.m_viewChangeTime + this.VIEW_CHANGE_TIME - var1) / (float)this.VIEW_CHANGE_TIME;
         this.GRID_ALPHA = 1.0F - var3;
         this.GRID_ALPHA *= this.GRID_ALPHA;
      }

      switch (this.m_view) {
         case Left:
         case Right:
            this.renderGridYZ(10);
            return;
         case Top:
         case Bottom:
            this.renderGridXZ(10);
            return;
         case Front:
         case Back:
            this.renderGridXY(10);
            return;
         default:
            switch (this.m_gridPlane) {
               case XY:
                  this.renderGridXY(10);
                  break;
               case XZ:
                  this.renderGridXZ(10);
                  break;
               case YZ:
                  this.renderGridYZ(10);
            }

      }
   }

   void renderAxis(PositionRotation var1) {
      this.renderAxis(var1.pos, var1.rot);
   }

   void renderAxis(Vector3f var1, Vector3f var2) {
      StateData var3 = this.stateDataRender();
      vboLines.flush();
      Matrix4f var4 = allocMatrix4f();
      var4.set(var3.m_gizmoParentTransform);
      var4.mul(var3.m_gizmoOriginTransform);
      var4.mul(var3.m_gizmoChildTransform);
      var4.translate(var1);
      var4.rotateXYZ(var2.x * 0.017453292F, var2.y * 0.017453292F, var2.z * 0.017453292F);
      var3.m_modelView.mul(var4, var4);
      PZGLUtil.pushAndLoadMatrix(5888, var4);
      releaseMatrix4f(var4);
      float var5 = 0.1F;
      vboLines.setLineWidth(3.0F);
      vboLines.addLine(0.0F, 0.0F, 0.0F, var5, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      vboLines.addLine(0.0F, 0.0F, 0.0F, 0.0F, 0.0F + var5, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F);
      vboLines.addLine(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F + var5, 0.0F, 0.0F, 1.0F, 1.0F);
      vboLines.flush();
      PZGLUtil.popMatrix(5888);
   }

   private void renderAABB(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      float var10 = var4 / 2.0F;
      float var11 = var5 / 2.0F;
      float var12 = var6 / 2.0F;
      vboLines.setOffset(var1, var2, var3);
      vboLines.setLineWidth(1.0F);
      float var13 = 1.0F;
      vboLines.addLine(var10, var11, var12, -var10, var11, var12, var7, var8, var9, var13);
      vboLines.addLine(var10, var11, var12, var10, -var11, var12, var7, var8, var9, var13);
      vboLines.addLine(var10, var11, var12, var10, var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(-var10, var11, var12, -var10, -var11, var12, var7, var8, var9, var13);
      vboLines.addLine(-var10, var11, var12, -var10, var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(var10, var11, -var12, var10, -var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(var10, var11, -var12, -var10, var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(-var10, var11, -var12, -var10, -var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(var10, -var11, -var12, -var10, -var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(var10, -var11, var12, var10, -var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(-var10, -var11, var12, -var10, -var11, -var12, var7, var8, var9, var13);
      vboLines.addLine(var10, -var11, var12, -var10, -var11, var12, var7, var8, var9, var13);
      vboLines.setOffset(0.0F, 0.0F, 0.0F);
   }

   private void renderAABB(float var1, float var2, float var3, Vector3f var4, Vector3f var5, float var6, float var7, float var8) {
      vboLines.setOffset(var1, var2, var3);
      vboLines.setLineWidth(1.0F);
      float var9 = 1.0F;
      vboLines.addLine(var5.x, var5.y, var5.z, var4.x, var5.y, var5.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var5.y, var5.z, var5.x, var4.y, var5.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var5.y, var5.z, var5.x, var5.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var4.x, var5.y, var5.z, var4.x, var4.y, var5.z, var6, var7, var8, var9);
      vboLines.addLine(var4.x, var5.y, var5.z, var4.x, var5.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var5.y, var4.z, var5.x, var4.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var5.y, var4.z, var4.x, var5.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var4.x, var5.y, var4.z, var4.x, var4.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var4.y, var4.z, var4.x, var4.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var4.y, var5.z, var5.x, var4.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var4.x, var4.y, var5.z, var4.x, var4.y, var4.z, var6, var7, var8, var9);
      vboLines.addLine(var5.x, var4.y, var5.z, var4.x, var4.y, var5.z, var6, var7, var8, var9);
      vboLines.setOffset(0.0F, 0.0F, 0.0F);
   }

   private void renderBox3D(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12) {
      StateData var13 = this.stateDataRender();
      vboLines.flush();
      Matrix4f var14 = allocMatrix4f();
      var14.identity();
      var14.translate(var1, var2, var3);
      var14.rotateXYZ(var7 * 0.017453292F, var8 * 0.017453292F, var9 * 0.017453292F);
      var13.m_modelView.mul(var14, var14);
      PZGLUtil.pushAndLoadMatrix(5888, var14);
      releaseMatrix4f(var14);
      this.renderAABB(var1 * 0.0F, var2 * 0.0F, var3 * 0.0F, var4, var5, var6, var10, var11, var12);
      vboLines.flush();
      PZGLUtil.popMatrix(5888);
   }

   private void calcMatrices(Matrix4f var1, Matrix4f var2) {
      float var3 = (float)this.screenWidth();
      float var4 = 1366.0F / var3;
      float var5 = (float)this.screenHeight() * var4;
      var3 = 1366.0F;
      var3 /= this.zoomMult();
      var5 /= this.zoomMult();
      var1.setOrtho(-var3 / 2.0F, var3 / 2.0F, -var5 / 2.0F, var5 / 2.0F, -10.0F, 10.0F);
      float var6 = (float)this.m_view_x / this.zoomMult() * var4;
      float var7 = (float)this.m_view_y / this.zoomMult() * var4;
      var1.translate(-var6, var7, 0.0F);
      var2.identity();
      float var8 = 0.0F;
      float var9 = 0.0F;
      float var10 = 0.0F;
      switch (this.m_view) {
         case Left:
            var9 = 270.0F;
            break;
         case Right:
            var9 = 90.0F;
            break;
         case Top:
            var9 = 90.0F;
            var10 = 90.0F;
            break;
         case Bottom:
            var9 = 90.0F;
            var10 = 270.0F;
         case Front:
         default:
            break;
         case Back:
            var9 = 180.0F;
            break;
         case UserDefined:
            var8 = this.m_viewRotation.x;
            var9 = this.m_viewRotation.y;
            var10 = this.m_viewRotation.z;
      }

      var2.rotateXYZ(var8 * 0.017453292F, var9 * 0.017453292F, var10 * 0.017453292F);
   }

   Ray getCameraRay(float var1, float var2, Ray var3) {
      return this.getCameraRay(var1, var2, this.m_projection, this.m_modelView, var3);
   }

   Ray getCameraRay(float var1, float var2, Matrix4f var3, Matrix4f var4, Ray var5) {
      Matrix4f var6 = allocMatrix4f();
      var6.set(var3);
      var6.mul(var4);
      var6.invert();
      this.m_viewport[2] = this.screenWidth();
      this.m_viewport[3] = this.screenHeight();
      Vector3f var7 = var6.unprojectInv(var1, var2, 0.0F, this.m_viewport, allocVector3f());
      Vector3f var8 = var6.unprojectInv(var1, var2, 1.0F, this.m_viewport, allocVector3f());
      var5.origin.set(var7);
      var5.direction.set(var8.sub(var7).normalize());
      releaseVector3f(var8);
      releaseVector3f(var7);
      releaseMatrix4f(var6);
      return var5;
   }

   float closest_distance_between_lines(Ray var1, Ray var2) {
      Vector3f var3 = allocVector3f().set(var1.direction);
      Vector3f var4 = allocVector3f().set(var2.direction);
      Vector3f var5 = allocVector3f().set(var1.origin).sub(var2.origin);
      float var6 = var3.dot(var3);
      float var7 = var3.dot(var4);
      float var8 = var4.dot(var4);
      float var9 = var3.dot(var5);
      float var10 = var4.dot(var5);
      float var11 = var6 * var8 - var7 * var7;
      float var12;
      float var13;
      if (var11 < 1.0E-8F) {
         var12 = 0.0F;
         var13 = var7 > var8 ? var9 / var7 : var10 / var8;
      } else {
         var12 = (var7 * var10 - var8 * var9) / var11;
         var13 = (var6 * var10 - var7 * var9) / var11;
      }

      Vector3f var14 = var5.add(var3.mul(var12)).sub(var4.mul(var13));
      var1.t = var12;
      var2.t = var13;
      releaseVector3f(var3);
      releaseVector3f(var4);
      releaseVector3f(var5);
      return var14.length();
   }

   Vector3f project(Vector3f var1, Vector3f var2, Vector3f var3) {
      return var3.set(var2).mul(var1.dot(var2) / var2.dot(var2));
   }

   Vector3f reject(Vector3f var1, Vector3f var2, Vector3f var3) {
      Vector3f var4 = this.project(var1, var2, allocVector3f());
      var3.set(var1).sub(var4);
      releaseVector3f(var4);
      return var3;
   }

   public static int intersect_ray_plane(Plane var0, Ray var1, Vector3f var2) {
      Vector3f var3 = allocVector3f().set(var1.direction).mul(100.0F);
      Vector3f var4 = allocVector3f().set(var1.origin).sub(var0.point);

      byte var7;
      try {
         float var5 = var0.normal.dot(var3);
         float var6 = -var0.normal.dot(var4);
         if (!(Math.abs(var5) < 1.0E-8F)) {
            float var12 = var6 / var5;
            byte var8;
            if (!(var12 < 0.0F) && !(var12 > 1.0F)) {
               var2.set(var1.origin).add(var3.mul(var12));
               var8 = 1;
               return var8;
            }

            var8 = 0;
            return var8;
         }

         if (var6 == 0.0F) {
            var7 = 2;
            return var7;
         }

         var7 = 0;
      } finally {
         releaseVector3f(var3);
         releaseVector3f(var4);
      }

      return var7;
   }

   float distance_between_point_ray(Vector3f var1, Ray var2) {
      Vector3f var3 = allocVector3f().set(var2.direction).mul(100.0F);
      Vector3f var4 = allocVector3f().set(var1).sub(var2.origin);
      float var5 = var4.dot(var3);
      float var6 = var3.dot(var3);
      float var7 = var5 / var6;
      Vector3f var8 = var3.mul(var7).add(var2.origin);
      float var9 = var8.sub(var1).length();
      releaseVector3f(var4);
      releaseVector3f(var3);
      return var9;
   }

   float closest_distance_line_circle(Ray var1, Circle var2, Vector3f var3) {
      Plane var4 = allocPlane().set(var2.orientation, var2.center);
      Vector3f var5 = allocVector3f();
      float var6;
      if (intersect_ray_plane(var4, var1, var5) == 1) {
         var3.set(var5).sub(var2.center).normalize().mul(var2.radius).add(var2.center);
         var6 = var5.sub(var3).length();
      } else {
         Vector3f var7 = allocVector3f().set(var1.origin).sub(var2.center);
         Vector3f var8 = this.reject(var7, var2.orientation, allocVector3f());
         var3.set(var8.normalize().mul(var2.radius).add(var2.center));
         var6 = this.distance_between_point_ray(var3, var1);
         releaseVector3f(var8);
         releaseVector3f(var7);
      }

      releaseVector3f(var5);
      releasePlane(var4);
      return var6;
   }

   private StateData stateDataMain() {
      return this.m_stateData[SpriteRenderer.instance.getMainStateIndex()];
   }

   private StateData stateDataRender() {
      return this.m_stateData[SpriteRenderer.instance.getRenderStateIndex()];
   }

   private static enum View {
      Left,
      Right,
      Top,
      Bottom,
      Front,
      Back,
      UserDefined;

      private View() {
      }
   }

   private static enum TransformMode {
      Global,
      Local;

      private TransformMode() {
      }
   }

   private static enum GridPlane {
      XY,
      XZ,
      YZ;

      private GridPlane() {
      }
   }

   private final class CharacterSceneModelCamera extends SceneModelCamera {
      private CharacterSceneModelCamera() {
         super();
      }

      public void Begin() {
         StateData var1 = UI3DScene.this.stateDataRender();
         GL11.glViewport(UI3DScene.this.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(), UI3DScene.this.getWidth().intValue(), UI3DScene.this.getHeight().intValue());
         PZGLUtil.pushAndLoadMatrix(5889, var1.m_projection);
         Matrix4f var2 = UI3DScene.allocMatrix4f();
         var2.set(var1.m_modelView);
         var2.mul(this.m_renderData.m_transform);
         PZGLUtil.pushAndLoadMatrix(5888, var2);
         UI3DScene.releaseMatrix4f(var2);
      }

      public void End() {
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
      }
   }

   private final class VehicleSceneModelCamera extends SceneModelCamera {
      private VehicleSceneModelCamera() {
         super();
      }

      public void Begin() {
         StateData var1 = UI3DScene.this.stateDataRender();
         GL11.glViewport(UI3DScene.this.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(), UI3DScene.this.getWidth().intValue(), UI3DScene.this.getHeight().intValue());
         PZGLUtil.pushAndLoadMatrix(5889, var1.m_projection);
         Matrix4f var2 = UI3DScene.allocMatrix4f();
         var2.set(var1.m_modelView);
         var2.mul(this.m_renderData.m_transform);
         PZGLUtil.pushAndLoadMatrix(5888, var2);
         UI3DScene.releaseMatrix4f(var2);
         GL11.glDepthRange(0.0, 1.0);
         GL11.glDepthMask(true);
      }

      public void End() {
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
      }
   }

   private static final class StateData {
      final Matrix4f m_projection = new Matrix4f();
      final Matrix4f m_modelView = new Matrix4f();
      int m_zoom;
      GridPlaneDrawer m_gridPlaneDrawer;
      OverlaysDrawer m_overlaysDrawer;
      final ArrayList<SceneObjectRenderData> m_objectData = new ArrayList();
      Gizmo m_gizmo = null;
      final Vector3f m_gizmoTranslate = new Vector3f();
      final Vector3f m_gizmoRotate = new Vector3f();
      final Matrix4f m_gizmoParentTransform = new Matrix4f();
      final Matrix4f m_gizmoOriginTransform = new Matrix4f();
      final Matrix4f m_gizmoChildTransform = new Matrix4f();
      final Matrix4f m_gizmoTransform = new Matrix4f();
      boolean m_hasGizmoOrigin;
      boolean m_selectedAttachmentIsChildAttachment;
      Axis m_gizmoAxis;
      final TranslateGizmoRenderData m_translateGizmoRenderData;
      final ArrayList<PositionRotation> m_axes;
      final ArrayList<AABB> m_aabb;
      final ArrayList<Box3D> m_box3D;

      private StateData() {
         this.m_gizmoAxis = UI3DScene.Axis.None;
         this.m_translateGizmoRenderData = new TranslateGizmoRenderData();
         this.m_axes = new ArrayList();
         this.m_aabb = new ArrayList();
         this.m_box3D = new ArrayList();
      }

      private float zoomMult() {
         return (float)Math.exp((double)((float)this.m_zoom * 0.2F)) * 160.0F / Math.max(1.82F, 1.0F);
      }
   }

   private final class RotateGizmo extends Gizmo {
      Axis m_trackAxis;
      final Circle m_trackCircle;
      final Matrix4f m_startXfrm;
      final Matrix4f m_startInvXfrm;
      final Vector3f m_startPointOnCircle;
      final Vector3f m_currentPointOnCircle;
      final ArrayList<Vector3f> m_circlePointsMain;
      final ArrayList<Vector3f> m_circlePointsRender;

      private RotateGizmo() {
         super();
         this.m_trackAxis = UI3DScene.Axis.None;
         this.m_trackCircle = new Circle();
         this.m_startXfrm = new Matrix4f();
         this.m_startInvXfrm = new Matrix4f();
         this.m_startPointOnCircle = new Vector3f();
         this.m_currentPointOnCircle = new Vector3f();
         this.m_circlePointsMain = new ArrayList();
         this.m_circlePointsRender = new ArrayList();
      }

      Axis hitTest(float var1, float var2) {
         if (!this.m_visible) {
            return UI3DScene.Axis.None;
         } else {
            StateData var3 = UI3DScene.this.stateDataMain();
            var2 = (float)UI3DScene.this.screenHeight() - var2;
            Ray var4 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Matrix4f var5 = UI3DScene.allocMatrix4f();
            var5.set(var3.m_gizmoParentTransform);
            var5.mul(var3.m_gizmoOriginTransform);
            var5.mul(var3.m_gizmoChildTransform);
            var5.mul(var3.m_gizmoTransform);
            Vector3f var6 = var5.getScale(UI3DScene.allocVector3f());
            var5.scale(1.0F / var6.x, 1.0F / var6.y, 1.0F / var6.z);
            UI3DScene.releaseVector3f(var6);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var5.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            float var17 = UI3DScene.this.m_gizmoScale / var3.zoomMult() * 1000.0F;
            float var7 = this.LENGTH * var17;
            Vector3f var8 = var5.transformProject(UI3DScene.allocVector3f().set(0.0F, 0.0F, 0.0F));
            Vector3f var9 = var5.transformDirection(UI3DScene.allocVector3f().set(1.0F, 0.0F, 0.0F)).normalize();
            Vector3f var10 = var5.transformDirection(UI3DScene.allocVector3f().set(0.0F, 1.0F, 0.0F)).normalize();
            Vector3f var11 = var5.transformDirection(UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F)).normalize();
            Vector2 var12 = UI3DScene.allocVector2();
            this.getCircleSegments(var8, var7, var10, var11, this.m_circlePointsMain);
            float var13 = this.hitTestCircle(var4, this.m_circlePointsMain, var12);
            ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(this.m_circlePointsMain);
            this.m_circlePointsMain.clear();
            this.getCircleSegments(var8, var7, var9, var11, this.m_circlePointsMain);
            float var14 = this.hitTestCircle(var4, this.m_circlePointsMain, var12);
            ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(this.m_circlePointsMain);
            this.m_circlePointsMain.clear();
            this.getCircleSegments(var8, var7, var9, var10, this.m_circlePointsMain);
            float var15 = this.hitTestCircle(var4, this.m_circlePointsMain, var12);
            ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(this.m_circlePointsMain);
            this.m_circlePointsMain.clear();
            UI3DScene.releaseVector2(var12);
            UI3DScene.releaseVector3f(var9);
            UI3DScene.releaseVector3f(var10);
            UI3DScene.releaseVector3f(var11);
            UI3DScene.releaseVector3f(var8);
            UI3DScene.releaseRay(var4);
            UI3DScene.releaseMatrix4f(var5);
            float var16 = 8.0F;
            if (var13 < var14 && var13 < var15) {
               return var13 <= var16 ? UI3DScene.Axis.X : UI3DScene.Axis.None;
            } else if (var14 < var13 && var14 < var15) {
               return var14 <= var16 ? UI3DScene.Axis.Y : UI3DScene.Axis.None;
            } else if (var15 < var13 && var15 < var14) {
               return var15 <= var16 ? UI3DScene.Axis.Z : UI3DScene.Axis.None;
            } else {
               return UI3DScene.Axis.None;
            }
         }
      }

      void startTracking(float var1, float var2, Axis var3) {
         StateData var4 = UI3DScene.this.stateDataMain();
         this.m_startXfrm.set(var4.m_gizmoParentTransform);
         this.m_startXfrm.mul(var4.m_gizmoOriginTransform);
         this.m_startXfrm.mul(var4.m_gizmoChildTransform);
         this.m_startXfrm.mul(var4.m_gizmoTransform);
         if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
            this.m_startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
         }

         this.m_startInvXfrm.set(var4.m_gizmoParentTransform);
         this.m_startInvXfrm.mul(var4.m_gizmoOriginTransform);
         this.m_startInvXfrm.mul(var4.m_gizmoChildTransform);
         this.m_startInvXfrm.mul(var4.m_gizmoTransform);
         this.m_startInvXfrm.invert();
         this.m_trackAxis = var3;
         this.getPointOnAxis(var1, var2, var3, this.m_trackCircle, this.m_startXfrm, this.m_startPointOnCircle);
      }

      void updateTracking(float var1, float var2) {
         Vector3f var3 = this.getPointOnAxis(var1, var2, this.m_trackAxis, this.m_trackCircle, this.m_startXfrm, UI3DScene.allocVector3f());
         if (this.m_currentPointOnCircle.equals(var3)) {
            UI3DScene.releaseVector3f(var3);
         } else {
            this.m_currentPointOnCircle.set(var3);
            UI3DScene.releaseVector3f(var3);
            float var4 = this.calculateRotation(this.m_startPointOnCircle, this.m_currentPointOnCircle, this.m_trackCircle);
            switch (this.m_trackAxis) {
               case X:
                  this.m_trackCircle.orientation.set(1.0F, 0.0F, 0.0F);
                  break;
               case Y:
                  this.m_trackCircle.orientation.set(0.0F, 1.0F, 0.0F);
                  break;
               case Z:
                  this.m_trackCircle.orientation.set(0.0F, 0.0F, 1.0F);
            }

            Vector3f var5 = UI3DScene.allocVector3f().set(this.m_trackCircle.orientation);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               this.m_startInvXfrm.transformDirection(var5);
            }

            Ray var6 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Vector3f var7 = this.m_startXfrm.transformDirection(UI3DScene.allocVector3f().set(var5)).normalize();
            float var8 = var6.direction.dot(var7);
            UI3DScene.releaseVector3f(var7);
            UI3DScene.releaseRay(var6);
            if (UI3DScene.this.m_gizmoParent instanceof SceneCharacter) {
               if (var8 > 0.0F) {
                  var4 *= -1.0F;
               }
            } else if (var8 < 0.0F) {
               var4 *= -1.0F;
            }

            Quaternionf var9 = UI3DScene.allocQuaternionf().fromAxisAngleDeg(var5, var4);
            UI3DScene.releaseVector3f(var5);
            var7 = var9.getEulerAnglesXYZ(new Vector3f());
            UI3DScene.releaseQuaternionf(var9);
            var7.x = (float)Math.floor((double)(var7.x * 57.295776F + 0.5F));
            var7.y = (float)Math.floor((double)(var7.y * 57.295776F + 0.5F));
            var7.z = (float)Math.floor((double)(var7.z * 57.295776F + 0.5F));
            LuaManager.caller.pcall(UIManager.getDefaultThread(), UI3DScene.this.getTable().rawget("onGizmoChanged"), new Object[]{UI3DScene.this.table, var7});
         }
      }

      void stopTracking() {
         this.m_trackAxis = UI3DScene.Axis.None;
      }

      void render() {
         if (this.m_visible) {
            StateData var1 = UI3DScene.this.stateDataRender();
            Matrix4f var2 = UI3DScene.allocMatrix4f();
            var2.set(var1.m_gizmoParentTransform);
            var2.mul(var1.m_gizmoOriginTransform);
            var2.mul(var1.m_gizmoChildTransform);
            var2.mul(var1.m_gizmoTransform);
            Vector3f var3 = var2.getScale(UI3DScene.allocVector3f());
            var2.scale(1.0F / var3.x, 1.0F / var3.y, 1.0F / var3.z);
            UI3DScene.releaseVector3f(var3);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var2.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            float var16 = (float)(Mouse.getXA() - UI3DScene.this.getAbsoluteX().intValue());
            float var4 = (float)(Mouse.getYA() - UI3DScene.this.getAbsoluteY().intValue());
            Ray var5 = UI3DScene.this.getCameraRay(var16, (float)UI3DScene.this.screenHeight() - var4, var1.m_projection, var1.m_modelView, UI3DScene.allocRay());
            float var6 = UI3DScene.this.m_gizmoScale / var1.zoomMult() * 1000.0F;
            float var7 = this.LENGTH * var6;
            Vector3f var8 = var2.transformProject(UI3DScene.allocVector3f().set(0.0F, 0.0F, 0.0F));
            Vector3f var9 = var2.transformDirection(UI3DScene.allocVector3f().set(1.0F, 0.0F, 0.0F)).normalize();
            Vector3f var10 = var2.transformDirection(UI3DScene.allocVector3f().set(0.0F, 1.0F, 0.0F)).normalize();
            Vector3f var11 = var2.transformDirection(UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F)).normalize();
            GL11.glClear(256);
            GL11.glEnable(2929);
            Axis var12 = this.m_trackAxis == UI3DScene.Axis.None ? var1.m_gizmoAxis : this.m_trackAxis;
            float var13;
            float var14;
            float var15;
            if (this.m_trackAxis == UI3DScene.Axis.None || this.m_trackAxis == UI3DScene.Axis.X) {
               var13 = var12 == UI3DScene.Axis.X ? 1.0F : 0.5F;
               var14 = 0.0F;
               var15 = 0.0F;
               this.renderAxis(var8, var7, var10, var11, var13, var14, var15, var5);
            }

            if (this.m_trackAxis == UI3DScene.Axis.None || this.m_trackAxis == UI3DScene.Axis.Y) {
               var13 = 0.0F;
               var14 = var12 == UI3DScene.Axis.Y ? 1.0F : 0.5F;
               var15 = 0.0F;
               this.renderAxis(var8, var7, var9, var11, var13, var14, var15, var5);
            }

            if (this.m_trackAxis == UI3DScene.Axis.None || this.m_trackAxis == UI3DScene.Axis.Z) {
               var13 = 0.0F;
               var14 = 0.0F;
               var15 = var12 == UI3DScene.Axis.Z ? 1.0F : 0.5F;
               this.renderAxis(var8, var7, var9, var10, var13, var14, var15, var5);
            }

            UI3DScene.releaseVector3f(var8);
            UI3DScene.releaseVector3f(var9);
            UI3DScene.releaseVector3f(var10);
            UI3DScene.releaseVector3f(var11);
            UI3DScene.releaseRay(var5);
            UI3DScene.releaseMatrix4f(var2);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            this.renderLineToOrigin();
         }
      }

      void getCircleSegments(Vector3f var1, float var2, Vector3f var3, Vector3f var4, ArrayList<Vector3f> var5) {
         Vector3f var6 = UI3DScene.allocVector3f();
         Vector3f var7 = UI3DScene.allocVector3f();
         byte var8 = 32;
         double var9 = 0.0 / (double)var8 * 0.01745329238474369;
         double var11 = Math.cos(var9);
         double var13 = Math.sin(var9);
         var3.mul((float)var11, var6);
         var4.mul((float)var13, var7);
         var6.add(var7).mul(var2);
         var5.add(UI3DScene.allocVector3f().set(var1).add(var6));

         for(int var15 = 1; var15 <= var8; ++var15) {
            var9 = (double)var15 * 360.0 / (double)var8 * 0.01745329238474369;
            var11 = Math.cos(var9);
            var13 = Math.sin(var9);
            var3.mul((float)var11, var6);
            var4.mul((float)var13, var7);
            var6.add(var7).mul(var2);
            var5.add(UI3DScene.allocVector3f().set(var1).add(var6));
         }

         UI3DScene.releaseVector3f(var6);
         UI3DScene.releaseVector3f(var7);
      }

      private float hitTestCircle(Ray var1, ArrayList<Vector3f> var2, Vector2 var3) {
         Ray var4 = UI3DScene.allocRay();
         Vector3f var5 = UI3DScene.allocVector3f();
         float var6 = UI3DScene.this.sceneToUIX(var1.origin.x, var1.origin.y, var1.origin.z);
         float var7 = UI3DScene.this.sceneToUIY(var1.origin.x, var1.origin.y, var1.origin.z);
         float var8 = 3.4028235E38F;
         Vector3f var9 = (Vector3f)var2.get(0);

         for(int var10 = 1; var10 < var2.size(); ++var10) {
            Vector3f var11 = (Vector3f)var2.get(var10);
            float var12 = UI3DScene.this.sceneToUIX(var9.x, var9.y, var9.z);
            float var13 = UI3DScene.this.sceneToUIY(var9.x, var9.y, var9.z);
            float var14 = UI3DScene.this.sceneToUIX(var11.x, var11.y, var11.z);
            float var15 = UI3DScene.this.sceneToUIY(var11.x, var11.y, var11.z);
            double var16 = Math.pow((double)(var14 - var12), 2.0) + Math.pow((double)(var15 - var13), 2.0);
            if (var16 < 0.001) {
               var9 = var11;
            } else {
               double var18 = (double)((var6 - var12) * (var14 - var12) + (var7 - var13) * (var15 - var13)) / var16;
               double var20 = (double)var12 + var18 * (double)(var14 - var12);
               double var22 = (double)var13 + var18 * (double)(var15 - var13);
               if (var18 <= 0.0) {
                  var20 = (double)var12;
                  var22 = (double)var13;
               } else if (var18 >= 1.0) {
                  var20 = (double)var14;
                  var22 = (double)var15;
               }

               float var24 = IsoUtils.DistanceTo2D(var6, var7, (float)var20, (float)var22);
               if (var24 < var8) {
                  var8 = var24;
                  var3.set((float)var20, (float)var22);
               }

               var9 = var11;
            }
         }

         UI3DScene.releaseVector3f(var5);
         UI3DScene.releaseRay(var4);
         return var8;
      }

      void renderAxis(Vector3f var1, float var2, Vector3f var3, Vector3f var4, float var5, float var6, float var7, Ray var8) {
         UI3DScene.vboLines.flush();
         UI3DScene.vboLines.setLineWidth(6.0F);
         this.getCircleSegments(var1, var2, var3, var4, this.m_circlePointsRender);
         Vector3f var9 = UI3DScene.allocVector3f();
         Vector3f var10 = (Vector3f)this.m_circlePointsRender.get(0);

         for(int var11 = 1; var11 < this.m_circlePointsRender.size(); ++var11) {
            Vector3f var12 = (Vector3f)this.m_circlePointsRender.get(var11);
            var9.set(var12.x - var1.x, var12.y - var1.y, var12.z - var1.z).normalize();
            float var13 = var9.dot(var8.direction);
            if (var13 < 0.1F) {
               UI3DScene.vboLines.addLine(var10.x, var10.y, var10.z, var12.x, var12.y, var12.z, var5, var6, var7, 1.0F);
            } else {
               UI3DScene.vboLines.addLine(var10.x, var10.y, var10.z, var12.x, var12.y, var12.z, var5 / 2.0F, var6 / 2.0F, var7 / 2.0F, 0.25F);
            }

            var10 = var12;
         }

         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(this.m_circlePointsRender);
         this.m_circlePointsRender.clear();
         UI3DScene.releaseVector3f(var9);
         UI3DScene.vboLines.flush();
      }

      Vector3f getPointOnAxis(float var1, float var2, Axis var3, Circle var4, Matrix4f var5, Vector3f var6) {
         float var7 = 1.0F;
         var4.radius = this.LENGTH * var7;
         var5.getTranslation(var4.center);
         float var8 = UI3DScene.this.sceneToUIX(var4.center.x, var4.center.y, var4.center.z);
         float var9 = UI3DScene.this.sceneToUIY(var4.center.x, var4.center.y, var4.center.z);
         var4.center.set(var8, var9, 0.0F);
         var4.orientation.set(0.0F, 0.0F, 1.0F);
         Ray var10 = UI3DScene.allocRay();
         var10.origin.set(var1, var2, 0.0F);
         var10.direction.set(0.0F, 0.0F, -1.0F);
         UI3DScene.this.closest_distance_line_circle(var10, var4, var6);
         UI3DScene.releaseRay(var10);
         return var6;
      }

      float calculateRotation(Vector3f var1, Vector3f var2, Circle var3) {
         if (var1.equals(var2)) {
            return 0.0F;
         } else {
            Vector3f var4 = UI3DScene.allocVector3f().set(var1).sub(var3.center).normalize();
            Vector3f var5 = UI3DScene.allocVector3f().set(var2).sub(var3.center).normalize();
            float var6 = (float)Math.acos((double)var5.dot(var4));
            Vector3f var7 = var4.cross(var5, UI3DScene.allocVector3f());
            int var8 = (int)Math.signum(var7.dot(var3.orientation));
            UI3DScene.releaseVector3f(var4);
            UI3DScene.releaseVector3f(var5);
            UI3DScene.releaseVector3f(var7);
            return (float)var8 * var6 * 57.295776F;
         }
      }
   }

   private final class ScaleGizmo extends Gizmo {
      final Matrix4f m_startXfrm = new Matrix4f();
      final Matrix4f m_startInvXfrm = new Matrix4f();
      final Vector3f m_startPos = new Vector3f();
      final Vector3f m_currentPos = new Vector3f();
      Axis m_trackAxis;
      boolean m_hideX;
      boolean m_hideY;
      boolean m_hideZ;
      final Cylinder cylinder;

      private ScaleGizmo() {
         super();
         this.m_trackAxis = UI3DScene.Axis.None;
         this.cylinder = new Cylinder();
      }

      Axis hitTest(float var1, float var2) {
         if (!this.m_visible) {
            return UI3DScene.Axis.None;
         } else {
            StateData var3 = UI3DScene.this.stateDataMain();
            Matrix4f var4 = UI3DScene.allocMatrix4f();
            var4.set(var3.m_gizmoParentTransform);
            var4.mul(var3.m_gizmoOriginTransform);
            var4.mul(var3.m_gizmoTransform);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var4.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            var2 = (float)UI3DScene.this.screenHeight() - var2;
            Ray var5 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Ray var6 = UI3DScene.allocRay();
            var4.transformProject(var6.origin.set(0.0F, 0.0F, 0.0F));
            float var7 = UI3DScene.this.m_gizmoScale / var3.zoomMult() * 1000.0F;
            float var8 = this.LENGTH * var7;
            float var9 = this.THICKNESS * var7;
            float var10 = 0.1F * var7;
            var4.transformDirection(var6.direction.set(1.0F, 0.0F, 0.0F)).normalize();
            float var11 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var12 = var6.t;
            float var13 = var5.t;
            if (var12 < var10 || var12 >= var10 + var8) {
               var12 = 3.4028235E38F;
               var11 = 3.4028235E38F;
            }

            float var14 = var6.direction.dot(var5.direction);
            this.m_hideX = Math.abs(var14) > 0.9F;
            var4.transformDirection(var6.direction.set(0.0F, 1.0F, 0.0F)).normalize();
            float var15 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var16 = var6.t;
            float var17 = var5.t;
            if (var16 < var10 || var16 >= var10 + var8) {
               var16 = 3.4028235E38F;
               var15 = 3.4028235E38F;
            }

            float var18 = var6.direction.dot(var5.direction);
            this.m_hideY = Math.abs(var18) > 0.9F;
            var4.transformDirection(var6.direction.set(0.0F, 0.0F, 1.0F)).normalize();
            float var19 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var20 = var6.t;
            float var21 = var5.t;
            if (var20 < var10 || var20 >= var10 + var8) {
               var20 = 3.4028235E38F;
               var19 = 3.4028235E38F;
            }

            float var22 = var6.direction.dot(var5.direction);
            this.m_hideZ = Math.abs(var22) > 0.9F;
            UI3DScene.releaseRay(var6);
            UI3DScene.releaseRay(var5);
            UI3DScene.releaseMatrix4f(var4);
            if (var12 >= var10 && var12 < var10 + var8 && var11 < var15 && var11 < var19) {
               return var11 <= var9 / 2.0F ? UI3DScene.Axis.X : UI3DScene.Axis.None;
            } else if (var16 >= var10 && var16 < var10 + var8 && var15 < var11 && var15 < var19) {
               return var15 <= var9 / 2.0F ? UI3DScene.Axis.Y : UI3DScene.Axis.None;
            } else if (var20 >= var10 && var20 < var10 + var8 && var19 < var11 && var19 < var15) {
               return var19 <= var9 / 2.0F ? UI3DScene.Axis.Z : UI3DScene.Axis.None;
            } else {
               return UI3DScene.Axis.None;
            }
         }
      }

      void startTracking(float var1, float var2, Axis var3) {
         StateData var4 = UI3DScene.this.stateDataMain();
         this.m_startXfrm.set(var4.m_gizmoParentTransform);
         this.m_startXfrm.mul(var4.m_gizmoOriginTransform);
         this.m_startXfrm.mul(var4.m_gizmoTransform);
         this.m_startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
         this.m_startInvXfrm.set(this.m_startXfrm);
         this.m_startInvXfrm.invert();
         this.m_trackAxis = var3;
         this.getPointOnAxis(var1, var2, var3, this.m_startXfrm, this.m_startPos);
      }

      void updateTracking(float var1, float var2) {
         Vector3f var3 = this.getPointOnAxis(var1, var2, this.m_trackAxis, this.m_startXfrm, UI3DScene.allocVector3f());
         if (this.m_currentPos.equals(var3)) {
            UI3DScene.releaseVector3f(var3);
         } else {
            UI3DScene.releaseVector3f(var3);
            this.m_currentPos.set(var3);
            StateData var4 = UI3DScene.this.stateDataMain();
            Vector3f var5 = (new Vector3f(this.m_currentPos)).sub(this.m_startPos);
            Vector3f var6;
            Vector3f var7;
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var6 = this.m_startInvXfrm.transformPosition(this.m_startPos, new Vector3f());
               var7 = this.m_startInvXfrm.transformPosition(this.m_currentPos, new Vector3f());
               Matrix4f var8 = (new Matrix4f(var4.m_gizmoParentTransform)).invert();
               var8.transformPosition(var6);
               var8.transformPosition(var7);
               var5.set(var7).sub(var6);
            } else {
               var6 = this.m_startInvXfrm.transformPosition(this.m_startPos, new Vector3f());
               var7 = this.m_startInvXfrm.transformPosition(this.m_currentPos, new Vector3f());
               var5.set(var7).sub(var6);
            }

            var5.x = (float)Math.floor((double)(var5.x * UI3DScene.this.gridMult())) / UI3DScene.this.gridMult();
            var5.y = (float)Math.floor((double)(var5.y * UI3DScene.this.gridMult())) / UI3DScene.this.gridMult();
            var5.z = (float)Math.floor((double)(var5.z * UI3DScene.this.gridMult())) / UI3DScene.this.gridMult();
            LuaManager.caller.pcall(UIManager.getDefaultThread(), UI3DScene.this.getTable().rawget("onGizmoChanged"), new Object[]{UI3DScene.this.table, var5});
         }
      }

      void stopTracking() {
         this.m_trackAxis = UI3DScene.Axis.None;
      }

      void render() {
         if (this.m_visible) {
            StateData var1 = UI3DScene.this.stateDataRender();
            float var2 = UI3DScene.this.m_gizmoScale / var1.zoomMult() * 1000.0F;
            float var3 = this.LENGTH * var2;
            float var4 = this.THICKNESS * var2;
            float var5 = 0.1F * var2;
            Matrix4f var6 = UI3DScene.allocMatrix4f();
            var6.set(var1.m_gizmoParentTransform);
            var6.mul(var1.m_gizmoOriginTransform);
            var6.mul(var1.m_gizmoChildTransform);
            var6.mul(var1.m_gizmoTransform);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var6.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            var1.m_modelView.mul(var6, var6);
            PZGLUtil.pushAndLoadMatrix(5888, var6);
            UI3DScene.releaseMatrix4f(var6);
            if (!this.m_hideX) {
               GL11.glColor3f(var1.m_gizmoAxis == UI3DScene.Axis.X ? 1.0F : 0.5F, 0.0F, 0.0F);
               GL11.glRotated(90.0, 0.0, 1.0, 0.0);
               GL11.glTranslatef(0.0F, 0.0F, var5);
               this.cylinder.draw(var4 / 2.0F, var4 / 2.0F, var3, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, var3);
               this.cylinder.draw(var4, var4, 0.1F * var2, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, -var5 - var3);
               GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
            }

            if (!this.m_hideY) {
               GL11.glColor3f(0.0F, var1.m_gizmoAxis == UI3DScene.Axis.Y ? 1.0F : 0.5F, 0.0F);
               GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
               GL11.glTranslatef(0.0F, 0.0F, var5);
               this.cylinder.draw(var4 / 2.0F, var4 / 2.0F, var3, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, var3);
               this.cylinder.draw(var4, var4, 0.1F * var2, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, -var5 - var3);
               GL11.glRotated(90.0, 1.0, 0.0, 0.0);
            }

            if (!this.m_hideZ) {
               GL11.glColor3f(0.0F, 0.0F, var1.m_gizmoAxis == UI3DScene.Axis.Z ? 1.0F : 0.5F);
               GL11.glTranslatef(0.0F, 0.0F, var5);
               this.cylinder.draw(var4 / 2.0F, var4 / 2.0F, var3, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, var3);
               this.cylinder.draw(var4, var4, 0.1F * var2, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, -0.1F - var3);
            }

            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            PZGLUtil.popMatrix(5888);
            this.renderLineToOrigin();
         }
      }
   }

   private final class TranslateGizmo extends Gizmo {
      final Matrix4f m_startXfrm = new Matrix4f();
      final Matrix4f m_startInvXfrm = new Matrix4f();
      final Vector3f m_startPos = new Vector3f();
      final Vector3f m_currentPos = new Vector3f();
      Axis m_trackAxis;
      Cylinder cylinder;

      private TranslateGizmo() {
         super();
         this.m_trackAxis = UI3DScene.Axis.None;
         this.cylinder = new Cylinder();
      }

      Axis hitTest(float var1, float var2) {
         if (!this.m_visible) {
            return UI3DScene.Axis.None;
         } else {
            StateData var3 = UI3DScene.this.stateDataMain();
            Matrix4f var4 = UI3DScene.allocMatrix4f();
            var4.set(var3.m_gizmoParentTransform);
            var4.mul(var3.m_gizmoOriginTransform);
            var4.mul(var3.m_gizmoChildTransform);
            var4.mul(var3.m_gizmoTransform);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var4.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            var2 = (float)UI3DScene.this.screenHeight() - var2;
            Ray var5 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Ray var6 = UI3DScene.allocRay();
            var4.transformPosition(var6.origin.set(0.0F, 0.0F, 0.0F));
            float var7 = UI3DScene.this.m_gizmoScale / var3.zoomMult() * 1000.0F;
            float var8 = this.LENGTH * var7;
            float var9 = this.THICKNESS * var7;
            float var10 = 0.1F * var7;
            var4.transformDirection(var6.direction.set(1.0F, 0.0F, 0.0F)).normalize();
            float var11 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var12 = var6.t;
            float var13 = var5.t;
            if (var12 < var10 || var12 >= var10 + var8) {
               var12 = 3.4028235E38F;
               var11 = 3.4028235E38F;
            }

            float var14 = var6.direction.dot(var5.direction);
            var3.m_translateGizmoRenderData.m_hideX = Math.abs(var14) > 0.9F;
            var4.transformDirection(var6.direction.set(0.0F, 1.0F, 0.0F)).normalize();
            float var15 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var16 = var6.t;
            float var17 = var5.t;
            if (var16 < var10 || var16 >= var10 + var8) {
               var16 = 3.4028235E38F;
               var15 = 3.4028235E38F;
            }

            float var18 = var6.direction.dot(var5.direction);
            var3.m_translateGizmoRenderData.m_hideY = Math.abs(var18) > 0.9F;
            var4.transformDirection(var6.direction.set(0.0F, 0.0F, 1.0F)).normalize();
            float var19 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var20 = var6.t;
            float var21 = var5.t;
            if (var20 < var10 || var20 >= var10 + var8) {
               var20 = 3.4028235E38F;
               var19 = 3.4028235E38F;
            }

            float var22 = var6.direction.dot(var5.direction);
            var3.m_translateGizmoRenderData.m_hideZ = Math.abs(var22) > 0.9F;
            UI3DScene.releaseRay(var6);
            UI3DScene.releaseRay(var5);
            UI3DScene.releaseMatrix4f(var4);
            if (var12 >= var10 && var12 < var10 + var8 && var11 < var15 && var11 < var19) {
               return var11 <= var9 / 2.0F ? UI3DScene.Axis.X : UI3DScene.Axis.None;
            } else if (var16 >= var10 && var16 < var10 + var8 && var15 < var11 && var15 < var19) {
               return var15 <= var9 / 2.0F ? UI3DScene.Axis.Y : UI3DScene.Axis.None;
            } else if (var20 >= var10 && var20 < var10 + var8 && var19 < var11 && var19 < var15) {
               return var19 <= var9 / 2.0F ? UI3DScene.Axis.Z : UI3DScene.Axis.None;
            } else {
               return UI3DScene.Axis.None;
            }
         }
      }

      void startTracking(float var1, float var2, Axis var3) {
         StateData var4 = UI3DScene.this.stateDataMain();
         this.m_startXfrm.set(var4.m_gizmoParentTransform);
         this.m_startXfrm.mul(var4.m_gizmoOriginTransform);
         this.m_startXfrm.mul(var4.m_gizmoChildTransform);
         this.m_startXfrm.mul(var4.m_gizmoTransform);
         if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
            this.m_startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
         }

         this.m_startInvXfrm.set(this.m_startXfrm);
         this.m_startInvXfrm.invert();
         this.m_trackAxis = var3;
         this.getPointOnAxis(var1, var2, var3, this.m_startXfrm, this.m_startPos);
      }

      void updateTracking(float var1, float var2) {
         Vector3f var3 = this.getPointOnAxis(var1, var2, this.m_trackAxis, this.m_startXfrm, UI3DScene.allocVector3f());
         if (this.m_currentPos.equals(var3)) {
            UI3DScene.releaseVector3f(var3);
         } else {
            UI3DScene.releaseVector3f(var3);
            this.m_currentPos.set(var3);
            StateData var4 = UI3DScene.this.stateDataMain();
            Vector3f var5 = (new Vector3f(this.m_currentPos)).sub(this.m_startPos);
            Vector3f var6;
            Vector3f var7;
            Matrix4f var8;
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var6 = this.m_startInvXfrm.transformPosition(this.m_startPos, UI3DScene.allocVector3f());
               var7 = this.m_startInvXfrm.transformPosition(this.m_currentPos, UI3DScene.allocVector3f());
               var8 = UI3DScene.allocMatrix4f();
               var8.set(var4.m_gizmoParentTransform);
               var8.mul(var4.m_gizmoOriginTransform);
               var8.mul(var4.m_gizmoChildTransform);
               var8.invert();
               var8.transformPosition(var6);
               var8.transformPosition(var7);
               UI3DScene.releaseMatrix4f(var8);
               var5.set(var7).sub(var6);
               UI3DScene.releaseVector3f(var6);
               UI3DScene.releaseVector3f(var7);
            } else {
               var6 = this.m_startInvXfrm.transformPosition(this.m_startPos, UI3DScene.allocVector3f());
               var7 = this.m_startInvXfrm.transformPosition(this.m_currentPos, UI3DScene.allocVector3f());
               var8 = UI3DScene.allocMatrix4f();
               var8.set(var4.m_gizmoTransform);
               var8.transformPosition(var6);
               var8.transformPosition(var7);
               UI3DScene.releaseMatrix4f(var8);
               var5.set(var7).sub(var6);
               UI3DScene.releaseVector3f(var6);
               UI3DScene.releaseVector3f(var7);
            }

            var5.x = (float)Math.floor((double)(var5.x * UI3DScene.this.gridMult())) / UI3DScene.this.gridMult();
            var5.y = (float)Math.floor((double)(var5.y * UI3DScene.this.gridMult())) / UI3DScene.this.gridMult();
            var5.z = (float)Math.floor((double)(var5.z * UI3DScene.this.gridMult())) / UI3DScene.this.gridMult();
            if (var4.m_selectedAttachmentIsChildAttachment) {
               var5.mul(-1.0F);
            }

            LuaManager.caller.pcall(UIManager.getDefaultThread(), UI3DScene.this.getTable().rawget("onGizmoChanged"), new Object[]{UI3DScene.this.table, var5});
         }
      }

      void stopTracking() {
         this.m_trackAxis = UI3DScene.Axis.None;
      }

      void render() {
         if (this.m_visible) {
            StateData var1 = UI3DScene.this.stateDataRender();
            Matrix4f var2 = UI3DScene.allocMatrix4f();
            var2.set(var1.m_gizmoParentTransform);
            var2.mul(var1.m_gizmoOriginTransform);
            var2.mul(var1.m_gizmoChildTransform);
            var2.mul(var1.m_gizmoTransform);
            Vector3f var3 = var2.getScale(UI3DScene.allocVector3f());
            var2.scale(1.0F / var3.x, 1.0F / var3.y, 1.0F / var3.z);
            UI3DScene.releaseVector3f(var3);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var2.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            var1.m_modelView.mul(var2, var2);
            PZGLUtil.pushAndLoadMatrix(5888, var2);
            UI3DScene.releaseMatrix4f(var2);
            float var7 = UI3DScene.this.m_gizmoScale / var1.zoomMult() * 1000.0F;
            float var4 = this.THICKNESS * var7;
            float var5 = this.LENGTH * var7;
            float var6 = 0.1F * var7;
            if (!var1.m_translateGizmoRenderData.m_hideX) {
               GL11.glColor3f(var1.m_gizmoAxis == UI3DScene.Axis.X ? 1.0F : 0.5F, 0.0F, 0.0F);
               GL11.glRotated(90.0, 0.0, 1.0, 0.0);
               GL11.glTranslatef(0.0F, 0.0F, var6);
               this.cylinder.draw(var4 / 2.0F, var4 / 2.0F, var5, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, var5);
               this.cylinder.draw(var4 / 2.0F * 2.0F, 0.0F, 0.1F * var7, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, -var6 - var5);
               GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
            }

            if (!var1.m_translateGizmoRenderData.m_hideY) {
               GL11.glColor3f(0.0F, var1.m_gizmoAxis == UI3DScene.Axis.Y ? 1.0F : 0.5F, 0.0F);
               GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
               GL11.glTranslatef(0.0F, 0.0F, var6);
               this.cylinder.draw(var4 / 2.0F, var4 / 2.0F, var5, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, var5);
               this.cylinder.draw(var4 / 2.0F * 2.0F, 0.0F, 0.1F * var7, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, -var6 - var5);
               GL11.glRotated(90.0, 1.0, 0.0, 0.0);
            }

            if (!var1.m_translateGizmoRenderData.m_hideZ) {
               GL11.glColor3f(0.0F, 0.0F, var1.m_gizmoAxis == UI3DScene.Axis.Z ? 1.0F : 0.5F);
               GL11.glTranslatef(0.0F, 0.0F, var6);
               this.cylinder.draw(var4 / 2.0F, var4 / 2.0F, var5, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, var5);
               this.cylinder.draw(var4 / 2.0F * 2.0F, 0.0F, 0.1F * var7, 8, 1);
               GL11.glTranslatef(0.0F, 0.0F, -var6 - var5);
            }

            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            PZGLUtil.popMatrix(5888);
            this.renderLineToOrigin();
         }
      }
   }

   private abstract static class SceneObject {
      final UI3DScene m_scene;
      final String m_id;
      boolean m_visible = true;
      final Vector3f m_translate = new Vector3f();
      final Vector3f m_rotate = new Vector3f();
      SceneObject m_parent;
      String m_attachment;
      String m_parentAttachment;
      boolean m_autoRotate = false;
      float m_autoRotateAngle = 0.0F;

      SceneObject(UI3DScene var1, String var2) {
         this.m_scene = var1;
         this.m_id = var2;
      }

      abstract SceneObjectRenderData renderMain();

      Matrix4f getLocalTransform(Matrix4f var1) {
         SceneModel var2 = (SceneModel)Type.tryCastTo(this, SceneModel.class);
         if (var2 != null && var2.m_useWorldAttachment) {
            var1.translation(-this.m_translate.x, this.m_translate.y, this.m_translate.z);
            var1.scale(-1.5F, 1.5F, 1.5F);
         } else {
            var1.translation(this.m_translate);
         }

         float var3 = this.m_rotate.y;
         if (this.m_autoRotate) {
            var3 += this.m_autoRotateAngle;
         }

         var1.rotateXYZ(this.m_rotate.x * 0.017453292F, var3 * 0.017453292F, this.m_rotate.z * 0.017453292F);
         if (this.m_attachment != null) {
            Matrix4f var4 = this.getAttachmentTransform(this.m_attachment, UI3DScene.allocMatrix4f());
            var4.invert();
            var1.mul(var4);
            UI3DScene.releaseMatrix4f(var4);
         }

         return var1;
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         this.getLocalTransform(var1);
         if (this.m_parent != null) {
            Matrix4f var2;
            if (this.m_parentAttachment != null) {
               var2 = this.m_parent.getAttachmentTransform(this.m_parentAttachment, UI3DScene.allocMatrix4f());
               var2.mul(var1, var1);
               UI3DScene.releaseMatrix4f(var2);
            }

            var2 = this.m_parent.getGlobalTransform(UI3DScene.allocMatrix4f());
            var2.mul(var1, var1);
            UI3DScene.releaseMatrix4f(var2);
         }

         return var1;
      }

      Matrix4f getAttachmentTransform(String var1, Matrix4f var2) {
         var2.identity();
         return var2;
      }
   }

   private static final class OriginAttachment extends SceneObject {
      SceneObject m_object;
      String m_attachmentName;

      OriginAttachment(UI3DScene var1) {
         super(var1, "OriginAttachment");
      }

      SceneObjectRenderData renderMain() {
         return null;
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         return this.m_object.getAttachmentTransform(this.m_attachmentName, var1);
      }
   }

   private static final class OriginBone extends SceneObject {
      SceneCharacter m_character;
      String m_boneName;

      OriginBone(UI3DScene var1) {
         super(var1, "OriginBone");
      }

      SceneObjectRenderData renderMain() {
         return null;
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         return this.m_character.getBoneMatrix(this.m_boneName, var1);
      }
   }

   private static final class OriginGizmo extends SceneObject {
      OriginGizmo(UI3DScene var1) {
         super(var1, "OriginGizmo");
      }

      SceneObjectRenderData renderMain() {
         return null;
      }
   }

   private final class GridPlaneDrawer extends TextureDraw.GenericDrawer {
      final UI3DScene m_scene;

      GridPlaneDrawer(UI3DScene var2) {
         this.m_scene = var2;
      }

      public void render() {
         StateData var1 = UI3DScene.this.stateDataRender();
         PZGLUtil.pushAndLoadMatrix(5889, var1.m_projection);
         PZGLUtil.pushAndLoadMatrix(5888, var1.m_modelView);
         GL11.glPushAttrib(2048);
         GL11.glViewport(UI3DScene.this.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(), UI3DScene.this.getWidth().intValue(), UI3DScene.this.getHeight().intValue());
         Objects.requireNonNull(this.m_scene);
         float var2 = 5.0F;
         UI3DScene.vboLines.setMode(4);
         UI3DScene.vboLines.setDepthTest(true);
         if (this.m_scene.m_gridPlane == UI3DScene.GridPlane.XZ) {
            UI3DScene.vboLines.addTriangle(-var2, 0.0F, -var2, var2, 0.0F, -var2, -var2, 0.0F, var2, 0.5F, 0.5F, 0.5F, 1.0F);
            UI3DScene.vboLines.addTriangle(var2, 0.0F, var2, -var2, 0.0F, var2, var2, 0.0F, -var2, 0.5F, 0.5F, 0.5F, 1.0F);
         }

         UI3DScene.vboLines.setMode(1);
         UI3DScene.vboLines.setDepthTest(false);
         GL11.glPopAttrib();
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
      }
   }

   private final class OverlaysDrawer extends TextureDraw.GenericDrawer {
      private OverlaysDrawer() {
      }

      void init() {
         StateData var1 = UI3DScene.this.stateDataMain();
         UI3DScene.s_aabbPool.release((List)var1.m_aabb);
         var1.m_aabb.clear();

         int var2;
         for(var2 = 0; var2 < UI3DScene.this.m_aabb.size(); ++var2) {
            AABB var3 = (AABB)UI3DScene.this.m_aabb.get(var2);
            var1.m_aabb.add(((AABB)UI3DScene.s_aabbPool.alloc()).set(var3));
         }

         UI3DScene.s_box3DPool.release((List)var1.m_box3D);
         var1.m_box3D.clear();

         for(var2 = 0; var2 < UI3DScene.this.m_box3D.size(); ++var2) {
            Box3D var4 = (Box3D)UI3DScene.this.m_box3D.get(var2);
            var1.m_box3D.add(((Box3D)UI3DScene.s_box3DPool.alloc()).set(var4));
         }

         UI3DScene.s_posRotPool.release((List)var1.m_axes);
         var1.m_axes.clear();

         for(var2 = 0; var2 < UI3DScene.this.m_axes.size(); ++var2) {
            PositionRotation var5 = (PositionRotation)UI3DScene.this.m_axes.get(var2);
            var1.m_axes.add(((PositionRotation)UI3DScene.s_posRotPool.alloc()).set(var5));
         }

      }

      public void render() {
         StateData var1 = UI3DScene.this.stateDataRender();
         PZGLUtil.pushAndLoadMatrix(5889, var1.m_projection);
         PZGLUtil.pushAndLoadMatrix(5888, var1.m_modelView);
         GL11.glPushAttrib(2048);
         GL11.glViewport(UI3DScene.this.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(), UI3DScene.this.getWidth().intValue(), UI3DScene.this.getHeight().intValue());
         UI3DScene.vboLines.setOffset(0.0F, 0.0F, 0.0F);
         if (UI3DScene.this.m_bDrawGrid) {
            UI3DScene.this.renderGrid();
         }

         int var2;
         for(var2 = 0; var2 < var1.m_aabb.size(); ++var2) {
            AABB var3 = (AABB)var1.m_aabb.get(var2);
            UI3DScene.this.renderAABB(var3.x, var3.y, var3.z, var3.w, var3.h, var3.L, var3.r, var3.g, var3.b);
         }

         for(var2 = 0; var2 < var1.m_box3D.size(); ++var2) {
            Box3D var4 = (Box3D)var1.m_box3D.get(var2);
            UI3DScene.this.renderBox3D(var4.x, var4.y, var4.z, var4.w, var4.h, var4.L, var4.rx, var4.ry, var4.rz, var4.r, var4.g, var4.b);
         }

         for(var2 = 0; var2 < var1.m_axes.size(); ++var2) {
            UI3DScene.this.renderAxis((PositionRotation)var1.m_axes.get(var2));
         }

         UI3DScene.vboLines.flush();
         if (var1.m_gizmo != null) {
            var1.m_gizmo.render();
         }

         UI3DScene.vboLines.flush();
         GL11.glPopAttrib();
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
      }
   }

   private static class SceneObjectRenderData {
      SceneObject m_object;
      final Matrix4f m_transform = new Matrix4f();
      private static final ObjectPool<SceneObjectRenderData> s_pool = new ObjectPool(SceneObjectRenderData::new);

      private SceneObjectRenderData() {
      }

      SceneObjectRenderData init(SceneObject var1) {
         this.m_object = var1;
         var1.getGlobalTransform(this.m_transform);
         return this;
      }

      void release() {
         s_pool.release((Object)this);
      }
   }

   private abstract class Gizmo {
      float LENGTH = 0.5F;
      float THICKNESS = 0.05F;
      boolean m_visible = false;

      private Gizmo() {
      }

      abstract Axis hitTest(float var1, float var2);

      abstract void startTracking(float var1, float var2, Axis var3);

      abstract void updateTracking(float var1, float var2);

      abstract void stopTracking();

      abstract void render();

      Vector3f getPointOnAxis(float var1, float var2, Axis var3, Matrix4f var4, Vector3f var5) {
         StateData var6 = UI3DScene.this.stateDataMain();
         var2 = (float)UI3DScene.this.screenHeight() - var2;
         Ray var7 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
         Ray var8 = UI3DScene.allocRay();
         var4.transformPosition(var8.origin.set(0.0F, 0.0F, 0.0F));
         switch (var3) {
            case X:
               var8.direction.set(1.0F, 0.0F, 0.0F);
               break;
            case Y:
               var8.direction.set(0.0F, 1.0F, 0.0F);
               break;
            case Z:
               var8.direction.set(0.0F, 0.0F, 1.0F);
         }

         var4.transformDirection(var8.direction).normalize();
         UI3DScene.this.closest_distance_between_lines(var8, var7);
         UI3DScene.releaseRay(var7);
         var5.set(var8.direction).mul(var8.t).add(var8.origin);
         UI3DScene.releaseRay(var8);
         return var5;
      }

      boolean hitTestRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         float var9 = UI3DScene.this.sceneToUIX(var3, var4, var5);
         float var10 = UI3DScene.this.sceneToUIY(var3, var4, var5);
         float var11 = UI3DScene.this.sceneToUIX(var6, var7, var8);
         float var12 = UI3DScene.this.sceneToUIY(var6, var7, var8);
         float var13 = this.THICKNESS / 2.0F * UI3DScene.this.zoomMult();
         float var14 = this.THICKNESS / 2.0F * UI3DScene.this.zoomMult();
         float var15 = Math.min(var9 - var13, var11 - var13);
         float var16 = Math.max(var9 + var13, var11 + var13);
         float var17 = Math.min(var10 - var14, var12 - var14);
         float var18 = Math.max(var10 + var14, var12 + var14);
         return var1 >= var15 && var2 >= var17 && var1 < var16 && var2 < var18;
      }

      void renderLineToOrigin() {
         StateData var1 = UI3DScene.this.stateDataRender();
         if (var1.m_hasGizmoOrigin) {
            UI3DScene.this.renderAxis(var1.m_gizmoTranslate, var1.m_gizmoRotate);
            Vector3f var2 = var1.m_gizmoTranslate;
            UI3DScene.vboLines.flush();
            Matrix4f var3 = UI3DScene.allocMatrix4f();
            var3.set(var1.m_modelView);
            var3.mul(var1.m_gizmoParentTransform);
            var3.mul(var1.m_gizmoOriginTransform);
            var3.mul(var1.m_gizmoChildTransform);
            PZGLUtil.pushAndLoadMatrix(5888, var3);
            UI3DScene.releaseMatrix4f(var3);
            UI3DScene.vboLines.setLineWidth(1.0F);
            UI3DScene.vboLines.addLine(var2.x, var2.y, var2.z, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            UI3DScene.vboLines.flush();
            PZGLUtil.popMatrix(5888);
         }
      }
   }

   static enum Axis {
      None,
      X,
      Y,
      Z;

      private Axis() {
      }
   }

   public static final class Plane {
      public final Vector3f point = new Vector3f();
      public final Vector3f normal = new Vector3f();

      public Plane() {
      }

      public Plane(Vector3f var1, Vector3f var2) {
         this.point.set(var2);
         this.normal.set(var1);
      }

      public Plane set(Vector3f var1, Vector3f var2) {
         this.point.set(var2);
         this.normal.set(var1);
         return this;
      }
   }

   public static final class Ray {
      public final Vector3f origin = new Vector3f();
      public final Vector3f direction = new Vector3f();
      public float t;

      public Ray() {
      }

      Ray(Ray var1) {
         this.origin.set(var1.origin);
         this.direction.set(var1.direction);
         this.t = var1.t;
      }
   }

   private static final class SceneCharacter extends SceneObject {
      final AnimatedModel m_animatedModel = new AnimatedModel();
      boolean m_bShowBones = false;
      boolean m_bClearDepthBuffer = true;
      boolean m_bUseDeferredMovement = false;

      SceneCharacter(UI3DScene var1, String var2) {
         super(var1, var2);
         this.m_animatedModel.setAnimSetName("player-vehicle");
         this.m_animatedModel.setState("idle");
         this.m_animatedModel.setOutfitName("Naked", false, false);
         this.m_animatedModel.setVisual(new HumanVisual(this.m_animatedModel));
         this.m_animatedModel.getHumanVisual().setHairModel("Bald");
         this.m_animatedModel.getHumanVisual().setBeardModel("");
         this.m_animatedModel.getHumanVisual().setSkinTextureIndex(0);
         this.m_animatedModel.setAlpha(0.5F);
         this.m_animatedModel.setAnimate(false);
      }

      SceneObjectRenderData renderMain() {
         this.m_animatedModel.update();
         CharacterRenderData var1 = (CharacterRenderData)UI3DScene.CharacterRenderData.s_pool.alloc();
         var1.initCharacter(this);
         SpriteRenderer.instance.drawGeneric(var1.m_drawer);
         return var1;
      }

      Matrix4f getLocalTransform(Matrix4f var1) {
         var1.identity();
         var1.rotateY(3.1415927F);
         var1.translate(-this.m_translate.x, this.m_translate.y, this.m_translate.z);
         var1.scale(-1.5F, 1.5F, 1.5F);
         float var2 = this.m_rotate.y;
         if (this.m_autoRotate) {
            var2 += this.m_autoRotateAngle;
         }

         var1.rotateXYZ(this.m_rotate.x * 0.017453292F, var2 * 0.017453292F, this.m_rotate.z * 0.017453292F);
         if (this.m_animatedModel.getAnimationPlayer().getMultiTrack().getTracks().isEmpty()) {
            return var1;
         } else {
            if (this.m_bUseDeferredMovement) {
               AnimationMultiTrack var3 = this.m_animatedModel.getAnimationPlayer().getMultiTrack();
               float var4 = ((AnimationTrack)var3.getTracks().get(0)).getCurrentDeferredRotation();
               org.lwjgl.util.vector.Vector3f var5 = new org.lwjgl.util.vector.Vector3f();
               ((AnimationTrack)var3.getTracks().get(0)).getCurrentDeferredPosition(var5);
               var1.translate(var5.x, var5.y, var5.z);
            }

            return var1;
         }
      }

      Matrix4f getAttachmentTransform(String var1, Matrix4f var2) {
         var2.identity();
         boolean var3 = this.m_animatedModel.isFemale();
         ModelScript var4 = ScriptManager.instance.getModelScript(var3 ? "FemaleBody" : "MaleBody");
         if (var4 == null) {
            return var2;
         } else {
            ModelAttachment var5 = var4.getAttachmentById(var1);
            if (var5 == null) {
               return var2;
            } else {
               var2.translation(var5.getOffset());
               Vector3f var6 = var5.getRotate();
               var2.rotateXYZ(var6.x * 0.017453292F, var6.y * 0.017453292F, var6.z * 0.017453292F);
               if (var5.getBone() != null) {
                  Matrix4f var7 = this.getBoneMatrix(var5.getBone(), UI3DScene.allocMatrix4f());
                  var7.mul(var2, var2);
                  UI3DScene.releaseMatrix4f(var7);
               }

               return var2;
            }
         }
      }

      int hitTestBone(int var1, Ray var2, Ray var3, Matrix4f var4) {
         AnimationPlayer var5 = this.m_animatedModel.getAnimationPlayer();
         SkinningData var6 = var5.getSkinningData();
         int var7 = (Integer)var6.SkeletonHierarchy.get(var1);
         if (var7 == -1) {
            return -1;
         } else {
            org.lwjgl.util.vector.Matrix4f var8 = var5.modelTransforms[var7];
            var2.origin.set(var8.m03, var8.m13, var8.m23);
            var4.transformPosition(var2.origin);
            var8 = var5.modelTransforms[var1];
            Vector3f var9 = UI3DScene.allocVector3f();
            var9.set(var8.m03, var8.m13, var8.m23);
            var4.transformPosition(var9);
            var2.direction.set(var9).sub(var2.origin);
            float var10 = var2.direction.length();
            var2.direction.normalize();
            this.m_scene.closest_distance_between_lines(var3, var2);
            float var11 = this.m_scene.sceneToUIX(var3.origin.x + var3.direction.x * var3.t, var3.origin.y + var3.direction.y * var3.t, var3.origin.z + var3.direction.z * var3.t);
            float var12 = this.m_scene.sceneToUIY(var3.origin.x + var3.direction.x * var3.t, var3.origin.y + var3.direction.y * var3.t, var3.origin.z + var3.direction.z * var3.t);
            float var13 = this.m_scene.sceneToUIX(var2.origin.x + var2.direction.x * var2.t, var2.origin.y + var2.direction.y * var2.t, var2.origin.z + var2.direction.z * var2.t);
            float var14 = this.m_scene.sceneToUIY(var2.origin.x + var2.direction.x * var2.t, var2.origin.y + var2.direction.y * var2.t, var2.origin.z + var2.direction.z * var2.t);
            int var15 = -1;
            float var16 = 10.0F;
            float var17 = (float)Math.sqrt(Math.pow((double)(var13 - var11), 2.0) + Math.pow((double)(var14 - var12), 2.0));
            if (var17 < var16) {
               if (var2.t >= 0.0F && var2.t < var10 * 0.5F) {
                  var15 = var7;
               } else if (var2.t >= var10 * 0.5F && var2.t < var10) {
                  var15 = var1;
               }
            }

            UI3DScene.releaseVector3f(var9);
            return var15;
         }
      }

      String pickBone(float var1, float var2) {
         if (this.m_animatedModel.getAnimationPlayer().modelTransforms == null) {
            return "";
         } else {
            var2 = (float)this.m_scene.screenHeight() - var2;
            Ray var3 = this.m_scene.getCameraRay(var1, var2, UI3DScene.allocRay());
            Matrix4f var4 = UI3DScene.allocMatrix4f();
            this.getLocalTransform(var4);
            Ray var5 = UI3DScene.allocRay();
            int var6 = -1;

            for(int var7 = 0; var7 < this.m_animatedModel.getAnimationPlayer().modelTransforms.length; ++var7) {
               var6 = this.hitTestBone(var7, var5, var3, var4);
               if (var6 != -1) {
                  break;
               }
            }

            UI3DScene.releaseRay(var5);
            UI3DScene.releaseRay(var3);
            UI3DScene.releaseMatrix4f(var4);
            return var6 == -1 ? "" : this.m_animatedModel.getAnimationPlayer().getSkinningData().getBoneAt(var6).Name;
         }
      }

      Matrix4f getBoneMatrix(String var1, Matrix4f var2) {
         var2.identity();
         if (this.m_animatedModel.getAnimationPlayer().modelTransforms == null) {
            return var2;
         } else {
            SkinningBone var3 = this.m_animatedModel.getAnimationPlayer().getSkinningData().getBone(var1);
            if (var3 == null) {
               return var2;
            } else {
               var2 = PZMath.convertMatrix(this.m_animatedModel.getAnimationPlayer().modelTransforms[var3.Index], var2);
               var2.transpose();
               return var2;
            }
         }
      }

      PositionRotation getBoneAxis(String var1, PositionRotation var2) {
         Matrix4f var3 = UI3DScene.allocMatrix4f().identity();
         var3.getTranslation(var2.pos);
         UI3DScene.releaseMatrix4f(var3);
         Quaternionf var4 = var3.getUnnormalizedRotation(UI3DScene.allocQuaternionf());
         var4.getEulerAnglesXYZ(var2.rot);
         UI3DScene.releaseQuaternionf(var4);
         return var2;
      }
   }

   private static final class SceneModel extends SceneObject {
      ModelScript m_modelScript;
      Model m_model;
      boolean m_useWorldAttachment = false;
      boolean m_weaponRotationHack = false;

      SceneModel(UI3DScene var1, String var2, ModelScript var3, Model var4) {
         super(var1, var2);
         Objects.requireNonNull(var3);
         Objects.requireNonNull(var4);
         this.m_modelScript = var3;
         this.m_model = var4;
      }

      SceneObjectRenderData renderMain() {
         if (!this.m_model.isReady()) {
            return null;
         } else {
            ModelRenderData var1 = (ModelRenderData)UI3DScene.ModelRenderData.s_pool.alloc();
            var1.initModel(this);
            SpriteRenderer.instance.drawGeneric(var1.m_drawer);
            return var1;
         }
      }

      Matrix4f getLocalTransform(Matrix4f var1) {
         super.getLocalTransform(var1);
         return var1;
      }

      Matrix4f getAttachmentTransform(String var1, Matrix4f var2) {
         var2.identity();
         ModelAttachment var3 = this.m_modelScript.getAttachmentById(var1);
         if (var3 == null) {
            return var2;
         } else {
            var2.translation(var3.getOffset());
            Vector3f var4 = var3.getRotate();
            var2.rotateXYZ(var4.x * 0.017453292F, var4.y * 0.017453292F, var4.z * 0.017453292F);
            return var2;
         }
      }
   }

   private static final class SceneVehicle extends SceneObject {
      String m_scriptName = "Base.ModernCar";
      VehicleScript m_script;
      Model m_model;

      SceneVehicle(UI3DScene var1, String var2) {
         super(var1, var2);
         this.setScriptName("Base.ModernCar");
      }

      SceneObjectRenderData renderMain() {
         if (this.m_script == null) {
            this.m_model = null;
            return null;
         } else {
            String var1 = this.m_script.getModel().file;
            this.m_model = ModelManager.instance.getLoadedModel(var1);
            if (this.m_model == null) {
               return null;
            } else {
               if (this.m_script.getSkinCount() > 0) {
                  this.m_model.tex = Texture.getSharedTexture("media/textures/" + this.m_script.getSkin(0).texture + ".png");
               }

               VehicleRenderData var2 = (VehicleRenderData)UI3DScene.VehicleRenderData.s_pool.alloc();
               var2.initVehicle(this);
               SetModelCamera var3 = (SetModelCamera)UI3DScene.s_SetModelCameraPool.alloc();
               SpriteRenderer.instance.drawGeneric(var3.init(this.m_scene.m_VehicleSceneModelCamera, var2));
               SpriteRenderer.instance.drawGeneric(var2.m_drawer);
               return var2;
            }
         }
      }

      void setScriptName(String var1) {
         this.m_scriptName = var1;
         this.m_script = ScriptManager.instance.getVehicle(var1);
      }
   }

   private static final class PositionRotation {
      final Vector3f pos = new Vector3f();
      final Vector3f rot = new Vector3f();

      private PositionRotation() {
      }

      PositionRotation set(PositionRotation var1) {
         this.pos.set(var1.pos);
         this.rot.set(var1.rot);
         return this;
      }

      PositionRotation set(float var1, float var2, float var3) {
         this.pos.set(var1, var2, var3);
         this.rot.set(0.0F, 0.0F, 0.0F);
         return this;
      }

      PositionRotation set(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.pos.set(var1, var2, var3);
         this.rot.set(var4, var5, var6);
         return this;
      }
   }

   private static final class AABB {
      float x;
      float y;
      float z;
      float w;
      float h;
      float L;
      float r;
      float g;
      float b;

      private AABB() {
      }

      AABB set(AABB var1) {
         return this.set(var1.x, var1.y, var1.z, var1.w, var1.h, var1.L, var1.r, var1.g, var1.b);
      }

      AABB set(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.w = var4;
         this.h = var5;
         this.L = var6;
         this.r = var7;
         this.g = var8;
         this.b = var9;
         return this;
      }
   }

   private static final class Box3D {
      float x;
      float y;
      float z;
      float w;
      float h;
      float L;
      float rx;
      float ry;
      float rz;
      float r;
      float g;
      float b;

      private Box3D() {
      }

      Box3D set(Box3D var1) {
         return this.set(var1.x, var1.y, var1.z, var1.w, var1.h, var1.L, var1.rx, var1.ry, var1.rz, var1.r, var1.g, var1.b);
      }

      Box3D set(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.w = var4;
         this.h = var5;
         this.L = var6;
         this.rx = var7;
         this.ry = var8;
         this.rz = var9;
         this.r = var10;
         this.g = var11;
         this.b = var12;
         return this;
      }
   }

   private static final class Circle {
      final Vector3f center = new Vector3f();
      final Vector3f orientation = new Vector3f();
      float radius = 1.0F;

      private Circle() {
      }
   }

   private static final class VehicleDrawer extends TextureDraw.GenericDrawer {
      SceneVehicle m_vehicle;
      VehicleRenderData m_renderData;
      boolean bRendered;
      final float[] fzeroes = new float[16];
      final Vector3f paintColor = new Vector3f(0.0F, 0.5F, 0.5F);
      final Matrix4f IDENTITY = new Matrix4f();

      private VehicleDrawer() {
      }

      public void init(SceneVehicle var1, VehicleRenderData var2) {
         this.m_vehicle = var1;
         this.m_renderData = var2;
         this.bRendered = false;
      }

      public void render() {
         for(int var1 = 0; var1 < this.m_renderData.m_models.size(); ++var1) {
            GL11.glPushAttrib(1048575);
            GL11.glPushClientAttrib(-1);
            this.render(var1);
            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
            Texture.lastTextureID = -1;
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            SpriteRenderer.ringBuffer.restoreVBOs = true;
         }

      }

      private void render(int var1) {
         this.m_renderData.m_transform.set((Matrix4fc)this.m_renderData.m_transforms.get(var1));
         ModelCamera.instance.Begin();
         Model var2 = (Model)this.m_renderData.m_models.get(var1);
         boolean var3 = var2.bStatic;
         Shader var4;
         if (Core.bDebug && DebugOptions.instance.ModelRenderWireframe.getValue()) {
            GL11.glPolygonMode(1032, 6913);
            GL11.glEnable(2848);
            GL11.glLineWidth(0.75F);
            var4 = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", var3);
            if (var4 != null) {
               var4.Start();
               var4.setTransformMatrix(this.IDENTITY.identity(), false);
               var2.Mesh.Draw(var4);
               var4.End();
            }

            GL11.glDisable(2848);
            ModelCamera.instance.End();
         } else {
            var4 = var2.Effect;
            int var5;
            if (var4 != null && var4.isVehicleShader()) {
               GL11.glDepthFunc(513);
               GL11.glDepthMask(true);
               GL11.glDepthRange(0.0, 1.0);
               GL11.glEnable(2929);
               GL11.glColor3f(1.0F, 1.0F, 1.0F);
               var4.Start();
               if (var2.tex != null) {
                  var4.setTexture(var2.tex, "Texture0", 0);
                  GL11.glTexEnvi(8960, 8704, 7681);
                  if (this.m_vehicle.m_script.getSkinCount() > 0 && this.m_vehicle.m_script.getSkin(0).textureMask != null) {
                     Texture var6 = Texture.getSharedTexture("media/textures/" + this.m_vehicle.m_script.getSkin(0).textureMask + ".png");
                     var4.setTexture(var6, "TextureMask", 2);
                     GL11.glTexEnvi(8960, 8704, 7681);
                  }
               }

               var4.setDepthBias(0.0F);
               var4.setAmbient(1.0F);
               var4.setLightingAmount(1.0F);
               var4.setHueShift(0.0F);
               var4.setTint(1.0F, 1.0F, 1.0F);
               var4.setAlpha(1.0F);

               for(var5 = 0; var5 < 5; ++var5) {
                  var4.setLight(var5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
               }

               var4.setTextureUninstall1(this.fzeroes);
               var4.setTextureUninstall2(this.fzeroes);
               var4.setTextureLightsEnables2(this.fzeroes);
               var4.setTextureDamage1Enables1(this.fzeroes);
               var4.setTextureDamage1Enables2(this.fzeroes);
               var4.setTextureDamage2Enables1(this.fzeroes);
               var4.setTextureDamage2Enables2(this.fzeroes);
               var4.setMatrixBlood1(this.fzeroes, this.fzeroes);
               var4.setMatrixBlood2(this.fzeroes, this.fzeroes);
               var4.setTextureRustA(0.0F);
               var4.setTexturePainColor(this.paintColor, 1.0F);
               var4.setTransformMatrix(this.IDENTITY.identity(), false);
               var2.Mesh.Draw(var4);
               var4.End();
            } else if (var4 != null && var2.Mesh != null && var2.Mesh.isReady()) {
               GL11.glDepthFunc(513);
               GL11.glDepthMask(true);
               GL11.glDepthRange(0.0, 1.0);
               GL11.glEnable(2929);
               GL11.glColor3f(1.0F, 1.0F, 1.0F);
               var4.Start();
               if (var2.tex != null) {
                  var4.setTexture(var2.tex, "Texture", 0);
               }

               var4.setDepthBias(0.0F);
               var4.setAmbient(1.0F);
               var4.setLightingAmount(1.0F);
               var4.setHueShift(0.0F);
               var4.setTint(1.0F, 1.0F, 1.0F);
               var4.setAlpha(1.0F);

               for(var5 = 0; var5 < 5; ++var5) {
                  var4.setLight(var5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
               }

               var4.setTransformMatrix(this.IDENTITY.identity(), false);
               var2.Mesh.Draw(var4);
               var4.End();
            }

            ModelCamera.instance.End();
            this.bRendered = true;
         }
      }

      public void postRender() {
      }
   }

   private static final class ModelDrawer extends TextureDraw.GenericDrawer {
      SceneModel m_model;
      ModelRenderData m_renderData;
      boolean bRendered;

      private ModelDrawer() {
      }

      public void init(SceneModel var1, ModelRenderData var2) {
         this.m_model = var1;
         this.m_renderData = var2;
         this.bRendered = false;
      }

      public void render() {
         StateData var1 = this.m_model.m_scene.stateDataRender();
         PZGLUtil.pushAndLoadMatrix(5889, var1.m_projection);
         PZGLUtil.pushAndLoadMatrix(5888, var1.m_modelView);
         Model var2 = this.m_model.m_model;
         Shader var3 = var2.Effect;
         if (var3 != null && var2.Mesh != null && var2.Mesh.isReady()) {
            GL11.glPushAttrib(1048575);
            GL11.glPushClientAttrib(-1);
            UI3DScene var4 = this.m_renderData.m_object.m_scene;
            GL11.glViewport(var4.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - var4.getAbsoluteY().intValue() - var4.getHeight().intValue(), var4.getWidth().intValue(), var4.getHeight().intValue());
            GL11.glDepthFunc(513);
            GL11.glDepthMask(true);
            GL11.glDepthRange(0.0, 1.0);
            GL11.glEnable(2929);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            var3.Start();
            if (var2.tex != null) {
               var3.setTexture(var2.tex, "Texture", 0);
            }

            var3.setDepthBias(0.0F);
            var3.setAmbient(1.0F);
            var3.setLightingAmount(1.0F);
            var3.setHueShift(0.0F);
            var3.setTint(1.0F, 1.0F, 1.0F);
            var3.setAlpha(1.0F);

            for(int var5 = 0; var5 < 5; ++var5) {
               var3.setLight(var5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
            }

            var3.setTransformMatrix(this.m_renderData.m_transform, false);
            var2.Mesh.Draw(var3);
            var3.End();
            if (Core.bDebug) {
            }

            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
            Texture.lastTextureID = -1;
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            SpriteRenderer.ringBuffer.restoreVBOs = true;
         }

         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
         this.bRendered = true;
      }

      public void postRender() {
      }
   }

   private static final class CharacterDrawer extends TextureDraw.GenericDrawer {
      SceneCharacter m_character;
      CharacterRenderData m_renderData;
      boolean bRendered;

      private CharacterDrawer() {
      }

      public void init(SceneCharacter var1, CharacterRenderData var2) {
         this.m_character = var1;
         this.m_renderData = var2;
         this.bRendered = false;
         this.m_character.m_animatedModel.renderMain();
      }

      public void render() {
         if (this.m_character.m_bClearDepthBuffer) {
            GL11.glClear(256);
         }

         boolean var1 = DebugOptions.instance.ModelRenderBones.getValue();
         DebugOptions.instance.ModelRenderBones.setValue(this.m_character.m_bShowBones);
         this.m_character.m_scene.m_CharacterSceneModelCamera.m_renderData = this.m_renderData;
         this.m_character.m_animatedModel.DoRender(this.m_character.m_scene.m_CharacterSceneModelCamera);
         DebugOptions.instance.ModelRenderBones.setValue(var1);
         this.bRendered = true;
         GL11.glDepthMask(true);
      }

      public void postRender() {
         this.m_character.m_animatedModel.postRender(this.bRendered);
      }
   }

   private static final class TranslateGizmoRenderData {
      boolean m_hideX;
      boolean m_hideY;
      boolean m_hideZ;

      private TranslateGizmoRenderData() {
      }
   }

   public static final class PlaneObjectPool extends ObjectPool<Plane> {
      int allocated = 0;

      public PlaneObjectPool() {
         super(Plane::new);
      }

      protected Plane makeObject() {
         ++this.allocated;
         return (Plane)super.makeObject();
      }
   }

   public static final class RayObjectPool extends ObjectPool<Ray> {
      int allocated = 0;

      public RayObjectPool() {
         super(Ray::new);
      }

      protected Ray makeObject() {
         ++this.allocated;
         return (Ray)super.makeObject();
      }
   }

   private static final class SetModelCamera extends TextureDraw.GenericDrawer {
      SceneModelCamera m_camera;
      SceneObjectRenderData m_renderData;

      private SetModelCamera() {
      }

      SetModelCamera init(SceneModelCamera var1, SceneObjectRenderData var2) {
         this.m_camera = var1;
         this.m_renderData = var2;
         return this;
      }

      public void render() {
         this.m_camera.m_renderData = this.m_renderData;
         ModelCamera.instance = this.m_camera;
      }

      public void postRender() {
         UI3DScene.s_SetModelCameraPool.release((Object)this);
      }
   }

   private abstract class SceneModelCamera extends ModelCamera {
      SceneObjectRenderData m_renderData;

      private SceneModelCamera() {
      }
   }

   private static class VehicleRenderData extends SceneObjectRenderData {
      final ArrayList<Model> m_models = new ArrayList();
      final ArrayList<Matrix4f> m_transforms = new ArrayList();
      final VehicleDrawer m_drawer = new VehicleDrawer();
      private static final ObjectPool<VehicleRenderData> s_pool = new ObjectPool(VehicleRenderData::new);

      private VehicleRenderData() {
      }

      SceneObjectRenderData initVehicle(SceneVehicle var1) {
         super.init(var1);
         this.m_models.clear();
         BaseVehicle.Matrix4fObjectPool var2 = (BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get();
         var2.release(this.m_transforms);
         this.m_transforms.clear();
         VehicleScript var3 = var1.m_script;
         if (var3.getModel() == null) {
            return null;
         } else {
            this.initVehicleModel(var1);
            float var4 = var3.getModelScale();
            Vector3f var5 = var3.getModel().getOffset();
            Matrix4f var6 = UI3DScene.allocMatrix4f();
            var6.translationRotateScale(var5.x * 1.0F, var5.y, var5.z, 0.0F, 0.0F, 0.0F, 1.0F, var4);
            this.m_transform.mul(var6, var6);

            for(int var7 = 0; var7 < var3.getPartCount(); ++var7) {
               VehicleScript.Part var8 = var3.getPart(var7);
               if (var8.wheel != null) {
                  this.initWheelModel(var1, var8, var6);
               }
            }

            UI3DScene.releaseMatrix4f(var6);
            this.m_drawer.init(var1, this);
            return this;
         }
      }

      private void initVehicleModel(SceneVehicle var1) {
         VehicleScript var2 = var1.m_script;
         float var3 = var2.getModelScale();
         float var4 = 1.0F;
         ModelScript var5 = ScriptManager.instance.getModelScript(var2.getModel().file);
         if (var5 != null && var5.scale != 1.0F) {
            var4 = var5.scale;
         }

         float var6 = 1.0F;
         if (var5 != null) {
            var6 = var5.invertX ? -1.0F : 1.0F;
         }

         var6 *= -1.0F;
         Quaternionf var7 = UI3DScene.allocQuaternionf();
         Matrix4f var8 = UI3DScene.allocMatrix4f();
         Vector3f var9 = var2.getModel().getRotate();
         var7.rotationXYZ(var9.x * 0.017453292F, var9.y * 0.017453292F, var9.z * 0.017453292F);
         Vector3f var10 = var2.getModel().getOffset();
         var8.translationRotateScale(var10.x * 1.0F, var10.y, var10.z, var7.x, var7.y, var7.z, var7.w, var3 * var4 * var6, var3 * var4, var3 * var4);
         if (var1.m_model.Mesh != null && var1.m_model.Mesh.isReady() && var1.m_model.Mesh.m_transform != null) {
            var1.m_model.Mesh.m_transform.transpose();
            var8.mul(var1.m_model.Mesh.m_transform);
            var1.m_model.Mesh.m_transform.transpose();
         }

         this.m_transform.mul(var8, var8);
         UI3DScene.releaseQuaternionf(var7);
         this.m_models.add(var1.m_model);
         this.m_transforms.add(var8);
      }

      private void initWheelModel(SceneVehicle var1, VehicleScript.Part var2, Matrix4f var3) {
         VehicleScript var4 = var1.m_script;
         float var5 = var4.getModelScale();
         VehicleScript.Wheel var6 = var4.getWheelById(var2.wheel);
         if (var6 != null && !var2.models.isEmpty()) {
            VehicleScript.Model var7 = (VehicleScript.Model)var2.models.get(0);
            Vector3f var8 = var7.getOffset();
            Vector3f var9 = var7.getRotate();
            Model var10 = ModelManager.instance.getLoadedModel(var7.file);
            if (var10 != null) {
               float var11 = var7.scale;
               float var12 = 1.0F;
               float var13 = 1.0F;
               ModelScript var14 = ScriptManager.instance.getModelScript(var7.file);
               if (var14 != null) {
                  var12 = var14.scale;
                  var13 = var14.invertX ? -1.0F : 1.0F;
               }

               Quaternionf var15 = UI3DScene.allocQuaternionf();
               var15.rotationXYZ(var9.x * 0.017453292F, var9.y * 0.017453292F, var9.z * 0.017453292F);
               Matrix4f var16 = UI3DScene.allocMatrix4f();
               var16.translation(var6.offset.x / var5 * 1.0F, var6.offset.y / var5, var6.offset.z / var5);
               Matrix4f var17 = UI3DScene.allocMatrix4f();
               var17.translationRotateScale(var8.x * 1.0F, var8.y, var8.z, var15.x, var15.y, var15.z, var15.w, var11 * var12 * var13, var11 * var12, var11 * var12);
               var16.mul(var17);
               UI3DScene.releaseMatrix4f(var17);
               var3.mul(var16, var16);
               if (var10.Mesh != null && var10.Mesh.isReady() && var10.Mesh.m_transform != null) {
                  var10.Mesh.m_transform.transpose();
                  var16.mul(var10.Mesh.m_transform);
                  var10.Mesh.m_transform.transpose();
               }

               UI3DScene.releaseQuaternionf(var15);
               this.m_models.add(var10);
               this.m_transforms.add(var16);
            }
         }
      }

      void release() {
         s_pool.release((Object)this);
      }
   }

   private static class ModelRenderData extends SceneObjectRenderData {
      final ModelDrawer m_drawer = new ModelDrawer();
      private static final ObjectPool<ModelRenderData> s_pool = new ObjectPool(ModelRenderData::new);

      private ModelRenderData() {
      }

      SceneObjectRenderData initModel(SceneModel var1) {
         super.init(var1);
         if (var1.m_useWorldAttachment) {
            if (var1.m_weaponRotationHack) {
               this.m_transform.rotateXYZ(0.0F, 3.1415927F, 1.5707964F);
            }

            if (var1.m_modelScript != null) {
               ModelAttachment var2 = var1.m_modelScript.getAttachmentById("world");
               if (var2 != null) {
                  Matrix4f var3 = ModelInstanceRenderData.makeAttachmentTransform(var2, UI3DScene.allocMatrix4f());
                  var3.invert();
                  this.m_transform.mul(var3);
                  UI3DScene.releaseMatrix4f(var3);
               }
            }
         }

         if (var1.m_model.isReady() && var1.m_model.Mesh.m_transform != null) {
            var1.m_model.Mesh.m_transform.transpose();
            this.m_transform.mul(var1.m_model.Mesh.m_transform);
            var1.m_model.Mesh.m_transform.transpose();
         }

         if (var1.m_modelScript != null && var1.m_modelScript.scale != 1.0F) {
            this.m_transform.scale(var1.m_modelScript.scale);
         }

         this.m_drawer.init(var1, this);
         return this;
      }

      void release() {
         s_pool.release((Object)this);
      }
   }

   private static class CharacterRenderData extends SceneObjectRenderData {
      final CharacterDrawer m_drawer = new CharacterDrawer();
      private static final ObjectPool<CharacterRenderData> s_pool = new ObjectPool(CharacterRenderData::new);

      private CharacterRenderData() {
      }

      SceneObjectRenderData initCharacter(SceneCharacter var1) {
         this.m_drawer.init(var1, this);
         super.init(var1);
         return this;
      }

      void release() {
         s_pool.release((Object)this);
      }
   }
}
