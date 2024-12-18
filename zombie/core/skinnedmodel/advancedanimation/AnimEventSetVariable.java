package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlTransient;

public class AnimEventSetVariable extends AnimEvent {
   @XmlTransient
   public AnimationVariableReference m_variableReference;
   @XmlTransient
   public String m_SetVariableValue;

   public AnimEventSetVariable(AnimEvent var1) {
      super(var1);
      String[] var2 = var1.m_ParameterValue.split("=");
      if (var2.length == 2) {
         this.m_variableReference = AnimationVariableReference.fromRawVariableName(var2[0]);
         this.m_SetVariableValue = var2[1];
      } else {
         this.m_variableReference = AnimationVariableReference.fromRawVariableName(var1.m_ParameterValue);
         this.m_SetVariableValue = "";
      }

   }
}
