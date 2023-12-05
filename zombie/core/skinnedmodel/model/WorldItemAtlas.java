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
import org.lwjgl.util.glu.GLU;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
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
   private final ObjectPool<WeaponPartParams> weaponPartParamPool = new ObjectPool(WeaponPartParams::new);
   private final ArrayList<WeaponPart> m_tempWeaponPartList = new ArrayList();
   private static final Matrix4f s_attachmentXfrm = new Matrix4f();
   private static final ImmutableColor ROTTEN_FOOD_COLOR = new ImmutableColor(0.5F, 0.5F, 0.5F);

   public WorldItemAtlas() {
   }

   public ItemTexture getItemTexture(InventoryItem var1) {
      return this.itemParams.init(var1) ? this.getItemTexture(this.itemParams) : null;
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
      if (DebugOptions.instance.WorldItemAtlasRender.getValue() && GameKeyboard.isKeyPressed(209)) {
         this.Reset();
      }

      if (DebugOptions.instance.WorldItemAtlasRender.getValue()) {
         int var1 = 512 / Core.TileScale;
         var1 /= 2;
         int var2 = 0;
         int var3 = 0;

         for(int var4 = 0; var4 < this.AtlasList.size(); ++var4) {
            Atlas var5 = (Atlas)this.AtlasList.get(var4);
            SpriteRenderer.instance.renderi((Texture)null, var2, var3, var1, var1, 1.0F, 1.0F, 1.0F, 0.75F, (Consumer)null);
            SpriteRenderer.instance.renderi(var5.tex, var2, var3, var1, var1, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
            float var6 = (float)var1 / (float)var5.tex.getWidth();

            int var7;
            for(var7 = 0; var7 <= var5.tex.getWidth() / var5.ENTRY_WID; ++var7) {
               SpriteRenderer.instance.renderline((Texture)null, (int)((float)var2 + (float)(var7 * var5.ENTRY_WID) * var6), var3, (int)((float)var2 + (float)(var7 * var5.ENTRY_WID) * var6), var3 + var1, 0.5F, 0.5F, 0.5F, 1.0F);
            }

            for(var7 = 0; var7 <= var5.tex.getHeight() / var5.ENTRY_HGT; ++var7) {
               SpriteRenderer.instance.renderline((Texture)null, var2, (int)((float)(var3 + var1) - (float)(var7 * var5.ENTRY_HGT) * var6), var2 + var1, (int)((float)(var3 + var1) - (float)(var7 * var5.ENTRY_HGT) * var6), 0.5F, 0.5F, 0.5F, 1.0F);
            }

            var3 += var1;
            if (var3 + var1 > Core.getInstance().getScreenHeight()) {
               var3 = 0;
               var2 += var1;
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

      ItemParams() {
         this.m_foodState = WorldItemAtlas.ItemParams.FoodState.Normal;
         this.m_angle = new Vector3f();
         this.m_transform = new Matrix4f();
         this.m_ambientR = 1.0F;
         this.m_ambientG = 1.0F;
         this.m_ambientB = 1.0F;
         this.alpha = 1.0F;
      }

      void copyFrom(ItemParams var1) {
         this.worldScale = var1.worldScale;
         this.worldZRotation = var1.worldZRotation;
         this.m_foodState = var1.m_foodState;
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
      }

      boolean init(InventoryItem var1) {
         this.Reset();
         this.worldScale = var1.worldScale;
         this.worldZRotation = (float)var1.worldZRotation;
         float var2 = 0.0F;
         String var3 = StringUtils.discardNullOrWhitespace(var1.getWorldStaticItem());
         String var19;
         if (var3 != null) {
            ModelScript var16 = ScriptManager.instance.getModelScript(var3);
            if (var16 == null) {
               return false;
            } else {
               String var18 = var16.getMeshName();
               var19 = var16.getTextureName();
               String var21 = var16.getShaderName();
               ImmutableColor var22 = ImmutableColor.white;
               float var23 = 1.0F;
               Food var25 = (Food)Type.tryCastTo(var1, Food.class);
               if (var25 != null) {
                  this.m_foodState = this.getFoodState(var25);
                  ModelScript var27;
                  if (var25.isCooked()) {
                     var27 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Cooked");
                     if (var27 != null) {
                        var19 = var27.getTextureName();
                        var18 = var27.getMeshName();
                        var21 = var27.getShaderName();
                        var16 = var27;
                     }
                  }

                  if (var25.isBurnt()) {
                     var27 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Burnt");
                     if (var27 != null) {
                        var19 = var27.getTextureName();
                        var18 = var27.getMeshName();
                        var21 = var27.getShaderName();
                        var16 = var27;
                     }
                  }

                  if (var25.isRotten()) {
                     var27 = ScriptManager.instance.getModelScript(var1.getWorldStaticItem() + "Rotten");
                     if (var27 != null) {
                        var19 = var27.getTextureName();
                        var18 = var27.getMeshName();
                        var21 = var27.getShaderName();
                        var16 = var27;
                     } else {
                        var22 = WorldItemAtlas.ROTTEN_FOOD_COLOR;
                     }
                  }
               }

               Clothing var28 = (Clothing)Type.tryCastTo(var1, Clothing.class);
               if (var28 != null || var1.getClothingItem() != null) {
                  String var29 = var16.getTextureName(true);
                  ItemVisual var30 = var1.getVisual();
                  ClothingItem var32 = var1.getClothingItem();
                  ImmutableColor var15 = var30.getTint(var32);
                  if (var29 == null) {
                     if (var32.textureChoices.isEmpty()) {
                        var29 = var30.getBaseTexture(var32);
                     } else {
                        var29 = var30.getTextureChoice(var32);
                     }
                  }

                  if (var29 != null) {
                     var19 = var29;
                     var22 = var15;
                  }
               }

               boolean var33 = var16.bStatic;
               Model var31 = ModelManager.instance.tryGetLoadedModel(var18, var19, var33, var21, true);
               if (var31 == null) {
                  ModelManager.instance.loadAdditionalModel(var18, var19, var33, var21);
               }

               var31 = ModelManager.instance.getLoadedModel(var18, var19, var33, var21);
               if (var31 != null && var31.isReady() && var31.Mesh != null && var31.Mesh.isReady()) {
                  this.init(var1, var31, var16, var23, var22, var2, false);
                  if (this.worldScale != 1.0F) {
                     this.m_transform.scale(var16.scale * this.worldScale);
                  } else if (var16.scale != 1.0F) {
                     this.m_transform.scale(var16.scale);
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
            Clothing var4 = (Clothing)Type.tryCastTo(var1, Clothing.class);
            String var8;
            String var9;
            Model var12;
            float var13;
            ImmutableColor var14;
            if (var4 == null) {
               HandWeapon var17 = (HandWeapon)Type.tryCastTo(var1, HandWeapon.class);
               if (var17 != null) {
                  var19 = StringUtils.discardNullOrWhitespace(var17.getStaticModel());
                  if (var19 == null) {
                     return false;
                  } else {
                     ModelScript var20 = ScriptManager.instance.getModelScript(var19);
                     if (var20 == null) {
                        return false;
                     } else {
                        var8 = var20.getMeshName();
                        var9 = var20.getTextureName();
                        String var24 = var20.getShaderName();
                        boolean var26 = var20.bStatic;
                        var12 = ModelManager.instance.tryGetLoadedModel(var8, var9, var26, var24, false);
                        if (var12 == null) {
                           ModelManager.instance.loadAdditionalModel(var8, var9, var26, var24);
                        }

                        var12 = ModelManager.instance.getLoadedModel(var8, var9, var26, var24);
                        if (var12 != null && var12.isReady() && var12.Mesh != null && var12.Mesh.isReady()) {
                           var13 = 1.0F;
                           var14 = ImmutableColor.white;
                           this.init(var1, var12, var20, var13, var14, var2, true);
                           if (this.worldScale != 1.0F) {
                              this.m_transform.scale(var20.scale * this.worldScale);
                           } else if (var20.scale != 1.0F) {
                              this.m_transform.scale(var20.scale);
                           }

                           this.m_angle.x = 0.0F;
                           this.m_angle.y = this.worldZRotation;
                           return this.initWeaponParts(var17, var20);
                        } else {
                           return false;
                        }
                     }
                  }
               } else {
                  return false;
               }
            } else {
               ClothingItem var5 = var1.getClothingItem();
               ItemVisual var6 = var1.getVisual();
               boolean var7 = false;
               var8 = var5.getModel(var7);
               if (var5 != null && var6 != null && !StringUtils.isNullOrWhitespace(var8) && "Bip01_Head".equalsIgnoreCase(var5.m_AttachBone) && (!var4.isCosmetic() || "Eyes".equals(var1.getBodyLocation()))) {
                  var9 = var6.getTextureChoice(var5);
                  boolean var10 = var5.m_Static;
                  String var11 = var5.m_Shader;
                  var12 = ModelManager.instance.tryGetLoadedModel(var8, var9, var10, var11, false);
                  if (var12 == null) {
                     ModelManager.instance.loadAdditionalModel(var8, var9, var10, var11);
                  }

                  var12 = ModelManager.instance.getLoadedModel(var8, var9, var10, var11);
                  if (var12 != null && var12.isReady() && var12.Mesh != null && var12.Mesh.isReady()) {
                     var13 = var6.getHue(var5);
                     var14 = var6.getTint(var5);
                     this.init(var1, var12, (ModelScript)null, var13, var14, var2, false);
                     this.m_angle.x = 180.0F + var2;
                     this.m_angle.y = this.worldZRotation;
                     this.m_angle.z = -90.0F;
                     this.m_transform.translate(-0.08F, 0.0F, 0.05F);
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            }
         }
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

         if (var2.Mesh != null && var2.Mesh.isReady() && var2.Mesh.m_transform != null) {
            var2.Mesh.m_transform.transpose();
            this.m_transform.mul(var2.Mesh.m_transform);
            var2.Mesh.m_transform.transpose();
         }

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

      boolean isStillValid(InventoryItem var1) {
         if (var1.worldScale == this.worldScale && (float)var1.worldZRotation == this.worldZRotation) {
            Food var2 = (Food)Type.tryCastTo(var1, Food.class);
            return var2 == null || this.getFoodState(var2) == this.m_foodState;
         } else {
            return false;
         }
      }

      void Reset() {
         this.m_model = null;
         this.m_foodState = WorldItemAtlas.ItemParams.FoodState.Normal;
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

      public boolean isStillValid(InventoryItem var1) {
         return this.entry == null ? false : this.itemParams.isStillValid(var1);
      }

      public boolean isRenderMainOK() {
         return this.entry.bRenderMainOK;
      }

      public boolean isTooBig() {
         return this.entry.bTooBig;
      }

      public void render(float var1, float var2, float var3, float var4, float var5, float var6) {
         if (this.entry.ready && this.entry.tex.isReady()) {
            SpriteRenderer.instance.m_states.getPopulatingActiveState().render(this.entry.tex, var1 - ((float)this.entry.w / 2.0F - this.entry.offsetX) / 2.5F, var2 - ((float)this.entry.h / 2.0F - this.entry.offsetY) / 2.5F, (float)this.entry.w / 2.5F, (float)this.entry.h / 2.5F, var3, var4, var5, var6, (Consumer)null);
         } else {
            SpriteRenderer.instance.drawGeneric(((ItemTextureDrawer)WorldItemAtlas.instance.itemTextureDrawerPool.alloc()).init(this, var1, var2, var3, var4, var5, var6));
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
                        var4.setTexture(this.entry.atlas.tex);
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
                     boolean var7 = this.renderModel(this.itemParams.m_model, (Matrix4f)null);
                     if (this.itemParams.m_weaponParts != null && !this.itemParams.m_weaponParts.isEmpty()) {
                        for(int var8 = 0; var8 < this.itemParams.m_weaponParts.size(); ++var8) {
                           WeaponPartParams var9 = (WeaponPartParams)this.itemParams.m_weaponParts.get(var8);
                           if (!this.renderModel(var9.m_model, var9.m_transform)) {
                              var7 = false;
                              break;
                           }
                        }
                     }

                     var4.endDrawing();
                     if (!var7) {
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

      boolean renderModel(Model var1, Matrix4f var2) {
         if (!var1.bStatic) {
            return false;
         } else {
            if (var1.Effect == null) {
               var1.CreateShader("basicEffect");
            }

            Shader var3 = var1.Effect;
            if (var3 != null && var1.Mesh != null && var1.Mesh.isReady()) {
               if (var1.tex != null && !var1.tex.isReady()) {
                  return false;
               } else {
                  PZGLUtil.pushAndLoadMatrix(5889, this.m_projection);
                  Matrix4f var4 = tempMatrix4f_1.set(this.m_modelView);
                  Matrix4f var5 = tempMatrix4f_2.set(this.itemParams.m_transform).invert();
                  var4.mul(var5);
                  PZGLUtil.pushAndLoadMatrix(5888, var4);
                  GL11.glBlendFunc(770, 771);
                  GL11.glDepthFunc(513);
                  GL11.glDepthMask(true);
                  GL11.glDepthRange(0.0, 1.0);
                  GL11.glEnable(2929);
                  GL11.glColor3f(1.0F, 1.0F, 1.0F);
                  var3.Start();
                  if (var1.tex == null) {
                     var3.setTexture(Texture.getErrorTexture(), "Texture", 0);
                  } else {
                     var3.setTexture(var1.tex, "Texture", 0);
                  }

                  var3.setDepthBias(0.0F);
                  var3.setAmbient(this.itemParams.m_ambientR * 0.4F, this.itemParams.m_ambientG * 0.4F, this.itemParams.m_ambientB * 0.4F);
                  var3.setLightingAmount(1.0F);
                  var3.setHueShift(this.itemParams.m_hue);
                  var3.setTint(this.itemParams.m_tintR, this.itemParams.m_tintG, this.itemParams.m_tintB);
                  var3.setAlpha(this.itemParams.alpha);

                  for(int var6 = 0; var6 < 5; ++var6) {
                     var3.setLight(var6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
                  }

                  Vector3f var8 = tempVector3f;
                  var8.x = 0.0F;
                  var8.y = 5.0F;
                  var8.z = -2.0F;
                  var8.rotateY(this.itemParams.m_angle.y * 0.017453292F);
                  float var7 = 1.5F;
                  var3.setLight(4, var8.x, var8.z, var8.y, this.itemParams.m_ambientR / 4.0F * var7, this.itemParams.m_ambientG / 4.0F * var7, this.itemParams.m_ambientB / 4.0F * var7, 5000.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
                  if (var2 == null) {
                     var3.setTransformMatrix(this.itemParams.m_transform, false);
                  } else {
                     tempMatrix4f_1.set(this.itemParams.m_transform);
                     tempMatrix4f_1.mul(var2);
                     var3.setTransformMatrix(tempMatrix4f_1, false);
                  }

                  var1.Mesh.Draw(var3);
                  var3.End();
                  if (Core.bDebug && DebugOptions.instance.ModelRenderAxis.getValue()) {
                     Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.5F, 1.0F);
                  }

                  PZGLUtil.popMatrix(5889);
                  PZGLUtil.popMatrix(5888);
                  return true;
               }
            } else {
               return false;
            }
         }
      }

      void calcMatrices(Matrix4f var1, Matrix4f var2, float var3, float var4) {
         var1.setOrtho(-0.26666668F, 0.26666668F, 0.26666668F, -0.26666668F, -10.0F, 10.0F);
         var2.identity();
         float var5 = 0.047085002F;
         var2.scale(var5 * (float)Core.TileScale / 2.0F);
         boolean var6 = true;
         if (var6) {
            var2.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
            var2.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         } else {
            var2.rotate(1.5707964F, 0.0F, 1.0F, 0.0F);
         }

         var2.scale(-3.75F, 3.75F, 3.75F);
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
      public final ArrayList<AtlasEntry> EntryList = new ArrayList();
      public boolean clear = true;

      public Atlas(int var2, int var3, int var4, int var5) {
         this.ENTRY_WID = var4;
         this.ENTRY_HGT = var5;
         this.tex = new Texture(var2, var3, 16);
         if (WorldItemAtlas.this.fbo == null) {
            WorldItemAtlas.this.fbo = new TextureFBO(this.tex, false);
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
            var3.invert();
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
               GL11.glMatrixMode(5889);
               GL11.glPushMatrix();
               GL11.glLoadIdentity();
               int var2 = this.m_atlas.tex.getWidth();
               int var3 = this.m_atlas.tex.getHeight();
               GLU.gluOrtho2D(0.0F, (float)var2, (float)var3, 0.0F);
               GL11.glMatrixMode(5888);
               GL11.glPushMatrix();
               GL11.glLoadIdentity();
               GL11.glDisable(3089);
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GL11.glClear(16640);
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
               var1.endDrawing();
               GL11.glEnable(3089);
               GL11.glMatrixMode(5889);
               GL11.glPopMatrix();
               GL11.glMatrixMode(5888);
               GL11.glPopMatrix();
               GL11.glPopAttrib();
               this.m_atlas.clear = false;
            }
         }
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
            int var2 = (int)(this.x - ((float)var1.w / 2.0F - var1.offsetX) / 2.5F);
            int var3 = (int)(this.y - ((float)var1.h / 2.0F - var1.offsetY) / 2.5F);
            int var4 = (int)((float)var1.w / 2.5F);
            int var5 = (int)((float)var1.h / 2.5F);
            var1.tex.bind();
            GL11.glBegin(7);
            GL11.glColor4f(this.r, this.g, this.b, this.a);
            GL11.glTexCoord2f(var1.tex.xStart, var1.tex.yStart);
            GL11.glVertex2i(var2, var3);
            GL11.glTexCoord2f(var1.tex.xEnd, var1.tex.yStart);
            GL11.glVertex2i(var2 + var4, var3);
            GL11.glTexCoord2f(var1.tex.xEnd, var1.tex.yEnd);
            GL11.glVertex2i(var2 + var4, var3 + var5);
            GL11.glTexCoord2f(var1.tex.xStart, var1.tex.yEnd);
            GL11.glVertex2i(var2, var3 + var5);
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
