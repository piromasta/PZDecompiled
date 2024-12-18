package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import org.joml.AxisAngle4f;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.IsoRoom;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;

public class ModelInstance {
   public static float MODEL_LIGHT_MULT_OUTSIDE = 1.7F;
   public static float MODEL_LIGHT_MULT_ROOM = 1.7F;
   public Model model;
   public AnimationPlayer AnimPlayer;
   public SkinningData data;
   public Texture tex;
   public ModelInstanceTextureInitializer m_textureInitializer;
   public IsoGameCharacter character;
   public IsoMovingObject object;
   public float tintR = 1.0F;
   public float tintG = 1.0F;
   public float tintB = 1.0F;
   public ModelInstance parent;
   public int parentBone;
   public String parentBoneName = null;
   public float hue;
   public float depthBias;
   public ModelInstance matrixModel;
   public SoftwareModelMeshInstance softwareMesh;
   public final ArrayList<ModelInstance> sub = new ArrayList();
   public float targetDepth;
   private int instanceSkip;
   private ItemVisual itemVisual = null;
   public boolean bResetAfterRender = false;
   private Object m_owner = null;
   public int renderRefCount;
   private static final int INITIAL_SKIP_VALUE = 2147483647;
   private int skipped = 2147483647;
   public final Object m_lock = "ModelInstance Thread Lock";
   public ModelScript m_modelScript = null;
   public String attachmentNameSelf = null;
   public String attachmentNameParent = null;
   public float scale = 1.0F;
   public String maskVariableValue = null;
   private static final Matrix4f gcAttachmentMatrix = new Matrix4f();
   private static final Matrix4f gcTransposedAttachmentMatrix = new Matrix4f();
   private static final org.lwjgl.util.vector.Matrix4f gcBoneModelTransform = new org.lwjgl.util.vector.Matrix4f();
   private static final Vector3f gcVector3f = new Vector3f();
   private static final AxisAngle4f gcAxisAngle4f = new AxisAngle4f();
   private static final Vector3 modelSpaceForward = new Vector3(0.0F, 0.0F, 1.0F);
   private static final Vector3 gcRotatedForward = new Vector3();
   public PlayerData[] playerData;
   private static final ColorInfo tempColorInfo = new ColorInfo();
   private static final ColorInfo tempColorInfo2 = new ColorInfo();

   public ModelInstance() {
   }

   public ModelInstance init(Model var1, IsoGameCharacter var2, AnimationPlayer var3) {
      this.data = (SkinningData)var1.Tag;
      this.model = var1;
      this.tex = var1.tex;
      if (!var1.bStatic && var3 == null) {
         var3 = AnimationPlayer.alloc(var1);
      }

      this.AnimPlayer = var3;
      this.character = var2;
      this.object = var2;
      return this;
   }

   public boolean isRendering() {
      return this.renderRefCount > 0;
   }

   public void reset() {
      if (this.tex instanceof SmartTexture) {
         Texture var1 = this.tex;
         Objects.requireNonNull(var1);
         RenderThread.queueInvokeOnRenderContext(var1::destroy);
      }

      this.AnimPlayer = null;
      this.character = null;
      this.data = null;
      this.hue = 0.0F;
      this.itemVisual = null;
      this.matrixModel = null;
      this.model = null;
      this.object = null;
      this.parent = null;
      this.parentBone = 0;
      this.parentBoneName = null;
      this.skipped = 2147483647;
      this.sub.clear();
      this.softwareMesh = null;
      this.tex = null;
      if (this.m_textureInitializer != null) {
         this.m_textureInitializer.release();
         this.m_textureInitializer = null;
      }

      this.tintR = 1.0F;
      this.tintG = 1.0F;
      this.tintB = 1.0F;
      this.bResetAfterRender = false;
      this.renderRefCount = 0;
      this.scale = 1.0F;
      this.m_owner = null;
      this.m_modelScript = null;
      this.attachmentNameSelf = null;
      this.attachmentNameParent = null;
      this.maskVariableValue = null;
      if (this.playerData != null) {
         ModelInstance.PlayerData.pool.release((Object[])this.playerData);
         Arrays.fill(this.playerData, (Object)null);
      }

   }

   public void LoadTexture(String var1) {
      if (var1 != null && var1.length() != 0) {
         this.tex = Texture.getSharedTexture("media/textures/" + var1 + ".png");
         if (this.tex == null) {
            if (var1.equals("Vest_White")) {
               this.tex = Texture.getSharedTexture("media/textures/Shirt_White.png");
            } else if (var1.contains("Hair")) {
               this.tex = Texture.getSharedTexture("media/textures/F_Hair_White.png");
            } else if (var1.contains("Beard")) {
               this.tex = Texture.getSharedTexture("media/textures/F_Hair_White.png");
            } else {
               DebugLog.log("ERROR: model texture \"" + var1 + "\" wasn't found");
            }
         }

      } else {
         this.tex = null;
      }
   }

   public void dismember(int var1) {
      this.AnimPlayer.dismember(var1);
   }

   public void UpdateDir() {
      if (this.AnimPlayer != null) {
         this.AnimPlayer.updateForwardDirection(this.character);
      }
   }

   public void Update(float var1) {
      float var2;
      if (this.character != null) {
         var2 = this.character.DistTo(IsoPlayer.getInstance());
         if (!this.character.amputations.isEmpty() && var2 > 0.0F && this.AnimPlayer != null) {
            this.AnimPlayer.dismembered.clear();
            ArrayList var3 = this.character.amputations;

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               String var5 = (String)var3.get(var4);
               this.AnimPlayer.dismember((Integer)this.AnimPlayer.getSkinningData().BoneIndices.get(var5));
            }
         }
      }

      this.instanceSkip = 0;
      if (this.AnimPlayer != null) {
         if (this.matrixModel == null) {
            if (this.skipped >= this.instanceSkip) {
               if (this.skipped == 2147483647) {
                  this.skipped = 1;
               }

               var2 = var1 * (float)this.skipped;
               this.AnimPlayer.Update(var2);
            } else {
               this.AnimPlayer.DoAngles(var1);
            }

            this.AnimPlayer.parentPlayer = null;
         } else {
            this.AnimPlayer.parentPlayer = this.matrixModel.AnimPlayer;
         }
      }

      if (this.skipped >= this.instanceSkip) {
         this.skipped = 0;
      }

      ++this.skipped;
   }

   public void SetForceDir(Vector2 var1) {
      if (this.AnimPlayer != null) {
         this.AnimPlayer.setTargetAndCurrentDirection(var1);
      }

   }

   public void setInstanceSkip(int var1) {
      this.instanceSkip = var1;

      for(int var2 = 0; var2 < this.sub.size(); ++var2) {
         ModelInstance var3 = (ModelInstance)this.sub.get(var2);
         var3.instanceSkip = var1;
      }

   }

   public void destroySmartTextures() {
      if (this.tex instanceof SmartTexture) {
         this.tex.destroy();
         this.tex = null;
      }

      for(int var1 = 0; var1 < this.sub.size(); ++var1) {
         ModelInstance var2 = (ModelInstance)this.sub.get(var1);
         var2.destroySmartTextures();
      }

   }

   public void updateLights() {
      int var1 = IsoCamera.frameState.playerIndex;
      if (this.playerData == null) {
         this.playerData = new PlayerData[4];
      }

      boolean var2 = this.playerData[var1] == null;
      if (this.playerData[var1] == null) {
         this.playerData[var1] = (PlayerData)ModelInstance.PlayerData.pool.alloc();
      }

      this.playerData[var1].updateLights(this.character, var2);
   }

   public ItemVisual getItemVisual() {
      return this.itemVisual;
   }

   public void setItemVisual(ItemVisual var1) {
      this.itemVisual = var1;
   }

   public void applyModelScriptScale(String var1) {
      this.m_modelScript = ScriptManager.instance.getModelScript(var1);
      if (this.m_modelScript != null) {
         this.scale = this.m_modelScript.scale;
      }

   }

   public ModelAttachment getAttachment(int var1) {
      return this.m_modelScript == null ? null : this.m_modelScript.getAttachment(var1);
   }

   public ModelAttachment getAttachmentById(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         return null;
      } else {
         return this.m_modelScript == null ? null : this.m_modelScript.getAttachmentById(var1);
      }
   }

   public Matrix4f getAttachmentMatrix(ModelAttachment var1, Matrix4f var2) {
      var2.translation(var1.getOffset());
      Vector3f var3 = var1.getRotate();
      var2.rotateXYZ(var3.x * 0.017453292F, var3.y * 0.017453292F, var3.z * 0.017453292F);
      return var2;
   }

   public Matrix4f getAttachmentMatrix(int var1, Matrix4f var2) {
      ModelAttachment var3 = this.getAttachment(var1);
      return var3 == null ? var2.identity() : this.getAttachmentMatrix(var3, var2);
   }

   public Matrix4f getAttachmentMatrixById(String var1, Matrix4f var2) {
      ModelAttachment var3 = this.getAttachmentById(var1);
      return var3 == null ? var2.identity() : this.getAttachmentMatrix(var3, var2);
   }

   public void setOwner(Object var1) {
      Objects.requireNonNull(var1);

      assert this.m_owner == null;

      this.m_owner = var1;
   }

   public void clearOwner(Object var1) {
      Objects.requireNonNull(var1);

      assert this.m_owner == var1;

      this.m_owner = null;
   }

   public Object getOwner() {
      return this.m_owner;
   }

   public void setTextureInitializer(ModelInstanceTextureInitializer var1) {
      this.m_textureInitializer = var1;
   }

   public ModelInstanceTextureInitializer getTextureInitializer() {
      return this.m_textureInitializer;
   }

   public boolean hasTextureCreator() {
      return this.m_textureInitializer != null && this.m_textureInitializer.isDirty();
   }

   public void getAttachmentWorldPosition(ModelAttachment var1, Vector3 var2, Vector3 var3) {
      float var4 = this.AnimPlayer.getRenderedAngle();
      this.AnimPlayer.getBoneModelTransform(this.parentBone, gcBoneModelTransform);
      this.getAttachmentMatrix(var1, gcAttachmentMatrix);
      PZMath.convertMatrix(gcBoneModelTransform, gcTransposedAttachmentMatrix);
      gcTransposedAttachmentMatrix.transpose();
      gcTransposedAttachmentMatrix.mul(gcAttachmentMatrix, gcAttachmentMatrix);
      gcAttachmentMatrix.getTranslation(gcVector3f);
      var2.x = gcVector3f.x;
      var2.y = gcVector3f.y;
      var2.z = gcVector3f.z;
      Model.VectorToWorldCoords(this.character.getX(), this.character.getY(), this.character.getZ(), var4, var2);
      gcAttachmentMatrix.getRotation(gcAxisAngle4f);
      this.rotateVectorByAxisAngle();
      var3.x = gcRotatedForward.x;
      var3.y = gcRotatedForward.y;
      var3.z = gcRotatedForward.z;
      Model.VectorToWorldCoords(0.0F, 0.0F, 0.0F, var4, var3);
   }

   private void rotateVectorByAxisAngle() {
      float var1 = gcAxisAngle4f.x;
      float var2 = gcAxisAngle4f.y;
      float var3 = gcAxisAngle4f.z;
      float var4 = gcAxisAngle4f.angle;
      float var5 = Math.cos(var4);
      float var6 = Math.sin(var4);
      float var7 = modelSpaceForward.x * (var5 + var1 * var1 * (1.0F - var5)) + modelSpaceForward.y * (var1 * var2 * (1.0F - var5) - var3 * var6) + modelSpaceForward.z * (var1 * var3 * (1.0F - var5) + var2 * var6);
      float var8 = modelSpaceForward.x * (var2 * var1 * (1.0F - var5) + var3 * var6) + modelSpaceForward.y * (var5 + var2 * var2 * (1.0F - var5)) + modelSpaceForward.z * (var2 * var3 * (1.0F - var5) - var1 * var6);
      float var9 = modelSpaceForward.x * (var3 * var1 * (1.0F - var5) - var2 * var6) + modelSpaceForward.y * (var3 * var2 * (1.0F - var5) + var1 * var6) + modelSpaceForward.z * (var5 + var3 * var3 * (1.0F - var5));
      gcRotatedForward.set(var7, var8, var9);
   }

   public static final class PlayerData {
      private FrameLightInfo[] frameLights;
      private ArrayList<IsoGridSquare.ResultLight> chosenLights;
      private Vector3f targetAmbient;
      public Vector3f currentAmbient;
      public EffectLight[] effectLightsMain;
      private static final ObjectPool<PlayerData> pool = new ObjectPool(PlayerData::new);

      public PlayerData() {
      }

      private void registerFrameLight(IsoGridSquare.ResultLight var1) {
         this.chosenLights.add(var1);
      }

      private void initFrameLightsForFrame() {
         if (this.frameLights == null) {
            this.effectLightsMain = new EffectLight[5];

            for(int var1 = 0; var1 < 5; ++var1) {
               this.effectLightsMain[var1] = new EffectLight();
            }

            this.frameLights = new FrameLightInfo[5];
            this.chosenLights = new ArrayList();
            this.targetAmbient = new Vector3f();
            this.currentAmbient = new Vector3f();
         }

         EffectLight[] var5 = this.effectLightsMain;
         int var2 = var5.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            EffectLight var4 = var5[var3];
            var4.radius = -1;
         }

         this.chosenLights.clear();
      }

      private void completeFrameLightsForFrame() {
         int var1;
         for(var1 = 0; var1 < 5; ++var1) {
            if (this.frameLights[var1] != null) {
               this.frameLights[var1].foundThisFrame = false;
            }
         }

         for(var1 = 0; var1 < this.chosenLights.size(); ++var1) {
            IsoGridSquare.ResultLight var2 = (IsoGridSquare.ResultLight)this.chosenLights.get(var1);
            boolean var3 = false;
            int var4 = 0;
            int var5 = 0;

            label80: {
               while(true) {
                  if (var5 >= 5) {
                     break label80;
                  }

                  if (this.frameLights[var5] != null && this.frameLights[var5].active) {
                     if (var2.id != -1) {
                        if (var2.id == this.frameLights[var5].id) {
                           break;
                        }
                     } else if (this.frameLights[var5].x == var2.x && this.frameLights[var5].y == var2.y && this.frameLights[var5].z == var2.z) {
                        break;
                     }
                  }

                  ++var5;
               }

               var3 = true;
               var4 = var5;
            }

            if (var3) {
               this.frameLights[var4].foundThisFrame = true;
               this.frameLights[var4].x = var2.x;
               this.frameLights[var4].y = var2.y;
               this.frameLights[var4].z = var2.z;
               this.frameLights[var4].flags = var2.flags;
               this.frameLights[var4].radius = var2.radius;
               this.frameLights[var4].targetColor.x = var2.r;
               this.frameLights[var4].targetColor.y = var2.g;
               this.frameLights[var4].targetColor.z = var2.b;
               this.frameLights[var4].Stage = ModelInstance.FrameLightBlendStatus.In;
            } else {
               for(var5 = 0; var5 < 5; ++var5) {
                  if (this.frameLights[var5] == null || !this.frameLights[var5].active) {
                     if (this.frameLights[var5] == null) {
                        this.frameLights[var5] = new FrameLightInfo();
                     }

                     this.frameLights[var5].x = var2.x;
                     this.frameLights[var5].y = var2.y;
                     this.frameLights[var5].z = var2.z;
                     this.frameLights[var5].r = var2.r;
                     this.frameLights[var5].g = var2.g;
                     this.frameLights[var5].b = var2.b;
                     this.frameLights[var5].flags = var2.flags;
                     this.frameLights[var5].radius = var2.radius;
                     this.frameLights[var5].id = var2.id;
                     this.frameLights[var5].currentColor.x = 0.0F;
                     this.frameLights[var5].currentColor.y = 0.0F;
                     this.frameLights[var5].currentColor.z = 0.0F;
                     this.frameLights[var5].targetColor.x = var2.r;
                     this.frameLights[var5].targetColor.y = var2.g;
                     this.frameLights[var5].targetColor.z = var2.b;
                     this.frameLights[var5].Stage = ModelInstance.FrameLightBlendStatus.In;
                     this.frameLights[var5].active = true;
                     this.frameLights[var5].foundThisFrame = true;
                     break;
                  }
               }
            }
         }

         float var6 = GameTime.getInstance().getMultiplier();

         for(int var7 = 0; var7 < 5; ++var7) {
            FrameLightInfo var8 = this.frameLights[var7];
            if (var8 != null && var8.active) {
               if (!var8.foundThisFrame) {
                  var8.targetColor.x = 0.0F;
                  var8.targetColor.y = 0.0F;
                  var8.targetColor.z = 0.0F;
                  var8.Stage = ModelInstance.FrameLightBlendStatus.Out;
               }

               var8.currentColor.x = this.step(var8.currentColor.x, var8.targetColor.x, java.lang.Math.signum(var8.targetColor.x - var8.currentColor.x) / (60.0F * var6));
               var8.currentColor.y = this.step(var8.currentColor.y, var8.targetColor.y, java.lang.Math.signum(var8.targetColor.y - var8.currentColor.y) / (60.0F * var6));
               var8.currentColor.z = this.step(var8.currentColor.z, var8.targetColor.z, java.lang.Math.signum(var8.targetColor.z - var8.currentColor.z) / (60.0F * var6));
               if (var8.Stage == ModelInstance.FrameLightBlendStatus.Out && var8.currentColor.x < 0.01F && var8.currentColor.y < 0.01F && var8.currentColor.z < 0.01F) {
                  var8.active = false;
               }
            }
         }

      }

      private void sortLights(IsoGameCharacter var1) {
         for(int var2 = 0; var2 < this.frameLights.length; ++var2) {
            FrameLightInfo var3 = this.frameLights[var2];
            if (var3 != null) {
               if (!var3.active) {
                  var3.distSq = 3.4028235E38F;
               } else {
                  var3.distSq = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), var1.getZ(), (float)var3.x + 0.5F, (float)var3.y + 0.5F, (float)var3.z);
               }
            }
         }

         Arrays.sort(this.frameLights, (var0, var1x) -> {
            boolean var2 = var0 == null || var0.radius == -1 || !var0.active;
            boolean var3 = var1x == null || var1x.radius == -1 || !var1x.active;
            if (var2 && var3) {
               return 0;
            } else if (var2) {
               return 1;
            } else if (var3) {
               return -1;
            } else if (var0.Stage.ordinal() < var1x.Stage.ordinal()) {
               return -1;
            } else {
               return var0.Stage.ordinal() > var1x.Stage.ordinal() ? 1 : (int)java.lang.Math.signum(var0.distSq - var1x.distSq);
            }
         });
      }

      private void updateLights(IsoGameCharacter var1, boolean var2) {
         this.initFrameLightsForFrame();
         if (var1 != null) {
            if (var1.getCurrentSquare() != null) {
               IsoGridSquare.ILighting var3 = var1.getCurrentSquare().lighting[IsoCamera.frameState.playerIndex];
               int var4 = Math.min(var3.resultLightCount(), 4);

               int var5;
               for(var5 = 0; var5 < var4; ++var5) {
                  IsoGridSquare.ResultLight var6 = var3.getResultLight(var5);
                  this.registerFrameLight(var6);
               }

               if (var2) {
                  for(var5 = 0; var5 < this.frameLights.length; ++var5) {
                     if (this.frameLights[var5] != null) {
                        this.frameLights[var5].active = false;
                     }
                  }
               }

               this.completeFrameLightsForFrame();
               var1.getCurrentSquare().interpolateLight(ModelInstance.tempColorInfo, var1.getX() % 1.0F, var1.getY() % 1.0F);
               this.targetAmbient.x = ModelInstance.tempColorInfo.r;
               this.targetAmbient.y = ModelInstance.tempColorInfo.g;
               this.targetAmbient.z = ModelInstance.tempColorInfo.b;
               if (var1.getZ() - (float)PZMath.fastfloor(var1.getZ()) > 0.2F) {
                  IsoGridSquare var15 = IsoWorld.instance.CurrentCell.getGridSquare(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()) + 1);
                  if (var15 != null) {
                     ColorInfo var16 = ModelInstance.tempColorInfo2;
                     var15.lighting[IsoCamera.frameState.playerIndex].lightInfo();
                     var15.interpolateLight(var16, var1.getX() % 1.0F, var1.getY() % 1.0F);
                     ModelInstance.tempColorInfo.interp(var16, (var1.getZ() - ((float)PZMath.fastfloor(var1.getZ()) + 0.2F)) / 0.8F, ModelInstance.tempColorInfo);
                     this.targetAmbient.set(ModelInstance.tempColorInfo.r, ModelInstance.tempColorInfo.g, ModelInstance.tempColorInfo.b);
                  }
               }

               float var17 = GameTime.getInstance().getMultiplier();
               this.currentAmbient.x = this.step(this.currentAmbient.x, this.targetAmbient.x, (this.targetAmbient.x - this.currentAmbient.x) / (10.0F * var17));
               this.currentAmbient.y = this.step(this.currentAmbient.y, this.targetAmbient.y, (this.targetAmbient.y - this.currentAmbient.y) / (10.0F * var17));
               this.currentAmbient.z = this.step(this.currentAmbient.z, this.targetAmbient.z, (this.targetAmbient.z - this.currentAmbient.z) / (10.0F * var17));
               if (var2) {
                  this.setCurrentToTarget();
               }

               this.sortLights(var1);
               float var18 = 0.7F;

               for(int var7 = 0; var7 < 5; ++var7) {
                  FrameLightInfo var8 = this.frameLights[var7];
                  if (var8 != null && var8.active) {
                     EffectLight var9 = this.effectLightsMain[var7];
                     int var10 = var8.flags;
                     if ((var10 & 1) != 0) {
                        IsoRoom var11 = var1.getCurrentSquare().getRoom();
                        if (var11 == null || var11.findRoomLightByID(var8.id) == null) {
                           var10 &= -2;
                        }
                     }

                     if ((var10 & 1) != 0) {
                        var9.set(var1.getX(), var1.getY(), (float)(PZMath.fastfloor(var1.getZ()) + 1), var8.currentColor.x * var18, var8.currentColor.y * var18, var8.currentColor.z * var18, var8.radius);
                     } else if ((var8.flags & 2) != 0) {
                        if (var1 instanceof IsoPlayer) {
                           int var10000;
                           if (GameClient.bClient) {
                              var10000 = ((IsoPlayer)var1).OnlineID + 1;
                           } else {
                              var10000 = ((IsoPlayer)var1).PlayerIndex + 1;
                           }

                           int var12 = ((IsoPlayer)var1).PlayerIndex;
                           int var13 = var12 * 4 + 1;
                           int var14 = var12 * 4 + 3 + 1;
                           if (var8.id < var13 || var8.id > var14) {
                              var9.set((float)var8.x, (float)var8.y, (float)var8.z, var8.currentColor.x, var8.currentColor.y, var8.currentColor.z, var8.radius);
                           }
                        } else {
                           var9.set((float)var8.x, (float)var8.y, (float)var8.z, var8.currentColor.x * 2.0F, var8.currentColor.y, var8.currentColor.z, var8.radius);
                        }
                     } else {
                        var9.set((float)var8.x + 0.5F, (float)var8.y + 0.5F, (float)var8.z + 0.5F, var8.currentColor.x * var18, var8.currentColor.y * var18, var8.currentColor.z * var18, var8.radius);
                     }
                  }
               }

               if (var4 <= 3 && var1 instanceof IsoPlayer && var1.getTorchStrength() > 0.0F) {
                  this.effectLightsMain[2].set(var1.getX() + var1.getForwardDirection().x * 0.5F, var1.getY() + var1.getForwardDirection().y * 0.5F, var1.getZ() + 0.25F, 1.0F, 1.0F, 1.0F, 2);
               }

               float var19 = 0.0F;
               float var20 = 1.0F;
               float var21 = this.lerp(var19, var20, this.currentAmbient.x);
               float var22 = this.lerp(var19, var20, this.currentAmbient.y);
               float var23 = this.lerp(var19, var20, this.currentAmbient.z);
               if (var1.getCurrentSquare().isOutside()) {
                  var21 *= ModelInstance.MODEL_LIGHT_MULT_OUTSIDE;
                  var22 *= ModelInstance.MODEL_LIGHT_MULT_OUTSIDE;
                  var23 *= ModelInstance.MODEL_LIGHT_MULT_OUTSIDE;
                  this.effectLightsMain[3].set(var1.getX() - 2.0F, var1.getY() - 2.0F, var1.getZ() + 1.0F, var21 / 4.0F, var22 / 4.0F, var23 / 4.0F, 5000);
                  this.effectLightsMain[4].set(var1.getX() + 2.0F, var1.getY() + 2.0F, var1.getZ() + 1.0F, var21 / 4.0F, var22 / 4.0F, var23 / 4.0F, 5000);
               } else if (var1.getCurrentSquare().getRoom() != null) {
                  var21 *= ModelInstance.MODEL_LIGHT_MULT_ROOM;
                  var22 *= ModelInstance.MODEL_LIGHT_MULT_ROOM;
                  var23 *= ModelInstance.MODEL_LIGHT_MULT_ROOM;
                  this.effectLightsMain[3].set(var1.getX() - 2.0F, var1.getY() - 2.0F, var1.getZ() + 1.0F, var21 / 4.0F, var22 / 4.0F, var23 / 4.0F, 5000);
                  this.effectLightsMain[4].set(var1.getX() + 2.0F, var1.getY() + 2.0F, var1.getZ() + 1.0F, var21 / 4.0F, var22 / 4.0F, var23 / 4.0F, 5000);
               }

            }
         }
      }

      private float lerp(float var1, float var2, float var3) {
         return var1 + (var2 - var1) * var3;
      }

      private void setCurrentToTarget() {
         for(int var1 = 0; var1 < this.frameLights.length; ++var1) {
            FrameLightInfo var2 = this.frameLights[var1];
            if (var2 != null) {
               var2.currentColor.set(var2.targetColor);
            }
         }

         this.currentAmbient.set(this.targetAmbient);
      }

      private float step(float var1, float var2, float var3) {
         if (var1 < var2) {
            return ClimateManager.clamp(0.0F, var2, var1 + var3);
         } else {
            return var1 > var2 ? ClimateManager.clamp(var2, 1.0F, var1 + var3) : var1;
         }
      }
   }

   public static final class FrameLightInfo {
      public FrameLightBlendStatus Stage;
      public int id;
      public int x;
      public int y;
      public int z;
      public float distSq;
      public int radius;
      public float r;
      public float g;
      public float b;
      public int flags;
      public final org.lwjgl.util.vector.Vector3f currentColor = new org.lwjgl.util.vector.Vector3f();
      public final org.lwjgl.util.vector.Vector3f targetColor = new org.lwjgl.util.vector.Vector3f();
      public boolean active;
      public boolean foundThisFrame;

      public FrameLightInfo() {
      }
   }

   public static enum FrameLightBlendStatus {
      In,
      During,
      Out;

      private FrameLightBlendStatus() {
      }
   }

   public static final class EffectLight {
      public float x;
      public float y;
      public float z;
      public float r;
      public float g;
      public float b;
      public int radius;

      public EffectLight() {
      }

      public void set(float var1, float var2, float var3, float var4, float var5, float var6, int var7) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.r = var4;
         this.g = var5;
         this.b = var6;
         this.radius = var7;
      }

      public void clear() {
         this.set(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0);
      }

      public void set(IsoLightSource var1) {
         this.x = (float)var1.x + 0.5F;
         this.y = (float)var1.y + 0.5F;
         this.z = (float)var1.z;
         this.r = var1.r;
         this.g = var1.g;
         this.b = var1.b;
         this.radius = var1.radius;
      }
   }
}
