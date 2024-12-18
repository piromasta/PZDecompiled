package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import org.joml.Vector3f;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

public final class ModelScript extends BaseScriptObject implements IModelAttachmentOwner {
   public static final String DEFAULT_SHADER_NAME = "basicEffect";
   public String fileName;
   public String name;
   public String meshName;
   public String textureName;
   public String shaderName;
   public boolean bStatic = true;
   public float scale = 1.0F;
   public final ArrayList<ModelAttachment> m_attachments = new ArrayList();
   public HashMap<String, ModelAttachment> m_attachmentById = new HashMap();
   public boolean invertX = false;
   public String postProcess = null;
   public Model loadedModel;
   public final ArrayList<AnimBoneWeight> boneWeights = new ArrayList();
   public String animationsMesh = null;
   private static final HashSet<String> reported = new HashSet();

   public ModelScript() {
      super(ScriptType.Model);
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
      ScriptManager var2 = ScriptManager.instance;
      this.fileName = var2.currentFileName;
      this.name = var1;
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.children.iterator();

      while(var4.hasNext()) {
         ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
         if ("attachment".equals(var5.type)) {
            this.LoadAttachment(var5);
         }
      }

      boolean var11 = false;
      Iterator var12 = var3.values.iterator();

      while(var12.hasNext()) {
         ScriptParser.Value var6 = (ScriptParser.Value)var12.next();
         String var7 = var6.getKey().trim();
         String var8 = var6.getValue().trim();
         if ("mesh".equalsIgnoreCase(var7)) {
            this.meshName = var8;
         } else if ("scale".equalsIgnoreCase(var7)) {
            this.scale = Float.parseFloat(var8);
         } else if ("shader".equalsIgnoreCase(var7)) {
            this.shaderName = var8;
         } else if ("static".equalsIgnoreCase(var7)) {
            this.bStatic = Boolean.parseBoolean(var8);
         } else if ("texture".equalsIgnoreCase(var7)) {
            this.textureName = var8;
         } else if ("invertX".equalsIgnoreCase(var7)) {
            this.invertX = Boolean.parseBoolean(var8);
         } else if ("postProcess".equalsIgnoreCase(var7)) {
            this.postProcess = var8;
         } else if ("undoCoreScale".equalsIgnoreCase(var7)) {
            var11 = Boolean.parseBoolean(var8);
         } else if ("boneWeight".equalsIgnoreCase(var7)) {
            String[] var9 = var8.split("\\s+");
            if (var9.length == 2) {
               AnimBoneWeight var10 = new AnimBoneWeight(var9[0], PZMath.tryParseFloat(var9[1], 1.0F));
               var10.includeDescendants = false;
               this.boneWeights.add(var10);
            }
         } else if ("animationsMesh".equalsIgnoreCase(var7)) {
            this.animationsMesh = StringUtils.discardNullOrWhitespace(var8);
         }
      }

      if (var11) {
         this.scale *= 0.6666667F;
      }

   }

   private ModelAttachment LoadAttachment(ScriptParser.Block var1) {
      ModelAttachment var2 = this.getAttachmentById(var1.id);
      if (var2 == null) {
         var2 = new ModelAttachment(var1.id);
         var2.setOwner(this);
         this.m_attachments.add(var2);
         this.m_attachmentById.put(var2.getId(), var2);
      }

      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if ("bone".equals(var5)) {
            var2.setBone(var6);
         } else if ("offset".equals(var5)) {
            this.LoadVector3f(var6, var2.getOffset());
         } else if ("rotate".equals(var5)) {
            this.LoadVector3f(var6, var2.getRotate());
         } else if ("scale".equals(var5)) {
            var2.setScale(PZMath.tryParseFloat(var6, 1.0F));
         }
      }

      return var2;
   }

   private void LoadVector3f(String var1, Vector3f var2) {
      String[] var3 = var1.split(" ");
      var2.set(Float.parseFloat(var3[0]), Float.parseFloat(var3[1]), Float.parseFloat(var3[2]));
   }

   public String getName() {
      return this.name;
   }

   public String getFullType() {
      return this.getModule().name + "." + this.name;
   }

   public String getMeshName() {
      return this.meshName;
   }

   public String getTextureName() {
      return StringUtils.isNullOrWhitespace(this.textureName) ? this.meshName : this.textureName;
   }

   public String getTextureName(boolean var1) {
      return StringUtils.isNullOrWhitespace(this.textureName) && !var1 ? this.meshName : this.textureName;
   }

   public String getShaderName() {
      return StringUtils.isNullOrWhitespace(this.shaderName) ? "basicEffect" : this.shaderName;
   }

   public String getFileName() {
      return this.fileName;
   }

   public int getAttachmentCount() {
      return this.m_attachments.size();
   }

   public ModelAttachment getAttachment(int var1) {
      return (ModelAttachment)this.m_attachments.get(var1);
   }

   public ModelAttachment getAttachmentById(String var1) {
      return (ModelAttachment)this.m_attachmentById.get(var1);
   }

   public ModelAttachment addAttachment(ModelAttachment var1) {
      var1.setOwner(this);
      this.m_attachments.add(var1);
      this.m_attachmentById.put(var1.getId(), var1);
      return var1;
   }

   public ModelAttachment removeAttachment(ModelAttachment var1) {
      var1.setOwner((IModelAttachmentOwner)null);
      this.m_attachments.remove(var1);
      this.m_attachmentById.remove(var1.getId());
      return var1;
   }

   public ModelAttachment addAttachmentAt(int var1, ModelAttachment var2) {
      var2.setOwner(this);
      this.m_attachments.add(var1, var2);
      this.m_attachmentById.put(var2.getId(), var2);
      return var2;
   }

   public ModelAttachment removeAttachment(int var1) {
      ModelAttachment var2 = (ModelAttachment)this.m_attachments.remove(var1);
      this.m_attachmentById.remove(var2.getId());
      var2.setOwner((IModelAttachmentOwner)null);
      return var2;
   }

   public void scaleAttachmentOffset(float var1) {
      for(int var2 = 0; var2 < this.getAttachmentCount(); ++var2) {
         ModelAttachment var3 = this.getAttachment(var2);
         var3.getOffset().mul(var1);
      }

   }

   public void beforeRenameAttachment(ModelAttachment var1) {
      this.m_attachmentById.remove(var1.getId());
   }

   public void afterRenameAttachment(ModelAttachment var1) {
      this.m_attachmentById.put(var1.getId(), var1);
   }

   public boolean isStatic() {
      return this.bStatic;
   }

   public void reset() {
      this.invertX = false;
      this.name = null;
      this.meshName = null;
      this.textureName = null;
      this.shaderName = null;
      this.bStatic = true;
      this.scale = 1.0F;
      this.boneWeights.clear();
   }

   private static void checkMesh(String var0, String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         String var2 = var1.toLowerCase(Locale.ENGLISH);
         if (!ZomboidFileSystem.instance.ActiveFileMap.containsKey("media/models_x/" + var2 + ".fbx") && !ZomboidFileSystem.instance.ActiveFileMap.containsKey("media/models_x/" + var2 + ".x") && !ZomboidFileSystem.instance.ActiveFileMap.containsKey("media/models/" + var2 + ".txt")) {
            reported.add(var1);
            DebugLog.Script.warn("no such mesh \"" + var1 + "\" for " + var0);
         }

      }
   }

   private static void checkTexture(String var0, String var1) {
      if (!GameServer.bServer) {
         if (!StringUtils.isNullOrWhitespace(var1)) {
            String var2 = var1.toLowerCase(Locale.ENGLISH);
            if (!ZomboidFileSystem.instance.ActiveFileMap.containsKey("media/textures/" + var2 + ".png")) {
               reported.add(var1);
               DebugLog.Script.warn("no such texture \"" + var1 + "\" for " + var0);
            }

         }
      }
   }

   private static void check(String var0, String var1) {
      check(var0, var1, (String)null);
   }

   private static void check(String var0, String var1, String var2) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         if (!reported.contains(var1)) {
            ModelScript var3 = ScriptManager.instance.getModelScript(var1);
            if (var3 == null) {
               reported.add(var1);
               DebugLog.Script.warn("no such model \"" + var1 + "\" for " + var0);
            } else {
               checkMesh(var3.getFullType(), var3.getMeshName());
               if (StringUtils.isNullOrWhitespace(var2)) {
                  checkTexture(var3.getFullType(), var3.getTextureName());
               }
            }

         }
      }
   }

   public static void ScriptsLoaded() {
      reported.clear();
      ArrayList var0 = ScriptManager.instance.getAllItems();
      Iterator var1 = var0.iterator();

      while(var1.hasNext()) {
         Item var2 = (Item)var1.next();
         var2.resolveModelScripts();
         check(var2.getFullName(), var2.getStaticModel());
         check(var2.getFullName(), var2.getWeaponSprite());
         check(var2.getFullName(), var2.worldStaticModel, var2.getClothingItem());
         if (var2.getType() == Item.Type.Food) {
            String var3 = var2.getStaticModel();
            if (!StringUtils.isNullOrWhitespace(var3)) {
               ModelScript var4 = ScriptManager.instance.getModelScript(var3);
               if (var4 != null && var4.getAttachmentCount() != 0) {
                  ModelScript var5 = ScriptManager.instance.getModelScript(var3 + "Burnt");
                  if (var5 != null) {
                     checkTexture(var5.getName(), var5.textureName);
                  }

                  if (var5 != null && var5.getAttachmentCount() != var4.getAttachmentCount()) {
                     DebugLog.Script.warn("different number of attachments on %s and %s", var4.name, var5.name);
                  }

                  var5 = ScriptManager.instance.getModelScript(var3 + "Cooked");
                  if (var5 != null) {
                     checkTexture(var5.getName(), var5.textureName);
                  }

                  if (var5 != null && var5.getAttachmentCount() != var4.getAttachmentCount()) {
                     DebugLog.Script.warn("different number of attachments on %s and %s", var4.name, var5.name);
                  }

                  var5 = ScriptManager.instance.getModelScript(var3 + "Rotten");
                  if (var5 != null) {
                     checkTexture(var5.getName(), var5.textureName);
                  }

                  if (var5 != null && var5.getAttachmentCount() != var4.getAttachmentCount()) {
                     DebugLog.Script.warn("different number of attachments on %s and %s", var4.name, var5.name);
                  }
               }
            }
         }
      }

   }
}
