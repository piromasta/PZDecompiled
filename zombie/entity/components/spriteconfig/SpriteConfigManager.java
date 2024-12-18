package zombie.entity.components.spriteconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import zombie.core.Core;
import zombie.core.properties.PropertyContainer;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.world.ScriptsDictionary;

public class SpriteConfigManager {
   public static final String FACE_SINGLE = "single";
   public static final String FACE_N = "n";
   public static final String FACE_W = "w";
   public static final String FACE_S = "s";
   public static final String FACE_E = "e";
   public static final String FACE_N_OPEN = "n_open";
   public static final String FACE_W_OPEN = "w_open";
   public static final int FACE_ID_SINGLE = 0;
   public static final int FACE_ID_N = 0;
   public static final int FACE_ID_W = 1;
   public static final int FACE_ID_S = 2;
   public static final int FACE_ID_E = 3;
   public static final int FACE_ID_CARDINAL_MAX = 4;
   public static final int FACE_ID_N_OPEN = 4;
   public static final int FACE_ID_W_OPEN = 5;
   public static final int FACE_ID_MAX = 6;
   private static boolean hasLoadErrors = false;
   private static final HashMap<String, ObjectInfo> objectInfos = new HashMap();
   private static final ArrayList<ObjectInfo> objectInfosList = new ArrayList();
   private static final HashSet<IsoSprite> registeredScriptedSprites = new HashSet();
   private static HashSet<String> tempFaceSprites = new HashSet();

   public SpriteConfigManager() {
   }

   public static int GetFaceIdForString(String var0) {
      if (var0.equalsIgnoreCase("n")) {
         return 0;
      } else if (var0.equalsIgnoreCase("n_open")) {
         return 4;
      } else if (var0.equalsIgnoreCase("e")) {
         return 3;
      } else if (var0.equalsIgnoreCase("s")) {
         return 2;
      } else if (var0.equalsIgnoreCase("w")) {
         return 1;
      } else if (var0.equalsIgnoreCase("w_open")) {
         return 5;
      } else {
         return var0.equalsIgnoreCase("single") ? 0 : -1;
      }
   }

   public static boolean HasLoadErrors() {
      return hasLoadErrors;
   }

   public static ObjectInfo GetObjectInfo(String var0) {
      return (ObjectInfo)objectInfos.get(var0);
   }

   public static ObjectInfo getObjectInfoFromSprite(String var0) {
      Iterator var1 = objectInfos.values().iterator();

      ObjectInfo var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (ObjectInfo)var1.next();
      } while(var2.getFaceForSprite(var0) == null);

      return var2;
   }

   public static ArrayList<ObjectInfo> GetObjectInfoList() {
      return objectInfosList;
   }

   public static void Reset() {
      objectInfos.clear();
      objectInfosList.clear();
      registeredScriptedSprites.clear();
      hasLoadErrors = false;
   }

   public static void InitScriptsPostTileDef() {
      Reset();
      ArrayList var0 = ScriptManager.instance.getAllGameEntities();
      Iterator var1 = var0.iterator();

      while(var1.hasNext()) {
         GameEntityScript var2 = (GameEntityScript)var1.next();
         parseEntityScript(var2);
      }

      registeredScriptedSprites.clear();
   }

   private static void parseEntityScript(GameEntityScript var0) {
      try {
         SpriteConfigScript var1 = (SpriteConfigScript)var0.getComponentScriptFor(ComponentType.SpriteConfig);
         if (var1 != null) {
            if (!var1.isValid()) {
               throw new Exception("Invalid SpriteConfig script for entity '" + var0.getName() + "'");
            }

            if (!parseSpriteConfigScript(var1)) {
               throw new Exception("Cannot parse SpriteConfig script for entity '" + var0.getName() + "'");
            }

            ArrayList var2 = var1.getAllTileNames();
            Iterator var3 = var2.iterator();

            String var4;
            IsoSprite var5;
            while(var3.hasNext()) {
               var4 = (String)var3.next();
               var5 = IsoSpriteManager.instance.getSprite(var4);
               if (var5 == null) {
                  throw new Exception("Sprite '" + var4 + "' does not exist. entity script: " + var0.getName());
               }

               if (registeredScriptedSprites.contains(var5)) {
                  throw new Exception("Sprite '" + var4 + "' is duplicate. entity script: " + var0.getName());
               }

               registeredScriptedSprites.add(var5);
            }

            var3 = var2.iterator();

            while(var3.hasNext()) {
               var4 = (String)var3.next();
               var5 = IsoSpriteManager.instance.getSprite(var4);
               if (var5 != null) {
                  PropertyContainer var6 = var5.getProperties();
                  var6.Set("EntityScriptName", var0.getName());
                  var6.Set(IsoFlagType.EntityScript);
                  if (var0.getName() == null) {
                     DebugLog.log("type = " + var0.getScriptObjectFullType());
                     DebugLog.log("name = " + var0.getScriptObjectName());
                     throw new Exception("KABLAMO!");
                  }
               }
            }
         }
      } catch (Exception var7) {
         hasLoadErrors = true;
         var7.printStackTrace();
      }

   }

   private static boolean parseSpriteConfigScript(SpriteConfigScript var0) {
      try {
         if (!var0.isValid()) {
            return false;
         } else {
            String var1 = var0.getName();
            if (objectInfos.containsKey(var1)) {
               throw new Exception("double entry, script: " + var1);
            } else {
               ObjectInfo var2 = new ObjectInfo(var0);
               long var3 = ScriptsDictionary.spriteConfigs.getVersionHash(var0);
               var2.setVersion(var3);

               for(int var5 = 0; var5 < 6; ++var5) {
                  if (!var0.isSingleFace() || var5 == 0) {
                     SpriteConfigScript.FaceScript var6 = var0.getFace(var5);
                     if (var6 != null) {
                        FaceInfo var7 = var2.CreateFace(var6.getFaceName(), var6.getTotalWidth(), var6.getTotalHeight(), var6.getZLayers());

                        int var10;
                        for(int var8 = 0; var8 < var6.getZLayers(); ++var8) {
                           SpriteConfigScript.ZLayer var9 = var6.getLayer(var8);

                           for(var10 = 0; var10 < var9.getHeight(); ++var10) {
                              SpriteConfigScript.XRow var11 = var9.getRow(var10);

                              for(int var12 = 0; var12 < var11.getWidth(); ++var12) {
                                 SpriteConfigScript.TileScript var13 = var11.getTile(var12);
                                 if (var13.isEmptySpace()) {
                                    var7.CreateEmpty(var13.isBlocksSquare(), var12, var10, var8);
                                 } else {
                                    String var14 = var13.getTileName();
                                    IsoSprite var15 = IsoSpriteManager.instance.getSprite(var14);
                                    if (var15 == null) {
                                       throw new Exception("Sprite is null '" + var14 + ", in script: " + var1);
                                    }

                                    var7.CreateTile(var13.getTileName(), var12, var10, var8, false);
                                 }
                              }
                           }
                        }

                        var7.ensureTileInfos();
                        tempFaceSprites.clear();

                        for(int var18 = 0; var18 < var7.tileInfos.length; ++var18) {
                           for(var10 = 0; var10 < var7.tileInfos[var18].length; ++var10) {
                              for(int var19 = 0; var19 < var7.tileInfos[var18][var10].length; ++var19) {
                                 TileInfo var17 = var7.tileInfos[var18][var10][var19];
                                 var17.masterOffsetX = var7.getMasterX() - var17.getX();
                                 var17.masterOffsetY = var7.getMasterY() - var17.getY();
                                 var17.masterOffsetZ = var7.getMasterZ() - var17.getZ();
                                 if (!var17.empty && var17.tile_sprite == null) {
                                    String var10002 = var7.getFaceName();
                                    throw new Exception("tile not empty, but sprite is null? face: " + var10002 + ", group: " + var2.groupName);
                                 }

                                 if (var17.tile_sprite != null && tempFaceSprites.contains(var17.tile_sprite)) {
                                    throw new Exception("double entry for sprite in face map: " + var17.tile_sprite + ", group: " + var2.groupName);
                                 }

                                 if (var17.tile_sprite != null) {
                                    tempFaceSprites.add(var17.tile_sprite);
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               objectInfos.put(var1, var2);
               objectInfosList.add(var2);
               return true;
            }
         }
      } catch (Exception var16) {
         hasLoadErrors = true;
         var16.printStackTrace();
         return false;
      }
   }

   @DebugClassFields
   public static class ObjectInfo {
      private final SpriteConfigScript script;
      private final String groupName;
      private final FaceInfo[] faces = new FaceInfo[6];
      private boolean isSingleFace;
      private long version = 0L;
      private String mainSpriteCache = null;
      private final Texture iconTexture;

      private ObjectInfo(SpriteConfigScript var1) {
         this.script = var1;
         this.groupName = var1.getName();
         if (var1 != null && !var1.getAllTileNames().isEmpty()) {
            String var2 = (String)var1.getAllTileNames().get(0);
            this.iconTexture = Texture.trygetTexture(var2);
         } else {
            this.iconTexture = null;
         }

      }

      public SpriteConfigScript getScript() {
         return this.script;
      }

      public CraftRecipeComponentScript getRecipe() {
         if (this.script != null) {
            GameEntityScript var1 = (GameEntityScript)this.script.getParent();
            if (var1 != null) {
               CraftRecipeComponentScript var2 = (CraftRecipeComponentScript)var1.getComponentScriptFor(ComponentType.CraftRecipe);
               if (var2 != null) {
                  return var2;
               }
            }
         }

         return null;
      }

      public String getName() {
         return this.groupName;
      }

      public long getVersion() {
         return this.version;
      }

      private void setVersion(long var1) {
         this.version = var1;
      }

      private FaceInfo CreateFace(String var1, int var2, int var3, int var4) {
         var1 = var1.toLowerCase();
         if (var1.equalsIgnoreCase("single")) {
            this.isSingleFace = true;
         }

         FaceInfo var5 = new FaceInfo(var1, var2, var3, var4);
         int var6 = SpriteConfigManager.GetFaceIdForString(var1);
         if (var6 != -1) {
            if (this.faces[var6] == null) {
               this.faces[var6] = var5;
            } else {
               DebugLog.log("MultiTileObject -> CreateFace face already created faceID: " + var1);
               if (Core.bDebug) {
                  throw new RuntimeException("MultiTileObject -> CreateFace face already created faceID: " + var1);
               }
            }

            return var5;
         } else {
            DebugLog.log("MultiTileObject -> CreateFace cannot get correct faceID: " + var1);
            if (Core.bDebug) {
               throw new RuntimeException("MultiTileObject -> CreateFace cannot get correct faceID: " + var1);
            } else {
               return null;
            }
         }
      }

      public String getMainSpriteNameUI() {
         if (this.mainSpriteCache == null) {
            for(int var1 = 0; var1 < 6; ++var1) {
               if (this.faces[var1] != null) {
                  String var2 = this.faces[var1].getMainSpriteName();
                  if (var2 != null) {
                     this.mainSpriteCache = var2;
                     break;
                  }
               }
            }
         }

         return this.mainSpriteCache;
      }

      public boolean isSingleFace() {
         return this.isSingleFace;
      }

      public boolean canRotate() {
         if (this.isSingleFace) {
            return false;
         } else {
            int var1 = 0;

            for(int var2 = 0; var2 < 4; ++var2) {
               if (this.faces[var2] != null) {
                  ++var1;
               }
            }

            return var1 > 1;
         }
      }

      public FaceInfo getFace(String var1) {
         return this.getFace(SpriteConfigManager.GetFaceIdForString(var1));
      }

      public FaceInfo getFace(int var1) {
         if (var1 >= 0 && var1 <= 6) {
            return this.faces[var1];
         } else {
            DebugLog.log("MultiTileObject -> getFace faceID out of bounds: " + var1);
            if (Core.bDebug) {
               throw new RuntimeException("MultiTileObject -> getFace faceID out of bounds: " + var1);
            } else {
               return null;
            }
         }
      }

      public FaceInfo getFaceForSprite(String var1) {
         for(int var2 = 0; var2 < this.faces.length; ++var2) {
            if (this.faces[var2] != null && this.faces[var2].sprites.containsKey(var1)) {
               return this.faces[var2];
            }
         }

         return null;
      }

      public boolean isProp() {
         return false;
      }

      public Texture getIconTexture() {
         return this.iconTexture;
      }

      public int getTime() {
         return 0;
      }

      public boolean needToBeLearn() {
         return false;
      }

      public List<String> getTags() {
         return new ArrayList();
      }

      public int getRequiredSkillCount() {
         return 0;
      }
   }

   @DebugClassFields
   public static class FaceInfo {
      private final HashMap<String, TileInfo> sprites;
      private final String face;
      private final int width;
      private final int height;
      private final int zLayers;
      private final TileInfo[][][] tileInfos;
      private int masterPosX;
      private int masterPosY;
      private int masterPosZ;
      private boolean isMasterSet = false;
      private final boolean isMultiSquare;

      private FaceInfo(String var1, int var2, int var3, int var4) {
         this.face = var1;
         this.width = var2;
         this.height = var3;
         this.zLayers = var4;
         this.tileInfos = new TileInfo[var4][var2][var3];
         this.sprites = new HashMap(var2 * var3 * var4);
         this.isMultiSquare = var2 > 1 || var3 > 1 || var4 > 1;
      }

      public String getFaceName() {
         return this.face;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public int getzLayers() {
         return this.zLayers;
      }

      public int getMasterX() {
         return this.masterPosX;
      }

      public int getMasterY() {
         return this.masterPosY;
      }

      public int getMasterZ() {
         return this.masterPosZ;
      }

      public boolean isMasterSet() {
         return this.isMasterSet;
      }

      public boolean isMultiSquare() {
         return this.isMultiSquare;
      }

      public TileInfo getMasterTileInfo() {
         return this.tileInfos[this.masterPosZ][this.masterPosX][this.masterPosY];
      }

      public boolean verifyObject(int var1, int var2, int var3, IsoObject var4) {
         TileInfo var5 = this.tileInfos[var3][var1][var2];
         return var5 != null ? var5.verifyObject(var4) : false;
      }

      private TileInfo CreateTile(String var1, int var2, int var3, int var4, boolean var5) {
         if (this.tileInfos[var4][var2][var3] != null) {
            DebugLog.log("FaceInfo -> CreateTile, tile already created: " + var1);
            if (Core.bDebug) {
               throw new RuntimeException("FaceInfo -> CreateTile, tile already created: " + var1);
            } else {
               return null;
            }
         } else {
            TileInfo var6 = null;
            if (!this.isMasterSet) {
               var6 = new TileInfo(var1, var2, var3, var4, true, false, true);
               this.masterPosX = var2;
               this.masterPosY = var3;
               this.masterPosZ = var4;
               this.isMasterSet = true;
            } else {
               var6 = new TileInfo(var1, var2, var3, var4, false, false, true);
            }

            this.tileInfos[var4][var2][var3] = var6;
            this.sprites.put(var1, var6);
            return var6;
         }
      }

      private TileInfo CreateEmpty(boolean var1, int var2, int var3, int var4) {
         TileInfo var5 = new TileInfo((String)null, var2, var3, var4, false, true, var1);
         this.tileInfos[var4][var2][var3] = var5;
         return var5;
      }

      public TileInfo getTileInfo(int var1, int var2, int var3) {
         return this.tileInfos[var3][var1][var2];
      }

      public TileInfo getTileInfoForSprite(String var1) {
         return (TileInfo)this.sprites.get(var1);
      }

      protected void ensureTileInfos() {
         for(int var1 = 0; var1 < this.zLayers; ++var1) {
            for(int var2 = 0; var2 < this.width; ++var2) {
               for(int var3 = 0; var3 < this.height; ++var3) {
                  if (this.tileInfos[var1][var2][var3] == null) {
                     this.CreateEmpty(false, var2, var3, var1);
                  }
               }
            }
         }

      }

      protected String getMainSpriteName() {
         for(int var1 = 0; var1 < this.zLayers; ++var1) {
            for(int var2 = this.width - 1; var2 >= 0; --var2) {
               for(int var3 = this.height - 1; var3 >= 0; --var3) {
                  if (this.tileInfos[var1][var2][var3] == null && this.tileInfos[var1][var2][var3].tile_sprite != null) {
                     return this.tileInfos[var1][var2][var3].tile_sprite;
                  }
               }
            }
         }

         return null;
      }
   }

   @DebugClassFields
   public static class TileInfo {
      private final String tile_sprite;
      private final int x;
      private final int y;
      private final int z;
      private int masterOffsetX;
      private int masterOffsetY;
      private int masterOffsetZ;
      private final boolean master;
      private final boolean empty;
      private final boolean blocking;

      private TileInfo(String var1, int var2, int var3, int var4, boolean var5, boolean var6, boolean var7) {
         this.tile_sprite = var1;
         this.x = var2;
         this.y = var3;
         this.z = var4;
         this.master = var5;
         this.empty = var6;
         this.blocking = var7;
      }

      public String getSpriteName() {
         return this.tile_sprite;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getZ() {
         return this.z;
      }

      public boolean isMaster() {
         return this.master;
      }

      public boolean isEmpty() {
         return this.empty;
      }

      public boolean isBlocking() {
         return this.blocking;
      }

      public int getMasterOffsetX() {
         return this.masterOffsetX;
      }

      public int getMasterOffsetY() {
         return this.masterOffsetY;
      }

      public int getMasterOffsetZ() {
         return this.masterOffsetZ;
      }

      public boolean verifyObject(IsoObject var1) {
         if (!this.isEmpty()) {
            IsoSprite var2 = var1.getSprite();
            if (var2 != null && var2.getName() != null && var2.getName().equalsIgnoreCase(this.getSpriteName())) {
               return true;
            }
         }

         return false;
      }
   }
}
