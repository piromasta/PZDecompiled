package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.Resources;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.PlayerID;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftLogicScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
public class CraftLogic extends Component {
   private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList());
   private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList());
   private String recipeTagQuery;
   private List<CraftRecipe> recipes;
   private StartMode startMode;
   private CraftRecipe currentRecipe;
   private int elapsedTime;
   private boolean doAutomaticCraftCheck;
   private boolean startRequested;
   private boolean stopRequested;
   private IsoPlayer requestingPlayer;
   private final CraftRecipeData craftData;
   private final CraftRecipeData craftTestData;
   private String inputsGroupName;
   private String outputsGroupName;

   private CraftLogic() {
      super(ComponentType.CraftLogic);
      this.startMode = StartMode.Manual;
      this.elapsedTime = 0;
      this.doAutomaticCraftCheck = true;
      this.startRequested = false;
      this.stopRequested = false;
      this.requestingPlayer = null;
      this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
      this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      CraftLogicScript var2 = (CraftLogicScript)var1;
      this.startMode = var2.getStartMode();
      this.recipeTagQuery = null;
      this.setRecipeTagQuery(var2.getRecipeTagQuery());
      this.inputsGroupName = var2.getInputsGroupName();
      this.outputsGroupName = var2.getOutputsGroupName();
      this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
   }

   public boolean isValid() {
      return super.isValid() && this.owner.hasComponent(ComponentType.Resources);
   }

   protected void reset() {
      super.reset();
      this.recipeTagQuery = null;
      this.recipes = null;
      this.craftData.reset();
      this.craftTestData.reset();
      this.startMode = StartMode.Manual;
      this.doAutomaticCraftCheck = true;
      this.startRequested = false;
      this.stopRequested = false;
      this.requestingPlayer = null;
      this.inputsGroupName = null;
      this.outputsGroupName = null;
      this.clearRecipe();
   }

   private void clearRecipe() {
      this.elapsedTime = 0;
      this.currentRecipe = null;
   }

   public StartMode getStartMode() {
      return this.startMode;
   }

   public int getElapsedTime() {
      return this.elapsedTime;
   }

   void setElapsedTime(int var1) {
      this.elapsedTime = var1;
   }

   public boolean isStartRequested() {
      return this.startRequested;
   }

   void setStartRequested(boolean var1) {
      this.startRequested = var1;
   }

   public boolean isStopRequested() {
      return this.stopRequested;
   }

   void setStopRequested(boolean var1) {
      this.stopRequested = var1;
   }

   public IsoPlayer getRequestingPlayer() {
      return this.requestingPlayer;
   }

   void setRequestingPlayer(IsoPlayer var1) {
      this.requestingPlayer = var1;
   }

   public boolean isDoAutomaticCraftCheck() {
      return this.doAutomaticCraftCheck;
   }

   void setDoAutomaticCraftCheck(boolean var1) {
      this.doAutomaticCraftCheck = var1;
   }

   CraftRecipeData getCraftData() {
      return this.craftData;
   }

   CraftRecipeData getCraftTestData() {
      return this.craftTestData;
   }

   public String getInputsGroupName() {
      return this.inputsGroupName;
   }

   public String getOutputsGroupName() {
      return this.outputsGroupName;
   }

   public String getRecipeTagQuery() {
      return this.recipeTagQuery;
   }

   public void setRecipeTagQuery(String var1) {
      if (this.recipeTagQuery == null || !this.recipeTagQuery.equalsIgnoreCase(var1)) {
         this.recipeTagQuery = var1;
         this.recipes = null;
         if (!StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            this.recipes = CraftRecipeManager.queryRecipes(var1);
         }
      }

   }

   public ArrayList<CraftRecipe> getRecipes(ArrayList<CraftRecipe> var1) {
      var1.clear();
      if (this.recipes != null) {
         var1.addAll(this.recipes);
      }

      return var1;
   }

   public List<CraftRecipe> getRecipes() {
      return this.recipes != null ? this.recipes : _emptyRecipeList;
   }

   public List<Resource> getInputResources() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         ResourceGroup var2 = var1.getResourceGroup(this.inputsGroupName);
         if (var2 != null) {
            return var2.getResources();
         }
      }

      return _emptyResourceList;
   }

   public List<Resource> getOutputResources() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         ResourceGroup var2 = var1.getResourceGroup(this.outputsGroupName);
         if (var2 != null) {
            return var2.getResources();
         }
      }

      return _emptyResourceList;
   }

   public boolean isRunning() {
      return this.currentRecipe != null;
   }

   public boolean isFinished() {
      if (this.isRunning()) {
         return this.elapsedTime >= this.currentRecipe.getTime();
      } else {
         return false;
      }
   }

   public CraftRecipe getCurrentRecipe() {
      return this.currentRecipe;
   }

   public double getProgress() {
      if (this.isRunning() && this.elapsedTime != 0) {
         return this.elapsedTime >= this.currentRecipe.getTime() ? 1.0 : (double)this.elapsedTime / (double)this.currentRecipe.getTime();
      } else {
         return 0.0;
      }
   }

   public void setRecipe(CraftRecipe var1) {
      if (var1 == null) {
         this.clearRecipe();
      } else if (this.currentRecipe != var1) {
         this.clearRecipe();
         this.currentRecipe = var1;
         this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
         this.craftData.setRecipe(this.currentRecipe);
         this.craftTestData.setRecipe(this.currentRecipe);
      }

   }

   public CraftRecipe getPossibleRecipe() {
      List var1 = this.getInputResources();
      List var2 = this.getOutputResources();
      return var1 != null && !var1.isEmpty() ? CraftUtil.getPossibleRecipe(this.craftTestData, this.recipes, var1, var2) : null;
   }

   protected void onRemovedFromOwner() {
   }

   protected void onConnectComponents() {
   }

   public CraftRecipeMonitor debugCanStart(IsoPlayer var1) {
      CraftRecipeMonitor var3;
      try {
         DebugLog.General.println("=== Starting debug canStart test ===");
         CraftRecipeMonitor var2 = CraftRecipeMonitor.Create();
         if (!this.isValid()) {
            var2.warn("Unable to start (not valid).");
            var2.close();
            var3 = var2.seal();
            return var3;
         }

         if (this.startMode != StartMode.Manual || this.owner.isUsingPlayer(var1)) {
            var2.logCraftLogic(this);
            List var11 = this.getInputResources();
            List var4 = this.getOutputResources();
            var2.logResources(var11, var4);
            CraftRecipeMonitor var5 = CraftUtil.debugCanStart(var1, this.craftTestData, this.recipes, var11, var4, var2);
            return var5;
         }

         var2.warn("Player is not the using player.");
         var2.close();
         var3 = var2.seal();
      } catch (Exception var9) {
         var9.printStackTrace();
         return null;
      } finally {
         this.craftData.setMonitor((CraftRecipeMonitor)null);
         this.craftTestData.setMonitor((CraftRecipeMonitor)null);
      }

      return var3;
   }

   public boolean canStart(IsoPlayer var1) {
      return this.canStart(StartMode.Manual, var1);
   }

   protected boolean canStart(StartMode var1, IsoPlayer var2) {
      if (!this.isValid()) {
         return false;
      } else if (!this.isRunning() && this.startMode != StartMode.Passive) {
         if (this.startMode != var1) {
            return false;
         } else if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(var2)) {
            return false;
         } else {
            List var3 = this.getInputResources();
            List var4 = this.getOutputResources();
            if (var3 != null && !var3.isEmpty()) {
               return CraftUtil.canStart(this.craftTestData, this.recipes, var3, var4);
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void start(IsoPlayer var1) {
      if (this.startMode != StartMode.Manual || this.owner.isUsingPlayer(var1)) {
         if (GameClient.bClient) {
            if (this.canStart(StartMode.Manual, var1)) {
               this.sendStartRequest(var1);
            }
         } else {
            this.startRequested = true;
            this.requestingPlayer = var1;
         }

      }
   }

   public void stop(IsoPlayer var1) {
      this.stop(var1, false);
   }

   public void stop(IsoPlayer var1, boolean var2) {
      if (this.isValid()) {
         if (this.startMode != StartMode.Manual || var2 || this.owner.isUsingPlayer(var1)) {
            if (GameClient.bClient) {
               this.sendStopRequest(var1);
            } else {
               this.stopRequested = true;
               this.requestingPlayer = var1;
            }

         }
      }
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         case CraftLogicStartRequest:
            this.receiveStartRequest(var1, var3);
            return true;
         case CraftLogicStopRequest:
            this.receiveStopRequest(var1, var3);
            return true;
         default:
            return false;
      }
   }

   public void sendStartRequest(IsoPlayer var1) {
      if (GameClient.bClient) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStartRequest);
         var2.bb.put((byte)(var1 != null ? 1 : 0));
         if (var1 != null) {
            PlayerID var3 = new PlayerID();
            var3.set(var1);
            var3.write(var2.bb);
         }

         this.sendClientPacket(var2);
      }
   }

   protected void receiveStartRequest(ByteBuffer var1, UdpConnection var2) throws IOException {
      if (GameServer.bServer) {
         IsoPlayer var3 = null;
         if (var1.get() == 1) {
            PlayerID var4 = new PlayerID();
            var4.parse(var1, var2);
            var4.parsePlayer(var2);
            var3 = var4.getPlayer();
            if (var3 == null) {
               throw new IOException("Player not found.");
            }
         }

         this.start(var3);
      }
   }

   public void sendStopRequest(IsoPlayer var1) {
      if (GameClient.bClient) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStopRequest);
         var2.bb.put((byte)(var1 != null ? 1 : 0));
         if (var1 != null) {
            PlayerID var3 = new PlayerID();
            var3.set(var1);
            var3.write(var2.bb);
         }

         this.sendClientPacket(var2);
      }
   }

   protected void receiveStopRequest(ByteBuffer var1, UdpConnection var2) throws IOException {
      if (GameServer.bServer) {
         IsoPlayer var3 = null;
         if (var1.get() == 1) {
            PlayerID var4 = new PlayerID();
            var4.parse(var1, var2);
            var4.parsePlayer(var2);
            var3 = var4.getPlayer();
            if (var3 == null) {
               throw new IOException("Player not found.");
            }
         }

         this.stop(var3);
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   protected void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (this.recipeTagQuery != null) {
         var2.addFlags(1);
         GameWindow.WriteString(var1, this.recipeTagQuery);
      }

      if (this.startMode != StartMode.Manual) {
         var2.addFlags(2);
         var1.put(this.startMode.getByteId());
      }

      if (this.inputsGroupName != null) {
         var2.addFlags(4);
         GameWindow.WriteString(var1, this.inputsGroupName);
      }

      if (this.outputsGroupName != null) {
         var2.addFlags(8);
         GameWindow.WriteString(var1, this.outputsGroupName);
      }

      if (this.currentRecipe != null) {
         var2.addFlags(16);
         GameWindow.WriteString(var1, this.currentRecipe.getScriptObjectFullType());
         var1.putLong(this.currentRecipe.getScriptVersion());
         var1.putInt(this.elapsedTime);
         this.craftData.save(var1);
      }

      var2.write();
      var2.release();
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      this.recipeTagQuery = null;
      this.startMode = StartMode.Manual;
      this.inputsGroupName = null;
      this.outputsGroupName = null;
      boolean var4 = false;
      String var5;
      if (var3.hasFlags(1)) {
         var5 = GameWindow.ReadString(var1);
         this.setRecipeTagQuery(var5);
      }

      if (var3.hasFlags(2)) {
         this.startMode = StartMode.fromByteId(var1.get());
      }

      if (var3.hasFlags(4)) {
         this.inputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(8)) {
         this.outputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(16)) {
         var5 = GameWindow.ReadString(var1);
         CraftRecipe var6 = ScriptManager.instance.getCraftRecipe(var5);
         this.setRecipe(var6);
         long var7 = var1.getLong();
         if (var6 == null || var7 != var6.getScriptVersion()) {
            var4 = true;
            DebugLog.General.warn("CraftRecipe '" + var5 + "' is null (" + (var6 == null) + ", or has script version mismatch. Cancelling current craft.");
         }

         this.elapsedTime = var1.getInt();
         if (this.currentRecipe == null) {
            this.elapsedTime = 0;
         }

         if (!this.craftData.load(var1, var2, var6, var4)) {
            var4 = true;
            this.craftData.setRecipe((CraftRecipe)null);
         }
      } else {
         this.setRecipe((CraftRecipe)null);
         this.elapsedTime = 0;
      }

      var3.release();
      if (var4) {
         this.clearRecipe();
      }

      this.doAutomaticCraftCheck = true;
   }
}
