package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlTransient;

public class AnimEventFlagWhileAlive extends AnimEvent {
   @XmlTransient
   public AnimationVariableReference m_variableReference;

   public AnimEventFlagWhileAlive(AnimEvent var1) {
      super(var1);
      this.m_variableReference = AnimationVariableReference.fromRawVariableName(var1.m_ParameterValue);
   }
}
