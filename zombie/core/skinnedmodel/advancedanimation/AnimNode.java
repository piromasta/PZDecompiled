package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@XmlRootElement
public final class AnimNode {
   private static final Comparator<AnimEvent> s_eventsComparator = (var0, var1) -> {
      return Float.compare(var0.m_TimePc, var1.m_TimePc);
   };
   public String m_Name = "";
   @XmlTransient
   private boolean m_isIdleAnim = false;
   public int m_Priority = 5;
   public int m_ConditionPriority = 0;
   public String m_AnimName = "";
   public List<String> m_AlternateAnims = new ArrayList();
   public String m_MatchingGrappledAnimNode = "";
   public float m_GrappleOffsetForward = 0.0F;
   public float m_GrappleOffsetYaw = 0.0F;
   public float m_GrappleTweenInTime = 0.25F;
   public GrappleOffsetBehaviour m_GrapplerOffsetBehaviour;
   public boolean m_isRagdoll;
   public float m_chanceToRagdoll;
   public float m_ragdollStartTimeMin;
   public float m_ragdollStartTimeMax;
   public float m_ragdollMaxTime;
   public String m_DeferredBoneName;
   public BoneAxis m_deferredBoneAxis;
   public boolean m_useDeferedRotation;
   public boolean m_useDeferredMovement;
   public float m_deferredRotationScale;
   public boolean m_Looped;
   public float m_BlendTime;
   public float m_BlendOutTime;
   public boolean m_StopAnimOnExit;
   public boolean m_EarlyTransitionOut;
   public String m_SpeedScale;
   public String m_SpeedScaleVariable;
   public float m_SpeedScaleRandomMultiplierMin;
   public float m_SpeedScaleRandomMultiplierMax;
   @XmlTransient
   private float m_SpeedScaleF;
   public float m_randomAdvanceFraction;
   public float m_maxTorsoTwist;
   public String m_Scalar;
   public String m_Scalar2;
   public boolean m_AnimReverse;
   public boolean m_SyncTrackingEnabled;
   public boolean m_IKAimingLeftArm;
   public boolean m_IKAimingRightArm;
   public List<Anim2DBlend> m_2DBlends;
   public AnimCondition[] m_Conditions;
   public List<AnimEvent> m_Events;
   public List<Anim2DBlendTriangle> m_2DBlendTri;
   public List<AnimTransition> m_Transitions;
   public List<AnimBoneWeight> m_SubStateBoneWeights;
   @XmlTransient
   public Anim2DBlendPicker m_picker;
   @XmlTransient
   public AnimState m_State;
   @XmlTransient
   private AnimTransition m_transitionOut;

   public AnimNode() {
      this.m_GrapplerOffsetBehaviour = GrappleOffsetBehaviour.Grappled;
      this.m_isRagdoll = false;
      this.m_chanceToRagdoll = 1.0F;
      this.m_ragdollStartTimeMin = 0.0F;
      this.m_ragdollStartTimeMax = 0.0F;
      this.m_ragdollMaxTime = 5.0F;
      this.m_DeferredBoneName = "Translation_Data";
      this.m_deferredBoneAxis = BoneAxis.Y;
      this.m_useDeferedRotation = false;
      this.m_useDeferredMovement = true;
      this.m_deferredRotationScale = 1.0F;
      this.m_Looped = true;
      this.m_BlendTime = 0.0F;
      this.m_BlendOutTime = -1.0F;
      this.m_StopAnimOnExit = false;
      this.m_EarlyTransitionOut = false;
      this.m_SpeedScale = "1.00";
      this.m_SpeedScaleVariable = null;
      this.m_SpeedScaleRandomMultiplierMin = 1.0F;
      this.m_SpeedScaleRandomMultiplierMax = 1.0F;
      this.m_SpeedScaleF = 1.0F / 0.0F;
      this.m_randomAdvanceFraction = 0.0F;
      this.m_maxTorsoTwist = 15.0F;
      this.m_Scalar = "";
      this.m_Scalar2 = "";
      this.m_AnimReverse = false;
      this.m_SyncTrackingEnabled = true;
      this.m_IKAimingLeftArm = false;
      this.m_IKAimingRightArm = false;
      this.m_2DBlends = new ArrayList();
      this.m_Conditions = new AnimCondition[0];
      this.m_Events = new ArrayList();
      this.m_2DBlendTri = new ArrayList();
      this.m_Transitions = new ArrayList();
      this.m_SubStateBoneWeights = new ArrayList();
      this.m_State = null;
   }

   public static AnimNode Parse(String var0) {
      try {
         AnimNode var1 = (AnimNode)PZXmlUtil.parse(AnimNode.class, var0);
         var1.m_isIdleAnim = var1.m_Name.contains("Idle");
         if (var1.m_2DBlendTri.size() > 0) {
            var1.m_picker = new Anim2DBlendPicker();
            var1.m_picker.SetPickTriangles(var1.m_2DBlendTri);
         }

         AnimCondition[] var2 = var1.m_Conditions;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            AnimCondition var5 = var2[var4];
            var5.parse(var1, (AnimNode)null);
         }

         PZArrayUtil.forEachReplace(var1.m_Events, (var0x) -> {
            if ("SetVariable".equalsIgnoreCase(var0x.m_EventName)) {
               AnimEventSetVariable var2 = new AnimEventSetVariable(var0x);
               return var2;
            } else if ("FlagWhileAlive".equalsIgnoreCase(var0x.m_EventName)) {
               AnimEventFlagWhileAlive var1 = new AnimEventFlagWhileAlive(var0x);
               return var1;
            } else {
               return var0x;
            }
         });
         var1.m_Events.sort(s_eventsComparator);

         Iterator var8;
         AnimEvent var10;
         for(var8 = var1.m_Events.iterator(); var8.hasNext(); var10.m_parentAnimNode = var1) {
            var10 = (AnimEvent)var8.next();
         }

         try {
            var1.m_SpeedScaleF = Float.parseFloat(var1.m_SpeedScale);
         } catch (NumberFormatException var6) {
            var1.m_SpeedScaleVariable = var1.m_SpeedScale;
         }

         if (var1.m_SubStateBoneWeights.isEmpty()) {
            var1.m_SubStateBoneWeights.add(new AnimBoneWeight("Bip01_Spine1", 0.5F));
            var1.m_SubStateBoneWeights.add(new AnimBoneWeight("Bip01_Neck", 1.0F));
            var1.m_SubStateBoneWeights.add(new AnimBoneWeight("Bip01_BackPack", 1.0F));
            var1.m_SubStateBoneWeights.add(new AnimBoneWeight("Bip01_Prop1", 1.0F));
            var1.m_SubStateBoneWeights.add(new AnimBoneWeight("Bip01_Prop2", 1.0F));
         }

         for(int var9 = 0; var9 < var1.m_SubStateBoneWeights.size(); ++var9) {
            AnimBoneWeight var11 = (AnimBoneWeight)var1.m_SubStateBoneWeights.get(var9);
            var11.boneName = JAssImpImporter.getSharedString(var11.boneName, "AnimBoneWeight.boneName");
         }

         var1.m_transitionOut = null;
         var8 = var1.m_Transitions.iterator();

         while(var8.hasNext()) {
            AnimTransition var12 = (AnimTransition)var8.next();
            if (StringUtils.isNullOrWhitespace(var12.m_Target)) {
               var1.m_transitionOut = var12;
            }
         }

         return var1;
      } catch (PZXmlParserException var7) {
         System.err.println("AnimNode.Parse threw an exception reading file: " + var0);
         ExceptionLogger.logException(var7);
         return null;
      }
   }

   public boolean checkConditions(IAnimationVariableSource var1) {
      return AnimCondition.pass(var1, this.m_Conditions);
   }

   public float getSpeedScale(IAnimationVariableSource var1) {
      return this.m_SpeedScaleF != 1.0F / 0.0F ? this.m_SpeedScaleF : var1.getVariableFloat(this.m_SpeedScale, 1.0F);
   }

   public boolean isIdleAnim() {
      return this.m_isIdleAnim;
   }

   public AnimTransition findTransitionTo(IAnimationVariableSource var1, AnimNode var2) {
      DebugLog.AnimationDetailed.debugln("  FindingTransition: <%s|%s>", this.m_AnimName, var2.m_Name);
      AnimTransition var3 = null;
      int var4 = 0;

      for(int var5 = this.m_Transitions.size(); var4 < var5; ++var4) {
         AnimTransition var6 = (AnimTransition)this.m_Transitions.get(var4);
         DebugLog.AnimationDetailed.debugln("    Transitions: <%s>", var6.m_Target);
         if (StringUtils.equalsIgnoreCase(var6.m_Target, var2.m_Name)) {
            var6.parse(this, var2);
            if (AnimCondition.pass(var1, var6.m_Conditions)) {
               DebugLog.AnimationDetailed.debugln("    *** Matched Transition: <%s> From Node <%s>", var6.m_Target, this.m_AnimName);
               var3 = var6;
               break;
            }
         }
      }

      return var3;
   }

   public String toString() {
      return String.format("AnimNode{ Name: %s, AnimName: %s, Conditions: %s }", this.m_Name, this.m_AnimName, this.getConditionsString());
   }

   public String getConditionsString() {
      return PZArrayUtil.arrayToString((Object[])this.m_Conditions, AnimCondition::getConditionString, "( ", " )", ", ");
   }

   public boolean isAbstract() {
      if (this.isRagdoll()) {
         return false;
      } else if (!StringUtils.isNullOrWhitespace(this.m_AnimName)) {
         return false;
      } else if (this.m_AlternateAnims.size() > 0) {
         return false;
      } else {
         return this.m_2DBlends.isEmpty();
      }
   }

   public float getBlendOutTime() {
      if (this.m_transitionOut != null) {
         return this.m_transitionOut.m_blendOutTime;
      } else {
         return this.m_BlendOutTime >= 0.0F ? this.m_BlendOutTime : this.m_BlendTime;
      }
   }

   public String getDeferredBoneName() {
      return StringUtils.isNullOrWhitespace(this.m_DeferredBoneName) ? "Translation_Data" : this.m_DeferredBoneName;
   }

   public BoneAxis getDeferredBoneAxis() {
      return this.m_deferredBoneAxis;
   }

   public int getPriority() {
      return this.m_Priority;
   }

   public int compareSelectionConditions(AnimNode var1) {
      return compareSelectionConditions(this, var1);
   }

   public static int compareSelectionConditions(AnimNode var0, AnimNode var1) {
      if (var0.isAbstract() != var1.isAbstract()) {
         return var0.isAbstract() ? -1 : 1;
      } else if (var0.m_ConditionPriority < var1.m_ConditionPriority) {
         return -1;
      } else if (var0.m_ConditionPriority > var1.m_ConditionPriority) {
         return 1;
      } else if (var0.m_Conditions.length < var1.m_Conditions.length) {
         return -1;
      } else {
         return var0.m_Conditions.length > var1.m_Conditions.length ? 1 : 0;
      }
   }

   public String getMatchingGrappledAnimNode() {
      return this.m_MatchingGrappledAnimNode;
   }

   public boolean isGrappler() {
      return !StringUtils.isNullOrWhitespace(this.m_MatchingGrappledAnimNode);
   }

   public boolean isRagdoll() {
      return this.m_isRagdoll;
   }

   public float getRagdollMaxTime() {
      return this.m_ragdollMaxTime;
   }

   public String getRandomAnim() {
      ArrayList var1 = new ArrayList();
      if (!StringUtils.isNullOrEmpty(this.m_AnimName)) {
         var1.add(this.m_AnimName);
      }

      var1.addAll(this.m_AlternateAnims);
      if (var1.size() < 1) {
         DebugType.Animation.error("animNames is empty! isAbstract() = %s", this.isAbstract() ? "true" : "false");
         return "";
      } else {
         int var2 = Rand.Next(var1.size());
         return (String)var1.get(var2);
      }
   }
}
