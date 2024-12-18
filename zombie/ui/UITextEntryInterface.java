package zombie.ui;

import zombie.core.Color;

public interface UITextEntryInterface {
   boolean isDoingTextEntry();

   void setDoingTextEntry(boolean var1);

   String getUIName();

   boolean isEditable();

   UINineGrid getFrame();

   boolean isIgnoreFirst();

   void setIgnoreFirst(boolean var1);

   void setSelectingRange(boolean var1);

   Color getStandardFrameColour();

   void onKeyEnter();

   void onKeyHome();

   void onKeyEnd();

   void onKeyUp();

   void onKeyDown();

   void onKeyLeft();

   void onKeyRight();

   void onKeyDelete();

   void onKeyBack();

   void pasteFromClipboard();

   void copyToClipboard();

   void cutToClipboard();

   void selectAll();

   boolean isTextLimit();

   boolean isOnlyNumbers();

   boolean isOnlyText();

   void onOtherKey(int var1);

   void putCharacter(char var1);
}
