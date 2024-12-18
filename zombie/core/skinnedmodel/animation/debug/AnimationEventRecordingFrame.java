package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimationEventRecordingFrame extends GenericNameValueRecordingFrame {
   private final ArrayList<AnimEvent> m_events = new ArrayList();
   private final ArrayList<String> m_tracks = new ArrayList();

   public AnimationEventRecordingFrame(String var1) {
      super(var1, "_events");
      this.addColumnInternal("animNode");
      this.addColumnInternal("track");
      this.addColumnInternal("animEvent.name");
      this.addColumnInternal("animEvent.time");
      this.addColumnInternal("animEvent.parameter");
   }

   public void logAnimEvent(AnimationTrack var1, AnimEvent var2) {
      this.m_tracks.add(var1 != null ? var1.getName() : "");
      this.m_events.add(var2);
   }

   public void reset() {
      this.m_events.clear();
   }

   public String getValueAt(int var1) {
      return "";
   }

   protected void onColumnAdded() {
   }

   protected void writeData(String var1, AnimEvent var2, StringBuilder var3) {
      appendCell(var3, var2.m_parentAnimNode != null ? var2.m_parentAnimNode.m_Name : "");
      appendCell(var3, var1);
      appendCell(var3, var2.m_EventName);
      if (var2.m_Time == AnimEvent.AnimEventTime.Percentage) {
         appendCell(var3, var2.m_TimePc);
      } else {
         appendCell(var3, var2.m_Time.toString());
      }

      appendCell(var3, var2.m_ParameterValue != null ? var2.m_ParameterValue : "");
   }

   protected void writeData() {
      if (this.m_outValues == null) {
         this.openValuesFile(false);
      }

      StringBuilder var1 = this.m_lineBuffer;

      for(int var2 = 0; var2 < this.m_events.size(); ++var2) {
         var1.setLength(0);
         AnimEvent var3 = (AnimEvent)this.m_events.get(var2);
         String var4 = (String)this.m_tracks.get(var2);
         this.writeData(var4, var3, var1);
         this.m_outValues.print(this.m_frameNumber);
         this.m_outValues.println(var1);
      }

   }
}
