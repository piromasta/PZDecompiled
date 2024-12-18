package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import zombie.characterTextures.ItemSmartTexture;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.debug.DebugOptions;
import zombie.entity.ComponentType;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;

public final class ItemModelRenderer {
   private static final ObjectPool<WeaponPartParams> s_weaponPartParamPool = new ObjectPool(WeaponPartParams::new);
   private static final ArrayList<WeaponPart> s_tempWeaponPartList = new ArrayList();
   private static final ColorInfo tempColorInfo = new ColorInfo();
   private static final Matrix4f s_attachmentXfrm = new Matrix4f();
   private static final WorldModelCamera worldModelCamera = new WorldModelCamera();
   static final Vector3f tempVector3f = new Vector3f(0.0F, 5.0F, -2.0F);
   private Model m_model;
   private ArrayList<WeaponPartParams> m_weaponParts;
   private float m_hue;
   private float m_tintR;
   private float m_tintG;
   private float m_tintB;
   public float m_x;
   public float m_y;
   public float m_z;
   public final Vector3f m_angle = new Vector3f();
   private final Matrix4f m_transform = new Matrix4f();
   private float m_ambientR;
   private float m_ambientG;
   private float m_ambientB;
   private float alpha = 1.0F;
   private float squareDepth;
   private boolean bRendered = false;
   private float bloodLevel = 0.0F;
   private float fluidLevel = 0.0F;
   private String modelTextureName = null;
   private String fluidTextureMask = null;
   private String tintMask = null;
   private final ItemSmartTexture smartTexture = new ItemSmartTexture((String)null);
   private Color tint;

   public ItemModelRenderer() {
      this.tint = Color.white;
   }

   public static boolean itemHasModel(InventoryItem var0) {
      if (var0 == null) {
         return false;
      } else {
         ModelScript var5;
         if (!StringUtils.isNullOrEmpty(var0.getWorldStaticItem())) {
            var5 = ScriptManager.instance.getModelScript(var0.getWorldStaticItem());
            return var5 != null;
         } else if (!(var0 instanceof Clothing)) {
            if (var0 instanceof HandWeapon) {
               var5 = ScriptManager.instance.getModelScript(var0.getStaticModel());
               return var5 != null;
            } else {
               return false;
            }
         } else {
            ClothingItem var1 = var0.getClothingItem();
            ItemVisual var2 = var0.getVisual();
            if (var1 != null && var2 != null && "Bip01_Head".equalsIgnoreCase(var1.m_AttachBone) && (!((Clothing)var0).isCosmetic() || "Eyes".equals(var0.getBodyLocation()))) {
               boolean var3 = false;
               String var4 = var1.getModel(var3);
               if (!StringUtils.isNullOrWhitespace(var4)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public RenderStatus renderMain(InventoryItem var1, IsoGridSquare var2, IsoGridSquare var3, float var4, float var5, float var6, float var7, float var8, boolean var9) {
      this.reset();
      if (!itemHasModel(var1)) {
         return ItemModelRenderer.RenderStatus.NoModel;
      } else {
         String var13;
         Model var17;
         ModelScript var23;
         String var24;
         String var25;
         if (!StringUtils.isNullOrEmpty(var1.getWorldStaticItem())) {
            var23 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem());
            if (var23 == null) {
               return ItemModelRenderer.RenderStatus.NoModel;
            } else {
               var24 = var23.getMeshName();
               var25 = var23.getTextureName();
               var13 = var23.getShaderName();
               ImmutableColor var27 = new ImmutableColor(var1.getColorRed(), var1.getColorGreen(), var1.getColorBlue(), 1.0F);
               float var29 = 1.0F;
               ModelScript var31;
               if (var1.hasComponent(ComponentType.FluidContainer)) {
                  this.fluidLevel = var1.getFluidContainer().getFilledRatio();
                  this.tint = var1.getFluidContainer().getColor();
                  var31 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "_Fluid");
                  if (var1.getFluidContainer().getFilledRatio() > 0.5F && var31 != null) {
                     var25 = var31.getTextureName();
                     var24 = var31.getMeshName();
                     var13 = var31.getShaderName();
                     var23 = var31;
                  }
               }

               if (var1 instanceof Food) {
                  if (var1.isCooked()) {
                     var31 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Cooked");
                     if (var31 != null) {
                        var25 = var31.getTextureName();
                        var24 = var31.getMeshName();
                        var13 = var31.getShaderName();
                        var23 = var31;
                     }
                  }

                  if (var1.isBurnt()) {
                     var31 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Burnt");
                     if (var31 != null) {
                        var25 = var31.getTextureName();
                        var24 = var31.getMeshName();
                        var13 = var31.getShaderName();
                        var23 = var31;
                     }
                  }

                  if (((Food)var1).isRotten()) {
                     var31 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Rotten");
                     if (var31 != null) {
                        var25 = var31.getTextureName();
                        var24 = var31.getMeshName();
                        var13 = var31.getShaderName();
                        var23 = var31;
                     } else {
                        var27 = WorldItemModelDrawer.ROTTEN_FOOD_COLOR;
                     }
                  }
               }

               if (var1 instanceof Clothing || var1.getClothingItem() != null) {
                  var25 = var23.getTextureName(true);
                  ItemVisual var33 = var1.getVisual();
                  ClothingItem var34 = var1.getClothingItem();
                  var27 = var33.getTint(var34);
                  if (var25 == null) {
                     if (var34.textureChoices.isEmpty()) {
                        var25 = var33.getBaseTexture(var34);
                     } else {
                        var25 = var33.getTextureChoice(var34);
                     }
                  }
               }

               boolean var35 = var23.bStatic;
               this.modelTextureName = this.initTextureName(var25);
               var17 = ModelManager.instance.tryGetLoadedModel(var24, var25, var35, var13, true);
               if (var17 == null) {
                  ModelManager.instance.loadAdditionalModel(var24, var25, var35, var13);
               }

               var17 = ModelManager.instance.getLoadedModel(var24, var25, var35, var13);
               if (var17 == null) {
                  return ItemModelRenderer.RenderStatus.NoModel;
               } else if (var17.isFailure()) {
                  return ItemModelRenderer.RenderStatus.Failed;
               } else if (var17.isReady() && var17.Mesh != null && var17.Mesh.isReady()) {
                  if (var17.tex != null && !var17.tex.isReady()) {
                     return ItemModelRenderer.RenderStatus.Loading;
                  } else {
                     String var37 = this.initTextureName(var25, "TINT");
                     Texture var38 = Texture.getSharedTexture(var37);
                     if (var38 != null) {
                        if (!var38.isReady()) {
                           return ItemModelRenderer.RenderStatus.Loading;
                        }

                        this.tintMask = var37;
                     }

                     if (this.fluidLevel > 0.0F && var17.tex != null) {
                        Texture var20 = Texture.getSharedTexture("media/textures/FullAlpha.png");
                        if (var20 != null && !var20.isReady()) {
                           return ItemModelRenderer.RenderStatus.Loading;
                        }

                        String var21 = this.initTextureName(var25, "FLUIDTINT");
                        var20 = Texture.getSharedTexture(var21);
                        if (var20 != null) {
                           if (!var20.isReady()) {
                              return ItemModelRenderer.RenderStatus.Loading;
                           }

                           this.fluidTextureMask = var21;
                        }
                     }

                     this.init(var1, var2, var3, var4, var5, var6, var17, var23, var29, var27, var7, false, var9);
                     if (var23.scale != 1.0F) {
                        this.m_transform.scale(var23.scale);
                     }

                     if (var1.worldScale != 1.0F) {
                        this.m_transform.scale(var1.worldScale);
                     }

                     this.m_angle.x = 0.0F;
                     if (var8 < 0.0F) {
                        this.m_angle.y = (float)var1.worldZRotation;
                     } else {
                        this.m_angle.y = var8;
                     }

                     this.m_angle.z = 0.0F;
                     if (Core.bDebug) {
                     }

                     return ItemModelRenderer.RenderStatus.Ready;
                  }
               } else {
                  return ItemModelRenderer.RenderStatus.Loading;
               }
            }
         } else if (!(var1 instanceof Clothing)) {
            if (var1 instanceof HandWeapon) {
               this.bloodLevel = var1.getBloodLevel();
               if (this.bloodLevel > 0.0F) {
                  Texture var22 = Texture.getSharedTexture("media/textures/BloodTextures/BloodOverlayWeapon.png");
                  if (var22 != null && !var22.isReady()) {
                     return ItemModelRenderer.RenderStatus.Loading;
                  }

                  var22 = Texture.getSharedTexture("media/textures/BloodTextures/BloodOverlayWeaponMask.png");
                  if (var22 != null && !var22.isReady()) {
                     return ItemModelRenderer.RenderStatus.Loading;
                  }
               }

               var23 = ScriptManager.instance.getModelScript(var1.getStaticModel());
               if (var23 == null) {
                  return ItemModelRenderer.RenderStatus.NoModel;
               } else {
                  var24 = var23.getMeshName();
                  var25 = var23.getTextureName();
                  var13 = var23.getShaderName();
                  boolean var26 = var23.bStatic;
                  this.modelTextureName = this.initTextureName(var25);
                  Model var28 = ModelManager.instance.tryGetLoadedModel(var24, var25, var26, var13, false);
                  if (var28 == null) {
                     ModelManager.instance.loadAdditionalModel(var24, var25, var26, var13);
                  }

                  var28 = ModelManager.instance.getLoadedModel(var24, var25, var26, var13);
                  if (var28 == null) {
                     return ItemModelRenderer.RenderStatus.NoModel;
                  } else if (var28.isFailure()) {
                     return ItemModelRenderer.RenderStatus.Failed;
                  } else if (var28.isReady() && var28.Mesh != null && var28.Mesh.isReady()) {
                     float var30 = 1.0F;
                     ImmutableColor var32 = new ImmutableColor(var1.getColorRed(), var1.getColorGreen(), var1.getColorBlue(), 1.0F);
                     this.init(var1, var2, var3, var4, var5, var6, var28, var23, var30, var32, var7, true, var9);
                     if (var23.scale != 1.0F) {
                        this.m_transform.scale(var23.scale);
                     }

                     if (var1.worldScale != 1.0F) {
                        this.m_transform.scale(var1.worldScale);
                     }

                     this.m_angle.x = 0.0F;
                     if (!WorldItemModelDrawer.NEW_WAY) {
                        this.m_angle.y = 180.0F;
                     }

                     if (var8 < 0.0F) {
                        this.m_angle.y = (float)var1.worldZRotation;
                     } else {
                        this.m_angle.y = var8;
                     }

                     RenderStatus var36 = this.initWeaponParts((HandWeapon)var1, var23);
                     if (var36 != ItemModelRenderer.RenderStatus.Ready) {
                        this.reset();
                        return var36;
                     } else {
                        return ItemModelRenderer.RenderStatus.Ready;
                     }
                  } else {
                     return ItemModelRenderer.RenderStatus.Loading;
                  }
               }
            } else {
               return ItemModelRenderer.RenderStatus.NoModel;
            }
         } else {
            ClothingItem var10 = var1.getClothingItem();
            ItemVisual var11 = var1.getVisual();
            if (var10 != null && var11 != null && "Bip01_Head".equalsIgnoreCase(var10.m_AttachBone) && (!((Clothing)var1).isCosmetic() || "Eyes".equals(var1.getBodyLocation()))) {
               boolean var12 = false;
               var13 = var10.getModel(var12);
               if (StringUtils.isNullOrWhitespace(var13)) {
                  return ItemModelRenderer.RenderStatus.NoModel;
               } else {
                  String var14 = var11.getTextureChoice(var10);
                  boolean var15 = var10.m_Static;
                  String var16 = var10.m_Shader;
                  this.modelTextureName = this.initTextureName(var14);
                  var17 = ModelManager.instance.tryGetLoadedModel(var13, var14, var15, var16, false);
                  if (var17 == null) {
                     ModelManager.instance.loadAdditionalModel(var13, var14, var15, var16);
                  }

                  var17 = ModelManager.instance.getLoadedModel(var13, var14, var15, var16);
                  if (var17 == null) {
                     return ItemModelRenderer.RenderStatus.NoModel;
                  } else if (var17.isFailure()) {
                     return ItemModelRenderer.RenderStatus.Failed;
                  } else if (var17.isReady() && var17.Mesh != null && var17.Mesh.isReady()) {
                     float var18 = var11.getHue(var10);
                     ImmutableColor var19 = var11.getTint(var10);
                     this.init(var1, var2, var3, var4, var5, var6, var17, (ModelScript)null, var18, var19, var7, false, var9);
                     if (WorldItemModelDrawer.NEW_WAY) {
                        this.m_angle.x = 180.0F + var7;
                        if (var8 < 0.0F) {
                           this.m_angle.y = (float)var1.worldZRotation;
                        } else {
                           this.m_angle.y = var8;
                        }

                        this.m_angle.z = -90.0F;
                        if (Core.bDebug) {
                        }

                        this.m_transform.translate(-var17.Mesh.minXYZ.x + 0.001F, 0.0F, 0.0F);
                     }

                     return ItemModelRenderer.RenderStatus.Ready;
                  } else {
                     return ItemModelRenderer.RenderStatus.Loading;
                  }
               }
            } else {
               return ItemModelRenderer.RenderStatus.NoModel;
            }
         }
      }
   }

   private String initTextureName(String var1) {
      return var1.contains("media/") ? var1 : "media/textures/" + var1 + ".png";
   }

   private String initTextureName(String var1, String var2) {
      if (var1.endsWith(".png")) {
         var1 = var1.substring(0, var1.length() - 4);
      }

      return var1.contains("media/") ? var1 + var2 + ".png" : "media/textures/" + var1 + var2 + ".png";
   }

   public boolean isRendered() {
      return this.bRendered;
   }

   private void init(InventoryItem var1, IsoGridSquare var2, IsoGridSquare var3, float var4, float var5, float var6, Model var7, ModelScript var8, float var9, ImmutableColor var10, float var11, boolean var12, boolean var13) {
      this.m_model = var7;
      if (this.m_weaponParts != null) {
         s_weaponPartParamPool.release((List)this.m_weaponParts);
         this.m_weaponParts.clear();
      }

      if (var1.getWorldItem() != null && var1.getWorldItem().isHighlighted()) {
         var10 = WorldItemModelDrawer.HIGHLIGHT_COLOR;
      }

      this.m_tintR = var10.r;
      this.m_tintG = var10.g;
      this.m_tintB = var10.b;
      this.m_hue = var9;
      this.m_x = var4;
      this.m_y = var5;
      this.m_z = var6;
      this.m_transform.rotationZ((90.0F + var11) * 0.017453292F);
      if (var1 instanceof Clothing) {
         float var14 = -0.08F;
         float var15 = 0.05F;
         this.m_transform.translate(var14, 0.0F, var15);
      }

      this.m_angle.x = 0.0F;
      this.m_angle.y = 525.0F;
      this.m_angle.z = 0.0F;
      if (WorldItemModelDrawer.NEW_WAY) {
         this.m_transform.identity();
         this.m_angle.y = 0.0F;
         if (var12) {
            this.m_transform.rotateXYZ(0.0F, 3.1415927F, 1.5707964F);
         }

         if (var8 != null) {
            ModelAttachment var16 = var8.getAttachmentById("world");
            if (var16 != null) {
               ModelInstanceRenderData.makeAttachmentTransform(var16, s_attachmentXfrm);
               s_attachmentXfrm.invert();
               this.m_transform.mul(s_attachmentXfrm);
            }
         }

         ModelInstanceRenderData.postMultiplyMeshTransform(this.m_transform, var7.Mesh);
      }

      var2.interpolateLight(tempColorInfo, this.m_x % 1.0F, this.m_y % 1.0F);
      if (GameServer.bServer && ServerGUI.isCreated()) {
         tempColorInfo.set(1.0F, 1.0F, 1.0F, 1.0F);
      }

      this.m_ambientR = tempColorInfo.r;
      this.m_ambientG = tempColorInfo.g;
      this.m_ambientB = tempColorInfo.b;
      this.alpha = IsoWorldInventoryObject.getSurfaceAlpha(var2, var6 - (float)PZMath.fastfloor(var6));
      IsoDepthHelper.Results var17 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var4, var5, var6);
      if (var13) {
         var17.depthStart -= IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / 8.0F), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / 8.0F), PZMath.fastfloor((float)var3.x / 8.0F), PZMath.fastfloor((float)var3.y / 8.0F), var3.z).depthStart;
      }

      this.squareDepth = var17.depthStart;
   }

   private RenderStatus initWeaponParts(HandWeapon var1, ModelScript var2) {
      ArrayList var3 = var1.getModelWeaponPart();
      if (var3 == null) {
         return ItemModelRenderer.RenderStatus.Ready;
      } else {
         ArrayList var4 = var1.getAllWeaponParts(s_tempWeaponPartList);

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            WeaponPart var6 = (WeaponPart)var4.get(var5);

            for(int var7 = 0; var7 < var3.size(); ++var7) {
               ModelWeaponPart var8 = (ModelWeaponPart)var3.get(var7);
               if (var6.getFullType().equals(var8.partType)) {
                  RenderStatus var9 = this.initWeaponPart(var8, var2);
                  if (var9 != ItemModelRenderer.RenderStatus.Ready) {
                     return var9;
                  }
                  break;
               }
            }
         }

         return ItemModelRenderer.RenderStatus.Ready;
      }
   }

   private RenderStatus initWeaponPart(ModelWeaponPart var1, ModelScript var2) {
      String var3 = StringUtils.discardNullOrWhitespace(var1.modelName);
      if (var3 == null) {
         return ItemModelRenderer.RenderStatus.NoModel;
      } else {
         ModelScript var4 = ScriptManager.instance.getModelScript(var3);
         if (var4 == null) {
            return ItemModelRenderer.RenderStatus.NoModel;
         } else {
            String var5 = var4.getMeshName();
            String var6 = var4.getTextureName();
            String var7 = var4.getShaderName();
            boolean var8 = var4.bStatic;
            Model var9 = ModelManager.instance.tryGetLoadedModel(var5, var6, var8, var7, false);
            if (var9 == null) {
               ModelManager.instance.loadAdditionalModel(var5, var6, var8, var7);
            }

            var9 = ModelManager.instance.getLoadedModel(var5, var6, var8, var7);
            if (var9 == null) {
               return ItemModelRenderer.RenderStatus.NoModel;
            } else if (var9.isFailure()) {
               return ItemModelRenderer.RenderStatus.Failed;
            } else if (var9.isReady() && var9.Mesh != null && var9.Mesh.isReady()) {
               WeaponPartParams var10 = (WeaponPartParams)s_weaponPartParamPool.alloc();
               var10.m_model = var9;
               var10.m_attachmentNameSelf = var1.attachmentNameSelf;
               var10.m_attachmentNameParent = var1.attachmentParent;
               var10.initTransform(var2, var4);
               this.m_transform.mul(var10.m_transform, var10.m_transform);
               if (this.m_weaponParts == null) {
                  this.m_weaponParts = new ArrayList();
               }

               this.m_weaponParts.add(var10);
               return ItemModelRenderer.RenderStatus.Ready;
            } else {
               return ItemModelRenderer.RenderStatus.Loading;
            }
         }
      }
   }

   public void DoRenderToWorld(float var1, float var2, float var3, Vector3f var4) {
      worldModelCamera.m_x = var1;
      worldModelCamera.m_y = var2;
      worldModelCamera.m_z = var3;
      worldModelCamera.m_angle.set(var4);
      this.DoRender(worldModelCamera, false, false);
   }

   public void DoRender(IModelCamera var1, boolean var2, boolean var3) {
      GL11.glPushAttrib(1048575);
      GL11.glPushClientAttrib(-1);
      var1.Begin();
      GL14.glBlendFuncSeparate(1, 771, 773, 1);
      GL11.glDepthFunc(515);
      GL11.glDepthMask(true);
      GL11.glDepthRange(0.0, 1.0);
      GL11.glEnable(2929);
      GL11.glColor3f(1.0F, 1.0F, 1.0F);
      this.bRendered = true;
      this.renderModel(this.m_model, this.m_transform, var2, var3, false);
      if (this.m_weaponParts != null) {
         for(int var4 = 0; var4 < this.m_weaponParts.size(); ++var4) {
            WeaponPartParams var5 = (WeaponPartParams)this.m_weaponParts.get(var4);
            this.renderModel(var5.m_model, var5.m_transform, var2, var3, true);
         }
      }

      if (Core.bDebug && DebugOptions.instance.Model.Render.Axis.getValue()) {
         Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.5F, 1.0F);
      }

      var1.End();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      SpriteRenderer.ringBuffer.restoreVBOs = true;
      GLStateRenderThread.restore();
   }

   void renderModel(Model var1, Matrix4f var2, boolean var3, boolean var4, boolean var5) {
      if (!var1.bStatic) {
         this.bRendered = false;
      } else {
         if (var1.Effect == null) {
            var1.CreateShader("basicEffect");
         }

         Shader var6 = var1.Effect;
         if (var6 != null && var1.Mesh != null && var1.Mesh.isReady()) {
            float var8;
            float var9;
            if (Core.bDebug && DebugOptions.instance.Model.Render.Wireframe.getValue()) {
               GL11.glPolygonMode(1032, 6913);
               GL11.glEnable(2848);
               GL11.glLineWidth(0.75F);
               Shader var10 = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", var1.bStatic, false);
               if (var10 != null) {
                  var10.Start();
                  if (var1.bStatic) {
                     var10.setTransformMatrix(var2, false);
                  }

                  var8 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
                  var9 = this.squareDepth - (var8 + 1.0F) / 2.0F + 0.5F;
                  if (!PerformanceSettings.FBORenderChunk) {
                     var9 = 0.5F;
                  }

                  var10.setTargetDepth(var9);
                  var1.Mesh.Draw(var10);
                  var10.End();
               }

               GL11.glPolygonMode(1032, 6914);
               GL11.glDisable(2848);
            } else {
               boolean var7 = !var5 && this.checkSmartTexture(this.modelTextureName);
               var6.Start();
               if (var7) {
                  var6.setTexture(this.smartTexture, "Texture", 0);
               } else if (var1.tex != null) {
                  var6.setTexture(var1.tex, "Texture", 0);
               }

               var6.setDepthBias(0.0F);
               var8 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
               var9 = this.squareDepth - (var8 + 1.0F) / 2.0F + 0.5F;
               if (!PerformanceSettings.FBORenderChunk) {
                  var9 = 0.5F;
               }

               var6.setTargetDepth(var9);
               var6.setAmbient(this.m_ambientR * 0.4F, this.m_ambientG * 0.4F, this.m_ambientB * 0.4F);
               var6.setLightingAmount(1.0F);
               var6.setHueShift(this.m_hue);
               var6.setTint(1.0F, 1.0F, 1.0F);
               if (this.tintMask == null) {
                  var6.setTint(this.m_tintR, this.m_tintG, this.m_tintB);
               }

               var6.setAlpha(this.alpha);

               for(int var11 = 0; var11 < 5; ++var11) {
                  var6.setLight(var11, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
               }

               Vector3f var12 = tempVector3f;
               var12.x = 0.0F;
               var12.y = 5.0F;
               var12.z = -2.0F;
               var12.rotateY((float)Math.toRadians((double)this.m_angle.y));
               var9 = 1.5F;
               var6.setLight(4, var12.x, var12.z, var12.y, this.m_ambientR / 4.0F * var9, this.m_ambientG / 4.0F * var9, this.m_ambientB / 4.0F * var9, 5000.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
               if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
                  var6.setAmbient(1.0F, 1.0F, 1.0F);
               }

               var6.setTransformMatrix(var2, false);
               if (var3 && var4) {
                  var6.setHighResDepthMultiplier(0.5F);
               }

               var1.Mesh.Draw(var6);
               if (var3 && var4) {
                  var6.setHighResDepthMultiplier(0.0F);
               }

               var6.End();
               if (var7) {
                  if (this.smartTexture.result != null) {
                     TextureCombiner.instance.releaseTexture(this.smartTexture.result);
                     this.smartTexture.result = null;
                  }

                  this.smartTexture.clear();
               }

            }
         } else {
            this.bRendered = false;
         }
      }
   }

   private boolean checkSmartTexture(String var1) {
      if (var1 == null) {
         return false;
      } else {
         boolean var2 = false;
         if (this.bloodLevel > 0.0F) {
            var2 = true;
         }

         String var3 = null;
         String var4 = null;
         if (this.fluidLevel > 0.0F && this.fluidTextureMask != null) {
            var2 = true;
            var3 = this.fluidTextureMask;
         }

         if (this.tintMask != null) {
            var2 = true;
            var4 = this.tintMask;
         }

         if (var2) {
            this.smartTexture.clear();
            this.smartTexture.add(var1);
            if (this.tintMask != null) {
               ImmutableColor var5 = new ImmutableColor(this.m_tintR, this.m_tintG, this.m_tintB, 1.0F);
               this.smartTexture.setTintMask(var4, "media/textures/FullAlpha.png", 300, var5.toMutableColor());
            }

            if (this.bloodLevel > 0.0F) {
               this.smartTexture.setBlood("media/textures/BloodTextures/BloodOverlayWeapon.png", "media/textures/BloodTextures/BloodOverlayWeaponMask.png", this.bloodLevel, 301);
            }

            if (this.fluidLevel > 0.0F && var3 != null) {
               String var6 = "media/textures/FullAlpha.png";
               this.smartTexture.setFluid(var3, var6, this.fluidLevel, 302, this.tint);
            }

            this.smartTexture.calculate();
            GL11.glDepthFunc(515);
            GL11.glDepthMask(true);
            GL11.glDepthRange(0.0, 1.0);
            GL11.glEnable(2929);
         }

         return var2;
      }
   }

   public void reset() {
      this.bRendered = false;
      this.bloodLevel = 0.0F;
      this.fluidLevel = 0.0F;
      this.modelTextureName = null;
      this.fluidTextureMask = null;
      this.tintMask = null;
      this.tint = Color.white;
      if (this.m_weaponParts != null) {
         s_weaponPartParamPool.release((List)this.m_weaponParts);
         this.m_weaponParts.clear();
      }

   }

   public static enum RenderStatus {
      NoModel,
      Failed,
      Loading,
      Ready;

      private RenderStatus() {
      }
   }

   private static final class WeaponPartParams {
      Model m_model;
      String m_attachmentNameSelf;
      String m_attachmentNameParent;
      final Matrix4f m_transform = new Matrix4f();

      private WeaponPartParams() {
      }

      void initTransform(ModelScript var1, ModelScript var2) {
         this.m_transform.identity();
         Matrix4f var3 = ItemModelRenderer.s_attachmentXfrm;
         ModelAttachment var4 = var1.getAttachmentById(this.m_attachmentNameParent);
         if (var4 != null) {
            ModelInstanceRenderData.makeAttachmentTransform(var4, var3);
            this.m_transform.mul(var3);
         }

         ModelAttachment var5 = var2.getAttachmentById(this.m_attachmentNameSelf);
         if (var5 != null) {
            ModelInstanceRenderData.makeAttachmentTransform(var5, var3);
            if (ModelInstanceRenderData.INVERT_ATTACHMENT_SELF_TRANSFORM) {
               var3.invert();
            }

            this.m_transform.mul(var3);
         }

      }
   }

   private static final class WorldModelCamera implements IModelCamera {
      float m_x;
      float m_y;
      float m_z;
      final Vector3f m_angle = new Vector3f();

      private WorldModelCamera() {
      }

      public void Begin() {
         Core.getInstance().DoPushIsoStuff(this.m_x, this.m_y, this.m_z, 0.0F, false);
         Matrix4f var1 = Core.getInstance().modelViewMatrixStack.peek();
         var1.rotate(-3.1415927F, 0.0F, 1.0F, 0.0F);
         var1.rotate(this.m_angle.x * 0.017453292F, 1.0F, 0.0F, 0.0F);
         var1.rotate(this.m_angle.y * 0.017453292F, 0.0F, 1.0F, 0.0F);
         var1.rotate(this.m_angle.z * 0.017453292F, 0.0F, 0.0F, 1.0F);
         GL11.glDepthMask(true);
      }

      public void End() {
         Core.getInstance().DoPopIsoStuff();
      }
   }
}
