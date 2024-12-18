package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.core.skinnedmodel.animation.BoneAxis;

@XmlRootElement
public final class AnimTransition {
   public String m_Source;
   public String m_Target;
   public String m_AnimName;
   public String m_DeferredBoneName = null;
   public BoneAxis m_deferredBoneAxis;
   public boolean m_useDeferedRotation;
   public boolean m_useDeferredMovement;
   public float m_deferredRotationScale;
   public float m_SyncAdjustTime;
   public float m_blendInTime;
   public float m_blendOutTime;
   public float m_speedScale;
   @XmlTransient
   private boolean m_isParsed;
   public AnimCondition[] m_Conditions;

   public AnimTransition() {
      this.m_deferredBoneAxis = BoneAxis.Y;
      this.m_useDeferedRotation = false;
      this.m_useDeferredMovement = true;
      this.m_deferredRotationScale = 1.0F;
      this.m_SyncAdjustTime = 0.0F;
      this.m_blendInTime = 1.0F / 0.0F;
      this.m_blendOutTime = 1.0F / 0.0F;
      this.m_speedScale = 1.0F / 0.0F;
      this.m_isParsed = false;
      this.m_Conditions = new AnimCondition[0];
   }

   public void parse(AnimNode var1, AnimNode var2) {
      if (!this.m_isParsed) {
         AnimCondition[] var3 = this.m_Conditions;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            AnimCondition var6 = var3[var5];
            var6.parse(var1, var2);
         }

         this.m_isParsed = true;
      }
   }
}
