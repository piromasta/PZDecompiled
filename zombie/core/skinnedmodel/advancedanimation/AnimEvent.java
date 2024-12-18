package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlTransient;

public class AnimEvent {
   public String m_EventName;
   public AnimEventTime m_Time;
   public float m_TimePc;
   public String m_ParameterValue;
   @XmlTransient
   public AnimNode m_parentAnimNode;

   public AnimEvent() {
      this.m_Time = AnimEvent.AnimEventTime.Percentage;
      this.m_parentAnimNode = null;
   }

   public AnimEvent(AnimEvent var1) {
      this.m_Time = AnimEvent.AnimEventTime.Percentage;
      this.m_parentAnimNode = null;
      this.m_EventName = var1.m_EventName;
      this.m_Time = var1.m_Time;
      this.m_TimePc = var1.m_TimePc;
      this.m_ParameterValue = var1.m_ParameterValue;
   }

   public String toString() {
      return String.format("%s { %s }", this.getClass().getName(), this.toDetailsString());
   }

   public String toDetailsString() {
      return String.format("Details: %s %s, time: %s", this.m_EventName, this.m_ParameterValue, this.m_Time == AnimEvent.AnimEventTime.Percentage ? Float.toString(this.m_TimePc) : this.m_Time.name());
   }

   public static enum AnimEventTime {
      Percentage,
      Start,
      End;

      private AnimEventTime() {
      }
   }
}
