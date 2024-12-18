package zombie.core.skinnedmodel.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import zombie.IndieGL;
import zombie.characterTextures.ItemSmartTexture;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.entity.ComponentType;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.PlayerCamera;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class WorldItemAtlas {
   public static final int ATLAS_SIZE = 512;
   public static final int MATRIX_SIZE = 1024;
   private static final float MAX_ZOOM = 2.5F;
   private TextureFBO fbo;
   public static final WorldItemAtlas instance = new WorldItemAtlas();
   private final HashMap<String, ItemTexture> itemTextureMap = new HashMap();
   private final ArrayList<Atlas> AtlasList = new ArrayList();
   private final ItemParams itemParams = new ItemParams();
   private final Checksummer checksummer = new Checksummer();
   private static final Stack<RenderJob> JobPool = new Stack();
   private final ArrayList<RenderJob> RenderJobs = new ArrayList();
   private final ObjectPool<ItemTextureDrawer> itemTextureDrawerPool = new ObjectPool(ItemTextureDrawer::new);
   private final ObjectPool<ItemTextureDepthDrawer> itemTextureDepthDrawerPool = new ObjectPool(ItemTextureDepthDrawer::new);
   private final ObjectPool<WeaponPartParams> weaponPartParamPool = new ObjectPool(WeaponPartParams::new);
   private final ArrayList<WeaponPart> m_tempWeaponPartList = new ArrayList();
   private static final Matrix4f s_attachmentXfrm = new Matrix4f();
   private static final ImmutableColor ROTTEN_FOOD_COLOR = new ImmutableColor(0.5F, 0.5F, 0.5F);

   public WorldItemAtlas() {
   }

   public ItemTexture getItemTexture(InventoryItem var1, boolean var2) {
      return this.itemParams.init(var1, var2) ? this.getItemTexture(this.itemParams) : null;
   }

   public ItemTexture getItemTexture(ItemParams var1) {
      String var2 = this.getItemKey(var1);
      ItemTexture var3 = (ItemTexture)this.itemTextureMap.get(var2);
      if (var3 != null) {
         return var3;
      } else {
         AtlasEntry var4 = new AtlasEntry();
         var4.key = var2;
         var3 = new ItemTexture();
         var3.itemParams.copyFrom(var1);
         var3.entry = var4;
         this.itemTextureMap.put(var2, var3);
         this.RenderJobs.add(WorldItemAtlas.RenderJob.getNew().init(var1, var4));
         return var3;
      }
   }

   private void assignEntryToAtlas(AtlasEntry var1, int var2, int var3) {
      if (var1.atlas == null) {
         for(int var4 = 0; var4 < this.AtlasList.size(); ++var4) {
            Atlas var5 = (Atlas)this.AtlasList.get(var4);
            if (!var5.isFull() && var5.ENTRY_WID == var2 && var5.ENTRY_HGT == var3) {
               var5.addEntry(var1);
               return;
            }
         }

         Atlas var6 = new Atlas(512, 512, var2, var3);
         var6.addEntry(var1);
         this.AtlasList.add(var6);
      }
   }

   private String getItemKey(ItemParams var1) {
      try {
         this.checksummer.reset();
         this.checksummer.update(var1.m_model.Name);
         if (var1.m_weaponParts != null) {
            for(int var2 = 0; var2 < var1.m_weaponParts.size(); ++var2) {
               WeaponPartParams var3 = (WeaponPartParams)var1.m_weaponParts.get(var2);
               this.checksummer.update(var3.m_model.Name);
            }
         }

         this.checksummer.update((int)(var1.worldScale * 1000.0F));
         this.checksummer.update((byte)((int)(var1.m_tintR * 255.0F)));
         this.checksummer.update((byte)((int)(var1.m_tintG * 255.0F)));
         this.checksummer.update((byte)((int)(var1.m_tintB * 255.0F)));
         this.checksummer.update((int)(var1.m_angle.x * 1000.0F));
         this.checksummer.update((int)(var1.m_angle.y * 1000.0F));
         this.checksummer.update((int)(var1.m_angle.z * 1000.0F));
         this.checksummer.update((byte)var1.m_foodState.ordinal());
         this.checksummer.update(var1.bMaxZoomIsOne);
         this.checksummer.update(var1.bFluidFilled);
         this.checksummer.update((int)var1.m_bloodLevel * 1000);
         this.checksummer.update((int)var1.m_fluidLevel * 1000);
         return this.checksummer.checksumToString();
      } catch (Throwable var4) {
         ExceptionLogger.logException(var4);
         return "bogus";
      }
   }

   public void render() {
      int var1;
      for(var1 = 0; var1 < this.AtlasList.size(); ++var1) {
         Atlas var2 = (Atlas)this.AtlasList.get(var1);
         if (var2.clear) {
            SpriteRenderer.instance.drawGeneric(new ClearAtlasTexture(var2));
         }
      }

      if (!this.RenderJobs.isEmpty()) {
         for(var1 = 0; var1 < this.RenderJobs.size(); ++var1) {
            RenderJob var3 = (RenderJob)this.RenderJobs.get(var1);
            if (var3.done != 1 || var3.renderRefCount <= 0) {
               if (var3.done == 1 && var3.renderRefCount == 0) {
                  this.RenderJobs.remove(var1--);

                  assert !JobPool.contains(var3);

                  JobPool.push(var3);
               } else {
                  var3.entry.bRenderMainOK = var3.renderMain();
                  if (var3.entry.bRenderMainOK) {
                     ++var3.renderRefCount;
                     SpriteRenderer.instance.drawGeneric(var3);
                  }
               }
            }
         }

      }
   }

   public void renderUI() {
      if (DebugOptions.instance.WorldItemAtlas.Render.getValue() && GameKeyboard.isKeyPressed(209)) {
         this.Reset();
      }

      if (DebugOptions.instance.WorldItemAtlas.Render.getValue()) {
         boolean var1 = false;
         int var2 = 2560 / Core.TileScale;
         var2 /= 2;
         int var3 = 0;
         int var4 = 0;

         for(int var5 = 0; var5 < this.AtlasList.size(); ++var5) {
            Atlas var6 = (Atlas)this.AtlasList.get(var5);
            Texture var7 = var1 ? var6.depth : var6.tex;
            SpriteRenderer.instance.renderi((Texture)null, var3, var4, var2, var2, 1.0F, 1.0F, 1.0F, 0.75F, (Consumer)null);
            SpriteRenderer.instance.renderi(var7, var3, var4, var2, var2, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
            float var8 = (float)var2 / (float)var7.getWidth();

            int var9;
            for(var9 = 0; var9 <= var7.getWidth() / var6.ENTRY_WID; ++var9) {
               SpriteRenderer.instance.renderline((Texture)null, (int)((float)var3 + (float)(var9 * var6.ENTRY_WID) * var8), var4, (int)((float)var3 + (float)(var9 * var6.ENTRY_WID) * var8), var4 + var2, 0.5F, 0.5F, 0.5F, 1.0F);
            }

            for(var9 = 0; var9 <= var7.getHeight() / var6.ENTRY_HGT; ++var9) {
               SpriteRenderer.instance.renderline((Texture)null, var3, (int)((float)(var4 + var2) - (float)(var9 * var6.ENTRY_HGT) * var8), var3 + var2, (int)((float)(var4 + var2) - (float)(var9 * var6.ENTRY_HGT) * var8), 0.5F, 0.5F, 0.5F, 1.0F);
            }

            var4 += var2;
            if (var4 + var2 > Core.getInstance().getScreenHeight()) {
               var4 = 0;
               var3 += var2;
            }
         }
      }

   }

   public void Reset() {
      if (this.fbo != null) {
         this.fbo.destroyLeaveTexture();
         this.fbo = null;
      }

      this.AtlasList.forEach(Atlas::Reset);
      this.AtlasList.clear();
      this.itemTextureMap.values().forEach(ItemTexture::Reset);
      this.itemTextureMap.clear();
      JobPool.forEach(RenderJob::Reset);
      JobPool.clear();
      this.RenderJobs.clear();
   }

   private static final class ItemParams {
      float worldScale = 1.0F;
      float worldZRotation = 0.0F;
      FoodState m_foodState;
      private Model m_model;
      private ArrayList<WeaponPartParams> m_weaponParts;
      private float m_hue;
      private float m_tintR;
      private float m_tintG;
      private float m_tintB;
      private final Vector3f m_angle;
      private final Matrix4f m_transform;
      private float m_ambientR;
      private float m_ambientG;
      private float m_ambientB;
      private float alpha;
      private float m_bloodLevel;
      private float m_fluidLevel;
      private boolean bMaxZoomIsOne;
      private boolean bFluidFilled;
      private String modelTextureName;
      private String fluidTextureMask;
      private final ItemSmartTexture smartTexture;
      private Color fluidTint;

      ItemParams() {
         this.m_foodState = WorldItemAtlas.ItemParams.FoodState.Normal;
         this.m_angle = new Vector3f();
         this.m_transform = new Matrix4f();
         this.m_ambientR = 1.0F;
         this.m_ambientG = 1.0F;
         this.m_ambientB = 1.0F;
         this.alpha = 1.0F;
         this.m_bloodLevel = 0.0F;
         this.m_fluidLevel = 0.0F;
         this.bMaxZoomIsOne = false;
         this.bFluidFilled = false;
         this.modelTextureName = null;
         this.fluidTextureMask = null;
         this.smartTexture = new ItemSmartTexture((String)null);
         this.fluidTint = Color.white;
      }

      void copyFrom(ItemParams var1) {
         this.worldScale = var1.worldScale;
         this.worldZRotation = var1.worldZRotation;
         this.m_foodState = var1.m_foodState;
         this.bFluidFilled = var1.bFluidFilled;
         this.modelTextureName = var1.modelTextureName;
         this.fluidTextureMask = var1.fluidTextureMask;
         this.m_model = var1.m_model;
         if (this.m_weaponParts != null) {
            WorldItemAtlas.instance.weaponPartParamPool.release((List)this.m_weaponParts);
            this.m_weaponParts.clear();
         }

         if (var1.m_weaponParts != null) {
            if (this.m_weaponParts == null) {
               this.m_weaponParts = new ArrayList();
            }

            for(int var2 = 0; var2 < var1.m_weaponParts.size(); ++var2) {
               WeaponPartParams var3 = (WeaponPartParams)var1.m_weaponParts.get(var2);
               this.m_weaponParts.add(((WeaponPartParams)WorldItemAtlas.instance.weaponPartParamPool.alloc()).init(var3));
            }
         }

         this.m_hue = var1.m_hue;
         this.m_tintR = var1.m_tintR;
         this.m_tintG = var1.m_tintG;
         this.m_tintB = var1.m_tintB;
         this.m_angle.set(var1.m_angle);
         this.m_transform.set(var1.m_transform);
         this.m_bloodLevel = var1.m_bloodLevel;
         this.m_fluidLevel = var1.m_fluidLevel;
         this.fluidTint = var1.fluidTint;
         this.bMaxZoomIsOne = var1.bMaxZoomIsOne;
      }

      boolean init(InventoryItem var1, boolean var2) {
         this.Reset();
         this.worldScale = var1.worldScale;
         this.worldZRotation = (float)var1.worldZRotation;
         this.bMaxZoomIsOne = var2;
         float var3 = 0.0F;
         String var4 = StringUtils.discardNullOrWhitespace(var1.getWorldStaticItem());
         String var20;
         if (var4 != null) {
            ModelScript var17 = ScriptManager.instance.getModelScript(var4);
            if (var17 == null) {
               return false;
            } else {
               String var19 = var17.getMeshName();
               var20 = var17.getTextureName();
               String var22 = var17.getShaderName();
               ImmutableColor var23 = new ImmutableColor(var1.getColorRed(), var1.getColorGreen(), var1.getColorBlue(), 1.0F);
               float var24 = 1.0F;
               if (var1.hasComponent(ComponentType.FluidContainer)) {
                  this.m_fluidLevel = var1.getFluidContainer().getFilledRatio();
                  this.fluidTint = var1.getFluidContainer().getColor();
                  if (var1.getFluidContainer().getFilledRatio() > 0.5F) {
                     this.bFluidFilled = true;
                     ModelScript var26 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "_Fluid");
                     if (var26 != null) {
                        var20 = var26.getTextureName();
                        var19 = var26.getMeshName();
                        var22 = var26.getShaderName();
                        var17 = var26;
                     }
                  }
               }

               Food var27 = (Food)Type.tryCastTo(var1, Food.class);
               if (var27 != null) {
                  this.m_foodState = this.getFoodState(var27);
                  ModelScript var29;
                  if (var27.isCooked()) {
                     var29 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Cooked");
                     if (var29 != null) {
                        var20 = var29.getTextureName();
                        var19 = var29.getMeshName();
                        var22 = var29.getShaderName();
                        var17 = var29;
                     }
                  }

                  if (var27.isBurnt()) {
                     var29 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Burnt");
                     if (var29 != null) {
                        var20 = var29.getTextureName();
                        var19 = var29.getMeshName();
                        var22 = var29.getShaderName();
                        var17 = var29;
                     }
                  }

                  if (var27.isRotten()) {
                     var29 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Rotten");
                     if (var29 != null) {
                        var20 = var29.getTextureName();
                        var19 = var29.getMeshName();
                        var22 = var29.getShaderName();
                        var17 = var29;
                     } else {
                        var23 = WorldItemAtlas.ROTTEN_FOOD_COLOR;
                     }
                  }
               }

               Clothing var30 = (Clothing)Type.tryCastTo(var1, Clothing.class);
               if (var30 != null || var1.getClothingItem() != null) {
                  String var31 = var17.getTextureName(true);
                  ItemVisual var32 = var1.getVisual();
                  ClothingItem var34 = var1.getClothingItem();
                  ImmutableColor var16 = var32.getTint(var34);
                  if (var31 == null) {
                     if (var34.textureChoices.isEmpty()) {
                        var31 = var32.getBaseTexture(var34);
                     } else {
                        var31 = var32.getTextureChoice(var34);
                     }
                  }

                  if (var31 != null) {
                     var20 = var31;
                     var23 = var16;
                  }
               }

               this.modelTextureName = this.initTextureName(var20);
               boolean var35 = var17.bStatic;
               Model var33 = ModelManager.instance.tryGetLoadedModel(var19, var20, var35, var22, true);
               if (var33 == null) {
                  ModelManager.instance.loadAdditionalModel(var19, var20, var35, var22);
               }

               var33 = ModelManager.instance.getLoadedModel(var19, var20, var35, var22);
               if (var33 != null && var33.isReady() && var33.Mesh != null && var33.Mesh.isReady()) {
                  if (this.m_fluidLevel > 0.0F && var33.tex != null) {
                     Texture var36 = Texture.getSharedTexture("media/textures/FullAlpha.png");
                     if (var36 != null && !var36.isReady()) {
                        return false;
                     }

                     String var37 = this.initTextureName(var20, "FLUIDTINT");
                     var36 = Texture.getSharedTexture(var37);
                     if (var36 != null) {
                        if (!var36.isReady()) {
                           return false;
                        }

                        this.fluidTextureMask = var37;
                     }
                  }

                  this.init(var1, var33, var17, var24, var23, var3, false);
                  if (this.worldScale != 1.0F) {
                     this.m_transform.scale(var17.scale * this.worldScale);
                  } else if (var17.scale != 1.0F) {
                     this.m_transform.scale(var17.scale);
                  }

                  this.m_angle.x = 0.0F;
                  this.m_angle.y = this.worldZRotation;
                  this.m_angle.z = 0.0F;
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            Clothing var5 = (Clothing)Type.tryCastTo(var1, Clothing.class);
            String var9;
            String var10;
            Model var13;
            float var14;
            ImmutableColor var15;
            if (var5 == null) {
               HandWeapon var18 = (HandWeapon)Type.tryCastTo(var1, HandWeapon.class);
               if (var18 != null) {
                  var20 = StringUtils.discardNullOrWhitespace(var18.getStaticModel());
                  if (var20 == null) {
                     return false;
                  } else {
                     ModelScript var21 = ScriptManager.instance.getModelScript(var20);
                     if (var21 == null) {
                        return false;
                     } else {
                        var9 = var21.getMeshName();
                        var10 = var21.getTextureName();
                        String var25 = var21.getShaderName();
                        boolean var28 = var21.bStatic;
                        this.modelTextureName = this.initTextureName(var10);
                        var13 = ModelManager.instance.tryGetLoadedModel(var9, var10, var28, var25, false);
                        if (var13 == null) {
                           ModelManager.instance.loadAdditionalModel(var9, var10, var28, var25);
                        }

                        var13 = ModelManager.instance.getLoadedModel(var9, var10, var28, var25);
                        if (var13 != null && var13.isReady() && var13.Mesh != null && var13.Mesh.isReady()) {
                           this.m_bloodLevel = var1.getBloodLevel();
                           var14 = 1.0F;
                           var15 = new ImmutableColor(var1.getColorRed(), var1.getColorGreen(), var1.getColorBlue(), 1.0F);
                           this.init(var1, var13, var21, var14, var15, var3, true);
                           if (this.worldScale != 1.0F) {
                              this.m_transform.scale(var21.scale * this.worldScale);
                           } else if (var21.scale != 1.0F) {
                              this.m_transform.scale(var21.scale);
                           }

                           this.m_angle.x = 0.0F;
                           this.m_angle.y = this.worldZRotation;
                           return this.initWeaponParts(var18, var21);
                        } else {
                           return false;
                        }
                     }
                  }
               } else {
                  return false;
               }
            } else {
               ClothingItem var6 = var1.getClothingItem();
               ItemVisual var7 = var1.getVisual();
               boolean var8 = false;
               var9 = var6.getModel(var8);
               if (var6 == null || var7 == null || StringUtils.isNullOrWhitespace(var9) || !"Bip01_Head".equalsIgnoreCase(var6.m_AttachBone) || var5.isCosmetic() && !"Eyes".equals(var1.getBodyLocation())) {
                  return false;
               } else {
                  var10 = var7.getTextureChoice(var6);
                  boolean var11 = var6.m_Static;
                  String var12 = var6.m_Shader;
                  this.modelTextureName = this.initTextureName(var10);
                  var13 = ModelManager.instance.tryGetLoadedModel(var9, var10, var11, var12, false);
                  if (var13 == null) {
                     ModelManager.instance.loadAdditionalModel(var9, var10, var11, var12);
                  }

                  var13 = ModelManager.instance.getLoadedModel(var9, var10, var11, var12);
                  if (var13 != null && var13.isReady() && var13.Mesh != null && var13.Mesh.isReady()) {
                     var14 = var7.getHue(var6);
                     var15 = var7.getTint(var6);
                     this.init(var1, var13, (ModelScript)null, var14, var15, var3, false);
                     this.m_angle.x = 180.0F + var3;
                     this.m_angle.y = this.worldZRotation;
                     this.m_angle.z = -90.0F;
                     this.m_transform.translate(-var13.Mesh.minXYZ.x + 0.001F, 0.0F, 0.0F);
                     return true;
                  } else {
                     return false;
                  }
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

      boolean initWeaponParts(HandWeapon var1, ModelScript var2) {
         ArrayList var3 = var1.getModelWeaponPart();
         if (var3 == null) {
            return true;
         } else {
            ArrayList var4 = var1.getAllWeaponParts(WorldItemAtlas.instance.m_tempWeaponPartList);

            for(int var5 = 0; var5 < var4.size(); ++var5) {
               WeaponPart var6 = (WeaponPart)var4.get(var5);

               for(int var7 = 0; var7 < var3.size(); ++var7) {
                  ModelWeaponPart var8 = (ModelWeaponPart)var3.get(var7);
                  if (var6.getFullType().equals(var8.partType)) {
                     if (!this.initWeaponPart(var8, var2)) {
                        return false;
                     }
                     break;
                  }
               }
            }

            return true;
         }
      }

      boolean initWeaponPart(ModelWeaponPart var1, ModelScript var2) {
         String var3 = StringUtils.discardNullOrWhitespace(var1.modelName);
         if (var3 == null) {
            return false;
         } else {
            ModelScript var4 = ScriptManager.instance.getModelScript(var3);
            if (var4 == null) {
               return false;
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
               if (var9 != null && var9.isReady() && var9.Mesh != null && var9.Mesh.isReady()) {
                  WeaponPartParams var10 = (WeaponPartParams)WorldItemAtlas.instance.weaponPartParamPool.alloc();
                  var10.m_model = var9;
                  var10.m_attachmentNameSelf = var1.attachmentNameSelf;
                  var10.m_attachmentNameParent = var1.attachmentParent;
                  var10.initTransform(var2, var4);
                  if (this.m_weaponParts == null) {
                     this.m_weaponParts = new ArrayList();
                  }

                  this.m_weaponParts.add(var10);
                  return true;
               } else {
                  return false;
               }
            }
         }
      }

      void init(InventoryItem var1, Model var2, ModelScript var3, float var4, ImmutableColor var5, float var6, boolean var7) {
         this.m_model = var2;
         this.m_tintR = var5.r;
         this.m_tintG = var5.g;
         this.m_tintB = var5.b;
         this.m_hue = var4;
         this.m_angle.set(0.0F);
         this.m_transform.identity();
         this.m_ambientR = this.m_ambientG = this.m_ambientB = 1.0F;
         if (var7) {
            this.m_transform.rotateXYZ(0.0F, 3.1415927F, 1.5707964F);
         }

         if (var3 != null) {
            ModelAttachment var8 = var3.getAttachmentById("world");
            if (var8 != null) {
               ModelInstanceRenderData.makeAttachmentTransform(var8, WorldItemAtlas.s_attachmentXfrm);
               WorldItemAtlas.s_attachmentXfrm.invert();
               this.m_transform.mul(WorldItemAtlas.s_attachmentXfrm);
            }
         }

         ModelInstanceRenderData.postMultiplyMeshTransform(this.m_transform, var2.Mesh);
      }

      Boolean getFluidRatio(InventoryItem var1) {
         return var1.getFluidContainer().getFilledRatio() > 0.5F ? true : false;
      }

      FoodState getFoodState(Food var1) {
         FoodState var2 = WorldItemAtlas.ItemParams.FoodState.Normal;
         if (var1.isCooked()) {
            var2 = WorldItemAtlas.ItemParams.FoodState.Cooked;
         }

         if (var1.isBurnt()) {
            var2 = WorldItemAtlas.ItemParams.FoodState.Burnt;
         }

         if (var1.isRotten()) {
            var2 = WorldItemAtlas.ItemParams.FoodState.Rotten;
         }

         return var2;
      }

      boolean isStillValid(InventoryItem var1, boolean var2) {
         if (var1.worldScale == this.worldScale && (float)var1.worldZRotation == this.worldZRotation) {
            if (var2 != this.bMaxZoomIsOne) {
               return false;
            } else {
               if (var1.hasComponent(ComponentType.FluidContainer)) {
                  if (this.getFluidRatio(var1) != this.bFluidFilled) {
                     return false;
                  }

                  if (var1.getFluidContainer().getFilledRatio() != this.m_fluidLevel) {
                     return false;
                  }

                  if (var1.getFluidContainer().getColor() != this.fluidTint) {
                     return false;
                  }
               }

               Food var3 = (Food)Type.tryCastTo(var1, Food.class);
               if (var3 != null && this.getFoodState(var3) != this.m_foodState) {
                  return false;
               } else {
                  HandWeapon var4 = (HandWeapon)Type.tryCastTo(var1, HandWeapon.class);
                  return var4 == null || var4.getBloodLevel() == this.m_bloodLevel;
               }
            }
         } else {
            return false;
         }
      }

      void Reset() {
         this.m_model = null;
         this.m_foodState = WorldItemAtlas.ItemParams.FoodState.Normal;
         this.bFluidFilled = false;
         this.modelTextureName = null;
         this.fluidTextureMask = null;
         this.m_bloodLevel = 0.0F;
         this.m_fluidLevel = 0.0F;
         this.fluidTint = Color.white;
         if (this.m_weaponParts != null) {
            WorldItemAtlas.instance.weaponPartParamPool.release((List)this.m_weaponParts);
            this.m_weaponParts.clear();
         }

      }

      static enum FoodState {
         Normal,
         Cooked,
         Burnt,
         Rotten;

         private FoodState() {
         }
      }
   }

   private static final class Checksummer {
      private MessageDigest md;
      private final StringBuilder sb = new StringBuilder();

      private Checksummer() {
      }

      public void reset() throws NoSuchAlgorithmException {
         if (this.md == null) {
            this.md = MessageDigest.getInstance("MD5");
         }

         this.md.reset();
      }

      public void update(byte var1) {
         this.md.update(var1);
      }

      public void update(boolean var1) {
         this.md.update((byte)(var1 ? 1 : 0));
      }

      public void update(int var1) {
         this.md.update((byte)(var1 & 255));
         this.md.update((byte)(var1 >> 8 & 255));
         this.md.update((byte)(var1 >> 16 & 255));
         this.md.update((byte)(var1 >> 24 & 255));
      }

      public void update(String var1) {
         if (var1 != null && !var1.isEmpty()) {
            this.md.update(var1.getBytes());
         }
      }

      public void update(ImmutableColor var1) {
         this.update((byte)((int)(var1.r * 255.0F)));
         this.update((byte)((int)(var1.g * 255.0F)));
         this.update((byte)((int)(var1.b * 255.0F)));
      }

      public void update(IsoGridSquare.ResultLight var1, float var2, float var3, float var4) {
         if (var1 != null && var1.radius > 0) {
            this.update((int)((float)var1.x - var2));
            this.update((int)((float)var1.y - var3));
            this.update((int)((float)var1.z - var4));
            this.update((byte)((int)(var1.r * 255.0F)));
            this.update((byte)((int)(var1.g * 255.0F)));
            this.update((byte)((int)(var1.b * 255.0F)));
            this.update((byte)var1.radius);
         }
      }

      public String checksumToString() {
         byte[] var1 = this.md.digest();
         this.sb.setLength(0);

         for(int var2 = 0; var2 < var1.length; ++var2) {
            this.sb.append(var1[var2] & 255);
         }

         return this.sb.toString();
      }
   }

   public static final class ItemTexture {
      final ItemParams itemParams = new ItemParams();
      AtlasEntry entry;

      public ItemTexture() {
      }

      public boolean isStillValid(InventoryItem var1, boolean var2) {
         return this.entry == null ? false : this.itemParams.isStillValid(var1, var2);
      }

      public boolean isRenderMainOK() {
         return this.entry.bRenderMainOK;
      }

      public boolean isTooBig() {
         return this.entry.bTooBig;
      }

      public void render(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
         if (PerformanceSettings.FBORenderChunk) {
            ItemTextureDepthDrawer var11 = (ItemTextureDepthDrawer)WorldItemAtlas.instance.itemTextureDepthDrawerPool.alloc();
            var11.init(this, var1, var2, var3, var4, var5, var6, var7, var8, var9);
            SpriteRenderer.instance.drawGeneric(var11);
         } else {
            float var10 = 2.5F;
            if (PerformanceSettings.FBORenderChunk) {
               if (this.itemParams.bMaxZoomIsOne) {
                  var10 = 1.0F;
               }

               IndieGL.StartShader(0);
               IndieGL.enableDepthTest();
               IndieGL.glDepthFunc(515);
               if (!FBORenderChunkManager.instance.isCaching()) {
                  IndieGL.glBlendFunc(770, 771);
               }
            }

            SpriteRenderer.instance.m_states.getPopulatingActiveState().render(this.entry.tex, var4 - ((float)this.entry.w / 2.0F - this.entry.offsetX) / var10, var5 - ((float)this.entry.h / 2.0F - this.entry.offsetY) / var10, (float)this.entry.w / var10, (float)this.entry.h / var10, var6, var7, var8, var9, (Consumer)null);
            if (PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching()) {
               IndieGL.StartShader(SceneShaderStore.DefaultShader);
            }

         }
      }

      void Reset() {
         this.itemParams.Reset();
         this.entry = null;
      }
   }

   private static final class AtlasEntry {
      public Atlas atlas;
      public String key;
      public int x;
      public int y;
      public int w;
      public int h;
      public float offsetX;
      public float offsetY;
      public Texture tex;
      public boolean ready = false;
      public boolean bRenderMainOK = false;
      public boolean bTooBig = false;

      private AtlasEntry() {
      }

      public void Reset() {
         this.atlas = null;
         this.tex.destroy();
         this.tex = null;
         this.ready = false;
         this.bRenderMainOK = false;
         this.bTooBig = false;
      }
   }

   private static final class RenderJob extends TextureDraw.GenericDrawer {
      public final ItemParams itemParams = new ItemParams();
      public AtlasEntry entry;
      public int done = 0;
      public int renderRefCount;
      public boolean bClearThisSlotOnly;
      int entryW;
      int entryH;
      final int[] m_viewport = new int[4];
      final Matrix4f m_matri4f = new Matrix4f();
      final Matrix4f m_projection = new Matrix4f();
      final Matrix4f m_modelView = new Matrix4f();
      final Vector3f m_scenePos = new Vector3f();
      final float[] m_bounds = new float[4];
      static final Vector3f tempVector3f = new Vector3f(0.0F, 5.0F, -2.0F);
      static final Matrix4f tempMatrix4f_1 = new Matrix4f();
      static final Matrix4f tempMatrix4f_2 = new Matrix4f();
      static final float[] xs = new float[8];
      static final float[] ys = new float[8];

      private RenderJob() {
      }

      public static RenderJob getNew() {
         return WorldItemAtlas.JobPool.isEmpty() ? new RenderJob() : (RenderJob)WorldItemAtlas.JobPool.pop();
      }

      public RenderJob init(ItemParams var1, AtlasEntry var2) {
         this.itemParams.copyFrom(var1);
         this.entry = var2;
         this.bClearThisSlotOnly = false;
         this.entryW = 0;
         this.entryH = 0;
         this.done = 0;
         this.renderRefCount = 0;
         return this;
      }

      public boolean renderMain() {
         Model var1 = this.itemParams.m_model;
         return var1 != null && var1.isReady() && var1.Mesh != null && var1.Mesh.isReady();
      }

      public void render() {
         if (this.done != 1) {
            Model var1 = this.itemParams.m_model;
            if (var1 != null && var1.Mesh != null && var1.Mesh.isReady()) {
               float var2 = 0.0F;
               float var3 = 0.0F;
               this.calcMatrices(this.m_projection, this.m_modelView, var2, var3);
               this.calcModelBounds(this.m_bounds);
               this.calcModelOffset();
               this.calcEntrySize();
               if (this.entryW > 0 && this.entryH > 0) {
                  if (this.entryW <= 512 && this.entryH <= 512) {
                     WorldItemAtlas.instance.assignEntryToAtlas(this.entry, this.entryW, this.entryH);
                     GL11.glPushAttrib(1048575);
                     GL11.glPushClientAttrib(-1);
                     GL11.glDepthMask(true);
                     GL11.glColorMask(true, true, true, true);
                     GL11.glDisable(3089);
                     TextureFBO var4 = WorldItemAtlas.instance.fbo;
                     if (var4.getTexture() != this.entry.atlas.tex) {
                        var4.setTextureAndDepth(this.entry.atlas.tex, this.entry.atlas.depth);
                     }

                     var4.startDrawing(this.entry.atlas.clear, this.entry.atlas.clear);
                     if (this.entry.atlas.clear) {
                        this.entry.atlas.clear = false;
                     }

                     this.clearColorAndDepth();
                     int var5 = this.entry.x - (int)this.entry.offsetX - (1024 - this.entry.w) / 2;
                     int var6 = -((int)this.entry.offsetY) - (1024 - this.entry.h) / 2;
                     var6 += 512 - (this.entry.y + this.entry.h);
                     GL11.glViewport(var5, var6, 1024, 1024);
                     boolean var7 = false;
                     if (var7) {
                        GL11.glEnable(2929);
                        GL11.glDisable(2884);
                        GL11.glBlendFunc(770, 771);
                        PZGLUtil.pushAndLoadMatrix(5889, this.m_projection);
                        Matrix4f var8 = tempMatrix4f_1.set(this.m_modelView);
                        tempMatrix4f_2.set(this.itemParams.m_transform).invert();
                        var8.mul(tempMatrix4f_2);
                        Vector3f var9 = var8.getScale(tempVector3f);
                        tempMatrix4f_1.scale(1.0F / var9.x, 1.0F / var9.y, 1.0F / var9.z);
                        PZGLUtil.pushAndLoadMatrix(5888, var8);
                        VBORenderer var10 = VBORenderer.getInstance();
                        float var11 = 1.0F;
                        float var12 = 1.0F;
                        float var13 = 1.0F;
                        float var14 = 1.0F;
                        var10.setDepthTestForAllRuns(Boolean.TRUE);
                        float var15 = 0.035F;
                        if (this.itemParams.bMaxZoomIsOne) {
                           var15 /= 2.5F;
                        }

                        float var16 = var15 / 5.0F;
                        var10.addBox(var15, var16, var15, var11, var12, var13, var14, (ShaderProgram)null);
                        var10.flush();
                        var10.setDepthTestForAllRuns((Boolean)null);
                        GL11.glEnable(2884);
                        PZGLUtil.popMatrix(5889);
                        PZGLUtil.popMatrix(5888);
                     }

                     boolean var17 = this.renderModel(this.itemParams.m_model, (Matrix4f)null, false);
                     if (this.itemParams.m_weaponParts != null && !this.itemParams.m_weaponParts.isEmpty()) {
                        for(int var18 = 0; var18 < this.itemParams.m_weaponParts.size(); ++var18) {
                           WeaponPartParams var19 = (WeaponPartParams)this.itemParams.m_weaponParts.get(var18);
                           if (!this.renderModel(var19.m_model, var19.m_transform, true)) {
                              var17 = false;
                              break;
                           }
                        }
                     }

                     var4.endDrawing();
                     if (!var17) {
                        GL11.glPopAttrib();
                        GL11.glPopClientAttrib();
                     } else {
                        this.entry.ready = true;
                        this.done = 1;
                        Texture.lastTextureID = -1;
                        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                        SpriteRenderer.ringBuffer.restoreVBOs = true;
                        GL11.glPopAttrib();
                        GL11.glPopClientAttrib();
                     }
                  } else {
                     this.entry.bTooBig = true;
                     this.done = 1;
                  }
               }
            }
         }
      }

      public void postRender() {
         if (this.entry != null) {
            assert this.renderRefCount > 0;

            --this.renderRefCount;
         }
      }

      void clearColorAndDepth() {
         GL11.glEnable(3089);
         GL11.glScissor(this.entry.x, 512 - (this.entry.y + this.entry.h), this.entry.w, this.entry.h);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClearDepth(1.0);
         GL11.glClear(16640);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
         this.restoreScreenStencil();
         GL11.glDisable(3089);
      }

      void restoreScreenStencil() {
         int var1 = SpriteRenderer.instance.getRenderingPlayerIndex();
         int var2 = var1 != 0 && var1 != 2 ? Core.getInstance().getOffscreenTrueWidth() / 2 : 0;
         int var3 = var1 != 0 && var1 != 1 ? Core.getInstance().getOffscreenTrueHeight() / 2 : 0;
         int var4 = Core.getInstance().getOffscreenTrueWidth();
         int var5 = Core.getInstance().getOffscreenTrueHeight();
         if (IsoPlayer.numPlayers > 1) {
            var4 /= 2;
         }

         if (IsoPlayer.numPlayers > 2) {
            var5 /= 2;
         }

         GL11.glScissor(var2, var3, var4, var5);
      }

      boolean renderModel(Model var1, Matrix4f var2, boolean var3) {
         if (!var1.bStatic) {
            return false;
         } else {
            if (var1.Effect == null) {
               var1.CreateShader("basicEffect");
            }

            Shader var4 = var1.Effect;
            if (var4 != null && var1.Mesh != null && var1.Mesh.isReady()) {
               boolean var5 = !var3 && this.checkSmartTexture(this.itemParams.modelTextureName);
               if (var1.tex != null && !var1.tex.isReady()) {
                  return false;
               } else {
                  PZGLUtil.pushAndLoadMatrix(5889, this.m_projection);
                  Matrix4f var6 = tempMatrix4f_1.set(this.m_modelView);
                  Matrix4f var7 = tempMatrix4f_2.set(this.itemParams.m_transform).invert();
                  var6.mul(var7);
                  PZGLUtil.pushAndLoadMatrix(5888, var6);
                  GL11.glBlendFunc(770, 771);
                  GL11.glDepthFunc(513);
                  GL11.glDepthMask(true);
                  GL11.glDepthRange(0.0, 1.0);
                  GL11.glEnable(2929);
                  if (Core.bDebug && DebugOptions.instance.Model.Render.Wireframe.getValue()) {
                     GL11.glPolygonMode(1032, 6913);
                     GL11.glEnable(2848);
                     GL11.glLineWidth(0.75F);
                     Shader var11 = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", var1.bStatic, false);
                     if (var11 != null) {
                        var11.Start();
                        if (var1.bStatic) {
                           var11.setTransformMatrix(this.itemParams.m_transform, true);
                        }

                        var1.Mesh.Draw(var11);
                        var11.End();
                     }

                     GL11.glPolygonMode(1032, 6914);
                     GL11.glDisable(2848);
                     PZGLUtil.popMatrix(5889);
                     PZGLUtil.popMatrix(5888);
                     GLStateRenderThread.restore();
                     return true;
                  } else {
                     GL11.glColor3f(1.0F, 1.0F, 1.0F);
                     var4.Start();
                     if (var5) {
                        var4.setTexture(this.itemParams.smartTexture, "Texture", 0);
                     } else if (var1.tex == null) {
                        var4.setTexture(Texture.getErrorTexture(), "Texture", 0);
                     } else {
                        var4.setTexture(var1.tex, "Texture", 0);
                     }

                     var4.setDepthBias(0.0F);
                     var4.setTargetDepth(0.5F);
                     var4.setAmbient(this.itemParams.m_ambientR * 0.4F, this.itemParams.m_ambientG * 0.4F, this.itemParams.m_ambientB * 0.4F);
                     var4.setLightingAmount(1.0F);
                     var4.setHueShift(this.itemParams.m_hue);
                     var4.setTint(this.itemParams.m_tintR, this.itemParams.m_tintG, this.itemParams.m_tintB);
                     var4.setAlpha(this.itemParams.alpha);

                     for(int var8 = 0; var8 < 5; ++var8) {
                        var4.setLight(var8, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
                     }

                     Vector3f var10 = tempVector3f;
                     var10.x = 0.0F;
                     var10.y = 5.0F;
                     var10.z = -2.0F;
                     var10.rotateY(this.itemParams.m_angle.y * 0.017453292F);
                     float var9 = 1.5F;
                     var4.setLight(4, var10.x, var10.z, var10.y, this.itemParams.m_ambientR / 4.0F * var9, this.itemParams.m_ambientG / 4.0F * var9, this.itemParams.m_ambientB / 4.0F * var9, 5000.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
                     if (var2 == null) {
                        var4.setTransformMatrix(this.itemParams.m_transform, false);
                     } else {
                        tempMatrix4f_1.set(this.itemParams.m_transform);
                        tempMatrix4f_1.mul(var2);
                        var4.setTransformMatrix(tempMatrix4f_1, false);
                     }

                     var1.Mesh.Draw(var4);
                     var4.End();
                     if (var5) {
                        if (this.itemParams.smartTexture.result != null) {
                           TextureCombiner.instance.releaseTexture(this.itemParams.smartTexture.result);
                           this.itemParams.smartTexture.result = null;
                        }

                        this.itemParams.smartTexture.clear();
                     }

                     if (Core.bDebug && DebugOptions.instance.Model.Render.Axis.getValue()) {
                        Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.5F, 1.0F);
                     }

                     PZGLUtil.popMatrix(5889);
                     PZGLUtil.popMatrix(5888);
                     GLStateRenderThread.restore();
                     return true;
                  }
               }
            } else {
               return false;
            }
         }
      }

      boolean checkSmartTexture(String var1) {
         if (var1 == null) {
            return false;
         } else {
            boolean var2 = false;
            if (this.itemParams.m_bloodLevel > 0.0F) {
               var2 = true;
            }

            String var3 = null;
            if (this.itemParams.m_fluidLevel > 0.0F && this.itemParams.fluidTextureMask != null) {
               var2 = true;
               var3 = this.itemParams.fluidTextureMask;
            }

            if (var2) {
               this.itemParams.smartTexture.clear();
               this.itemParams.smartTexture.add(var1);
               if (this.itemParams.m_bloodLevel > 0.0F) {
                  this.itemParams.smartTexture.setBlood("media/textures/BloodTextures/BloodOverlayWeapon.png", "media/textures/BloodTextures/BloodOverlayWeaponMask.png", this.itemParams.m_bloodLevel, 300);
               }

               if (var3 != null) {
                  String var4 = "media/textures/FullAlpha.png";
                  if (Texture.getTexture(var3) != null) {
                     this.itemParams.smartTexture.setFluid(var3, var4, this.itemParams.m_fluidLevel, 300, this.itemParams.fluidTint);
                  }
               }

               this.itemParams.smartTexture.calculate();
            }

            return var2;
         }
      }

      void calcMatrices(Matrix4f var1, Matrix4f var2, float var3, float var4) {
         var1.setOrtho(-0.26666668F, 0.26666668F, 0.26666668F, -0.26666668F, -10.0F, 10.0F);
         float var5 = 2.5F;
         if (this.itemParams.bMaxZoomIsOne) {
            var5 = 1.0F;
         }

         var2.identity();
         var2.scale(Core.scale * (float)Core.TileScale / 2.0F);
         boolean var6 = true;
         if (var6) {
            var2.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
            var2.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         } else {
            var2.rotate(1.5707964F, 0.0F, 1.0F, 0.0F);
         }

         var2.scale(-1.5F * var5, 1.5F * var5, 1.5F * var5);
         var2.rotateXYZ(this.itemParams.m_angle.x * 0.017453292F, this.itemParams.m_angle.y * 0.017453292F, this.itemParams.m_angle.z * 0.017453292F);
         var2.translate(var3, 0.0F, var4);
         var2.mul(this.itemParams.m_transform);
      }

      void calcModelBounds(float[] var1) {
         var1[0] = 3.4028235E38F;
         var1[1] = 3.4028235E38F;
         var1[2] = -3.4028235E38F;
         var1[3] = -3.4028235E38F;
         this.calcModelBounds(this.itemParams.m_model, this.m_modelView, var1);
         if (this.itemParams.m_weaponParts != null) {
            for(int var2 = 0; var2 < this.itemParams.m_weaponParts.size(); ++var2) {
               WeaponPartParams var3 = (WeaponPartParams)this.itemParams.m_weaponParts.get(var2);
               Matrix4f var4 = tempMatrix4f_1.set(this.m_modelView).mul(var3.m_transform);
               this.calcModelBounds(var3.m_model, var4, var1);
            }
         }

         float var5 = 2.0F;
         var1[0] *= var5;
         var1[1] *= var5;
         var1[2] *= var5;
         var1[3] *= var5;
      }

      void calcModelBounds(Model var1, Matrix4f var2, float[] var3) {
         Vector3f var4 = var1.Mesh.minXYZ;
         Vector3f var5 = var1.Mesh.maxXYZ;
         xs[0] = var4.x;
         ys[0] = var4.y;
         xs[1] = var4.x;
         ys[1] = var5.y;
         xs[2] = var5.x;
         ys[2] = var5.y;
         xs[3] = var5.x;
         ys[3] = var4.y;

         for(int var6 = 0; var6 < 4; ++var6) {
            this.sceneToUI(xs[var6], ys[var6], var4.z, this.m_projection, var2, this.m_scenePos);
            var3[0] = PZMath.min(var3[0], this.m_scenePos.x);
            var3[2] = PZMath.max(var3[2], this.m_scenePos.x);
            var3[1] = PZMath.min(var3[1], this.m_scenePos.y);
            var3[3] = PZMath.max(var3[3], this.m_scenePos.y);
            this.sceneToUI(xs[var6], ys[var6], var5.z, this.m_projection, var2, this.m_scenePos);
            var3[0] = PZMath.min(var3[0], this.m_scenePos.x);
            var3[2] = PZMath.max(var3[2], this.m_scenePos.x);
            var3[1] = PZMath.min(var3[1], this.m_scenePos.y);
            var3[3] = PZMath.max(var3[3], this.m_scenePos.y);
         }

      }

      void calcModelOffset() {
         float var1 = this.m_bounds[0];
         float var2 = this.m_bounds[1];
         float var3 = this.m_bounds[2];
         float var4 = this.m_bounds[3];
         this.entry.offsetX = var1 + (var3 - var1) / 2.0F - 512.0F;
         this.entry.offsetY = var2 + (var4 - var2) / 2.0F - 512.0F;
      }

      void calcEntrySize() {
         float var1 = this.m_bounds[0];
         float var2 = this.m_bounds[1];
         float var3 = this.m_bounds[2];
         float var4 = this.m_bounds[3];
         float var5 = 2.0F;
         var1 -= var5;
         var2 -= var5;
         var3 += var5;
         var4 += var5;
         byte var6 = 16;
         var1 = (float)Math.floor((double)(var1 / (float)var6)) * (float)var6;
         var3 = (float)Math.ceil((double)(var3 / (float)var6)) * (float)var6;
         var2 = (float)Math.floor((double)(var2 / (float)var6)) * (float)var6;
         var4 = (float)Math.ceil((double)(var4 / (float)var6)) * (float)var6;
         this.entryW = (int)(var3 - var1);
         this.entryH = (int)(var4 - var2);
      }

      Vector3f sceneToUI(float var1, float var2, float var3, Matrix4f var4, Matrix4f var5, Vector3f var6) {
         Matrix4f var7 = this.m_matri4f;
         var7.set(var4);
         var7.mul(var5);
         this.m_viewport[0] = 0;
         this.m_viewport[1] = 0;
         this.m_viewport[2] = 512;
         this.m_viewport[3] = 512;
         var7.project(var1, var2, var3, this.m_viewport, var6);
         return var6;
      }

      public void Reset() {
         this.itemParams.Reset();
         this.entry = null;
      }
   }

   private final class Atlas {
      public final int ENTRY_WID;
      public final int ENTRY_HGT;
      public Texture tex;
      public Texture depth;
      public final ArrayList<AtlasEntry> EntryList = new ArrayList();
      public boolean clear = true;

      public Atlas(int var2, int var3, int var4, int var5) {
         this.ENTRY_WID = var4;
         this.ENTRY_HGT = var5;
         this.tex = new Texture(var2, var3, 16);
         this.depth = new Texture(var2, var3, 512);
         if (WorldItemAtlas.this.fbo == null) {
            WorldItemAtlas.this.fbo = new TextureFBO(this.tex, this.depth, false);
         }
      }

      public boolean isFull() {
         int var1 = this.tex.getWidth() / this.ENTRY_WID;
         int var2 = this.tex.getHeight() / this.ENTRY_HGT;
         return this.EntryList.size() >= var1 * var2;
      }

      public AtlasEntry addItem(String var1) {
         int var2 = this.tex.getWidth() / this.ENTRY_WID;
         int var3 = this.EntryList.size();
         int var4 = var3 % var2;
         int var5 = var3 / var2;
         AtlasEntry var6 = new AtlasEntry();
         var6.atlas = this;
         var6.key = var1;
         var6.x = var4 * this.ENTRY_WID;
         var6.y = var5 * this.ENTRY_HGT;
         var6.w = this.ENTRY_WID;
         var6.h = this.ENTRY_HGT;
         var6.tex = this.tex.split(var1, var6.x, this.tex.getHeight() - (var6.y + this.ENTRY_HGT), var6.w, var6.h);
         var6.tex.setName(var1);
         this.EntryList.add(var6);
         return var6;
      }

      public void addEntry(AtlasEntry var1) {
         int var2 = this.tex.getWidth() / this.ENTRY_WID;
         int var3 = this.EntryList.size();
         int var4 = var3 % var2;
         int var5 = var3 / var2;
         var1.atlas = this;
         var1.x = var4 * this.ENTRY_WID;
         var1.y = var5 * this.ENTRY_HGT;
         var1.w = this.ENTRY_WID;
         var1.h = this.ENTRY_HGT;
         var1.tex = this.tex.split(var1.key, var1.x, this.tex.getHeight() - (var1.y + this.ENTRY_HGT), var1.w, var1.h);
         var1.tex.setName(var1.key);
         this.EntryList.add(var1);
      }

      public void Reset() {
         this.EntryList.forEach(AtlasEntry::Reset);
         this.EntryList.clear();
         if (!this.tex.isDestroyed()) {
            RenderThread.invokeOnRenderContext(() -> {
               GL11.glDeleteTextures(this.tex.getID());
            });
         }

         this.tex = null;
         if (!this.depth.isDestroyed()) {
            RenderThread.invokeOnRenderContext(() -> {
               GL11.glDeleteTextures(this.depth.getID());
            });
         }

         this.depth = null;
      }
   }

   private static final class WeaponPartParams {
      Model m_model;
      String m_attachmentNameSelf;
      String m_attachmentNameParent;
      final Matrix4f m_transform = new Matrix4f();

      private WeaponPartParams() {
      }

      WeaponPartParams init(WeaponPartParams var1) {
         this.m_model = var1.m_model;
         this.m_attachmentNameSelf = var1.m_attachmentNameSelf;
         this.m_attachmentNameParent = var1.m_attachmentNameParent;
         this.m_transform.set(var1.m_transform);
         return this;
      }

      void initTransform(ModelScript var1, ModelScript var2) {
         this.m_transform.identity();
         Matrix4f var3 = WorldItemAtlas.s_attachmentXfrm;
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

   private static final class ClearAtlasTexture extends TextureDraw.GenericDrawer {
      Atlas m_atlas;

      ClearAtlasTexture(Atlas var1) {
         this.m_atlas = var1;
      }

      public void render() {
         TextureFBO var1 = WorldItemAtlas.instance.fbo;
         if (var1 != null && this.m_atlas.tex != null) {
            if (this.m_atlas.clear) {
               if (var1.getTexture() != this.m_atlas.tex) {
                  var1.setTexture(this.m_atlas.tex);
               }

               var1.startDrawing(false, false);
               GL11.glPushAttrib(2048);
               GL11.glViewport(0, 0, var1.getWidth(), var1.getHeight());
               Matrix4f var2 = Core.getInstance().projectionMatrixStack.alloc();
               int var3 = this.m_atlas.tex.getWidth();
               int var4 = this.m_atlas.tex.getHeight();
               var2.setOrtho2D(0.0F, (float)var3, (float)var4, 0.0F);
               Core.getInstance().projectionMatrixStack.push(var2);
               Matrix4f var5 = Core.getInstance().modelViewMatrixStack.alloc();
               var5.identity();
               Core.getInstance().modelViewMatrixStack.push(var5);
               GL11.glDisable(3089);
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GL11.glClear(16640);
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
               var1.endDrawing();
               GL11.glEnable(3089);
               Core.getInstance().projectionMatrixStack.pop();
               Core.getInstance().modelViewMatrixStack.pop();
               GL11.glPopAttrib();
               this.m_atlas.clear = false;
            }
         }
      }
   }

   private static final class ItemTextureDepthDrawer extends TextureDraw.GenericDrawer {
      ItemTexture itemTexture;
      float ox;
      float oy;
      float oz;
      float x;
      float y;
      float r;
      float g;
      float b;
      float a;

      private ItemTextureDepthDrawer() {
      }

      ItemTextureDepthDrawer init(ItemTexture var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
         this.itemTexture = var1;
         this.ox = var2;
         this.oy = var3;
         this.oz = var4;
         this.x = var5;
         this.y = var6;
         this.r = var7;
         this.g = var8;
         this.b = var9;
         this.a = var10;
         return this;
      }

      public void render() {
         AtlasEntry var1 = this.itemTexture.entry;
         if (var1 != null && var1.ready && var1.tex.isReady()) {
            if (DeadBodyAtlas.DeadBodyAtlasShader == null) {
               DeadBodyAtlas.DeadBodyAtlasShader = new DeadBodyAtlas.DeadBodyAtlasShader("DeadBodyAtlas");
            }

            if (DeadBodyAtlas.DeadBodyAtlasShader.getShaderProgram().isCompiled()) {
               float var2 = 2.5F;
               if (this.itemTexture.itemParams.bMaxZoomIsOne) {
                  var2 = 1.0F;
               }

               float var3 = this.x - ((float)var1.w / 2.0F - var1.offsetX) / var2;
               float var4 = this.y - ((float)var1.h / 2.0F - var1.offsetY) / var2;
               float var5 = (float)var1.w / var2;
               float var6 = (float)var1.h / var2;
               GL13.glActiveTexture(33985);
               GL11.glEnable(3553);
               GL11.glBindTexture(3553, var1.atlas.depth.getID());
               GL13.glActiveTexture(33984);
               GL11.glDepthMask(true);
               GL11.glDepthFunc(515);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               float var7 = (Float)Core.getInstance().FloatParamMap.get(0);
               float var8 = (Float)Core.getInstance().FloatParamMap.get(1);
               int var9 = SpriteRenderer.instance.getRenderingPlayerIndex();
               PlayerCamera var10 = SpriteRenderer.instance.getRenderingPlayerCamera(var9);
               float var11 = var10.fixJigglyModelsSquareX;
               float var12 = var10.fixJigglyModelsSquareY;
               float var13 = var1.offsetX;
               float var14 = var1.offsetY;
               float var15 = -(var13 + 2.0F * var14) / (64.0F * (float)Core.TileScale);
               float var16 = -(var13 - 2.0F * var14) / (-64.0F * (float)Core.TileScale);
               var16 = 0.0F;
               var15 = 0.0F;
               float var17 = 137.0F;
               if (this.itemTexture.itemParams.bMaxZoomIsOne) {
                  var17 *= 2.5F;
               }

               float var18 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var7), PZMath.fastfloor(var8), this.ox + var11 + var15 + 0.5F * var17, this.oy + var12 + var16 + 0.5F * var17, this.oz + 1.0F).depthStart;
               float var19 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var7), PZMath.fastfloor(var8), this.ox + var11 + var15 - 0.5F * var17, this.oy + var12 + var16 - 0.5F * var17, this.oz).depthStart;
               float var20 = 0.00145F;
               if (this.itemTexture.itemParams.bMaxZoomIsOne) {
                  var20 *= 1.0F;
               }

               var18 += var20;
               var19 += var20;
               VBORenderer var21 = VBORenderer.getInstance();
               var21.startRun(var21.FORMAT_PositionColorUV);
               var21.setMode(7);
               var21.setDepthTest(true);
               var21.setShaderProgram(DeadBodyAtlas.DeadBodyAtlasShader.getShaderProgram());
               Texture var22 = var1.tex;
               var21.setTextureID(var22.getTextureId());
               var21.cmdUseProgram(DeadBodyAtlas.DeadBodyAtlasShader.getShaderProgram());
               var21.cmdShader1f("zDepthBlendZ", var18);
               var21.cmdShader1f("zDepthBlendToZ", var19);
               var21.addQuad(var3, var4, var22.getXStart(), var22.getYStart(), var3 + var5, var4 + var6, var22.getXEnd(), var22.getYEnd(), 0.0F, this.r, this.g, this.b, this.a);
               var21.endRun();
               var21.flush();
               GL13.glActiveTexture(33985);
               GL11.glBindTexture(3553, 0);
               GL11.glDisable(3553);
               GL13.glActiveTexture(33984);
               ShaderHelper.glUseProgramObjectARB(0);
               Texture.lastTextureID = -1;
               SpriteRenderer.ringBuffer.restoreBoundTextures = true;
               GLStateRenderThread.restore();
            }
         }
      }

      public void postRender() {
         this.itemTexture = null;
         WorldItemAtlas.instance.itemTextureDepthDrawerPool.release((Object)this);
      }
   }

   private static final class ItemTextureDrawer extends TextureDraw.GenericDrawer {
      ItemTexture itemTexture;
      float x;
      float y;
      float r;
      float g;
      float b;
      float a;

      private ItemTextureDrawer() {
      }

      ItemTextureDrawer init(ItemTexture var1, float var2, float var3, float var4, float var5, float var6, float var7) {
         this.itemTexture = var1;
         this.x = var2;
         this.y = var3;
         this.r = var4;
         this.g = var5;
         this.b = var6;
         this.a = var7;
         return this;
      }

      public void render() {
         AtlasEntry var1 = this.itemTexture.entry;
         if (var1 != null && var1.ready && var1.tex.isReady()) {
            float var2 = 2.5F;
            if (this.itemTexture.itemParams.bMaxZoomIsOne) {
               var2 = 1.0F;
            }

            int var3 = (int)(this.x - ((float)var1.w / 2.0F - var1.offsetX) / var2);
            int var4 = (int)(this.y - ((float)var1.h / 2.0F - var1.offsetY) / var2);
            int var5 = (int)((float)var1.w / var2);
            int var6 = (int)((float)var1.h / var2);
            var1.tex.bind();
            GL11.glBegin(7);
            GL11.glColor4f(this.r, this.g, this.b, this.a);
            GL11.glTexCoord2f(var1.tex.xStart, var1.tex.yStart);
            GL11.glVertex2i(var3, var4);
            GL11.glTexCoord2f(var1.tex.xEnd, var1.tex.yStart);
            GL11.glVertex2i(var3 + var5, var4);
            GL11.glTexCoord2f(var1.tex.xEnd, var1.tex.yEnd);
            GL11.glVertex2i(var3 + var5, var4 + var6);
            GL11.glTexCoord2f(var1.tex.xStart, var1.tex.yEnd);
            GL11.glVertex2i(var3, var4 + var6);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnd();
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
         }
      }

      public void postRender() {
         this.itemTexture = null;
         WorldItemAtlas.instance.itemTextureDrawerPool.release((Object)this);
      }
   }
}
