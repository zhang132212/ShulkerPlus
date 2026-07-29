package review;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.PermissionAttachment;

/** Test-only driver. Install exclusively in the isolated offline review server. */
public final class ReviewHarness extends JavaPlugin implements Listener {
    double balance = 100;
    int withdrawals, deposits;
    boolean failWithdraw, failDeposit;
    Object provider;
    Class<?> economy;
    PermissionAttachment attachment;
    Inventory prepared;
    Object imbueSession;
    String raceMode;

    @Override public void onEnable() {
        try {
            Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
            ClassLoader loader = vault.getClass().getClassLoader();
            economy = loader.loadClass("net.milkbowl.vault.economy.Economy");
            Class<?> response = loader.loadClass("net.milkbowl.vault.economy.EconomyResponse");
            Class<?> responseType = loader.loadClass("net.milkbowl.vault.economy.EconomyResponse$ResponseType");
            Constructor<?> result = response.getConstructor(double.class, double.class, responseType, String.class);
            provider = Proxy.newProxyInstance(loader, new Class[]{economy}, (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("toString") || name.equals("getName")) return "ReviewEconomy";
                if (name.equals("hashCode")) return System.identityHashCode(proxy);
                if (name.equals("equals")) return proxy == args[0];
                if (name.equals("getBalance")) return balance;
                if (name.equals("has")) return balance >= ((Number)args[args.length - 1]).doubleValue();
                if (name.equals("withdrawPlayer") || name.equals("depositPlayer")) {
                    double amount = ((Number)args[args.length - 1]).doubleValue();
                    boolean take = name.equals("withdrawPlayer");
                    boolean ok = take ? !failWithdraw && balance >= amount : !failDeposit;
                    if (take) withdrawals++; else deposits++;
                    if (ok) balance += take ? -amount : amount;
                    Object kind = Enum.valueOf((Class)responseType, ok ? "SUCCESS" : "FAILURE");
                    return result.newInstance(amount, balance, kind, ok ? null : "injected test failure");
                }
                if (method.getReturnType() == boolean.class) return true;
                if (method.getReturnType() == int.class) return 2;
                if (method.getReturnType() == double.class) return 0d;
                if (method.getReturnType() == String.class) return "review";
                if (List.class.isAssignableFrom(method.getReturnType())) return List.of();
                return null;
            });
            registerEconomy();
            Bukkit.getPluginManager().registerEvents(this, this);
            getCommand("reviewtest").setExecutor(this);
            for (String name : List.of("Vault", "ProtocolLib", "EnchantPlus", "ShulkerPlus", "MenuScrollGuard", "CraftEngine", "ModelEngine", "IllagerInvasionLeaves")) {
                Plugin p = Bukkit.getPluginManager().getPlugin(name);
                getLogger().info("REVIEW_PLUGIN " + name + "=" + (p != null && p.isEnabled()) + (p == null ? "" : " version=" + p.getDescription().getVersion()));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) void registerEconomy() {
        Bukkit.getServicesManager().register((Class)economy, provider, this, ServicePriority.Highest);
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player p = event.getPlayer(); p.setOp(false); p.setGameMode(GameMode.SURVIVAL);
        p.setInvulnerable(true); p.setAllowFlight(true); p.setFlying(true);
        p.teleport(p.getWorld().getSpawnLocation().add(0, 5, 0));
        attachment = p.addAttachment(this); attachment.setPermission("shulkerplus.use", true);
    }
    void reset(Player p) {
        p.closeInventory(); p.getInventory().clear(); p.setItemOnCursor(null);
        p.getInventory().setHeldItemSlot(0);
        balance = 100; withdrawals = deposits = 0; failWithdraw = failDeposit = false;
        attachment.setPermission("shulkerplus.use", true);
        attachment.setPermission("menuscrollguard.bypass", false);
        raceMode=null;
    }
    @EventHandler(priority=EventPriority.MONITOR) public void race(org.bukkit.event.player.PlayerInteractEvent event) {
        if(raceMode==null || event.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getHand()!=EquipmentSlot.HAND)return;
        Player p=event.getPlayer();
        if(raceMode.equals("replace"))p.getInventory().setItem(0,new ItemStack(Material.DIAMOND,42));
        if(raceMode.equals("drop"))p.getInventory().setItem(0,null);
        if(raceMode.equals("move"))p.getInventory().setHeldItemSlot(2);
        raceMode=null;
    }
    int count(Player p, Material material) {
        return Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull)
                .filter(i -> i.getType() == material).mapToInt(ItemStack::getAmount).sum();
    }
    void assertThat(Player p, String name, boolean condition) {
        String line = "REVIEW " + name + " " + (condition ? "PASS" : "FAIL");
        getLogger().info(line); p.sendMessage(line);
    }
    void snapshot(Player p, String name) {
        String line = "REVIEW " + name + " SNAPSHOT balance=" + balance + " withdrawals=" + withdrawals
                + " deposits=" + deposits + " books=" + count(p,Material.ENCHANTED_BOOK)
                + " diamonds=" + count(p,Material.DIAMOND) + " shulkers=" + count(p,Material.SHULKER_BOX)
                + " sticks=" + count(p,Material.STICK)
                + " input22=" + (prepared == null || prepared.getItem(22) == null ? 0 : prepared.getItem(22).getAmount())
                + " top=" + p.getOpenInventory().getTopInventory().getType()
                + " cursor=" + p.getItemOnCursor().getType() + ":" + p.getItemOnCursor().getAmount();
        getLogger().info(line); p.sendMessage(line);
    }
    void buy(Player p) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "enchantplus buy " + p.getName() + " minecraft:sharpness 1 25"); }
    void sell(Player p) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "enchantplus sell " + p.getName() + " diamond 70 2"); }
    void commands(Player p) {
        reset(p); Bukkit.dispatchCommand(p, "enchantplus buy " + p.getName() + " minecraft:sharpness 1 0");
        assertThat(p,"buy_permission",count(p,Material.ENCHANTED_BOOK)==0 && withdrawals==0);
        reset(p); buy(p); assertThat(p,"buy_success",count(p,Material.ENCHANTED_BOOK)==1 && balance==75 && withdrawals==1);
        reset(p); balance=1; buy(p); assertThat(p,"buy_insufficient",count(p,Material.ENCHANTED_BOOK)==0 && balance==1 && withdrawals==0);
        reset(p); failWithdraw=true; buy(p); assertThat(p,"buy_provider_failure",count(p,Material.ENCHANTED_BOOK)==0 && balance==100 && withdrawals==1);
        reset(p); Bukkit.getServicesManager().unregister(economy,provider); buy(p);
        assertThat(p,"buy_missing_provider",count(p,Material.ENCHANTED_BOOK)==0 && balance==100); registerEconomy();
        reset(p); p.getInventory().addItem(new ItemStack(Material.DIAMOND,70)); sell(p);
        assertThat(p,"sell_success",count(p,Material.DIAMOND)==0 && balance==240 && deposits==1);
        reset(p); p.getInventory().addItem(new ItemStack(Material.DIAMOND,70)); failDeposit=true; sell(p);
        assertThat(p,"sell_refund_70",count(p,Material.DIAMOND)==70 && balance==100 && deposits==1);
        reset(p); ItemStack custom=new ItemStack(Material.DIAMOND,70); ItemMeta meta=custom.getItemMeta(); meta.setDisplayName("custom"); custom.setItemMeta(meta); p.getInventory().addItem(custom); sell(p);
        assertThat(p,"sell_exact_metadata",count(p,Material.DIAMOND)==70 && deposits==0);
        p.sendMessage("REVIEW commands DONE");
    }
    void openGui(Player p, String mode) throws Exception {
        reset(p); failWithdraw=mode.equals("fail"); if(mode.equals("poor")) balance=1;
        Plugin ep=Bukkit.getPluginManager().getPlugin("EnchantPlus");
        Class<?> entry=ep.getClass().getClassLoader().loadClass("com.youulun.enchantplus.search.BookEntry");
        Object book=entry.getConstructor().newInstance();
        for(var e:Map.of("name",(Object)"Review Sharpness","ench","minecraft:sharpness","level",1,"price",25).entrySet()) {
            Field f=entry.getDeclaredField(e.getKey());f.setAccessible(true);f.set(book,e.getValue());
        }
        Object gui=ep.getClass().getMethod("getSearchGUI").invoke(ep);
        gui.getClass().getMethod("open",Player.class,List.class,String.class).invoke(gui,p,List.of(book),"review");
        prepared=p.getOpenInventory().getTopInventory();
    }
    ItemStack box() {
        ItemStack item=new ItemStack(Material.SHULKER_BOX);BlockStateMeta meta=(BlockStateMeta)item.getItemMeta();
        org.bukkit.block.ShulkerBox box=(org.bukkit.block.ShulkerBox)meta.getBlockState();box.getInventory().setItem(0,new ItemStack(Material.DIAMOND,8));
        meta.setBlockState(box);item.setItemMeta(meta);return item;
    }
    void prepareShulker(Player p,String mode) throws Exception {
        reset(p); Plugin sp=Bukkit.getPluginManager().getPlugin("ShulkerPlus");
        Field nested=sp.getClass().getDeclaredField("enableNestedOpening");nested.setAccessible(true);nested.setBoolean(sp,!mode.equals("nestedoff"));
        if(mode.equals("denied")) attachment.setPermission("shulkerplus.use",false);
        if(Set.of("replace","drop","move").contains(mode))raceMode=mode;
        p.getInventory().setItem(0,mode.startsWith("nested")?new ItemStack(Material.CRAFTING_TABLE):box());
        p.getInventory().setItem(1,box());p.updateInventory();
    }
    void imbue(Player p) throws Exception {
        reset(p); Plugin plugin=Bukkit.getPluginManager().getPlugin("IllagerInvasionLeaves");
        attachment.setPermission("menuscrollguard.bypass",true);
        ClassLoader cl=plugin.getClass().getClassLoader();Class<?> session=cl.loadClass("dev.zhe.illagerinvasion.block.ImbuingTableListener$Session");
        Constructor<?> ctor=session.getDeclaredConstructor();ctor.setAccessible(true);imbueSession=ctor.newInstance();
        prepared=((InventoryHolder)imbueSession).getInventory();
        ItemStack book=new ItemStack(Material.ENCHANTED_BOOK);EnchantmentStorageMeta bm=(EnchantmentStorageMeta)book.getItemMeta();
        bm.addStoredEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING,1,true);book.setItemMeta(bm);
        ItemStack items=new ItemStack(Material.STICK,8);items.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING,1);
        Class<?> itemsClass=cl.loadClass("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
        Class<?> keyClass=cl.loadClass("net.momirealms.craftengine.core.util.Key");Object key=keyClass.getMethod("of",String.class).invoke(null,"illagerinvasion:hallowed_gem");
        Object def=itemsClass.getMethod("byId",keyClass).invoke(null,key);
        if(def==null)throw new IllegalStateException("Missing real CraftEngine hallowed_gem");
        ItemStack gem=(ItemStack)def.getClass().getMethod("buildBukkitItem").invoke(def);gem.setAmount(2);
        prepared.setItem(19,book);prepared.setItem(22,items);prepared.setItem(25,gem);
        Method update=session.getDeclaredMethod("updateResult");update.setAccessible(true);update.invoke(imbueSession);
        p.openInventory(prepared);
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player p)||args.length==0)return true;
        try {
            String mode=args.length>1?args[1]:"success";
            switch(args[0]) {
                case "commands" -> Bukkit.getScheduler().runTask(this, () -> commands(p));
                case "content" -> {Plugin ce=Bukkit.getPluginManager().getPlugin("CraftEngine");ClassLoader cl=ce.getClass().getClassLoader();Class<?> key=cl.loadClass("net.momirealms.craftengine.core.util.Key");Class<?> blocks=cl.loadClass("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");for(String id:List.of("illagerinvasion:imbuing_table","illagerinvasion:magic_fire")){Object k=key.getMethod("of",String.class).invoke(null,id);assertThat(p,"registered_"+id,blocks.getMethod("byId",key).invoke(null,k)!=null);}}
                case "gui" -> {openGui(p,mode);p.sendMessage("REVIEW gui READY");}
                case "shulker" -> {prepareShulker(p,mode);p.sendMessage("REVIEW shulker READY");}
                case "guard" -> {reset(p);prepared=Bukkit.createInventory(null,27,"Review virtual menu");prepared.setItem(0,new ItemStack(Material.DIAMOND,8));p.openInventory(prepared);p.sendMessage("REVIEW guard READY");}
                case "imbue" -> {imbue(p);if(mode.equals("collect")){ItemStack output=prepared.getItem(4).clone();p.getInventory().setItem(9,output);p.setItemOnCursor(output.clone());}p.sendMessage("REVIEW imbue READY");}
                case "check" -> {snapshot(p,mode);switch(mode) {
                    case "gui_success" -> assertThat(p,mode,balance==75&&withdrawals==1&&count(p,Material.ENCHANTED_BOOK)==1);
                    case "gui_fail" -> assertThat(p,mode,balance==100&&withdrawals==1&&count(p,Material.ENCHANTED_BOOK)==0);
                    case "guard_shift", "guard_pickup" -> assertThat(p,mode,count(p,Material.DIAMOND)==0&&prepared.getItem(0).getAmount()==8&&p.getItemOnCursor().getType().isAir());
                    case "shulker_open" -> {Object handle=p.getClass().getMethod("getHandle").invoke(p);Object menu=handle.getClass().getField("containerMenu").get(handle);assertThat(p,mode,menu.getClass().getSimpleName().equals("ShulkerBoxMenu"));}
                    case "shulker_denied" -> assertThat(p,mode,p.getOpenInventory().getTopInventory().getType()==org.bukkit.event.inventory.InventoryType.CRAFTING);
                    case "nestedoff" -> assertThat(p,mode,p.getOpenInventory().getTopInventory().getType()==org.bukkit.event.inventory.InventoryType.WORKBENCH);
                    case "nestedon" -> {Object handle=p.getClass().getMethod("getHandle").invoke(p);Object menu=handle.getClass().getField("containerMenu").get(handle);assertThat(p,mode,menu.getClass().getSimpleName().equals("ShulkerBoxMenu"));}
                    case "shulker_no_nesting" -> assertThat(p,mode,count(p,Material.SHULKER_BOX)==2 && Arrays.stream(p.getOpenInventory().getTopInventory().getContents()).filter(Objects::nonNull).noneMatch(i->i.getType()==Material.SHULKER_BOX));
                    case "race_replace" -> assertThat(p,mode,p.getOpenInventory().getTopInventory().getType()==org.bukkit.event.inventory.InventoryType.CRAFTING && count(p,Material.DIAMOND)==42 && count(p,Material.SHULKER_BOX)==1);
                    case "race_drop" -> assertThat(p,mode,p.getOpenInventory().getTopInventory().getType()==org.bukkit.event.inventory.InventoryType.CRAFTING && count(p,Material.SHULKER_BOX)==1);
                    case "race_move" -> assertThat(p,mode,p.getOpenInventory().getTopInventory().getType()==org.bukkit.event.inventory.InventoryType.CRAFTING && p.getInventory().getHeldItemSlot()==2 && count(p,Material.SHULKER_BOX)==2);
                    case "shulker_conserve" -> {ItemStack b=p.getInventory().getItem(0);int inside=b!=null&&b.getItemMeta() instanceof BlockStateMeta bm ? Arrays.stream(((org.bukkit.block.ShulkerBox)bm.getBlockState()).getInventory().getContents()).filter(Objects::nonNull).filter(i->i.getType()==Material.DIAMOND).mapToInt(ItemStack::getAmount).sum():-100;assertThat(p,mode,inside+count(p,Material.DIAMOND)==8);}
                    case "imbue_stack" -> assertThat(p,mode,prepared.getItem(22)!=null&&prepared.getItem(22).getAmount()==7&&count(p,Material.STICK)==1);
                    case "imbue_collect" -> assertThat(p,mode,prepared.getItem(22).getAmount()==8&&prepared.getItem(4)!=null&&p.getItemOnCursor().getAmount()==1&&count(p,Material.STICK)==1);
                }}
                case "stop" -> {getLogger().info("REVIEW COMPLETE");Bukkit.getScheduler().runTaskLater(this,Bukkit::shutdown,5);}
            }
        }catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"REVIEW ERROR",e);p.sendMessage("REVIEW ERROR "+e);}
        return true;
    }
}
