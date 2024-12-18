package zombie.core.utils;

import java.util.ArrayList;
import java.util.List;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimTransition;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.util.StringUtils;

public final class TransitionNodeProxy {
   public ArrayList<NodeLayerPair> m_allNewNodes = new ArrayList();
   public List<NodeLayerPair> m_allOutgoingNodes = new ArrayList();
   public List<TransitionNodeProxyData> m_foundTransitions = new ArrayList();

   public TransitionNodeProxy() {
   }

   public Boolean HasAnyPossibleTransitions() {
      return !this.m_allNewNodes.isEmpty() && !this.m_allOutgoingNodes.isEmpty();
   }

   public static class NodeLayerPair {
      public LiveAnimNode liveAnimNode = null;
      public AnimLayer animLayer = null;

      public NodeLayerPair(LiveAnimNode var1, AnimLayer var2) {
         this.liveAnimNode = var1;
         this.animLayer = var2;
      }
   }

   public static class TransitionNodeProxyData {
      public LiveAnimNode m_NewAnimNode = null;
      public LiveAnimNode m_OldAnimNode = null;
      public AnimTransition m_transitionOut = null;
      public AnimLayer m_animLayerIn = null;
      public AnimLayer m_animLayerOut = null;

      public TransitionNodeProxyData() {
      }

      public Boolean HasValidAnimNodes() {
         return this.m_NewAnimNode != null && this.m_OldAnimNode != null;
      }

      public Boolean HasValidTransitions() {
         return this.m_transitionOut != null;
      }

      void reset() {
         this.m_NewAnimNode = null;
         this.m_OldAnimNode = null;
         this.m_transitionOut = null;
      }

      private boolean isUsingAnimNodesDeferredInfo() {
         return this.m_transitionOut == null || StringUtils.isNullOrWhitespace(this.m_transitionOut.m_DeferredBoneName);
      }

      public String getDeferredBoneName() {
         return this.isUsingAnimNodesDeferredInfo() ? this.m_OldAnimNode.getDeferredBoneName() : this.m_transitionOut.m_DeferredBoneName;
      }

      public BoneAxis getDeferredBoneAxis() {
         return this.isUsingAnimNodesDeferredInfo() ? this.m_OldAnimNode.getDeferredBoneAxis() : this.m_transitionOut.m_deferredBoneAxis;
      }

      public boolean getUseDeferredRotation() {
         return this.isUsingAnimNodesDeferredInfo() ? this.m_OldAnimNode.getUseDeferredRotation() : this.m_transitionOut.m_useDeferedRotation;
      }

      public boolean getUseDeferredMovement() {
         return this.isUsingAnimNodesDeferredInfo() ? this.m_OldAnimNode.getUseDeferredMovement() : this.m_transitionOut.m_useDeferredMovement;
      }

      public float getDeferredRotationScale() {
         return this.isUsingAnimNodesDeferredInfo() ? this.m_OldAnimNode.getDeferredRotationScale() : this.m_transitionOut.m_deferredRotationScale;
      }
   }
}
