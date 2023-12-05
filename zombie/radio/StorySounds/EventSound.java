package zombie.radio.StorySounds;

import java.util.ArrayList;
import zombie.core.Color;

public final class EventSound {
   protected String name;
   protected Color color;
   protected ArrayList<DataPoint> dataPoints;
   protected ArrayList<StorySound> storySounds;

   public EventSound() {
      this("Unnamed");
   }

   public EventSound(String var1) {
      this.color = new Color(1.0F, 1.0F, 1.0F);
      this.dataPoints = new ArrayList();
      this.storySounds = new ArrayList();
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color var1) {
      this.color = var1;
   }

   public ArrayList<DataPoint> getDataPoints() {
      return this.dataPoints;
   }

   public void setDataPoints(ArrayList<DataPoint> var1) {
      this.dataPoints = var1;
   }

   public ArrayList<StorySound> getStorySounds() {
      return this.storySounds;
   }

   public void setStorySounds(ArrayList<StorySound> var1) {
      this.storySounds = var1;
   }
}
