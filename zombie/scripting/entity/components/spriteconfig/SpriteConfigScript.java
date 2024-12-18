package zombie.scripting.entity.components.spriteconfig;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.world.ScriptsDictionary;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
public class SpriteConfigScript extends ComponentScript {
   private final FaceScript[] faces = new FaceScript[6];
   private boolean isSingleFace = false;
   private boolean isMultiTile = false;
   private boolean isValid = false;
   private boolean isProp = false;
   private final ArrayList<String> allTileNames = new ArrayList();
   private String cornerSprite = null;
   private int health = -1;
   private int skillBaseHealth = 200;
   private boolean isThumpable = true;
   private String breakSound = "BreakObject";
   private boolean dontNeedFrame = false;
   private boolean needWindowFrame = false;
   private ArrayList<String> previousStage = new ArrayList();
   private int bonusHealth = 0;
   private String OnCreate;
   private String OnIsValid;
   private boolean needToBeAgainstWall = false;

   public FaceScript getFace(int var1) {
      return this.faces[var1];
   }

   public String getCornerSprite() {
      return this.cornerSprite;
   }

   public ArrayList<String> getAllTileNames() {
      return this.allTileNames;
   }

   public boolean isSingleFace() {
      return this.isSingleFace;
   }

   public boolean isMultiTile() {
      return this.isMultiTile;
   }

   public boolean isValid() {
      return this.isValid;
   }

   public boolean isProp() {
      return this.isProp;
   }

   public int getHealth() {
      return this.health;
   }

   public int getSkillBaseHealth() {
      return this.skillBaseHealth;
   }

   public boolean getIsThumpable() {
      return this.isThumpable;
   }

   public String getBreakSound() {
      return this.breakSound;
   }

   public boolean getDontNeedFrame() {
      return this.dontNeedFrame;
   }

   public ArrayList<String> getPreviousStages() {
      return this.previousStage;
   }

   public int getBonusHealth() {
      return this.bonusHealth;
   }

   public String getOnCreate() {
      return this.OnCreate;
   }

   public String getOnIsValid() {
      return this.OnIsValid;
   }

   private SpriteConfigScript() {
      super(ComponentType.SpriteConfig);
   }

   public boolean isoMasterOnly() {
      return false;
   }

   public void getVersion(IVersionHash var1) {
      var1.add(this.getName());
      var1.add(String.valueOf(this.isValid()));
      if (this.isValid()) {
         FaceScript[] var2 = this.faces;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            FaceScript var5 = var2[var4];
            if (var5 != null) {
               var5.getVersion(var1);
            }
         }
      }

   }

   public void PreReload() {
      for(int var1 = 0; var1 < this.faces.length; ++var1) {
         this.faces[var1] = null;
      }

      this.cornerSprite = null;
      this.isSingleFace = false;
      this.isMultiTile = false;
      this.isValid = false;
      this.allTileNames.clear();
      this.isProp = false;
      this.health = -1;
      this.skillBaseHealth = 200;
      this.isThumpable = true;
      this.breakSound = "BreakObject";
      this.dontNeedFrame = false;
      this.needWindowFrame = false;
      this.needToBeAgainstWall = false;
      this.previousStage.clear();
      this.bonusHealth = 0;
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      if (var1 == ScriptLoadMode.Init) {
         ScriptsDictionary.registerScript(this);
      }

   }

   protected void copyFrom(ComponentScript var1) {
      throw new RuntimeException("Unfinished Todo");
   }

   protected void load(ScriptParser.Block var1) throws Exception {
      super.load(var1);
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && var5.isEmpty()) {
         }
      }

      var2 = var1.children.iterator();

      while(true) {
         label103:
         while(var2.hasNext()) {
            ScriptParser.Block var10 = (ScriptParser.Block)var2.next();
            if (var10.type.equalsIgnoreCase("face") && !this.isSingleFace) {
               int var11 = SpriteConfigManager.GetFaceIdForString(var10.id);
               if (var11 == -1) {
                  DebugLog.General.error("Cannot find face for: " + var10.id);
                  continue;
               }

               FaceScript var13 = new FaceScript();
               var13.faceName = var10.id;
               var13.faceID = var11;
               this.loadFace(var13, var10);
               this.faces[var11] = var13;
               if (var10.id.equalsIgnoreCase("single")) {
                  this.isSingleFace = true;
               }
            } else {
               String var10001 = var10.type;
               DebugLog.General.error("Unknown block '" + var10001 + "' in entity iso script: " + this.getName());
            }

            Iterator var12 = var1.values.iterator();

            while(true) {
               while(true) {
                  if (!var12.hasNext()) {
                     continue label103;
                  }

                  ScriptParser.Value var14 = (ScriptParser.Value)var12.next();
                  String var6 = var14.getKey().trim();
                  String var7 = var14.getValue().trim();
                  if (var6.equalsIgnoreCase("health")) {
                     this.health = PZMath.tryParseInt(var7, -1);
                  } else if (var6.equalsIgnoreCase("isProp")) {
                     this.isProp = Boolean.parseBoolean(var7);
                  } else if (var6.equalsIgnoreCase("skillBaseHealth")) {
                     this.skillBaseHealth = PZMath.tryParseInt(var7, 200);
                  } else if (var6.equalsIgnoreCase("OnCreate")) {
                     this.OnCreate = var7;
                  } else if (var6.equalsIgnoreCase("OnIsValid")) {
                     this.OnIsValid = var7;
                  } else if (var6.equalsIgnoreCase("isThumpable")) {
                     this.isThumpable = var7.equalsIgnoreCase("true");
                  } else if (var6.equalsIgnoreCase("breakSound")) {
                     this.breakSound = var7;
                  } else if (var6.equalsIgnoreCase("corner")) {
                     this.cornerSprite = var7;
                  } else if (var6.equalsIgnoreCase("dontNeedFrame")) {
                     this.dontNeedFrame = var7.equalsIgnoreCase("true");
                  } else if (var6.equalsIgnoreCase("needWindowFrame")) {
                     this.needWindowFrame = var7.equalsIgnoreCase("true");
                  } else if (var6.equalsIgnoreCase("needToBeAgainstWall")) {
                     this.needToBeAgainstWall = Boolean.parseBoolean(var7);
                  } else if (var6.equalsIgnoreCase("previousStage")) {
                     String[] var8 = var7.split(";");

                     for(int var9 = 0; var9 < var8.length; ++var9) {
                        this.previousStage.add(var8[var9]);
                     }
                  } else if (var6.equalsIgnoreCase("bonusHealth")) {
                     this.bonusHealth = PZMath.tryParseInt(var7, 0);
                  }
               }
            }
         }

         this.checkScripts();
         return;
      }
   }

   private void warnOrError(String var1) {
      if (Core.bDebug) {
         String var10002 = this.getName();
         throw new RuntimeException("[" + var10002 + "] " + var1);
      } else {
         DebugLogStream var10000 = DebugLog.General;
         String var10001 = this.getName();
         var10000.warn("[" + var10001 + "] " + var1);
      }
   }

   private void checkScripts() {
      if (this.isSingleFace) {
         for(int var1 = 0; var1 < 6; ++var1) {
            FaceScript var2 = this.faces[var1];
            if (var2 != null && var2.faceID != 0) {
               this.warnOrError("SingleFace has other faces defined.");
               this.faces[var1] = null;
            }
         }
      }

      boolean var14 = false;
      boolean var15 = false;
      int var3 = 0;
      FaceScript[] var4 = this.faces;
      int var5 = var4.length;

      int var6;
      FaceScript var7;
      for(var6 = 0; var6 < var5; ++var6) {
         var7 = var4[var6];
         if (var7 != null) {
            var14 = true;
            ++var3;
            Iterator var8 = var7.layers.iterator();

            while(var8.hasNext()) {
               ZLayer var9 = (ZLayer)var8.next();
               var7.totalHeight = PZMath.max(var7.totalHeight, var9.rows.size());
               Iterator var10 = var9.rows.iterator();

               while(var10.hasNext()) {
                  XRow var11 = (XRow)var10.next();
                  var7.totalWidth = PZMath.max(var7.totalWidth, var11.tiles.size());
                  Iterator var12 = var11.tiles.iterator();

                  while(var12.hasNext()) {
                     TileScript var13 = (TileScript)var12.next();
                     if (var13.tileName != null) {
                        if (this.allTileNames.contains(var13.tileName)) {
                           this.warnOrError("Tile duplicate: " + var13.tileName);
                           var15 = true;
                        } else {
                           this.allTileNames.add(var13.tileName);
                        }
                     }
                  }
               }
            }

            if (var3 > 1 || var7.totalHeight > 1 || var7.totalWidth > 1) {
               this.isMultiTile = true;
            }
         }
      }

      var4 = this.faces;
      var5 = var4.length;

      for(var6 = 0; var6 < var5; ++var6) {
         var7 = var4[var6];
         if (var7 != null) {
            byte var16 = -1;
            switch (var7.faceID) {
               case 0:
                  var16 = 4;
                  break;
               case 1:
                  var16 = 5;
               case 2:
               case 3:
               default:
                  break;
               case 4:
                  var16 = 0;
                  break;
               case 5:
                  var16 = 1;
            }

            if (var16 != -1 && this.getFace(var16) != null && (var7.totalWidth != this.getFace(var16).totalWidth || var7.totalHeight != this.getFace(var16).totalHeight)) {
               DebugLog.General.error("Complementary faces are different sizes. This is not supported. In entity iso script: " + this.getName());
            }
         }
      }

      if (var14 && !var15) {
         this.isValid = true;
      }

   }

   private void loadFace(FaceScript var1, ScriptParser.Block var2) {
      ZLayer var3 = new ZLayer();
      this.loadLayer(var3, var2);
      if (var3.rows.size() > 0) {
         var1.layers.add(var3);
      } else {
         Iterator var4 = var2.children.iterator();

         while(var4.hasNext()) {
            ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
            if (var5.type.equalsIgnoreCase("layer")) {
               this.loadLayer(var3, var5);
               var1.layers.add(var3);
               var3 = new ZLayer();
            }
         }

      }
   }

   private void loadLayer(ZLayer var1, ScriptParser.Block var2) {
      Iterator var3 = var2.values.iterator();

      while(true) {
         String var5;
         String var6;
         do {
            if (!var3.hasNext()) {
               return;
            }

            ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
            var5 = var4.getKey().trim();
            var6 = var4.getValue().trim();
         } while(!var5.equalsIgnoreCase("row"));

         XRow var7 = new XRow();
         String[] var8 = var6.split("\\s+");
         String[] var9 = var8;
         int var10 = var8.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            String var12 = var9[var11];
            TileScript var13 = new TileScript();
            if (!var12.equalsIgnoreCase("true") && !var12.equalsIgnoreCase("false")) {
               var13.tileName = var12;
               var13.isEmptySpace = false;
               var13.blocksSquare = true;
            } else {
               boolean var14 = var12.equalsIgnoreCase("true");
               var13.isEmptySpace = true;
               var13.blocksSquare = var14;
            }

            var7.tiles.add(var13);
         }

         var1.rows.add(var7);
      }
   }

   public boolean getNeedWindowFrame() {
      return this.needWindowFrame;
   }

   public boolean getNeedToBeAgainstWall() {
      return this.needToBeAgainstWall;
   }

   @DebugClassFields
   public static class FaceScript {
      private String faceName;
      private int faceID = -1;
      private int totalWidth = 0;
      private int totalHeight = 0;
      private final ArrayList<ZLayer> layers = new ArrayList();

      public FaceScript() {
      }

      public String getFaceName() {
         return this.faceName.toLowerCase();
      }

      public int getTotalWidth() {
         return this.totalWidth;
      }

      public int getTotalHeight() {
         return this.totalHeight;
      }

      public int getZLayers() {
         return this.layers.size();
      }

      public ZLayer getLayer(int var1) {
         return (ZLayer)this.layers.get(var1);
      }

      private void getVersion(IVersionHash var1) {
         var1.add(this.faceName);
         var1.add(String.valueOf(this.layers.size()));
         Iterator var2 = this.layers.iterator();

         while(var2.hasNext()) {
            ZLayer var3 = (ZLayer)var2.next();
            var3.getVersion(var1);
         }

      }
   }

   @DebugClassFields
   public static class ZLayer {
      private final ArrayList<XRow> rows = new ArrayList();

      public ZLayer() {
      }

      public int getHeight() {
         return this.rows.size();
      }

      public XRow getRow(int var1) {
         return (XRow)this.rows.get(var1);
      }

      private void getVersion(IVersionHash var1) {
         var1.add(String.valueOf(this.getHeight()));
         Iterator var2 = this.rows.iterator();

         while(var2.hasNext()) {
            XRow var3 = (XRow)var2.next();
            var3.getVersion(var1);
         }

      }
   }

   @DebugClassFields
   public static class XRow {
      private final ArrayList<TileScript> tiles = new ArrayList();

      public XRow() {
      }

      public int getWidth() {
         return this.tiles.size();
      }

      public TileScript getTile(int var1) {
         return (TileScript)this.tiles.get(var1);
      }

      private void getVersion(IVersionHash var1) {
         var1.add(String.valueOf(this.getWidth()));
         Iterator var2 = this.tiles.iterator();

         while(var2.hasNext()) {
            TileScript var3 = (TileScript)var2.next();
            var3.getVersion(var1);
         }

      }
   }

   @DebugClassFields
   public static class TileScript {
      private String tileName;
      private boolean isEmptySpace = false;
      private boolean blocksSquare = false;

      public TileScript() {
      }

      public String getTileName() {
         return this.tileName;
      }

      public boolean isEmptySpace() {
         return this.isEmptySpace;
      }

      public boolean isBlocksSquare() {
         return this.blocksSquare;
      }

      private void getVersion(IVersionHash var1) {
         if (this.tileName != null) {
            var1.add(this.tileName);
         }

         var1.add(String.valueOf(this.isEmptySpace));
         var1.add(String.valueOf(this.blocksSquare));
      }
   }
}
