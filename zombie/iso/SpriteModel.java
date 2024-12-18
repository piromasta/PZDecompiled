package zombie.iso;

import java.util.Iterator;
import org.joml.Vector3f;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;

public final class SpriteModel extends BaseScriptObject {
   public String modelScriptName;
   public String textureName;
   public final Vector3f translate = new Vector3f();
   public final Vector3f rotate = new Vector3f();
   public float scale = 1.0F;
   public String animationName = null;
   public float animationTime = -1.0F;
   public String runtimeString = null;

   public SpriteModel() {
      super(ScriptType.SpriteModel);
   }

   protected SpriteModel(ScriptType var1) {
      super(var1);
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      this.modelScriptName = var3.getValue("modelScript").getValue().trim();
      ScriptParser.Value var4;
      if ((var4 = var3.getValue("texture")) != null) {
         this.textureName = StringUtils.discardNullOrWhitespace(var4.getValue().trim());
      }

      this.parseVector3f(var3.getValue("translate").getValue().trim(), this.translate);
      this.parseVector3f(var3.getValue("rotate").getValue().trim(), this.rotate);
      this.scale = PZMath.tryParseFloat(var3.getValue("scale").getValue().trim(), 1.0F);
      String var5;
      if ((var4 = var3.getValue("animation")) != null) {
         var5 = var4.getValue().trim();
         this.animationName = StringUtils.discardNullOrWhitespace(var5);
      }

      if ((var4 = var3.getValue("animationTime")) != null) {
         var5 = var4.getValue();
         this.animationTime = PZMath.tryParseFloat(var5, -1.0F);
      }

      Iterator var7 = var3.children.iterator();

      while(var7.hasNext()) {
         ScriptParser.Block var6 = (ScriptParser.Block)var7.next();
         if ("xxx".equals(var6.type)) {
         }
      }

   }

   void parseVector3f(String var1, Vector3f var2) {
      String[] var3 = var1.split(" ");
      var2.setComponent(0, PZMath.tryParseFloat(var3[0], 0.0F));
      var2.setComponent(1, PZMath.tryParseFloat(var3[1], 0.0F));
      var2.setComponent(2, PZMath.tryParseFloat(var3[2], 0.0F));
   }

   public SpriteModel set(SpriteModel var1) {
      this.modelScriptName = var1.modelScriptName;
      this.textureName = var1.textureName;
      this.translate.set(var1.translate);
      this.rotate.set(var1.rotate);
      this.scale = var1.scale;
      this.animationName = var1.animationName;
      this.animationTime = var1.animationTime;
      return this;
   }

   public String getModelScriptName() {
      return this.modelScriptName;
   }

   public void setModelScriptName(String var1) {
      this.modelScriptName = var1;
   }

   public String getTextureName() {
      return this.textureName;
   }

   public void setTextureName(String var1) {
      this.textureName = StringUtils.discardNullOrWhitespace(var1);
   }

   public Vector3f getTranslate() {
      return this.translate;
   }

   public Vector3f getRotate() {
      return this.rotate;
   }

   public float getScale() {
      return this.scale;
   }

   public void setScale(float var1) {
      this.scale = var1;
   }

   public String getAnimationName() {
      return this.animationName;
   }

   public void setAnimationName(String var1) {
      this.animationName = var1;
   }

   public float getAnimationTime() {
      return this.animationTime;
   }

   public void setAnimationTime(float var1) {
      this.animationTime = var1;
   }

   public String getRuntimeString() {
      return this.runtimeString;
   }

   public void setRuntimeString(String var1) {
      this.runtimeString = StringUtils.discardNullOrWhitespace(var1);
   }

   public void parseRuntimeString(String var1, int var2, int var3, String var4) throws RuntimeException {
      if (var4 != null) {
         if (var4.contains("standard_door")) {
            this.parseStandardDoor(var1, var2, var3, var4);
         } else if (var4.contains("pair_door")) {
            this.parsePairDoor(var1, var2, var3, var4);
         }

      }
   }

   void parseStandardDoor(String var1, int var2, int var3, String var4) throws RuntimeException {
      String[] var5 = var4.trim().split(" ");
      if (!"standard_door".equalsIgnoreCase(var5[0])) {
         throw new RuntimeException("expected \"standard_door\" but got \"%s\"".formatted(var4));
      } else {
         int var16;
         String var6 = var5[1];
         String var7 = var5[2];
         boolean var8 = true;
         String var9 = "IsoObject/door1";
         int var10 = var2 + var3 * 8;
         int var11 = var2 / 4 * 4;
         label52:
         switch (var6) {
            case "w":
               switch (var7) {
                  case "closed":
                     var16 = var11 + 2 + var3 * 8;
                     this.modelScriptName = String.format("Base.%s_%d", var1, var10);
                     this.translate.set(-0.469F, 0.0F, -0.375F);
                     this.rotate.set(0.0F, 90.0F, 0.0F);
                     this.scale = 0.6666667F;
                     this.animationName = "Open";
                     this.animationTime = 0.0F;
                     var9 = "IsoObject/door_w_se.glb";
                     break label52;
                  case "open":
                     var16 = var11 + 2 + var3 * 8;
                     this.modelScriptName = String.format("Base.%s_%d", var1, var10);
                     this.translate.set(-0.469F, 0.0F, -0.375F);
                     this.rotate.set(0.0F, 90.0F, 0.0F);
                     this.scale = 0.6666667F;
                     this.animationName = "Close";
                     this.animationTime = 0.0F;
                     var9 = "IsoObject/door_w_se.glb";
                     break label52;
                  default:
                     throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
               }
            case "n":
               switch (var7) {
                  case "closed":
                     var16 = var11 + 2 + var3 * 8;
                     this.modelScriptName = String.format("Base.%s_%d", var1, var10);
                     this.translate.set(-0.383F, 0.0F, -0.445F);
                     this.rotate.set(0.0F, 0.0F, 0.0F);
                     this.scale = 0.6666667F;
                     this.animationName = "Open";
                     this.animationTime = 0.0F;
                     var9 = "IsoObject/door_n_sw.glb";
                     break label52;
                  case "open":
                     var16 = var11 + 2 + var3 * 8;
                     this.modelScriptName = String.format("Base.%s_%d", var1, var10);
                     this.translate.set(-0.383F, 0.0F, -0.445F);
                     this.rotate.set(0.0F, 0.0F, 0.0F);
                     this.scale = 0.6666667F;
                     this.animationName = "Close";
                     this.animationTime = 0.0F;
                     var9 = "IsoObject/door_n_sw.glb";
                     break label52;
                  default:
                     throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
               }
            default:
               throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
         }

         this.runtimeString = var4.trim();
         if (this.createDoorModelScriptIfNeeded(var1, var16, var9) != null) {
            ;
         }
      }
   }

   void parsePairDoor(String var1, int var2, int var3, String var4) throws RuntimeException {
      String[] var5 = var4.trim().split(" ");
      if (!"pair_door".equalsIgnoreCase(var5[0])) {
         throw new RuntimeException("expected \"pair_door\" but got \"%s\"".formatted(var5[0]));
      } else {
         int var16;
         String var6 = var5[1];
         String var7 = var5[2];
         String var8 = var5[3];
         boolean var9 = true;
         String var10 = "IsoObject/door_pair_left_w.glb";
         int var11 = var2 + var3 * 8;
         label98:
         switch (var7) {
            case "w":
               if ("left".equalsIgnoreCase(var6)) {
                  switch (var8) {
                     case "closed":
                        var16 = 4 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(-0.492F, 0.0F, -0.1F);
                        this.rotate.set(0.0F, 90.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Open";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_left_w.glb";
                        break label98;
                     case "open":
                        var16 = 2 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(-0.492F, 0.0F, -0.1F);
                        this.rotate.set(0.0F, 90.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Close";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_left_w_open.glb";
                        break label98;
                     default:
                        throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
                  }
               } else {
                  if (!"right".equalsIgnoreCase(var6)) {
                     throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
                  }

                  switch (var8) {
                     case "closed":
                        var16 = 7 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(-0.49F, 0.0F, 0.1F);
                        this.rotate.set(0.0F, 90.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Open";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_right_w.glb";
                        break label98;
                     case "open":
                        var16 = 7 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(-0.49F, 0.0F, 0.1F);
                        this.rotate.set(0.0F, 90.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Close";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_right_w.glb";
                        break label98;
                     default:
                        throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
                  }
               }
            case "n":
               if ("left".equalsIgnoreCase(var6)) {
                  switch (var8) {
                     case "closed":
                        var16 = var2 + 1 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(0.1F, 0.0F, -0.47F);
                        this.rotate.set(0.0F, 0.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Open";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_left_n.glb";
                        break label98;
                     case "open":
                        var16 = var2 - 1 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(0.1F, 0.0F, -0.47F);
                        this.rotate.set(0.0F, 0.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Close";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_left_n.glb";
                        break label98;
                     default:
                        throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
                  }
               } else {
                  if (!"right".equalsIgnoreCase(var6)) {
                     throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
                  }

                  switch (var8) {
                     case "closed":
                        var16 = 5 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(-0.117F, 0.0F, -0.47F);
                        this.rotate.set(0.0F, 0.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Open";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_right_n.glb";
                        break label98;
                     case "open":
                        var16 = 7 + var3 * 8;
                        this.modelScriptName = String.format("Base.%s_%d", var1, var11);
                        this.translate.set(-0.117F, 0.0F, -0.47F);
                        this.rotate.set(0.0F, 0.0F, 0.0F);
                        this.scale = 0.6666667F;
                        this.animationName = "Close";
                        this.animationTime = 0.0F;
                        var10 = "IsoObject/door_pair_right_n_open.glb";
                        break label98;
                     default:
                        throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
                  }
               }
            default:
               throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
         }

         this.runtimeString = var4.trim();
         if (this.createDoorModelScriptIfNeeded(var1, var16, var10) == null) {
            throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(var4));
         }
      }
   }

   ModelScript createDoorModelScriptIfNeeded(String var1, int var2, String var3) {
      ModelScript var4 = ScriptManager.instance.getModelScript(this.getModelScriptName());
      if (var4 == null) {
         var4 = new ModelScript();
         var4.setModule(ScriptManager.instance.getModule("Base"));
         var4.InitLoadPP(this.getModelScriptName().substring("Base.".length()));
         ScriptManager.instance.addModelScript(var4);
      }

      var4.fileName = ZomboidFileSystem.instance.getMediaPath("scripts/models_runtime.txt");
      var4.name = this.getModelScriptName().substring("Base.".length());
      var4.meshName = var3;
      var4.textureName = String.format("%s_%d", var1, var2);
      var4.scale = var3.endsWith(".glb") ? 1.0F : 0.01F;
      var4.shaderName = "door";
      var4.bStatic = false;
      int var5 = var3.lastIndexOf(47) + 1;
      int var6 = var3.lastIndexOf(46);
      if (var6 == -1) {
         var6 = var3.length();
      }

      var4.animationsMesh = var3.substring(var5, var6);
      var4.loadedModel = null;
      return var4;
   }
}
