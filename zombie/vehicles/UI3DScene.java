package zombie.vehicles;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjglx.BufferUtils;
import org.lwjglx.util.glu.Cylinder;
import org.lwjglx.util.glu.PartialDisk;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.physics.Bullet;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
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
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Mask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.VehicleScript;
import zombie.seating.SeatingManager;
import zombie.tileDepth.CylinderUtils;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryFile;
import zombie.tileDepth.TileGeometryManager;
import zombie.tileDepth.TileGeometryUtils;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.Rasterize;

public final class UI3DScene extends UIElement {
   public static final float Z_SCALE = 0.8164967F;
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
   private boolean m_bDrawAttachments;
   private boolean m_bDrawGrid;
   private boolean m_bDrawGridAxes;
   private boolean m_bDrawGeometry;
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
   private final OriginGeometry m_originGeometry;
   private final OriginGizmo m_originGizmo;
   private final OriginVehiclePart m_originVehiclePart;
   private float m_gizmoScale;
   private boolean m_gizmoAxisVisibleX;
   private boolean m_gizmoAxisVisibleY;
   private boolean m_gizmoAxisVisibleZ;
   private String m_selectedAttachment;
   private final ArrayList<PositionRotation> m_axes;
   private final OriginBone m_highlightBone;
   private final OriginVehiclePart m_highlightPartBone;
   private final PolygonEditor m_polygonEditor;
   private static Clipper s_clipper;
   private static final ObjectPool<PositionRotation> s_posRotPool = new ObjectPool(PositionRotation::new);
   private final ArrayList<AABB> m_aabb;
   private static final ObjectPool<AABB> s_aabbPool = new ObjectPool(AABB::new);
   private final ArrayList<Box3D> m_box3D;
   private static final ObjectPool<Box3D> s_box3DPool = new ObjectPool(Box3D::new);
   private final ArrayList<PhysicsMesh> m_physicsMesh;
   private static final ObjectPool<PhysicsMesh> s_physicsMeshPool = new ObjectPool(PhysicsMesh::new);
   final Vector3f tempVector3f;
   final int[] m_viewport;
   private final float GRID_DARK;
   private final float GRID_LIGHT;
   private float GRID_ALPHA;
   private final int HALF_GRID;
   private static VBORenderer vboRenderer = null;
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
      this.m_bDrawAttachments = false;
      this.m_bDrawGrid = true;
      this.m_bDrawGridAxes = false;
      this.m_bDrawGeometry = true;
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
      this.m_originGeometry = new OriginGeometry(this);
      this.m_originGizmo = new OriginGizmo(this);
      this.m_originVehiclePart = new OriginVehiclePart(this);
      this.m_gizmoScale = 1.0F;
      this.m_gizmoAxisVisibleX = true;
      this.m_gizmoAxisVisibleY = true;
      this.m_gizmoAxisVisibleZ = true;
      this.m_selectedAttachment = null;
      this.m_axes = new ArrayList();
      this.m_highlightBone = new OriginBone(this);
      this.m_highlightPartBone = new OriginVehiclePart(this);
      this.m_polygonEditor = new PolygonEditor(this);
      this.m_aabb = new ArrayList();
      this.m_box3D = new ArrayList();
      this.m_physicsMesh = new ArrayList();
      this.tempVector3f = new Vector3f();
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
            if (var2.isInstance(var5)) {
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
         vboRenderer = VBORenderer.getInstance();
         super.render();
         IndieGL.glDepthMask(true);
         SpriteRenderer.instance.glClearDepth(1.0F);
         IndieGL.glClear(256);
         StateData var1 = this.stateDataMain();
         this.setModelViewProjection(var1);
         if (this.m_bDrawGridPlane) {
            SpriteRenderer.instance.drawGeneric(var1.m_gridPlaneDrawer);
         }

         PZArrayUtil.forEach((List)var1.m_objectData, SceneObjectRenderData::release);
         var1.m_objectData.clear();

         for(int var2 = 0; var2 < this.m_objects.size(); ++var2) {
            SceneObject var3 = (SceneObject)this.m_objects.get(var2);
            if (var3.m_visible) {
               if (var3.m_autoRotate) {
                  var3.m_autoRotateAngle = (float)((double)var3.m_autoRotateAngle + UIManager.getMillisSinceLastRender() / 30.0);
                  if (var3.m_autoRotateAngle > 360.0F) {
                     var3.m_autoRotateAngle = 0.0F;
                  }
               }

               SceneObjectRenderData var4 = var3.renderMain();
               if (var4 != null) {
                  var1.m_objectData.add(var4);
               }
            }
         }

         float var8 = (float)(Mouse.getXA() - this.getAbsoluteX().intValue());
         float var9 = (float)(Mouse.getYA() - this.getAbsoluteY().intValue());
         this.setGizmoTransforms(var1);
         if (this.m_gizmo != null) {
            var1.m_gizmoAxis = this.m_gizmo.hitTest(var8, var9);
         }

         var1.m_overlaysDrawer.init();
         SpriteRenderer.instance.drawGeneric(var1.m_overlaysDrawer);
         Vector3f var10;
         if (this.m_bDrawGrid) {
            var10 = this.uiToScene(var8, var9, 0.0F, this.tempVector3f);
            var10.x = (float)Math.round(var10.x * this.gridMult()) / this.gridMult();
            var10.y = (float)Math.round(var10.y * this.gridMult()) / this.gridMult();
            var10.z = (float)Math.round(var10.z * this.gridMult()) / this.gridMult();
            int var5 = TextManager.instance.MeasureStringX(UIFont.Small, String.format("X: %.3f", var10.x));
            int var6 = TextManager.instance.MeasureStringX(UIFont.Small, String.format("Y: %.3f", var10.y));
            int var7 = TextManager.instance.MeasureStringX(UIFont.Small, String.format("Z: %.3f", var10.z));
            this.DrawText(UIFont.Small, String.format("X: %.3f", var10.x), (double)(this.width - 20.0F - (float)var7 - 20.0F - (float)var6 - 20.0F - (float)var5), 10.0, 1.0, 0.0, 0.0, 1.0);
            this.DrawText(UIFont.Small, String.format("Y: %.3f", var10.y), (double)(this.width - 20.0F - (float)var7 - 20.0F - (float)var6), 10.0, 0.0, 1.0, 0.0, 1.0);
            this.DrawText(UIFont.Small, String.format("Z: %.3f", var10.z), (double)(this.width - 20.0F - (float)var7), 10.0, 0.0, 0.5, 1.0, 1.0);
         }

         float var15;
         if (this.m_gizmo == this.m_rotateGizmo && this.m_rotateGizmo.m_trackAxis != UI3DScene.Axis.None) {
            var10 = this.m_rotateGizmo.m_startXfrm.getTranslation(allocVector3f());
            float var12 = this.sceneToUIX(var10.x, var10.y, var10.z);
            var15 = this.sceneToUIY(var10.x, var10.y, var10.z);
            LineDrawer.drawLine(var12, var15, var8, var9, 0.5F, 0.5F, 0.5F, 1.0F, 1);
            releaseVector3f(var10);
         }

         Matrix4f var11;
         Vector3f var13;
         float var17;
         if (this.m_highlightBone.m_boneName != null) {
            var11 = this.m_highlightBone.getGlobalTransform(allocMatrix4f());
            this.m_highlightBone.m_character.getGlobalTransform(allocMatrix4f()).mul(var11, var11);
            var13 = var11.getTranslation(allocVector3f());
            var15 = this.sceneToUIX(var13.x, var13.y, var13.z);
            var17 = this.sceneToUIY(var13.x, var13.y, var13.z);
            LineDrawer.drawCircle(var15, var17, 10.0F, 16, 1.0F, 1.0F, 1.0F);
            releaseVector3f(var13);
            releaseMatrix4f(var11);
         }

         if (this.m_highlightPartBone.m_vehicle != null) {
            var11 = this.m_highlightPartBone.getGlobalBoneTransform(allocMatrix4f());
            var13 = var11.getTranslation(allocVector3f());
            var15 = this.sceneToUIX(var13.x, var13.y, var13.z);
            var17 = this.sceneToUIY(var13.x, var13.y, var13.z);
            LineDrawer.drawCircle(var15, var17, 10.0F, 16, 1.0F, 1.0F, 1.0F);
            releaseVector3f(var13);
            releaseMatrix4f(var11);
         }

         for(int var14 = 0; var14 < this.m_objects.size(); ++var14) {
            ScenePolygon var16 = (ScenePolygon)Type.tryCastTo((SceneObject)this.m_objects.get(var14), ScenePolygon.class);
            if (var16 != null && var16.m_editing) {
               var16.renderPoints();
            }
         }

      }
   }

   private void setModelViewProjection(StateData var1) {
      this.calcMatrices(this.m_projection, this.m_modelView);
      var1.m_projection.set(this.m_projection);
      long var2 = System.currentTimeMillis();
      if (this.m_viewChangeTime + this.VIEW_CHANGE_TIME > var2) {
         float var4 = (float)(this.m_viewChangeTime + this.VIEW_CHANGE_TIME - var2) / (float)this.VIEW_CHANGE_TIME;
         Quaternionf var5 = allocQuaternionf().setFromUnnormalized(this.m_modelView);
         var1.m_modelView.set(this.m_modelViewChange.slerp(var5, 1.0F - var4));
         releaseQuaternionf(var5);
      } else {
         var1.m_modelView.set(this.m_modelView);
      }

      var1.m_zoom = this.m_zoom;
   }

   private void setGizmoTransforms(StateData var1) {
      var1.m_gizmo = this.m_gizmo;
      if (this.m_gizmo != null) {
         var1.m_gizmoTranslate.set(this.m_gizmoPos);
         var1.m_gizmoRotate.set(this.m_gizmoRotate);
         var1.m_gizmoTransform.translation(this.m_gizmoPos);
         var1.m_gizmoTransform.rotateXYZ(this.m_gizmoRotate.x * 0.017453292F, this.m_gizmoRotate.y * 0.017453292F, this.m_gizmoRotate.z * 0.017453292F);
      }

      var1.m_gizmoChildTransform.identity();
      var1.m_gizmoChildAttachmentTransform.identity();
      var1.m_selectedAttachmentIsChildAttachment = this.m_gizmoChild != null && this.m_gizmoChild.m_attachment != null && this.m_gizmoChild.m_attachment.equals(this.m_selectedAttachment);
      if (this.m_gizmoChild != null) {
         this.m_gizmoChild.getLocalTransform(var1.m_gizmoChildTransform);
         this.m_gizmoChild.getAttachmentTransform(this.m_gizmoChild.m_attachment, var1.m_gizmoChildAttachmentTransform);
         var1.m_gizmoChildAttachmentTransformInv.set(var1.m_gizmoChildAttachmentTransform).invert();
      }

      var1.m_gizmoOriginTransform.identity();
      var1.m_hasGizmoOrigin = this.m_gizmoOrigin != null;
      var1.m_gizmoOriginIsGeometry = this.m_gizmoOrigin == this.m_originGeometry;
      if (this.m_gizmoOrigin != null && this.m_gizmoOrigin != this.m_gizmoParent) {
         this.m_gizmoOrigin.getGlobalTransform(var1.m_gizmoOriginTransform);
      }

      var1.m_gizmoParentTransform.identity();
      if (this.m_gizmoParent != null) {
         this.m_gizmoParent.getGlobalTransform(var1.m_gizmoParentTransform);
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

   public static Ray allocRay() {
      return (Ray)((ObjectPool)TL_Ray_pool.get()).alloc();
   }

   public static void releaseRay(Ray var0) {
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

   private static Vector2f allocVector2f() {
      return BaseVehicle.allocVector2f();
   }

   private static void releaseVector2f(Vector2f var0) {
      BaseVehicle.releaseVector2f(var0);
   }

   private static Vector3f allocVector3f() {
      return (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();
   }

   private static void releaseVector3f(Vector3f var0) {
      ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var0);
   }

   public Object fromLua0(String var1) {
      ArrayList var11;
      Iterator var13;
      SceneObject var14;
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
            this.m_highlightPartBone.m_vehicle = null;
            return null;
         case "clearPhysicsMeshes":
            s_physicsMeshPool.releaseAll(this.m_physicsMesh);
            this.m_physicsMesh.clear();
            return null;
         case "getDrawGeometry":
            return this.m_bDrawGeometry ? Boolean.TRUE : Boolean.FALSE;
         case "getGeometryNames":
            var11 = new ArrayList();
            var13 = this.m_objects.iterator();

            while(var13.hasNext()) {
               var14 = (SceneObject)var13.next();
               SceneGeometry var15 = (SceneGeometry)Type.tryCastTo(var14, SceneGeometry.class);
               if (var15 != null) {
                  var11.add(var15.m_id);
               }
            }

            return var11;
         case "getGizmoPos":
            return this.m_gizmoPos;
         case "getGridMult":
            return BoxedStaticValues.toDouble((double)this.gridMult());
         case "getObjectNames":
            var11 = new ArrayList();
            var13 = this.m_objects.iterator();

            while(var13.hasNext()) {
               var14 = (SceneObject)var13.next();
               var11.add(var14.m_id);
            }

            return var11;
         case "getView":
            return this.m_view.name();
         case "getViewRotation":
            return this.m_viewRotation;
         case "getModelCount":
            int var10 = 0;

            for(int var12 = 0; var12 < this.m_objects.size(); ++var12) {
               if (this.m_objects.get(var12) instanceof SceneModel) {
                  ++var10;
               }
            }

            return BoxedStaticValues.toDouble((double)var10);
         case "rotateAllGeometry":
            Matrix4f var4 = allocMatrix4f().rotationXYZ(0.0F, 4.712389F, 0.0F);
            Matrix4f var5 = allocMatrix4f();
            Quaternionf var6 = allocQuaternionf();
            Iterator var7 = this.m_objects.iterator();

            while(var7.hasNext()) {
               SceneObject var8 = (SceneObject)var7.next();
               SceneGeometry var9 = (SceneGeometry)Type.tryCastTo(var8, SceneGeometry.class);
               if (var9 != null) {
                  var9.getLocalTransform(var5);
                  var4.mul(var5, var5);
                  var5.getTranslation(var9.m_translate);
                  var6.setFromUnnormalized(var5);
                  var6.getEulerAnglesXYZ(var9.m_rotate);
                  var9.m_rotate.mul(57.295776F);
               }
            }

            releaseMatrix4f(var4);
            releaseMatrix4f(var5);
            releaseQuaternionf(var6);
            return null;
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
      SceneObject var18;
      AABB var19;
      SceneModel var20;
      SceneBox var22;
      SceneCylinder var24;
      AnimationPlayer var26;
      SceneCharacter var27;
      AnimationMultiTrack var39;
      ScenePolygon var40;
      switch (var1) {
         case "addCylinderAABB":
            var24 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            this.m_aabb.add(var24.getAABB((AABB)s_aabbPool.alloc()));
            return null;
         case "createCharacter":
            var18 = this.getSceneObjectById((String)var2, false);
            if (var18 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            ScenePlayer var37 = new ScenePlayer(this, (String)var2);
            var37.initAnimatedModel();
            this.m_objects.add(var37);
            return var37;
         case "createBox":
            var18 = this.getSceneObjectById((String)var2, false);
            if (var18 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            SceneBox var35 = new SceneBox(this, (String)var2);
            this.m_objects.add(var35);
            return var35;
         case "createCylinder":
            var18 = this.getSceneObjectById((String)var2, false);
            if (var18 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            SceneCylinder var32 = new SceneCylinder(this, (String)var2);
            var32.m_height = 1.0F;
            var32.m_radius = 0.5F;
            this.m_objects.add(var32);
            return var32;
         case "createPolygon":
            var18 = this.getSceneObjectById((String)var2, false);
            if (var18 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            } else {
               ScenePolygon var31 = new ScenePolygon(this, (String)var2);

               for(var21 = 0; var21 < this.m_objects.size(); ++var21) {
                  if (!(this.m_objects.get(var21) instanceof SceneGeometry)) {
                     this.m_objects.add(var21, var31);
                     return var31;
                  }
               }

               this.m_objects.add(var31);
               return var31;
            }
         case "createVehicle":
            var18 = this.getSceneObjectById((String)var2, false);
            if (var18 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            SceneVehicle var30 = new SceneVehicle(this, (String)var2);
            this.m_objects.add(var30);
            return null;
         case "getBoxMaxExtents":
            var22 = (SceneBox)this.getSceneObjectById((String)var2, SceneBox.class, true);
            return var22.m_max;
         case "getBoxMinExtents":
            var22 = (SceneBox)this.getSceneObjectById((String)var2, SceneBox.class, true);
            return var22.m_min;
         case "getCharacterAnimate":
            var27 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            return var27.m_animatedModel.isAnimate();
         case "getCharacterAnimationDuration":
            var27 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var26 = var27.m_animatedModel.getAnimationPlayer();
            if (var26 == null) {
               return null;
            } else {
               var39 = var26.getMultiTrack();
               if (var39 != null && !var39.getTracks().isEmpty()) {
                  return KahluaUtil.toDouble((double)((AnimationTrack)var39.getTracks().get(0)).getDuration());
               }

               return null;
            }
         case "getCharacterAnimationTime":
            var27 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var26 = var27.m_animatedModel.getAnimationPlayer();
            if (var26 == null) {
               return null;
            } else {
               var39 = var26.getMultiTrack();
               if (var39 != null && !var39.getTracks().isEmpty()) {
                  return KahluaUtil.toDouble((double)((AnimationTrack)var39.getTracks().get(0)).getCurrentTimeValue());
               }

               return null;
            }
         case "getCharacterShowBones":
            var27 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            return var27.m_bShowBones;
         case "getCylinderHeight":
            var24 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            return (double)var24.m_height;
         case "getCylinderRadius":
            var24 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            return (double)var24.m_radius;
         case "getGeometryType":
            SceneGeometry var41 = (SceneGeometry)this.getSceneObjectById((String)var2, SceneGeometry.class, false);
            return var41 == null ? null : var41.getTypeName();
         case "getPolygonExtents":
            var40 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            return var40.m_extents;
         case "getPolygonPlane":
            var40 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            return var40.m_plane.name();
         case "getModelIgnoreVehicleScale":
            var20 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            return var20.m_ignoreVehicleScale ? Boolean.TRUE : Boolean.FALSE;
         case "getModelScript":
            var5 = 0;

            for(int var23 = 0; var23 < this.m_objects.size(); ++var23) {
               SceneModel var38 = (SceneModel)Type.tryCastTo((SceneObject)this.m_objects.get(var23), SceneModel.class);
               if (var38 != null && var5++ == ((Double)var2).intValue()) {
                  return var38.m_modelScript;
               }
            }

            return null;
         case "getModelSpriteModel":
            var20 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            return var20.m_spriteModel;
         case "getObjectAutoRotate":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_autoRotate ? Boolean.TRUE : Boolean.FALSE;
         case "getObjectExists":
            return this.getSceneObjectById((String)var2, false) != null;
         case "getObjectParent":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_parent == null ? null : var18.m_parent.m_id;
         case "getObjectParentAttachment":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_parentAttachment;
         case "getObjectParentVehicle":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_parentVehiclePart == null ? null : var18.m_parentVehiclePart.m_vehicle.m_id;
         case "getObjectParentVehiclePart":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_parentVehiclePart == null ? null : var18.m_parentVehiclePart.m_partId;
         case "getObjectParentVehiclePartModel":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_parentVehiclePart == null ? null : var18.m_parentVehiclePart.m_partModelId;
         case "getObjectParentVehiclePartModelAttachment":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_parentVehiclePart == null ? null : var18.m_parentVehiclePart.m_attachmentName;
         case "getObjectRotation":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_rotate;
         case "getObjectScale":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_scale;
         case "getObjectTranslation":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_translate;
         case "getVehicleScript":
            SceneVehicle var28 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            return var28.m_script;
         case "isCharacterFemale":
            var27 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            return var27.m_animatedModel.isFemale();
         case "isObjectVisible":
            var18 = this.getSceneObjectById((String)var2, true);
            return var18.m_visible ? Boolean.TRUE : Boolean.FALSE;
         case "moveCylinderToGround":
            var24 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            var19 = var24.getAABB((AABB)s_aabbPool.alloc());
            Boolean var36 = Boolean.FALSE;
            if (var24.m_translate.y != var19.h / 2.0F) {
               var24.m_translate.y = var19.h / 2.0F;
               var36 = Boolean.TRUE;
            }

            s_aabbPool.release((Object)var19);
            return var36;
         case "moveCylinderToOrigin":
            var24 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            var19 = var24.getAABB((AABB)s_aabbPool.alloc());
            var24.m_translate.set(0.0F, var19.h / 2.0F, 0.0F);
            s_aabbPool.release((Object)var19);
            return null;
         case "recalculateBoxCenter":
            var22 = (SceneBox)this.getSceneObjectById((String)var2, SceneBox.class, true);
            Vector3f var17 = allocVector3f().set(var22.m_min);
            Vector3f var34 = allocVector3f().set(var22.m_max);
            Vector3f var25 = allocVector3f().set(var34).add(var17).mul(0.5F).setComponent(1, var17.y);
            Matrix4f var29 = var22.getLocalTransform(allocMatrix4f());
            var17.sub(var25);
            var34.sub(var25);
            var29.transformPosition(var25);
            var22.m_translate.set(var25);
            var22.getLocalTransform(var29);
            var29.invert();
            var22.m_min.set(var17);
            var22.m_max.set(var34);
            releaseMatrix4f(var29);
            releaseVector3f(var25);
            releaseVector3f(var34);
            releaseVector3f(var17);
            return null;
         case "removeModel":
            var20 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            this.m_objects.remove(var20);
            Iterator var16 = this.m_objects.iterator();

            while(var16.hasNext()) {
               SceneObject var33 = (SceneObject)var16.next();
               if (var33.m_parent == var20) {
                  var33.m_attachment = null;
                  var33.m_parent = null;
                  var33.m_parentAttachment = null;
               }
            }

            return null;
         case "removeObject":
            var18 = this.getSceneObjectById((String)var2, true);
            this.m_objects.remove(var18);
            return null;
         case "setDrawAttachments":
            this.m_bDrawAttachments = (Boolean)var2;
            return null;
         case "setDrawGrid":
            this.m_bDrawGrid = (Boolean)var2;
            return null;
         case "setDrawGridAxes":
            this.m_bDrawGridAxes = (Boolean)var2;
            return null;
         case "setDrawGeometry":
            this.m_bDrawGeometry = (Boolean)var2;
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
         case "setRotateGizmoSnap":
            this.m_rotateGizmo.m_bSnap = (Boolean)var2;
            return null;
         case "setScaleGizmoSnap":
            this.m_scaleGizmo.m_bSnap = (Boolean)var2;
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
      IsoSpriteGrid var9;
      int var10;
      int var11;
      int var14;
      int var17;
      int var18;
      int var19;
      int var20;
      int var21;
      SceneObject var27;
      String var28;
      SceneModel var30;
      ModelScript var31;
      IsoSprite var32;
      Model var34;
      AnimationPlayer var38;
      SceneCharacter var39;
      SceneModel var42;
      ModelAttachment var47;
      ScenePolygon var48;
      AnimationMultiTrack var55;
      SceneCylinder var61;
      switch (var1) {
         case "addAttachment":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            if (var30.m_modelScript.getAttachmentById((String)var3) != null) {
               throw new IllegalArgumentException("model script \"" + var2 + "\" already has attachment named \"" + var3 + "\"");
            }

            var47 = new ModelAttachment((String)var3);
            var30.m_modelScript.addAttachment(var47);
            return var47;
         case "addBoneAxis":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var28 = (String)var3;
            PositionRotation var68 = var39.getBoneAxis(var28, (PositionRotation)s_posRotPool.alloc());
            this.m_axes.add(var68);
            return null;
         case "addPolygonPoint":
            var48 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var48.m_points.add(new Vector2f((Vector2f)var3));
            var48.triangulate();
            return null;
         case "applyDeltaRotation":
            Vector3f var75 = (Vector3f)var2;
            Vector3f var65 = (Vector3f)var3;
            Quaternionf var66 = allocQuaternionf().rotationXYZ(var75.x * 0.017453292F, var75.y * 0.017453292F, var75.z * 0.017453292F);
            Quaternionf var59 = allocQuaternionf().rotationXYZ(var65.x * 0.017453292F, var65.y * 0.017453292F, var65.z * 0.017453292F);
            var66.mul(var59);
            var66.getEulerAnglesXYZ(var75);
            releaseQuaternionf(var66);
            releaseQuaternionf(var59);
            var75.mul(57.295776F);
            if (this.m_rotateGizmo.m_bSnap) {
               var75.x = (float)Math.floor((double)(var75.x + 0.5F));
               var75.y = (float)Math.floor((double)(var75.y + 0.5F));
               var75.z = (float)Math.floor((double)(var75.z + 0.5F));
            }

            return var75;
         case "cloneObject":
            var27 = this.getSceneObjectById((String)var2, true);
            SceneObject var63 = this.getSceneObjectById((String)var3, false);
            if (var63 != null) {
               throw new IllegalStateException("scene object \"" + var3 + "\" exists");
            }

            var63 = var27.clone((String)var3);
            this.m_objects.add(var63);
            return var63;
         case "configDepthTexture":
            SceneDepthTexture var74 = (SceneDepthTexture)this.getSceneObjectById((String)var2, SceneDepthTexture.class, true);
            var74.m_texture = (Texture)var3;
            return var74;
         case "copyGeometryFromSpriteGrid":
            var25 = (String)var2;
            var28 = (String)var3;
            var32 = IsoSpriteManager.instance.getSprite(var28);
            var9 = var32.getSpriteGrid();
            var10 = var9.getSpriteIndex(var32);
            var11 = 0;

            for(; var11 < var9.getSpriteCount(); ++var11) {
               if (var11 != var10) {
                  IsoSprite var62 = var9.getSpriteFromIndex(var11);
                  if (var62 != null) {
                     String var67 = var62.tilesetName;
                     var14 = var62.tileSheetIndex % 8;
                     int var71 = var62.tileSheetIndex / 8;
                     ArrayList var72 = TileGeometryManager.getInstance().getGeometry(var25, var67, var14, var71);
                     if (var72 != null && !var72.isEmpty()) {
                        var67 = var32.tilesetName;
                        var14 = var32.tileSheetIndex % 8;
                        var71 = var32.tileSheetIndex / 8;
                        TileGeometryManager.getInstance().copyGeometry(var25, var67, var14, var71, var72);
                        var17 = var9.getSpriteGridPosX(var32);
                        var18 = var9.getSpriteGridPosY(var32);
                        var19 = var9.getSpriteGridPosX(var62);
                        var20 = var9.getSpriteGridPosY(var62);
                        var72 = TileGeometryManager.getInstance().getGeometry(var25, var67, var14, var71);

                        for(var21 = 0; var21 < var72.size(); ++var21) {
                           TileGeometryFile.Geometry var76 = (TileGeometryFile.Geometry)var72.get(var21);
                           var76.offset(var19 - var17, var20 - var18);
                        }

                        return null;
                     }
                  }
               }
            }

            return null;
         case "createModel":
            var27 = this.getSceneObjectById((String)var2, false);
            if (var27 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            } else {
               var31 = ScriptManager.instance.getModelScript((String)var3);
               if (var31 == null) {
                  throw new NullPointerException("model script \"" + var3 + "\" not found");
               } else {
                  var34 = ModelManager.instance.getLoadedModel((String)var3);
                  if (var34 == null) {
                     throw new NullPointerException("model \"" + var3 + "\" not found");
                  }

                  var42 = new SceneModel(this, (String)var2, var31, var34);
                  this.m_objects.add(var42);
                  return null;
               }
            }
         case "dragGizmo":
            float var73 = ((Double)var2).floatValue();
            float var56 = ((Double)var3).floatValue();
            if (this.m_gizmo == null) {
               throw new NullPointerException("gizmo is null");
            }

            this.m_gizmo.updateTracking(var73, var56);
            return null;
         case "dragView":
            var6 = ((Double)var2).intValue();
            var7 = ((Double)var3).intValue();
            this.m_view_x -= var6;
            this.m_view_y -= var7;
            this.calcMatrices(this.m_projection, this.m_modelView);
            return null;
         case "getCharacterAnimationKeyframeTimes":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var38 = var39.m_animatedModel.getAnimationPlayer();
            if (var38 == null) {
               return null;
            } else {
               var55 = var38.getMultiTrack();
               if (var55 != null && !var55.getTracks().isEmpty()) {
                  AnimationTrack var53 = (AnimationTrack)var55.getTracks().get(0);
                  AnimationClip var44 = var53.getClip();
                  if (var44 == null) {
                     return null;
                  }

                  if (var3 == null) {
                     var3 = new ArrayList();
                  }

                  ArrayList var51 = (ArrayList)var3;
                  var51.clear();
                  Keyframe[] var58 = var44.getKeyframes();

                  for(int var64 = 0; var64 < var58.length; ++var64) {
                     Keyframe var69 = var58[var64];
                     Double var70 = KahluaUtil.toDouble((double)var69.Time);
                     if (!var51.contains(var70)) {
                        var51.add(var70);
                     }
                  }

                  return var51;
               }

               return null;
            }
         case "moveCylinderToOrigin":
            var61 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            var28 = (String)var3;
            AABB var60 = var61.getAABB((AABB)s_aabbPool.alloc());
            if (var28 != null && !"None".equalsIgnoreCase(var28)) {
               if ("X".equalsIgnoreCase(var28)) {
                  var61.m_translate.setComponent(0, 0.0F);
               } else if ("Y".equalsIgnoreCase(var28)) {
                  var61.m_translate.setComponent(1, var60.h / 2.0F);
               } else if ("Z".equalsIgnoreCase(var28)) {
                  var61.m_translate.setComponent(2, 0.0F);
               }
            } else {
               var61.m_translate.set(0.0F, var60.h / 2.0F, 0.0F);
            }

            s_aabbPool.release((Object)var60);
            return null;
         case "removeAttachment":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var47 = var30.m_modelScript.getAttachmentById((String)var3);
            if (var47 == null) {
               throw new IllegalArgumentException("model script \"" + var2 + "\" attachment \"" + var3 + "\" not found");
            }

            var30.m_modelScript.removeAttachment(var47);
            return null;
         case "removePolygonPoint":
            var48 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var7 = ((Double)var3).intValue();
            if (var48.m_points.size() <= 3) {
               return null;
            }

            var48.m_points.remove(var7);
            var48.triangulate();
            return null;
         case "setCharacterAlpha":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_animatedModel.setAlpha(((Double)var3).floatValue());
            return null;
         case "setCharacterAnimate":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_animatedModel.setAnimate((Boolean)var3);
            return null;
         case "setCharacterAnimationClip":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            AnimationSet var43 = AnimationSet.GetAnimationSet(var39.m_animatedModel.GetAnimSetName(), false);
            if (var43 == null) {
               return null;
            } else {
               AnimState var57 = var43.GetState(var39.m_animatedModel.getState());
               if (var57 != null && !var57.m_Nodes.isEmpty()) {
                  AnimNode var50 = (AnimNode)var57.m_Nodes.get(0);
                  var50.m_AnimName = (String)var3;
                  var39.m_animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                  var39.m_animatedModel.getAdvancedAnimator().SetState(var57.m_Name);
                  return null;
               }

               return null;
            }
         case "setCharacterAnimationSpeed":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            AnimationMultiTrack var41 = var39.m_animatedModel.getAnimationPlayer().getMultiTrack();
            if (var41.getTracks().isEmpty()) {
               return null;
            }

            ((AnimationTrack)var41.getTracks().get(0)).SpeedDelta = PZMath.clamp(((Double)var3).floatValue(), 0.0F, 10.0F);
            return null;
         case "setCharacterAnimationTime":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_animatedModel.setTrackTime(((Double)var3).floatValue());
            var38 = var39.m_animatedModel.getAnimationPlayer();
            if (var38 == null) {
               return null;
            } else {
               var55 = var38.getMultiTrack();
               if (var55 != null && !var55.getTracks().isEmpty()) {
                  ((AnimationTrack)var55.getTracks().get(0)).setCurrentTimeValue(((Double)var3).floatValue());
                  return null;
               }

               return null;
            }
         case "setCharacterAnimSet":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var28 = (String)var3;
            if (!var28.equals(var39.m_animatedModel.GetAnimSetName())) {
               var39.m_animatedModel.setAnimSetName(var28);
               var39.m_animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
               ActionGroup var52 = ActionGroup.getActionGroup(var39.m_animatedModel.GetAnimSetName());
               ActionContext var49 = var39.m_animatedModel.getActionContext();
               if (var52 != var49.getGroup()) {
                  var49.setGroup(var52);
               }

               var39.m_animatedModel.getAdvancedAnimator().SetState(var49.getCurrentStateName(), PZArrayUtil.listConvert(var49.getChildStates(), (var0) -> {
                  return var0.getName();
               }));
            }

            return null;
         case "setCharacterClearDepthBuffer":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_bClearDepthBuffer = (Boolean)var3;
            return null;
         case "setCharacterFemale":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            boolean var35 = (Boolean)var3;
            if (var35 != var39.m_animatedModel.isFemale()) {
               var39.m_animatedModel.setOutfitName("Naked", var35, false);
            }

            return null;
         case "setCharacterShowBones":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_bShowBones = (Boolean)var3;
            return null;
         case "setCharacterShowBip01":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_bShowBip01 = (Boolean)var3;
            return null;
         case "setCharacterUseDeferredMovement":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_bUseDeferredMovement = (Boolean)var3;
            return null;
         case "setCylinderHeight":
            var61 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            var61.m_height = PZMath.clamp(((Double)var3).floatValue(), 0.01F, 2.44949F);
            return null;
         case "setCylinderRadius":
            var61 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            var61.m_radius = PZMath.clamp(((Double)var3).floatValue(), 0.01F, 10.0F);
            return null;
         case "setGeometryExtents":
            var48 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var48.m_extents.set((Vector3f)var3);
            return null;
         case "setGeometrySelected":
            SceneGeometry var54 = (SceneGeometry)this.getSceneObjectById((String)var2, SceneGeometry.class, true);
            var54.m_bSelected = (Boolean)var3;
            return null;
         case "setPolygonEditing":
            var48 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var48.m_editing = (Boolean)var3;
            return null;
         case "setPolygonHighlightPoint":
            var48 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var48.m_highlightPointIndex = ((Double)var3).intValue();
            return null;
         case "setPolygonPlane":
            var48 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var48.m_plane = UI3DScene.GridPlane.valueOf((String)var3);
            switch (var48.m_plane) {
               case XY:
                  var48.m_rotate.set(0.0F, 0.0F, 0.0F);
                  break;
               case XZ:
                  var48.m_rotate.set(270.0F, 0.0F, 0.0F);
                  break;
               case YZ:
                  var48.m_rotate.set(0.0F, 90.0F, 0.0F);
            }

            if (var48.m_points.isEmpty()) {
               var48.m_points.add(new Vector2f(-0.5F, -0.5F));
               var48.m_points.add(new Vector2f(0.5F, -0.5F));
               var48.m_points.add(new Vector2f(0.5F, 0.5F));
               var48.m_points.add(new Vector2f(-0.5F, 0.5F));
            }

            var48.triangulate();
            return null;
         case "setGizmoAxisVisible":
            Axis var46 = UI3DScene.Axis.valueOf((String)var2);
            Boolean var33 = (Boolean)var3;
            switch (var46) {
               case X:
                  this.m_gizmoAxisVisibleX = var33;
                  break;
               case Y:
                  this.m_gizmoAxisVisibleY = var33;
                  break;
               case Z:
                  this.m_gizmoAxisVisibleZ = var33;
            }

            return null;
         case "setGizmoOrigin":
            SceneVehicle var37;
            switch ((String)var2) {
               case "centerOfMass":
                  this.m_gizmoParent = (SceneObject)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "chassis":
                  var37 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoParent = var37;
                  this.m_originGizmo.m_translate.set(var37.m_script.getCenterOfMassOffset());
                  this.m_originGizmo.m_rotate.zero();
                  this.m_gizmoOrigin = this.m_originGizmo;
                  this.m_gizmoChild = null;
                  break;
               case "character":
                  SceneCharacter var45 = (SceneCharacter)this.getSceneObjectById((String)var3, SceneCharacter.class, true);
                  this.m_gizmoParent = var45;
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "model":
                  var42 = (SceneModel)this.getSceneObjectById((String)var3, SceneModel.class, true);
                  this.m_gizmoParent = var42;
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "object":
                  SceneObject var40 = this.getSceneObjectById((String)var3, true);
                  this.m_gizmoParent = var40;
                  this.m_gizmoOrigin = this.m_gizmoParent;
                  this.m_gizmoChild = null;
                  break;
               case "vehicleModel":
                  var37 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoParent = var37;
                  this.m_originGizmo.m_translate.set(var37.m_script.getModel().getOffset());
                  this.m_originGizmo.m_rotate.zero();
                  this.m_gizmoOrigin = this.m_originGizmo;
                  this.m_gizmoChild = null;
            }

            return null;
         case "setCharacterState":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var39.m_animatedModel.setState((String)var3);
            return null;
         case "setHighlightBone":
            var39 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var28 = (String)var3;
            this.m_highlightBone.m_character = var39;
            this.m_highlightBone.m_boneName = var28;
            this.m_highlightPartBone.m_vehicle = null;
            return null;
         case "setModelIgnoreVehicleScale":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var30.m_ignoreVehicleScale = (Boolean)var3;
            return null;
         case "setModelScript":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var31 = ScriptManager.instance.getModelScript((String)var3);
            if (var31 == null) {
               throw new NullPointerException("model script \"" + var3 + "\" not found");
            } else {
               var34 = ModelManager.instance.getLoadedModel((String)var3);
               if (var34 == null) {
                  throw new NullPointerException("model \"" + var3 + "\" not found");
               }

               var30.m_modelScript = var31;
               var30.m_model = var34;
               return null;
            }
         case "setModelSpriteModel":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            SpriteModel var29 = (SpriteModel)var3;
            var30.setSpriteModel(var29);
            return null;
         case "setModelSpriteModelEditor":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var30.m_spriteModelEditor = (Boolean)var3;
            return null;
         case "setModelUseWorldAttachment":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var30.m_useWorldAttachment = (Boolean)var3;
            return null;
         case "setModelWeaponRotationHack":
            var30 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var30.m_weaponRotationHack = (Boolean)var3;
            return null;
         case "setObjectAutoRotate":
            var27 = this.getSceneObjectById((String)var2, true);
            var27.m_autoRotate = (Boolean)var3;
            if (!var27.m_autoRotate) {
               var27.m_autoRotateAngle = 0.0F;
            }

            return null;
         case "setObjectVisible":
            var27 = this.getSceneObjectById((String)var2, true);
            var27.m_visible = (Boolean)var3;
            return null;
         case "setVehicleScript":
            SceneVehicle var26 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            var26.setScriptName((String)var3);
            return null;
         case "subtractSpriteGridPixels":
            var25 = (String)var2;
            var28 = (String)var3;
            var32 = IsoSpriteManager.instance.getSprite(var28);
            var9 = var32.getSpriteGrid();
            var10 = var9.getSpriteIndex(var32);
            var11 = var9.getSpriteGridPosX(var32);
            int var12 = var9.getSpriteGridPosY(var32);
            TileDepthTexture var13 = TileDepthTextureManager.getInstance().getTexture(var25, var32.tilesetName, var32.tileSheetIndex);

            for(var14 = 0; var14 < var9.getSpriteCount(); ++var14) {
               if (var14 != var10) {
                  IsoSprite var15 = var9.getSpriteFromIndex(var14);
                  if (var15 != null) {
                     Texture var16 = var15.getTextureForCurrentFrame(IsoDirections.N);
                     if (var16 != null && var16.getMask() != null) {
                        var17 = var9.getSpriteGridPosX(var15);
                        var18 = var9.getSpriteGridPosY(var15);
                        var19 = var11 - var17;
                        var20 = var12 - var18;

                        for(var21 = 0; var21 < 256; ++var21) {
                           int var22 = var21 + var19 * 32 + var20 * 32;

                           for(int var23 = 0; var23 < 128; ++var23) {
                              int var24 = var23 + var19 * 64 - var20 * 64;
                              if (var16.isMaskSet(var24, var22)) {
                                 var13.setPixel(var23, var21, -1.0F);
                              }
                           }
                        }
                     }
                  }
               }
            }

            var13.updateGPUTexture();
            return null;
         case "testGizmoAxis":
            var6 = ((Double)var2).intValue();
            var7 = ((Double)var3).intValue();
            if (this.m_gizmo == null) {
               return "None";
            }

            StateData var8 = this.stateDataMain();
            this.setModelViewProjection(var8);
            this.setGizmoTransforms(var8);
            return this.m_gizmo.hitTest((float)var6, (float)var7).toString();
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\"", var1, var2, var3));
      }
   }

   public Object fromLua3(String var1, Object var2, Object var3, Object var4) {
      float var7;
      float var8;
      float var9;
      Matrix4f var11;
      ScenePolygon var18;
      String var19;
      SceneModel var20;
      int var22;
      ModelAttachment var24;
      String var25;
      Matrix4f var28;
      Boolean var36;
      switch (var1) {
         case "addAxis":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            this.m_axes.add(((PositionRotation)s_posRotPool.alloc()).set(var7, var8, var9));
            return null;
         case "changeCylinderHeight":
            SceneCylinder var39 = (SceneCylinder)this.getSceneObjectById((String)var2, SceneCylinder.class, true);
            var19 = (String)var3;
            var9 = PZMath.clamp(((Double)var4).floatValue(), 0.01F, 2.44949F);
            var28 = var39.getLocalTransform(allocMatrix4f());
            Vector3f var37 = var28.transformDirection(allocVector3f().set(0.0F, 0.0F, 1.0F));
            if ("zMax".equalsIgnoreCase(var19)) {
               var39.m_translate.add(var37.mul(var9 - var39.m_height).div(2.0F));
            } else if ("zMin".equalsIgnoreCase(var19)) {
               var39.m_translate.add(var37.mul(-(var9 - var39.m_height)).div(2.0F));
            }

            var39.m_height = var9;
            releaseMatrix4f(var28);
            releaseVector3f(var37);
            return null;
         case "copyGeometryFrom":
            var17 = (String)var2;
            var19 = (String)var3;
            var25 = (String)var4;
            IsoSprite var30 = IsoSpriteManager.instance.getSprite(var25);
            IsoSprite var34 = IsoSpriteManager.instance.getSprite(var19);
            String var38 = var30.tilesetName;
            int var13 = var30.tileSheetIndex % 8;
            int var14 = var30.tileSheetIndex / 8;
            ArrayList var15 = TileGeometryManager.getInstance().getGeometry(var17, var38, var13, var14);
            if (var15 != null && !var15.isEmpty()) {
               var38 = var34.tilesetName;
               var13 = var34.tileSheetIndex % 8;
               var14 = var34.tileSheetIndex / 8;
               TileGeometryManager.getInstance().copyGeometry(var17, var38, var13, var14, var15);
               return null;
            }

            return null;
         case "createAnimal":
            SceneObject var35 = this.getSceneObjectById((String)var2, false);
            if (var35 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            }

            SceneAnimal var33 = new SceneAnimal(this, (String)var2, (AnimalDefinitions)var3, (AnimalBreed)var4);
            var33.initAnimatedModel();
            this.m_objects.add(var33);
            return var33;
         case "getGeometryDepthAt":
            SceneGeometry var32 = (SceneGeometry)this.getSceneObjectById((String)var2, SceneGeometry.class, true);
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            return (double)var32.getNormalizedDepthAt(var8, var9);
         case "getPolygonPoint":
            var18 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var22 = ((Double)var3).intValue();
            ((Vector2f)var4).set((Vector2fc)var18.m_points.get(var22));
            return var4;
         case "pickCharacterBone":
            SceneCharacter var29 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            return var29.pickBone(var8, var9);
         case "placeAttachmentAtOrigin":
            var20 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var24 = var20.m_modelScript.getAttachmentById((String)var3);
            var36 = (Boolean)var4;
            if (var24 == null) {
               throw new IllegalArgumentException("model script \"" + var2 + "\" attachment \"" + var3 + "\" not found");
            }

            var28 = allocMatrix4f();
            var28.identity();
            if (var36) {
               var28.rotateXYZ(0.0F, 3.1415927F, 1.5707964F);
            }

            var11 = ModelInstanceRenderData.makeAttachmentTransform(var24, allocMatrix4f());
            var11.invert();
            var28.mul(var11);
            var28.getTranslation(var20.m_translate);
            Quaternionf var12 = var28.getUnnormalizedRotation(allocQuaternionf());
            var12.getEulerAnglesXYZ(var20.m_rotate);
            var20.m_rotate.mul(57.295776F);
            releaseQuaternionf(var12);
            releaseMatrix4f(var11);
            releaseMatrix4f(var28);
            return null;
         case "polygonToUI":
            var18 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            this.m_polygonEditor.setPlane(var18.m_translate, var18.m_rotate, var18.m_plane);
            this.m_polygonEditor.planeToUI((Vector2f)var3, (Vector2f)var4);
            return var4;
         case "rasterizePolygon":
            var18 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var18.rasterize((var2x, var3x) -> {
               LuaManager.caller.protectedCallVoid(LuaManager.thread, var3, var4, BoxedStaticValues.toDouble((double)var2x), BoxedStaticValues.toDouble((double)var3x));
            });
            return null;
         case "setAnimalDefinition":
            SceneAnimal var21 = (SceneAnimal)this.getSceneObjectById((String)var2, SceneAnimal.class, true);
            var21.setAnimalDefinition((AnimalDefinitions)var3, (AnimalBreed)var4);
            return null;
         case "setAttachmentToOrigin":
            var20 = (SceneModel)this.getSceneObjectById((String)var2, SceneModel.class, true);
            var24 = var20.m_modelScript.getAttachmentById((String)var3);
            var36 = (Boolean)var4;
            if (var24 == null) {
               throw new IllegalArgumentException("model script \"" + var2 + "\" attachment \"" + var3 + "\" not found");
            }

            var28 = var20.getGlobalTransform(allocMatrix4f());
            if (var36) {
               var11 = allocMatrix4f().rotationXYZ(0.0F, 3.1415927F, 1.5707964F);
               var11.invert();
               var11.mul(var28, var28);
               releaseMatrix4f(var11);
            }

            var28.invert();
            var28.getTranslation(var24.getOffset());
            Quaternionf var31 = var28.getUnnormalizedRotation(allocQuaternionf());
            var31.getEulerAnglesXYZ(var24.getRotate());
            var24.getRotate().mul(57.295776F);
            releaseQuaternionf(var31);
            releaseMatrix4f(var28);
            return null;
         case "setPolygonPoint":
            var18 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var22 = ((Double)var3).intValue();
            ((Vector2f)var18.m_points.get(var22)).set((Vector2f)var4);
            var18.triangulate();
            return null;
         case "setGizmoOrigin":
            switch ((String)var2) {
               case "bone":
                  SceneCharacter var26 = (SceneCharacter)this.getSceneObjectById((String)var3, SceneCharacter.class, true);
                  this.m_gizmoParent = var26;
                  this.m_originBone.m_character = var26;
                  this.m_originBone.m_boneName = (String)var4;
                  this.m_gizmoOrigin = this.m_originBone;
                  this.m_gizmoChild = null;
                  break;
               case "geometry":
                  SceneGeometry var10 = (SceneGeometry)this.getSceneObjectById((String)var3, SceneGeometry.class, true);
                  this.m_gizmoParent = var10;
                  this.m_originGeometry.m_sceneGeometry = var10;
                  this.m_originGeometry.m_originHint = (String)var4;
                  this.m_gizmoOrigin = this.m_originGeometry;
                  this.m_gizmoChild = null;
            }

            return null;
         case "setGizmoXYZ":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            var9 = ((Double)var4).floatValue();
            this.m_gizmoPos.set(var7, var8, var9);
            return null;
         case "setShowVehiclePartBones":
            SceneVehicle var16 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            var19 = (String)var3;
            var25 = (String)var4;
            var16.m_showBones_partId = var19;
            var16.m_showBones_modelId = var25;
            return null;
         case "startGizmoTracking":
            var7 = ((Double)var2).floatValue();
            var8 = ((Double)var3).floatValue();
            Axis var23 = UI3DScene.Axis.valueOf((String)var4);
            if (this.m_gizmo != null) {
               this.m_gizmo.startTracking(var7, var8, var23);
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
      String var9;
      int var10;
      int var11;
      ArrayList var12;
      ScenePolygon var22;
      SceneObject var24;
      int var27;
      switch (var1) {
         case "loadFromGeometryFile":
            var8 = (String)var2;
            var9 = (String)var3;
            var10 = ((Double)var4).intValue();
            var11 = ((Double)var5).intValue();
            int var34 = this.m_objects.size() - 1;

            for(; var34 >= 0; --var34) {
               SceneObject var36 = (SceneObject)this.m_objects.get(var34);
               if (Type.tryCastTo(var36, SceneGeometry.class) != null) {
                  this.m_objects.remove(var34);
               }
            }

            var12 = TileGeometryManager.getInstance().getGeometry(var8, var9, var10, var11);
            if (var12 == null) {
               return null;
            } else {
               int var37 = 1;
               Iterator var39 = var12.iterator();

               while(true) {
                  while(var39.hasNext()) {
                     TileGeometryFile.Geometry var40 = (TileGeometryFile.Geometry)var39.next();
                     TileGeometryFile.Box var41 = var40.asBox();
                     if (var41 != null) {
                        SceneBox var42 = new SceneBox(this, "box" + var37);
                        var42.m_translate.set(var41.translate);
                        var42.m_rotate.set(var41.rotate);
                        var42.m_min.set(var41.min);
                        var42.m_max.set(var41.max);
                        this.m_objects.add(var37 - 1, var42);
                        ++var37;
                     } else {
                        TileGeometryFile.Cylinder var17 = var40.asCylinder();
                        if (var17 != null) {
                           SceneCylinder var43 = new SceneCylinder(this, "cylinder" + var37);
                           var43.m_translate.set(var17.translate);
                           var43.m_rotate.set(var17.rotate);
                           var43.m_radius = Math.max(var17.radius1, var17.radius2);
                           var43.m_height = var17.height;
                           this.m_objects.add(var37 - 1, var43);
                           ++var37;
                        } else {
                           TileGeometryFile.Polygon var18 = var40.asPolygon();
                           if (var18 != null) {
                              ScenePolygon var19 = new ScenePolygon(this, "polygon" + var37);
                              var19.m_translate.set(var18.translate);
                              var19.m_rotate.set(var18.rotate);
                              var19.m_plane = UI3DScene.GridPlane.valueOf(var18.plane.name());

                              for(int var20 = 0; var20 < var18.m_points.size(); var20 += 2) {
                                 var19.m_points.add(new Vector2f(var18.m_points.get(var20), var18.m_points.get(var20 + 1)));
                              }

                              var19.triangulate();
                              this.m_objects.add(var37 - 1, var19);
                              ++var37;
                           }
                        }
                     }
                  }

                  return null;
               }
            }
         case "pickPolygonEdge":
            var22 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var27 = var22.pickEdge(((Double)var3).floatValue(), ((Double)var4).floatValue(), ((Double)var5).floatValue());
            return BoxedStaticValues.toDouble((double)var27);
         case "pickPolygonPoint":
            var22 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            var27 = var22.pickPoint(((Double)var3).floatValue(), ((Double)var4).floatValue(), ((Double)var5).floatValue());
            return BoxedStaticValues.toDouble((double)var27);
         case "setGizmoOrigin":
            switch ((String)var2) {
               case "attachment":
                  SceneObject var32 = this.getSceneObjectById((String)var3, true);
                  this.m_gizmoParent = this.getSceneObjectById((String)var4, true);
                  this.m_originAttachment.m_object = this.m_gizmoParent;
                  this.m_originAttachment.m_attachmentName = (String)var5;
                  this.m_gizmoOrigin = this.m_originAttachment;
                  this.m_gizmoChild = var32;
               default:
                  return null;
            }
         case "setHighlightPartBone":
            SceneVehicle var28 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            this.m_highlightPartBone.m_vehicle = var28;
            this.m_highlightPartBone.m_partId = (String)var3;
            this.m_highlightPartBone.m_partModelId = (String)var4;
            this.m_highlightPartBone.m_attachmentName = (String)var5;
            this.m_highlightBone.m_character = null;
            this.m_highlightBone.m_boneName = null;
            return null;
         case "setObjectParent":
            var24 = this.getSceneObjectById((String)var2, true);
            var24.m_translate.zero();
            var24.m_rotate.zero();
            var24.m_attachment = (String)var3;
            var24.m_parent = this.getSceneObjectById((String)var4, false);
            var24.m_parentAttachment = (String)var5;
            if (var24.m_parent != null && var24.m_parent.m_parent == var24) {
               var24.m_parent.m_parent = null;
            }

            var24.m_parentVehiclePart = null;
            return null;
         case "setObjectPosition":
            var24 = this.getSceneObjectById((String)var2, true);
            var24.m_translate.set(((Double)var3).floatValue(), ((Double)var4).floatValue(), ((Double)var5).floatValue());
            return null;
         case "setPassengerPosition":
            SceneCharacter var23 = (SceneCharacter)this.getSceneObjectById((String)var2, SceneCharacter.class, true);
            SceneVehicle var26 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
            VehicleScript.Passenger var29 = var26.m_script.getPassengerById((String)var4);
            if (var29 == null) {
               return null;
            }

            VehicleScript.Position var31 = var29.getPositionById((String)var5);
            if (var31 != null) {
               this.tempVector3f.set(var26.m_script.getModel().getOffset());
               this.tempVector3f.add(var31.getOffset());
               var23.m_translate.set(this.tempVector3f);
               var23.m_rotate.set(var31.rotate);
               var23.m_parent = var26;
               if (var23.m_animatedModel != null) {
                  String var33 = "inside".equalsIgnoreCase(var31.getId()) ? "player-vehicle" : "player-editor";
                  if (!var33.equals(var23.m_animatedModel.GetAnimSetName())) {
                     var23.m_animatedModel.setAnimSetName(var33);
                     var23.m_animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                     ActionGroup var35 = ActionGroup.getActionGroup(var23.m_animatedModel.GetAnimSetName());
                     ActionContext var38 = var23.m_animatedModel.getActionContext();
                     if (var35 != var38.getGroup()) {
                        var38.setGroup(var35);
                     }

                     var23.m_animatedModel.getAdvancedAnimator().SetState(var38.getCurrentStateName(), PZArrayUtil.listConvert(var38.getChildStates(), (var0) -> {
                        return var0.getName();
                     }));
                  }
               }
            }

            return null;
         case "uiToPolygonPoint":
            var22 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            this.m_polygonEditor.setPlane(var22.m_translate, var22.m_rotate, var22.m_plane);
            this.m_polygonEditor.uiToPlane2D(((Double)var3).floatValue(), ((Double)var4).floatValue(), (Vector2f)var5);
            return var5;
         case "uiToGrid":
            float var21 = ((Double)var2).floatValue();
            float var25 = ((Double)var3).floatValue();
            return this.uiToGrid(var21, var25, UI3DScene.GridPlane.valueOf((String)var4), (Vector3f)var5) ? Boolean.TRUE : Boolean.FALSE;
         case "updateGeometryFile":
            var8 = (String)var2;
            var9 = (String)var3;
            var10 = ((Double)var4).intValue();
            var11 = ((Double)var5).intValue();
            var12 = new ArrayList();
            Iterator var13 = this.m_objects.iterator();

            while(var13.hasNext()) {
               SceneObject var14 = (SceneObject)var13.next();
               SceneGeometry var15 = (SceneGeometry)Type.tryCastTo(var14, SceneGeometry.class);
               if (var15 != null) {
                  TileGeometryFile.Geometry var16 = var15.toGeometryFileObject();
                  if (var16 != null) {
                     var12.add(var16);
                  }
               }
            }

            TileGeometryManager.getInstance().setGeometry(var8, var9, var10, var11, var12);
            var12.clear();
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5));
      }
   }

   public Object fromLua5(String var1, Object var2, Object var3, Object var4, Object var5, Object var6) {
      float var12;
      float var13;
      switch (var1) {
         case "addPolygonPointOnEdge":
            ScenePolygon var17 = (ScenePolygon)this.getSceneObjectById((String)var2, ScenePolygon.class, true);
            float var18 = ((Double)var3).floatValue();
            float var19 = ((Double)var4).floatValue();
            var12 = ((Double)var5).floatValue();
            var13 = ((Double)var6).floatValue();
            return BoxedStaticValues.toDouble((double)var17.addPointOnEdge(var18, var19, var12, var13));
         case "pickVehiclePartBone":
            SceneVehicle var16 = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            String var10 = (String)var3;
            String var11 = (String)var4;
            var12 = ((Double)var5).floatValue();
            var13 = ((Double)var6).floatValue();
            VehicleScript.Part var14 = var16.m_script.getPartById(var10);
            VehicleScript.Model var15 = var14.getModelById(var11);
            return var16.pickBone(var14, var15, var12, var13);
         case "setObjectParentToVehiclePart":
            SceneObject var9 = this.getSceneObjectById((String)var3, true);
            var9.m_translate.zero();
            var9.m_rotate.zero();
            var9.m_parent = null;
            var9.m_attachment = null;
            var9.m_parentAttachment = null;
            var9.m_parentVehiclePart = new ParentVehiclePart();
            var9.m_parentVehiclePart.m_vehicle = (SceneVehicle)this.getSceneObjectById((String)var2, SceneVehicle.class, true);
            var9.m_parentVehiclePart.m_partId = (String)var4;
            var9.m_parentVehiclePart.m_partModelId = (String)var5;
            var9.m_parentVehiclePart.m_attachmentName = (String)var6;
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5, var6));
      }
   }

   public Object fromLua6(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7) {
      float var29;
      float var30;
      float var35;
      float var36;
      float var38;
      float var40;
      switch (var1) {
         case "addAABB":
            var29 = ((Double)var2).floatValue();
            var30 = ((Double)var3).floatValue();
            var35 = ((Double)var4).floatValue();
            var36 = ((Double)var5).floatValue();
            var38 = ((Double)var6).floatValue();
            var40 = ((Double)var7).floatValue();
            this.m_aabb.add(((AABB)s_aabbPool.alloc()).set(var29, var30, var35, var36, var38, var40, 1.0F, 1.0F, 1.0F, 1.0F, false));
            return null;
         case "addAxis":
            var29 = ((Double)var2).floatValue();
            var30 = ((Double)var3).floatValue();
            var35 = ((Double)var4).floatValue();
            var36 = ((Double)var5).floatValue();
            var38 = ((Double)var6).floatValue();
            var40 = ((Double)var7).floatValue();
            this.m_axes.add(((PositionRotation)s_posRotPool.alloc()).set(var29, var30, var35, var36, var38, var40));
            return null;
         case "addAxisRelativeToOrigin":
            var29 = ((Double)var2).floatValue();
            var30 = ((Double)var3).floatValue();
            var35 = ((Double)var4).floatValue();
            var36 = ((Double)var5).floatValue();
            var38 = ((Double)var6).floatValue();
            var40 = ((Double)var7).floatValue();
            this.m_axes.add(((PositionRotation)s_posRotPool.alloc()).set(var29, var30, var35, var36, var38, var40));
            ((PositionRotation)this.m_axes.get(this.m_axes.size() - 1)).bRelativeToOrigin = true;
            return null;
         case "addBox3D":
            Vector3f var27 = (Vector3f)var2;
            Vector3f var28 = (Vector3f)var3;
            Vector3f var33 = (Vector3f)var4;
            var36 = ((Double)var5).floatValue();
            var38 = ((Double)var6).floatValue();
            var40 = ((Double)var7).floatValue();
            this.m_box3D.add(((Box3D)s_box3DPool.alloc()).set(var27.x, var27.y, var27.z, -var28.x / 2.0F, -var28.y / 2.0F, -var28.z / 2.0F, var28.x / 2.0F, var28.y / 2.0F, var28.z / 2.0F, var33.x, var33.y, var33.z, var36, var38, var40, 1.0F, false));
            return null;
         case "getAdjacentSeatingPosition":
            var10 = (String)var2;
            SceneCharacter var26 = (SceneCharacter)this.getSceneObjectById((String)var3, SceneCharacter.class, true);
            String var31 = (String)var4;
            String var34 = (String)var5;
            String var37 = (String)var6;
            IsoSprite var39 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var31);
            Vector2f var41 = (Vector2f)var7;
            boolean var42 = SeatingManager.getInstance().getAdjacentPosition(var10, var39, var34, var37, var26.m_animatedModel.getAnimationPlayer().getModel(), "player", "sitonfurniture", "SitOnFurniture" + var37, var41);
            if (var42) {
               Vector3f var43 = SeatingManager.getInstance().getTranslation(var10, var39, var34, new Vector3f());
               var41.sub(var43.x(), var43.y());
               var41.add(var26.m_translate.x, var26.m_translate.z);
            }

            return var42 ? Boolean.TRUE : Boolean.FALSE;
         case "shiftGeometryByPixels":
            var10 = (String)var2;
            String var11 = (String)var3;
            var12 = ((Double)var4).intValue();
            int var32 = ((Double)var5).intValue();
            int var14 = ((Double)var6).intValue();
            int var15 = ((Double)var7).intValue();
            ArrayList var16 = TileGeometryManager.getInstance().getGeometry(var10, var11, var12, var32);
            if (var16 == null) {
               return null;
            } else {
               short var17 = 128;
               float var18 = 2.0F / (float)var17;
               Iterator var19 = var16.iterator();

               while(var19.hasNext()) {
                  TileGeometryFile.Geometry var20 = (TileGeometryFile.Geometry)var19.next();
                  TileGeometryFile.Box var21 = var20.asBox();
                  if (var21 != null) {
                     var21.translate.add((float)var14 * (var18 / 2.0F), 0.0F, (float)(-var14) * (var18 / 2.0F));
                  } else {
                     TileGeometryFile.Cylinder var22 = var20.asCylinder();
                     if (var22 != null) {
                        var22.translate.add((float)var14 * (var18 / 2.0F), 0.0F, (float)(-var14) * (var18 / 2.0F));
                     } else {
                        TileGeometryFile.Polygon var23 = var20.asPolygon();
                        if (var23 != null) {
                           var23.translate.add((float)var14 * (var18 / 2.0F), 0.0F, (float)(-var14) * (var18 / 2.0F));
                        }
                     }
                  }
               }

               return null;
            }
         case "renderSpriteGridTextureMask":
            SpriteGridTextureMaskDrawer var25 = new SpriteGridTextureMaskDrawer();
            var25.scene = this;
            var25.sx = ((Double)var2).floatValue();
            var25.sy = ((Double)var3).floatValue();
            var25.sx2 = ((Double)var4).floatValue();
            var25.sy2 = ((Double)var5).floatValue();
            var25.pixelSize = ((Double)var6).floatValue();
            var25.sprite = (IsoSprite)var7;
            var25.r = 1.0F;
            var25.g = 0.0F;
            var25.b = 0.0F;
            var25.a = 1.0F;
            SpriteRenderer.instance.drawGeneric(var25);
            return null;
         case "renderTextureMask":
            TextureMaskDrawer var24 = new TextureMaskDrawer();
            var24.scene = this;
            var24.sx = ((Double)var2).floatValue();
            var24.sy = ((Double)var3).floatValue();
            var24.sx2 = ((Double)var4).floatValue();
            var24.sy2 = ((Double)var5).floatValue();
            var24.pixelSize = ((Double)var6).floatValue();
            var24.texture = (Texture)var7;
            var24.r = 1.0F;
            var24.g = 0.0F;
            var24.b = 0.0F;
            var24.a = 1.0F;
            SpriteRenderer.instance.drawGeneric(var24);
            return null;
         case "setGizmoOrigin":
            switch ((String)var2) {
               case "vehiclePart":
                  SceneVehicle var13 = (SceneVehicle)this.getSceneObjectById((String)var3, SceneVehicle.class, true);
                  this.m_gizmoParent = var13;
                  this.m_originVehiclePart.m_vehicle = var13;
                  this.m_originVehiclePart.m_partId = (String)var4;
                  this.m_originVehiclePart.m_partModelId = (String)var5;
                  this.m_originVehiclePart.m_attachmentName = (String)var6;
                  this.m_originVehiclePart.m_bBoneOnly = (Boolean)var7;
                  this.m_gizmoOrigin = this.m_originVehiclePart;
                  this.m_gizmoChild = null;
               default:
                  return null;
            }
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5, var6, var7));
      }
   }

   public Object fromLua7(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7, Object var8) {
      float var15;
      float var16;
      float var17;
      Vector3f var18;
      Vector3f var19;
      switch (var1) {
         case "addBox3D":
            var18 = (Vector3f)var2;
            var19 = (Vector3f)var3;
            Vector3f var21 = (Vector3f)var4;
            Vector3f var22 = (Vector3f)var5;
            var15 = ((Double)var6).floatValue();
            var16 = ((Double)var7).floatValue();
            var17 = ((Double)var8).floatValue();
            this.m_box3D.add(((Box3D)s_box3DPool.alloc()).set(var18.x, var18.y, var18.z, var21.x, var21.y, var21.z, var22.x, var22.y, var22.z, var19.x, var19.y, var19.z, var15, var16, var17, 1.0F, false));
            return null;
         case "addPhysicsMesh":
            var18 = (Vector3f)var2;
            var19 = (Vector3f)var3;
            float var20 = ((Double)var4).floatValue();
            String var14 = (String)var5;
            var15 = ((Double)var6).floatValue();
            var16 = ((Double)var7).floatValue();
            var17 = ((Double)var8).floatValue();
            this.m_physicsMesh.add(((PhysicsMesh)s_physicsMeshPool.alloc()).set(var18, var19, var20, var14, var15, var16, var17));
            return null;
         case "createDepthTexture":
            SceneObject var11 = this.getSceneObjectById((String)var2, false);
            if (var11 != null) {
               throw new IllegalStateException("scene object \"" + var2 + "\" exists");
            } else {
               SceneDepthTexture var12 = new SceneDepthTexture(this, (String)var2);
               var12.m_texture = (Texture)var3;

               for(int var13 = 0; var13 < this.m_objects.size(); ++var13) {
                  if (!(this.m_objects.get(var13) instanceof SceneDepthTexture)) {
                     this.m_objects.add(var13, var12);
                     return var12;
                  }
               }

               this.m_objects.add(var12);
               return var12;
            }
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5, var6, var7, var8));
      }
   }

   public Object fromLua9(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7, Object var8, Object var9, Object var10) {
      float var17;
      float var18;
      float var19;
      float var20;
      switch (var1) {
         case "addAABB":
            float var23 = ((Double)var2).floatValue();
            float var24 = ((Double)var3).floatValue();
            float var25 = ((Double)var4).floatValue();
            float var26 = ((Double)var5).floatValue();
            var17 = ((Double)var6).floatValue();
            var18 = ((Double)var7).floatValue();
            var19 = ((Double)var8).floatValue();
            var20 = ((Double)var9).floatValue();
            float var22 = ((Double)var10).floatValue();
            this.m_aabb.add(((AABB)s_aabbPool.alloc()).set(var23, var24, var25, var26, var17, var18, var19, var20, var22, 1.0F, false));
            return null;
         case "addBox3D":
            Vector3f var13 = (Vector3f)var2;
            Vector3f var14 = (Vector3f)var3;
            Vector3f var15 = (Vector3f)var4;
            Vector3f var16 = (Vector3f)var5;
            var17 = ((Double)var6).floatValue();
            var18 = ((Double)var7).floatValue();
            var19 = ((Double)var8).floatValue();
            var20 = ((Double)var9).floatValue();
            boolean var21 = (Boolean)var10;
            this.m_box3D.add(((Box3D)s_box3DPool.alloc()).set(var13.x, var13.y, var13.z, var15.x, var15.y, var15.z, var16.x, var16.y, var16.z, var14.x, var14.y, var14.z, var17, var18, var19, var20, var21));
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

      if (this.m_view == UI3DScene.View.UserDefined) {
         Vector3f var5 = allocVector3f();
         switch (this.m_gridPlane) {
            case XY:
               var5.set(0.0F, 0.0F, 1.0F);
               break;
            case XZ:
               var5.set(0.0F, 1.0F, 0.0F);
               break;
            case YZ:
               var5.set(1.0F, 0.0F, 0.0F);
         }

         Vector3f var6 = allocVector3f().set(0.0F);
         Plane var7 = allocPlane().set(var5, var6);
         releaseVector3f(var5);
         releaseVector3f(var6);
         Ray var8 = this.getCameraRay(var1, (float)this.screenHeight() - var2, allocRay());
         if (intersect_ray_plane(var7, var8, var4) != 1) {
            var4.set(0.0F);
         }

         releasePlane(var7);
         releaseRay(var8);
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
      Matrix4f var4 = allocMatrix4f();
      var4.set(this.m_projection);
      var4.mul(this.m_modelView);
      this.m_viewport[2] = this.screenWidth();
      this.m_viewport[3] = this.screenHeight();
      var4.project(var1, var2, var3, this.m_viewport, this.tempVector3f);
      releaseMatrix4f(var4);
      return (float)this.screenHeight() - this.tempVector3f.y();
   }

   public float sceneToUIX(Vector3f var1) {
      return this.sceneToUIX(var1.x, var1.y, var1.z);
   }

   public float sceneToUIY(Vector3f var1) {
      return this.sceneToUIY(var1.x, var1.y, var1.z);
   }

   public boolean uiToGrid(float var1, float var2, GridPlane var3, Vector3f var4) {
      Plane var5 = allocPlane();
      var5.point.set(0.0F);
      switch (var3) {
         case XY:
            var5.normal.set(0.0F, 0.0F, 1.0F);
            break;
         case XZ:
            var5.normal.set(0.0F, 1.0F, 0.0F);
            break;
         case YZ:
            var5.normal.set(1.0F, 0.0F, 0.0F);
      }

      Ray var6 = this.getCameraRay(var1, (float)this.screenHeight() - var2, allocRay());
      boolean var7 = intersect_ray_plane(var5, var6, var4) == 1;
      if (!var7) {
         var4.set(0.0F);
      }

      releasePlane(var5);
      releaseRay(var6);
      return var7;
   }

   private void renderGridXY(int var1) {
      int var2;
      int var3;
      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboRenderer.addLine((float)var2 + (float)var3 / (float)var1, -5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 5.0F, 0.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboRenderer.addLine(-5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboRenderer.addLine((float)var2, -5.0F, 0.0F, (float)var2, 5.0F, 0.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboRenderer.addLine(-5.0F, (float)var2, 0.0F, 5.0F, (float)var2, 0.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      if (this.m_bDrawGridAxes) {
         byte var4 = 0;
         vboRenderer.addLine(-5.0F, 0.0F, (float)var4, 5.0F, 0.0F, (float)var4, 1.0F, 0.0F, 0.0F, this.GRID_ALPHA);
         var4 = 0;
         vboRenderer.addLine(0.0F, -5.0F, (float)var4, 0.0F, 5.0F, (float)var4, 0.0F, 1.0F, 0.0F, this.GRID_ALPHA);
      }

   }

   private void renderGridXZ(int var1) {
      int var2;
      int var3;
      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboRenderer.addLine((float)var2 + (float)var3 / (float)var1, 0.0F, -5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 5.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboRenderer.addLine(-5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboRenderer.addLine((float)var2, 0.0F, -5.0F, (float)var2, 0.0F, 5.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboRenderer.addLine(-5.0F, 0.0F, (float)var2, 5.0F, 0.0F, (float)var2, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      if (this.m_bDrawGridAxes) {
         byte var4 = 0;
         vboRenderer.addLine(-5.0F, 0.0F, (float)var4, 5.0F, 0.0F, (float)var4, 1.0F, 0.0F, 0.0F, this.GRID_ALPHA);
         var4 = 0;
         vboRenderer.addLine((float)var4, 0.0F, -5.0F, (float)var4, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, this.GRID_ALPHA);
      }

   }

   private void renderGridYZ(int var1) {
      int var2;
      int var3;
      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboRenderer.addLine(0.0F, (float)var2 + (float)var3 / (float)var1, -5.0F, 0.0F, (float)var2 + (float)var3 / (float)var1, 5.0F, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 < 5; ++var2) {
         for(var3 = 1; var3 < var1; ++var3) {
            vboRenderer.addLine(0.0F, -5.0F, (float)var2 + (float)var3 / (float)var1, 0.0F, 5.0F, (float)var2 + (float)var3 / (float)var1, 0.2F, 0.2F, 0.2F, this.GRID_ALPHA);
         }
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboRenderer.addLine(0.0F, (float)var2, -5.0F, 0.0F, (float)var2, 5.0F, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      for(var2 = -5; var2 <= 5; ++var2) {
         vboRenderer.addLine(0.0F, -5.0F, (float)var2, 0.0F, 5.0F, (float)var2, 0.1F, 0.1F, 0.1F, this.GRID_ALPHA);
      }

      if (this.m_bDrawGridAxes) {
         byte var4 = 0;
         vboRenderer.addLine(0.0F, -5.0F, (float)var4, 0.0F, 5.0F, (float)var4, 0.0F, 1.0F, 0.0F, this.GRID_ALPHA);
         var4 = 0;
         vboRenderer.addLine((float)var4, 0.0F, -5.0F, (float)var4, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, this.GRID_ALPHA);
      }

   }

   private void renderGrid() {
      vboRenderer.startRun(vboRenderer.FORMAT_PositionColor);
      vboRenderer.setMode(1);
      vboRenderer.setLineWidth(1.0F);
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
            vboRenderer.endRun();
            return;
         case Top:
         case Bottom:
            this.renderGridXZ(10);
            vboRenderer.endRun();
            return;
         case Front:
         case Back:
            this.renderGridXY(10);
            vboRenderer.endRun();
            return;
         default:
            switch (this.m_gridPlane) {
               case XY:
                  this.renderGridXY(10);
                  vboRenderer.endRun();
                  break;
               case XZ:
                  this.renderGridXZ(10);
                  vboRenderer.endRun();
                  break;
               case YZ:
                  this.renderGridYZ(10);
                  vboRenderer.endRun();
            }

      }
   }

   void renderAxis(PositionRotation var1) {
      this.renderAxis(var1.pos, var1.rot, var1.bRelativeToOrigin);
   }

   void renderAxis(Vector3f var1, Vector3f var2, boolean var3) {
      StateData var4 = this.stateDataRender();
      vboRenderer.flush();
      Matrix4f var5 = allocMatrix4f().identity();
      if (!var3) {
         var5.mul(var4.m_gizmoParentTransform);
         var5.mul(var4.m_gizmoOriginTransform);
         var5.mul(var4.m_gizmoChildTransform);
         if (var4.m_selectedAttachmentIsChildAttachment) {
            var5.mul(var4.m_gizmoChildAttachmentTransformInv);
         }
      }

      var5.translate(var1);
      var5.rotateXYZ(var2.x * 0.017453292F, var2.y * 0.017453292F, var2.z * 0.017453292F);
      var4.m_modelView.mul(var5, var5);
      PZGLUtil.pushAndLoadMatrix(5888, var5);
      releaseMatrix4f(var5);
      float var6 = 0.1F;
      Model.debugDrawAxis(0.0F, 0.0F, 0.0F, var6, 3.0F);
      PZGLUtil.popMatrix(5888);
   }

   private void renderAABB(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, boolean var14) {
      vboRenderer.addAABB(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13, var14);
   }

   private void renderAABB(float var1, float var2, float var3, Vector3f var4, Vector3f var5, float var6, float var7, float var8) {
      vboRenderer.addAABB(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   private void renderBox3D(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, boolean var17) {
      StateData var18 = this.stateDataRender();
      vboRenderer.flush();
      Matrix4f var19 = allocMatrix4f();
      var19.identity();
      var19.translate(var1, var2, var3);
      var19.rotateXYZ(var10 * 0.017453292F, var11 * 0.017453292F, var12 * 0.017453292F);
      var18.m_modelView.mul(var19, var19);
      PZGLUtil.pushAndLoadMatrix(5888, var19);
      releaseMatrix4f(var19);
      this.renderAABB(var1 * 0.0F, var2 * 0.0F, var3 * 0.0F, var4, var5, var6, var7, var8, var9, var13, var14, var15, var16, var17);
      vboRenderer.flush();
      PZGLUtil.popMatrix(5888);
   }

   private void renderPhysicsMesh(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float[] var10) {
      StateData var11 = this.stateDataRender();
      vboRenderer.flush();
      Matrix4f var12 = allocMatrix4f();
      var12.identity();
      var12.translate(var1, var2, var3);
      var12.rotateXYZ(var4 * 0.017453292F, var5 * 0.017453292F, var6 * 0.017453292F);
      var11.m_modelView.mul(var12, var12);
      PZGLUtil.pushAndLoadMatrix(5888, var12);
      releaseMatrix4f(var12);
      vboRenderer.startRun(vboRenderer.FORMAT_PositionColor);
      vboRenderer.setMode(1);

      for(int var13 = 0; var13 < var10.length / 3 - 1; ++var13) {
         int var14 = var13 * 3;
         int var15 = (var13 + 1) * 3;
         vboRenderer.addLine(var10[var14], var10[var14 + 1], var10[var14 + 2], var10[var15], var10[var15 + 1], var10[var15 + 2], var7, var8, var9, 1.0F);
      }

      vboRenderer.endRun();
      vboRenderer.flush();
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
      return this.getCameraRay(var1, var2, var3, var4, this.screenWidth(), this.screenHeight(), var5);
   }

   Ray getCameraRay(float var1, float var2, Matrix4f var3, Matrix4f var4, int var5, int var6, Ray var7) {
      Matrix4f var8 = allocMatrix4f();
      var8.set(var3);
      var8.mul(var4);
      var8.invert();
      this.m_viewport[2] = var5;
      this.m_viewport[3] = var6;
      Vector3f var9 = var8.unprojectInv(var1, var2, 0.0F, this.m_viewport, allocVector3f());
      Vector3f var10 = var8.unprojectInv(var1, var2, 1.0F, this.m_viewport, allocVector3f());
      var7.origin.set(var9);
      var7.direction.set(var10.sub(var9).normalize());
      releaseVector3f(var10);
      releaseVector3f(var9);
      releaseMatrix4f(var8);
      return var7;
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

      byte var8;
      try {
         float var5 = var0.normal.dot(var3);
         float var6 = -var0.normal.dot(var4);
         if (Math.abs(var5) < 1.0E-8F) {
            byte var12;
            if (var6 == 0.0F) {
               var12 = 2;
               return var12;
            }

            var12 = 0;
            return var12;
         }

         float var7 = var6 / var5;
         if (!(var7 < 0.0F) && !(var7 > 1.0F)) {
            var2.set(var1.origin).add(var3.mul(var7));
            var8 = 1;
            return var8;
         }

         var8 = 0;
      } finally {
         releaseVector3f(var3);
         releaseVector3f(var4);
      }

      return var8;
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

   public static enum GridPlane {
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
      final Matrix4f m_gizmoChildAttachmentTransform = new Matrix4f();
      final Matrix4f m_gizmoChildAttachmentTransformInv = new Matrix4f();
      final Matrix4f m_gizmoTransform = new Matrix4f();
      boolean m_hasGizmoOrigin;
      boolean m_gizmoOriginIsGeometry;
      boolean m_selectedAttachmentIsChildAttachment;
      Axis m_gizmoAxis;
      final TranslateGizmoRenderData m_translateGizmoRenderData;
      final ArrayList<PositionRotation> m_axes;
      final ArrayList<AABB> m_aabb;
      final ArrayList<Box3D> m_box3D;
      final ArrayList<PhysicsMesh> m_physicsMesh;

      private StateData() {
         this.m_gizmoAxis = UI3DScene.Axis.None;
         this.m_translateGizmoRenderData = new TranslateGizmoRenderData();
         this.m_axes = new ArrayList();
         this.m_aabb = new ArrayList();
         this.m_box3D = new ArrayList();
         this.m_physicsMesh = new ArrayList();
      }

      private float zoomMult() {
         return (float)Math.exp((double)((float)this.m_zoom * 0.2F)) * 160.0F / Math.max(1.82F, 1.0F);
      }
   }

   private final class RotateGizmo extends Gizmo {
      Axis m_trackAxis;
      boolean m_bSnap;
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
         this.m_bSnap = true;
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
            UI3DScene.this.setModelViewProjection(var3);
            UI3DScene.this.setGizmoTransforms(var3);
            var2 = (float)UI3DScene.this.screenHeight() - var2;
            Ray var4 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Matrix4f var5 = UI3DScene.allocMatrix4f();
            var5.set(var3.m_gizmoParentTransform);
            var5.mul(var3.m_gizmoOriginTransform);
            var5.mul(var3.m_gizmoChildTransform);
            if (var3.m_selectedAttachmentIsChildAttachment) {
               var5.mul(var3.m_gizmoChildAttachmentTransformInv);
            }

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
         if (!var4.m_selectedAttachmentIsChildAttachment) {
            this.m_startXfrm.mul(var4.m_gizmoTransform);
         }

         if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
            this.m_startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
         }

         this.m_startInvXfrm.set(var4.m_gizmoParentTransform);
         this.m_startInvXfrm.mul(var4.m_gizmoOriginTransform);
         this.m_startInvXfrm.mul(var4.m_gizmoChildTransform);
         if (!var4.m_selectedAttachmentIsChildAttachment) {
            this.m_startInvXfrm.mul(var4.m_gizmoTransform);
         }

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
            if (GameKeyboard.isKeyDown(29)) {
               if (var4 > 0.0F) {
                  var4 = (float)((int)(var4 / 5.0F) * 5);
               } else {
                  var4 = (float)(Math.round(-var4 / 5.0F) * -5);
               }
            }

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
            Ray var6 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Vector3f var7 = this.m_startXfrm.transformDirection(UI3DScene.allocVector3f().set(var5)).normalize();
            float var8 = var6.direction.dot(var7);
            UI3DScene.releaseVector3f(var7);
            UI3DScene.releaseRay(var6);
            if (UI3DScene.this.m_gizmoParent instanceof SceneCharacter) {
               if (var8 > 0.0F) {
                  var4 *= -1.0F;
               }
            } else if (UI3DScene.this.m_gizmoOrigin instanceof OriginVehiclePart) {
               if (var8 > 0.0F) {
                  var4 *= -1.0F;
               }
            } else if (var8 < 0.0F) {
               var4 *= -1.0F;
            }

            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               this.m_startInvXfrm.transformDirection(var5);
            }

            Quaternionf var9 = UI3DScene.allocQuaternionf().fromAxisAngleDeg(var5, var4);
            UI3DScene.releaseVector3f(var5);
            var7 = var9.getEulerAnglesXYZ(new Vector3f());
            UI3DScene.releaseQuaternionf(var9);
            var7.mul(57.295776F);
            if (this.m_bSnap) {
            }

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
            if (var1.m_selectedAttachmentIsChildAttachment) {
               var2.mul(var1.m_gizmoChildAttachmentTransformInv);
            }

            var2.mul(var1.m_gizmoTransform);
            Vector3f var3 = var2.getScale(UI3DScene.allocVector3f());
            var2.scale(1.0F / var3.x, 1.0F / var3.y, 1.0F / var3.z);
            UI3DScene.releaseVector3f(var3);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var2.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            float var13 = (float)(Mouse.getXA() - UI3DScene.this.getAbsoluteX().intValue());
            float var4 = (float)(Mouse.getYA() - UI3DScene.this.getAbsoluteY().intValue());
            Ray var5 = UI3DScene.this.getCameraRay(var13, (float)UI3DScene.this.screenHeight() - var4, var1.m_projection, var1.m_modelView, UI3DScene.allocRay());
            float var6 = UI3DScene.this.m_gizmoScale / var1.zoomMult() * 1000.0F;
            float var7 = this.LENGTH * var6;
            GL11.glClear(256);
            GL11.glEnable(2929);
            GL11.glDepthFunc(513);
            Matrix4f var8 = UI3DScene.allocMatrix4f();
            Axis var9 = this.m_trackAxis == UI3DScene.Axis.None ? var1.m_gizmoAxis : this.m_trackAxis;
            float var10;
            float var11;
            float var12;
            if (this.m_trackAxis == UI3DScene.Axis.None || this.m_trackAxis == UI3DScene.Axis.X) {
               var10 = var9 == UI3DScene.Axis.X ? 1.0F : 0.5F;
               var11 = 0.0F;
               var12 = 0.0F;
               var8.set(var2);
               var8.rotateY(1.5707964F);
               this.renderAxis(var8, 0.01F * var6, var7 / 2.0F, var10, var11, var12, var5);
            }

            if (this.m_trackAxis == UI3DScene.Axis.None || this.m_trackAxis == UI3DScene.Axis.Y) {
               var10 = 0.0F;
               var11 = var9 == UI3DScene.Axis.Y ? 1.0F : 0.5F;
               var12 = 0.0F;
               var8.set(var2);
               var8.rotateX(1.5707964F);
               this.renderAxis(var8, 0.01F * var6, var7 / 2.0F, var10, var11, var12, var5);
            }

            if (this.m_trackAxis == UI3DScene.Axis.None || this.m_trackAxis == UI3DScene.Axis.Z) {
               var10 = 0.0F;
               var11 = 0.0F;
               var12 = var9 == UI3DScene.Axis.Z ? 1.0F : 0.5F;
               var8.set(var2);
               this.renderAxis(var8, 0.01F * var6, var7 / 2.0F, var10, var11, var12, var5);
            }

            UI3DScene.releaseMatrix4f(var8);
            UI3DScene.releaseRay(var5);
            UI3DScene.releaseMatrix4f(var2);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            GL11.glDepthFunc(519);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
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

      void renderAxis(Matrix4f var1, float var2, float var3, float var4, float var5, float var6, Ray var7) {
         Ray var8 = UI3DScene.allocRay().set(var7);
         var1.invert();
         var1.transformPosition(var8.origin);
         var1.transformDirection(var8.direction);
         var1.invert();
         VBORenderer var9 = VBORenderer.getInstance();
         var9.cmdPushAndMultMatrix(5888, var1);
         var9.addTorus((double)var2, (double)var3, 8, 32, var4, var5, var6, var8);
         var9.cmdPopMatrix(5888);
         var9.flush();
         UI3DScene.releaseRay(var8);
      }

      void renderAxis(Vector3f var1, float var2, Vector3f var3, Vector3f var4, float var5, float var6, float var7, Ray var8) {
         UI3DScene.vboRenderer.flush();
         UI3DScene.vboRenderer.setLineWidth(6.0F);
         this.getCircleSegments(var1, var2, var3, var4, this.m_circlePointsRender);
         Vector3f var9 = UI3DScene.allocVector3f();
         Vector3f var10 = (Vector3f)this.m_circlePointsRender.get(0);

         for(int var11 = 1; var11 < this.m_circlePointsRender.size(); ++var11) {
            Vector3f var12 = (Vector3f)this.m_circlePointsRender.get(var11);
            var9.set(var12.x - var1.x, var12.y - var1.y, var12.z - var1.z).normalize();
            float var13 = var9.dot(var8.direction);
            if (var13 < 0.1F) {
               UI3DScene.vboRenderer.addLine(var10.x, var10.y, var10.z, var12.x, var12.y, var12.z, var5, var6, var7, 1.0F);
            } else {
               UI3DScene.vboRenderer.addLine(var10.x, var10.y, var10.z, var12.x, var12.y, var12.z, var5 / 2.0F, var6 / 2.0F, var7 / 2.0F, 0.25F);
            }

            var10 = var12;
         }

         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(this.m_circlePointsRender);
         this.m_circlePointsRender.clear();
         UI3DScene.releaseVector3f(var9);
         UI3DScene.vboRenderer.flush();
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
      boolean m_bSnap;
      boolean m_hideX;
      boolean m_hideY;
      boolean m_hideZ;
      final Cylinder cylinder;

      private ScaleGizmo() {
         super();
         this.m_trackAxis = UI3DScene.Axis.None;
         this.m_bSnap = true;
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
            if (var3.m_selectedAttachmentIsChildAttachment) {
               var4.mul(var3.m_gizmoChildAttachmentTransformInv);
            }

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
         this.m_startXfrm.mul(var4.m_gizmoChildTransform);
         if (!var4.m_selectedAttachmentIsChildAttachment) {
            this.m_startXfrm.mul(var4.m_gizmoTransform);
         }

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
            this.m_currentPos.set(var3);
            UI3DScene.releaseVector3f(var3);
            StateData var4 = UI3DScene.this.stateDataMain();
            Vector3f var5 = (new Vector3f(this.m_currentPos)).sub(this.m_startPos);
            Vector3f var6;
            Vector3f var7;
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var6 = this.m_startInvXfrm.transformPosition(this.m_startPos, UI3DScene.allocVector3f());
               var7 = this.m_startInvXfrm.transformPosition(this.m_currentPos, UI3DScene.allocVector3f());
               Matrix4f var8 = UI3DScene.allocMatrix4f();
               var8.set(var4.m_gizmoParentTransform);
               var8.mul(var4.m_gizmoOriginTransform);
               if (!var4.m_selectedAttachmentIsChildAttachment) {
                  var8.mul(var4.m_gizmoChildTransform);
               }

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
               var5.set(var7).sub(var6);
               UI3DScene.releaseVector3f(var6);
               UI3DScene.releaseVector3f(var7);
            }

            if (this.m_bSnap) {
               var5.x = (float)PZMath.fastfloor(var5.x * UI3DScene.this.gridMult()) / UI3DScene.this.gridMult();
               var5.y = (float)PZMath.fastfloor(var5.y * UI3DScene.this.gridMult()) / UI3DScene.this.gridMult();
               var5.z = (float)PZMath.fastfloor(var5.z * UI3DScene.this.gridMult()) / UI3DScene.this.gridMult();
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
            float var2 = UI3DScene.this.m_gizmoScale / var1.zoomMult() * 1000.0F;
            float var3 = this.LENGTH * var2;
            float var4 = this.THICKNESS * var2;
            float var5 = 0.1F * var2;
            Matrix4f var6 = UI3DScene.allocMatrix4f();
            var6.set(var1.m_gizmoParentTransform);
            var6.mul(var1.m_gizmoOriginTransform);
            var6.mul(var1.m_gizmoChildTransform);
            if (var1.m_selectedAttachmentIsChildAttachment) {
               var6.mul(var1.m_gizmoChildAttachmentTransformInv);
            }

            var6.mul(var1.m_gizmoTransform);
            Vector3f var7 = var6.getScale(UI3DScene.allocVector3f());
            var6.scale(1.0F / var7.x, 1.0F / var7.y, 1.0F / var7.z);
            UI3DScene.releaseVector3f(var7);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var6.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            var1.m_modelView.mul(var6, var6);
            VBORenderer var12 = VBORenderer.getInstance();
            var12.cmdPushAndLoadMatrix(5888, var6);
            float var8;
            float var9;
            float var10;
            float var11;
            if (!this.m_hideX) {
               var8 = var1.m_gizmoAxis != UI3DScene.Axis.X && this.m_trackAxis != UI3DScene.Axis.X ? 0.5F : 1.0F;
               var9 = 0.0F;
               var10 = 0.0F;
               var11 = 1.0F;
               var6.rotation(1.5707964F, 0.0F, 1.0F, 0.0F);
               var6.translate(0.0F, 0.0F, var5);
               var12.cmdPushAndMultMatrix(5888, var6);
               var12.addCylinder_Fill(var4 / 2.0F, var4 / 2.0F, var3, 8, 1, var8, var9, var10, var11);
               var12.cmdPopMatrix(5888);
               var6.translate(0.0F, 0.0F, var3);
               var12.cmdPushAndMultMatrix(5888, var6);
               var12.addCylinder_Fill(var4, var4, 0.1F * var2, 8, 1, var8, var9, var10, var11);
               var12.cmdPopMatrix(5888);
            }

            if (!this.m_hideY) {
               var8 = 0.0F;
               var9 = var1.m_gizmoAxis != UI3DScene.Axis.Y && this.m_trackAxis != UI3DScene.Axis.Y ? 0.5F : 1.0F;
               var10 = 0.0F;
               var11 = 1.0F;
               var6.rotation(-1.5707964F, 1.0F, 0.0F, 0.0F);
               var6.translate(0.0F, 0.0F, var5);
               var12.cmdPushAndMultMatrix(5888, var6);
               var12.addCylinder_Fill(var4 / 2.0F, var4 / 2.0F, var3, 8, 1, var8, var9, var10, var11);
               var12.cmdPopMatrix(5888);
               var6.translate(0.0F, 0.0F, var3);
               var12.cmdPushAndMultMatrix(5888, var6);
               var12.addCylinder_Fill(var4, var4, 0.1F * var2, 8, 1, var8, var9, var10, var11);
               var12.cmdPopMatrix(5888);
            }

            if (!this.m_hideZ) {
               var8 = 0.0F;
               var9 = 0.0F;
               var10 = var1.m_gizmoAxis != UI3DScene.Axis.Z && this.m_trackAxis != UI3DScene.Axis.Z ? 0.5F : 1.0F;
               var11 = 1.0F;
               var6.translation(0.0F, 0.0F, var5);
               var12.cmdPushAndMultMatrix(5888, var6);
               var12.addCylinder_Fill(var4 / 2.0F, var4 / 2.0F, var3, 8, 1, var8, var9, var10, var11);
               var12.cmdPopMatrix(5888);
               var6.translate(0.0F, 0.0F, var3);
               var12.cmdPushAndMultMatrix(5888, var6);
               var12.addCylinder_Fill(var4, var4, 0.1F * var2, 8, 1, var8, var9, var10, var11);
               var12.cmdPopMatrix(5888);
            }

            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            UI3DScene.releaseMatrix4f(var6);
            var12.cmdPopMatrix(5888);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
         }
      }
   }

   private final class TranslateGizmo extends Gizmo {
      final Matrix4f m_startXfrm = new Matrix4f();
      final Matrix4f m_startInvXfrm = new Matrix4f();
      final Vector3f m_startPos = new Vector3f();
      final Vector3f m_currentPos = new Vector3f();
      Axis m_trackAxis;
      boolean m_bDoubleAxis;
      final PartialDisk disk;

      private TranslateGizmo() {
         super();
         this.m_trackAxis = UI3DScene.Axis.None;
         this.m_bDoubleAxis = false;
         this.disk = new PartialDisk();
      }

      Axis hitTest(float var1, float var2) {
         if (!this.m_visible) {
            return UI3DScene.Axis.None;
         } else {
            StateData var3 = UI3DScene.this.stateDataMain();
            UI3DScene.this.setModelViewProjection(var3);
            UI3DScene.this.setGizmoTransforms(var3);
            Matrix4f var4 = UI3DScene.allocMatrix4f();
            var4.set(var3.m_gizmoParentTransform);
            var4.mul(var3.m_gizmoOriginTransform);
            var4.mul(var3.m_gizmoChildTransform);
            if (var3.m_selectedAttachmentIsChildAttachment) {
               var4.mul(var3.m_gizmoChildAttachmentTransformInv);
            }

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
            if (!UI3DScene.this.m_gizmoAxisVisibleX || var12 < var10 || var12 >= var10 + var8) {
               var12 = 3.4028235E38F;
               var11 = 3.4028235E38F;
            }

            float var14 = var6.direction.dot(var5.direction);
            var3.m_translateGizmoRenderData.m_hideX = !UI3DScene.this.m_gizmoAxisVisibleX || Math.abs(var14) > 0.9F;
            var4.transformDirection(var6.direction.set(0.0F, 1.0F, 0.0F)).normalize();
            float var15 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var16 = var6.t;
            float var17 = var5.t;
            if (!UI3DScene.this.m_gizmoAxisVisibleY || var16 < var10 || var16 >= var10 + var8) {
               var16 = 3.4028235E38F;
               var15 = 3.4028235E38F;
            }

            float var18 = var6.direction.dot(var5.direction);
            var3.m_translateGizmoRenderData.m_hideY = !UI3DScene.this.m_gizmoAxisVisibleY || Math.abs(var18) > 0.9F;
            var4.transformDirection(var6.direction.set(0.0F, 0.0F, 1.0F)).normalize();
            float var19 = UI3DScene.this.closest_distance_between_lines(var6, var5);
            float var20 = var6.t;
            float var21 = var5.t;
            if (!UI3DScene.this.m_gizmoAxisVisibleZ || var20 < var10 || var20 >= var10 + var8) {
               var20 = 3.4028235E38F;
               var19 = 3.4028235E38F;
            }

            float var22 = var6.direction.dot(var5.direction);
            var3.m_translateGizmoRenderData.m_hideZ = !UI3DScene.this.m_gizmoAxisVisibleZ || Math.abs(var22) > 0.9F;
            Axis var23 = UI3DScene.Axis.None;
            if (this.m_bDoubleAxis) {
               float var24 = var9 * 1.5F;
               float var25 = var24 + var10;
               float var26 = var25 + var8 / 2.0F;
               Vector3f var27 = UI3DScene.allocVector3f();
               Vector2f var28 = UI3DScene.allocVector2f();
               if (UI3DScene.this.m_gizmoOrigin instanceof SceneCharacter) {
                  var4.scale(0.6666667F);
               }

               if (this.getPointOnDualAxis(var1, -(var2 - (float)UI3DScene.this.screenHeight()), UI3DScene.Axis.XY, var4, var27, var28) && var28.x >= 0.0F && var28.y >= 0.0F && var28.length() >= var25 && var28.length() < var26) {
                  var23 = UI3DScene.Axis.XY;
               }

               if (this.getPointOnDualAxis(var1, -(var2 - (float)UI3DScene.this.screenHeight()), UI3DScene.Axis.XZ, var4, var27, var28) && var28.x >= 0.0F && var28.y >= 0.0F && var28.length() >= var25 && var28.length() < var26) {
                  var23 = UI3DScene.Axis.XZ;
               }

               if (this.getPointOnDualAxis(var1, -(var2 - (float)UI3DScene.this.screenHeight()), UI3DScene.Axis.YZ, var4, var27, var28) && var28.x >= 0.0F && var28.y >= 0.0F && var28.length() >= var25 && var28.length() < var26) {
                  var23 = UI3DScene.Axis.YZ;
               }

               UI3DScene.releaseVector3f(var27);
               UI3DScene.releaseVector2f(var28);
            }

            UI3DScene.releaseRay(var6);
            UI3DScene.releaseRay(var5);
            UI3DScene.releaseMatrix4f(var4);
            if (var23 != UI3DScene.Axis.None) {
               return var23;
            } else if (var12 >= var10 && var12 < var10 + var8 && var11 < var15 && var11 < var19) {
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
         UI3DScene.this.setModelViewProjection(var4);
         UI3DScene.this.setGizmoTransforms(var4);
         this.m_startXfrm.set(var4.m_gizmoParentTransform);
         this.m_startXfrm.mul(var4.m_gizmoOriginTransform);
         this.m_startXfrm.mul(var4.m_gizmoChildTransform);
         if (!var4.m_selectedAttachmentIsChildAttachment) {
            this.m_startXfrm.mul(var4.m_gizmoTransform);
         }

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
            this.m_currentPos.set(var3);
            UI3DScene.releaseVector3f(var3);
            StateData var4 = UI3DScene.this.stateDataMain();
            UI3DScene.this.setModelViewProjection(var4);
            UI3DScene.this.setGizmoTransforms(var4);
            Vector3f var5 = (new Vector3f(this.m_currentPos)).sub(this.m_startPos);
            if (UI3DScene.this.m_selectedAttachment == null && UI3DScene.this.m_gizmoChild == null && !var4.m_gizmoOriginIsGeometry) {
               var5.set(this.m_currentPos).sub(this.m_startPos);
            } else {
               Vector3f var6;
               Vector3f var7;
               Matrix4f var8;
               if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
                  var6 = this.m_startInvXfrm.transformPosition(this.m_startPos, UI3DScene.allocVector3f());
                  var7 = this.m_startInvXfrm.transformPosition(this.m_currentPos, UI3DScene.allocVector3f());
                  var8 = UI3DScene.allocMatrix4f();
                  var8.set(var4.m_gizmoParentTransform);
                  var8.mul(var4.m_gizmoOriginTransform);
                  if (!var4.m_selectedAttachmentIsChildAttachment) {
                     var8.mul(var4.m_gizmoChildTransform);
                  }

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
            if (var1.m_selectedAttachmentIsChildAttachment) {
               var2.mul(var1.m_gizmoChildAttachmentTransformInv);
            }

            var2.mul(var1.m_gizmoTransform);
            Vector3f var3 = var2.getScale(UI3DScene.allocVector3f());
            var2.scale(1.0F / var3.x, 1.0F / var3.y, 1.0F / var3.z);
            UI3DScene.releaseVector3f(var3);
            if (UI3DScene.this.m_transformMode == UI3DScene.TransformMode.Global) {
               var2.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            var1.m_modelView.mul(var2, var2);
            float var13 = UI3DScene.this.m_gizmoScale / var1.zoomMult() * 1000.0F;
            float var4 = this.THICKNESS * var13;
            float var5 = this.LENGTH * var13;
            float var6 = 0.1F * var13;
            VBORenderer var7 = VBORenderer.getInstance();
            var7.cmdPushAndLoadMatrix(5888, var2);
            boolean var8;
            float var9;
            float var10;
            float var11;
            float var12;
            if (!var1.m_translateGizmoRenderData.m_hideX) {
               var8 = var1.m_gizmoAxis == UI3DScene.Axis.X || this.m_trackAxis == UI3DScene.Axis.X;
               var8 |= var1.m_gizmoAxis == UI3DScene.Axis.XY || this.m_trackAxis == UI3DScene.Axis.XY;
               var8 |= var1.m_gizmoAxis == UI3DScene.Axis.XZ || this.m_trackAxis == UI3DScene.Axis.XZ;
               var9 = var8 ? 1.0F : 0.5F;
               var10 = 0.0F;
               var11 = 0.0F;
               var12 = 1.0F;
               var2.rotation(1.5707964F, 0.0F, 1.0F, 0.0F);
               var2.translate(0.0F, 0.0F, var6);
               var7.cmdPushAndMultMatrix(5888, var2);
               var7.addCylinder_Fill(var4 / 2.0F, var4 / 2.0F, var5, 8, 1, var9, var10, var11, var12);
               var7.cmdPopMatrix(5888);
               var2.translate(0.0F, 0.0F, var5);
               var7.cmdPushAndMultMatrix(5888, var2);
               var7.addCylinder_Fill(var4 / 2.0F * 2.0F, 0.0F, 0.1F * var13, 8, 1, var9, var10, var11, var12);
               var7.cmdPopMatrix(5888);
            }

            if (!var1.m_translateGizmoRenderData.m_hideY) {
               var8 = var1.m_gizmoAxis == UI3DScene.Axis.Y || this.m_trackAxis == UI3DScene.Axis.Y;
               var8 |= var1.m_gizmoAxis == UI3DScene.Axis.XY || this.m_trackAxis == UI3DScene.Axis.XY;
               var8 |= var1.m_gizmoAxis == UI3DScene.Axis.YZ || this.m_trackAxis == UI3DScene.Axis.YZ;
               var9 = 0.0F;
               var10 = var8 ? 1.0F : 0.5F;
               var11 = 0.0F;
               var12 = 1.0F;
               var2.rotation(-1.5707964F, 1.0F, 0.0F, 0.0F);
               var2.translate(0.0F, 0.0F, var6);
               var7.cmdPushAndMultMatrix(5888, var2);
               var7.addCylinder_Fill(var4 / 2.0F, var4 / 2.0F, var5, 8, 1, var9, var10, var11, var12);
               var7.cmdPopMatrix(5888);
               var2.translate(0.0F, 0.0F, var5);
               var7.cmdPushAndMultMatrix(5888, var2);
               var7.addCylinder_Fill(var4 / 2.0F * 2.0F, 0.0F, 0.1F * var13, 8, 1, var9, var10, var11, var12);
               var7.cmdPopMatrix(5888);
            }

            if (!var1.m_translateGizmoRenderData.m_hideZ) {
               var8 = var1.m_gizmoAxis == UI3DScene.Axis.Z || this.m_trackAxis == UI3DScene.Axis.Z;
               var8 |= var1.m_gizmoAxis == UI3DScene.Axis.XZ || this.m_trackAxis == UI3DScene.Axis.XZ;
               var8 |= var1.m_gizmoAxis == UI3DScene.Axis.YZ || this.m_trackAxis == UI3DScene.Axis.YZ;
               var9 = 0.0F;
               var10 = 0.0F;
               var11 = var8 ? 1.0F : 0.5F;
               var12 = 1.0F;
               var2.translation(0.0F, 0.0F, var6);
               var7.cmdPushAndMultMatrix(5888, var2);
               var7.addCylinder_Fill(var4 / 2.0F, var4 / 2.0F, var5, 8, 1, var9, var10, var11, var12);
               var7.cmdPopMatrix(5888);
               var2.translate(0.0F, 0.0F, var5);
               var7.cmdPushAndMultMatrix(5888, var2);
               var7.addCylinder_Fill(var4 / 2.0F * 2.0F, 0.0F, 0.1F * var13, 8, 1, var9, var10, var11, var12);
               var7.cmdPopMatrix(5888);
            }

            if (this.m_bDoubleAxis) {
               float var15 = var4 * 1.5F;
               boolean var14;
               if (!var1.m_translateGizmoRenderData.m_hideX && !var1.m_translateGizmoRenderData.m_hideY) {
                  var14 = var1.m_gizmoAxis == UI3DScene.Axis.XY || this.m_trackAxis == UI3DScene.Axis.XY;
                  GL11.glColor4f(1.0F, 1.0F, 0.0F, var14 ? 1.0F : 0.5F);
                  GL11.glTranslatef(var15, var15, 0.0F);
                  this.disk.draw(var6, var6 + var5 / 2.0F, 5, 1, 0.0F, 90.0F);
                  GL11.glTranslatef(-var15, -var15, 0.0F);
               }

               if (!var1.m_translateGizmoRenderData.m_hideX && !var1.m_translateGizmoRenderData.m_hideZ) {
                  var14 = var1.m_gizmoAxis == UI3DScene.Axis.XZ || this.m_trackAxis == UI3DScene.Axis.XZ;
                  GL11.glColor4f(1.0F, 0.0F, 1.0F, var14 ? 1.0F : 0.5F);
                  GL11.glTranslatef(var15, 0.0F, var15);
                  GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
                  this.disk.draw(var6, var6 + var5 / 2.0F, 5, 1, 90.0F, 90.0F);
                  GL11.glRotated(90.0, 1.0, 0.0, 0.0);
                  GL11.glTranslatef(-var15, 0.0F, -var15);
               }

               if (!var1.m_translateGizmoRenderData.m_hideY && !var1.m_translateGizmoRenderData.m_hideZ) {
                  var14 = var1.m_gizmoAxis == UI3DScene.Axis.YZ || this.m_trackAxis == UI3DScene.Axis.YZ;
                  GL11.glColor4f(0.0F, 1.0F, 1.0F, var14 ? 1.0F : 0.5F);
                  GL11.glTranslatef(0.0F, var15, var15);
                  GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
                  this.disk.draw(var6, var6 + var5 / 2.0F, 5, 1, 0.0F, 90.0F);
                  GL11.glRotated(90.0, 0.0, 1.0, 0.0);
                  GL11.glTranslatef(0.0F, -var15, -var15);
               }
            }

            UI3DScene.releaseMatrix4f(var2);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            var7.cmdPopMatrix(5888);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
         }
      }
   }

   private abstract static class SceneObject {
      final UI3DScene m_scene;
      final String m_id;
      boolean m_visible = true;
      final Vector3f m_translate = new Vector3f();
      final Vector3f m_rotate = new Vector3f();
      final Vector3f m_scale = new Vector3f(1.0F);
      SceneObject m_parent;
      String m_attachment;
      String m_parentAttachment;
      boolean m_autoRotate = false;
      float m_autoRotateAngle = 0.0F;
      ParentVehiclePart m_parentVehiclePart = null;

      SceneObject(UI3DScene var1, String var2) {
         this.m_scene = var1;
         this.m_id = var2;
      }

      void initClone(SceneObject var1) {
         var1.m_visible = this.m_visible;
         var1.m_translate.set(this.m_translate);
         var1.m_rotate.set(this.m_rotate);
         var1.m_scale.set(this.m_scale);
         var1.m_parent = this.m_parent;
         var1.m_attachment = this.m_attachment;
         var1.m_parentAttachment = this.m_parentAttachment;
         var1.m_parentVehiclePart = this.m_parentVehiclePart;
         var1.m_autoRotate = this.m_autoRotate;
         var1.m_autoRotateAngle = this.m_autoRotateAngle;
      }

      SceneObject clone(String var1) {
         throw new RuntimeException("not implemented");
      }

      abstract SceneObjectRenderData renderMain();

      Matrix4f getLocalTransform(Matrix4f var1) {
         var1.identity();
         SceneModel var2 = (SceneModel)Type.tryCastTo(this, SceneModel.class);
         if (var2 != null && var2.m_spriteModelEditor) {
            var1.translate(this.m_translate.x, this.m_translate.y, this.m_translate.z);
         } else if (var2 != null && var2.m_useWorldAttachment) {
            var1.translate(-this.m_translate.x, this.m_translate.y, this.m_translate.z);
         } else {
            var1.translate(this.m_translate);
         }

         float var3 = this.m_rotate.y;
         var1.rotateXYZ(this.m_rotate.x * 0.017453292F, var3 * 0.017453292F, this.m_rotate.z * 0.017453292F);
         var1.scale(this.m_scale.x, this.m_scale.y, this.m_scale.z);
         if (var2 != null && var2.m_spriteModelEditor) {
            var1.scale(-1.5F, 1.5F, 1.5F);
         } else if (var2 != null && var2.m_useWorldAttachment) {
            var1.scale(-1.5F, 1.5F, 1.5F);
         }

         if (this.m_attachment != null) {
            Matrix4f var4 = this.getAttachmentTransform(this.m_attachment, UI3DScene.allocMatrix4f());
            if (ModelInstanceRenderData.INVERT_ATTACHMENT_SELF_TRANSFORM) {
               var4.invert();
            }

            var1.mul(var4);
            UI3DScene.releaseMatrix4f(var4);
         }

         if (this.m_autoRotate) {
            var1.rotateY(this.m_autoRotateAngle * 0.017453292F);
         }

         return var1;
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         this.getLocalTransform(var1);
         Matrix4f var2;
         if (this.m_parent != null) {
            if (this.m_parentAttachment != null) {
               var2 = this.m_parent.getAttachmentTransform(this.m_parentAttachment, UI3DScene.allocMatrix4f());
               var2.mul(var1, var1);
               UI3DScene.releaseMatrix4f(var2);
            }

            var2 = this.m_parent.getGlobalTransform(UI3DScene.allocMatrix4f());
            var2.mul(var1, var1);
            UI3DScene.releaseMatrix4f(var2);
         }

         if (this.m_parentVehiclePart != null) {
            var2 = this.m_parentVehiclePart.getGlobalTransform(UI3DScene.allocMatrix4f());
            Matrix4f var3 = this.getAttachmentTransform(this.m_parentVehiclePart.m_attachmentName, UI3DScene.allocMatrix4f());
            var1.mul(var3);
            UI3DScene.releaseMatrix4f(var3);
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

   private static final class OriginGeometry extends SceneObject {
      SceneGeometry m_sceneGeometry;
      String m_originHint;

      OriginGeometry(UI3DScene var1) {
         super(var1, "OriginGeometry");
      }

      SceneObjectRenderData renderMain() {
         return null;
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         return this.m_sceneGeometry.getOriginTransform(this.m_originHint, var1);
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

   private static final class OriginVehiclePart extends SceneObject {
      SceneVehicle m_vehicle;
      String m_partId;
      String m_partModelId;
      String m_attachmentName;
      boolean m_bBoneOnly = true;

      OriginVehiclePart(UI3DScene var1) {
         super(var1, "OriginVehiclePart");
      }

      SceneObjectRenderData renderMain() {
         return null;
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         this.m_vehicle.getTransformForPart(this.m_partId, this.m_partModelId, this.m_attachmentName, this.m_bBoneOnly, var1);
         VehicleRenderData var2 = (VehicleRenderData)UI3DScene.VehicleRenderData.s_pool.alloc();
         var2.initVehicle(this.m_vehicle);
         VehicleModelRenderData var3 = (VehicleModelRenderData)var2.m_partToRenderData.get(this.m_partId);
         if (var3 != null) {
            var3.xfrm.mul(var1, var1);
         }

         var2.release();
         return var1;
      }

      Matrix4f getGlobalBoneTransform(Matrix4f var1) {
         var1.identity();
         SceneVehicleModelInfo var2 = this.m_vehicle.getModelInfoForPart(this.m_partId);
         if (var2 == null) {
            return var1;
         } else {
            AnimationPlayer var3 = var2.getAnimationPlayer();
            if (var3 == null) {
               return var1;
            } else {
               SkinningBone var4 = var3.getSkinningData().getBone(this.m_attachmentName);
               if (var4 == null) {
                  return var1;
               } else {
                  var1 = PZMath.convertMatrix(var3.getModelTransformAt(var4.Index), var1);
                  var1.transpose();
                  VehicleRenderData var5 = (VehicleRenderData)UI3DScene.VehicleRenderData.s_pool.alloc();
                  var5.initVehicle(this.m_vehicle);
                  VehicleModelRenderData var6 = (VehicleModelRenderData)var5.m_partToRenderData.get(this.m_partId);
                  if (var6 != null) {
                     var6.xfrm.mul(var1, var1);
                  }

                  var5.release();
                  return var1;
               }
            }
         }
      }
   }

   public static final class PolygonEditor {
      final UI3DScene m_scene;
      final Plane m_plane = new Plane();
      final Vector3f m_rotate = new Vector3f();
      GridPlane m_gridPlane;

      PolygonEditor(UI3DScene var1) {
         this.m_gridPlane = UI3DScene.GridPlane.XY;
         this.m_scene = var1;
      }

      void setPlane(Vector3f var1, Vector3f var2, GridPlane var3) {
         this.m_plane.point.set(var1);
         this.m_plane.normal.set(0.0F, 0.0F, 1.0F);
         Matrix4f var4 = UI3DScene.allocMatrix4f().rotationXYZ(var2.x * 0.017453292F, var2.y * 0.017453292F, var2.z * 0.017453292F);
         var4.transformDirection(this.m_plane.normal);
         UI3DScene.releaseMatrix4f(var4);
         this.m_rotate.set(var2);
         this.m_gridPlane = var3;
      }

      boolean uiToPlane3D(float var1, float var2, Vector3f var3) {
         boolean var4 = false;
         Ray var5 = this.m_scene.getCameraRay(var1, (float)this.m_scene.screenHeight() - var2, UI3DScene.allocRay());
         UI3DScene var10000 = this.m_scene;
         if (UI3DScene.intersect_ray_plane(this.m_plane, var5, var3) == 1) {
            var4 = true;
         }

         UI3DScene.releaseRay(var5);
         return var4;
      }

      boolean uiToPlane2D(float var1, float var2, Vector2f var3) {
         Vector3f var4 = UI3DScene.allocVector3f();
         boolean var5 = this.uiToPlane3D(var1, var2, var4);
         if (var5) {
            Matrix4f var6 = UI3DScene.allocMatrix4f();
            var6.translation(this.m_plane.point);
            var6.rotateXYZ(this.m_rotate.x * 0.017453292F, this.m_rotate.y * 0.017453292F, this.m_rotate.z * 0.017453292F);
            var6.invert();
            var6.transformPosition(var4);
            var3.set(var4.x, var4.y);
            UI3DScene.releaseMatrix4f(var6);
         }

         UI3DScene.releaseVector3f(var4);
         return var5;
      }

      Vector3f planeTo3D(Vector2f var1, Vector3f var2) {
         Matrix4f var3 = UI3DScene.allocMatrix4f();
         var3.translation(this.m_plane.point);
         var3.rotateXYZ(this.m_rotate.x * 0.017453292F, this.m_rotate.y * 0.017453292F, this.m_rotate.z * 0.017453292F);
         var3.transformPosition(var1.x, var1.y, 0.0F, var2);
         UI3DScene.releaseMatrix4f(var3);
         return var2;
      }

      Vector2f planeToUI(Vector2f var1, Vector2f var2) {
         Vector3f var3 = this.planeTo3D(var1, UI3DScene.allocVector3f());
         var2.set(this.m_scene.sceneToUIX(var3), this.m_scene.sceneToUIY(var3));
         UI3DScene.releaseVector3f(var3);
         return var2;
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
         UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.FORMAT_PositionColor);
         UI3DScene.vboRenderer.setMode(4);
         UI3DScene.vboRenderer.setDepthTest(true);
         if (this.m_scene.m_gridPlane == UI3DScene.GridPlane.XZ) {
            UI3DScene.vboRenderer.addTriangle(-var2, 0.0F, -var2, var2, 0.0F, -var2, -var2, 0.0F, var2, 0.5F, 0.5F, 0.5F, 1.0F);
            UI3DScene.vboRenderer.addTriangle(var2, 0.0F, var2, -var2, 0.0F, var2, var2, 0.0F, -var2, 0.5F, 0.5F, 0.5F, 1.0F);
         }

         UI3DScene.vboRenderer.endRun();
         UI3DScene.vboRenderer.flush();
         GL11.glPopAttrib();
         ShaderHelper.glUseProgramObjectARB(0);
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
            Box3D var10 = (Box3D)UI3DScene.this.m_box3D.get(var2);
            var1.m_box3D.add(((Box3D)UI3DScene.s_box3DPool.alloc()).set(var10));
         }

         UI3DScene.s_physicsMeshPool.releaseAll(var1.m_physicsMesh);
         var1.m_physicsMesh.clear();

         for(var2 = 0; var2 < UI3DScene.this.m_physicsMesh.size(); ++var2) {
            PhysicsMesh var11 = (PhysicsMesh)UI3DScene.this.m_physicsMesh.get(var2);
            if (var11.physicsShapeScript != null) {
               PhysicsShapeScript var4 = ScriptManager.instance.getPhysicsShape(var11.physicsShapeScript);
               if (var4 != null) {
                  PhysicsShape var5 = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(var4.meshName);
                  if (var5 != null && var5.isReady()) {
                     for(int var6 = 0; var6 < var5.meshes.size(); ++var6) {
                        PhysicsShape.OneMesh var7 = (PhysicsShape.OneMesh)var5.meshes.get(var6);
                        PhysicsMesh var8 = ((PhysicsMesh)UI3DScene.s_physicsMeshPool.alloc()).set(var11);
                        Matrix4f var9 = Bullet.translationRotateScale(var4.translate, var4.rotate, var4.scale * var11.scale, UI3DScene.allocMatrix4f());
                        var7.m_transform.transpose();
                        var9.mul(var7.m_transform);
                        var7.m_transform.transpose();
                        var8.points = Bullet.transformPhysicsMeshPoints(var9, var7.m_points, false);
                        UI3DScene.releaseMatrix4f(var9);
                        var1.m_physicsMesh.add(var8);
                     }
                  }
               }
            }
         }

         UI3DScene.s_posRotPool.release((List)var1.m_axes);
         var1.m_axes.clear();

         for(var2 = 0; var2 < UI3DScene.this.m_axes.size(); ++var2) {
            PositionRotation var12 = (PositionRotation)UI3DScene.this.m_axes.get(var2);
            var1.m_axes.add(((PositionRotation)UI3DScene.s_posRotPool.alloc()).set(var12));
         }

      }

      public void render() {
         StateData var1 = UI3DScene.this.stateDataRender();
         DefaultShader var10000 = SceneShaderStore.DefaultShader;
         DefaultShader.isActive = false;
         ShaderHelper.forgetCurrentlyBound();
         GL20.glUseProgram(0);
         PZGLUtil.pushAndLoadMatrix(5889, var1.m_projection);
         PZGLUtil.pushAndLoadMatrix(5888, var1.m_modelView);
         GL11.glPushAttrib(2048);
         GL11.glViewport(UI3DScene.this.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(), UI3DScene.this.getWidth().intValue(), UI3DScene.this.getHeight().intValue());
         UI3DScene.vboRenderer.setOffset(0.0F, 0.0F, 0.0F);
         if (UI3DScene.this.m_bDrawGrid) {
            UI3DScene.this.renderGrid();
         }

         int var2;
         for(var2 = 0; var2 < var1.m_aabb.size(); ++var2) {
            AABB var3 = (AABB)var1.m_aabb.get(var2);
            UI3DScene.this.renderAABB(var3.x, var3.y, var3.z, -var3.w / 2.0F, -var3.h / 2.0F, -var3.L / 2.0F, var3.w / 2.0F, var3.h / 2.0F, var3.L / 2.0F, var3.r, var3.g, var3.b, 1.0F, false);
         }

         for(var2 = 0; var2 < var1.m_box3D.size(); ++var2) {
            Box3D var4 = (Box3D)var1.m_box3D.get(var2);
            UI3DScene.this.renderBox3D(var4.x, var4.y, var4.z, var4.xMin, var4.yMin, var4.zMin, var4.xMax, var4.yMax, var4.zMax, var4.rx, var4.ry, var4.rz, var4.r, var4.g, var4.b, var4.a, var4.bQuads);
         }

         for(var2 = 0; var2 < var1.m_physicsMesh.size(); ++var2) {
            PhysicsMesh var5 = (PhysicsMesh)var1.m_physicsMesh.get(var2);
            UI3DScene.this.renderPhysicsMesh(var5.x, var5.y, var5.z, var5.rx, var5.ry, var5.rz, var5.r, var5.g, var5.b, var5.points);
         }

         for(var2 = 0; var2 < var1.m_axes.size(); ++var2) {
            UI3DScene.this.renderAxis((PositionRotation)var1.m_axes.get(var2));
         }

         UI3DScene.vboRenderer.flush();
         if (var1.m_gizmo != null) {
            GL11.glDisable(3553);
            var1.m_gizmo.render();
            GL11.glEnable(3553);
         }

         UI3DScene.vboRenderer.flush();
         GL11.glPopAttrib();
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
         ShaderHelper.glUseProgramObjectARB(0);
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
         if (var3 != UI3DScene.Axis.XY && var3 != UI3DScene.Axis.XZ && var3 != UI3DScene.Axis.YZ) {
            var2 = (float)UI3DScene.this.screenHeight() - var2;
            Ray var10 = UI3DScene.this.getCameraRay(var1, var2, UI3DScene.allocRay());
            Ray var11 = UI3DScene.allocRay();
            var4.transformPosition(var11.origin.set(0.0F, 0.0F, 0.0F));
            switch (var3) {
               case X:
                  var11.direction.set(1.0F, 0.0F, 0.0F);
                  break;
               case Y:
                  var11.direction.set(0.0F, 1.0F, 0.0F);
                  break;
               case Z:
                  var11.direction.set(0.0F, 0.0F, 1.0F);
            }

            var4.transformDirection(var11.direction).normalize();
            UI3DScene.this.closest_distance_between_lines(var11, var10);
            UI3DScene.releaseRay(var10);
            var5.set(var11.direction).mul(var11.t).add(var11.origin);
            UI3DScene.releaseRay(var11);
            return var5;
         } else {
            Vector3f var7 = var4.transformPosition(UI3DScene.allocVector3f().set(0.0F, 0.0F, 0.0F));
            Vector3f var8 = UI3DScene.allocVector3f();
            GridPlane var9 = UI3DScene.GridPlane.XY;
            switch (var3) {
               case XY:
                  var8.set(0.0F, 0.0F, 0.0F);
                  var9 = UI3DScene.GridPlane.XY;
                  break;
               case XZ:
                  var8.set(90.0F, 0.0F, 0.0F);
                  var9 = UI3DScene.GridPlane.XZ;
                  break;
               case YZ:
                  var8.set(0.0F, 90.0F, 0.0F);
                  var9 = UI3DScene.GridPlane.YZ;
            }

            UI3DScene.this.m_polygonEditor.setPlane(var7, var8, var9);
            UI3DScene.this.m_polygonEditor.uiToPlane3D(var1, var2, var5.set(0.0F));
            UI3DScene.releaseVector3f(var7);
            UI3DScene.releaseVector3f(var8);
            return var5;
         }
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

      boolean getPointOnDualAxis(float var1, float var2, Axis var3, Matrix4f var4, Vector3f var5, Vector2f var6) {
         Plane var7 = UI3DScene.allocPlane();
         var4.transformPosition(var7.point.set(0.0F, 0.0F, 0.0F));
         switch (var3) {
            case XY:
               var7.normal.set(0.0F, 0.0F, 1.0F);
               break;
            case XZ:
               var7.normal.set(0.0F, 1.0F, 0.0F);
               break;
            case YZ:
               var7.normal.set(1.0F, 0.0F, 0.0F);
         }

         var4.transformDirection(var7.normal);
         Ray var8 = UI3DScene.this.getCameraRay(var1, (float)UI3DScene.this.screenHeight() - var2, UI3DScene.allocRay());
         boolean var9 = UI3DScene.intersect_ray_plane(var7, var8, var5) == 1;
         UI3DScene.releaseRay(var8);
         UI3DScene.releasePlane(var7);
         if (var9) {
            Matrix4f var10 = UI3DScene.allocMatrix4f().set(var4);
            var10.invert();
            Vector3f var11 = var10.transformPosition(var5, UI3DScene.allocVector3f());
            UI3DScene.releaseMatrix4f(var10);
            switch (var3) {
               case XY:
                  var6.set(var11.x, var11.y);
                  break;
               case XZ:
                  var6.set(var11.x, var11.z);
                  break;
               case YZ:
                  var6.set(var11.y, var11.z);
            }

            UI3DScene.releaseVector3f(var11);
            return true;
         } else {
            return false;
         }
      }

      void renderLineToOrigin() {
         StateData var1 = UI3DScene.this.stateDataRender();
         if (var1.m_hasGizmoOrigin) {
            UI3DScene.this.renderAxis(var1.m_gizmoTranslate, var1.m_gizmoRotate, false);
            Vector3f var2 = var1.m_gizmoTranslate;
            UI3DScene.vboRenderer.flush();
            Matrix4f var3 = UI3DScene.allocMatrix4f();
            var3.set(var1.m_modelView);
            var3.mul(var1.m_gizmoParentTransform);
            var3.mul(var1.m_gizmoOriginTransform);
            var3.mul(var1.m_gizmoChildTransform);
            if (var1.m_selectedAttachmentIsChildAttachment) {
               var3.mul(var1.m_gizmoChildAttachmentTransformInv);
            }

            UI3DScene.vboRenderer.cmdPushAndLoadMatrix(5888, var3);
            UI3DScene.releaseMatrix4f(var3);
            UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.FORMAT_PositionColor);
            UI3DScene.vboRenderer.setMode(1);
            UI3DScene.vboRenderer.setLineWidth(2.0F);
            UI3DScene.vboRenderer.addLine(var2.x, var2.y, var2.z, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            UI3DScene.vboRenderer.endRun();
            UI3DScene.vboRenderer.cmdPopMatrix(5888);
            UI3DScene.vboRenderer.flush();
         }
      }
   }

   static enum Axis {
      None,
      X,
      Y,
      Z,
      XY,
      XZ,
      YZ;

      private Axis() {
      }
   }

   private abstract static class SceneCharacter extends SceneObject {
      final AnimatedModel m_animatedModel = new AnimatedModel();
      boolean m_bShowBones = false;
      boolean m_bShowBip01 = false;
      boolean m_bClearDepthBuffer = true;
      boolean m_bUseDeferredMovement = false;

      SceneCharacter(UI3DScene var1, String var2) {
         super(var1, var2);
      }

      abstract void initAnimatedModel();

      SceneObjectRenderData renderMain() {
         this.m_animatedModel.update();
         CharacterRenderData var1 = (CharacterRenderData)UI3DScene.CharacterRenderData.s_pool.alloc();
         var1.initCharacter(this);
         SpriteRenderer.instance.drawGeneric(var1.m_drawer);
         return var1;
      }

      Matrix4f getLocalTransform(Matrix4f var1) {
         var1.identity();
         var1.translate(this.m_translate.x, this.m_translate.y, this.m_translate.z);
         float var2 = this.m_rotate.y;
         var1.rotateXYZ(-this.m_rotate.x * 0.017453292F, -var2 * 0.017453292F, this.m_rotate.z * 0.017453292F);
         var1.scale(1.5F * this.m_scale.x, 1.5F * this.m_scale.y, 1.5F * this.m_scale.z);
         Matrix4f var3 = UI3DScene.allocMatrix4f();
         var3.identity();
         var3.rotateY(3.1415927F);
         var3.scale(-1.0F, 1.0F, 1.0F);
         var1.mul(var3);
         UI3DScene.releaseMatrix4f(var3);
         if (this.m_autoRotate) {
            var1.rotateY(this.m_autoRotateAngle * 0.017453292F);
         }

         if (this.m_animatedModel.getAnimationPlayer().getMultiTrack().getTracks().isEmpty()) {
            return var1;
         } else {
            if (this.m_bUseDeferredMovement) {
               AnimationMultiTrack var5 = this.m_animatedModel.getAnimationPlayer().getMultiTrack();
               float var6 = ((AnimationTrack)var5.getTracks().get(0)).getCurrentDeferredRotation();
               org.lwjgl.util.vector.Vector3f var4 = new org.lwjgl.util.vector.Vector3f();
               ((AnimationTrack)var5.getTracks().get(0)).getCurrentDeferredPosition(var4);
               var1.translate(var4.x, var4.y, var4.z);
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
               ModelInstanceRenderData.makeAttachmentTransform(var5, var2);
               if (var5.getBone() != null) {
                  Matrix4f var6 = this.getBoneMatrix(var5.getBone(), UI3DScene.allocMatrix4f());
                  var6.mul(var2, var2);
                  UI3DScene.releaseMatrix4f(var6);
               }

               return var2;
            }
         }
      }

      int hitTestBone(int var1, Ray var2, Ray var3, Matrix4f var4, Vector2f var5) {
         AnimationPlayer var6 = this.m_animatedModel.getAnimationPlayer();
         SkinningData var7 = var6.getSkinningData();
         int var8 = (Integer)var7.SkeletonHierarchy.get(var1);
         if (var8 == -1) {
            return -1;
         } else {
            org.lwjgl.util.vector.Matrix4f var9 = var6.getModelTransformAt(var8);
            var2.origin.set(var9.m03, var9.m13, var9.m23);
            var4.transformPosition(var2.origin);
            var9 = var6.getModelTransformAt(var1);
            Vector3f var10 = UI3DScene.allocVector3f();
            var10.set(var9.m03, var9.m13, var9.m23);
            var4.transformPosition(var10);
            var2.direction.set(var10).sub(var2.origin);
            float var11 = var2.direction.length();
            var2.direction.normalize();
            this.m_scene.closest_distance_between_lines(var3, var2);
            float var12 = this.m_scene.sceneToUIX(var3.origin.x + var3.direction.x * var3.t, var3.origin.y + var3.direction.y * var3.t, var3.origin.z + var3.direction.z * var3.t);
            float var13 = this.m_scene.sceneToUIY(var3.origin.x + var3.direction.x * var3.t, var3.origin.y + var3.direction.y * var3.t, var3.origin.z + var3.direction.z * var3.t);
            float var14 = this.m_scene.sceneToUIX(var2.origin.x + var2.direction.x * var2.t, var2.origin.y + var2.direction.y * var2.t, var2.origin.z + var2.direction.z * var2.t);
            float var15 = this.m_scene.sceneToUIY(var2.origin.x + var2.direction.x * var2.t, var2.origin.y + var2.direction.y * var2.t, var2.origin.z + var2.direction.z * var2.t);
            int var16 = -1;
            float var17 = 10.0F;
            float var18 = (float)Math.sqrt(Math.pow((double)(var14 - var12), 2.0) + Math.pow((double)(var15 - var13), 2.0));
            if (var18 < var17) {
               if ((!(var2.t >= 0.0F) || !(var2.t <= 0.01F)) && (!(var2.t >= 0.09F) || !(var2.t <= 1.0F))) {
                  var5.set(var18, 0.0F);
               } else {
                  var5.set(var18 / 10.0F, 0.0F);
               }

               if (var2.t >= 0.0F && var2.t < var11 * 0.5F) {
                  var16 = var8;
               } else if (var2.t >= var11 * 0.5F && var2.t < var11) {
                  var16 = var1;
               }
            }

            UI3DScene.releaseVector3f(var10);
            return var16;
         }
      }

      String pickBone(float var1, float var2) {
         if (this.m_animatedModel.getAnimationPlayer().getModelTransformsCount() == 0) {
            return "";
         } else {
            var2 = (float)this.m_scene.screenHeight() - var2;
            Ray var3 = this.m_scene.getCameraRay(var1, var2, UI3DScene.allocRay());
            Matrix4f var4 = UI3DScene.allocMatrix4f();
            this.getLocalTransform(var4);
            Ray var5 = UI3DScene.allocRay();
            int var6 = -1;
            Vector2f var7 = UI3DScene.allocVector2f();
            float var8 = 3.4028235E38F;

            for(int var9 = 0; var9 < this.m_animatedModel.getAnimationPlayer().getModelTransformsCount(); ++var9) {
               int var10 = this.hitTestBone(var9, var5, var3, var4, var7);
               if (var10 != -1 && var7.x < var8) {
                  var8 = var7.x;
                  var6 = var10;
               }
            }

            UI3DScene.releaseVector2f(var7);
            UI3DScene.releaseRay(var5);
            UI3DScene.releaseRay(var3);
            UI3DScene.releaseMatrix4f(var4);
            return var6 == -1 ? "" : this.m_animatedModel.getAnimationPlayer().getSkinningData().getBoneAt(var6).Name;
         }
      }

      Matrix4f getBoneMatrix(String var1, Matrix4f var2) {
         var2.identity();
         if (this.m_animatedModel.getAnimationPlayer().getModelTransformsCount() == 0) {
            return var2;
         } else {
            SkinningBone var3 = this.m_animatedModel.getAnimationPlayer().getSkinningData().getBone(var1);
            if (var3 == null) {
               return var2;
            } else {
               var2 = PZMath.convertMatrix(this.m_animatedModel.getAnimationPlayer().getModelTransformAt(var3.Index), var2);
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

   private static final class SceneVehicle extends SceneObject {
      String m_scriptName = "Base.ModernCar";
      VehicleScript m_script;
      final ArrayList<SceneVehicleModelInfo> m_modelInfo = new ArrayList();
      String m_showBones_partId = null;
      String m_showBones_modelId = null;
      boolean m_init = false;

      SceneVehicle(UI3DScene var1, String var2) {
         super(var1, var2);
         this.setScriptName("Base.ModernCar");
      }

      SceneObjectRenderData renderMain() {
         if (this.m_script == null) {
            return null;
         } else {
            if (!this.m_init) {
               this.m_init = true;
               String var1 = this.m_script.getModel().file;
               Model var2 = ModelManager.instance.getLoadedModel(var1);
               if (var2 == null) {
                  return null;
               }

               SceneVehicleModelInfo var3 = (SceneVehicleModelInfo)UI3DScene.SceneVehicleModelInfo.s_pool.alloc();
               var3.sceneVehicle = this;
               var3.part = null;
               var3.scriptModel = this.m_script.getModel();
               var3.modelScript = ScriptManager.instance.getModelScript(var3.scriptModel.file);
               var3.wheelIndex = -1;
               var3.model = var2;
               var3.tex = var2.tex;
               if (this.m_script.getSkinCount() > 0) {
                  var3.tex = Texture.getSharedTexture("media/textures/" + this.m_script.getSkin(0).texture + ".png");
               }

               var3.releaseAnimationPlayer();
               var3.m_animPlayer = null;
               var3.m_track = null;
               this.m_modelInfo.add(var3);

               for(int var4 = 0; var4 < this.m_script.getPartCount(); ++var4) {
                  VehicleScript.Part var5 = this.m_script.getPart(var4);
                  if (var5.wheel == null) {
                     for(int var6 = 0; var6 < var5.getModelCount(); ++var6) {
                        VehicleScript.Model var7 = var5.getModel(var6);
                        var1 = var7.file;
                        if (var1 != null) {
                           var2 = ModelManager.instance.getLoadedModel(var1);
                           if (var2 != null) {
                              var3 = (SceneVehicleModelInfo)UI3DScene.SceneVehicleModelInfo.s_pool.alloc();
                              var3.sceneVehicle = this;
                              var3.part = var5;
                              var3.scriptModel = var7;
                              var3.modelScript = ScriptManager.instance.getModelScript(var7.file);
                              var3.wheelIndex = -1;
                              var3.model = var2;
                              var3.tex = var2.tex;
                              var3.releaseAnimationPlayer();
                              var3.m_animPlayer = null;
                              var3.m_track = null;
                              this.m_modelInfo.add(var3);
                           }
                        }
                     }
                  }
               }
            }

            if (this.m_modelInfo.isEmpty()) {
               return null;
            } else {
               VehicleRenderData var8 = (VehicleRenderData)UI3DScene.VehicleRenderData.s_pool.alloc();
               var8.initVehicle(this);
               SetModelCamera var9 = (SetModelCamera)UI3DScene.s_SetModelCameraPool.alloc();
               SpriteRenderer.instance.drawGeneric(var9.init(this.m_scene.m_VehicleSceneModelCamera, var8));
               SpriteRenderer.instance.drawGeneric(var8.m_drawer);
               return var8;
            }
         }
      }

      Matrix4f getAttachmentTransform(String var1, Matrix4f var2) {
         var2.identity();
         ModelAttachment var3 = this.m_script.getAttachmentById(var1);
         if (var3 == null) {
            return var2;
         } else {
            ModelInstanceRenderData.makeAttachmentTransform(var3, var2);
            if (var3.getBone() != null) {
               Matrix4f var4 = this.getBoneMatrix(var3.getBone(), UI3DScene.allocMatrix4f());
               var4.mul(var2, var2);
               UI3DScene.releaseMatrix4f(var4);
            }

            return var2;
         }
      }

      Matrix4f getBoneMatrix(String var1, Matrix4f var2) {
         var2.identity();
         if (this.m_modelInfo.isEmpty()) {
            return var2;
         } else {
            SceneVehicleModelInfo var3 = (SceneVehicleModelInfo)this.m_modelInfo.get(0);
            if (var3 == null) {
               return var2;
            } else {
               AnimationPlayer var4 = var3.getAnimationPlayer();
               if (var4 == null) {
                  return var2;
               } else if (var4.getModelTransformsCount() == 0) {
                  return var2;
               } else {
                  SkinningBone var5 = var4.getSkinningData().getBone(var1);
                  if (var5 == null) {
                     return var2;
                  } else {
                     var2 = PZMath.convertMatrix(var4.getModelTransformAt(var5.Index), var2);
                     var2.transpose();
                     return var2;
                  }
               }
            }
         }
      }

      Matrix4f getTransformForPart(String var1, String var2, String var3, boolean var4, Matrix4f var5) {
         var5.identity();
         VehicleScript.Part var6 = this.m_script.getPartById(var1);
         if (var6 == null) {
            return var5;
         } else {
            VehicleScript.Model var7 = var6.getModelById(var2);
            if (var7 == null) {
               return var5;
            } else if (var7.getFile() == null) {
               return var5;
            } else {
               ModelScript var8 = ScriptManager.instance.getModelScript(var7.getFile());
               if (var8 == null) {
                  return var5;
               } else {
                  ModelAttachment var9 = var8.getAttachmentById(var3);
                  if (var9 == null) {
                     return var5;
                  } else {
                     var5.scale(1.0F / var8.scale);
                     Matrix4f var10;
                     if (var9.getBone() != null) {
                        var10 = this.getBoneMatrix(var6, var9, UI3DScene.allocMatrix4f());
                        var10.mul(var5, var5);
                        UI3DScene.releaseMatrix4f(var10);
                     }

                     if (var4) {
                        return var5;
                     } else {
                        var10 = ModelInstanceRenderData.makeAttachmentTransform(var9, UI3DScene.allocMatrix4f());
                        var5.mul(var10);
                        UI3DScene.releaseMatrix4f(var10);
                        return var5;
                     }
                  }
               }
            }
         }
      }

      Matrix4f getBoneMatrix(VehicleScript.Part var1, ModelAttachment var2, Matrix4f var3) {
         var3.identity();
         SceneVehicleModelInfo var4 = this.getModelInfoForPart(var1.getId());
         if (var4 == null) {
            return var3;
         } else {
            AnimationPlayer var5 = var4.getAnimationPlayer();
            if (var5 == null) {
               return var3;
            } else if (var5.getModelTransformsCount() == 0) {
               return var3;
            } else {
               SkinningBone var6 = var5.getSkinningData().getBone(var2.getBone());
               if (var6 == null) {
                  return var3;
               } else {
                  var3 = PZMath.convertMatrix(var5.getModelTransformAt(var6.Index), var3);
                  var3.transpose();
                  return var3;
               }
            }
         }
      }

      int hitTestBone(int var1, Ray var2, Ray var3, AnimationPlayer var4, Matrix4f var5, Vector2f var6) {
         SkinningData var7 = var4.getSkinningData();
         int var8 = (Integer)var7.SkeletonHierarchy.get(var1);
         if (var8 == -1) {
            return -1;
         } else {
            org.lwjgl.util.vector.Matrix4f var9 = var4.getModelTransformAt(var8);
            var2.origin.set(var9.m03, var9.m13, var9.m23);
            var5.transformPosition(var2.origin);
            var9 = var4.getModelTransformAt(var1);
            Vector3f var10 = UI3DScene.allocVector3f();
            var10.set(var9.m03, var9.m13, var9.m23);
            var5.transformPosition(var10);
            var2.direction.set(var10).sub(var2.origin);
            float var11 = var2.direction.length();
            var2.direction.normalize();
            this.m_scene.closest_distance_between_lines(var3, var2);
            float var12 = this.m_scene.sceneToUIX(var3.origin.x + var3.direction.x * var3.t, var3.origin.y + var3.direction.y * var3.t, var3.origin.z + var3.direction.z * var3.t);
            float var13 = this.m_scene.sceneToUIY(var3.origin.x + var3.direction.x * var3.t, var3.origin.y + var3.direction.y * var3.t, var3.origin.z + var3.direction.z * var3.t);
            float var14 = this.m_scene.sceneToUIX(var2.origin.x + var2.direction.x * var2.t, var2.origin.y + var2.direction.y * var2.t, var2.origin.z + var2.direction.z * var2.t);
            float var15 = this.m_scene.sceneToUIY(var2.origin.x + var2.direction.x * var2.t, var2.origin.y + var2.direction.y * var2.t, var2.origin.z + var2.direction.z * var2.t);
            int var16 = -1;
            float var17 = 10.0F;
            float var18 = (float)Math.sqrt(Math.pow((double)(var14 - var12), 2.0) + Math.pow((double)(var15 - var13), 2.0));
            if (var18 < var17) {
               if ((!(var2.t >= 0.0F) || !(var2.t <= 0.01F)) && (!(var2.t >= 0.09F) || !(var2.t <= 1.0F))) {
                  var6.set(var18, 0.0F);
               } else {
                  var6.set(var18 / 10.0F, 0.0F);
               }

               if (var2.t >= 0.0F && var2.t < var11 * 0.5F) {
                  var16 = var8;
               } else if (var2.t >= var11 * 0.5F && var2.t < var11) {
                  var16 = var1;
               }
            }

            UI3DScene.releaseVector3f(var10);
            return var16;
         }
      }

      String pickBone(VehicleScript.Part var1, VehicleScript.Model var2, float var3, float var4) {
         SceneVehicleModelInfo var5 = this.getModelInfoForPart(var1.getId(), var2.getId());
         if (var5 == null) {
            return "";
         } else {
            AnimationPlayer var6 = var5.getAnimationPlayer();
            if (var6 != null && var6.getModelTransformsCount() != 0) {
               var4 = (float)this.m_scene.screenHeight() - var4;
               Ray var7 = this.m_scene.getCameraRay(var3, var4, UI3DScene.allocRay());
               Matrix4f var8 = UI3DScene.allocMatrix4f();
               this.getLocalTransform(var8);
               VehicleRenderData var9 = (VehicleRenderData)UI3DScene.VehicleRenderData.s_pool.alloc();
               var9.initVehicle(this);
               VehicleModelRenderData var10 = (VehicleModelRenderData)var9.m_partToRenderData.get(var1.getId());
               if (var10 != null) {
                  var8.set(var10.xfrm);
               }

               var9.release();
               Ray var15 = UI3DScene.allocRay();
               int var16 = -1;
               Vector2f var11 = UI3DScene.allocVector2f();
               float var12 = 3.4028235E38F;

               for(int var13 = 0; var13 < var6.getModelTransformsCount(); ++var13) {
                  int var14 = this.hitTestBone(var13, var15, var7, var6, var8, var11);
                  if (var14 != -1 && var11.x < var12) {
                     var12 = var11.x;
                     var16 = var14;
                  }
               }

               UI3DScene.releaseVector2f(var11);
               UI3DScene.releaseRay(var15);
               UI3DScene.releaseRay(var7);
               UI3DScene.releaseMatrix4f(var8);
               return var16 == -1 ? "" : var6.getSkinningData().getBoneAt(var16).Name;
            } else {
               return "";
            }
         }
      }

      void setScriptName(String var1) {
         this.m_scriptName = var1;
         this.m_script = ScriptManager.instance.getVehicle(var1);
         UI3DScene.SceneVehicleModelInfo.s_pool.releaseAll(this.m_modelInfo);
         this.m_modelInfo.clear();
         this.m_init = false;
      }

      SceneVehicleModelInfo getModelInfoForPart(String var1) {
         for(int var2 = 0; var2 < this.m_modelInfo.size(); ++var2) {
            SceneVehicleModelInfo var3 = (SceneVehicleModelInfo)this.m_modelInfo.get(var2);
            if (var3.part != null && var3.part.getId().equalsIgnoreCase(var1)) {
               return var3;
            }
         }

         return null;
      }

      SceneVehicleModelInfo getModelInfoForPart(String var1, String var2) {
         for(int var3 = 0; var3 < this.m_modelInfo.size(); ++var3) {
            SceneVehicleModelInfo var4 = (SceneVehicleModelInfo)this.m_modelInfo.get(var3);
            if (var4.part != null && var4.part.getId().equalsIgnoreCase(var1) && var4.scriptModel.getId().equalsIgnoreCase(var2)) {
               return var4;
            }
         }

         return null;
      }
   }

   private static final class ScenePolygon extends SceneGeometry {
      GridPlane m_plane;
      final Vector3f m_extents;
      final ArrayList<Vector2f> m_points;
      boolean m_editing;
      int m_highlightPointIndex;
      final TFloatArrayList m_triangles;
      static final Rasterize s_rasterize = new Rasterize();

      ScenePolygon(UI3DScene var1, String var2) {
         super(var1, var2);
         this.m_plane = UI3DScene.GridPlane.XZ;
         this.m_extents = new Vector3f(1.0F);
         this.m_points = new ArrayList();
         this.m_editing = true;
         this.m_highlightPointIndex = -1;
         this.m_triangles = new TFloatArrayList();
      }

      public String getTypeName() {
         return "polygon";
      }

      void initClone(SceneObject var1) {
         ScenePolygon var2 = (ScenePolygon)var1;
         super.initClone(var1);
         var2.m_plane = this.m_plane;
         var2.m_extents.set(this.m_extents);
         Iterator var3 = this.m_points.iterator();

         while(var3.hasNext()) {
            Vector2f var4 = (Vector2f)var3.next();
            var2.m_points.add(new Vector2f(var4));
         }

      }

      SceneObject clone(String var1) {
         ScenePolygon var2 = new ScenePolygon(this.m_scene, var1);
         this.initClone(var2);
         return var2;
      }

      SceneObjectRenderData renderMain() {
         if (!this.m_scene.m_bDrawGeometry) {
            return null;
         } else {
            ScenePolygonRenderData var1 = (ScenePolygonRenderData)UI3DScene.ScenePolygonRenderData.s_pool.alloc();
            var1.initPolygon(this);
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
         return var2;
      }

      public boolean isPolygon() {
         return true;
      }

      Matrix4f getOriginTransform(String var1, Matrix4f var2) {
         return this.getGlobalTransform(var2);
      }

      float getNormalizedDepthAt(float var1, float var2) {
         Vector3f var3 = UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F);
         Matrix4f var4 = UI3DScene.allocMatrix4f().rotationXYZ(this.m_rotate.x * 0.017453292F, this.m_rotate.y * 0.017453292F, this.m_rotate.z * 0.017453292F);
         var4.transformDirection(var3);
         UI3DScene.releaseMatrix4f(var4);
         float var5 = TileGeometryUtils.getNormalizedDepthOnPlaneAt(var1, var2, this.m_translate, var3);
         UI3DScene.releaseVector3f(var3);
         return var5;
      }

      TileGeometryFile.Geometry toGeometryFileObject() {
         TileGeometryFile.Polygon var1 = new TileGeometryFile.Polygon();
         var1.plane = TileGeometryFile.Plane.valueOf(this.m_plane.name());
         var1.translate.set(this.m_translate);
         var1.rotate.set(this.m_rotate);
         Iterator var2 = this.m_points.iterator();

         while(var2.hasNext()) {
            Vector2f var3 = (Vector2f)var2.next();
            var1.m_points.add(var3.x);
            var1.m_points.add(var3.y);
         }

         return var1;
      }

      int addPointOnEdge(float var1, float var2, float var3, float var4) {
         if (this.pickPoint(var1, var2, 5.0F) != -1) {
            return -1;
         } else {
            int var5 = this.pickEdge(var1, var2, 10.0F);
            if (var5 == -1) {
               return -1;
            } else {
               Vector2f var6 = new Vector2f();
               if (this.m_scene.m_polygonEditor.uiToPlane2D(var3, var4, var6)) {
                  this.m_points.add(var5 + 1, var6);
                  this.triangulate();
                  return var5;
               } else {
                  return -1;
               }
            }
         }
      }

      int pickEdge(float var1, float var2, float var3) {
         float var4 = 3.4028235E38F;
         int var5 = -1;
         this.m_scene.m_polygonEditor.setPlane(this.m_translate, this.m_rotate, this.m_plane);
         Vector2f var6 = UI3DScene.allocVector2f().set(var1, var2);
         Vector2f var7 = UI3DScene.allocVector2f();
         Vector2f var8 = UI3DScene.allocVector2f();

         for(int var9 = 0; var9 < this.m_points.size(); ++var9) {
            this.m_scene.m_polygonEditor.planeToUI((Vector2f)this.m_points.get(var9), var7);
            this.m_scene.m_polygonEditor.planeToUI((Vector2f)this.m_points.get((var9 + 1) % this.m_points.size()), var8);
            float var10 = this.distanceOfPointToLineSegment(var7, var8, var6);
            if (var10 < var4 && var10 < var3) {
               var4 = var10;
               var5 = var9;
            }
         }

         UI3DScene.releaseVector2f(var6);
         UI3DScene.releaseVector2f(var7);
         UI3DScene.releaseVector2f(var8);
         return var5;
      }

      float distanceOfPointToLineSegment(Vector2f var1, Vector2f var2, Vector2f var3) {
         Vector2f var4 = UI3DScene.allocVector2f().set(var2).sub(var1);
         Vector2f var5 = UI3DScene.allocVector2f().set(var1).sub(var3);
         float var6 = var4.dot(var5);
         if (var6 > 0.0F) {
            float var10 = var5.dot(var5);
            UI3DScene.releaseVector2f(var4);
            UI3DScene.releaseVector2f(var5);
            return var10;
         } else {
            Vector2f var7 = UI3DScene.allocVector2f().set(var3).sub(var2);
            if (var4.dot(var7) > 0.0F) {
               float var11 = var7.dot(var7);
               UI3DScene.releaseVector2f(var7);
               UI3DScene.releaseVector2f(var4);
               UI3DScene.releaseVector2f(var5);
               return var11;
            } else {
               UI3DScene.releaseVector2f(var7);
               Vector2f var8 = UI3DScene.allocVector2f().set(var4).mul(var6 / var4.dot(var4));
               var5.sub(var8, var8);
               float var9 = var8.dot(var8);
               UI3DScene.releaseVector2f(var8);
               UI3DScene.releaseVector2f(var4);
               UI3DScene.releaseVector2f(var5);
               return var9;
            }
         }
      }

      boolean isClockwise() {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.m_points.size(); ++var2) {
            float var3 = ((Vector2f)this.m_points.get(var2)).x;
            float var4 = ((Vector2f)this.m_points.get(var2)).y;
            float var5 = ((Vector2f)this.m_points.get((var2 + 1) % this.m_points.size())).x;
            float var6 = ((Vector2f)this.m_points.get((var2 + 1) % this.m_points.size())).y;
            var1 += (var5 - var3) * (var6 + var4);
         }

         return (double)var1 > 0.0;
      }

      void triangulate() {
         this.m_triangles.clear();
         if (this.m_points.size() >= 3) {
            if (UI3DScene.s_clipper == null) {
               UI3DScene.s_clipper = new Clipper();
            }

            UI3DScene.s_clipper.clear();
            ByteBuffer var1 = ByteBuffer.allocateDirect(8 * this.m_points.size() * 3);
            int var2;
            Vector2f var3;
            if (this.isClockwise()) {
               for(var2 = this.m_points.size() - 1; var2 >= 0; --var2) {
                  var3 = (Vector2f)this.m_points.get(var2);
                  var1.putFloat(var3.x);
                  var1.putFloat(var3.y);
               }
            } else {
               for(var2 = 0; var2 < this.m_points.size(); ++var2) {
                  var3 = (Vector2f)this.m_points.get(var2);
                  var1.putFloat(var3.x);
                  var1.putFloat(var3.y);
               }
            }

            UI3DScene.s_clipper.addPath(this.m_points.size(), var1, false);
            var2 = UI3DScene.s_clipper.generatePolygons();
            if (var2 >= 1) {
               var1.clear();
               int var5 = UI3DScene.s_clipper.triangulate(0, var1);
               this.m_triangles.clear();

               for(int var4 = 0; var4 < var5; ++var4) {
                  this.m_triangles.add(var1.getFloat());
                  this.m_triangles.add(var1.getFloat());
               }

            }
         }
      }

      int pickPoint(float var1, float var2, float var3) {
         float var4 = 3.4028235E38F;
         int var5 = -1;
         Vector2f var6 = UI3DScene.allocVector2f();
         this.m_scene.m_polygonEditor.setPlane(this.m_translate, this.m_rotate, this.m_plane);

         for(int var7 = 0; var7 < this.m_points.size(); ++var7) {
            Vector2f var8 = (Vector2f)this.m_points.get(var7);
            this.m_scene.m_polygonEditor.planeToUI(var8, var6);
            float var9 = IsoUtils.DistanceTo2D(var1, var2, var6.x, var6.y);
            if (var9 < var3 && var9 < var4) {
               var4 = var9;
               var5 = var7;
            }
         }

         UI3DScene.releaseVector2f(var6);
         return var5;
      }

      void renderPoints() {
         this.m_scene.m_polygonEditor.setPlane(this.m_translate, this.m_rotate, this.m_plane);
         Vector2f var1 = UI3DScene.allocVector2f();

         for(int var2 = 0; var2 < this.m_points.size(); ++var2) {
            Vector2f var3 = (Vector2f)this.m_points.get(var2);
            this.m_scene.m_polygonEditor.planeToUI(var3, var1);
            if (var2 == this.m_highlightPointIndex) {
               this.m_scene.DrawTextureScaledCol((Texture)null, (double)var1.x - 5.0, (double)var1.y - 5.0, 10.0, 10.0, 0.0, 1.0, 0.0, 1.0);
            } else {
               this.m_scene.DrawTextureScaledCol((Texture)null, (double)var1.x - 5.0, (double)var1.y - 5.0, 10.0, 10.0, 1.0, 1.0, 1.0, 1.0);
            }
         }

         UI3DScene.releaseVector2f(var1);
      }

      Vector2f uiToTile(Vector2f var1, float var2, Vector2f var3, Vector2f var4) {
         float var5 = (var3.x - var1.x) / var2;
         float var6 = (var3.y - var1.y) / var2;
         return var4.set(var5, var6);
      }

      void rasterize(BiConsumer<Integer, Integer> var1) {
         Vector2f var2 = UI3DScene.SceneDepthTexture.calculateTextureTopLeft(this.m_scene, 0.0F, 0.0F, 0.0F, UI3DScene.allocVector2f());
         float var3 = UI3DScene.SceneDepthTexture.calculatePixelSize(this.m_scene);
         this.m_scene.m_polygonEditor.setPlane(this.m_translate, this.m_rotate, this.m_plane);
         Vector2f var4 = UI3DScene.allocVector2f();
         Vector2f var5 = UI3DScene.allocVector2f();
         Vector2f var6 = UI3DScene.allocVector2f();
         Vector2f var7 = UI3DScene.allocVector2f();

         for(int var8 = 0; var8 < this.m_triangles.size(); var8 += 6) {
            float var9 = this.m_triangles.get(var8);
            float var10 = this.m_triangles.get(var8 + 1);
            float var11 = this.m_triangles.get(var8 + 2);
            float var12 = this.m_triangles.get(var8 + 3);
            float var13 = this.m_triangles.get(var8 + 4);
            float var14 = this.m_triangles.get(var8 + 5);
            this.m_scene.m_polygonEditor.planeToUI(var4.set(var9, var10), var5);
            this.m_scene.m_polygonEditor.planeToUI(var4.set(var11, var12), var6);
            this.m_scene.m_polygonEditor.planeToUI(var4.set(var13, var14), var7);
            this.uiToTile(var2, var3, var5, var5);
            this.uiToTile(var2, var3, var6, var6);
            this.uiToTile(var2, var3, var7, var7);
            s_rasterize.scanTriangle(var5.x, var5.y, var6.x, var6.y, var7.x, var7.y, -1000, 1000, var1);
         }

         UI3DScene.releaseVector2f(var4);
         UI3DScene.releaseVector2f(var5);
         UI3DScene.releaseVector2f(var6);
         UI3DScene.releaseVector2f(var7);
         UI3DScene.releaseVector2f(var2);
      }
   }

   public static final class Ray {
      public final Vector3f origin = new Vector3f();
      public final Vector3f direction = new Vector3f();
      public float t;

      public Ray() {
      }

      Ray set(Ray var1) {
         this.origin.set(var1.origin);
         this.direction.set(var1.direction);
         this.t = var1.t;
         return this;
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

   private abstract static class SceneGeometry extends SceneObject {
      boolean m_bSelected = false;

      SceneGeometry(UI3DScene var1, String var2) {
         super(var1, var2);
      }

      public abstract String getTypeName();

      public boolean isBox() {
         return false;
      }

      public SceneBox asBox() {
         return (SceneBox)Type.tryCastTo(this, SceneBox.class);
      }

      public boolean isCylinder() {
         return false;
      }

      public SceneCylinder asCylinder() {
         return (SceneCylinder)Type.tryCastTo(this, SceneCylinder.class);
      }

      public boolean isPolygon() {
         return false;
      }

      public ScenePolygon asPolygon() {
         return (ScenePolygon)Type.tryCastTo(this, ScenePolygon.class);
      }

      abstract Matrix4f getOriginTransform(String var1, Matrix4f var2);

      abstract float getNormalizedDepthAt(float var1, float var2);

      abstract TileGeometryFile.Geometry toGeometryFileObject();
   }

   private static final class SceneModel extends SceneObject {
      SpriteModel m_spriteModel;
      ModelScript m_modelScript;
      Model m_model;
      Texture m_texture;
      boolean m_useWorldAttachment = false;
      boolean m_weaponRotationHack = false;
      boolean m_ignoreVehicleScale = false;
      boolean m_spriteModelEditor = false;

      SceneModel(UI3DScene var1, String var2, ModelScript var3, Model var4) {
         super(var1, var2);
         Objects.requireNonNull(var3);
         Objects.requireNonNull(var4);
         this.m_modelScript = var3;
         this.m_model = var4;
         this.setSpriteModel((SpriteModel)null);
      }

      void setSpriteModel(SpriteModel var1) {
         this.m_spriteModel = var1;
         this.m_texture = null;
         if (var1 != null && var1.getTextureName() != null) {
            if (var1.getTextureName().contains("media/")) {
               this.m_texture = Texture.getSharedTexture(var1.getTextureName());
            } else {
               this.m_texture = Texture.getSharedTexture("media/textures/" + var1.getTextureName() + ".png");
            }
         }

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
            ModelInstanceRenderData.makeAttachmentTransform(var3, var2);
            return var2;
         }
      }
   }

   private static final class SceneCylinder extends SceneGeometry {
      float m_radius;
      float m_height;

      SceneCylinder(UI3DScene var1, String var2) {
         super(var1, var2);
      }

      public String getTypeName() {
         return "cylinder";
      }

      void initClone(SceneObject var1) {
         SceneCylinder var2 = (SceneCylinder)var1;
         super.initClone(var1);
         var2.m_radius = this.m_radius;
         var2.m_height = this.m_height;
      }

      SceneObject clone(String var1) {
         SceneCylinder var2 = new SceneCylinder(this.m_scene, var1);
         this.initClone(var2);
         return var2;
      }

      SceneObjectRenderData renderMain() {
         if (!this.m_scene.m_bDrawGeometry) {
            return null;
         } else {
            SceneCylinderDrawer var1 = (SceneCylinderDrawer)UI3DScene.SceneCylinderDrawer.s_pool.alloc();
            var1.m_sceneObject = this;
            var1.m_radiusBase = this.m_radius;
            var1.m_radiusTop = this.m_radius;
            var1.m_length = this.m_height;
            SpriteRenderer.instance.drawGeneric(var1);
            return null;
         }
      }

      public boolean isCylinder() {
         return true;
      }

      Matrix4f getOriginTransform(String var1, Matrix4f var2) {
         var2.identity();
         switch (var1) {
            case "xMin":
               var2.translation(-this.m_radius, 0.0F, 0.0F);
               break;
            case "xMax":
               var2.translation(this.m_radius, 0.0F, 0.0F);
               break;
            case "yMin":
               var2.translation(0.0F, -this.m_radius, 0.0F);
               break;
            case "yMax":
               var2.translation(0.0F, this.m_radius, 0.0F);
               break;
            case "zMin":
               var2.translation(0.0F, 0.0F, -this.m_height / 2.0F);
               break;
            case "zMax":
               var2.translation(0.0F, 0.0F, this.m_height / 2.0F);
         }

         return var2;
      }

      float getNormalizedDepthAt(float var1, float var2) {
         return TileGeometryUtils.getNormalizedDepthOnCylinderAt(var1, var2, this.m_translate, this.m_rotate, this.m_radius, this.m_height);
      }

      TileGeometryFile.Geometry toGeometryFileObject() {
         TileGeometryFile.Cylinder var1 = new TileGeometryFile.Cylinder();
         var1.translate.set(this.m_translate);
         var1.rotate.set(this.m_rotate);
         var1.radius1 = this.m_radius;
         var1.radius2 = this.m_radius;
         var1.height = this.m_height;
         return var1;
      }

      boolean intersect(Ray var1, CylinderUtils.IntersectionRecord var2) {
         return CylinderUtils.intersect(this.m_radius, this.m_height, var1, var2);
      }

      AABB getAABB(AABB var1) {
         Matrix4f var2 = UI3DScene.allocMatrix4f().rotationXYZ(this.m_rotate.x * 0.017453292F, this.m_rotate.y * 0.017453292F, this.m_rotate.z * 0.017453292F);
         Vector3f var3 = var2.transformDirection(UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F)).normalize();
         UI3DScene.releaseMatrix4f(var2);
         Vector3f var4 = UI3DScene.allocVector3f().set(var3).mul(-this.m_height / 2.0F).add(this.m_translate);
         Vector3f var5 = UI3DScene.allocVector3f().set(var3).mul(this.m_height / 2.0F).add(this.m_translate);
         UI3DScene.releaseVector3f(var3);
         Vector3f var6 = UI3DScene.allocVector3f().set(var5).sub(var4);
         Vector3f var7 = UI3DScene.allocVector3f().set(var6).mul(var6);
         var7.div(var6.dot(var6));
         UI3DScene.releaseVector3f(var6);
         Vector3f var8 = UI3DScene.allocVector3f().set((double)this.m_radius * Math.sqrt(1.0 - (double)var7.x), (double)this.m_radius * Math.sqrt(1.0 - (double)var7.y), (double)this.m_radius * Math.sqrt(1.0 - (double)var7.z));
         UI3DScene.releaseVector3f(var7);
         Vector3f var9 = UI3DScene.allocVector3f().set(var4).sub(var8);
         Vector3f var10 = UI3DScene.allocVector3f().set(var5).sub(var8);
         Vector3f var11 = UI3DScene.allocVector3f().set(var4).add(var8);
         Vector3f var12 = UI3DScene.allocVector3f().set(var5).add(var8);
         UI3DScene.releaseVector3f(var4);
         UI3DScene.releaseVector3f(var5);
         UI3DScene.releaseVector3f(var8);
         Vector3f var13 = UI3DScene.allocVector3f().set(var9).min(var10);
         Vector3f var14 = UI3DScene.allocVector3f().set(var11).max(var12);
         UI3DScene.releaseVector3f(var9);
         UI3DScene.releaseVector3f(var10);
         UI3DScene.releaseVector3f(var11);
         UI3DScene.releaseVector3f(var12);
         var1.set(this.m_translate.x, this.m_translate.y, this.m_translate.z, var14.x - var13.x, var14.y - var13.y, var14.z - var13.z, 1.0F, 1.0F, 1.0F, 1.0F, false);
         UI3DScene.releaseVector3f(var13);
         UI3DScene.releaseVector3f(var14);
         return var1;
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
      float a;
      boolean bQuads = false;

      private AABB() {
      }

      AABB set(AABB var1) {
         return this.set(var1.x, var1.y, var1.z, var1.w, var1.h, var1.L, var1.r, var1.g, var1.b, var1.a, var1.bQuads);
      }

      AABB set(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, boolean var11) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.w = var4;
         this.h = var5;
         this.L = var6;
         this.r = var7;
         this.g = var8;
         this.b = var9;
         this.a = var10;
         this.bQuads = var11;
         return this;
      }
   }

   private static final class ScenePlayer extends SceneCharacter {
      ScenePlayer(UI3DScene var1, String var2) {
         super(var1, var2);
      }

      void initAnimatedModel() {
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
   }

   private static final class SceneBox extends SceneGeometry {
      final Vector3f m_min = new Vector3f(-0.5F, 0.0F, -0.5F);
      final Vector3f m_max = new Vector3f(0.5F, 2.44949F, 0.5F);

      SceneBox(UI3DScene var1, String var2) {
         super(var1, var2);
      }

      public String getTypeName() {
         return "box";
      }

      void initClone(SceneObject var1) {
         SceneBox var2 = (SceneBox)var1;
         super.initClone(var1);
         var2.m_min.set(this.m_min);
         var2.m_max.set(this.m_max);
      }

      SceneObject clone(String var1) {
         SceneBox var2 = new SceneBox(this.m_scene, var1);
         this.initClone(var2);
         return var2;
      }

      SceneObjectRenderData renderMain() {
         if (!this.m_scene.m_bDrawGeometry) {
            return null;
         } else {
            Box3D var1 = (Box3D)UI3DScene.s_box3DPool.alloc();
            var1.x = this.m_translate.x;
            var1.y = this.m_translate.y;
            var1.z = this.m_translate.z;
            var1.rx = this.m_rotate.x;
            var1.ry = this.m_rotate.y;
            var1.rz = this.m_rotate.z;
            var1.xMin = this.m_min.x;
            var1.yMin = this.m_min.y;
            var1.zMin = this.m_min.z;
            var1.xMax = this.m_max.x;
            var1.yMax = this.m_max.y;
            var1.zMax = this.m_max.z;
            var1.r = var1.g = var1.b = var1.a = 1.0F;
            var1.bQuads = false;
            if (this.m_bSelected) {
               var1.b = 0.0F;
            }

            this.m_scene.m_box3D.add(var1);
            return null;
         }
      }

      public boolean isBox() {
         return true;
      }

      Matrix4f getOriginTransform(String var1, Matrix4f var2) {
         var2.identity();
         switch (var1) {
            case "xMin":
               var2.translation(this.m_min.x(), 0.0F, 0.0F);
               break;
            case "xMax":
               var2.translation(this.m_max.x(), 0.0F, 0.0F);
               break;
            case "yMin":
               var2.translation(0.0F, this.m_min.y(), 0.0F);
               break;
            case "yMax":
               var2.translation(0.0F, this.m_max.y(), 0.0F);
               break;
            case "zMin":
               var2.translation(0.0F, 0.0F, this.m_min.z());
               break;
            case "zMax":
               var2.translation(0.0F, 0.0F, this.m_max.z());
         }

         return var2;
      }

      float getNormalizedDepthAt(float var1, float var2) {
         return TileGeometryUtils.getNormalizedDepthOnBoxAt(var1, var2, this.m_translate, this.m_rotate, this.m_min, this.m_max);
      }

      TileGeometryFile.Geometry toGeometryFileObject() {
         TileGeometryFile.Box var1 = new TileGeometryFile.Box();
         var1.translate.set(this.m_translate);
         var1.rotate.set(this.m_rotate);
         var1.min.set(this.m_min);
         var1.max.set(this.m_max);
         return var1;
      }
   }

   private static final class ParentVehiclePart {
      SceneVehicle m_vehicle;
      String m_partId;
      String m_partModelId;
      String m_attachmentName;

      private ParentVehiclePart() {
      }

      Matrix4f getGlobalTransform(Matrix4f var1) {
         this.m_vehicle.getTransformForPart(this.m_partId, this.m_partModelId, this.m_attachmentName, false, var1);
         VehicleRenderData var2 = (VehicleRenderData)UI3DScene.VehicleRenderData.s_pool.alloc();
         var2.initVehicle(this.m_vehicle);
         VehicleModelRenderData var3 = (VehicleModelRenderData)var2.m_partToRenderData.get(this.m_partId);
         if (var3 != null) {
            var3.xfrm.mul(var1, var1);
         }

         var2.release();
         return var1;
      }

      VehicleScript.Part getScriptPart() {
         return this.m_vehicle != null && this.m_vehicle.m_script != null && this.m_partId != null ? this.m_vehicle.m_script.getPartById(this.m_partId) : null;
      }

      VehicleScript.Model getScriptModel() {
         VehicleScript.Part var1 = this.getScriptPart();
         return var1 != null && this.m_partModelId != null ? var1.getModelById(this.m_partModelId) : null;
      }
   }

   private static final class PositionRotation {
      final Vector3f pos = new Vector3f();
      final Vector3f rot = new Vector3f();
      boolean bRelativeToOrigin = false;

      private PositionRotation() {
      }

      PositionRotation set(PositionRotation var1) {
         this.pos.set(var1.pos);
         this.rot.set(var1.rot);
         this.bRelativeToOrigin = var1.bRelativeToOrigin;
         return this;
      }

      PositionRotation set(float var1, float var2, float var3) {
         this.pos.set(var1, var2, var3);
         this.rot.set(0.0F, 0.0F, 0.0F);
         this.bRelativeToOrigin = false;
         return this;
      }

      PositionRotation set(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.pos.set(var1, var2, var3);
         this.rot.set(var4, var5, var6);
         this.bRelativeToOrigin = false;
         return this;
      }
   }

   private static final class SceneDepthTexture extends SceneObject {
      Texture m_texture;

      SceneDepthTexture(UI3DScene var1, String var2) {
         super(var1, var2);
      }

      SceneObjectRenderData renderMain() {
         IndieGL.enableDepthTest();
         IndieGL.glDepthMask(true);
         IndieGL.glDepthFunc(519);
         if (!this.m_scene.m_bDrawGeometry) {
            IndieGL.glColorMask(false, false, false, false);
         }

         this.renderTexture(this.m_translate.x, this.m_translate.y, this.m_translate.z);
         if (!this.m_scene.m_bDrawGeometry) {
            IndieGL.glColorMask(true, true, true, true);
         }

         IndieGL.disableDepthTest();
         IndieGL.glDepthMask(false);
         return null;
      }

      static float calculatePixelSize(UI3DScene var0) {
         float var1 = var0.sceneToUIX(0.0F, 0.0F, 0.0F);
         float var2 = var0.sceneToUIY(0.0F, 0.0F, 0.0F);
         float var3 = var0.sceneToUIX(1.0F, 0.0F, 0.0F);
         float var4 = var0.sceneToUIY(1.0F, 0.0F, 0.0F);
         return (float)(Math.sqrt((double)((var3 - var1) * (var3 - var1) + (var4 - var2) * (var4 - var2))) / Math.sqrt(5120.0));
      }

      static Vector2f calculateTextureTopLeft(UI3DScene var0, float var1, float var2, float var3, Vector2f var4) {
         float var5 = var0.sceneToUIX(var1, var2, var3);
         float var6 = var0.sceneToUIY(var1, var2, var3);
         float var7 = calculatePixelSize(var0);
         float var8 = var5 - 64.0F * var7;
         float var9 = var6 - 224.0F * var7;
         byte var10 = 1;
         var8 += (float)var10 * var7;
         return var4.set(var8, var9);
      }

      void renderTexture(float var1, float var2, float var3) {
         Matrix4f var4 = UI3DScene.allocMatrix4f();
         var4.set(this.m_scene.m_projection);
         var4.mul(this.m_scene.m_modelView);
         Vector3f var5 = UI3DScene.allocVector3f();
         float var6 = var4.transformPosition(var5.set(var1 + 1.5F, var2 + 0.0F, var3 + 1.5F)).z;
         float var7 = var4.transformPosition(var5.set(var1 - 0.5F, var2 + 0.0F, var3 - 0.5F)).z;
         var6 = (var6 + 1.0F) / 2.0F;
         var7 = (var7 + 1.0F) / 2.0F;
         UI3DScene.releaseMatrix4f(var4);
         UI3DScene.releaseVector3f(var5);
         this.m_texture.getTextureId().setMagFilter(9728);
         IndieGL.StartShader(SceneShaderStore.TileDepthShader.getID());
         IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "zDepth", var6);
         boolean var8 = false;
         IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "drawPixels", var8 ? 1 : 0);
         IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "zDepthBlendZ", var6);
         IndieGL.shaderSetValue(SceneShaderStore.TileDepthShader, "zDepthBlendToZ", var7);
         float var9 = calculatePixelSize(this.m_scene);
         Vector2f var10 = calculateTextureTopLeft(this.m_scene, var1, var2, var3, UI3DScene.allocVector2f());
         SpriteRenderer.instance.render(this.m_texture, var10.x + this.m_texture.getOffsetX() * var9, var10.y + this.m_texture.getOffsetY() * var9, (float)this.m_texture.getWidth() * var9, (float)this.m_texture.getHeight() * var9, 1.0F, 1.0F, 1.0F, 0.75F, (Consumer)null);
         UI3DScene.releaseVector2f(var10);
         int var11 = SpriteRenderer.instance.m_states.getPopulatingActiveState().numSprites;
         TextureDraw var12 = SpriteRenderer.instance.m_states.getPopulatingActiveState().sprite[var11 - 1];
         Texture var13 = this.m_texture;
         var12.tex1 = var13;
         var12.tex1_u0 = var13.getXStart();
         var12.tex1_u1 = var13.getXEnd();
         var12.tex1_u2 = var13.getXEnd();
         var12.tex1_u3 = var13.getXStart();
         var12.tex1_v0 = var13.getYStart();
         var12.tex1_v1 = var13.getYStart();
         var12.tex1_v2 = var13.getYEnd();
         var12.tex1_v3 = var13.getYEnd();
         IndieGL.EndShader();
      }
   }

   private static final class SceneAnimal extends SceneCharacter implements IAnimalVisual {
      AnimalVisual m_visual;
      final ItemVisuals m_itemVisuals = new ItemVisuals();
      AnimalDefinitions m_definition;
      AnimalBreed m_breed;

      SceneAnimal(UI3DScene var1, String var2, AnimalDefinitions var3, AnimalBreed var4) {
         super(var1, var2);
         this.m_definition = var3;
         this.m_breed = var4;
      }

      void setAnimalDefinition(AnimalDefinitions var1, AnimalBreed var2) {
         this.m_definition = var1;
         this.m_breed = var2;
         if (this.isFemale()) {
            this.m_visual.setSkinTextureName((String)PZArrayUtil.pickRandom((List)this.m_breed.texture));
         } else {
            this.m_visual.setSkinTextureName(this.m_breed.textureMale);
         }

         if (!this.m_animatedModel.GetAnimSetName().endsWith("-editor")) {
            this.m_animatedModel.setAnimSetName(this.m_definition.animset);
         }

         this.m_animatedModel.setModelData(this.m_visual, this.m_itemVisuals);
      }

      void initAnimatedModel() {
         this.m_visual = new AnimalVisual(this);
         if (this.isFemale()) {
            this.m_visual.setSkinTextureName((String)PZArrayUtil.pickRandom((List)this.m_breed.texture));
         } else {
            this.m_visual.setSkinTextureName(this.m_breed.textureMale);
         }

         this.m_animatedModel.setAnimSetName(this.m_definition.animset);
         this.m_animatedModel.setState("idle");
         this.m_animatedModel.setModelData(this.m_visual, this.m_itemVisuals);
         this.m_animatedModel.setAlpha(0.5F);
         this.m_animatedModel.setAnimate(false);
      }

      public AnimalVisual getAnimalVisual() {
         return this.m_visual;
      }

      public String getAnimalType() {
         return this.m_definition.getAnimalType();
      }

      public float getAnimalSize() {
         return 1.0F;
      }

      public HumanVisual getHumanVisual() {
         return null;
      }

      public void getItemVisuals(ItemVisuals var1) {
         var1.clear();
         var1.addAll(this.m_itemVisuals);
      }

      public boolean isFemale() {
         return this.m_definition.female;
      }

      public boolean isZombie() {
         return false;
      }

      public boolean isSkeleton() {
         return false;
      }
   }

   private static final class Box3D {
      float x;
      float y;
      float z;
      float xMin;
      float yMin;
      float zMin;
      float xMax;
      float yMax;
      float zMax;
      float rx;
      float ry;
      float rz;
      float r;
      float g;
      float b;
      float a;
      boolean bQuads = false;

      private Box3D() {
      }

      Box3D set(Box3D var1) {
         return this.set(var1.x, var1.y, var1.z, var1.xMin, var1.yMin, var1.zMin, var1.xMax, var1.yMax, var1.zMax, var1.rx, var1.ry, var1.rz, var1.r, var1.g, var1.b, var1.a, var1.bQuads);
      }

      Box3D set(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, boolean var17) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.xMin = var4;
         this.yMin = var5;
         this.zMin = var6;
         this.xMax = var7;
         this.yMax = var8;
         this.zMax = var9;
         this.rx = var10;
         this.ry = var11;
         this.rz = var12;
         this.r = var13;
         this.g = var14;
         this.b = var15;
         this.a = var16;
         this.bQuads = var17;
         return this;
      }
   }

   private static final class SpriteGridTextureMaskDrawer extends TextureDraw.GenericDrawer {
      UI3DScene scene;
      IsoSprite sprite;
      float sx;
      float sy;
      float sx2;
      float sy2;
      float pixelSize;
      float r;
      float g;
      float b;
      float a;

      private SpriteGridTextureMaskDrawer() {
      }

      public void render() {
         IsoSpriteGrid var1 = this.sprite.getSpriteGrid();
         int var2 = var1.getSpriteIndex(this.sprite);
         int var3 = var1.getSpriteGridPosX(this.sprite);
         int var4 = var1.getSpriteGridPosY(this.sprite);
         int var5 = var1.getSpriteGridPosZ(this.sprite);

         for(int var6 = 0; var6 < var1.getSpriteCount(); ++var6) {
            if (var6 != var2) {
               IsoSprite var7 = var1.getSpriteFromIndex(var6);
               if (var7 != null) {
                  Texture var8 = var7.getTextureForCurrentFrame(IsoDirections.N);
                  if (var8 != null && var8.getMask() != null) {
                     int var9 = var1.getSpriteGridPosX(var7);
                     int var10 = var1.getSpriteGridPosY(var7);
                     int var11 = var1.getSpriteGridPosZ(var7);
                     int var12 = var9 - var3;
                     int var13 = var10 - var4;
                     int var14 = var11 - var5;
                     this.render(var8, this.scene.sceneToUIX((float)var12 + 0.5F, (float)(var14 * 3) * 0.8164967F, (float)var13 + 0.5F) - 64.0F * this.pixelSize, this.scene.sceneToUIY((float)var12 + 0.5F, (float)(var14 * 3) * 0.8164967F, (float)var13 + 0.5F) - 256.0F * this.pixelSize);
                  }
               }
            }
         }

      }

      void render(Texture var1, float var2, float var3) {
         Mask var4 = var1.getMask();
         if (var4 != null) {
            VBORenderer var5 = VBORenderer.getInstance();
            var5.startRun(VBORenderer.getInstance().FORMAT_PositionColor);
            float var6 = 0.0F;

            for(int var7 = 0; var7 < 256; ++var7) {
               for(int var8 = 0; var8 < 128; ++var8) {
                  if (var1.isMaskSet(var8, var7)) {
                     float var9 = var2 + (float)var8 * this.pixelSize;
                     float var10 = var3 + (float)var7 * this.pixelSize;
                     float var11 = var2 + (float)(var8 + 1) * this.pixelSize;
                     float var12 = var3 + (float)(var7 + 1) * this.pixelSize;
                     var5.addLine(var9, var10, var6, var11, var10, var6, this.r, this.g, this.b, this.a);
                     var5.addLine(var11, var10, var6, var11, var12, var6, this.r, this.g, this.b, this.a);
                     var5.addLine(var11, var12, var6, var9, var12, var6, this.r, this.g, this.b, this.a);
                     var5.addLine(var9, var12, var6, var9, var10, var6, this.r, this.g, this.b, this.a);
                  }
               }
            }

            var5.endRun();
            var5.flush();
         }
      }
   }

   private static final class TextureMaskDrawer extends TextureDraw.GenericDrawer {
      UI3DScene scene;
      Texture texture;
      float sx;
      float sy;
      float sx2;
      float sy2;
      float pixelSize;
      float r;
      float g;
      float b;
      float a;

      private TextureMaskDrawer() {
      }

      public void render() {
         Mask var1 = this.texture.getMask();
         if (var1 != null) {
            VBORenderer var2 = VBORenderer.getInstance();
            var2.startRun(VBORenderer.getInstance().FORMAT_PositionColor);
            float var3 = 0.0F;

            for(int var4 = 0; var4 < 256; ++var4) {
               for(int var5 = 0; var5 < 128; ++var5) {
                  if (this.texture.isMaskSet(var5, var4)) {
                     float var6 = this.sx + (float)var5 * this.pixelSize;
                     float var7 = this.sy + (float)var4 * this.pixelSize;
                     float var8 = this.sx + (float)(var5 + 1) * this.pixelSize;
                     float var9 = this.sy + (float)(var4 + 1) * this.pixelSize;
                     var2.addLine(var6, var7, var3, var8, var7, var3, this.r, this.g, this.b, this.a);
                     var2.addLine(var8, var7, var3, var8, var9, var3, this.r, this.g, this.b, this.a);
                     var2.addLine(var8, var9, var3, var6, var9, var3, this.r, this.g, this.b, this.a);
                     var2.addLine(var6, var9, var3, var6, var7, var3, this.r, this.g, this.b, this.a);
                  }
               }
            }

            var2.endRun();
            var2.flush();
         }
      }
   }

   public static final class PhysicsMesh {
      float x;
      float y;
      float z;
      float rx;
      float ry;
      float rz;
      float r;
      float g;
      float b;
      String physicsShapeScript;
      float scale;
      float[] points;

      public PhysicsMesh() {
      }

      PhysicsMesh set(Vector3f var1, Vector3f var2, float var3, String var4, float var5, float var6, float var7) {
         this.x = var1.x;
         this.y = var1.y;
         this.z = var1.z;
         this.rx = var2.x;
         this.ry = var2.y;
         this.rz = var2.z;
         this.scale = var3;
         this.physicsShapeScript = var4;
         this.r = var5;
         this.g = var6;
         this.b = var7;
         this.points = null;
         return this;
      }

      PhysicsMesh set(PhysicsMesh var1) {
         this.x = var1.x;
         this.y = var1.y;
         this.z = var1.z;
         this.rx = var1.rx;
         this.ry = var1.ry;
         this.rz = var1.rz;
         this.scale = var1.scale;
         this.physicsShapeScript = var1.physicsShapeScript;
         this.r = var1.r;
         this.g = var1.g;
         this.b = var1.b;
         this.points = var1.points;
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
         int var1;
         for(var1 = 0; var1 < this.m_renderData.m_models.size(); ++var1) {
            GL11.glPushAttrib(1048575);
            GL11.glPushClientAttrib(-1);
            this.render(var1);
            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
            Texture.lastTextureID = -1;
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            SpriteRenderer.ringBuffer.restoreVBOs = true;
         }

         GL11.glPushAttrib(1048575);
         GL11.glPushClientAttrib(-1);

         for(var1 = 0; var1 < this.m_renderData.m_models.size(); ++var1) {
            VehicleModelRenderData var2 = (VehicleModelRenderData)this.m_renderData.m_models.get(var1);
            this.m_renderData.m_transform.set(var2.xfrm);
            ModelCamera.instance.Begin();
            this.renderSkeleton(var2);
            ModelCamera.instance.End();
         }

         GL11.glPopAttrib();
         GL11.glPopClientAttrib();
      }

      private void render(int var1) {
         VehicleModelRenderData var2 = (VehicleModelRenderData)this.m_renderData.m_models.get(var1);
         this.m_renderData.m_transform.set(var2.xfrm);
         ModelCamera.instance.Begin();
         Model var3 = var2.model;
         boolean var4 = var3.bStatic;
         Shader var5;
         if (Core.bDebug && DebugOptions.instance.Model.Render.Wireframe.getValue()) {
            GL11.glPolygonMode(1032, 6913);
            GL11.glEnable(2848);
            GL11.glLineWidth(0.75F);
            var5 = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", var4, false);
            if (var5 != null) {
               var5.Start();
               if (var3.bStatic) {
                  var5.setTransformMatrix(this.IDENTITY.identity(), false);
               } else {
                  var5.setMatrixPalette(var2.matrixPalette, true);
               }

               var3.Mesh.Draw(var5);
               var5.End();
            }

            GL11.glDisable(2848);
            ModelCamera.instance.End();
         } else {
            var5 = var3.Effect;
            if (var5 != null && var5.isVehicleShader()) {
               GL11.glDepthFunc(513);
               GL11.glDepthMask(true);
               GL11.glDepthRange(0.0, 1.0);
               GL11.glEnable(2929);
               GL11.glColor3f(1.0F, 1.0F, 1.0F);
               var5.Start();
               Texture var8 = var2.tex;
               if (var8 == null && var1 > 0) {
                  var8 = ((VehicleModelRenderData)this.m_renderData.m_models.get(0)).tex;
               }

               if (var8 != null) {
                  var5.setTexture(var8, "Texture0", 0);
                  GL11.glTexEnvi(8960, 8704, 7681);
                  if (this.m_vehicle.m_script.getSkinCount() > 0 && this.m_vehicle.m_script.getSkin(0).textureMask != null) {
                     Texture var7 = Texture.getSharedTexture("media/textures/" + this.m_vehicle.m_script.getSkin(0).textureMask + ".png");
                     var5.setTexture(var7, "TextureMask", 2);
                     GL11.glTexEnvi(8960, 8704, 7681);
                  }
               }

               var5.setDepthBias(0.0F);
               var5.setAmbient(1.0F);
               var5.setLightingAmount(1.0F);
               var5.setHueShift(0.0F);
               var5.setTint(1.0F, 1.0F, 1.0F);
               var5.setAlpha(1.0F);

               for(int var9 = 0; var9 < 5; ++var9) {
                  var5.setLight(var9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
               }

               var5.setTextureUninstall1(this.fzeroes);
               var5.setTextureUninstall2(this.fzeroes);
               var5.setTextureLightsEnables2(this.fzeroes);
               var5.setTextureDamage1Enables1(this.fzeroes);
               var5.setTextureDamage1Enables2(this.fzeroes);
               var5.setTextureDamage2Enables1(this.fzeroes);
               var5.setTextureDamage2Enables2(this.fzeroes);
               var5.setMatrixBlood1(this.fzeroes, this.fzeroes);
               var5.setMatrixBlood2(this.fzeroes, this.fzeroes);
               var5.setTextureRustA(0.0F);
               var5.setTexturePainColor(this.paintColor, 1.0F);
               if (var3.bStatic) {
                  var5.setTransformMatrix(this.IDENTITY.identity(), false);
               } else {
                  var5.setMatrixPalette(var2.matrixPalette, true);
               }

               var5.setTargetDepth(0.5F);
               var3.Mesh.Draw(var5);
               var5.End();
            } else if (var5 != null && var3.Mesh != null && var3.Mesh.isReady()) {
               GL11.glDepthFunc(513);
               GL11.glDepthMask(true);
               GL11.glDepthRange(0.0, 1.0);
               GL11.glEnable(2929);
               GL11.glColor3f(1.0F, 1.0F, 1.0F);
               var5.Start();
               if (var3.tex != null) {
                  var5.setTexture(var3.tex, "Texture", 0);
               }

               var5.setDepthBias(0.0F);
               var5.setAmbient(1.0F);
               var5.setLightingAmount(1.0F);
               var5.setHueShift(0.0F);
               var5.setTint(1.0F, 1.0F, 1.0F);
               var5.setAlpha(1.0F);

               for(int var6 = 0; var6 < 5; ++var6) {
                  var5.setLight(var6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
               }

               var5.setTransformMatrix(this.IDENTITY.identity(), false);
               var5.setTargetDepth(0.5F);
               var3.Mesh.Draw(var5);
               var5.End();
            }

            ModelCamera.instance.End();
            this.bRendered = true;
         }
      }

      private void renderSkeleton(VehicleModelRenderData var1) {
         TFloatArrayList var2 = var1.m_boneCoords;
         if (!var2.isEmpty()) {
            VBORenderer var3 = VBORenderer.getInstance();
            var3.startRun(var3.FORMAT_PositionColor);
            var3.setDepthTest(false);
            var3.setLineWidth(1.0F);
            var3.setMode(1);

            for(int var4 = 0; var4 < var2.size(); var4 += 6) {
               Color var5 = Model.debugDrawColours[var4 % Model.debugDrawColours.length];
               var3.addElement();
               var3.setColor(var5.r, var5.g, var5.b, 1.0F);
               float var6 = var2.get(var4);
               float var7 = var2.get(var4 + 1);
               float var8 = var2.get(var4 + 2);
               var3.setVertex(var6, var7, var8);
               var3.addElement();
               var3.setColor(var5.r, var5.g, var5.b, 1.0F);
               var6 = var2.get(var4 + 3);
               var7 = var2.get(var4 + 4);
               var8 = var2.get(var4 + 5);
               var3.setVertex(var6, var7, var8);
            }

            var3.endRun();
            var3.flush();
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            GL11.glEnable(2929);
         }
      }

      public void postRender() {
      }
   }

   private static final class ModelDrawer extends TextureDraw.GenericDrawer {
      SceneModel m_model;
      ModelRenderData m_renderData;
      boolean bRendered;
      FloatBuffer matrixPalette;
      TFloatArrayList boneCoords;
      Texture texture;

      private ModelDrawer() {
      }

      public void init(SceneModel var1, ModelRenderData var2) {
         this.m_model = var1;
         this.m_renderData = var2;
         this.bRendered = false;
         this.matrixPalette = null;
         this.boneCoords = null;
         this.texture = var1.m_texture;
         if (!var1.m_modelScript.bStatic && var1.m_spriteModel != null && var1.m_spriteModel.getAnimationName() != null) {
            this.matrixPalette = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(var1.m_model, var1.m_spriteModel.getAnimationName(), var1.m_spriteModel.getAnimationTime());
            if (this.matrixPalette != null) {
               this.matrixPalette.position(0);
            }

            this.boneCoords = IsoObjectAnimations.getInstance().getBonesForFrame(var1.m_model, var1.m_spriteModel.getAnimationName(), var1.m_spriteModel.getAnimationTime());
         }

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
            Texture var5 = var2.tex;
            if (this.texture != null) {
               var5 = this.texture;
            }

            int var6;
            if (var5 != null) {
               var3.setTexture(var5, "Texture", 0);
               if (var3.getShaderProgram().getName().equalsIgnoreCase("door")) {
                  var6 = var5.getWidthHW();
                  int var7 = var5.getHeightHW();
                  float var8 = var5.xStart * (float)var6 - var5.offsetX;
                  float var9 = var5.yStart * (float)var7 - var5.offsetY;
                  float var10 = var8 + (float)var5.getWidthOrig();
                  float var11 = var9 + (float)var5.getHeightOrig();
                  Vector2 var12 = BaseVehicle.allocVector2();
                  var3.getShaderProgram().setValue("UVOffset", var12.set(var8 / (float)var6, var9 / (float)var7));
                  var3.getShaderProgram().setValue("UVScale", var12.set((var10 - var8) / (float)var6, (var11 - var9) / (float)var7));
                  BaseVehicle.releaseVector2(var12);
                  GL11.glEnable(2884);
                  GL11.glCullFace(1028);
               }
            }

            var3.setDepthBias(0.0F);
            var3.setAmbient(1.0F);
            var3.setLightingAmount(1.0F);
            var3.setHueShift(0.0F);
            var3.setTint(1.0F, 1.0F, 1.0F);
            var3.setAlpha(1.0F);

            for(var6 = 0; var6 < 5; ++var6) {
               var3.setLight(var6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
            }

            if (var2.bStatic) {
               var3.setTransformMatrix(this.m_renderData.m_transform, false);
            } else if ("door".equalsIgnoreCase(var3.getShaderProgram().getName())) {
               if (this.matrixPalette != null) {
                  var3.setMatrixPalette(this.matrixPalette);
               }

               PZGLUtil.pushAndMultMatrix(5888, this.m_renderData.m_transform);
               if (this.m_model.m_modelScript.meshName.contains("door1")) {
               }
            }

            var3.setTargetDepth(0.5F);
            var2.Mesh.Draw(var3);
            var3.End();
            if (DebugOptions.instance.Model.Render.Bones.getValue()) {
               this.renderSkeleton();
            }

            if (!var2.bStatic && "door".equalsIgnoreCase(var3.getShaderProgram().getName())) {
               PZGLUtil.popMatrix(5888);
            }

            if (Core.bDebug) {
            }

            if (DebugOptions.instance.Model.Render.Axis.getValue()) {
            }

            if (var4.m_bDrawAttachments) {
               Matrix4f var13 = UI3DScene.allocMatrix4f();
               var13.set(this.m_renderData.m_transform);
               var13.mul(var2.Mesh.m_transform);
               var13.scale(this.m_model.m_modelScript.scale);
               Matrix4f var14 = UI3DScene.allocMatrix4f();

               for(int var15 = 0; var15 < this.m_model.m_modelScript.getAttachmentCount(); ++var15) {
                  ModelAttachment var16 = this.m_model.m_modelScript.getAttachment(var15);
                  ModelInstanceRenderData.makeAttachmentTransform(var16, var14);
                  var13.mul(var14, var14);
                  PZGLUtil.pushAndMultMatrix(5888, var14);
                  Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.1F, 1.0F);
                  PZGLUtil.popMatrix(5888);
               }

               UI3DScene.releaseMatrix4f(var13);
               UI3DScene.releaseMatrix4f(var14);
            }

            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
            Texture.lastTextureID = -1;
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            SpriteRenderer.ringBuffer.restoreVBOs = true;
            GL20.glUseProgram(0);
            ShaderHelper.forgetCurrentlyBound();
         }

         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
         this.bRendered = true;
      }

      public void postRender() {
      }

      private void renderSkeleton() {
         if (this.boneCoords != null) {
            if (!this.boneCoords.isEmpty()) {
               VBORenderer var1 = VBORenderer.getInstance();
               var1.flush();
               var1.startRun(var1.FORMAT_PositionColor);
               var1.setDepthTest(false);
               var1.setLineWidth(1.0F);
               var1.setMode(1);

               for(int var2 = 0; var2 < this.boneCoords.size(); var2 += 6) {
                  Color var3 = Model.debugDrawColours[var2 % Model.debugDrawColours.length];
                  float var4 = this.boneCoords.get(var2) / this.m_model.m_modelScript.scale;
                  float var5 = this.boneCoords.get(var2 + 1) / this.m_model.m_modelScript.scale;
                  float var6 = this.boneCoords.get(var2 + 2) / this.m_model.m_modelScript.scale;
                  float var7 = this.boneCoords.get(var2 + 3) / this.m_model.m_modelScript.scale;
                  float var8 = this.boneCoords.get(var2 + 4) / this.m_model.m_modelScript.scale;
                  float var9 = this.boneCoords.get(var2 + 5) / this.m_model.m_modelScript.scale;
                  var1.addLine(var4, var5, var6, var7, var8, var9, var3.r, var3.g, var3.b, 1.0F);
               }

               var1.endRun();
               var1.flush();
               GL11.glColor3f(1.0F, 1.0F, 1.0F);
               GL11.glEnable(2929);
            }
         }
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

         boolean var1 = DebugOptions.instance.Model.Render.Bones.getValue();
         DebugOptions.instance.Model.Render.Bones.setValue(this.m_character.m_bShowBones);
         this.m_character.m_scene.m_CharacterSceneModelCamera.m_renderData = this.m_renderData;
         this.m_character.m_animatedModel.setShowBip01(this.m_character.m_bShowBip01);
         this.m_character.m_animatedModel.DoRender(this.m_character.m_scene.m_CharacterSceneModelCamera);
         DebugOptions.instance.Model.Render.Bones.setValue(var1);
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

   private static class VehicleModelRenderData {
      public Model model;
      public Texture tex;
      public final Matrix4f xfrm = new Matrix4f();
      public FloatBuffer matrixPalette;
      private final TFloatArrayList m_boneCoords = new TFloatArrayList();
      private final ArrayList<org.lwjgl.util.vector.Matrix4f> m_boneMatrices = new ArrayList();
      private static final ObjectPool<VehicleModelRenderData> s_pool = new ObjectPool(VehicleModelRenderData::new);

      private VehicleModelRenderData() {
      }

      void initSkeleton(SceneVehicleModelInfo var1) {
         this.m_boneCoords.clear();
         this.initSkeleton(var1.getAnimationPlayer());
      }

      private void initSkeleton(AnimationPlayer var1) {
         if (var1 != null && var1.hasSkinningData() && !var1.isBoneTransformsNeedFirstFrame()) {
            Integer var2 = (Integer)var1.getSkinningData().BoneIndices.get("Translation_Data");

            for(int var3 = 0; var3 < var1.getModelTransformsCount(); ++var3) {
               if (var2 == null || var3 != var2) {
                  int var4 = (Integer)var1.getSkinningData().SkeletonHierarchy.get(var3);
                  if (var4 >= 0) {
                     this.initSkeleton(var1, var3);
                     this.initSkeleton(var1, var4);
                  }
               }
            }

         }
      }

      private void initSkeleton(AnimationPlayer var1, int var2) {
         org.lwjgl.util.vector.Matrix4f var3 = var1.getModelTransformAt(var2);
         float var4 = var3.m03;
         float var5 = var3.m13;
         float var6 = var3.m23;
         this.m_boneCoords.add(var4);
         this.m_boneCoords.add(var5);
         this.m_boneCoords.add(var6);
      }

      void release() {
         s_pool.release((Object)this);
      }
   }

   private static class VehicleRenderData extends SceneObjectRenderData {
      final ArrayList<VehicleModelRenderData> m_models = new ArrayList();
      final HashMap<String, VehicleModelRenderData> m_partToRenderData = new HashMap();
      final VehicleDrawer m_drawer = new VehicleDrawer();
      private static final ObjectPool<VehicleRenderData> s_pool = new ObjectPool(VehicleRenderData::new);

      private VehicleRenderData() {
      }

      SceneObjectRenderData initVehicle(SceneVehicle var1) {
         super.init(var1);
         UI3DScene.VehicleModelRenderData.s_pool.release((List)this.m_models);
         this.m_models.clear();
         VehicleScript var2 = var1.m_script;
         if (var2.getModel() == null) {
            return null;
         } else {
            this.initVehicleModel(var1);
            float var3 = var2.getModelScale();
            Vector3f var4 = var2.getModel().getOffset();
            Matrix4f var5 = UI3DScene.allocMatrix4f();
            var5.translationRotateScale(var4.x * 1.0F, var4.y, var4.z, 0.0F, 0.0F, 0.0F, 1.0F, var3);
            this.m_transform.mul(var5, var5);

            for(int var6 = 0; var6 < var2.getPartCount(); ++var6) {
               VehicleScript.Part var7 = var2.getPart(var6);
               if (var7.wheel == null) {
                  this.initPartModels(var1, var7, var5);
               } else {
                  this.initWheelModel(var1, var7, var5);
               }
            }

            UI3DScene.releaseMatrix4f(var5);
            this.m_drawer.init(var1, this);
            return this;
         }
      }

      private void initVehicleModel(SceneVehicle var1) {
         SceneVehicleModelInfo var2 = (SceneVehicleModelInfo)var1.m_modelInfo.get(0);
         VehicleModelRenderData var3 = (VehicleModelRenderData)UI3DScene.VehicleModelRenderData.s_pool.alloc();
         var3.model = var2.model;
         var3.tex = var2.tex;
         var3.m_boneCoords.clear();
         this.m_models.add(var3);
         VehicleScript var4 = var1.m_script;
         float var5 = var4.getModelScale();
         float var6 = 1.0F;
         ModelScript var7 = var2.modelScript;
         if (var7 != null && var7.scale != 1.0F) {
            var6 = var7.scale;
         }

         float var8 = 1.0F;
         if (var7 != null) {
            var8 = var7.invertX ? -1.0F : 1.0F;
         }

         var8 *= -1.0F;
         Quaternionf var9 = UI3DScene.allocQuaternionf();
         Matrix4f var10 = var3.xfrm;
         Vector3f var11 = var4.getModel().getRotate();
         var9.rotationXYZ(var11.x * 0.017453292F, var11.y * 0.017453292F, var11.z * 0.017453292F);
         Vector3f var12 = var4.getModel().getOffset();
         var10.translationRotateScale(var12.x * 1.0F, var12.y, var12.z, var9.x, var9.y, var9.z, var9.w, var5 * var6 * var8, var5 * var6, var5 * var6);
         ModelInstanceRenderData.postMultiplyMeshTransform(var10, var3.model.Mesh);
         this.m_transform.mul(var10, var10);
         UI3DScene.releaseQuaternionf(var9);
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
               VehicleModelRenderData var11 = (VehicleModelRenderData)UI3DScene.VehicleModelRenderData.s_pool.alloc();
               var11.model = var10;
               var11.tex = var10.tex;
               var11.m_boneCoords.clear();
               this.m_models.add(var11);
               float var12 = var7.scale;
               float var13 = 1.0F;
               float var14 = 1.0F;
               ModelScript var15 = ScriptManager.instance.getModelScript(var7.file);
               if (var15 != null) {
                  var13 = var15.scale;
                  var14 = var15.invertX ? -1.0F : 1.0F;
               }

               Quaternionf var16 = UI3DScene.allocQuaternionf();
               var16.rotationXYZ(var9.x * 0.017453292F, var9.y * 0.017453292F, var9.z * 0.017453292F);
               Matrix4f var17 = var11.xfrm;
               var17.translation(var6.offset.x / var5 * 1.0F, var6.offset.y / var5, var6.offset.z / var5);
               Matrix4f var18 = UI3DScene.allocMatrix4f();
               var18.translationRotateScale(var8.x * 1.0F, var8.y, var8.z, var16.x, var16.y, var16.z, var16.w, var12 * var13 * var14, var12 * var13, var12 * var13);
               var17.mul(var18);
               UI3DScene.releaseMatrix4f(var18);
               var3.mul(var17, var17);
               ModelInstanceRenderData.postMultiplyMeshTransform(var17, var10.Mesh);
               UI3DScene.releaseQuaternionf(var16);
            }
         }
      }

      private void initPartModels(SceneVehicle var1, VehicleScript.Part var2, Matrix4f var3) {
         for(int var4 = 0; var4 < var2.getModelCount(); ++var4) {
            VehicleScript.Model var5 = var2.getModel(var4);
            if (var2.parent != null && var5.attachmentNameParent != null) {
               VehicleModelRenderData var6 = (VehicleModelRenderData)this.m_partToRenderData.get(var2.parent);
               if (var6 != null) {
                  this.initChildPartModel(var1, var6, var2, var5);
               }
            } else {
               this.initPartModel(var1, var2, var5, var3);
            }
         }

      }

      private void initPartModel(SceneVehicle var1, VehicleScript.Part var2, VehicleScript.Model var3, Matrix4f var4) {
         SceneVehicleModelInfo var5 = var1.getModelInfoForPart(var2.getId());
         if (var5 != null) {
            Vector3f var6 = var3.getOffset();
            Vector3f var7 = var3.getRotate();
            Model var8 = var5.model;
            if (var8 != null) {
               VehicleModelRenderData var9 = (VehicleModelRenderData)UI3DScene.VehicleModelRenderData.s_pool.alloc();
               var9.model = var8;
               var9.tex = var8.tex == null ? ((SceneVehicleModelInfo)var1.m_modelInfo.get(0)).tex : var8.tex;
               var9.m_boneCoords.clear();
               AnimationPlayer var10 = var8.bStatic ? null : var5.getAnimationPlayer();
               if (var10 != null) {
                  var5.updateAnimationPlayer();
                  SkinningData var11 = (SkinningData)var8.Tag;
                  if (Core.bDebug && var11 == null) {
                     DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
                  }

                  org.lwjgl.util.vector.Matrix4f[] var12 = var10.getSkinTransforms(var11);
                  if (var9.matrixPalette == null || var9.matrixPalette.capacity() < var12.length * 16) {
                     var9.matrixPalette = BufferUtils.createFloatBuffer(var12.length * 16);
                  }

                  var9.matrixPalette.clear();

                  for(int var13 = 0; var13 < var12.length; ++var13) {
                     var12[var13].store(var9.matrixPalette);
                  }

                  var9.matrixPalette.flip();
                  if (var2.getId().equalsIgnoreCase(var1.m_showBones_partId) && var3.getId().equalsIgnoreCase(var1.m_showBones_modelId)) {
                     var9.initSkeleton(var5);
                  }

                  this.m_partToRenderData.put(var2.getId(), var9);
               }

               this.m_models.add(var9);
               float var17 = var3.scale;
               float var18 = 1.0F;
               float var19 = 1.0F;
               ModelScript var14 = ScriptManager.instance.getModelScript(var3.file);
               if (var14 != null) {
                  var18 = var14.scale;
                  var19 = var14.invertX ? -1.0F : 1.0F;
               }

               var19 *= -1.0F;
               Quaternionf var15 = UI3DScene.allocQuaternionf();
               var15.rotationXYZ(var7.x * 0.017453292F, var7.y * 0.017453292F, var7.z * 0.017453292F);
               Matrix4f var16 = var9.xfrm;
               var16.translationRotateScale(var6.x * 1.0F, var6.y, var6.z, var15.x, var15.y, var15.z, var15.w, var17 * var18 * var19, var17 * var18, var17 * var18);
               var4.mul(var16, var16);
               ModelInstanceRenderData.postMultiplyMeshTransform(var16, var8.Mesh);
               UI3DScene.releaseQuaternionf(var15);
            }
         }
      }

      void initChildPartModel(SceneVehicle var1, VehicleModelRenderData var2, VehicleScript.Part var3, VehicleScript.Model var4) {
         SceneVehicleModelInfo var5 = var1.getModelInfoForPart(var3.getId());
         if (var5 != null) {
            Model var6 = var5.model;
            if (var6 != null) {
               VehicleModelRenderData var7 = (VehicleModelRenderData)UI3DScene.VehicleModelRenderData.s_pool.alloc();
               var7.model = var6;
               var7.tex = var6.tex == null ? ((SceneVehicleModelInfo)var1.m_modelInfo.get(0)).tex : var6.tex;
               var7.m_boneCoords.clear();
               SceneVehicleModelInfo var8 = var1.getModelInfoForPart(var3.parent);
               Matrix4f var9 = UI3DScene.allocMatrix4f();
               this.initTransform(var1, var8.getAnimationPlayer(), var8.modelScript, var5.modelScript, var4.attachmentNameParent, var4.attachmentNameSelf, var9);
               var2.xfrm.mul(var9, var7.xfrm);
               float var10 = var4.scale;
               float var11 = var5.modelScript.scale;
               boolean var12 = var4.bIgnoreVehicleScale;
               float var13 = var12 ? 1.5F / var1.m_script.getModelScale() : 1.0F;
               var7.xfrm.scale(var10 * var11 * var13);
               ModelInstanceRenderData.postMultiplyMeshTransform(var7.xfrm, var6.Mesh);
               UI3DScene.releaseMatrix4f(var9);
               this.m_models.add(var7);
            }
         }
      }

      void initTransform(SceneVehicle var1, AnimationPlayer var2, ModelScript var3, ModelScript var4, String var5, String var6, Matrix4f var7) {
         var7.identity();
         Matrix4f var8 = UI3DScene.allocMatrix4f();
         ModelAttachment var9 = var3.getAttachmentById(var5);
         if (var9 == null) {
            var9 = var1.m_script.getAttachmentById(var5);
         }

         if (var9 != null) {
            ModelInstanceRenderData.makeBoneTransform(var2, var9.getBone(), var7);
            var7.scale(1.0F / var3.scale);
            ModelInstanceRenderData.makeAttachmentTransform(var9, var8);
            var7.mul(var8);
         }

         ModelAttachment var10 = var4.getAttachmentById(var6);
         if (var10 != null) {
            ModelInstanceRenderData.makeAttachmentTransform(var10, var8);
            if (ModelInstanceRenderData.INVERT_ATTACHMENT_SELF_TRANSFORM) {
               var8.invert();
            }

            var7.mul(var8);
         }

         UI3DScene.releaseMatrix4f(var8);
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

         if (var1.m_ignoreVehicleScale && var1.m_parentVehiclePart != null && var1.m_parentVehiclePart.m_vehicle.m_script != null) {
            this.m_transform.scale(1.5F / var1.m_parentVehiclePart.m_vehicle.m_script.getModelScale());
         }

         ModelInstanceRenderData.postMultiplyMeshTransform(this.m_transform, var1.m_model.Mesh);
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

   private static final class ScenePolygonDrawer extends TextureDraw.GenericDrawer {
      ScenePolygonRenderData m_renderData;

      private ScenePolygonDrawer() {
      }

      public void init(ScenePolygonRenderData var1) {
         this.m_renderData = var1;
      }

      public void render() {
         UI3DScene var1 = this.m_renderData.m_polygon.m_scene;
         StateData var2 = var1.stateDataRender();
         GL11.glViewport(var1.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - var1.getAbsoluteY().intValue() - var1.getHeight().intValue(), var1.getWidth().intValue(), var1.getHeight().intValue());
         PZGLUtil.pushAndLoadMatrix(5889, var2.m_projection);
         Matrix4f var3 = UI3DScene.allocMatrix4f();
         var3.set(var2.m_modelView);
         var3.mul(this.m_renderData.m_transform);
         PZGLUtil.pushAndLoadMatrix(5888, var3);
         UI3DScene.releaseMatrix4f(var3);
         GL11.glDepthMask(false);
         GL11.glDepthFunc(513);
         ScenePolygon var4 = this.m_renderData.m_polygon;
         Vector3f var5 = var4.m_extents;
         GL11.glPolygonMode(1032, 6914);
         UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.FORMAT_PositionColor);
         UI3DScene.vboRenderer.setLineWidth(2.0F);
         UI3DScene.vboRenderer.setMode(4);
         UI3DScene.vboRenderer.setDepthTest(true);
         GL20.glUseProgram(0);
         ShaderHelper.forgetCurrentlyBound();
         boolean var6 = var1.m_bDrawGeometry;
         GL11.glColorMask(var6, var6, var6, var6);
         float var10 = 0.25F;
         ArrayList var11 = this.m_renderData.m_triangles;
         int var12;
         Vector3f var13;
         Vector3f var14;
         if (!var11.isEmpty()) {
            float var7 = 0.0F;
            float var8 = 1.0F;
            float var9 = 0.0F;

            for(var12 = 0; var12 < var11.size(); var12 += 3) {
               var13 = (Vector3f)var11.get(var12);
               var14 = (Vector3f)var11.get(var12 + 1);
               Vector3f var15 = (Vector3f)var11.get(var12 + 2);
               UI3DScene.vboRenderer.addTriangle(var13.x, var13.y, var13.z, var14.x, var14.y, var14.z, var15.x, var15.y, var15.z, var7, var8, var9, var10);
            }
         }

         UI3DScene.vboRenderer.endRun();
         UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.FORMAT_PositionColor);
         UI3DScene.vboRenderer.setMode(1);
         UI3DScene.vboRenderer.setDepthTest(false);
         GL11.glDepthFunc(519);
         GL11.glColorMask(true, true, true, true);
         GL11.glPolygonMode(1032, 6914);

         for(var12 = 0; var12 < this.m_renderData.m_points.size(); ++var12) {
            var13 = (Vector3f)this.m_renderData.m_points.get(var12);
            var14 = (Vector3f)this.m_renderData.m_points.get((var12 + 1) % this.m_renderData.m_points.size());
            UI3DScene.vboRenderer.addLine(var13.x, var13.y, var13.z, var14.x, var14.y, var14.z, 1.0F, 1.0F, 1.0F, var4.m_editing ? 1.0F : 0.5F);
         }

         UI3DScene.vboRenderer.endRun();
         UI3DScene.vboRenderer.flush();
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
         ShaderHelper.glUseProgramObjectARB(0);
         GLStateRenderThread.restore();
      }

      public void postRender() {
      }
   }

   private static class ScenePolygonRenderData extends SceneObjectRenderData {
      final ScenePolygonDrawer m_drawer = new ScenePolygonDrawer();
      ScenePolygon m_polygon;
      final ArrayList<Vector3f> m_points = new ArrayList();
      final ArrayList<Vector3f> m_triangles = new ArrayList();
      private static final ObjectPool<ScenePolygonRenderData> s_pool = new ObjectPool(ScenePolygonRenderData::new);

      private ScenePolygonRenderData() {
      }

      SceneObjectRenderData initPolygon(ScenePolygon var1) {
         super.init(var1);
         PolygonEditor var2 = var1.m_scene.m_polygonEditor;
         var2.setPlane(var1.m_translate, var1.m_rotate, var1.m_plane);
         this.m_points.clear();

         int var3;
         for(var3 = 0; var3 < var1.m_points.size(); ++var3) {
            Vector2f var4 = (Vector2f)var1.m_points.get(var3);
            this.m_points.add(UI3DScene.allocVector3f().set(var4.x, var4.y, 0.0F));
         }

         this.m_triangles.clear();

         for(var3 = 0; var3 < var1.m_triangles.size(); var3 += 2) {
            float var6 = var1.m_triangles.get(var3);
            float var5 = var1.m_triangles.get(var3 + 1);
            this.m_triangles.add(UI3DScene.allocVector3f().set(var6, var5, 0.0F));
         }

         this.m_drawer.init(this);
         this.m_polygon = var1;
         return this;
      }

      void release() {
         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).releaseAll(this.m_points);
         this.m_points.clear();
         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).releaseAll(this.m_triangles);
         this.m_triangles.clear();
         s_pool.release((Object)this);
      }
   }

   private static final class SceneCylinderDrawer extends TextureDraw.GenericDrawer {
      SceneCylinder m_sceneObject;
      float m_radiusBase;
      float m_radiusTop;
      float m_length;
      int m_slices = 32;
      int m_stacks = 2;
      private static final ObjectPool<SceneCylinderDrawer> s_pool = new ObjectPool(SceneCylinderDrawer::new);

      private SceneCylinderDrawer() {
      }

      public void render() {
         UI3DScene var1 = this.m_sceneObject.m_scene;
         StateData var2 = var1.stateDataRender();
         PZGLUtil.pushAndLoadMatrix(5889, var2.m_projection);
         PZGLUtil.pushAndLoadMatrix(5888, var2.m_modelView);
         boolean var3 = false;
         GL11.glPolygonMode(1032, var3 ? 6913 : 6914);
         GL20.glUseProgram(0);
         ShaderHelper.forgetCurrentlyBound();
         GL11.glDisable(2929);

         for(int var4 = 7; var4 >= 0; --var4) {
            GL13.glActiveTexture('蓀' + var4);
            GL11.glDisable(3553);
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         boolean var14 = false;
         if (var14) {
            GL11.glEnable(2896);
            GL11.glEnable(16384);
            float[] var5 = new float[]{1.0F, 1.5F, 1.0F, 0.0F};
            GL11.glLightfv(16384, 4611, var5);
            GL11.glLightf(16384, 4615, 1.0F);
            GL11.glLightf(16384, 4616, 0.0F);
            GL11.glLightf(16384, 4617, 0.0F);
         }

         VBORenderer var15 = VBORenderer.getInstance();
         Matrix4f var6 = UI3DScene.allocMatrix4f();
         this.m_sceneObject.getGlobalTransform(var6);
         PZGLUtil.pushAndMultMatrix(5888, var6);
         GL11.glEnable(2929);
         GL11.glDepthFunc(513);
         Core.getInstance().modelViewMatrixStack.peek().translate(0.0F, 0.0F, -this.m_length / 2.0F);
         float var7 = 1.0F;
         float var8 = 1.0F;
         float var9 = this.m_sceneObject.m_bSelected ? 0.0F : 1.0F;
         var15.addCylinder_Line(this.m_radiusBase, this.m_radiusTop, this.m_length, this.m_slices, this.m_stacks, var7, var8, var9, 0.75F);
         var15.flush();
         Core.getInstance().modelViewMatrixStack.peek().translate(0.0F, 0.0F, this.m_length / 2.0F);
         GL11.glDisable(2929);
         GL11.glDepthFunc(519);
         if (var14) {
            GL11.glDisable(2896);
            GL11.glDisable(16384);
         }

         GL11.glPolygonMode(1032, 6914);
         float var10 = (float)(Mouse.getXA() - var1.getAbsoluteX().intValue());
         float var11 = (float)(Mouse.getYA() - var1.getAbsoluteY().intValue());
         Ray var12 = var1.getCameraRay(var10, (float)var1.screenHeight() - var11, var2.m_projection, var2.m_modelView, UI3DScene.allocRay());
         var6.invert();
         var6.transformPosition(var12.origin);
         var6.transformDirection(var12.direction);
         UI3DScene.releaseMatrix4f(var6);
         CylinderUtils.IntersectionRecord var13 = new CylinderUtils.IntersectionRecord();
         if (this.m_sceneObject.intersect(var12, var13)) {
            var15.startRun(var15.FORMAT_PositionColor);
            var15.setMode(1);
            var15.addLine(var13.location.x, var13.location.y, var13.location.z, var13.location.x + var13.normal.x, var13.location.y + var13.normal.y, var13.location.z + var13.normal.z, 1.0F, 1.0F, 1.0F, 1.0F);
            UI3DScene.vboRenderer.endRun();
            var15.flush();
            Model.debugDrawAxis(var13.location.x, var13.location.y, var13.location.z, 0.1F, 1.0F);
         }

         UI3DScene.releaseRay(var12);
         PZGLUtil.popMatrix(5888);
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
         ShaderHelper.glUseProgramObjectARB(0);
         GLStateRenderThread.restore();
      }

      public void postRender() {
         s_pool.release((Object)this);
      }
   }

   private static final class SceneVehicleModelInfo {
      SceneVehicle sceneVehicle;
      VehicleScript.Part part;
      VehicleScript.Model scriptModel;
      ModelScript modelScript;
      int wheelIndex;
      Model model;
      Texture tex;
      AnimationPlayer m_animPlayer;
      AnimationTrack m_track;
      private static final ObjectPool<SceneVehicleModelInfo> s_pool = new ObjectPool(SceneVehicleModelInfo::new);

      private SceneVehicleModelInfo() {
      }

      public AnimationPlayer getAnimationPlayer() {
         if (this.part != null && this.part.parent != null) {
            SceneVehicleModelInfo var1 = this.sceneVehicle.getModelInfoForPart(this.part.parent);
            if (var1 != null) {
               return var1.getAnimationPlayer();
            }
         }

         String var3 = this.scriptModel.file;
         Model var2 = ModelManager.instance.getLoadedModel(var3);
         if (var2 != null && !var2.bStatic) {
            if (this.m_animPlayer != null && this.m_animPlayer.getModel() != var2) {
               this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
            }

            if (this.m_animPlayer == null) {
               this.m_animPlayer = AnimationPlayer.alloc(var2);
            }

            return this.m_animPlayer;
         } else {
            return null;
         }
      }

      public void releaseAnimationPlayer() {
         this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
      }

      public void playPartAnim(String var1) {
         VehicleScript.Anim var2 = this.part.getAnimById(var1);
         if (var2 != null && !StringUtils.isNullOrWhitespace(var2.anim)) {
            AnimationPlayer var3 = this.getAnimationPlayer();
            if (var3 != null && var3.isReady()) {
               if (var3.getMultiTrack().getIndexOfTrack(this.m_track) != -1) {
                  var3.getMultiTrack().removeTrack(this.m_track);
               }

               this.m_track = null;
               SkinningData var4 = var3.getSkinningData();
               if (var4 == null || var4.AnimationClips.containsKey(var2.anim)) {
                  AnimationTrack var5 = var3.play(var2.anim, var2.bLoop);
                  this.m_track = var5;
                  if (var5 != null) {
                     var5.setLayerIdx(0);
                     var5.BlendDelta = 1.0F;
                     var5.SpeedDelta = var2.rate;
                     var5.IsPlaying = var2.bAnimate;
                     var5.reverse = var2.bReverse;
                     if (!this.modelScript.boneWeights.isEmpty()) {
                        var5.setBoneWeights(this.modelScript.boneWeights);
                        var5.initBoneWeights(var4);
                     }

                     if (this.part.window != null) {
                        float var6 = 0.0F;
                        var5.setCurrentTimeValue(var5.getDuration() * var6);
                     }
                  }

               }
            }
         }
      }

      protected void updateAnimationPlayer() {
         AnimationPlayer var1 = this.getAnimationPlayer();
         if (var1 != null && var1.isReady()) {
            AnimationMultiTrack var2 = var1.getMultiTrack();
            float var3 = 0.016666668F;
            var3 *= 0.8F;
            var3 *= GameTime.instance.getUnmoddedMultiplier();
            var1.Update(var3);

            AnimationTrack var5;
            for(int var4 = 0; var4 < var2.getTrackCount(); ++var4) {
               var5 = (AnimationTrack)var2.getTracks().get(var4);
               if (var5.IsPlaying && var5.isFinished()) {
                  var2.removeTrackAt(var4);
                  --var4;
               }
            }

            if (this.part != null) {
               if (this.m_track != null && var2.getIndexOfTrack(this.m_track) == -1) {
                  this.m_track = null;
               }

               if (this.m_track != null) {
                  if (this.part.window != null) {
                     var5 = this.m_track;
                     float var6 = 0.0F;
                     var5.setCurrentTimeValue(var5.getDuration() * var6);
                  }

               } else {
                  if (this.part.door != null) {
                     boolean var7 = false;
                     this.playPartAnim(var7 ? "Opened" : "Closed");
                  }

                  if (this.part.window != null) {
                     this.playPartAnim("ClosedToOpen");
                  }

               }
            }
         }
      }

      void release() {
         s_pool.release((Object)this);
      }
   }
}
