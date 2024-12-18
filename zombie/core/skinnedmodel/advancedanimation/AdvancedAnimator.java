package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.CharacterActionAnims;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.utils.TransitionNodeProxy;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.Lambda;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

public final class AdvancedAnimator implements IAnimEventCallback {
   private IAnimatable character;
   public AnimationSet animSet;
   public final ArrayList<IAnimEventCallback> animCallbackHandlers = new ArrayList();
   private AnimLayer m_rootLayer = null;
   private final List<SubLayerSlot> m_subLayers = new ArrayList();
   public static float s_MotionScale = 0.76F;
   public static float s_RotationScale = 0.76F;
   private static AnimatorDebugMonitor debugMonitor;
   private static long animSetModificationTime = -1L;
   private static long actionGroupModificationTime = -1L;
   private final AnimationVariableWhileAliveFlagsContainer m_setFlagCounters = new AnimationVariableWhileAliveFlagsContainer();
   private AnimationPlayerRecorder m_recorder = null;

   public AdvancedAnimator() {
   }

   public static void systemInit() {
      DebugFileWatcher.instance.add(new PredicatedFileWatcher("media/AnimSets", AdvancedAnimator::isAnimSetFilePath, AdvancedAnimator::onAnimSetsRefreshTriggered));
      DebugFileWatcher.instance.add(new PredicatedFileWatcher("media/actiongroups", AdvancedAnimator::isActionGroupFilePath, AdvancedAnimator::onActionGroupsRefreshTriggered));
      LoadDefaults();
   }

   private static boolean isAnimSetFilePath(String var0) {
      if (var0 == null) {
         return false;
      } else if (!var0.endsWith(".xml")) {
         return false;
      } else {
         ArrayList var1 = ZomboidFileSystem.instance.getModIDs();

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            String var3 = (String)var1.get(var2);
            ChooseGameInfo.Mod var4 = ChooseGameInfo.getModDetails(var3);
            if (var4 != null && var4.animSetsFile != null && var4.animSetsFile.common.canonicalFile != null && var0.startsWith(var4.animSetsFile.common.canonicalFile.getPath())) {
               return true;
            }

            if (var4 != null && var4.animSetsFile != null && var4.animSetsFile.version.canonicalFile != null && var0.startsWith(var4.animSetsFile.version.canonicalFile.getPath())) {
               return true;
            }
         }

         String var5 = ZomboidFileSystem.instance.getAnimSetsPath();
         if (!var0.startsWith(var5)) {
            return false;
         } else {
            return true;
         }
      }
   }

   private static boolean isActionGroupFilePath(String var0) {
      if (var0 == null) {
         return false;
      } else if (!var0.endsWith(".xml")) {
         return false;
      } else {
         ArrayList var1 = ZomboidFileSystem.instance.getModIDs();

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            String var3 = (String)var1.get(var2);
            ChooseGameInfo.Mod var4 = ChooseGameInfo.getModDetails(var3);
            if (var4 != null && var4.actionGroupsFile != null && var4.actionGroupsFile.common.canonicalFile != null && var0.startsWith(var4.actionGroupsFile.common.canonicalFile.getPath())) {
               return true;
            }

            if (var4 != null && var4.actionGroupsFile != null && var4.actionGroupsFile.version.canonicalFile != null && var0.startsWith(var4.actionGroupsFile.version.canonicalFile.getPath())) {
               return true;
            }
         }

         String var5 = ZomboidFileSystem.instance.getActionGroupsPath();
         if (!var0.startsWith(var5)) {
            return false;
         } else {
            return true;
         }
      }
   }

   private static void onActionGroupsRefreshTriggered(String var0) {
      DebugLog.General.println("DebugFileWatcher Hit. ActionGroups: " + var0);
      actionGroupModificationTime = System.currentTimeMillis() + 1000L;
   }

   private static void onAnimSetsRefreshTriggered(String var0) {
      DebugLog.General.println("DebugFileWatcher Hit. AnimSets: " + var0);
      animSetModificationTime = System.currentTimeMillis() + 1000L;
   }

   public static void checkModifiedFiles() {
      if (animSetModificationTime != -1L && animSetModificationTime < System.currentTimeMillis()) {
         DebugLog.General.println("Refreshing AnimSets.");
         animSetModificationTime = -1L;
         LoadDefaults();
         LuaManager.GlobalObject.refreshAnimSets(true);
      }

      if (actionGroupModificationTime != -1L && actionGroupModificationTime < System.currentTimeMillis()) {
         DebugLog.General.println("Refreshing action groups.");
         actionGroupModificationTime = -1L;
         LuaManager.GlobalObject.reloadActionGroups();
      }

   }

   private static void LoadDefaults() {
      try {
         Element var0 = PZXmlUtil.parseXml("media/AnimSets/Defaults.xml");
         String var1 = var0.getElementsByTagName("MotionScale").item(0).getTextContent();
         s_MotionScale = Float.parseFloat(var1);
         String var2 = var0.getElementsByTagName("RotationScale").item(0).getTextContent();
         s_RotationScale = Float.parseFloat(var2);
      } catch (PZXmlParserException var3) {
         DebugLog.General.error("Exception thrown: " + var3);
         var3.printStackTrace();
      }

   }

   public String GetDebug() {
      StringBuilder var1 = new StringBuilder();
      var1.append("GameState: ");
      if (this.character instanceof IsoGameCharacter) {
         IsoGameCharacter var2 = (IsoGameCharacter)this.character;
         var1.append(var2.getCurrentState() == null ? "null" : var2.getCurrentState().getClass().getSimpleName()).append("\n");
      }

      if (this.m_rootLayer != null) {
         var1.append("Layer: ").append(0).append("\n");
         var1.append(this.m_rootLayer.GetDebugString()).append("\n");
      }

      for(int var4 = 0; var4 < this.m_subLayers.size(); ++var4) {
         SubLayerSlot var3 = (SubLayerSlot)this.m_subLayers.get(var4);
         if (var3.shouldBeActive) {
            var1.append("SubLayer: ").append(var4).append("\n");
            var1.append(var3.animLayer.GetDebugString()).append("\n");
         }
      }

      var1.append("Variables:\n");
      var1.append("Weapon: ").append(this.character.getVariableString("weapon")).append("\n");
      var1.append("Aim: ").append(this.character.getVariableString("aim")).append("\n");
      Iterator var5 = this.character.getGameVariables().iterator();

      while(var5.hasNext()) {
         IAnimationVariableSlot var6 = (IAnimationVariableSlot)var5.next();
         var1.append("  ").append(var6.getKey()).append(" : ").append(var6.getValueString()).append("\n");
      }

      return var1.toString();
   }

   public void OnAnimDataChanged(boolean var1) {
      if (var1 && this.character instanceof IsoGameCharacter) {
         IsoGameCharacter var2 = (IsoGameCharacter)this.character;
         ++var2.getStateMachine().activeStateChanged;
         var2.setDefaultState();
         if (var2 instanceof IsoZombie) {
            var2.setOnFloor(false);
         }

         --var2.getStateMachine().activeStateChanged;
      }

      this.SetAnimSet(AnimationSet.GetAnimationSet(this.character.GetAnimSetName(), false));
      if (this.character.getAnimationPlayer() != null) {
         this.character.getAnimationPlayer().reset();
      }

      if (this.m_rootLayer != null) {
         this.m_rootLayer.Reset();
      }

      for(int var4 = 0; var4 < this.m_subLayers.size(); ++var4) {
         SubLayerSlot var3 = (SubLayerSlot)this.m_subLayers.get(var4);
         var3.animLayer.Reset();
      }

   }

   public void Reset() {
      if (this.m_rootLayer != null) {
         this.m_rootLayer.Reset();
      }

      for(int var1 = 0; var1 < this.m_subLayers.size(); ++var1) {
         SubLayerSlot var2 = (SubLayerSlot)this.m_subLayers.get(var1);
         var2.animLayer.Reset();
      }

   }

   public void Reload() {
   }

   public void init(IAnimatable var1) {
      this.character = var1;
      this.m_rootLayer = new AnimLayer(var1, this);
   }

   public void SetAnimSet(AnimationSet var1) {
      this.animSet = var1;
   }

   public void OnAnimEvent(AnimLayer var1, AnimEvent var2) {
      for(int var3 = 0; var3 < this.animCallbackHandlers.size(); ++var3) {
         IAnimEventCallback var4 = (IAnimEventCallback)this.animCallbackHandlers.get(var3);
         var4.OnAnimEvent(var1, var2);
      }

   }

   public String getCurrentStateName() {
      return this.m_rootLayer == null ? null : this.m_rootLayer.getCurrentStateName();
   }

   public boolean containsState(String var1) {
      return this.animSet != null && this.animSet.containsState(var1);
   }

   public void SetState(String var1) {
      this.SetState(var1, PZArrayList.emptyList());
   }

   public void SetState(String var1, List<String> var2) {
      if (this.animSet == null) {
         DebugLog.Animation.error("(" + var1 + ") Cannot set state. AnimSet is null.");
      } else {
         if (!this.animSet.containsState(var1)) {
            DebugLog.Animation.error("State not found: " + var1);
         }

         this.m_rootLayer.TransitionTo(this.animSet.GetState(var1), false);
         PZArrayUtil.forEach(this.m_subLayers, (var0) -> {
            var0.shouldBeActive = false;
         });
         DebugLog.AnimationDetailed.debugln("*** SetState: <%s>", var1);
         Lambda.forEachFrom(PZArrayUtil::forEach, (List)var2, this, (var0, var1x) -> {
            DebugLog.AnimationDetailed.debugln("  SetSubState: <%s>", var0);
            SubLayerSlot var2 = var1x.getOrCreateSlot(var0);
            var2.transitionTo(var1x.animSet.GetState(var0), false);
         });
         if (var2.isEmpty()) {
            DebugLog.AnimationDetailed.debugln("  SetSubState: NoneToSet");
         }

         PZArrayUtil.forEach(this.m_subLayers, SubLayerSlot::applyTransition);
      }
   }

   protected SubLayerSlot getOrCreateSlot(String var1) {
      SubLayerSlot var2 = null;
      int var3 = 0;

      int var4;
      SubLayerSlot var5;
      for(var4 = this.m_subLayers.size(); var3 < var4; ++var3) {
         var5 = (SubLayerSlot)this.m_subLayers.get(var3);
         if (var5.animLayer.isCurrentState(var1)) {
            var2 = var5;
            break;
         }
      }

      if (var2 != null) {
         return var2;
      } else {
         var3 = 0;

         for(var4 = this.m_subLayers.size(); var3 < var4; ++var3) {
            var5 = (SubLayerSlot)this.m_subLayers.get(var3);
            if (var5.animLayer.isStateless()) {
               var2 = var5;
               break;
            }
         }

         if (var2 != null) {
            return var2;
         } else {
            SubLayerSlot var6 = new SubLayerSlot(this.m_rootLayer, this.character, this);
            this.m_subLayers.add(var6);
            return var6;
         }
      }
   }

   public void update(float var1) {
      GameProfiler.getInstance().invokeAndMeasure("AdvancedAnimator.Update", this, var1, AdvancedAnimator::updateInternal);
   }

   private void updateInternal(float var1) {
      if (this.character.getAnimationPlayer() != null) {
         if (this.character.getAnimationPlayer().isReady()) {
            if (this.animSet != null) {
               if (!this.m_rootLayer.hasState()) {
                  this.m_rootLayer.TransitionTo(this.animSet.GetState("Idle"), true);
               }

               this.m_rootLayer.UpdateLiveAnimNodes();

               int var2;
               SubLayerSlot var3;
               for(var2 = 0; var2 < this.m_subLayers.size(); ++var2) {
                  var3 = (SubLayerSlot)this.m_subLayers.get(var2);
                  var3.animLayer.UpdateLiveAnimNodes();
               }

               this.GenerateTransitionData();
               this.m_rootLayer.Update(var1);

               for(var2 = 0; var2 < this.m_subLayers.size(); ++var2) {
                  var3 = (SubLayerSlot)this.m_subLayers.get(var2);
                  var3.update(var1);
               }

               if (debugMonitor != null && this.character instanceof IsoGameCharacter) {
                  if (debugMonitor.getTarget() != this.character) {
                     return;
                  }

                  var2 = 1 + this.getActiveSubLayerCount();
                  AnimLayer[] var6 = new AnimLayer[var2];
                  var6[0] = this.m_rootLayer;
                  var2 = 0;

                  for(int var4 = 0; var4 < this.m_subLayers.size(); ++var4) {
                     SubLayerSlot var5 = (SubLayerSlot)this.m_subLayers.get(var4);
                     if (var5.shouldBeActive) {
                        var6[1 + var2] = var5.animLayer;
                        ++var2;
                     }
                  }

                  debugMonitor.update((IsoGameCharacter)this.character, var6);
               }

            }
         }
      }
   }

   private void GenerateTransitionData() {
      TransitionNodeProxy var1 = new TransitionNodeProxy();
      this.m_rootLayer.FindTransitioningLiveAnimNode(var1, true);

      int var2;
      for(var2 = 0; var2 < this.m_subLayers.size(); ++var2) {
         SubLayerSlot var3 = (SubLayerSlot)this.m_subLayers.get(var2);
         var3.animLayer.FindTransitioningLiveAnimNode(var1, false);
      }

      if (!var1.m_allNewNodes.isEmpty() || !var1.m_allOutgoingNodes.isEmpty()) {
         DebugLog.AnimationDetailed.debugln("************* New Nodes *************");

         for(var2 = 0; var2 < var1.m_allNewNodes.size(); ++var2) {
            DebugLog.AnimationDetailed.debugln("  %s", ((TransitionNodeProxy.NodeLayerPair)var1.m_allNewNodes.get(var2)).liveAnimNode.getName());
         }

         DebugLog.AnimationDetailed.debugln("************* Out Nodes *************");

         for(var2 = 0; var2 < var1.m_allOutgoingNodes.size(); ++var2) {
            DebugLog.AnimationDetailed.debugln("  %s", ((TransitionNodeProxy.NodeLayerPair)var1.m_allOutgoingNodes.get(var2)).liveAnimNode.getName());
         }

         DebugLog.AnimationDetailed.debugln("*************************************");
      }

      if (var1.HasAnyPossibleTransitions()) {
         this.FindTransitionsFromProxy(var1);
         this.ProcessTransitions(var1);
      }
   }

   public void FindTransitionsFromProxy(TransitionNodeProxy var1) {
      for(int var2 = 0; var2 < var1.m_allNewNodes.size(); ++var2) {
         TransitionNodeProxy.NodeLayerPair var3 = (TransitionNodeProxy.NodeLayerPair)var1.m_allNewNodes.get(var2);
         AnimNode var4 = var3.liveAnimNode.getSourceNode();

         for(boolean var5 = false; var2 < var1.m_allOutgoingNodes.size(); ++var2) {
            TransitionNodeProxy.NodeLayerPair var6 = (TransitionNodeProxy.NodeLayerPair)var1.m_allOutgoingNodes.get(var2);
            if (var4 != var6.liveAnimNode.getSourceNode()) {
               AnimTransition var7 = var6.liveAnimNode.findTransitionTo(this.character, var3.liveAnimNode.getSourceNode());
               if (var7 != null) {
                  TransitionNodeProxy.TransitionNodeProxyData var8 = new TransitionNodeProxy.TransitionNodeProxyData();
                  var8.m_animLayerIn = var3.animLayer;
                  var8.m_NewAnimNode = var3.liveAnimNode;
                  var8.m_animLayerOut = var6.animLayer;
                  var8.m_OldAnimNode = var6.liveAnimNode;
                  var8.m_transitionOut = var7;
                  var1.m_foundTransitions.add(var8);
                  DebugLog.AnimationDetailed.debugln("** NEW ** Anim: <%s>; <%s>; this: <%s>", var8.m_NewAnimNode.getName(), var8.m_transitionOut != null ? "true" : "false", this.toString());
               }
            }
         }
      }

   }

   public void ProcessTransitions(TransitionNodeProxy var1) {
      for(int var2 = 0; var2 < var1.m_foundTransitions.size(); ++var2) {
         TransitionNodeProxy.TransitionNodeProxyData var3 = (TransitionNodeProxy.TransitionNodeProxyData)var1.m_foundTransitions.get(var2);
         AnimationTrack var4 = var3.m_animLayerOut.startTransitionAnimation(var3);
         var3.m_NewAnimNode.startTransitionIn(var3.m_OldAnimNode, var3.m_transitionOut, var4);
         var3.m_OldAnimNode.setTransitionOut(var3.m_transitionOut);
      }

   }

   public void render() {
      if (this.character.getAnimationPlayer() != null) {
         if (this.character.getAnimationPlayer().isReady()) {
            if (this.animSet != null) {
               if (this.m_rootLayer.hasState()) {
                  this.m_rootLayer.render();
               }
            }
         }
      }
   }

   public void printDebugCharacterActions(String var1) {
      if (this.animSet != null) {
         AnimState var2 = this.animSet.GetState("actions");
         if (var2 != null) {
            boolean var4 = false;
            boolean var5 = false;
            CharacterActionAnims[] var7 = CharacterActionAnims.values();
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               CharacterActionAnims var10 = var7[var9];
               var4 = false;
               String var6;
               if (var10 == CharacterActionAnims.None) {
                  var6 = var1;
                  var4 = true;
               } else {
                  var6 = var10.toString();
               }

               boolean var3 = false;
               Iterator var11 = var2.m_Nodes.iterator();

               while(var11.hasNext()) {
                  AnimNode var12 = (AnimNode)var11.next();
                  AnimCondition[] var13 = var12.m_Conditions;
                  int var14 = var13.length;

                  for(int var15 = 0; var15 < var14; ++var15) {
                     AnimCondition var16 = var13[var15];
                     if (var16.m_Type == AnimCondition.Type.STRING && var16.m_Name.toLowerCase().equals("performingaction") && var16.m_StringValue.equalsIgnoreCase(var6)) {
                        var3 = true;
                        break;
                     }
                  }

                  if (var3) {
                     break;
                  }
               }

               if (var3) {
                  if (var4) {
                     var5 = true;
                  }
               } else {
                  DebugLog.log("WARNING: did not find node with condition 'PerformingAction = " + var6 + "' in player/actions/");
               }
            }

            if (var5) {
               if (DebugLog.isEnabled(DebugType.Animation)) {
                  DebugLog.Animation.debugln("SUCCESS - Current 'actions' TargetNode: '" + var1 + "' was found.");
               }
            } else if (DebugLog.isEnabled(DebugType.Animation)) {
               DebugLog.Animation.debugln("FAIL - Current 'actions' TargetNode: '" + var1 + "' not found.");
            }
         }
      }

   }

   public ArrayList<String> debugGetVariables() {
      ArrayList var1 = new ArrayList();
      if (this.animSet != null) {
         Iterator var2 = this.animSet.states.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            AnimState var4 = (AnimState)var3.getValue();
            Iterator var5 = var4.m_Nodes.iterator();

            while(var5.hasNext()) {
               AnimNode var6 = (AnimNode)var5.next();
               AnimCondition[] var7 = var6.m_Conditions;
               int var8 = var7.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  AnimCondition var10 = var7[var9];
                  if (var10.m_Name != null && !var1.contains(var10.m_Name.toLowerCase())) {
                     var1.add(var10.m_Name.toLowerCase());
                  }
               }
            }
         }
      }

      return var1;
   }

   public AnimatorDebugMonitor getDebugMonitor() {
      return debugMonitor;
   }

   public void setDebugMonitor(AnimatorDebugMonitor var1) {
      debugMonitor = var1;
   }

   public IAnimatable getCharacter() {
      return this.character;
   }

   public void updateSpeedScale(String var1, float var2) {
      if (this.m_rootLayer != null) {
         List var3 = this.m_rootLayer.getLiveAnimNodes();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            LiveAnimNode var5 = (LiveAnimNode)var3.get(var4);
            if (var5.isActive() && var5.getSourceNode() != null && var1.equals(var5.getSourceNode().m_SpeedScaleVariable)) {
               var5.getSourceNode().m_SpeedScale = "" + var2;

               for(int var6 = 0; var6 < var5.getMainAnimationTracksCount(); ++var6) {
                  var5.getMainAnimationTrackAt(var6).SpeedDelta = var2;
               }
            }
         }
      }

   }

   public boolean containsAnyIdleNodes() {
      if (this.m_rootLayer == null) {
         return false;
      } else {
         boolean var1 = false;
         List var2 = this.m_rootLayer.getLiveAnimNodes();

         int var3;
         for(var3 = 0; var3 < var2.size() && !var1; ++var3) {
            var1 = ((LiveAnimNode)var2.get(var3)).isIdleAnimActive();
         }

         for(var3 = 0; var3 < this.getSubLayerCount(); ++var3) {
            AnimLayer var4 = this.getSubLayerAt(var3);
            var2 = var4.getLiveAnimNodes();

            for(int var5 = 0; var5 < var2.size(); ++var5) {
               var1 = ((LiveAnimNode)var2.get(var5)).isIdleAnimActive();
               if (!var1) {
                  break;
               }
            }
         }

         return var1;
      }
   }

   public AnimLayer getRootLayer() {
      return this.m_rootLayer;
   }

   public int getSubLayerCount() {
      return this.m_subLayers.size();
   }

   public AnimLayer getSubLayerAt(int var1) {
      return ((SubLayerSlot)this.m_subLayers.get(var1)).animLayer;
   }

   public int getActiveSubLayerCount() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.m_subLayers.size(); ++var2) {
         SubLayerSlot var3 = (SubLayerSlot)this.m_subLayers.get(var2);
         if (var3.shouldBeActive) {
            ++var1;
         }
      }

      return var1;
   }

   public void setRecorder(AnimationPlayerRecorder var1) {
      this.m_recorder = var1;
   }

   public boolean isRecording() {
      return this.m_recorder != null && this.m_recorder.isRecording();
   }

   public void incrementWhileAliveFlag(AnimationVariableReference var1) {
      int var2 = this.m_setFlagCounters.incrementWhileAliveFlag(var1);
      DebugType.Animation.trace("Variable: %s. Count: %d", var1, var2);
      var1.setVariable(this.getCharacter(), var2 > 0);
   }

   public void decrementWhileAliveFlag(AnimationVariableReference var1) {
      int var2 = this.m_setFlagCounters.decrementWhileAliveFlag(var1);
      DebugType.Animation.trace("Variable: %s. Count: %d", var1, var2);
      var1.setVariable(this.getCharacter(), var2 > 0);
   }

   public static class SubLayerSlot {
      public boolean shouldBeActive = false;
      public final AnimLayer animLayer;

      public SubLayerSlot(AnimLayer var1, IAnimatable var2, AdvancedAnimator var3) {
         this.animLayer = new AnimLayer(var1, var2, var3);
      }

      public void update(float var1) {
         for(int var2 = 0; var2 < this.animLayer.getLiveAnimNodes().size(); ++var2) {
            LiveAnimNode var3 = (LiveAnimNode)this.animLayer.getLiveAnimNodes().get(var2);
            DebugLog.AnimationDetailed.debugln("  Anim: <%d> : <%s>", var2, var3.getName());
         }

         this.animLayer.Update(var1);
      }

      public void transitionTo(AnimState var1, boolean var2) {
         DebugLog.AnimationDetailed.debugln("SubLayerSlot: TransitionTo: from Anim <%s> to State <%s>", this.animLayer.getLiveAnimNodes().isEmpty() ? "NoAnim" : ((LiveAnimNode)this.animLayer.getLiveAnimNodes().get(0)).getName(), var1 != null ? var1.m_Name : "NoState");
         this.animLayer.TransitionTo(var1, var2);
         this.shouldBeActive = var1 != null;
      }

      public void applyTransition() {
         if (!this.shouldBeActive) {
            this.transitionTo((AnimState)null, false);
         }

      }
   }
}
