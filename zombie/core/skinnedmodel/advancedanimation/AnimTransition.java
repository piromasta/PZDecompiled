package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public final class AnimTransition {
   public String m_Target;
   public String m_AnimName;
   public float m_SyncAdjustTime = 0.0F;
   public float m_blendInTime = 1.0F / 0.0F;
   public float m_blendOutTime = 1.0F / 0.0F;
   public float m_speedScale = 1.0F / 0.0F;
   public List<AnimCondition> m_Conditions = new ArrayList();

   public AnimTransition() {
   }
}
