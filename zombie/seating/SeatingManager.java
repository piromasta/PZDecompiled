package zombie.seating;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class SeatingManager {
   private static SeatingManager instance;
   private final String DEFAULT_FACING = "S";
   private final ArrayList<ModData> m_modData = new ArrayList();
   private final SeatingData m_mergedTilesets = new SeatingData((String)null);

   public static SeatingManager getInstance() {
      if (instance == null) {
         instance = new SeatingManager();
      }

      return instance;
   }

   private SeatingManager() {
   }

   public void init() {
      ArrayList var1 = ZomboidFileSystem.instance.getModIDs();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         ChooseGameInfo.Mod var4 = ChooseGameInfo.getAvailableModDetails(var3);
         if (var4 != null) {
            File var5 = new File(var4.mediaFile.common.absoluteFile, "seating.txt");
            if (var5.exists()) {
               this.initModData(var4);
            }
         }
      }

      this.initGameData();
      this.initMergedTilesets();
   }

   public void initGameData() {
      ModData var1 = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
      var1.m_data.init();
      this.m_modData.add(var1);
   }

   public void initModData(ChooseGameInfo.Mod var1) {
      ModData var2 = new ModData(var1.getId(), var1.mediaFile.common.absoluteFile.getAbsolutePath());
      var2.m_data.init();
      this.m_modData.add(var2);
   }

   private void initMergedTilesets() {
      this.m_mergedTilesets.Reset();
      this.m_mergedTilesets.initMerged();

      for(int var1 = 0; var1 < this.m_modData.size(); ++var1) {
         ModData var2 = (ModData)this.m_modData.get(var1);
         this.m_mergedTilesets.mergeTilesets(var2.m_data);
      }

   }

   public void mergeAfterEditing() {
      this.initMergedTilesets();
   }

   public ArrayList<String> getModIDs() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.m_modData.size(); ++var2) {
         ModData var3 = (ModData)this.m_modData.get(var2);
         var1.add(var3.m_modID);
      }

      return var1;
   }

   ModData getModData(String var1) {
      for(int var2 = 0; var2 < this.m_modData.size(); ++var2) {
         ModData var3 = (ModData)this.m_modData.get(var2);
         if (StringUtils.equals(var1, var3.m_modID)) {
            return var3;
         }
      }

      return null;
   }

   ModData getModDataRequired(String var1) {
      ModData var2 = this.getModData(var1);
      if (var2 == null) {
         throw new RuntimeException("unknown modID \"%s\"".formatted(var1));
      } else {
         return var2;
      }
   }

   SeatingData getSeatingDataRequired(String var1) {
      return this.getModDataRequired(var1).m_data;
   }

   public int addTilePosition(String var1, String var2, int var3, int var4, String var5) {
      return this.getSeatingDataRequired(var1).addPosition(var2, var3, var4, var5);
   }

   public void removeTilePosition(String var1, String var2, int var3, int var4, int var5) {
      this.getSeatingDataRequired(var1).removePosition(var2, var3, var4, var5);
   }

   public int getTilePositionCount(String var1, String var2, int var3, int var4) {
      return this.getSeatingDataRequired(var1).getPositionCount(var2, var3, var4);
   }

   public int getTilePositionCount(String var1, int var2, int var3) {
      return this.m_mergedTilesets.getPositionCount(var1, var2, var3);
   }

   public int getTilePositionCount(IsoObject var1) {
      if (var1 != null && var1.getSprite() != null && var1.getSprite().tilesetName != null) {
         IsoSprite var2 = var1.getSprite();
         int var3 = var2.tileSheetIndex % 8;
         int var4 = var2.tileSheetIndex / 8;
         return this.m_mergedTilesets.getPositionCount(var2.tilesetName, var3, var4);
      } else {
         return 0;
      }
   }

   public String getTilePositionID(String var1, String var2, int var3, int var4, int var5) {
      return this.getSeatingDataRequired(var1).getPositionID(var2, var3, var4, var5);
   }

   public boolean hasTilePositionWithID(String var1, String var2, int var3, int var4, String var5) {
      return this.getSeatingDataRequired(var1).hasPositionWithID(var2, var3, var4, var5);
   }

   public Vector3f getTilePositionTranslate(String var1, String var2, int var3, int var4, int var5) {
      return this.getSeatingDataRequired(var1).getPositionTranslate(var2, var3, var4, var5);
   }

   public String getTilePositionProperty(String var1, String var2, int var3, int var4, int var5, String var6) {
      return StringUtils.isNullOrWhitespace(var6) ? null : this.getSeatingDataRequired(var1).getPositionProperty(var2, var3, var4, var5, var6);
   }

   public void setTilePositionProperty(String var1, String var2, int var3, int var4, int var5, String var6, String var7) {
      if (!StringUtils.isNullOrWhitespace(var6)) {
         SeatingFile.Tile var8 = this.getSeatingDataRequired(var1).getOrCreateTile(var2, var3, var4);
         SeatingFile.Position var9 = var8.getPositionByIndex(var5);
         if (var9 != null) {
            var9.setProperty(var6, var7);
         }
      }
   }

   public String getTileProperty(String var1, int var2, int var3, String var4) {
      return this.m_mergedTilesets.getProperty(var1, var2, var3, var4);
   }

   public String getTileProperty(String var1, String var2, int var3, int var4, String var5) {
      return this.getSeatingDataRequired(var1).getProperty(var2, var3, var4, var5);
   }

   public void setTileProperty(String var1, String var2, int var3, int var4, String var5, String var6) {
      this.getSeatingDataRequired(var1).setProperty(var2, var3, var4, var5, var6);
   }

   public SeatingFile.Tile getTile(String var1, String var2, int var3, int var4) {
      return this.getSeatingDataRequired(var1).getTile(var2, var3, var4);
   }

   public SeatingFile.Tile getOrCreateTile(String var1, String var2, int var3, int var4) {
      return this.getSeatingDataRequired(var1).getOrCreateTile(var2, var3, var4);
   }

   public Vector3f getTranslation(String var1, IsoSprite var2, String var3, Vector3f var4) {
      var4.set(0.0F);
      return var2 != null && var2.tilesetName != null ? this.getTranslation(var1, var2.tilesetName, var2.tileSheetIndex, var3, var4) : var4;
   }

   private Vector3f getTranslation(SeatingData var1, String var2, int var3, String var4, Vector3f var5) {
      var5.set(0.0F);
      int var6 = var3 % 8;
      int var7 = var3 / 8;
      SeatingFile.Position var8 = var1.getPositionWithID(var2, var6, var7, var4);
      return var8 == null ? var5 : var5.set(var8.translate.x, var8.translate.z, var8.translate.y);
   }

   public Vector3f getTranslation(String var1, String var2, int var3, String var4, Vector3f var5) {
      return this.getTranslation(this.getSeatingDataRequired(var1), var2, var3, var4, var5);
   }

   public Vector3f getTranslation(String var1, int var2, String var3, Vector3f var4) {
      return this.getTranslation(this.m_mergedTilesets, var1, var2, var3, var4);
   }

   private Vector3f getTranslation(SeatingData var1, IsoSprite var2, String var3, Vector3f var4) {
      var4.set(0.0F);
      return var2 != null && var2.tilesetName != null ? this.getTranslation(var1, var2.tilesetName, var2.tileSheetIndex, var3, var4) : var4;
   }

   public Vector3f getTranslation(IsoSprite var1, String var2, Vector3f var3) {
      return this.getTranslation(this.m_mergedTilesets, var1, var2, var3);
   }

   public boolean getAdjacentPosition(IsoGameCharacter var1, IsoObject var2, String var3, String var4, String var5, String var6, Vector3f var7) {
      var7.set(0.0F);
      if (var2 != null && var2.getSprite() != null && var2.getSprite().tilesetName != null) {
         Model var8 = var1.getAnimationPlayer().getModel();
         AnimationSet var9 = var1.getAdvancedAnimator().animSet;
         Vector2f var10 = new Vector2f();
         boolean var11 = this.getAdjacentPosition(this.m_mergedTilesets, var2.getSprite(), var3, var4, var8, var9.m_Name, var5, var6, var10);
         if (!var11) {
            return false;
         } else {
            var7.set((float)var2.square.x + 0.5F + var10.x, (float)var2.square.y + 0.5F + var10.y, (float)var2.square.z);
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean getAdjacentPosition(SeatingData var1, IsoSprite var2, String var3, String var4, Model var5, String var6, String var7, String var8, Vector2f var9) {
      var9.set(0.0F);
      if (var2 != null && var2.tilesetName != null) {
         int var10 = var2.tileSheetIndex % 8;
         int var11 = var2.tileSheetIndex / 8;
         SeatingFile.Position var12 = var1.getPositionWithID(var2.tilesetName, var10, var11, var3);
         if (var12 == null) {
            return false;
         } else if ("Left".equalsIgnoreCase(var4) && var12.getProperty("BlockLeft") != null) {
            return false;
         } else if ("Right".equalsIgnoreCase(var4) && var12.getProperty("BlockRight") != null) {
            return false;
         } else {
            Vector3f var13 = this.getTranslation(var1, var2, var3, new Vector3f());
            float var14 = var13.x;
            float var15 = var13.y;
            float var16 = var13.z;
            float var19 = var16 / 2.44949F;
            AnimationSet var20 = AnimationSet.GetAnimationSet(var6, false);
            AnimState var21 = var20.GetState(var7);
            AnimNode var22 = (AnimNode)PZArrayUtil.find(var21.m_Nodes, (var1x) -> {
               return var8.equalsIgnoreCase(var1x.m_Name);
            });
            Vector2 var23 = new Vector2();
            SkinningData var24 = (SkinningData)var5.Tag;
            this.getTotalDeferredMovement(var24, var22, var23);
            var23.scale(1.5F);
            float var25 = 0.0F;
            float var10000;
            switch (var4) {
               case "Front":
                  var10000 = 0.0F;
                  break;
               case "Left":
                  var10000 = 90.0F;
                  break;
               case "Right":
                  var10000 = -90.0F;
                  break;
               default:
                  var10000 = 0.0F;
            }

            float var26 = var10000;
            switch (var3) {
               case "N":
                  var10000 = (180.0F + var26) * 0.017453292F;
                  break;
               case "S":
                  var10000 = (0.0F + var26) * 0.017453292F;
                  break;
               case "W":
                  var10000 = (90.0F + var26) * 0.017453292F;
                  break;
               case "E":
                  var10000 = (270.0F + var26) * 0.017453292F;
                  break;
               default:
                  var10000 = var25;
            }

            var25 = var10000;
            var23.rotate(var25);
            var9.set(var14 - var23.x, var15 - var23.y);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean getAdjacentPosition(String var1, IsoSprite var2, String var3, String var4, Model var5, String var6, String var7, String var8, Vector2f var9) {
      return this.getAdjacentPosition(this.getSeatingDataRequired(var1), var2, var3, var4, var5, var6, var7, var8, var9);
   }

   private String getFacingDirection(SeatingData var1, String var2, int var3, int var4) {
      int var5 = var1.getPositionCount(var2, var3, var4);
      String var6 = var5 == 0 ? null : var1.getPositionID(var2, var3, var4, 0);
      if (var6 == null) {
         String var7 = String.format("%s_%d", var2, var3 + var4 * 8);
         IsoSprite var8 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var7);
         if (var8 == null) {
            return "S";
         }

         var6 = var8.getProperties().Val("Facing");
         if (var6 == null) {
            return "S";
         }
      }

      return var6;
   }

   public String getFacingDirection(String var1, String var2, int var3, int var4) {
      return this.getFacingDirection(this.getSeatingDataRequired(var1), var2, var3, var4);
   }

   public String getFacingDirection(String var1, int var2, int var3) {
      return this.getFacingDirection(this.m_mergedTilesets, var1, var2, var3);
   }

   public String getFacingDirection(IsoSprite var1) {
      return var1 != null && var1.tilesetName != null ? this.getFacingDirection(var1.tilesetName, var1.tileSheetIndex % 8, var1.tileSheetIndex / 8) : "S";
   }

   public String getFacingDirection(IsoObject var1) {
      return var1 == null ? "S" : this.getFacingDirection(var1.getSprite());
   }

   void getTotalDeferredMovement(SkinningData var1, AnimNode var2, Vector2 var3) {
      var3.set(0.0F, 0.0F);
      String var4 = var2.m_AnimName;
      String var5 = var2.getDeferredBoneName();
      BoneAxis var6 = var2.getDeferredBoneAxis();
      int var7 = (Integer)var1.BoneIndices.getOrDefault(var5, -1);
      AnimationClip var8 = (AnimationClip)var1.AnimationClips.get(var4);
      Keyframe[] var9 = var8.getBoneFramesAt(var7);
      Keyframe var10 = var9[0];
      Keyframe var11 = var9[var9.length - 1];
      Vector2 var12 = this.getDeferredMovement(var6, var10.Position, new Vector2());
      Vector2 var13 = this.getDeferredMovement(var6, var11.Position, new Vector2());
      var3.set(var13.x - var12.x, var13.y - var12.y);
   }

   public Vector2 getDeferredMovement(BoneAxis var1, org.lwjgl.util.vector.Vector3f var2, Vector2 var3) {
      if (var1 == BoneAxis.Y) {
         var3.set(var2.x, -var2.z);
      } else {
         var3.set(var2.x, var2.y);
      }

      return var3;
   }

   public float getAnimationTrackFraction(IsoGameCharacter var1, String var2) {
      AdvancedAnimator var3 = var1.getAdvancedAnimator();
      AnimLayer var4 = var3.getRootLayer();
      List var5 = var4.getLiveAnimNodes();

      for(int var6 = 0; var6 < var5.size(); ++var6) {
         LiveAnimNode var7 = (LiveAnimNode)var5.get(var6);
         if (var2.equalsIgnoreCase(var7.getName())) {
            for(int var8 = 0; var8 < var7.getMainAnimationTracksCount(); ++var8) {
               AnimationTrack var9 = var7.getMainAnimationTrackAt(var8);
               if (var9.IsPlaying) {
                  return var9.getCurrentTimeFraction();
               }
            }
         }
      }

      return -1.0F;
   }

   public void write(String var1) {
      this.getSeatingDataRequired(var1).write();
   }

   public void fixDefaultPositions() {
      Iterator var1 = this.m_modData.iterator();

      while(var1.hasNext()) {
         ModData var2 = (ModData)var1.next();
         var2.m_data.fixDefaultPositions();
      }

   }

   public void Reset() {
      Iterator var1 = this.m_modData.iterator();

      while(var1.hasNext()) {
         ModData var2 = (ModData)var1.next();
         var2.m_data.Reset();
      }

      this.m_modData.clear();
      this.m_mergedTilesets.Reset();
   }

   static final class ModData {
      final String m_modID;
      final String m_mediaAbsPath;
      final SeatingData m_data;

      ModData(String var1, String var2) {
         this.m_modID = var1;
         this.m_mediaAbsPath = var2;
         this.m_data = new SeatingData(var2);
      }
   }
}
